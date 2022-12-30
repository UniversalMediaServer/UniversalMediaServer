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
package net.pms.platform.windows;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.platform.PlatformProgramPaths;
import net.pms.platform.PlatformUtils;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.FFmpegProgramInfo;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import net.pms.util.ProgramExecutableType;

/**
 * This class keeps track of paths to external programs on Windows.
 *
 * @author Nadahar
 */
public class WindowsProgramPaths extends PlatformProgramPaths {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsProgramPaths.class);
	private final FFmpegProgramInfo ffmpegInfo;
	private final ExternalProgramInfo mPlayerInfo;
	private final ExternalProgramInfo vlcInfo;
	private final ExternalProgramInfo mEncoderInfo;
	private final ExternalProgramInfo tsMuxeRInfo;
	private final ExternalProgramInfo flacInfo;
	private final ExternalProgramInfo dcRawInfo;
	private final ExternalProgramInfo aviSynthInfo;
	private final ExternalProgramInfo interFrameInfo;
	private final ExternalProgramInfo ffms2Info;
	private final ExternalProgramInfo directShowSourceInfo;
	private final ExternalProgramInfo mvtools2Info;
	private final ExternalProgramInfo masktools2Info;
	private final ExternalProgramInfo convert2dTo3dInfo;
	private final ExternalProgramInfo youtubeDlInfo;
	private final Path mediaInfo;
	private final Path ctrlSender;
	private final Path taskKill;

	/**
	 * Not to be instantiated, call {@link PlatformProgramPaths#get()} instead.
	 */
	public WindowsProgramPaths() {
		// FFmpeg
		Path ffmpeg = resolve("ffmpeg.exe");
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
		Path mPlayer = resolve("mplayer.exe");
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
		Path mEncoder = resolve("mencoder.exe");
		mEncoderInfo = new ExternalProgramInfo("MEncoder", ProgramExecutableType.BUNDLED);
		mEncoderInfo.setPath(ProgramExecutableType.BUNDLED, mEncoder);
		mEncoder = FileUtil.findExecutableInOSPath(Paths.get("mencoder.exe"));
		if (mEncoder != null) {
			mEncoderInfo.setPath(ProgramExecutableType.INSTALLED, mEncoder);
		}

		// tsMuxeR
		Path tsMuxeR = resolve("tsMuxeR.exe");
		tsMuxeRInfo = new ExternalProgramInfo("tsMuxeR", ProgramExecutableType.BUNDLED);
		tsMuxeRInfo.setPath(ProgramExecutableType.BUNDLED, tsMuxeR);
		tsMuxeR = FileUtil.findExecutableInOSPath(Paths.get("tsMuxeR.exe"));
		if (tsMuxeR != null) {
			tsMuxeRInfo.setPath(ProgramExecutableType.INSTALLED, tsMuxeR);
		}

		// FLAC
		Path flac = resolve("flac.exe");
		flacInfo = new ExternalProgramInfo("FLAC", ProgramExecutableType.BUNDLED);
		flacInfo.setPath(ProgramExecutableType.BUNDLED, flac);

		// DCRaw
		Path dcRaw = resolve("dcrawMS.exe");
		dcRawInfo = new ExternalProgramInfo("DCRaw", ProgramExecutableType.BUNDLED);
		dcRawInfo.setPath(ProgramExecutableType.BUNDLED, dcRaw);
		dcRaw = FileUtil.findExecutableInOSPath(Paths.get("dcrawMS.exe"));
		if (dcRaw != null) {
			dcRawInfo.setPath(ProgramExecutableType.INSTALLED, dcRaw);
		}

		// AviSynth
		Path aviSynth = resolve("avisynth.dll");
		aviSynthInfo = new ExternalProgramInfo("AviSynth", ProgramExecutableType.BUNDLED);
		aviSynthInfo.setPath(ProgramExecutableType.BUNDLED, aviSynth);

		// InterFrame
		Path interframe = resolve("interframe");
		interFrameInfo = new ExternalProgramInfo("InterFrame", ProgramExecutableType.BUNDLED);
		interFrameInfo.setPath(ProgramExecutableType.BUNDLED, interframe);

		// Convert 2dto3d
		Path convert2dTo3d = resolve("avisynth/convert2dto3d.avsi");
		convert2dTo3dInfo = new ExternalProgramInfo("convert2dto3d", ProgramExecutableType.BUNDLED);
		convert2dTo3dInfo.setPath(ProgramExecutableType.BUNDLED, convert2dTo3d);

		// FFMS2
		Path ffms2 = resolve("avisynth/ffms2.dll");
		ffms2Info = new ExternalProgramInfo("ffms2", ProgramExecutableType.BUNDLED);
		ffms2Info.setPath(ProgramExecutableType.BUNDLED, ffms2);

		// DirectShowSource
		Path directShowSource = resolve("avisynth/DirectShowSource.dll");
		directShowSourceInfo = new ExternalProgramInfo("DirectShowSource", ProgramExecutableType.BUNDLED);
		directShowSourceInfo.setPath(ProgramExecutableType.BUNDLED, directShowSource);

		// mvtools2
		Path mvtools2 = resolve("avisynth/mvtools2.dll");
		mvtools2Info = new ExternalProgramInfo("mvtools2", ProgramExecutableType.BUNDLED);
		mvtools2Info.setPath(ProgramExecutableType.BUNDLED, mvtools2);

		// masktools2
		Path masktools2 = resolve("avisynth/masktools2.dll");
		masktools2Info = new ExternalProgramInfo("masktools2", ProgramExecutableType.BUNDLED);
		masktools2Info.setPath(ProgramExecutableType.BUNDLED, masktools2);

		// CtrlSender
		Path tmpCtrlSender = resolve("ctrlsender.exe");
		try {
			if (!new FilePermissions(tmpCtrlSender).isExecutableFile()) {
				tmpCtrlSender = null;
			}
		} catch (FileNotFoundException e) {
			tmpCtrlSender = null;
		}
		ctrlSender = tmpCtrlSender;

		// youtube-dl
		Path youtubeDl = resolve("youtube-dl.exe");
		youtubeDlInfo = new ExternalProgramInfo("youtube-dl", ProgramExecutableType.BUNDLED);
		youtubeDlInfo.setPath(ProgramExecutableType.BUNDLED, youtubeDl);

		// mediaInfo
		Path tmpMediaInfo = resolve("mediainfo.dll");
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
	public ExternalProgramInfo getFLAC() {
		return flacInfo;
	}

	@Override
	public ExternalProgramInfo getDCRaw() {
		return dcRawInfo;
	}

	@Override
	public ExternalProgramInfo getAviSynth() {
		return aviSynthInfo;
	}

	@Override
	public ExternalProgramInfo getInterFrame() {
		return interFrameInfo;
	}

	@Override
	public ExternalProgramInfo getFFMS2() {
		return ffms2Info;
	}

	@Override
	public ExternalProgramInfo getDirectShowSource() {
		return ffms2Info;
	}

	@Override
	public ExternalProgramInfo getMvtools2() {
		return mvtools2Info;
	}

	@Override
	public ExternalProgramInfo getMasktools2() {
		return masktools2Info;
	}

	@Override
	public ExternalProgramInfo getConvert2dTo3d() {
		return convert2dTo3dInfo;
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
