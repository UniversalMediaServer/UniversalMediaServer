package net.pms.store.container.audioaddict;

import net.pms.renderers.Renderer;

/**
 * A DI.fm/AudioAddict radio channel: a continuous internet-radio stream. ICY in-band metadata and
 * the radio-style serving are inherited from {@link AudioAddictBroadcastStream}. A live title
 * source is not wired yet, so {@code getStreamTitle()} stays at its {@code null} default for now.
 */
public class AudioAddictRadioStream extends AudioAddictBroadcastStream {

	public AudioAddictRadioStream(Renderer renderer, String fluxName, String url, String thumbURL) {
		super(renderer, fluxName, url, thumbURL);
	}

}
