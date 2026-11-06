package concurrency;

import dao.ConcurrentPharmacyOperations;
import dao.MedicineService;
import dao.OrderService;
import dao.PrescriptionService;
import dao.StockService;
import dao.WarehouseService;
import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
import entity.model.OverTheCounterMedicine;
import entity.model.Prescription;
import entity.model.Stock;
import entity.model.Warehouse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ConcurrentPharmacyOperationsChecks {
    private static int checks;

    private ConcurrentPharmacyOperationsChecks() {
    }

    public static void main(String[] args) throws Exception {
        MedicineService medicineService = new MedicineService();
        OrderService orderService = new OrderService();
        PrescriptionService prescriptionService = new PrescriptionService();
        StockService stockService = new StockService();
        WarehouseService warehouseService = new WarehouseService();
        Customer customer = customer(1);
        Warehouse firstWarehouse = warehouse(1);
        Warehouse secondWarehouse = warehouse(2);
        warehouseService.addWarehouse(firstWarehouse);
        warehouseService.addWarehouse(secondWarehouse);

        Medicine nearExpiry = medicine(0, "Near expiry", 1, LocalDate.now().plusDays(3));
        Medicine regular = medicine(0, "Regular", 2, LocalDate.now().plusDays(30));
        medicineService.addMedicine(nearExpiry);
        medicineService.addMedicine(regular);
        stockService.addStock(new Stock(0, nearExpiry, firstWarehouse, 5));
        stockService.addStock(new Stock(0, regular, secondWarehouse, 1));

        Order todayOrder = order(0, customer, List.of(nearExpiry, nearExpiry, regular),
                LocalDateTime.now());
        orderService.addOrder(todayOrder);
        orderService.addOrder(order(0, customer, List.of(regular),
                LocalDateTime.now().minusDays(1)));

        List<Prescription> pending = new ArrayList<>();
        for (int id = 1; id <= 4; id++) {
            Prescription prescription = pendingPrescription(id, customer);
            prescriptionService.addPrescription(prescription);
            pending.add(prescription);
        }
        Prescription approved = pendingPrescription(5, customer);
        prescriptionService.addPrescription(approved);
        prescriptionService.approve(approved);

        try (ConcurrentPharmacyOperations operations = new ConcurrentPharmacyOperations(
                medicineService, stockService, prescriptionService, orderService, 4)) {
            checkStockAvailability(operations, todayOrder);
            checkExpirySweep(operations, nearExpiry);
            checkPrescriptionQueue(operations, pending);
            checkDashboard(operations, todayOrder, nearExpiry, regular, pending);
        }
        checkConstructorValidation(medicineService, stockService,
                prescriptionService, orderService);
        System.out.println("Concurrent pharmacy checks passed: " + checks);
    }

    private static void checkStockAvailability(
            ConcurrentPharmacyOperations operations, Order order) throws Exception {
        ConcurrentPharmacyOperations.StockCheck result =
                operations.checkOrderStockAvailability(order);
        check(result.getItems().size() == 2, "stock lookup groups repeated medicine entries");
        check(result.getItems().get(0).getRequestedQuantity() == 2,
                "stock lookup counts each order item occurrence");
        check(result.canFulfillOrder(), "stock lookup confirms sufficient quantities");
        check(!operations.checkOrderStockAvailability(
                order(0, customer(2), List.of(order.getItems().get(0),
                        order.getItems().get(0), order.getItems().get(0),
                        order.getItems().get(0), order.getItems().get(0),
                        order.getItems().get(0)),
                        LocalDateTime.now())).canFulfillOrder(),
                "stock lookup rejects insufficient quantities");
        check(!operations.checkOrderStockAvailability(
                order(0, customer(3), List.of(), LocalDateTime.now())).canFulfillOrder(),
                "an empty order is not fulfillable");
    }

    private static void checkExpirySweep(
            ConcurrentPharmacyOperations operations, Medicine nearExpiry) throws Exception {
        List<Medicine> result = operations.scanNearExpiry(5);
        check(result.size() == 1 && result.get(0) == nearExpiry,
                "parallel expiry sweep returns only near-expiry medicines");
        expectThrows(IllegalArgumentException.class, () -> operations.scanNearExpiry(-1));
    }

    private static void checkPrescriptionQueue(ConcurrentPharmacyOperations operations,
            List<Prescription> pending) throws Exception {
        CountDownLatch reviewersStarted = new CountDownLatch(2);
        List<ConcurrentPharmacyOperations.PrescriptionReview> reviews =
                operations.processPendingVerifications(prescription -> {
                    reviewersStarted.countDown();
                    try {
                        if (!reviewersStarted.await(2, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Prescription reviews did not run concurrently");
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Reviewer was interrupted", exception);
                    }
                    return prescription.getPrescriptionId() % 2 == 0;
                });
        Set<String> reviewerThreads = new HashSet<>();
        for (ConcurrentPharmacyOperations.PrescriptionReview review : reviews) {
            reviewerThreads.add(review.getReviewerThread());
            String expectedStatus = review.getPrescriptionId() % 2 == 0
                    ? "Approved" : "Rejected";
            check(expectedStatus.equals(review.getStatus()),
                    "review result reflects the pharmacist decision");
        }
        check(reviews.size() == pending.size(), "all pending prescriptions are processed");
        check(reviewerThreads.size() > 1, "prescription reviews use multiple worker threads");
    }

    private static void checkDashboard(ConcurrentPharmacyOperations operations, Order todayOrder,
            Medicine nearExpiry, Medicine regular, List<Prescription> pending) throws Exception {
        ConcurrentPharmacyOperations.OperationsDashboard dashboard =
                operations.loadDashboard(2, 5);
        check(dashboard.getLowStockMedicines().size() == 1
                        && dashboard.getLowStockMedicines().get(0).getMedicine() == regular,
                "dashboard includes low-stock results");
        check(dashboard.getNearExpiryMedicines().size() == 1
                        && dashboard.getNearExpiryMedicines().get(0) == nearExpiry,
                "dashboard includes near-expiry results");
        check(dashboard.getPendingPrescriptions().isEmpty(),
                "dashboard excludes prescriptions already reviewed");
        check(dashboard.getTodayOrders().size() == 1
                        && dashboard.getTodayOrders().get(0) == todayOrder,
                "dashboard includes only today's orders");
        check(!dashboard.getCompletedAt().isBefore(dashboard.getStartedAt()),
                "dashboard exposes an ordered start and completion time");
        System.out.println("Dashboard load before: " + dashboard.getStartedAt());
        System.out.println("Dashboard load after:  " + dashboard.getCompletedAt());
        expectThrows(IllegalArgumentException.class, () -> operations.loadDashboard(-1, 5));
    }

    private static void checkConstructorValidation(MedicineService medicineService,
            StockService stockService, PrescriptionService prescriptionService,
            OrderService orderService) {
        expectThrows(IllegalArgumentException.class, () -> new ConcurrentPharmacyOperations(
                medicineService, stockService, prescriptionService, orderService, 0));
    }

    private static Customer customer(int id) {
        return new Customer(id, "Test", "Customer", "test" + id + "@example.com",
                "Test Address", List.of());
    }

    private static Warehouse warehouse(int id) {
        return new Warehouse(0, "Warehouse " + id, "City " + id, new ArrayList<>());
    }

    private static Medicine medicine(int id, String name, int warehouseId,
            LocalDate expiryDate) {
        return new OverTheCounterMedicine(id, name, new BigDecimal("10.00"),
                expiryDate, warehouseId);
    }

    private static Prescription pendingPrescription(int id, Customer customer) {
        return new Prescription(0, customer, "/prescriptions/" + id + ".pdf",
                null, null, "Pending");
    }

    private static Order order(int id, Customer customer, List<Medicine> items,
            LocalDateTime orderDate) {
        return new Order(id, customer, items, orderDate, null, "Placed", BigDecimal.ZERO);
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static <T extends Throwable> void expectThrows(Class<T> expected,
            ThrowingRunnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError("Expected " + expected.getSimpleName()
                    + " but got " + actual.getClass().getSimpleName(), actual);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
