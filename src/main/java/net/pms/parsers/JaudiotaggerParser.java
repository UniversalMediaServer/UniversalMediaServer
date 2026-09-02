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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.openhft.hashing.LongHashFunction;
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
import net.pms.store.AudioCoverResolver;
import net.pms.store.MediaStore;
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
import org.jaudiotagger.audio.exceptions.CannotWriteException;
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

	// Thumbnail ids of covers already stored, by the hash of the raw image.
	private static final int COVER_CACHE_SIZE = 64;
	private static final LongHashFunction COVER_HASH = LongHashFunction.xx3();
	private static final Map<Long, Long> COVER_THUMBNAIL_IDS = Collections.synchronizedMap(new CoverThumbnailCache());

	// Access ordered map that drops the least recently used cover once it is full.
	private static class CoverThumbnailCache extends LinkedHashMap<Long, Long> {

		private static final long serialVersionUID = 1L;

		CoverThumbnailCache() {
			super(16, 0.75f, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest) {
			return size() > COVER_CACHE_SIZE;
		}
	}
	public static final String PARSER_NAME = "JAUDIO";
	private static final String MULTI_VALUE_SEPARATOR = " / ";

	private static final Pattern BIT_DEPTH_SUFFIX = Pattern.compile("\\s+(\\d{1,2})\\s*bits?$");

	// The container belongs to the file type, not to the audio header JAudiotagger describes.
	private static final Map<Format.Identifier, String> CONTAINER_BY_IDENTIFIER = Map.ofEntries(
		Map.entry(Format.Identifier.ADPCM, FormatConfiguration.ADPCM),
		Map.entry(Format.Identifier.ADTS, FormatConfiguration.ADTS),
		Map.entry(Format.Identifier.AIFF, FormatConfiguration.AIFF),
		Map.entry(Format.Identifier.APE, FormatConfiguration.MONKEYS_AUDIO),
		Map.entry(Format.Identifier.ATRAC, FormatConfiguration.ATRAC),
		Map.entry(Format.Identifier.AU, FormatConfiguration.AU),
		Map.entry(Format.Identifier.DFF, FormatConfiguration.DFF),
		Map.entry(Format.Identifier.DSF, FormatConfiguration.DSF),
		Map.entry(Format.Identifier.FLAC, FormatConfiguration.FLAC),
		Map.entry(Format.Identifier.M4A, FormatConfiguration.M4A),
		Map.entry(Format.Identifier.MKA, FormatConfiguration.MKA),
		Map.entry(Format.Identifier.MLP, FormatConfiguration.MLP),
		Map.entry(Format.Identifier.MP3, FormatConfiguration.MP3),
		Map.entry(Format.Identifier.MPA, FormatConfiguration.MPA),
		Map.entry(Format.Identifier.MPC, FormatConfiguration.MPC),
		Map.entry(Format.Identifier.OGA, FormatConfiguration.OGA),
		Map.entry(Format.Identifier.RA, FormatConfiguration.RA),
		Map.entry(Format.Identifier.SHN, FormatConfiguration.SHORTEN),
		Map.entry(Format.Identifier.THREEGA, FormatConfiguration.THREEGA),
		Map.entry(Format.Identifier.THREEG2A, FormatConfiguration.THREEGPP2),
		Map.entry(Format.Identifier.TTA, FormatConfiguration.TTA),
		Map.entry(Format.Identifier.WAV, FormatConfiguration.WAV),
		Map.entry(Format.Identifier.WMA, FormatConfiguration.WMA),
		Map.entry(Format.Identifier.WV, FormatConfiguration.WAVPACK));

	/**
	 * This class is not meant to be instantiated.
	 */
	private JaudiotaggerParser() {
	}

	/**
	 * Translates JAudiotagger's prose description of the audio header ("FLAC 24 bits", "MPEG-1 Layer 3")
	 * into the vocabulary the renderer "Supported" lines are written in. Anything unrecognized is passed
	 * through unchanged.
	 */
	private static String normalizeCodec(String encodingType) {
		String value = StringUtils.trimToNull(encodingType);
		if (value == null) {
			return null;
		}
		value = value.toLowerCase(Locale.ROOT);
		if (value.contains("(windows media")) {
			value = value.substring(0, value.indexOf("(windows media")).trim();
		}
		value = BIT_DEPTH_SUFFIX.matcher(value).replaceFirst("").trim();
		if (value.startsWith("flac")) {
			return FormatConfiguration.FLAC;
		} else if (value.startsWith("alac") || value.startsWith("apple lossless")) {
			return FormatConfiguration.ALAC;
		} else if (value.startsWith("aac")) {
			return FormatConfiguration.AAC_LC;
		} else if (value.equals("mp3") || value.contains("layer 3")) {
			return FormatConfiguration.MP3;
		} else if (value.contains("layer 2")) {
			return FormatConfiguration.MP2;
		} else if (value.contains("layer 1")) {
			return FormatConfiguration.MPA;
		} else if (value.contains("vorbis")) {
			return FormatConfiguration.VORBIS;
		} else if (value.contains("opus")) {
			return FormatConfiguration.OPUS;
		} else if (value.startsWith("wavpack")) {
			return FormatConfiguration.WAVPACK;
		} else if (value.startsWith("wav") || value.contains("pcm")) {
			return FormatConfiguration.LPCM;
		} else if (value.startsWith("monkey")) {
			return FormatConfiguration.MONKEYS_AUDIO;
		} else if (value.startsWith("aiff")) {
			return FormatConfiguration.AIFF;
		} else if (value.startsWith("asf") || value.startsWith("windows media")) {
			return FormatConfiguration.WMA;
		}
		return value;
	}

	private static String getContainer(Format format, String codec) {
		if (format != null && format.getIdentifier() != null) {
			String container = CONTAINER_BY_IDENTIFIER.get(format.getIdentifier());
			if (container != null) {
				return container;
			}
		}
		return codec;
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

					Matcher bitDepth = BIT_DEPTH_SUFFIX.matcher(StringUtils.lowerCase(StringUtils.trimToEmpty(ah.getEncodingType())));
					if (bitDepth.find()) {
						audio.setBitDepth(Integer.parseInt(bitDepth.group(1)));
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
						audio.setCodec(normalizeCodec(ah.getEncodingType()));
					}
				}

				Tag t = af.getTag();
				if (t != null) {
					Long thumbId = getThumbnailId(media, t, file);
					if (thumbId != null) {
						media.setThumbnailId(thumbId);
					}
					audioMetadata.setAlbum(extractAudioTagKeyValue(t, FieldKey.ALBUM));
					audioMetadata.setArtist(extractAudioTagKeyValue(t, FieldKey.ARTIST));
					audioMetadata.setAlbumArtist(extractAudioTagKeyValue(t, FieldKey.ALBUM_ARTIST));
					audioMetadata.setComposer(extractAudioTagKeyValue(t, FieldKey.COMPOSER));
					audioMetadata.setConductor(extractAudioTagKeyValue(t, FieldKey.CONDUCTOR));
					audioMetadata.setSongname(extractAudioTagKeyValue(t, FieldKey.TITLE));
					audioMetadata.setMbidRecord(extractAudioTagKeyValue(t, FieldKey.MUSICBRAINZ_RELEASEID));
					audioMetadata.setMbidTrack(extractAudioTagKeyValue(t, FieldKey.MUSICBRAINZ_TRACK_ID));
					audioMetadata.setRating(convertTagRatingToStar(t));
					audioMetadata.setGenre(extractAudioTagKeyValues(t, FieldKey.GENRE));
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
			} catch (Exception e) {
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
				media.setContainer(getContainer(format, audio.getCodec()));
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
			addDiscogsID(af, audioMetadata);
			addAudioTrackRating(af, audioMetadata);
		} catch (Exception e) {
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
			} catch (Exception e) {
				LOGGER.debug("Error parsing audio file tag for \"{}\": {}", file.getName(), e.getMessage());
				LOGGER.trace("", e);
			}
		}
		return null;
	}

	/**
	 * Resolves the stored thumbnail for the cover of this track.
	 *
	 * The hashing inside the thumbnail table happen once per cover instead of once per track. Hashing the raw bytes rather than the album
	 * name keeps that exact - equal bytes are the same picture, whatever the tags say.
	 */
	private static Long getThumbnailId(MediaInfo media, Tag t, File file) {
		Cover cover = getCover(t, false);
		if (cover == null) {
			return null;
		}
		Long hash = COVER_HASH.hashBytes(cover.bytes());
		Long cachedId = COVER_THUMBNAIL_IDS.get(hash);
		if (cachedId != null) {
			media.setThumbnailSource(cover.source());
			return cachedId;
		}
		if (MediaStore.isServingRequest()) {
			media.setThumbnailPending(true);
			AudioCoverResolver.enqueueCover(file);
			return null;
		}
		DLNAThumbnail thumbnail = toThumbnail(media, cover.bytes());
		if (thumbnail == null) {
			return null;
		}
		Long id = ThumbnailStore.getId(thumbnail);
		if (id != null) {
			COVER_THUMBNAIL_IDS.put(hash, id);
		}
		media.setThumbnailSource(cover.source());
		return id;
	}

	private static DLNAThumbnail getThumbnail(MediaInfo media, Tag t) {
		Cover cover = getCover(t, true);
		if (cover == null) {
			return null;
		}
		DLNAThumbnail thumbnail = toThumbnail(media, cover.bytes());
		if (thumbnail != null) {
			media.setThumbnailSource(cover.source());
		}
		return thumbnail;
	}

	private static DLNAThumbnail toThumbnail(MediaInfo media, byte[] cover) {
		try {
			return DLNAThumbnail.toThumbnail(cover, 640, 480, ScaleType.MAX, ImageFormat.SOURCE, false);
		} catch (IOException e) {
			LOGGER.debug("Error parsing audio artwork for \"{}\": {}", media.getTitle(), e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * The raw cover of a track together with where it came from.
	 */
	private record Cover(byte[] bytes, ThumbnailSource source) { }

	/**
	 * @return the embedded cover.
	 */
	private static Cover getCover(Tag t, boolean allowRemoteLookup) {
		if (t == null) {
			return null;
		}
		if (!t.getArtworkList().isEmpty()) {
			byte[] cover = t.getArtworkList().get(0).getBinaryData();
			if (cover != null && cover.length > 0) {
				return new Cover(cover, ThumbnailSource.EMBEDDED);
			}
		}
		if (allowRemoteLookup && isRemoteCoverLookupConfigured()) {
			byte[] cover = CoverUtil.get().getThumbnail(t);
			if (cover != null && cover.length > 0) {
				return new Cover(cover, ThumbnailSource.MUSICBRAINZ);
			}
		}
		return null;
	}

	private static boolean isRemoteCoverLookupConfigured() {
		return CONFIGURATION.getAudioThumbnailMethod().equals(CoverSupplier.COVER_ART_ARCHIVE);
	}

	/**
	 * Joins all values of a repeated field, a tag with GENRE=Baroque and GENRE=Classical keeps both.
	 */
	private static String extractAudioTagKeyValues(Tag t, FieldKey key) {
		try {
			Set<String> values = new LinkedHashSet<>();
			for (String value : t.getAll(key)) {
				if (StringUtils.isNotBlank(value)) {
					values.add(value.trim());
				}
			}
			return values.isEmpty() ? null : String.join(MULTI_VALUE_SEPARATOR, values);
		} catch (KeyNotFoundException e) {
			LOGGER.trace("tag field not found", e);
			return null;
		}
	}

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
			LOGGER.trace("audio musicBrainz tag not parsed", e);
		}
	}

	private static void addDiscogsID(AudioFile af, MediaAudioMetadata audioMetadata) {
		try {
			Tag t = af.getTag();
			if (t != null) {
				String val = t.getFirst(FieldKey.URL_DISCOGS_RELEASE_SITE);
				if (StringUtils.isNotBlank(val)) {
					val = val.substring(val.lastIndexOf("/") + 1);
				}
				if (StringUtils.isNotBlank(val)) {
					Long discogsId = Long.parseLong(val);
					audioMetadata.setDiscogsReleaseId(discogsId);
				}
			}
		} catch (UnsupportedOperationException | KeyNotFoundException e) {
			LOGGER.trace("audio discogs tag not parsed", e);
		}
	}

	private static void addAudioTrackRating(AudioFile af, MediaAudioMetadata audioMetadata) {
		try {
			Tag t = af.getTag();
			if (t != null) {
				audioMetadata.setRating(convertTagRatingToStar(t));
			}
		} catch (Exception e) {
			LOGGER.trace("audio rating tag not parsed.", e);
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

	/**
	 * Writes a 0-5 star rating into the tag of an audio file, or removes the
	 * rating field when RATINGINSTARS is NULL.
	 *
	 * <pre>
	 *
	 * ID3v2 Tags support:
	 * =======================================
	 *
	 * There is a "Popularimeter" frame in the ID3v2 specification meant for this purpose.
	 * The frame is called POPM and Windows Explorer, Windows Media Player, Winamp, foobar2000, MediaMonkey,
	 * and other software all map roughly the same ranges of 0–255 to a 0–5 stars value for display.
	 *
	 * The following list details how Windows Explorer reads and writes the POPM frame:
	 *
	 * 224–255 = 5 stars when READ with Windows Explorer, writes 255
	 * 160–223 = 4 stars when READ with Windows Explorer, writes 196
	 * 096-159 = 3 stars when READ with Windows Explorer, writes 128
	 * 032-095 = 2 stars when READ with Windows Explorer, writes 64
	 * 001-031 = 1 star when READ with Windows Explorer, writes 1
	 *
	 *
	 * Vorbis
	 * =======================================
	 *
	 *  Ratings are usually mapped as 1-5 stars with 20,40,60,80,100 as the actual string values.
	 *
	 * </pre>
	 *
	 * @param filename the audio file to update
	 * @param ratingInStars the rating (0 - 5 stars), or NULL to remove the rating
	 * @return true if the file was actually rewritten, which changes its content hash
	 */
	public static boolean writeRatingToFile(String filename, Integer ratingInStars) {
		if (StringUtils.isEmpty(filename)) {
			LOGGER.warn("cannot update rating in file. Filename is empty or NULL");
			return false;
		}
		try {
			AudioFile audioFile = AudioFileIO.read(new File(filename));
			Tag tag = audioFile.getTag();
			if (tag == null) {
				LOGGER.warn("cannot update rating in file \"{}\". No tag found.", filename);
				return false;
			}
			if (ratingInStars == null) {
				tag.deleteField(FieldKey.RATING);
			} else {
				tag.setField(FieldKey.RATING, convertStarsToTagValue(tag, ratingInStars));
			}
			audioFile.commit();
			return true;
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException e) {
			LOGGER.warn("Error writing Tag info.", e);
			return false;
		}
	}

	/**
	 * Converts a 0-5 star rating to the tag format specific value.
	 *
	 * @param tag the tag to update
	 * @param stars number of stars (0 - 5)
	 * @return the tag specific rating value
	 */
	private static String convertStarsToTagValue(Tag tag, Integer stars) {
		int num;
		if (tag instanceof FlacTag || tag instanceof VorbisCommentTag) {
			num = convertStarsToVorbis(stars);
		} else if (tag instanceof AbstractID3v2Tag || tag instanceof ID3v11Tag) {
			num = convertStarsToID3(stars);
		} else {
			// Don't know ... maybe we use vorbis tags by default
			num = convertStarsToVorbis(stars);
		}
		return Integer.toString(num);
	}

	/**
	 * Converts 0-5 stars to the ID3 POPM value.
	 */
	private static int convertStarsToID3(int rating) {
		return switch (rating) {
			case 0 -> 0;
			case 1 -> 1;
			case 2 -> 64;
			case 3 -> 128;
			case 4 -> 196;
			default -> 255;
		};
	}

	/**
	 * Converts 0-5 stars to the VORBIS tag value.
	 */
	private static int convertStarsToVorbis(int rating) {
		return rating * 20;
	}

}
