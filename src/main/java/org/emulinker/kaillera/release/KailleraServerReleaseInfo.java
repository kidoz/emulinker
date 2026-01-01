package org.emulinker.kaillera.release;

import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuUtil;

/**
 * Provides release and build information for the EmuLinker project.
 * This class also formats a welcome message for printing at server startup.
 */
public final class KailleraServerReleaseInfo implements ReleaseInfo
{
	private final String	productName		= "EmuLinker Kaillera Server BETA";

	private final int		majorVersion	= 1;
	private final int		minorVersion	= 0;
	private final int		buildNumber		= 2;

	private final String	releaseDate		= "05/22/2006";
	private final String	licenseInfo		= "Usage of this sofware is subject to the terms found in the included license";
	private final String	website			= "http://www.emulinker.org";

	public KailleraServerReleaseInfo()
	{
		//generic constructor
	}

	public String getProductName()
	{
		return productName;
	}

	public int getMajorVersion()
	{
		return majorVersion;
	}

	public int getMinorVersion()
	{
		return minorVersion;
	}

	public String getReleaseDate()
	{
		return releaseDate;
	}

	public String getLicenseInfo()
	{
		return licenseInfo;
	}

	public String getWebsiteString()
	{
		return website;
	}

	public int getBuildNumber()
	{
		// TODO: modify this to pull from an Ant build version file
		return buildNumber;
	}

	/**
	 * Returns the version number for the EmuLinker server in the form
	 * <p>
	 * <i>major</i>.<i>minor</i>
	 * </p>
	 */
	public String getVersionString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getMajorVersion());
		sb.append(".");
		sb.append(getMinorVersion());
		sb.append(".");
		sb.append(getBuildNumber());
		return sb.toString();
	}

	/**
	 * Formats release information into a welcome message.  This message
	 * is printed by the server at server startup.
	 */
	public String getWelcome()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("// " + getProductName() + " version " + getVersionString() + " (" + getReleaseDate() + ") ");
		sb.append(EmuUtil.LB);
		sb.append("// " + getLicenseInfo());
		sb.append(EmuUtil.LB);
		sb.append("// For the most up-to-date information please visit: " + getWebsiteString());
		return sb.toString();
	}
}
