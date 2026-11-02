package exception;

public class InsufficientStockException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsufficientStockException() {
    }

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
