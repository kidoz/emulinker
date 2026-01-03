package su.kidoz.kaillera.model.exception;

public class StartGameException extends ActionException {
    public StartGameException(String message) {
        super(message);
    }

    public StartGameException(String message, Exception source) {
        super(message, source);
    }
}
