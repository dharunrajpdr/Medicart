package entity.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Order {
    private static final String[] VALID_STATUSES = {
        "Placed", "Pending Verification", "Shipped", "Cancelled", "Completed"
    };

    private int orderId;
    private Customer customer;
    private List<Medicine> items = new ArrayList<>();
    private LocalDateTime orderDate = LocalDateTime.now();
    private Prescription prescription;
    private String status = "Placed";
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public Order() {
    }

    public Order(int orderId, Customer customer, List<Medicine> items, LocalDateTime orderDate,
            Prescription prescription, String status, BigDecimal totalAmount) {
        setOrderId(orderId);
        setCustomer(customer);
        setItems(items);
        setOrderDate(orderDate);
        setPrescription(prescription);
        setStatus(status);
        setTotalAmount(totalAmount);
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = ModelValidation.nonNegativeId(orderId, "orderId");
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = Objects.requireNonNull(customer, "customer cannot be null");
    }

    public List<Medicine> getItems() {
        return new ArrayList<>(items);
    }

    public void setItems(List<Medicine> items) {
        Objects.requireNonNull(items, "items cannot be null");
        for (Medicine item : items) {
            if (item == null) {
                throw new IllegalArgumentException("items cannot contain null");
            }
        }
        this.items = new ArrayList<>(items);
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = Objects.requireNonNull(orderDate, "orderDate cannot be null");
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        String value = ModelValidation.requiredText(status, "status", 20);
        for (String validStatus : VALID_STATUSES) {
            if (validStatus.equals(value)) {
                if ("Shipped".equals(value)) {
                    if (containsExpiredMedicine()) {
                        throw new IllegalStateException(
                                "An order containing expired medicine cannot be shipped");
                    }
                    if (!canShip()) {
                        throw new IllegalStateException(
                                "An order with prescription medicine needs an approved prescription before shipping");
                    }
                }
                this.status = value;
                return;
            }
        }
        throw new IllegalArgumentException("unsupported order status: " + value);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = ModelValidation.nonNegativeAmount(totalAmount, "totalAmount");
    }

    public void checkout() {
        if (items.isEmpty()) {
            throw new IllegalStateException("An order must contain at least one medicine");
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        boolean prescriptionPending = false;
        for (Medicine item : items) {
            if (item.isExpired()) {
                throw new IllegalStateException("Expired medicine cannot be ordered: " + item.getName());
            }
            calculatedTotal = calculatedTotal.add(item.getPrice());
            if (item.isPrescriptionRequired() && !hasApprovedPrescription(item)) {
                prescriptionPending = true;
            }
        }

        setTotalAmount(calculatedTotal);
        if (prescriptionPending) {
            holdForVerification();
        } else {
            setStatus("Placed");
        }
    }

    public void holdForVerification() {
        boolean hasPrescriptionMedicine = false;
        for (Medicine item : items) {
            if (item.isPrescriptionRequired()) {
                hasPrescriptionMedicine = true;
                break;
            }
        }
        if (!hasPrescriptionMedicine) {
            throw new IllegalStateException(
                    "Only an order containing prescription medicine can be held for verification");
        }
        setStatus("Pending Verification");
    }

    private boolean canShip() {
        for (Medicine item : items) {
            if (item.isPrescriptionRequired() && !hasApprovedPrescription(item)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsExpiredMedicine() {
        for (Medicine item : items) {
            if (item.isExpired()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasApprovedPrescription(Medicine item) {
        Prescription requiredPrescription = prescription;
        if (item instanceof PrescriptionMedicine) {
            Prescription itemPrescription = ((PrescriptionMedicine) item).getPrescription();
            if (itemPrescription != null) {
                requiredPrescription = itemPrescription;
            }
        }
        if (requiredPrescription == null || !requiredPrescription.isApproved()) {
            return false;
        }
        Customer prescriptionCustomer = requiredPrescription.getCustomer();
        return customer != null && prescriptionCustomer != null
                && (customer == prescriptionCustomer
                || customer.getCustomerId() > 0
                && customer.getCustomerId() == prescriptionCustomer.getCustomerId());
    }
}
