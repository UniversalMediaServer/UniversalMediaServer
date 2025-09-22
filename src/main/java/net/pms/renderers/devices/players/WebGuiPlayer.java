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

import java.util.Map;
import net.pms.network.StartStopListener;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.WebGuiRenderer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebGuiPlayer extends LogicalPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebGuiPlayer.class);

	private StoreItem playingRes;

	public WebGuiPlayer(WebGuiRenderer renderer) {
		super(renderer);
		LOGGER.debug("Created web gui player for " + renderer.getRendererName());
	}

	@Override
	public void setURI(String uri, String metadata) {
		PlaylistItem item = resolveURI(uri, metadata);
		if (item != null) {
			StoreResource r = renderer.getMediaStore().getValidResource(item.getUri(), item.getName());
			if (r != null) {
				((WebGuiRenderer) renderer).sendMessage("setPlayId", r.getId());
				return;
			}
		}
		LOGGER.debug("Bad uri " + uri);
	}

	@Override
	public void pause() {
		((WebGuiRenderer) renderer).sendMessage("pause");
	}

	@Override
	public void play() {
		((WebGuiRenderer) renderer).sendMessage("play");
	}

	@Override
	public void stop() {
		((WebGuiRenderer) renderer).sendMessage("stop");
	}

	@Override
	public void mute() {
		((WebGuiRenderer) renderer).sendMessage("mute");
	}

	@Override
	public void setVolume(int volume) {
		((WebGuiRenderer) renderer).sendMessage("setvolume", "" + volume);
	}

	@Override
	public int getControls() {
		return renderer.getUmsConfiguration().isWebPlayerControllable() ? Renderer.PLAYCONTROL | Renderer.VOLUMECONTROL : 0;
	}

	@Override
	public void start() {
		playingRes = renderer.getPlayingRes();
		state.setName(playingRes.getDisplayName());
		if (playingRes.getMediaInfo() != null) {
			state.setDuration(StringUtil.shortTime(playingRes.getMediaInfo().getDurationString(), 4));
		}
	}

	public void updateStatus(Map<String, String> status) {
		String s = status.get("playback");
		if (s != null) {
			state.setPlayback(
				switch (s) {
					case "STOPPED" -> PlayerState.STOPPED;
					case "PLAYING" -> PlayerState.PLAYING;
					case "PAUSED" -> PlayerState.PAUSED;
					default -> PlayerState.UNKNOWN;
				}
			);
		}
		s = status.get("mute");
		if (s != null) {
			state.setMuted(!"0".equals(status.get("mute")));
		}
		s = status.get("volume");
		if (s != null) {
			try {
				state.setVolume(StringUtil.hasValue(s) ? Integer.parseInt(s) : 0);
			} catch (NumberFormatException e) {
				LOGGER.debug("Unexpected volume value \"{}\"", status.get("volume"));
			}
		}
		if (state.isStopped()) {
			state.setPosition("");
		} else {
			s = status.get("position");
			if (s != null) {
				try {
					long seconds = Integer.parseInt(s);
					state.setPosition(seconds * 1000);
					if (playingRes != null) {
						playingRes.setLastStartPosition(seconds);
						if (playingRes.getMediaStatus() != null) {
							playingRes.getMediaStatus().setLastPlaybackPosition(seconds);
						}
					}
				} catch (NumberFormatException e) {
					LOGGER.debug("Unexpected position value \"{}\"", s);
				} catch (Exception e) {
					LOGGER.debug("Exception \"{}\"", s);
				}
			}
			if ((state.isPlaying() || state.isPaused()) && playingRes != null) {
				StartStopListener startStopListener = new StartStopListener(renderer.getUUID(), playingRes);
				startStopListener.start();
				startStopListener.stop();
			}
		}
		alert();
	}

}
