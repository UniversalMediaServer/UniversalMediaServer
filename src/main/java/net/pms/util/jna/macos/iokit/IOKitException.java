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
package net.pms.util.jna.macos.iokit;

import java.io.IOException;


/**
 * A checked {@link Exception} indicating an exception related to
 * {@link IOKit}.
 */
public class IOKitException extends IOException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an {@code IOKitException} with {@code null} as its error
	 * detail message.
	 */
	public IOKitException() {
		super();
	}

	/**
	 * Constructs an {@code IOKitException} with the specified detail
	 * message.
	 *
	 * @param message the detail message.
	 */
	public IOKitException(String message) {
		super(message);
	}

	/**
	 * Constructs an {@code IOKitException} with the specified detail
	 * message and cause.
	 * <p>
	 * Note that the detail message associated with {@code cause} is
	 * <i>not</i> automatically incorporated into this exception's detail
	 * message.
	 *
	 * @param message the detail message.
	 * @param cause The cause. A null value is permitted, and indicates that
	 *            the cause is nonexistent or unknown.
	 */
	public IOKitException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs an {@code IOKitException} with the specified cause and a
	 * detail message of {@code (cause==null ? null : cause.toString())}
	 * (which typically contains the class and detail message of
	 * {@code cause}). This constructor is useful for IO exceptions that are
	 * little more than wrappers for other {@link Throwable}s.
	 *
	 * @param cause The cause. A null value is permitted, and indicates that
	 *            the cause is nonexistent or unknown.
	 */
	public IOKitException(Throwable cause) {
		super(cause);
	}
}
