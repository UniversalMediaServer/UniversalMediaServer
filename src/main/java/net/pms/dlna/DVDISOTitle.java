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
package net.pms.dlna;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.FormatFactory;
import net.pms.formats.ISOVOB;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.audio.MediaAudio;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.renderers.Renderer;
import net.pms.util.FileUtil;
import net.pms.util.Iso639;
import net.pms.util.MPlayerDvdAudioStreamChannels;
import net.pms.util.MPlayerDvdAudioStreamTypes;
import net.pms.util.ProcessUtil;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DVDISOTitle extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(DVDISOTitle.class);

	public static final Pattern AUDIO_STREAM_PATTERN = Pattern.compile(
		"^audio stream: (?<StreamNumber>\\d+) format: (?<Codec>\\S+) \\((?<Channels>\\S+)\\) language: (?<Language>\\w*) aid: (?<AID>\\d+)\\.$"
	);
	public static final Pattern SUBTITLE_STREAM_PATTERN = Pattern.compile(
		"^subtitle \\( sid \\): (?<StreamNumber>\\d+) language: (?<Language>\\w*)$"
	);

	private final File file;
	private final int title;
	private final String parentName;

	private long length;

	public DVDISOTitle(File file, String parentName, int title) {
		this.file = file;
		this.title = title;
		this.parentName = parentName;
		setLastModified(file.lastModified());
	}

	@Override
	protected void resolveOnce() {
		if (getMedia() == null) {
			setMedia(new MediaInfo());
		}

		OutputParams params = new OutputParams(configuration);
		params.setMaxBufferSize(1);
		params.setLog(true);

		boolean generateThumbnails = false;
		if (configuration.isDvdIsoThumbnails()) {
			try {
				params.setWorkDir(configuration.getTempFolder());
				generateThumbnails = true;
			} catch (IOException e1) {
				LOGGER.error("Could not create temporary folder, DVD thumbnails won't be generated: {}", e1.getMessage());
				LOGGER.trace("", e1);
			}
		}

		String[] cmd;
		if (generateThumbnails) {
			String outFolder = "jpeg:outdir=mplayer_thumbs:subdirs=\"" + this.hashCode() + "\"";
			cmd = new String[] {
				configuration.getMPlayerPath(),
				"-identify",
				"-ss",
				Integer.toString(configuration.getThumbnailSeekPos()),
				"-frames",
				"1",
				"-v",
				"-ao",
				"null",
				"-vo",
				outFolder,
				"-dvd-device",
				ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath()),
				"dvd://" + title
			};
		} else {
			cmd = new String[] {
				configuration.getMPlayerPath(),
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
		Runnable r = () -> {
			UMSUtils.sleep(10000);
			pw.stopProcess();
		};

		Thread failsafe = new Thread(r, "DVD ISO Title Failsafe");
		failsafe.start();
		pw.runInSameThread();
		List<String> lines = pw.getOtherResults();

		String duration = null;
		int nbsectors = 0;
		String fps = null;
		String aspect = null;
		String width = null;
		String height = null;
		String codecV = null;
		ArrayList<MediaAudio> audioTracks = new ArrayList<>();
		ArrayList<MediaSubtitle> subtitles = new ArrayList<>();
		if (lines != null) {
			for (String line : lines) {
				if (line.startsWith("DVD start=")) {
					nbsectors = Integer.parseInt(line.substring(line.lastIndexOf('=') + 1).trim());
				} else if (line.startsWith("audio stream:")) {
					MediaAudio audio = parseMEncoderAudioStream(line);
					if (audio != null) {
						audioTracks.add(audio);
					}
				} else if (line.startsWith("subtitle")) {
					MediaSubtitle subtitle = parseMEncoderSubtitleStream(line);
					if (subtitle != null) {
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
					if ("0x31435657".equals(formatStr)) {
						codecV = FormatConfiguration.VC1;
					} else if ("0x10000001".equals(formatStr)) {
						codecV = FormatConfiguration.MPEG1;
					} else if ("0x10000002".equals(formatStr)) {
						codecV = FormatConfiguration.MPEG2;
					} else {
						LOGGER.warn("Unknown video format value \"{}\"", formatStr);
					}
				}
			}
		}

		if (generateThumbnails) {
			try {
				String frameName = "" + this.hashCode();
				frameName = configuration.getTempFolder() + "/mplayer_thumbs/" + frameName + "00000001/00000001.jpg";
				frameName = frameName.replace(',', '_');
				File jpg = new File(frameName);

				if (jpg.exists()) {
					try (InputStream inputStream = new FileInputStream(jpg)) {
						getMedia().setThumb(DLNAThumbnail.toThumbnail(
							inputStream,
							640,
							480,
							ScaleType.MAX,
							ImageFormat.SOURCE,
							false
						));
					}

					if (!jpg.delete()) {
						jpg.deleteOnExit();
					}

					// Try and retry
					if (!jpg.getParentFile().delete() && !jpg.getParentFile().delete()) {
						LOGGER.debug("Failed to delete \"" + jpg.getParentFile().getAbsolutePath() + "\"");
					}
				}

				jpg = new File(frameName + "1.jpg");

				if (jpg.exists()) {
					if (!jpg.delete()) {
						jpg.deleteOnExit();
					}

					if (!jpg.getParentFile().delete()) {
						if (!jpg.getParentFile().delete()) {
							jpg.getParentFile().deleteOnExit();
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("Error during DVD ISO thumbnail retrieval: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}
		// No point in trying to re-parse the thumbnail later
		getMedia().setThumbready(true);

		length = nbsectors * 2048L;

		double d = 0;
		if (duration != null) {
			d = Double.parseDouble(duration);
		}

		getMedia().setAudioTracks(audioTracks);
		getMedia().setSubtitlesTracks(subtitles);

		if (duration != null) {
			getMedia().setDuration(d);
		}
		getMedia().setFrameRate(fps);
		getMedia().setAspectRatioDvdIso(aspect);
		getMedia().setDvdtrack(title);
		getMedia().setContainer(FormatConfiguration.ISO);
		getMedia().setCodecV(codecV != null ? codecV : FormatConfiguration.MPEG2);
		getMedia().setVideoTrackCount(1);

		try {
			getMedia().setWidth(Integer.parseInt(width));
		} catch (NumberFormatException nfe) {
			LOGGER.debug("Could not parse DVD video width \"{}\"", width);
		}

		try {
			getMedia().setHeight(Integer.parseInt(height));
		} catch (NumberFormatException nfe) {
			LOGGER.debug("Could not parse DVD video height \"{}\"", height);
		}

		getMedia().setMediaparsed(true);
	}

	public long getLength() {
		return length;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return (isBlank(parentName) ? "" : parentName + " ") + Messages.getString("Title") + " " + title;
	}

	@Override
	public String getSystemName() {
		return file.getAbsolutePath() + File.separator + "Title " + title;
	}

	@Override
	public String getFileName() {
		return file.getAbsolutePath();
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public boolean isValid() {
		if (getFormat() == null) {
			setFormat(FormatFactory.getFormat(ISOVOB.class));
		}
		return true;
	}

	@Override
	public long length() {
		return MediaInfo.TRANS_SIZE;
	}

	// Ditlew
	@Override
	public long length(Renderer renderer) {
		// WDTV Live at least, needs a realistic size for stop/resume to work properly. 2030879 = ((15000 + 256) * 1024 / 8 * 1.04) : 1.04 = overhead
		int cbrVideoBitrate = getDefaultRenderer().getCBRVideoBitrate();
		return (cbrVideoBitrate > 0) ? (long) (((cbrVideoBitrate + 256) * 1024 / (double) 8 * 1.04) * getMedia().getDurationInSeconds()) : length();
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		File cachedThumbnail = null;
		File thumbFolder = null;
		boolean alternativeCheck = false;
		while (cachedThumbnail == null) {
			if (thumbFolder == null) {
				thumbFolder = file.getParentFile();
			}

			cachedThumbnail = FileUtil.replaceExtension(thumbFolder, file, "jpg", true, true);

			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.replaceExtension(thumbFolder, file, "png", true, true);
			}

			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.getFileNameWithAddedExtension(thumbFolder, file, ".cover.jpg");
			}

			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.getFileNameWithAddedExtension(thumbFolder, file, ".cover.png");
			}

			if (alternativeCheck) {
				break;
			}

			if (StringUtils.isNotBlank(configuration.getAlternateThumbFolder())) {
				thumbFolder = new File(configuration.getAlternateThumbFolder());

				if (!thumbFolder.isDirectory()) {
					break;
				}
			}

			alternativeCheck = true;
		}

		if (cachedThumbnail != null) {
			return DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(cachedThumbnail));
		} else if (getMedia() != null && getMedia().getThumb() != null) {
			return getMedia().getThumbnailInputStream();
		} else {
			return getGenericThumbnailInputStream(null);
		}
	}

	protected MediaAudio parseMEncoderAudioStream(String line) {
		if (isBlank(line)) {
			return null;
		}
		Matcher matcher = AUDIO_STREAM_PATTERN.matcher(line);
		if (matcher.find()) {
			MediaAudio audio = new MediaAudio();
			try {
				audio.setTrack(Integer.parseInt(matcher.group("StreamNumber")));
			} catch (NumberFormatException e) {
				LOGGER.error(
					"Could not parse audio stream number \"{}\": {}",
					matcher.group("StreamNumber"),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			audio.setCodecA(MPlayerDvdAudioStreamTypes.typeOf(matcher.group("Codec")).getFormatConfigurationCode());
			audio.getAudioProperties().setNumberOfChannels(
				MPlayerDvdAudioStreamChannels.typeOf(matcher.group("Channels")).getNumberOfChannels()
			);
			String languageCode = Iso639.getISOCode(matcher.group("Language"));
			audio.setLang(isBlank(languageCode) ? MediaLang.UND : languageCode);
			try {
				audio.setId(Integer.parseInt(matcher.group("AID")));
			} catch (NumberFormatException e) {
				LOGGER.error("Could not parse audio id \"{}\": {}", matcher.group("AID"), e.getMessage());
				LOGGER.trace("", e);
			}
			if (audio.getId() >= MPlayerDvdAudioStreamTypes.FIRST_LPCM_AID) {
				if (!FormatConfiguration.LPCM.equals(audio.getCodecA())) {
					LOGGER.warn(
						"Unexpected error parsing DVD audio stream codec. AID dictates LPCM while codec is {}",
						audio.getId(),
						audio.getCodecA()
					);
				}
			} else if (audio.getId() >= MPlayerDvdAudioStreamTypes.FIRST_DTS_AID) {
				if (!FormatConfiguration.DTS.equals(audio.getCodecA())) {
					LOGGER.warn(
						"Unexpected error parsing DVD audio stream codec. AID dictates DTS while codec is {}",
						audio.getId(),
						audio.getCodecA()
					);
				}
			} else if (audio.getId() >= MPlayerDvdAudioStreamTypes.FIRST_AC3_AID) {
				if (!FormatConfiguration.AC3.equals(audio.getCodecA())) {
					LOGGER.warn(
						"Unexpected error parsing DVD audio stream codec. AID dictates AC3 while codec is {}",
						audio.getId(),
						audio.getCodecA()
					);
				}
			} else if (!FormatConfiguration.MP2.equals(audio.getCodecA())) {
				LOGGER.warn(
					"Unexpected error parsing DVD audio stream codec. AID dictates MP2 while codec is {}",
					audio.getId(),
					audio.getCodecA()
				);
			}
			/*
			 * MPlayer doesn't give information about bit rate, sampling rate
			 * and bit depth, so we'll have to "guess" the sample rate. Bit rate
			 * and bit depth are impossible to guess.
			 */
			switch (audio.getCodecA()) {
				case FormatConfiguration.LPCM:
					// Could be 48 kHz or 96 kHz, 48 kHz is most common
				case FormatConfiguration.DTS:
					// Could be 48 kHz or 96 kHz, 48 kHz is most common
				case FormatConfiguration.MP2:
					// Only 48 kHz is allowed;
				case FormatConfiguration.AC3:
					// Only 48 kHz is allowed;
					audio.getAudioProperties().setSampleFrequency(48000);
					break;
				default:
					// Shouldn't happen
			}
			return audio;
		}
		LOGGER.warn("Could not parse DVD audio stream \"{}\"", line);
		return null;
	}

	protected MediaSubtitle parseMEncoderSubtitleStream(String line) {
		if (isBlank(line)) {
			return null;
		}
		Matcher matcher = SUBTITLE_STREAM_PATTERN.matcher(line);
		if (matcher.find()) {
			MediaSubtitle subtitle = new MediaSubtitle();
			subtitle.setType(SubtitleType.UNKNOWN); // Not strictly true, but we lack a proper type
			try {
				subtitle.setId(Integer.parseInt(matcher.group("StreamNumber")));
			} catch (NumberFormatException e) {
				LOGGER.error(
					"Could not parse subtitle stream number \"{}\": {}",
					matcher.group("StreamNumber"),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}
			String languageCode = Iso639.getISOCode(matcher.group("Language"));
			subtitle.setLang(isBlank(languageCode) ? MediaLang.UND : languageCode);

			return subtitle;
		}
		LOGGER.warn("Could not parse DVD subtitle stream \"{}\"", line);
		return null;
	}

	@Override
	protected String getDisplayNameSuffix(Renderer renderer, UmsConfiguration configuration) {
		String nameSuffix = super.getDisplayNameSuffix(renderer, configuration);
		if (
			getMedia() != null &&
			renderer != null &&
			getMedia().getDurationInSeconds() > 0 &&
			renderer.isShowDVDTitleDuration()
		) {
			nameSuffix += " (" + StringUtil.convertTimeToString(getMedia().getDurationInSeconds(), "%01d:%02d:%02.0f") + ")";
		}

		return nameSuffix;
	}
}
