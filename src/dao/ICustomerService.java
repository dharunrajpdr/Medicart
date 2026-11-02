package dao;

import entity.model.Customer;
import entity.model.Prescription;
import java.util.List;

public interface ICustomerService {
    Customer getCustomerById(int customerId);

    List<Customer> getAllCustomers();

    void addCustomer(Customer customer);

    void updateCustomer(Customer customer);

    void deleteCustomer(int customerId);

    void uploadPrescription(Customer customer, Prescription prescription);
}
