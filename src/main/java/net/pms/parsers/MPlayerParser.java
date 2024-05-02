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
package net.pms.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil;
import net.pms.io.FailSafeProcessWrapper;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.InputFile;
import net.pms.util.Iso639;
import net.pms.util.MPlayerDvdAudioStreamChannels;
import net.pms.util.MPlayerDvdAudioStreamTypes;
import net.pms.util.ProcessUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MPlayerParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(JaudiotaggerParser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String PARSER_NAME = "MPLAYER";

	public static final Pattern AUDIO_STREAM_PATTERN = Pattern.compile(
		"^audio stream: (?<StreamNumber>\\d+) format: (?<Codec>\\S+) \\((?<Channels>\\S+)\\) language: (?<Language>\\w*) aid: (?<AID>\\d+)\\.$"
	);
	public static final Pattern SUBTITLE_STREAM_PATTERN = Pattern.compile(
		"^subtitle \\( sid \\): (?<StreamNumber>\\d+) language: (?<Language>\\w*)$"
	);

	/**
	 * This class is not meant to be instantiated.
	 */
	private MPlayerParser() {
	}

	public static boolean isValid() {
		return CONFIGURATION.getMPlayerPath() != null;
	}

	/**
	 * Parse a DVD iso file.
	 * @param file the iso file.
	 * @param titles fill titles duration.
	 * @return volumeId
	 */
	public static String parseIsoFile(File file, Map<Integer, Double> titles) {
		if (!isValid()) {
			return null;
		}
		String volumeId = null;
		String[] cmd = new String[]{
			CONFIGURATION.getMPlayerPath(),
			"-identify",
			"-endpos",
			"0",
			"-ao",
			"null",
			"-vc",
			"null",
			"-vo",
			"null",
			"-dvd-device",
			ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath()),
			"dvd://"
		};
		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);
		params.setLog(true);
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmd, params, true, false);
		Runnable r = () -> {
			UMSUtils.sleep(10000);
			pw.stopProcess();
		};

		Thread failsafe = new Thread(r, "DVDISO Failsafe");
		failsafe.start();
		pw.runInSameThread();
		List<String> lines = pw.getOtherResults();
		if (lines != null) {
			for (String line : lines) {
				if (line.startsWith("ID_DVD_TITLE_") && line.contains("_LENGTH")) {
					int rank = Integer.parseInt(line.substring(13, line.indexOf("_LENGT")));
					double duration = Double.parseDouble(line.substring(line.lastIndexOf("LENGTH=") + 7));
					titles.put(rank, duration);
				} else if (line.startsWith("ID_DVD_VOLUME_ID")) {
					volumeId = line.substring(line.lastIndexOf("_ID=") + 4).trim();
					if (CONFIGURATION.isPrettifyFilenames()) {
						volumeId = volumeId.replaceAll("_", " ");
						if (StringUtils.isNotBlank(volumeId) && volumeId.equals(volumeId.toUpperCase(PMS.getLocale()))) {
							volumeId = WordUtils.capitalize(volumeId.toLowerCase(PMS.getLocale()));
						}
					}
				}
			}
		}
		return volumeId;
	}

	public static void parseDvdTitle(MediaInfo media, File file, int title) {
		if (!isValid()) {
			return;
		}
		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);
		params.setLog(true);

		boolean generateThumbnails = false;
		String frameName = "" + media.hashCode();
		if (CONFIGURATION.isDvdIsoThumbnails()) {
			try {
				params.setWorkDir(CONFIGURATION.getTempFolder());
				generateThumbnails = true;
			} catch (IOException e1) {
				LOGGER.error("Could not create temporary folder, DVD thumbnails won't be generated: {}", e1.getMessage());
				LOGGER.trace("", e1);
			}
		}

		String[] cmd;
		if (generateThumbnails) {
			cmd = new String[] {
				CONFIGURATION.getMPlayerPath(),
				"-identify",
				"-ss",
				Integer.toString(CONFIGURATION.getThumbnailSeekPos()),
				"-frames",
				"1",
				"-v",
				"-ao",
				"null",
				"-vo",
				"jpeg:outdir=mplayer_thumbs:subdirs=\"" + frameName + "\"",
				"-dvd-device",
				ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath()),
				"dvd://" + title
			};
		} else {
			cmd = new String[] {
				CONFIGURATION.getMPlayerPath(),
				"-identify",
				"-endpos",
				"0",
				"-v",
				"-ao",
				"null",
				"-vc",
				"null",
				"-vo",
				"null",
				"-dvd-device",
				ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath()),
				"dvd://" + title
			};
		}

		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmd, params, true, false);
		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 10000);
		media.setParsing(true);
		fspw.runInSameThread();

		if (fspw.hasFail()) {
			LOGGER.info("Error parsing information from the file: " + file);
		} else {
			if (generateThumbnails) {
				DLNAThumbnail thumbnail = MPlayerParser.getThumbnail(frameName);
				if (thumbnail != null) {
					Long thumbId = ThumbnailStore.getId(thumbnail);
					media.setThumbnailId(thumbId);
					media.setThumbnailSource(ThumbnailSource.MPLAYER_SEEK);
				}
			}
			parseDvdTitleInfo(media, pw.getOtherResults(), title);
		}
		media.setParsing(false);
	}

	public static DLNAThumbnail getThumbnail(MediaInfo media, InputFile inputFile, Double seekPosition) {
		if (!isValid()) {
			return null;
		}
		File tmpFolder;
		try {
			tmpFolder = CONFIGURATION.getTempFolder();
		} catch (IOException ex) {
			return null;
		}

		File file = inputFile.getFile();
		String frameName = "" + inputFile.hashCode();
		String[] args = new String[14];
		args[0] = CONFIGURATION.getMPlayerPath();
		args[1] = "-ss";
		double thumbnailSeekPos = seekPosition != null ? seekPosition : CONFIGURATION.getThumbnailSeekPos();
		thumbnailSeekPos = Math.min(thumbnailSeekPos, media.getDurationInSeconds());
		args[2] = Integer.toString((int) thumbnailSeekPos);

		args[3] = "-quiet";

		if (file != null) {
			args[4] = ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath());
		} else {
			args[4] = "-";
		}

		args[5] = "-msglevel";
		args[6] = "all=4";
		args[7] = "-vf";
		args[8] = "scale=320:-2";
		args[9] = "-frames";
		args[10] = "1";
		args[11] = "-vo";
		frameName = "mplayer_thumbs:subdirs=\"" + frameName + "\"";
		frameName = frameName.replace(',', '_');
		args[12] = "jpeg:outdir=" + frameName;
		args[13] = "-nosound";
		OutputParams params = new OutputParams(CONFIGURATION);
		params.setWorkDir(tmpFolder);
		params.setMaxBufferSize(1);
		params.setStdIn(inputFile.getPush());
		params.setLog(true);
		params.setNoExitCheck(true); // not serious if anything happens during the thumbnailer
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args, true, params);

		// FAILSAFE
		media.waitMediaParsing(5);
		media.setParsing(true);

		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 3000);
		fspw.runInSameThread();
		media.setParsing(false);
		if (fspw.hasFail()) {
			LOGGER.info("Error parsing information from the file: " + file);
			return null;
		}
		DLNAThumbnail thumbnail = MPlayerParser.getThumbnail(frameName);
		if (thumbnail != null) {
			media.setThumbnailSource(ThumbnailSource.MPLAYER_SEEK);
		}
		return thumbnail;
	}

	private static DLNAThumbnail getThumbnail(String frameName) {
		File tmpFolder;
		try {
			tmpFolder = CONFIGURATION.getTempFolder();
		} catch (IOException ex) {
			return null;
		}

		String jpgPath = tmpFolder + "/mplayer_thumbs/" + frameName + "00000001/00000001.jpg";
		jpgPath = jpgPath.replace(',', '_');
		File jpg = new File(jpgPath);

		if (jpg.exists()) {
			DLNAThumbnail result = null;
			try (InputStream is = new FileInputStream(jpg)) {
				int sz = is.available();
				if (sz > 0) {
					byte[] bytes = new byte[sz];
					is.read(bytes);
					result = DLNAThumbnail.toThumbnail(
							bytes,
							640,
							480,
							ImagesUtil.ScaleType.MAX,
							ImageFormat.SOURCE,
							false
					);
				}
			} catch (IOException e) {
				LOGGER.debug("Error while decoding thumbnail: " + e.getMessage());
				LOGGER.trace("", e);
			}

			if (!jpg.delete()) {
				jpg.deleteOnExit();
			}

			// Try and retry
			if (!jpg.getParentFile().delete() && !jpg.getParentFile().delete()) {
				LOGGER.debug("Failed to delete \"" + jpg.getParentFile().getAbsolutePath() + "\"");
			}
			return result;
		}
		return null;
	}

	private static void parseDvdTitleInfo(MediaInfo media, List<String> lines, int title) {
		String duration = null;
		int nbsectors = 0;
		String fps = null;
		String codecV = null;
		String width = null;
		String height = null;
		String aspect = null;
		MediaVideo videoTrack = new MediaVideo();
		ArrayList<MediaAudio> audioTracks = new ArrayList<>();
		ArrayList<MediaSubtitle> subtitles = new ArrayList<>();
		if (lines != null) {
			for (String line : lines) {
				if (line.startsWith("DVD start=")) {
					nbsectors = Integer.parseInt(line.substring(line.lastIndexOf('=') + 1).trim());
				} else if (line.startsWith("audio stream:")) {
					MediaAudio audio = parseAudioStream(line);
					if (audio != null) {
						audio.setId(audioTracks.size());
						audioTracks.add(audio);
					}
				} else if (line.startsWith("subtitle")) {
					MediaSubtitle subtitle = parseSubtitleStream(line);
					if (subtitle != null) {
						subtitle.setId(subtitles.size());
						subtitles.add(subtitle);
					}
				} else if (line.startsWith("ID_VIDEO_WIDTH=")) {
					width = line.substring(line.indexOf("ID_VIDEO_WIDTH=") + 15).trim();
				} else if (line.startsWith("ID_VIDEO_HEIGHT=")) {
					height = line.substring(line.indexOf("ID_VIDEO_HEIGHT=") + 16).trim();
				} else if (line.startsWith("ID_VIDEO_FPS=")) {
					fps = line.substring(line.indexOf("ID_VIDEO_FPS=") + 13).trim();
				} else if (line.startsWith("ID_LENGTH=")) {
					duration = line.substring(line.indexOf("ID_LENGTH=") + 10).trim();
				} else if (line.startsWith("ID_VIDEO_ASPECT=")) {
					aspect = line.substring(line.indexOf("ID_VIDEO_ASPECT=") + 16).trim();
				} else if (line.startsWith("ID_VIDEO_FORMAT=")) {
					String formatStr = line.substring(line.lastIndexOf("=") + 1).trim();
					switch (formatStr) {
						case "0x31435657" -> {
							codecV = FormatConfiguration.VC1;
						}
						case "0x10000001" -> {
							codecV = FormatConfiguration.MPEG1;
						}
						case "0x10000002" -> {
							codecV = FormatConfiguration.MPEG2;
						}
						default -> LOGGER.warn("Unknown video format value \"{}\"", formatStr);
					}
				}
			}
		}

		media.setSize(nbsectors * 2048L);

		if (duration != null) {
			try {
				double d = Double.parseDouble(duration);
				media.setDuration(d);
				videoTrack.setDuration(d);
			} catch (NumberFormatException e) {
				LOGGER.error("Could not parse DVD video duration: {}", duration);
				LOGGER.trace("", e);
			}
		}
		if (fps != null) {
			try {
				double d = Double.parseDouble(fps);
				media.setFrameRate(d);
				videoTrack.setFrameRate(d);
			} catch (NumberFormatException e) {
				LOGGER.error("Could not parse DVD video fps: {}", fps);
				LOGGER.trace("", e);
			}
		}
		media.setAspectRatioDvdIso(aspect);
		media.setDvdtrack(title);
		media.setContainer(FormatConfiguration.ISO);
		videoTrack.setCodec(codecV != null ? codecV : FormatConfiguration.MPEG2);

		try {
			videoTrack.setWidth(Integer.parseInt(width));
		} catch (NumberFormatException nfe) {
			LOGGER.debug("Could not parse DVD video width \"{}\"", width);
		}

		try {
			videoTrack.setHeight(Integer.parseInt(height));
		} catch (NumberFormatException nfe) {
			LOGGER.debug("Could not parse DVD video height \"{}\"", height);
		}
		videoTrack.setId(0);
		media.addVideoTrack(videoTrack);
		media.setAudioTracks(audioTracks);
		media.setSubtitlesTracks(subtitles);
		media.setMediaParser(PARSER_NAME);
	}

	private static MediaAudio parseAudioStream(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		Matcher matcher = AUDIO_STREAM_PATTERN.matcher(line);
		if (matcher.find()) {
			MediaAudio audio = new MediaAudio();
			try {
				audio.setStreamOrder(Integer.valueOf(matcher.group("StreamNumber")));
			} catch (NumberFormatException e) {
				LOGGER.error(
					"Could not parse audio stream number \"{}\": {}",
					matcher.group("StreamNumber"),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			audio.setCodec(MPlayerDvdAudioStreamTypes.typeOf(matcher.group("Codec")).getFormatConfigurationCode());
			audio.setNumberOfChannels(
				MPlayerDvdAudioStreamChannels.typeOf(matcher.group("Channels")).getNumberOfChannels()
			);
			String languageCode = Iso639.getISOCode(matcher.group("Language"));
			audio.setLang(StringUtils.isBlank(languageCode) ? MediaLang.UND : languageCode);
			try {
				audio.setOptionalId(Long.valueOf(matcher.group("AID")));
			} catch (NumberFormatException e) {
				LOGGER.error("Could not parse audio id \"{}\": {}", matcher.group("AID"), e.getMessage());
				LOGGER.trace("", e);
			}
			if (audio.getOptionalId() >= MPlayerDvdAudioStreamTypes.FIRST_LPCM_AID) {
				if (!FormatConfiguration.LPCM.equals(audio.getCodec())) {
					LOGGER.warn(
						"Unexpected error parsing DVD audio stream codec. AID dictates LPCM while codec is {}",
						audio.getOptionalId()
					);
				}
			} else if (audio.getOptionalId() >= MPlayerDvdAudioStreamTypes.FIRST_DTS_AID) {
				if (!FormatConfiguration.DTS.equals(audio.getCodec())) {
					LOGGER.warn(
						"Unexpected error parsing DVD audio stream codec. AID dictates DTS while codec is {}",
						audio.getOptionalId()
					);
				}
			} else if (audio.getOptionalId() >= MPlayerDvdAudioStreamTypes.FIRST_AC3_AID) {
				if (!FormatConfiguration.AC3.equals(audio.getCodec())) {
					LOGGER.warn(
						"Unexpected error parsing DVD audio stream codec. AID dictates AC3 while codec is {}",
						audio.getOptionalId()
					);
				}
			} else if (!FormatConfiguration.MP2.equals(audio.getCodec())) {
				LOGGER.warn(
					"Unexpected error parsing DVD audio stream codec. AID dictates MP2 while codec is {}",
					audio.getOptionalId()
				);
			}
			/*
			 * MPlayer doesn't give information about bit rate, sampling rate
			 * and bit depth, so we'll have to "guess" the sample rate. Bit rate
			 * and bit depth are impossible to guess.
			 */
			switch (audio.getCodec()) {
				// Could be 48 kHz or 96 kHz, 48 kHz is most common
				case FormatConfiguration.LPCM, FormatConfiguration.DTS,
					// Only 48 kHz is allowed
					FormatConfiguration.MP2, FormatConfiguration.AC3 ->	{
						audio.setSampleRate(48000);
					}
				default -> {
					// Shouldn't happen
				}
			}
			return audio;
		}
		LOGGER.warn("Could not parse DVD audio stream \"{}\"", line);
		return null;
	}

	private static MediaSubtitle parseSubtitleStream(String line) {
		if (StringUtils.isBlank(line)) {
			return null;
		}
		Matcher matcher = SUBTITLE_STREAM_PATTERN.matcher(line);
		if (matcher.find()) {
			MediaSubtitle subtitle = new MediaSubtitle();
			subtitle.setType(SubtitleType.UNKNOWN); // Not strictly true, but we lack a proper type
			try {
				subtitle.setStreamOrder(Integer.valueOf(matcher.group("StreamNumber")));
			} catch (NumberFormatException e) {
				LOGGER.error(
					"Could not parse subtitle stream number \"{}\": {}",
					matcher.group("StreamNumber"),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}
			String languageCode = Iso639.getISOCode(matcher.group("Language"));
			subtitle.setLang(StringUtils.isBlank(languageCode) ? MediaLang.UND : languageCode);

			return subtitle;
		}
		LOGGER.warn("Could not parse DVD subtitle stream \"{}\"", line);
		return null;
	}

}
