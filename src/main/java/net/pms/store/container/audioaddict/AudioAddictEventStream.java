package net.pms.store.container.audioaddict;

import java.io.InputStream;
import net.pms.renderers.Renderer;

/**
 * A DI.fm/AudioAddict event (i.e. a live DJ stream).
 */
public class AudioAddictEventStream extends AudioAddictBroadcastStream {

	public AudioAddictEventStream(Renderer renderer, String fluxName, String url, String thumbURL) {
		super(renderer, fluxName, url, thumbURL);
	}

	@Override
	public InputStream getInputStream() {
		return maybeCapture(super.getInputStream(), "event-" + getName());
	}

}
