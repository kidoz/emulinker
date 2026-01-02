package org.emulinker.kaillera.controller.v086.action;

import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.model.event.ServerEvent;

public interface V086ServerEventHandler {
    String toString();

    void handleEvent(ServerEvent event, V086ClientHandler clientHandler);

    int getHandledEventCount();
}
