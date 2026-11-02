package exception;

public class PrescriptionRequiredException extends Exception {
    private static final long serialVersionUID = 1L;

    public PrescriptionRequiredException() {
    }

    public PrescriptionRequiredException(String message) {
        super(message);
    }

    public PrescriptionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
