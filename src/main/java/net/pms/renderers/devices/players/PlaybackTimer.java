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
package net.pms.renderers.devices.players;

import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaybackTimer extends MinimalPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlaybackTimer.class);

	public PlaybackTimer(Renderer renderer) {
		super(renderer);
		LOGGER.debug("Created playback timer for " + renderer.getRendererName());
	}

	@Override
	public void start() {
		final StoreItem res = renderer.getPlayingRes();
		state.setName(res.getDisplayName());
		final long duration;
		if (res.getMediaInfo() != null) {
			duration = (long) res.getMediaInfo().getDurationInSeconds() * 1000;
			state.setDuration(duration);
		} else {
			duration = 0;
		}
		Runnable r = () -> {
			state.setPlayback(PlayerState.PLAYING);
			while (res == renderer.getPlayingRes()) {
				long elapsed = System.currentTimeMillis() - res.getLastStartSystemTime();
				if ((long) res.getLastStartPosition() != 0) {
					elapsed += (long) (res.getLastStartPosition() * 1000);
				}

				if (duration == 0 || elapsed < duration + 500) {
					// Position is valid as far as we can tell
					state.setPosition(elapsed);
				} else {
					// Position is invalid, blink instead
					state.setPosition("NOT_IMPLEMENTED" + (elapsed / 1000 % 2 == 0 ? "  " : "--"));
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
