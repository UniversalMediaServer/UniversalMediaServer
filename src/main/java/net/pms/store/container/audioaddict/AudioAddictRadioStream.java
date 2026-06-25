package net.pms.store.container.audioaddict;

import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;

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

	@Override
	protected String getStreamTitle() {
		if (network == null || channelId == null) {
			return null;
		}
		return AudioAddictService.get().getCurrentTrackTitle(network, channelId);
	}

}
