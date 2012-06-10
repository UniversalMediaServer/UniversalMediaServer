package net.pms.configuration;

import net.pms.util.PropertiesUtil;

class LinuxDefaultPaths implements ProgramPaths {
	@Override
	public String getEac3toPath() {
		return "eac3to";
	}

	@Override
	public String getFfmpegPath() {
		return "ffmpeg";
	}

	@Override
	public String getFlacPath() {
		return "flac";
	}

	@Override
	public String getMencoderPath() {
		return "mencoder";
	}

	@Override
	public String getMplayerPath() {
		return "mplayer";
	}

	@Override
	public String getTsmuxerPath() {
		return getBinariesPath() + "linux/tsMuxeR";
	}

	@Override
	public String getVlcPath() {
		return "vlc";
	}

	@Override
	public String getDCRaw() {
		return "dcraw";
	}
	
	@Override
	public String getIMConvertPath() {
		return "convert";
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
	private String getBinariesPath() {
		String path = PropertiesUtil.getProjectProperties().get("project.binaries.dir");

		if (path != null && !"".equals(path)) {
			if (path.endsWith("/")) {
				return path;
			} else {
				return path + "/";
			}
		} else {
			return "";
		}
	}
}
