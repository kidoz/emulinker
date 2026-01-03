package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraUser;

/**
 * Event interface for user-specific events.
 *
 * <p>
 * Sealed to restrict implementations to known user event types.
 */
public sealed interface UserEvent extends KailleraEvent
        permits ConnectedEvent, InfoMessageEvent, LoginProgressEvent {
    KailleraUser getUser();
}
