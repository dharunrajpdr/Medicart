package dao;

import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
import entity.model.Prescription;
import entity.model.Stock;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportGenerator {
    public List<Medicine> getMedicinesNearExpiry(List<? extends Medicine> medicines,
            int daysThreshold) {
        Objects.requireNonNull(medicines, "medicines cannot be null");
        if (daysThreshold < 0) {
            throw new IllegalArgumentException("daysThreshold cannot be negative");
        }

        List<Medicine> result = new ArrayList<>();
        for (Medicine medicine : medicines) {
            Objects.requireNonNull(medicine, "medicines cannot contain null");
            if (medicine.isNearExpiry(daysThreshold)) {
                result.add(medicine);
            }
        }
        result.sort(Comparator.comparing(Medicine::getExpiryDate)
                .thenComparingInt(Medicine::getMedicineId));
        return result;
    }

    public List<Stock> getLowStockMedicines(List<Stock> stock, int threshold) {
        Objects.requireNonNull(stock, "stock cannot be null");
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold cannot be negative");
        }

        List<Stock> result = new ArrayList<>();
        for (Stock item : stock) {
            Objects.requireNonNull(item, "stock cannot contain null");
            if (item.getQuantityAvailable() < threshold) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparingInt(
                item -> item.getMedicine().getMedicineId()));
        return result;
    }

    public List<TopSellingMedicine> getTopSellingMedicines(List<? extends Medicine> items,
            int topN) {
        Objects.requireNonNull(items, "items cannot be null");
        if (topN < 0) {
            throw new IllegalArgumentException("topN cannot be negative");
        }

        Map<Integer, TopSellingMedicine> grouped = new HashMap<>();
        for (Medicine medicine : items) {
            Objects.requireNonNull(medicine, "items cannot contain null");
            int medicineId = medicine.getMedicineId();
            if (medicineId <= 0) {
                throw new IllegalArgumentException("Top-selling medicines must have persisted IDs");
            }
            TopSellingMedicine result = grouped.get(medicineId);
            if (result == null) {
                grouped.put(medicineId, new TopSellingMedicine(medicine, 1));
            } else {
                result.incrementUnitsSold();
            }
        }

        List<TopSellingMedicine> result = new ArrayList<>(grouped.values());
        result.sort(Comparator.comparingLong(TopSellingMedicine::getUnitsSold).reversed()
                .thenComparingInt(value -> value.getMedicine().getMedicineId()));
        if (topN < result.size()) {
            return new ArrayList<>(result.subList(0, topN));
        }
        return result;
    }

    public List<Prescription> getPendingPrescriptions(List<Prescription> prescriptions) {
        Objects.requireNonNull(prescriptions, "prescriptions cannot be null");
        List<Prescription> result = new ArrayList<>();
        for (Prescription prescription : prescriptions) {
            Objects.requireNonNull(prescription, "prescriptions cannot contain null");
            if ("Pending".equals(prescription.getStatus())) {
                result.add(prescription);
            }
        }
        result.sort(Comparator.comparing(Prescription::getUploadedOn)
                .thenComparingInt(Prescription::getPrescriptionId));
        return result;
    }

    public List<WarehouseMonthlyRevenue> getMonthlyRevenueByWarehouse(List<Order> orders) {
        Objects.requireNonNull(orders, "orders cannot be null");
        Map<WarehouseMonthKey, BigDecimal> totals = new HashMap<>();
        for (Order order : orders) {
            Objects.requireNonNull(order, "orders cannot contain null");
            YearMonth month = YearMonth.from(order.getOrderDate());
            for (Medicine medicine : order.getItems()) {
                Objects.requireNonNull(medicine, "order items cannot contain null");
                int warehouseId = medicine.getWarehouseId();
                if (warehouseId <= 0) {
                    throw new IllegalArgumentException(
                            "Revenue items must reference a persisted warehouse");
                }
                WarehouseMonthKey key = new WarehouseMonthKey(warehouseId, month);
                totals.merge(key, medicine.getPrice(), BigDecimal::add);
            }
        }

        List<WarehouseMonthlyRevenue> result = new ArrayList<>();
        for (Map.Entry<WarehouseMonthKey, BigDecimal> entry : totals.entrySet()) {
            result.add(new WarehouseMonthlyRevenue(entry.getKey().warehouseId,
                    entry.getKey().month, entry.getValue()));
        }
        result.sort(Comparator.comparing(WarehouseMonthlyRevenue::getMonth)
                .thenComparingInt(WarehouseMonthlyRevenue::getWarehouseId));
        return result;
    }

    public List<FrequentCustomer> getFrequentCustomers(List<Order> orders, int minOrders) {
        Objects.requireNonNull(orders, "orders cannot be null");
        if (minOrders < 0) {
            throw new IllegalArgumentException("minOrders cannot be negative");
        }

        Map<Integer, FrequentCustomer> grouped = new HashMap<>();
        for (Order order : orders) {
            Objects.requireNonNull(order, "orders cannot contain null");
            Customer customer = Objects.requireNonNull(order.getCustomer(),
                    "order customer cannot be null");
            int customerId = customer.getCustomerId();
            if (customerId <= 0) {
                throw new IllegalArgumentException(
                        "Frequent customers must have persisted IDs");
            }
            FrequentCustomer result = grouped.get(customerId);
            if (result == null) {
                grouped.put(customerId, new FrequentCustomer(customer, 1));
            } else {
                result.incrementOrderCount();
            }
        }

        List<FrequentCustomer> result = new ArrayList<>();
        for (FrequentCustomer customer : grouped.values()) {
            if (customer.getOrderCount() > minOrders) {
                result.add(customer);
            }
        }
        result.sort(Comparator.comparingLong(FrequentCustomer::getOrderCount).reversed()
                .thenComparingInt(value -> value.getCustomer().getCustomerId()));
        return result;
    }

    public static final class TopSellingMedicine {
        private final Medicine medicine;
        private long unitsSold;

        private TopSellingMedicine(Medicine medicine, long unitsSold) {
            this.medicine = medicine;
            this.unitsSold = unitsSold;
        }

        public Medicine getMedicine() {
            return medicine;
        }

        public long getUnitsSold() {
            return unitsSold;
        }

        private void incrementUnitsSold() {
            unitsSold = Math.incrementExact(unitsSold);
        }
    }

    public static final class WarehouseMonthlyRevenue {
        private final int warehouseId;
        private final YearMonth month;
        private final BigDecimal revenue;

        private WarehouseMonthlyRevenue(int warehouseId, YearMonth month, BigDecimal revenue) {
            this.warehouseId = warehouseId;
            this.month = month;
            this.revenue = revenue;
        }

        public int getWarehouseId() {
            return warehouseId;
        }

        public YearMonth getMonth() {
            return month;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }
    }

    public static final class FrequentCustomer {
        private final Customer customer;
        private long orderCount;

        private FrequentCustomer(Customer customer, long orderCount) {
            this.customer = customer;
            this.orderCount = orderCount;
        }

        public Customer getCustomer() {
            return customer;
        }

        public long getOrderCount() {
            return orderCount;
        }

        private void incrementOrderCount() {
            orderCount = Math.incrementExact(orderCount);
        }
    }

    private static final class WarehouseMonthKey {
        private final int warehouseId;
        private final YearMonth month;

        private WarehouseMonthKey(int warehouseId, YearMonth month) {
            this.warehouseId = warehouseId;
            this.month = month;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof WarehouseMonthKey)) {
                return false;
            }
            WarehouseMonthKey that = (WarehouseMonthKey) other;
            return warehouseId == that.warehouseId && month.equals(that.month);
        }

        @Override
        public int hashCode() {
            return Objects.hash(warehouseId, month);
        }
    }
}
