package util;

import exception.DataAccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private DBConnection() {
    }

    public static Connection getConnection() {
        String driver = PropertyUtil.getRequiredProperty("db.driver");
        try {
            Class.forName(driver);
            return DriverManager.getConnection(
                    PropertyUtil.getPropertyString(),
                    PropertyUtil.getRequiredProperty("db.username"),
                    PropertyUtil.getRequiredProperty("db.password"));
        } catch (ClassNotFoundException | SQLException exception) {
            throw new DataAccessException("Unable to establish a database connection", exception);
        }
    }
}
