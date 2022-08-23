/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
