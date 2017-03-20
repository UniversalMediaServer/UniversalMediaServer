/*
 * Universal Media Server, for streaming any medias to DLNA
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

/**
 * Defines a set of actions for when a media is fully played:
 * <ul>
 * <li>0 = Do nothing</li>
 * <li>1 = Mark media with thumbnail overlay or text</li>
 * <li>2 = Hide the file if video</li>
 * <li>3 = Move the file to a different folder</li>
 * <li>4 = Move the file to the recycle/trash bin</li>
 * </ul>
 * The {@link FullyPlayedAction} class is final and cannot be sub-classed.</p>
 */
public class FullyPlayedAction {
	public static final int NO_ACTION_INT = 0;
	public static final int MARK_INT = 1;
	public static final int HIDE_VIDEO_INT = 2;
	public static final int MOVE_FOLDER_INT = 3;
	public static final int MOVE_TRASH_INT = 4;

	public static final Integer NO_ACTION_INTEGER = NO_ACTION_INT;
	public static final Integer MARK_INTEGER = MARK_INT;
	public static final Integer HIDE_VIDEO_INTEGER = HIDE_VIDEO_INT;
	public static final Integer MOVE_FOLDER_INTEGER = MOVE_FOLDER_INT;
	public static final Integer MOVE_TRASH_INTEGER = MOVE_TRASH_INT;

	/**
	 * <code>NO_ACTION</code> nothing should be done when fully played.
	 */
	public static final FullyPlayedAction NO_ACTION = new FullyPlayedAction(NO_ACTION_INT, "No action");

	/**
	 * <code>MARK</code> media should be marked with thumbnail overlay or
	 * text when fully played.
	 */
	public static final FullyPlayedAction MARK = new FullyPlayedAction(MARK_INT, "Mark");

	/**
	 * <code>HIDE_VIDEO</code> video media should be hidden when fully
	 * played.
	 */
	public static final FullyPlayedAction HIDE_VIDEO = new FullyPlayedAction(HIDE_VIDEO_INT, "Hide video");

	/**
	 * <code>MOVE_FOLDER</code> file should be moved to a folder when fully
	 * played.
	 */
	public static final FullyPlayedAction MOVE_FOLDER = new FullyPlayedAction(MOVE_FOLDER_INT, "Move to folder");

	/**
	 * Use <code>MOVE_TRASH</code> file should be moved to recycle/trash bin
	 * if possible when fully played.
	 */
	public static final FullyPlayedAction MOVE_TRASH = new FullyPlayedAction(MOVE_TRASH_INT, "Move to trash");

	public final int FullyPlayedActionInt;
	public final String FullyPlayedActionStr;

	/**
	 * Instantiate a {@link FullyPlayedAction} object.
	 */
	private FullyPlayedAction(int FullyPlayedActionInt, String FullyPlayedActionStr) {
		this.FullyPlayedActionInt = FullyPlayedActionInt;
		this.FullyPlayedActionStr = FullyPlayedActionStr;
	}

	/**
	 * Returns the string representation of this {@link FullyPlayedAction}.
	 */
	@Override
	public String toString() {
		return FullyPlayedActionStr;
	}

	/**
	 * Returns the integer representation of this {@link FullyPlayedAction}.
	 */
	public int toInt() {
		return FullyPlayedActionInt;
	}

	/**
	 * Convert a {@link FullyPlayedAction} to an {@link Integer} object.
	 *
	 * @return This {@link FullyPlayedAction}'s Integer mapping.
	 */
	public Integer toInteger() {
		switch (FullyPlayedActionInt) {
			case NO_ACTION_INT:
				return NO_ACTION_INTEGER;
			case MARK_INT:
				return MARK_INTEGER;
			case HIDE_VIDEO_INT:
				return HIDE_VIDEO_INTEGER;
			case MOVE_FOLDER_INT:
				return MOVE_FOLDER_INTEGER;
			case MOVE_TRASH_INT:
				return MOVE_TRASH_INTEGER;
			default:
				throw new IllegalStateException("FullyPlayedAction " + FullyPlayedActionStr + ", " + FullyPlayedActionInt + " is unknown.");
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link FullyPlayedAction}. If the conversion fails, this method
	 * returns {@link #NO_ACTION}.
	 */
	public static FullyPlayedAction toFullyPlayedAction(String sArg) {
		return toFullyPlayedAction(sArg, FullyPlayedAction.NO_ACTION);
	}

	/**
	 * Converts the integer passed as argument to a
	 * {@link FullyPlayedAction}. If the conversion fails, this method
	 * returns {@link #NO_ACTION}.
	 */
	public static FullyPlayedAction toFullyPlayedAction(int val) {
		return toFullyPlayedAction(val, FullyPlayedAction.NO_ACTION);
	}

	/**
	 * Converts the integer passed as argument to a
	 * {@link FullyPlayedAction}. If the conversion fails, this method
	 * returns the specified default.
	 */
	public static FullyPlayedAction toFullyPlayedAction(int val, FullyPlayedAction defaultFullyPlayedAction) {
		switch (val) {
			case NO_ACTION_INT:
				return NO_ACTION;
			case MARK_INT:
				return MARK;
			case HIDE_VIDEO_INT:
				return HIDE_VIDEO;
			case MOVE_FOLDER_INT:
				return MOVE_FOLDER;
			case MOVE_TRASH_INT:
				return MOVE_TRASH;
			default:
				return defaultFullyPlayedAction;
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link FullyPlayedAction}. If the conversion fails, this method
	 * returns the specified default.
	 */
	public static FullyPlayedAction toFullyPlayedAction(String sArg, FullyPlayedAction defaultFullyPlayedAction) {
		if (sArg == null) {
			return defaultFullyPlayedAction;
		}

		sArg = sArg.toLowerCase();
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + FullyPlayedActionInt;
		result = prime * result + ((FullyPlayedActionStr == null) ? 0 : FullyPlayedActionStr.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FullyPlayedAction)) {
			return false;
		}
		FullyPlayedAction other = (FullyPlayedAction) obj;
		if (FullyPlayedActionInt != other.FullyPlayedActionInt) {
			return false;
		}
		if (FullyPlayedActionStr == null) {
			if (other.FullyPlayedActionStr != null) {
				return false;
			}
		} else if (!FullyPlayedActionStr.equals(other.FullyPlayedActionStr)) {
			return false;
		}
		return true;
	}
}
