package dao;

import entity.model.Customer;

public interface ICustomerDao extends ICrudDao<Customer> {
    boolean hasOrdersOrPrescriptions(int customerId);
}
