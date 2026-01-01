package org.emulinker.kaillera.service;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;

import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.LoginException;
import org.emulinker.kaillera.model.exception.NewConnectionException;
import org.emulinker.kaillera.model.exception.QuitException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.emulinker.kaillera.model.exception.ServerFullException;

/**
 * Service layer for user management operations.
 *
 * <p>
 * Handles user lifecycle (connection, login, logout) and user lookups.
 */
public interface UserService {

    /**
     * Creates a new connection for an incoming client.
     *
     * @param clientSocketAddress
     *            the client's socket address
     * @param protocol
     *            the protocol version
     * @param listener
     *            the event listener for the user
     * @return the newly created user
     * @throws ServerFullException
     *             if the server is at capacity
     * @throws NewConnectionException
     *             if connection cannot be established
     */
    KailleraUser newConnection(InetSocketAddress clientSocketAddress, String protocol,
            KailleraEventListener listener) throws ServerFullException, NewConnectionException;

    /**
     * Logs in a user after connection and speed test.
     *
     * @param user
     *            the user to log in
     * @throws LoginException
     *             if login fails
     */
    void login(KailleraUser user) throws LoginException;

    /**
     * Logs out a user and cleans up their resources.
     *
     * @param user
     *            the user to log out
     * @param message
     *            the quit message
     * @throws QuitException
     *             if quit operation fails
     * @throws DropGameException
     *             if dropping the game fails
     * @throws QuitGameException
     *             if quitting the game fails
     * @throws CloseGameException
     *             if closing the game fails
     */
    void quit(KailleraUser user, String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException;

    /**
     * Finds a user by their ID.
     *
     * @param userId
     *            the user ID
     * @return the user, or empty if not found
     */
    Optional<KailleraUser> findUser(int userId);

    /**
     * Returns all connected users.
     *
     * @return collection of all users
     */
    Collection<? extends KailleraUser> getAllUsers();

    /**
     * Returns the current number of connected users.
     *
     * @return the user count
     */
    int getUserCount();

    /**
     * Returns the maximum allowed users.
     *
     * @return the max user count
     */
    int getMaxUsers();
}
