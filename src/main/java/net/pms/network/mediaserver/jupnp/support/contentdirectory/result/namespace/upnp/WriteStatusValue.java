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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp;

/**
 * allowedValueList for the upnp:writeStatus Property
 */
public enum WriteStatusValue {

	/**
	 * The object’s resource(s) MAY be deleted and/or modified.
	 */
	WRITABLE("WRITABLE"),
	/**
	 * The object’s resource(s) MAY NOT be deleted and/or modified.
	 */
	PROTECTED("PROTECTED"),
	/**
	 * The object’s resource(s) MAY NOT be modified.
	 */
	NOT_WRITABLE("NOT_WRITABLE"),
	/**
	 * The object’s resource(s) write status is unknown.
	 */
	UNKNOWN("UNKNOWN"),
	/**
	 * Some of the object’s resource(s) have a different write status.
	 */
	MIXED("MIXED");

	private final String value;

	WriteStatusValue(String value) {
		this.value = value;
	}

	public static WriteStatusValue valueOrNullOf(String s) {
		for (WriteStatusValue value : values()) {
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
