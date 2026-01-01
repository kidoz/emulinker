package org.emulinker.util;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

public class EmuLinkerPropertiesConfig extends PropertiesConfiguration {
    public EmuLinkerPropertiesConfig() throws ConfigurationException {
        super();
        // Enable comma-separated list parsing (required in Commons Configuration 2.x)
        setListDelimiterHandler(new DefaultListDelimiterHandler(','));
        FileHandler handler = new FileHandler(this);
        handler.load(EmuLinkerPropertiesConfig.class.getResource("/application.properties"));
        setThrowExceptionOnMissing(true);
    }
}
