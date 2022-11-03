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
 * An enum representing the C++ enum {@code infooptions_t} defined in
 * {@code MediaInfoDLL.h}, "Option if InfoKind = Info_Options".
 * <p>
 * Get(...)[infooptions_t] return a string like "YNYN...". Use this
 * {@code enum} to know at what correspond the {@code Y} (Yes) or {@code N}
 * (No).
 * <p>
 * If {@code Get(...)[0]==Y, then : }
 *
 * @author Nadahar
 */
public enum InfoOptions {

	/** Show this parameter in {@link MediaInfo#Inform()} */
	SHOW_IN_INFORM(0),

	/** Reserved for future use */
	RESERVED(1),

	/**
	 * Internal use only (info : Must be showed in
	 * {@code Info_Capacities()})
	 */
	SHOW_IN_SUPPORTED(2),

	/**
	 * Value return by a standard {@link MediaInfo#get}() can be : {@code T}
	 * (Text), {@code I} (Integer, warning up to 64 bits), {@code F}
	 * (Float), {@code D} (Date), {@code B} (Binary datas coded Base64)
	 * (Numbers are in Base 10)
	 */
	TYPE_OF_VALUE(3);

	private final int value;

	private InfoOptions(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}


