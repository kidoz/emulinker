package su.kidoz.kaillera.service;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;

import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.KailleraEventListener;
import su.kidoz.kaillera.model.exception.CloseGameException;
import su.kidoz.kaillera.model.exception.DropGameException;
import su.kidoz.kaillera.model.exception.LoginException;
import su.kidoz.kaillera.model.exception.NewConnectionException;
import su.kidoz.kaillera.model.exception.QuitException;
import su.kidoz.kaillera.model.exception.QuitGameException;
import su.kidoz.kaillera.model.exception.ServerFullException;

/**
 * Service interface for user management operations. Separates user lifecycle
 * management from the main server implementation for better testability and
 * separation of concerns.
 */
public interface UserService {

    /**
     * Creates a new connection for a user.
     *
     * @param clientSocketAddress
     *            the client's socket address
     * @param protocol
     *            the protocol version
     * @param listener
     *            the event listener for this user
     * @return the newly created user
     * @throws ServerFullException
     *             if the server is at capacity
     * @throws NewConnectionException
     *             if the connection cannot be created
     */
    KailleraUser newConnection(InetSocketAddress clientSocketAddress, String protocol,
            KailleraEventListener listener) throws ServerFullException, NewConnectionException;

    /**
     * Validates and logs in a user.
     *
     * @param user
     *            the user to log in
     * @throws LoginException
     *             for login failures
     */
    void login(KailleraUser user) throws LoginException;

    /**
     * Logs out a user from the server.
     *
     * @param user
     *            the user to quit
     * @param message
     *            the quit message
     * @throws QuitException
     *             if quit fails
     * @throws DropGameException
     *             if dropping from game fails
     * @throws QuitGameException
     *             if quitting game fails
     * @throws CloseGameException
     *             if closing game fails
     */
    void quit(KailleraUser user, String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException;

    /**
     * Finds a user by their ID.
     *
     * @param userId
     *            the user ID
     * @return Optional containing the user, or empty if not found
     */
    Optional<KailleraUser> findUser(int userId);

    /**
     * Gets all connected users.
     *
     * @return collection of users
     */
    Collection<? extends KailleraUser> getAllUsers();

    /**
     * Gets the number of currently connected users.
     *
     * @return the user count
     */
    int getUserCount();

    /**
     * Gets the maximum number of users allowed.
     *
     * @return the maximum user count
     */
    int getMaxUsers();
}
