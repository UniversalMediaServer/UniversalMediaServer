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
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import net.pms.Messages;
import net.pms.io.SystemUtils;
import net.pms.util.PropertiesUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
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

	// MEncoder has a hardwired maximum of 16 threads for -lavcopts and -lavdopts
	private static final int MENCODER_MAX_THREADS = 16;

	private static final String KEY_ALTERNATE_SUBS_FOLDER = "alternate_subs_folder";
	private static final String KEY_ALTERNATE_THUMB_FOLDER = "alternate_thumb_folder";
	private static final String KEY_APERTURE_ENABLED = "aperture";
	private static final String KEY_AUDIO_BITRATE = "audiobitrate";
	private static final String KEY_AUDIO_CHANNEL_COUNT = "audiochannels";
	private static final String KEY_AUDIO_RESAMPLE = "audio_resample";
	private static final String KEY_AUDIO_THUMBNAILS_METHOD = "audio_thumbnails_method";
	private static final String KEY_AUTO_UPDATE = "auto_update";
	private static final String KEY_AVISYNTH_CONVERT_FPS = "avisynth_convertfps";
	private static final String KEY_AVISYNTH_INTERFRAME = "avisynth_interframe";
	private static final String KEY_AVISYNTH_INTERFRAME_GPU = "avisynth_interframegpu";
	private static final String KEY_AVISYNTH_MULTITHREADING = "avisynth_multithreading";
	private static final String KEY_AVISYNTH_SCRIPT = "avisynth_script";
	private static final String KEY_BUFFER_TYPE = "buffertype";
	private static final String KEY_CHAPTER_INTERVAL = "chapter_interval";
	private static final String KEY_CHAPTER_SUPPORT = "chapter_support";
	private static final String KEY_CHARSET_ENCODING = "charsetencoding";
	private static final String KEY_CODEC_SPEC_SCRIPT = "codec_spec_script";
	private static final String KEY_DISABLE_FAKESIZE = "disable_fakesize";
	private static final String KEY_DVDISO_THUMBNAILS = "dvd_isos_thumbnails";
	private static final String KEY_EMBED_DTS_IN_PCM = "embed_dts_in_pcm";
	private static final String KEY_ENGINES = "engines";
	private static final String KEY_FFMPEG_ALTERNATIVE_PATH = "alternativeffmpegpath";
	private static final String KEY_FFMPEG_SETTINGS = "ffmpeg";
	private static final String KEY_FIX_25FPS_AV_MISMATCH = "fix_25fps_av_mismatch";
	private static final String KEY_FORCETRANSCODE = "forcetranscode";
	private static final String KEY_FOLDER_LIMIT="folder_limit";
	private static final String KEY_HIDE_EMPTY_FOLDERS = "hide_empty_folders";
	private static final String KEY_HIDE_ENGINENAMES = "hide_enginenames";
	private static final String KEY_HIDE_EXTENSIONS = "hide_extensions";
	private static final String KEY_HIDE_MEDIA_LIBRARY_FOLDER = "hide_media_library_folder";
	private static final String KEY_HIDE_TRANSCODE_FOLDER = "hide_transcode_folder";
	private static final String KEY_HIDE_VIDEO_SETTINGS = "hidevideosettings";
	private static final String KEY_HTTP_ENGINE_V2 = "http_engine_v2";
	private static final String KEY_IMAGE_THUMBNAILS_ENABLED = "image_thumbnails";
	private static final String KEY_IP_FILTER = "ip_filter";
	private static final String KEY_IPHOTO_ENABLED = "iphoto";
	private static final String KEY_ITUNES_ENABLED = "itunes";
	private static final String KEY_LANGUAGE = "language";
	private static final String KEY_MAX_AUDIO_BUFFER = "maxaudiobuffer";
	private static final String KEY_MAX_BITRATE = "maximumbitrate";
	private static final String KEY_MAX_MEMORY_BUFFER_SIZE = "maxvideobuffer";
	private static final String KEY_MENCODER_AC3_FIXED = "mencoder_ac3_fixed";
	private static final String KEY_MENCODER_ASS_DEFAULTSTYLE = "mencoder_ass_defaultstyle";
	private static final String KEY_MENCODER_ASS_MARGIN = "mencoder_ass_margin";
	private static final String KEY_MENCODER_ASS = "mencoder_ass";
	private static final String KEY_MENCODER_ASS_OUTLINE = "mencoder_ass_outline";
	private static final String KEY_MENCODER_ASS_SCALE = "mencoder_ass_scale";
	private static final String KEY_MENCODER_ASS_SHADOW = "mencoder_ass_shadow";
	private static final String KEY_MENCODER_AUDIO_LANGS = "mencoder_audiolangs";
	private static final String KEY_MENCODER_AUDIO_SUB_LANGS = "mencoder_audiosublangs";
	private static final String KEY_MENCODER_CUSTOM_OPTIONS = "mencoder_decode"; // TODO (breaking change): should be renamed to e.g. mencoder_custom_options
	private static final String KEY_MENCODER_DISABLE_SUBS = "mencoder_disablesubs";
	private static final String KEY_MENCODER_FONT_CONFIG = "mencoder_fontconfig";
	private static final String KEY_MENCODER_FONT = "mencoder_font";
	private static final String KEY_MENCODER_FORCED_SUB_LANG = "forced_sub_lang";
	private static final String KEY_MENCODER_FORCED_SUB_TAGS = "forced_sub_tags";
	private static final String KEY_MENCODER_FORCE_FPS = "mencoder_forcefps";
	private static final String KEY_MENCODER_INTELLIGENT_SYNC = "mencoder_intelligent_sync";
	private static final String KEY_MENCODER_MAIN_SETTINGS = "mencoder_encode";
	private static final String KEY_MENCODER_MAX_THREADS = "mencoder_max_threads";
	private static final String KEY_MENCODER_MT = "mencoder_mt";
	private static final String KEY_MENCODER_MUX_COMPATIBLE = "mencoder_mux_compatible";
	private static final String KEY_MENCODER_NOASS_BLUR = "mencoder_noass_blur";
	private static final String KEY_MENCODER_NOASS_OUTLINE = "mencoder_noass_outline";
	private static final String KEY_MENCODER_NOASS_SCALE = "mencoder_noass_scale";
	private static final String KEY_MENCODER_NOASS_SUBPOS = "mencoder_noass_subpos";
	private static final String KEY_MENCODER_NO_OUT_OF_SYNC = "mencoder_nooutofsync";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_HEIGHT = "mencoder_overscan_compensation_height";
	private static final String KEY_MENCODER_OVERSCAN_COMPENSATION_WIDTH = "mencoder_overscan_compensation_width";
	private static final String KEY_MENCODER_REMUX_AC3 = "mencoder_remux_ac3";
	private static final String KEY_MENCODER_REMUX_MPEG2 = "mencoder_remux_mpeg2";
	private static final String KEY_MENCODER_SCALER = "mencoder_scaler";
	private static final String KEY_MENCODER_SCALEX = "mencoder_scalex";
	private static final String KEY_MENCODER_SCALEY = "mencoder_scaley";
	private static final String KEY_MENCODER_SUB_CP = "mencoder_subcp";
	private static final String KEY_MENCODER_SUB_FRIBIDI = "mencoder_subfribidi";
	private static final String KEY_MENCODER_SUB_LANGS = "mencoder_sublangs";
	private static final String KEY_MENCODER_USE_PCM = "mencoder_usepcm";
	private static final String KEY_MENCODER_USE_PCM_FOR_HQ_AUDIO_ONLY = "mencoder_usepcm_for_hq_audio_only";
	private static final String KEY_MENCODER_VOBSUB_SUBTITLE_QUALITY = "mencoder_vobsub_subtitle_quality";
	private static final String KEY_MENCODER_YADIF = "mencoder_yadif";
	private static final String KEY_MINIMIZED = "minimized";
	private static final String KEY_MIN_MEMORY_BUFFER_SIZE = "minvideobuffer";
	private static final String KEY_MIN_STREAM_BUFFER = "minwebbuffer";
	private static final String KEY_MUX_ALLAUDIOTRACKS = "tsmuxer_mux_all_audiotracks";
	private static final String KEY_NETWORK_INTERFACE = "network_interface";
	private static final String KEY_NOTRANSCODE = "notranscode";
	private static final String KEY_NUMBER_OF_CPU_CORES = "nbcores";
	private static final String KEY_OPEN_ARCHIVES = "enable_archive_browsing";
	private static final String KEY_OVERSCAN = "mencoder_overscan";
	private static final String KEY_PLUGIN_DIRECTORY = "plugins";
	private static final String KEY_PREVENTS_SLEEP = "prevents_sleep_mode";
	private static final String KEY_PROFILE_NAME = "name";
	private static final String KEY_PROXY_SERVER_PORT = "proxy";
	private static final String KEY_RENDERER_DEFAULT = "renderer_default";
	private static final String KEY_RENDERER_FORCE_DEFAULT = "renderer_force_default";
	private static final String KEY_SERVER_HOSTNAME = "hostname";
	private static final String KEY_SERVER_PORT = "port";
	private static final String KEY_SHARES = "shares";
	private static final String KEY_SKIP_LOOP_FILTER_ENABLED = "skiploopfilter";
	private static final String KEY_SKIP_NETWORK_INTERFACES = "skip_network_interfaces";
	private static final String KEY_SORT_METHOD = "key_sort_method";
	private static final String KEY_SUBS_COLOR = "subs_color";
	private static final String KEY_TEMP_FOLDER_PATH = "temp";
	private static final String KEY_THUMBNAIL_GENERATION_ENABLED = "thumbnails"; // TODO (breaking change): should be renamed to e.g. generate_thumbnails
	private static final String KEY_THUMBNAIL_SEEK_POS = "thumbnail_seek_pos";
	private static final String KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS = "transcode_block_multiple_connections";
	private static final String KEY_TRANSCODE_KEEP_FIRST_CONNECTION = "transcode_keep_first_connection";
	private static final String KEY_TSMUXER_FORCEFPS = "tsmuxer_forcefps";
	private static final String KEY_TSMUXER_PREREMIX_AC3 = "tsmuxer_preremix_ac3";
	private static final String KEY_TURBO_MODE_ENABLED = "turbomode";
	private static final String KEY_UPNP_PORT = "upnp_port";
	private static final String KEY_USE_CACHE = "usecache";
	private static final String KEY_USE_MPLAYER_FOR_THUMBS = "use_mplayer_for_video_thumbs";
	private static final String KEY_USE_SUBTITLES = "autoloadsrt";
	private static final String KEY_UUID = "uuid";
	private static final String KEY_VIDEOTRANSCODE_START_DELAY = "key_videotranscode_start_delay";
	private static final String KEY_VIRTUAL_FOLDERS = "vfolders";
	private static final String KEY_BUFFER_MAX = "buffer_max";
	private static final String KEY_PLUGIN_PURGE_ACTION = "plugin_purge";
	private static final String KEY_SEARCH_FOLDER = "search_folder";
	private static final String KEY_SEARCH_RECURSE = "search_recurse";

	// the name of the subdirectory under which PMS config files are stored for this build (default: PMS).
	// see Build for more details
	private static final String PROFILE_DIRECTORY_NAME = Build.getProfileDirectoryName();

	// the default profile name displayed on the renderer
	private static String HOSTNAME;

	private static String DEFAULT_AVI_SYNTH_SCRIPT;
	private static final String BUFFER_TYPE_FILE = "file";
	private static final int MAX_MAX_MEMORY_DEFAULT_SIZE = 400;
	private static final int BUFFER_MEMORY_FACTOR = 368;
	private static int MAX_MAX_MEMORY_BUFFER_SIZE = MAX_MAX_MEMORY_DEFAULT_SIZE;
	private static final char LIST_SEPARATOR = ',';
	private static final String KEY_FOLDERS = "folders";
	private final PropertiesConfiguration configuration;
	private final TempFolder tempFolder;
	private final ProgramPathDisabler programPaths;

	private final IpFilter filter = new IpFilter();

	/**
	 * The set of the keys defining when the HTTP server has to restarted due to a configuration change
	 */
	public static final Set<String> NEED_RELOAD_FLAGS = new HashSet<String>(
		Arrays.asList(
			KEY_ALTERNATE_THUMB_FOLDER,
			KEY_NETWORK_INTERFACE,
			KEY_IP_FILTER,
			KEY_SORT_METHOD,
			KEY_HIDE_EMPTY_FOLDERS,
			KEY_HIDE_TRANSCODE_FOLDER,
			KEY_HIDE_MEDIA_LIBRARY_FOLDER,
			KEY_OPEN_ARCHIVES,
			KEY_USE_CACHE,
			KEY_HIDE_ENGINENAMES,
			KEY_ITUNES_ENABLED,
			KEY_IPHOTO_ENABLED,
			KEY_APERTURE_ENABLED,
			KEY_ENGINES,
			KEY_FOLDERS,
			KEY_HIDE_VIDEO_SETTINGS,
			KEY_AUDIO_THUMBNAILS_METHOD,
			KEY_NOTRANSCODE,
			KEY_FORCETRANSCODE,
			KEY_SERVER_PORT,
			KEY_SERVER_HOSTNAME,
			KEY_CHAPTER_SUPPORT,
			KEY_HIDE_EXTENSIONS
		)
	);

	/*
		The following code enables a single setting - UMS_PROFILE - to be used to
		initialize PROFILE_PATH i.e. the path to the current session's profile (AKA UMS.conf).
		It also initializes PROFILE_DIRECTORY - i.e. the directory the profile is located in -
		which is needed for configuration-by-convention detection of WEB.conf (anything else?).

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
	private static final String PROFILE_DIRECTORY; // path to directory containing UMS config files
	private static final String PROFILE_PATH; // abs path to profile file e.g. /path/to/UMS.conf
	private static final String SKEL_PROFILE_PATH ; // abs path to skel (default) profile file e.g. /etc/skel/.config/universalmediaserver/UMS.conf
	                                                // "project.skelprofile.dir" project property
	private static final String PROPERTY_PROFILE_PATH = "ums.profile.path";

	static {
        // first try the system property, typically set via the profile chooser
		String profile = System.getProperty(PROPERTY_PROFILE_PATH);

		// failing that, try the environment variable
		if (profile == null) {
			profile = System.getenv(ENV_PROFILE_PATH);
		}

		if (profile != null) {
			File f = new File(profile);

			// if it exists, we know whether it's a file or directory
			// otherwise, it must be a file since we don't autovivify directories

			if (f.exists() && f.isDirectory()) {
				PROFILE_DIRECTORY = FilenameUtils.normalize(f.getAbsolutePath());
				PROFILE_PATH = FilenameUtils.normalize(new File(f, DEFAULT_PROFILE_FILENAME).getAbsolutePath());
			} else { // doesn't exist or is a file (i.e. not a directory)
				PROFILE_PATH = FilenameUtils.normalize(f.getAbsolutePath());
				PROFILE_DIRECTORY = FilenameUtils.normalize(f.getParentFile().getAbsolutePath());
			}
		} else {
			String profileDir = null;

			if (Platform.isWindows()) {
				String programData = System.getenv("ALLUSERSPROFILE");
				if (programData != null) {
					profileDir = String.format("%s\\%s", programData, PROFILE_DIRECTORY_NAME);
				} else {
					profileDir = ""; // i.e. current (working) directory
				}
			} else if (Platform.isMac()) {
				profileDir = String.format(
					"%s/%s/%s",
					System.getProperty("user.home"),
					"/Library/Application Support",
					PROFILE_DIRECTORY_NAME
				);
			} else {
				String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");

				if (xdgConfigHome == null) {
					profileDir = String.format("%s/.config/%s", System.getProperty("user.home"), PROFILE_DIRECTORY_NAME);
				} else {
					profileDir = String.format("%s/%s", xdgConfigHome, PROFILE_DIRECTORY_NAME);
				}
			}

			File f = new File(profileDir);

			if ((f.exists() || f.mkdir()) && f.isDirectory()) {
				PROFILE_DIRECTORY = FilenameUtils.normalize(f.getAbsolutePath());
			} else {
				PROFILE_DIRECTORY = FilenameUtils.normalize(new File("").getAbsolutePath());
			}

			PROFILE_PATH = FilenameUtils.normalize(new File(PROFILE_DIRECTORY, DEFAULT_PROFILE_FILENAME).getAbsolutePath());
		}
        // set SKEL_PROFILE_PATH for Linux systems
        String skelDir = PropertiesUtil.getProjectProperties().get("project.skelprofile.dir");
        if (Platform.isLinux() && StringUtils.isNotBlank(skelDir)) {
            SKEL_PROFILE_PATH = FilenameUtils.normalize(new File(new File(skelDir, PROFILE_DIRECTORY_NAME).getAbsolutePath(), DEFAULT_PROFILE_FILENAME).getAbsolutePath());
        } else {
            SKEL_PROFILE_PATH = null;
        }
	}

	/**
	 * Default constructor that will attempt to load the PMS configuration file
	 * from the profile path.
	 *
	 * @throws org.apache.commons.configuration.ConfigurationException
	 * @throws java.io.IOException
	 */
	public PmsConfiguration() throws ConfigurationException, IOException {
		this(true);
	}

	/**
	 * Constructor that will initialize the PMS configuration.
	 *
	 * @param loadFile Set to true to attempt to load the PMS configuration
	 * 					file from the profile path. Set to false to skip
	 * 					loading.
	 * @throws org.apache.commons.configuration.ConfigurationException
	 * @throws java.io.IOException
	 */
	public PmsConfiguration(boolean loadFile) throws ConfigurationException, IOException {
		configuration = new PropertiesConfiguration();
		configuration.setListDelimiter((char) 0);

		if (loadFile) {
			File pmsConfFile = new File(PROFILE_PATH);
	
			if (pmsConfFile.isFile() && pmsConfFile.canRead()) {
				configuration.load(PROFILE_PATH);
			} else if (SKEL_PROFILE_PATH != null) {
                File pmsSkelConfFile = new File(SKEL_PROFILE_PATH);
                if (pmsSkelConfFile.isFile() && pmsSkelConfFile.canRead()) {
                    // load defaults from skel file, save them later to PROFILE_PATH
                    configuration.load(pmsSkelConfFile);
                    LOGGER.info("Default configuration loaded from " + SKEL_PROFILE_PATH);
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
	private static ProgramPathDisabler createProgramPathsChain(Configuration configuration) {
		return new ProgramPathDisabler(
			new ConfigurationProgramPaths(configuration,
			new WindowsRegistryProgramPaths(
			new PlatformSpecificDefaultPathsFactory().get())));
	}

	public File getTempFolder() throws IOException {
		return tempFolder.getTempFolder();
	}

	public String getVlcPath() {
		return programPaths.getVlcPath();
	}

	public void disableVlc() {
		programPaths.disableVlc();
	}

	public String getEac3toPath() {
		return programPaths.getEac3toPath();
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

	public void disableMEncoder() {
		programPaths.disableMencoder();
	}

	public String getFfmpegPath() {
		return programPaths.getFfmpegPath();
	}

	public void disableFfmpeg() {
		programPaths.disableFfmpeg();
	}

	public String getMplayerPath() {
		return programPaths.getMplayerPath();
	}

	public void disableMplayer() {
		programPaths.disableMplayer();
	}

	public String getTsmuxerPath() {
		return programPaths.getTsmuxerPath();
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
		return configuration.getBoolean(KEY_TSMUXER_FORCEFPS, true);
	}

	/**
	 * Force tsMuxeR to mux all audio tracks.
	 * TODO: Remove this redundant code.
	 * @return True
	 */
	public boolean isTsmuxerPreremuxAc3() {
		return true;
	}

	/**
	 * The AC3 audio bitrate determines the quality of digital audio sound. An AV-receiver
	 * or amplifier has to be capable of playing this quality. Default value is 640.
	 * @return The AC3 audio bitrate.
	 */
	public int getAudioBitrate() {
		return getInt(KEY_AUDIO_BITRATE, 640);
	}

	/**
	 * Force tsMuxeR to mux all audio tracks.
	 * TODO: Remove this redundant code; getter always returns true.
	 */
	public void setTsmuxerPreremuxAc3(boolean value) {
		configuration.setProperty(KEY_TSMUXER_PREREMIX_AC3, value);
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
		String value = getString(KEY_SERVER_HOSTNAME, "");
		if (StringUtils.isNotBlank(value)) {
			return value;
		} else {
			return null;
		}
	}

	/**
	 * Set the hostname of the server.
	 * @param value The hostname.
	 */
	public void setHostname(String value) {
		configuration.setProperty(KEY_SERVER_HOSTNAME, value);
	}

	/**
	 * The TCP/IP port number for a proxy server. Default value is -1.
	 * TODO: Is this still used?
	 * @return The proxy port number.
	 */
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
		String value = getString(KEY_LANGUAGE, def);
		return StringUtils.isNotBlank(value) ? value.trim() : def;
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
		try {
			return configuration.getInt(key, def);
		} catch (ConversionException e) {
			return def;
		}
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
		try {
			return configuration.getBoolean(key, def);
		} catch (ConversionException e) {
			return def;
		}
	}

	/**
	 * Return the <code>String</code> value for a given configuration key. First, the
	 * key is looked up in the current configuration settings. If it exists and contains
	 * a valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private String getString(String key, String def) {
		String value = configuration.getString(key, def);
		if (value != null) {
			value = value.trim();
		}
		if ("".equals(value)) {
			return def;
		}
		return value;
	}
	
	/**
	 * Return a <code>List</code> of <code>String</code> values for a given configuration
	 * key. First, the key is looked up in the current configuration settings. If it
	 * exists and contains a valid value, that value is returned. If the key contains an
	 * invalid value or cannot be found, a list with the specified default values is
	 * returned.
	 * @param key The key to look up.
	 * @param def The default values to return when no valid key value can be found.
	 *            These values should be entered as a comma separated string, whitespace
	 *            will be trimmed. For example: <code>"gnu,    gnat  ,moo "</code> will be
	 *            returned as <code>{ "gnu", "gnat", "moo" }</code>.
	 * @return The list of value strings configured for the key.
	 */
	private List<String> getStringList(String key, String def) {
		String value = getString(key, def);
		if (value != null) {
			String[] arr = value.split(",");
			List<String> result = new ArrayList<String>(arr.length);
			for (String str : arr) {
				if (str.trim().length() > 0) {
					result.add(str.trim());
				}
			}
			return result;
		} else {
			return Collections.emptyList();
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
	 * value is 400.
	 *
	 * @return The maximum memory buffer size.
	 */
	public int getMaxMemoryBufferSize() {
		return Math.max(0, Math.min(MAX_MAX_MEMORY_BUFFER_SIZE, getInt(KEY_MAX_MEMORY_BUFFER_SIZE, 400)));
	}

	/**
	 * Returns the top limit that can be set for the maximum memory buffer size.
	 * @return The top limit.
	 */
	public String getMaxMemoryBufferSizeStr() {
		return String.valueOf(MAX_MAX_MEMORY_BUFFER_SIZE);
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
	public String getMencoderAssScale() {
		return getString(KEY_MENCODER_ASS_SCALE, "1.4");
	}

	/**
	 * Some versions of MEncoder produce garbled audio because the "ac3" codec is used
	 * instead of the "ac3_fixed" codec. Returns true if "ac3_fixed" should be used.
	 * Default is false.
	 * See https://code.google.com/p/ps3mediaserver/issues/detail?id=1092#c1
	 * @return True if "ac3_fixed" should be used. 
	 */
	public boolean isMencoderAc3Fixed() {
		return configuration.getBoolean(KEY_MENCODER_AC3_FIXED, false);
	}

	/**
	 * Returns the margin used for ASS subtitling. Default value is 10.
	 * @return The ASS margin.
	 */
	public String getMencoderAssMargin() {
		return getString(KEY_MENCODER_ASS_MARGIN, "10");
	}

	/**
	 * Returns the outline parameter used for ASS subtitling. Default value is 1.
	 * @return The ASS outline parameter.
	 */
	public String getMencoderAssOutline() {
		return getString(KEY_MENCODER_ASS_OUTLINE, "1");
	}

	/**
	 * Returns the shadow parameter used for ASS subtitling. Default value is 1.
	 * @return The ASS shadow parameter.
	 */
	public String getMencoderAssShadow() {
		return getString(KEY_MENCODER_ASS_SHADOW, "1");
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
	public void setMencoderAssMargin(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_MARGIN, value);
	}

	/**
	 * Set the outline parameter used for ASS subtitling.
	 * @param value The ASS outline parameter value to set.
	 */
	public void setMencoderAssOutline(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_OUTLINE, value);
	}

	/**
	 * Set the shadow parameter used for ASS subtitling.
	 * @param value The ASS shadow parameter value to set.
	 */
	public void setMencoderAssShadow(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_SHADOW, value);
	}

	/**
	 * Set the font scale used for ASS subtitling.
	 * @param value The ASS font scale value to set.
	 */
	public void setMencoderAssScale(String value) {
		configuration.setProperty(KEY_MENCODER_ASS_SCALE, value);
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
	 * Older versions of MEncoder do not support ASS/SSA subtitles on all
	 * platforms. Returns true if MEncoder supports them. Default is true
	 * on Windows and OS X, false otherwise.
	 * See https://code.google.com/p/ps3mediaserver/issues/detail?id=1097
	 * @return True if MEncoder supports ASS/SSA subtitles.
	 */
	public boolean isMencoderAss() {
		return getBoolean(KEY_MENCODER_ASS, Platform.isWindows() || Platform.isMac());
	}

	/**
	 * Returns whether or not subtitles should be disabled when using MEncoder
	 * as transcoding engine. Default is false, meaning subtitles should not
	 * be disabled.
	 * @return True if subtitles should be disabled, false otherwise.
	 */
	public boolean isMencoderDisableSubs() {
		return getBoolean(KEY_MENCODER_DISABLE_SUBS, false);
	}

	/**
	 * Returns whether or not the Pulse Code Modulation audio format should be
	 * forced when using MEncoder as transcoding engine. The default is false.
	 * @return True if PCM should be forced, false otherwise.
	 */
	public boolean isMencoderUsePcm() {
		return getBoolean(KEY_MENCODER_USE_PCM, false);
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
	 * Returns the name of a TrueType font to use for MEncoder subtitles.
	 * Default is <code>""</code>.
	 * @return The font name.
	 */
	public String getMencoderFont() {
		return getString(KEY_MENCODER_FONT, "");
	}

	/**
	 * Returns the audio language priority for MEncoder as a comma separated
	 * string. For example: <code>"eng,fre,jpn,ger,und"</code>, where "und"
	 * stands for "undefined". Default value is "loc,eng,fre,jpn,ger,und".
	 *
	 * @return The audio language priority string.
	 */
	public String getMencoderAudioLanguages() {
		return getString(KEY_MENCODER_AUDIO_LANGS, getDefaultLanguages());
	}

	/**
	 * Returns a string of comma separated audio or subtitle languages,
	 * ordered by priority.
	 * @return The string of languages.
	 */
	private String getDefaultLanguages() {
		if ("fr".equals(getLanguage())) {
			return "fre,jpn,ger,eng,und";
		} else {
			return "eng,fre,jpn,ger,und";
		}
	}

	/**
	 * Returns the subtitle language priority for MEncoder as a comma separated
	 * string. For example: <code>"loc,eng,fre,jpn,ger,und"</code>, where "loc"
	 * stands for the preferred local language and "und" stands for "undefined".
	 * Default value is "loc,eng,fre,jpn,ger,und".
	 *
	 * @return The subtitle language priority string.
	 */
	public String getMencoderSubLanguages() {
		return getString(KEY_MENCODER_SUB_LANGS, getDefaultLanguages());
	}

	/**
	 * Returns the ISO 639 language code for the subtitle language that should
	 * be forced upon MEncoder. 
	 * @return The subtitle language code.
	 */
	public String getMencoderForcedSubLanguage() {
		return getString(KEY_MENCODER_FORCED_SUB_LANG, getLanguage());
	}

	/**
	 * Returns the tag string that identifies the subtitle language that
	 * should be forced upon MEncoder.
	 * @return The tag string.
	 */
	public String getMencoderForcedSubTags() {
  		return getString(KEY_MENCODER_FORCED_SUB_TAGS, "forced");
  	}

	/**
	 * Returns a string of audio language and subtitle language pairs
	 * ordered by priority for MEncoder to try to match. Audio language
	 * and subtitle language should be comma separated as a pair,
	 * individual pairs should be semicolon separated. "*" can be used to
	 * match any language, "loc" to match the local language. Subtitle
	 * language can be defined as "off".
	 * Default value is <code>"loc,off;jpn,loc;*,loc;*,*"</code>.
	 *
	 * @return The audio and subtitle languages priority string.
	 */
	public String getMencoderAudioSubLanguages() {
		return getString(KEY_MENCODER_AUDIO_SUB_LANGS, "");
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
	 * Returns the character encoding (or code page) that MEncoder should use
	 * for displaying subtitles. Default is "cp1252".
	 * @return The character encoding.
	 */
	public String getMencoderSubCp() {
		return getString(KEY_MENCODER_SUB_CP, "cp1252");
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
	 * 			otherwise.
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
	 * Sets the audio language priority for MEncoder as a comma separated
	 * string. For example: <code>"eng,fre,jpn,ger,und"</code>, where "und"
	 * stands for "undefined".
	 * @param value The audio language priority string.
	 */
	public void setMencoderAudioLanguages(String value) {
		configuration.setProperty(KEY_MENCODER_AUDIO_LANGS, value);
	}

	/**
	 * Sets the subtitle language priority for MEncoder as a comma
	 * separated string. For example: <code>"eng,fre,jpn,ger,und"</code>,
	 * where "und" stands for "undefined".
	 * @param value The subtitle language priority string.
	 */
	public void setMencoderSubLanguages(String value) {
		configuration.setProperty(KEY_MENCODER_SUB_LANGS, value);
	}

	/**
	 * Sets the ISO 639 language code for the subtitle language that should
	 * be forced upon MEncoder. 
	 * @param value The subtitle language code.
	 */
	public void setMencoderForcedSubLanguage(String value) {
		configuration.setProperty(KEY_MENCODER_FORCED_SUB_LANG, value);
	}

	/**
	 * Sets the tag string that identifies the subtitle language that
	 * should be forced upon MEncoder.
	 * @param value The tag string.
	 */
	public void setMencoderForcedSubTags(String value) {
		configuration.setProperty(KEY_MENCODER_FORCED_SUB_TAGS, value);
	}

	/**
	 * Sets a string of audio language and subtitle language pairs
	 * ordered by priority for MEncoder to try to match. Audio language
	 * and subtitle language should be comma separated as a pair,
	 * individual pairs should be semicolon separated. "*" can be used to
	 * match any language. Subtitle language can be defined as "off". For
	 * example: <code>"en,off;jpn,eng;*,eng;*;*"</code>.
	 * @param value The audio and subtitle languages priority string.
	 */
	public void setMencoderAudioSubLanguages(String value) {
		configuration.setProperty(KEY_MENCODER_AUDIO_SUB_LANGS, value);
	}

	/**
	 * @deprecated Use {@link #getMencoderCustomOptions()} instead.
	 * <p>
	 * Returns custom commandline options to pass on to MEncoder.
	 * @return The custom options string.
	 */
	@Deprecated
	public String getMencoderDecode() {
		return getMencoderCustomOptions();
	}

	/**
	 * Returns custom commandline options to pass on to MEncoder.
	 * @return The custom options string.
	 */
	public String getMencoderCustomOptions() {
		return getString(KEY_MENCODER_CUSTOM_OPTIONS, "");
	}

	/**
	 * @deprecated Use {@link #setMencoderCustomOptions(String)} instead.
	 * <p>
	 * Sets custom commandline options to pass on to MEncoder.
	 * @param value The custom options string.
	 */
	@Deprecated
	public void setMencoderDecode(String value) {
		setMencoderCustomOptions(value);
	}

	/**
	 * Sets custom commandline options to pass on to MEncoder.
	 * @param value The custom options string.
	 */
	public void setMencoderCustomOptions(String value) {
		configuration.setProperty(KEY_MENCODER_CUSTOM_OPTIONS, value);
	}

	/**
	 * Sets the character encoding (or code page) that MEncoder should use
	 * for displaying subtitles. Default is "cp1252".
	 * @param value The character encoding.
	 */
	public void setMencoderSubCp(String value) {
		configuration.setProperty(KEY_MENCODER_SUB_CP, value);
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
	 * Sets the name of a TrueType font to use for MEncoder subtitles.
	 * @param value The font name.
	 */
	public void setMencoderFont(String value) {
		configuration.setProperty(KEY_MENCODER_FONT, value);
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
	 * Set whether or not subtitles should be disabled when using MEncoder
	 * as transcoding engine.
	 * @param value Set to true if subtitles should be disabled.
	 */
	public void setMencoderDisableSubs(boolean value) {
		configuration.setProperty(KEY_MENCODER_DISABLE_SUBS, value);
	}

	/**
	 * Sets whether or not the Pulse Code Modulation audio format should be
	 * forced when using MEncoder as transcoding engine.
	 * @param value Set to true if PCM should be forced.
	 */
	public void setMencoderUsePcm(boolean value) {
		configuration.setProperty(KEY_MENCODER_USE_PCM, value);
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
	 * @see {@link #getMencoderScaleX(int)}, {@link #getMencoderScaleY(int)}
	 */
	public boolean isMencoderScaler() {
		return getBoolean(KEY_MENCODER_SCALER, false);
	}

	/**
	 * Set to true if MEncoder should be used to upscale the video to an
	 * optimal resolution. Set to false to leave upscaling to the renderer.
	 *
	 * @param value Set to true if MEncoder should be used to upscale.
	 * @see {@link #setMencoderScaleX(int)}, {@link #setMencoderScaleY(int)}
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
	 * Returns the number of audio channels that MEncoder should use for
	 * transcoding. Default value is 6 (for 5.1 audio).
	 *
	 * @return The number of audio channels.
	 */
	public int getAudioChannelCount() {
		return getInt(KEY_AUDIO_CHANNEL_COUNT, 6);
	}

	/**
	 * Sets the number of audio channels that MEncoder should use for
	 * transcoding.
	 *
	 * @param value The number of audio channels.
	 */
	public void setAudioChannelCount(int value) {
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
	 * Returns the maximum video bitrate to be used by MEncoder. The default
	 * value is 110.
	 *
	 * @return The maximum video bitrate.
	 */
	public String getMaximumBitrate() {
		return getString(KEY_MAX_BITRATE, "110");
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
	 * @deprecated Use {@link #isThumbnailGenerationEnabled()} instead.
	 * <p>
	 * Returns true if thumbnail generation is enabled, false otherwise.
	 * This only determines whether a thumbnailer (e.g. dcraw, MPlayer)
	 * is used to generate thumbnails. It does not reflect whether
	 * thumbnails should be displayed or not.
	 *
	 * @return boolean indicating whether thumbnail generation is enabled.
	 */
	@Deprecated
	public boolean getThumbnailsEnabled() {
		return isThumbnailGenerationEnabled();
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
	 * @deprecated Use {@link #setThumbnailGenerationEnabled(boolean)} instead.
	 * <p>
	 * Sets the thumbnail generation option.
	 * This only determines whether a thumbnailer (e.g. dcraw, MPlayer)
	 * is used to generate thumbnails. It does not reflect whether
	 * thumbnails should be displayed or not.
	 *
	 * @return boolean indicating whether thumbnail generation is enabled.
	 */
	@Deprecated
	public void setThumbnailsEnabled(boolean value) {
		setThumbnailGenerationEnabled(value);
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
	 * @deprecated This method is not used anywhere.
	 */
	@Deprecated
	public boolean isTurboModeEnabled() {
		return getBoolean(KEY_TURBO_MODE_ENABLED, false);
	}

	/**
	 * @deprecated This method is not used anywhere.
	 */
	@Deprecated
	public void setTurboModeEnabled(boolean value) {
		configuration.setProperty(KEY_TURBO_MODE_ENABLED, value);
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
	 * Returns true when PMS should check for external subtitle files with the
	 * same name as the media (*.srt, *.sub, *.ass, etc.). The default value is
	 * true.
	 *
	 * @return True if PMS should check for external subtitle files, false if
	 * 		they should be ignored.
	 */
	public boolean getUseSubtitles() {
		return getBoolean(KEY_USE_SUBTITLES, true);
	}

	/**
	 * Set to true if PMS should check for external subtitle files with the
	 * same name as the media (*.srt, *.sub, *.ass etc.).
	 *
	 * @param value True if PMS should check for external subtitle files.
	 */
	public void setUseSubtitles(boolean value) {
		configuration.setProperty(KEY_USE_SUBTITLES, value);
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
		return getBoolean(KEY_USE_CACHE, false);
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
	 * Set to true if PMS should pass the flag "convertfps=true" to AviSynth.
	 *
	 * @param value True if PMS should pass the flag.
	 */
	public void setAvisynthConvertFps(boolean value) {
		configuration.setProperty(KEY_AVISYNTH_CONVERT_FPS, value);
	}

	/**
	 * Returns true if PMS should pass the flag "convertfps=true" to AviSynth.
	 *
	 * @return True if PMS should pass the flag.
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
	public String getCodecSpecificConfig() {
		return getString(KEY_CODEC_SPEC_SCRIPT, "");
	}

	/**
	 * Sets additional codec specific configuration options for MEncoder.
	 *
	 * @param value The additional configuration options.
	 */
	public void setCodecSpecificConfig(String value) {
		configuration.setProperty(KEY_CODEC_SPEC_SCRIPT, value);
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

	public boolean isFileBuffer() {
		String bufferType = getString(KEY_BUFFER_TYPE, "").trim();
		return bufferType.equals(BUFFER_TYPE_FILE);
	}

	public void setFfmpegSettings(String value) {
		configuration.setProperty(KEY_FFMPEG_SETTINGS, value);
	}

	public String getFfmpegSettings() {
		return getString(KEY_FFMPEG_SETTINGS, "-g 1 -q:v 1 -qmin 2");
	}

	public boolean isMencoderNoOutOfSync() {
		return getBoolean(KEY_MENCODER_NO_OUT_OF_SYNC, true);
	}

	public void setMencoderNoOutOfSync(boolean value) {
		configuration.setProperty(KEY_MENCODER_NO_OUT_OF_SYNC, value);
	}

	public boolean getTrancodeBlocksMultipleConnections() {
		return configuration.getBoolean(KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS, false);
	}

	public void setTranscodeBlocksMultipleConnections(boolean value) {
		configuration.setProperty(KEY_TRANSCODE_BLOCKS_MULTIPLE_CONNECTIONS, value);
	}

	public boolean getTrancodeKeepFirstConnections() {
		return configuration.getBoolean(KEY_TRANSCODE_KEEP_FIRST_CONNECTION, true);
	}

	public void setTrancodeKeepFirstConnections(boolean value) {
		configuration.setProperty(KEY_TRANSCODE_KEEP_FIRST_CONNECTION, value);
	}

	public String getCharsetEncoding() {
		return getString(KEY_CHARSET_ENCODING, "850");
	}

	public void setCharsetEncoding(String value) {
		configuration.setProperty(KEY_CHARSET_ENCODING, value);
	}

	public boolean isMencoderIntelligentSync() {
		return getBoolean(KEY_MENCODER_INTELLIGENT_SYNC, true);
	}

	public void setMencoderIntelligentSync(boolean value) {
		configuration.setProperty(KEY_MENCODER_INTELLIGENT_SYNC, value);
	}

	public String getFfmpegAlternativePath() {
		return getString(KEY_FFMPEG_ALTERNATIVE_PATH, null);
	}

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
	 * Parallels: "tap,vmnet,vnic".
	 * @return The string of network interface names to skip.
	 */
	public List<String> getSkipNetworkInterfaces() {
		return getStringList(KEY_SKIP_NETWORK_INTERFACES, "tap,vmnet,vnic");
	}
	
	public void setSkipLoopFilterEnabled(boolean value) {
		configuration.setProperty(KEY_SKIP_LOOP_FILTER_ENABLED, value);
	}

	public String getMencoderMainSettings() {
		return getString(KEY_MENCODER_MAIN_SETTINGS, "keyint=5:vqscale=1:vqmin=2");
	}

	public void setMencoderMainSettings(String value) {
		configuration.setProperty(KEY_MENCODER_MAIN_SETTINGS, value);
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

	public List<String> getEnginesAsList(SystemUtils registry) {
		List<String> engines = stringToList(getString(KEY_ENGINES, "mencoder,avsmencoder,tsmuxer,ffmpegvideo,ffmpegaudio,mplayeraudio,tsmuxeraudio,vlcvideo,mencoderwebvideo,mplayervideodump,mplayerwebaudio,vlcaudio,ffmpegdvrmsremux,rawthumbs"));
		engines = hackAvs(registry, engines);
		return engines;
	}

	private static String listToString(List<String> enginesAsList) {
		return StringUtils.join(enginesAsList, LIST_SEPARATOR);
	}

	private static List<String> stringToList(String input) {
		List<String> output = new ArrayList<String>();
		Collections.addAll(output, StringUtils.split(input, LIST_SEPARATOR));
		return output;
	}
	// TODO: Get this out of here
	private static boolean avsHackLogged = false;

	// TODO: Get this out of here
	private static List<String> hackAvs(SystemUtils registry, List<String> input) {
		List<String> toBeRemoved = new ArrayList<String>();
		for (String engineId : input) {
			if (engineId.startsWith("avs") && !registry.isAvis() && Platform.isWindows()) {
				if (!avsHackLogged) {
					LOGGER.info("AviSynth is not installed. You cannot use " + engineId + " as a transcoding engine.");
					avsHackLogged = true;
				}
				toBeRemoved.add(engineId);
			}
		}
		List<String> output = new ArrayList<String>();
		output.addAll(input);
		output.removeAll(toBeRemoved);
		return output;
	}

	public void save() throws ConfigurationException {
		configuration.save();
		LOGGER.info("Configuration saved to: " + PROFILE_PATH);
	}

	public String getFolders() {
		return getString(KEY_FOLDERS, "");
	}

	public void setFolders(String value) {
		configuration.setProperty(KEY_FOLDERS, value);
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

	public String getNoTranscode() {
		return getString(KEY_NOTRANSCODE, "");
	}

	public void setNoTranscode(String value) {
		configuration.setProperty(KEY_NOTRANSCODE, value);
	}

	public String getForceTranscode() {
		return getString(KEY_FORCETRANSCODE, "");
	}

	public void setForceTranscode(String value) {
		configuration.setProperty(KEY_FORCETRANSCODE, value);
	}

	public void setMencoderMT(boolean value) {
		configuration.setProperty(KEY_MENCODER_MT, value);
	}

	public boolean getMencoderMT() {
		boolean isMultiCore = getNumberOfCpuCores() > 1;
		return getBoolean(KEY_MENCODER_MT, isMultiCore);
	}

	public void setRemuxAC3(boolean value) {
		configuration.setProperty(KEY_MENCODER_REMUX_AC3, value);
	}

	public boolean isRemuxAC3() {
		return getBoolean(KEY_MENCODER_REMUX_AC3, true);
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

	public void setMencoderAssDefaultStyle(boolean value) {
		configuration.setProperty(KEY_MENCODER_ASS_DEFAULTSTYLE, value);
	}

	public boolean isMencoderAssDefaultStyle() {
		return getBoolean(KEY_MENCODER_ASS_DEFAULTSTYLE, true);
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
	 * </ul>
	 * Default value is 3.
	 * @return The sort method
	 */
	public int getSortMethod() {
		return getInt(KEY_SORT_METHOD, 3);
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

	public String getAlternateSubsFolder() {
		return getString(KEY_ALTERNATE_SUBS_FOLDER, "");
	}

	public void setAlternateSubsFolder(String value) {
		configuration.setProperty(KEY_ALTERNATE_SUBS_FOLDER, value);
	}

	public void setDTSEmbedInPCM(boolean value) {
		configuration.setProperty(KEY_EMBED_DTS_IN_PCM, value);
	}

	public boolean isDTSEmbedInPCM() {
		return getBoolean(KEY_EMBED_DTS_IN_PCM, false);
	}

	public void setMencoderMuxWhenCompatible(boolean value) {
		configuration.setProperty(KEY_MENCODER_MUX_COMPATIBLE, value);
	}

	public boolean isMencoderMuxWhenCompatible() {
		return getBoolean(KEY_MENCODER_MUX_COMPATIBLE, true);
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

	public boolean getIphotoEnabled() {
		return getBoolean(KEY_IPHOTO_ENABLED, false);
	}

	public void setIphotoEnabled(boolean value) {
		configuration.setProperty(KEY_IPHOTO_ENABLED, value);
	}
	
	public boolean getApertureEnabled() {
		return getBoolean(KEY_APERTURE_ENABLED, false);
	}

	public void setApertureEnabled(boolean value) {
		configuration.setProperty(KEY_APERTURE_ENABLED, value);
	}

	public boolean getItunesEnabled() {
		return getBoolean(KEY_ITUNES_ENABLED, false);
	}

	public void setItunesEnabled(boolean value) {
		configuration.setProperty(KEY_ITUNES_ENABLED, value);
	}

	public boolean isHideEmptyFolders() {
		return getBoolean(PmsConfiguration.KEY_HIDE_EMPTY_FOLDERS, false);
	}

	public void setHideEmptyFolders(final boolean value) {
		this.configuration.setProperty(PmsConfiguration.KEY_HIDE_EMPTY_FOLDERS, value);
	}

	public boolean isHideMediaLibraryFolder() {
		return getBoolean(PmsConfiguration.KEY_HIDE_MEDIA_LIBRARY_FOLDER, false);
	}

	public void setHideMediaLibraryFolder(final boolean value) {
		this.configuration.setProperty(PmsConfiguration.KEY_HIDE_MEDIA_LIBRARY_FOLDER, value);
	}

	public boolean getHideTranscodeEnabled() {
		return getBoolean(KEY_HIDE_TRANSCODE_FOLDER, false);
	}

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
		return configuration.getProperty(property);
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

	/**
	 * Returns the name of the renderer to fall back on when header matching
	 * fails. PMS will recognize the configured renderer instead of "Unknown
	 * renderer". Default value is "", which means PMS will return the unknown
	 * renderer when no match can be made.
	 *
	 * @return The name of the renderer PMS should fall back on when header
	 * 			matching fails.
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
	 * 				<code>""</code> or a case insensitive match with the name
	 * 				used in any render configuration file.
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
	 * @param value Set to true when the fallback renderer should always be
	 *				picked.
	 * @see #setRendererDefault(String)
	 */
	public void setRendererForceDefault(boolean value) {
		configuration.setProperty(KEY_RENDERER_FORCE_DEFAULT, value);
	}

	public String getVirtualFolders() {
		return getString(KEY_VIRTUAL_FOLDERS, "");
	}

	public String getProfilePath() {
		return PROFILE_PATH;
	}

	public String getProfileDirectory() {
		return PROFILE_DIRECTORY;
	}

	public String getPluginDirectory() {
		return getString(KEY_PLUGIN_DIRECTORY, "plugins");
	}

	public void setPluginDirectory(String value) {
		configuration.setProperty(KEY_PLUGIN_DIRECTORY, value);
	}

	public String getProfileName() {
		if (HOSTNAME == null) { // calculate this lazily
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
		return Build.isUpdatable() && configuration.getBoolean(KEY_AUTO_UPDATE, false);
	}

	public void setAutoUpdate(boolean value) {
		configuration.setProperty(KEY_AUTO_UPDATE, value);
	}

	public String getIMConvertPath() {
		return programPaths.getIMConvertPath();
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
	
	public boolean initBufferMax() {
		return getBoolean(KEY_BUFFER_MAX, false);
	}
	
	public String getPluginPurgeAction() {
		return getString(KEY_PLUGIN_PURGE_ACTION, "delete");
	}
	
	public boolean getSearchFolder() {
		return getBoolean(KEY_SEARCH_FOLDER, false);
	}
	
	public int getSearchRecurse() {
		if (getBoolean(KEY_SEARCH_RECURSE, true)) {
			return 100;
		}
		else {
			return 0;
		}
	}
	
	public void reload() {
		try {
			configuration.refresh();
		} catch (ConfigurationException e) {
		}
	}
}
