package net.pms.store.container.audioaddict;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictPlaylistDto;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.external.audioaddict.Platform;
import net.pms.media.MediaInfo;
import net.pms.renderers.Renderer;
import net.pms.store.StoreResource;
import net.pms.store.StoreContainer;
import net.pms.store.item.WebAudioStream;

/**
 * A single curated playlist holding its fixed tracks. The track content URLs are signed and
 * expire, so the children are (re)resolved on every browse instead of being cached.
 */
public class AudioAddictPlaylist extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictPlaylist.class.getName());

	private final Platform network;
	private final int playlistId;

	public AudioAddictPlaylist(Renderer renderer, Platform network, AudioAddictPlaylistDto playlist) {
		super(renderer, playlist.name, playlist.albumArt);
		this.network = network;
		this.playlistId = playlist.id;
	}

	@Override
	public void discoverChildren() {
		addTracks();
	}

	@Override
	public boolean isRefreshNeeded() {
		// Content URLs are time limited, so always refetch fresh ones on browse.
		return true;
	}

	@Override
	public void doRefreshChildren() {
		clearChildren();
		addTracks();
	}

	private void addTracks() {
		List<AudioAddictTrackDto> tracks = AudioAddictService.get().getPlaylistTracks(network, playlistId);
		LOGGER.debug("{} : adding {} tracks for playlist {}.", network.displayName, tracks.size(), playlistId);
		for (AudioAddictTrackDto track : tracks) {
			MediaInfo mi = new MediaInfo();
			mi.setMimeType("audio/mpeg");
			mi.setMediaParser("STATIC");
			String title = track.artist != null ? (track.artist + " - " + track.title) : track.title;
			StoreResource sr = new WebAudioStream(renderer, title, track.contentUrl, track.albumArt);
			sr.setMediaInfo(mi);
			addChild(sr);
		}
	}
}
