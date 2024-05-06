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
package net.pms.dlna;

import java.io.IOException;

/**
 * An {@link IOException} indicating a DLNA media format profile related issue.
 *
 * @author Nadahar
 */
public class DLNAProfileException extends IOException {

	private static final long serialVersionUID = 1L;

	public DLNAProfileException() {
	}

	public DLNAProfileException(String message) {
		super(message);
	}

	public DLNAProfileException(Throwable cause) {
		super(cause);
	}

	public DLNAProfileException(String message, Throwable cause) {
		super(message, cause);
	}
}
