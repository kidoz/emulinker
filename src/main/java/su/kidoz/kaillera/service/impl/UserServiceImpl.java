package su.kidoz.kaillera.service.impl;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;

import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.kaillera.model.KailleraUser;
import su.kidoz.kaillera.model.event.KailleraEventListener;
import su.kidoz.kaillera.model.exception.CloseGameException;
import su.kidoz.kaillera.model.exception.DropGameException;
import su.kidoz.kaillera.model.exception.LoginException;
import su.kidoz.kaillera.model.exception.NewConnectionException;
import su.kidoz.kaillera.model.exception.QuitException;
import su.kidoz.kaillera.model.exception.QuitGameException;
import su.kidoz.kaillera.model.exception.ServerFullException;
import su.kidoz.kaillera.service.UserService;

/**
 * Default implementation of UserService.
 *
 * <p>
 * Delegates operations to the underlying KailleraServer model.
 */
public class UserServiceImpl implements UserService {

    private final KailleraServer server;

    public UserServiceImpl(final KailleraServer server) {
        this.server = server;
    }

    @Override
    public KailleraUser newConnection(final InetSocketAddress clientSocketAddress,
            final String protocol, final KailleraEventListener listener)
            throws ServerFullException, NewConnectionException {
        return server.newConnection(clientSocketAddress, protocol, listener);
    }

    @Override
    public void login(final KailleraUser user) throws LoginException {
        try {
            server.login(user);
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
    }

    @Override
    public void quit(final KailleraUser user, final String message)
            throws QuitException, DropGameException, QuitGameException, CloseGameException {
        server.quit(user, message);
    }

    @Override
    public Optional<KailleraUser> findUser(final int userId) {
        return Optional.ofNullable(server.getUser(userId));
    }

    @Override
    public Collection<? extends KailleraUser> getAllUsers() {
        return server.getUsers();
    }

    @Override
    public int getUserCount() {
        return server.getNumUsers();
    }

    @Override
    public int getMaxUsers() {
        return server.getMaxUsers();
    }
}
