package su.kidoz.kaillera.service.impl;

import su.kidoz.kaillera.model.KailleraGame;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.exception.GameDataException;
import su.kidoz.kaillera.model.exception.GameKickException;
import su.kidoz.kaillera.model.exception.UserReadyException;
import su.kidoz.kaillera.model.impl.AutoFireDetector;
import su.kidoz.kaillera.service.GameInputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of GameInputService.
 *
 * <p>
 * Delegates operations to the underlying KailleraUser and KailleraGame models.
 */
public class GameInputServiceImpl implements GameInputService {

    private static final Logger log = LoggerFactory.getLogger(GameInputServiceImpl.class);

    public GameInputServiceImpl() {
    }

    @Override
    public void submitFrameData(KailleraUser user, byte[] data) throws GameDataException {
        user.addGameData(data);
    }

    @Override
    public void markPlayerReady(KailleraUser user) throws UserReadyException {
        user.playerReady();
    }

    @Override
    public void kickPlayer(KailleraUser requester, int targetUserId) throws GameKickException {
        requester.gameKick(targetUserId);
    }

    @Override
    public void reportDroppedPacket(KailleraUser user) {
        user.droppedPacket();
    }

    @Override
    public boolean configureAutofire(KailleraUser user, int autofireValue) {
        KailleraGame game = getGameForUser(user);
        if (game == null) {
            return false;
        }

        if (!game.getOwner().equals(user)) {
            log.warn("User {} tried to configure autofire but is not game owner", user.getName());
            return false;
        }

        AutoFireDetector detector = game.getAutoFireDetector();
        if (detector != null) {
            detector.setSensitivity(autofireValue);
            return true;
        }

        return false;
    }

    private KailleraGame getGameForUser(KailleraUser user) {
        // The user's current game is accessible through the server's game lookup.
        // The user object itself doesn't expose the game directly in the interface.
        for (KailleraGame game : user.getServer().getGames()) {
            if (game.getPlayers().contains(user)) {
                return game;
            }
        }
        return null;
    }
}
