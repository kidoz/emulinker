package su.kidoz.kaillera.controller.v086.action;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import su.kidoz.kaillera.controller.v086.annotation.V086Command;
import su.kidoz.kaillera.controller.v086.annotation.V086GameEvent;
import su.kidoz.kaillera.controller.v086.annotation.V086ServerEvent;
import su.kidoz.kaillera.controller.v086.annotation.V086UserEvent;
import su.kidoz.kaillera.controller.v086.protocol.AllReady;
import su.kidoz.kaillera.controller.v086.protocol.CachedGameData;
import su.kidoz.kaillera.controller.v086.protocol.Chat;
import su.kidoz.kaillera.controller.v086.protocol.ClientACK;
import su.kidoz.kaillera.controller.v086.protocol.CreateGame;
import su.kidoz.kaillera.controller.v086.protocol.GameChat;
import su.kidoz.kaillera.controller.v086.protocol.GameData;
import su.kidoz.kaillera.controller.v086.protocol.GameKick;
import su.kidoz.kaillera.controller.v086.protocol.JoinGame;
import su.kidoz.kaillera.controller.v086.protocol.KeepAlive;
import su.kidoz.kaillera.controller.v086.protocol.PlayerDrop;
import su.kidoz.kaillera.controller.v086.protocol.Quit;
import su.kidoz.kaillera.controller.v086.protocol.QuitGame;
import su.kidoz.kaillera.controller.v086.protocol.StartGame;
import su.kidoz.kaillera.controller.v086.protocol.UserInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central routing hub for the V086 protocol controller. Maps incoming protocol
 * messages to their command action handlers and domain events to their event
 * renderer handlers.
 *
 * <p>
 * The router provides O(1) lookup for actions via array indexing by message ID,
 * and event handlers via class-keyed maps.
 *
 * <h2>Message Flow</h2>
 *
 * <pre>
 * Client UDP Message
 *     ↓ parse
 * V086Message (with ID)
 *     ↓ getAction(messageId)
 * CommandAction.performAction()
 *     ↓ modifies domain model
 * KailleraEvent fired
 *     ↓ getXxxEventHandler(eventClass)
 * EventRenderer.handleEvent()
 *     ↓ creates response
 * V086Message sent to client
 * </pre>
 *
 * <h2>Handler Types</h2>
 * <ul>
 * <li>{@link V086ServerEventHandler} - Server-wide events (chat, user
 * join/quit)</li>
 * <li>{@link V086GameEventHandler} - Game-specific events (game data,
 * sync)</li>
 * <li>{@link V086UserEventHandler} - User-specific events (connection, info
 * messages)</li>
 * </ul>
 *
 * @see V086Action
 */
public final class ActionRouter {

    private static final Logger log = LoggerFactory.getLogger(ActionRouter.class);

    /** Maximum protocol message ID supported (exclusive). */
    private static final int MAX_MESSAGE_ID = 25;

    /**
     * Required message IDs that must have action handlers registered. These
     * represent the core protocol messages that clients can send.
     */
    private static final int[] REQUIRED_MESSAGE_IDS = {UserInformation.ID, ClientACK.ID, Chat.ID,
            CreateGame.ID, JoinGame.ID, KeepAlive.ID, QuitGame.ID, Quit.ID, StartGame.ID,
            GameChat.ID, GameKick.ID, AllReady.ID, CachedGameData.ID, GameData.ID, PlayerDrop.ID};

    private final V086Action[] actions;
    private final Map<Class<?>, V086ServerEventHandler> serverEventHandlers;
    private final Map<Class<?>, V086GameEventHandler> gameEventHandlers;
    private final Map<Class<?>, V086UserEventHandler> userEventHandlers;

    public ActionRouter(List<V086Action> actions, List<V086ServerEventHandler> serverEventHandlers,
            List<V086GameEventHandler> gameEventHandlers,
            List<V086UserEventHandler> userEventHandlers) {
        this.actions = createActionMappings(actions);
        validateActionMappings();
        this.serverEventHandlers = Collections
                .unmodifiableMap(createServerEventHandlers(serverEventHandlers));
        this.gameEventHandlers = Collections
                .unmodifiableMap(createGameEventHandlers(gameEventHandlers));
        this.userEventHandlers = Collections
                .unmodifiableMap(createUserEventHandlers(userEventHandlers));
    }

    /**
     * Gets the action for a given message ID.
     *
     * @param messageId
     *            the V086 protocol message ID
     * @return the action handler, or null if no action is registered for this
     *         message ID
     */
    public V086Action getAction(int messageId) {
        if (messageId < 0 || messageId >= actions.length) {
            return null;
        }
        return actions[messageId];
    }

    /**
     * Gets all registered actions indexed by message ID.
     *
     * @return array of actions where index corresponds to message ID
     */
    public V086Action[] getActions() {
        return actions.clone();
    }

    /**
     * Gets the handler for a server event type.
     *
     * @param eventClass
     *            the event class
     * @return the handler, or null if no handler is registered
     */
    public V086ServerEventHandler getServerEventHandler(Class<?> eventClass) {
        return serverEventHandlers.get(eventClass);
    }

    /**
     * Gets all server event handlers.
     *
     * @return unmodifiable map of event class to handler
     */
    public Map<Class<?>, V086ServerEventHandler> getServerEventHandlers() {
        return serverEventHandlers;
    }

    /**
     * Gets the handler for a game event type.
     *
     * @param eventClass
     *            the event class
     * @return the handler, or null if no handler is registered
     */
    public V086GameEventHandler getGameEventHandler(Class<?> eventClass) {
        return gameEventHandlers.get(eventClass);
    }

    /**
     * Gets all game event handlers.
     *
     * @return unmodifiable map of event class to handler
     */
    public Map<Class<?>, V086GameEventHandler> getGameEventHandlers() {
        return gameEventHandlers;
    }

    /**
     * Gets the handler for a user event type.
     *
     * @param eventClass
     *            the event class
     * @return the handler, or null if no handler is registered
     */
    public V086UserEventHandler getUserEventHandler(Class<?> eventClass) {
        return userEventHandlers.get(eventClass);
    }

    /**
     * Gets all user event handlers.
     *
     * @return unmodifiable map of event class to handler
     */
    public Map<Class<?>, V086UserEventHandler> getUserEventHandlers() {
        return userEventHandlers;
    }

    private V086Action[] createActionMappings(List<V086Action> availableActions) {
        V086Action[] actionArray = new V086Action[MAX_MESSAGE_ID];

        // Map message IDs to command actions (inbound)
        // Array access is faster than HashMap and avoids Integer boxing
        for (V086Action action : availableActions) {
            V086Command mapping = action.getClass().getAnnotation(V086Command.class);
            if (mapping == null) {
                log.debug("Skipping unannotated action: {}", action.getClass().getSimpleName());
                continue;
            }

            int messageId = mapping.messageId();
            if (messageId < 0 || messageId >= MAX_MESSAGE_ID) {
                throw new IllegalStateException("Invalid message ID " + messageId + " for action "
                        + action.getClass().getName());
            }
            if (actionArray[messageId] != null) {
                throw new IllegalStateException("Duplicate action mapping for message ID 0x"
                        + Integer.toHexString(messageId) + ": "
                        + actionArray[messageId].getClass().getName() + " and "
                        + action.getClass().getName());
            }

            actionArray[messageId] = action;
        }

        return actionArray;
    }

    private Map<Class<?>, V086ServerEventHandler> createServerEventHandlers(
            List<V086ServerEventHandler> availableHandlers) {
        Map<Class<?>, V086ServerEventHandler> handlers = new HashMap<>();

        // Map server events to event renderers (outbound)
        for (V086ServerEventHandler handler : availableHandlers) {
            V086ServerEvent mapping = handler.getClass().getAnnotation(V086ServerEvent.class);
            if (mapping == null) {
                log.debug("Skipping unannotated server handler: {}",
                        handler.getClass().getSimpleName());
                continue;
            }

            Class<?> eventType = mapping.eventType();
            if (handlers.put(eventType, handler) != null) {
                throw new IllegalStateException(
                        "Duplicate server event handler for " + eventType.getName());
            }
        }

        return handlers;
    }

    private Map<Class<?>, V086GameEventHandler> createGameEventHandlers(
            List<V086GameEventHandler> availableHandlers) {
        Map<Class<?>, V086GameEventHandler> handlers = new HashMap<>();

        // Map game events to event renderers (outbound)
        for (V086GameEventHandler handler : availableHandlers) {
            V086GameEvent mapping = handler.getClass().getAnnotation(V086GameEvent.class);
            if (mapping == null) {
                log.debug("Skipping unannotated game handler: {}",
                        handler.getClass().getSimpleName());
                continue;
            }

            Class<?> eventType = mapping.eventType();
            if (handlers.put(eventType, handler) != null) {
                throw new IllegalStateException(
                        "Duplicate game event handler for " + eventType.getName());
            }
        }

        return handlers;
    }

    private Map<Class<?>, V086UserEventHandler> createUserEventHandlers(
            List<V086UserEventHandler> availableHandlers) {
        Map<Class<?>, V086UserEventHandler> handlers = new HashMap<>();

        // Map user events to event renderers (outbound)
        for (V086UserEventHandler handler : availableHandlers) {
            V086UserEvent mapping = handler.getClass().getAnnotation(V086UserEvent.class);
            if (mapping == null) {
                log.debug("Skipping unannotated user handler: {}",
                        handler.getClass().getSimpleName());
                continue;
            }

            Class<?> eventType = mapping.eventType();
            if (handlers.put(eventType, handler) != null) {
                throw new IllegalStateException(
                        "Duplicate user event handler for " + eventType.getName());
            }
        }

        return handlers;
    }

    /**
     * Validates that all required protocol message IDs have action handlers
     * registered. This ensures the router is properly configured at startup.
     *
     * @throws IllegalStateException
     *             if any required message ID is missing a handler
     */
    private void validateActionMappings() {
        StringBuilder missingHandlers = new StringBuilder();

        for (int messageId : REQUIRED_MESSAGE_IDS) {
            if (messageId < 0 || messageId >= actions.length || actions[messageId] == null) {
                if (!missingHandlers.isEmpty()) {
                    missingHandlers.append(", ");
                }
                missingHandlers.append("0x").append(Integer.toHexString(messageId));
            }
        }

        if (!missingHandlers.isEmpty()) {
            throw new IllegalStateException(
                    "ActionRouter validation failed: missing handlers for message IDs: "
                            + missingHandlers);
        }

        long registeredCount = Arrays.stream(actions).filter(Objects::nonNull).count();
        log.info("Protocol validation passed: {} action handlers registered for {} required IDs",
                registeredCount, REQUIRED_MESSAGE_IDS.length);
    }
}
