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
 * Collection of all curated playlists of a radio network (e.g. "Ambient Guitar"). In contrast
 * to the live radio channels these are fixed track lists.
 */
public class AudioAddictPlaylists extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictPlaylists.class.getName());

	private final Platform network;

	public AudioAddictPlaylists(Renderer renderer, Platform network) {
		super(renderer, "Playlists", network.albumArt);
		this.network = network;
	}

	@Override
	public void discoverChildren() {
		List<AudioAddictPlaylistDto> playlists = AudioAddictService.get().getPlaylists(network);
		LOGGER.debug("{} : adding {} playlists.", network.displayName, playlists.size());
		for (AudioAddictPlaylistDto playlist : playlists) {
			addChild(new AudioAddictPlaylist(renderer, network, playlist));
		}
	}
}
