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
package net.pms.platform.linux;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.platform.PlatformProgramPaths;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.FFmpegProgramInfo;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import net.pms.util.ProgramExecutableType;

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
	private final ExternalProgramInfo flacInfo;
	private final ExternalProgramInfo dcRawInfo;
	private final ExternalProgramInfo youtubeDlInfo;

	/**
	 * Not to be instantiated, call {@link PlatformProgramPaths#get()} instead.
	 */
	public LinuxProgramPaths() {
		// We use the system FFmpeg if it exists, otherwise use ours
		Path ffmpeg = FileUtil.findExecutableInOSPath(Paths.get("ffmpeg"));
		ffmpegInfo = new FFmpegProgramInfo("FFmpeg", ProgramExecutableType.INSTALLED);
		if (ffmpeg != null) {
			ffmpegInfo.setPath(ProgramExecutableType.INSTALLED, ffmpeg);
		} else {
			ffmpeg = PLATFORM_BINARIES_FOLDER.resolve("ffmpeg");
			ffmpegInfo.setPath(ProgramExecutableType.BUNDLED, ffmpeg);
			ffmpegInfo.setDefault(ProgramExecutableType.BUNDLED);
			try {
				if (!new FilePermissions(ffmpeg).isExecutable()) {
					LOGGER.trace("Insufficient permission to executable \"{}\"", ffmpeg);
				}
			} catch (FileNotFoundException e) {
				LOGGER.trace("Executable \"{}\" not found: {}", ffmpeg, e.getMessage());
			}
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

		// youtube-dl
		Path youtubeDl = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			youtubeDl = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("youtube-dl");
		}
		if (youtubeDl == null || !Files.exists(youtubeDl)) {
			youtubeDl = PLATFORM_BINARIES_FOLDER.resolve("youtube-dl");
		}
		youtubeDlInfo = new ExternalProgramInfo("youtube-dl", ProgramExecutableType.BUNDLED);
		youtubeDlInfo.setPath(ProgramExecutableType.BUNDLED, youtubeDl);
		youtubeDl = FileUtil.findExecutableInOSPath(Paths.get("youtube-dl"));
		if (youtubeDl != null) {
			youtubeDlInfo.setPath(ProgramExecutableType.INSTALLED, youtubeDl);
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
	public ExternalProgramInfo getFLAC() {
		return flacInfo;
	}

	@Override
	public ExternalProgramInfo getDCRaw() {
		return dcRawInfo;
	}

	@Override
	public ExternalProgramInfo getAviSynth() {
		return null;
	}

	@Override
	public ExternalProgramInfo getInterFrame() {
		return null;
	}

	@Override
	public ExternalProgramInfo getFFMS2() {
		return null;
	}

	@Override
	public ExternalProgramInfo getDirectShowSource() {
		return null;
	}

	@Override
	public ExternalProgramInfo getMvtools2() {
		return null;
	}

	@Override
	public ExternalProgramInfo getMasktools2() {
		return null;
	}

	@Override
	public ExternalProgramInfo getConvert2dTo3d() {
		return null;
	}

	@Override
	public ExternalProgramInfo getYoutubeDl() {
		return youtubeDlInfo;
	}

}
