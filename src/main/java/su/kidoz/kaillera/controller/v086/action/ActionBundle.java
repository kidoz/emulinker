package su.kidoz.kaillera.controller.v086.action;

/**
 * Bundle of all V086 actions to simplify dependency injection. This avoids
 * having 22+ constructor parameters in V086Controller.
 */
public record ActionBundle(ACKAction ackAction, AdminCommandAction adminCommandAction,
        CachedGameDataAction cachedGameDataAction, ChatAction chatAction,
        CloseGameAction closeGameAction, CreateGameAction createGameAction,
        DropGameAction dropGameAction, GameChatAction gameChatAction, GameDataAction gameDataAction,
        GameDesynchAction gameDesynchAction, GameInfoAction gameInfoAction,
        GameKickAction gameKickAction, GameOwnerCommandAction gameOwnerCommandAction,
        GameStatusAction gameStatusAction, GameTimeoutAction gameTimeoutAction,
        InfoMessageAction infoMessageAction, JoinGameAction joinGameAction,
        KeepAliveAction keepAliveAction, LoginAction loginAction,
        LoginProgressAction loginProgressAction, PlayerDesynchAction playerDesynchAction,
        QuitAction quitAction, QuitGameAction quitGameAction, StartGameAction startGameAction,
        UserReadyAction userReadyAction) {
}
