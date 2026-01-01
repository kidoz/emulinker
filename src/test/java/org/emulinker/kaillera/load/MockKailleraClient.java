package org.emulinker.kaillera.load;

import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.KailleraEvent;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.event.UserJoinedEvent;
import org.emulinker.kaillera.model.event.UserJoinedGameEvent;

/**
 * A simulated Kaillera client for load testing.
 *
 * <p>
 * This client can connect to a server, perform operations, and track events
 * received.
 */
public class MockKailleraClient implements KailleraEventListener {

    private final String name;
    private final int clientId;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicInteger eventsReceived = new AtomicInteger(0);
    private final CopyOnWriteArrayList<KailleraEvent> events = new CopyOnWriteArrayList<>();

    private KailleraUser user;
    private KailleraGame currentGame;
    private CountDownLatch loginLatch;
    private CountDownLatch gameLatch;

    public MockKailleraClient(String name, int clientId) {
        this.name = name;
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public int getClientId() {
        return clientId;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    public int getEventsReceived() {
        return eventsReceived.get();
    }

    public KailleraUser getUser() {
        return user;
    }

    public KailleraGame getCurrentGame() {
        return currentGame;
    }

    /**
     * Connects to the server and waits for login completion.
     */
    public boolean connect(KailleraServer server, long timeoutMs) throws Exception {
        loginLatch = new CountDownLatch(1);

        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 27888 + (clientId % 1000));
        user = server.newConnection(address, "v086", this);
        connected.set(true);

        user.setName(name);
        user.setClientType("LoadTestClient");
        user.setConnectionType(KailleraUser.CONNECTION_TYPE_LAN);
        user.setPing(10);
        user.login();

        return loginLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a game and returns it.
     */
    public KailleraGame createGame(String romName) throws Exception {
        if (!loggedIn.get()) {
            throw new IllegalStateException("Not logged in");
        }
        currentGame = user.createGame(romName);
        return currentGame;
    }

    /**
     * Joins an existing game.
     */
    public KailleraGame joinGame(int gameId, long timeoutMs) throws Exception {
        if (!loggedIn.get()) {
            throw new IllegalStateException("Not logged in");
        }
        gameLatch = new CountDownLatch(1);
        currentGame = user.joinGame(gameId);
        gameLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        return currentGame;
    }

    /**
     * Sends a chat message.
     */
    public void chat(String message) throws Exception {
        if (!loggedIn.get()) {
            throw new IllegalStateException("Not logged in");
        }
        user.chat(message);
    }

    /**
     * Quits from the server.
     */
    public void quit() throws Exception {
        if (connected.get()) {
            user.quit("Load test complete");
            connected.set(false);
            loggedIn.set(false);
        }
    }

    // KailleraEventListener implementation

    @Override
    public void actionPerformed(KailleraEvent event) {
        eventsReceived.incrementAndGet();
        events.add(event);

        if (event instanceof UserJoinedEvent joinedEvent) {
            if (joinedEvent.getUser().equals(user)) {
                loggedIn.set(true);
                if (loginLatch != null) {
                    loginLatch.countDown();
                }
            }
        } else if (event instanceof UserJoinedGameEvent) {
            if (gameLatch != null) {
                gameLatch.countDown();
            }
        }
    }

    @Override
    public void stop() {
        connected.set(false);
        loggedIn.set(false);
    }
}
