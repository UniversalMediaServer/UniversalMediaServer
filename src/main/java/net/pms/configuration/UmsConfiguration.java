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
package net.pms.configuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sun.jna.Platform;
import ch.qos.logback.classic.Level;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.CodeEnter;
import net.pms.encoders.Engine;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.EngineId;
import net.pms.encoders.FFmpegLogLevels;
import net.pms.encoders.StandardEngineId;
import net.pms.formats.Format;
import net.pms.gui.GuiManager;
import net.pms.platform.PlatformProgramPaths;
import net.pms.platform.PlatformUtils;
import net.pms.platform.TempFolder;
import net.pms.platform.windows.WindowsRegistry;
import net.pms.renderers.ConnectedRenderers;
import net.pms.service.Services;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import net.pms.util.CoverSupplier;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import net.pms.util.FileUtil.FileLocation;
import net.pms.util.FullyPlayedAction;
import net.pms.util.InvalidArgumentException;
import net.pms.util.IpFilter;
import net.pms.util.Languages;
import net.pms.util.LogSystemInformationMode;
import net.pms.util.ProgramExecutableType;
import net.pms.util.PropertiesUtil;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleColor;
import net.pms.util.UMSUtils;
import net.pms.util.UniqueList;

/**
 * Container for all configurable UMS settings. Settings are typically defined by three things:
 * a unique key for use in the configuration file "UMS.conf", a getter (and setter) method and
 * a default value. When a key cannot be found in the current configuration, the getter will
 * return a default value. Setters only store a value, they do not permanently save it to
 * file.
 */
public class UmsConfiguration extends BaseConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(UmsConfiguration.class);

	/*
	 * Hard coded values
	 */
	private static final int DEFAULT_MEDIA_SERVER_PORT = 5001;
	private static final int DEFAULT_WEB_GUI_PORT = 9001;
	private static final int DEFAULT_WEB_PLAYER_PORT = 9002;
	// 90000 lines is approximately 10 MiB depending on locale and message length
	private static final int LOGGING_LOGS_TAB_LINEBUFFER_MAX = 90000;
	private static final int LOGGING_LOGS_TAB_LINEBUFFER_MIN = 100;
	private static final int LOGGING_LOGS_TAB_LINEBUFFER_STEP = 500;
	private static final String DEFAULT_PROFILE_FILENAME = "UMS.conf";
	private static final String PROPERTY_PROFILE_PATH = "ums.profile.path";
	private static final String ENV_PROFILE_PATH = "UMS_PROFILE";
	private static final String DEFAULT_SHARED_CONF_FILENAME = "SHARED.conf";
	private static final String DEFAULT_CREDENTIALS_FILENAME = "UMS.cred";
	/*
	 * MEncoder has a hardwired maximum of 8 threads for -lavcopts and 16
	 * for -lavdopts.
	 * The Windows SubJunk Builds can take 16 for both, but we keep it at 8
	 * for compatibility with other operating systems.
	 */
	private static final int MENCODER_MAX_THREADS = 8;
	private static final int WEB_MAX_THREADS = 100;
	private static final int MAX_MAX_MEMORY_DEFAULT_SIZE = 400;
	private static final int BUFFER_MEMORY_FACTOR = 368;
	private static final char LIST_SEPARATOR = ',';

	private static volatile boolean enabledEnginesBuilt = false;
	private static final ReentrantReadWriteLock ENABLED_ENGINES_LOCK = new ReentrantReadWriteLock();
	private static UniqueList<EngineId> enabledEngines;

	private static volatile boolean enginesPriorityBuilt = false;
	private static final ReentrantReadWriteLock ENGINES_PRIORITY_LOCK = new ReentrantReadWriteLock();
	private static UniqueList<EngineId> enginesPriority;

	// The name of the subdirectory under which UMS config files are stored for this build (default: UMS).
	// See Build for more details
	private static final String PROFILE_DIRECTORY_NAME = Build.getProfileDirectoryName();

	// The default profile name displayed on the renderer
	private static String hostName;

	private static String defaultAviSynthScript;

	private static int maxMaxMemoryBufferSize = MAX_MAX_MEMORY_DEFAULT_SIZE;

	/*
		The following code enables a single setting - UMS_PROFILE - to be used to
		initialize PROFILE_PATH i.e. the path to the current session's profile (AKA UMS.conf).
		It also initializes PROFILE_DIRECTORY - i.e. the directory the profile is located in -
		which is needed to detect the default SHARED.conf location (anything else?).

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

	// Path to directory containing UMS config files
	private static final String PROFILE_DIRECTORY;

	// Absolute path to profile file e.g. /path/to/UMS.conf
	private static final String PROFILE_PATH;

	// Absolute path to SHARED.conf file e.g. /path/to/SHARED.conf
	private static String sharedConfPath;

	// Absolute path to skel (default) profile file e.g. /etc/skel/.config/universalmediaserver/UMS.conf
	// "project.skelprofile.dir" project property
	private static final String SKEL_PROFILE_PATH;

	private static final String SYSTEM_PROFILE_DIRECTORY;

	/*
	 * Configuration file keys
	 */
	private static final String KEY_3D_SUBTITLES_DEPTH = "3d_subtitles_depth";
	private static final String KEY_ALTERNATE_SUBTITLES_FOLDER = "alternate_subtitles_folder";
	private static final String KEY_ALTERNATE_THUMB_FOLDER = "alternate_thumb_folder";
	private static final String KEY_APPEND_PROFILE_NAME = "append_profile_name";
	private static final String KEY_ATZ_LIMIT = "atz_limit";
	private static final String KEY_AUTOMATIC_DISCOVER = "automatic_discover";
	private static final String KEY_AUTOMATIC_MAXIMUM_BITRATE = "automatic_maximum_bitrate";
	private static final String KEY_AUDIO_BITRATE = "audio_bitrate";
	private static final String KEY_AUDIO_CHANNEL_COUNT = "audio_channels";
	private static final String KEY_AUDIO_EMBED_DTS_IN_PCM = "audio_embed_dts_in_pcm";
	private static final String KEY_AUDIO_LANGUAGES = "audio_languages";
	private static final String KEY_AUDIO_LIKES_IN_ROOT_FOLDER = "audio_likes_visible_root";
	private static final String KEY_AUDIO_REMUX_AC3 = "audio_remux_ac3";
	private static final String KEY_AUDIO_RESAMPLE = "audio_resample";
	private static final String KEY_AUDIO_SUB_LANGS = "audio_subtitles_languages";
	private static final String KEY_AUDIO_THUMBNAILS_METHOD = "audio_thumbnails_method";
	private static final String KEY_AUDIO_USE_PCM = "audio_use_pcm";
	private static final String KEY_AUDIO_UPDATE_RATING_TAG = "audio_update_rating_tag";
	private static final String KEY_AUTHENTICATE_LOCALHOST_AS_ADMIN = "authenticate_localhost_as_admin";
	private static final String KEY_AUTHENTICATION_ENABLED = "authentication_enabled";
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
	private static final String KEY_API_KEY = "api_key";
	private static final String KEY_CHAPTER_INTERVAL = "chapter_interval";
	private static final String KEY_CHAPTER_SUPPORT = "chapter_support";
	private static final String KEY_CHROMECAST_DBG = "chromecast_debug";
	private static final String KEY_CHROMECAST_EXT = "chromecast_extension";
	private static final String KEY_CODE_CHARS = "code_charset";
	private static final String KEY_CODE_THUMBS = "code_show_thumbs_no_code";
	private static final String KEY_CODE_TMO = "code_valid_timeout";
	private static final String KEY_CODE_USE = "code_enable";
	private static final String KEY_SORT_AUDIO_TRACKS_BY_ALBUM_POSITION = "sort_audio_tracks_by_album_position";
	private static final String KEY_DATABASE_MEDIA_CACHE_SIZE_KB = "database_media_cache_size";
	private static final String KEY_DATABASE_MEDIA_USE_CACHE_SOFT = "database_media_use_cache_soft";
	private static final String KEY_DATABASE_MEDIA_USE_MEMORY_INDEXES = "database_media_use_memory_indexes";
	private static final String KEY_DISABLE_EXTERNAL_ENTITIES = "disable_external_entities";
	private static final String KEY_DISABLE_FAKESIZE = "disable_fakesize";
	private static final String KEY_DISABLE_SUBTITLES = "disable_subtitles";
	private static final String KEY_DISABLE_TRANSCODE_FOR_EXTENSIONS = "disable_transcode_for_extensions";
	private static final String KEY_DISABLE_TRANSCODING = "disable_transcoding";
	private static final String KEY_DVDISO_THUMBNAILS = "dvd_isos_thumbnails";
	private static final String KEY_DYNAMIC_PLS = "dynamic_playlist";
	private static final String KEY_DYNAMIC_PLS_AUTO_SAVE = "dynamic_playlist_auto_save";
	private static final String KEY_DYNAMIC_PLS_HIDE = "dynamic_playlist_hide_folder";
	private static final String KEY_DYNAMIC_PLS_SAVE_PATH = "dynamic_playlist_save_path";
	private static final String KEY_ENCODED_AUDIO_PASSTHROUGH = "encoded_audio_passthrough";
	private static final String KEY_ENGINES = "engines";
	private static final String KEY_ENGINES_PRIORITY = "engines_priority";
	/* Start without external network (increase startup speed) */
	private static final String KEY_EXTERNAL_NETWORK = "external_network";
	private static final String KEY_FFMPEG_AVAILABLE_GPU_ACCELERATION_METHODS = "ffmpeg_available_gpu_acceleration_methods";
	private static final String KEY_FFMPEG_AVISYNTH_2D_TO_3D = "ffmpeg_avisynth_2d_to_3d_conversion";
	private static final String KEY_FFMPEG_AVISYNTH_CONVERSION_ALGORITHM_2D_TO_3D = "ffmpeg_avisynth_conversion_algorithm_index_2d_to_3d";
	private static final String KEY_FFMPEG_AVISYNTH_CONVERT_FPS = "ffmpeg_avisynth_convertfps";
	private static final String KEY_FFMPEG_AVISYNTH_FRAME_STRETCH_FACTOR_2D_TO_3D = "ffmpeg_avisynth_frame_stretch_factor_2d_to_3d";
	private static final String KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE = "ffmpeg_avisynth_horizontal_resize";
	private static final String KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE_RESOLUTION = "ffmpeg_avisynth_horizontal_resize_resolution";
	private static final String KEY_FFMPEG_AVISYNTH_INTERFRAME = "ffmpeg_avisynth_interframe";
	private static final String KEY_FFMPEG_AVISYNTH_INTERFRAME_GPU = "ffmpeg_avisynth_interframegpu";
	private static final String KEY_FFMPEG_AVISYNTH_LIGHT_OFFSET_FACTOR_2D_TO_3D = "ffmpeg_avisynth_light_offset_factor_2d_to_3d";
	private static final String KEY_FFMPEG_AVISYNTH_MULTITHREADING = "ffmpeg_avisynth_multithreading";
	private static final String KEY_FFMPEG_AVISYNTH_OUTPUT_FORMAT_3D = "ffmpeg_avisynth_output_format_index_3d";
	private static final String KEY_FFMPEG_AVISYNTH_USE_FFMS2 = "ffmpeg_avisynth_use_ffms2";
	private static final String KEY_FFMPEG_FONTCONFIG = "ffmpeg_fontconfig";
	private static final String KEY_FFMPEG_GPU_DECODING_ACCELERATION_METHOD = "ffmpeg_gpu_decoding_acceleration_method";
	private static final String KEY_FFMPEG_GPU_DECODING_ACCELERATION_THREAD_NUMBER = "ffmpeg_gpu_decoding_acceleration_thread_number";
	private static final String KEY_FFMPEG_GPU_ENCODING_H264_ACCELERATION_METHOD = "ffmpeg_gpu_encoding_H264_acceleration_method";
	private static final String KEY_FFMPEG_GPU_ENCODING_H265_ACCELERATION_METHOD = "ffmpeg_gpu_encoding_H265_acceleration_method";
	private static final String KEY_FFMPEG_LOGGING_LEVEL = "ffmpeg_logging_level";
	private static final String KEY_FFMPEG_MENCODER_PROBLEMATIC_SUBTITLES = "ffmpeg_mencoder_problematic_subtitles";
	private static final String KEY_FFMPEG_MULTITHREADING = "ffmpeg_multithreading";
	private static final String KEY_FFMPEG_MUX_TSMUXER_COMPATIBLE = "ffmpeg_mux_tsmuxer_compatible";
	private static final String KEY_FFMPEG_SOX = "ffmpeg_sox";
	private static final String KEY_FIX_25FPS_AV_MISMATCH = "fix_25fps_av_mismatch";
	private static final String KEY_FOLDER_LIMIT = "folder_limit";
	private static final String KEY_FOLDER_NAMES_IGNORED = "folder_names_ignored";
	private static final String KEY_FORCE_EXTERNAL_SUBTITLES = "force_external_subtitles";
	private static final String KEY_FORCE_TRANSCODE_FOR_EXTENSIONS = "force_transcode_for_extensions";
	private static final String KEY_FORCED_SUBTITLE_LANGUAGE = "forced_subtitle_language";
	private static final String KEY_FORCED_SUBTITLE_TAGS = "forced_subtitle_tags";
	private static final String KEY_FULLY_PLAYED_ACTION = "fully_played_action";
	private static final String KEY_FULLY_PLAYED_OUTPUT_DIRECTORY = "fully_played_output_directory";
	private static final String KEY_GPU_ACCELERATION = "gpu_acceleration";
	private static final String KEY_GUI_LOG_SEARCH_CASE_SENSITIVE = "gui_log_search_case_sensitive";
	private static final String KEY_GUI_LOG_SEARCH_MULTILINE = "gui_log_search_multiline";
	private static final String KEY_GUI_LOG_SEARCH_USE_REGEX = "gui_log_search_use_regex";
	private static final String KEY_HIDE_ADVANCED_OPTIONS = "hide_advanced_options";
	private static final String KEY_HIDE_EMPTY_FOLDERS = "hide_empty_folders";
	private static final String KEY_HIDE_ENGINENAMES = "hide_enginenames";
	private static final String KEY_HIDE_EXTENSIONS = "hide_extensions";
	private static final String KEY_IGNORE_THE_WORD_A_AND_THE = "ignore_the_word_a_and_the";
	private static final String KEY_IMAGE_THUMBNAILS_ENABLED = "image_thumbnails";
	private static final String KEY_INFO_DB_RETRY = "infodb_retry";
	private static final String KEY_IP_FILTER = "ip_filter";
	private static final String KEY_ITUNES_LIBRARY_PATH = "itunes_library_path";
	private static final String KEY_JWT_SIGNER_SECRET = "jwt_secret";
	private static final String KEY_LANGUAGE = "language";
	private static final String KEY_LIVE_SUBTITLES_KEEP = "live_subtitles_keep";
	private static final String KEY_LIVE_SUBTITLES_LIMIT = "live_subtitles_limit";
	private static final String KEY_LOG_SYSTEM_INFO = "log_system_info";
	private static final String KEY_LOGGING_LOGFILE_NAME = "logging_logfile_name";
	private static final String KEY_LOGGING_BUFFERED = "logging_buffered";
	private static final String KEY_LOGGING_FILTER_CONSOLE = "logging_filter_console";
	private static final String KEY_LOGGING_FILTER_LOGS_TAB = "logging_filter_logs_tab";
	private static final String KEY_LOGGING_LOGS_TAB_LINEBUFFER = "logging_logs_tab_linebuffer";
	private static final String KEY_LOGGING_SYSLOG_FACILITY = "logging_syslog_facility";
	private static final String KEY_LOGGING_SYSLOG_HOST = "logging_syslog_host";
	private static final String KEY_LOGGING_SYSLOG_PORT = "logging_syslog_port";
	private static final String KEY_LOGGING_USE_SYSLOG = "logging_use_syslog";
	private static final String KEY_LOG_DATABASE = "log_database";
	private static final String KEY_MANAGED_PLAYLIST_FOLDER = "managed_playlist_folder";
	private static final String KEY_MAX_AUDIO_BUFFER = "maximum_audio_buffer_size";
	private static final String KEY_MAX_BITRATE = "maximum_bitrate";
	private static final String KEY_MAX_MEMORY_BUFFER_SIZE = "maximum_video_buffer_size";
	private static final String KEY_MENCODER_ASS = "mencoder_ass";
	private static final String KEY_MENCODER_AC3_FIXED = "mencoder_ac3_fixed";
	private static final String KEY_MENCODER_CODEC_SPECIFIC_SCRIPT = "mencoder_codec_specific_script";
	private static final String KEY_MENCODER_CUSTOM_OPTIONS = "mencoder_custom_options";
	private static final String KEY_MENCODER_FONT_CONFIG = "mencoder_fontconfig";
	private static final String KEY_MENCODER_FORCE_FPS = "mencoder_forcefps";
	private static final String KEY_MENCODER_INTELLIGENT_SYNC = "mencoder_intelligent_sync";
	private static final String KEY_MENCODER_MAX_THREADS = "mencoder_max_threads";
	private static final String KEY_MENCODER_MUX_COMPATIBLE = "mencoder_mux_compatible";
	private static final String KEY_MENCODER_MT = "mencoder_mt";
	private static final String KEY_MENCODER_NO_OUT_OF_SYNC = "mencoder_nooutofsync";
	private static final String KEY_MENCODER_NOASS_BLUR = "mencoder_noass_blur";
	private static final String KEY_MENCODER_NOASS_OUTLINE = "mencoder_noass_outline";
	private static final String KEY_MENCODER_NOASS_SCALE = "mencoder_noass_scale";
	private static final String KEY_MENCODER_NOASS_SUBPOS = "mencoder_noass_subpos";
	private static final String KEY_MENCODER_NORMALIZE_VOLUME = "mencoder_normalize_volume";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT = "mencoder_overscan_compensation_height";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH = "mencoder_overscan_compensation_width";
	private static final String KEY_MENCODER_REMUX_MPEG2 = "mencoder_remux_mpeg2";
	private static final String KEY_MENCODER_SCALER = "mencoder_scaler";
	private static final String KEY_MENCODER_SCALEX = "mencoder_scalex";
	private static final String KEY_MENCODER_SCALEY = "mencoder_scaley";
	private static final String KEY_MENCODER_SUB_FRIBIDI = "mencoder_subfribidi";
	private static final String KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY = "mencoder_usepcm_for_hq_audio_only";
	private static final String KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY = "mencoder_vobsub_subtitle_quality";
	private static final String KEY_MENCODER_YADIF = "mencoder_yadif";
	private static final String KEY_MIN_MEMORY_BUFFER_SIZE = "minimum_video_buffer_size";
	private static final String KEY_MIN_PLAY_TIME = "minimum_watched_play_time";
	private static final String KEY_MIN_PLAY_TIME_FILE = "min_playtime_file";
	private static final String KEY_MIN_PLAY_TIME_WEB = "min_playtime_web";
	private static final String KEY_MIN_STREAM_BUFFER = "minimum_web_buffer_size";
	private static final String KEY_MINIMIZED = "minimized";
	private static final String KEY_MPEG2_MAIN_SETTINGS = "mpeg2_main_settings";
	private static final String KEY_MUX_ALLAUDIOTRACKS = "tsmuxer_mux_all_audiotracks";
	private static final String KEY_NETWORK_DEVICES_FILTER = "network_devices_filter";
	private static final String KEY_NETWORK_INTERFACE = "network_interface";
	private static final String KEY_NUMBER_OF_CPU_CORES = "number_of_cpu_cores";
	private static final String KEY_OPEN_ARCHIVES = "enable_archive_browsing";
	private static final String KEY_OVERSCAN = "mencoder_overscan";
	private static final String KEY_PLAYLIST_AUTO_ADD_ALL = "playlist_auto_add_all";
	private static final String KEY_PLAYLIST_AUTO_CONT = "playlist_auto_continue";
	private static final String KEY_PLAYLIST_AUTO_PLAY = "playlist_auto_play";
	private static final String KEY_PRETTIFY_FILENAMES = "prettify_filenames";
	private static final String KEY_PREVENT_SLEEP = "prevent_sleep";
	private static final String KEY_PROFILE_NAME = "name";
	private static final String KEY_RENDERER_DEFAULT = "renderer_default";
	private static final String KEY_RENDERER_FORCE_DEFAULT = "renderer_force_default";
	private static final String KEY_RESUME = "resume";
	private static final String KEY_RESUME_BACK = "resume_back";
	private static final String KEY_RESUME_KEEP_TIME = "resume_keep_time";
	private static final String KEY_RESUME_REWIND = "resume_rewind";
	private static final String KEY_ROOT_LOG_LEVEL = "log_level";
	private static final String KEY_RUN_WIZARD = "run_wizard";
	private static final String KEY_SCAN_SHARED_FOLDERS_ON_STARTUP = "scan_shared_folders_on_startup";
	private static final String KEY_SCRIPT_DIR = "script_dir";
	private static final String KEY_SEARCH_FOLDER = "search_folder";
	private static final String KEY_SEARCH_IN_FOLDER = "search_in_folder";
	private static final String KEY_SEARCH_RECURSE = "search_recurse"; // legacy option
	private static final String KEY_SEARCH_RECURSE_DEPTH = "search_recurse_depth";
	private static final String KEY_SELECTED_RENDERERS = "selected_renderers";
	private static final String KEY_SERVER_ENGINE = "server_engine";
	private static final String KEY_SERVER_HOSTNAME = "hostname";
	private static final String KEY_SERVER_NAME = "server_name";
	private static final String KEY_SERVER_PORT = "port";
	private static final String KEY_SHARED_CONF_PATH = "shared_conf";
	private static final String KEY_SHARES = "shares";
	private static final String KEY_SHOW_APERTURE_LIBRARY = "show_aperture_library";
	private static final String KEY_SHOW_INFO_ABOUT_AUTOMATIC_VIDEO_SETTING = "show_info";
	private static final String KEY_SHOW_IPHOTO_LIBRARY = "show_iphoto_library";
	private static final String KEY_SHOW_ITUNES_LIBRARY = "show_itunes_library";
	private static final String KEY_SHOW_LIVE_SUBTITLES_FOLDER = "show_live_subtitles_folder";
	private static final String KEY_SHOW_MEDIA_LIBRARY_FOLDER = "show_media_library_folder";
	private static final String KEY_SHOW_RECENTLY_PLAYED_FOLDER = "show_recently_played_folder";
	private static final String KEY_SHOW_SERVER_SETTINGS_FOLDER = "show_server_settings_folder";
	private static final String KEY_SHOW_SPLASH_SCREEN = "show_splash_screen";
	private static final String KEY_SHOW_TRANSCODE_FOLDER = "show_transcode_folder";
	private static final String KEY_SINGLE = "single_instance";
	private static final String KEY_SKIP_LOOP_FILTER_ENABLED = "mencoder_skip_loop_filter";
	private static final String KEY_SKIP_NETWORK_INTERFACES = "skip_network_interfaces";
	private static final String KEY_SORT_METHOD = "sort_method";
	private static final String KEY_SORT_PATHS = "sort_paths";
	private static final String KEY_SPEED_DBG = "speed_debug";
	private static final String KEY_SUBS_COLOR = "subtitles_color";
	private static final String KEY_SUBS_FONT = "subtitles_font";
	private static final String KEY_SUBS_INFO_LEVEL = "subs_info_level";
	private static final String KEY_SUBTITLES_CODEPAGE = "subtitles_codepage";
	private static final String KEY_SUBTITLES_LANGUAGES = "subtitles_languages";
	private static final String KEY_TEMP_FOLDER_PATH = "temp_directory";
	private static final String KEY_THUMBNAIL_GENERATION_ENABLED = "generate_thumbnails";
	private static final String KEY_THUMBNAIL_SEEK_POS = "thumbnail_seek_position";
	private static final String KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS = "transcode_block_multiple_connections";
	private static final String KEY_TRANSCODE_FOLDER_NAME = "transcode_folder_name";
	private static final String KEY_TRANSCODE_KEEP_FIRST_CONNECTION = "transcode_keep_first_connection";
	private static final String KEY_TSMUXER_FORCEFPS = "tsmuxer_forcefps";
	private static final String KEY_UPNP_ALIVE_DELAY = "upnp_alive_delay";
	private static final String KEY_UPNP_DEBUG = "upnp_debug";
	private static final String KEY_UPNP_ENABLED = "upnp_enable";
	private static final String KEY_UPNP_PORT = "upnp_port";
	private static final String KEY_USE_CACHE = "use_cache";
	private static final String KEY_USE_EMBEDDED_SUBTITLES_STYLE = "use_embedded_subtitles_style";
	private static final String KEY_USE_IMDB_INFO = "use_imdb_info";
	private static final String KEY_USE_MPLAYER_FOR_THUMBS = "use_mplayer_for_video_thumbs";
	private static final String KEY_USE_SYMLINKS_TARGET_FILE = "use_symlinks_target_file";
	private static final String KEY_UUID = "uuid";
	private static final String KEY_VIDEOTRANSCODE_START_DELAY = "videotranscode_start_delay";
	private static final String KEY_VLC_AUDIO_SYNC_ENABLED = "vlc_audio_sync_enabled";
	private static final String KEY_VLC_SAMPLE_RATE = "vlc_sample_rate";
	private static final String KEY_VLC_SAMPLE_RATE_OVERRIDE = "vlc_sample_rate_override";
	private static final String KEY_VLC_SCALE = "vlc_scale";
	private static final String KEY_VLC_SUBTITLE_ENABLED = "vlc_subtitle_enabled";
	private static final String KEY_VLC_USE_EXPERIMENTAL_CODECS = "vlc_use_experimental_codecs";
	private static final String KEY_VLC_USE_HW_ACCELERATION = "vlc_use_hw_acceleration";
	private static final String KEY_WAS_YOUTUBE_DL_ENABLED_ONCE = "was_youtube_dl_enabled_once";
	private static final String KEY_WEB_GUI_ON_START = "web_gui_on_start";
	private static final String KEY_WEB_GUI_PORT = "web_gui_port";
	private static final String KEY_WEB_LOW_SPEED = "web_low_speed";
	private static final String KEY_WEB_PATH = "web_path";
	private static final String KEY_WEB_PLAYER_AUTH = "web_player_auth";
	private static final String KEY_WEB_PLAYER_CONT_AUDIO = "web_player_continue_audio";
	private static final String KEY_WEB_PLAYER_CONT_IMAGE = "web_player_continue_image";
	private static final String KEY_WEB_PLAYER_CONT_VIDEO = "web_player_continue_video";
	private static final String KEY_WEB_PLAYER_CONTROLLABLE = "web_player_controllable";
	private static final String KEY_WEB_PLAYER_CONTROLS = "web_player_controls";
	private static final String KEY_WEB_PLAYER_DOWNLOAD = "web_player_download";
	private static final String KEY_WEB_PLAYER_ENABLE = "web_player_enable";
	private static final String KEY_WEB_PLAYER_HTTPS = "web_player_https";
	private static final String KEY_WEB_PLAYER_IMAGE_SLIDE = "web_player_image_show_delay";
	private static final String KEY_WEB_PLAYER_LOOP_AUDIO = "web_player_loop_audio";
	private static final String KEY_WEB_PLAYER_LOOP_IMAGE = "web_player_loop_image";
	private static final String KEY_WEB_PLAYER_LOOP_VIDEO = "web_player_loop_video";
	private static final String KEY_WEB_PLAYER_MP4_TRANS = "web_mp4_trans";
	private static final String KEY_WEB_PLAYER_PORT = "web_player_port";
	private static final String KEY_WEB_PLAYER_SUB_LANG = "web_use_browser_sub_lang";
	private static final String KEY_WEB_PLAYER_SUBS_TRANS = "web_subtitles_transcoded";
	private static final String KEY_WEB_THREADS = "web_threads";
	private static final String KEY_WEB_TRANSCODE = "web_transcode";
	private static final String KEY_X264_CONSTANT_RATE_FACTOR = "x264_constant_rate_factor";

	/**
	 * Old Web interface stuff
	 */
	// TODO: remove on old player removal
	@Deprecated
	private static final String KEY_WEB_AUTHENTICATE = "web_authenticate";
	@Deprecated
	private static final String KEY_BUMP_ADDRESS = "bump";
	@Deprecated
	private static final String KEY_BUMP_IPS = "allowed_bump_ips";
	@Deprecated
	private static final String KEY_BUMP_JS = "bump.js";
	@Deprecated
	private static final String KEY_BUMP_SKIN_DIR = "bump.skin";
	@Deprecated
	private static final String KEY_WEB_BROWSE_LANG = "web_use_browser_lang";
	@Deprecated
	private static final String KEY_WEB_FLASH = "web_flash";
	@Deprecated
	private static final String KEY_WEB_HEIGHT = "web_height";
	@Deprecated
	private static final String KEY_WEB_SIZE = "web_size";
	@Deprecated
	private static final String KEY_WEB_WIDTH = "web_width";

	/**
	 * The map of keys that need to be refactored.
	 * Keys will be refactored on next start.
	 */
	private static final Map<String, String> REFACTORED_KEYS = Map.ofEntries(
		//since 11.5
		new AbstractMap.SimpleEntry<>("web_enable", KEY_WEB_PLAYER_ENABLE),
		new AbstractMap.SimpleEntry<>("web_continue_audio", KEY_WEB_PLAYER_CONT_AUDIO),
		new AbstractMap.SimpleEntry<>("web_continue_image", KEY_WEB_PLAYER_CONT_IMAGE),
		new AbstractMap.SimpleEntry<>("web_continue_video", KEY_WEB_PLAYER_CONT_VIDEO),
		new AbstractMap.SimpleEntry<>("web_https", KEY_WEB_PLAYER_HTTPS),
		new AbstractMap.SimpleEntry<>("web_control", KEY_WEB_PLAYER_CONTROLLABLE),
		new AbstractMap.SimpleEntry<>("web_image_show_delay", KEY_WEB_PLAYER_IMAGE_SLIDE),
		new AbstractMap.SimpleEntry<>("web_loop_audio", KEY_WEB_PLAYER_LOOP_AUDIO),
		new AbstractMap.SimpleEntry<>("web_loop_image", KEY_WEB_PLAYER_LOOP_IMAGE),
		new AbstractMap.SimpleEntry<>("web_loop_video", KEY_WEB_PLAYER_LOOP_VIDEO),
		//since 11.6
		new AbstractMap.SimpleEntry<>("fmpeg_sox", KEY_FFMPEG_SOX),
		new AbstractMap.SimpleEntry<>("ALIVE_delay", KEY_UPNP_ALIVE_DELAY)
	);

	/**
	 * The set of keys that was removed.
	 * Keys will be delete on next start.
	 */
	private static final Set<String> REMOVED_KEYS = Set.of(
		"alternativeffmpegpath",	//not used
		"buffer_max",				//not used
		"http_engine_v2",			//replaced
		"hide_subs_info",			//replaced
		"media_lib_sort",			//not used
		"no_shared",				//not used
		"plugin_purge",				//not used
		"proxy"						//not used
	);

	/**
	 * The set of keys defining when the media server should be restarted due to a
	 * configuration change.
	 */
	public static final Set<String> NEED_MEDIA_SERVER_RELOAD_FLAGS = Set.of(
		KEY_CHROMECAST_EXT,
		KEY_NETWORK_INTERFACE,
		KEY_SERVER_ENGINE,
		KEY_SERVER_HOSTNAME,
		KEY_SERVER_PORT,
		KEY_UPNP_ENABLED
	);

	/**
	 * The set of keys defining when the HTTP web player server should be restarted
	 * due to a configuration change.
	 */
	public static final Set<String> NEED_WEB_PLAYER_SERVER_RELOAD_FLAGS = Set.of(
		KEY_WEB_PLAYER_ENABLE,
		KEY_WEB_PLAYER_HTTPS,
		KEY_WEB_PLAYER_PORT
	);

	/**
	 * The set of keys defining when the HTTP web gui server should be restarted
	 * due to a configuration change.
	 */
	public static final Set<String> NEED_WEB_GUI_SERVER_RELOAD_FLAGS = Set.of(
		KEY_WEB_GUI_PORT
	);

	/**
	 * The set of keys defining when the renderers should be reloaded due to a
	 * configuration change.
	 */
	public static final Set<String> NEED_RENDERERS_RELOAD_FLAGS = Set.of(
		KEY_RENDERER_DEFAULT,
		KEY_RENDERER_FORCE_DEFAULT,
		KEY_SELECTED_RENDERERS
	);

	/**
	 * The set of keys defining when the media library has to reset due to a
	 * configuration change.
	 *
	 * It will need a renderers reload as renderers build from it.
	 */
	public static final Set<String> NEED_MEDIA_LIBRARY_RELOAD_FLAGS = Set.of(
		KEY_FULLY_PLAYED_ACTION,
		KEY_SHOW_RECENTLY_PLAYED_FOLDER,
		KEY_USE_CACHE
	);

	/**
	 * The set of keys defining when the renderers has to rebuild their root folder
	 * due to a configuration change.
	 */
	public static final Set<String> NEED_RENDERERS_ROOT_RELOAD_FLAGS = Set.of(
		KEY_ATZ_LIMIT,
		KEY_AUDIO_THUMBNAILS_METHOD,
		KEY_CHAPTER_SUPPORT,
		KEY_DISABLE_TRANSCODE_FOR_EXTENSIONS,
		KEY_DISABLE_TRANSCODING,
		KEY_FORCE_TRANSCODE_FOR_EXTENSIONS,
		KEY_HIDE_EMPTY_FOLDERS,
		KEY_OPEN_ARCHIVES,
		KEY_PRETTIFY_FILENAMES,
		KEY_SHOW_APERTURE_LIBRARY,
		KEY_SHOW_IPHOTO_LIBRARY,
		KEY_SHOW_ITUNES_LIBRARY,
		KEY_SHOW_LIVE_SUBTITLES_FOLDER,
		KEY_SHOW_MEDIA_LIBRARY_FOLDER,
		KEY_SHOW_SERVER_SETTINGS_FOLDER,
		KEY_SHOW_TRANSCODE_FOLDER,
		KEY_SORT_METHOD
	);

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

		// ensure that the SYSTEM_PROFILE_DIRECTORY exists
		File systemProfileDirectory = new File(SYSTEM_PROFILE_DIRECTORY);
		if (!systemProfileDirectory.exists()) {
			systemProfileDirectory.mkdirs();
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

	// Path to default logfile directory
	private String defaultLogFileDir = null;

	// Path to default zipped logfile directory
	private String defaultZippedLogFileDir = null;

	@Nonnull
	private final PlatformProgramPaths programPaths;

	protected TempFolder tempFolder;
	protected IpFilter filter;

	/**
	 * Default constructor that will attempt to load the PMS configuration file
	 * from the profile path.
	 *
	 * @throws org.apache.commons.configuration.ConfigurationException
	 * @throws InterruptedException
	 */
	public UmsConfiguration() throws ConfigurationException, InterruptedException {
		this(true);
	}

	/**
	 * Constructor that will initialize the PMS configuration.
	 *
	 * @param loadFile Set to true to attempt to load the PMS configuration
	 *                 file from the profile path. Set to false to skip
	 *                 loading.
	 * @throws ConfigurationException
	 * @throws InterruptedException
	 */
	public UmsConfiguration(boolean loadFile) throws ConfigurationException, InterruptedException {
		super(true);
		if (loadFile) {
			File pmsConfFile = new File(PROFILE_PATH);

			try {
				((PropertiesConfiguration) configuration).load(pmsConfFile);
			} catch (ConfigurationException e) {
				if (Platform.isLinux() && SKEL_PROFILE_PATH != null) {
					LOGGER.debug("Failed to load {} ({}) - attempting to load skel profile", PROFILE_PATH, e.getMessage());
					File skelConfigFile = new File(SKEL_PROFILE_PATH);

					try {
						// Load defaults from skel profile, save them later to PROFILE_PATH
						((PropertiesConfiguration) configuration).load(skelConfigFile);
						LOGGER.info("Default configuration loaded from {}", SKEL_PROFILE_PATH);
					} catch (ConfigurationException ce) {
						LOGGER.warn("Can't load neither {}: {} nor {}: {}", PROFILE_PATH, e.getMessage(), SKEL_PROFILE_PATH, ce.getMessage());
					}
				} else {
					LOGGER.warn("Can't load {}: {}", PROFILE_PATH, e.getMessage());
				}
			}
		}

		((PropertiesConfiguration) configuration).setPath(PROFILE_PATH);
		for (Entry<String, String> refactoredKey : REFACTORED_KEYS.entrySet()) {
			if (configuration.containsKey(refactoredKey.getKey())) {
				Object value = configuration.getProperty(refactoredKey.getKey());
				configuration.setProperty(refactoredKey.getValue(), value);
				configuration.clearProperty(refactoredKey.getKey());
			}
		}
		/**
		 * This block is to recover old settings from v13 because of an accidental
		 * code merge from the v14 branch, which got released in 13.7.0-13.8.1.
		 */
		String networkDevicesFilter = getString(KEY_NETWORK_DEVICES_FILTER, null);
		if (StringUtils.isNotBlank(networkDevicesFilter)) {
			configuration.setProperty("ip_filter", networkDevicesFilter);
			configuration.clearProperty(KEY_NETWORK_DEVICES_FILTER);
		}
		for (String removedKey : REMOVED_KEYS) {
			configuration.clearProperty(removedKey);
		}
		tempFolder = new TempFolder(getString(KEY_TEMP_FOLDER_PATH, null));
		programPaths = new ConfigurableProgramPaths(configuration);
		filter = new IpFilter();
		PMS.setLocale(getLanguageLocale(true));
		//TODO: The line below should be removed once all calls to Locale.getDefault() is replaced with PMS.getLocale()
		Locale.setDefault(getLanguageLocale());

		// Set DEFAULT_AVI_SYNTH_SCRIPT according to language
		defaultAviSynthScript = "<movie>\n<sub>\n";

		long usableMemory = (Runtime.getRuntime().maxMemory() / 1048576) - BUFFER_MEMORY_FACTOR;
		if (usableMemory > MAX_MAX_MEMORY_DEFAULT_SIZE) {
			maxMaxMemoryBufferSize = (int) usableMemory;
		}

		if ("".equals(getJwtSecret())) {
			String randomUuid = UUID.randomUUID().toString();
			setJwtSecret(randomUuid.substring(0, randomUuid.indexOf("-")));
		}
	}

	protected UmsConfiguration(Configuration configuration, ConfigurationReader configurationReader) {
		// Just instantiate
		super(configuration, configurationReader);
		tempFolder = new TempFolder(getString(KEY_TEMP_FOLDER_PATH, null));
		filter = null;
		programPaths = new ConfigurableProgramPaths(configuration);
	}

	private static String verifyLogFolder(File folder, String fallbackTo) {
		try {
			FilePermissions permissions = FileUtil.getFilePermissions(folder);
			if (LOGGER.isTraceEnabled()) {
				if (!permissions.isFolder()) {
					LOGGER.trace("getDefaultLogFileFolder: \"{}\" is not a folder, falling back to {} for logging", folder.getAbsolutePath(), fallbackTo);
				} else if (!permissions.isBrowsable()) {
					LOGGER.trace("getDefaultLogFileFolder: \"{}\" is not browsable, falling back to {} for logging", folder.getAbsolutePath(), fallbackTo);
				} else if (!permissions.isWritable()) {
					LOGGER.trace("getDefaultLogFileFolder: \"{}\" is not writable, falling back to {} for logging", folder.getAbsolutePath(), fallbackTo);
				}
			}

			if (permissions.isFolder() && permissions.isBrowsable() && permissions.isWritable()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Default logfile folder set to: {}", folder.getAbsolutePath());
				}

				return folder.getAbsolutePath();
			}
		} catch (FileNotFoundException e) {
			LOGGER.trace("getDefaultLogFileFolder: \"{}\" not found, falling back to {} for logging: {}", folder.getAbsolutePath(), fallbackTo, e.getMessage());
		}

		return null;
	}

	/**
	 * @return first writable folder in the following order:
	 * <p>
	 *     1. (On Linux only) path to {@code /var/log/ums/%USERNAME%/}.
	 * </p>
	 * <p>
	 *     2. Path to profile folder ({@code ~/.config/UMS/} on Linux, {@code %ALLUSERSPROFILE%\UMS} on Windows and
	 *     {@code ~/Library/Application Support/UMS/} on Mac).
	 * </p>
	 * <p>
	 *     3. Path to user-defined temporary folder specified by {@code temp_directory} parameter in UMS.conf.
	 * </p>
	 * <p>
	 *     4. Path to system temporary folder.
	 * </p>
	 * <p>
	 *     5. Path to current working directory.
	 * </p>
	 */
	public synchronized String getDefaultLogFileFolder() {
		if (defaultLogFileDir == null) {
			if (Platform.isLinux()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("getDefaultLogFileFolder: System is Linux, trying \"/var/log/UMS/{}/\"", System.getProperty("user.name"));
				}

				final File logDirectory = new File("/var/log/UMS/" + System.getProperty("user.name") + "/");
				if (!logDirectory.exists()) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("getDefaultLogFileFolder: Trying to create: \"{}\"", logDirectory.getAbsolutePath());
					}

					try {
						FileUtils.forceMkdir(logDirectory);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("getDefaultLogFileFolder: \"{}\" created", logDirectory.getAbsolutePath());
						}
					} catch (IOException e) {
						LOGGER.debug("Could not create \"{}\": {}", logDirectory.getAbsolutePath(), e.getMessage());
					}
				}

				defaultLogFileDir = verifyLogFolder(logDirectory, "profile folder");
			}

			if (defaultLogFileDir == null) {
				// Log to profile directory if it is writable.
				final File profileDirectory = new File(PROFILE_DIRECTORY);
				defaultLogFileDir = verifyLogFolder(profileDirectory, "temporary folder");
			}

			if (defaultLogFileDir == null) {
				// Try user-defined temporary folder or fall back to system temporary folder.
				try {
					defaultLogFileDir = verifyLogFolder(getTempFolder(), "working folder");
				} catch (IOException e) {
					LOGGER.error("Could not determine default logfile folder, falling back to working directory: {}", e.getMessage());
					defaultLogFileDir = "";
				}
			}
		}

		return defaultLogFileDir;
	}

	public String getDefaultLogFileName() {
		String s = getString(KEY_LOGGING_LOGFILE_NAME, "debug.log");
		if (FileUtil.isValidFileName(s)) {
			return s;
		}

		return "debug.log";
	}

	public String getDefaultLogFilePath() {
		return FileUtil.appendPathSeparator(getDefaultLogFileFolder()) + getDefaultLogFileName();
	}

	/**
	 * @return Path to desktop folder ({@code ~/Desktop/UMS-log} on Linux, {@code %USERPROFILE%/Desktop/UMS-log} on Windows and
	 *     {@code ~/Desktop/UMS-log} on Mac). If desktop path is not writable then fall back to UMS log file path.
	 */
	public String getDefaultZippedLogFileFolder() {
		if (defaultZippedLogFileDir == null) {
			final File zippedLogDir = new File(
					System.getProperty("user.home") +
							File.separator + "Desktop" +
							File.separator + "UMS-log"
			);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("getDefaultLogFileFolder: Trying \"{}\"", zippedLogDir.getAbsolutePath());
			}

			if (!zippedLogDir.exists()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("getDefaultLogFileFolder: Trying to create: \"{}\"", zippedLogDir.getAbsolutePath());
				}

				try {
					FileUtils.forceMkdir(zippedLogDir);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("getDefaultLogFileFolder: \"{}\" created", zippedLogDir.getAbsolutePath());
					}
				} catch (IOException e) {
					LOGGER.debug("Could not create \"{}\": {}", zippedLogDir.getAbsolutePath(), e.getMessage());
				}
			}

			defaultZippedLogFileDir = verifyLogFolder(zippedLogDir, "UMS log file path");
		}

		if (defaultZippedLogFileDir == null) {
			// default to UMS log file path
			defaultZippedLogFileDir = getDefaultLogFilePath();
		}

		return defaultZippedLogFileDir;
	}

	public File getTempFolder() throws IOException {
		return tempFolder.getTempFolder();
	}

	public LogSystemInformationMode getLogSystemInformation() {
		LogSystemInformationMode defaultValue = LogSystemInformationMode.TRACE_ONLY;
		String value = getString(KEY_LOG_SYSTEM_INFO, defaultValue.toString());
		LogSystemInformationMode result = LogSystemInformationMode.typeOf(value);
		return result != null ? result : defaultValue;
	}

	public int getMencoderMaxThreads() {
		return Math.min(getInt(KEY_MENCODER_MAX_THREADS, getNumberOfCpuCores()), MENCODER_MAX_THREADS);
	}

	/**
	 * @return {@code true} if custom program paths are supported, {@code false}
	 *         otherwise.
	 */
	public boolean isCustomProgramPathsSupported() {
		return programPaths instanceof ConfigurableProgramPaths;
	}

	public boolean isAudioUpdateTag() {
		return getBoolean(KEY_AUDIO_UPDATE_RATING_TAG, false);
	}

	/**
	 * Returns the configured {@link ProgramExecutableType} for the specified
	 * {@link Engine}. Note that this can be different from the
	 * {@link Engine#currentExecutableType} for the same {@link Engine}.
	 *
	 * @param engine the {@link Engine} for which to get the configured
	 *            {@link ProgramExecutableType}.
	 * @return The configured {@link ProgramExecutableType}, the default
	 *         {@link ProgramExecutableType} if none is configured or
	 *         {@code null} if there is no default.
	 *
	 * @see Engine#getCurrentExecutableType()
	 */
	@Nullable
	public ProgramExecutableType getEngineExecutableType(@Nonnull Engine engine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine cannot be null");
		}

		ProgramExecutableType executableType = ProgramExecutableType.toProgramExecutableType(
			getString(engine.getExecutableTypeKey(), null),
			engine.getProgramInfo().getDefault()
		);

		// The default might also be null, in which case the current should be used.
		return executableType == null ? engine.getCurrentExecutableType() : executableType;
	}

	/**
	 * Sets the configured {@link ProgramExecutableType} for the specified
	 * {@link Engine}.
	 *
	 * @param engine the {@link Engine} for which to set the configured
	 *            {@link ProgramExecutableType}.
	 * @param executableType the {@link ProgramExecutableType} to set.
	 * @return {@code true} if a change was made, {@code false} otherwise.
	 */
	public boolean setEngineExecutableType(@Nonnull Engine engine, @Nonnull ProgramExecutableType executableType) {
		if (engine == null) {
			throw new IllegalArgumentException("engine cannot be null");
		}

		if (executableType == null) {
			throw new IllegalArgumentException("executableType cannot be null");
		}

		String key = engine.getExecutableTypeKey();
		if (key != null) {
			String currentValue = configuration.getString(key);
			String newValue = executableType.toRootString();
			if (newValue.equals(currentValue)) {
				return false;
			}

			configuration.setProperty(key, newValue);
			engine.determineCurrentExecutableType(executableType);
			return true;
		}

		return false;
	}

	/**
	 * Gets the configured {@link Path} for the specified {@link EngineId}. The
	 * {@link Engine} must be registered. No check for existence or search in
	 * the OS path is performed.
	 *
	 * @param engineId the {@link EngineId} for the registered {@link Engine}
	 *            whose configured {@link Path} to get.
	 * @return The configured {@link Path} or {@code null} if missing, blank or
	 *         invalid.
	 */
	@Nullable
	public Path getEngineCustomPath(@Nullable EngineId engineId) {
		if (engineId == null) {
			return null;
		}

		return UmsConfiguration.this.getEngineCustomPath(EngineFactory.getEngine(engineId, false, false));
	}

	/**
	 * Gets the configured {@link Path} for the specified {@link Engine}. No
	 * check for existence or search in the OS path is performed.
	 *
	 * @param engine the {@link Engine} whose configured {@link Path} to get.
	 * @return The configured {@link Path} or {@code null} if missing, blank or
	 *         invalid.
	 */
	@Nullable
	public Path getEngineCustomPath(@Nullable Engine engine) {
		if (
			engine == null ||
			StringUtils.isBlank(engine.getConfigurablePathKey()) ||
			!(programPaths instanceof ConfigurableProgramPaths)
		) {
			return null;
		}

		try {
			return ((ConfigurableProgramPaths) programPaths).getCustomProgramPath(engine.getConfigurablePathKey());
		} catch (ConfigurationException e) {
			LOGGER.warn(
				"An invalid executable path is configured for transcoding engine {}. The path is being ignored: {}",
				engine,
				e.getMessage()
			);
			LOGGER.trace("", e);
			return null;
		}
	}

	/**
	 * Sets the custom executable {@link Path} for the specified {@link Engine}
	 * in the configuration.
	 * <p>
	 * <b>Note:</b> This isn't normally what you'd want. To change the
	 * {@link Path} <b>for the {@link Engine} instance</b> in the same
	 * operation, use {@link Engine#setCustomExecutablePath} instead.
	 *
	 * @param engine the {@link Engine} whose custom executable {@link Path} to
	 *            set.
	 * @param path the {@link Path} to set or {@code null} to clear.
	 * @return {@code true} if a change was made to the configuration,
	 *         {@code false} otherwise.
	 * @throws IllegalStateException If {@code engine} has no configurable path
	 *             key or custom program paths aren't supported.
	 */
	public boolean setEngineCustomPath(@Nonnull Engine engine, @Nullable Path path) {
		if (engine == null) {
			throw new IllegalArgumentException("engine cannot be null");
		}

		if (StringUtils.isBlank(engine.getConfigurablePathKey())) {
			throw new IllegalStateException(
				"Can't set custom executable path for engine " + engine + "because it has no configurable path key"
			);
		}

		if (!isCustomProgramPathsSupported()) {
			throw new IllegalStateException("The program paths aren't configurable");
		}

		return ((ConfigurableProgramPaths) programPaths).setCustomProgramPathConfiguration(
			path,
			engine.getConfigurablePathKey()
		);
	}

	/**
	 * @return The {@link ExternalProgramInfo} for VLC.
	 */
	@Nullable
	public ExternalProgramInfo getVLCPaths() {
		return programPaths.getVLC();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for MEncoder.
	 */
	@Nullable
	public ExternalProgramInfo getMEncoderPaths() {
		return programPaths.getMEncoder();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for DCRaw.
	 */
	@Nullable
	public ExternalProgramInfo getDCRawPaths() {
		return programPaths.getDCRaw();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for FFmpeg.
	 */
	@Nullable
	public ExternalProgramInfo getFFmpegPaths() {
		return programPaths.getFFmpeg();
	}

	/**
	 * @return The configured path to the FFmpeg executable.
	 */
	@Nullable
	public String getFFmpegPath() {
		Path executable = null;
		ExternalProgramInfo ffmpegPaths = getFFmpegPaths();
		if (ffmpegPaths != null) {
			executable = ffmpegPaths.getDefaultPath();
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for MPlayer.
	 */
	@Nullable
	public ExternalProgramInfo getMPlayerPaths() {
		return programPaths.getMPlayer();
	}

	/**
	 * @return The configured path to the MPlayer executable. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getMPlayerPath() {
		Path executable = null;
		ExternalProgramInfo mPlayerPaths = getMPlayerPaths();
		if (mPlayerPaths != null) {
			ProgramExecutableType executableType = ProgramExecutableType.toProgramExecutableType(
				ConfigurableProgramPaths.KEY_MPLAYER_EXECUTABLE_TYPE,
				mPlayerPaths.getDefault()
			);
			if (executableType != null) {
				executable = mPlayerPaths.getPath(executableType);
			}

			if (executable == null) {
				executable = mPlayerPaths.getDefaultPath();
			}
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for MPlayer
	 * both in {@link UmsConfiguration} and the {@link ExternalProgramInfo}.
	 *
	 * @param customPath the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomMPlayerPath(@Nullable Path customPath) {
		if (!isCustomProgramPathsSupported()) {
			throw new IllegalStateException("The program paths aren't configurable");
		}

		((ConfigurableProgramPaths) programPaths).setCustomMPlayerPath(customPath);
	}

	/**
	 * @return The {@link ExternalProgramInfo} for tsMuxeR.
	 */
	@Nullable
	public ExternalProgramInfo getTsMuxeRPaths() {
		return programPaths.getTsMuxeR();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for FLAC.
	 */
	@Nullable
	public ExternalProgramInfo getFLACPaths() {
		return programPaths.getFLAC();
	}

	/**
	 * @return The configured path to the FLAC executable. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getFLACPath() {
		Path executable = null;
		ExternalProgramInfo flacPaths = getFLACPaths();
		if (flacPaths != null) {
			ProgramExecutableType executableType = ProgramExecutableType.toProgramExecutableType(
				ConfigurableProgramPaths.KEY_FLAC_EXECUTABLE_TYPE,
				flacPaths.getDefault()
			);
			if (executableType != null) {
				executable = flacPaths.getPath(executableType);
			}

			if (executable == null) {
				executable = flacPaths.getDefaultPath();
			}
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for FLAC
	 * both in {@link UmsConfiguration} and the {@link ExternalProgramInfo}.
	 *
	 * @param customPath the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomFlacPath(@Nullable Path customPath) {
		if (!isCustomProgramPathsSupported()) {
			throw new IllegalStateException("The program paths aren't configurable");
		}

		((ConfigurableProgramPaths) programPaths).setCustomFlacPath(customPath);
	}

	/**
	 * @return The {@link ExternalProgramInfo} for AviSynth.
	 */
	@Nullable
	public ExternalProgramInfo getAviSynthPaths() {
		return programPaths.getAviSynth();
	}

	/**
	 * @return The configured path to the AviSynth folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getAviSynthPath() {
		Path executable = null;
		ExternalProgramInfo aviSynthPaths = getAviSynthPaths();
		if (aviSynthPaths != null) {
			executable = aviSynthPaths.getDefaultPath();
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for Interframe.
	 */
	@Nullable
	public ExternalProgramInfo getInterFramePaths() {
		return programPaths.getInterFrame();
	}

	/**
	 * @return The configured path to the Interframe folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getInterFramePath() {
		Path executable = null;
		ExternalProgramInfo interFramePaths = getInterFramePaths();
		if (interFramePaths != null) {
			ProgramExecutableType executableType = ProgramExecutableType.toProgramExecutableType(
				ConfigurableProgramPaths.KEY_INTERFRAME_EXECUTABLE_TYPE,
				interFramePaths.getDefault()
			);
			if (executableType != null) {
				executable = interFramePaths.getPath(executableType);
			}

			if (executable == null) {
				executable = interFramePaths.getDefaultPath();
			}
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for AviSynth DirectShowSource library.
	 */
	@Nullable
	public ExternalProgramInfo getDirectShowSourcePaths() {
		return programPaths.getDirectShowSource();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for AviSynth FFMS2 library.
	 */
	@Nullable
	public ExternalProgramInfo getFFMS2Paths() {
		return programPaths.getFFMS2();
	}

	/**
	 * @return The configured path to the FFMS2 folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getFFMS2Path() {
		Path executable = null;
		ExternalProgramInfo ffms2Paths = getFFMS2Paths();
		if (ffms2Paths != null) {
			ProgramExecutableType executableType = ProgramExecutableType.toProgramExecutableType(
				ConfigurableProgramPaths.KEY_FFMS2_EXECUTABLE_TYPE,
				ffms2Paths.getDefault()
			);
			if (executableType != null) {
				executable = ffms2Paths.getPath(executableType);
			}

			if (executable == null) {
				executable = ffms2Paths.getDefaultPath();
			}
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for mvtools2 AviSynth plugin.
	 */
	@Nullable
	public ExternalProgramInfo getMvtools2Paths() {
		return programPaths.getMvtools2();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for Depan AviSynth plugin.
	 */
	@Nullable
	public ExternalProgramInfo getDepanPaths() {
		return programPaths.getDepan();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for masktools2 AviSynth plugin.
	 */
	@Nullable
	public ExternalProgramInfo getMasktools2Paths() {
		return programPaths.getMasktools2();
	}

	/**
	 * @return The configured path to the mvtools2 AviSynth folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getMvtools2Path() {
		Path executable = null;
		ExternalProgramInfo mvtools2Paths = getMvtools2Paths();
		if (mvtools2Paths != null) {
			executable = mvtools2Paths.getDefaultPath();
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The configured path to the Depan AviSynth folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getDepanPath() {
		Path executable = null;
		ExternalProgramInfo depanPaths = getDepanPaths();
		if (depanPaths != null) {
			executable = depanPaths.getDefaultPath();
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The configured path to the masktools2 AviSynth folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getMasktools2Path() {
		Path executable = null;
		ExternalProgramInfo masktools2Paths = getMasktools2Paths();
		if (masktools2Paths != null) {
			executable = masktools2Paths.getDefaultPath();
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for Convert2dTo3d.
	 */
	@Nullable
	public ExternalProgramInfo getConvert2dTo3dPaths() {
		return programPaths.getConvert2dTo3d();
	}

	/**
	 * @return The configured path to the Convert2dTo3d folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getConvert2dTo3dPath() {
		Path executable = null;
		ExternalProgramInfo convert2dTo3dPaths = getConvert2dTo3dPaths();
		if (convert2dTo3dPaths != null) {
			ProgramExecutableType executableType = ProgramExecutableType.toProgramExecutableType(
				ConfigurableProgramPaths.KEY_2DTO3D_EXECUTABLE_TYPE,
				convert2dTo3dPaths.getDefault()
			);
			if (executableType != null) {
				executable = convert2dTo3dPaths.getPath(executableType);
			}

			if (executable == null) {
				executable = convert2dTo3dPaths.getDefaultPath();
			}
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * @return The {@link ExternalProgramInfo} for CropResize.
	 */
	@Nullable
	public ExternalProgramInfo getCropResizePaths() {
		return programPaths.getCropResize();
	}

	/**
	 * @return The configured path to the CropResize folder. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getCropResizePath() {
		Path executable = null;
		ExternalProgramInfo cropResizePaths = getCropResizePaths();
		if (cropResizePaths != null) {
			ProgramExecutableType executableType = ProgramExecutableType.toProgramExecutableType(
				ConfigurableProgramPaths.KEY_CROP_RESIZE_EXECUTABLE_TYPE,
				cropResizePaths.getDefault()
			);
			if (executableType != null) {
				executable = cropResizePaths.getPath(executableType);
			}

			if (executable == null) {
				executable = cropResizePaths.getDefaultPath();
			}
		}
		return executable == null ? null : executable.toString();
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for
	 * Interframe both in {@link UmsConfiguration} and the
	 * {@link ExternalProgramInfo}.
	 *
	 * @param customPath the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomInterFramePath(@Nullable Path customPath) {
		if (!isCustomProgramPathsSupported()) {
			throw new IllegalStateException("The program paths aren't configurable");
		}

		((ConfigurableProgramPaths) programPaths).setCustomInterFramePath(customPath);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for
	 * FFMS2 both in {@link PmsConfiguration} and the
	 * {@link ExternalProgramInfo}.
	 *
	 * @param customPath the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomFFMS2Path(@Nullable Path customPath) {
		if (!isCustomProgramPathsSupported()) {
			throw new IllegalStateException("The program paths aren't configurable");
		}

		((ConfigurableProgramPaths) programPaths).setCustomFFMS2Path(customPath);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for
	 * Convert2dTo3d both in {@link PmsConfiguration} and the
	 * {@link ExternalProgramInfo}.
	 *
	 * @param customPath the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomConvert2dTo3dPath(@Nullable Path customPath) {
		if (!isCustomProgramPathsSupported()) {
			throw new IllegalStateException("The program paths aren't configurable");
		}

		((ConfigurableProgramPaths) programPaths).setCustomConvert2dTo3dPath(customPath);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for
	 * CropResize both in {@link PmsConfiguration} and the
	 * {@link ExternalProgramInfo}.
	 *
	 * @param customPath the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomCropResizePath(@Nullable Path customPath) {
		if (!isCustomProgramPathsSupported()) {
			throw new IllegalStateException("The program paths aren't configurable");
		}

		((ConfigurableProgramPaths) programPaths).setCustomCropResizePath(customPath);
	}

	/**
	 * @return The {@link ExternalProgramInfo} for youtube-dl.
	 */
	@Nullable
	public ExternalProgramInfo getYoutubeDlPaths() {
		return programPaths.getYoutubeDl();
	}

	/**
	 * @return The configured path to the FLAC executable. If none is
	 *         configured, the default is used.
	 */
	@Nullable
	public String getYoutubeDlPath() {
		Path executable = null;
		ExternalProgramInfo youtubeDlPaths = getYoutubeDlPaths();
		if (youtubeDlPaths != null) {
			executable = youtubeDlPaths.getDefaultPath();
		}
		return executable != null ? executable.toString() : null;
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
	 * The bitrate for AC-3 audio transcoding.
	 *
	 * @return The user-specified AC-3 audio bitrate or 448
	 */
	public int getAudioBitrate() {
		return getInt(KEY_AUDIO_BITRATE, 448);
	}

	public String getApiKey() {
		return getString(KEY_API_KEY, "");
	}

	public final String getJwtSecret() {
		//don't use RendererConfiguration.getString as it will log the value
		//jwt_secret can elevate users !!!
		return configuration.getString(KEY_JWT_SIGNER_SECRET, "");
	}

	public final void setJwtSecret(String value) {
		configuration.setProperty(KEY_JWT_SIGNER_SECRET, value);
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
	 * Get the MediaServer Engine version.
	 * @return the MediaServer engine version selected, or 0 for default.
	 */
	public int getServerEngine() {
		return getInt(KEY_SERVER_ENGINE, 0);
	}

	public void setServerEngine(int value) {
		configuration.setProperty(KEY_SERVER_ENGINE, value);
	}

	/**
	 * The server port where UMS listens for TCP/IP traffic. Default value is 5001.
	 * @return The port number.
	 */
	public int getMediaServerPort() {
		return getInt(KEY_SERVER_PORT, DEFAULT_MEDIA_SERVER_PORT);
	}

	/**
	 * Set the server port where UMS must listen for TCP/IP traffic.
	 * @param value The TCP/IP port number.
	 */
	public void setMediaServerPort(int value) {
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

	public String getServerDisplayName() {
		if (isAppendProfileName()) {
			return String.format("%s [%s]", getString(KEY_SERVER_NAME, PMS.NAME), getProfileName());
		}

		return getString(KEY_SERVER_NAME, PMS.NAME);
	}

	/**
	 * The name of the server.
	 *
	 * @return The name of the server.
	 */
	public String getServerName() {
		return getString(KEY_SERVER_NAME, PMS.NAME);
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
	 * Gets the language {@link String} as stored in the {@link UmsConfiguration}.
	 * May return <code>null</code>.
	 * @return The language {@link String}
	 */
	public String getLanguageRawString() {
		return configuration.getString(KEY_LANGUAGE);
	}

	/**
	 * Gets the {@link java.util.Locale} of the preferred language for the UMS
	 * user interface. The default is based on the default (OS) locale.
	 * @param log determines if any issues should be logged.
	 * @return The {@link java.util.Locale}.
	 */
	public final Locale getLanguageLocale(boolean log) {
		String languageCode = configuration.getString(KEY_LANGUAGE);
		Locale locale = null;
		if (languageCode != null && !languageCode.isEmpty()) {
			locale = Languages.toLocale(Locale.forLanguageTag(languageCode));
			if (log && locale == null) {
				LOGGER.error("Invalid or unsupported language tag \"{}\", defaulting to OS language.", languageCode);
			}

		} else if (log) {
			LOGGER.info("Language not specified, defaulting to OS language.");
		}

		if (locale == null) {
			locale = Languages.toLocale(Locale.getDefault());
			if (log && locale == null) {
				LOGGER.error("Unsupported language tag \"{}\", defaulting to US English.", Locale.getDefault().toLanguageTag());
			}
		}

		if (locale == null) {
			locale = Locale.forLanguageTag("en-US"); // Default
		}

		return locale;
	}

	/**
	 * Gets the {@link java.util.Locale} of the preferred language for the UMS
	 * user interface. The default is based on the default (OS) locale. Doesn't
	 * log potential issues.
	 * @return The {@link java.util.Locale}.
	 */
	public final Locale getLanguageLocale() {
		return getLanguageLocale(false);
	}

	/**
	 * Gets the {@link java.util.Locale} compatible tag of the preferred
	 * language for the UMS user interface. The default is based on the default (OS) locale.
	 * @return The <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a> language tag.
	 */
	public String getLanguageTag() {
		return getLanguageLocale().toLanguageTag();
	}

	/**
	 * Sets the preferred language for the UMS user interface.
	 * @param locale The {@link java.net.Locale}.
	 */
	public void setLanguage(Locale locale) {
		if (locale != null) {
			if (Languages.isValid(locale)) {
				configuration.setProperty(KEY_LANGUAGE, Languages.toLanguageTag(locale));
				PMS.setLocale(Languages.toLocale(locale));
				//TODO: The line below should be removed once all calls to Locale.getDefault() is replaced with PMS.getLocale()
				Locale.setDefault(Languages.toLocale(locale));
			} else {
				LOGGER.error("setLanguage() aborted because of unsupported language tag \"{}\"", locale.toLanguageTag());
			}
		} else {
			configuration.setProperty(KEY_LANGUAGE, "");
		}
	}

	/**
	 * Sets the preferred language for the UMS user interface.
	 * @param value The <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a> language tag.
	 */
	public void setLanguage(String value) {
		if (value != null && !value.isEmpty()) {
			setLanguage(Locale.forLanguageTag(value));
		} else {
			LOGGER.error("setLanguage() aborted because language tag is empty");
		}
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
		return Math.max(0, Math.min(maxMaxMemoryBufferSize, getInt(KEY_MAX_MEMORY_BUFFER_SIZE, 200)));
	}

	/**
	 * Set the preferred maximum for the transcoding memory buffer in megabytes. The top
	 * limit for the value is {@link #MAX_MAX_MEMORY_BUFFER_SIZE}.
	 *
	 * @param value The maximum buffer size.
	 */
	public void setMaxMemoryBufferSize(int value) {
		configuration.setProperty(KEY_MAX_MEMORY_BUFFER_SIZE, Math.max(0, Math.min(maxMaxMemoryBufferSize, value)));
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
	 * Returns the number of seconds from the start of a video file (the seek
	 * position) where the thumbnail image for the movie should be extracted
	 * from. Default is 4 seconds.
	 *
	 * @return The seek position in seconds.
	 */
	public int getThumbnailSeekPos() {
		return getInt(KEY_THUMBNAIL_SEEK_POS, 4);
	}

	/**
	 * Sets the number of seconds from the start of a video file (the seek
	 * position) where the thumbnail image for the movie should be extracted
	 * from.
	 *
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
		return getString(KEY_SUBS_FONT, "");
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
				Messages.getString("AudioLanguages")
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
				Messages.getString("SubtitlesLanguages")
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
				PMS.getLocale().getLanguage()
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
	 * and subtitle language should be comma-separated as a pair,
	 * individual pairs should be semicolon separated. "*" can be used to
	 * match any language. Subtitle language can be defined as "off".
	 * Default value is <code>"*,*"</code>.
	 *
	 * @return The audio and subtitle languages priority string.
	 */
	public String getAudioSubLanguages() {
		return configurationReader.getPossiblyBlankConfigurationString(
				KEY_AUDIO_SUB_LANGS,
				Messages.getString("AudioSubtitlesPairs")
		);
	}

	/**
	 * Sets a string of audio language and subtitle language pairs
	 * ordered by priority to try to match. Audio language
	 * and subtitle language should be comma-separated as a pair,
	 * individual pairs should be semicolon separated. "*" can be used to
	 * match any language. Subtitle language can be defined as "off".
	 *
	 * Example: <code>"en,off;jpn,eng;*,eng;*;*"</code>.
	 *
	 * @param value The audio and subtitle languages priority string.
	 */
	public void setAudioSubLanguages(String value) {
		configuration.setProperty(KEY_AUDIO_SUB_LANGS, value);
	}

	/**
	 * Returns whether or not MEncoder should use FriBiDi mode, which
	 * is needed to display subtitles in languages that read from right to
	 * left, like Arabic, Farsi, Hebrew, Urdu, etc. Default value is false.
	 *
	 * @return True if FriBiDi mode should be used, false otherwise.
	 */
	public boolean isMencoderSubFribidi() {
		return getBoolean(KEY_MENCODER_SUB_FRIBIDI, false);
	}

	/**
	 * Returns the character encoding (or code page) that should used
	 * for displaying non-Unicode external subtitles. Default is empty string
	 * (do not force encoding with -subcp key).
	 *
	 * @return The character encoding.
	 */
	public String getSubtitlesCodepage() {
		return getString(KEY_SUBTITLES_CODEPAGE, "");
	}

	/**
	 * Whether MEncoder should use fontconfig for displaying subtitles.
	 *
	 * @return True if fontconfig should be used, false otherwise.
	 */
	public boolean isMencoderFontConfig() {
		return getBoolean(KEY_MENCODER_FONT_CONFIG, true);
	}

	/**
	 * Set to true if MEncoder should be forced to use the framerate that is
	 * parsed by FFmpeg.
	 *
	 * @param value Set to true if the framerate should be forced, false
	 *              otherwise.
	 */
	public void setMencoderForceFps(boolean value) {
		configuration.setProperty(KEY_MENCODER_FORCE_FPS, value);
	}

	/**
	 * Whether MEncoder should be forced to use the framerate that is
	 * parsed by FFmpeg.
	 *
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
	 * Sets the subtitle language priority as a comma-separated string.
	 *
	 * Example: <code>"eng,fre,jpn,ger,und"</code>, where "und" stands for
	 * "undefined".
	 *
	 * @param value The subtitle language priority string.
	 */
	public void setSubtitlesLanguages(String value) {
		configuration.setProperty(KEY_SUBTITLES_LANGUAGES, value);
	}

	/**
	 * Sets the ISO 639 language code for the subtitle language that should
	 * be forced.
	 *
	 * @param value The subtitle language code.
	 */
	public void setForcedSubtitleLanguage(String value) {
		configuration.setProperty(KEY_FORCED_SUBTITLE_LANGUAGE, value);
	}

	/**
	 * Sets the tag string that identifies the subtitle language that
	 * should be forced.
	 *
	 * @param value The tag string.
	 */
	public void setForcedSubtitleTags(String value) {
		configuration.setProperty(KEY_FORCED_SUBTITLE_TAGS, value);
	}

	/**
	 * Returns custom commandline options to pass on to MEncoder.
	 *
	 * @return The custom options string.
	 */
	public String getMencoderCustomOptions() {
		return getString(KEY_MENCODER_CUSTOM_OPTIONS, "");
	}

	/**
	 * Sets custom commandline options to pass on to MEncoder.
	 *
	 * @param value The custom options string.
	 */
	public void setMencoderCustomOptions(String value) {
		configuration.setProperty(KEY_MENCODER_CUSTOM_OPTIONS, value);
	}

	/**
	 * Sets the character encoding (or code page) that should be used
	 * for displaying non-Unicode external subtitles. Default is empty (autodetect).
	 *
	 * @param value The character encoding.
	 */
	public void setSubtitlesCodepage(String value) {
		configuration.setProperty(KEY_SUBTITLES_CODEPAGE, value);
	}

	/**
	 * Sets whether or not MEncoder should use FriBiDi mode, which
	 * is needed to display subtitles in languages that read from right to
	 * left, like Arabic, Farsi, Hebrew, Urdu, etc. Default value is false.
	 *
	 * @param value Set to true if FriBiDi mode should be used.
	 */
	public void setMencoderSubFribidi(boolean value) {
		configuration.setProperty(KEY_MENCODER_SUB_FRIBIDI, value);
	}

	/**
	 * Sets the name of a TrueType font to use for subtitles.
	 *
	 * @param value The font name.
	 */
	public void setFont(String value) {
		configuration.setProperty(KEY_SUBS_FONT, value);
	}

	/**
	 * Older versions of MEncoder do not support ASS/SSA subtitles on all
	 * platforms. Set to true if MEncoder supports them. Default should be
	 * true on Windows and OS X, false otherwise.
	 * See https://code.google.com/p/ps3mediaserver/issues/detail?id=1097
	 *
	 * @param value Set to true if MEncoder supports ASS/SSA subtitles.
	 */
	public void setMencoderAss(boolean value) {
		configuration.setProperty(KEY_MENCODER_ASS, value);
	}

	/**
	 * Sets whether or not MEncoder should use fontconfig for displaying
	 * subtitles.
	 *
	 * @param value Set to true if fontconfig should be used.
	 */
	public void setMencoderFontConfig(boolean value) {
		configuration.setProperty(KEY_MENCODER_FONT_CONFIG, value);
	}

	/**
	 * Sets whether or not the Pulse Code Modulation audio format should be
	 * forced.
	 *
	 * @param value Set to true if PCM should be forced.
	 */
	public void setAudioUsePCM(boolean value) {
		configuration.setProperty(KEY_AUDIO_USE_PCM, value);
	}

	/**
	 * Sets whether or not the Pulse Code Modulation audio format should be
	 * used only for HQ audio codecs.
	 *
	 * @param value Set to true if PCM should be used only for HQ audio.
	 */
	public void setMencoderUsePcmForHQAudioOnly(boolean value) {
		configuration.setProperty(KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY, value);
	}

	/**
	 * Whether archives (e.g. .zip or .rar) should be browsable.
	 *
	 * @return True if archives should be browsable.
	 */
	public boolean isArchiveBrowsing() {
		return getBoolean(KEY_OPEN_ARCHIVES, false);
	}

	/**
	 * Sets whether archives (e.g. .zip or .rar) should be browsable.
	 *
	 * @param value Set to true if archives should be browsable.
	 */
	public void setArchiveBrowsing(boolean value) {
		configuration.setProperty(KEY_OPEN_ARCHIVES, value);
	}

	/**
	 * Returns true if MEncoder should use the deinterlace filter, false
	 * otherwise.
	 *
	 * @return True if the deinterlace filter should be used.
	 */
	public boolean isMencoderYadif() {
		return getBoolean(KEY_MENCODER_YADIF, false);
	}

	/**
	 * Set to true if MEncoder should use the deinterlace filter, false
	 * otherwise.
	 *
	 * @param value Set to true if the deinterlace filter should be used.
	 */
	public void setMencoderYadif(boolean value) {
		configuration.setProperty(KEY_MENCODER_YADIF, value);
	}

	/**
	 * Whether MEncoder should be used to upscale the video to an
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
	 * @return The selected renderers as a list.
	 */
	public List<String> getSelectedRenderers() {
		return getStringList(KEY_SELECTED_RENDERERS, RendererConfigurations.ALL_RENDERERS_KEY);
	}

	/**
	 * @param value The comma-separated list of selected renderers.
	 * @return {@code true} if this call changed the {@link Configuration},
	 *         {@code false} otherwise.
	 */
	public boolean setSelectedRenderers(String value) {
		if (value.isEmpty()) {
			value = EMPTY_LIST_VALUE;
		}

		if (!value.equals(configuration.getString(KEY_SELECTED_RENDERERS, null))) {
			configuration.setProperty(KEY_SELECTED_RENDERERS, value);
			return true;
		}

		return false;
	}

	/**
	 * @param value a string list of renderers.
	 * @return {@code true} if this call changed the {@link Configuration},
	 *         {@code false} otherwise.
	 */
	public boolean setSelectedRenderers(List<String> value) {
		if (value == null) {
			return setSelectedRenderers("");
		}

		List<String> currentValue = getStringList(KEY_SELECTED_RENDERERS, null);
		if (currentValue == null || value.size() != currentValue.size() || !value.containsAll(currentValue)) {
			setStringList(KEY_SELECTED_RENDERERS, value);
			return true;
		}

		return false;
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
	 *
	 * @param value True if thumbnails could be generated.
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
		return getInt(KEY_NUMBER_OF_CPU_CORES, getNumberOfSystemCpuCores());
	}

	/**
	 * Returns the number of CPU cores on the system.
	 *
	 * @return The number of CPU cores.
	 */
	public static int getNumberOfSystemCpuCores() {
		return Math.max(1, Runtime.getRuntime().availableProcessors());
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
	 * Whether we should start minimized, i.e. without its window opened.
	 *
	 * @return whether we should start minimized
	 */
	public boolean isMinimized() {
		return getBoolean(KEY_MINIMIZED, true);
	}

	/**
	 * Whether we should start minimized, i.e. without its window opened.
	 *
	 * @param value whether we should start minimized, false otherwise.
	 */
	public void setMinimized(boolean value) {
		configuration.setProperty(KEY_MINIMIZED, value);
	}

	/**
	 * Whether we should open a browser.
	 *
	 * @return True if UMS should open the web gui on start, false otherwise.
	 */
	public boolean isWebGuiOnStart() {
		return getBoolean(KEY_WEB_GUI_ON_START, true);
	}

	/**
	 * Whether we should open a browser.
	 *
	 * @param value whether we should open the web gui on start.
	 */
	public void setWebGuiOnStart(boolean value) {
		configuration.setProperty(KEY_WEB_GUI_ON_START, value);
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
				if (!PlatformUtils.INSTANCE.isAdmin()) {
					try {
						GuiManager.showErrorMessage(Messages.getString("UmsMustRunAdministrator"), Messages.getString("PermissionsError"));
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
	 * Whether to show the "Server Settings" folder on the renderer.
	 *
	 * @return whether the folder is shown
	 */
	public boolean isShowServerSettingsFolder() {
		return getBoolean(KEY_SHOW_SERVER_SETTINGS_FOLDER, false);
	}

	/**
	 * Whether to show the "Server Settings" folder on the renderer.
	 *
	 * @param value whether the folder is shown
	 */
	public void setShowServerSettingsFolder(boolean value) {
		configuration.setProperty(KEY_SHOW_SERVER_SETTINGS_FOLDER, value);
	}

	/**
	 * Gets the {@link FullyPlayedAction}.
	 *
	 * @return What to do with a file after it has been fully played
	 */
	public FullyPlayedAction getFullyPlayedAction() {
		return FullyPlayedAction.typeOf(getInt(KEY_FULLY_PLAYED_ACTION, FullyPlayedAction.MARK.getValue()), FullyPlayedAction.MARK);
	}

	/**
	 * Sets the {@link FullyPlayedAction}.
	 *
	 * @param action what to do with a file after it has been fully played
	 */
	public void setFullyPlayedAction(FullyPlayedAction action) {
		configuration.setProperty(KEY_FULLY_PLAYED_ACTION, action.getValue());
	}

	/**
	 * Returns the folder to move fully played files to.
	 *
	 * @see #getFullyPlayedAction()
	 * @return The folder to move fully played files to
	 */
	public String getFullyPlayedOutputDirectory() {
		return getString(KEY_FULLY_PLAYED_OUTPUT_DIRECTORY, "");
	}

	/**
	 * Sets the folder to move fully played files to.
	 *
	 * @see #getFullyPlayedAction()
	 * @param value the folder to move fully played files to
	 */
	public void setFullyPlayedOutputDirectory(String value) {
		configuration.setProperty(KEY_FULLY_PLAYED_OUTPUT_DIRECTORY, value);
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
		return getString(KEY_AVISYNTH_SCRIPT, defaultAviSynthScript);
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
		if (StringUtils.isBlank(mpegSettings) || mpegSettings.contains("Automatic")) {
			return mpegSettings;
		}

		return convertMencoderSettingToFFmpegFormat(mpegSettings);
	}

	public void setFFmpegLoggingLevel(String value) {
		configuration.setProperty(KEY_FFMPEG_LOGGING_LEVEL, value);
	}

	public String getFFmpegLoggingLevel() {
		return getString(KEY_FFMPEG_LOGGING_LEVEL, "fatal");
	}

	public void setFfmpegMultithreading(boolean value) {
		configuration.setProperty(KEY_FFMPEG_MULTITHREADING, value);
	}

	public boolean isFfmpegMultithreading() {
		return getBoolean(KEY_FFMPEG_MULTITHREADING, getNumberOfCpuCores() > 1);
	}

	public String getFFmpegGPUDecodingAccelerationMethod() {
		return getString(KEY_FFMPEG_GPU_DECODING_ACCELERATION_METHOD, Messages.getString("None_lowercase"));
	}

	public String getFFmpegGPUH264EncodingAccelerationMethod() {
		return getString(KEY_FFMPEG_GPU_ENCODING_H264_ACCELERATION_METHOD, "libx264");
	}

	public String getFFmpegGPUH265EncodingAccelerationMethod() {
		return getString(KEY_FFMPEG_GPU_ENCODING_H265_ACCELERATION_METHOD, "libx265");
	}

	public void setFFmpegGPUDecodingAccelerationMethod(String value) {
		configuration.setProperty(KEY_FFMPEG_GPU_DECODING_ACCELERATION_METHOD, value);
	}

	public String getFFmpegGPUDecodingAccelerationThreadNumber() {
		return getString(KEY_FFMPEG_GPU_DECODING_ACCELERATION_THREAD_NUMBER, "1");
	}

	public void setFFmpegGPUH264EncodingAccelerationMethod(String value) {
		configuration.setProperty(KEY_FFMPEG_GPU_ENCODING_H264_ACCELERATION_METHOD, value);
	}

	public void setFFmpegGPUH265EncodingAccelerationMethod(String value) {
		configuration.setProperty(KEY_FFMPEG_GPU_ENCODING_H265_ACCELERATION_METHOD, value);
	}

	public void setFFmpegGPUDecodingAccelerationThreadNumber(String value) {
		configuration.setProperty(KEY_FFMPEG_GPU_DECODING_ACCELERATION_THREAD_NUMBER, value);
	}

	public String[] getFFmpegAvailableGPUDecodingAccelerationMethods() {
		return getString(KEY_FFMPEG_AVAILABLE_GPU_ACCELERATION_METHODS, Messages.getString("None_lowercase")).split(",");
	}

	public static String[] getFFmpegAvailableGPUH264EncodingAccelerationMethods() {
		return new String[] {"libx264", "h264_nvenc", "h264_amf", "h264_qsv", "h264_mf", "libx264rgb"};
	}

	public  static  String[] getFFmpegAvailableGPUH265EncodingAccelerationMethods() {
		return new String[] {"libx265", "hevc_nvenc", "hevc_amf", "hevc_qsv", "hevc_mf"};
	}

	public void setFFmpegAvailableGPUDecodingAccelerationMethods(List<String> methods) {
		configuration.setProperty(KEY_FFMPEG_AVAILABLE_GPU_ACCELERATION_METHODS, collectionToString(methods));
	}

	public void setFfmpegAviSynthMultithreading(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_MULTITHREADING, value);
	}

	public boolean isFfmpegAviSynthMultithreading() {
		return getBoolean(KEY_FFMPEG_AVISYNTH_MULTITHREADING, getNumberOfCpuCores() > 1);
	}

	/**
	 * Whether we should pass the flag "convertfps=true" to AviSynth.
	 *
	 * @param value True if we should pass the flag.
	 */
	public void setFfmpegAvisynthConvertFps(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_CONVERT_FPS, value);
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

	/**
	 * Whether we should use FFMS2 instead of DirectShowSource in AviSynth.
	 *
	 * @param value True if we should use FFMS2 instead of DirectShowSource
	 *              in AviSynth.
	 */
	public void setFfmpegAvisynthUseFFMS2(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_USE_FFMS2, value);
	}

	/**
	 * Returns true if we should use FFMS2 instead of DirectShowSource in
	 * AviSynth.
	 *
	 * @return True if we should use FFMS2 instead of DirectShowSource in
	 *         AviSynth.
	 */
	public boolean getFfmpegAvisynthUseFFMS2() {
		return getBoolean(KEY_FFMPEG_AVISYNTH_USE_FFMS2, false);
	}

	/**
	 * Whether we should convert 2D video to 3D in AviSynth.
	 *
	 * @param value True if we should pass the flag.
	 */
	public void setFfmpegAvisynth2Dto3D(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_2D_TO_3D, value);
	}

	/**
	 * Returns true if we should convert 2D video to 3D in AviSynth.
	 *
	 * @return True if we should convert 2D video to 3D in AviSynth.
	 */
	public boolean isFfmpegAvisynth2Dto3D() {
		return getBoolean(KEY_FFMPEG_AVISYNTH_2D_TO_3D, false);
	}

	/**
	 * Returns the index of the 2D to 3D conversion algorithm that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @return The index of the format.
	 */
	public int getFfmpegAvisynthOutputFormat3D() {
		return getInt(KEY_FFMPEG_AVISYNTH_OUTPUT_FORMAT_3D, 4);
	}

	/**
	 * Sets the index of the 2D to 3D conversion algorithm that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @param value The index of the format.
	 */
	public void setFfmpegAvisynthOutputFormat3D(int value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_OUTPUT_FORMAT_3D, value);
	}

	/**
	 * Returns the index of the 2D to 3D conversion algorithm that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @return The index of the algorithm.
	 */
	public int getFfmpegAvisynthConversionAlgorithm2Dto3D() {
		return getInt(KEY_FFMPEG_AVISYNTH_CONVERSION_ALGORITHM_2D_TO_3D, 1);
	}

	/**
	 * Sets the index of the 2D to 3D conversion algorithm that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @param value The index of the algorithm.
	 */
	public void setFfmpegAvisynthConversionAlgorithm2Dto3D(int value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_CONVERSION_ALGORITHM_2D_TO_3D, value);
	}

	/**
	 * Returns the frame stretch factor that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @return The frame stretch factor.
	 */
	public int getFfmpegAvisynthFrameStretchFactor() {
		return getInt(KEY_FFMPEG_AVISYNTH_FRAME_STRETCH_FACTOR_2D_TO_3D, 8);
	}

	/**
	 * Sets the frame stretch factor that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @param value The index of the algorithm.
	 */
	public void setFfmpegAvisynthFrameStretchFactor(int value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_FRAME_STRETCH_FACTOR_2D_TO_3D, value);
	}

	/**
	 * Returns the light offset factor that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @return The light offset factor.
	 */
	public int getFfmpegAvisynthLightOffsetFactor() {
		return getInt(KEY_FFMPEG_AVISYNTH_LIGHT_OFFSET_FACTOR_2D_TO_3D, 3);
	}

	/**
	 * Sets the light offset factor that AviSynth should use for
	 * 2D to 3D conversion.
	 *
	 * @param value The index of the algorithm.
	 */
	public void setFfmpegAvisynthLightOffsetFactor(int value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_LIGHT_OFFSET_FACTOR_2D_TO_3D, value);
	}

	/**
	 * Whether we should resize the input 2D video for 3D conversion in AviSynth.
	 *
	 * @param value True if we should resize.
	 */
	public void setFfmpegAvisynthHorizontalResize(boolean value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE, value);
	}

	/**
	 * Returns true if we should resize the input 2D video for 3D conversion in AviSynth.
	 *
	 * @return True if we should resize.
	 */
	public boolean isFfmpegAvisynthHorizontalResize() {
		return getBoolean(KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE, true);
	}

	public int getFfmpegAvisynthHorizontalResizeResolution() {
		return getInt(KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE_RESOLUTION, 1920);
	}

	public void setFfmpegAvisynthHorizontalResizeResolution(int value) {
		configuration.setProperty(KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE_RESOLUTION, value);
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

	/**
	 * Lazy implementation, call before accessing {@link #enabledEngines}.
	 */
	private void buildEnabledEngines() {
		if (enabledEnginesBuilt) {
			return;
		}

		ENABLED_ENGINES_LOCK.writeLock().lock();
		try {
			// Not a bug, using double checked locking
			if (enabledEnginesBuilt) {
				return;
			}

			String engines = configuration.getString(KEY_ENGINES);
			enabledEngines = stringToEngineIdSet(engines);
			if (StringUtils.isBlank(engines)) {
				configuration.setProperty(KEY_ENGINES, collectionToString(enabledEngines));
			}

			enabledEnginesBuilt = true;
		} finally {
			ENABLED_ENGINES_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Gets a {@link UniqueList} of the {@link EngineId}s in no particular
	 * order. Returns a new instance, any modifications won't affect original
	 * list.
	 *
	 * @return A copy of the {@link List} of {@link EngineId}s.
	 */
	public List<EngineId> getEnabledEngines() {
		buildEnabledEngines();
		ENABLED_ENGINES_LOCK.readLock().lock();
		try {
			return new ArrayList<>(enabledEngines);
		} finally {
			ENABLED_ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Gets the enabled status of the specified {@link EngineId}.
	 *
	 * @param id the {@link EngineId} to check.
	 * @return {@code true} if the {@link Engine} with {@code id} is enabled,
	 *         {@code false} otherwise.
	 */
	public boolean isEngineEnabled(EngineId id) {
		if (id == null) {
			throw new NullPointerException("id cannot be null");
		}

		buildEnabledEngines();
		ENABLED_ENGINES_LOCK.readLock().lock();
		try {
			return enabledEngines.contains(id);
		} finally {
			ENABLED_ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Gets the enabled status of the specified {@link Engine}.
	 *
	 * @param engine the {@link Engine} to check.
	 * @return {@code true} if {@code player} is enabled, {@code false}
	 *         otherwise.
	 */
	public boolean isEngineEnabled(Engine engine) {
		if (engine == null) {
			throw new NullPointerException("engine cannot be null");
		}

		return isEngineEnabled(engine.getEngineId());
	}

	/**
	 * Sets the enabled status of the specified {@link EngineId}.
	 *
	 * @param id the {@link EngineId} whose enabled status to set.
	 * @param enabled the enabled status to set.
	 */
	public void setEngineEnabled(EngineId id, boolean enabled) {
		if (id == null) {
			throw new IllegalArgumentException("Unrecognized id");
		}

		ENABLED_ENGINES_LOCK.writeLock().lock();
		try {
			buildEnabledEngines();
			if (enabledEngines.contains(id)) {
				if (!enabled) {
					enabledEngines.remove(id);
				}
			} else {
				if (enabled) {
					enabledEngines.add(id);
				}
			}
			configuration.setProperty(KEY_ENGINES, collectionToString(enabledEngines));
		} finally {
			ENABLED_ENGINES_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Sets the enabled status of the specified {@link Engine}.
	 *
	 * @param engine the {@link Engine} whose enabled status to set.
	 * @param enabled the enabled status to set.
	 */
	public void setEngineEnabled(Engine engine, boolean enabled) {
		setEngineEnabled(engine.getEngineId(), enabled);
	}

	/**
	 * This is to make sure that any incorrect capitalization in the
	 * configuration file is corrected. This should only need to be called from
	 * {@link EngineFactory#registerEngine(Engine)}.
	 *
	 * @param engine the {@link Engine} for which to assure correct
	 *            capitalization.
	 */
	public void capitalizeEngineId(Engine engine) {
		if (engine == null) {
			throw new NullPointerException("engine cannot be null");
		}

		String engines = configuration.getString(KEY_ENGINES);
		if (StringUtils.isNotBlank(engines)) {
			String capitalizedEngines = StringUtil.caseReplace(engines.trim(), engine.getEngineId().toString());
			if (!engines.equals(capitalizedEngines)) {
				configuration.setProperty(KEY_ENGINES, capitalizedEngines);
			}
		}

		engines = configuration.getString(KEY_ENGINES_PRIORITY);
		if (StringUtils.isNotBlank(engines)) {
			String capitalizedEngines = StringUtil.caseReplace(engines.trim(), engine.getEngineId().toString());
			if (!engines.equals(capitalizedEngines)) {
				configuration.setProperty(KEY_ENGINES_PRIORITY, capitalizedEngines);
			}
		}
	}

	/**
	 * Lazy implementation, call before accessing {@link #enginesPriority}.
	 */
	private void buildEnginesPriority() {
		if (enginesPriorityBuilt) {
			return;
		}
		ENGINES_PRIORITY_LOCK.writeLock().lock();
		try {
			// Not a bug, using double checked locking
			if (enginesPriorityBuilt) {
				return;
			}

			String enginesPriorityString = configuration.getString(KEY_ENGINES_PRIORITY);
			enginesPriority = stringToEngineIdSet(enginesPriorityString);
			if (StringUtils.isBlank(enginesPriorityString)) {
				configuration.setProperty(KEY_ENGINES_PRIORITY, collectionToString(enginesPriority));
			}

			enginesPriorityBuilt = true;
		} finally {
			ENGINES_PRIORITY_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Returns the priority index according to the rules of {@link List#indexOf}.
	 *
	 * @param id the {@link EngineId} whose position to return.
	 * @return The priority index of {@code id}, or {@code -1} if the priority
	 *         list doesn't contain {@code id}.
	 */
	public int getEnginePriority(EngineId id) {
		if (id == null) {
			throw new NullPointerException("id cannot be null");
		}

		buildEnginesPriority();
		ENGINES_PRIORITY_LOCK.readLock().lock();
		try {
			int index = enginesPriority.indexOf(id);
			if (index >= 0) {
				return index;
			}
		} finally {
			ENGINES_PRIORITY_LOCK.readLock().unlock();
		}

		// The engine isn't listed, add it last
		ENGINES_PRIORITY_LOCK.writeLock().lock();
		try {
			enginesPriority.add(id);
			return enginesPriority.indexOf(id);
		} finally {
			ENGINES_PRIORITY_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Returns the priority index according to the rules of {@link List#indexOf}.
	 *
	 * @param engine the {@link Engine} whose position to return.
	 * @return the priority index of {@code engine}, or -1 if this the priority
	 *         list doesn't contain {@code engine}.
	 */
	public int getEnginePriority(Engine engine) {
		if (engine == null) {
			throw new NullPointerException("engine cannot be null");
		}

		return getEnginePriority(engine.getEngineId());
	}

	/**
	 * Moves or inserts a {@link Engine} directly above another {@link Engine}
	 * in the priority list. If {code aboveEngine} is {@code null},
	 * {@code engine} will be placed first in the list. If {@code aboveEngine}
	 * isn't found, {@code engine} will be placed last in the list.
	 *
	 * @param engine the {@link Engine} to move or insert in the priority list.
	 * @param aboveEngine the {@link Engine} to place {@code engine} relative
	 *            to.
	 */
	public void setEnginePriorityAbove(@Nonnull Engine engine, @Nullable Engine aboveEngine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine cannot be null");
		}

		setEnginePriorityAbove(engine.getEngineId(), aboveEngine == null ? null : aboveEngine.getEngineId());
	}

	/**
	 * Moves or inserts a {@link EngineId} directly above another
	 * {@link EngineId} in the priority list. If {code aboveId} is {@code null},
	 * {@code id} will be placed first in the list. If {@code aboveId} isn't
	 * found, {@code id} will be placed last in the list.
	 *
	 * @param id the {@link EngineId} to move or insert in the priority list.
	 * @param aboveId the {@link EngineId} to place {@code id} relative to.
	 */
	public void setEnginePriorityAbove(EngineId id, EngineId aboveId) {
		if (id == null) {
			throw new IllegalArgumentException("Unrecognized id");
		}

		ENGINES_PRIORITY_LOCK.writeLock().lock();
		try {
			buildEnginesPriority();

			if (enginesPriority.indexOf(id) > -1) {
				enginesPriority.remove(id);
			}

			int newPosition;
			if (aboveId == null) {
				newPosition = 0;
			} else {
				newPosition = enginesPriority.indexOf(aboveId);
				if (newPosition < 0) {
					newPosition = enginesPriority.size();
				}
			}
			enginesPriority.add(newPosition, id);
			configuration.setProperty(KEY_ENGINES_PRIORITY, collectionToString(enginesPriority));
		} finally {
			ENGINES_PRIORITY_LOCK.writeLock().unlock();
		}

		EngineFactory.sortEngines();
	}

	/**
	 * Moves or inserts a {@link Engine} directly below another {@link Engine}
	 * in the priority list. If {code belowEngine} is {@code null} or isn't
	 * found, {@code engine} will be placed last in the list.
	 *
	 * @param engine the {@link Engine} to move or insert in the priority list.
	 * @param belowEngine the {@link Engine} to place {@code engine} relative
	 *            to.
	 */
	public void setEnginePriorityBelow(Engine engine, Engine belowEngine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine cannot be null");
		}

		setEnginePriorityBelow(engine.getEngineId(), belowEngine == null ? null : belowEngine.getEngineId());
	}

	/**
	 * Moves or inserts a {@link EngineId} directly below another
	 * {@link EngineId} in the priority list. If {code belowId} is {@code null}
	 * or isn't found, {@code id} will be placed last in the list.
	 *
	 * @param id the {@link EngineId} to move or insert in the priority list.
	 * @param belowId the {@link EngineId} to place {@code id} relative to.
	 */
	public void setEnginePriorityBelow(EngineId id, EngineId belowId) {
		if (id == null) {
			throw new IllegalArgumentException("Unrecognized id");
		}

		ENGINES_PRIORITY_LOCK.writeLock().lock();
		try {
			buildEnginesPriority();

			if (enginesPriority.indexOf(id) > -1) {
				enginesPriority.remove(id);
			}

			int newPosition;
			if (belowId == null) {
				newPosition = enginesPriority.size();
			} else {
				newPosition = enginesPriority.indexOf(belowId) + 1;
				if (newPosition < 0) {
					newPosition = enginesPriority.size();
				}
			}

			enginesPriority.add(newPosition, id);
			configuration.setProperty(KEY_ENGINES_PRIORITY, collectionToString(enginesPriority));
		} finally {
			ENGINES_PRIORITY_LOCK.writeLock().unlock();
		}
		EngineFactory.sortEngines();
	}

	private static String collectionToString(Collection<?> list) {
		return StringUtils.join(list, LIST_SEPARATOR);
	}

	@SuppressWarnings("unused")
	private static List<String> stringToStringList(String input) {
		List<String> output = new ArrayList<>();
		Collections.addAll(output, StringUtils.split(input, LIST_SEPARATOR));
		return output;
	}

	private static UniqueList<EngineId> stringToEngineIdSet(String input) {
		UniqueList<EngineId> output = new UniqueList<>();
		if (StringUtils.isBlank(input)) {
			output.addAll(StandardEngineId.ALL);
			return output;
		}

		input = input.trim().toLowerCase(Locale.ROOT);
		if ("none".equals(input)) {
			return output;
		}

		for (String s : StringUtils.split(input, LIST_SEPARATOR)) {
			EngineId engineId = StandardEngineId.toEngineID(s);
			if (engineId != null) {
				output.add(engineId);
			} else {
				LOGGER.warn("Unknown transcoding engine \"{}\"", s);
			}
		}

		return output;
	}

	/**
     * Save the configuration. Before this method can be called a valid file
     * name must have been set.
     *
     * @throws ConfigurationException if an error occurs or no file name has
     * been set yet
     */
	public void save() throws ConfigurationException {
		((PropertiesConfiguration) configuration).save();
		LOGGER.info("Configuration saved to \"{}\"", PROFILE_PATH);
	}

	private ArrayList<String> ignoredFolderNames;

	/**
	 * Whether folder_names_ignored has been read.
	 */
	private boolean ignoredFolderNamesRead;


	/**
	 * @return The {@link List} of {@link Path}s of ignored folder names.
	 */
	@Nonnull
	public ArrayList<String> getIgnoredFolderNames() {
		if (!ignoredFolderNamesRead) {
			String ignoredFolderNamesString = configuration.getString(KEY_FOLDER_NAMES_IGNORED, ".unwanted");

			ArrayList<String> folders = new ArrayList<>();
			if (ignoredFolderNamesString == null || ignoredFolderNamesString.length() == 0) {
				return folders;
			}

			String[] foldersArray = ignoredFolderNamesString.trim().split("\\s*,\\s*");
			ignoredFolderNames = new ArrayList<>();

			for (String folder : foldersArray) {
				/*
				 * Unescape embedded commas. Note: Backslashing isn't safe as it
				 * conflicts with the Windows path separator.
				 */
				folder = folder.replace("&comma;", ",");

				// add the path even if there are problems so that the user can update the shared folders as required.
				ignoredFolderNames.add(folder);
			}

			ignoredFolderNamesRead = true;
		}

		return ignoredFolderNames;
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

	/**
	 * @return {@code true} if subtitles information should be added to video
	 *         names, {@code false} otherwise.
	 */
	public SubtitlesInfoLevel getSubtitlesInfoLevel() {
		SubtitlesInfoLevel subtitlesInfoLevel = SubtitlesInfoLevel.typeOf(getString(KEY_SUBS_INFO_LEVEL, null));
		if (subtitlesInfoLevel != null) {
			return subtitlesInfoLevel;
		}
		return SubtitlesInfoLevel.BASIC; // Default
	}
	public static synchronized JsonArray getFfmpegLoglevels() {
		String[] values = FFmpegLogLevels.getLabels();
		String[] labels = FFmpegLogLevels.getLabels();
		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
	}
	/**
	 * Sets if subtitles information should be added to video names.
	 *
	 * @param value whether or not subtitles information should be added.
	 */
	public void setSubtitlesInfoLevel(SubtitlesInfoLevel value) {
		configuration.setProperty(KEY_SUBS_INFO_LEVEL, value == null ? "" : value.toString());
	}

	/**
	 * @return available subtitles info levels as a JSON array
	 */
	public static synchronized JsonArray getSubtitlesInfoLevelsAsJsonArray() {
		String[] values = new String[] {
			SubtitlesInfoLevel.NONE.toString(),
			SubtitlesInfoLevel.BASIC.toString(),
			SubtitlesInfoLevel.FULL.toString()
		};
		String[] labels = new String[] {
			"i18n@None",
			"i18n@Basic",
			"i18n@Full"
		};
		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
	}

	/**
	 * @return available fully played actions as a JSON array
	 */
	public static synchronized JsonArray getFullyPlayedActionsAsJsonArray() {
		String[] values = new String[]{
			String.valueOf(FullyPlayedAction.NO_ACTION.getValue()),
			String.valueOf(FullyPlayedAction.MARK.getValue()),
			String.valueOf(FullyPlayedAction.HIDE_MEDIA.getValue()),
			String.valueOf(FullyPlayedAction.MOVE_FOLDER.getValue()),
			String.valueOf(FullyPlayedAction.MOVE_FOLDER_AND_MARK.getValue()),
			String.valueOf(FullyPlayedAction.MOVE_TRASH.getValue())
		};
		String[] labels = new String[]{
			"i18n@DoNothing",
			"i18n@MarkMedia",
			"i18n@HideMedia",
			"i18n@MoveFileToDifferentFolder",
			"i18n@MoveFileDifferentFolderMark",
			"i18n@MoveFileRecycleTrashBin"
		};
		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
	}

	public static synchronized JsonArray getSubtitlesCodepageArray() {
		String[] values = new String[]{
			"", "cp874", "cp932", "cp936", "cp949", "cp950", "cp1250",
			"cp1251", "cp1252", "cp1253", "cp1254", "cp1255", "cp1256",
			"cp1257", "cp1258", "ISO-2022-CN", "ISO-2022-JP", "ISO-2022-KR",
			"ISO-8859-1", "ISO-8859-2", "ISO-8859-3", "ISO-8859-4",
			"ISO-8859-5", "ISO-8859-6", "ISO-8859-7", "ISO-8859-8",
			"ISO-8859-9", "ISO-8859-10", "ISO-8859-11", "ISO-8859-13",
			"ISO-8859-14", "ISO-8859-15", "ISO-8859-16", "Big5", "EUC-JP",
			"EUC-KR", "GB18030", "IBM420", "IBM424", "KOI8-R", "Shift_JIS", "TIS-620"
		};
		String[] labels = new String[]{
			"i18n@AutoDetect",
			"i18n@CharacterSet.874",
			"i18n@CharacterSet.932",
			"i18n@CharacterSet.936",
			"i18n@CharacterSet.949",
			"i18n@CharacterSet.950",
			"i18n@CharacterSet.1250",
			"i18n@CharacterSet.1251",
			"i18n@CharacterSet.1252",
			"i18n@CharacterSet.1253",
			"i18n@CharacterSet.1254",
			"i18n@CharacterSet.1255",
			"i18n@CharacterSet.1256",
			"i18n@CharacterSet.1257",
			"i18n@CharacterSet.1258",
			"i18n@CharacterSet.2022-CN",
			"i18n@CharacterSet.2022-JP",
			"i18n@CharacterSet.2022-KR",
			"i18n@CharacterSet.8859-1",
			"i18n@CharacterSet.8859-2",
			"i18n@CharacterSet.8859-3",
			"i18n@CharacterSet.8859-4",
			"i18n@CharacterSet.8859-5",
			"i18n@CharacterSet.8859-6",
			"i18n@CharacterSet.8859-7",
			"i18n@CharacterSet.8859-8",
			"i18n@CharacterSet.8859-9",
			"i18n@CharacterSet.8859-10",
			"i18n@CharacterSet.8859-11",
			"i18n@CharacterSet.8859-13",
			"i18n@CharacterSet.8859-14",
			"i18n@CharacterSet.8859-15",
			"i18n@CharacterSet.8859-16",
			"i18n@CharacterSet.Big5",
			"i18n@CharacterSet.EUC-JP",
			"i18n@CharacterSet.EUC-KR",
			"i18n@CharacterSet.GB18030",
			"i18n@CharacterSet.IBM420",
			"i18n@CharacterSet.IBM424",
			"i18n@CharacterSet.KOI8-R",
			"i18n@CharacterSet.ShiftJIS",
			"i18n@CharacterSet.TIS-620"
		};
		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
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

	public boolean isDisableTranscoding() {
		return getBoolean(KEY_DISABLE_TRANSCODING, false);
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
		return getBoolean(KEY_MENCODER_MT, getNumberOfCpuCores() > 1);
	}

	public void setAudioRemuxAC3(boolean value) {
		configuration.setProperty(KEY_AUDIO_REMUX_AC3, value);
	}

	public boolean isAudioRemuxAC3() {
		return getBoolean(KEY_AUDIO_REMUX_AC3, true);
	}

	public void setFFmpegSoX(boolean value) {
		configuration.setProperty(KEY_FFMPEG_SOX, value);
	}

	public boolean isFFmpegSoX() {
		return getBoolean(KEY_FFMPEG_SOX, false);
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
		return getBoolean(KEY_USE_EMBEDDED_SUBTITLES_STYLE, true);
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
	private static int findPathSort(String[] paths, String path) throws NumberFormatException {
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

	public CoverSupplier getAudioThumbnailMethod() {
		return CoverSupplier.toCoverSupplier(getInt(KEY_AUDIO_THUMBNAILS_METHOD, 1));
	}

	public void setAudioThumbnailMethod(CoverSupplier value) {
		configuration.setProperty(KEY_AUDIO_THUMBNAILS_METHOD, value.toInt());
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
		return getBoolean(KEY_MENCODER_MUX_COMPATIBLE, false);
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
		return getBoolean(KEY_FFMPEG_MUX_TSMUXER_COMPATIBLE, false);
	}

	/**
	 * Whether FFmpegVideo should defer to MEncoderVideo when there are
	 * subtitles that need to be transcoded which FFmpeg will need to
	 * initially parse, which can cause timeouts.
	 *
	 * @param value
	 */
	public void setFFmpegDeferToMEncoderForProblematicSubtitles(boolean value) {
		configuration.setProperty(KEY_FFMPEG_MENCODER_PROBLEMATIC_SUBTITLES, value);
	}

	/**
	 * Whether FFmpegVideo should defer to MEncoderVideo when there are
	 * subtitles that need to be transcoded which FFmpeg will need to
	 * initially parse, which can cause timeouts.
	 *
	 * @return
	 */
	public boolean isFFmpegDeferToMEncoderForProblematicSubtitles() {
		return getBoolean(KEY_FFMPEG_MENCODER_PROBLEMATIC_SUBTITLES, true);
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

	public void setPreventSleep(PreventSleepMode value) {
		if (value == null) {
			throw new NullPointerException("value cannot be null");
		}

		configuration.setProperty(KEY_PREVENT_SLEEP, value.getValue());
		SleepManager sleepManager = Services.sleepManager();
		if (sleepManager != null) {
			sleepManager.setMode(value);
		}
	}

	public PreventSleepMode getPreventSleep() {
		return PreventSleepMode.typeOf(getString(KEY_PREVENT_SLEEP, PreventSleepMode.PLAYBACK.getValue()));
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
		return getBoolean(UmsConfiguration.KEY_HIDE_ADVANCED_OPTIONS, true);
	}

	public void setHideAdvancedOptions(final boolean value) {
		this.configuration.setProperty(UmsConfiguration.KEY_HIDE_ADVANCED_OPTIONS, value);
	}

	public boolean isHideEmptyFolders() {
		return getBoolean(UmsConfiguration.KEY_HIDE_EMPTY_FOLDERS, false);
	}

	public void setHideEmptyFolders(final boolean value) {
		this.configuration.setProperty(UmsConfiguration.KEY_HIDE_EMPTY_FOLDERS, value);
	}

	public boolean isUseSymlinksTargetFile() {
		return getBoolean(UmsConfiguration.KEY_USE_SYMLINKS_TARGET_FILE, false);
	}

	public void setUseSymlinksTargetFile(final boolean value) {
		this.configuration.setProperty(UmsConfiguration.KEY_USE_SYMLINKS_TARGET_FILE, value);
	}

	/**
	 * Whether to show the "Media Library" folder on the renderer.
	 *
	 * @return whether the folder is shown
	 */
	public boolean isShowMediaLibraryFolder() {
		return getBoolean(UmsConfiguration.KEY_SHOW_MEDIA_LIBRARY_FOLDER, true);
	}

	/**
	 * Whether to show the "Media Library" folder on the renderer.
	 *
	 * @param value whether the folder is shown
	 */
	public void setShowMediaLibraryFolder(final boolean value) {
		this.configuration.setProperty(UmsConfiguration.KEY_SHOW_MEDIA_LIBRARY_FOLDER, value);
	}

	/**
	 * Whether to show the "#--TRANSCODE--#" folder on the renderer.
	 *
	 * @return whether the folder is shown
	 */
	public boolean isShowTranscodeFolder() {
		return getBoolean(KEY_SHOW_TRANSCODE_FOLDER, true);
	}

	/**
	 * Whether to show the "#--TRANSCODE--#" folder on the renderer.
	 *
	 * @param value whether the folder is shown
	 */
	public void setShowTranscodeFolder(boolean value) {
		configuration.setProperty(KEY_SHOW_TRANSCODE_FOLDER, value);
	}

	public boolean isDvdIsoThumbnails() {
		return getBoolean(KEY_DVDISO_THUMBNAILS, true);
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

	public SubtitleColor getSubsColor() {
		String colorString = getString(KEY_SUBS_COLOR, null);
		if (StringUtils.isNotBlank(colorString)) {
			try {
				return new SubtitleColor(colorString);
			} catch (InvalidArgumentException e) {
				LOGGER.error("Using default subtitle color: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}

		return new SubtitleColor(0xFF, 0xFF, 0xFF);
	}

	public void setSubsColor(SubtitleColor color) {
		if (color.getAlpha() != 0xFF) {
			configuration.setProperty(KEY_SUBS_COLOR, color.get0xRRGGBBAA());
		} else {
			configuration.setProperty(KEY_SUBS_COLOR, color.get0xRRGGBB());
		}
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

	public boolean isIgnoreTheWordAandThe() {
		return getBoolean(KEY_IGNORE_THE_WORD_A_AND_THE, true);
	}

	public void setIgnoreTheWordAandThe(boolean value) {
		configuration.setProperty(KEY_IGNORE_THE_WORD_A_AND_THE, value);
	}

	public boolean isPrettifyFilenames() {
		return getBoolean(KEY_PRETTIFY_FILENAMES, false);
	}

	public void setPrettifyFilenames(boolean value) {
		configuration.setProperty(KEY_PRETTIFY_FILENAMES, value);
	}

	public boolean isUseInfoFromIMDb() {
		return getBoolean(KEY_USE_IMDB_INFO, true);
	}

	public void setUseInfoFromIMDb(boolean value) {
		configuration.setProperty(KEY_USE_IMDB_INFO, value);
	}

	public boolean isRunWizard() {
		return getBoolean(KEY_RUN_WIZARD, true);
	}

	public void setRunWizard(boolean value) {
		configuration.setProperty(KEY_RUN_WIZARD, value);
	}

	/**
	 * Whether to scan shared folders on startup.
	 *
	 * @return whether to scan shared folders on startup
	 */
	public boolean isScanSharedFoldersOnStartup() {
		return getBoolean(KEY_SCAN_SHARED_FOLDERS_ON_STARTUP, true);
	}

	/**
	 * Whether to scan shared folders on startup.
	 *
	 * @param value whether to scan shared folders on startup
	 */
	public void setScanSharedFoldersOnStartup(final boolean value) {
		this.configuration.setProperty(KEY_SCAN_SHARED_FOLDERS_ON_STARTUP, value);
	}

	/**
	 * Whether to show the "Recently Played" folder on the renderer.
	 *
	 * @return whether the folder is shown
	 */
	public boolean isShowRecentlyPlayedFolder() {
		return getBoolean(UmsConfiguration.KEY_SHOW_RECENTLY_PLAYED_FOLDER, true);
	}

	/**
	 * Whether to show the "Recently Played" folder on the renderer.
	 *
	 * @param value whether the folder is shown
	 */
	public void setShowRecentlyPlayedFolder(final boolean value) {
		this.configuration.setProperty(UmsConfiguration.KEY_SHOW_RECENTLY_PLAYED_FOLDER, value);
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

	public String getProfilePath() {
		return PROFILE_PATH;
	}

	public String getProfileDirectory() {
		return PROFILE_DIRECTORY;
	}

	/**
	 * Returns the absolute path to the SHARED.conf file.
	 * By default this is <pre>PROFILE_DIRECTORY + File.pathSeparator + SHARED.conf</pre>,
	 * but it can be overridden via the <pre>shared_conf</pre> profile option.
	 * The existence of the file is not checked.
	 *
	 * @return the path to the SHARED.conf file.
	 */
	public String getSharedConfPath() {
		// Initialise this here rather than in the constructor or statically
		// so that custom settings are logged to the logfile/Logs tab.
		if (sharedConfPath == null) {
			sharedConfPath = FileUtil.getFileLocation(
				getString(KEY_SHARED_CONF_PATH, null),
				PROFILE_DIRECTORY,
				DEFAULT_SHARED_CONF_FILENAME
			).getFilePath();
		}

		return getString(KEY_SHARED_CONF_PATH, sharedConfPath);
	}

	public String getProfileName() {
		if (hostName == null) { // Initialise this lazily
			try {
				hostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				LOGGER.info("Can't determine hostname");
				hostName = "unknown host";
			}
		}

		return getString(KEY_PROFILE_NAME, hostName);
	}

	public boolean isAuthenticationEnabled() {
		return getBoolean(KEY_AUTHENTICATION_ENABLED, true);
	}

	public void setAuthenticationEnabled(boolean value) {
		configuration.setProperty(KEY_AUTHENTICATION_ENABLED, value);
	}

	public boolean isAuthenticateLocalhostAsAdmin() {
		return getBoolean(KEY_AUTHENTICATE_LOCALHOST_AS_ADMIN, false);
	}

	public void setAuthenticateLocalhostAsAdmin(boolean value) {
		configuration.setProperty(KEY_AUTHENTICATE_LOCALHOST_AS_ADMIN, value);
	}

	public boolean isAutoUpdate() {
		return Build.isUpdatable() && getBoolean(KEY_AUTO_UPDATE, true);
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

	public void setUuid(String value) {
		configuration.setProperty(KEY_UUID, value);
	}

	public void addConfigurationListener(ConfigurationListener l) {
		((PropertiesConfiguration) configuration).addConfigurationListener(l);
	}

	public void removeConfigurationListener(ConfigurationListener l) {
		((PropertiesConfiguration) configuration).removeConfigurationListener(l);
	}

	public boolean getFolderLimit() {
		return getBoolean(KEY_FOLDER_LIMIT, false);
	}

	public String getScriptDir() {
		return getString(KEY_SCRIPT_DIR, null);
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
			((PropertiesConfiguration) configuration).refresh();
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
		return getString(KEY_TRANSCODE_FOLDER_NAME, Messages.getString("Transcode_FolderName"));
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
	 * Get the state of the GUI log tab "Case sensitive" check box
	 * @return true if enabled, false if disabled
	 */
	public boolean getGUILogSearchCaseSensitive() {
		return getBoolean(KEY_GUI_LOG_SEARCH_CASE_SENSITIVE, false);
	}

	/**
	 * Set the state of the GUI log tab "Case sensitive" check box
	 * @param value true if enabled, false if disabled
	 */
	public void setGUILogSearchCaseSensitive(boolean value) {
		configuration.setProperty(KEY_GUI_LOG_SEARCH_CASE_SENSITIVE, value);
	}

	/**
	 * Get the state of the GUI log tab "Multiline" check box
	 * @return true if enabled, false if disabled
	 */
	public boolean getGUILogSearchMultiLine() {
		return getBoolean(KEY_GUI_LOG_SEARCH_MULTILINE, false);
	}

	/**
	 * Set the state of the GUI log tab "Multiline" check box
	 * @param value true if enabled, false if disabled
	 */
	public void setGUILogSearchMultiLine(boolean value) {
		configuration.setProperty(KEY_GUI_LOG_SEARCH_MULTILINE, value);
	}

	/**
	 * Get the state of the GUI log tab "RegEx" check box
	 * @return true if enabled, false if disabled
	 */
	public boolean getGUILogSearchRegEx() {
		return getBoolean(KEY_GUI_LOG_SEARCH_USE_REGEX, false);
	}

	/**
	 * Set the state of the GUI log tab "RegEx" check box
	 * @param value true if enabled, false if disabled
	 */
	public void setGUILogSearchRegEx(boolean value) {
		configuration.setProperty(KEY_GUI_LOG_SEARCH_USE_REGEX, value);
	}

	public boolean getExternalNetwork() {
		return getBoolean(KEY_EXTERNAL_NETWORK, true);
	}

	public void setExternalNetwork(boolean b) {
		configuration.setProperty(KEY_EXTERNAL_NETWORK, b);
	}

	/* Credential path handling */
	public static final String KEY_CRED_PATH = "cred.path";

	public void initCred() throws IOException {
		File credFile = getCredFile();

		if (!credFile.exists()) {
			// Create an empty file and save the path if needed
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(credFile), StandardCharsets.UTF_8))) {
				writer.write("# Add credentials to the file");
				writer.newLine();
				writer.write("# on the format tag=user,password");
				writer.newLine();
				writer.write("# For example:");
				writer.newLine();
				writer.write("# channels.xxx=name,secret");
				writer.newLine();
			}

			// Save the path if we got here
			configuration.setProperty(KEY_CRED_PATH, credFile.getAbsolutePath());
			try {
				((PropertiesConfiguration) configuration).save();
			} catch (ConfigurationException e) {
				LOGGER.warn("An error occurred while saving configuration: {}", e.getMessage());
			}
		}
	}

	public File getCredFile() {
		String path = getString(KEY_CRED_PATH, "");
		if (path != null && !path.trim().isEmpty()) {
			return new File(path);
		}

		return new File(getProfileDirectory(), DEFAULT_CREDENTIALS_FILENAME);
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
		} catch (NumberFormatException e) {
			setATZLimit(0);
		}
	}

	public String getDataDir() {
		return getProfileDirectory() + File.separator + "data";
	}

	public String getDataFile(String str) {
		return getDataDir() + File.separator + str;
	}

	private static final String KEY_URL_RES_ORDER = "url_resolve_order";

	public String[] getURLResolveOrder() {
		return getString(KEY_URL_RES_ORDER, "").split(",");
	}

	/**
	 * Whether to show the "#--LIVE SUBTITLES--#" folder on the renderer.
	 *
	 * @return whether the folder is shown
	 */
	public boolean isShowLiveSubtitlesFolder() {
		return getBoolean(KEY_SHOW_LIVE_SUBTITLES_FOLDER, false);
	}

	/**
	 * Whether to show the "#--LIVE SUBTITLES--#" folder on the renderer.
	 *
	 * @param value whether the folder is shown
	 */
	public void setShowLiveSubtitlesFolder(boolean value) {
		configuration.setProperty(KEY_SHOW_LIVE_SUBTITLES_FOLDER, value);
	}

	public boolean displayAudioLikesInRootFolder() {
		return getBoolean(KEY_AUDIO_LIKES_IN_ROOT_FOLDER, false);
	}

	public int getLiveSubtitlesLimit() {
		return getInt(KEY_LIVE_SUBTITLES_LIMIT, 20);
	}

	public void setLiveSubtitlesLimit(int value) {
		if (value > 0) {
			configuration.setProperty(KEY_LIVE_SUBTITLES_LIMIT, value);
		}
	}

	public boolean isLiveSubtitlesKeep() {
		return getBoolean(KEY_LIVE_SUBTITLES_KEEP, true);
	}

	public void setLiveSubtitlesKeep(boolean value) {
		configuration.setProperty(KEY_LIVE_SUBTITLES_KEEP, value);
	}

	public boolean getLoggingBuffered() {
		return getBoolean(KEY_LOGGING_BUFFERED, false);
	}

	public void setLoggingBuffered(boolean value) {
		configuration.setProperty(KEY_LOGGING_BUFFERED, value);
	}

	public Level getLoggingFilterConsole() {
		return Level.toLevel(getString(KEY_LOGGING_FILTER_CONSOLE, "INFO"), Level.INFO);
	}

	public void setLoggingFilterConsole(Level value) {
		configuration.setProperty(KEY_LOGGING_FILTER_CONSOLE, value.levelStr);
	}

	public Level getLoggingFilterLogsTab() {
		return Level.toLevel(getString(KEY_LOGGING_FILTER_LOGS_TAB, "INFO"), Level.INFO);
	}

	public void setLoggingFilterLogsTab(Level value) {
		configuration.setProperty(KEY_LOGGING_FILTER_LOGS_TAB, value.levelStr);
	}

	public static int getLoggingLogsTabLinebufferMin() {
		return LOGGING_LOGS_TAB_LINEBUFFER_MIN;
	}

	public static int getLoggingLogsTabLinebufferMax() {
		return LOGGING_LOGS_TAB_LINEBUFFER_MAX;
	}

	public static int getLoggingLogsTabLinebufferStep() {
		return LOGGING_LOGS_TAB_LINEBUFFER_STEP;
	}

	public int getLoggingLogsTabLinebuffer() {
		return Math.min(Math.max(getInt(KEY_LOGGING_LOGS_TAB_LINEBUFFER, 1000), LOGGING_LOGS_TAB_LINEBUFFER_MIN), LOGGING_LOGS_TAB_LINEBUFFER_MAX);
	}

	public void setLoggingLogsTabLinebuffer(int value) {
		value = Math.min(Math.max(value, LOGGING_LOGS_TAB_LINEBUFFER_MIN), LOGGING_LOGS_TAB_LINEBUFFER_MAX);
		configuration.setProperty(KEY_LOGGING_LOGS_TAB_LINEBUFFER, value);
	}

	public String getLoggingSyslogFacility() {
		return getString(KEY_LOGGING_SYSLOG_FACILITY, "USER");
	}

	public void setLoggingSyslogFacility(String value) {
		configuration.setProperty(KEY_LOGGING_SYSLOG_FACILITY, value);
	}

	public void setLoggingSyslogFacilityDefault() {
		setLoggingSyslogFacility("USER");
	}

	public String getLoggingSyslogHost() {
		return getString(KEY_LOGGING_SYSLOG_HOST, "");
	}

	public void setLoggingSyslogHost(String value) {
		configuration.setProperty(KEY_LOGGING_SYSLOG_HOST, value);
	}

	public int getLoggingSyslogPort() {
		int i = getInt(KEY_LOGGING_SYSLOG_PORT, 514);
		if (i < 1 || i > 65535) {
			return 514;
		}

		return i;
	}

	public void setLoggingSyslogPort(int value) {
		if (value < 1 || value > 65535) {
			setLoggingSyslogPortDefault();
		} else {
			configuration.setProperty(KEY_LOGGING_SYSLOG_PORT, value);
		}
	}

	public void setLoggingSyslogPortDefault() {
		setLoggingSyslogPort(514);
	}

	public boolean getLoggingUseSyslog() {
		return getBoolean(KEY_LOGGING_USE_SYSLOG, false);
	}

	public void setLoggingUseSyslog(boolean value) {
		configuration.setProperty(KEY_LOGGING_USE_SYSLOG, value);
	}

	/**
	 * Returns whether database logging is enabled. The returned value is
	 * {@code true} if either the value is {@code true} or a command line
	 * argument has forced it to {@code true}.
	 *
	 * @return {@code true} if database logging is enabled, {@code false}
	 *         otherwise.
	 */
	public boolean getDatabaseLogging() {
		boolean dbLog = getBoolean(KEY_LOG_DATABASE, false);
		return dbLog || PMS.getLogDB();
	}

	/**
	 * Get the embedded Media database cache size.
	 * @return the cache size in Kb
	 */
	public int getDatabaseMediaCacheSize() {
		return getInt(KEY_DATABASE_MEDIA_CACHE_SIZE_KB, -1);
	}

	/**
	 * Set the embedded Media database cache size.
	 * @param value the cache size in Kb
	 */
	public void setDatabaseMediaCacheSize(int value) {
		configuration.setProperty(KEY_DATABASE_MEDIA_CACHE_SIZE_KB, value);
	}

	/**
	 * Return whether the embedded Media database table indexes should sit in memory.
	 * @return true if table indexes should sit on memory
	 */
	public boolean isDatabaseMediaUseMemoryIndexes() {
		return getBoolean(KEY_DATABASE_MEDIA_USE_MEMORY_INDEXES, false);
	}

	/**
	 * Return whether the embedded Media database use soft cache.
	 * @return true if table use soft cache
	 */
	public boolean isDatabaseMediaUseCacheSoft() {
		return getBoolean(KEY_DATABASE_MEDIA_USE_CACHE_SOFT, false);
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

	public int getMinimumWatchedPlayTime() {
		return getInt(KEY_MIN_PLAY_TIME, 30000);
	}

	public int getMinimumWatchedPlayTimeSeconds() {
		return getMinimumWatchedPlayTime() / 1000;
	}

	public int getMinPlayTimeWeb() {
		return getInt(KEY_MIN_PLAY_TIME_WEB, getMinimumWatchedPlayTime());
	}

	public int getMinPlayTimeFile() {
		return getInt(KEY_MIN_PLAY_TIME_FILE, getMinimumWatchedPlayTime());
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

	public int getDepth3D() {
		return getInt(KEY_3D_SUBTITLES_DEPTH, 0);
	}

	public void setDepth3D(int value) {
		configuration.setProperty(KEY_3D_SUBTITLES_DEPTH, value);
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
	 * Whether UMS should allow only one instance by shutting down
	 * the first one when a second one is launched.
	 *
	 * @return value whether to kill the old UMS instance
	 */
	public boolean isRunSingleInstance() {
		return getBoolean(KEY_SINGLE, true);
	}

	public File getWebPath() {
		File path = new File(getString(KEY_WEB_PATH, "web"));
		if (!path.exists()) {
			//check if we are running from sources
			File srcPath = new File("src/main/external-resources/web");
			if (!srcPath.exists()) {
				path.mkdirs();
			} else {
				path = srcPath;
			}
		}
		return path;
	}

	public File getWebFile(String file) {
		return new File(getWebPath().getAbsolutePath() + File.separator + file);
	}

	public boolean isWebAuthenticate() {
		return getBoolean(KEY_WEB_AUTHENTICATE, false);
	}

	/**
	 * Default port for the web gui server.
	 * @return the port that will be used for the web gui server.
	 */
	public int getWebGuiServerPort() {
		return getInt(KEY_WEB_GUI_PORT, DEFAULT_WEB_GUI_PORT);
	}

	public int getWebThreads() {
		int x = getInt(KEY_WEB_THREADS, 30);
		return (x > WEB_MAX_THREADS ? WEB_MAX_THREADS : x);
	}

	/**
	 * Web player enable.
	 *
	 * @return Whether the web player will be loaded and accessible or not
	 */
	public boolean useWebPlayerServer() {
		return getBoolean(KEY_WEB_PLAYER_ENABLE, true);
	}

	/**
	 * Default port for the web player server.
	 * @return the port that will be used for the web player server.
	 */
	public int getWebPlayerServerPort() {
		return getInt(KEY_WEB_PLAYER_PORT, DEFAULT_WEB_PLAYER_PORT);
	}

	public boolean getWebPlayerHttps() {
		return getBoolean(KEY_WEB_PLAYER_HTTPS, false);
	}

	public boolean isWebPlayerControllable() {
		return getBoolean(KEY_WEB_PLAYER_CONTROLLABLE, true);
	}

	public boolean isWebPlayerAuthenticationEnabled() {
		return getBoolean(KEY_WEB_PLAYER_AUTH, false);
	}

	public boolean useWebPlayerControls() {
		return getBoolean(KEY_WEB_PLAYER_CONTROLS, true);
	}

	public boolean useWebPlayerDownload() {
		return getBoolean(KEY_WEB_PLAYER_DOWNLOAD, true);
	}

	public boolean isWebPlayerMp4Trans() {
		return getBoolean(KEY_WEB_PLAYER_MP4_TRANS, false);
	}

	public boolean getWebPlayerAutoCont(Format f) {
		String key = KEY_WEB_PLAYER_CONT_VIDEO;
		boolean def = false;
		if (f.isAudio()) {
			key = KEY_WEB_PLAYER_CONT_AUDIO;
			def = true;
		}

		if (f.isImage()) {
			key = KEY_WEB_PLAYER_CONT_IMAGE;
			def = false;
		}

		return getBoolean(key, def);
	}

	public boolean getWebPlayerAutoLoop(Format f) {
		String key = KEY_WEB_PLAYER_LOOP_VIDEO;
		if (f.isAudio()) {
			key = KEY_WEB_PLAYER_LOOP_AUDIO;
		}

		if (f.isImage()) {
			key = KEY_WEB_PLAYER_LOOP_IMAGE;
		}

		return getBoolean(key, false);
	}

	public int getWebPlayerImgSlideDelay() {
		return getInt(KEY_WEB_PLAYER_IMAGE_SLIDE, 0);
	}

	//TODO : Lang can be set from react
	public boolean getWebPlayerSubs() {
		return getBoolean(KEY_WEB_PLAYER_SUBS_TRANS, false);
	}

	//TODO : Lang can be set from react
	public boolean useWebPlayerSubLang() {
		return getBoolean(KEY_WEB_PLAYER_SUB_LANG, false);
	}

	@Deprecated
	public String getBumpAddress() {
		return getString(KEY_BUMP_ADDRESS, "");
	}

	@Deprecated
	public void setBumpAddress(String value) {
		configuration.setProperty(KEY_BUMP_ADDRESS, value);
	}

	@Deprecated
	public String getBumpJS(String fallback) {
		return getString(KEY_BUMP_JS, fallback);
	}

	@Deprecated
	public String getBumpSkinDir(String fallback) {
		return getString(KEY_BUMP_SKIN_DIR, fallback);
	}

	@Deprecated
	public String getWebSize() {
		return getString(KEY_WEB_SIZE, "");
	}

	@Deprecated
	public int getWebHeight() {
		return getInt(KEY_WEB_HEIGHT, 0);
	}

	@Deprecated
	public int getWebWidth() {
		return getInt(KEY_WEB_WIDTH, 0);
	}

	@Deprecated
	public boolean getWebFlash() {
		return getBoolean(KEY_WEB_FLASH, false);
	}

	@Deprecated
	public String getBumpAllowedIps() {
		return getString(KEY_BUMP_IPS, "");
	}

	@Deprecated
	public boolean useWebLang() {
		return getBoolean(KEY_WEB_BROWSE_LANG, false);
	}

	public String getWebTranscode() {
		return getString(KEY_WEB_TRANSCODE, null);
	}

	public int getWebLowSpeed() {
		return getInt(KEY_WEB_LOW_SPEED, 0);
	}

	public boolean isAutomaticMaximumBitrate() {
		return getBoolean(KEY_AUTOMATIC_MAXIMUM_BITRATE, true);
	}

	public void setAutomaticMaximumBitrate(boolean b) {
		if (!isAutomaticMaximumBitrate() && b) {
			// get all bitrates from renderers
			ConnectedRenderers.calculateAllSpeeds();
		}

		configuration.setProperty(KEY_AUTOMATIC_MAXIMUM_BITRATE, b);
	}

	public boolean isSpeedDbg() {
		return getBoolean(KEY_SPEED_DBG, false);
	}

	public boolean getAutoDiscover() {
		return getBoolean(KEY_AUTOMATIC_DISCOVER, false);
	}

	public boolean useCode() {
		return getBoolean(KEY_CODE_USE, true);
	}

	public int getCodeValidTmo() {
		return (getInt(KEY_CODE_TMO, 4 * 60) * 60 * 1000);
	}

	public boolean isShowCodeThumbs() {
		return getBoolean(KEY_CODE_THUMBS, true);
	}

	public int getCodeCharSet() {
		int cs = getInt(KEY_CODE_CHARS, CodeEnter.DIGITS);
		if (cs < CodeEnter.DIGITS || cs > CodeEnter.BOTH) {
			// ensure we go a legal value
			cs = CodeEnter.DIGITS;
		}

		return cs;
	}

	public boolean isSortAudioTracksByAlbumPosition() {
		return getBoolean(KEY_SORT_AUDIO_TRACKS_BY_ALBUM_POSITION, true);
	}

	public boolean isDynamicPls() {
		return getBoolean(KEY_DYNAMIC_PLS, false);
	}

	public boolean isDynamicPlsAutoSave() {
		return getBoolean(KEY_DYNAMIC_PLS_AUTO_SAVE, false);
	}

	public String getDynamicPlsSavePath() {
		String path = getString(KEY_DYNAMIC_PLS_SAVE_PATH, "");
		if (StringUtils.isEmpty(path)) {
			path = getDataFile("dynpls");
			// ensure that this path exists
			new File(path).mkdirs();
		}

		return path;
	}

	public String getDynamicPlsSaveFile(String str) {
		return getDynamicPlsSavePath() + File.separator + str;
	}

	public boolean isHideSavedPlaylistFolder() {
		return getBoolean(KEY_DYNAMIC_PLS_HIDE, false);
	}

	public boolean isAutoContinue() {
		return getBoolean(KEY_PLAYLIST_AUTO_CONT, false);
	}

	public boolean isAutoAddAll() {
		return getBoolean(KEY_PLAYLIST_AUTO_ADD_ALL, false);
	}

	public String getAutoPlay() {
		return getString(KEY_PLAYLIST_AUTO_PLAY, null);
	}

	public boolean useChromecastExt() {
		return getBoolean(KEY_CHROMECAST_EXT, false);
	}

	public boolean isChromecastDbg() {
		return getBoolean(KEY_CHROMECAST_DBG, false);
	}

	public String getManagedPlaylistFolder() {
		return getString(KEY_MANAGED_PLAYLIST_FOLDER, "");
	}

	/**
	 * Enable the automatically saving of modified properties to the disk.
	 */
	public void setAutoSave() {
		((PropertiesConfiguration) configuration).setAutoSave(true);
	}

	public boolean isUpnpEnabled() {
		return getBoolean(KEY_UPNP_ENABLED, true);
	}

	public boolean isUpnpDebug() {
		return getBoolean(KEY_UPNP_DEBUG, false);
	}

	public String getRootLogLevel() {
		String level = getString(KEY_ROOT_LOG_LEVEL, "DEBUG").toUpperCase();
		return "ALL TRACE DEBUG INFO WARN ERROR OFF".contains(level) ? level : "DEBUG";
	}

	public void setRootLogLevel(ch.qos.logback.classic.Level level) {
		configuration.setProperty(KEY_ROOT_LOG_LEVEL, level.toString());
	}

	public boolean isShowSplashScreen() {
		return getBoolean(KEY_SHOW_SPLASH_SCREEN, true);
	}

	public void setShowSplashScreen(boolean value) {
		configuration.setProperty(KEY_SHOW_SPLASH_SCREEN, value);
	}

	public boolean isInfoDbRetry() {
		return getBoolean(KEY_INFO_DB_RETRY, false);
	}

	public int getUpnpSendAliveDelay() {
		return getInt(KEY_UPNP_ALIVE_DELAY, 0);
	}

	/**
	 * This will show the info display informing user that automatic
	 * video setting were updated and is highly recommended.
	 * @return if info will be shown
	 */
	public boolean showInfoAboutVideoAutomaticSetting() {
		return getBoolean(KEY_SHOW_INFO_ABOUT_AUTOMATIC_VIDEO_SETTING, true);
	}

	public void setShowInfoAboutVideoAutomaticSetting(boolean value) {
		configuration.setProperty(KEY_SHOW_INFO_ABOUT_AUTOMATIC_VIDEO_SETTING, value);
	}

	/**
	 * @return whether UMS has run once.
	 */
	public boolean hasRunOnce() {
		return getBoolean(KEY_WAS_YOUTUBE_DL_ENABLED_ONCE, false);
	}

	/**
	 * Records that UMS has run once.
	 */
	public void setHasRunOnce() {
		configuration.setProperty(KEY_WAS_YOUTUBE_DL_ENABLED_ONCE, true);
	}

	/**
	 * This {@code enum} represents the available "levels" for subtitles
	 * information display that is to be appended to the video name.
	 */
	public enum SubtitlesInfoLevel {

		/** Don't show subtitles information */
		NONE,

		/** Show only basic subtitles information */
		BASIC,

		/** Show full subtitles information */
		FULL;

		@Override
		public String toString() {
			switch (this) {
				case BASIC -> {
					return "basic";
				}
				case FULL -> {
					return "full";
				}
				case NONE -> {
					return "none";
				}
				default -> throw new AssertionError("Missing implementation of SubtitlesInfoLevel \"" + name() + "\"");
			}
		}

		/**
		 * Tries to parse the specified {@link String} and return the
		 * corresponding {@link SubtitlesInfoLevel}.
		 *
		 * @param infoLevelString the {@link String} to parse.
		 * @return The corresponding {@link SubtitlesInfoLevel} or {@code null}
		 *         if the parsing failed.
		 */
		public static SubtitlesInfoLevel typeOf(String infoLevelString) {
			if (StringUtils.isBlank(infoLevelString)) {
				return null;
			}
			infoLevelString = infoLevelString.trim().toLowerCase(Locale.ROOT);
			return switch (infoLevelString) {
				case "off", "none", "0" -> NONE;
				case "basic", "simple", "1" -> BASIC;
				case "full", "advanced", "2" -> FULL;
				default -> null;
			};
		}
	}

	/**
	 * Whether to disable connection to external entities to prevent the XML External Entity vulnerability.
	 *
	 * @return default {@code true} whether to disable external entities.
	 */
	public boolean disableExternalEntities() {
		return getBoolean(KEY_DISABLE_EXTERNAL_ENTITIES, true);
	}

	/**
	 * @return all audio cover suppliers as a JSON array
	 */
	public static synchronized JsonArray getAudioCoverSuppliersAsJsonArray() {
		JsonArray jsonArray = new JsonArray();

		JsonObject noneObject = new JsonObject();
		noneObject.addProperty("value", CoverSupplier.NONE_INTEGER.toString());
		noneObject.addProperty("label", "i18n@None");
		jsonArray.add(noneObject);

		JsonObject coverArtArchiveObject = new JsonObject();
		coverArtArchiveObject.addProperty("value", CoverSupplier.COVER_ART_ARCHIVE_INTEGER.toString());
		coverArtArchiveObject.addProperty("label", "i18n@DownloadFromCoverArtArchive");
		jsonArray.add(coverArtArchiveObject);

		return jsonArray;
	}

	/**
	 * @return sort method as a JSON array
	 */
	public static synchronized JsonArray getSortMethodsAsJsonArray() {
		String[] values = new String[]{
			"" + UMSUtils.SORT_LOC_SENS,  // alphabetical
			"" + UMSUtils.SORT_LOC_NAT,   // natural sort
			"" + UMSUtils.SORT_INS_ASCII, // ASCIIbetical
			"" + UMSUtils.SORT_MOD_NEW,   // newest first
			"" + UMSUtils.SORT_MOD_OLD,   // oldest first
			"" + UMSUtils.SORT_RANDOM,    // random
			"" + UMSUtils.SORT_NO_SORT    // no sorting
		};
		String[] labels = new String[]{
			"i18n@AlphabeticalAZ",
			"i18n@Alphanumeric",
			"i18n@Asciibetical",
			"i18n@ByDateNewestFirst",
			"i18n@ByDateOldestFirst",
			"i18n@Random",
			"i18n@NoSorting"
		};

		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
	}

	public static synchronized JsonObject getAllEnginesAsJsonObject() {
		JsonObject result = new JsonObject();
		for (EngineId engineId : StandardEngineId.ALL) {
			Engine engine = EngineFactory.getEngine(engineId, false, false);
			if (engine != null) {
				JsonObject jsonPlayer = new JsonObject();
				jsonPlayer.add("id", new JsonPrimitive(engineId.getName()));
				jsonPlayer.add("name", new JsonPrimitive(engine.getName()));
				jsonPlayer.add("isAvailable", new JsonPrimitive(engine.isAvailable()));
				jsonPlayer.add("purpose", new JsonPrimitive(engine.purpose()));
				jsonPlayer.add("statusText", engine.getStatusTextFullAsJsonArray());
				result.add(engineId.getName(), jsonPlayer);
			}
		}
		return result;
	}

	public static synchronized JsonArray getAllEnginesAsJsonArray() {
		JsonArray result = new JsonArray();
		for (EngineId engineId : StandardEngineId.ALL) {
			result.add(engineId.getName());
		}
		return result;
	}

	public static synchronized JsonArray getEnginesPurposesAsJsonArray() {
		JsonArray result = new JsonArray();
		result.add("i18n@VideoFilesEngines"); //Player.VIDEO_SIMPLEFILE_PLAYER = 0
		result.add("i18n@AudioFilesEngines"); //Player.AUDIO_SIMPLEFILE_PLAYER = 1
		result.add("i18n@WebVideoStreamingEngines"); //Player.VIDEO_WEBSTREAM_PLAYER = 2
		result.add("i18n@WebAudioStreamingEngines"); //Player.AUDIO_WEBSTREAM_PLAYER = 3
		result.add("i18n@MiscEngines"); //Player.MISC_PLAYER = 4
		return result;
	}

	public static synchronized JsonArray getSubtitlesDepthArray() {
		String[] values = new String[]{"-5", "-4", "-3", "-2", "-1", "0", "1", "2", "3", "4", "5"};
		String[] labels = new String[]{"-5", "-4", "-3", "-2", "-1", "0", "1", "2", "3", "4", "5"};
		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
	}

	public static synchronized JsonArray getFFmpegAvailableGPUH264EncodingAccelerationMethodsArray() {
		String[] values = getFFmpegAvailableGPUH264EncodingAccelerationMethods();
		String[] labels = getFFmpegAvailableGPUH264EncodingAccelerationMethods();
		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
	}

	public static synchronized JsonArray getFFmpegAvailableGPUH265EncodingAccelerationMethodsArray() {
		String[] values = getFFmpegAvailableGPUH265EncodingAccelerationMethods();
		String[] labels = getFFmpegAvailableGPUH265EncodingAccelerationMethods();
		return UMSUtils.getArraysAsJsonArrayOfObjects(values, labels, null);
	}

	public static JsonObject getWebSettingsWithDefaults() {
		// populate WEB_SETTINGS_WITH_DEFAULTS with all defaults
		JsonObject jObj = new JsonObject();
		jObj.addProperty(KEY_ALTERNATE_SUBTITLES_FOLDER, "");
		jObj.addProperty(KEY_ALTERNATE_THUMB_FOLDER, "");
		jObj.addProperty(KEY_APPEND_PROFILE_NAME, false);
		jObj.addProperty(KEY_ATZ_LIMIT, 10000);
		jObj.addProperty(KEY_AUDIO_CHANNEL_COUNT, "6");
		jObj.addProperty(KEY_AUDIO_EMBED_DTS_IN_PCM, false);
		jObj.addProperty(KEY_AUDIO_BITRATE, "448");
		jObj.addProperty(KEY_AUDIO_LANGUAGES, Messages.getString("AudioLanguages"));
		jObj.addProperty(KEY_AUDIO_REMUX_AC3, true);
		jObj.addProperty(KEY_AUDIO_RESAMPLE, true);
		jObj.addProperty(KEY_AUDIO_SUB_LANGS, "");
		jObj.addProperty(KEY_AUDIO_THUMBNAILS_METHOD, "1");
		jObj.addProperty(KEY_AUDIO_USE_PCM, false);
		jObj.addProperty(KEY_AUTO_UPDATE, true);
		jObj.addProperty(KEY_AUTOLOAD_SUBTITLES, true);
		jObj.addProperty(KEY_AUTOMATIC_MAXIMUM_BITRATE, true);
		jObj.addProperty(KEY_CHAPTER_INTERVAL, 5);
		jObj.addProperty(KEY_CHAPTER_SUPPORT, false);
		jObj.addProperty(KEY_CHROMECAST_EXT, false);
		jObj.addProperty(KEY_DISABLE_SUBTITLES, false);
		jObj.addProperty(KEY_DISABLE_TRANSCODE_FOR_EXTENSIONS, "");
		jObj.addProperty(KEY_OPEN_ARCHIVES, false);
		jObj.addProperty(KEY_ENCODED_AUDIO_PASSTHROUGH, false);
		JsonArray transcodingEngines = UmsConfiguration.getAllEnginesAsJsonArray();
		jObj.add(KEY_ENGINES, transcodingEngines);
		jObj.add(KEY_ENGINES_PRIORITY, transcodingEngines);
		jObj.addProperty(KEY_FORCE_TRANSCODE_FOR_EXTENSIONS, "");
		jObj.addProperty(KEY_FULLY_PLAYED_ACTION, String.valueOf(FullyPlayedAction.MARK.getValue()));
		jObj.addProperty(KEY_FULLY_PLAYED_OUTPUT_DIRECTORY, "");
		jObj.addProperty(KEY_GPU_ACCELERATION, false);
		jObj.addProperty(KEY_EXTERNAL_NETWORK, true);
		jObj.addProperty(KEY_FFMPEG_FONTCONFIG, false);
		jObj.addProperty(KEY_FFMPEG_GPU_DECODING_ACCELERATION_METHOD, "none");
		jObj.addProperty(KEY_FFMPEG_GPU_DECODING_ACCELERATION_THREAD_NUMBER, 1);
		jObj.addProperty(KEY_FFMPEG_GPU_ENCODING_H264_ACCELERATION_METHOD, "libx264");
		jObj.addProperty(KEY_FFMPEG_GPU_ENCODING_H265_ACCELERATION_METHOD, "libx265");
		jObj.addProperty(KEY_FFMPEG_LOGGING_LEVEL, "fatal");
		jObj.addProperty(KEY_FFMPEG_MENCODER_PROBLEMATIC_SUBTITLES, true);
		jObj.addProperty(KEY_FFMPEG_MULTITHREADING, getNumberOfSystemCpuCores() > 1);
		jObj.addProperty(KEY_FFMPEG_MUX_TSMUXER_COMPATIBLE, false);
		jObj.addProperty(KEY_FFMPEG_SOX, true);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_CONVERT_FPS, true);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_INTERFRAME, false);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_INTERFRAME_GPU, false);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_MULTITHREADING, getNumberOfSystemCpuCores() > 1);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_2D_TO_3D, false);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_CONVERSION_ALGORITHM_2D_TO_3D, "1");
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_FRAME_STRETCH_FACTOR_2D_TO_3D, 8);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_LIGHT_OFFSET_FACTOR_2D_TO_3D, 3);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_OUTPUT_FORMAT_3D, "4");
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE, true);
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_HORIZONTAL_RESIZE_RESOLUTION, "1920");
		jObj.addProperty(KEY_FFMPEG_AVISYNTH_USE_FFMS2, false);
		jObj.addProperty(KEY_FORCE_EXTERNAL_SUBTITLES, true);
		jObj.addProperty(KEY_FORCED_SUBTITLE_LANGUAGE, "");
		jObj.addProperty(KEY_FORCED_SUBTITLE_TAGS, "forced");
		jObj.addProperty(KEY_THUMBNAIL_GENERATION_ENABLED, true);
		jObj.addProperty(KEY_HIDE_EMPTY_FOLDERS, false);
		jObj.addProperty(KEY_HIDE_ENGINENAMES, true);
		jObj.addProperty(KEY_HIDE_EXTENSIONS, true);
		jObj.addProperty(KEY_SERVER_HOSTNAME, "");
		jObj.addProperty(KEY_IGNORE_THE_WORD_A_AND_THE, true);
		jObj.addProperty(KEY_IP_FILTER, "");
		jObj.addProperty(KEY_LANGUAGE, "en-US");
		jObj.addProperty(KEY_LIVE_SUBTITLES_KEEP, false);
		jObj.addProperty(KEY_LIVE_SUBTITLES_LIMIT, 20);
		jObj.addProperty(KEY_MENCODER_ASS, true);
		jObj.addProperty(KEY_MENCODER_CODEC_SPECIFIC_SCRIPT, "");
		jObj.addProperty(KEY_MENCODER_CUSTOM_OPTIONS, "");
		jObj.addProperty(KEY_MENCODER_FONT_CONFIG, true);
		jObj.addProperty(KEY_MENCODER_FORCE_FPS, false);
		jObj.addProperty(KEY_MENCODER_INTELLIGENT_SYNC, true);
		jObj.addProperty(KEY_MENCODER_MT, getNumberOfSystemCpuCores() > 1);
		jObj.addProperty(KEY_MENCODER_MUX_COMPATIBLE, false);
		jObj.addProperty(KEY_MENCODER_NOASS_OUTLINE, 1);
		jObj.addProperty(KEY_MENCODER_NO_OUT_OF_SYNC, false);
		jObj.addProperty(KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT, "0");
		jObj.addProperty(KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH, "0");
		jObj.addProperty(KEY_MENCODER_REMUX_MPEG2, true);
		jObj.addProperty(KEY_MENCODER_SCALER, false);
		jObj.addProperty(KEY_MENCODER_SCALEX, "0");
		jObj.addProperty(KEY_MENCODER_SCALEY, "0");
		jObj.addProperty(KEY_SKIP_LOOP_FILTER_ENABLED, false);
		jObj.addProperty(KEY_MENCODER_SUB_FRIBIDI, false);
		jObj.addProperty(KEY_MENCODER_YADIF, false);
		jObj.addProperty(KEY_MAX_MEMORY_BUFFER_SIZE, 200);
		jObj.addProperty(KEY_MAX_BITRATE, 90);
		jObj.addProperty(KEY_MINIMIZED, false);
		jObj.addProperty(KEY_TSMUXER_FORCEFPS, true);
		jObj.addProperty(KEY_MUX_ALLAUDIOTRACKS, false);
		jObj.addProperty(KEY_MPEG2_MAIN_SETTINGS, "Automatic (Wired)");
		jObj.addProperty(KEY_NETWORK_INTERFACE, "");
		int numberOfCpuCores = Runtime.getRuntime().availableProcessors();
		if (numberOfCpuCores < 1) {
			numberOfCpuCores = 1;
		}
		jObj.addProperty(KEY_NUMBER_OF_CPU_CORES, numberOfCpuCores);
		jObj.addProperty(KEY_SERVER_PORT, 5001);
		jObj.addProperty(KEY_PRETTIFY_FILENAMES, false);
		jObj.addProperty(KEY_RENDERER_DEFAULT, "");
		jObj.addProperty(KEY_RENDERER_FORCE_DEFAULT, false);
		jObj.addProperty(KEY_RESUME, true);
		JsonArray allRenderers = new JsonArray();
		allRenderers.add(RendererConfigurations.ALL_RENDERERS_KEY);
		jObj.add(KEY_SELECTED_RENDERERS, allRenderers);
		jObj.addProperty(KEY_SERVER_ENGINE, "0");
		jObj.addProperty(KEY_SERVER_NAME, "Universal Media Server");
		jObj.addProperty(KEY_SHOW_LIVE_SUBTITLES_FOLDER, false);
		jObj.addProperty(KEY_SHOW_MEDIA_LIBRARY_FOLDER, true);
		jObj.addProperty(KEY_SHOW_RECENTLY_PLAYED_FOLDER, true);
		jObj.addProperty(KEY_SHOW_SERVER_SETTINGS_FOLDER, false);
		jObj.addProperty(KEY_SHOW_SPLASH_SCREEN, true);
		jObj.addProperty(KEY_SHOW_TRANSCODE_FOLDER, true);
		jObj.addProperty(KEY_SORT_METHOD, "4");
		jObj.addProperty(KEY_SUBS_INFO_LEVEL, "basic");
		jObj.addProperty(KEY_SUBTITLES_CODEPAGE, "");
		jObj.addProperty(KEY_SUBTITLES_LANGUAGES, Messages.getString("SubtitlesLanguages"));
		jObj.addProperty(KEY_SUBS_COLOR, "0xFFFFFFFF");
		jObj.addProperty(KEY_SUBS_FONT, "");
		jObj.addProperty(KEY_ASS_MARGIN, 10);
		jObj.addProperty(KEY_ASS_SCALE, 1.4);
		jObj.addProperty(KEY_ASS_SHADOW, 1);
		jObj.addProperty(KEY_THUMBNAIL_SEEK_POS, 4);
		jObj.addProperty(KEY_UPNP_ENABLED, true);
		jObj.addProperty(KEY_USE_EMBEDDED_SUBTITLES_STYLE, true);
		jObj.addProperty(KEY_USE_CACHE, true);
		jObj.addProperty(KEY_USE_IMDB_INFO, true);
		jObj.addProperty(KEY_USE_SYMLINKS_TARGET_FILE, true);
		jObj.addProperty(KEY_VLC_AUDIO_SYNC_ENABLED, false);
		jObj.addProperty(KEY_VLC_USE_EXPERIMENTAL_CODECS, false);
		jObj.addProperty(KEY_WEB_PLAYER_ENABLE, true);
		jObj.addProperty(KEY_WEB_GUI_ON_START, true);
		jObj.addProperty(KEY_WEB_GUI_PORT, DEFAULT_WEB_GUI_PORT);
		jObj.addProperty(KEY_WEB_PLAYER_AUTH, false);
		jObj.addProperty(KEY_WEB_PLAYER_CONTROLS, true);
		jObj.addProperty(KEY_WEB_PLAYER_DOWNLOAD, true);
		jObj.addProperty(KEY_WEB_PLAYER_PORT, DEFAULT_WEB_PLAYER_PORT);
		jObj.addProperty(KEY_X264_CONSTANT_RATE_FACTOR, "Automatic (Wired)");
		jObj.addProperty(KEY_3D_SUBTITLES_DEPTH, "0");
		return jObj;
	}

}
