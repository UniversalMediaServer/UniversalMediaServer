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
public enum SubtitleType {
	// MediaInfo database of codec signatures (not comprehensive)
	// http://mediainfo.svn.sourceforge.net/viewvc/mediainfo/MediaInfoLib/trunk/Source/Resource/Text/DataBase/

	// SubtitleType(int index, String description, List<String> fileExtensions, List<String> libMediaInfoCodecs, int category)
	UNKNOWN     (0,  "Generic",                     list(),             list(),                                                           Type.UNDEFINED),
	SUBRIP      (1,  "SubRip",                      list("srt"),        list("S_TEXT/UTF8", "S_UTF8", "Subrip"),                          Type.TEXT),
	TEXT        (2,  "Text file",                   list("txt"),        list(),                                                           Type.TEXT),
	MICRODVD    (3,  "MicroDVD",                    list("sub"),        list(),                                                           Type.TEXT),
	SAMI        (4,  "SAMI",                        list("smi"),        list(),                                                           Type.TEXT),
	ASS         (5,  "(Advanced) SubStation Alpha", list("ass", "ssa"), list("S_TEXT/SSA", "S_TEXT/ASS", "S_SSA", "S_ASS", "SSA", "ASS"), Type.TEXT),
	VOBSUB      (6,  "VobSub",                      list("idx"),        list("S_VOBSUB", "subp", "mp4s", "E0", "RLE"),                    Type.PICTURE), // TODO: "RLE" may also apply to other formats
	UNSUPPORTED (7,  "Unsupported",                 list(),             list(),                                                           Type.UNDEFINED),
	USF         (8,  "Universal Subtitle Format",   list(),             list("S_TEXT/USF", "S_USF"),                                      Type.TEXT),
	BMP         (9,  "BMP",                         list(),             list("S_IMAGE/BMP"),                                              Type.PICTURE),
	DIVX        (10, "DIVX subtitles",              list(),             list("DXSB"),                                                     Type.PICTURE),
	TX3G        (11, "Timed text (TX3G)",           list(),             list("tx3g"),                                                     Type.TEXT),
	PGS         (12, "Blu-ray subtitles",           list(),             list("S_HDMV/PGS", "PGS", "144"),                                 Type.PICTURE),
	WEBVTT      (13, "WebVTT",                      list("vtt"),        list("WebVTT"),                                                   Type.TEXT);
//	EIA_608		(14, "CEA-608",						list(),				list("EIA-608"),										  		  type.TEXT),
//	EIA_708		(14, "CEA-708",						list(),				list("EIA-708"),										  		  type.TEXT);

	public enum Type {
		TEXT,
		PICTURE,
		UNDEFINED
	}

	private final int index;
	private final String description;
	private final List<String> fileExtensions;
	private final List<String> libMediaInfoCodecs;
	private final Type category;

	private final static Map<Integer, SubtitleType> stableIndexToSubtitleTypeMap;
	private final static Map<String, SubtitleType> fileExtensionToSubtitleTypeMap;
	private final static Map<String, SubtitleType> libmediainfoCodecToSubtitleTypeMap;

	/**
	 * A constant {@link Set} of lower-case file extensions for supported
	 * subtitles types
	 */
	public final static Set<String> SUPPORTED_FILE_EXTENSIONS;
	private static List<String> list(String... args) {
		return new ArrayList<>(Arrays.asList(args));
	}

	static {
		stableIndexToSubtitleTypeMap = new HashMap<>();
		fileExtensionToSubtitleTypeMap = new HashMap<>();
		libmediainfoCodecToSubtitleTypeMap = new HashMap<>();
		for (SubtitleType subtitleType : values()) {
			stableIndexToSubtitleTypeMap.put(subtitleType.getStableIndex(), subtitleType);
			for (String fileExtension : subtitleType.fileExtensions) {
				fileExtensionToSubtitleTypeMap.put(fileExtension.toLowerCase(Locale.ROOT), subtitleType);
			}
			for (String codec : subtitleType.libMediaInfoCodecs) {
				libmediainfoCodecToSubtitleTypeMap.put(codec.toLowerCase(Locale.ROOT), subtitleType);
			}
		}
		SUPPORTED_FILE_EXTENSIONS = Collections.unmodifiableSet(
			new LinkedHashSet<>(fileExtensionToSubtitleTypeMap.keySet())
		);
	}

	public static SubtitleType valueOfStableIndex(int stableIndex) {
		SubtitleType subtitleType = stableIndexToSubtitleTypeMap.get(stableIndex);
		if (subtitleType == null) {
			subtitleType = UNKNOWN;
		}
		return subtitleType;
	}

	/**
	 * @deprecated use getSubtitleTypeByFileExtension(String fileExtension) instead
	 */
	@Deprecated
	public static SubtitleType getSubtitleTypeByFileExtension(String fileExtension) {
		return valueOfFileExtension(fileExtension);
	}

	public static SubtitleType valueOfFileExtension(String fileExtension) {
		if (isBlank(fileExtension)) {
			return UNKNOWN;
		}
		SubtitleType subtitleType = fileExtensionToSubtitleTypeMap.get(fileExtension.toLowerCase());
		if (subtitleType == null) {
			subtitleType = UNKNOWN;
		}
		return subtitleType;
	}

	/**
	 * @deprecated use SubtitleType {@link #valueOfMediaInfoValue} instead.
	 */
	@Deprecated
	public static SubtitleType getSubtitleTypeByLibMediaInfoCodec(String codec) {
		return valueOfMediaInfoValue(codec);
	}

	public static SubtitleType valueOfMediaInfoValue(String value) {
		return valueOfMediaInfoValue(value, UNKNOWN);
	}

	public static SubtitleType valueOfMediaInfoValue(String value, SubtitleType defaultType) {
		if (isBlank(value)) {
			return defaultType;
		}
		SubtitleType subtitleType = libmediainfoCodecToSubtitleTypeMap.get(
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

	private SubtitleType(
		int index,
		String description,
		List<String> fileExtensions,
		List<String> libMediaInfoCodecs,
		Type category
	) {
		this.index = index;
		this.description = description;
		this.fileExtensions = fileExtensions;
		this.libMediaInfoCodecs = libMediaInfoCodecs;
		this.category = category;
	}

	public String getDescription() {
		return description;
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
		return category == Type.TEXT;
	}

	public boolean isPicture() {
		return category == Type.PICTURE;
	}
}
