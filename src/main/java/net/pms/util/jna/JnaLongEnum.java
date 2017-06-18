/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
package net.pms.util.jna;


/**
 * This interface provides the possibility for automatic conversion between
 * {@link Enum} and C-style long long enums.
 *
 * @param <T> the Enum type
 */
public interface JnaLongEnum<T> {

	/**
	 * @return This constant's {@code long} value.
	 */
	long getValue();

	/**
	 * Tries to find a constant corresponding to {@code value}.
	 *
	 * @param value the integer value to look for.
	 * @return The corresponding constant or {@code null} if not found.
	 */
	T typeForValue(long value);
}
