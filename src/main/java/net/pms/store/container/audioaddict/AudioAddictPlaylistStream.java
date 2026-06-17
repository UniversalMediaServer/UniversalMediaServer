package net.pms.store.container.audioaddict;

import java.io.InputStream;
import net.pms.encoders.TranscodingSettings;
import net.pms.external.audioaddict.AudioAddictPlaylistDto;
import net.pms.external.audioaddict.Platform;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.item.WebAudioStream;

/**
 * A curated playlist represented as a single, continuously playable item. Playing it streams
 * the playlist tracks back to back via AudioAddictPlaylistInputStream. AudioAddict doesn't
 * expose the playlist items, so we have to imitate a web player behaviour.
 */
public class AudioAddictPlaylistStream extends WebAudioStream {

	private final Platform network;
	private final int playlistId;
	private final boolean loop;

	public AudioAddictPlaylistStream(Renderer renderer, Platform network, AudioAddictPlaylistDto playlist, boolean loop) {
		super(renderer, playlist.name,
			String.format("https://api.audioaddict.com/v1/%s/playlists/%d/play", network.shortName, playlist.id),
			playlist.albumArt);
		this.network = network;
		this.playlistId = playlist.id;
		this.loop = loop;

		MediaInfo mi = new MediaInfo();
		mi.setMimeType("audio/mpeg");
		mi.setMediaParser("AudioAddictPlaylistStream");
		if (playlist.description != null) {
			MediaAudioMetadata md = new MediaAudioMetadata();
			md.setAlbum(playlist.description);
			mi.setAudioMetadata(md);
		}
		setMediaInfo(mi);
	}

	@Override
	public InputStream getInputStream() {
		return new AudioAddictPlaylistInputStream(network, playlistId, loop);
	}

	/**
	 * The stream is already transcoded to MP3 by the API, so no transcoding settings. If transcoding is needed, we need
	 * to investigate, how to glue things together.
	 */
	@Override
	public TranscodingSettings resolveTranscodingSettings() {
		return null;
	}
}
