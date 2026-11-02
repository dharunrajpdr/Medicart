package exception;

public class PrescriptionAlreadyVerifiedException extends Exception {
    private static final long serialVersionUID = 1L;

    public PrescriptionAlreadyVerifiedException() {
    }

    public PrescriptionAlreadyVerifiedException(String message) {
        super(message);
    }

    public PrescriptionAlreadyVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
