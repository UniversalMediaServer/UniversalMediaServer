/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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

package net.pms.configuration;

import com.google.gson.Gson;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.Player;
import net.pms.external.StartStopListenerDelegate;
import net.pms.formats.*;
import net.pms.formats.audio.MP3;
import net.pms.formats.image.BMP;
import net.pms.formats.image.GIF;
import net.pms.formats.image.JPG;
import net.pms.formats.image.PNG;
import net.pms.image.ImageFormat;
import net.pms.io.OutputParams;
import net.pms.network.HTTPResource;
import net.pms.remote.RemoteUtil;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebRender extends DeviceConfiguration implements RendererConfiguration.OutputOverride {
	private String user;
	private String ip;
	@SuppressWarnings("unused")
	private int port;
	private String ua;
	private String defaultMime;
	private int browser = 0;
	private String platform = null;
	private int screenWidth = 0;
	private int screenHeight = 0;
	private boolean isTouchDevice = false;
	private String subLang;
	private Gson gson;
	private static final PmsConfiguration pmsconfiguration = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(WebRender.class);
	private static final Format[] supportedFormats = {
		new GIF(),
		new JPG(),
		new MP3(),
		new PNG(),
		new BMP()
	};

	private static final Matcher umsInfo = Pattern.compile("platform=(.+)&width=(.+)&height=(.+)&isTouchDevice=(.+)").matcher("");

	protected static final int CHROME = 1;
	protected static final int MSIE = 2;
	protected static final int FIREFOX = 3;
	protected static final int SAFARI = 4;
	protected static final int PS4 = 5;
	protected static final int XBOX1 = 6;
	protected static final int OPERA = 7;
	protected static final int EDGE = 8;
	protected static final int CHROMIUM = 9;
	protected static final int VIVALDI = 10;

	private StartStopListenerDelegate startStop;

	public WebRender(String user) throws ConfigurationException {
		super(NOFILE, null);
		this.user = user;
		ip = "";
		port = 0;
		ua = "";
		fileless = true;
		String userFmt = pmsconfiguration.getWebTranscode();
		defaultMime = userFmt != null ? ("video/" + userFmt) : RemoteUtil.transMime();
		startStop = null;
		subLang = "";
		if (pmsConfiguration.useWebControl()) {
			controls = BasicPlayer.PLAYCONTROL|BasicPlayer.VOLUMECONTROL;
		}
		gson = new Gson();
		push = new ArrayList<>();
	}

	@Override
	public boolean load(File f) {
		// FIXME: These are just preliminary
		configuration.addProperty(MEDIAPARSERV2, true);
		configuration.addProperty(MEDIAPARSERV2_THUMB, true);
		configuration.addProperty(SUPPORTED, "f:flv v:h264|hls a:aac-lc m:video/flash");
		configuration.addProperty(SUPPORTED, "f:mp4 m:video/mp4");
		configuration.addProperty(SUPPORTED, "f:mp3 n:2 m:audio/mpeg");
		configuration.addProperty(SUPPORTED, "f:ogg v:theora m:video/ogg");
		configuration.addProperty(SUPPORTED, "f:oga a:vorbis|flac m:audio/ogg");
		configuration.addProperty(SUPPORTED, "f:wav n:2 m:audio/wav");
		configuration.addProperty(SUPPORTED, "f:webm v:vp8|vp9 m:video/webm");
		configuration.addProperty(SUPPORTED, "f:bmp m:image/bmp");
		configuration.addProperty(SUPPORTED, "f:jpg m:image/jpeg");
		configuration.addProperty(SUPPORTED, "f:png m:image/png");
		configuration.addProperty(SUPPORTED, "f:gif m:image/gif");
		configuration.addProperty(SUPPORTED, "f:tiff m:image/tiff");
		configuration.addProperty(TRANSCODE_AUDIO, MP3);
		return true;
	}

	@Override
	public boolean associateIP(InetAddress sa) {
		ip = sa.getHostAddress();
		return super.associateIP(sa);
	}

	@Override
	public InetAddress getAddress() {
		try {
			return InetAddress.getByName(ip);
		} catch (Exception e) {
			return null;
		}
	}

	public void associatePort(int port) {
		this.port = port;
	}

	public void setUA(String ua) {
		LOGGER.debug("Setting web client ua: {}", ua);
		this.ua = ua.toLowerCase();
	}

	public static String getBrowserName(int browser) {
		switch (browser) {
			case CHROME:  return "Chrome";
			case MSIE:    return "Internet Explorer";
			case FIREFOX: return "Firefox";
			case SAFARI:  return "Safari";
			case PS4:     return "Playstation 4";
			case XBOX1:   return "Xbox One";
			case OPERA:   return "Opera";
			case EDGE:    return "Edge";
			case CHROMIUM:return "Chromium";
			case VIVALDI: return "Vivaldi";
			default:      return Messages.getString("PMS.142");
		}
	}

	public static int getBrowser(String userAgent) {
		String ua = userAgent.toLowerCase();
		return
			ua.contains("edge")          ? EDGE :
			ua.contains("chrome")        ? CHROME :
			(ua.contains("msie") ||
			ua.contains("trident"))      ? MSIE :
			ua.contains("firefox")       ? FIREFOX :
			ua.contains("safari")        ? SAFARI :
			ua.contains("playstation 4") ? PS4 :
			ua.contains("xbox one")      ? XBOX1 :
			ua.contains("opera")         ? OPERA :
			ua.contains("chromium")      ? CHROMIUM :
			ua.contains("vivaldi")       ? VIVALDI :
			0;
	}

	public void setBrowserInfo(String info, String userAgent) {
		setUA(userAgent);
		browser = getBrowser(userAgent);

		if (info != null && umsInfo.reset(info).find()) {
			platform = umsInfo.group(1).toLowerCase();
			screenWidth = Integer.valueOf(umsInfo.group(2));
			screenHeight = Integer.valueOf(umsInfo.group(3));
			isTouchDevice = Boolean.valueOf(umsInfo.group(4));

			LOGGER.debug("Setting {} browser info: platform:{}, screen:{}x{}, isTouchDevice:{}",
				getRendererName(), platform, screenWidth, screenHeight, isTouchDevice);
		}
		active = true;
		uuid = getConfName() + ":" + ip;
	}

	@Override
	public String getRendererName() {
		return (pmsconfiguration.isWebAuthenticate() ? user + "@" : "") + getBrowserName(browser);
	}

	@Override
	public String getConfName() {
		return getBrowserName(browser);
	}

	public int getBrowser() {
		return browser;
	}

	public String getUser() {
		return user;
	}

	@Override
	public String getRendererIcon() {
		switch (browser) {
			case CHROME:  return "chrome.png";
			case MSIE:    return "internetexplorer.png";
			case FIREFOX: return "firefox.png";
			case SAFARI:  return "safari.png";
			case PS4:     return "ps4.png";
			case XBOX1:   return "xbox-one.png";
			case OPERA:   return "opera.png";
			case EDGE:    return "edge.png";
			case CHROMIUM:return "chromium.png";
			case VIVALDI: return "vivaldi.png";
			default:      return super.getRendererIcon();
		}
	}

	@Override
	public String toString() {
		return getRendererName();
	}

	@Override
	public boolean isMediaInfoThumbnailGeneration() {
		return false;
	}

	@Override
	public boolean isLimitFolders() {
		// no folder limit on the web clients
		return false;
	}

	public boolean isChromeTrick() {
		return browser == CHROME && pmsconfiguration.getWebChrome();
	}

	public boolean isFirefoxLinuxMp4() {
		return browser == FIREFOX && platform != null && platform.contains("linux") && pmsconfiguration.getWebFirefoxLinuxMp4();
	}

	public boolean isScreenSizeConstrained() {
		return (screenWidth != 0 && RemoteUtil.getWidth() > screenWidth) ||
			(screenHeight != 0 && RemoteUtil.getHeight() > screenHeight);
	}

	public int getVideoWidth() {
		return isScreenSizeConstrained() ? screenWidth : RemoteUtil.getWidth();
	}

	public int getVideoHeight() {
		return isScreenSizeConstrained() ? screenHeight : RemoteUtil.getHeight();
	}

	public String getVideoMimeType() {
		if (isChromeTrick()) {
			return HTTPResource.WEBM_TYPEMIME;
		} else if (isFirefoxLinuxMp4()) {
			return HTTPResource.MP4_TYPEMIME;
		}
		return defaultMime;
	}

	@Override
	public int getAutoPlayTmo() {
		return 0;
	}

	@Override
	public boolean isNoDynPlsFolder() {
		return true;
	}

	public boolean isLowBitrate() {
		// FIXME: this should return true if either network speed or client cpu are slow
		boolean slow = false;
		try {
			// note here if we get a low speed then calcspeed
			// will return -1 which will ALWAYS be less that the configed value.
			slow = getInt(calculatedSpeed(), 0) < pmsConfiguration.getWebLowSpeed();
		} catch (Exception e) {
		}
		return slow || (screenWidth < 720 && (ua.contains("mobi") || isTouchDevice));
	}

	@Override
	public boolean getOutputOptions(List<String> cmdList, DLNAResource resource, Player player, OutputParams params) {
		if (player instanceof FFMpegVideo) {
			if (resource.getFormat().isVideo()) {
				DLNAMediaInfo media = resource.getMedia();
				boolean flash = media != null && "video/flash".equals(media.getMimeType());
				if (flash) {
					ffFlashCmds(cmdList, media);
				} else {
					String mimeType = getVideoMimeType();
					switch (mimeType) {
						case HTTPResource.OGG_TYPEMIME:
							ffOggCmd(cmdList);
							break;
						case HTTPResource.MP4_TYPEMIME:
							ffMp4Cmd(cmdList);
							break;
						case HTTPResource.WEBM_TYPEMIME:
							if (isChromeTrick()) {
								ffChromeCmd(cmdList);
							} else {
								// nothing here yet
							}
							break;
					}
				}
				if (isLowBitrate()) {
					cmdList.addAll(((FFMpegVideo) player).getVideoBitrateOptions(resource, media, params));
				}
			} else {
				// nothing here yet
			}
			return true;
//		} else if (player instanceof MEncoderVideo) {
//			// nothing here yet
		}
		return false;
	}

	private static void ffFlashCmds(List<String> cmdList, DLNAMediaInfo media) {
		// Can't streamcopy if filters are present
		boolean canCopy = !(cmdList.contains("-vf") || cmdList.contains("-filter_complex"));
		cmdList.add("-c:v");
		if (canCopy && media != null && media.getCodecV() != null && media.getCodecV().equals("h264")) {
			cmdList.add("copy");
		} else {
			cmdList.add("flv");
			cmdList.add("-qmin");
			cmdList.add("2");
			cmdList.add("-qmax");
			cmdList.add("6");
		}
		if (canCopy && media != null && media.getFirstAudioTrack() != null && media.getFirstAudioTrack().isAAC()) {
			cmdList.add("-c:a");
			cmdList.add("copy");
		} else {
			cmdList.add("-ar");
			cmdList.add("44100");
		}
		cmdList.add("-f");
		cmdList.add("flv");
	}

	private static void ffOggCmd(List<String> cmdList) {
		/*cmdList.add("-c:v");
		cmdList.add("libtheora");*/
		cmdList.add("-qscale:v");
		cmdList.add("10");
		cmdList.add("-acodec");
		cmdList.add("libvorbis");
		/*cmdList.add("-qscale:a");
		cmdList.add("6");*/
		/*cmdList.add("-bufsize");
		cmdList.add("300k");
		cmdList.add("-b:a");
		cmdList.add("128k");*/
		cmdList.add("-f");
		cmdList.add("ogg");
	}

	private static void ffMp4Cmd(List<String> cmdList) {
		// see http://stackoverflow.com/questions/8616855/how-to-output-fragmented-mp4-with-ffmpeg
		cmdList.add(1, "-re");
		cmdList.add("-g");
		cmdList.add("52"); // see https://code.google.com/p/stream-m/#FRAGMENT_SIZES

		cmdList.add("-c:v");
		cmdList.add("libx264");
		cmdList.add("-preset");
		cmdList.add("ultrafast");
		/*cmdList.add("-tune");
		cmdList.add("zerolatency");
		cmdList.add("-profile:v");
		cmdList.add("high");
		cmdList.add("-level:v");
		cmdList.add("3.1");*/
		cmdList.add("-c:a");
		cmdList.add("aac");
		cmdList.add("-ab");
		cmdList.add("16k");
//		cmdList.add("-ar");
//		cmdList.add("44100");
		/*cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");*/
//		cmdList.add("-frag_duration");
//		cmdList.add("300");
//		cmdList.add("-frag_size");
//		cmdList.add("100");
//		cmdList.add("-flags");
//		cmdList.add("+aic+mv4");
		cmdList.add("-movflags");
		cmdList.add("frag_keyframe+empty_moov");
		cmdList.add("-f");
		cmdList.add("mp4");
	}

	private static void ffChromeCmd(List<String> cmdList) {
		//-c:v libx264 -profile:v high -level 4.1 -map 0:a -c:a libmp3lame -ac 2 -preset ultrafast -b:v 35000k -bufsize 35000k -f matroska
		cmdList.add("-c:v");
		cmdList.add("libx264");
		cmdList.add("-profile:v");
		cmdList.add("high");
		cmdList.add("-level:v");
		cmdList.add("3.1");
		cmdList.add("-c:a");
		cmdList.add("libmp3lame");
		cmdList.add("-ac");
		cmdList.add("2");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-preset");
		cmdList.add("ultrafast");
		cmdList.add("-f");
		cmdList.add("matroska");
	}

	@SuppressWarnings("unused")
	private static void ffhlsCmd(List<String> cmdList, DLNAMediaInfo media) {
		// Can't streamcopy if filters are present
		boolean canCopy = !(cmdList.contains("-vf") || cmdList.contains("-filter_complex"));
		cmdList.add("-c:v");
		if (canCopy && media != null && media.getCodecV() != null && media.getCodecV().equals("h264")) {
			cmdList.add("copy");
		} else {
			cmdList.add("flv");
			cmdList.add("-qmin");
			cmdList.add("2");
			cmdList.add("-qmax");
			cmdList.add("6");
		}
		if (canCopy && media != null && media.getFirstAudioTrack() != null && media.getFirstAudioTrack().isAAC()) {
			cmdList.add("-c:a");
			cmdList.add("copy");
		} else {
			cmdList.add("-ar");
			cmdList.add("44100");
		}
		cmdList.add("-f");
		cmdList.add("HLS");
	}

	public boolean isImageFormatSupported(ImageFormat format) {
		if (format == null) {
			return false;
		}
		if (format == ImageFormat.GIF || format == ImageFormat.JPEG || format == ImageFormat.PNG) {
			return true;
		}
		switch (format) {
			case BMP:
				return
					browser == FIREFOX || browser == CHROME ||
					browser == CHROMIUM || browser == OPERA ||
					browser == MSIE || browser == EDGE || browser == SAFARI;
			case TIFF:
				return browser == EDGE || browser == CHROMIUM || browser == SAFARI || browser == MSIE;
			case WEBP:
				return browser == CHROME || browser == CHROMIUM || browser == OPERA;
			default:
				return false;
		}
	}

	public static boolean supportedFormat(Format f) {
		for (Format f1 : supportedFormats) {
			if (f.getIdentifier() == f1.getIdentifier() || f1.mimeType().equals(f.mimeType())) {
				return true;
			}
		}
		return false;
	}

	public static boolean supports(DLNAResource dlna) {
		if (dlna instanceof VirtualVideoAction) {
			return true;
		}
		DLNAMediaInfo media = dlna.getMedia();
		return
			media != null && RemoteUtil.directmime(media.getMimeType()) ||
			supportedFormat(dlna.getFormat()) ||
			dlna.getPlayer() instanceof FFMpegVideo;
	}

	@Override
	public String getFFmpegVideoFilterOverride() {
		return "scale=" + getVideoWidth() + ":" + getVideoHeight();
	}

	@Override
	public boolean isTranscodeToMPEGTSH264AC3() {
		return true;
	}

	@Override
	public boolean isTranscodeToMPEGTSH264AAC() {
		return true;
	}

	@Override
	public boolean nox264() {
		return true;
	}

	@Override
	public boolean addSubtitles() {
		return true;
	}

	@Override
	public BasicPlayer getPlayer() {
		if (player == null) {
			player = new WebPlayer(this);
		}
		return player;
	}

	@Override
	public String getSubLanguage() {
		if (!useWebSubLang() || StringUtils.isEmpty(subLang)) {
			return super.getSubLanguage();
		}
		return subLang;
	}

	public void setSubLang(String s) {
		subLang = s;
	}

	private ArrayList<String[]> push;

	public void push(String... args) {
		push.add(args);
	}

	public String getPushData() {
		String json = "";
		if (push.size() > 0) {
			json = gson.toJson(push);
			push.clear();
		}
		return json;
	}

	@Override
	public void notify(String type, String msg) {
		push("notify", type, msg);
	}

	public void start(DLNAResource dlna) {
		if (getPlayingRes() != dlna) {
			stop();
		}
		setPlayingRes(dlna);
		if (startStop == null) {
			startStop = new StartStopListenerDelegate(ip);
		}
		startStop.setRenderer(this);
		startStop.start(getPlayingRes());
	}

	public void stop() {
		if (startStop == null) {
			return;
		}
		startStop.stop();
		startStop = null;
	}

	public static class WebPlayer extends BasicPlayer.Logical {
		private HashMap<String, String> data;
		private Gson gson;

		public WebPlayer(WebRender renderer) {
			super(renderer);
			data = new HashMap<>();
			gson = renderer.gson;
			LOGGER.debug("Created web player for " + renderer.getRendererName());
		}

		@Override
		public void setURI(String uri, String metadata) {
			Playlist.Item item = resolveURI(uri, metadata);
			if (item != null) {
				DLNAResource r = DLNAResource.getValidResource(item.uri, item.name, renderer);
				if (r != null) {
					((WebRender)renderer).push("seturl", "/play/" + r.getId());
					return;
				}
			}
			LOGGER.debug("Bad uri " + uri);
		}

		@Override
		public void pause() {
			((WebRender)renderer).push("control", "pause");
		}

		@Override
		public void play() {
			((WebRender)renderer).push("control", "play");
		}

		@Override
		public void stop() {
			((WebRender)renderer).push("control", "stop");
		}

		@Override
		public void mute() {
			((WebRender)renderer).push("control", "mute");
		}

		@Override
		public void setVolume(int volume) {
			((WebRender)renderer).push("control", "setvolume", "" + volume);
		}

		@Override
		public int getControls() {
			return renderer.pmsConfiguration.useWebControl() ? PLAYCONTROL|VOLUMECONTROL : 0;
		}

		@Override
		public void start() {
			DLNAResource d = renderer.getPlayingRes();
			state.name = d.getDisplayName();
			if (d.getMedia() != null) {
				state.duration = StringUtil.shortTime(d.getMedia().getDurationString(), 4);
			}
		}

		public void setData(String jsonData) {
			data = gson.fromJson(jsonData, data.getClass());
			String s = data.get("playback");
			state.playback = "STOPPED".equals(s) ? STOPPED :
				"PLAYING".equals(s) ? PLAYING :
				"PAUSED".equals(s) ? PAUSED : -1;
			state.mute = "0".equals(data.get("mute")) ? false : true;
			s = data.get("volume");
			try {
				state.volume = StringUtil.hasValue(s) ? Integer.valueOf(s) : 0;
			} catch (NumberFormatException e) {
				LOGGER.debug("Unexpected volume value \"{}\"", data.get("volume"));
			}
			long seconds = 0;
			if (data.get("position") != null) {
				try {
					seconds = Integer.valueOf(data.get("position"));
				} catch (NumberFormatException e) {
					LOGGER.debug("Unexpected position value \"{}\"", data.get("position"));
				}
			}
			state.position = DurationFormatUtils.formatDuration(seconds * 1000, "HH:mm:ss");
			alert();
			if (state.playback == STOPPED) {
				((WebRender)renderer).stop();
			}
		}
	}
}
