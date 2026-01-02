package org.emulinker.kaillera.admin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.emulinker.kaillera.admin.dto.ActionResultDTO;
import org.emulinker.kaillera.admin.dto.ControllerDTO;
import org.emulinker.kaillera.admin.dto.GameDTO;
import org.emulinker.kaillera.admin.dto.KickUserRequest;
import org.emulinker.kaillera.admin.dto.ServerInfoDTO;
import org.emulinker.kaillera.admin.dto.UserDTO;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.util.EmuLinkerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
public class AdminRestController {

    private static final Logger log = LoggerFactory.getLogger(AdminRestController.class);

    private final KailleraServer kailleraServer;
    private final ConnectController connectController;
    private final EmuLinkerExecutor executor;

    @Autowired
    public AdminRestController(KailleraServer kailleraServer, ConnectController connectController,
            EmuLinkerExecutor executor) {
        this.kailleraServer = kailleraServer;
        this.connectController = connectController;
        this.executor = executor;
    }

    @GetMapping("/server-info")
    public ServerInfoDTO getServerInfo() {
        ServerInfoDTO.StatsDTO stats = new ServerInfoDTO.StatsDTO(
                connectController.getRequestCount(), connectController.getConnectCount(),
                connectController.getProtocolErrorCount(),
                connectController.getDeniedServerFullCount(),
                connectController.getDeniedOtherCount());

        ServerInfoDTO.ThreadPoolDTO threadPool = new ServerInfoDTO.ThreadPoolDTO(
                executor.getActiveCount(), executor.getPoolSize(), executor.getMaximumPoolSize(),
                executor.getTaskCount());

        return new ServerInfoDTO(kailleraServer.getReleaseInfo().getProductName(),
                kailleraServer.getReleaseInfo().getVersionString(),
                kailleraServer.getReleaseInfo().getBuildNumber(), true,
                connectController.getBindPort(),
                (System.currentTimeMillis() - connectController.getStartTime()) / 60000,
                kailleraServer.getNumUsers(), kailleraServer.getMaxUsers(),
                kailleraServer.getNumGames(), kailleraServer.getMaxGames(), stats, threadPool);
    }

    @GetMapping("/users")
    public List<UserDTO> getUsers() {
        return kailleraServer.getUsers().stream().map(user -> {
            // Null-safe socket address handling for users that haven't fully connected
            String address = "unknown";
            var socketAddr = user.getSocketAddress();
            if (socketAddr != null && socketAddr.getAddress() != null) {
                address = socketAddr.getAddress().getHostAddress() + ":" + socketAddr.getPort();
            }
            return new UserDTO(user.getID(), user.getName(),
                    KailleraUser.STATUS_NAMES[user.getStatus()],
                    KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()], user.getPing(),
                    address, user.getConnectTime());
        }).collect(Collectors.toList());
    }

    @GetMapping("/games")
    public List<GameDTO> getGames() {
        return kailleraServer.getGames().stream()
                .map(game -> new GameDTO(game.getID(), game.getRomName(), game.getOwner().getName(),
                        KailleraGame.STATUS_NAMES[game.getStatus()], game.getNumPlayers()))
                .collect(Collectors.toList());
    }

    @GetMapping("/controllers")
    public List<ControllerDTO> getControllers() {
        return connectController.getControllers().stream()
                .map(controller -> new ControllerDTO(controller.getVersion(),
                        controller.getBufferSize(), controller.getNumClients(),
                        Arrays.asList(controller.getClientTypes())))
                .collect(Collectors.toList());
    }

    /**
     * Kicks a user from the server.
     *
     * @param userId
     *            the ID of the user to kick
     * @param request
     *            the kick request containing the reason
     * @return result indicating success or failure
     */
    @PostMapping("/users/{userId}/kick")
    public ResponseEntity<ActionResultDTO> kickUser(@PathVariable int userId,
            @RequestBody KickUserRequest request) {
        KailleraUser user = kailleraServer.getUser(userId);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String reason = request.reason();
        if (reason == null || reason.isBlank()) {
            reason = "Kicked by administrator";
        }

        try {
            user.quit("Kicked: " + reason);
            log.info("Admin kicked user {} (ID: {}): {}", user.getName(), userId, reason);
            return ResponseEntity
                    .ok(ActionResultDTO.ok("User " + user.getName() + " has been kicked"));
        } catch (Exception e) {
            log.error("Failed to kick user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ActionResultDTO.error("Failed to kick user: " + e.getMessage()));
        }
    }
}
