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
package net.pms.network.mediaserver.mdns.chromecast;

import java.io.IOException;
import net.pms.configuration.DeviceConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.Media;
import su.litvak.chromecast.api.v2.MediaStatus;
import su.litvak.chromecast.api.v2.Status;

public class ChromecastPlayer extends BasicPlayer.Logical {
	private static final String MEDIA_PLAYER = "CC1AD845";
	private static final Logger LOGGER = LoggerFactory.getLogger(ChromecastPlayer.class);
	private final ChromeCast api;
	private Thread poller;

	public ChromecastPlayer(DeviceConfiguration d, ChromeCast api) {
		super(d);
		this.api = api;
	}

	@Override
	public void setURI(String uri, String metadata) {
		Playlist.Item item = resolveURI(uri, metadata);
		if (item != null) {
			// this is a bit circular but what the heck
			DLNAResource r = DLNAResource.getValidResource(item.uri, item.name, renderer);
			if (r == null) {
				LOGGER.debug("Bad media in cc seturi: " + uri);
				return;
			}
			try {
				api.launchApp(MEDIA_PLAYER);
				api.load("", null, item.uri, r.mimeType());
			} catch (IOException e) {
				LOGGER.debug("Bad chromecast load: " + e);
			}
		}
	}

	@Override
	public void play() {
		try {
			api.play();
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast play " + e);
		}
	}

	@Override
	public void pause() {
		try {
			api.pause();
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast pause " + e);
		}
	}

	@Override
	public void stop() {
		try {
			api.stopApp();
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast stop " + e);
		}
	}

	@Override
	public void forward() {
		try {
			api.seek(60);
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast fwd " + e);
		}
	}

	@Override
	public void rewind() {
		try {
			api.seek(-60);
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast rwd " + e);
		}
	}

	@Override
	public void setVolume(int volume) {
		try {
			api.setVolume(volume);
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast volume " + e);
		}
	}

	@Override
	public int getControls() {
		return PLAYCONTROL | VOLUMECONTROL;
	}

	private int translateState(MediaStatus.PlayerState s) {
		switch (s) {
			case IDLE: return STOPPED;
			case PLAYING: // buffering is a kind of playing
			case BUFFERING: return PLAYING;
			case PAUSED: return PAUSED;
			default: return -1;
		}
	}

	public void startPoll() {
		Runnable r = () -> {
			for (;;) {
				try {
					Thread.sleep(1000);
					Status s1 = api.getStatus();
					if (s1 == null || !s1.isAppRunning(MEDIA_PLAYER)) {
						continue;
					}
					MediaStatus status = api.getMediaStatus();
					if (status == null) {
						continue;
					}
					state.playback = translateState(status.playerState);
					Media m = status.media;
					if (m != null) {
						if (m.url != null) {
							state.uri = status.media.url;
						}
						if (m.duration != null) {
							state.duration = StringUtil.convertTimeToString(status.media.duration, "%02d:%02d:%02.0f");
						}
					}
					state.position = StringUtil.convertTimeToString(status.currentTime, "%02d:%02d:%02.0f");
					if (status.volume != null) {
						state.volume = status.volume.level.intValue();
						state.mute = status.volume.muted;
					}
					alert();
				} catch (InterruptedException | IOException e) {
					LOGGER.debug("Bad chromecast mediastate " + e);
				}
			}
		};

		poller = new Thread(r);
		poller.start();
	}
}
