/*
 * Digital Media Server, for streaming digital media to UPnP AV or DLNA
 * compatible devices based on PS3 Media Server and Universal Media Server.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/.
 */
package net.pms.encoders;

/**
 * This {@code enum} indicates if the error only applies to a particular use (
 * {@link #SPECIFIC} of an executable or to all use ({@link #GENERAL}).
 * <p>
 * A general error is for example if the executable isn't found, is corrupt or
 * is incompatible with the system. Specific errors are those that would still
 * allow the executable to be used for other purposes, but which makes it
 * unsuitable for a particular use. That could be things like failing version
 * criteria or lacking support for specified codecs or formats.
 */
public enum ExecutableErrorType {

	/** An error that applies to any use of an executable */
	GENERAL,

	/** An error that applies only to specific use of an executable */
	SPECIFIC
}
