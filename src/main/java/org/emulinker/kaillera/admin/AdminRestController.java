package org.emulinker.kaillera.admin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadPoolExecutor;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.util.EmuLinkerExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    private final KailleraServer kailleraServer;
    private final ConnectController connectController;
    private final EmuLinkerExecutor executor;

    @Autowired
    public AdminRestController(KailleraServer kailleraServer, ConnectController connectController, EmuLinkerExecutor executor) {
        this.kailleraServer = kailleraServer;
        this.connectController = connectController;
        this.executor = executor;
    }

    @GetMapping("/server-info")
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serverName", kailleraServer.getReleaseInfo().getProductName());
        info.put("version", kailleraServer.getReleaseInfo().getVersionString());
        info.put("build", kailleraServer.getReleaseInfo().getBuildNumber());
        info.put("running", true);
        info.put("connectPort", connectController.getBindPort());
        info.put("uptimeMinutes", (System.currentTimeMillis() - connectController.getStartTime()) / 60000);
        info.put("userCount", kailleraServer.getNumUsers());
        info.put("maxUsers", kailleraServer.getMaxUsers());
        info.put("gameCount", kailleraServer.getNumGames());
        info.put("maxGames", kailleraServer.getMaxGames());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("requestCount", connectController.getRequestCount());
        stats.put("connectCount", connectController.getConnectCount());
        stats.put("protocolErrors", connectController.getProtocolErrorCount());
        stats.put("deniedFull", connectController.getDeniedServerFullCount());
        stats.put("deniedOther", connectController.getDeniedOtherCount());
        info.put("stats", stats);

        Map<String, Object> pool = new HashMap<>();
        pool.put("active", executor.getActiveCount());
        pool.put("poolSize", executor.getPoolSize());
        pool.put("maxPoolSize", executor.getMaximumPoolSize());
        pool.put("taskCount", executor.getTaskCount());
        info.put("threadPool", pool);

        return info;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return kailleraServer.getUsers().stream().map(user -> {
            Map<String, Object> u = new HashMap<>();
            u.put("id", user.getID());
            u.put("name", user.getName());
            u.put("status", KailleraUser.STATUS_NAMES[user.getStatus()]);
            u.put("connectionType", KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]);
            u.put("ping", user.getPing());
            u.put("address", user.getSocketAddress().getAddress().getHostAddress() + ":" + user.getSocketAddress().getPort());
            u.put("connectTime", user.getConnectTime());
            return u;
        }).collect(Collectors.toList());
    }

    @GetMapping("/games")
    public List<Map<String, Object>> getGames() {
        return kailleraServer.getGames().stream().map(game -> {
            Map<String, Object> g = new HashMap<>();
            g.put("id", game.getID());
            g.put("rom", game.getRomName());
            g.put("owner", game.getOwner().getName());
            g.put("status", KailleraGame.STATUS_NAMES[game.getStatus()]);
            g.put("players", game.getNumPlayers());
            return g;
        }).collect(Collectors.toList());
    }

    @GetMapping("/controllers")
    public List<Map<String, Object>> getControllers() {
        return connectController.getControllers().stream().map(controller -> {
            Map<String, Object> c = new HashMap<>();
            c.put("version", controller.getVersion());
            c.put("bufferSize", controller.getBufferSize());
            c.put("numClients", controller.getNumClients());
            c.put("clientTypes", controller.getClientTypes());
            return c;
        }).collect(Collectors.toList());
    }
}
