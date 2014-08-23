package net.pms.configuration;

import java.net.InetAddress;
import java.io.File;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.Player;
import net.pms.formats.*;
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
	private static final PmsConfiguration pmsconfiguration = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(WebRender.class);
	private static final Format[] supportedFormats ={
			new GIF(),
			new JPG(),
			new MP3(),
			new PNG()
	};

	public WebRender(String name) throws ConfigurationException {
		super(NOFILE, null);
		this.name = name;
		ip = "";
		port = 0;
		ua = "";
		fileless = true;
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
	public String getRendererName() {
		String rendererName = "";
		if (pmsconfiguration.isWebAuthenticate()) {
			rendererName = name + "@";
		}

		if (ua.contains("chrome")) {
			rendererName += "Chrome";
		} else  if (ua.contains("msie")) {
			rendererName += "Internet Explorer";
		} else if (ua.contains("firefox")) {
			rendererName += "Firefox";
		} else if (ua.contains("safari")) {
			rendererName += "Safari";
		} else if (ua.contains("playstation 4")) {
			rendererName += "PlayStation 4";
		} else if (ua.contains("xbox one")) {
			rendererName += "Xbox One";
		} else {
			rendererName += Messages.getString("PMS.142");
		}

		return rendererName;
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

	@Override
	public String getRendererIcon() {
		if (StringUtils.isEmpty(ua)) {
			return super.getRendererIcon();
		}
		if (ua.contains("chrome")) {
			return "chrome.png";
		}
		if (ua.contains("msie")) {
			return "internetexplorer.png";
		}
		if (ua.contains("firefox")) {
			return "firefox.png";
		}
		if (ua.contains("playstation 4")) {
			return "ps4.png";
		}
		if (ua.contains("xbox one")) {
			return "xbox-one.png";
		}
		return super.getRendererIcon();
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

	public boolean isChrome() {
		return pmsconfiguration.getWebChrome() && (ua != null) && ua.contains("chrome");
	}

	@Override
	public boolean getOutputOptions(DLNAResource dlna, Player player, List<String> cmdList) {
		if (player instanceof FFMpegVideo) {
			if (dlna.getFormat().isVideo()) {
				DLNAMediaInfo media = dlna.getMedia();
				boolean flash = media != null && "video/flash".equals(media.getMimeType());
				if (flash) {
					fflashCmds(cmdList, media);
				} else {
					if(RemoteUtil.MIME_TRANS.equals(RemoteUtil.MIME_OGG))  {
						if (isChrome()) {
							chromeCmd(cmdList);
						} else {
							ffoggCmd(cmdList);
						}
					}
					else if (RemoteUtil.MIME_TRANS.equals(RemoteUtil.MIME_MP4)) {
						ffmp4Cmd(cmdList);
					}
					else if (RemoteUtil.MIME_TRANS.equals(RemoteUtil.MIME_WEBM)) {
						// nothing here yet
					}
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
		cmdList.add("1000k");
		cmdList.add("-b:a");
		cmdList.add("128k");*/
		cmdList.add("-f");
		cmdList.add("ogg");
	}

	private void ffmp4Cmd(List<String> cmdList) {
		cmdList.add("-c:v");
		cmdList.add("libx264");
		cmdList.add("-preset");
		cmdList.add("ultrafast");
		cmdList.add("-tune");
		cmdList.add("zerolatency");
		cmdList.add("-profile:v");
		cmdList.add("high");
		cmdList.add("-level:v");
		cmdList.add("3.1");
		cmdList.add("-c:a");
		cmdList.add("aac");
		cmdList.add("-ab");
		cmdList.add("16k");
		cmdList.add("-ar");
		cmdList.add("44100");
		cmdList.add("-strict");
		cmdList.add("experimental");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-frag_duration");
		cmdList.add("300");
		cmdList.add("-frag_size");
		cmdList.add("100");
		cmdList.add("-flags");
		cmdList.add("+aic+mv4");
		cmdList.add("-movflags");
		cmdList.add("+faststart");
		cmdList.add("-f");
		cmdList.add("mp4");
		//cmdList.add("separate_moof+frag_keyframe+empty_moov");
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
		return "scale=" + RemoteUtil.getWidth() + ":" + RemoteUtil.getHeight();
	}
}
