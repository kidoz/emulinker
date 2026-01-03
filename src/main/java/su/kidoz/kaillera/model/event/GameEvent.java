package su.kidoz.kaillera.model.event;

import su.kidoz.kaillera.model.KailleraGame;

/**
 * Event interface for game-related events.
 *
 * <p>
 * Sealed to restrict implementations to known game event types.
 */
public sealed interface GameEvent extends KailleraEvent permits AllReadyEvent, GameChatEvent,
        GameDataEvent, GameDesynchEvent, GameInfoEvent, GameStartedEvent, GameTimeoutEvent,
        PlayerDesynchEvent, UserDroppedGameEvent, UserJoinedGameEvent, UserQuitGameEvent {
    KailleraGame getGame();
}
