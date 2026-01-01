package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraServer;

/**
 * Event interface for server-wide events.
 *
 * <p>
 * Sealed to restrict implementations to known server event types.
 */
public sealed interface ServerEvent extends KailleraEvent permits ChatEvent, GameClosedEvent,
        GameCreatedEvent, GameStatusChangedEvent, UserJoinedEvent, UserQuitEvent {
    KailleraServer getServer();
}
