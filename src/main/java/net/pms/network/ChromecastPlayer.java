package net.pms.network;

import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.Media;
import su.litvak.chromecast.api.v2.MediaStatus;
import su.litvak.chromecast.api.v2.Status;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ChromecastPlayer extends UPNPHelper.Player {
	private static final String MediaPlayer = "CC1AD845";
	private static final Logger LOGGER = LoggerFactory.getLogger(ChromecastPlayer.class);
	private ChromeCast api;
	private Thread poller;

	public ChromecastPlayer(DeviceConfiguration d, ChromeCast api) {
		super(d);
		this.api = api;
	}

	@Override
	public void setURI(String uri, String metadata) {
		try {
			// this is a bit bad design but what the heck
			String id = uri.substring(uri.indexOf("get/") + 4);
			DLNAResource r = PMS.get().getRootFolder(renderer).getDLNAResource(id, renderer);
			if (r == null) {
				LOGGER.debug("Bad media in cc seturi " + id);
				return;
			}
			String mime = r.mimeType();
			api.launchApp(MediaPlayer);
			api.load("", null, uri, mime);
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast load " + e);
		}
	}

	@Override
	public void play() {
		try {
			api.play();
			forceStop = false;
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
			forceStop = true;
		} catch (IOException e) {
			LOGGER.debug("Bad chromecast stop " + e);
		}
	}

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
		return BasicPlayer.PLAYCONTROL | BasicPlayer.VOLUMECONTROL;
	}

	private int translateState(MediaStatus.PlayerState s) {
		switch(s) {
			case IDLE: return STOPPED;
			case PLAYING: // buffering is a kind of playing
			case BUFFERING: return PLAYING;
			case PAUSED: return PAUSED;
			default: return -1;
		}
	}

	public String getName() throws IOException {
		if (StringUtils.isNotEmpty(api.getName())) {
			return api.getName();
		}
		throw new IOException("xxx");
	}

	public void startPoll() {
		final ChromecastPlayer player = this;
		Runnable r = new Runnable() {
			@Override
			public void run() {
				for(;;) {
					try {
						Thread.sleep(1000);
						Status s1 = api.getStatus();
						if (!s1.isAppRunning(MediaPlayer)) {
							continue;
						}
						State s = getState();
						MediaStatus status = api.getMediaStatus();
						if (status == null) {
							continue;
						}
						s.playback = translateState(status.playerState);
						Media m = status.media;
						if (m != null) {
							if (m.url != null) {
								s.uri = status.media.url;
							}
							if(m.duration != null) {
								s.duration = StringUtil.convertTimeToString(status.media.duration, "%02d:%02d:%02.0f");
							}
						}
						s.position = StringUtil.convertTimeToString(status.currentTime, "%02d:%02d:%02.0f");
						if (status.volume != null) {
							s.volume = status.volume.level.intValue();
							s.mute = status.volume.muted;
						}
					} catch (Exception e) {
						LOGGER.debug("Bad chromecast mediastate " + e);
					}
					if (renderer.gui != null) {
						// ugly hack
						renderer.gui.actionPerformed(new ActionEvent(player, 0, null));
					}
				}
			}
		};
		poller = new Thread(r);
		poller.start();
	}
}
