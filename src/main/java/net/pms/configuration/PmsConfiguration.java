/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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

import com.sun.jna.Platform;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.formats.Format;
import net.pms.io.SystemUtils;
import net.pms.util.FileUtil;
import net.pms.util.FileUtil.FileLocation;
import net.pms.util.PropertiesUtil;
import net.pms.util.UMSUtils;
import net.pms.util.WindowsRegistry;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for all configurable PMS settings. Settings are typically defined by three things:
 * a unique key for use in the configuration file "UMS.conf", a getter (and setter) method and
 * a default value. When a key cannot be found in the current configuration, the getter will
 * return a default value. Setters only store a value, they do not permanently save it to
 * file.
 */
public class PmsConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(PmsConfiguration.class);
	private static final int DEFAULT_PROXY_SERVER_PORT = -1;
	private static final int DEFAULT_SERVER_PORT = 5001;

	/*
	 * MEncoder has a hardwired maximum of 8 threads for -lavcopts and 16
	 * for -lavdopts.
	 * The Windows SubJunk Builds can take 16 for both, but we keep it at 8
	 * for compatibility with other operating systems.
	 */
	private static final int MENCODER_MAX_THREADS = 8;

	// TODO: Get this out of here
	private static boolean avsHackLogged = false;

	private static final String KEY_3D_SUBTITLES_DEPTH = "3d_subtitles_depth";
	private static final String KEY_ALTERNATE_SUBTITLES_FOLDER = "alternate_subtitles_folder";
	private static final String KEY_ALTERNATE_THUMB_FOLDER = "alternate_thumb_folder";
	private static final String KEY_APPEND_PROFILE_NAME = "append_profile_name";
	private static final String KEY_AUTOMATIC_MAXIMUM_BITRATE = "automatic_maximum_bitrate";
	private static final String KEY_SHOW_APERTURE_LIBRARY = "show_aperture_library";
	private static final String KEY_ATZ_LIMIT = "atz_limit";
	private static final String KEY_AUDIO_BITRATE = "audio_bitrate";
	private static final String KEY_AUDIO_CHANNEL_COUNT = "audio_channels";
	private static final String KEY_AUDIO_LANGUAGES = "audio_languages";
	private static final String KEY_AUDIO_RESAMPLE = "audio_resample";
	private static final String KEY_AUDIO_SUB_LANGS = "audio_subtitles_languages";
	private static final String KEY_AUDIO_THUMBNAILS_METHOD = "audio_thumbnails_method";
	private static final String KEY_AUTO_UPDATE = "auto_update";
	private static final String KEY_AUTOLOAD_SUBTITLES = "autoload_external_subtitles";
	private static final String KEY_AVISYNTH_CONVERT_FPS = "avisynth_convert_fps";
	private static final String KEY_AVISYNTH_INTERFRAME = "avisynth_interframe";
	private static final String KEY_AVISYNTH_INTERFRAME_GPU = "avisynth_interframegpu";
	private static final String KEY_AVISYNTH_MULTITHREADING = "avisynth_multithreading";
	private static final String KEY_AVISYNTH_SCRIPT = "avisynth_script";
	private static final String KEY_ASS_MARGIN = "subtitles_ass_margin";
	private static final String KEY_ASS_OUTLINE = "subtitles_ass_outline";
	private static final String KEY_ASS_SCALE = "subtitles_ass_scale";
	private static final String KEY_ASS_SHADOW = "subtitles_ass_shadow";
	private static final String KEY_BUFFER_MAX = "buffer_max";
	private static final String KEY_CHAPTER_INTERVAL = "chapter_interval";
	private static final String KEY_CHAPTER_SUPPORT = "chapter_support";
	private static final String KEY_MENCODER_CODEC_SPECIFIC_SCRIPT = "mencoder_codec_specific_script";
	private static final String KEY_DISABLE_FAKESIZE = "disable_fakesize";
	public static final String KEY_DISABLE_SUBTITLES = "disable_subtitles";
	private static final String KEY_DVDISO_THUMBNAILS = "dvd_isos_thumbnails";
	private static final String KEY_AUDIO_EMBED_DTS_IN_PCM = "audio_embed_dts_in_pcm";
	private static final String KEY_ENCODED_AUDIO_PASSTHROUGH = "encoded_audio_passthrough";
	private static final String KEY_ENGINES = "engines";
	private static final String KEY_FFMPEG_ALTERNATIVE_PATH = "alternativeffmpegpath"; // TODO: FFmpegDVRMSRemux will be removed and DVR-MS will be transcoded
	private static final String KEY_FFMPEG_MULTITHREADING = "ffmpeg_multithreading";
	private static final String KEY_FFMPEG_AVISYNTH_MULTITHREADING = "ffmpeg_avisynth_multithreading";
	private static final String KEY_FFMPEG_AVISYNTH_CONVERT_FPS = "ffmpeg_avisynth_convertfps";
	private static final String KEY_FFMPEG_AVISYNTH_INTERFRAME = "ffmpeg_avisynth_interframe";
	private static final String KEY_FFMPEG_AVISYNTH_INTERFRAME_GPU = "ffmpeg_avisynth_interframegpu";
	private static final String KEY_FFMPEG_FONTCONFIG = "ffmpeg_fontconfig";
	private static final String KEY_FFMPEG_MUX_TSMUXER_COMPATIBLE = "ffmpeg_mux_tsmuxer_compatible";
	private static final String KEY_FFMPEG_MENCODER_SUBTITLES = "ffmpeg_mencoder_subtitles";
	private static final String KEY_FIX_25FPS_AV_MISMATCH = "fix_25fps_av_mismatch";
	private static final String KEY_FOLDERS = "folders";
	private static final String KEY_FOLDERS_IGNORED = "folders_ignored";
	private static final String KEY_FOLDERS_MONITORED = "folders_monitored";
	private static final String KEY_FONT = "subtitles_font";
	private static final String KEY_FORCED_SUBTITLE_LANGUAGE = "forced_subtitle_language";
	private static final String KEY_FORCED_SUBTITLE_TAGS = "forced_subtitle_tags";
	private static final String KEY_FORCE_EXTERNAL_SUBTITLES = "force_external_subtitles";
	private static final String KEY_FORCE_TRANSCODE_FOR_EXTENSIONS = "force_transcode_for_extensions";
	private static final String KEY_FOLDER_LIMIT = "folder_limit";
	public static final String KEY_GPU_ACCELERATION = "gpu_acceleration";
	private static final String KEY_HIDE_ADVANCED_OPTIONS = "hide_advanced_options";
	private static final String KEY_HIDE_EMPTY_FOLDERS = "hide_empty_folders";
	private static final String KEY_HIDE_ENGINENAMES = "hide_enginenames";
	private static final String KEY_HIDE_EXTENSIONS = "hide_extensions";
	private static final String KEY_HIDE_RECENTLY_PLAYED_FOLDER = "hide_recently_played_folder";
	private static final String KEY_HIDE_LIVE_SUBTITLES_FOLDER = "hide_live_subtitles_folder";
	private static final String KEY_HIDE_MEDIA_LIBRARY_FOLDER = "hide_media_library_folder";
	private static final String KEY_HIDE_SUBS_INFO = "hide_subs_info";
	private static final String KEY_HIDE_TRANSCODE_FOLDER = "hide_transcode_folder";
	private static final String KEY_HIDE_VIDEO_SETTINGS = "hide_video_settings";
	private static final String KEY_HTTP_ENGINE_V2 = "http_engine_v2";
	private static final String KEY_IGNORE_THE_WORD_THE = "ignore_the_word_the";
	private static final String KEY_IGNORED_RENDERERS = "ignored_renderers";
	private static final String KEY_IMAGE_THUMBNAILS_ENABLED = "image_thumbnails";
	private static final String KEY_IP_FILTER = "ip_filter";
	private static final String KEY_ITUNES_LIBRARY_PATH = "itunes_library_path";
	private static final String KEY_SHOW_IPHOTO_LIBRARY = "show_iphoto_library";
	private static final String KEY_SHOW_ITUNES_LIBRARY = "show_itunes_library";
	private static final String KEY_LANGUAGE = "language";
	private static final String KEY_MAX_AUDIO_BUFFER = "maximum_audio_buffer_size";
	private static final String KEY_MAX_BITRATE = "maximum_bitrate";
	private static final String KEY_MEDIA_LIB_SORT = "media_lib_sort";
	private static final String KEY_MAX_MEMORY_BUFFER_SIZE = "maximum_video_buffer_size";
	private static final String KEY_MENCODER_ASS = "mencoder_ass";
	private static final String KEY_MENCODER_AC3_FIXED = "mencoder_ac3_fixed";
	private static final String KEY_MENCODER_CUSTOM_OPTIONS = "mencoder_custom_options";
	private static final String KEY_MENCODER_FONT_CONFIG = "mencoder_fontconfig";
	private static final String KEY_MENCODER_FORCE_FPS = "mencoder_forcefps";
	private static final String KEY_MENCODER_INTELLIGENT_SYNC = "mencoder_intelligent_sync";
	private static final String KEY_MENCODER_MAX_THREADS = "mencoder_max_threads";
	private static final String KEY_MENCODER_MT = "mencoder_mt";
	private static final String KEY_MENCODER_MUX_COMPATIBLE = "mencoder_mux_compatible";
	private static final String KEY_MENCODER_NOASS_BLUR = "mencoder_noass_blur";
	private static final String KEY_MENCODER_NOASS_OUTLINE = "mencoder_noass_outline";
	private static final String KEY_MENCODER_NOASS_SCALE = "mencoder_noass_scale";
	private static final String KEY_MENCODER_NOASS_SUBPOS = "mencoder_noass_subpos";
	private static final String KEY_MENCODER_NO_OUT_OF_SYNC = "mencoder_nooutofsync";
	private static final String KEY_MENCODER_NORMALIZE_VOLUME = "mencoder_normalize_volume";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT = "mencoder_overscan_compensation_height";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH = "mencoder_overscan_compensation_width";
	private static final String KEY_AUDIO_REMUX_AC3 = "audio_remux_ac3";
	private static final String KEY_MENCODER_REMUX_MPEG2 = "mencoder_remux_mpeg2";
	private static final String KEY_MENCODER_SCALER = "mencoder_scaler";
	private static final String KEY_MENCODER_SCALEX = "mencoder_scalex";
	private static final String KEY_MENCODER_SCALEY = "mencoder_scaley";
	private static final String KEY_MENCODER_SUB_FRIBIDI = "mencoder_subfribidi";
	private static final String KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY = "mencoder_usepcm_for_hq_audio_only";
	private static final String KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY = "mencoder_vobsub_subtitle_quality";
	private static final String KEY_MENCODER_YADIF = "mencoder_yadif";
	private static final String KEY_MIN_MEMORY_BUFFER_SIZE = "minimum_video_buffer_size";
	private static final String KEY_MIN_PLAY_TIME = "min_playtime";
	private static final String KEY_MIN_PLAY_TIME_WEB = "min_playtime_web";
	private static final String KEY_MIN_PLAY_TIME_FILE = "min_playtime_file";
	private static final String KEY_MIN_STREAM_BUFFER = "minimum_web_buffer_size";
	private static final String KEY_MINIMIZED = "minimized";
	private static final String KEY_MPEG2_MAIN_SETTINGS = "mpeg2_main_settings";
	private static final String KEY_MUX_ALLAUDIOTRACKS = "tsmuxer_mux_all_audiotracks";
	private static final String KEY_NETWORK_INTERFACE = "network_interface";
	private static final String KEY_HIDE_NEW_MEDIA_FOLDER = "hide_new_media_folder";
	private static final String KEY_DISABLE_TRANSCODE_FOR_EXTENSIONS = "disable_transcode_for_extensions";
	private static final String KEY_NUMBER_OF_CPU_CORES = "number_of_cpu_cores";
	private static final String KEY_OPEN_ARCHIVES = "enable_archive_browsing";
	private static final String KEY_LIVE_SUBTITLES_LIMIT = "live_subtitles_limit";
	private static final String KEY_LIVE_SUBTITLES_KEEP = "live_subtitles_keep";
	private static final String KEY_OVERSCAN = "mencoder_overscan";
	private static final String KEY_PING_PATH = "ping_path";
	private static final String KEY_PLUGIN_DIRECTORY = "plugins";
	private static final String KEY_PLUGIN_PURGE_ACTION = "plugin_purge";
	private static final String KEY_PREVENTS_SLEEP = "prevents_sleep_mode";
	private static final String KEY_PRETTIFY_FILENAMES = "prettify_filenames";
	private static final String KEY_PROFILE_NAME = "name";
	private static final String KEY_PROXY_SERVER_PORT = "proxy";
	private static final String KEY_RENDERER_DEFAULT = "renderer_default";
	private static final String KEY_RENDERER_FORCE_DEFAULT = "renderer_force_default";
	private static final String KEY_RESUME = "resume";
	private static final String KEY_RESUME_REWIND = "resume_rewind";
	private static final String KEY_RESUME_BACK = "resume_back";
	private static final String KEY_RESUME_KEEP_TIME = "resume_keep_time";
	private static final String KEY_RUN_WIZARD = "run_wizard";
	private static final String KEY_SCRIPT_DIR = "script_dir";
	private static final String KEY_SEARCH_FOLDER = "search_folder";
	private static final String KEY_SEARCH_IN_FOLDER = "search_in_folder";
	private static final String KEY_SEARCH_RECURSE = "search_recurse"; // legacy option
	private static final String KEY_SEARCH_RECURSE_DEPTH = "search_recurse_depth";
	private static final String KEY_SERVER_HOSTNAME = "hostname";
	private static final String KEY_SERVER_NAME = "server_name";
	private static final String KEY_SERVER_PORT = "port";
	private static final String KEY_SHARES = "shares";
	private static final String KEY_SINGLE = "single_instance";
	private static final String KEY_SKIP_LOOP_FILTER_ENABLED = "mencoder_skip_loop_filter";
	private static final String KEY_SKIP_NETWORK_INTERFACES = "skip_network_interfaces";
	private static final String KEY_SORT_METHOD = "sort_method";
	private static final String KEY_SORT_PATHS = "sort_paths";
	private static final String KEY_SPEED_DBG = "speed_debug";
	private static final String KEY_SUBS_COLOR = "subtitles_color";
	private static final String KEY_USE_EMBEDDED_SUBTITLES_STYLE = "use_embedded_subtitles_style";
	private static final String KEY_SUBTITLES_CODEPAGE = "subtitles_codepage";
	private static final String KEY_SUBTITLES_LANGUAGES = "subtitles_languages";
	private static final String KEY_TEMP_FOLDER_PATH = "temp_directory";
	private static final String KEY_THUMBNAIL_GENERATION_ENABLED = "generate_thumbnails";
	private static final String KEY_THUMBNAIL_SEEK_POS = "thumbnail_seek_position";
	private static final String KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS = "transcode_block_multiple_connections";
	private static final String KEY_TRANSCODE_FOLDER_NAME = "transcode_folder_name";
	private static final String KEY_TRANSCODE_KEEP_FIRST_CONNECTION = "transcode_keep_first_connection";
	private static final String KEY_TSMUXER_FORCEFPS = "tsmuxer_forcefps";
	private static final String KEY_UPNP_PORT = "upnp_port";
	private static final String KEY_USE_CACHE = "use_cache";
	private static final String KEY_USE_MPLAYER_FOR_THUMBS = "use_mplayer_for_video_thumbs";
	private static final String KEY_AUDIO_USE_PCM = "audio_use_pcm";
	private static final String KEY_UUID = "uuid";
	private static final String KEY_VIDEOTRANSCODE_START_DELAY = "videotranscode_start_delay";
	private static final String KEY_VIRTUAL_FOLDERS = "virtual_folders";
	private static final String KEY_VIRTUAL_FOLDERS_FILE = "virtual_folders_file";
	private static final String KEY_VLC_USE_HW_ACCELERATION = "vlc_use_hw_acceleration";
	private static final String KEY_VLC_USE_EXPERIMENTAL_CODECS = "vlc_use_experimental_codecs";
	private static final String KEY_VLC_AUDIO_SYNC_ENABLED = "vlc_audio_sync_enabled";
	private static final String KEY_VLC_SUBTITLE_ENABLED = "vlc_subtitle_enabled";
	private static final String KEY_VLC_SCALE = "vlc_scale";
	private static final String KEY_VLC_SAMPLE_RATE_OVERRIDE = "vlc_sample_rate_override";
	private static final String KEY_VLC_SAMPLE_RATE = "vlc_sample_rate";
	private static final String KEY_WEB_AUTHENTICATE = "web_authenticate";
	private static final String KEY_WEB_CONF_PATH = "web_conf";
	private static final String KEY_WEB_CONT_AUDIO = "web_continue_audio";
	private static final String KEY_WEB_CONT_IMAGE = "web_continue_image";
	private static final String KEY_WEB_CONT_VIDEO = "web_continue_video";
	private static final String KEY_WEB_ENABLE = "web_enable";
	private static final String KEY_WEB_IMAGE_SLIDE = "web_image_show_delay";
	private static final String KEY_WEB_LOOP_AUDIO = "web_loop_audio";
	private static final String KEY_WEB_LOOP_IMAGE = "web_loop_image";
	private static final String KEY_WEB_LOOP_VIDEO = "web_loop_video";
	private static final String KEY_WEB_MP4_TRANS = "web_mp4_trans";
	private static final String KEY_WEB_THREADS = "web_threads";
	private static final String KEY_WEB_PATH = "web_path";
	private static final String KEY_X264_CONSTANT_RATE_FACTOR = "x264_constant_rate_factor";

	// Deprecated settings
	@Deprecated
	private static final String KEY_MENCODER_ASS_DEFAULTSTYLE = "mencoder_ass_defaultstyle";

	// The name of the subdirectory under which UMS config files are stored for this build (default: UMS).
	// See Build for more details
	private static final String PROFILE_DIRECTORY_NAME = Build.getProfileDirectoryName();

	// The default profile name displayed on the renderer
	private static String HOSTNAME;

	private static String DEFAULT_AVI_SYNTH_SCRIPT;
	private static final int MAX_MAX_MEMORY_DEFAULT_SIZE = 400;
	private static final int BUFFER_MEMORY_FACTOR = 368;
	private static int MAX_MAX_MEMORY_BUFFER_SIZE = MAX_MAX_MEMORY_DEFAULT_SIZE;
	private static final char LIST_SEPARATOR = ',';
	private final PropertiesConfiguration configuration;
	private final ConfigurationReader configurationReader;
	private final TempFolder tempFolder;
	private final ProgramPaths programPaths;

	private final IpFilter filter = new IpFilter();

	/**
	 * The set of keys defining when the HTTP server has to restarted due to a configuration change
	 */
	public static final Set<String> NEED_RELOAD_FLAGS = new HashSet<>(
		Arrays.asList(
			KEY_ALTERNATE_THUMB_FOLDER,
			KEY_ATZ_LIMIT,
			KEY_AUDIO_THUMBNAILS_METHOD,
			KEY_CHAPTER_SUPPORT,
			KEY_DISABLE_TRANSCODE_FOR_EXTENSIONS,
			KEY_ENGINES,
			KEY_FOLDERS,
			KEY_FORCE_TRANSCODE_FOR_EXTENSIONS,
			KEY_HIDE_EMPTY_FOLDERS,
			KEY_HIDE_ENGINENAMES,
			KEY_HIDE_EXTENSIONS,
			KEY_HIDE_LIVE_SUBTITLES_FOLDER,
			KEY_HIDE_MEDIA_LIBRARY_FOLDER,
			KEY_HIDE_TRANSCODE_FOLDER,
			KEY_HIDE_VIDEO_SETTINGS,
			KEY_IGNORE_THE_WORD_THE,
			KEY_IP_FILTER,
			KEY_NETWORK_INTERFACE,
			KEY_OPEN_ARCHIVES,
			KEY_PRETTIFY_FILENAMES,
			KEY_SERVER_HOSTNAME,
			KEY_SERVER_NAME,
			KEY_SERVER_PORT,
			KEY_SHOW_APERTURE_LIBRARY,
			KEY_SHOW_IPHOTO_LIBRARY,
			KEY_SHOW_ITUNES_LIBRARY,
			KEY_SORT_METHOD,
			KEY_USE_CACHE
		)
	);

	/*
		The following code enables a single setting - UMS_PROFILE - to be used to
		initialize PROFILE_PATH i.e. the path to the current session's profile (AKA UMS.conf).
		It also initializes PROFILE_DIRECTORY - i.e. the directory the profile is located in -
		which is needed to detect the default WEB.conf location (anything else?).

		While this convention - and therefore PROFILE_DIRECTORY - will remain,
		adding more configurables - e.g. web_conf = ... - is on the TODO list.

		UMS_PROFILE is read (in this order) from the property ums.profile.path or the
		environment variable UMS_PROFILE. If UMS is launched with the command-line option
		"profiles" (e.g. from a shortcut), it displays a file chooser dialog that
		allows the ums.profile.path property to be set. This makes it easy to run UMS
		under multiple profiles without fiddling with environment variables, properties or
		command-line arguments.

		1) if UMS_PROFILE is not set, UMS.conf is located in: 

			Windows:             %ALLUSERSPROFILE%\$build
			Mac OS X:            $HOME/Library/Application Support/$build
			Everything else:     $HOME/.config/$build

		- where $build is a subdirectory that ensures incompatible UMS builds don't target/clobber
		the same configuration files. The default value for $build is "UMS". Other builds might use e.g.
		"UMS Rendr Edition" or "ums-mlx".

		2) if a relative or absolute *directory path* is supplied (the directory must exist),
		it is used as the profile directory and the profile is located there under the default profile name (UMS.conf):

			UMS_PROFILE = /absolute/path/to/dir
			UMS_PROFILE = relative/path/to/dir # relative to the working directory

		Amongst other things, this can be used to restore the legacy behaviour of locating UMS.conf in the current
		working directory e.g.:

			UMS_PROFILE=. ./UMS.sh

		3) if a relative or absolute *file path* is supplied (the file doesn't have to exist),
		it is taken to be the profile, and its parent dir is taken to be the profile (i.e. config file) dir:

			UMS_PROFILE = UMS.conf            # profile dir = .
			UMS_PROFILE = folder/dev.conf     # profile dir = folder
			UMS_PROFILE = /path/to/some.file  # profile dir = /path/to/
	 */
	private static final String DEFAULT_PROFILE_FILENAME = "UMS.conf";
	private static final String ENV_PROFILE_PATH = "UMS_PROFILE";
	private static final String DEFAULT_WEB_CONF_FILENAME = "WEB.conf";

	// Path to directory containing UMS config files
	private static final String PROFILE_DIRECTORY;

	// Absolute path to profile file e.g. /path/to/UMS.conf
	private static final String PROFILE_PATH;

	// Absolute path to WEB.conf file e.g. /path/to/WEB.conf
	private static String WEB_CONF_PATH;

	// Absolute path to skel (default) profile file e.g. /etc/skel/.config/universalmediaserver/UMS.conf
	// "project.skelprofile.dir" project property
	private static final String SKEL_PROFILE_PATH; 

	private static final String PROPERTY_PROFILE_PATH = "ums.profile.path";
	private static final String SYSTEM_PROFILE_DIRECTORY;

	static {
		// first of all, set up the path to the default system profile directory
		if (Platform.isWindows()) {
			String programData = System.getenv("ALLUSERSPROFILE");

			if (programData != null) {
				SYSTEM_PROFILE_DIRECTORY = String.format("%s\\%s", programData, PROFILE_DIRECTORY_NAME);
			} else {
				SYSTEM_PROFILE_DIRECTORY = ""; // i.e. current (working) directory
			}
		} else if (Platform.isMac()) {
			SYSTEM_PROFILE_DIRECTORY = String.format(
				"%s/%s/%s",
				System.getProperty("user.home"),
				"/Library/Application Support",
				PROFILE_DIRECTORY_NAME
			);
		} else {
			String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");

			if (xdgConfigHome == null) {
				SYSTEM_PROFILE_DIRECTORY = String.format("%s/.config/%s", System.getProperty("user.home"), PROFILE_DIRECTORY_NAME);
			} else {
				SYSTEM_PROFILE_DIRECTORY = String.format("%s/%s", xdgConfigHome, PROFILE_DIRECTORY_NAME);
			}
		}

		// now set the profile path. first: check for a custom setting.
		// try the system property, typically set via the profile chooser
		String customProfilePath = System.getProperty(PROPERTY_PROFILE_PATH);

		// failing that, try the environment variable
		if (StringUtils.isBlank(customProfilePath)) {
			customProfilePath = System.getenv(ENV_PROFILE_PATH);
		}

		// if customProfilePath is still blank, the default profile dir/filename is used
		FileLocation profileLocation = FileUtil.getFileLocation(
			customProfilePath,
			SYSTEM_PROFILE_DIRECTORY,
			DEFAULT_PROFILE_FILENAME
		);
		PROFILE_PATH = profileLocation.getFilePath();
		PROFILE_DIRECTORY = profileLocation.getDirectoryPath();

		// Set SKEL_PROFILE_PATH for Linux systems
		String skelDir = PropertiesUtil.getProjectProperties().get("project.skelprofile.dir");
		if (Platform.isLinux() && StringUtils.isNotBlank(skelDir)) {
			SKEL_PROFILE_PATH = FilenameUtils.normalize(
				new File(
					new File(
						skelDir,
						PROFILE_DIRECTORY_NAME
					).getAbsolutePath(),
					DEFAULT_PROFILE_FILENAME
				).getAbsolutePath()
			);
		} else {
			SKEL_PROFILE_PATH = null;
		}
	}

	/**
	 * Default constructor that will attempt to load the PMS configuration file
	 * from the profile path.
	 *
	 * @throws org.apache.commons.configuration.ConfigurationException
	 */
	public PmsConfiguration() throws ConfigurationException {
		this(true);
	}

	/**
	 * Constructor that will initialize the PMS configuration.
	 *
	 * @param loadFile Set to true to attempt to load the PMS configuration
	 *                 file from the profile path. Set to false to skip
	 *                 loading.
	 * @throws org.apache.commons.configuration.ConfigurationException
	 */
	public PmsConfiguration(boolean loadFile) throws ConfigurationException {
		configuration = new PropertiesConfiguration();
		configurationReader = new ConfigurationReader(configuration, true); // true: log
		configuration.setListDelimiter((char) 0);

		if (loadFile) {
			File pmsConfFile = new File(PROFILE_PATH);

			if (pmsConfFile.isFile()) {
				if (FileUtil.isFileReadable(pmsConfFile)) {
					configuration.load(PROFILE_PATH);
				} else {
					LOGGER.warn("Can't load {}", PROFILE_PATH);
				}
			} else if (SKEL_PROFILE_PATH != null) {
				File pmsSkelConfFile = new File(SKEL_PROFILE_PATH);

				if (pmsSkelConfFile.isFile()) {
					if (FileUtil.isFileReadable(pmsSkelConfFile)) {
						// Load defaults from skel file, save them later to PROFILE_PATH
						configuration.load(pmsSkelConfFile);
						LOGGER.info("Default configuration loaded from " + SKEL_PROFILE_PATH);
					} else {
						LOGGER.warn("Can't load {}", SKEL_PROFILE_PATH);
					}
				}
			}
		}

		configuration.setPath(PROFILE_PATH);

		tempFolder = new TempFolder(getString(KEY_TEMP_FOLDER_PATH, null));
		programPaths = createProgramPathsChain(configuration);
		Locale.setDefault(new Locale(getLanguage()));

		// Set DEFAULT_AVI_SYNTH_SCRIPT according to language
		DEFAULT_AVI_SYNTH_SCRIPT = "<movie>\n<sub>\n";

		long usableMemory = (Runtime.getRuntime().maxMemory() / 1048576) - BUFFER_MEMORY_FACTOR;
		if (usableMemory > MAX_MAX_MEMORY_DEFAULT_SIZE) {
			MAX_MAX_MEMORY_BUFFER_SIZE = (int) usableMemory;
		}
	}

	/**
	 * Check if we have disabled something first, then check the config file,
	 * then the Windows registry, then check for a platform-specific
	 * default.
	 */
	private static ProgramPaths createProgramPathsChain(Configuration configuration) {
		return new ConfigurationProgramPaths(
			configuration,
			new WindowsRegistryProgramPaths(
				new PlatformSpecificDefaultPathsFactory().get()
			)
		);
	}

	/**
	 * Return the <code>int</code> value for a given configuration key. First, the key
	 * is looked up in the current configuration settings. If it exists and contains a
	 * valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private int getInt(String key, int def) {
		return configurationReader.getInt(key, def);
	}

	/**
	 * Return the <code>boolean</code> value for a given configuration key. First, the
	 * key is looked up in the current configuration settings. If it exists and contains
	 * a valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private boolean getBoolean(String key, boolean def) {
		return configurationReader.getBoolean(key, def);
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

	/**
	 * Return a <code>List</code> of <code>String</code> values for a given configuration
	 * key. First, the key is looked up in the current configuration settings. If it
	 * exists and contains a valid value, that value is returned. If the key contains an
	 * invalid value or cannot be found, a list with the specified default values is
	 * returned.
	 * @param key The key to look up.
	 * @param def The default values to return when no valid key value can be found.
	 *            These values should be entered as a comma-separated string. Whitespace
	 *            will be trimmed. For example: <code>"gnu,    gnat  ,moo "</code> will be
	 *            returned as <code>{ "gnu", "gnat", "moo" }</code>.
	 * @return The list of value strings configured for the key.
	 */
	private List<String> getStringList(String key, String def) {
		return configurationReader.getStringList(key, def);
	}

	public File getTempFolder() throws IOException {
		return tempFolder.getTempFolder();
	}

	public String getVlcPath() {
		return programPaths.getVlcPath();
	}

	public String getMencoderPath() {
		return programPaths.getMencoderPath();
	}

	public int getMencoderMaxThreads() {
		return Math.min(getInt(KEY_MENCODER_MAX_THREADS, getNumberOfCpuCores()), MENCODER_MAX_THREADS);
	}

	public String getDCRawPath() {
		return programPaths.getDCRaw();
	}

	public String getFfmpegPath() {
		return programPaths.getFfmpegPath();
	}

	public String getMplayerPath() {
		return programPaths.getMplayerPath();
	}

	public String getTsmuxerPath() {
		return programPaths.getTsmuxerPath();
	}

	public String getTsmuxerNewPath() {
		return programPaths.getTsmuxerNewPath();
	}

	public String getFlacPath() {
		return programPaths.getFlacPath();
	}

	public String getInterFramePath() {
		return programPaths.getInterFramePath();
	}

	/**
	 * If the framerate is not recognized correctly and the video runs too fast or too
	 * slow, tsMuxeR can be forced to parse the fps from FFmpeg. Default value is true.
	 * @return True if tsMuxeR should parse fps from FFmpeg.
	 */
	public boolean isTsmuxerForceFps() {
		return getBoolean(KEY_TSMUXER_FORCEFPS, true);
	}

	/**
	 * The AC-3 audio bitrate determines the quality of digital audio sound. An AV-receiver
	 * or amplifier has to be capable of playing this quality. Default value is 640.
	 * @return The AC-3 audio bitrate.
	 */
	public int getAudioBitrate() {
		return getInt(KEY_AUDIO_BITRATE, 640);
	}

	/**
	 * If the framerate is not recognized correctly and the video runs too fast or too
	 * slow, tsMuxeR can be forced to parse the fps from FFmpeg.
	 * @param value Set to true if tsMuxeR should parse fps from FFmpeg.
	 */
	public void setTsmuxerForceFps(boolean value) {
		configuration.setProperty(KEY_TSMUXER_FORCEFPS, value);
	}

	/**
	 * The server port where PMS listens for TCP/IP traffic. Default value is 5001.
	 * @return The port number.
	 */
	public int getServerPort() {
		return getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
	}

	/**
	 * Set the server port where PMS must listen for TCP/IP traffic.
	 * @param value The TCP/IP port number.
	 */
	public void setServerPort(int value) {
		configuration.setProperty(KEY_SERVER_PORT, value);
	}

	/**
	 * The hostname of the server.
	 * @return The hostname if it is defined, otherwise <code>null</code>.
	 */
	public String getServerHostname() {
		return getString(KEY_SERVER_HOSTNAME, null);
	}

	/**
	 * Set the hostname of the server.
	 * @param value The hostname.
	 */
	public void setHostname(String value) {
		configuration.setProperty(KEY_SERVER_HOSTNAME, value);
	}

	/**
	 * The name of the server.
	 *
	 * @return The name of the server.
	 */
	public String getServerName() {
		return getString(KEY_SERVER_NAME, "Universal Media Server");
	}

	/**
	 * Set the name of the server.
	 *
	 * @param value The name.
	 */
	public void setServerName(String value) {
		configuration.setProperty(KEY_SERVER_NAME, value);
	}

	/**
	 * The TCP/IP port number for a proxy server. Default value is -1.
	 *
	 * @return The proxy port number.
	 */
	// no longer used
	@Deprecated
	public int getProxyServerPort() {
		return getInt(KEY_PROXY_SERVER_PORT, DEFAULT_PROXY_SERVER_PORT);
	}

	/**
	 * Get the code of the preferred language for the PMS user interface. Default
	 * is based on the locale.
	 * @return The ISO 639 language code.
	 */
	public String getLanguage() {
		String def = Locale.getDefault().getLanguage();

		if (def == null) {
			def = "en";
		}

		return getString(KEY_LANGUAGE, def);
	}

	/**
	 * Returns the preferred minimum size for the transcoding memory buffer in megabytes.
	 * Default value is 12.
	 * @return The minimum memory buffer size.
	 */
	public int getMinMemoryBufferSize() {
		return getInt(KEY_MIN_MEMORY_BUFFER_SIZE, 12);
	}

	/**
	 * Returns the preferred maximum size for the transcoding memory buffer in megabytes.
	 * The value returned has a top limit of {@link #MAX_MAX_MEMORY_BUFFER_SIZE}. Default
	 * value is 200.
	 *
	 * @return The maximum memory buffer size.
	 */
	public int getMaxMemoryBufferSize() {
		return Math.max(0, Math.min(MAX_MAX_MEMORY_BUFFER_SIZE, getInt(KEY_MAX_MEMORY_BUFFER_SIZE, 200)));
	}

	/**
	 * Set the preferred maximum for the transcoding memory buffer in megabytes. The top
	 * limit for the value is {@link #MAX_MAX_MEMORY_BUFFER_SIZE}.
	 *
	 * @param value The maximum buffer size.
	 */
	public void setMaxMemoryBufferSize(int value) {
		configuration.setProperty(KEY_MAX_MEMORY_BUFFER_SIZE, Math.max(0, Math.min(MAX_MAX_MEMORY_BUFFER_SIZE, value)));
	}

	/**
	 * Returns the font scale used for ASS subtitling. Default value is 1.4.
	 * @return The ASS font scale.
	 */
	public String getAssScale() {
		return getString(KEY_ASS_SCALE, "1.4");
	}

	/**
	 * Some versions of MEncoder produce garbled audio because the "ac3" codec is used
	 * instead of the "ac3_fixed" codec. Returns true if "ac3_fixed" should be used.
	 * Default is false.
	 * See https://code.google.com/p/ps3mediaserver/issues/detail?id=1092#c1
	 * @return True if "ac3_fixed" should be used.
	 */
	public boolean isMencoderAc3Fixed() {
		return getBoolean(KEY_MENCODER_AC3_FIXED, false);
	}

	/**
	 * Returns the margin used for ASS subtitling. Default value is 10.
	 * @return The ASS margin.
	 */
	public String getAssMargin() {
		return getString(KEY_ASS_MARGIN, "10");
	}

	/**
	 * Returns the outline parameter used for ASS subtitling. Default value is 1.
	 * @return The ASS outline parameter.
	 */
	public String getAssOutline() {
		return getString(KEY_ASS_OUTLINE, "1");
	}

	/**
	 * Returns the shadow parameter used for ASS subtitling. Default value is 1.
	 * @return The ASS shadow parameter.
	 */
	public String getAssShadow() {
		return getString(KEY_ASS_SHADOW, "1");
	}

	/**
	 * Returns the subfont text scale parameter used for subtitling without ASS.
	 * Default value is 3.
	 * @return The subfont text scale parameter.
	 */
	public String getMencoderNoAssScale() {
		return getString(KEY_MENCODER_NOASS_SCALE, "3");
	}

	/**
	 * Returns the subpos parameter used for subtitling without ASS.
	 * Default value is 2.
	 * @return The subpos parameter.
	 */
	public String getMencoderNoAssSubPos() {
		return getString(KEY_MENCODER_NOASS_SUBPOS, "2");
	}

	/**
	 * Returns the subfont blur parameter used for subtitling without ASS.
	 * Default value is 1.
	 * @return The subfont blur parameter.
	 */
	public String getMencoderNoAssBlur() {
		return getString(KEY_MENCODER_NOASS_BLUR, "1");
	}

	/**
	 * Returns the subfont outline parameter used for subtitling without ASS.
	 * Default value is 1.
	 * @return The subfont outline parameter.
	 */
	public String getMencoderNoAssOutline() {
		return getString(KEY_MENCODER_NOASS_OUTLINE, "1");
	}

	/**
	 * Set the subfont outline parameter used for subtitling without ASS.
	 * @param value The subfont outline parameter value to set.
	 */
	public void setMencoderNoAssOutline(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_OUTLINE, value);
	}

	/**
	 * Some versions of MEncoder produce garbled audio because the "ac3" codec is used
	 * instead of the "ac3_fixed" codec.
	 * See https://code.google.com/p/ps3mediaserver/issues/detail?id=1092#c1
	 * @param value Set to true if "ac3_fixed" should be used.
	 */
	public void setMencoderAc3Fixed(boolean value) {
		configuration.setProperty(KEY_MENCODER_AC3_FIXED, value);
	}

	/**
	 * Set the margin used for ASS subtitling.
	 * @param value The ASS margin value to set.
	 */
	public void setAssMargin(String value) {
		configuration.setProperty(KEY_ASS_MARGIN, value);
	}

	/**
	 * Set the outline parameter used for ASS subtitling.
	 * @param value The ASS outline parameter value to set.
	 */
	public void setAssOutline(String value) {
		configuration.setProperty(KEY_ASS_OUTLINE, value);
	}

	/**
	 * Set the shadow parameter used for ASS subtitling.
	 * @param value The ASS shadow parameter value to set.
	 */
	public void setAssShadow(String value) {
		configuration.setProperty(KEY_ASS_SHADOW, value);
	}

	/**
	 * Set the font scale used for ASS subtitling.
	 * @param value The ASS font scale value to set.
	 */
	public void setAssScale(String value) {
		configuration.setProperty(KEY_ASS_SCALE, value);
	}

	/**
	 * Set the subfont text scale parameter used for subtitling without ASS.
	 * @param value The subfont text scale parameter value to set.
	 */
	public void setMencoderNoAssScale(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_SCALE, value);
	}

	/**
	 * Set the subfont blur parameter used for subtitling without ASS.
	 * @param value The subfont blur parameter value to set.
	 */
	public void setMencoderNoAssBlur(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_BLUR, value);
	}

	/**
	 * Set the subpos parameter used for subtitling without ASS.
	 * @param value The subpos parameter value to set.
	 */
	public void setMencoderNoAssSubPos(String value) {
		configuration.setProperty(KEY_MENCODER_NOASS_SUBPOS, value);
	}

	/**
	 * Set the maximum number of concurrent MEncoder threads.
	 * XXX Currently unused.
	 * @param value The maximum number of concurrent threads.
	 */
	public void setMencoderMaxThreads(int value) {
		configuration.setProperty(KEY_MENCODER_MAX_THREADS, value);
	}

	/**
	 * Set the preferred language for the PMS user interface.
	 * @param value The ISO 639 language code.
	 */
	public void setLanguage(String value) {
		configuration.setProperty(KEY_LANGUAGE, value);
		Locale.setDefault(new Locale(getLanguage()));
	}

	/**
	 * Returns the number of seconds from the start of a video file (the seek
	 * position) where the thumbnail image for the movie should be extracted
	 * from. Default is 2 seconds.
	 * @return The seek position in seconds.
	 */
	public int getThumbnailSeekPos() {
		return getInt(KEY_THUMBNAIL_SEEK_POS, 2);
	}

	/**
	 * Sets the number of seconds from the start of a video file (the seek
	 * position) where the thumbnail image for the movie should be extracted
	 * from.
	 * @param value The seek position in seconds.
	 */
	public void setThumbnailSeekPos(int value) {
		configuration.setProperty(KEY_THUMBNAIL_SEEK_POS, value);
	}

	/**
	 * Returns whether the user wants ASS/SSA subtitle support. Default is
	 * true.
	 *
	 * @return True if MEncoder should use ASS/SSA support.
	 */
	public boolean isMencoderAss() {
		return getBoolean(KEY_MENCODER_ASS, true);
	}

	/**
	 * Returns whether or not subtitles should be disabled for all
	 * transcoding engines. Default is false, meaning subtitles should not
	 * be disabled.
	 *
	 * @return True if subtitles should be disabled, false otherwise.
	 */
	public boolean isDisableSubtitles() {
		return getBoolean(KEY_DISABLE_SUBTITLES, false);
	}

	/**
	 * Set whether or not subtitles should be disabled for
	 * all transcoding engines.
	 *
	 * @param value Set to true if subtitles should be disabled.
	 */
	public void setDisableSubtitles(boolean value) {
		configuration.setProperty(KEY_DISABLE_SUBTITLES, value);
	}

	/**
	 * Returns whether or not the Pulse Code Modulation audio format should be
	 * forced. The default is false.
	 * @return True if PCM should be forced, false otherwise.
	 */
	public boolean isAudioUsePCM() {
		return getBoolean(KEY_AUDIO_USE_PCM, false);
	}

	/**
	 * Returns whether or not the Pulse Code Modulation audio format should be
	 * used only for HQ audio codecs. The default is false.
	 * @return True if PCM should be used only for HQ audio codecs, false otherwise.
	 */
	public boolean isMencoderUsePcmForHQAudioOnly() {
		return getBoolean(KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY, false);
	}

	/**
	 * Returns the name of a TrueType font to use for subtitles.
	 * Default is <code>""</code>.
	 * @return The font name.
	 */
	public String getFont() {
		return getString(KEY_FONT, "");
	}

	/**
	 * Returns the audio language priority as a comma separated
	 * string. For example: <code>"eng,fre,jpn,ger,und"</code>, where "und"
	 * stands for "undefined".
	 * Can be a blank string.
	 * Default value is "loc,eng,fre,jpn,ger,und".
	 *
	 * @return The audio language priority string.
	 */
	public String getAudioLanguages() {
		return configurationReader.getPossiblyBlankConfigurationString(
				KEY_AUDIO_LANGUAGES,
				Messages.getString("MEncoderVideo.126")
		);
	}

	/**
	 * Returns the subtitle language priority as a comma-separated
	 * string. For example: <code>"eng,fre,jpn,ger,und"</code>, where "und"
	 * stands for "undefined".
	 * Can be a blank string.
	 * Default value is a localized list (e.g. "eng,fre,jpn,ger,und").
	 *
	 * @return The subtitle language priority string.
	 */
	public String getSubtitlesLanguages() {
		return configurationReader.getPossiblyBlankConfigurationString(
				KEY_SUBTITLES_LANGUAGES,
				Messages.getString("MEncoderVideo.127")
		);
	}

	/**
	 * Returns the ISO 639 language code for the subtitle language that should
	 * be forced.
	 * Can be a blank string.
	 * @return The subtitle language code.
	 */
	public String getForcedSubtitleLanguage() {
		return configurationReader.getPossiblyBlankConfigurationString(
				KEY_FORCED_SUBTITLE_LANGUAGE,
				getLanguage()
		);
	}

	/**
	 * Returns the tag string that identifies the subtitle language that
	 * should be forced.
	 * @return The tag string.
	 */
	public String getForcedSubtitleTags() {
		return getString(KEY_FORCED_SUBTITLE_TAGS, "forced");
	}

	/**
	 * Returns a string of audio language and subtitle language pairs
	 * ordered by priority to try to match. Audio language
	 * and subtitle language should be comma separated as a pair,
	 * individual pairs should be semicolon separated. "*" can be used to
	 * match any language. Subtitle language can be defined as "off".
	 * Default value is <code>"*,*"</code>.
	 *
	 * @return The audio and subtitle languages priority string.
	 */
	public String getAudioSubLanguages() {
		return configurationReader.getPossiblyBlankConfigurationString(
				KEY_AUDIO_SUB_LANGS,
				Messages.getString("MEncoderVideo.128")
		);
	}

	/**
	 * Returns whether or not MEncoder should use FriBiDi mode, which
	 * is needed to display subtitles in languages that read from right to
	 * left, like Arabic, Farsi, Hebrew, Urdu, etc. Default value is false.
	 * @return True if FriBiDi mode should be used, false otherwise.
	 */
	public boolean isMencoderSubFribidi() {
		return getBoolean(KEY_MENCODER_SUB_FRIBIDI, false);
	}

	/**
	 * Returns the character encoding (or code page) that should used
	 * for displaying non-Unicode external subtitles. Default is empty string
	 * (do not force encoding with -subcp key).
	 * @return The character encoding.
	 */
	public String getSubtitlesCodepage() {
		return getString(KEY_SUBTITLES_CODEPAGE, "");
	}

	/**
	 * Returns whether or not MEncoder should use fontconfig for displaying
	 * subtitles. Default is false.
	 * @return True if fontconfig should be used, false otherwise.
	 */
	public boolean isMencoderFontConfig() {
		return getBoolean(KEY_MENCODER_FONT_CONFIG, true);
	}

	/**
	 * Set to true if MEncoder should be forced to use the framerate that is
	 * parsed by FFmpeg.
	 * @param value Set to true if the framerate should be forced, false
	 *              otherwise.
	 */
	public void setMencoderForceFps(boolean value) {
		configuration.setProperty(KEY_MENCODER_FORCE_FPS, value);
	}

	/**
	 * Returns true if MEncoder should be forced to use the framerate that is
	 * parsed by FFmpeg.
	 * @return True if the framerate should be forced, false otherwise.
	 */
	public boolean isMencoderForceFps() {
		return getBoolean(KEY_MENCODER_FORCE_FPS, false);
	}

	/**
	 * Sets the audio language priority as a comma separated
	 * string. For example: <code>"eng,fre,jpn,ger,und"</code>, where "und"
	 * stands for "undefined".
	 * @param value The audio language priority string.
	 */
	public void setAudioLanguages(String value) {
		configuration.setProperty(KEY_AUDIO_LANGUAGES, value);
	}

	/**
	 * Sets the subtitle language priority as a comma
	 * separated string. For example: <code>"eng,fre,jpn,ger,und"</code>,
	 * where "und" stands for "undefined".
	 * @param value The subtitle language priority string.
	 */
	public void setSubtitlesLanguages(String value) {
		configuration.setProperty(KEY_SUBTITLES_LANGUAGES, value);
	}

	/**
	 * Sets the ISO 639 language code for the subtitle language that should
	 * be forced.
	 * @param value The subtitle language code.
	 */
	public void setForcedSubtitleLanguage(String value) {
		configuration.setProperty(KEY_FORCED_SUBTITLE_LANGUAGE, value);
	}

	/**
	 * Sets the tag string that identifies the subtitle language that
	 * should be forced.
	 * @param value The tag string.
	 */
	public void setForcedSubtitleTags(String value) {
		configuration.setProperty(KEY_FORCED_SUBTITLE_TAGS, value);
	}

	/**
	 * Sets a string of audio language and subtitle language pairs
	 * ordered by priority to try to match. Audio language
	 * and subtitle language should be comma separated as a pair,
	 * individual pairs should be semicolon separated. "*" can be used to
	 * match any language. Subtitle language can be defined as "off". For
	 * example: <code>"en,off;jpn,eng;*,eng;*;*"</code>.
	 * @param value The audio and subtitle languages priority string.
	 */
	public void setAudioSubLanguages(String value) {
		configuration.setProperty(KEY_AUDIO_SUB_LANGS, value);
	}

	/**
	 * Returns custom commandline options to pass on to MEncoder.
	 * @return The custom options string.
	 */
	public String getMencoderCustomOptions() {
		return getString(KEY_MENCODER_CUSTOM_OPTIONS, "");
	}

	/**
	 * Sets custom commandline options to pass on to MEncoder.
	 * @param value The custom options string.
	 */
	public void setMencoderCustomOptions(String value) {
		configuration.setProperty(KEY_MENCODER_CUSTOM_OPTIONS, value);
	}

	/**
	 * Sets the character encoding (or code page) that should be used
	 * for displaying non-Unicode external subtitles. Default is empty (autodetect).
	 * @param value The character encoding.
	 */
	public void setSubtitlesCodepage(String value) {
		configuration.setProperty(KEY_SUBTITLES_CODEPAGE, value);
	}

	/**
	 * Sets whether or not MEncoder should use FriBiDi mode, which
	 * is needed to display subtitles in languages that read from right to
	 * left, like Arabic, Farsi, Hebrew, Urdu, etc. Default value is false.
	 * @param value Set to true if FriBiDi mode should be used.
	 */
	public void setMencoderSubFribidi(boolean value) {
		configuration.setProperty(KEY_MENCODER_SUB_FRIBIDI, value);
	}

	/**
	 * Sets the name of a TrueType font to use for subtitles.
	 * @param value The font name.
	 */
	public void setFont(String value) {
		configuration.setProperty(KEY_FONT, value);
	}

	/**
	 * Older versions of MEncoder do not support ASS/SSA subtitles on all
	 * platforms. Set to true if MEncoder supports them. Default should be
	 * true on Windows and OS X, false otherwise.
	 * See https://code.google.com/p/ps3mediaserver/issues/detail?id=1097
	 * @param value Set to true if MEncoder supports ASS/SSA subtitles.
	 */
	public void setMencoderAss(boolean value) {
		configuration.setProperty(KEY_MENCODER_ASS, value);
	}

	/**
	 * Sets whether or not MEncoder should use fontconfig for displaying
	 * subtitles.
	 * @param value Set to true if fontconfig should be used.
	 */
	public void setMencoderFontConfig(boolean value) {
		configuration.setProperty(KEY_MENCODER_FONT_CONFIG, value);
	}

	/**
	 * Sets whether or not the Pulse Code Modulation audio format should be
	 * forced.
	 * @param value Set to true if PCM should be forced.
	 */
	public void setAudioUsePCM(boolean value) {
		configuration.setProperty(KEY_AUDIO_USE_PCM, value);
	}

	/**
	 * Sets whether or not the Pulse Code Modulation audio format should be
	 * used only for HQ audio codecs.
	 * @param value Set to true if PCM should be used only for HQ audio.
	 */
	public void setMencoderUsePcmForHQAudioOnly(boolean value) {
		configuration.setProperty(KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY, value);
	}

	/**
	 * Returns true if archives (e.g. .zip or .rar) should be browsable by
	 * PMS, false otherwise.
	 * @return True if archives should be browsable.
	 */
	public boolean isArchiveBrowsing() {
		return getBoolean(KEY_OPEN_ARCHIVES, false);
	}

	/**
	 * Set to true if archives (e.g. .zip or .rar) should be browsable by
	 * PMS, false otherwise.
	 * @param value Set to true if archives should be browsable.
	 */
	public void setArchiveBrowsing(boolean value) {
		configuration.setProperty(KEY_OPEN_ARCHIVES, value);
	}

	/**
	 * Returns true if MEncoder should use the deinterlace filter, false
	 * otherwise.
	 * @return True if the deinterlace filter should be used.
	 */
	public boolean isMencoderYadif() {
		return getBoolean(KEY_MENCODER_YADIF, false);
	}

	/**
	 * Set to true if MEncoder should use the deinterlace filter, false
	 * otherwise.
	 * @param value Set ot true if the deinterlace filter should be used.
	 */
	public void setMencoderYadif(boolean value) {
		configuration.setProperty(KEY_MENCODER_YADIF, value);
	}

	/**
	 * Returns true if MEncoder should be used to upscale the video to an
	 * optimal resolution. Default value is false, meaning the renderer will
	 * upscale the video itself.
	 *
	 * @return True if MEncoder should be used, false otherwise.
	 * @see #getMencoderScaleX()
	 * @see #getMencoderScaleY()
	 */
	public boolean isMencoderScaler() {
		return getBoolean(KEY_MENCODER_SCALER, false);
	}

	/**
	 * Set to true if MEncoder should be used to upscale the video to an
	 * optimal resolution. Set to false to leave upscaling to the renderer.
	 *
	 * @param value Set to true if MEncoder should be used to upscale.
	 * @see #setMencoderScaleX(int)
	 * @see #setMencoderScaleY(int)
	 */
	public void setMencoderScaler(boolean value) {
		configuration.setProperty(KEY_MENCODER_SCALER, value);
	}

	/**
	 * Returns the width in pixels to which a video should be scaled when
	 * {@link #isMencoderScaler()} returns true.
	 *
	 * @return The width in pixels.
	 */
	public int getMencoderScaleX() {
		return getInt(KEY_MENCODER_SCALEX, 0);
	}

	/**
	 * Sets the width in pixels to which a video should be scaled when
	 * {@link #isMencoderScaler()} returns true.
	 *
	 * @param value The width in pixels.
	 */
	public void setMencoderScaleX(int value) {
		configuration.setProperty(KEY_MENCODER_SCALEX, value);
	}

	/**
	 * Returns the height in pixels to which a video should be scaled when
	 * {@link #isMencoderScaler()} returns true.
	 *
	 * @return The height in pixels.
	 */
	public int getMencoderScaleY() {
		return getInt(KEY_MENCODER_SCALEY, 0);
	}

	/**
	 * Sets the height in pixels to which a video should be scaled when
	 * {@link #isMencoderScaler()} returns true.
	 *
	 * @param value The height in pixels.
	 */
	public void setMencoderScaleY(int value) {
		configuration.setProperty(KEY_MENCODER_SCALEY, value);
	}

	/**
	 * Returns the number of audio channels that should be used for
	 * transcoding. Default value is 6 (for 5.1 audio).
	 *
	 * @return The number of audio channels.
	 */
	public int getAudioChannelCount() {
		int valueFromUserConfig = getInt(KEY_AUDIO_CHANNEL_COUNT, 6);

		if (valueFromUserConfig != 6 && valueFromUserConfig != 2) {
			return 6;
		}

		return valueFromUserConfig;
	}

	/**
	 * Sets the number of audio channels that MEncoder should use for
	 * transcoding.
	 *
	 * @param value The number of audio channels.
	 */
	public void setAudioChannelCount(int value) {
		if (value != 6 && value != 2) {
			value = 6;
		}
		configuration.setProperty(KEY_AUDIO_CHANNEL_COUNT, value);
	}

	/**
	 * Sets the AC3 audio bitrate, which determines the quality of digital
	 * audio sound. An AV-receiver or amplifier has to be capable of playing
	 * this quality.
	 *
	 * @param value The AC3 audio bitrate.
	 */
	public void setAudioBitrate(int value) {
		configuration.setProperty(KEY_AUDIO_BITRATE, value);
	}

	/**
	 * Returns the maximum video bitrate to be used by MEncoder and FFmpeg.
	 *
	 * @return The maximum video bitrate.
	 */
	public String getMaximumBitrate() {
		String maximumBitrate = getMaximumBitrateDisplay();
		if ("0".equals(maximumBitrate)) {
			maximumBitrate = "1000";
		}
		return maximumBitrate;
	}

	/**
	 * The same as getMaximumBitrate() but this value is displayed to the user
	 * because for our own uses we turn the value "0" into the value "1000" but
	 * that can be confusing for the user.
	 *
	 * @return The maximum video bitrate to display in the GUI.
	 */
	public String getMaximumBitrateDisplay() {
		return getString(KEY_MAX_BITRATE, "90");
	}

	/**
	 * Sets the maximum video bitrate to be used by MEncoder.
	 *
	 * @param value The maximum video bitrate.
	 */
	public void setMaximumBitrate(String value) {
		configuration.setProperty(KEY_MAX_BITRATE, value);
	}

	/**
	 * @return The ignored renderers as a list.
	 */
	public List<String> getIgnoredRenderers() {
		return getStringList(KEY_IGNORED_RENDERERS, "");
	}

	/**
	 * @param value The comma-separated list of ignored renderers.
	 */
	public void setIgnoredRenderers(String value) {
		configuration.setProperty(KEY_IGNORED_RENDERERS, value);
	}

	/**
	 * Returns true if thumbnail generation is enabled, false otherwise.
	 *
	 * @return boolean indicating whether thumbnail generation is enabled.
	 */
	public boolean isThumbnailGenerationEnabled() {
		return getBoolean(KEY_THUMBNAIL_GENERATION_ENABLED, true);
	}

	/**
	 * Sets the thumbnail generation option.
	 */
	public void setThumbnailGenerationEnabled(boolean value) {
		configuration.setProperty(KEY_THUMBNAIL_GENERATION_ENABLED, value);
	}

	/**
	 * Returns true if PMS should generate thumbnails for images. Default value
	 * is true.
	 *
	 * @return True if image thumbnails should be generated.
	 */
	public boolean getImageThumbnailsEnabled() {
		return getBoolean(KEY_IMAGE_THUMBNAILS_ENABLED, true);
	}

	/**
	 * Set to true if PMS should generate thumbnails for images.
	 *
	 * @param value True if image thumbnails should be generated.
	 */
	public void setImageThumbnailsEnabled(boolean value) {
		configuration.setProperty(KEY_IMAGE_THUMBNAILS_ENABLED, value);
	}

	/**
	 * Returns the number of CPU cores that should be used for transcoding.
	 *
	 * @return The number of CPU cores.
	 */
	public int getNumberOfCpuCores() {
		int nbcores = Runtime.getRuntime().availableProcessors();
		if (nbcores < 1) {
			nbcores = 1;
		}
		return getInt(KEY_NUMBER_OF_CPU_CORES, nbcores);
	}

	/**
	 * Sets the number of CPU cores that should be used for transcoding. The
	 * maximum value depends on the physical available count of "real processor
	 * cores". That means hyperthreading virtual CPU cores do not count! If you
	 * are not sure, analyze your CPU with the free tool CPU-z on Windows
	 * systems. On Linux have a look at the virtual proc-filesystem: in the
	 * file "/proc/cpuinfo" you will find more details about your CPU. You also
	 * get much information about CPUs from AMD and Intel from their Wikipedia
	 * articles.
	 * <p>
	 * PMS will detect and set the correct amount of cores as the default value.
	 *
	 * @param value The number of CPU cores.
	 */
	public void setNumberOfCpuCores(int value) {
		configuration.setProperty(KEY_NUMBER_OF_CPU_CORES, value);
	}

	/**
	 * Returns true if PMS should start minimized, i.e. without its window
	 * opened. Default value false: to start with a window.
	 *
	 * @return True if PMS should start minimized, false otherwise.
	 */
	public boolean isMinimized() {
		return getBoolean(KEY_MINIMIZED, false);
	}

	/**
	 * Set to true if PMS should start minimized, i.e. without its window
	 * opened.
	 *
	 * @param value True if PMS should start minimized, false otherwise.
	 */
	public void setMinimized(boolean value) {
		configuration.setProperty(KEY_MINIMIZED, value);
	}

	/**
	 * Returns true if UMS should automatically start on Windows.
	 *
	 * @return True if UMS should start automatically, false otherwise.
	 */
	public boolean isAutoStart() {
		if (Platform.isWindows()) {
			File f = new File(WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Common Startup") + "\\Universal Media Server.lnk");

			if (f.exists()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Set to true if UMS should automatically start on Windows.
	 *
	 * @param value True if UMS should start automatically, false otherwise.
	 */
	public void setAutoStart(boolean value) {
		File sourceFile = new File(WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Common Programs") + "\\Universal Media Server.lnk");
		File destinationFile = new File(WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Common Startup") + "\\Universal Media Server.lnk");

		if (value) {
			try {
				FileUtils.copyFile(sourceFile, destinationFile);
				if (destinationFile.exists()) {
					LOGGER.info("UMS will start automatically with Windows");
				} else {
					LOGGER.info("An error occurred while trying to make UMS start automatically with Windows");
				}
			} catch (IOException e) {
				if (!isAdmin()) {
					try {
						JOptionPane.showMessageDialog(
							(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
							Messages.getString("NetworkTab.58"),
							Messages.getString("Dialog.PermissionsError"),
							JOptionPane.ERROR_MESSAGE
						);
					} catch (NullPointerException e2) {
						// This happens on the initial program load, ignore it
					}
				} else {
					LOGGER.info("An error occurred while trying to make UMS start automatically with Windows");
				}
			}
		} else {
			if (destinationFile.delete()) {
				LOGGER.info("UMS will not start automatically with Windows");
			} else {
				LOGGER.info("An error occurred while trying to make UMS not start automatically with Windows");
			}
		}
	}

	/**
	 * Whether we should check for external subtitle files with the same
	 * name as the media (*.srt, *.sub, *.ass, etc.).
	 *
	 * Note: This will return true if either the autoload external subtitles
	 * setting is enabled or the force external subtitles setting is enabled
	 *
	 * @return Whether we should check for external subtitle files.
	 */
	public boolean isAutoloadExternalSubtitles() {
		return getBoolean(KEY_AUTOLOAD_SUBTITLES, true) || isForceExternalSubtitles();
	}

	/**
	 * Whether we should check for external subtitle files with the same
	 * name as the media (*.srt, *.sub, *.ass etc.).
	 *
	 * @param value Whether we should check for external subtitle files.
	 */
	public void setAutoloadExternalSubtitles(boolean value) {
		configuration.setProperty(KEY_AUTOLOAD_SUBTITLES, value);
	}

	/**
	 * Whether we should force external subtitles with the same name as the
	 * media (*.srt, *.sub, *.ass, etc.) to display, regardless of whether
	 * language preferences disable them.
	 *
	 * @return Whether we should force external subtitle files.
	 */
	public boolean isForceExternalSubtitles() {
		return getBoolean(KEY_FORCE_EXTERNAL_SUBTITLES, true);
	}

	/**
	 * Whether we should force external subtitles with the same name as the
	 * media (*.srt, *.sub, *.ass, etc.) to display, regardless of whether
	 * language preferences disable them.
	 *
	 * @param value Whether we should force external subtitle files.
	 */
	public void setForceExternalSubtitles(boolean value) {
		configuration.setProperty(KEY_FORCE_EXTERNAL_SUBTITLES, value);
	}

	/**
	 * Returns true if PMS should hide the "# Videosettings #" folder on the
	 * DLNA device. The default value is false: PMS will display the folder.
	 *
	 * @return True if PMS should hide the folder, false othewise.
	 */
	public boolean getHideVideoSettings() {
		return getBoolean(KEY_HIDE_VIDEO_SETTINGS, true);
	}

	/**
	 * Set to true if PMS should hide the "# Videosettings #" folder on the
	 * DLNA device, or set to false to make PMS display the folder.
	 *
	 * @param value True if PMS should hide the folder.
	 */
	public void setHideVideoSettings(boolean value) {
		configuration.setProperty(KEY_HIDE_VIDEO_SETTINGS, value);
	}

	/**
	 * Returns true if PMS should cache scanned media in its internal database,
	 * speeding up later retrieval. When false is returned, PMS will not use
	 * cache and media will have to be rescanned.
	 *
	 * @return True if PMS should cache media.
	 */
	public boolean getUseCache() {
		return getBoolean(KEY_USE_CACHE, true);
	}

	/**
	 * Set to true if PMS should cache scanned media in its internal database,
	 * speeding up later retrieval.
	 *
	 * @param value True if PMS should cache media.
	 */
	public void setUseCache(boolean value) {
		configuration.setProperty(KEY_USE_CACHE, value);
	}

	/**
	 * Whether we should pass the flag "convertfps=true" to AviSynth.
	 *
	 * @param value True if we should pass the flag.
	 */
	public void setAvisynthConvertFps(boolean value) {
		configuration.setProperty(KEY_AVISYNTH_CONVERT_FPS, value);
	}

	/**
	 * Returns true if we should pass the flag "convertfps=true" to AviSynth.
	 *
	 * @return True if we should pass the flag.
	 */
	public boolean getAvisynthConvertFps() {
		return getBoolean(KEY_AVISYNTH_CONVERT_FPS, true);
	}

	public void setAvisynthInterFrame(boolean value) {
		configuration.setProperty(KEY_AVISYNTH_INTERFRAME, value);
	}

	public boolean getAvisynthInterFrame() {
		return getBoolean(KEY_AVISYNTH_INTERFRAME, false);
	}

	public void setAvisynthInterFrameGPU(boolean value) {
		configuration.setProperty(KEY_AVISYNTH_INTERFRAME_GPU, value);
	}

	public boolean getAvisynthInterFrameGPU() {
		return getBoolean(KEY_AVISYNTH_INTERFRAME_GPU, false);
	}

	public void setAvisynthMultiThreading(boolean value) {
		configuration.setProperty(KEY_AVISYNTH_MULTITHREADING, value);
	}

	public boolean getAvisynthMultiThreading() {
		return getBoolean(KEY_AVISYNTH_MULTITHREADING, false);
	}

	/**
	 * Returns the template for the AviSynth script. The script string can
	 * contain the character "\u0001", which should be treated as the newline
	 * separator character.
	 *
	 * @return The AviSynth script template.
	 */
	public String getAvisynthScript() {
		return getString(KEY_AVISYNTH_SCRIPT, DEFAULT_AVI_SYNTH_SCRIPT);
	}

	/**
	 * Sets the template for the AviSynth script. The script string may contain
	 * the character "\u0001", which will be treated as newline character.
	 *
	 * @param value The AviSynth script template.
	 */
	public void setAvisynthScript(String value) {
		configuration.setProperty(KEY_AVISYNTH_SCRIPT, value);
	}

	/**
	 * Returns additional codec specific configuration options for MEncoder.
	 *
	 * @return The configuration options.
	 */
	public String getMencoderCodecSpecificConfig() {
		return getString(KEY_MENCODER_CODEC_SPECIFIC_SCRIPT, "");
	}

	/**
	 * Sets additional codec specific configuration options for MEncoder.
	 *
	 * @param value The additional configuration options.
	 */
	public void setMencoderCodecSpecificConfig(String value) {
		configuration.setProperty(KEY_MENCODER_CODEC_SPECIFIC_SCRIPT, value);
	}

	/**
	 * Returns the maximum size (in MB) that PMS should use for buffering
	 * audio.
	 *
	 * @return The maximum buffer size.
	 */
	public int getMaxAudioBuffer() {
		return getInt(KEY_MAX_AUDIO_BUFFER, 100);
	}

	/**
	 * Returns the minimum size (in MB) that PMS should use for the buffer used
	 * for streaming media.
	 *
	 * @return The minimum buffer size.
	 */
	public int getMinStreamBuffer() {
		return getInt(KEY_MIN_STREAM_BUFFER, 1);
	}

	/**
	 * Converts the getMPEG2MainSettings() from MEncoder's format to FFmpeg's.
	 *
	 * @return MPEG-2 settings formatted for FFmpeg.
	 */
	public String getMPEG2MainSettingsFFmpeg() {
		String mpegSettings = getMPEG2MainSettings();

		if (mpegSettings.contains("Automatic")) {
			return mpegSettings;
		}

		String mpegSettingsArray[] = mpegSettings.split(":");

		String pairArray[];
		StringBuilder returnString = new StringBuilder();
		for (String pair : mpegSettingsArray) {
			pairArray = pair.split("=");
			switch (pairArray[0]) {
				case "keyint":
					returnString.append("-g ").append(pairArray[1]).append(" ");
					break;
				case "vqscale":
					returnString.append("-q:v ").append(pairArray[1]).append(" ");
					break;
				case "vqmin":
					returnString.append("-qmin ").append(pairArray[1]).append(" ");
					break;
				case "vqmax":
					returnString.append("-qmax ").append(pairArray[1]).append(" ");
					break;
				default:
					break;
			}
		}

		return returnString.toString();
	}

	public void setFfmpegMultithreading(boolean value) {
		configuration.setProperty(KEY_FFMPEG_MULTITHREADING, value);
	}

	public boolean isFfmpegMultithreading() {
		boolean isMultiCore = getNumberOfCpuCores() > 1;
		return getBoolean(KEY_FFMPEG_MULTITHREADING, isMultiCore);
	}

	public void setFfmpegAviSynthMultithreading(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_MULTITHREADING, value);
	}

	public boolean isFfmpegAviSynthMultithreading() {
		boolean isMultiCore = getNumberOfCpuCores() > 1;
		return getBoolean(KEY_FFMPEG_AVISYNTH_MULTITHREADING, isMultiCore);
	}

	/**
	 * Whether we should pass the flag "convertfps=true" to AviSynth.
	 *
	 * @param value True if we should pass the flag.
	 */
	public void setFfmpegAvisynthConvertFps(boolean value) {
		configuration.setProperty(KEY_AVISYNTH_CONVERT_FPS, value);
	}

	/**
	 * Returns true if we should pass the flag "convertfps=true" to AviSynth.
	 *
	 * @return True if we should pass the flag.
	 */
	public boolean getFfmpegAvisynthConvertFps() {
		return getBoolean(KEY_FFMPEG_AVISYNTH_CONVERT_FPS, true);
	}

	public void setFfmpegAvisynthInterFrame(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_INTERFRAME, value);
	}

	public boolean getFfmpegAvisynthInterFrame() {
		return getBoolean(KEY_FFMPEG_AVISYNTH_INTERFRAME, false);
	}

	public void setFfmpegAvisynthInterFrameGPU(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_INTERFRAME_GPU, value);
	}

	public boolean getFfmpegAvisynthInterFrameGPU() {
		return getBoolean(KEY_FFMPEG_AVISYNTH_INTERFRAME_GPU, false);
	}

	public boolean isMencoderNoOutOfSync() {
		return getBoolean(KEY_MENCODER_NO_OUT_OF_SYNC, true);
	}

	public void setMencoderNoOutOfSync(boolean value) {
		configuration.setProperty(KEY_MENCODER_NO_OUT_OF_SYNC, value);
	}

	public boolean getTrancodeBlocksMultipleConnections() {
		return getBoolean(KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS, false);
	}

	public void setTranscodeBlocksMultipleConnections(boolean value) {
		configuration.setProperty(KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS, value);
	}

	public boolean getTrancodeKeepFirstConnections() {
		return getBoolean(KEY_TRANSCODE_KEEP_FIRST_CONNECTION, true);
	}

	public void setTrancodeKeepFirstConnections(boolean value) {
		configuration.setProperty(KEY_TRANSCODE_KEEP_FIRST_CONNECTION, value);
	}

	public boolean isMencoderIntelligentSync() {
		return getBoolean(KEY_MENCODER_INTELLIGENT_SYNC, true);
	}

	public void setMencoderIntelligentSync(boolean value) {
		configuration.setProperty(KEY_MENCODER_INTELLIGENT_SYNC, value);
	}

	@Deprecated
	public String getFfmpegAlternativePath() {
		return getString(KEY_FFMPEG_ALTERNATIVE_PATH, null);
	}

	@Deprecated
	public void setFfmpegAlternativePath(String value) {
		configuration.setProperty(KEY_FFMPEG_ALTERNATIVE_PATH, value);
	}

	public boolean getSkipLoopFilterEnabled() {
		return getBoolean(KEY_SKIP_LOOP_FILTER_ENABLED, false);
	}

	/**
	 * The list of network interfaces that should be skipped when checking
	 * for an available network interface. Entries should be comma separated
	 * and typically exclude the number at the end of the interface name.
	 * <p>
	 * Default is to skip the interfaces created by Virtualbox, OpenVPN and
	 * Parallels: "tap,vmnet,vnic,virtualbox".
	 * @return The string of network interface names to skip.
	 */
	public List<String> getSkipNetworkInterfaces() {
		return getStringList(KEY_SKIP_NETWORK_INTERFACES, "tap,vmnet,vnic,virtualbox");
	}

	public void setSkipLoopFilterEnabled(boolean value) {
		configuration.setProperty(KEY_SKIP_LOOP_FILTER_ENABLED, value);
	}

	public String getMPEG2MainSettings() {
		return getString(KEY_MPEG2_MAIN_SETTINGS, "Automatic (Wired)");
	}

	public void setMPEG2MainSettings(String value) {
		configuration.setProperty(KEY_MPEG2_MAIN_SETTINGS, value);
	}

	public String getx264ConstantRateFactor() {
		return getString(KEY_X264_CONSTANT_RATE_FACTOR, "Automatic (Wired)");
	}

	public void setx264ConstantRateFactor(String value) {
		configuration.setProperty(KEY_X264_CONSTANT_RATE_FACTOR, value);
	}

	public String getMencoderVobsubSubtitleQuality() {
		return getString(KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY, "3");
	}

	public void setMencoderVobsubSubtitleQuality(String value) {
		configuration.setProperty(KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY, value);
	}

	public String getMencoderOverscanCompensationWidth() {
		return getString(KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH, "0");
	}

	public void setMencoderOverscanCompensationWidth(String value) {
		if (value.trim().length() == 0) {
			value = "0";
		}
		configuration.setProperty(KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH, value);
	}

	public String getMencoderOverscanCompensationHeight() {
		return getString(KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT, "0");
	}

	public void setMencoderOverscanCompensationHeight(String value) {
		if (value.trim().length() == 0) {
			value = "0";
		}
		configuration.setProperty(KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT, value);
	}

	public void setEnginesAsList(ArrayList<String> enginesAsList) {
		configuration.setProperty(KEY_ENGINES, listToString(enginesAsList));
	}

	/**
	 * TODO look at the changes that were made to this in PMS and if they seem
	 * stable, merge them.
	 */
	public List<String> getEnginesAsList(SystemUtils registry) {
		String defaultEngines = StringUtils.join(
			new String[] {
				"ffmpegvideo",
				"mencoder",
				"tsmuxer",
				"ffmpegaudio",
				"tsmuxeraudio",
				"ffmpegwebvideo",
				"vlcwebvideo", // (VLCWebVideo)
				"vlcvideo", // (VideoLanVideoStreaming) TODO (legacy web video engine): remove
				"mencoderwebvideo",
				"vlcaudio", // (VideoLanAudioStreaming) TODO (legacy web audio engine): remove
				"ffmpegdvrmsremux",
				"rawthumbs"
			},
			","
		);
		List<String> engines = stringToList(
			// Possibly blank: An empty string means: disable all engines
			// http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=15416
			configurationReader.getPossiblyBlankConfigurationString(
				KEY_ENGINES,
				defaultEngines
			)
		);

		engines = hackAvs(registry, engines);
		return engines;
	}

	private static String listToString(List<String> enginesAsList) {
		return StringUtils.join(enginesAsList, LIST_SEPARATOR);
	}

	private static List<String> stringToList(String input) {
		List<String> output = new ArrayList<>();
		Collections.addAll(output, StringUtils.split(input, LIST_SEPARATOR));
		return output;
	}

	// TODO: Get this out of here
	private static List<String> hackAvs(SystemUtils registry, List<String> input) {
		List<String> toBeRemoved = new ArrayList<>();
		for (String engineId : input) {
			if (engineId.startsWith("avs") && !registry.isAvis() && Platform.isWindows()) {
				if (!avsHackLogged) {
					LOGGER.info("AviSynth is not installed. You cannot use " + engineId + " as a transcoding engine.");
					avsHackLogged = true;
				}

				toBeRemoved.add(engineId);
			}
		}

		List<String> output = new ArrayList<>();
		output.addAll(input);
		output.removeAll(toBeRemoved);
		return output;
	}

	public void save() throws ConfigurationException {
		configuration.save();
		LOGGER.info("Configuration saved to: " + PROFILE_PATH);
	}

	public String getFolders(ArrayList<String> tags) {
		return tagLoop(tags, ".folders", KEY_FOLDERS);
	}

	public String getFoldersIgnored(ArrayList<String> tags) {
		return tagLoop(tags, ".ignore", KEY_FOLDERS_IGNORED);
	}

	public void setFolders(String value) {
		configuration.setProperty(KEY_FOLDERS, value);
	}

	public String getFoldersMonitored() {
		return getString(KEY_FOLDERS_MONITORED, "");
	}

	public void setFoldersMonitored(String value) {
		configuration.setProperty(KEY_FOLDERS_MONITORED, value);
	}

	public String getNetworkInterface() {
		return getString(KEY_NETWORK_INTERFACE, "");
	}

	public void setNetworkInterface(String value) {
		configuration.setProperty(KEY_NETWORK_INTERFACE, value);
	}

	public boolean isHideEngineNames() {
		return getBoolean(KEY_HIDE_ENGINENAMES, true);
	}

	public void setHideEngineNames(boolean value) {
		configuration.setProperty(KEY_HIDE_ENGINENAMES, value);
	}

	public boolean isHideExtensions() {
		return getBoolean(KEY_HIDE_EXTENSIONS, true);
	}

	public void setHideExtensions(boolean value) {
		configuration.setProperty(KEY_HIDE_EXTENSIONS, value);
	}

	public String getShares() {
		return getString(KEY_SHARES, "");
	}

	public void setShares(String value) {
		configuration.setProperty(KEY_SHARES, value);
	}

	public String getDisableTranscodeForExtensions() {
		return getString(KEY_DISABLE_TRANSCODE_FOR_EXTENSIONS, "");
	}

	public void setDisableTranscodeForExtensions(String value) {
		configuration.setProperty(KEY_DISABLE_TRANSCODE_FOR_EXTENSIONS, value);
	}

	public String getForceTranscodeForExtensions() {
		return getString(KEY_FORCE_TRANSCODE_FOR_EXTENSIONS, "");
	}

	public void setForceTranscodeForExtensions(String value) {
		configuration.setProperty(KEY_FORCE_TRANSCODE_FOR_EXTENSIONS, value);
	}

	public void setMencoderMT(boolean value) {
		configuration.setProperty(KEY_MENCODER_MT, value);
	}

	public boolean getMencoderMT() {
		boolean isMultiCore = getNumberOfCpuCores() > 1;
		return getBoolean(KEY_MENCODER_MT, isMultiCore);
	}

	public void setAudioRemuxAC3(boolean value) {
		configuration.setProperty(KEY_AUDIO_REMUX_AC3, value);
	}

	public boolean isAudioRemuxAC3() {
		return getBoolean(KEY_AUDIO_REMUX_AC3, true);
	}

	public void setMencoderRemuxMPEG2(boolean value) {
		configuration.setProperty(KEY_MENCODER_REMUX_MPEG2, value);
	}

	public boolean isMencoderRemuxMPEG2() {
		return getBoolean(KEY_MENCODER_REMUX_MPEG2, true);
	}

	public void setDisableFakeSize(boolean value) {
		configuration.setProperty(KEY_DISABLE_FAKESIZE, value);
	}

	public boolean isDisableFakeSize() {
		return getBoolean(KEY_DISABLE_FAKESIZE, false);
	}

	/**
	 * Whether the style rules defined by styled subtitles (ASS/SSA) should
	 * be followed (true) or overridden by our style rules (false) when
	 * using MEncoder.
	 *
	 * @see #setUseEmbeddedSubtitlesStyle(boolean)
	 * @param value whether to use the embedded styles or ours
	 * @deprecated
	 */
	@Deprecated
	public void setMencoderAssDefaultStyle(boolean value) {
		configuration.setProperty(KEY_MENCODER_ASS_DEFAULTSTYLE, value);
	}

	/**
	 * Whether the style rules defined by styled subtitles (ASS/SSA) should
	 * be followed (true) or overridden by our style rules (false) when
	 * using MEncoder.
	 *
	 * @see #isUseEmbeddedSubtitlesStyle()
	 * @return whether to use the embedded styles or ours
	 * @deprecated
	 */
	@Deprecated
	public boolean isMencoderAssDefaultStyle() {
		return getBoolean(KEY_MENCODER_ASS_DEFAULTSTYLE, true);
	}

	/**
	 * Whether the style rules defined by styled subtitles (ASS/SSA) should
	 * be followed (true) or overridden by our style rules (false).
	 *
	 * @param value whether to use the embedded styles or ours
	 */
	public void setUseEmbeddedSubtitlesStyle(boolean value) {
		configuration.setProperty(KEY_USE_EMBEDDED_SUBTITLES_STYLE, value);
	}

	/**
	 * Whether the style rules defined by styled subtitles (ASS/SSA) should
	 * be followed (true) or overridden by our style rules (false).
	 *
	 * @return whether to use the embedded styles or ours
	 */
	public boolean isUseEmbeddedSubtitlesStyle() {
		return getBoolean(KEY_USE_EMBEDDED_SUBTITLES_STYLE, true) || getBoolean(KEY_MENCODER_ASS_DEFAULTSTYLE, true);
	}

	public int getMEncoderOverscan() {
		return getInt(KEY_OVERSCAN, 0);
	}

	public void setMEncoderOverscan(int value) {
		configuration.setProperty(KEY_OVERSCAN, value);
	}

	/**
	 * Returns sort method to use for ordering lists of files. One of the
	 * following values is returned:
	 * <ul>
	 * <li>0: Locale-sensitive A-Z</li>
	 * <li>1: Sort by modified date, newest first</li>
	 * <li>2: Sort by modified date, oldest first</li>
	 * <li>3: Case-insensitive ASCIIbetical sort</li>
	 * <li>4: Locale-sensitive natural sort</li>
	 * <li>5: Random</li>
	 * </ul>
	 * Default value is 4.
	 * @return The sort method
	 */
	private int findPathSort(String[] paths, String path) throws NumberFormatException{
		for (String path1 : paths) {
			String[] kv = path1.split(",");
			if (kv.length < 2) {
				continue;
			}
			if (kv[0].equals(path)) {
				return Integer.parseInt(kv[1]);
			}
		}
		return -1;
	}

	public int getSortMethod(File path) {
		int cnt = 0;
		String raw = getString(KEY_SORT_PATHS, null);
		if (StringUtils.isEmpty(raw)) {
			return getInt(KEY_SORT_METHOD, UMSUtils.SORT_LOC_NAT);
		}
		if (Platform.isWindows()) {
			// windows is crap
			raw = raw.toLowerCase();
		}
		String[] paths = raw.split(" ");

		while (path != null && (cnt++ < 100)) {
			String key = path.getAbsolutePath();
			if (Platform.isWindows()) {
				key = key.toLowerCase();
			}
			try {
				int ret = findPathSort(paths, key);
				if (ret != -1) {
					return ret;
				}
			} catch (NumberFormatException e) {
				// just ignore
			}
			path = path.getParentFile();
		}
		return getInt(KEY_SORT_METHOD, UMSUtils.SORT_LOC_NAT);
	}

	/**
	 * Set the sort method to use for ordering lists of files. The following
	 * values are recognized:
	 * <ul>
	 * <li>0: Locale-sensitive A-Z</li>
	 * <li>1: Sort by modified date, newest first</li>
	 * <li>2: Sort by modified date, oldest first</li>
	 * <li>3: Case-insensitive ASCIIbetical sort</li>
	 * <li>4: Locale-sensitive natural sort</li>
	 * <li>5: Random</li>
	 * </ul>
	 * @param value The sort method to use
	 */
	public void setSortMethod(int value) {
		configuration.setProperty(KEY_SORT_METHOD, value);
	}

	public int getAudioThumbnailMethod() {
		return getInt(KEY_AUDIO_THUMBNAILS_METHOD, 0);
	}

	public void setAudioThumbnailMethod(int value) {
		configuration.setProperty(KEY_AUDIO_THUMBNAILS_METHOD, value);
	}

	public String getAlternateThumbFolder() {
		return getString(KEY_ALTERNATE_THUMB_FOLDER, "");
	}

	public void setAlternateThumbFolder(String value) {
		configuration.setProperty(KEY_ALTERNATE_THUMB_FOLDER, value);
	}

	public String getAlternateSubtitlesFolder() {
		return getString(KEY_ALTERNATE_SUBTITLES_FOLDER, "");
	}

	public void setAlternateSubtitlesFolder(String value) {
		configuration.setProperty(KEY_ALTERNATE_SUBTITLES_FOLDER, value);
	}

	public void setAudioEmbedDtsInPcm(boolean value) {
		configuration.setProperty(KEY_AUDIO_EMBED_DTS_IN_PCM, value);
	}

	public boolean isAudioEmbedDtsInPcm() {
		return getBoolean(KEY_AUDIO_EMBED_DTS_IN_PCM, false);
	}

	public void setEncodedAudioPassthrough(boolean value) {
		configuration.setProperty(KEY_ENCODED_AUDIO_PASSTHROUGH, value);
	}

	public boolean isEncodedAudioPassthrough() {
		return getBoolean(KEY_ENCODED_AUDIO_PASSTHROUGH, false);
	}

	public void setMencoderMuxWhenCompatible(boolean value) {
		configuration.setProperty(KEY_MENCODER_MUX_COMPATIBLE, value);
	}

	public boolean isMencoderMuxWhenCompatible() {
		return getBoolean(KEY_MENCODER_MUX_COMPATIBLE, true);
	}

	public void setMEncoderNormalizeVolume(boolean value) {
		configuration.setProperty(KEY_MENCODER_NORMALIZE_VOLUME, value);
	}

	public boolean isMEncoderNormalizeVolume() {
		return getBoolean(KEY_MENCODER_NORMALIZE_VOLUME, false);
	}

	public void setFFmpegMuxWithTsMuxerWhenCompatible(boolean value) {
		configuration.setProperty(KEY_FFMPEG_MUX_TSMUXER_COMPATIBLE, value);
	}

	public boolean isFFmpegMuxWithTsMuxerWhenCompatible() {
		return getBoolean(KEY_FFMPEG_MUX_TSMUXER_COMPATIBLE, true);
	}

	/**
	 * Whether FFmpegVideo should defer to MEncoderVideo when there are
	 * subtitles that need to be transcoded.
	 *
	 * @param value
	 */
	public void setFFmpegDeferToMEncoderForSubtitles(boolean value) {
		configuration.setProperty(KEY_FFMPEG_MENCODER_SUBTITLES, value);
	}

	/**
	 * Whether FFmpegVideo should defer to MEncoderVideo when there are
	 * subtitles that need to be transcoded.
	 *
	 * @return
	 */
	public boolean isFFmpegDeferToMEncoderForSubtitles() {
		return getBoolean(KEY_FFMPEG_MENCODER_SUBTITLES, false);
	}

	public void setFFmpegFontConfig(boolean value) {
		configuration.setProperty(KEY_FFMPEG_FONTCONFIG, value);
	}

	public boolean isFFmpegFontConfig() {
		return getBoolean(KEY_FFMPEG_FONTCONFIG, false);
	}

	public void setMuxAllAudioTracks(boolean value) {
		configuration.setProperty(KEY_MUX_ALLAUDIOTRACKS, value);
	}

	public boolean isMuxAllAudioTracks() {
		return getBoolean(KEY_MUX_ALLAUDIOTRACKS, false);
	}

	public void setUseMplayerForVideoThumbs(boolean value) {
		configuration.setProperty(KEY_USE_MPLAYER_FOR_THUMBS, value);
	}

	public boolean isUseMplayerForVideoThumbs() {
		return getBoolean(KEY_USE_MPLAYER_FOR_THUMBS, false);
	}

	public String getIpFilter() {
		return getString(KEY_IP_FILTER, "");
	}

	public synchronized IpFilter getIpFiltering() {
	    filter.setRawFilter(getIpFilter());
	    return filter;
	}

	public void setIpFilter(String value) {
		configuration.setProperty(KEY_IP_FILTER, value);
	}

	public void setPreventsSleep(boolean value) {
		configuration.setProperty(KEY_PREVENTS_SLEEP, value);
	}

	public boolean isPreventsSleep() {
		return getBoolean(KEY_PREVENTS_SLEEP, false);
	}

	public void setHTTPEngineV2(boolean value) {
		configuration.setProperty(KEY_HTTP_ENGINE_V2, value);
	}

	public boolean isHTTPEngineV2() {
		return getBoolean(KEY_HTTP_ENGINE_V2, true);
	}

	public boolean isShowIphotoLibrary() {
		return getBoolean(KEY_SHOW_IPHOTO_LIBRARY, false);
	}

	public void setShowIphotoLibrary(boolean value) {
		configuration.setProperty(KEY_SHOW_IPHOTO_LIBRARY, value);
	}

	public boolean isShowApertureLibrary() {
		return getBoolean(KEY_SHOW_APERTURE_LIBRARY, false);
	}

	public void setShowApertureLibrary(boolean value) {
		configuration.setProperty(KEY_SHOW_APERTURE_LIBRARY, value);
	}

	public boolean isShowItunesLibrary() {
		return getBoolean(KEY_SHOW_ITUNES_LIBRARY, false);
	}

	public String getItunesLibraryPath() {
		return getString(KEY_ITUNES_LIBRARY_PATH, "");
	}

	public void setShowItunesLibrary(boolean value) {
		configuration.setProperty(KEY_SHOW_ITUNES_LIBRARY, value);
	}

	public boolean isHideAdvancedOptions() {
		return getBoolean(PmsConfiguration.KEY_HIDE_ADVANCED_OPTIONS, true);
	}

	public void setHideAdvancedOptions(final boolean value) {
		this.configuration.setProperty(PmsConfiguration.KEY_HIDE_ADVANCED_OPTIONS, value);
	}

	public boolean isHideEmptyFolders() {
		return getBoolean(PmsConfiguration.KEY_HIDE_EMPTY_FOLDERS, false);
	}

	public void setHideEmptyFolders(final boolean value) {
		this.configuration.setProperty(PmsConfiguration.KEY_HIDE_EMPTY_FOLDERS, value);
	}

	public boolean isHideMediaLibraryFolder() {
		return getBoolean(PmsConfiguration.KEY_HIDE_MEDIA_LIBRARY_FOLDER, true);
	}

	public void setHideMediaLibraryFolder(final boolean value) {
		this.configuration.setProperty(PmsConfiguration.KEY_HIDE_MEDIA_LIBRARY_FOLDER, value);
	}

	// TODO (breaking change): rename to e.g. isTranscodeFolderEnabled
	// (and return true by default)
	public boolean getHideTranscodeEnabled() {
		return getBoolean(KEY_HIDE_TRANSCODE_FOLDER, false);
	}

	// TODO (breaking change): rename to e.g. setTranscodeFolderEnabled
	// (and negate the value in the caller)
	public void setHideTranscodeEnabled(boolean value) {
		configuration.setProperty(KEY_HIDE_TRANSCODE_FOLDER, value);
	}

	public boolean isDvdIsoThumbnails() {
		return getBoolean(KEY_DVDISO_THUMBNAILS, false);
	}

	public void setDvdIsoThumbnails(boolean value) {
		configuration.setProperty(KEY_DVDISO_THUMBNAILS, value);
	}

	public Object getCustomProperty(String property) {
		return configurationReader.getCustomProperty(property);
	}

	public void setCustomProperty(String property, Object value) {
		configuration.setProperty(property, value);
	}

	public boolean isChapterSupport() {
		return getBoolean(KEY_CHAPTER_SUPPORT, false);
	}

	public void setChapterSupport(boolean value) {
		configuration.setProperty(KEY_CHAPTER_SUPPORT, value);
	}

	public int getChapterInterval() {
		return getInt(KEY_CHAPTER_INTERVAL, 5);
	}

	public void setChapterInterval(int value) {
		configuration.setProperty(KEY_CHAPTER_INTERVAL, value);
	}

	public int getSubsColor() {
		return getInt(KEY_SUBS_COLOR, 0xffffffff);
	}

	public void setSubsColor(int value) {
		configuration.setProperty(KEY_SUBS_COLOR, value);
	}

	public boolean isFix25FPSAvMismatch() {
		return getBoolean(KEY_FIX_25FPS_AV_MISMATCH, false);
	}

	public void setFix25FPSAvMismatch(boolean value) {
		configuration.setProperty(KEY_FIX_25FPS_AV_MISMATCH, value);
	}

	public int getVideoTranscodeStartDelay() {
		return getInt(KEY_VIDEOTRANSCODE_START_DELAY, 6);
	}

	public void setVideoTranscodeStartDelay(int value) {
		configuration.setProperty(KEY_VIDEOTRANSCODE_START_DELAY, value);
	}

	public boolean isAudioResample() {
		return getBoolean(KEY_AUDIO_RESAMPLE, true);
	}

	public void setAudioResample(boolean value) {
		configuration.setProperty(KEY_AUDIO_RESAMPLE, value);
	}

	public boolean isIgnoreTheWordThe() {
		return getBoolean(KEY_IGNORE_THE_WORD_THE, true);
	}

	public void setIgnoreTheWordThe(boolean value) {
		configuration.setProperty(KEY_IGNORE_THE_WORD_THE, value);
	}

	public boolean isPrettifyFilenames() {
		return getBoolean(KEY_PRETTIFY_FILENAMES, false);
	}

	public void setPrettifyFilenames(boolean value) {
		configuration.setProperty(KEY_PRETTIFY_FILENAMES, value);
	}

	public boolean isRunWizard() {
		return getBoolean(KEY_RUN_WIZARD, true);
	}

	public void setRunWizard(boolean value) {
		configuration.setProperty(KEY_RUN_WIZARD, value);
	}

	public boolean isHideNewMediaFolder() {
		return getBoolean(KEY_HIDE_NEW_MEDIA_FOLDER, false);
	}

	public void setHideNewMediaFolder(final boolean value) {
		this.configuration.setProperty(KEY_HIDE_NEW_MEDIA_FOLDER, value);
	}

	public boolean isHideRecentlyPlayedFolder() {
		return getBoolean(PmsConfiguration.KEY_HIDE_RECENTLY_PLAYED_FOLDER, false);
	}

	public void setHideRecentlyPlayedFolder(final boolean value) {
		this.configuration.setProperty(PmsConfiguration.KEY_HIDE_RECENTLY_PLAYED_FOLDER, value);
	}

	/**
	 * Returns the name of the renderer to fall back on when header matching
	 * fails. PMS will recognize the configured renderer instead of "Unknown
	 * renderer". Default value is "", which means PMS will return the unknown
	 * renderer when no match can be made.
	 *
	 * @return The name of the renderer PMS should fall back on when header
	 *         matching fails.
	 * @see #isRendererForceDefault()
	 */
	public String getRendererDefault() {
		return getString(KEY_RENDERER_DEFAULT, "");
	}

	/**
	 * Sets the name of the renderer to fall back on when header matching
	 * fails. PMS will recognize the configured renderer instead of "Unknown
	 * renderer". Set to "" to make PMS return the unknown renderer when no
	 * match can be made.
	 *
	 * @param value The name of the renderer to fall back on. This has to be
	 *              <code>""</code> or a case insensitive match with the name
	 *              used in any render configuration file.
	 * @see #setRendererForceDefault(boolean)
	 */
	public void setRendererDefault(String value) {
		configuration.setProperty(KEY_RENDERER_DEFAULT, value);
	}

	/**
	 * Returns true when PMS should not try to guess connecting renderers
	 * and instead force picking the defined fallback renderer. Default
	 * value is false, which means PMS will attempt to recognize connecting
	 * renderers by their headers.
	 *
	 * @return True when the fallback renderer should always be picked.
	 * @see #getRendererDefault()
	 */
	public boolean isRendererForceDefault() {
		return getBoolean(KEY_RENDERER_FORCE_DEFAULT, false);
	}

	/**
	 * Set to true when PMS should not try to guess connecting renderers
	 * and instead force picking the defined fallback renderer. Set to false
	 * to make PMS attempt to recognize connecting renderers by their headers.
	 *
	 * @param value True when the fallback renderer should always be picked.
	 * @see #setRendererDefault(String)
	 */
	public void setRendererForceDefault(boolean value) {
		configuration.setProperty(KEY_RENDERER_FORCE_DEFAULT, value);
	}

	public String getVirtualFolders(ArrayList<String> tags) {
		return tagLoop(tags, ".vfolders", KEY_VIRTUAL_FOLDERS);
	}

	public String getVirtualFoldersFile(ArrayList<String> tags) {
		return tagLoop(tags, ".vfolders.file", KEY_VIRTUAL_FOLDERS_FILE);
	}

	public String getProfilePath() {
		return PROFILE_PATH;
	}

	public String getProfileDirectory() {
		return PROFILE_DIRECTORY;
	}

	/**
	 * Returns the absolute path to the WEB.conf file. By default
	 * this is <pre>PROFILE_DIRECTORY + File.pathSeparator + WEB.conf</pre>,
	 * but it can be overridden via the <pre>web_conf</pre> profile option.
	 * The existence of the file is not checked.
	 *
	 * @return the path to the WEB.conf file.
	 */
	public String getWebConfPath() {
		// Initialise this here rather than in the constructor
		// or statically so that custom settings are logged
		// to the debug.log/Logs tab.
		if (WEB_CONF_PATH == null) {
			WEB_CONF_PATH = FileUtil.getFileLocation(
				getString(KEY_WEB_CONF_PATH, null),
				PROFILE_DIRECTORY,
				DEFAULT_WEB_CONF_FILENAME
			).getFilePath();
		}

		return WEB_CONF_PATH;
	}

	public String getPluginDirectory() {
		return getString(KEY_PLUGIN_DIRECTORY, "plugins");
	}

	public void setPluginDirectory(String value) {
		configuration.setProperty(KEY_PLUGIN_DIRECTORY, value);
	}

	public String getProfileName() {
		if (HOSTNAME == null) { // Initialise this lazily
			try {
				HOSTNAME = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				LOGGER.info("Can't determine hostname");
				HOSTNAME = "unknown host";
			}
		}

		return getString(KEY_PROFILE_NAME, HOSTNAME);
	}

	public boolean isAutoUpdate() {
		return Build.isUpdatable() && getBoolean(KEY_AUTO_UPDATE, false);
	}

	public void setAutoUpdate(boolean value) {
		configuration.setProperty(KEY_AUTO_UPDATE, value);
	}

	public int getUpnpPort() {
		return getInt(KEY_UPNP_PORT, 1900);
	}

	public String getUuid() {
		return getString(KEY_UUID, null);
	}

	public void setUuid(String value){
		configuration.setProperty(KEY_UUID, value);
	}

	public void addConfigurationListener(ConfigurationListener l) {
		configuration.addConfigurationListener(l);
	}

	public void removeConfigurationListener(ConfigurationListener l) {
		configuration.removeConfigurationListener(l);
	}

	public boolean getFolderLimit() {
		return getBoolean(KEY_FOLDER_LIMIT, false);
	}

	// FIXME this is undocumented and misnamed
	@Deprecated
	public boolean initBufferMax() {
		return getBoolean(KEY_BUFFER_MAX, false);
	}

	public String getScriptDir() {
		return getString(KEY_SCRIPT_DIR, null);
	}

	public String getPluginPurgeAction() {
		return getString(KEY_PLUGIN_PURGE_ACTION, "delete");
	}

	public boolean getSearchFolder() {
		return getBoolean(KEY_SEARCH_FOLDER, false);
	}

	public boolean getSearchInFolder() {
		return getBoolean(KEY_SEARCH_IN_FOLDER, false) && getSearchFolder();
	}

	public int getSearchDepth() {
		int ret = (getBoolean(KEY_SEARCH_RECURSE, true) ? 100 : 2);
	   	return getInt(KEY_SEARCH_RECURSE_DEPTH, ret);
	}

	public void reload() {
		try {
			configuration.refresh();
		} catch (ConfigurationException e) {
			LOGGER.error(null, e);
		}
	}

	/**
	 * Retrieve the name of the folder used to select subtitles, audio channels, chapters, engines &amp;c.
	 * Defaults to the localized version of <pre>#--TRANSCODE--#</pre>.
	 * @return The folder name.
	 */
	public String getTranscodeFolderName() {
		return getString(KEY_TRANSCODE_FOLDER_NAME, Messages.getString("TranscodeVirtualFolder.0"));
	}

	/**
	 * Set a custom name for the <pre>#--TRANSCODE--#</pre> folder.
	 * @param name The folder name.
	 */
	public void setTranscodeFolderName(String name) {
		configuration.setProperty(KEY_TRANSCODE_FOLDER_NAME, name);
	}

	/**
	 * State if the video hardware acceleration is allowed
	 * @return true if hardware acceleration is allowed, false otherwise
	 */
	public boolean isGPUAcceleration() {
		return getBoolean(KEY_GPU_ACCELERATION, false);
	}

	/**
	 * Set the video hardware acceleration enable/disable
	 * @param value true if hardware acceleration is allowed, false otherwise
	 */
	public void setGPUAcceleration(boolean value) {
		configuration.setProperty(KEY_GPU_ACCELERATION, value);
	}

	/**
	 * Finds out whether the program has admin rights.
	 * It only checks on Windows and returns true if on a non-Windows OS.
	 *
	 * Note: Detection of Windows 8 depends on the user having a version of
	 * JRE newer than 1.6.0_31 installed.
	 *
	 * TODO: We should make it check for rights on other operating systems.
	 */
	public boolean isAdmin() {
		if (
			"Windows 8".equals(System.getProperty("os.name")) ||
			"Windows 7".equals(System.getProperty("os.name")) ||
			"Windows Vista".equals(System.getProperty("os.name"))
		) {
			try {
				String command = "reg query \"HKU\\S-1-5-19\"";
				Process p = Runtime.getRuntime().exec(command);
				p.waitFor();
				int exitValue = p.exitValue();

				if (0 == exitValue) {
					return true;
				}

				return false;
			} catch (IOException | InterruptedException e) {
				LOGGER.error("Something prevented UMS from checking Windows permissions", e);
			}
		}

		return true;
	}

	/* Start without external netowrk (increase startup speed) */
	public static final String KEY_EXTERNAL_NETWORK = "external_network";

	public boolean getExternalNetwork() {
		return getBoolean(KEY_EXTERNAL_NETWORK, true);
	}

	public void setExternalNetwork(boolean b) {
		configuration.setProperty(KEY_EXTERNAL_NETWORK, b);
	}

	/* Credential path handling */
	public static final String KEY_CRED_PATH = "cred.path";

	public void initCred() throws IOException {
		String cp = getCredPath();
		if (StringUtils.isEmpty(cp)) {
			// need to make sure we got a cred path here
			cp = new File(getProfileDirectory() + File.separator + "UMS.cred").getAbsolutePath();
			configuration.setProperty(KEY_CRED_PATH, cp);
			try {
				configuration.save();
			} catch (ConfigurationException e) {
			}
		}

		// Now we know cred path is set
		File f = new File(cp);
		if (!f.exists()) {
			// Cred path is set but file isn't there
			// Create empty file with some comments
			try (FileOutputStream fos = new FileOutputStream(f)) {
				StringBuilder sb = new StringBuilder();
				sb.append("# Add credentials to the file");
				sb.append("\n");
				sb.append("# on the format tag=user,pwd");
				sb.append("\n");
				sb.append("# For example:");
				sb.append("\n");
				sb.append("# channels.xxx=name,secret");
				sb.append("\n");
				fos.write(sb.toString().getBytes());
				fos.flush();
			}
		}
	}

	public String getCredPath() {
		return getString(KEY_CRED_PATH, "");
	}

	public File getCredFile() {
		return new File(getCredPath());
	}

	public int getATZLimit() {
		int tmp = getInt(KEY_ATZ_LIMIT, 10000);
		if (tmp <= 2) {
			// this is silly, ignore
			tmp = 10000;
		}
		return tmp;
	}

	public void setATZLimit(int val) {
		if (val <= 2) {
			// clear prop
			configuration.clearProperty(KEY_ATZ_LIMIT);
			return;
		}
		configuration.setProperty(KEY_ATZ_LIMIT, val);
	}

	public void setATZLimit(String str) {
		try {
			setATZLimit(Integer.parseInt(str));
		} catch (Exception e) {
			setATZLimit(0);
		}
	}

	public String getDataDir() {
		return getProfileDirectory() + File.separator + "data";
	}

	public String getDataFile(String str) {
		return getDataDir() + File.separator + str;
	}

	private String KEY_URL_RES_ORDER = "url_resolve_order";

	public String[] getURLResolveOrder() {
		return getString(KEY_URL_RES_ORDER, "").split(",");
	}

	public boolean isHideLiveSubtitlesFolder() {
		return getBoolean(KEY_HIDE_LIVE_SUBTITLES_FOLDER, true);
	}

	public void setHideLiveSubtitlesFolder(boolean value) {
		configuration.setProperty(KEY_HIDE_LIVE_SUBTITLES_FOLDER, value);
	}

	public int liveSubtitlesLimit() {
		return getInt(KEY_LIVE_SUBTITLES_LIMIT, 20);
	}
	
	public boolean isLiveSubtitlesKeep() {
		return getBoolean(KEY_LIVE_SUBTITLES_KEEP, false);
	}

	public boolean isVlcUseHardwareAccel() {
		return getBoolean(KEY_VLC_USE_HW_ACCELERATION, false);
	}

	public void setVlcUseHardwareAccel(boolean value) {
		configuration.setProperty(KEY_VLC_USE_HW_ACCELERATION, value);
	}

	public boolean isVlcExperimentalCodecs() {
		return getBoolean(KEY_VLC_USE_EXPERIMENTAL_CODECS, false);
	}

	public void setVlcExperimentalCodecs(boolean value) {
		configuration.setProperty(KEY_VLC_USE_EXPERIMENTAL_CODECS, value);
	}

	public boolean isVlcAudioSyncEnabled() {
		return getBoolean(KEY_VLC_AUDIO_SYNC_ENABLED, false);
	}

	public void setVlcAudioSyncEnabled(boolean value) {
		configuration.setProperty(KEY_VLC_AUDIO_SYNC_ENABLED, value);
	}

	public boolean isVlcSubtitleEnabled() {
		return getBoolean(KEY_VLC_SUBTITLE_ENABLED, true);
	}

	public void setVlcSubtitleEnabled(boolean value) {
		configuration.setProperty(KEY_VLC_SUBTITLE_ENABLED, value);
	}

	public String getVlcScale() {
		return getString(KEY_VLC_SCALE, "1.0");
	}

	public void setVlcScale(String value) {
		configuration.setProperty(KEY_VLC_SCALE, value);
	}

	public boolean getVlcSampleRateOverride() {
		return getBoolean(KEY_VLC_SAMPLE_RATE_OVERRIDE, false);
	}

	public void setVlcSampleRateOverride(boolean value) {
		configuration.setProperty(KEY_VLC_SAMPLE_RATE_OVERRIDE, value);
	}

	public String getVlcSampleRate() {
		return getString(KEY_VLC_SAMPLE_RATE, "48000");
	}

	public void setVlcSampleRate(String value) {
		configuration.setProperty(KEY_VLC_SAMPLE_RATE, value);
	}

	public boolean isResumeEnabled()  {
		return getBoolean(KEY_RESUME, true);
	}

	public void setResume(boolean value) {
		configuration.setProperty(KEY_RESUME, value);
	}

	public int getMinPlayTime() {
		return getInt(KEY_MIN_PLAY_TIME, 10000);
	}

	public int getMinPlayTimeWeb() {
		return getInt(KEY_MIN_PLAY_TIME_WEB, getMinPlayTime());
	}

	public int getMinPlayTimeFile() {
		return getInt(KEY_MIN_PLAY_TIME_FILE, getMinPlayTime());
	}

	public int getResumeRewind() {
		return getInt(KEY_RESUME_REWIND, 17000);
	}

	public double getResumeBackFactor() {
		int percent = getInt(KEY_RESUME_BACK, 92);
		if (percent > 97) {
			percent = 97;
		}
		if (percent < 10) {
			percent = 10;
		}
		return (percent / 100.0);
	}

	public int getResumeKeepTime() {
		return getInt(KEY_RESUME_KEEP_TIME, 0);
	}

	public boolean hideSubsInfo() {
		return getBoolean(KEY_HIDE_SUBS_INFO, false);
	}

	public String getPlugins(ArrayList<String> tags) {
		return tagLoop(tags, ".plugins", "dummy");
	}

	public boolean isHideWebFolder(ArrayList<String> tags) {
		return tagLoopBool(tags, ".web", "dummy", false);
	}

	private String tagLoop(ArrayList<String> tags, String suff, String fallback) {
		if (tags == null || tags.isEmpty()) {
			// no tags use fallback
			return getString(fallback, "");
		}

		for (String tag : tags) {
			String x = (tag.toLowerCase() + suff).replaceAll(" ", "_");
			String res = getString(x, "");
			if (StringUtils.isNotBlank(res)) {
				// use first tag found
				return res;
			}
		}

		// down here no matching tag was found
		// return fallback
		return getString(fallback, "");
	}

	private boolean tagLoopBool(ArrayList<String> tags, String suff, String fallback, boolean def) {
		String b = tagLoop(tags, suff, fallback);
		if (StringUtils.isBlank(b)) {
			return def;
		}

		return b.trim().equalsIgnoreCase("true");
	}

	/**
	 * Whether the profile name should be appended to the server name when
	 * displayed on the renderer
	 *
	 * @return True if the profile name should be appended.
	 */
	public boolean isAppendProfileName() {
		return getBoolean(KEY_APPEND_PROFILE_NAME, false);
	}

	/**
	 * Set whether the profile name should be appended to the server name
	 * when displayed on the renderer
	 *
	 * @param value Set to true if the profile name should be appended.
	 */
	public void setAppendProfileName(boolean value) {
		configuration.setProperty(KEY_APPEND_PROFILE_NAME, value);
	}

	public String getDepth3D() {
		return getString(KEY_3D_SUBTITLES_DEPTH, "0");
	}

	public void setDepth3D(String value) {
		configuration.setProperty(KEY_3D_SUBTITLES_DEPTH, value);
	}

	/**
	 * @deprecated
	 * @see #setRunSingleInstance(boolean)
	 */
	@Deprecated
	public void setSingle(boolean value) {
		setRunSingleInstance(value);
	}

	/**
	 * Set whether UMS should allow only one instance by shutting down
	 * the first one when a second one is launched.
	 *
	 * @param value whether to kill the old UMS instance
	 */
	public void setRunSingleInstance(boolean value) {
		configuration.setProperty(KEY_SINGLE, value);
	}

	/**
	 * @deprecated
	 * @see #isRunSingleInstance()
	 */
	@Deprecated
	public boolean getSingle() {
		return isRunSingleInstance();
	}

	/**
	 * Whether UMS should allow only one instance by shutting down
	 * the first one when a second one is launched.
	 *
	 * @return value whether to kill the old UMS instance
	 */
	public boolean isRunSingleInstance() {
		return getBoolean(KEY_SINGLE, true);
	}

	/**
	 * Web stuff
	 */
	private static final String KEY_NO_FOLDERS = "no_shared";
	private static final String KEY_WEB_HTTPS = "use_https";
	private static final String KEY_WEB_PORT = "web_port";
	private static final int WEB_MAX_THREADS = 100;

	public boolean getNoFolders(String tag) {
		if (tag == null) {
			return getBoolean(KEY_NO_FOLDERS, false);
		}
		String x = (tag.toLowerCase() + ".no_shared").replaceAll(" ", "_");
		return getBoolean(x, false);
	}

	public boolean getWebHttps() {
		return getBoolean(KEY_WEB_HTTPS, false);
	}

	public File getWebPath() {
		File path = new File(getString(KEY_WEB_PATH, "web"));
		if (!path.exists()) {
			path.mkdirs();
		}
		return path;
	}

	public File getWebFile(String file) {
		return new File(getWebPath().getAbsolutePath() + File.separator + file);
	}

	public boolean isWebAuthenticate() {
		return getBoolean(KEY_WEB_AUTHENTICATE, false);
	}

	public int getWebThreads() {
		int x = getInt(KEY_WEB_THREADS, 30);
		return (x > WEB_MAX_THREADS ? WEB_MAX_THREADS : x);
	}

	public boolean isWebMp4Trans() {
		return getBoolean(KEY_WEB_MP4_TRANS, false);
	}

	public int getWebPort() {
		return getInt(KEY_WEB_PORT, 0);
	}

	public boolean useWebInterface() {
		return getBoolean(KEY_WEB_ENABLE, true);
	}

	public boolean isAutomaticMaximumBitrate() {
		return getBoolean(KEY_AUTOMATIC_MAXIMUM_BITRATE, false);
	}

	public void setAutomaticMaximumBitrate(boolean b) {
		if (!isAutomaticMaximumBitrate() && b) {
			// get all bitrates from renderers
			RendererConfiguration.calculateAllSpeeds();
		}
		configuration.setProperty(KEY_AUTOMATIC_MAXIMUM_BITRATE, b);
	}

	public String pingPath() {
		return getString(KEY_PING_PATH, null);
	}

	public boolean isSpeedDbg() {
		return getBoolean(KEY_SPEED_DBG, false);
	}

	public int mediaLibrarySort() {
		return getInt(KEY_MEDIA_LIB_SORT, UMSUtils.SORT_NO_SORT);
	}

	public boolean getWebAutoCont(Format f) {
		String key = KEY_WEB_CONT_VIDEO;
		boolean def = false;
		if (f.isAudio()) {
			key = KEY_WEB_CONT_AUDIO;
			def = true;
		}
		if (f.isImage()) {
			key = KEY_WEB_CONT_IMAGE;
			def = false;
		}
		return getBoolean(key, def);
	}

	public boolean getWebAutoLoop(Format f) {
		String key = KEY_WEB_LOOP_VIDEO;
		if (f.isAudio()) {
			key = KEY_WEB_LOOP_AUDIO;
		}
		if (f.isImage()) {
			key = KEY_WEB_LOOP_IMAGE;
		}
		return getBoolean(key, false);
	}

	public int getWebImgSlideDelay() {
		return getInt(KEY_WEB_IMAGE_SLIDE, 0);
	}
}
