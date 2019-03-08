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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.pms.util.FileUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * This class keeps track of paths to external programs on macOS.
 *
 * @author Nadahar
 */
public class OSXProgramPaths extends PlatformProgramPaths {
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
	@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
	protected OSXProgramPaths() {
		// FFmpeg
		Path ffmpeg = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			ffmpeg = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("ffmpeg");
		}
		if (ffmpeg == null || !Files.exists(ffmpeg)) {
			ffmpeg = PLATFORM_BINARIES_FOLDER.resolve("ffmpeg");
		}
		ffmpegInfo = new FFmpegProgramInfo("FFmpeg", ProgramExecutableType.BUNDLED);
		ffmpegInfo.setPath(ProgramExecutableType.BUNDLED, ffmpeg);
		ffmpeg = FileUtil.findExecutableInOSPath(Paths.get("ffmpeg"));
		if (ffmpeg != null) {
			ffmpegInfo.setPath(ProgramExecutableType.INSTALLED, ffmpeg);
		}

		// MPlayer
		Path mPlayer = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			mPlayer = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("mplayer");
		}
		if (mPlayer == null || !Files.exists(mPlayer)) {
			mPlayer = PLATFORM_BINARIES_FOLDER.resolve("mplayer");
		}
		mPlayerInfo = new ExternalProgramInfo("MPlayer", ProgramExecutableType.BUNDLED);
		mPlayerInfo.setPath(ProgramExecutableType.BUNDLED, mPlayer);
		mPlayer = FileUtil.findExecutableInOSPath(Paths.get("mplayer"));
		if (mPlayer != null) {
			mPlayerInfo.setPath(ProgramExecutableType.INSTALLED, mPlayer);
		}

		// VLC
		Path vlc = Paths.get("/Applications/VLC.app/Contents/MacOS/VLC");
		if (!Files.exists(vlc)) {
			vlc = FileUtil.findExecutableInOSPath(Paths.get("VLC"));
		}
		if (vlc != null) {
			vlcInfo = new ExternalProgramInfo("VLC", ProgramExecutableType.INSTALLED);
			vlcInfo.setPath(ProgramExecutableType.INSTALLED, vlc);
		} else {
			vlcInfo = new ExternalProgramInfo("VLC", null);
		}

		// MEncoder
		Path mEncoder = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			mEncoder = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("mencoder");
		}
		if (mEncoder == null || !Files.exists(mEncoder)) {
			mEncoder = PLATFORM_BINARIES_FOLDER.resolve("mencoder");
		}
		mEncoderInfo = new ExternalProgramInfo("MEncoder", ProgramExecutableType.BUNDLED);
		mEncoderInfo.setPath(ProgramExecutableType.BUNDLED, mEncoder);
		mEncoder = FileUtil.findExecutableInOSPath(Paths.get("mencoder"));
		if (mEncoder != null) {
			mEncoderInfo.setPath(ProgramExecutableType.INSTALLED, mEncoder);
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
		tsMuxeR = Paths.get("/Applications/tsMuxerGUI.app/Contents/MacOS/tsMuxeR");
		if (!Files.exists(tsMuxeR)) {
			tsMuxeR = FileUtil.findExecutableInOSPath(Paths.get("tsMuxeR"));
		}
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

		// FLAC
		Path flac = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			flac = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("flac");
		}
		if (flac == null || !Files.exists(flac)) {
			flac = PLATFORM_BINARIES_FOLDER.resolve("flac");
		}
		flacInfo = new ExternalProgramInfo("FLAC", ProgramExecutableType.BUNDLED);
		flacInfo.setPath(ProgramExecutableType.BUNDLED, flac);
		flac = FileUtil.findExecutableInOSPath(Paths.get("flac"));
		if (flac != null) {
			flacInfo.setPath(ProgramExecutableType.INSTALLED, flac);
		}

		// DCRaw
		Path dcRaw = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			dcRaw = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("dcraw");
		}
		if (dcRaw == null || !Files.exists(dcRaw)) {
			dcRaw = PLATFORM_BINARIES_FOLDER.resolve("dcraw");
		}
		dcRawInfo = new ExternalProgramInfo("DCRaw", ProgramExecutableType.BUNDLED);
		dcRawInfo.setPath(ProgramExecutableType.BUNDLED, dcRaw);
		dcRaw = FileUtil.findExecutableInOSPath(Paths.get("dcraw"));
		if (dcRaw != null) {
			dcRawInfo.setPath(ProgramExecutableType.INSTALLED, dcRaw);
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
