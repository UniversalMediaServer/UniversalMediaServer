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

import java.awt.event.ActionEvent;
import java.util.Map;
import net.pms.renderers.JUPnPDeviceHelper;
import net.pms.renderers.Renderer;
import net.pms.store.StoreResource;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.model.meta.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A logical player to manage upnp playback
public class UPNPPlayer extends LogicalPlayer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UPNPPlayer.class);

	protected Device dev;
	protected String uuid;
	protected String instanceID;
	protected Map<String, String> data;
	protected String lastUri;
	private boolean ignoreUpnpDuration;

	public UPNPPlayer(Renderer renderer) {
		super(renderer);
		uuid = renderer.getUUID();
		dev = JUPnPDeviceHelper.getDevice(uuid);
		data = renderer.connect(this);
		lastUri = null;
		ignoreUpnpDuration = false;
		LOGGER.debug("Created upnp player for " + renderer.getRendererName());
		refresh();
	}

	@Override
	public void setURI(String uri, String metadata) {
		PlaylistItem item = resolveURI(uri, metadata);
		if (item != null) {
			if (item.getName() != null) {
				state.setName(item.getName());
			}
			JUPnPDeviceHelper.setAVTransportURI(dev, item.getUri(), renderer.isPushMetadata() ? item.getMetadata() : null);
		}
	}

	@Override
	public void play() {
		JUPnPDeviceHelper.play(dev);
	}

	@Override
	public void pause() {
		JUPnPDeviceHelper.pause(dev);
	}

	@Override
	public void stop() {
		JUPnPDeviceHelper.stop(dev);
	}

	@Override
	public void forward() {
		JUPnPDeviceHelper.seek(dev, JUPnPDeviceHelper.REL_TIME, jump(60));
	}

	@Override
	public void rewind() {
		JUPnPDeviceHelper.seek(dev, JUPnPDeviceHelper.REL_TIME, jump(-60));
	}

	@Override
	public void mute() {
		JUPnPDeviceHelper.setMute(dev, !state.isMuted());
	}

	@Override
	public void setVolume(int volume) {
		JUPnPDeviceHelper.setVolume(dev, volume * maxVol / 100);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (renderer.isUpnpConnected()) {
			refresh();
		} else if (!state.isStopped()) {
			reset();
		}
	}

	public void refresh() {
		String s = data.get("TransportState");
		state.setPlayback(
			switch (s) {
				case "STOPPED" -> PlayerState.STOPPED;
				case "PLAYING" -> PlayerState.PLAYING;
				case "PAUSED_PLAYBACK" -> PlayerState.PAUSED;
				default -> PlayerState.UNKNOWN;
			}
		);
		state.setMuted(!"0".equals(data.get("Mute")));
		s = data.get("Volume");
		state.setVolume(s == null ? 0 : (Integer.parseInt(s) * 100 / maxVol));
		state.setPosition(data.get("RelTime"));
		if (!ignoreUpnpDuration) {
			state.setDuration(data.get("CurrentMediaDuration"));
		}
		state.setUri(data.get("AVTransportURI"));
		state.setMetadata(data.get("AVTransportURIMetaData"));

		// update playlist only if uri has changed
		if (!StringUtils.isBlank(state.getUri()) && !state.getUri().equals(lastUri)) {
			playlist.set(state.getUri(), null, state.getMetadata());
		}
		lastUri = state.getUri();
		alert();
	}

	@Override
	public void start() {
		StoreResource d = renderer.getPlayingRes();
		state.setName(d.getDisplayName());
		if (d.getMediaInfo() != null) {
			String duration = d.getMediaInfo().getDurationString();
			ignoreUpnpDuration = !StringUtil.isZeroTime(duration);
			if (ignoreUpnpDuration) {
				state.setDuration(StringUtil.shortTime(d.getMediaInfo().getDurationString(), 4));
			}
		}
	}

	@Override
	public void close() {
		renderer.disconnect(this);
		super.close();
	}

	public String jump(double seconds) {
		double t = StringUtil.convertStringToTime(state.getPosition()) + seconds;
		return t > 0 ? StringUtil.convertTimeToString(t, "%02d:%02d:%02.0f") : "00:00:00";
	}
}
