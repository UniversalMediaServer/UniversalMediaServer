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
package net.pms.dlna;

import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

public enum SubtitleType {
	UNKNOWN ("Unknown"),
	SUBRIP ("SubRip", "srt"),
	TEXT ("Text file", "txt"),
	MICRODVD ("MicroDVD", "sub"),
	SAMI ("SAMI", "smi"),
	ASS ("(Advanced) SubStation Alpha", "ass", "ssa"),
	VOBSUB ("VobSub", "idx"),
	EMBEDDED ("Embedded"),
	UNSUPPORTED ("Unsupported");

	private String description;
	private List<String> fileExtensions;

	private static Map<String, SubtitleType> fileExtensionToSubtitleTypeMap;
	static {
		fileExtensionToSubtitleTypeMap = new HashMap<String, SubtitleType>();
		for (SubtitleType subtitleType : values()) {
			for (String fileExtension : subtitleType.fileExtensions) {
				fileExtensionToSubtitleTypeMap.put(fileExtension, subtitleType);
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
	public static Set<String> getSupportedFileExtensions() {
		return fileExtensionToSubtitleTypeMap.keySet();
	}

	private SubtitleType(String description, String... fileExtensions) {
		this.description = description;
		this.fileExtensions = new ArrayList<String>(Arrays.asList(fileExtensions));
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