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
package net.pms.network.mediaserver;

import java.awt.event.ActionEvent;
import java.util.Map;
import net.pms.configuration.DeviceConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.meta.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A logical player to manage upnp playback
public class UPNPPlayer extends BasicPlayer.Logical {

	private static final Logger LOGGER = LoggerFactory.getLogger(UPNPPlayer.class);

	protected Device dev;
	protected String uuid;
	protected String instanceID;
	protected Map<String, String> data;
	protected String lastUri;
	private boolean ignoreUpnpDuration;

	public UPNPPlayer(DeviceConfiguration renderer) {
		super(renderer);
		uuid = renderer.getUUID();
		instanceID = renderer.getInstanceID();
		dev = UPNPControl.getDevice(uuid);
		data = UPNPControl.rendererMap.get(uuid, instanceID).connect(this);
		lastUri = null;
		ignoreUpnpDuration = false;
		LOGGER.debug("Created upnp player for " + renderer.getRendererName());
		refresh();
	}

	@Override
	public void setURI(String uri, String metadata) {
		BasicPlayer.Logical.Playlist.Item item = resolveURI(uri, metadata);
		if (item != null) {
			if (item.name != null) {
				state.name = item.name;
			}
			UPNPControl.setAVTransportURI(dev, instanceID, item.uri, renderer.isPushMetadata() ? item.metadata : null);
		}
	}

	@Override
	public void play() {
		UPNPControl.play(dev, instanceID);
	}

	@Override
	public void pause() {
		UPNPControl.pause(dev, instanceID);
	}

	@Override
	public void stop() {
		UPNPControl.stop(dev, instanceID);
	}

	@Override
	public void forward() {
		UPNPControl.seek(dev, instanceID, UPNPControl.REL_TIME, jump(60));
	}

	@Override
	public void rewind() {
		UPNPControl.seek(dev, instanceID, UPNPControl.REL_TIME, jump(-60));
	}

	@Override
	public void mute() {
		UPNPControl.setMute(dev, instanceID, !state.mute);
	}

	@Override
	public void setVolume(int volume) {
		UPNPControl.setVolume(dev, instanceID, volume * maxVol / 100);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (renderer.isUpnpConnected()) {
			refresh();
		} else if (state.playback != STOPPED) {
			reset();
		}
	}

	public void refresh() {
		String s = data.get("TransportState");
		state.playback =
				"STOPPED".equals(s) ? STOPPED :
				"PLAYING".equals(s) ? PLAYING :
				"PAUSED_PLAYBACK".equals(s) ? PAUSED :
				-1;
		state.mute = !"0".equals(data.get("Mute"));
		s = data.get("Volume");
		state.volume = s == null ? 0 : (Integer.parseInt(s) * 100 / maxVol);
		state.position = data.get("RelTime");
		if (!ignoreUpnpDuration) {
			state.duration = data.get("CurrentMediaDuration");
		}
		state.uri = data.get("AVTransportURI");
		state.metadata = data.get("AVTransportURIMetaData");

		// update playlist only if uri has changed
		if (!StringUtils.isBlank(state.uri) && !state.uri.equals(lastUri)) {
			playlist.set(state.uri, null, state.metadata);
		}
		lastUri = state.uri;
		alert();
	}

	@Override
	public void start() {
		DLNAResource d = renderer.getPlayingRes();
		state.name = d.getDisplayName();
		if (d.getMedia() != null) {
			String duration = d.getMedia().getDurationString();
			ignoreUpnpDuration = !StringUtil.isZeroTime(duration);
			if (ignoreUpnpDuration) {
				state.duration = StringUtil.shortTime(d.getMedia().getDurationString(), 4);
			}
		}
	}

	@Override
	public void close() {
		UPNPControl.rendererMap.get(uuid, instanceID).disconnect(this);
		super.close();
	}

	public String jump(double seconds) {
		double t = StringUtil.convertStringToTime(state.position) + seconds;
		return t > 0 ? StringUtil.convertTimeToString(t, "%02d:%02d:%02.0f") : "00:00:00";
	}
}
