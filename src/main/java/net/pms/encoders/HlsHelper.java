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
package net.pms.encoders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.TimeRange;
import net.pms.media.audio.MediaAudio;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.renderers.Renderer;
import org.apache.commons.lang3.StringUtils;

/**
 * HlsConfiguration Helper.
 * Some specs :
 * For maximum compatibility, some H.264 variants SHOULD be less than or equal to High Profile, Level 4.1.
 * Profile and Level for H.264 MUST be less than or equal to High Profile, Level 5.2.
 * For backward compatibility, content SHOULD NOT use a higher level than required by the content resolution and frame rate.
 * For HDR content, frame rates less than or equal to 30 fps SHOULD be provided.
 * Stereo audio in AAC, HE-AAC v1, or HE-AAC v2 format MUST be provided.
 * You SHOULD NOT use HE-AAC if your audio bit rate is above 64 kbit/s.
 */
public class HlsHelper {
	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String NONE_CONF_NAME = "NONE";
	private static final String COPY_CONF_NAME = "COPY";

	/**
	 * This class is not meant to be instantiated.
	 */
	private HlsHelper() {
	}

	public static HlsConfiguration getByKey(String label) {
		String[] labelParts = label.split("_");
		if (labelParts.length > 1) {
			HlsVideoConfiguration videoConf = HlsVideoConfiguration.getByKey(labelParts[0]);
			HlsAudioConfiguration audioConf = HlsAudioConfiguration.getByKey(labelParts[1]);
			int audioStream = -1;
			int subtitle = -1;
			if (labelParts.length == 3) {
				int parsedValue;
				try {
					parsedValue = Integer.parseInt(labelParts[2]);
				} catch (NumberFormatException e) {
					//remove audioStream
					parsedValue = -1;
				}
				if (audioConf != null && !audioConf.label.equals(NONE_CONF_NAME)) {
					audioStream = parsedValue;
				} else {
					subtitle = parsedValue;
				}
			}
			if (videoConf != null && audioConf != null) {
				return new HlsConfiguration(videoConf, audioConf, audioStream, subtitle);
			}
		}
		return null;
	}

	/*
	EXT-X-VERSION Features and usage notes
	2 :
		IV attribute of the EXT-X-KEY tag
	3 :
		Floating-point EXTINF duration values
	4 :
		EXT-X-BYTERANGE, EXT-X-I-FRAME-STREAM-INF, EXT-X-I-FRAMES-ONLY, EXT-X-MEDIA, the AUDIO and VIDEO attributes of the EXT-X-STREAM-INF tag
	5 :
		KEYFORMAT and KEYFORMATVERSIONS attributes of the EXT-X-KEY tag.
		The EXT-X-MAP tag.
		SUBTITLES media type.
		SAMPLE-AES encryption method EXT-X-KEY
	6 :
		CLOSED-CAPTIONS media type. Allow EXT-X-MAP for subtitle playlists
	7 :
		EXT-X-SESSION-DATA, EXT-X-SESSION-KEY, EXT-X-DATERANGE, 'SERVICEn' values of INSTREAM-ID, AVERAGE-BANDWIDTH, FRAME-RATE, CHANNELS, and HDCP-LEVEL attributes.
	8 :
		EXT-X-GAP, EXT-X-DEFINE, Variable Substitution, VIDEO-RANGE attribute.

	The following features aren't backward compatible. Older clients may fail to play the content if you use these features but don't specify the protocol version where they were introduced:
		You must use at least protocol version 2 if you have IV in EXT-X-KEY.
		You must use at least protocol version 3 if you have floating point EXTINF duration values.
		You must use at least protocol version 4 if you have EXT-X-BYTERANGE or EXT-X-IFRAME-ONLY.
		You must use at least protocol version 5 if you specify the SAMPLE-AES encryption method in EXT-X-KEY, or if you have KEYFORMAT and KEYFORMATVERSIONS attributes in EXT-X-KEY, or if you have EXT-X-MAP.
	    You must use at least protocol version 6 if you have EXT-X-MAP in a Media Playlist that does not contain EXT-X-I-FRAMES-ONLY.
		You must use at least protocol version 7 if you specify "SERVICE" values for the INSTREAM-ID attribute of EXT-X-MEDIA.
	    You must use at least protocol version 8 if you use variable substitution.
	*/

	public static String getHLSm3u8(DLNAResource dlna, Renderer renderer, String baseUrl) {
		if (dlna.getMedia() != null) {
			int hlsVersion = renderer.getHlsVersion();
			MediaInfo mediaVideo = dlna.getMedia();
			// add 5% to handle cropped borders
			int maxHeight = (int) (mediaVideo.getHeight() * 1.05);
			String id = dlna.getResourceId();
			StringBuilder sb = new StringBuilder();
			sb.append("#EXTM3U\n");
			if (hlsVersion > 1) {
				sb.append("#EXT-X-VERSION:").append(hlsVersion).append("\n");
			}
			//add audio languages
			List<HlsAudioConfiguration> audioGroups = new ArrayList<>();
			MediaAudio mediaAudioDefault = null;
			if (!mediaVideo.getAudioTracksList().isEmpty()) {
				//try to find the preferred language
				mediaAudioDefault = null;
				StringTokenizer st = new StringTokenizer(CONFIGURATION.getAudioLanguages(), ",");
				while (st.hasMoreTokens() && mediaAudioDefault == null) {
					String lang = st.nextToken().trim();
					for (MediaAudio mediaAudio : mediaVideo.getAudioTracksList()) {
						if (mediaAudio.matchCode(lang)) {
							mediaAudioDefault = mediaAudio;
							break;
						}
					}
				}
				//try to keep the codec to copy only
				if (mediaAudioDefault != null && isMediaAudioCompatible(mediaAudioDefault)) {
					HlsAudioConfiguration audioGroup = getAudioConfiguration(mediaAudioDefault);
					if (audioGroup != null) {
						audioGroups.add(audioGroup);
					}
				}
			}
			HlsAudioConfiguration askedDefaultAudioGroup = renderer.isTranscodeToAAC() ? HlsAudioConfiguration.getByKey("AAC-LC") : HlsAudioConfiguration.getByKey("AC3");
			if (!audioGroups.contains(askedDefaultAudioGroup)) {
				audioGroups.add(askedDefaultAudioGroup);
			}
			Map<String, Integer> audioNames = new HashMap<>();
			for (HlsAudioConfiguration audioGroup : audioGroups) {
				String groupId = audioGroup.label;
				for (MediaAudio mediaAudio : mediaVideo.getAudioTracksList()) {
					sb.append("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"").append(groupId).append("\",LANGUAGE=\"");
					sb.append(mediaAudio.getLang()).append("\",NAME=\"");
					String audioName = mediaAudio.getLangFullName();
					if (audioGroup.audioChannels != 2) {
						audioName = switch (audioGroup.audioChannels) {
							case 6 -> audioName.concat(" (5.1)");
							case 8 -> audioName.concat(" (7.1)");
							default -> audioName.concat(" (" + audioGroup.audioChannels + "ch)");
						};
					}
					if (audioNames.containsKey(audioName)) {
						audioNames.put(audioName, audioNames.get(audioName) + 1);
						audioName = audioName.concat(" [" + audioNames.get(audioName) + "]");
					} else {
						audioNames.put(audioName, 0);
					}
					sb.append(audioName);
					sb.append("\",CHANNELS=").append(audioGroup.audioChannels);
					sb.append(",AUTOSELECT=YES,DEFAULT=");
					if (mediaAudioDefault != null && mediaAudioDefault.equals(mediaAudio)) {
						sb.append("YES\n");
					} else {
						sb.append("NO").append(",URI=\"").append(baseUrl).append(id).append("/hls/").append(NONE_CONF_NAME).append("_").append(audioGroup.label).append("_").append(mediaAudio.getId()).append(".m3u8\"\n");
					}
				}
			}
			boolean subtitleAdded = false;
			for (MediaSubtitle mediaSubtitle : mediaVideo.getSubtitlesTracks()) {
				if (mediaSubtitle.isEmbedded() && mediaSubtitle.getType().isText()) {
					subtitleAdded = true;
					sb.append("#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"sub1\",CHARACTERISTICS=\"public.accessibility.transcribes-spoken-dialog\",AUTOSELECT=YES");
					sb.append(",DEFAULT=").append(mediaSubtitle.isDefault() ? "YES" : "NO");
					sb.append(",FORCED=NO");
					String subtitleName;
					if (StringUtils.isNotBlank(mediaSubtitle.getSubtitlesTrackTitleFromMetadata())) {
						subtitleName = mediaSubtitle.getSubtitlesTrackTitleFromMetadata();
					} else if (StringUtils.isNotBlank(mediaSubtitle.getName())) {
						subtitleName = mediaSubtitle.getName();
					} else if (StringUtils.isNotBlank(mediaSubtitle.getLangFullName())) {
						subtitleName = mediaSubtitle.getLangFullName();
					} else {
						subtitleName = String.valueOf(mediaSubtitle.getId());
					}
					if (mediaSubtitle.isForced() && !subtitleName.toLowerCase().contains("forced")) {
						subtitleName = subtitleName.concat(" (forced)");
					}
					sb.append(",NAME=\"").append(subtitleName).append("\"");
					sb.append(",LANGUAGE=\"").append(mediaSubtitle.getLang()).append("\"");
					sb.append(",URI=\"").append(baseUrl).append(id).append("/hls/").append(NONE_CONF_NAME).append("_").append(NONE_CONF_NAME).append("_").append(mediaSubtitle.getId()).append(".m3u8\"\n");
				}
			}
			//adding chapters
			if (mediaVideo.hasChapters()) {
				sb.append("#EXT-X-SESSION-DATA:DATA-ID=\"com.apple.hls.chapters\",URI=\"").append(baseUrl).append(id).append("/hls/chapters.json\"\n");
			}
			//adding video
			List<HlsVideoConfiguration> videoGroups = new ArrayList<>();
			//always add copy conf first
			videoGroups.add(HlsVideoConfiguration.getByKey(COPY_CONF_NAME));
			if (renderer.getHlsMultiVideoQuality()) {
				//always add basic LD conf and other conf that match
				for (HlsVideoConfiguration videoConf : HlsVideoConfiguration.getValues()) {
					if (videoConf.isTranscodable && !videoGroups.contains(videoConf) &&
						(mediaVideo.getHeight() != videoConf.resolutionHeight && mediaVideo.getWidth() != videoConf.resolutionWidth) &&
						((maxHeight >= videoConf.resolutionHeight && mediaVideo.getWidth() >= videoConf.resolutionWidth) || "LD".equals(videoConf.label))
					) {
						videoGroups.add(videoConf);
					}
				}
			}
			for (HlsVideoConfiguration videoGroup : videoGroups) {
				for (HlsAudioConfiguration audioGroup : audioGroups) {
					sb.append("#EXT-X-STREAM-INF:BANDWIDTH=");
					if (videoGroup.label.equals(COPY_CONF_NAME)) {
						sb.append(mediaVideo.getBitrate());
						sb.append(",RESOLUTION=").append(mediaVideo.getWidth()).append("x").append(mediaVideo.getHeight());
						sb.append(",AUDIO=\"").append(audioGroup.label).append("\"");
						sb.append(",CODECS=\"").append("avc1.64001e");
					} else {
						sb.append(videoGroup.bandwidth);
						sb.append(",RESOLUTION=").append(videoGroup.resolutionWidth).append("x").append(videoGroup.resolutionHeight);
						sb.append(",AUDIO=\"").append(audioGroup.label).append("\"");
						sb.append(",CODECS=\"").append(videoGroup.videoCodec);
					}
					sb.append(",").append(audioGroup.audioCodec).append("\"");
					if (subtitleAdded) {
						sb.append(",SUBTITLES=\"sub1\"");
					}
					sb.append("\n");
					sb.append(baseUrl).append(id).append("/hls/").append(videoGroup.label).append("_").append(audioGroup.label).append("_");
					if (mediaAudioDefault != null) {
						sb.append(mediaAudioDefault.getId());
					} else {
						sb.append("0");
					}
					sb.append(".m3u8\n");
				}
			}

			return sb.toString();
		}
		return null;
	}

	/*
	* Target durations SHOULD be 6 seconds.
	* Segment durations SHOULD be nominally 6 seconds
	* Media Segments MUST NOT exceed the target duration by more than 0.5 seconds.
	*/
	public static final double DEFAULT_TARGETDURATION = 6;

	public static String getHLSm3u8ForRendition(DLNAResource dlna, Renderer renderer, String baseUrl, String rendition) {
		if (dlna.getMedia() != null) {
			int hlsVersion = renderer.getHlsVersion();
			Double duration = dlna.getMedia().getDuration();
			double partLen = duration;
			String id = dlna.getResourceId();
			String targetDurationStr = String.valueOf(Double.valueOf(Math.ceil(DEFAULT_TARGETDURATION)).intValue());
			String defaultDurationStr = hlsVersion > 2 ? String.format(Locale.ENGLISH, "%.6f", DEFAULT_TARGETDURATION) : targetDurationStr;
			String filename = rendition.startsWith(NONE_CONF_NAME + "_" + NONE_CONF_NAME + "_") ? "vtt" : "ts";
			StringBuilder sb = new StringBuilder();
			sb.append("#EXTM3U\n");
			if (hlsVersion > 1) {
				sb.append("#EXT-X-VERSION:").append(hlsVersion).append("\n");
			}
			sb.append("#EXT-X-TARGETDURATION:").append(targetDurationStr).append("\n");
			sb.append("#EXT-X-MEDIA-SEQUENCE:0\n");
			sb.append("#EXT-X-PLAYLIST-TYPE:VOD\n");
			sb.append("#EXT-X-INDEPENDENT-SEGMENTS\n");
			int partCount = 0;
			while (partLen > 0) {
				sb.append("#EXTINF:");
				if (partLen >= DEFAULT_TARGETDURATION) {
					sb.append(defaultDurationStr);
				} else if (hlsVersion > 2) {
					sb.append(String.format(Locale.ENGLISH, "%.6f", partLen));
				} else {
					sb.append(String.valueOf(Double.valueOf(Math.ceil(partLen)).intValue()));
				}
				sb.append(",\n");
				sb.append(baseUrl).append(id).append("/hls/").append(rendition).append("/").append(partCount).append(".").append(filename).append("\n");
				partLen -= DEFAULT_TARGETDURATION;
				partCount++;
			}
			sb.append("#EXT-X-ENDLIST\n");
			return sb.toString();
		}
		return null;
	}

	public static TimeRange getTimeRange(String url) {
		if (!url.contains("/")) {
			return null;
		}
		String positionStr = url.substring(url.lastIndexOf("/") + 1);
		if (!positionStr.contains(".")) {
			return null;
		}
		positionStr = positionStr.substring(0, positionStr.indexOf("."));
		int position;
		try {
			position = Integer.parseInt(positionStr);
		} catch (NumberFormatException es) {
			return null;
		}
		double askedStart =  Double.valueOf(position) * HlsHelper.DEFAULT_TARGETDURATION;
		return new TimeRange(askedStart, askedStart + HlsHelper.DEFAULT_TARGETDURATION);
	}

	public static InputStream getInputStream(String url, DLNAResource resource, Renderer renderer) throws IOException {
		if (!url.contains("/hls/")) {
			return null;
		}
		String rendition = url.substring(url.lastIndexOf("/hls/") + 5);
		rendition = rendition.substring(0, rendition.indexOf("/"));
		//here we need to set rendition to renderer
		HlsHelper.HlsConfiguration hlsConfiguration = getByKey(rendition);
		Range timeRange = HlsHelper.getTimeRange(url);
		if (hlsConfiguration != null && timeRange != null) {
			return resource.getInputStream(timeRange, renderer, hlsConfiguration);
		}
		return null;
	}

	public static boolean isMediaCompatible(MediaInfo mediaVideo) {
		if (mediaVideo.isH264() && mediaVideo.getAvcAsInt() <= 52) {
			//can't check that, we do not store it on database
			//profile is not saved in database...
			//getAvcProfileId(mediaVideo.getH264Profile()) <= 100
			if (!mediaVideo.hasAudio()) {
				return true;
			}
			for (MediaAudio mediaAudio : mediaVideo.getAudioTracksList()) {
				if (isMediaAudioCompatible(mediaAudio)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isMediaAudioCompatible(MediaAudio mediaAudio) {
		return getAudioConfiguration(mediaAudio) != null;
	}

	public static HlsAudioConfiguration getAudioConfiguration(MediaAudio mediaAudio) {
		if (mediaAudio.isAACLC()) {
			if (mediaAudio.getAudioProperties().getNumberOfChannels() < 3) {
				return HlsAudioConfiguration.getByKey("AAC-LC");
			} else if (mediaAudio.getAudioProperties().getNumberOfChannels() == 6) {
				return HlsAudioConfiguration.getByKey("AAC-LC-6");
			}
		} else if (mediaAudio.isHEAAC()) {
			if (mediaAudio.getAudioProperties().getNumberOfChannels() < 3) {
				return HlsAudioConfiguration.getByKey("HE-AAC");
			} else if (mediaAudio.getAudioProperties().getNumberOfChannels() == 6) {
				return HlsAudioConfiguration.getByKey("HE-AAC-6");
			}
		} else if (mediaAudio.isAC3()) {
			if (mediaAudio.getAudioProperties().getNumberOfChannels() < 3) {
				return HlsAudioConfiguration.getByKey("AC3");
			} else if (mediaAudio.getAudioProperties().getNumberOfChannels() == 6) {
				return HlsAudioConfiguration.getByKey("AC3-6");
			}
		} else if (mediaAudio.isEAC3()) {
			if (mediaAudio.getAudioProperties().getNumberOfChannels() < 3) {
				return HlsAudioConfiguration.getByKey("EAC3");
			} else if (mediaAudio.getAudioProperties().getNumberOfChannels() == 6) {
				return HlsAudioConfiguration.getByKey("EAC3-6");
			} else if (mediaAudio.getAudioProperties().getNumberOfChannels() == 8) {
				return HlsAudioConfiguration.getByKey("EAC3-8");
			} else if (mediaAudio.getAudioProperties().getNumberOfChannels() == 16) {
				return HlsAudioConfiguration.getByKey("EAC3-16");
			}
		}
		return null;
	}

	public static int getAvcProfileId(String profile) {
		if (profile == null) {
			return 255;
		}
		return switch (profile) {
			case "CAVLC 4:4:4 Intra" -> 44;
			case "Baseline" -> 66;
			case "Main" -> 77;
			case "Scalable Baseline" -> 83;
			case "Scalable High" -> 86;
			case "Extended" -> 88;
			case "High" -> 100;
			case "High 10" -> 110;
			case "Multiview High" -> 118;
			case "High 4:2:2" -> 122;
			case "Stereo High" -> 128;
			case "Multiview Depth High" -> 138;
			case "High 4:4:4" -> 144;
			case "High 4:4:4 Predictive" -> 244;
			default -> 255;
		};
	}

	public static class HlsConfiguration {
		public final HlsVideoConfiguration video;
		public final HlsAudioConfiguration audio;
		public final int audioStream;
		public final int subtitle;

		public HlsConfiguration(HlsVideoConfiguration video, HlsAudioConfiguration audio, int audioStream, int subtitle) {
			this.video = video;
			this.audio = audio;
			this.audioStream = audioStream;
			this.subtitle = subtitle;
		}

		public boolean isSubtitle() {
			return subtitle > -1;
		}

		@Override
		public String toString() {
			return "BANDWIDTH=" + video.bandwidth + ",RESOLUTION=" + video.resolutionWidth + "x" + video.resolutionHeight + ",CODECS=\"" + video.videoCodec + "," + audio.audioCodec + "\"";
		}
	}

	/*
	 * video :
	 * avc1.XXXXYY			-> h264			XXXX -> profile (42001 = main, 4D40 = main, 6400 = high) and YY -> hex of level
	 * profiles hex :
	 * 4200 -> Baseline
	 * 4240 -> Constrained Baseline
	 * 4D00 -> Main
	 * 4D40 -> Constrained main
	 * 5800 -> Extended
	 * 6400 -> High
	 * 6408 -> High Progressive
	 * 6E00 -> High 10
	 * 6E10 -> High 10 Intra
	 * 7A00 -> High 4:2:2
	 */
	public static class HlsVideoConfiguration {
		private static final Map<String, HlsVideoConfiguration> BY_LABEL = new LinkedHashMap<>();
		static {
			BY_LABEL.put(NONE_CONF_NAME, new HlsVideoConfiguration(NONE_CONF_NAME, "", -1, 0, 0, "", 0, false));
			BY_LABEL.put(COPY_CONF_NAME, new HlsVideoConfiguration(COPY_CONF_NAME, "", 0, 0, 0, "avc1.64001e", 0, false));
			BY_LABEL.put("UHD2", new HlsVideoConfiguration("UHD2", "Ultra High Definition 8K", 7680, 4320, 72000000, "avc1.64001e", 64000000, true));
			BY_LABEL.put("UHD1", new HlsVideoConfiguration("UHD1", "Ultra High Definition 4K", 3840, 2160, 18000000, "avc1.64001e", 16000000, true));
			BY_LABEL.put("QHD", new HlsVideoConfiguration("QHD", "Quad High Definition", 2560, 1440, 8000000, "avc1.64001e", 9600000, true));
			BY_LABEL.put("FHD", new HlsVideoConfiguration("FHD", "Full High Definition", 1920, 1080, 5000000, "avc1.64001e", 4800000, true));
			BY_LABEL.put("HD", new HlsVideoConfiguration("HD", "High Definition", 1280, 720, 2800000, "avc1.64001e", 2400000, true));
			BY_LABEL.put("SD", new HlsVideoConfiguration("SD", "Standard Definition", 842, 480, 1400000, "avc1.64001e", 1200000, true));
			BY_LABEL.put("LD", new HlsVideoConfiguration("LD", "Low Definition", 640, 360, 800000, "avc1.4D401e", 600000, true));
			BY_LABEL.put("ULD", new HlsVideoConfiguration("ULD", "Ultra-Low Definition", 426, 240, 350000, "avc1.4D401e", 350000, true));
		}
		public final String label;
		public final String description;
		public final int resolutionWidth;
		public final int resolutionHeight;
		public final int bandwidth;
		public final String videoCodec;
		public final int maxVideoBitRate;
		public final boolean isTranscodable;

		public HlsVideoConfiguration(String label, String description, int resolutionWidth, int resolutionHeight, int bandwidth, String videoCodec, int maxVideoBitRate, boolean isTranscodable) {
			this.label = label;
			this.description = description;
			this.resolutionWidth = resolutionWidth;
			this.resolutionHeight = resolutionHeight;
			this.bandwidth = bandwidth;
			this.videoCodec = videoCodec;
			this.maxVideoBitRate = maxVideoBitRate;
			this.isTranscodable = isTranscodable;
		}

		public static HlsVideoConfiguration getByKey(String label) {
			return BY_LABEL.get(label);
		}

		public static String[] getKeys() {
			return BY_LABEL.keySet().toArray(new String[0]);
		}
		public static HlsVideoConfiguration[] getValues() {
			return BY_LABEL.values().toArray(new HlsVideoConfiguration[0]);
		}
	}

	/*
	 * audio :
	 * mp4a.40.2			-> AAC-LC
	 * mp4a.40.5			-> HE-AAC		ffmpeg can't transcode
	 * mp4a.40.29			-> HE-AACv2		ffmpeg can't transcode
	 * mp4a.40.33			-> MP2
	 * mp4a.40.34			-> MP3
	 * ac-3					-> AC3
	 * ec-3					-> EAC3
	 */
	public static class HlsAudioConfiguration {
		private static final Map<String, HlsAudioConfiguration> BY_LABEL = new LinkedHashMap<>();
		static {
			BY_LABEL.put("AAC-LC", new HlsAudioConfiguration("AAC-LC", "AAC", "mp4a.40.2", 160000, 2, true));
			BY_LABEL.put("AAC-LC-6", new HlsAudioConfiguration("AAC-LC-6", "AAC 5.1", "mp4a.40.2", 320000, 6, false));
			BY_LABEL.put("HE-AAC", new HlsAudioConfiguration("HE-AAC", "HE-AAC", "mp4a.40.5", 160000, 2, false));
			BY_LABEL.put("HE-AAC-6", new HlsAudioConfiguration("HE-AAC-6", "HE-AAC 5.1", "mp4a.40.5", 160000, 2, false));
			BY_LABEL.put("HE-AACv2", new HlsAudioConfiguration("HE-AACv2", "HE-AACv2", "mp4a.40.29", 160000, 2, false));
			BY_LABEL.put("MP3", new HlsAudioConfiguration("MP3", "MPEG-1/2 Audio Layer III", "mp4a.40.34", 1200000, 2, false));
			BY_LABEL.put("AC3", new HlsAudioConfiguration("AC3", "Dolby Digital", "ac-3", 1200000, 2, true));
			BY_LABEL.put("AC3-6", new HlsAudioConfiguration("AC3-6", "Dolby Digital 5.1", "ac-3", 384000, 6, false));
			BY_LABEL.put("EAC3", new HlsAudioConfiguration("EAC3", "Dolby Digital Plus", "ec-3", 160000, 2, true));
			BY_LABEL.put("EAC3-6", new HlsAudioConfiguration("EAC3-6", "Dolby Digital Plus 5.1", "ec-3", 192000, 6, false));
			BY_LABEL.put("EAC3-8", new HlsAudioConfiguration("EAC3-8", "Dolby Digital Plus 7.1", "ec-3", 384000, 8, false));
			BY_LABEL.put("EAC3-16", new HlsAudioConfiguration("EAC3-16", "Dolby Digital Plus with Dolby Atmos", "ec-3", 768000, 16, false));
			BY_LABEL.put(NONE_CONF_NAME, new HlsAudioConfiguration(NONE_CONF_NAME, "Removed audio", "", 0, 0, false));
			BY_LABEL.put(COPY_CONF_NAME, new HlsAudioConfiguration(COPY_CONF_NAME, "Copied audio", "", 0, 0, false));
		}
		public final String label;
		public final String description;
		public final String audioCodec;
		public final int audioBitRate;
		public final int audioChannels;
		public final boolean isTranscodable;

		public static HlsAudioConfiguration getByKey(String label) {
			return BY_LABEL.get(label);
		}

		public HlsAudioConfiguration(String label, String description, String audioCodec, int audioBitRate, int audioChannels, boolean isTranscodable) {
			this.label = label;
			this.description = description;
			this.audioCodec = audioCodec;
			this.audioBitRate = audioBitRate;
			this.audioChannels = audioChannels;
			this.isTranscodable = isTranscodable;
		}
	}
}
