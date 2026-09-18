package net.pms.store.container.audioaddict;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictPlaylistDto;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

/**
 * The curated playlists this member follows.
 */
public class AudioAddictFollowedPlaylists extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictFollowedPlaylists.class.getName());

	private final Platform network;
	private volatile boolean populated;

	public AudioAddictFollowedPlaylists(Renderer renderer, Platform network) {
		super(renderer, "Followed", network.albumArt);
		this.network = network;
	}

	@Override
	public synchronized void discoverChildren() {
		if (populated) {
			return;
		}
		addPlaylists();
		populated = true;
	}

	@Override
	public synchronized void doRefreshChildren() {
		clearChildren();
		addPlaylists();
		populated = true;
	}

	private void addPlaylists() {
		boolean loop = renderer.getUmsConfiguration().isAudioAddictPlaylistLoop();
		List<AudioAddictPlaylistDto> playlists = AudioAddictService.get().getFollowedPlaylists(network);
		LOGGER.debug("{} : adding {} followed playlists.", network.displayName, playlists.size());
		for (AudioAddictPlaylistDto playlist : playlists) {
			addChild(new AudioAddictPlaylistStream(renderer, network, playlist, loop));
		}
	}
}
