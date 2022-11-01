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
package net.pms.parsers.mediainfo;

/**
 * An enum representing the C++ enum {@code stream_t} defined in
 * {@code MediaInfoDLL.h}, "Kinds of Stream".
 *
 * @author Nadahar
 */
public enum StreamKind {

	/** StreamKind = General */
	GENERAL(0),

	/** StreamKind = Video */
	VIDEO(1),

	/** StreamKind = Audio */
	AUDIO(2),

	/** StreamKind = Text */
	TEXT(3),

	/** StreamKind = Other */
	OTHER(4),

	/** StreamKind = Image */
	IMAGE(5),

	/** StreamKind = Menu */
	MENU(6);

	private final int value;

	private StreamKind(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
