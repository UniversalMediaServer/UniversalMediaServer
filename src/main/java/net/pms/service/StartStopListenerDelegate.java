/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.service;

import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.renderers.Renderer;

// a utility class, instances of which trigger start/stop callbacks before/after streaming a resource
public class StartStopListenerDelegate {
	private final String rendererId;
	private DLNAResource dlna;
	private boolean started = false;
	private boolean stopped = false;
	private Renderer renderer;

	public StartStopListenerDelegate(String rendererId) {
		this.rendererId = rendererId;
		renderer = null;
	}

	public void setRenderer(Renderer r) {
		renderer = r;
	}

	public Renderer getRenderer() {
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
			Services.sleepManager().startPlaying();
		} else {
			Services.sleepManager().postponeSleep();
		}
	}

	public synchronized void stop() {
		if (started && !stopped) {
			dlna.stopPlaying(rendererId, renderer);
			stopped = true;
			Services.sleepManager().stopPlaying();
		}
	}
}
