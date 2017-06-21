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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Enum with possible audio track attributes.
 * AUDIO_ATTRIBUTE (Set<String> libMediaInfoKeys, boolean multipleValuesPossible,
 * boolean getLargerValue, Integer defaultValue, Integer minimumValue)
 */
public enum AudioAttribute {
	CHANNELS_NUMBER (set("Channel(s)"), true, true, 2, 1),
	DELAY (set("Video_Delay"), false, false, 0, null),
	SAMPLE_FREQUENCY (set("SamplingRate"), true, true, 48000, 1);

	private final Set<String> libMediaInfoKeys;
	private final boolean multipleValuesPossible;
	private final boolean getLargerValue;
	private final Integer defaultValue;
	private final Integer minimumValue;

	private static final Pattern libMediaInfoKeyPattern = Pattern.compile("^\\s*(\\S+)\\s*:");
	private final static Map<String, AudioAttribute> libMediaInfoKeyToAudioAttributeMap;
	private static Set<String> set(String... args) {
		return new HashSet<>(Arrays.asList(args));
	}

	static {
		libMediaInfoKeyToAudioAttributeMap = new HashMap<>();
		for (AudioAttribute audioAttribute : values()) {
			for (String libMediaInfoKey : audioAttribute.libMediaInfoKeys) {
				libMediaInfoKeyToAudioAttributeMap.put(libMediaInfoKey.toLowerCase(), audioAttribute);
			}
		}
	}

	private AudioAttribute(Set<String> libMediaInfoKeys, boolean multipleValuesPossible,
						   boolean getLargerValue, Integer defaultValue, Integer minimumValue) {
		this.libMediaInfoKeys = libMediaInfoKeys;
		this.multipleValuesPossible = multipleValuesPossible;
		this.getLargerValue = getLargerValue;
		this.defaultValue = defaultValue;
		this.minimumValue = minimumValue;
	}

	public static AudioAttribute getAudioAttributeByLibMediaInfoKeyValuePair(String keyValuePair) {
		if (isBlank(keyValuePair)) {
			throw new IllegalArgumentException("Empty keyValuePair passed in.");
		}

		Matcher keyMatcher = libMediaInfoKeyPattern.matcher(keyValuePair);
		if (keyMatcher.find()) {
			String key = keyMatcher.group(1);
			AudioAttribute audioAttribute = libMediaInfoKeyToAudioAttributeMap.get(key.toLowerCase());
			if (audioAttribute == null) {
				throw new IllegalArgumentException("Can't find AudioAttribute for key '" + key + "'.");
			} else {
				return audioAttribute;
			}
		} else {
			throw new IllegalArgumentException("Can't find key in keyValuePair '" + keyValuePair + "'.");
		}
	}

	public Integer getDefaultValue() {
		return defaultValue;
	}

	public boolean isGetLargerValue() {
		return getLargerValue;
	}

	public boolean isMultipleValuesPossible() {
		return multipleValuesPossible;
	}

	public Integer getMinimumValue() {
		return minimumValue;
	}
}
