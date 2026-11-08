package dao;

import entity.model.Prescription;
import exception.DataAccessException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcPrescriptionDao implements IPrescriptionDao {
    private final DatabaseContext database;

    public JdbcPrescriptionDao() {
        this(new DatabaseContext());
    }

    public JdbcPrescriptionDao(DatabaseContext database) {
        this.database = Objects.requireNonNull(database, "database cannot be null");
    }

    @Override
    public Prescription findById(int id) {
        return database.execute(connection -> {
            String sql = "SELECT PrescriptionID, CustomerID, DocumentPath, UploadedOn, "
                    + "VerifiedBy, VerifiedOn, Status FROM Prescription WHERE PrescriptionID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? JdbcRowMapper.prescription(result, connection) : null;
                }
            }
        });
    }

    @Override
    public List<Prescription> findAll() {
        return database.execute(connection -> {
            List<Prescription> prescriptions = new ArrayList<>();
            String sql = "SELECT PrescriptionID, CustomerID, DocumentPath, UploadedOn, "
                    + "VerifiedBy, VerifiedOn, Status FROM Prescription "
                    + "ORDER BY PrescriptionID";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    prescriptions.add(JdbcRowMapper.prescription(result, connection));
                }
            }
            return prescriptions;
        });
    }

    @Override
    public void insert(Prescription prescription) {
        Objects.requireNonNull(prescription, "prescription cannot be null");
        database.execute(connection -> {
            boolean explicitId = prescription.getPrescriptionId() > 0;
            String sql = explicitId
                    ? "INSERT INTO Prescription (PrescriptionID, CustomerID, DocumentPath, "
                            + "UploadedOn, VerifiedBy, VerifiedOn, Status) VALUES (?, ?, ?, "
                            + "?, ?, ?, ?)"
                    : "INSERT INTO Prescription "
                            + "(CustomerID, DocumentPath, UploadedOn, VerifiedBy, VerifiedOn, "
                            + "Status) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int firstField = explicitId ? 2 : 1;
                if (explicitId) {
                    statement.setInt(1, prescription.getPrescriptionId());
                }
                bind(statement, prescription, firstField);
                statement.executeUpdate();
                if (!explicitId) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new java.sql.SQLException(
                                    "Prescription insert did not return an ID");
                        }
                        prescription.setPrescriptionId(keys.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    @Override
    public void update(Prescription prescription) {
        Objects.requireNonNull(prescription, "prescription cannot be null");
        database.execute(connection -> {
            String sql = "UPDATE Prescription SET CustomerID = ?, DocumentPath = ?, "
                    + "UploadedOn = ?, VerifiedBy = ?, VerifiedOn = ?, Status = ? "
                    + "WHERE PrescriptionID = ? AND Status = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, prescription, 1);
                statement.setInt(7, prescription.getPrescriptionId());
                statement.setString(8, prescription.getStatus());
                if (statement.executeUpdate() != 1) {
                    throw new DataAccessException(
                            "Prescription does not exist or its status changed: "
                                    + prescription.getPrescriptionId());
                }
            }
            return null;
        });
    }

    @Override
    public void delete(int id) {
        JdbcCustomerDao.deleteRow(database,
                "DELETE FROM Prescription WHERE PrescriptionID = ?", id, "Prescription");
    }

    @Override
    public boolean transitionStatus(int prescriptionId, String expectedStatus,
            String newStatus, LocalDate verifiedOn) {
        Objects.requireNonNull(expectedStatus, "expectedStatus cannot be null");
        Objects.requireNonNull(newStatus, "newStatus cannot be null");
        return database.execute(connection -> {
            String sql = "UPDATE Prescription SET Status = ?, VerifiedOn = ? "
                    + "WHERE PrescriptionID = ? AND Status = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newStatus);
                if (verifiedOn == null) {
                    statement.setNull(2, Types.DATE);
                } else {
                    statement.setDate(2, java.sql.Date.valueOf(verifiedOn));
                }
                statement.setInt(3, prescriptionId);
                statement.setString(4, expectedStatus);
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static void bind(PreparedStatement statement, Prescription prescription,
            int firstField)
            throws java.sql.SQLException {
        statement.setInt(firstField, prescription.getCustomer().getCustomerId());
        statement.setString(firstField + 1, prescription.getDocumentPath());
        statement.setTimestamp(firstField + 2,
                java.sql.Timestamp.valueOf(prescription.getUploadedOn()));
        if (prescription.getVerifiedBy() == null) {
            statement.setNull(firstField + 3, Types.INTEGER);
        } else {
            statement.setInt(firstField + 3, prescription.getVerifiedBy());
        }
        if (prescription.getVerifiedOn() == null) {
            statement.setNull(firstField + 4, Types.DATE);
        } else {
            statement.setDate(firstField + 4,
                    java.sql.Date.valueOf(prescription.getVerifiedOn()));
        }
        statement.setString(firstField + 5, prescription.getStatus());
    }
}
