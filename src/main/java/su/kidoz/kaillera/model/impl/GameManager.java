package su.kidoz.kaillera.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import su.kidoz.kaillera.model.KailleraGame;

/**
 * Manages game storage, ID generation, and game lifecycle tracking. Extracted
 * from KailleraServerImpl to improve separation of concerns.
 */
public class GameManager {
    private static final int MAX_GAME_ID = 0xFFFF;

    private final Map<Integer, KailleraGameImpl> games;
    private final AtomicInteger gameCounter;

    public GameManager(int initialCapacity) {
        this.games = new ConcurrentHashMap<>(initialCapacity);
        this.gameCounter = new AtomicInteger(1);
    }

    /**
     * Generates the next unique game ID, wrapping around at MAX_GAME_ID.
     */
    public int getNextGameID() {
        for (int attempts = 0; attempts < MAX_GAME_ID; attempts++) {
            int candidate = gameCounter.getAndUpdate(val -> val >= MAX_GAME_ID ? 1 : val + 1);
            if (!games.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No available game IDs");
    }

    /**
     * Adds a game to the managed collection.
     */
    public void addGame(KailleraGameImpl game) {
        games.put(game.getID(), game);
    }

    /**
     * Retrieves a game by ID.
     */
    public KailleraGameImpl getGame(int gameID) {
        return games.get(gameID);
    }

    /**
     * Removes a game by ID.
     *
     * @return the removed game, or null if not found
     */
    public KailleraGameImpl removeGame(int gameID) {
        return games.remove(gameID);
    }

    /**
     * Checks if a game exists.
     */
    public boolean containsGame(int gameID) {
        return games.containsKey(gameID);
    }

    /**
     * Returns an unmodifiable view of all games.
     */
    public Collection<KailleraGameImpl> getGames() {
        return Collections.unmodifiableCollection(games.values());
    }

    /**
     * Returns the number of games.
     */
    public int getNumGames() {
        return games.size();
    }

    /**
     * Returns the number of games currently in playing status.
     */
    public int getNumGamesPlaying() {
        int count = 0;
        for (KailleraGameImpl game : games.values()) {
            if (game.getStatus() != KailleraGame.STATUS_WAITING) {
                count++;
            }
        }
        return count;
    }

    /**
     * Clears all games.
     */
    public void clear() {
        games.clear();
    }
}
