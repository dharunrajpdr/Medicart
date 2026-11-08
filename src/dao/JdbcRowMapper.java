package dao;

import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
import entity.model.OverTheCounterMedicine;
import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import entity.model.Stock;
import entity.model.Warehouse;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class JdbcRowMapper {
    private JdbcRowMapper() {
    }

    static Customer customer(ResultSet result) throws SQLException {
        return new Customer(result.getInt("CustomerID"), result.getString("FirstName"),
                result.getString("LastName"), result.getString("Email"),
                result.getString("Address"), List.of());
    }

    static Medicine medicine(ResultSet result) throws SQLException {
        int id = result.getInt("MedicineID");
        String name = result.getString("Name");
        BigDecimal price = result.getBigDecimal("Price");
        LocalDate expiry = result.getDate("ExpiryDate").toLocalDate();
        int warehouseId = result.getInt("WarehouseID");
        if (result.getBoolean("RequiresPrescription")) {
            return new PrescriptionMedicine(id, name, price, expiry, warehouseId, null);
        }
        return new OverTheCounterMedicine(id, name, price, expiry, warehouseId);
    }

    static Warehouse warehouse(ResultSet result) throws SQLException {
        return new Warehouse(result.getInt("WarehouseID"),
                result.getString("WarehouseName"), result.getString("City"), List.of());
    }

    static Stock stock(ResultSet result, Connection connection) throws SQLException {
        Medicine medicine = findMedicine(connection, result.getInt("MedicineID"));
        Warehouse warehouse = findWarehouse(connection, result.getInt("WarehouseID"));
        return new Stock(result.getInt("StockID"), medicine, warehouse,
                result.getInt("QuantityAvailable"));
    }

    static Prescription prescription(ResultSet result, Connection connection) throws SQLException {
        Customer customer = findCustomer(connection, result.getInt("CustomerID"));
        Timestamp uploadedOn = result.getTimestamp("UploadedOn");
        java.sql.Date verifiedDate = result.getDate("VerifiedOn");
        int verifier = result.getInt("VerifiedBy");
        Integer verifiedBy = result.wasNull() ? null : verifier;
        return new Prescription(result.getInt("PrescriptionID"), customer,
                result.getString("DocumentPath"), verifiedBy,
                verifiedDate == null ? null : verifiedDate.toLocalDate(),
                result.getString("Status"),
                uploadedOn.toLocalDateTime());
    }

    static Order order(ResultSet result, Connection connection) throws SQLException {
        Customer customer = findCustomer(connection, result.getInt("CustomerID"));
        int prescriptionId = result.getInt("PrescriptionID");
        Prescription prescription = result.wasNull() ? null
                : findPrescription(connection, prescriptionId);
        Timestamp orderDate = result.getTimestamp("OrderDate");
        Order order = new Order(result.getInt("OrderID"), customer,
                findOrderItems(connection, result.getInt("OrderID")),
                orderDate.toLocalDateTime(), prescription,
                result.getString("Status"), result.getBigDecimal("TotalAmount"));
        return order;
    }

    private static List<Medicine> findOrderItems(Connection connection, int orderId)
            throws SQLException {
        String sql = "SELECT m.MedicineID, m.Name, m.RequiresPrescription, m.Price, "
                + "m.ExpiryDate, m.WarehouseID, oi.PrescriptionID FROM OrderItem oi "
                + "JOIN Medicine m ON m.MedicineID = oi.MedicineID "
                + "WHERE oi.OrderID = ? ORDER BY oi.ItemNumber";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                List<Medicine> items = new ArrayList<>();
                while (result.next()) {
                    Medicine item = medicine(result);
                    int prescriptionId = result.getInt("PrescriptionID");
                    if (!result.wasNull() && item instanceof PrescriptionMedicine) {
                        ((PrescriptionMedicine) item).setPrescription(
                                findPrescription(connection, prescriptionId));
                    }
                    items.add(item);
                }
                return items;
            }
        }
    }

    private static Customer findCustomer(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CustomerID, FirstName, LastName, Email, Address "
                        + "FROM Customer WHERE CustomerID = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Referenced customer does not exist: " + id);
                }
                return customer(result);
            }
        }
    }

    private static Medicine findMedicine(Connection connection, int id) throws SQLException {
        String sql = "SELECT MedicineID, Name, RequiresPrescription, Price, ExpiryDate, "
                + "WarehouseID FROM Medicine WHERE MedicineID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Referenced medicine does not exist: " + id);
                }
                return medicine(result);
            }
        }
    }

    private static Warehouse findWarehouse(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT WarehouseID, WarehouseName, City FROM Warehouse WHERE WarehouseID = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Referenced warehouse does not exist: " + id);
                }
                return warehouse(result);
            }
        }
    }

    private static Prescription findPrescription(Connection connection, int id)
            throws SQLException {
        String sql = "SELECT PrescriptionID, CustomerID, DocumentPath, UploadedOn, VerifiedBy, "
                + "VerifiedOn, Status FROM Prescription WHERE PrescriptionID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Referenced prescription does not exist: " + id);
                }
                return prescription(result, connection);
            }
        }
    }
}
