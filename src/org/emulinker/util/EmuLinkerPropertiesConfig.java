package org.emulinker.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.stereotype.Component;

@Component
public class EmuLinkerPropertiesConfig extends PropertiesConfiguration
{
	//private static Log	log	= LogFactory.getLog(EmuLinkerPropertiesConfig.class);

	public EmuLinkerPropertiesConfig() throws ConfigurationException
	{
		super(EmuLinkerPropertiesConfig.class.getResource("/emulinker.cfg"));
		setThrowExceptionOnMissing(true);
	}
}
