package su.kidoz.util;

public interface Executable extends Runnable {
    boolean isRunning();

    void stop();
}
