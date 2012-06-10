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
	private static final Format[] FORMATS = new Format[] { new WEB(),
			new MKV(), new M4A(), new MP3(), new ISO(), new MPG(), new WAV(),
			new JPG(), new OGG(), new PNG(), new GIF(), new TIF(), new FLAC(),
			new DVRMS(), new RAW() };

	/**
	 * The list of registered formats.
	 */
	private static ArrayList<Format> formats = new ArrayList<Format>(Arrays.asList(FORMATS));
	
	/**
	 * This class is not meant to be instantiated.
	 */
	private FormatFactory() {
	}

	/**
	 * Match a given filename to all known formats and return a fresh instance
	 * of that format. Matching is done by the extension (e.g. ".gif") or
	 * protocol (e.g. "http://") of the filename. Will return <code>null</code>
	 * if no match can be made.
	 * 
	 * @param filename The filename to match.
	 * @return The format.
	 * @see Format#match(String)
	 */
	public static Format getAssociatedExtension(final String filename) {
		for (Format ext : formats) {
			if (ext.match(filename)) {
				LOGGER.trace("Matched format " + ext + " to \"" + filename + "\"");

				// Return a fresh instance
				return ext.duplicate();
			}
		}

		LOGGER.trace("Could not match any format to \"" + filename + "\"");
		return null;
	}

	/**
	 * Returns the list of known formats.
	 *
	 * @return The list of known formats.
	 */
	public static ArrayList<Format> getExtensions() {
		return formats;
	}

	/**
	 * Sets the list of known formats.
	 *
	 * @param formatList The list of known formats.
	 */
	public static void setExtensions(ArrayList<Format> formatList) {
		formats = formatList;
	}
}

