package su.kidoz.kaillera.model.impl;

public class PlayerActionQueue {
    private final int gameBufferSize;
    private final int gameTimeoutMillis;

    private final int thisPlayerNumber;
    private final KailleraUserImpl thisPlayer;
    private volatile boolean synched = false;
    private volatile PlayerTimeoutException lastTimeout;

    private final byte[] array;
    private final int[] heads;
    private int tail = 0;

    public PlayerActionQueue(int playerNumber, KailleraUserImpl player, int numPlayers,
            int gameBufferSize, int gameTimeoutMillis, boolean capture) {
        this.thisPlayerNumber = playerNumber;
        this.thisPlayer = player;
        this.gameBufferSize = gameBufferSize;
        this.gameTimeoutMillis = gameTimeoutMillis;

        array = new byte[gameBufferSize];
        heads = new int[numPlayers];
    }

    public int getPlayerNumber() {
        return thisPlayerNumber;
    }

    public KailleraUserImpl getPlayer() {
        return thisPlayer;
    }

    public synchronized void setSynched(boolean synched) {
        this.synched = synched;

        if (!synched) {
            notifyAll();
        }
    }

    public boolean isSynched() {
        return synched;
    }

    public void setLastTimeout(PlayerTimeoutException e) {
        this.lastTimeout = e;
    }

    public PlayerTimeoutException getLastTimeout() {
        return lastTimeout;
    }

    public synchronized void addActions(byte[] actions) {
        if (!synched)
            return;

        for (int i = 0; i < actions.length; i++) {
            array[tail] = actions[i];
            tail = ((tail + 1) % gameBufferSize);
        }

        notifyAll();
        lastTimeout = null;
    }

    public synchronized void getAction(int playerNumber, byte[] actions, int location,
            int actionLength) throws PlayerTimeoutException {
        // Validate array bounds to prevent ArrayIndexOutOfBoundsException
        if (playerNumber < 1 || playerNumber > heads.length) {
            throw new IllegalArgumentException(
                    "Invalid playerNumber: " + playerNumber + ", max: " + heads.length);
        }
        if (location < 0 || actionLength < 0 || location + actionLength > actions.length) {
            throw new IllegalArgumentException("Invalid array bounds: location=" + location
                    + ", actionLength=" + actionLength + ", array.length=" + actions.length);
        }

        // Use while loop to handle spurious wakeups
        long deadline = System.currentTimeMillis() + gameTimeoutMillis;
        while (getSize(playerNumber) < actionLength && synched) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            try {
                wait(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (getSize(playerNumber) >= actionLength) {
            int headIndex = playerNumber - 1;
            for (int i = 0; i < actionLength; i++) {
                actions[(location + i)] = array[heads[headIndex]];
                heads[headIndex] = ((heads[headIndex] + 1) % gameBufferSize);
            }
            return;
        }

        if (!synched)
            return;

        throw new PlayerTimeoutException(thisPlayerNumber, thisPlayer);
    }

    private synchronized int getSize(int playerNumber) {
        return (tail + gameBufferSize - heads[playerNumber - 1]) % gameBufferSize;
    }
}
