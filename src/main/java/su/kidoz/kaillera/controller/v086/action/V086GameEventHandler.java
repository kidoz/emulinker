package su.kidoz.kaillera.controller.v086.action;

import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.model.event.GameEvent;

public interface V086GameEventHandler {
    String toString();

    void handleEvent(GameEvent event, V086ClientHandler clientHandler);

    int getHandledEventCount();
}
