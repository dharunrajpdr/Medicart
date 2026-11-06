package dao;

import entity.model.Medicine;
import entity.model.Order;
import entity.model.Prescription;
import entity.model.Stock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class ConcurrentPharmacyOperations implements AutoCloseable {
    private static final int DEFAULT_WORKER_COUNT = 4;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final IMedicineService medicineService;
    private final IStockService stockService;
    private final IPrescriptionService prescriptionService;
    private final IOrderService orderService;
    private final ReportGenerator reportGenerator = new ReportGenerator();
    private final ExecutorService executor;

    public ConcurrentPharmacyOperations(IMedicineService medicineService,
            IStockService stockService, IPrescriptionService prescriptionService,
            IOrderService orderService) {
        this(medicineService, stockService, prescriptionService, orderService,
                DEFAULT_WORKER_COUNT);
    }

    public ConcurrentPharmacyOperations(IMedicineService medicineService,
            IStockService stockService, IPrescriptionService prescriptionService,
            IOrderService orderService, int workerCount) {
        this.medicineService = Objects.requireNonNull(
                medicineService, "medicineService cannot be null");
        this.stockService = Objects.requireNonNull(stockService, "stockService cannot be null");
        this.prescriptionService = Objects.requireNonNull(
                prescriptionService, "prescriptionService cannot be null");
        this.orderService = Objects.requireNonNull(orderService, "orderService cannot be null");
        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be positive");
        }
        this.executor = Executors.newFixedThreadPool(workerCount, workerThreadFactory());
    }

    public StockCheck checkOrderStockAvailability(Order order)
            throws InterruptedException, ExecutionException {
        Objects.requireNonNull(order, "order cannot be null");
        Map<StockKey, Integer> requested = new LinkedHashMap<>();
        for (Medicine medicine : order.getItems()) {
            if (medicine.getMedicineId() <= 0 || medicine.getWarehouseId() <= 0) {
                throw new IllegalArgumentException(
                        "Order medicines must have persisted medicine and warehouse IDs");
            }
            StockKey key = new StockKey(medicine.getMedicineId(), medicine.getWarehouseId());
            requested.merge(key, 1, Math::addExact);
        }

        List<Callable<StockAvailability>> tasks = new ArrayList<>();
        for (Map.Entry<StockKey, Integer> entry : requested.entrySet()) {
            StockKey key = entry.getKey();
            int requestedQuantity = entry.getValue();
            tasks.add(() -> {
                int availableQuantity = 0;
                for (Stock stock : stockService.getAllStocks()) {
                    if (stock.getMedicine().getMedicineId() == key.medicineId
                            && stock.getWarehouse().getWarehouseId() == key.warehouseId) {
                        availableQuantity = stock.getQuantityAvailable();
                        break;
                    }
                }
                return new StockAvailability(key.medicineId, key.warehouseId,
                        requestedQuantity, availableQuantity);
            });
        }

        List<StockAvailability> availability = invokeAll(tasks);
        return new StockCheck(availability);
    }

    public List<Medicine> scanNearExpiry(int days)
            throws InterruptedException, ExecutionException {
        if (days < 0) {
            throw new IllegalArgumentException("days cannot be negative");
        }
        List<Medicine> medicines = medicineService.getAllMedicines();
        List<Callable<Medicine>> tasks = new ArrayList<>(medicines.size());
        for (Medicine medicine : medicines) {
            tasks.add(() -> medicineService.isNearExpiry(medicine, days) ? medicine : null);
        }

        List<Medicine> nearExpiry = new ArrayList<>();
        for (Medicine medicine : invokeAll(tasks)) {
            if (medicine != null) {
                nearExpiry.add(medicine);
            }
        }
        return reportGenerator.getMedicinesNearExpiry(nearExpiry, days);
    }

    /**
     * The reviewer predicate is invoked concurrently and must be safe for concurrent calls.
     */
    public List<PrescriptionReview> processPendingVerifications(
            Predicate<Prescription> reviewerApproves)
            throws InterruptedException, ExecutionException {
        Objects.requireNonNull(reviewerApproves, "reviewerApproves cannot be null");
        List<Prescription> pending = new ArrayList<>();
        for (Prescription prescription : prescriptionService.getAllPrescriptions()) {
            if ("Pending".equals(prescription.getStatus())) {
                pending.add(prescription);
            }
        }

        List<Callable<PrescriptionReview>> tasks = new ArrayList<>(pending.size());
        for (Prescription prescription : pending) {
            tasks.add(() -> {
                boolean approved = reviewerApproves.test(prescription);
                if (approved) {
                    prescriptionService.approve(prescription);
                } else {
                    prescriptionService.reject(prescription);
                }
                return new PrescriptionReview(prescription.getPrescriptionId(),
                        prescription.getStatus(), Thread.currentThread().getName());
            });
        }

        List<PrescriptionReview> reviews = invokeAll(tasks);
        reviews.sort(Comparator.comparingInt(PrescriptionReview::getPrescriptionId));
        return reviews;
    }

    public OperationsDashboard loadDashboard(int lowStockThreshold, int nearExpiryDays)
            throws InterruptedException, ExecutionException {
        if (lowStockThreshold < 0) {
            throw new IllegalArgumentException("lowStockThreshold cannot be negative");
        }
        if (nearExpiryDays < 0) {
            throw new IllegalArgumentException("nearExpiryDays cannot be negative");
        }

        Instant startedAt = Instant.now();
        LocalDate today = LocalDate.now();
        Future<List<Stock>> stocksTask = executor.submit(stockService::getAllStocks);
        Future<List<Medicine>> medicinesTask = executor.submit(medicineService::getAllMedicines);
        Future<List<Prescription>> prescriptionsTask =
                executor.submit(prescriptionService::getAllPrescriptions);
        Future<List<Order>> ordersTask = executor.submit(orderService::getAllOrders);
        Future<?>[] tasks = {
            stocksTask, medicinesTask, prescriptionsTask, ordersTask
        };

        try {
            List<Stock> lowStock = reportGenerator.getLowStockMedicines(
                    stocksTask.get(), lowStockThreshold);
            List<Medicine> nearExpiry = reportGenerator.getMedicinesNearExpiry(
                    medicinesTask.get(), nearExpiryDays);
            List<Prescription> pending = reportGenerator.getPendingPrescriptions(
                    prescriptionsTask.get());
            List<Order> todayOrders = new ArrayList<>();
            for (Order order : ordersTask.get()) {
                if (order.getOrderDate().toLocalDate().equals(today)) {
                    todayOrders.add(order);
                }
            }
            todayOrders.sort(Comparator.comparing(Order::getOrderDate)
                    .thenComparingInt(Order::getOrderId));
            return new OperationsDashboard(lowStock, nearExpiry, pending, todayOrders,
                    startedAt, Instant.now());
        } catch (InterruptedException | ExecutionException exception) {
            for (Future<?> task : tasks) {
                task.cancel(true);
            }
            throw exception;
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent workers did not terminate");
                }
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping concurrent workers",
                    exception);
        }
    }

    private <T> List<T> invokeAll(List<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(task));
            }
            List<T> results = new ArrayList<>(futures.size());
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException | ExecutionException exception) {
            for (Future<?> future : futures) {
                future.cancel(true);
            }
            throw exception;
        }
    }

    private static ThreadFactory workerThreadFactory() {
        AtomicInteger workerNumber = new AtomicInteger(1);
        return task -> new Thread(task, "medicart-worker-" + workerNumber.getAndIncrement());
    }

    public static final class StockCheck {
        private final List<StockAvailability> items;

        private StockCheck(List<StockAvailability> items) {
            this.items = List.copyOf(items);
        }

        public List<StockAvailability> getItems() {
            return items;
        }

        public boolean canFulfillOrder() {
            return !items.isEmpty() && items.stream().allMatch(StockAvailability::isAvailable);
        }
    }

    public static final class StockAvailability {
        private final int medicineId;
        private final int warehouseId;
        private final int requestedQuantity;
        private final int availableQuantity;

        private StockAvailability(int medicineId, int warehouseId,
                int requestedQuantity, int availableQuantity) {
            this.medicineId = medicineId;
            this.warehouseId = warehouseId;
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
        }

        public int getMedicineId() {
            return medicineId;
        }

        public int getWarehouseId() {
            return warehouseId;
        }

        public int getRequestedQuantity() {
            return requestedQuantity;
        }

        public int getAvailableQuantity() {
            return availableQuantity;
        }

        public boolean isAvailable() {
            return availableQuantity >= requestedQuantity;
        }
    }

    public static final class PrescriptionReview {
        private final int prescriptionId;
        private final String status;
        private final String reviewerThread;

        private PrescriptionReview(int prescriptionId, String status, String reviewerThread) {
            this.prescriptionId = prescriptionId;
            this.status = status;
            this.reviewerThread = reviewerThread;
        }

        public int getPrescriptionId() {
            return prescriptionId;
        }

        public String getStatus() {
            return status;
        }

        public String getReviewerThread() {
            return reviewerThread;
        }
    }

    public static final class OperationsDashboard {
        private final List<Stock> lowStockMedicines;
        private final List<Medicine> nearExpiryMedicines;
        private final List<Prescription> pendingPrescriptions;
        private final List<Order> todayOrders;
        private final Instant startedAt;
        private final Instant completedAt;

        private OperationsDashboard(List<Stock> lowStockMedicines,
                List<Medicine> nearExpiryMedicines, List<Prescription> pendingPrescriptions,
                List<Order> todayOrders, Instant startedAt, Instant completedAt) {
            this.lowStockMedicines = List.copyOf(lowStockMedicines);
            this.nearExpiryMedicines = List.copyOf(nearExpiryMedicines);
            this.pendingPrescriptions = List.copyOf(pendingPrescriptions);
            this.todayOrders = List.copyOf(todayOrders);
            this.startedAt = startedAt;
            this.completedAt = completedAt;
        }

        public List<Stock> getLowStockMedicines() {
            return lowStockMedicines;
        }

        public List<Medicine> getNearExpiryMedicines() {
            return nearExpiryMedicines;
        }

        public List<Prescription> getPendingPrescriptions() {
            return pendingPrescriptions;
        }

        public List<Order> getTodayOrders() {
            return todayOrders;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public Instant getCompletedAt() {
            return completedAt;
        }
    }

    private static final class StockKey {
        private final int medicineId;
        private final int warehouseId;

        private StockKey(int medicineId, int warehouseId) {
            this.medicineId = medicineId;
            this.warehouseId = warehouseId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof StockKey)) {
                return false;
            }
            StockKey key = (StockKey) other;
            return medicineId == key.medicineId && warehouseId == key.warehouseId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(medicineId, warehouseId);
        }
    }
}
