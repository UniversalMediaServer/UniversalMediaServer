/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2010  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.FileUtil;
import org.apache.commons.io.FileUtils;

/**
 * Logback PropertyDefiner to set the path for the <code>debug.log</code> file.
 * @author thomas@innot.de
 */
public class DebugLogPathDefiner extends PropertyDefinerBase {
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * @return first writable folder in the following order:
	 * <p>
	 *     1. (On Linux only) path to {@code /var/log/universalmediaserver/%USERNAME%/}.
	 * </p>
	 * <p>
	 *     2. Path to profile folder ({@code ~/.config/UMS/} on Linux, {@code %ALLUSERSPROFILE%\UMS} on Windows and
	 *     {@code ~/Library/Application Support/UMS/} on Mac).
	 * </p>
	 * <p>
	 *     3. Path to user-defined temp folder specified by {@code temp_directory} param in UMS.conf.
	 * </p>
	 * <p>
	 *     4. Path to system temp folder.
	 * </p>
	 */
	@Override
	public String getPropertyValue() {
		if (Platform.isLinux()) {
			final String username = System.getProperty("user.name");
			final File logDirectory = new File("/var/log/universalmediaserver/" + username + "/");
			try {
				FileUtils.forceMkdir(logDirectory);
				if (FileUtil.isDirectoryWritable(logDirectory)) {
					return logDirectory.getAbsolutePath();
				}
			} catch (IOException ex) {
				// Could not create directory, possible permissions problems.
			}
		}

		// Check if profile directory is writable.
		final File logDirectory = new File(configuration.getProfileDirectory());
		if (FileUtil.isDirectoryWritable(logDirectory)) {
			return logDirectory.getAbsolutePath();
		}

		// Try user-defined temp folder or fallback to system temp folder, which should be writable.
		try {
			return configuration.getTempFolder().getAbsolutePath();
		} catch (IOException ex) {
			return System.getProperty("java.io.tmpdir");
		}
	}
}
