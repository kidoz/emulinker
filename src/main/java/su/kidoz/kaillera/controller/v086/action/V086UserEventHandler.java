package su.kidoz.kaillera.controller.v086.action;

import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.model.event.UserEvent;

public interface V086UserEventHandler {
    String toString();

    void handleEvent(UserEvent event, V086ClientHandler clientHandler);

    int getHandledEventCount();
}
