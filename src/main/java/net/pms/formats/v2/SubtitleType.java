/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  I. Sokolov
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
package net.pms.formats.v2;

import java.util.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Enum with possible types of subtitle tracks and methods for determining
 * them by file extension or libmediainfo output
 *
 * @since 1.60.0
 */
@SuppressWarnings("checkstyle:MethodParamPad")
public enum SubtitleType {
	// MediaInfo database of codec signatures https://github.com/MediaArea/MediaInfoLib/blob/master/Source/Resource/Text/DataBase/Codec.csv

	// SubtitleType(int index, String description, List<String> fileExtensions, List<String> libMediaInfoCodecs, int category)
	UNKNOWN     (0,  "Generic",                     "Unknown",     list(),             list(),                                                           Category.UNDEF),
	SUBRIP      (1,  "SubRip",                      "SubRip",      list("srt"),        list("S_TEXT/UTF8", "S_UTF8", "Subrip"),                          Category.TEXT),
	TEXT        (2,  "Text file",                   "Text",        list("txt"),        list(),                                                           Category.TEXT),
	MICRODVD    (3,  "MicroDVD",                    "MicroDVD",    list("sub"),        list(),                                                           Category.TEXT),
	SAMI        (4,  "SAMI",                        "SAMI",        list("smi"),        list(),                                                           Category.TEXT),
	ASS         (5,  "(Advanced) SubStation Alpha", "SSA",         list("ass", "ssa"), list("S_TEXT/SSA", "S_TEXT/ASS", "S_SSA", "S_ASS", "SSA", "ASS"), Category.TEXT),
	VOBSUB      (6,  "VobSub",                      "VobSub",      list("idx"),        list("S_VOBSUB", "subp", "mp4s", "E0", "RLE"),                    Category.PICTURE), // TODO: "RLE" may also apply to other formats
	UNSUPPORTED (7,  "Unsupported",                 "Unsupported", list(),             list(),                                                           Category.UNDEF),
	USF         (8,  "Universal Subtitle Format",   "USF",         list(),             list("S_TEXT/USF", "S_USF"),                                      Category.TEXT),
	BMP         (9,  "BMP",                         "BMP",         list(),             list("S_IMAGE/BMP"),                                              Category.PICTURE),
	DIVX        (10, "DIVX subtitles",              "DIVX",        list(),             list("DXSB"),                                                     Category.PICTURE),
	TX3G        (11, "Timed text (TX3G)",           "TX3G",        list(),             list("tx3g"),                                                     Category.TEXT),
	PGS         (12, "Blu-ray subtitles",           "PGS",         list(),             list("S_HDMV/PGS", "PGS", "144"),                                 Category.PICTURE),
	WEBVTT      (13, "Web Video Text Tracks",       "WebVTT",      list("vtt"),        list("WebVTT", "S_TEXT/WEBVTT"),                                  Category.TEXT),
	TEXTST      (14, "HDMV Text SubTitles",         "HDMV",        list(),             list("S_HDMV/TEXTST"),                                            Category.TEXT),
	DVBSUB      (15, "DVB Subtitles",               "DVB",         list(),             list("S_DVBSUB"),                                                 Category.PICTURE),
	EIA608      (16, "EIA-608 subtitles",           "EIA-608",     list(),             list("EIA-608"),                                                  Category.TEXT);
//	EIA708      (17, "EIA-708 subtitles",           "EIA-708",     list(),             list("EIA-708"),                                                  Category.TEXT);

	public static enum Category {
		/** Text based subtitles */
		TEXT,

		/** Image based subtitles */
		PICTURE,

		/** Undefined category */
		UNDEF
	}

	private final int index;
	private final String description;
	private final String shortName;
	private final List<String> fileExtensions;
	private final List<String> libMediaInfoCodecs;
	private final Category category;

	private final static Map<Integer, SubtitleType> STABLE_INDEX_TO_SUBTITLE_TYPE_MAP;
	private final static Map<String, SubtitleType> FILE_EXTENSION_TO_SUBTITLE_TYPE_MAP;
	private final static Map<String, SubtitleType> LIBMEDIAINFO_CODEC_TO_SUBTITLE_TYPE_MAP;

	/**
	 * A constant {@link Set} of lower-case file extensions for supported
	 * subtitles types
	 */
	public final static Set<String> SUPPORTED_FILE_EXTENSIONS;
	private static List<String> list(String... args) {
		return new ArrayList<>(Arrays.asList(args));
	}

	static {
		STABLE_INDEX_TO_SUBTITLE_TYPE_MAP = new HashMap<>();
		FILE_EXTENSION_TO_SUBTITLE_TYPE_MAP = new HashMap<>();
		LIBMEDIAINFO_CODEC_TO_SUBTITLE_TYPE_MAP = new HashMap<>();
		for (SubtitleType subtitleType : values()) {
			STABLE_INDEX_TO_SUBTITLE_TYPE_MAP.put(subtitleType.getStableIndex(), subtitleType);
			for (String fileExtension : subtitleType.fileExtensions) {
				FILE_EXTENSION_TO_SUBTITLE_TYPE_MAP.put(fileExtension.toLowerCase(Locale.ROOT), subtitleType);
			}
			for (String codec : subtitleType.libMediaInfoCodecs) {
				LIBMEDIAINFO_CODEC_TO_SUBTITLE_TYPE_MAP.put(codec.toLowerCase(Locale.ROOT), subtitleType);
			}
		}
		SUPPORTED_FILE_EXTENSIONS = Collections.unmodifiableSet(
			new LinkedHashSet<>(FILE_EXTENSION_TO_SUBTITLE_TYPE_MAP.keySet())
		);
	}

	public static SubtitleType valueOfStableIndex(int stableIndex) {
		SubtitleType subtitleType = STABLE_INDEX_TO_SUBTITLE_TYPE_MAP.get(stableIndex);
		if (subtitleType == null) {
			subtitleType = UNKNOWN;
		}
		return subtitleType;
	}

	public static SubtitleType valueOfFileExtension(String fileExtension) {
		if (isBlank(fileExtension)) {
			return UNKNOWN;
		}
		SubtitleType subtitleType = FILE_EXTENSION_TO_SUBTITLE_TYPE_MAP.get(fileExtension.toLowerCase());
		if (subtitleType == null) {
			subtitleType = UNKNOWN;
		}
		return subtitleType;
	}

	public static SubtitleType valueOfMediaInfoValue(String value) {
		return valueOfMediaInfoValue(value, UNKNOWN);
	}

	public static SubtitleType valueOfMediaInfoValue(String value, SubtitleType defaultType) {
		if (isBlank(value)) {
			return defaultType;
		}
		SubtitleType subtitleType = LIBMEDIAINFO_CODEC_TO_SUBTITLE_TYPE_MAP.get(
			trim(value).toLowerCase(Locale.ROOT)
		);
		if (subtitleType == null) {
			subtitleType = defaultType;
		}
		return subtitleType;
	}

	public static Set<String> getSupportedFileExtensions() {
		return SUPPORTED_FILE_EXTENSIONS;
	}

	/**
	 * Creates a new instance with the specified parameters.
	 *
	 * @param index the subtitle type index.
	 * @param description the full description.
	 * @param shortName the abbreviated description.
	 * @param fileExtensions a {@link List} of possible file extensions.
	 * @param libMediaInfoCodecs a {@link List} of MediaInfo codec names.
	 * @param category the {@link Category} for the subtitle type.
	 */
	private SubtitleType(
		int index,
		String description,
		String shortName,
		List<String>
		fileExtensions,
		List<String> libMediaInfoCodecs,
		Category category
	) {
		this.index = index;
		this.description = description;
		this.shortName = shortName;
		this.fileExtensions = fileExtensions;
		this.libMediaInfoCodecs = libMediaInfoCodecs;
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public String getShortName() {
		return shortName;
	}

	public String getExtension() {
		if (fileExtensions.isEmpty()) {
			return "";
		}
		return fileExtensions.get(0);
	}

	public int getStableIndex() {
		return index;
	}

	public boolean isText() {
		return category == Category.TEXT;
	}

	public boolean isPicture() {
		return category == Category.PICTURE;
	}
}
