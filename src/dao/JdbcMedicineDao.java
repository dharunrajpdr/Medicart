package dao;

import entity.model.Medicine;
import exception.DataAccessException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcMedicineDao implements IMedicineDao {
    private final DatabaseContext database;

    public JdbcMedicineDao() {
        this(new DatabaseContext());
    }

    public JdbcMedicineDao(DatabaseContext database) {
        this.database = Objects.requireNonNull(database, "database cannot be null");
    }

    @Override
    public Medicine findById(int id) {
        return database.execute(connection -> {
            String sql = "SELECT MedicineID, Name, RequiresPrescription, Price, ExpiryDate, "
                    + "WarehouseID FROM Medicine WHERE MedicineID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? JdbcRowMapper.medicine(result) : null;
                }
            }
        });
    }

    @Override
    public List<Medicine> findAll() {
        return database.execute(connection -> {
            List<Medicine> medicines = new ArrayList<>();
            String sql = "SELECT MedicineID, Name, RequiresPrescription, Price, ExpiryDate, "
                    + "WarehouseID FROM Medicine ORDER BY MedicineID";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    medicines.add(JdbcRowMapper.medicine(result));
                }
            }
            return medicines;
        });
    }

    @Override
    public void insert(Medicine medicine) {
        Objects.requireNonNull(medicine, "medicine cannot be null");
        database.execute(connection -> {
            boolean explicitId = medicine.getMedicineId() > 0;
            String sql = explicitId
                    ? "INSERT INTO Medicine (MedicineID, Name, RequiresPrescription, Price, "
                            + "ExpiryDate, WarehouseID) VALUES (?, ?, ?, ?, ?, ?)"
                    : "INSERT INTO Medicine "
                            + "(Name, RequiresPrescription, Price, ExpiryDate, WarehouseID) "
                            + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int firstField = explicitId ? 2 : 1;
                if (explicitId) {
                    statement.setInt(1, medicine.getMedicineId());
                }
                bind(statement, medicine, firstField);
                statement.executeUpdate();
                if (!explicitId) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new java.sql.SQLException("Medicine insert did not return an ID");
                        }
                        medicine.setMedicineId(keys.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    @Override
    public void update(Medicine medicine) {
        Objects.requireNonNull(medicine, "medicine cannot be null");
        database.execute(connection -> {
            String sql = "UPDATE Medicine SET Name = ?, RequiresPrescription = ?, Price = ?, "
                    + "ExpiryDate = ?, WarehouseID = ? WHERE MedicineID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, medicine, 1);
                statement.setInt(6, medicine.getMedicineId());
                if (statement.executeUpdate() != 1) {
                    throw new DataAccessException(
                            "Medicine does not exist: " + medicine.getMedicineId());
                }
            }
            return null;
        });
    }

    @Override
    public void delete(int id) {
        JdbcCustomerDao.deleteRow(database,
                "DELETE FROM Medicine WHERE MedicineID = ?", id, "Medicine");
    }

    private static void bind(PreparedStatement statement, Medicine medicine, int firstField)
            throws java.sql.SQLException {
        statement.setString(firstField, medicine.getName());
        statement.setBoolean(firstField + 1, medicine.isPrescriptionRequired());
        statement.setBigDecimal(firstField + 2, medicine.getPrice());
        statement.setDate(firstField + 3, java.sql.Date.valueOf(medicine.getExpiryDate()));
        statement.setInt(firstField + 4, medicine.getWarehouseId());
    }
}
