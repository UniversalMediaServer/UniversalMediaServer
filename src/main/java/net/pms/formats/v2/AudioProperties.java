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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isEmpty;


/**
 * Class for storing and parsing from libmediainfo audio track's properties
 * (bitrate, channels number, etc) and meta information (title, album, etc)
 *
 * This class is not thread-safe
 *
 * @since 1.60.0
 */
public class AudioProperties {
	private static final Logger LOGGER = LoggerFactory.getLogger(AudioProperties.class);

	private static final Pattern intPattern = Pattern.compile("([\\+-]?\\d+)([eE][\\+-]?\\d+)?");
	private static final Pattern floatPattern = Pattern.compile("([\\+-]?\\d(\\.\\d*)?|\\.\\d+)([eE][\\+-]?(\\d(\\.\\d*)?|\\.\\d+))?");

	private int numberOfChannels = 2;


	public int getAttribute(AudioAttribute attribute) {
		switch (attribute) {
			case CHANNELS_NUMBER:
				return getNumberOfChannels();
			default:
				throw new IllegalArgumentException("Unimplemented attribute");
		}
	}

	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	public void setNumberOfChannels(int numberOfChannels) {
		if (numberOfChannels < 1) {
			throw new IllegalArgumentException("Channel number can't be less than 1.");
		}
		this.numberOfChannels = numberOfChannels;
	}

	public void setNumberOfChannels(String mediaInfoValue) {
		this.numberOfChannels = getChannelsNumberFromLibMediaInfo(mediaInfoValue);
	}

	public static int getChannelsNumberFromLibMediaInfo(String mediaInfoValue) {
		if (isEmpty(mediaInfoValue)) {
			LOGGER.warn("Empty value passed in. Returning default number 2.");
			return 2;
		}

		// examples of libmediainfo output:
		// Channel(s) : 2 channels
		// Channel(s) : 6 channels
		// Channel(s) : 2 channels / 1 channel / 1 channel

		int result = -1;
		Matcher intMatcher = intPattern.matcher(mediaInfoValue);
		while (intMatcher.find()) {
			String matchResult = intMatcher.group();
			try {
				int currentResult = Integer.parseInt(matchResult);
				if (currentResult > result) {
					result = currentResult;
				}
			} catch (NumberFormatException ex) {
				LOGGER.warn("NumberFormatException during parsing substring {} from value {}", matchResult, mediaInfoValue);
			}
		}

		if (result <= 0) {
			LOGGER.warn("Can't parse value {}. Returning default number 2.", mediaInfoValue);
			return 2;
		} else {
			return result;
		}
	}
}
