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

import net.pms.configuration.DeviceConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaybackTimer extends MinimalPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlaybackTimer.class);
	private long duration = 0;

	public PlaybackTimer(DeviceConfiguration renderer) {
		super(renderer);
		LOGGER.debug("Created playback timer for " + renderer.getRendererName());
	}

	@Override
	public void start() {
		final DLNAResource res = renderer.getPlayingRes();
		state.name = res.getDisplayName();
		duration = 0;
		if (res.getMedia() != null) {
			duration = (long) res.getMedia().getDurationInSeconds() * 1000;
			state.duration = DurationFormatUtils.formatDuration(duration, "HH:mm:ss");
		}
		Runnable r = () -> {
			state.playback = PLAYING;
			while (res == renderer.getPlayingRes()) {
				long elapsed;
				if ((long) res.getLastStartPosition() == 0) {
					elapsed = System.currentTimeMillis() - res.getStartTime();
				} else {
					elapsed = System.currentTimeMillis() - (long) res.getLastStartSystemTime();
					elapsed += (long) (res.getLastStartPosition() * 1000);
				}

				if (duration == 0 || elapsed < duration + 500) {
					// Position is valid as far as we can tell
					state.position = DurationFormatUtils.formatDuration(elapsed, "HH:mm:ss");
				} else {
					// Position is invalid, blink instead
					state.position = ("NOT_IMPLEMENTED" + (elapsed / 1000 % 2 == 0 ? "  " : "--"));
				}
				alert();
				UMSUtils.sleep(1000);
			}
			// Reset only if another item hasn't already begun playing
			if (renderer.getPlayingRes() == null) {
				reset();
			}
		};
		new Thread(r).start();
	}
}
