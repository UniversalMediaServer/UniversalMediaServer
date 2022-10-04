/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.parsers.mediainfo;

/**
 * An enum representing the C++ enum {@code fileoptions_t} defined in
 * {@code MediaInfoDLL.h}, "File opening options".
 *
 * @author Nadahar
 */
public enum FileOptions {

	/** No options */
	NOTHING(0x00),

	/** Do not browse folders recursively */
	NO_RECURSIVE(0x01),

	/** Close all files before open */
	CLOSE_ALL(0x02);

	private final int value;

	private FileOptions(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
