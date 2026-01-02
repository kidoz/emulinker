package org.emulinker.net;

public class BindException extends Exception {
    private final int port;

    public BindException(String msg, int port, Exception e) {
        super(msg, e);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number: " + port);
        }
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
