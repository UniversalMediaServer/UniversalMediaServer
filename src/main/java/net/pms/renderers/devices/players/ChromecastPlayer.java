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

import java.io.IOException;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.Media;
import su.litvak.chromecast.api.v2.MediaStatus;
import su.litvak.chromecast.api.v2.Status;

public class ChromecastPlayer extends LogicalPlayer {
	private static final String MEDIA_PLAYER = "CC1AD845";
	private static final Logger LOGGER = LoggerFactory.getLogger(ChromecastPlayer.class);
	private final ChromeCast api;

	public ChromecastPlayer(Renderer renderer, ChromeCast api) {
		super(renderer);
		this.api = api;
	}

	@Override
	public void setURI(String uri, String metadata) {
		PlaylistItem playlistItem = resolveURI(uri, metadata);
		if (playlistItem != null) {
			// this is a bit circular but what the heck
			StoreResource r = renderer.getMediaStore().getValidResource(playlistItem.getUri(), playlistItem.getName());
			StoreItem item = r instanceof StoreItem libraryItem ? libraryItem : null;
			if (item == null) {
				LOGGER.debug("Bad media in cc seturi: " + uri);
				return;
			}
			try {
				api.launchApp(MEDIA_PLAYER);
				api.load("", null, playlistItem.getUri(), item.getMimeType());
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
		return Renderer.PLAYCONTROL | Renderer.VOLUMECONTROL;
	}

	private int translateState(MediaStatus.PlayerState s) {
		return switch (s) {
			case IDLE -> PlayerState.STOPPED;
			// buffering is a kind of playing
			case PLAYING, BUFFERING -> PlayerState.PLAYING;
			case PAUSED -> PlayerState.PAUSED;
			default -> -1;
		};
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
					state.setPlayback(translateState(status.playerState));
					Media m = status.media;
					if (m != null) {
						if (m.url != null) {
							state.setUri(status.media.url);
						}
						if (m.duration != null) {
							state.setDuration(status.media.duration);
						}
					}
					state.setPosition(status.currentTime);
					if (status.volume != null) {
						state.setVolume(status.volume.level.intValue());
						state.setMuted(status.volume.muted);
					}
					alert();
				} catch (IOException e) {
					LOGGER.debug("Bad chromecast mediastate " + e);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};

		new Thread(r).start();
	}
}
