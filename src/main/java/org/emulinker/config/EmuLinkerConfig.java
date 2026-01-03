package org.emulinker.config;

import java.time.Duration;
import org.emulinker.kaillera.access.FileBasedAccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.action.ACKAction;
import org.emulinker.kaillera.controller.v086.action.ActionBundle;
import org.emulinker.kaillera.controller.v086.action.ActionRouter;
import org.emulinker.kaillera.controller.v086.action.AdminCommandAction;
import org.emulinker.kaillera.controller.v086.action.CachedGameDataAction;
import org.emulinker.kaillera.controller.v086.action.ChatAction;
import org.emulinker.kaillera.controller.v086.action.CloseGameAction;
import org.emulinker.kaillera.controller.v086.action.CreateGameAction;
import org.emulinker.kaillera.controller.v086.action.DropGameAction;
import org.emulinker.kaillera.controller.v086.action.GameChatAction;
import org.emulinker.kaillera.controller.v086.action.GameDataAction;
import org.emulinker.kaillera.controller.v086.action.GameDesynchAction;
import org.emulinker.kaillera.controller.v086.action.GameInfoAction;
import org.emulinker.kaillera.controller.v086.action.GameKickAction;
import org.emulinker.kaillera.controller.v086.action.GameOwnerCommandAction;
import org.emulinker.kaillera.controller.v086.action.GameStatusAction;
import org.emulinker.kaillera.controller.v086.action.GameTimeoutAction;
import org.emulinker.kaillera.controller.v086.action.InfoMessageAction;
import org.emulinker.kaillera.controller.v086.action.JoinGameAction;
import org.emulinker.kaillera.controller.v086.action.KeepAliveAction;
import org.emulinker.kaillera.controller.v086.action.LoginAction;
import org.emulinker.kaillera.controller.v086.action.LoginProgressAction;
import org.emulinker.kaillera.controller.v086.action.PlayerDesynchAction;
import org.emulinker.kaillera.controller.v086.action.QuitAction;
import org.emulinker.kaillera.controller.v086.action.QuitGameAction;
import org.emulinker.kaillera.controller.v086.action.StartGameAction;
import org.emulinker.kaillera.controller.v086.action.UserReadyAction;
import org.emulinker.kaillera.master.MasterListStatsCollector;
import org.emulinker.kaillera.master.client.MasterListUpdaterImpl;
import org.emulinker.kaillera.model.impl.AutoFireDetectorFactoryImpl;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.release.KailleraServerReleaseInfo;

import su.kidoz.kaillera.model.impl.GameManager;
import su.kidoz.kaillera.model.impl.UserManager;
import su.kidoz.kaillera.model.validation.LoginValidator;
import su.kidoz.kaillera.service.AnnouncementService;
import su.kidoz.kaillera.service.ChatModerationService;
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
import org.springframework.boot.http.client.HttpClientSettings;
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
        HttpClientSettings settings = HttpClientSettings.defaults()
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
            EmuLinkerExecutor executor, ControllersConfig controllersConfig,
            ServerConfig serverConfig, ActionRouter actionRouter) throws Exception {
        return new V086Controller(kailleraServer, executor, controllersConfig, serverConfig,
                actionRouter);
    }

    @Bean
    public LoginValidator loginValidator(FileBasedAccessManager accessManager,
            ServerConfig serverConfig) {
        return new LoginValidator(accessManager, serverConfig);
    }

    @Bean
    public ChatModerationService chatModerationService(FileBasedAccessManager accessManager,
            ServerConfig serverConfig) {
        return new ChatModerationService(accessManager, serverConfig.getChatFloodTime(),
                serverConfig.getMaxChatLength());
    }

    @Bean
    public AnnouncementService announcementService() {
        return new AnnouncementService();
    }

    @Bean
    public UserManager userManager(ServerConfig serverConfig) {
        return new UserManager(serverConfig.getMaxUsers());
    }

    @Bean
    public GameManager gameManager(ServerConfig serverConfig) {
        return new GameManager(serverConfig.getMaxUsers());
    }

    @Bean
    public KailleraServerImpl kailleraServerImpl(EmuLinkerExecutor executor,
            FileBasedAccessManager accessManager, ServerConfig serverConfig, GameConfig gameConfig,
            MasterListConfig masterListConfig, MasterListStatsCollector statsCollector,
            KailleraServerReleaseInfo releaseInfo, AutoFireDetectorFactoryImpl autoFireFactory,
            LoginValidator loginValidator, ChatModerationService chatModerationService,
            AnnouncementService announcementService, UserManager userManager,
            GameManager gameManager) throws Exception {
        return new KailleraServerImpl(executor, accessManager, serverConfig, gameConfig,
                masterListConfig, statsCollector, releaseInfo, autoFireFactory, loginValidator,
                chatModerationService, announcementService, userManager, gameManager);
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

    // V086 Action beans

    @Bean
    public ACKAction ackAction() {
        return new ACKAction();
    }

    @Bean
    public AdminCommandAction adminCommandAction() {
        return new AdminCommandAction();
    }

    @Bean
    public CachedGameDataAction cachedGameDataAction() {
        return new CachedGameDataAction();
    }

    @Bean
    public ChatAction chatAction(AdminCommandAction adminCommandAction) {
        return new ChatAction(adminCommandAction);
    }

    @Bean
    public CloseGameAction closeGameAction() {
        return new CloseGameAction();
    }

    @Bean
    public CreateGameAction createGameAction() {
        return new CreateGameAction();
    }

    @Bean
    public DropGameAction dropGameAction() {
        return new DropGameAction();
    }

    @Bean
    public GameChatAction gameChatAction(GameOwnerCommandAction gameOwnerCommandAction) {
        return new GameChatAction(gameOwnerCommandAction);
    }

    @Bean
    public GameDataAction gameDataAction() {
        return new GameDataAction();
    }

    @Bean
    public GameDesynchAction gameDesynchAction() {
        return new GameDesynchAction();
    }

    @Bean
    public GameInfoAction gameInfoAction() {
        return new GameInfoAction();
    }

    @Bean
    public GameKickAction gameKickAction() {
        return new GameKickAction();
    }

    @Bean
    public GameOwnerCommandAction gameOwnerCommandAction() {
        return new GameOwnerCommandAction();
    }

    @Bean
    public GameStatusAction gameStatusAction() {
        return new GameStatusAction();
    }

    @Bean
    public GameTimeoutAction gameTimeoutAction() {
        return new GameTimeoutAction();
    }

    @Bean
    public InfoMessageAction infoMessageAction() {
        return new InfoMessageAction();
    }

    @Bean
    public JoinGameAction joinGameAction() {
        return new JoinGameAction();
    }

    @Bean
    public KeepAliveAction keepAliveAction() {
        return new KeepAliveAction();
    }

    @Bean
    public LoginAction loginAction() {
        return new LoginAction();
    }

    @Bean
    public LoginProgressAction loginProgressAction() {
        return new LoginProgressAction();
    }

    @Bean
    public PlayerDesynchAction playerDesynchAction() {
        return new PlayerDesynchAction();
    }

    @Bean
    public QuitAction quitAction() {
        return new QuitAction();
    }

    @Bean
    public QuitGameAction quitGameAction() {
        return new QuitGameAction();
    }

    @Bean
    public StartGameAction startGameAction() {
        return new StartGameAction();
    }

    @Bean
    public UserReadyAction userReadyAction() {
        return new UserReadyAction();
    }

    @Bean
    public ActionBundle actionBundle(ACKAction ackAction, AdminCommandAction adminCommandAction,
            CachedGameDataAction cachedGameDataAction, ChatAction chatAction,
            CloseGameAction closeGameAction, CreateGameAction createGameAction,
            DropGameAction dropGameAction, GameChatAction gameChatAction,
            GameDataAction gameDataAction, GameDesynchAction gameDesynchAction,
            GameInfoAction gameInfoAction, GameKickAction gameKickAction,
            GameOwnerCommandAction gameOwnerCommandAction, GameStatusAction gameStatusAction,
            GameTimeoutAction gameTimeoutAction, InfoMessageAction infoMessageAction,
            JoinGameAction joinGameAction, KeepAliveAction keepAliveAction, LoginAction loginAction,
            LoginProgressAction loginProgressAction, PlayerDesynchAction playerDesynchAction,
            QuitAction quitAction, QuitGameAction quitGameAction, StartGameAction startGameAction,
            UserReadyAction userReadyAction) {
        return new ActionBundle(ackAction, adminCommandAction, cachedGameDataAction, chatAction,
                closeGameAction, createGameAction, dropGameAction, gameChatAction, gameDataAction,
                gameDesynchAction, gameInfoAction, gameKickAction, gameOwnerCommandAction,
                gameStatusAction, gameTimeoutAction, infoMessageAction, joinGameAction,
                keepAliveAction, loginAction, loginProgressAction, playerDesynchAction, quitAction,
                quitGameAction, startGameAction, userReadyAction);
    }

    @Bean
    public ActionRouter actionRouter(ActionBundle actionBundle) {
        return new ActionRouter(actionBundle);
    }
}
