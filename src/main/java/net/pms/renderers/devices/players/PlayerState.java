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
package net.pms.renderers.devices.players;

import net.pms.util.StringUtil;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class PlayerState extends PlayerItem {
	static final int UNKNOWN = -1;
	static final int STOPPED = 0;
	static final int PLAYING = 1;
	static final int PAUSED = 2;

	private int playback;
	private boolean mute;
	private int volume;
	private String position;
	private String duration;
	private long buffer;

	public int getPlayback() {
		return playback;
	}

	public void setPlayback(int value) {
		playback = value;
	}

	public boolean isUnknown() {
		return playback == UNKNOWN;
	}

	public boolean isStopped() {
		return playback == STOPPED;
	}

	public boolean isPlaying() {
		return playback == PLAYING;
	}

	public boolean isPaused() {
		return playback == PAUSED;
	}

	public boolean isMuted() {
		return mute;
	}

	public void setMuted(boolean value) {
		mute = value;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int value) {
		volume = value;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String value) {
		duration = value;
	}

	public void setDuration(long value) {
		duration = DurationFormatUtils.formatDuration(value, "HH:mm:ss");
	}

	public void setDuration(double value) {
		duration = StringUtil.convertTimeToString(value, "%02d:%02d:%02.0f");
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String value) {
		position = value;
	}

	public void setPosition(long value) {
		position = DurationFormatUtils.formatDuration(value, "HH:mm:ss");
	}

	public void setPosition(double value) {
		position = StringUtil.convertTimeToString(value, "%02d:%02d:%02.0f");
	}

	public long getBuffer() {
		return buffer;
	}

	public void setBuffer(long value) {
		buffer = value;
	}

	public void reset() {
		playback = STOPPED;
		position = "";
		duration = "";
		name = " ";
		buffer = 0;
	}
}
