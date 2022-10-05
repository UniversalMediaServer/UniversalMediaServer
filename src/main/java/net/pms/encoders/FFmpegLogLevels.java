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
package net.pms.encoders;

import java.util.HashMap;
import java.util.Map;

public enum FFmpegLogLevels {
	//Show nothing at all; be silent.
	QUIET("quiet", -8),
	//Only show fatal errors which could lead the process to crash, such as an assertion failure. This is not currently used for anything.
	PANIC("panic", 0),
	//Only show fatal errors. These are errors after which the process absolutely cannot continue.
	FATAL("fatal", 8),
	//Show all errors, including ones which can be recovered from.
	ERROR("error", 16),
	//Show all warnings and errors. Any message related to possibly incorrect or unexpected events will be shown.
	WARNING("warning", 24),
	//Show informative messages during processing. This is in addition to warnings and errors. This is the default value.
	INFO("info", 32),
	//Same as info, except more verbose.
	VERBOSE("verbose", 40),
	//Show everything, including debugging information.
	DEBUG("debug", 48),
	TRACE("trace", 56);

	public final int level;
	public final String label;

	private FFmpegLogLevels(String label, int level) {
		this.label = label;
		this.level = level;
	}

	public boolean isMoreVerboseThan(FFmpegLogLevels otherFFmpegLogLevel) {
		if (otherFFmpegLogLevel == null) {
			return true;
		}
		return otherFFmpegLogLevel.level < level;
	}

	private static final Map<String, FFmpegLogLevels> BY_LABEL = new HashMap<>();

	static {
		for (FFmpegLogLevels e : values()) {
			BY_LABEL.put(e.label, e);
		}
	}

	public static String[] getLabels() {
		return BY_LABEL.keySet().toArray(new String[0]);
	}

	public static FFmpegLogLevels valueOfLabel(String label) {
		return BY_LABEL.get(label);
	}

	public static FFmpegLogLevels valueOfLevel(int level) {
		for (FFmpegLogLevels e : values()) {
			if (e.level == level) {
				return e;
			}
		}
		return null;
	}
}