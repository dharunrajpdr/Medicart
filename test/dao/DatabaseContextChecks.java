package dao;

import exception.DataAccessException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public final class DatabaseContextChecks {
    private static int checks;

    private DatabaseContextChecks() {
    }

    public static void main(String[] args) {
        checkConnectionIsClosedAfterUse();
        checkTransactionCommit();
        checkTransactionRollback();
        System.out.println("Database context checks passed: " + checks);
    }

    private static void checkConnectionIsClosedAfterUse() {
        AtomicInteger closeCount = new AtomicInteger();
        Connection connection = connection(new AtomicInteger(), new AtomicInteger(), closeCount);
        DatabaseContext context = context(connection);
        String result = context.execute(ignored -> "connected");
        check("connected".equals(result), "execute returns work result");
        check(closeCount.get() == 1, "execute closes its connection");
    }

    private static void checkTransactionCommit() {
        AtomicInteger commitCount = new AtomicInteger();
        AtomicInteger rollbackCount = new AtomicInteger();
        AtomicInteger closeCount = new AtomicInteger();
        Connection connection = connection(commitCount, rollbackCount, closeCount);
        DatabaseContext context = context(connection);
        int result = context.inTransaction(ignored -> 42);
        check(result == 42, "transaction returns work result");
        check(commitCount.get() == 1, "successful transaction commits once");
        check(rollbackCount.get() == 0, "successful transaction does not roll back");
        check(closeCount.get() == 1, "transaction closes its connection");
    }

    private static void checkTransactionRollback() {
        AtomicInteger commitCount = new AtomicInteger();
        AtomicInteger rollbackCount = new AtomicInteger();
        AtomicInteger closeCount = new AtomicInteger();
        Connection connection = connection(commitCount, rollbackCount, closeCount);
        DatabaseContext context = context(connection);
        DataAccessException failure = expectThrows(DataAccessException.class,
                () -> context.inTransaction(ignored -> {
                    throw new SQLException("simulated failure");
                }));
        check(failure.getCause() instanceof SQLException,
                "transaction preserves the database failure as cause");
        check(commitCount.get() == 0, "failed transaction does not commit");
        check(rollbackCount.get() == 1, "failed transaction rolls back once");
        check(closeCount.get() == 1, "failed transaction closes its connection");
    }

    private static DatabaseContext context(Connection connection) {
        return new DatabaseContext() {
            @Override
            public Connection getConnection() {
                return connection;
            }
        };
    }

    private static Connection connection(AtomicInteger commits,
            AtomicInteger rollbacks, AtomicInteger closes) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "commit":
                            commits.incrementAndGet();
                            return null;
                        case "rollback":
                            rollbacks.incrementAndGet();
                            return null;
                        case "close":
                            closes.incrementAndGet();
                            return null;
                        case "setAutoCommit":
                            return null;
                        case "isClosed":
                            return false;
                        case "toString":
                            return "TestConnection";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static <T extends Throwable> T expectThrows(Class<T> expected,
            ThrowingRunnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return expected.cast(actual);
            }
            throw new AssertionError("Expected " + expected.getSimpleName()
                    + " but got " + actual.getClass().getSimpleName(), actual);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
