package su.kidoz.kaillera.controller.v086.action;

public final class FatalActionException extends Exception {
    public FatalActionException(String message) {
        super(message);
    }

    public FatalActionException(String message, Exception source) {
        super(message, source);
    }
}
