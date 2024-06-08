/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.configuration;

import net.pms.PMS;
import org.apache.commons.lang3.StringUtils;

// a one-stop class for values and methods specific to custom UMS builds
public class Build {

	/**
	 * This class should not be instantiated.
	 */
	private Build() {}

	/**
	 * Repository where to locate the file. Note: using "raw.github.com"
	 * to access the raw file.
	 */
	private static final String REPO = "https://raw.github.com/UniversalMediaServer/UniversalMediaServer";

	/**
	 * The URL of the properties file used by the {@link AutoUpdater} to announce UMS updates.
	 * Can be null/empty if not used. Not used if IS_UPDATABLE is set to false.
	 */
	private static final String UPDATE_SERVER_URL = REPO + "/main/src/main/external-resources/update/latest_version.properties";

	/**
	 * The url of the releases page on Github
	 */
	private static final String RELEASES_PAGE_URL = "https://github.com/UniversalMediaServer/UniversalMediaServer/releases";

	/**
	 * If false, manual and automatic update checks are unconditionally disabled.
	 */
	private static final boolean IS_UPDATABLE = true;

	/**
	 * the name of the subdirectory under which UMS config files are stored for this build.
	 * the default value is "UMS" e.g.
	 *
	 *     Windows:
	 *
	 *         %ALLUSERSPROFILE%\UMS
	 *
	 *     Mac OS X:
	 *
	 *         /home/<username>/Library/Application Support/UMS
	 *
	 *     Linux &c.
	 *
	 *         /home/<username>/.config/UMS
	 *
	 * a custom build can change this to avoid interfering with the config files of other
	 * builds e.g.:
	 *
	 *     PROFILE_DIRECTORY_NAME = "UMS Rendr Edition";
	 *     PROFILE_DIRECTORY_NAME = "pms-mlx";
	 *
	 * Note: custom Windows builds that change this value should change the corresponding "$ALLUSERSPROFILE\UMS"
	 * value in nsis/setup.nsi
	 *
	 * @return The profile directory name
	 */

	private static final String PROFILE_DIRECTORY_NAME = "UMS";

	/**
	 * @return whether updating has been disabled, or there is no defined server.
	 */
	public static boolean isUpdatable() {
		return IS_UPDATABLE && getUpdateServerURL() != null;
	}

	/**
	 * @return the URL where the newest version of the software can be downloaded.
	 */
	public static String getUpdateServerURL() {
		return StringUtils.isNotBlank(UPDATE_SERVER_URL) ? UPDATE_SERVER_URL : null;
	}

	/**
	 * Returns the {@link #PROFILE_DIRECTORY_NAME} where configuration files
	 * for this version of UMS are stored.
	 *
	 * @return The profile directory name
	 */
	public static String getProfileDirectoryName() {
		return PMS.isRunningTests() ? "UMS-tests" : PROFILE_DIRECTORY_NAME;
	}

	/**
	 * @return the {@link #RELEASES_PAGE_URL} where releases are located
	 */
	public static String getReleasesPageUrl() {
		return RELEASES_PAGE_URL;
	}
}
