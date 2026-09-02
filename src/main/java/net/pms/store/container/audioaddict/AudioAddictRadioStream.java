package net.pms.store.container.audioaddict;

import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.NowPlayingInfo;

/**
 * A DI.fm/AudioAddict radio channel: a continuous internet-radio stream. The live title is
 * taken from the global "currently_playing" lookup for this channel.
 */
public class AudioAddictRadioStream extends AudioAddictBroadcastStream {

	private final Platform network;
	private final Integer channelId;

	public AudioAddictRadioStream(Renderer renderer, String fluxName, String url, String thumbURL, Platform network, Integer channelId) {
		super(renderer, fluxName, url, thumbURL);
		this.network = network;
		this.channelId = channelId;
	}

	/**
	 * The channel's live track is a global fact of that channel, so it is looked up rather than read
	 * out of the stream.
	 */
	@Override
	protected NowPlayingInfo getNowPlaying() {
		if (network == null || channelId == null) {
			return null;
		}
		AudioAddictTrackDto track = AudioAddictService.get().getCurrentTrack(network, channelId);
		return track == null ? null : NowPlayingInfo.of(track.artist, track.title, track.albumArt);
	}

	/**
	 * @return the AudioAddict numeric channel id (may be {@code null})
	 */
	public Integer getChannelId() {
		return channelId;
	}

	/**
	 * @return the AudioAddict network short name (e.g. "di"), or "null" when unknown.
	 */
	public String getNetworkShortName() {
		return network != null ? network.shortName : null;
	}

}
