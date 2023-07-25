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
package net.pms.media;

/**
 * This class keeps track of media file status.
 */
public class MediaStatus {
	private Double lastPlaybackPosition = null;
	private String lastPlaybackTime;
	private int playbackCount = 0;

	public int getPlaybackCount() {
		return playbackCount;
	}

	public void setPlaybackCount(int value) {
		this.playbackCount = value;
	}

	public Double getLastPlaybackPosition() {
		return lastPlaybackPosition;
	}

	public String getLastPlaybackPositionForUPnP() {
		if (lastPlaybackPosition == null) {
			return null;
		}

		int secondsValue = lastPlaybackPosition.intValue();

		int seconds = secondsValue % 60;
		int hours = secondsValue / 60;
		int minutes = hours % 60;
		hours = hours / 60;

		String hoursString = String.valueOf(hours);
		String minutesString = String.valueOf(minutes);
		String secondsString = String.valueOf(seconds);

		if (minutesString.length() == 1) {
			minutesString = "0" + minutesString;
		}

		if (secondsString.length() == 1) {
			secondsString = "0" + secondsString;
		}

		return hoursString + ":" + minutesString + ":" + secondsString + ".000";
	}

	public void setLastPlaybackPosition(double value) {
		this.lastPlaybackPosition = value;
	}

	public String getLastPlaybackTime() {
		return lastPlaybackTime;
	}

	public void setLastPlaybackTime(String value) {
		this.lastPlaybackTime = value;
	}
}
