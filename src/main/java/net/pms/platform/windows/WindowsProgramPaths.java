/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.platform.windows;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.platform.PlatformUtils;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.FFmpegProgramInfo;
import net.pms.util.ProgramExecutableType;
import net.pms.platform.linux.LinuxProgramPaths;
import net.pms.platform.PlatformProgramPaths;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of paths to external programs on Windows.
 *
 * @author Nadahar
 */
public class WindowsProgramPaths extends PlatformProgramPaths {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxProgramPaths.class);
	private final FFmpegProgramInfo ffmpegInfo;
	private final ExternalProgramInfo mPlayerInfo;
	private final ExternalProgramInfo vlcInfo;
	private final ExternalProgramInfo mEncoderInfo;
	private final ExternalProgramInfo tsMuxeRInfo;
	private final ExternalProgramInfo tsMuxeRNewInfo;
	private final ExternalProgramInfo flacInfo;
	private final ExternalProgramInfo dcRawInfo;
	private final ExternalProgramInfo interFrameInfo;
	private final ExternalProgramInfo youtubeDlInfo;
	private final Path mediaInfo;
	private final Path ctrlSender;
	private final Path taskKill;

	/**
	 * Not to be instantiated, call {@link PlatformProgramPaths#get()} instead.
	 */
	public WindowsProgramPaths() {
		// FFmpeg
		Path ffmpeg = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			ffmpeg = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("ffmpeg.exe");
		}
		if (ffmpeg == null || !Files.exists(ffmpeg)) {
			ffmpeg = PLATFORM_BINARIES_FOLDER.resolve("ffmpeg.exe");
		}
		try {
			if (!new FilePermissions(ffmpeg).isExecutable()) {
				LOGGER.trace("Insufficient permission to executable \"{}\"", ffmpeg);
			}
		} catch (FileNotFoundException e) {
			LOGGER.trace("Executable \"{}\" not found: {}", ffmpeg, e.getMessage());
		}

		ffmpegInfo = new FFmpegProgramInfo("FFmpeg", ProgramExecutableType.BUNDLED);
		ffmpegInfo.setPath(ProgramExecutableType.BUNDLED, ffmpeg);
		ffmpeg = FileUtil.findExecutableInOSPath(Paths.get("ffmpeg.exe"));
		if (ffmpeg != null) {
			ffmpegInfo.setPath(ProgramExecutableType.INSTALLED, ffmpeg);
		}

		// MPlayer
		Path mPlayer = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			mPlayer = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("mplayer.exe");
		}
		if (mPlayer == null || !Files.exists(mPlayer)) {
			mPlayer = PLATFORM_BINARIES_FOLDER.resolve("mplayer.exe");
		}
		mPlayerInfo = new ExternalProgramInfo("MPlayer", ProgramExecutableType.BUNDLED);
		mPlayerInfo.setPath(ProgramExecutableType.BUNDLED, mPlayer);
		mPlayer = FileUtil.findExecutableInOSPath(Paths.get("mplayer.exe"));
		if (mPlayer != null) {
			mPlayerInfo.setPath(ProgramExecutableType.INSTALLED, mPlayer);
		}

		// VLC
		Path vlcPath = PlatformUtils.INSTANCE.getVlcPath();
		if (vlcPath == null || !Files.exists(vlcPath)) {
			vlcPath = FileUtil.findExecutableInOSPath(Paths.get("vlc.exe"));
		}
		if (vlcPath != null) {
			vlcInfo = new ExternalProgramInfo("VLC", ProgramExecutableType.INSTALLED);
			vlcInfo.setPath(ProgramExecutableType.INSTALLED, vlcPath);
		} else {
			vlcInfo = new ExternalProgramInfo("VLC", null);
		}

		// MEncoder
		Path mEncoder = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			mEncoder = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("mencoder.exe");
		}
		if (mEncoder == null || !Files.exists(mEncoder)) {
			mEncoder = PLATFORM_BINARIES_FOLDER.resolve("mencoder.exe");
		}
		mEncoderInfo = new ExternalProgramInfo("MEncoder", ProgramExecutableType.BUNDLED);
		mEncoderInfo.setPath(ProgramExecutableType.BUNDLED, mEncoder);
		mEncoder = FileUtil.findExecutableInOSPath(Paths.get("mencoder.exe"));
		if (mEncoder != null) {
			mEncoderInfo.setPath(ProgramExecutableType.INSTALLED, mEncoder);
		}

		// tsMuxeR
		Path tsMuxeR = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			tsMuxeR = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("tsMuxeR.exe");
		}
		if (tsMuxeR == null || !Files.exists(tsMuxeR)) {
			tsMuxeR = PLATFORM_BINARIES_FOLDER.resolve("tsMuxeR.exe");
		}
		tsMuxeRInfo = new ExternalProgramInfo("tsMuxeR", ProgramExecutableType.BUNDLED);
		tsMuxeRInfo.setPath(ProgramExecutableType.BUNDLED, tsMuxeR);
		tsMuxeR = FileUtil.findExecutableInOSPath(Paths.get("tsMuxeR.exe"));
		if (tsMuxeR != null) {
			tsMuxeRInfo.setPath(ProgramExecutableType.INSTALLED, tsMuxeR);
		}

		// tsMuxeRNew
		Path tsMuxeRNew = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			tsMuxeRNew = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("tsMuxeR-new.exe");
		}
		if (tsMuxeRNew == null || !Files.exists(tsMuxeRNew)) {
			tsMuxeRNew = PLATFORM_BINARIES_FOLDER.resolve("tsMuxeR-new.exe");
		}
		tsMuxeRNewInfo = new ExternalProgramInfo("tsMuxeRNew", ProgramExecutableType.BUNDLED);
		tsMuxeRNewInfo.setPath(ProgramExecutableType.BUNDLED, tsMuxeRNew);

		// FLAC
		Path flac = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			flac = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("flac.exe");
		}
		if (flac == null || !Files.exists(flac)) {
			flac = PLATFORM_BINARIES_FOLDER.resolve("flac.exe");
		}
		flacInfo = new ExternalProgramInfo("FLAC", ProgramExecutableType.BUNDLED);
		flacInfo.setPath(ProgramExecutableType.BUNDLED, flac);

		// DCRaw
		Path dcRaw = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			dcRaw = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("dcrawMS.exe");
		}
		if (dcRaw == null || !Files.exists(dcRaw)) {
			dcRaw = PLATFORM_BINARIES_FOLDER.resolve("dcrawMS.exe");
		}
		dcRawInfo = new ExternalProgramInfo("DCRaw", ProgramExecutableType.BUNDLED);
		dcRawInfo.setPath(ProgramExecutableType.BUNDLED, dcRaw);
		dcRaw = FileUtil.findExecutableInOSPath(Paths.get("dcrawMS.exe"));
		if (dcRaw != null) {
			dcRawInfo.setPath(ProgramExecutableType.INSTALLED, dcRaw);
		}

		// InterFrame
		Path interframe = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			interframe = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("interframe");
		}
		if (interframe == null || !Files.exists(interframe)) {
			interframe = PLATFORM_BINARIES_FOLDER.resolve("interframe");
		}
		interFrameInfo = new ExternalProgramInfo("InterFrame", ProgramExecutableType.BUNDLED);
		interFrameInfo.setPath(ProgramExecutableType.BUNDLED, interframe);

		// CtrlSender
		Path tmpCtrlSender = Paths.get("src/main/external-resources/lib/ctrlsender/ctrlsender.exe");
		if (!Files.exists(tmpCtrlSender)) {
			tmpCtrlSender = PLATFORM_BINARIES_FOLDER.resolve("ctrlsender.exe");
		}
		try {
			if (!new FilePermissions(tmpCtrlSender).isExecutableFile()) {
				tmpCtrlSender = null;
			}
		} catch (FileNotFoundException e) {
			tmpCtrlSender = null;
		}
		ctrlSender = tmpCtrlSender;

		// youtube-dl
		Path youtubeDl = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			youtubeDl = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("youtube-dl.exe");
		}
		if (youtubeDl == null || !Files.exists(youtubeDl)) {
			youtubeDl = PLATFORM_BINARIES_FOLDER.resolve("youtube-dl.exe");
		}
		youtubeDlInfo = new ExternalProgramInfo("youtube-dl", ProgramExecutableType.BUNDLED);
		youtubeDlInfo.setPath(ProgramExecutableType.BUNDLED, youtubeDl);

		// mediaInfo
		Path tmpMediaInfo = null;
		if (PLATFORM_DEVELOPMENT_BINARIES_FOLDER != null) {
			tmpMediaInfo = PLATFORM_DEVELOPMENT_BINARIES_FOLDER.resolve("mediainfo.dll");
		}
		if (tmpMediaInfo == null || !Files.exists(tmpMediaInfo)) {
			tmpMediaInfo = PLATFORM_BINARIES_FOLDER.resolve("mediainfo.dll");
		}
		if (Files.exists(tmpMediaInfo)) {
			mediaInfo = tmpMediaInfo.getParent();
		} else {
			mediaInfo = null;
		}

		taskKill = FileUtil.findExecutableInOSPath(Paths.get("taskkill.exe"));
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
		return interFrameInfo;
	}

	@Override
	public ExternalProgramInfo getYoutubeDl() {
		return youtubeDlInfo;
	}

	/**
	 * @return The {@link Path} for {@code MediaInfo dll}.
	 */
	public Path getMediaInfo() {
		return mediaInfo;
	}

	/**
	 * @return The {@link Path} for {@code ctrlsender.exe}.
	 */
	@Override
	public Path getCtrlSender() {
		return ctrlSender;
	}

	/**
	 * @return The {@link Path} for {@code taskkill.exe}.
	 */
	@Override
	public Path getTaskKill() {
		return taskKill;
	}

	/**
	 * @return The Windows {@code PATHEXT} environment variable as a
	 *         {@link List} of {@link String}s containing the extensions without
	 *         the {@code .}.
	 */
	@Nonnull
	public static List<String> getWindowsPathExtensions() {
		List<String> result = new ArrayList<>();
		String osPathExtensions = System.getenv("PATHEXT");
		if (StringUtils.isBlank(osPathExtensions)) {
			return result;
		}
		String[] extensions = osPathExtensions.split(File.pathSeparator);
		for (String extension : extensions) {
			result.add(extension.replace(".", ""));
		}
		return result;
	}
}
