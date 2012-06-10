package net.pms.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

class ConfigurationProgramPaths implements ProgramPaths {
	private static final String KEY_VLC_PATH        = "vlc_path";
	private static final String KEY_EAC3TO_PATH     = "eac3to_path";
	private static final String KEY_MENCODER_PATH   = "mencoder_path";
	private static final String KEY_FFMPEG_PATH     = "ffmpeg_path";
	private static final String KEY_MPLAYER_PATH    = "mplayer_path";
	private static final String KEY_TSMUXER_PATH    = "tsmuxer_path";
	private static final String KEY_FLAC_PATH       = "flac_path";
	private static final String KEY_DCRAW           = "dcraw_path";
	private static final String KEY_IMCONVERT_PATH  = "imconvert_path";
	private static final String KEY_INTERFRAME_PATH = "interframe_path";

	private final Configuration configuration;
	private final ProgramPaths defaults;

	public ConfigurationProgramPaths(Configuration configuration, ProgramPaths defaults) {
		this.configuration = configuration;
		this.defaults = defaults;
	}

	@Override
	public String getEac3toPath() {
		return stringFromConfigFile(KEY_EAC3TO_PATH, defaults.getEac3toPath());
	}

	@Override
	public String getFfmpegPath() {
		return stringFromConfigFile(KEY_FFMPEG_PATH, defaults.getFfmpegPath());
	}

	@Override
	public String getFlacPath() {
		return stringFromConfigFile(KEY_FLAC_PATH, defaults.getFlacPath());
	}

	@Override
	public String getMencoderPath() {
		return stringFromConfigFile(KEY_MENCODER_PATH, defaults.getMencoderPath());
	}

	@Override
	public String getMplayerPath() {
		return stringFromConfigFile(KEY_MPLAYER_PATH, defaults.getMplayerPath());
	}

	@Override
	public String getTsmuxerPath() {
		return stringFromConfigFile(KEY_TSMUXER_PATH, defaults.getTsmuxerPath());
	}

	@Override
	public String getVlcPath() {
		return stringFromConfigFile(KEY_VLC_PATH, defaults.getVlcPath());
	}

	private String stringFromConfigFile(String key, String def) {
		String value = configuration.getString(key);
		return StringUtils.isNotBlank(value) ? value : def;
	}

	@Override
	public String getDCRaw() {
		return stringFromConfigFile(KEY_DCRAW, defaults.getDCRaw());
	}

	@Override
	public String getIMConvertPath() {
		return stringFromConfigFile(KEY_IMCONVERT_PATH, defaults.getIMConvertPath());
	}

	@Override
	public String getInterFramePath() {
		return stringFromConfigFile(KEY_INTERFRAME_PATH, defaults.getInterFramePath());
	}
}
