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
package net.pms.platform.windows;

import com.sun.jna.Pointer;
import javax.annotation.Nonnull;

/**
 * A simple extension of {@link com.sun.jna.platform.win32.Guid.GUID} that
 * overrides {@link #toString()}.
 */
public class GUID extends com.sun.jna.platform.win32.Guid.GUID {

	/**
	 * Instantiates a new {@link GUID}.
	 */
	public GUID() {
		super();
	}

	/**
	 * Instantiates a copy of the specified {@link GUID}.
	 *
	 * @param guid the {@link GUID} to copy.
	 */
	public GUID(@Nonnull GUID guid) {
		super(guid);
	}

	/**
	 * Instantiates a new {@link GUID} from the specified {@link String}.
	 *
	 * @param guid the string representation.
	 */
	public GUID(@Nonnull String guid) {
		super(guid);
	}

	/**
	 * Instantiates a new {@link GUID} from a byte array of 16 bytes.
	 *
	 * @param data the 16 byte array.
	 */
	public GUID(@Nonnull byte[] data) {
		super(data);
	}

	/**
	 * Instantiates a new {@link GUID} from the specified {@link Pointer} - only
	 * use if the memory pointed to is known to contain a valid GUID.
	 *
	 * @param pointer the {@link Pointer} to the existing GUID structure.
	 */
	public GUID(Pointer pointer) {
		super(pointer);
	}

	@Override
	public String toString() {
		return super.toGuidString();
	}
}
