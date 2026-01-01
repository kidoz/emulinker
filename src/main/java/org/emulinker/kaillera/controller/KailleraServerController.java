package org.emulinker.kaillera.controller;

import java.net.InetSocketAddress;

import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.exception.*;

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
