package net.pms.configuration;

import org.apache.commons.configuration.Configuration;

class ConfigurationProgramPaths implements ProgramPaths {
	private static final String KEY_VLC_PATH        = "vlc_path";
	private static final String KEY_MENCODER_PATH   = "mencoder_path";
	private static final String KEY_FFMPEG_PATH     = "ffmpeg_path";
	private static final String KEY_MPLAYER_PATH    = "mplayer_path";
	private static final String KEY_TSMUXER_PATH    = "tsmuxer_path";
	private static final String KEY_FLAC_PATH       = "flac_path";
	private static final String KEY_DCRAW           = "dcraw_path";
	private static final String KEY_IMCONVERT_PATH  = "imconvert_path";
	private static final String KEY_INTERFRAME_PATH = "interframe_path";

	private final ConfigurationReader configurationReader;
	private final ProgramPaths defaults;

	public ConfigurationProgramPaths(Configuration configuration, ProgramPaths defaults) {
		this.configurationReader = new ConfigurationReader(configuration, true); // true: log
		this.defaults = defaults;
	}

	@Override
	public String getFfmpegPath() {
		return getString(KEY_FFMPEG_PATH, defaults.getFfmpegPath());
	}

	@Override
	public String getFlacPath() {
		return getString(KEY_FLAC_PATH, defaults.getFlacPath());
	}

	@Override
	public String getMencoderPath() {
		return getString(KEY_MENCODER_PATH, defaults.getMencoderPath());
	}

	@Override
	public String getMplayerPath() {
		return getString(KEY_MPLAYER_PATH, defaults.getMplayerPath());
	}

	@Override
	public String getTsmuxerPath() {
		return getString(KEY_TSMUXER_PATH, defaults.getTsmuxerPath());
	}

	@Override
	public String getVlcPath() {
		return getString(KEY_VLC_PATH, defaults.getVlcPath());
	}

	/**
	 * Return the <code>String</code> value for a given configuration key if the
	 * value is non-blank (i.e. not null, not an empty string, not all whitespace).
	 * Otherwise return the supplied default value.
	 * The value is returned with leading and trailing whitespace removed in both cases.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private String getString(String key, String def) {
		return configurationReader.getNonBlankConfigurationString(key, def);
	}

	@Override
	public String getDCRaw() {
		return getString(KEY_DCRAW, defaults.getDCRaw());
	}

	@Override
	public String getInterFramePath() {
		return getString(KEY_INTERFRAME_PATH, defaults.getInterFramePath());
	}
}
