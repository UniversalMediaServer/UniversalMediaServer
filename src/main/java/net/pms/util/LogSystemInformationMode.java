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
package net.pms.util;

import java.util.Locale;

/**
 * This is a representation of the different modes of system information logging.
 *
 * @author Nadahar
 */
public enum LogSystemInformationMode {
	/** Never log system information */
	NEVER,

	/** Only log system information when the log level is trace */
	TRACE_ONLY,

	/** Always log system information */
	ALWAYS;

	/**
	 * Tries to parse a {@link String} value into a
	 * {@link LogSystemInformationMode}. If the parsing fails, {@code null} is
	 * returned.
	 *
	 * @param logMode the {@link String} representing the mode of system
	 *            information logging.
	 * @return The corresponding {@link LogSystemInformationMode} or
	 *         {@code null}.
	 */
	public static LogSystemInformationMode typeOf(String logMode) {
		if (logMode == null) {
			return null;
		}
		logMode = logMode.trim().toLowerCase(Locale.ROOT);
		return switch (logMode) {
			case "never", "off", "none", "no", "false" -> NEVER;
			case "trace", "trace only", "trace_only" -> TRACE_ONLY;
			case "always", "on", "yes", "true" -> ALWAYS;
			default -> null;
		};
	}

	/**
	 * @return the {@link Enum} value as a {@link String}.
	 */
	public String getValue() {
		return super.toString();
	}

	@Override
	public String toString() {
		switch (this) {
			case NEVER -> {
				return "Never";
			}
			case TRACE_ONLY -> {
				return "Trace only";
			}
			case ALWAYS -> {
				return "Always";
			}
			default -> throw new IllegalStateException("Unimplemented enum value: " + super.toString());
		}
	}
}
