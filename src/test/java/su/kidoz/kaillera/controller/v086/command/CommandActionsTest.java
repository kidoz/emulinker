package su.kidoz.kaillera.controller.v086.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import su.kidoz.kaillera.controller.v086.V086ClientHandler;
import su.kidoz.kaillera.controller.v086.action.FatalActionException;
import su.kidoz.kaillera.controller.v086.protocol.Chat_Notification;
import su.kidoz.kaillera.controller.v086.protocol.Chat_Request;
import su.kidoz.kaillera.controller.v086.protocol.GameChat_Request;
import su.kidoz.kaillera.controller.v086.protocol.Quit_Request;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.exception.ChatException;

/**
 * Unit tests for V086 command action handlers.
 *
 * <p>
 * Tests verify that command actions properly handle protocol messages, delegate
 * to domain model, and handle error conditions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Command Actions Tests")
class CommandActionsTest {

    @Mock
    private V086ClientHandler clientHandler;

    @Mock
    private KailleraUser user;

    @BeforeEach
    void setUp() {
        when(clientHandler.getUser()).thenReturn(user);
    }

    @Nested
    @DisplayName("ChatCommandAction")
    class ChatCommandActionTests {

        private ChatCommandAction action;
        private AdminCommandAction adminAction;

        @BeforeEach
        void setUp() {
            adminAction = new AdminCommandAction();
            action = new ChatCommandAction(adminAction);
        }

        @Test
        @DisplayName("should process valid chat message")
        void shouldProcessValidChatMessage() throws Exception {
            Chat_Request message = new Chat_Request(1, "Hello world");

            action.performAction(message, clientHandler);

            verify(user).chat("Hello world");
            assertEquals(1, action.getActionPerformedCount());
        }

        @Test
        @DisplayName("should increment action count for each message")
        void shouldIncrementActionCount() throws Exception {
            Chat_Request message1 = new Chat_Request(1, "First");
            Chat_Request message2 = new Chat_Request(2, "Second");

            action.performAction(message1, clientHandler);
            action.performAction(message2, clientHandler);

            assertEquals(2, action.getActionPerformedCount());
        }

        @Test
        @DisplayName("should throw FatalActionException for wrong message type")
        void shouldThrowForWrongMessageType() throws Exception {
            Chat_Notification wrongMessage = new Chat_Notification(1, "user", "message");

            assertThrows(FatalActionException.class,
                    () -> action.performAction(wrongMessage, clientHandler));
        }

        @Test
        @DisplayName("should handle chat denial gracefully")
        void shouldHandleChatDenialGracefully() throws Exception {
            Chat_Request message = new Chat_Request(1, "Denied message");
            doThrow(new ChatException("Flood control")).when(user).chat("Denied message");
            when(clientHandler.getNextMessageNumber()).thenReturn(2);

            // Should not throw - handles exception internally
            action.performAction(message, clientHandler);

            // Action count is still incremented before the exception
            assertEquals(1, action.getActionPerformedCount());
        }

    }

    @Nested
    @DisplayName("GameChatCommandAction")
    class GameChatCommandActionTests {

        private GameChatCommandAction action;

        @BeforeEach
        void setUp() {
            action = new GameChatCommandAction(new GameOwnerCommandAction());
        }

        @Test
        @DisplayName("should process valid game chat message")
        void shouldProcessValidGameChatMessage() throws Exception {
            GameChat_Request message = new GameChat_Request(1, "In-game message");

            action.performAction(message, clientHandler);

            verify(user).gameChat("In-game message", 1);
            assertEquals(1, action.getActionPerformedCount());
        }

        @Test
        @DisplayName("should throw FatalActionException for wrong message type")
        void shouldThrowForWrongMessageType() throws Exception {
            Chat_Request wrongMessage = new Chat_Request(1, "wrong type");

            assertThrows(FatalActionException.class,
                    () -> action.performAction(wrongMessage, clientHandler));
        }
    }

    @Nested
    @DisplayName("QuitCommandAction")
    class QuitCommandActionTests {

        private QuitCommandAction action;

        @BeforeEach
        void setUp() {
            action = new QuitCommandAction();
        }

        @Test
        @DisplayName("should process quit message")
        void shouldProcessQuitMessage() throws Exception {
            Quit_Request message = new Quit_Request(1, "Goodbye");

            action.performAction(message, clientHandler);

            verify(user).quit("Goodbye");
            assertEquals(1, action.getActionPerformedCount());
        }

    }

    @Nested
    @DisplayName("KeepAliveAction")
    class KeepAliveActionTests {

        private KeepAliveAction action;

        @BeforeEach
        void setUp() {
            action = new KeepAliveAction();
        }

        @Test
        @DisplayName("should update keep-alive timestamp")
        void shouldUpdateKeepAliveTimestamp() throws Exception {
            var message = mock(su.kidoz.kaillera.controller.v086.protocol.KeepAlive.class);

            action.performAction(message, clientHandler);

            verify(user).updateLastKeepAlive();
        }
    }

    @Nested
    @DisplayName("CreateGameCommandAction")
    class CreateGameCommandActionTests {

        private CreateGameCommandAction action;

        @BeforeEach
        void setUp() {
            action = new CreateGameCommandAction();
        }

        @Test
        @DisplayName("should create game with ROM name")
        void shouldCreateGameWithRomName() throws Exception {
            var message = mock(su.kidoz.kaillera.controller.v086.protocol.CreateGame_Request.class);
            when(message.getRomName()).thenReturn("Super Mario Bros");

            action.performAction(message, clientHandler);

            verify(user).createGame("Super Mario Bros");
            assertEquals(1, action.getActionPerformedCount());
        }
    }

    @Nested
    @DisplayName("JoinGameCommandAction")
    class JoinGameCommandActionTests {

        private JoinGameCommandAction action;

        @BeforeEach
        void setUp() {
            action = new JoinGameCommandAction();
        }

        @Test
        @DisplayName("should join game by ID")
        void shouldJoinGameById() throws Exception {
            var message = mock(su.kidoz.kaillera.controller.v086.protocol.JoinGame_Request.class);
            when(message.getGameID()).thenReturn(42);

            action.performAction(message, clientHandler);

            verify(user).joinGame(42);
            assertEquals(1, action.getActionPerformedCount());
        }
    }
}
