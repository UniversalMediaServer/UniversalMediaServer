package net.pms.configuration;

import java.net.InetAddress;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.Player;
import net.pms.formats.*;
import net.pms.io.OutputParams;
import net.pms.remote.RemoteUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebRender extends DeviceConfiguration implements RendererConfiguration.OutputOverride {
	private String name;
	private String ip;
	private int port;
	private String ua;
	private String defaultMime;
	private int browser = 0;
	private String platform = null;
	private int screenWidth = 0;
	private int screenHeight = 0;
	private boolean isTouchDevice = false;
	private static final PmsConfiguration pmsconfiguration = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(WebRender.class);
	private static final Format[] supportedFormats ={
			new GIF(),
			new JPG(),
			new MP3(),
			new PNG()
	};

	public static final String umsInfoScript = StringUtils.join(new String[] {
		"<script>",
		"if ((' '+document.cookie).indexOf(' UMSINFO=')==-1) {",
			"var isTouchDevice = window.screenX == 0 && ('ontouchstart' in window || 'onmsgesturechange' in window);",
			"document.cookie='UMSINFO=platform='+navigator.platform+'&width='+screen.width+'&height='+screen.height+'&isTouchDevice='+isTouchDevice+';Path=/';",
		"}",
		"</script>"}, "\r\n");
	private static final Matcher umsInfo = Pattern.compile("platform=(.+)&width=(.+)&height=(.+)&isTouchDevice=(.+)").matcher("");

	protected static final int CHROME = 1;
	protected static final int MSIE = 2;
	protected static final int FIREFOX = 3;
	protected static final int SAFARI = 4;
	protected static final int PS4 = 5;
	protected static final int XBOX1 = 6;

	public WebRender(String name) throws ConfigurationException {
		super(NOFILE, null);
		this.name = name;
		ip = "";
		port = 0;
		ua = "";
		fileless = true;
		String userFmt = pmsconfiguration.getWebTranscode();
		defaultMime = userFmt != null ? ("video/" + userFmt) : RemoteUtil.transMime();
	}

	@Override
	public boolean load(File f) {
		// FIXME: These are just preliminary
		configuration.addProperty(MEDIAPARSERV2, true);
		configuration.addProperty(MEDIAPARSERV2_THUMB, true);
		configuration.addProperty(SUPPORTED, "f:flv v:h264|hls a:aac m:video/flash");
		configuration.addProperty(SUPPORTED, "f:mp4 m:video/mp4");
		configuration.addProperty(SUPPORTED, "f:mp3 n:2 m:audio/mpeg");
//		configuration.addProperty(SUPPORTED, "f:wav n:2 m:audio/wav");
		configuration.addProperty(TRANSCODE_AUDIO, MP3);
		return true;
	}

	@Override
	public boolean associateIP(InetAddress sa) {
		ip = sa.getHostAddress();
		return super.associateIP(sa);
	}

	public void associatePort(int port) {
		this.port = port;
	}

	public void setUA(String ua) {
		this.ua = ua.toLowerCase();
	}

	public void setBrowserInfo(String info, String userAgent) {
		setUA(userAgent);

		if (ua.contains("chrome")) {
			browser = CHROME;
		} else  if (ua.contains("msie")) {
			browser = MSIE;
		} else if (ua.contains("firefox")) {
			browser = FIREFOX;
		} else if (ua.contains("safari")) {
			browser = SAFARI;
		} else if (ua.contains("playstation 4")) {
			browser = PS4;
		} else if (ua.contains("xbox one")) {
			browser = XBOX1;
		}
		if (info != null && umsInfo.reset(info).find()) {
			platform = umsInfo.group(1).toLowerCase();
			screenWidth = Integer.valueOf(umsInfo.group(2));
			screenHeight = Integer.valueOf(umsInfo.group(3));
			isTouchDevice = Boolean.valueOf(umsInfo.group(4));

			LOGGER.debug("Setting {} browser info: plaform:{}, screen:{}x{}, isTouchDevice:{}",
				getRendererName(), platform, screenWidth, screenHeight, isTouchDevice);
		}
	}

	@Override
	public String getRendererName() {
		String s = pmsconfiguration.isWebAuthenticate() ? "@" : "";
		switch (browser) {
			case CHROME:  return s + "Chrome";
			case MSIE:    return s + "Internet Explorer";
			case FIREFOX: return s + "Firefox";
			case SAFARI:  return s + "Safari";
			case PS4:     return s + "Playstation 4";
			case XBOX1:   return s + "Xbox One";
			default:      return s + Messages.getString("PMS.142");
		}
	}

	@Override
	public String getRendererIcon() {
		switch (browser) {
			case CHROME:  return "chrome.png";
			case MSIE:    return "internetexplorer.png";
			case FIREFOX: return "firefox.png";
//			case SAFARI:  return "safari.png"; // TODO
			case PS4:     return "ps4.png";
			case XBOX1:   return "xbox-one.png";
			default:      return super.getRendererIcon();
		}
	}

	@Override
	public String toString() {
		return getRendererName();
	}

	@Override
	public boolean isMediaParserV2ThumbnailGeneration() {
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
		return browser == FIREFOX && platform.contains("linux") && pmsconfiguration.getWebFirefoxLinuxMp4();
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
			return RemoteUtil.MIME_WEBM;
		} else if (isFirefoxLinuxMp4()) {
			return RemoteUtil.MIME_MP4;
		}
		return defaultMime;
	}

	public boolean isLowBitrate() {
		// FIXME: this should return true if either network speed or client cpu are slow
		return screenWidth < 720 && (ua.contains("mobi") || isTouchDevice);
	}

	@Override
	public boolean getOutputOptions(List<String> cmdList, DLNAResource dlna, Player player, OutputParams params) {
		if (player instanceof FFMpegVideo) {
			if (dlna.getFormat().isVideo()) {
				DLNAMediaInfo media = dlna.getMedia();
				boolean flash = media != null && "video/flash".equals(media.getMimeType());
				if (flash) {
					fflashCmds(cmdList, media);
				} else {
					String mime = getVideoMimeType();
					if (mime.equals(RemoteUtil.MIME_OGG)) {
						ffoggCmd(cmdList);
					} else if (mime.equals(RemoteUtil.MIME_MP4)) {
						ffmp4Cmd(cmdList);
					} else if (mime.equals(RemoteUtil.MIME_WEBM)) {
						if (isChromeTrick()) {
							chromeCmd(cmdList);
						} else {
							// nothing here yet
						}
					}
				}
				if (isLowBitrate()) {
					cmdList.addAll(((FFMpegVideo) player).getVideoBitrateOptions(dlna, media, params));
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

	private void fflashCmds(List<String> cmdList, DLNAMediaInfo media) {
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

	private void ffoggCmd(List<String> cmdList) {
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

	private void ffmp4Cmd(List<String> cmdList) {
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
		cmdList.add("-strict");
		cmdList.add("experimental");
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

	private void chromeCmd(List<String> cmdList)  {
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

	private void ffhlsCmd(List<String> cmdList, DLNAMediaInfo media) {
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


	public static boolean supportedFormat(Format f) {
		for(Format f1 : supportedFormats) {
			if(f.getIdentifier() == f1.getIdentifier()) {
				return true;
			}
		}
		return false;
	}

	public static boolean supports(DLNAResource dlna) {
		DLNAMediaInfo m = dlna.getMedia();
		return (m != null && RemoteUtil.directmime(m.getMimeType())) ||
				(supportedFormat(dlna.getFormat())) ||
			(dlna.getPlayer() instanceof FFMpegVideo);
	}

	@Override
	public String getFFmpegVideoFilterOverride() {
		return "scale=" + getVideoWidth() + ":" + getVideoHeight();
	}

	public boolean isTranscodeToMPEGTSH264AC3() {
		return true;
	}

	public boolean isTranscodeToMPEGTSH264AAC() {
		return true;
	}

	public boolean nox264() {
		return true;
	}
}
