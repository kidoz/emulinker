package org.emulinker.kaillera.model.impl;

import org.emulinker.kaillera.model.*;

public interface AutoFireDetectorFactory
{
	AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity);
}
