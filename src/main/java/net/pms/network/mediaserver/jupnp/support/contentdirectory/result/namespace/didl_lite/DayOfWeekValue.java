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
 * allowedValueList for the DayOfWeek Property
 */
public enum DayOfWeekValue {

	SUN("SUN"),
	MON("MON"),
	TUE("TUE"),
	WED("WED"),
	THU("THU"),
	FRI("FRI"),
	SAT("SAT");

	private final String value;

	DayOfWeekValue(String value) {
		this.value = value;
	}

	public static DayOfWeekValue valueOrNullOf(String s) {
		for (DayOfWeekValue value : values()) {
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
