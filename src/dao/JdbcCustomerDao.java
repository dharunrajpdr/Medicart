package dao;

import entity.model.Customer;
import exception.DataAccessException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcCustomerDao implements ICustomerDao {
    private final DatabaseContext database;

    public JdbcCustomerDao() {
        this(new DatabaseContext());
    }

    public JdbcCustomerDao(DatabaseContext database) {
        this.database = Objects.requireNonNull(database, "database cannot be null");
    }

    @Override
    public Customer findById(int id) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT CustomerID, FirstName, LastName, Email, Address "
                            + "FROM Customer WHERE CustomerID = ?")) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? JdbcRowMapper.customer(result) : null;
                }
            }
        });
    }

    @Override
    public List<Customer> findAll() {
        return database.execute(connection -> {
            List<Customer> customers = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT CustomerID, FirstName, LastName, Email, Address "
                            + "FROM Customer ORDER BY CustomerID");
                    ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    customers.add(JdbcRowMapper.customer(result));
                }
            }
            return customers;
        });
    }

    @Override
    public void insert(Customer customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        database.execute(connection -> {
            boolean explicitId = customer.getCustomerId() > 0;
            String sql = explicitId
                    ? "INSERT INTO Customer (CustomerID, FirstName, LastName, Email, Address) "
                            + "VALUES (?, ?, ?, ?, ?)"
                    : "INSERT INTO Customer (FirstName, LastName, Email, Address) "
                            + "VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int firstField = explicitId ? 2 : 1;
                if (explicitId) {
                    statement.setInt(1, customer.getCustomerId());
                }
                bind(statement, customer, firstField);
                statement.executeUpdate();
                if (!explicitId) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Customer insert did not return an ID");
                        }
                        customer.setCustomerId(keys.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    @Override
    public void update(Customer customer) {
        Objects.requireNonNull(customer, "customer cannot be null");
        database.execute(connection -> {
            String sql = "UPDATE Customer SET FirstName = ?, LastName = ?, Email = ?, "
                    + "Address = ? WHERE CustomerID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, customer, 1);
                statement.setInt(5, customer.getCustomerId());
                if (statement.executeUpdate() != 1) {
                    throw new DataAccessException(
                            "Customer does not exist: " + customer.getCustomerId());
                }
            }
            return null;
        });
    }

    @Override
    public void delete(int id) {
        deleteRow(database, "DELETE FROM Customer WHERE CustomerID = ?", id, "Customer");
    }

    @Override
    public boolean hasOrdersOrPrescriptions(int customerId) {
        return database.execute(connection -> {
            String sql = "SELECT EXISTS(SELECT 1 FROM `Order` WHERE CustomerID = ?) "
                    + "OR EXISTS(SELECT 1 FROM Prescription WHERE CustomerID = ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, customerId);
                statement.setInt(2, customerId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() && result.getBoolean(1);
                }
            }
        });
    }

    static void deleteRow(DatabaseContext database, String sql, int id, String entityName) {
        database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                if (statement.executeUpdate() != 1) {
                    throw new DataAccessException(entityName + " does not exist: " + id);
                }
            }
            return null;
        });
    }

    private static void bind(PreparedStatement statement, Customer customer, int firstField)
            throws SQLException {
        statement.setString(firstField, customer.getFirstName());
        statement.setString(firstField + 1, customer.getLastName());
        statement.setString(firstField + 2, customer.getEmail());
        statement.setString(firstField + 3, customer.getAddress());
    }
}
