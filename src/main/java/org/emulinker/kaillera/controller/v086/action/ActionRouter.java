package org.emulinker.kaillera.controller.v086.action;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.emulinker.kaillera.controller.v086.protocol.AllReady;
import org.emulinker.kaillera.controller.v086.protocol.CachedGameData;
import org.emulinker.kaillera.controller.v086.protocol.Chat;
import org.emulinker.kaillera.controller.v086.protocol.ClientACK;
import org.emulinker.kaillera.controller.v086.protocol.CreateGame;
import org.emulinker.kaillera.controller.v086.protocol.GameChat;
import org.emulinker.kaillera.controller.v086.protocol.GameData;
import org.emulinker.kaillera.controller.v086.protocol.GameKick;
import org.emulinker.kaillera.controller.v086.protocol.JoinGame;
import org.emulinker.kaillera.controller.v086.protocol.KeepAlive;
import org.emulinker.kaillera.controller.v086.protocol.PlayerDrop;
import org.emulinker.kaillera.controller.v086.protocol.Quit;
import org.emulinker.kaillera.controller.v086.protocol.QuitGame;
import org.emulinker.kaillera.controller.v086.protocol.StartGame;
import org.emulinker.kaillera.controller.v086.protocol.UserInformation;
import org.emulinker.kaillera.model.event.AllReadyEvent;
import org.emulinker.kaillera.model.event.ChatEvent;
import org.emulinker.kaillera.model.event.ConnectedEvent;
import org.emulinker.kaillera.model.event.GameChatEvent;
import org.emulinker.kaillera.model.event.GameClosedEvent;
import org.emulinker.kaillera.model.event.GameCreatedEvent;
import org.emulinker.kaillera.model.event.GameDataEvent;
import org.emulinker.kaillera.model.event.GameDesynchEvent;
import org.emulinker.kaillera.model.event.GameInfoEvent;
import org.emulinker.kaillera.model.event.GameStartedEvent;
import org.emulinker.kaillera.model.event.GameStatusChangedEvent;
import org.emulinker.kaillera.model.event.GameTimeoutEvent;
import org.emulinker.kaillera.model.event.InfoMessageEvent;
import org.emulinker.kaillera.model.event.PlayerDesynchEvent;
import org.emulinker.kaillera.model.event.UserDroppedGameEvent;
import org.emulinker.kaillera.model.event.UserJoinedEvent;
import org.emulinker.kaillera.model.event.UserJoinedGameEvent;
import org.emulinker.kaillera.model.event.UserQuitEvent;
import org.emulinker.kaillera.model.event.UserQuitGameEvent;

/**
 * Central routing hub for the V086 protocol controller. Maps incoming protocol
 * messages to their action handlers and domain events to their protocol
 * response handlers.
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
 * V086Action.performAction()
 *     ↓ modifies domain model
 * KailleraEvent fired
 *     ↓ getXxxEventHandler(eventClass)
 * V086XxxEventHandler.handleEvent()
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
 * @see ActionBundle
 */
public final class ActionRouter {

    /** Maximum protocol message ID supported (exclusive). */
    private static final int MAX_MESSAGE_ID = 25;

    private final V086Action[] actions;
    private final Map<Class<?>, V086ServerEventHandler> serverEventHandlers;
    private final Map<Class<?>, V086GameEventHandler> gameEventHandlers;
    private final Map<Class<?>, V086UserEventHandler> userEventHandlers;

    public ActionRouter(ActionBundle actionBundle) {
        this.actions = createActionMappings(actionBundle);
        this.serverEventHandlers = Collections
                .unmodifiableMap(createServerEventHandlers(actionBundle));
        this.gameEventHandlers = Collections.unmodifiableMap(createGameEventHandlers(actionBundle));
        this.userEventHandlers = Collections.unmodifiableMap(createUserEventHandlers(actionBundle));
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

    private V086Action[] createActionMappings(ActionBundle bundle) {
        V086Action[] actionArray = new V086Action[MAX_MESSAGE_ID];

        // Map message IDs to actions
        // Array access is faster than HashMap and avoids Integer boxing
        actionArray[UserInformation.ID] = bundle.loginAction();
        actionArray[ClientACK.ID] = bundle.ackAction();
        actionArray[Chat.ID] = bundle.chatAction();
        actionArray[CreateGame.ID] = bundle.createGameAction();
        actionArray[JoinGame.ID] = bundle.joinGameAction();
        actionArray[KeepAlive.ID] = bundle.keepAliveAction();
        actionArray[QuitGame.ID] = bundle.quitGameAction();
        actionArray[Quit.ID] = bundle.quitAction();
        actionArray[StartGame.ID] = bundle.startGameAction();
        actionArray[GameChat.ID] = bundle.gameChatAction();
        actionArray[GameKick.ID] = bundle.gameKickAction();
        actionArray[AllReady.ID] = bundle.userReadyAction();
        actionArray[CachedGameData.ID] = bundle.cachedGameDataAction();
        actionArray[GameData.ID] = bundle.gameDataAction();
        actionArray[PlayerDrop.ID] = bundle.dropGameAction();

        return actionArray;
    }

    private Map<Class<?>, V086ServerEventHandler> createServerEventHandlers(ActionBundle bundle) {
        Map<Class<?>, V086ServerEventHandler> handlers = new HashMap<>();

        handlers.put(ChatEvent.class, bundle.chatAction());
        handlers.put(GameCreatedEvent.class, bundle.createGameAction());
        handlers.put(UserJoinedEvent.class, bundle.loginAction());
        handlers.put(GameClosedEvent.class, bundle.closeGameAction());
        handlers.put(UserQuitEvent.class, bundle.quitAction());
        handlers.put(GameStatusChangedEvent.class, bundle.gameStatusAction());

        return handlers;
    }

    private Map<Class<?>, V086GameEventHandler> createGameEventHandlers(ActionBundle bundle) {
        Map<Class<?>, V086GameEventHandler> handlers = new HashMap<>();

        handlers.put(UserJoinedGameEvent.class, bundle.joinGameAction());
        handlers.put(UserQuitGameEvent.class, bundle.quitGameAction());
        handlers.put(GameStartedEvent.class, bundle.startGameAction());
        handlers.put(GameChatEvent.class, bundle.gameChatAction());
        handlers.put(AllReadyEvent.class, bundle.userReadyAction());
        handlers.put(GameDataEvent.class, bundle.gameDataAction());
        handlers.put(UserDroppedGameEvent.class, bundle.dropGameAction());
        handlers.put(GameDesynchEvent.class, bundle.gameDesynchAction());
        handlers.put(PlayerDesynchEvent.class, bundle.playerDesynchAction());
        handlers.put(GameInfoEvent.class, bundle.gameInfoAction());
        handlers.put(GameTimeoutEvent.class, bundle.gameTimeoutAction());

        return handlers;
    }

    private Map<Class<?>, V086UserEventHandler> createUserEventHandlers(ActionBundle bundle) {
        Map<Class<?>, V086UserEventHandler> handlers = new HashMap<>();

        handlers.put(ConnectedEvent.class, bundle.ackAction());
        handlers.put(InfoMessageEvent.class, bundle.infoMessageAction());

        return handlers;
    }
}
