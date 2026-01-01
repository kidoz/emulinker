package org.emulinker.kaillera.model.impl;

import org.emulinker.kaillera.model.KailleraGame;

public interface AutoFireDetectorFactory {
    AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity);
}
