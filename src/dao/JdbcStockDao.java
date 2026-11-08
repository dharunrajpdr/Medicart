package dao;

import entity.model.Stock;
import exception.DataAccessException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcStockDao implements IStockDao {
    private final DatabaseContext database;

    public JdbcStockDao() {
        this(new DatabaseContext());
    }

    public JdbcStockDao(DatabaseContext database) {
        this.database = Objects.requireNonNull(database, "database cannot be null");
    }

    @Override
    public Stock findById(int id) {
        return database.execute(connection -> {
            String sql = "SELECT StockID, MedicineID, WarehouseID, QuantityAvailable "
                    + "FROM Stock WHERE StockID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? JdbcRowMapper.stock(result, connection) : null;
                }
            }
        });
    }

    @Override
    public List<Stock> findAll() {
        return findByWarehouseId(0);
    }

    @Override
    public List<Stock> findByWarehouseId(int warehouseId) {
        return database.execute(connection -> {
            String sql = "SELECT StockID, MedicineID, WarehouseID, QuantityAvailable FROM Stock"
                    + (warehouseId > 0 ? " WHERE WarehouseID = ?" : "")
                    + " ORDER BY StockID";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (warehouseId > 0) {
                    statement.setInt(1, warehouseId);
                }
                try (ResultSet result = statement.executeQuery()) {
                    List<Stock> found = new ArrayList<>();
                    while (result.next()) {
                        found.add(JdbcRowMapper.stock(result, connection));
                    }
                    return found;
                }
            }
        });
    }

    @Override
    public void insert(Stock stock) {
        Objects.requireNonNull(stock, "stock cannot be null");
        database.execute(connection -> {
            boolean explicitId = stock.getStockId() > 0;
            String sql = explicitId
                    ? "INSERT INTO Stock (StockID, MedicineID, WarehouseID, QuantityAvailable) "
                            + "VALUES (?, ?, ?, ?)"
                    : "INSERT INTO Stock (MedicineID, WarehouseID, QuantityAvailable) "
                            + "VALUES (?, ?, ?)";
            try (PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int firstField = explicitId ? 2 : 1;
                if (explicitId) {
                    statement.setInt(1, stock.getStockId());
                }
                bind(statement, stock, firstField);
                statement.executeUpdate();
                if (!explicitId) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new java.sql.SQLException("Stock insert did not return an ID");
                        }
                        stock.setStockId(keys.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    @Override
    public void update(Stock stock) {
        Objects.requireNonNull(stock, "stock cannot be null");
        database.execute(connection -> {
            String sql = "UPDATE Stock SET MedicineID = ?, WarehouseID = ?, "
                    + "QuantityAvailable = ? WHERE StockID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, stock, 1);
                statement.setInt(4, stock.getStockId());
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Stock does not exist: " + stock.getStockId());
                }
            }
            return null;
        });
    }

    @Override
    public void delete(int id) {
        JdbcCustomerDao.deleteRow(database,
                "DELETE FROM Stock WHERE StockID = ?", id, "Stock");
    }

    @Override
    public boolean adjustAvailability(int medicineId, int warehouseId, int quantityDelta) {
        if (quantityDelta == 0) {
            throw new IllegalArgumentException("quantityDelta cannot be zero");
        }
        return database.execute(connection -> {
            String sql = "UPDATE Stock SET QuantityAvailable = QuantityAvailable + ? "
                    + "WHERE MedicineID = ? AND WarehouseID = ? "
                    + "AND QuantityAvailable + ? >= 0";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, quantityDelta);
                statement.setInt(2, medicineId);
                statement.setInt(3, warehouseId);
                statement.setInt(4, quantityDelta);
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static void bind(PreparedStatement statement, Stock stock, int firstField)
            throws java.sql.SQLException {
        statement.setInt(firstField, stock.getMedicine().getMedicineId());
        statement.setInt(firstField + 1, stock.getWarehouse().getWarehouseId());
        statement.setInt(firstField + 2, stock.getQuantityAvailable());
    }
}
