package org.emulinker.release;

/**
 * Provides release information about the project.
 *
 * ReleaseInfo is a top-level EmuLinker component; its implementation class is
 * loaded via Spring upon startup.
 *
 * @author Paul Cowan
 * @see www.emulinker.org
 */
public interface ReleaseInfo {
    /**
     * @return The name of this software.
     */
    String getProductName();

    /**
     * @return Major version number
     */
    int getMajorVersion();

    /**
     * @return Minor version number
     */
    int getMinorVersion();

    /**
     * @return Build number
     */
    int getBuildNumber();

    /**
     * @return The release date of this software
     */
    String getReleaseDate();

    /**
     * @return A string containing the full version information
     */
    String getVersionString();

    /**
     * @return License information
     */
    String getLicenseInfo();

    /**
     * @return A string containing software website iformation
     */
    String getWebsiteString();

    /**
     * @return A string containg a welcome message intended to be display on
     *         software startup
     */
    String getWelcome();
}
