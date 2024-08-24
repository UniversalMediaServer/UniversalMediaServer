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
import java.io.IOException;
import java.util.Locale;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.musicbrainz.coverart.CoverUtil;
import net.pms.formats.Format;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.CoverSupplier;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaudiotaggerParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(JaudiotaggerParser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final String PARSER_NAME = "JAUDIO";

	/**
	 * This class is not meant to be instantiated.
	 */
	private JaudiotaggerParser() {
	}

	public static void parse(MediaInfo media, File file, Format format) {
		if (file != null) {
			media.setSize(file.length());
			MediaAudio audio = new MediaAudio();
			MediaAudioMetadata audioMetadata = new MediaAudioMetadata();
			try {
				AudioFile af;
				if ("mp2".equalsIgnoreCase(FileUtil.getExtension(file))) {
					af = AudioFileIO.readAs(file, "mp3");
				} else {
					af = AudioFileIO.read(file);
				}
				AudioHeader ah = af.getAudioHeader();

				if (ah != null) {
					int length = ah.getTrackLength();
					int rate = ah.getSampleRateAsNumber();

					if (ah.getEncodingType() != null && ah.getEncodingType().toLowerCase().contains("flac 24")) {
						audio.setBitDepth(24);
					}

					audio.setSampleRate(rate);
					media.setDuration((double) length);
					media.setBitRate((int) ah.getBitRateAsNumber());

					audio.setNumberOfChannels(2); // set default value of channels to 2
					String channels = ah.getChannels().toLowerCase(Locale.ROOT);
					if (StringUtils.isNotBlank(channels)) {
						if (channels.equals("1") || channels.contains("mono")) { // parse value "1" or "Mono"
							audio.setNumberOfChannels(1);
						} else if (!(channels.equals("2") || channels.equals("0") || channels.contains("stereo"))) {
							// No need to parse stereo as it's set as default
							try {
								audio.setNumberOfChannels(Integer.parseInt(channels));
							} catch (IllegalArgumentException e) {
								LOGGER.debug("Could not parse number of audio channels from \"{}\"", channels);
							}
						}
					}

					if (StringUtils.isNotBlank(ah.getEncodingType())) {
						audio.setCodec(ah.getEncodingType());
					}

					if (audio.getCodec() != null && audio.getCodec().contains("(windows media")) {
						audio.setCodec(audio.getCodec().substring(0, audio.getCodec().indexOf("(windows media")).trim());
					}
				}

				Tag t = af.getTag();
				if (t != null) {
					DLNAThumbnail thumbnail = getThumbnail(media, t);
					if (thumbnail != null) {
						Long thumbId = ThumbnailStore.getId(thumbnail);
						media.setThumbnailId(thumbId);
					}
					audioMetadata.setAlbum(extractAudioTagKeyValue(t, FieldKey.ALBUM));
					audioMetadata.setArtist(extractAudioTagKeyValue(t, FieldKey.ARTIST));
					audioMetadata.setComposer(extractAudioTagKeyValue(t, FieldKey.COMPOSER));
					audioMetadata.setConductor(extractAudioTagKeyValue(t, FieldKey.CONDUCTOR));
					audioMetadata.setSongname(extractAudioTagKeyValue(t, FieldKey.TITLE));
					audioMetadata.setMbidRecord(extractAudioTagKeyValue(t, FieldKey.MUSICBRAINZ_RELEASEID));
					audioMetadata.setMbidTrack(extractAudioTagKeyValue(t, FieldKey.MUSICBRAINZ_TRACK_ID));
					audioMetadata.setRating(convertTagRatingToStar(t));
					audioMetadata.setGenre(extractAudioTagKeyValue(t, FieldKey.GENRE));
					audioMetadata.setDisc(extractAudioTagKeyIntegerValue(t, FieldKey.DISC_NO, 1));

					String keyyear = extractAudioTagKeyValue(t, FieldKey.YEAR);
					if (keyyear != null) {
						if (keyyear.length() > 4) {
							// Extract just the year, skipping  '-month-day'
							keyyear = keyyear.substring(0, 4);
						}
						if (NumberUtils.isParsable(keyyear)) {
							audioMetadata.setYear(Integer.parseInt(keyyear));
						}
					}

					Integer trackNum = extractAudioTagKeyIntegerValue(t, FieldKey.TRACK, 1);
					audioMetadata.setTrack(trackNum);
				}
			} catch (CannotReadException e) {
				if (e.getMessage().startsWith(
					ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().substring(0, ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().indexOf("{"))
				)) {
					LOGGER.debug("No audio tag support for audio file \"{}\"", file.getName());
				} else {
					LOGGER.error("Error reading audio tag for \"{}\": {}", file.getName(), e.getMessage());
					LOGGER.trace("", e);
				}
			} catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | NumberFormatException | KeyNotFoundException e) {
				LOGGER.debug("Error parsing audio file tag for \"{}\": {}", file.getName(), e.getMessage());
				LOGGER.trace("", e);
			}

			// Set container for formats that the normal parsing fails to do from Format
			if (StringUtils.isBlank(media.getContainer()) && format != null && format.getIdentifier() != null) {
				switch (format.getIdentifier()) {
					case ADPCM -> audio.setCodec(FormatConfiguration.ADPCM);
					case DSF -> audio.setCodec(FormatConfiguration.DSF);
					case DFF -> audio.setCodec(FormatConfiguration.DFF);
					default -> {
						//nothing to do
					}
				}
			}

			if (StringUtils.isBlank(audioMetadata.getSongname())) {
				audioMetadata.setSongname(file.getName());
			}

			media.setAudioMetadata(audioMetadata);
			media.addAudioTrack(audio);
			if (StringUtils.isBlank(media.getContainer())) {
				media.setContainer(audio.getCodec());
			}
			Parser.postParse(media, Format.AUDIO);
			media.setMediaParser(PARSER_NAME);
		}
	}

	public static void parse(File file, MediaAudioMetadata audioMetadata) {
		try {
			AudioFile af;
			String extension = FileUtil.getExtension(file);
			if (extension != null && "mp2".equals(extension.toLowerCase(Locale.ROOT))) {
				af = AudioFileIO.readAs(file, "mp3");
			} else {
				af = AudioFileIO.read(file);
			}
			addMusicBrainzIDs(af, audioMetadata);
			addAudioTrackRating(af, audioMetadata);
		} catch (IOException | CannotReadException | InvalidAudioFrameException | ReadOnlyFileException | TagException e) {
			LOGGER.debug("Could not parse audio file");
		}
	}

	public static DLNAThumbnail getThumbnail(MediaInfo media, File file) {
		if (file != null) {
			try {
				AudioFile af;
				if ("mp2".equalsIgnoreCase(FileUtil.getExtension(file))) {
					af = AudioFileIO.readAs(file, "mp3");
				} else {
					af = AudioFileIO.read(file);
				}
				Tag t = af.getTag();
				return getThumbnail(media, t);
			} catch (CannotReadException e) {
				if (e.getMessage().startsWith(
					ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().substring(0, ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().indexOf("{"))
				)) {
					LOGGER.debug("No audio tag support for audio file \"{}\"", file.getName());
				} else {
					LOGGER.error("Error reading audio tag for \"{}\": {}", file.getName(), e.getMessage());
					LOGGER.trace("", e);
				}
			} catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | NumberFormatException | KeyNotFoundException e) {
				LOGGER.debug("Error parsing audio file tag for \"{}\": {}", file.getName(), e.getMessage());
				LOGGER.trace("", e);
			}
		}
		return null;
	}

	private static DLNAThumbnail getThumbnail(MediaInfo media, Tag t) {
		if (t != null) {
			if (!t.getArtworkList().isEmpty()) {
				byte[] cover = t.getArtworkList().get(0).getBinaryData();
				if (cover != null && cover.length > 0) {
					try {
						DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(
							cover,
							640,
							480,
							ScaleType.MAX,
							ImageFormat.SOURCE,
							false
						);
						if (thumbnail != null) {
							media.setThumbnailSource(ThumbnailSource.EMBEDDED);
							return thumbnail;
						}
					} catch (IOException e) {
						LOGGER.debug("Error parsing embedded audio artwork for \"{}\": {}", media.getTitle(), e.getMessage());
						LOGGER.trace("", e);
					}
				}
			}
			if (CONFIGURATION.getAudioThumbnailMethod().equals(CoverSupplier.COVER_ART_ARCHIVE)) {
				byte[] cover = CoverUtil.get().getThumbnail(t);
				if (cover != null && cover.length > 0) {
					try {
						DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(
							cover,
							640,
							480,
							ScaleType.MAX,
							ImageFormat.SOURCE,
							false
						);
						if (thumbnail != null) {
							media.setThumbnailSource(ThumbnailSource.MUSICBRAINZ);
							return thumbnail;
						}
					} catch (IOException e) {
						LOGGER.debug("Error parsing cover art archive audio artwork for \"{}\": {}", media.getTitle(), e.getMessage());
						LOGGER.trace("", e);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Extracts key value.
	 *
	 * @param key
	 * @return If key is not available or blanc, NULL will be returned, otherwise string key value
	 */
	private static String extractAudioTagKeyValue(Tag t, FieldKey key) {
		try {
			String value = t.getFirst(key);
			if (StringUtils.isAllBlank(value)) {
				LOGGER.trace("tag field is blanc");
				return null;
			}
			return value;
		} catch (KeyNotFoundException e) {
			LOGGER.trace("tag field not found", e);
			return null;
		}
	}

	/**
	 * Extracts key value and converts it to Integer.
	 *
	 * @param t
	 * @param key
	 * @param defaultValue
	 * @return	If key is not available or blanc, defaultValue will be returned
	 */
	private static Integer extractAudioTagKeyIntegerValue(Tag t, FieldKey key, Integer defaultValue) {
		String value = extractAudioTagKeyValue(t, key);
		if (value != null) {
			try {
				return Integer.valueOf(value);
			} catch (NumberFormatException e) {
				LOGGER.trace("no int value available for key ", e);
			}
		}
		return defaultValue;
	}

	private static void addMusicBrainzIDs(AudioFile af, MediaAudioMetadata audioMetadata) {
		try {
			Tag t = af.getTag();
			if (t != null) {
				String val = t.getFirst(FieldKey.MUSICBRAINZ_RELEASEID);
				audioMetadata.setMbidRecord(val.isEmpty() ? null : val);
				val = t.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID);
				audioMetadata.setMbidTrack(val.isEmpty() ? null : val);
			}
		} catch (UnsupportedOperationException | KeyNotFoundException e) {
			LOGGER.trace("audio musicBrainz tag not parsed: " + e.getMessage());
		}
	}

	private static void addAudioTrackRating(AudioFile af, MediaAudioMetadata audioMetadata) {
		try {
			Tag t = af.getTag();
			if (t != null) {
				audioMetadata.setRating(convertTagRatingToStar(t));
			}
		} catch (Exception e) {
			LOGGER.trace("audio rating tag not parsed: " + e.getMessage());
		}
	}

	/**
	 * Converts TAG values read from file to 0-5 stars
	 *
	 * @param tag
	 */
	private static Integer convertTagRatingToStar(Tag tag) {
		try {
			if (tag == null) {
				return null;
			}

			String value = tag.getFirst(FieldKey.RATING);
			if (!StringUtils.isBlank(value)) {
				int num = Integer.parseInt(value);
				if (tag instanceof FlacTag || tag instanceof VorbisCommentTag) {
					return convertVorbisToStars(num);
				} else if (tag instanceof AbstractID3v2Tag || tag instanceof ID3v11Tag) {
					return convertID3ToStars(num);
				} else {
					// Don't know ... maybe we use vorbis tags by default
					return convertVorbisToStars(num);
				}
			}
		} catch (NumberFormatException | KeyNotFoundException e) {
			// Value couldn't be read.
			LOGGER.trace("conversion error", e);
		}
		return null;
	}

	private static Integer convertID3ToStars(Integer num) {
		if (num == null) {
			return null;
		}
		if (num == 0) {
			return 0;
		} else if (num < 32) {
			return 1;
		} else if (num < 96) {
			return 2;
		} else if (num < 160) {
			return 3;
		} else if (num < 224) {
			return 4;
		} else {
			return 5;
		}
	}

	private static Integer convertVorbisToStars(Integer num) {
		if (num == null) {
			return null;
		}
		if (num == 0) {
			return 0;
		} else if (num < 21) {
			return 1;
		} else if (num < 41) {
			return 2;
		} else if (num < 61) {
			return 3;
		} else if (num < 81) {
			return 4;
		} else {
			return 5;
		}
	}

}
