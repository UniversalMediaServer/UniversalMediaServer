package net.pms.store.container.audioaddict;

import net.pms.encoders.TranscodingSettings;
import net.pms.external.audioaddict.AudioAddictPlaylistDto;
import net.pms.external.audioaddict.Platform;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.LiveStreamRelay.Source;
import org.apache.commons.lang3.StringUtils;

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
		String description = StringUtils.trimToNull(playlist.description);
		String genres = StringUtils.trimToNull(playlist.genres);
		String curator = StringUtils.trimToNull(playlist.curator);
		if (description != null || genres != null || curator != null) {
			MediaAudioMetadata md = new MediaAudioMetadata();
			md.setAlbum(description);
			md.setGenre(genres);
			md.setArtist(curator);
			mi.setAudioMetadata(md);
		}
		setMediaInfo(mi);
	}

	public int getPlaylistId() {
		return playlistId;
	}

	/**
	 * The playlist is put together here, so this stream knows itself what it is playing. It is opened
	 * through the relay of the base class like any other endless stream: one session serves every
	 * listener, which is also what keeps the playlist cursor of the account from being advanced once
	 * per listener.
	 */
	@Override
	protected Source openSource() {
		AudioAddictPlaylistInputStream source = new AudioAddictPlaylistInputStream(network, playlistId, loop);
		return new Source(source, source::getNowPlaying);
	}

	/**
	 * The stream is already delivered as MP3. If transcoding is needed, we need to glue things together.
	 */
	@Override
	public TranscodingSettings resolveTranscodingSettings() {
		return null;
	}
}
