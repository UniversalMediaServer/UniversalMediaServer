// a utility class, instances of which trigger start/stop callbacks before/after streaming a resource
package net.pms.external;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;

public class StartStopListenerDelegate {
	private final String rendererId;
	private DLNAResource dlna;
	private boolean started = false;
	private boolean stopped = false;
	private RendererConfiguration renderer;

	public StartStopListenerDelegate(String rendererId) {
		this.rendererId = rendererId;
		renderer = null;
	}

	public void setRenderer(RendererConfiguration r) {
		renderer = r;
	}

	public RendererConfiguration getRenderer() {
		return renderer;
	}

	// technically, these don't need to be synchronized as there should be
	// one thread per request/response, but it doesn't hurt to enforce the contract
	public synchronized void start(DLNAResource dlna) {
		assert this.dlna == null;
		this.dlna = dlna;
		Format ext = dlna.getFormat();
		// only trigger the start/stop events for audio and video
		if (!started && ext != null && (ext.isVideo() || ext.isAudio())) {
			dlna.startPlaying(rendererId, renderer);
			started = true;
			PMS.get().getSleepManager().startPlaying();
		} else {
			PMS.get().getSleepManager().postponeSleep();
		}
	}

	public synchronized void stop() {
		if (started && !stopped) {
			dlna.stopPlaying(rendererId, renderer);
			stopped = true;
			PMS.get().getSleepManager().stopPlaying();
		}
	}
}
