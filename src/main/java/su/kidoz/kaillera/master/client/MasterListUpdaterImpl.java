package su.kidoz.kaillera.master.client;

import su.kidoz.config.MasterListConfig;
import su.kidoz.kaillera.controller.connectcontroller.ConnectController;
import su.kidoz.kaillera.master.PublicServerInformation;
import su.kidoz.kaillera.master.StatsCollector;
import su.kidoz.kaillera.model.KailleraServer;
import su.kidoz.release.ReleaseInfo;
import su.kidoz.util.EmuLinkerExecutor;
import su.kidoz.util.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

public class MasterListUpdaterImpl implements MasterListUpdater, Executable {
    private static final Logger log = LoggerFactory.getLogger(MasterListUpdaterImpl.class);

    private final EmuLinkerExecutor threadPool;
    private final ConnectController connectController;
    private final KailleraServer kailleraServer;
    private final StatsCollector statsCollector;
    private final ReleaseInfo releaseInfo;

    private final PublicServerInformation publicInfo;

    private final boolean touchKaillera;
    private final boolean touchEmulinker;

    private EmuLinkerMasterUpdateTask emulinkerMasterTask;
    private KailleraMasterUpdateTask kailleraMasterTask;

    private volatile boolean stopFlag = false;
    private volatile boolean isRunning = false;

    public MasterListUpdaterImpl(MasterListConfig config, EmuLinkerExecutor threadPool,
            ConnectController connectController, KailleraServer kailleraServer,
            StatsCollector statsCollector, ReleaseInfo releaseInfo, RestClient restClient) {
        this.threadPool = threadPool;
        this.connectController = connectController;
        this.kailleraServer = kailleraServer;
        this.statsCollector = statsCollector;
        this.releaseInfo = releaseInfo;

        this.touchKaillera = config.isTouchKaillera();
        this.touchEmulinker = config.isTouchEmulinker();

        if (touchKaillera || touchEmulinker) {
            publicInfo = new PublicServerInformation(config);
        } else {
            publicInfo = null;
        }

        if (touchKaillera)
            kailleraMasterTask = new KailleraMasterUpdateTask(publicInfo, connectController,
                    kailleraServer, statsCollector, restClient);

        if (touchEmulinker)
            emulinkerMasterTask = new EmuLinkerMasterUpdateTask(publicInfo, connectController,
                    kailleraServer, releaseInfo, restClient, config.getEmulinkerMasterUrl());
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized String toString() {
        return "MasterListUpdaterImpl[touchKaillera=" + touchKaillera + " touchEmulinker="
                + touchEmulinker + "]";
    }

    public synchronized void start() {
        if (publicInfo != null) {
            log.debug("MasterListUpdater thread received start request!");
            log.debug("MasterListUpdater thread starting (ThreadPool:" + threadPool.getActiveCount()
                    + "/" + threadPool.getPoolSize() + ")");
            threadPool.execute(this);
            log.debug("MasterListUpdater thread started (ThreadPool:" + threadPool.getActiveCount()
                    + "/" + threadPool.getPoolSize() + ")");
        }
    }

    public synchronized void stop() {
        if (publicInfo != null) {
            log.debug("MasterListUpdater thread received stop request!");

            if (!isRunning()) {
                log.debug("MasterListUpdater thread stop request ignored: not running!");
                return;
            }

            stopFlag = true;
        }
    }

    public void run() {
        isRunning = true;
        log.debug("MasterListUpdater thread running...");

        try {
            while (!stopFlag) {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (stopFlag)
                    break;

                log.info("MasterListUpdater touching masters...");
                statsCollector.getStartedGamesList(); // Fetch list (currently unused, for future
                                                      // use)

                if (emulinkerMasterTask != null)
                    emulinkerMasterTask.touchMaster();

                if (kailleraMasterTask != null)
                    kailleraMasterTask.touchMaster();

                statsCollector.clearStartedGamesList();
            }
        } finally {
            isRunning = false;
            log.debug("MasterListUpdater thread exiting...");
        }
    }
}
