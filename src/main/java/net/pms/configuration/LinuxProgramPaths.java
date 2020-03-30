/*
 * Digital Media Server, for streaming digital media to DLNA compatible devices
 * based on www.ps3mediaserver.org and www.universalmediaserver.com.
 * Copyright (C) 2016 Digital Media Server developers.
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

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import com.sun.jna.Platform;


/**
 * This class keeps track of paths to external programs on Linux.
 *
 * @author Nadahar
 */
public class LinuxProgramPaths extends PlatformProgramPaths {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxProgramPaths.class);
	private final FFmpegProgramInfo ffmpegInfo;
	private final ExternalProgramInfo mPlayerInfo;
	private final ExternalProgramInfo vlcInfo;
	private final ExternalProgramInfo mEncoderInfo;
	private final ExternalProgramInfo tsMuxeRInfo;
	private final ExternalProgramInfo tsMuxeRNewInfo;
	private final ExternalProgramInfo flacInfo;
	private final ExternalProgramInfo dcRawInfo;

	/**
	 * Not to be instantiated, call {@link PlatformProgramPaths#get()} instead.
	 */
	protected LinuxProgramPaths() {
		// FFmpeg
		Path ffmpeg = null;
		if (Platform.is64Bit()) {
			if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
				ffmpeg = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("ffmpeg64");
			}
			if (ffmpeg == null || !Files.exists(ffmpeg)) {
				ffmpeg = PLATFORM_BINARIES_FOLDER.resolve("ffmpeg64");
			}
			try {
				if (!new FilePermissions(ffmpeg).isExecutable()) {
					LOGGER.trace("Insufficient permission to executable \"{}\"", ffmpeg);
					LOGGER.trace("Looking for non-64 version");
					ffmpeg = null;
				}
			} catch (FileNotFoundException e) {
				LOGGER.trace("Executable \"{}\" not found: {}", ffmpeg, e.getMessage());
				LOGGER.trace("Looking for non-64 version");
				ffmpeg = null;
			}
		}
		if (ffmpeg == null) {
			ffmpeg = PLATFORM_BINARIES_FOLDER.resolve("ffmpeg");
			try {
				if (!new FilePermissions(ffmpeg).isExecutable()) {
					LOGGER.trace("Insufficient permission to executable \"{}\"", ffmpeg);
					if (Platform.is64Bit()) {
						ffmpeg = PLATFORM_BINARIES_FOLDER.resolve("ffmpeg64");
					}
				}
			} catch (FileNotFoundException e) {
				LOGGER.trace("Executable \"{}\" not found: {}", ffmpeg, e.getMessage());
				if (Platform.is64Bit()) {
					ffmpeg = PLATFORM_BINARIES_FOLDER.resolve("ffmpeg64");
				}
			}
		}

		ffmpegInfo = new FFmpegProgramInfo("FFmpeg", ProgramExecutableType.BUNDLED);
		ffmpegInfo.setPath(ProgramExecutableType.BUNDLED, ffmpeg);

		ffmpeg = null;
		if (Platform.is64Bit()) {
			ffmpeg = FileUtil.findExecutableInOSPath(Paths.get("ffmpeg64"));
		}
		if (ffmpeg == null) {
			ffmpeg = FileUtil.findExecutableInOSPath(Paths.get("ffmpeg"));
		}
		if (ffmpeg != null) {
			ffmpegInfo.setPath(ProgramExecutableType.INSTALLED, ffmpeg);
		}

		// MPlayer
		Path mPlayer = FileUtil.findExecutableInOSPath(Paths.get("mplayer"));
		if (mPlayer != null) {
			mPlayerInfo = new ExternalProgramInfo("MPlayer", ProgramExecutableType.INSTALLED);
			mPlayerInfo.setPath(ProgramExecutableType.INSTALLED, mPlayer);
		} else {
			mPlayerInfo = new ExternalProgramInfo("MPlayer", null);
		}

		// VLC
		Path vlc = FileUtil.findExecutableInOSPath(Paths.get("vlc"));
		if (vlc != null) {
			vlcInfo = new ExternalProgramInfo("VLC", ProgramExecutableType.INSTALLED);
			vlcInfo.setPath(ProgramExecutableType.INSTALLED, vlc);
		} else {
			vlcInfo = new ExternalProgramInfo("VLC", null);
		}

		// MEncoder
		Path mEncoder = FileUtil.findExecutableInOSPath(Paths.get("mencoder"));
		if (mEncoder != null) {
			mEncoderInfo = new ExternalProgramInfo("MEncoder", ProgramExecutableType.INSTALLED);
			mEncoderInfo.setPath(ProgramExecutableType.INSTALLED, mEncoder);
		} else {
			mEncoderInfo = new ExternalProgramInfo("MEncoder", null);
		}

		// tsMuxeR
		Path tsMuxeR = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			tsMuxeR = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("tsMuxeR");
		}
		if (tsMuxeR == null || !Files.exists(tsMuxeR)) {
			tsMuxeR = PLATFORM_BINARIES_FOLDER.resolve("tsMuxeR");
		}
		tsMuxeRInfo = new ExternalProgramInfo("tsMuxeR", ProgramExecutableType.BUNDLED);
		tsMuxeRInfo.setPath(ProgramExecutableType.BUNDLED, tsMuxeR);
		tsMuxeR = FileUtil.findExecutableInOSPath(Paths.get("tsMuxeR"));
		if (tsMuxeR != null) {
			tsMuxeRInfo.setPath(ProgramExecutableType.INSTALLED, tsMuxeR);
		}

		// tsMuxeRNew
		Path tsMuxeRNew = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			tsMuxeRNew = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("tsMuxeR-new");
		}
		if (tsMuxeRNew == null || !Files.exists(tsMuxeRNew)) {
			tsMuxeRNew = PLATFORM_BINARIES_FOLDER.resolve("tsMuxeR-new");
		}
		tsMuxeRNewInfo = new ExternalProgramInfo("tsMuxeRNew", ProgramExecutableType.BUNDLED);
		tsMuxeRNewInfo.setPath(ProgramExecutableType.BUNDLED, tsMuxeRNew);
		tsMuxeRNew = FileUtil.findExecutableInOSPath(Paths.get("tsMuxeR-new"));
		if (tsMuxeRNew != null) {
			tsMuxeRNewInfo.setPath(ProgramExecutableType.INSTALLED, tsMuxeRNew);
		}

		// FLAC
		Path flac = FileUtil.findExecutableInOSPath(Paths.get("flac"));
		if (flac != null) {
			flacInfo = new ExternalProgramInfo("FLAC", ProgramExecutableType.INSTALLED);
			flacInfo.setPath(ProgramExecutableType.INSTALLED, flac);
		} else {
			flacInfo = new ExternalProgramInfo("FLAC", null);
		}

		// DCRaw
		Path dcRaw = FileUtil.findExecutableInOSPath(Paths.get("dcraw"));
		if (dcRaw != null) {
			dcRawInfo = new ExternalProgramInfo("DCRaw", ProgramExecutableType.INSTALLED);
			dcRawInfo.setPath(ProgramExecutableType.INSTALLED, dcRaw);
		} else {
			dcRawInfo = new ExternalProgramInfo("DCRaw", null);
		}
	}

	@Override
	public FFmpegProgramInfo getFFmpeg() {
		return ffmpegInfo;
	}

	@Override
	public ExternalProgramInfo getMPlayer() {
		return mPlayerInfo;
	}

	@Override
	public ExternalProgramInfo getVLC() {
		return vlcInfo;
	}

	@Override
	public ExternalProgramInfo getMEncoder() {
		return mEncoderInfo;
	}

	@Override
	public ExternalProgramInfo getTsMuxeR() {
		return tsMuxeRInfo;
	}

	@Override
	public ExternalProgramInfo getTsMuxeRNew() {
		return tsMuxeRNewInfo;
	}

	@Override
	public ExternalProgramInfo getFLAC() {
		return flacInfo;
	}

	@Override
	public ExternalProgramInfo getDCRaw() {
		return dcRawInfo;
	}

	@Override
	public ExternalProgramInfo getInterFrame() {
		return null;
	}
}
