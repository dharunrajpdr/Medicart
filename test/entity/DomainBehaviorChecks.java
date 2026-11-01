package entity.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DomainBehaviorChecks {
    private DomainBehaviorChecks() {
    }

    public static void main(String[] args) {
        Medicine otc = medicine(1, LocalDate.now().plusDays(3));
        check(!otc.isExpired(), "future expiry is not expired");
        check(otc.isNearExpiry(3), "near-expiry boundary is inclusive");
        check(!otc.isNearExpiry(2), "near-expiry threshold is respected");
        otc.addToCart();

        Medicine expired = medicine(2, LocalDate.now().minusDays(1));
        check(expired.isExpired(), "past expiry is expired");
        expectThrows(IllegalStateException.class, expired::addToCart);
        expectThrows(IllegalArgumentException.class, () -> otc.isNearExpiry(-1));

        Customer customer = customer(1);
        Prescription prescription = new Prescription(
                1, customer, "/prescriptions/test.pdf", null, null, "Pending");
        PrescriptionMedicine prescriptionMedicine = new PrescriptionMedicine(
                3, "Prescription Medicine", new BigDecimal("25.00"),
                LocalDate.now().plusDays(20), 1, prescription);
        expectThrows(IllegalStateException.class, prescriptionMedicine::addToCart);

        Order heldOrder = order(customer, prescriptionMedicine, null);
        heldOrder.checkout();
        check("Pending Verification".equals(heldOrder.getStatus()),
                "checkout holds an order without approved prescription");
        check(heldOrder.getTotalAmount().compareTo(new BigDecimal("25.00")) == 0,
                "checkout calculates order total");
        expectThrows(IllegalStateException.class, () -> heldOrder.setStatus("Shipped"));

        prescription.approve();
        check(prescription.isApproved(), "approval updates prescription status");
        check(prescription.getVerifiedOn() != null, "approval records verification date");
        prescriptionMedicine.addToCart();
        expectThrows(IllegalStateException.class, prescription::approve);

        Order approvedOrder = order(customer, prescriptionMedicine, prescription);
        approvedOrder.checkout();
        check("Placed".equals(approvedOrder.getStatus()),
                "approved prescription allows order placement");
        approvedOrder.setStatus("Shipped");
        check("Shipped".equals(approvedOrder.getStatus()),
                "approved prescription allows shipping");

        Order expiredOrder = order(customer, expired, null);
        expectThrows(IllegalStateException.class, expiredOrder::checkout);
        expectThrows(IllegalStateException.class, () -> expiredOrder.setStatus("Shipped"));
        expectThrows(IllegalStateException.class, () -> order(customer, otc, null).holdForVerification());
        expectThrows(IllegalStateException.class,
                () -> new Order(2, customer, new ArrayList<>(), LocalDateTime.now(),
                        null, "Placed", BigDecimal.ZERO).checkout());
        Order wrongCustomerOrder = order(customer(2), prescriptionMedicine, prescription);
        wrongCustomerOrder.checkout();
        check("Pending Verification".equals(wrongCustomerOrder.getStatus()),
                "another customer's prescription cannot clear the verification hold");

        Prescription rejectedPrescription = new Prescription(
                2, customer, "/prescriptions/rejected.pdf", null, null, "Pending");
        rejectedPrescription.reject();
        check("Rejected".equals(rejectedPrescription.getStatus()), "rejection updates status");
        expectThrows(IllegalStateException.class, rejectedPrescription::approve);

        Warehouse warehouse = new Warehouse(1, "Main", "City", new ArrayList<>());
        Stock stock = new Stock(1, otc, warehouse, 5);
        warehouse.setStock(List.of(stock));
        warehouse.reserveStock(otc, 3);
        check(stock.getQuantityAvailable() == 2, "reservation reduces stock");
        expectThrows(IllegalStateException.class, () -> warehouse.reserveStock(otc, 3));
        warehouse.releaseStock(otc, 4);
        check(stock.getQuantityAvailable() == 6, "release restores stock");
        expectThrows(IllegalArgumentException.class, () -> warehouse.reserveStock(otc, 0));

        System.out.println("Domain behavior checks passed.");
    }

    private static Medicine medicine(int medicineId, LocalDate expiryDate) {
        return new OverTheCounterMedicine(
                medicineId, "OTC Medicine", new BigDecimal("10.00"), expiryDate, 1);
    }

    private static Customer customer(int customerId) {
        return new Customer(customerId, "Test", "Customer",
                "customer" + customerId + "@example.com", "Address", new ArrayList<>());
    }

    private static Order order(Customer customer, Medicine item, Prescription prescription) {
        return new Order(1, customer, List.of(item), LocalDateTime.now(),
                prescription, "Placed", BigDecimal.ZERO);
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static <T extends Throwable> void expectThrows(Class<T> expected,
            Runnable operation) {
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
