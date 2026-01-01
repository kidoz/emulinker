package org.emulinker.kaillera.service.impl;

import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.exception.ChatException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.exception.GameChatException;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.service.CommunicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of CommunicationService.
 *
 * <p>
 * Delegates operations to the underlying KailleraServer and KailleraUser
 * models.
 */
public class CommunicationServiceImpl implements CommunicationService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationServiceImpl.class);

    private final KailleraServer server;

    public CommunicationServiceImpl(final KailleraServer server) {
        this.server = server;
    }

    @Override
    public void serverChat(final KailleraUser user, final String message)
            throws ChatException, FloodException {
        server.chat(user, message);
    }

    @Override
    public void gameChat(final KailleraUser user, final String message, final int messageId)
            throws GameChatException {
        user.gameChat(message, messageId);
    }

    @Override
    public void announce(final String message, final boolean includeGames) {
        if (server instanceof KailleraServerImpl serverImpl) {
            serverImpl.announce(message, includeGames);
        } else {
            log.warn("Announce not supported for server type: {}", server.getClass().getName());
        }
    }

    @Override
    public boolean privateMessage(final KailleraUser from, final int toUserId,
            final String message) {
        final KailleraUser target = server.getUser(toUserId);
        if (target == null) {
            return false;
        }

        // Private messages are implemented via the event system in the existing code.
        // For now, we just verify the user exists.
        log.info("Private message from {} to {}: {}", from.getName(), target.getName(), message);
        return true;
    }
}
