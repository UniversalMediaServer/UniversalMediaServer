/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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
package net.pms.util;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 * Defines a set of actions for when a media is fully played.
 */
public enum FullyPlayedAction {

	/** Do nothing when fully played. */
	NO_ACTION(0),

	/** Mark media with thumbnail overlay or text when fully played. */
	MARK(1),

	/** Hide the media if video when fully played. */
	HIDE_VIDEO(2),

	/** Move the file to a different folder when fully played. */
	MOVE_FOLDER(3),

	/** Move the file to the recycle/trash bin if possible when fully played. */
	MOVE_TRASH(4);

	private final int value;

	private FullyPlayedAction(int value) {
		this.value = value;
	}

	/**
	 * Returns the string representation of this {@link FullyPlayedAction}.
	 */
	@Override
	public String toString() {
		switch (this) {
			case HIDE_VIDEO:
				return "Hide video";
			case MARK:
				return "Mark";
			case MOVE_FOLDER:
				return "Move to folder";
			case MOVE_TRASH:
				return "Move to trash";
			case NO_ACTION:
				return "No action";
			default:
				throw new IllegalStateException("Unimplemented value " + super.toString() + " in FullyPlayedAction");
		}
	}

	/**
	 * @return The integer representation of this {@link FullyPlayedAction}.
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Returns the {@link FullyPlayedAction} corresponding to the given integer
	 * value, or {@code null} if the value is invalid.
	 *
	 * @param value the integer value to find.
	 * @return The corresponding value or {@code null} if the value is invalid.
	 */
	public static FullyPlayedAction typeOf(int value) {
		return typeOf(value, null);
	}

	/**
	 * Returns the {@link FullyPlayedAction} corresponding to the given integer
	 * value, or {@code defaultFullyPlayedAction} if the value is invalid.
	 *
	 * @param value the integer value to find.
	 * @param defaultFullyPlayedAction the default value to return if the value
	 *            is invalid.
	 * @return The corresponding value or {@code defaultFullyPlayedAction} if
	 *         the value is invalid.
	 */
	public static FullyPlayedAction typeOf(int value, FullyPlayedAction defaultFullyPlayedAction) {
		for (FullyPlayedAction action : FullyPlayedAction.values()) {
			if (action.value == value) {
				return action;
			}
		}
		return defaultFullyPlayedAction;
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link FullyPlayedAction}. If the conversion fails, this method returns
	 * {@code null}.
	 *
	 * @param sArg the {@link String} to try convert.
	 * @return The corresponding value or {@code null} if not match is found.
	 */
	public static FullyPlayedAction toFullyPlayedAction(String sArg) {
		return toFullyPlayedAction(sArg, null);
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link FullyPlayedAction}. If the conversion fails, this method returns
	 * {@code defaultFullyPlayedAction}.
	 *
	 * @param sArg the {@link String} to try convert.
	 * @param defaultFullyPlayedAction the default value to return if the
	 *            conversion fails.
	 * @return The corresponding value or {@code defaultFullyPlayedAction} if
	 *         not match is found.
	 */
	public static FullyPlayedAction toFullyPlayedAction(String sArg, FullyPlayedAction defaultFullyPlayedAction) {
		if (StringUtils.isBlank(sArg)) {
			return defaultFullyPlayedAction;
		}
		sArg = sArg.toLowerCase(Locale.ROOT);
		if (sArg.contains("action")) {
			return FullyPlayedAction.NO_ACTION;
		}
		if (sArg.contains("mark")) {
			return FullyPlayedAction.MARK;
		}
		if (sArg.contains("hide")) {
			return FullyPlayedAction.HIDE_VIDEO;
		}
		if (sArg.contains("folder")) {
			return FullyPlayedAction.MOVE_FOLDER;
		}
		if (sArg.contains("trash") || sArg.contains("recycle")) {
			return FullyPlayedAction.MOVE_TRASH;
		}
		return defaultFullyPlayedAction;
	}
}
