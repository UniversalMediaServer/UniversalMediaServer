// a utility class, instances of which trigger start/stop callbacks before/after streaming a resource
package net.pms.external;

import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;

public class StartStopListenerDelegate {
	private final String rendererId;
	private DLNAResource dlna;
	private boolean started = false;
	private boolean stopped = false;

	public StartStopListenerDelegate(String rendererId) {
		this.rendererId = rendererId;
	}

	// technically, these don't need to be synchronized as there should be
	// one thread per request/response, but it doesn't hurt to enforce the contract
	public synchronized void start(DLNAResource dlna) {
		assert this.dlna == null;
		this.dlna = dlna;
		Format ext = dlna.getExt();
		// only trigger the start/stop events for audio and video
		if (!started && ext != null && (ext.isVideo() || ext.isAudio())) {
			dlna.startPlaying(rendererId);
			started = true;
		}
	}

	public synchronized void stop() {
		if (started && !stopped) {
			dlna.stopPlaying(rendererId);
			stopped = true;
		}
	}
}
