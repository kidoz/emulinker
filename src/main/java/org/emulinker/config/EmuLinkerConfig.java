package org.emulinker.config;

import org.emulinker.kaillera.access.AccessManager2;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.master.MasterListStatsCollector;
import org.emulinker.kaillera.master.client.MasterListUpdaterImpl;
import org.emulinker.kaillera.model.impl.AutoFireDetectorFactoryImpl;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.release.KailleraServerReleaseInfo;
import org.emulinker.util.EmuLinkerExecutor;
import org.emulinker.util.EmuLinkerPropertiesConfig;
// package org.emulinker.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmuLinkerConfig {

    @Bean
    public EmuLinkerPropertiesConfig emuLinkerPropertiesConfig() throws Exception {
        return new EmuLinkerPropertiesConfig();
    }

    @Bean
    public EmuLinkerExecutor emuLinkerExecutor(EmuLinkerPropertiesConfig config) {
        return new EmuLinkerExecutor(config);
    }

    @Bean
    public KailleraServerReleaseInfo kailleraServerReleaseInfo() {
        return new KailleraServerReleaseInfo();
    }

    @Bean
    public AccessManager2 accessManager2(EmuLinkerExecutor executor, EmuLinkerPropertiesConfig config) throws Exception {
        return new AccessManager2(executor);
    }

    @Bean
    public ConnectController connectController(EmuLinkerExecutor executor, V086Controller v086Controller, AccessManager2 accessManager, EmuLinkerPropertiesConfig config) throws Exception {
        KailleraServerController[] controllers = new KailleraServerController[] { v086Controller };
        return new ConnectController(executor, controllers, accessManager, config);
    }

    @Bean
    public V086Controller v086Controller(KailleraServerImpl kailleraServer, EmuLinkerExecutor executor, AccessManager2 accessManager, EmuLinkerPropertiesConfig config) throws Exception {
        return new V086Controller(kailleraServer, executor, accessManager, config);
    }

    @Bean
    public KailleraServerImpl kailleraServerImpl(EmuLinkerExecutor executor, AccessManager2 accessManager, EmuLinkerPropertiesConfig config, MasterListStatsCollector statsCollector, KailleraServerReleaseInfo releaseInfo, AutoFireDetectorFactoryImpl autoFireFactory) throws Exception {
        return new KailleraServerImpl(executor, accessManager, config, statsCollector, releaseInfo, autoFireFactory);
    }

    @Bean
    public AutoFireDetectorFactoryImpl autoFireDetectorFactoryImpl() {
        return new AutoFireDetectorFactoryImpl();
    }

    @Bean
    public MasterListStatsCollector masterListStatsCollector() {
        return new MasterListStatsCollector();
    }

    @Bean
    public MasterListUpdaterImpl masterListUpdaterImpl(EmuLinkerPropertiesConfig config, EmuLinkerExecutor executor, ConnectController connectController, KailleraServerImpl kailleraServer, MasterListStatsCollector statsCollector, KailleraServerReleaseInfo releaseInfo) throws Exception {
        return new MasterListUpdaterImpl(config, executor, connectController, kailleraServer, statsCollector, releaseInfo);
    }

}
