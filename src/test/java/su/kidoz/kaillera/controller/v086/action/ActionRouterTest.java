package su.kidoz.kaillera.controller.v086.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import su.kidoz.kaillera.controller.v086.command.ACKCommandAction;
import su.kidoz.kaillera.controller.v086.command.AdminCommandAction;
import su.kidoz.kaillera.controller.v086.command.CachedGameDataAction;
import su.kidoz.kaillera.controller.v086.command.ChatCommandAction;
import su.kidoz.kaillera.controller.v086.command.CreateGameCommandAction;
import su.kidoz.kaillera.controller.v086.command.DropGameCommandAction;
import su.kidoz.kaillera.controller.v086.command.GameChatCommandAction;
import su.kidoz.kaillera.controller.v086.command.GameDataCommandAction;
import su.kidoz.kaillera.controller.v086.command.GameKickAction;
import su.kidoz.kaillera.controller.v086.command.GameOwnerCommandAction;
import su.kidoz.kaillera.controller.v086.command.JoinGameCommandAction;
import su.kidoz.kaillera.controller.v086.command.KeepAliveAction;
import su.kidoz.kaillera.controller.v086.command.LoginCommandAction;
import su.kidoz.kaillera.controller.v086.command.QuitCommandAction;
import su.kidoz.kaillera.controller.v086.command.QuitGameCommandAction;
import su.kidoz.kaillera.controller.v086.command.StartGameCommandAction;
import su.kidoz.kaillera.controller.v086.command.UserReadyCommandAction;
import su.kidoz.kaillera.controller.v086.event.ACKEventRenderer;
import su.kidoz.kaillera.controller.v086.event.ChatEventRenderer;
import su.kidoz.kaillera.controller.v086.event.CloseGameAction;
import su.kidoz.kaillera.controller.v086.event.CreateGameEventRenderer;
import su.kidoz.kaillera.controller.v086.event.DropGameEventRenderer;
import su.kidoz.kaillera.controller.v086.event.GameChatEventRenderer;
import su.kidoz.kaillera.controller.v086.event.GameDataEventRenderer;
import su.kidoz.kaillera.controller.v086.event.GameDesynchAction;
import su.kidoz.kaillera.controller.v086.event.GameInfoAction;
import su.kidoz.kaillera.controller.v086.event.GameStatusAction;
import su.kidoz.kaillera.controller.v086.event.GameTimeoutAction;
import su.kidoz.kaillera.controller.v086.event.InfoMessageAction;
import su.kidoz.kaillera.controller.v086.event.JoinGameEventRenderer;
import su.kidoz.kaillera.controller.v086.event.LoginEventRenderer;
import su.kidoz.kaillera.controller.v086.event.LoginProgressAction;
import su.kidoz.kaillera.controller.v086.event.PlayerDesynchAction;
import su.kidoz.kaillera.controller.v086.event.QuitEventRenderer;
import su.kidoz.kaillera.controller.v086.event.QuitGameEventRenderer;
import su.kidoz.kaillera.controller.v086.event.StartGameEventRenderer;
import su.kidoz.kaillera.controller.v086.event.UserReadyEventRenderer;
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

    private ActionRouter router;

    @BeforeEach
    void setUp() {
        // Create real action instances
        AdminCommandAction adminCommandAction = new AdminCommandAction();
        GameOwnerCommandAction gameOwnerCommandAction = new GameOwnerCommandAction();

        router = new ActionRouter(List.of(new ACKCommandAction(),
                new ChatCommandAction(adminCommandAction), new CreateGameCommandAction(),
                new DropGameCommandAction(), new GameChatCommandAction(gameOwnerCommandAction),
                new GameDataCommandAction(), new JoinGameCommandAction(), new LoginCommandAction(),
                new QuitCommandAction(), new QuitGameCommandAction(), new StartGameCommandAction(),
                new UserReadyCommandAction(), adminCommandAction, new CachedGameDataAction(),
                new GameKickAction(), gameOwnerCommandAction, new KeepAliveAction()),
                List.of(new ChatEventRenderer(), new CreateGameEventRenderer(),
                        new LoginEventRenderer(), new CloseGameAction(), new QuitEventRenderer(),
                        new GameStatusAction()),
                List.of(new JoinGameEventRenderer(), new QuitGameEventRenderer(),
                        new StartGameEventRenderer(), new GameChatEventRenderer(),
                        new UserReadyEventRenderer(), new GameDataEventRenderer(),
                        new DropGameEventRenderer(), new GameDesynchAction(),
                        new PlayerDesynchAction(), new GameInfoAction(), new GameTimeoutAction()),
                List.of(new ACKEventRenderer(), new InfoMessageAction(),
                        new LoginProgressAction()));
    }

    @Nested
    @DisplayName("Message ID to Action Mapping")
    class MessageIdToActionMapping {

        @Test
        @DisplayName("should map UserInformation ID to LoginCommandAction")
        void shouldMapUserInformationToLogin() {
            V086Action action = router.getAction(UserInformation.ID);
            assertNotNull(action);
            assertTrue(action instanceof LoginCommandAction);
        }

        @Test
        @DisplayName("should map ClientACK ID to ACKCommandAction")
        void shouldMapClientACKToAck() {
            V086Action action = router.getAction(ClientACK.ID);
            assertNotNull(action);
            assertTrue(action instanceof ACKCommandAction);
        }

        @Test
        @DisplayName("should map Chat ID to ChatCommandAction")
        void shouldMapChatToChatAction() {
            V086Action action = router.getAction(Chat.ID);
            assertNotNull(action);
            assertTrue(action instanceof ChatCommandAction);
        }

        @Test
        @DisplayName("should map CreateGame ID to CreateGameCommandAction")
        void shouldMapCreateGameToAction() {
            V086Action action = router.getAction(CreateGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof CreateGameCommandAction);
        }

        @Test
        @DisplayName("should map JoinGame ID to JoinGameCommandAction")
        void shouldMapJoinGameToAction() {
            V086Action action = router.getAction(JoinGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof JoinGameCommandAction);
        }

        @Test
        @DisplayName("should map KeepAlive ID to KeepAliveAction")
        void shouldMapKeepAliveToAction() {
            V086Action action = router.getAction(KeepAlive.ID);
            assertNotNull(action);
            assertTrue(action instanceof KeepAliveAction);
        }

        @Test
        @DisplayName("should map QuitGame ID to QuitGameCommandAction")
        void shouldMapQuitGameToAction() {
            V086Action action = router.getAction(QuitGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof QuitGameCommandAction);
        }

        @Test
        @DisplayName("should map Quit ID to QuitCommandAction")
        void shouldMapQuitToAction() {
            V086Action action = router.getAction(Quit.ID);
            assertNotNull(action);
            assertTrue(action instanceof QuitCommandAction);
        }

        @Test
        @DisplayName("should map StartGame ID to StartGameCommandAction")
        void shouldMapStartGameToAction() {
            V086Action action = router.getAction(StartGame.ID);
            assertNotNull(action);
            assertTrue(action instanceof StartGameCommandAction);
        }

        @Test
        @DisplayName("should map GameChat ID to GameChatCommandAction")
        void shouldMapGameChatToAction() {
            V086Action action = router.getAction(GameChat.ID);
            assertNotNull(action);
            assertTrue(action instanceof GameChatCommandAction);
        }

        @Test
        @DisplayName("should map GameKick ID to GameKickAction")
        void shouldMapGameKickToAction() {
            V086Action action = router.getAction(GameKick.ID);
            assertNotNull(action);
            assertTrue(action instanceof GameKickAction);
        }

        @Test
        @DisplayName("should map AllReady ID to UserReadyCommandAction")
        void shouldMapAllReadyToAction() {
            V086Action action = router.getAction(AllReady.ID);
            assertNotNull(action);
            assertTrue(action instanceof UserReadyCommandAction);
        }

        @Test
        @DisplayName("should map CachedGameData ID to CachedGameDataAction")
        void shouldMapCachedGameDataToAction() {
            V086Action action = router.getAction(CachedGameData.ID);
            assertNotNull(action);
            assertTrue(action instanceof CachedGameDataAction);
        }

        @Test
        @DisplayName("should map GameData ID to GameDataCommandAction")
        void shouldMapGameDataToAction() {
            V086Action action = router.getAction(GameData.ID);
            assertNotNull(action);
            assertTrue(action instanceof GameDataCommandAction);
        }

        @Test
        @DisplayName("should map PlayerDrop ID to DropGameCommandAction")
        void shouldMapPlayerDropToAction() {
            V086Action action = router.getAction(PlayerDrop.ID);
            assertNotNull(action);
            assertTrue(action instanceof DropGameCommandAction);
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
        @DisplayName("should map ChatEvent to ChatEventRenderer handler")
        void shouldMapChatEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(ChatEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof ChatEventRenderer);
        }

        @Test
        @DisplayName("should map GameCreatedEvent to CreateGameEventRenderer handler")
        void shouldMapGameCreatedEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(GameCreatedEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof CreateGameEventRenderer);
        }

        @Test
        @DisplayName("should map UserJoinedEvent to LoginEventRenderer handler")
        void shouldMapUserJoinedEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(UserJoinedEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof LoginEventRenderer);
        }

        @Test
        @DisplayName("should map GameClosedEvent to CloseGameAction handler")
        void shouldMapGameClosedEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(GameClosedEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof CloseGameAction);
        }

        @Test
        @DisplayName("should map UserQuitEvent to QuitEventRenderer handler")
        void shouldMapUserQuitEventToHandler() {
            V086ServerEventHandler handler = router.getServerEventHandler(UserQuitEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof QuitEventRenderer);
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
        @DisplayName("should map UserJoinedGameEvent to JoinGameEventRenderer handler")
        void shouldMapUserJoinedGameEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(UserJoinedGameEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof JoinGameEventRenderer);
        }

        @Test
        @DisplayName("should map UserQuitGameEvent to QuitGameEventRenderer handler")
        void shouldMapUserQuitGameEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(UserQuitGameEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof QuitGameEventRenderer);
        }

        @Test
        @DisplayName("should map GameStartedEvent to StartGameEventRenderer handler")
        void shouldMapGameStartedEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(GameStartedEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof StartGameEventRenderer);
        }

        @Test
        @DisplayName("should map GameDataEvent to GameDataEventRenderer handler")
        void shouldMapGameDataEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(GameDataEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof GameDataEventRenderer);
        }

        @Test
        @DisplayName("should map AllReadyEvent to UserReadyEventRenderer handler")
        void shouldMapAllReadyEventToHandler() {
            V086GameEventHandler handler = router.getGameEventHandler(AllReadyEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof UserReadyEventRenderer);
        }
    }

    @Nested
    @DisplayName("User Event Handler Mapping")
    class UserEventHandlerMapping {

        @Test
        @DisplayName("should map ConnectedEvent to ACKEventRenderer handler")
        void shouldMapConnectedEventToHandler() {
            V086UserEventHandler handler = router.getUserEventHandler(ConnectedEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof ACKEventRenderer);
        }

        @Test
        @DisplayName("should map InfoMessageEvent to InfoMessageAction handler")
        void shouldMapInfoMessageEventToHandler() {
            V086UserEventHandler handler = router.getUserEventHandler(InfoMessageEvent.class);
            assertNotNull(handler);
            assertTrue(handler instanceof InfoMessageAction);
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
                if (actions1[i] != null || actions2[i] != null) {
                    assertEquals(actions1[i], actions2[i]);
                }
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
            assertEquals(original, router.getAction(UserInformation.ID));
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
            // Create a router with a missing required action mapping (no ACKCommandAction)
            AdminCommandAction adminCommandAction = new AdminCommandAction();
            GameOwnerCommandAction gameOwnerCommandAction = new GameOwnerCommandAction();

            assertThrows(IllegalStateException.class,
                    () -> new ActionRouter(
                            List.of(new ChatCommandAction(adminCommandAction),
                                    new CreateGameCommandAction(), new DropGameCommandAction(),
                                    new GameChatCommandAction(gameOwnerCommandAction),
                                    new GameDataCommandAction(), new JoinGameCommandAction(),
                                    new LoginCommandAction(), new QuitCommandAction(),
                                    new QuitGameCommandAction(), new StartGameCommandAction(),
                                    new UserReadyCommandAction(), adminCommandAction,
                                    new CachedGameDataAction(), new GameKickAction(),
                                    gameOwnerCommandAction, new KeepAliveAction()),
                            List.of(new ChatEventRenderer(), new CreateGameEventRenderer(),
                                    new LoginEventRenderer(), new CloseGameAction(),
                                    new QuitEventRenderer(), new GameStatusAction()),
                            List.of(new JoinGameEventRenderer(), new QuitGameEventRenderer(),
                                    new StartGameEventRenderer(), new GameChatEventRenderer(),
                                    new UserReadyEventRenderer(), new GameDataEventRenderer(),
                                    new DropGameEventRenderer(), new GameDesynchAction(),
                                    new PlayerDesynchAction(), new GameInfoAction(),
                                    new GameTimeoutAction()),
                            List.of(new ACKEventRenderer(), new InfoMessageAction(),
                                    new LoginProgressAction())));
        }
    }
}
