package dao;

import entity.model.Medicine;
import entity.model.Order;
import exception.ExpiredMedicineException;
import exception.OrderNotFoundException;
import exception.PrescriptionRequiredException;
import java.util.List;
import java.util.Objects;

public class OrderService implements IOrderService {
    private final InMemoryRepository<Order> orders = new InMemoryRepository<>();
    private final IOrderDao orderDao;

    public OrderService() {
        this(null);
    }

    public OrderService(IOrderDao orderDao) {
        this.orderDao = orderDao;
    }

    @Override
    public Order getOrderById(int orderId) throws OrderNotFoundException {
        Order order = orderDao == null ? orders.get(orderId) : orderDao.findById(orderId);
        if (order == null) {
            throw new OrderNotFoundException("Order does not exist: " + orderId);
        }
        return order;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderDao == null ? orders.getAll() : orderDao.findAll();
    }

    @Override
    public synchronized void addOrder(Order order) {
        validateOrder(order);
        if (orderDao != null) {
            orderDao.insert(order);
            return;
        }
        orders.add(order, Order::getOrderId, Order::setOrderId);
    }

    @Override
    public synchronized void updateOrder(Order order) throws OrderNotFoundException {
        validateOrder(order);
        if (orderDao != null) {
            if (orderDao.findById(order.getOrderId()) == null) {
                throw new OrderNotFoundException("Order does not exist: " + order.getOrderId());
            }
            orderDao.update(order);
            return;
        }
        if (!orders.contains(order.getOrderId())) {
            throw new OrderNotFoundException("Order does not exist: " + order.getOrderId());
        }
        orders.update(order, Order::getOrderId);
    }

    @Override
    public synchronized void deleteOrder(int orderId) throws OrderNotFoundException {
        if (orderDao != null) {
            if (orderDao.findById(orderId) == null) {
                throw new OrderNotFoundException("Order does not exist: " + orderId);
            }
            orderDao.delete(orderId);
            return;
        }
        if (!orders.delete(orderId)) {
            throw new OrderNotFoundException("Order does not exist: " + orderId);
        }
    }

    @Override
    public synchronized void checkout(Order order)
            throws ExpiredMedicineException, PrescriptionRequiredException {
        validateOrder(order);
        for (Medicine medicine : order.getItems()) {
            if (medicine.isExpired()) {
                throw new ExpiredMedicineException(
                        "Expired medicine cannot be ordered: " + medicine.getName());
            }
        }
        try {
            order.checkout();
        } catch (IllegalStateException exception) {
            throw new IllegalArgumentException("Order cannot be checked out", exception);
        }
        persistWorkflowOrder(order);
    }

    @Override
    public synchronized void holdForVerification(Order order) {
        validateOrder(order);
        order.holdForVerification();
        persistWorkflowOrder(order);
    }

    private void persistWorkflowOrder(Order order) {
        if (orderDao != null) {
            if (order.getOrderId() > 0 && orderDao.findById(order.getOrderId()) != null) {
                orderDao.update(order);
            } else {
                orderDao.insert(order);
            }
            return;
        }
        int orderId = order.getOrderId();
        if (orderId > 0 && orders.contains(orderId)) {
            orders.update(order, Order::getOrderId);
        } else {
            orders.add(order, Order::getOrderId, Order::setOrderId);
        }
    }

    private static void validateOrder(Order order) {
        Objects.requireNonNull(order, "order cannot be null");
        Objects.requireNonNull(order.getCustomer(), "customer cannot be null");
        for (Medicine medicine : order.getItems()) {
            Objects.requireNonNull(medicine, "order items cannot contain null");
        }
    }
}
