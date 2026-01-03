package su.kidoz.kaillera.service.impl;

import java.util.Collection;
import java.util.Optional;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.exception.CloseGameException;
import su.kidoz.kaillera.model.exception.CreateGameException;
import su.kidoz.kaillera.model.exception.DropGameException;
import su.kidoz.kaillera.model.exception.FloodException;
import su.kidoz.kaillera.model.exception.JoinGameException;
import su.kidoz.kaillera.model.exception.QuitGameException;
import su.kidoz.kaillera.model.exception.StartGameException;
import su.kidoz.kaillera.service.GameService;

/**
 * Default implementation of GameService.
 *
 * <p>
 * Delegates operations to the underlying KailleraServer and KailleraUser
 * models.
 */
public class GameServiceImpl implements GameService {

    private final KailleraServer server;

    public GameServiceImpl(final KailleraServer server) {
        this.server = server;
    }

    @Override
    public KailleraGame createGame(final KailleraUser user, final String romName)
            throws CreateGameException, FloodException {
        return server.createGame(user, romName);
    }

    @Override
    public KailleraGame joinGame(final KailleraUser user, final int gameId)
            throws JoinGameException {
        return user.joinGame(gameId);
    }

    @Override
    public void startGame(final KailleraUser user) throws StartGameException {
        user.startGame();
    }

    @Override
    public void quitGame(final KailleraUser user)
            throws DropGameException, QuitGameException, CloseGameException {
        user.quitGame();
    }

    @Override
    public void dropGame(final KailleraUser user) throws DropGameException {
        user.dropGame();
    }

    @Override
    public Optional<KailleraGame> findGame(final int gameId) {
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
