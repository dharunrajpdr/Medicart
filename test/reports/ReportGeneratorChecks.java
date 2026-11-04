package reports;

import dao.ReportGenerator;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class ReportGeneratorChecks {
    private static int checks;

    private ReportGeneratorChecks() {
    }

    public static void main(String[] args) {
        ReportGenerator reports = new ReportGenerator();
        checkNearExpiry(reports);
        checkLowStock(reports);
        checkTopSelling(reports);
        checkPendingPrescriptions(reports);
        checkMonthlyRevenue(reports);
        checkFrequentCustomers(reports);
        System.out.println("Report generator checks passed: " + checks);
    }

    private static void checkNearExpiry(ReportGenerator reports) {
        Medicine soon = medicine(1, "Soon", 1, 10, LocalDate.now().plusDays(3));
        Medicine today = medicine(2, "Today", 1, 10, LocalDate.now());
        Medicine outside = medicine(3, "Outside", 1, 10, LocalDate.now().plusDays(4));
        Medicine expired = medicine(4, "Expired", 1, 10, LocalDate.now().minusDays(1));
        List<Medicine> result = reports.getMedicinesNearExpiry(
                List.of(outside, expired, today, soon), 3);
        check(result.size() == 2, "near-expiry filters inclusively and excludes expired items");
        check(result.get(0) == today && result.get(1) == soon,
                "near-expiry report is sorted by expiry date");
        check(reports.getMedicinesNearExpiry(List.of(), 3).isEmpty(),
                "near-expiry handles an empty list");
        expectThrows(IllegalArgumentException.class,
                () -> reports.getMedicinesNearExpiry(List.of(soon), -1));
        expectThrows(NullPointerException.class,
                () -> reports.getMedicinesNearExpiry(null, 3));
    }

    private static void checkLowStock(ReportGenerator reports) {
        Warehouse warehouse = warehouse(1);
        Stock low = new Stock(1, medicine(1, "Low", 1, 4, LocalDate.now().plusDays(10)),
                warehouse, 2);
        Stock atThreshold = new Stock(2, medicine(2, "Threshold", 1, 4,
                LocalDate.now().plusDays(10)), warehouse, 5);
        Stock result = reports.getLowStockMedicines(List.of(atThreshold, low), 5).get(0);
        check(result == low, "low-stock report uses strict below-threshold comparison");
        check(reports.getLowStockMedicines(List.of(), 5).isEmpty(),
                "low-stock handles an empty list");
        expectThrows(IllegalArgumentException.class,
                () -> reports.getLowStockMedicines(List.of(low), -1));
        expectThrows(NullPointerException.class,
                () -> reports.getLowStockMedicines(null, 5));
    }

    private static void checkTopSelling(ReportGenerator reports) {
        Medicine one = medicine(1, "One", 1, 10, LocalDate.now().plusDays(10));
        Medicine two = medicine(2, "Two", 1, 20, LocalDate.now().plusDays(10));
        Medicine three = medicine(3, "Three", 1, 30, LocalDate.now().plusDays(10));
        List<ReportGenerator.TopSellingMedicine> result =
                reports.getTopSellingMedicines(List.of(three, two, one, one, two), 2);
        check(result.size() == 2, "top-selling respects topN");
        check(result.get(0).getMedicine().getMedicineId() == 1
                        && result.get(0).getUnitsSold() == 2,
                "top-selling aggregates repeated medicine IDs");
        check(result.get(1).getMedicine().getMedicineId() == 2,
                "top-selling breaks quantity ties by medicine ID");
        check(reports.getTopSellingMedicines(List.of(one), 0).isEmpty(),
                "top-selling accepts zero topN");
        check(reports.getTopSellingMedicines(List.of(), 5).isEmpty(),
                "top-selling handles empty items");
        expectThrows(IllegalArgumentException.class,
                () -> reports.getTopSellingMedicines(List.of(one), -1));
        expectThrows(IllegalArgumentException.class,
                () -> reports.getTopSellingMedicines(List.of(
                        medicine(0, "Unsaved", 1, 1, LocalDate.now().plusDays(1))), 1));
    }

    private static void checkPendingPrescriptions(ReportGenerator reports) {
        Customer patient = customer(1);
        Prescription later = prescription(2, patient, "Pending",
                LocalDateTime.of(2026, 2, 1, 9, 0));
        Prescription approved = prescription(3, patient, "Approved",
                LocalDateTime.of(2026, 1, 1, 9, 0));
        Prescription earlier = prescription(1, patient, "Pending",
                LocalDateTime.of(2026, 1, 1, 9, 0));
        List<Prescription> result = reports.getPendingPrescriptions(
                List.of(later, approved, earlier));
        check(result.size() == 2, "pending-prescription report filters status");
        check(result.get(0) == earlier && result.get(1) == later,
                "pending prescriptions are ordered by upload timestamp then ID");
        check(reports.getPendingPrescriptions(List.of()).isEmpty(),
                "pending-prescription report handles empty input");
    }

    private static void checkMonthlyRevenue(ReportGenerator reports) {
        Customer buyer = customer(1);
        Medicine warehouseOne = medicine(1, "One", 1, 10, LocalDate.now().plusDays(10));
        Medicine warehouseTwo = medicine(2, "Two", 2, 15, LocalDate.now().plusDays(10));
        Order january = order(1, buyer, List.of(warehouseOne, warehouseOne, warehouseTwo),
                LocalDateTime.of(2026, 1, 15, 11, 0));
        Order february = order(2, buyer, List.of(warehouseTwo),
                LocalDateTime.of(2026, 2, 2, 8, 0));
        List<ReportGenerator.WarehouseMonthlyRevenue> result =
                reports.getMonthlyRevenueByWarehouse(List.of(january, february));
        check(result.size() == 3, "revenue groups by warehouse and month");
        check(revenueFor(result, 1, YearMonth.of(2026, 1))
                        .compareTo(new BigDecimal("20.00")) == 0,
                "revenue sums each medicine entry for its warehouse");
        check(revenueFor(result, 2, YearMonth.of(2026, 1))
                        .compareTo(new BigDecimal("15.00")) == 0,
                "revenue attributes each entry to its medicine warehouse");
        check(revenueFor(result, 2, YearMonth.of(2026, 2))
                        .compareTo(new BigDecimal("15.00")) == 0,
                "revenue separates months");
        check(reports.getMonthlyRevenueByWarehouse(List.of()).isEmpty(),
                "revenue report handles empty orders");
    }

    private static void checkFrequentCustomers(ReportGenerator reports) {
        Customer first = customer(1);
        Customer sameId = customer(1);
        Customer second = customer(2);
        List<Order> orders = List.of(
                order(1, first, List.of(), LocalDateTime.of(2026, 1, 1, 0, 0)),
                order(2, sameId, List.of(), LocalDateTime.of(2026, 1, 2, 0, 0)),
                order(3, first, List.of(), LocalDateTime.of(2026, 1, 3, 0, 0)),
                order(4, second, List.of(), LocalDateTime.of(2026, 1, 4, 0, 0)));
        List<ReportGenerator.FrequentCustomer> result =
                reports.getFrequentCustomers(orders, 2);
        check(result.size() == 1, "frequent customer count uses strict minOrders threshold");
        check(result.get(0).getCustomer().getCustomerId() == 1
                        && result.get(0).getOrderCount() == 3,
                "frequent customers group by customer ID");
        check(reports.getFrequentCustomers(List.of(), 0).isEmpty(),
                "frequent-customer report handles empty orders");
        check(reports.getFrequentCustomers(orders, 0).size() == 2,
                "zero minOrders includes customers with orders");
        expectThrows(IllegalArgumentException.class,
                () -> reports.getFrequentCustomers(orders, -1));
    }

    private static BigDecimal revenueFor(
            List<ReportGenerator.WarehouseMonthlyRevenue> reports,
            int warehouseId, YearMonth month) {
        for (ReportGenerator.WarehouseMonthlyRevenue report : reports) {
            if (report.getWarehouseId() == warehouseId && report.getMonth().equals(month)) {
                return report.getRevenue();
            }
        }
        throw new AssertionError("Missing revenue row for warehouse " + warehouseId + " and " + month);
    }

    private static Medicine medicine(int id, String name, int warehouseId,
            int price, LocalDate expiryDate) {
        return new OverTheCounterMedicine(id, name, BigDecimal.valueOf(price),
                expiryDate, warehouseId);
    }

    private static Warehouse warehouse(int id) {
        return new Warehouse(id, "Warehouse " + id, "City", new ArrayList<>());
    }

    private static Customer customer(int id) {
        return new Customer(id, "Test", "Customer", "customer" + id + "@example.com",
                "Address", new ArrayList<>());
    }

    private static Prescription prescription(int id, Customer customer, String status,
            LocalDateTime uploadedOn) {
        return new Prescription(id, customer, "/prescriptions/" + id + ".pdf",
                null, null, status, uploadedOn);
    }

    private static Order order(int id, Customer customer, List<Medicine> items,
            LocalDateTime orderDate) {
        return new Order(id, customer, items, orderDate, null, "Completed", BigDecimal.ZERO);
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static <T extends Throwable> void expectThrows(Class<T> expected,
            Runnable operation) {
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
}
