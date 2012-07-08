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
package net.pms.formats;

import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.trim;

public enum SubtitleType {
	UNKNOWN ("Generic", list(), list()),
	SUBRIP ("SubRip", list("srt"), list("S_TEXT/UTF8", "S_UTF8")),
	TEXT ("Text file", list("txt"), list()),
	MICRODVD ("MicroDVD", list("sub"), list()),
	SAMI ("SAMI", list("smi"), list()),
	ASS ("(Advanced) SubStation Alpha",
			list("ass", "ssa"),
			list("S_TEXT/SSA", "S_TEXT/ASS", "S_SSA", "S_ASS")),
	VOBSUB ("VobSub", list("idx"), list("S_VOBSUB", "subp")),
	UNSUPPORTED ("Unsupported", list(), list()),
	USF ("Universal Subtitle Format", list(), list("S_TEXT/USF", "S_USF")),
	BMP ("BMP", list(), list("S_IMAGE/BMP")),
	DIVX ("DIVX subtitles", list(), list("DXSB")),
	TX3G ("Timed text (TX3G)", list(), list("tx3g")),
	PGS ("Blu-ray subtitles", list(), list("S_HDMV/PGS", "PGS", "144"));

	private String description;
	private List<String> fileExtensions;
	private List<String> libMediaInfoCodecs;

	private static Map<String, SubtitleType> fileExtensionToSubtitleTypeMap;
	private static Map<String, SubtitleType> libmediainfoCodecToSubtitleTypeMap;
	private static List<String> list(String... args) {
		return new ArrayList<String>(Arrays.asList(args));
	}

	static {
		fileExtensionToSubtitleTypeMap = new HashMap<String, SubtitleType>();
		libmediainfoCodecToSubtitleTypeMap = new HashMap<String, SubtitleType>();
		for (SubtitleType subtitleType : values()) {
			for (String fileExtension : subtitleType.fileExtensions) {
				fileExtensionToSubtitleTypeMap.put(fileExtension.toLowerCase(), subtitleType);
			}
			for (String codec : subtitleType.libMediaInfoCodecs) {
				libmediainfoCodecToSubtitleTypeMap.put(codec.toLowerCase(), subtitleType);
			}
		}
	}

	public static SubtitleType getSubtitleTypeByFileExtension(String fileExtension) {
		if (isBlank(fileExtension)) {
			return UNKNOWN;
		}
		SubtitleType subtitleType = fileExtensionToSubtitleTypeMap.get(fileExtension.toLowerCase());
		if (subtitleType == null) {
			subtitleType = UNKNOWN;
		}
		return subtitleType;
	}

	public static SubtitleType getSubtitleTypeByLibMediaInfoCodec(String codec) {
		if (isBlank(codec)) {
			return UNKNOWN;
		}
		SubtitleType subtitleType = libmediainfoCodecToSubtitleTypeMap.get(trim(codec).toLowerCase());
		if (subtitleType == null) {
			subtitleType = UNKNOWN;
		}
		return subtitleType;
	}

	public static Set<String> getSupportedFileExtensions() {
		return fileExtensionToSubtitleTypeMap.keySet();
	}

	private SubtitleType(String description, List<String> fileExtensions, List<String> libMediaInfoCodecs) {
		this.description = description;
		this.fileExtensions = fileExtensions;
		this.libMediaInfoCodecs = libMediaInfoCodecs;
	}

	public String getDescription() {
		return description;
	}

	public String getExtension() {
		if (fileExtensions.isEmpty()) {
			return "";
		} else {
			return fileExtensions.get(0);
		}
	}
}