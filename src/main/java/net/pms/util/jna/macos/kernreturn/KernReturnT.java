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
package net.pms.util.jna.macos.kernreturn;


/**
 * Common interface for all {@code kern_return_t} values, which maps return
 * codes to {@link KernReturnT} instances.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:JavadocStyle")
public interface KernReturnT {

	/**
	 * @return The integer value for this instance.
	 */
	int getValue();

	/**
	 * Returns a standardized string representation of a {@link KernReturnT}
	 * return code in the form {@code "<name> (0x<hexcode>)"}.
	 * <p>
	 * Subclasses should use {@link Long#toHexString()} since
	 * {@code kern_return_t} is unsigned. Subclasses may also reroute
	 * {@link #toString()} to {@link #toStandardString()}. Example implementation:
	 *
	 * <pre>
	 * {@code return "<name> (0x" +  Long.toHexString(<code> & 0xFFFFFFFFL) + ")";}
	 * </pre>
	 *
	 * @return The formatted {@link String}.
	 */
	String toStandardString();
}
