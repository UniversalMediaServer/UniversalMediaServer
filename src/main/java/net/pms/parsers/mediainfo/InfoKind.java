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
 * An enum representing the C++ enum {@code info_t} defined in
 * {@code MediaInfoDLL.h}, "Kind of information".
 *
 * @author Nadahar
 */
public enum InfoKind {

	/** InfoKind = Unique name of parameter */
	NAME(0),

	/** InfoKind = Value of parameter */
	TEXT(1),

	/** InfoKind = Unique name of measure unit of parameter */
	MEASURE(2),

	/** InfoKind = See {@link InfoOptionsType} */
	OPTIONS(3),

	/** InfoKind = Translated name of parameter */
	NAME_TEXT(4),

	/** InfoKind = Translated name of measure unit */
	MEASURE_TEXT(5),

	/** InfoKind = More information about the parameter */
	INFO(6),

	/** InfoKind = Information : how data is found */
	HOW_TO(7);

	private final int value;

	private InfoKind(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
