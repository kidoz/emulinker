package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraUser;

/**
 * Event interface for user-specific events.
 *
 * <p>
 * Sealed to restrict implementations to known user event types.
 */
public sealed interface UserEvent extends KailleraEvent permits ConnectedEvent, InfoMessageEvent {
    KailleraUser getUser();
}
