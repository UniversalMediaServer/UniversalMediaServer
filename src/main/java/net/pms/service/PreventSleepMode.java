/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.service;

import java.util.Locale;
import net.pms.Messages;

/**
 * This is a representation of the different modes of system sleep prevention.
 *
 * @author Nadahar
 */
public enum PreventSleepMode {
	/** Never manipulate OS sleep mode */
	NEVER,

	/** Prevent sleep during playback */
	PLAYBACK,

	/** Prevent sleep while UMS is running */
	RUNNING;

	/**
	 * Tries to parse a {@link String} value into a {@link PreventSleepMode}. If
	 * the parsing fails, {@code null} is returned.
	 *
	 * @param sleepMode the {@link String} representing the mode of sleep
	 *            prevention.
	 * @return The corresponding {@link PreventSleepMode} or {@code null}.
	 */
	public static PreventSleepMode typeOf(String sleepMode) {
		if (sleepMode == null) {
			return null;
		}
		sleepMode = sleepMode.trim().toLowerCase(Locale.ROOT);
		return switch (sleepMode) {
			case "never", "off", "none", "no" -> NEVER;
			case "playback", "during playback", "while playing", "while streaming" -> PLAYBACK;
			case "running", "while running", "when running", "yes" -> RUNNING;
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
			case NEVER:
				return Messages.getString("Never");
			case PLAYBACK:
				return Messages.getString("DuringPlayback");
			case RUNNING:
				return Messages.getString("WhileRunning");
			default:
				throw new IllegalStateException("Unimplemented enum value: " + super.toString());
		}
	}
}
