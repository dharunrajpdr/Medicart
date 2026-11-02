package exception;

public class InvalidMedicineDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidMedicineDataException() {
    }

    public InvalidMedicineDataException(String message) {
        super(message);
    }

    public InvalidMedicineDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
