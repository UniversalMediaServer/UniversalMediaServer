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

import com.google.gson.Gson;
import java.util.HashMap;
import net.pms.renderers.devices.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebPlayer extends LogicalPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebPlayer.class);
	private static final Gson GSON = new Gson();
	private HashMap<String, String> data;

	public WebPlayer(WebRender renderer) {
		super(renderer);
		data = new HashMap<>();
		LOGGER.debug("Created web player for " + renderer.getRendererName());
	}

	@Override
	public void setURI(String uri, String metadata) {
		PlaylistItem item = resolveURI(uri, metadata);
		if (item != null) {
			DLNAResource r = DLNAResource.getValidResource(item.uri, item.name, renderer);
			if (r != null) {
				((WebRender) renderer).push("seturl", "/play/" + r.getId());
				return;
			}
		}
		LOGGER.debug("Bad uri " + uri);
	}

	@Override
	public void pause() {
		((WebRender) renderer).push("control", "pause");
	}

	@Override
	public void play() {
		((WebRender) renderer).push("control", "play");
	}

	@Override
	public void stop() {
		((WebRender) renderer).push("control", "stop");
	}

	@Override
	public void mute() {
		((WebRender) renderer).push("control", "mute");
	}

	@Override
	public void setVolume(int volume) {
		((WebRender) renderer).push("control", "setvolume", "" + volume);
	}

	@Override
	public int getControls() {
		return renderer.getPmsConfiguration().useWebControl() ? PLAYCONTROL | VOLUMECONTROL : 0;
	}

	@Override
	public void start() {
		DLNAResource d = renderer.getPlayingRes();
		state.name = d.getDisplayName();
		if (d.getMedia() != null) {
			state.duration = StringUtil.shortTime(d.getMedia().getDurationString(), 4);
		}
	}

	public void setDataFromJson(String jsonData) {
		data = GSON.fromJson(jsonData, HashMap.class);
		String s = data.get("playback");
		if (s != null) {
			state.playback = switch (s) {
				case "STOPPED" -> STOPPED;
				case "PLAYING" -> PLAYING;
				case "PAUSED" -> PAUSED;
				default -> -1;
			};
		}
		s = data.get("mute");
		if (s != null) {
			state.mute = !"0".equals(data.get("mute"));
		}
		s = data.get("volume");
		if (s != null) {
			try {
				state.volume = StringUtil.hasValue(s) ? Integer.parseInt(s) : 0;
			} catch (NumberFormatException e) {
				LOGGER.debug("Unexpected volume value \"{}\"", data.get("volume"));
			}
		}
		if (state.playback == STOPPED) {
			state.position = "";
		} else {
			s = data.get("position");
			if (s != null) {
				try {
					long seconds = Integer.parseInt(s);
					state.position = DurationFormatUtils.formatDuration(seconds * 1000, "HH:mm:ss");
				} catch (NumberFormatException e) {
					LOGGER.debug("Unexpected position value \"{}\"", data.get("position"));
				}
			}
		}
		alert();
		if (state.playback == STOPPED) {
			((WebRender) renderer).stop();
		}
	}
}
