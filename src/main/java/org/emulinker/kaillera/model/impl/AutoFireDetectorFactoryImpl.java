package org.emulinker.kaillera.model.impl;

import org.emulinker.kaillera.model.*;

public class AutoFireDetectorFactoryImpl implements AutoFireDetectorFactory {
    public AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity) {
        return new AutoFireScanner(game, defaultSensitivity);
    }
}
