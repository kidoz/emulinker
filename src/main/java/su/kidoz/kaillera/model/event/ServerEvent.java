package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraServer;

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
