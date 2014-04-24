/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package net.pms.formats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class matches and instantiates formats.
 */
public final class FormatFactory {
	/**
	 * Logger used for all logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatFactory.class);

	/**
	 * Initial list of known formats.
	 */
	private static final Format[] FORMATS = new Format[] {
		new DVRMS(),
		new FLAC(),
		new GIF(),
		new ISO(),
		new JPG(),
		new M4A(),
		new MKV(),
		new MP3(),
		new MPG(),
		new OGG(),
		new PNG(),
		new RAW(),
		new TIF(),
		new WAV(),
		new WEB(),
		new PLAYLIST(),
	};

	/**
	 * The list of registered formats.
	 */
	private static List<Format> formats = new ArrayList<>(Arrays.asList(FORMATS));
	
	/**
	 * This class is not meant to be instantiated.
	 */
	private FormatFactory() {
	}

	/**
	 * @deprecated Use {@link #getAssociatedFormat(String)} instead.
	 */
	@Deprecated
	public static Format getAssociatedExtension(final String filename) {
		return getAssociatedFormat(filename);
	}

	/**
	 * Match a given filename to all known formats and return a fresh instance
	 * of that format. Matching is done by the file extension (e.g. ".gif") or
	 * protocol (e.g. "http://") of the filename. Will return <code>null</code>
	 * if no match can be made.
	 * 
	 * @param filename The filename to match.
	 * @return The format.
	 * @see Format#match(String)
	 * @since 1.90.0
	 */
	public static Format getAssociatedFormat(final String filename) {
		for (Format format : formats) {
			if (format.match(filename)) {
				LOGGER.trace("Matched format " + format + " to \"" + filename + "\"");

				// Return a fresh instance
				return format.duplicate();
			}
		}

		LOGGER.trace("Could not match any format to \"" + filename + "\"");
		return null;
	}

	/**
	 * Returns the list of supported formats.
	 *
	 * @return The list of supported formats.
	 * @since 1.90.0
	 */
	public static List<Format> getSupportedFormats() {
		return formats;
	}

	/**
	 * Sets the list of known formats.
	 *
	 * @param formatList The list of supported formats.
	 * @since 1.90.0
	 */
	public static void setFormats(List<Format> formatList) {
		formats = formatList;
	}
}
