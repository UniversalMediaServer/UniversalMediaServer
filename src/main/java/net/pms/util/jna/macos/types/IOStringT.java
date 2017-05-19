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

import java.nio.charset.Charset;
import net.pms.util.jna.FixedCharArrayByReference;


/**
 * An implementation of {@code io_string_t} as specified in
 * {@code devices/devices.defs}.
 *
 * @author Nadahar
 */
public class IOStringT extends FixedCharArrayByReference {

	/**
	 * The size of the native array of 8-bit chars referenced by
	 * {@link IOStringT}.
	 */
	public static final long SIZE = 512;

	/**
	 * Creates an unallocated {@link IOStringT}.
	 */
	public IOStringT() {
		super(SIZE);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param allocate if {@code true} {@link com.sun.jna.Memory} is allocated
	 *            for the referenced array.
	 */
	public IOStringT(boolean allocate) {
		super(SIZE);
		if (allocate) {
			setByteArray(new byte[(int) SIZE]);
		}
	}

	/**
	 * Creates a new {@link IOStringT} and sets its content to {@code content}.
	 * <p>
	 * <b>Relying on the default charset can often lead to bugs. Use
	 * {@link #IOStringT(String, Charset)} instead.</b>
	 *
	 * @param content the {@link String} to store in the referenced 8-bit char
	 *            array.
	 */
	public IOStringT(String content) {
		super(SIZE);
		setString(content);
	}

	/**
	 * Creates a new {@link IOStringT} and sets its content to {@code content}.
	 *
	 * @param content the {@link String} to store in the referenced 8-bit char
	 *            array.
	 * @param charset the {@link Charset} to use when encoding {@code content}.
	 */
	public IOStringT(String content, Charset charset) {
		super(SIZE);
		setString(content, charset);
	}
}
