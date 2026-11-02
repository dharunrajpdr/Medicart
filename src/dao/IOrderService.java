package dao;

import entity.model.Order;
import exception.ExpiredMedicineException;
import exception.OrderNotFoundException;
import exception.PrescriptionRequiredException;
import java.util.List;

public interface IOrderService {
    Order getOrderById(int orderId) throws OrderNotFoundException;

    List<Order> getAllOrders();

    void addOrder(Order order);

    void updateOrder(Order order) throws OrderNotFoundException;

    void deleteOrder(int orderId) throws OrderNotFoundException;

    void checkout(Order order) throws ExpiredMedicineException, PrescriptionRequiredException;

    void holdForVerification(Order order);
}
