package net.pms.configuration;

import com.sun.jna.Platform;
import java.io.File;
import net.pms.util.PropertiesUtil;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

class LinuxDefaultPaths implements ProgramPaths {
	private final String BINARIES_SEARCH_PATH = getBinariesSearchPath();

	@Override
	public String getFfmpegPath() {
		String ffmpegPath;
		if (Platform.is64Bit()) {
			ffmpegPath = getBinaryPath("ffmpeg64");

			/**
			 * Use the default FFmpeg binary if we can't find ffmpeg64
			 */
			if (ffmpegPath.equals("ffmpeg64")) {
				ffmpegPath = getBinaryPath("ffmpeg");
			}
		} else {
			ffmpegPath = getBinaryPath("ffmpeg");
		}

		return ffmpegPath;
	}

	@Override
	public String getFlacPath() {
		return getBinaryPath("flac");
	}

	@Override
	public String getMencoderPath() {
		return getBinaryPath("mencoder");
	}

	@Override
	public String getMplayerPath() {
		return getBinaryPath("mplayer");
	}

	@Override
	public String getTsmuxerPath() {
		return getBinaryPath("tsMuxeR");
	}

	@Override
	public String getTsmuxerNewPath() {
		return getBinaryPath("tsMuxeR-new");
	}

	@Override
	public String getVlcPath() {
		return getBinaryPath("vlc");
	}

	@Override
	public String getDCRaw() {
		return getBinaryPath("dcraw");
	}

	@Override
	public String getInterFramePath() {
		return null;
	}

	/**
	 * Returns the path where binaries can be found. This path differs between
	 * the build phase and the test phase. The path will end with a slash unless
	 * it is empty.
	 *
	 * @return The path for binaries.
	 */
	private String getBinariesSearchPath() {
		String path = PropertiesUtil.getProjectProperties().get("project.binaries.dir");

		if (isNotBlank(path)) {
			if (path.endsWith("/")) {
				return path;
			} else {
				return path + "/";
			}
		} else {
			return "linux/";
		}
	}

	/**
	 * Returns the path to requested binary tool.
	 * Either absolute if executable found in project.binaries.dir or
	 * short to search in system-wide PATH.
	 *
	 * @param tool The name of binary tool
	 * @return Path to binary
	 */
	private String getBinaryPath(String tool) {
		File f = new File(BINARIES_SEARCH_PATH + tool);
		if (f.canExecute()) {
			return BINARIES_SEARCH_PATH + tool;
		} else {
			return tool;
		}
	}
}
