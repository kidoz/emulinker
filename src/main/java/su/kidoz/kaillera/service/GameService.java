package su.kidoz.kaillera.service;

import java.util.Collection;
import java.util.Optional;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.exception.CloseGameException;
import su.kidoz.kaillera.model.exception.CreateGameException;
import su.kidoz.kaillera.model.exception.DropGameException;
import su.kidoz.kaillera.model.exception.FloodException;
import su.kidoz.kaillera.model.exception.JoinGameException;
import su.kidoz.kaillera.model.exception.QuitGameException;
import su.kidoz.kaillera.model.exception.StartGameException;

/**
 * Service interface for game lifecycle management. Separates game operations
 * from the main server implementation for better testability and separation of
 * concerns.
 */
public interface GameService {

    /**
     * Creates a new game.
     *
     * @param user
     *            the user creating the game
     * @param romName
     *            the ROM/game name
     * @return the newly created game
     * @throws CreateGameException
     *             if the game cannot be created
     * @throws FloodException
     *             if the user is creating games too frequently
     */
    KailleraGame createGame(KailleraUser user, String romName)
            throws CreateGameException, FloodException;

    /**
     * Joins a user to an existing game.
     *
     * @param user
     *            the user joining
     * @param gameId
     *            the game ID to join
     * @return the joined game
     * @throws JoinGameException
     *             if the user cannot join
     */
    KailleraGame joinGame(KailleraUser user, int gameId) throws JoinGameException;

    /**
     * Starts a game.
     *
     * @param user
     *            the user requesting the start
     * @throws StartGameException
     *             if the game cannot be started
     */
    void startGame(KailleraUser user) throws StartGameException;

    /**
     * Removes a user from a game (quit).
     *
     * @param user
     *            the user quitting
     * @throws QuitGameException
     *             if the quit fails
     * @throws CloseGameException
     *             if closing the game fails
     * @throws DropGameException
     *             if dropping fails
     */
    void quitGame(KailleraUser user)
            throws DropGameException, QuitGameException, CloseGameException;

    /**
     * Drops a user from a running game.
     *
     * @param user
     *            the user dropping
     * @throws DropGameException
     *             if the drop fails
     */
    void dropGame(KailleraUser user) throws DropGameException;

    /**
     * Finds a game by its ID.
     *
     * @param gameId
     *            the game ID
     * @return Optional containing the game, or empty if not found
     */
    Optional<KailleraGame> findGame(int gameId);

    /**
     * Gets all active games.
     *
     * @return collection of active games
     */
    Collection<? extends KailleraGame> getAllGames();

    /**
     * Gets the number of active games.
     *
     * @return the game count
     */
    int getGameCount();

    /**
     * Gets the maximum number of games allowed.
     *
     * @return the maximum game count
     */
    int getMaxGames();
}
