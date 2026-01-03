package su.kidoz.kaillera.model.impl;

import su.kidoz.kaillera.model.KailleraUser;

public interface AutoFireDetector {
    void start(int numPlayers);
    void addPlayer(KailleraUser user, int playerNumber);
    void addData(int playerNumber, byte[] data, int bytesPerAction);
    void stop(int playerNumber);
    void stop();

    void setSensitivity(int sensitivity);
    int getSensitivity();
}
