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
}
