package su.kidoz.kaillera.controller;

import java.net.InetSocketAddress;

import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.exception.NewConnectionException;
import su.kidoz.kaillera.model.exception.ServerFullException;

public interface KailleraServerController {

    void start();

    void stop();
    int newConnection(InetSocketAddress clientSocketAddress, String protocol)
            throws ServerFullException, NewConnectionException;

    KailleraServer getServer();

    int getBufferSize();

    String getVersion();

    int getNumClients();

    String[] getClientTypes();
}
