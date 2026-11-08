package dao;

import exception.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import util.DBConnection;

public class DatabaseContext {
    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    public Connection getConnection() {
        return DBConnection.getConnection();
    }

    public <T> T execute(SqlWork<T> work) {
        Objects.requireNonNull(work, "work cannot be null");
        try (Connection connection = getConnection()) {
            return work.execute(connection);
        } catch (SQLException exception) {
            throw new DataAccessException("Database operation failed", exception);
        }
    }

    public <T> T inTransaction(SqlWork<T> work) {
        Objects.requireNonNull(work, "work cannot be null");
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Database transaction failed", exception);
        }
    }

    private static void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
