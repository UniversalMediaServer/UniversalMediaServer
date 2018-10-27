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
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import net.pms.util.PropertiesUtil;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WindowsDefaultPaths implements ProgramPaths {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsDefaultPaths.class);

	@Override
	public String getFfmpegPath() {
		if (Platform.is64Bit()) {
			return getBinariesPath() + "win32/ffmpeg64.exe";
		}
		return getBinariesPath() + "win32/ffmpeg.exe";
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

	/**
	 * @return The {@link Path} for {@code ctrlsender.exe}.
	 */
	public Path getCtrlSender() {
		Path tmpCtrlSender = Paths.get("src/main/external-resources/lib/ctrlsender/ctrlsender.exe");
		if (!Files.exists(tmpCtrlSender)) {
			tmpCtrlSender =  Paths.get(getBinariesPath(), ("win32/ctrlsender.exe"));
		}
		try {
			if (!new FilePermissions(tmpCtrlSender).isExecutableFile()) {
				tmpCtrlSender = null;
			}
		} catch (FileNotFoundException e) {
			tmpCtrlSender = null;
		}
		return tmpCtrlSender;
	}

	/**
	 * @return The {@link Path} for {@code taskkill.exe}.
	 */
	public Path getTaskKill() {
		return FileUtil.findExecutableInOSPath(Paths.get("taskkill.exe"));
	}
}
