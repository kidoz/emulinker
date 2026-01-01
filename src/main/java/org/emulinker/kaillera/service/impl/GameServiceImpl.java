package org.emulinker.kaillera.service.impl;

import java.util.Collection;
import java.util.Optional;

import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.exception.CloseGameException;
import org.emulinker.kaillera.model.exception.CreateGameException;
import org.emulinker.kaillera.model.exception.DropGameException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.exception.JoinGameException;
import org.emulinker.kaillera.model.exception.QuitGameException;
import org.emulinker.kaillera.model.exception.StartGameException;
import org.emulinker.kaillera.service.GameService;

/**
 * Default implementation of GameService.
 *
 * <p>
 * Delegates operations to the underlying KailleraServer and KailleraUser
 * models.
 */
public class GameServiceImpl implements GameService {

    private final KailleraServer server;

    public GameServiceImpl(KailleraServer server) {
        this.server = server;
    }

    @Override
    public KailleraGame createGame(KailleraUser user, String romName)
            throws CreateGameException, FloodException {
        return server.createGame(user, romName);
    }

    @Override
    public KailleraGame joinGame(KailleraUser user, int gameId) throws JoinGameException {
        return user.joinGame(gameId);
    }

    @Override
    public void startGame(KailleraUser user) throws StartGameException {
        user.startGame();
    }

    @Override
    public void quitGame(KailleraUser user)
            throws DropGameException, QuitGameException, CloseGameException {
        user.quitGame();
    }

    @Override
    public void dropGame(KailleraUser user) throws DropGameException {
        user.dropGame();
    }

    @Override
    public Optional<KailleraGame> findGame(int gameId) {
        return Optional.ofNullable(server.getGame(gameId));
    }

    @Override
    public Collection<? extends KailleraGame> getAllGames() {
        return server.getGames();
    }

    @Override
    public int getGameCount() {
        return server.getNumGames();
    }

    @Override
    public int getMaxGames() {
        return server.getMaxGames();
    }
}
