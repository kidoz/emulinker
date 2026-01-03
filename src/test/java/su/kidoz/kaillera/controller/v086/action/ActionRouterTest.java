package su.kidoz.kaillera.controller.v086.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
import su.kidoz.kaillera.model.event.AllReadyEvent;
import su.kidoz.kaillera.model.event.ChatEvent;
import su.kidoz.kaillera.model.event.ConnectedEvent;
import su.kidoz.kaillera.model.event.GameClosedEvent;
import su.kidoz.kaillera.model.event.GameCreatedEvent;
import su.kidoz.kaillera.model.event.GameDataEvent;
import su.kidoz.kaillera.model.event.GameStartedEvent;
import su.kidoz.kaillera.model.event.InfoMessageEvent;
import su.kidoz.kaillera.model.event.UserJoinedEvent;
import su.kidoz.kaillera.model.event.UserJoinedGameEvent;
import su.kidoz.kaillera.model.event.UserQuitEvent;
import su.kidoz.kaillera.model.event.UserQuitGameEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ActionRouter - the central routing hub for V086 protocol.
 *
 * <p>
 * Tests verify that:
 * <ul>
 * <li>All required message IDs have action handlers</li>
 * <li>All event types have appropriate handlers</li>
 * <li>Invalid message IDs return null</li>
 * <li>Validation fails when handlers are missing</li>
 * </ul>
 */
@DisplayName("ActionRouter Tests")
class ActionRouterTest {

    private ActionBundle actionBundle;
    private ActionRouter router;

    @BeforeEach
    void setUp() {
        // Create real action instances
        AdminCommandAction adminCommandAction = new AdminCommandAction();
        GameOwnerCommandAction gameOwnerCommandAction = new GameOwnerCommandAction();

        actionBundle = new ActionBundle(new ACKAction(), adminCommandAction,
                new CachedGameDataAction(), new ChatAction(adminCommandAction),
                new CloseGameAction(), new CreateGameAction(), new DropGameAction(),
                new GameChatAction(gameOwnerCommandAction), new GameDataAction(),
                new GameDesynchAction(), new GameInfoAction(), new GameKickAction(),
                gameOwnerCommandAction, new GameStatusAction(), new GameTimeoutAction(),
                new InfoMessageAction(), new JoinGameAction(), new KeepAliveAction(),
                new LoginAction(), new LoginProgressAction(), new PlayerDesynchAction(),
                new QuitAction(), new QuitGameAction(), new StartGameAction(),
                new UserReadyAction());

        router = new ActionRouter(actionBundle);
    }

    @Nested
    @DisplayName("Message ID to Action Mapping")
    class MessageIdToActionMapping {

        @Test
        @DisplayName("should map UserInformation ID to LoginAction")
        void shouldMapUserInformationToLogin() {
            V086Action action = router.getAction(UserInformation.ID);
            assertNotNull(action);
            assertTrue(action instanceof LoginAction);
        }

        @Test
        @DisplayName("should map ClientACK ID to ACKAction")
        void shouldMapClientACKToAck() {
            V086Action action = router.getAction(ClientACK.ID);
            assertNotNull(action);
            assertTrue(action instanceof ACKAction);
        }

        @Test
        @DisplayName("should map Chat ID to ChatAction")
        void shouldMapChatToChatAction() {
            V086Action action = router.getAction(Chat.ID);
            assertNotNull(action);
            assertTrue(action instanceof ChatAction);
        }

        @Test
        @DisplayName("should map CreateGame ID to CreateGameAction")
        void shouldMapCreateGameToAction() {
            V086Action action = router.getAction(CreateGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof CreateGameAction);
        }

        @Test
        @DisplayName("should map JoinGame ID to JoinGameAction")
        void shouldMapJoinGameToAction() {
            V086Action action = router.getAction(JoinGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof JoinGameAction);
        }

        @Test
        @DisplayName("should map KeepAlive ID to KeepAliveAction")
        void shouldMapKeepAliveToAction() {
            V086Action action = router.getAction(KeepAlive.ID);
            assertNotNull(action);
            assertTrue(action instanceof KeepAliveAction);
        }

        @Test
        @DisplayName("should map QuitGame ID to QuitGameAction")
        void shouldMapQuitGameToAction() {
            V086Action action = router.getAction(QuitGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof QuitGameAction);
        }

        @Test
        @DisplayName("should map Quit ID to QuitAction")
        void shouldMapQuitToAction() {
            V086Action action = router.getAction(Quit.ID);
            assertNotNull(action);
            assertTrue(action instanceof QuitAction);
        }

        @Test
        @DisplayName("should map StartGame ID to StartGameAction")
        void shouldMapStartGameToAction() {
            V086Action action = router.getAction(StartGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof StartGameAction);
        }

        @Test
        @DisplayName("should map GameChat ID to GameChatAction")
        void shouldMapGameChatToAction() {
            V086Action action = router.getAction(GameChat.ID);
            assertNotNull(action);
            assertTrue(action instanceof GameChatAction);
        }

        @Test
        @DisplayName("should map GameKick ID to GameKickAction")
        void shouldMapGameKickToAction() {
            V086Action action = router.getAction(GameKick.ID);
            assertNotNull(action);
            assertTrue(action instanceof GameKickAction);
        }

        @Test
        @DisplayName("should map AllReady ID to UserReadyAction")
        void shouldMapAllReadyToAction() {
            V086Action action = router.getAction(AllReady.ID);
            assertNotNull(action);
            assertTrue(action instanceof UserReadyAction);
        }

        @Test
        @DisplayName("should map CachedGameData ID to CachedGameDataAction")
        void shouldMapCachedGameDataToAction() {
            V086Action action = router.getAction(CachedGameData.ID);
            assertNotNull(action);
            assertTrue(action instanceof CachedGameDataAction);
        }

        @Test
        @DisplayName("should map GameData ID to GameDataAction")
        void shouldMapGameDataToAction() {
            V086Action action = router.getAction(GameData.ID);
            assertNotNull(action);
            assertTrue(action instanceof GameDataAction);
        }

        @Test
        @DisplayName("should map PlayerDrop ID to DropGameAction")
        void shouldMapPlayerDropToAction() {
            V086Action action = router.getAction(PlayerDrop.ID);
            assertNotNull(action);
            assertTrue(action instanceof DropGameAction);
        }
    }

    @Nested
    @DisplayName("Invalid Message ID Handling")
    class InvalidMessageIdHandling {

        @Test
        @DisplayName("should return null for negative message ID")
        void shouldReturnNullForNegativeId() {
            assertNull(router.getAction(-1));
        }

        @Test
        @DisplayName("should return null for message ID beyond max")
        void shouldReturnNullForIdBeyondMax() {
            assertNull(router.getAction(100));
        }

        @Test
        @DisplayName("should return null for unregistered message ID")
        void shouldReturnNullForUnregisteredId() {
            // Message ID 0 is not registered
            assertNull(router.getAction(0));
        }
    }

    @Nested
    @DisplayName("Server Event Handler Mapping")
    class ServerEventHandlerMapping {

        @Test
        @DisplayName("should map ChatEvent to ChatAction handler")
        void shouldMapChatEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(ChatEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.chatAction(), handler);
        }

        @Test
        @DisplayName("should map GameCreatedEvent to CreateGameAction handler")
        void shouldMapGameCreatedEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(GameCreatedEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.createGameAction(), handler);
        }

        @Test
        @DisplayName("should map UserJoinedEvent to LoginAction handler")
        void shouldMapUserJoinedEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(UserJoinedEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.loginAction(), handler);
        }

        @Test
        @DisplayName("should map GameClosedEvent to CloseGameAction handler")
        void shouldMapGameClosedEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(GameClosedEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.closeGameAction(), handler);
        }

        @Test
        @DisplayName("should map UserQuitEvent to QuitAction handler")
        void shouldMapUserQuitEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(UserQuitEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.quitAction(), handler);
        }

        @Test
        @DisplayName("should return null for unregistered event type")
        void shouldReturnNullForUnregisteredEvent() {
            assertNull(router.getServerEventHandler(Object.class));
        }
    }

    @Nested
    @DisplayName("Game Event Handler Mapping")
    class GameEventHandlerMapping {

        @Test
        @DisplayName("should map UserJoinedGameEvent to JoinGameAction handler")
        void shouldMapUserJoinedGameEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(UserJoinedGameEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.joinGameAction(), handler);
        }

        @Test
        @DisplayName("should map UserQuitGameEvent to QuitGameAction handler")
        void shouldMapUserQuitGameEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(UserQuitGameEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.quitGameAction(), handler);
        }

        @Test
        @DisplayName("should map GameStartedEvent to StartGameAction handler")
        void shouldMapGameStartedEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(GameStartedEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.startGameAction(), handler);
        }

        @Test
        @DisplayName("should map GameDataEvent to GameDataAction handler")
        void shouldMapGameDataEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(GameDataEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.gameDataAction(), handler);
        }

        @Test
        @DisplayName("should map AllReadyEvent to UserReadyAction handler")
        void shouldMapAllReadyEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(AllReadyEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.userReadyAction(), handler);
        }
    }

    @Nested
    @DisplayName("User Event Handler Mapping")
    class UserEventHandlerMapping {

        @Test
        @DisplayName("should map ConnectedEvent to ACKAction handler")
        void shouldMapConnectedEventToHandler() {
            V086UserEventHandler handler = router.getUserEventHandler(ConnectedEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.ackAction(), handler);
        }

        @Test
        @DisplayName("should map InfoMessageEvent to InfoMessageAction handler")
        void shouldMapInfoMessageEventToHandler() {
            V086UserEventHandler handler = router.getUserEventHandler(InfoMessageEvent.class);
            assertNotNull(handler);
            assertSame(actionBundle.infoMessageAction(), handler);
        }
    }

    @Nested
    @DisplayName("Actions Array Access")
    class ActionsArrayAccess {

        @Test
        @DisplayName("should return cloned actions array")
        void shouldReturnClonedActionsArray() {
            V086Action[] actions1 = router.getActions();
            V086Action[] actions2 = router.getActions();

            // Arrays should be different instances (cloned)
            assertTrue(actions1 != actions2);

            // But contents should be the same
            assertEquals(actions1.length, actions2.length);
            for (int i = 0; i < actions1.length; i++) {
                assertSame(actions1[i], actions2[i]);
            }
        }

        @Test
        @DisplayName("modifying returned array should not affect router")
        void modifyingArrayShouldNotAffectRouter() {
            V086Action[] actions = router.getActions();
            V086Action original = actions[UserInformation.ID];

            // Modify the returned array
            actions[UserInformation.ID] = null;

            // Router should still have the original action
            assertSame(original, router.getAction(UserInformation.ID));
        }
    }

    @Nested
    @DisplayName("Handler Maps Access")
    class HandlerMapsAccess {

        @Test
        @DisplayName("should return unmodifiable server event handlers map")
        void shouldReturnUnmodifiableServerEventHandlersMap() {
            var handlers = router.getServerEventHandlers();
            assertThrows(UnsupportedOperationException.class,
                    () -> handlers.put(Object.class, mock(V086ServerEventHandler.class)));
        }

        @Test
        @DisplayName("should return unmodifiable game event handlers map")
        void shouldReturnUnmodifiableGameEventHandlersMap() {
            var handlers = router.getGameEventHandlers();
            assertThrows(UnsupportedOperationException.class,
                    () -> handlers.put(Object.class, mock(V086GameEventHandler.class)));
        }

        @Test
        @DisplayName("should return unmodifiable user event handlers map")
        void shouldReturnUnmodifiableUserEventHandlersMap() {
            var handlers = router.getUserEventHandlers();
            assertThrows(UnsupportedOperationException.class,
                    () -> handlers.put(Object.class, mock(V086UserEventHandler.class)));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw exception when required action is missing")
        void shouldThrowWhenRequiredActionMissing() {
            // Create a bundle with a null action for a required message ID
            ActionBundle invalidBundle = new ActionBundle(null, // ACKAction is required
                    new AdminCommandAction(), new CachedGameDataAction(),
                    new ChatAction(new AdminCommandAction()), new CloseGameAction(),
                    new CreateGameAction(), new DropGameAction(),
                    new GameChatAction(new GameOwnerCommandAction()), new GameDataAction(),
                    new GameDesynchAction(), new GameInfoAction(), new GameKickAction(),
                    new GameOwnerCommandAction(), new GameStatusAction(), new GameTimeoutAction(),
                    new InfoMessageAction(), new JoinGameAction(), new KeepAliveAction(),
                    new LoginAction(), new LoginProgressAction(), new PlayerDesynchAction(),
                    new QuitAction(), new QuitGameAction(), new StartGameAction(),
                    new UserReadyAction());

            assertThrows(IllegalStateException.class, () -> new ActionRouter(invalidBundle));
        }
    }
}
