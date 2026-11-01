package entity.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Customer {
    private int customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private List<Order> orders = new ArrayList<>();

    public Customer() {
    }

    public Customer(int customerId, String firstName, String lastName, String email,
            String address, List<Order> orders) {
        setCustomerId(customerId);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setAddress(address);
        setOrders(orders);
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = ModelValidation.nonNegativeId(customerId, "customerId");
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = ModelValidation.requiredText(firstName, "firstName", 50);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = ModelValidation.requiredText(lastName, "lastName", 50);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String normalized = ModelValidation.requiredText(email, "email", 100);
        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("email must be a valid email address");
        }
        this.email = normalized;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = ModelValidation.requiredText(address, "address", 200);
    }

    public List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    public void setOrders(List<Order> orders) {
        Objects.requireNonNull(orders, "orders cannot be null");
        for (Order order : orders) {
            if (order == null) {
                throw new IllegalArgumentException("orders cannot contain null");
            }
        }
        this.orders = new ArrayList<>(orders);
    }
}
