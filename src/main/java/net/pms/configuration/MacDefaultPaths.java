package net.pms.configuration;

import net.pms.util.PropertiesUtil;

class MacDefaultPaths implements ProgramPaths {
	@Override
	public String getEac3toPath() {
		return null;
	}

	@Override
	public String getFfmpegPath() {
		return getBinariesPath() + "osx/ffmpeg";
	}

	@Override
	public String getFlacPath() {
		return getBinariesPath() + "osx/flac";
	}

	@Override
	public String getMencoderPath() {
		return getBinariesPath() + "osx/mencoder";
	}

	@Override
	public String getMplayerPath() {
		return getBinariesPath() + "osx/mplayer";
	}

	@Override
	public String getTsmuxerPath() {
		return getBinariesPath() + "osx/tsMuxeR";
	}

	@Override
	public String getVlcPath() {
		return "/Applications/VLC.app/Contents/MacOS/VLC";
	}

	@Override
	public String getDCRaw() {
		return getBinariesPath() + "osx/dcraw";
	}
	
	@Override
	public String getIMConvertPath() {
		return getBinariesPath() + "osx/convert";
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
