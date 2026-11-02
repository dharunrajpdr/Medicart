package exception;

public class ExpiredMedicineException extends Exception {
    private static final long serialVersionUID = 1L;

    public ExpiredMedicineException() {
    }

    public ExpiredMedicineException(String message) {
        super(message);
    }

    public ExpiredMedicineException(String message, Throwable cause) {
        super(message, cause);
    }
}
