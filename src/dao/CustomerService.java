package dao;

import entity.model.Customer;
import entity.model.Prescription;
import java.util.List;
import java.util.Objects;

public class CustomerService implements ICustomerService {
    private final InMemoryRepository<Customer> customers = new InMemoryRepository<>();
    private final IPrescriptionService prescriptionService;

    public CustomerService() {
        this(new PrescriptionService());
    }

    public CustomerService(IPrescriptionService prescriptionService) {
        this.prescriptionService = Objects.requireNonNull(
                prescriptionService, "prescriptionService cannot be null");
    }

    @Override
    public Customer getCustomerById(int customerId) {
        return customers.get(customerId);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customers.getAll();
    }

    @Override
    public void addCustomer(Customer customer) {
        validateCustomer(customer);
        ensureEmailAvailable(customer, 0);
        customers.add(customer, Customer::getCustomerId, Customer::setCustomerId);
    }

    @Override
    public void updateCustomer(Customer customer) {
        validateCustomer(customer);
        if (!customers.contains(customer.getCustomerId())) {
            throw new IllegalArgumentException(
                    "Customer does not exist: " + customer.getCustomerId());
        }
        ensureEmailAvailable(customer, customer.getCustomerId());
        customers.update(customer, Customer::getCustomerId);
    }

    @Override
    public void deleteCustomer(int customerId) {
        Customer customer = customers.get(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer does not exist: " + customerId);
        }
        if (!customer.getOrders().isEmpty() || hasPrescriptions(customerId)) {
            throw new IllegalStateException("Customer has related orders or prescriptions");
        }
        customers.delete(customerId);
    }

    @Override
    public void uploadPrescription(Customer customer, Prescription prescription) {
        Objects.requireNonNull(customer, "customer cannot be null");
        Objects.requireNonNull(prescription, "prescription cannot be null");
        Customer storedCustomer = customers.get(customer.getCustomerId());
        if (storedCustomer == null) {
            throw new IllegalArgumentException("Customer does not exist: " + customer.getCustomerId());
        }
        Customer prescriptionCustomer = prescription.getCustomer();
        if (prescriptionCustomer == null
                || prescriptionCustomer.getCustomerId() != storedCustomer.getCustomerId()) {
            throw new IllegalArgumentException("Prescription must belong to the customer");
        }
        prescription.setCustomer(storedCustomer);
        prescriptionService.addPrescription(prescription);
    }

    private static void validateCustomer(Customer customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        Objects.requireNonNull(customer.getFirstName(), "firstName cannot be null");
        Objects.requireNonNull(customer.getLastName(), "lastName cannot be null");
        Objects.requireNonNull(customer.getEmail(), "email cannot be null");
        Objects.requireNonNull(customer.getAddress(), "address cannot be null");
    }

    private void ensureEmailAvailable(Customer candidate, int excludedCustomerId) {
        for (Customer existing : customers.getAll()) {
            if (existing.getCustomerId() != excludedCustomerId
                    && existing.getEmail().equalsIgnoreCase(candidate.getEmail())) {
                throw new IllegalArgumentException("Customer email already exists");
            }
        }
    }

    private boolean hasPrescriptions(int customerId) {
        for (Prescription prescription : prescriptionService.getAllPrescriptions()) {
            if (prescription.getCustomer().getCustomerId() == customerId) {
                return true;
            }
        }
        return false;
    }
}
