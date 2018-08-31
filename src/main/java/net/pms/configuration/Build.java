/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms.configuration;

import com.sun.jna.Platform;
import net.pms.PMS;
import org.apache.commons.lang3.StringUtils;

// a one-stop class for values and methods specific to custom PMS builds
public class Build {
	/**
	 * Repository where to locate the file. Note: using "raw.github.com"
	 * to access the raw file.
	 */
	private static final String REPO = "https://raw.github.com/UniversalMediaServer/UniversalMediaServer";

	/**
	 * The URL of the properties file used by the {@link AutoUpdater} to announce PMS updates.
	 * Can be null/empty if not used. Not used if IS_UPDATABLE is set to false.
	 */
	private static final String UPDATE_SERVER_URL = REPO + "/master/src/main/external-resources/update/latest_version.properties";

	/**
	 * If false, manual and automatic update checks are unconditionally disabled.
	 */
	private static final boolean IS_UPDATABLE = true;

	/**
	 * the name of the subdirectory under which PMS config files are stored for this build.
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
	 *     PROFILE_DIRECTORY_NAME = "PMS Rendr Edition";
	 *     PROFILE_DIRECTORY_NAME = "pms-mlx";
	 *
	 * Note: custom Windows builds that change this value should change the corresponding "$ALLUSERSPROFILE\UMS"
	 * value in nsis/setup.nsi
	 *
	 * @return The profile directory name
	 */

	private static final String PROFILE_DIRECTORY_NAME = "UMS";

	/**
	 * Determines whether or not this PMS build can be updated to a more
	 * recent version.
	 * @return True if this build can be updated, false otherwise.
	 */
	public static boolean isUpdatable() {
		return IS_UPDATABLE && Platform.isWindows() && getUpdateServerURL() != null;
	}

	/**
	 * Returns the URL where the newest version of the software can be downloaded.
	 * @return The URL.
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
}
