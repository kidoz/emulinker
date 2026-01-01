package org.emulinker.kaillera.model.impl;

public class PlayerActionQueue {
    private int gameBufferSize;
    private int gameTimeoutMillis;
    private boolean capture;

    private int thisPlayerNumber;
    private KailleraUserImpl thisPlayer;
    private volatile boolean synched = false;
    private volatile PlayerTimeoutException lastTimeout;

    private final byte[] array;
    private final int[] heads;
    private int tail = 0;

    // private OutputStream os;
    // private InputStream is;

    public PlayerActionQueue(int playerNumber, KailleraUserImpl player, int numPlayers,
            int gameBufferSize, int gameTimeoutMillis, boolean capture) {
        this.thisPlayerNumber = playerNumber;
        this.thisPlayer = player;
        this.gameBufferSize = gameBufferSize;
        this.gameTimeoutMillis = gameTimeoutMillis;
        this.capture = capture;

        array = new byte[gameBufferSize];
        heads = new int[numPlayers];
        /*
         * if(capture) { try { os = new BufferedOutputStream(new
         * FileOutputStream("test.cap")); } catch(Exception e) { e.printStackTrace(); }
         * }
         */
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

        if (getSize(playerNumber) < actionLength && synched) {
            try {
                wait(gameTimeoutMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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

    private int getSize(int playerNumber) {
        return (tail + gameBufferSize - heads[playerNumber - 1]) % gameBufferSize;
    }
}
