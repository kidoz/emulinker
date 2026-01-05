package su.kidoz.config;

import su.kidoz.kaillera.access.AccessManager;
import su.kidoz.release.ReleaseInfo;
import su.kidoz.util.EmuLinkerExecutor;

/**
 * Groups low-level infrastructure dependencies for the Kaillera server.
 *
 * <p>
 * This record bundles system-level components that are typically initialized
 * once at application startup and have application-wide scope.
 *
 * @param threadPool
 *            executor service for virtual threads
 * @param accessManager
 *            handles user access control and banning
 * @param releaseInfo
 *            server version and release information
 */
public record ServerInfrastructure(EmuLinkerExecutor threadPool, AccessManager accessManager,
        ReleaseInfo releaseInfo) {
}
