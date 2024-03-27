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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite;

/**
 * allowedValueList for the DaylightSaving Property
 */
public enum DaylightSavingValue {

	/**
	 * The reference point for the associated local time value is Daylight
	 * Saving Time, even if the indicated time falls outside the period of the
	 * year when Daylight Saving Time is actually observed.
	 */
	DAYLIGHTSAVING("DAYLIGHTSAVING"),
	/**
	 * The reference point for the associated local time value is Standard Time,
	 * even if the indicated time falls outside the period of the year when
	 * Standard Time is actually observed.
	 */
	STANDARD("STANDARD"),
	/**
	 * The reference point for the associated local time value depends on
	 * whether Daylight Saving Time is in effect or not.
	 *
	 * During the time interval starting one hour before the switch is made from
	 * Daylight Saving Time back to Standard time and ending one hour after that
	 * switching point however, the reference point is ambiguous and is device
	 * dependent.
	 */
	UNKNOWN("UNKNOWN");

	private final String value;

	DaylightSavingValue(String value) {
		this.value = value;
	}

	public static DaylightSavingValue valueOrNullOf(String s) {
		for (DaylightSavingValue value : values()) {
			if (value.toString().equals(s)) {
				return value;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return value;
	}

}
