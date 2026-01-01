package org.emulinker.config;

import java.time.Duration;
import org.emulinker.kaillera.access.FileBasedAccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.master.MasterListStatsCollector;
import org.emulinker.kaillera.master.client.MasterListUpdaterImpl;
import org.emulinker.kaillera.model.impl.AutoFireDetectorFactoryImpl;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.release.KailleraServerReleaseInfo;
import org.emulinker.kaillera.service.CommunicationService;
import org.emulinker.kaillera.service.GameInputService;
import org.emulinker.kaillera.service.GameService;
import org.emulinker.kaillera.service.ServerAdminService;
import org.emulinker.kaillera.service.UserService;
import org.emulinker.kaillera.service.impl.CommunicationServiceImpl;
import org.emulinker.kaillera.service.impl.GameInputServiceImpl;
import org.emulinker.kaillera.service.impl.GameServiceImpl;
import org.emulinker.kaillera.service.impl.ServerAdminServiceImpl;
import org.emulinker.kaillera.service.impl.UserServiceImpl;
import org.emulinker.util.EmuLinkerExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ServerConfig.class, GameConfig.class, MasterListConfig.class,
        ControllersConfig.class})
public class EmuLinkerConfig {

    @Bean
    public EmuLinkerExecutor emuLinkerExecutor() {
        return new EmuLinkerExecutor();
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
                .build();
    }

    @Bean
    public KailleraServerReleaseInfo kailleraServerReleaseInfo() {
        return new KailleraServerReleaseInfo();
    }

    @Bean
    public FileBasedAccessManager fileBasedAccessManager(EmuLinkerExecutor executor)
            throws Exception {
        return new FileBasedAccessManager(executor);
    }

    @Bean
    public ConnectController connectController(EmuLinkerExecutor executor,
            V086Controller v086Controller, FileBasedAccessManager accessManager,
            ControllersConfig controllersConfig) throws Exception {
        KailleraServerController[] controllers = new KailleraServerController[]{v086Controller};
        return new ConnectController(executor, controllers, accessManager, controllersConfig);
    }

    @Bean
    public V086Controller v086Controller(KailleraServerImpl kailleraServer,
            EmuLinkerExecutor executor, FileBasedAccessManager accessManager,
            ControllersConfig controllersConfig, ServerConfig serverConfig) throws Exception {
        return new V086Controller(kailleraServer, executor, accessManager, controllersConfig,
                serverConfig);
    }

    @Bean
    public KailleraServerImpl kailleraServerImpl(EmuLinkerExecutor executor,
            FileBasedAccessManager accessManager, ServerConfig serverConfig, GameConfig gameConfig,
            MasterListConfig masterListConfig, MasterListStatsCollector statsCollector,
            KailleraServerReleaseInfo releaseInfo, AutoFireDetectorFactoryImpl autoFireFactory)
            throws Exception {
        return new KailleraServerImpl(executor, accessManager, serverConfig, gameConfig,
                masterListConfig, statsCollector, releaseInfo, autoFireFactory);
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
    public MasterListUpdaterImpl masterListUpdaterImpl(MasterListConfig masterListConfig,
            EmuLinkerExecutor executor, ConnectController connectController,
            KailleraServerImpl kailleraServer, MasterListStatsCollector statsCollector,
            KailleraServerReleaseInfo releaseInfo, RestClient restClient) throws Exception {
        return new MasterListUpdaterImpl(masterListConfig, executor, connectController,
                kailleraServer, statsCollector, releaseInfo, restClient);
    }

    // Service layer beans

    @Bean
    public UserService userService(KailleraServerImpl kailleraServer) {
        return new UserServiceImpl(kailleraServer);
    }

    @Bean
    public GameService gameService(KailleraServerImpl kailleraServer) {
        return new GameServiceImpl(kailleraServer);
    }

    @Bean
    public CommunicationService communicationService(KailleraServerImpl kailleraServer) {
        return new CommunicationServiceImpl(kailleraServer);
    }

    @Bean
    public GameInputService gameInputService() {
        return new GameInputServiceImpl();
    }

    @Bean
    public ServerAdminService serverAdminService(KailleraServerImpl kailleraServer,
            FileBasedAccessManager accessManager) {
        return new ServerAdminServiceImpl(kailleraServer, accessManager);
    }
}
