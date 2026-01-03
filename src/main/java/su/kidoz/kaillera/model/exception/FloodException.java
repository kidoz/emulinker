package su.kidoz.kaillera.model.exception;

public class FloodException extends ActionException {
    public FloodException() {
    }

    public FloodException(String message) {
        super(message);
    }

    public FloodException(String message, Exception source) {
        super(message, source);
    }
}
