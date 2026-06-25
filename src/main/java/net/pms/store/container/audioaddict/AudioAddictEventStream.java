package net.pms.store.container.audioaddict;

import net.pms.renderers.Renderer;

/**
 * A DI.fm/AudioAddict event (i.e. a live DJ stream).
 */
public class AudioAddictEventStream extends AudioAddictBroadcastStream {

	public AudioAddictEventStream(Renderer renderer, String fluxName, String url, String thumbURL) {
		super(renderer, fluxName, url, thumbURL);
	}

}
