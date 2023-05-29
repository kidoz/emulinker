package org.emulinker.kaillera.model.impl;

import org.emulinker.kaillera.model.KailleraGame;
import org.springframework.stereotype.Component;

@Component
public class AutoFireDetectorFactoryImpl implements AutoFireDetectorFactory
{
	public AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity)
	{
		return new AutoFireScanner2(game, defaultSensitivity);
	}
}
