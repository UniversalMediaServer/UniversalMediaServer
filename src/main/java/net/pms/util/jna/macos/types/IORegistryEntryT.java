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
package net.pms.util.jna.macos.types;

import com.sun.jna.NativeLong;


/**
 * Represents the {@code io_registry_entry_t} type.
 */
public class IORegistryEntryT extends IOObjectT {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance with value {@code 0}.
	 */
	public IORegistryEntryT() {
	}

	/**
	 * Creates a new instance with value {@code value}.
	 *
	 * @param value the value of the new instance.
	 */
	public IORegistryEntryT(int value) {
		super(value);
	}

	/**
	 * Creates a new instance with value {@code value}.
	 *
	 * @param value the value of the new instance.
	 */
	public IORegistryEntryT(long value) {
		super(value);
	}

	/**
	 * Creates a new instance with value {@code value}.
	 *
	 * @param value the value of the new instance.
	 */
	public IORegistryEntryT(NativeLong value) {
		super(value);
	}

	/**
	 * Creates a new {@link IORegistryEntryT} from any {@link MachPortT} or
	 * subclass instance. Since these object aren't created by Java, they aren't
	 * created with their proper type and as a result can't be cast from
	 * {@link MachPortT} to {@link IORegistryEntryT}. Use this method as a
	 * replacement for casting.
	 *
	 * @param machPort the {@link MachPortT} or subclass to "cast" from.
	 * @return The new {@link IORegistryEntryT} instance.
	 */
	public static IORegistryEntryT toIORegistryT(MachPortT machPort) {
		return machPort == null ? null : new IORegistryEntryT(machPort.longValue());
	}
}
