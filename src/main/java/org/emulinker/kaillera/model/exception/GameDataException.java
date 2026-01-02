package org.emulinker.kaillera.model.exception;

public class GameDataException extends ActionException {
    private byte[] response;

    public GameDataException(String message) {
        super(message);
    }

    public GameDataException(String message, byte[] data, int actionsPerMessage, int playerNumber,
            int numPlayers) {
        super(message);

        // Validate inputs to prevent overflow
        if (actionsPerMessage <= 0 || numPlayers <= 0 || data == null || data.length == 0) {
            return;
        }

        int bytesPerAction = (data.length / actionsPerMessage);
        if (bytesPerAction <= 0) {
            return;
        }

        // Check for integer overflow before allocation
        if (numPlayers > Integer.MAX_VALUE / actionsPerMessage
                || (numPlayers * actionsPerMessage) > Integer.MAX_VALUE / bytesPerAction) {
            return;
        }

        int arraySize = (numPlayers * actionsPerMessage * bytesPerAction);
        response = new byte[arraySize];
        for (int actionCounter = 0; actionCounter < actionsPerMessage; actionCounter++) {
            System.arraycopy(data, 0, response, ((actionCounter * (numPlayers * bytesPerAction))
                    + ((playerNumber - 1) * bytesPerAction)), bytesPerAction);
        }
    }

    public boolean hasResponse() {
        return (response != null);
    }

    public byte[] getResponse() {
        if (!hasResponse())
            return null;

        return response;
    }
}
