package exception;

public class WarehouseCapacityException extends Exception {
    private static final long serialVersionUID = 1L;

    public WarehouseCapacityException() {
    }

    public WarehouseCapacityException(String message) {
        super(message);
    }

    public WarehouseCapacityException(String message, Throwable cause) {
        super(message, cause);
    }
}
