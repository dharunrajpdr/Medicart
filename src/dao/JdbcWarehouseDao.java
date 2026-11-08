package dao;

import entity.model.Stock;
import entity.model.Warehouse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcWarehouseDao implements IWarehouseDao {
    private final DatabaseContext database;
    private final IStockDao stocks;

    public JdbcWarehouseDao() {
        this(new DatabaseContext());
    }

    public JdbcWarehouseDao(DatabaseContext database) {
        this(database, new JdbcStockDao(database));
    }

    public JdbcWarehouseDao(DatabaseContext database, IStockDao stocks) {
        this.database = Objects.requireNonNull(database, "database cannot be null");
        this.stocks = Objects.requireNonNull(stocks, "stocks cannot be null");
    }

    @Override
    public Warehouse findById(int id) {
        Warehouse warehouse = database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT WarehouseID, WarehouseName, City FROM Warehouse "
                            + "WHERE WarehouseID = ?")) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? JdbcRowMapper.warehouse(result) : null;
                }
            }
        });
        return warehouse == null ? null : withStock(warehouse);
    }

    @Override
    public List<Warehouse> findAll() {
        List<Warehouse> warehouses = database.execute(connection -> {
            List<Warehouse> found = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT WarehouseID, WarehouseName, City FROM Warehouse "
                            + "ORDER BY WarehouseID");
                    ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    found.add(JdbcRowMapper.warehouse(result));
                }
            }
            return found;
        });
        for (int index = 0; index < warehouses.size(); index++) {
            warehouses.set(index, withStock(warehouses.get(index)));
        }
        return warehouses;
    }

    @Override
    public void insert(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse cannot be null");
        int id = database.inTransaction(connection -> {
            boolean explicitId = warehouse.getWarehouseId() > 0;
            String sql = explicitId
                    ? "INSERT INTO Warehouse (WarehouseID, WarehouseName, City) VALUES (?, ?, ?)"
                    : "INSERT INTO Warehouse (WarehouseName, City) VALUES (?, ?)";
            try (PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (explicitId) {
                    statement.setInt(1, warehouse.getWarehouseId());
                    bind(statement, warehouse, 2);
                } else {
                    bind(statement, warehouse, 1);
                }
                statement.executeUpdate();
                int warehouseId = warehouse.getWarehouseId();
                if (!explicitId) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new java.sql.SQLException(
                                    "Warehouse insert did not return an ID");
                        }
                        warehouseId = keys.getInt(1);
                    }
                }
                for (Stock stock : warehouse.getStock()) {
                    insertStock(connection, stock, warehouseId);
                }
                return warehouseId;
            }
        });
        warehouse.setWarehouseId(id);
        for (Stock stock : warehouse.getStock()) {
            stock.setWarehouse(warehouse);
        }
    }

    @Override
    public void update(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse cannot be null");
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE Warehouse SET WarehouseName = ?, City = ? "
                            + "WHERE WarehouseID = ?")) {
                bind(statement, warehouse, 1);
                statement.setInt(3, warehouse.getWarehouseId());
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException(
                            "Warehouse does not exist: " + warehouse.getWarehouseId());
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM Stock WHERE WarehouseID = ?")) {
                statement.setInt(1, warehouse.getWarehouseId());
                statement.executeUpdate();
            }
            for (Stock stock : warehouse.getStock()) {
                insertStock(connection, stock, warehouse.getWarehouseId());
            }
            return null;
        });
        for (Stock stock : warehouse.getStock()) {
            stock.setWarehouse(warehouse);
        }
    }

    @Override
    public void delete(int id) {
        JdbcCustomerDao.deleteRow(database,
                "DELETE FROM Warehouse WHERE WarehouseID = ?", id, "Warehouse");
    }

    private Warehouse withStock(Warehouse warehouse) {
        List<Stock> warehouseStocks = stocks.findByWarehouseId(warehouse.getWarehouseId());
        warehouse.setStock(warehouseStocks);
        return warehouse;
    }

    private static void bind(PreparedStatement statement, Warehouse warehouse, int firstField)
            throws java.sql.SQLException {
        statement.setString(firstField, warehouse.getWarehouseName());
        statement.setString(firstField + 1, warehouse.getCity());
    }

    private static void insertStock(java.sql.Connection connection, Stock stock, int warehouseId)
            throws java.sql.SQLException {
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
            statement.setInt(firstField, stock.getMedicine().getMedicineId());
            statement.setInt(firstField + 1, warehouseId);
            statement.setInt(firstField + 2, stock.getQuantityAvailable());
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
    }
}
