package su.kidoz.kaillera.controller.messaging;

public class ParseException extends Exception {
    public ParseException() {
    }

    public ParseException(String msg) {
        super(msg);
    }

    public ParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
