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
package net.pms.image;

import java.io.IOException;

/**
 * This is simply a checked wrapper for {@link RuntimeException}s thrown by
 * {@link ImageIO}. It is used to translate thrown {@link RuntimeException}s to
 * {@link IOException}s so they can be handled. This is needed because
 * {@link ImageIO} has the nasty habit of throwing {@link RuntimeException}s
 * when something goes wrong during an operation.
 *
 * @author Nadahar
 */

public class ImageIORuntimeException extends IOException {
	private static final long serialVersionUID = 1L;

	public ImageIORuntimeException(RuntimeException cause) {
		super(cause);
	}

	public ImageIORuntimeException(String message, RuntimeException cause) {
		super(message, cause);
	}

}
