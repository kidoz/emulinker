package org.emulinker.kaillera.master.client;

/**
 * Interface for master list updater components that periodically update master
 * server lists.
 */
public interface MasterListUpdater {

    /**
     * Starts the master list updater.
     */
    void start();

    /**
     * Stops the master list updater.
     */
    void stop();
}
