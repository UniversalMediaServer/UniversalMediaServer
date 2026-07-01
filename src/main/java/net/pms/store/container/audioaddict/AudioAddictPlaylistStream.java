package net.pms.store.container.audioaddict;

import java.io.InputStream;
import net.pms.encoders.TranscodingSettings;
import net.pms.external.audioaddict.AudioAddictPlaylistDto;
import net.pms.external.audioaddict.Platform;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;

/**
 * A curated playlist represented as a single, continuously playable item. Playing it streams
 * the playlist tracks back to back via AudioAddictPlaylistInputStream. AudioAddict doesn't
 * expose the playlist items, so we have to imitate a web player behaviour.
 */
public class AudioAddictPlaylistStream extends AudioAddictBroadcastStream {

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

	public int getPlaylistId() {
		return playlistId;
	}

	@Override
	public InputStream getInputStream() {
		return maybeCapture(new AudioAddictPlaylistInputStream(network, playlistId, loop), "playlist-" + playlistId);
	}

	/**
	 * Provides the same continuous playlist stream but with ICY metadata interleaved, so a
	 * renderer that asked for "Icy-MetaData" can display the current track.
	 */
	@Override
	public InputStream getIcyInputStream(int metaInt) {
		AudioAddictPlaylistInputStream source = new AudioAddictPlaylistInputStream(network, playlistId, loop);
		return new IcyMetadataInputStream(source, metaInt, source::getStreamTitle);
	}

	/**
	 * The stream is already delivered as MP3. If transcoding is needed, we need to glue things together.
	 */
	@Override
	public TranscodingSettings resolveTranscodingSettings() {
		return null;
	}
}
