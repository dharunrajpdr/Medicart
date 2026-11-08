package dao;

import entity.model.Medicine;
import entity.model.Order;
import exception.DataAccessException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcOrderDao implements IOrderDao {
    private final DatabaseContext database;

    public JdbcOrderDao() {
        this(new DatabaseContext());
    }

    public JdbcOrderDao(DatabaseContext database) {
        this.database = Objects.requireNonNull(database, "database cannot be null");
    }

    @Override
    public Order findById(int id) {
        return database.execute(connection -> {
            String sql = selectOrder() + " WHERE o.OrderID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? JdbcRowMapper.order(result, connection) : null;
                }
            }
        });
    }

    @Override
    public List<Order> findAll() {
        return database.execute(connection -> {
            List<Order> orders = new ArrayList<>();
            String sql = selectOrder() + " ORDER BY o.OrderID";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    orders.add(JdbcRowMapper.order(result, connection));
                }
            }
            return orders;
        });
    }

    @Override
    public void insert(Order order) {
        Objects.requireNonNull(order, "order cannot be null");
        int generatedId = database.inTransaction(connection -> {
            boolean explicitId = order.getOrderId() > 0;
            String sql = explicitId
                    ? "INSERT INTO `Order` (OrderID, CustomerID, OrderDate, PrescriptionID, "
                            + "Status, TotalAmount) VALUES (?, ?, ?, ?, ?, ?)"
                    : "INSERT INTO `Order` "
                            + "(CustomerID, OrderDate, PrescriptionID, Status, TotalAmount) "
                            + "VALUES (?, ?, ?, ?, ?)";
            int orderId;
            try (PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (explicitId) {
                    statement.setInt(1, order.getOrderId());
                    bindHeader(statement, order, 2);
                } else {
                    bindHeader(statement, order, 1);
                }
                statement.executeUpdate();
                orderId = order.getOrderId();
                if (!explicitId) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new java.sql.SQLException("Order insert did not return an ID");
                        }
                        orderId = keys.getInt(1);
                    }
                }
            }
            replaceItems(connection, orderId, order);
            return orderId;
        });
        order.setOrderId(generatedId);
    }

    @Override
    public void update(Order order) {
        Objects.requireNonNull(order, "order cannot be null");
        database.inTransaction(connection -> {
            String sql = "UPDATE `Order` SET CustomerID = ?, OrderDate = ?, PrescriptionID = ?, "
                    + "Status = ?, TotalAmount = ? WHERE OrderID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindHeader(statement, order, 1);
                statement.setInt(6, order.getOrderId());
                if (statement.executeUpdate() != 1) {
                    throw new DataAccessException("Order does not exist: " + order.getOrderId());
                }
            }
            replaceItems(connection, order.getOrderId(), order);
            return null;
        });
    }

    @Override
    public void delete(int id) {
        JdbcCustomerDao.deleteRow(database,
                "DELETE FROM `Order` WHERE OrderID = ?", id, "Order");
    }

    private static String selectOrder() {
        return "SELECT o.OrderID, o.CustomerID, o.OrderDate, o.PrescriptionID, "
                + "o.Status, o.TotalAmount FROM `Order` o";
    }

    private static void bindHeader(PreparedStatement statement, Order order, int firstField)
            throws java.sql.SQLException {
        statement.setInt(firstField, order.getCustomer().getCustomerId());
        statement.setTimestamp(firstField + 1,
                java.sql.Timestamp.valueOf(order.getOrderDate()));
        if (order.getPrescription() == null) {
            statement.setNull(firstField + 2, Types.INTEGER);
        } else {
            statement.setInt(firstField + 2, order.getPrescription().getPrescriptionId());
        }
        statement.setString(firstField + 3, order.getStatus());
        statement.setBigDecimal(firstField + 4, order.getTotalAmount());
    }

    private static void replaceItems(java.sql.Connection connection, int orderId, Order order)
            throws java.sql.SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM OrderItem WHERE OrderID = ?")) {
            delete.setInt(1, orderId);
            delete.executeUpdate();
        }
        String sql = "INSERT INTO OrderItem "
                + "(OrderID, ItemNumber, MedicineID, PrescriptionID) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            int itemNumber = 1;
            for (Medicine medicine : order.getItems()) {
                insert.setInt(1, orderId);
                insert.setInt(2, itemNumber++);
                insert.setInt(3, medicine.getMedicineId());
                if (medicine instanceof entity.model.PrescriptionMedicine
                        && ((entity.model.PrescriptionMedicine) medicine).getPrescription() != null) {
                    insert.setInt(4, ((entity.model.PrescriptionMedicine) medicine)
                            .getPrescription().getPrescriptionId());
                } else if (order.getPrescription() != null) {
                    insert.setInt(4, order.getPrescription().getPrescriptionId());
                } else {
                    insert.setNull(4, Types.INTEGER);
                }
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
