package org.emulinker.util;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

public class EmuLinkerXMLConfig extends XMLConfiguration {
    public EmuLinkerXMLConfig() throws ConfigurationException {
        super();
        FileHandler handler = new FileHandler(this);
        handler.load(EmuLinkerXMLConfig.class.getResource("/emulinker.xml"));
        setThrowExceptionOnMissing(true);
    }
}
