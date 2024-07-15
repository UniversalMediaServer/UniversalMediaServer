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
package net.pms.util;

import net.pms.formats.Format;
import net.pms.store.StoreItem;

public class PlayerUtil {
	/**
	 * This class is not meant to be instantiated.
	 */
	private PlayerUtil() { }

	// Returns whether or not the supplied store item matches the supplied format and format identifier
	private static boolean isType(StoreItem item, int matchType, Format.Identifier matchIdentifier) {
		boolean match = false;

		if (item != null) {
			Format format = item.getFormat();

			// the int returned by format.getType() is a bitmap, so match by checking the bit is set
			// XXX the old isCompatible implementations (incorrectly) used to match
			// with getType() == matchType
			if ((format != null) && ((format.getType() & matchType) == matchType)) {
				if (matchIdentifier == null) { // match any identifier
					match = true;
				} else { // match the specified format identifier
					Format.Identifier identifier = format.getIdentifier();
					match = identifier.equals(matchIdentifier);
				}
			}
		}

		return match;
	}

	/**
	 * Returns whether or not the supplied store item is an image file.
	 *
	 * @param item the store item
	 * @return true if the store item is an image file, false otherwise.
	 */
	public static boolean isImage(StoreItem item) {
		return isType(item, Format.IMAGE, null);
	}

	/**
	 * Returns whether or not the supplied store item is an image file.
	 *
	 * @param item the store item
	 * @param identifier the format identifier to match against
	 * @return true if the store item is an image file with the specified format identifier, false otherwise
	 */
	public static boolean isImage(StoreItem item, Format.Identifier identifier) {
		return isType(item, Format.IMAGE, identifier);
	}

	/**
	 * Returns whether or not the supplied store item is an audio file.
	 *
	 * @param item the store item
	 * @return true if the store item is an audio file, false otherwise.
	 */
	public static boolean isAudio(StoreItem item) {
		return isType(item, Format.AUDIO, null);
	}

	/**
	 * Returns whether or not the supplied store item is an audio file.
	 *
	 * @param item the store item
	 * @param identifier the format identifier to match against
	 * @return true if the store item is an audio file with the specified format identifier, false otherwise
	 */
	public static boolean isAudio(StoreItem item, Format.Identifier identifier) {
		return isType(item, Format.AUDIO, identifier);
	}

	/**
	 * Returns whether or not the supplied store item is a video file.
	 *
	 * @param item the store item
	 * @return true if the store item is a video file, false otherwise.
	 */
	public static boolean isVideo(StoreItem item) {
		return isType(item, Format.VIDEO, null);
	}

	/**
	 * Returns whether or not the supplied store item is a video file.
	 *
	 * @param item the store item
	 * @param identifier the format identifier to match against
	 * @return true if the store item is a video file with the specified format identifier, false otherwise.
	 */
	public static boolean isVideo(StoreItem item, Format.Identifier identifier) {
		return isType(item, Format.VIDEO, identifier);
	}

	/**
	 * Returns whether or not the supplied store item is a web audio file.
	 *
	 * @param resource the store item
	 * @return true if the store item is a web audio file, false otherwise.
	 */
	public static boolean isWebAudio(StoreItem item) {
		return isType(item, Format.AUDIO, Format.Identifier.WEB);
	}

	/**
	 * Returns whether or not the supplied store item is a web video file.
	 *
	 * @param item the store item
	 * @return true if the store item is a web video file, false otherwise.
	 */
	public static boolean isWebVideo(StoreItem item) {
		return isType(item, Format.VIDEO, Format.Identifier.WEB);
	}
}
