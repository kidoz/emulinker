package org.emulinker.kaillera.controller;

import java.net.InetSocketAddress;

import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.exception.*;
import org.picocontainer.Startable;

public interface KailleraServerController extends Startable
{
	int newConnection(InetSocketAddress clientSocketAddress, String protocol) throws ServerFullException, NewConnectionException;

	KailleraServer getServer();

	int getBufferSize();

	String getVersion();

	int getNumClients();

	String[] getClientTypes();
}
