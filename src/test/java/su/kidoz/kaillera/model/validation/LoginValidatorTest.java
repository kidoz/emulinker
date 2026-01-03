package su.kidoz.kaillera.model.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import su.kidoz.config.ServerConfig;
import su.kidoz.kaillera.access.AccessManager;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.exception.ClientAddressException;
import su.kidoz.kaillera.model.exception.LoginException;
import su.kidoz.kaillera.model.exception.PingTimeException;
import su.kidoz.kaillera.model.exception.UserNameException;
import su.kidoz.kaillera.model.impl.KailleraUserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginValidator Tests")
class LoginValidatorTest {

    @Mock
    private AccessManager accessManager;

    @Mock
    private ServerConfig serverConfig;

    @Mock
    private KailleraUser user;

    @Mock
    private KailleraUserImpl userImpl;

    private LoginValidator validator;

    @BeforeEach
    void setUp() {
        when(serverConfig.getMaxPing()).thenReturn(100);
        when(serverConfig.getMaxUserNameLength()).thenReturn(30);
        when(serverConfig.getMaxClientNameLength()).thenReturn(50);
        when(serverConfig.isAllowMultipleConnections()).thenReturn(false);
        validator = new LoginValidator(accessManager, serverConfig);
    }

    @Nested
    @DisplayName("validateNotAlreadyLoggedIn")
    class ValidateNotAlreadyLoggedIn {
        @Test
        @DisplayName("should pass when user is not logged in")
        void shouldPassWhenNotLoggedIn() throws LoginException {
            when(user.isLoggedIn()).thenReturn(false);
            assertDoesNotThrow(() -> validator.validateNotAlreadyLoggedIn(user));
        }

        @Test
        @DisplayName("should throw when user is already logged in")
        void shouldThrowWhenAlreadyLoggedIn() {
            when(user.isLoggedIn()).thenReturn(true);
            assertThrows(LoginException.class, () -> validator.validateNotAlreadyLoggedIn(user));
        }
    }

    @Nested
    @DisplayName("validateUserExists")
    class ValidateUserExists {
        @Test
        @DisplayName("should pass when user exists in list")
        void shouldPassWhenUserExists() throws LoginException {
            assertDoesNotThrow(() -> validator.validateUserExists(user, userImpl));
        }

        @Test
        @DisplayName("should throw when user is null (timed out)")
        void shouldThrowWhenUserNull() {
            assertThrows(LoginException.class, () -> validator.validateUserExists(user, null));
        }
    }

    @Nested
    @DisplayName("validateAccessLevel")
    class ValidateAccessLevel {
        @Test
        @DisplayName("should return access level for normal user")
        void shouldReturnAccessForNormalUser() throws Exception {
            InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    1234);
            when(user.getSocketAddress()).thenReturn(sockAddr);
            when(accessManager.getAccess(any())).thenReturn(AccessManager.ACCESS_NORMAL);

            int access = validator.validateAccessLevel(user);
            assertEquals(AccessManager.ACCESS_NORMAL, access);
        }

        @Test
        @DisplayName("should throw for banned user")
        void shouldThrowForBannedUser() throws Exception {
            InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    1234);
            when(user.getSocketAddress()).thenReturn(sockAddr);
            when(accessManager.getAccess(any())).thenReturn(-1); // Banned

            assertThrows(LoginException.class, () -> validator.validateAccessLevel(user));
        }
    }

    @Nested
    @DisplayName("validatePing")
    class ValidatePing {
        @Test
        @DisplayName("should pass for ping within limit")
        void shouldPassForValidPing() throws PingTimeException {
            when(user.getPing()).thenReturn(50);
            assertDoesNotThrow(() -> validator.validatePing(user, AccessManager.ACCESS_NORMAL));
        }

        @Test
        @DisplayName("should throw for ping too high")
        void shouldThrowForHighPing() {
            when(user.getPing()).thenReturn(200);
            assertThrows(PingTimeException.class,
                    () -> validator.validatePing(user, AccessManager.ACCESS_NORMAL));
        }

        @Test
        @DisplayName("should throw for negative ping")
        void shouldThrowForNegativePing() {
            when(user.getPing()).thenReturn(-1);
            assertThrows(PingTimeException.class,
                    () -> validator.validatePing(user, AccessManager.ACCESS_NORMAL));
        }

        @Test
        @DisplayName("should skip ping check for admin")
        void shouldSkipPingCheckForAdmin() throws PingTimeException {
            when(user.getPing()).thenReturn(500);
            assertDoesNotThrow(() -> validator.validatePing(user, AccessManager.ACCESS_ADMIN));
        }
    }

    @Nested
    @DisplayName("validateUserName")
    class ValidateUserName {
        @Test
        @DisplayName("should pass for valid username")
        void shouldPassForValidUsername() throws UserNameException {
            when(user.getName()).thenReturn("TestUser");
            assertDoesNotThrow(() -> validator.validateUserName(user, AccessManager.ACCESS_NORMAL));
        }

        @Test
        @DisplayName("should throw for empty username")
        void shouldThrowForEmptyUsername() {
            when(user.getName()).thenReturn("   ");
            assertThrows(UserNameException.class,
                    () -> validator.validateUserName(user, AccessManager.ACCESS_NORMAL));
        }

        @Test
        @DisplayName("should throw for username too long")
        void shouldThrowForLongUsername() {
            when(user.getName()).thenReturn("A".repeat(50));
            assertThrows(UserNameException.class,
                    () -> validator.validateUserName(user, AccessManager.ACCESS_NORMAL));
        }

        @Test
        @DisplayName("should throw for username with control characters")
        void shouldThrowForControlChars() {
            when(user.getName()).thenReturn("Test\u0000User");
            assertThrows(UserNameException.class,
                    () -> validator.validateUserName(user, AccessManager.ACCESS_NORMAL));
        }
    }

    @Nested
    @DisplayName("validateAddressMatch")
    class ValidateAddressMatch {
        @Test
        @DisplayName("should pass when addresses match")
        void shouldPassWhenAddressesMatch() throws Exception {
            InetAddress addr = InetAddress.getLoopbackAddress();
            InetSocketAddress sockAddr = new InetSocketAddress(addr, 1234);

            when(user.getSocketAddress()).thenReturn(sockAddr);
            when(userImpl.getConnectSocketAddress()).thenReturn(sockAddr);

            assertDoesNotThrow(() -> validator.validateAddressMatch(user, userImpl));
        }

        @Test
        @DisplayName("should throw when addresses don't match")
        void shouldThrowWhenAddressesDontMatch() throws Exception {
            InetSocketAddress loginAddr = new InetSocketAddress(
                    InetAddress.getByName("192.168.1.1"), 1234);
            InetSocketAddress connectAddr = new InetSocketAddress(
                    InetAddress.getByName("192.168.1.2"), 1234);

            when(user.getSocketAddress()).thenReturn(loginAddr);
            when(userImpl.getConnectSocketAddress()).thenReturn(connectAddr);

            assertThrows(ClientAddressException.class,
                    () -> validator.validateAddressMatch(user, userImpl));
        }
    }
}
