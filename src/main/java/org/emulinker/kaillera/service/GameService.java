package org.emulinker.kaillera.service;

import java.util.Collection;
import java.util.Optional;

import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.CreateGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.exception.JoinGameException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.emulinker.kaillera.model.exception.StartGameException;

/**
 * Service layer for game lifecycle management.
 *
 * <p>
 * Handles game creation, joining, starting, and cleanup.
 */
public interface GameService {

    /**
     * Creates a new game hosted by the given user.
     *
     * @param user
     *            the user creating the game
     * @param romName
     *            the ROM name for the game
     * @return the created game
     * @throws CreateGameException
     *             if game creation fails
     * @throws FloodException
     *             if the user is creating games too fast
     */
    KailleraGame createGame(KailleraUser user, String romName)
            throws CreateGameException, FloodException;

    /**
     * Joins a user to an existing game.
     *
     * @param user
     *            the user joining
     * @param gameId
     *            the ID of the game to join
     * @return the joined game
     * @throws JoinGameException
     *             if join fails
     */
    KailleraGame joinGame(KailleraUser user, int gameId) throws JoinGameException;

    /**
     * Starts the game.
     *
     * @param user
     *            the user requesting to start (must be owner)
     * @throws StartGameException
     *             if the game cannot be started
     */
    void startGame(KailleraUser user) throws StartGameException;

    /**
     * Quits the user from their current game.
     *
     * @param user
     *            the user quitting
     * @throws DropGameException
     *             if drop fails
     * @throws QuitGameException
     *             if quit fails
     * @throws CloseGameException
     *             if game close fails
     */
    void quitGame(KailleraUser user)
            throws DropGameException, QuitGameException, CloseGameException;

    /**
     * Drops the user from their current game.
     *
     * @param user
     *            the user dropping
     * @throws DropGameException
     *             if drop fails
     */
    void dropGame(KailleraUser user) throws DropGameException;

    /**
     * Finds a game by its ID.
     *
     * @param gameId
     *            the game ID
     * @return the game, or empty if not found
     */
    Optional<KailleraGame> findGame(int gameId);

    /**
     * Returns all active games.
     *
     * @return collection of all games
     */
    Collection<? extends KailleraGame> getAllGames();

    /**
     * Returns the current number of active games.
     *
     * @return the game count
     */
    int getGameCount();

    /**
     * Returns the maximum allowed games.
     *
     * @return the max game count
     */
    int getMaxGames();
}
