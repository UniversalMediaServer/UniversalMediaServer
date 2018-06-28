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
import java.io.File;
import net.pms.util.PropertiesUtil;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WindowsDefaultPaths implements ProgramPaths {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsDefaultPaths.class);

	/**
	 * Our source for FFmpeg binaries has started to disclose the minimum
	 * version of Windows they work on, on the download page at
	 * https://ffmpeg.zeranoe.com/builds/
	 *
	 * At the time of writing, it is Windows 7+, so we carry on that logic
	 * here.
	 *
	 * Note: This does not mean the "old" version actually works on Vista
	 * and XP, rather that the new version is guaranteed not to. This note
	 * should be removed/changed if XP and Vista are tested.
	 *
	 * @return the path to the correct FFmpeg version
	 */
	@Override
	public String getFfmpegPath() {
		String path = getBinariesPath() + "win32/ffmpeg";

		if (Platform.is64Bit()) {
			path += "64";
		}

		if (
			System.getProperty("os.name") == "Windows Vista" ||
			System.getProperty("os.name") == "Windows XP"
		) {
			path += "-old";
		}

		return path + ".exe";
	}

	@Override
	public String getFlacPath() {
		return getBinariesPath() + "win32/flac.exe";
	}

	@Override
	public String getMencoderPath() {
		return getBinariesPath() + "win32/mencoder.exe";
	}

	@Override
	public String getMplayerPath() {
		return getBinariesPath() + "win32/mplayer.exe";
	}

	@Override
	public String getTsmuxerPath() {
		return getBinariesPath() + "win32/tsMuxeR.exe";
	}

	@Override
	public String getTsmuxerNewPath() {
		return getBinariesPath() + "win32/tsMuxeR-new.exe";
	}

	@Override
	public String getVlcPath() {
		return getBinariesPath() +  "videolan/vlc.exe";
	}

	@Override
	public String getDCRaw() {
		return getBinariesPath() + "win32/dcrawMS.exe";
	}

	@Override
	public String getInterFramePath() {
		return getBinariesPathAbsolute() + "win32/interframe/";
	}

	/**
	 * Returns the relative path where binaries can be found. This path differs between
	 * the build phase and the test phase. The path will end with a slash unless
	 * it is empty.
	 *
	 * @return The relative path for binaries.
	 */
	private String getBinariesPath() {
		String path = PropertiesUtil.getProjectProperties().get("project.binaries.dir");

		if (isNotBlank(path)) {
			if (path.endsWith("/")) {
				return path;
			} else {
				return path + "/";
			}
		} else {
			return "";
		}
	}

	/**
	 * Returns the absolute path where binaries can be found. This path differs between
	 * the build phase and the test phase. The path will end with a slash unless
	 * it is empty.
	 *
	 * @return The absolute path for binaries.
	 */
	private String getBinariesPathAbsolute() {
		File dir = new File(".");
		try {
			String path = dir.getCanonicalPath();
			if (isNotBlank(path)) {
				if (path.endsWith("/")) {
					return path;
				} else {
					return path + "/";
				}
			} else {
				return "";
			}
		} catch (Exception e) {
			LOGGER.info("Couldn't get the absolute path");
			return "";
		}
	}
}
