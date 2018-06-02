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


/**
 * Represents a reference to the {@code io_service_t} type.
 */
public class IOServiceTRef extends MachPortTRef {

	/**
	 * Creates a new {@link IOServiceTRef} without allocating a {@link Pointer};
	 * the internal {@link Pointer} is {@code null}. If you're going to use this
	 * instance as an argument that returns a value, use
	 * {@link #IOServiceTRef(boolean)} and set {@code allocate} to {@code true}.
	 */
	public IOServiceTRef() {
	}

	/**
	 * Creates a new {@link IOServiceTRef}. If you're going to use this instance
	 * as an argument that returns a value, set {@code allocate} to {@code true}.
	 *
	 * @param allocate Whether to allocate {@link Memory} for the internal
	 *            {@link Pointer} or not. If {@code false} the internal
	 *            {@link Pointer} is set to {@code null}.
	 */
	public IOServiceTRef(boolean allocate) {
		super(allocate);
	}

	/**
	 * Creates a new {@link IOServiceTRef} from a {@link IOServiceT}. Allocates
	 * {@link Memory} for the internal {@link Pointer} and puts the value from
	 * {@code port} in the allocated {@link Memory}.
	 *
	 * @param service the {@link IOServiceT} to "convert" to a
	 *            {@link IOServiceTRef}.
	 */
	public IOServiceTRef(IOServiceT service) {
		super(service);
	}

	/**
	 * @return The {@link IOServiceT} of this {@link IOServiceTRef} or
	 *         {@code null} if the internal {@link Pointer} points to
	 *         {@code null}.
	 */
	@Override
	public IOServiceT getValue() {
		if (getPointer() == null) {
			return null;
		}
		return new IOServiceT(getPointer().getNativeLong(0));
	}
}
