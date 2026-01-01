package org.emulinker.kaillera.model.impl;

import org.emulinker.kaillera.model.KailleraUser;

public interface AutoFireDetector {
    void start(int numPlayers);
    void addPlayer(KailleraUser user, int playerNumber);
    void addData(int playerNumber, byte[] data, int bytesPerAction);
    void stop(int playerNumber);
    void stop();

    void setSensitivity(int sensivitiy);
    int getSensitivity();
}
