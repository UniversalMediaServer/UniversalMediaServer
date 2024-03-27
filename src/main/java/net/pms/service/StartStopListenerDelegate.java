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

import net.pms.formats.Format;
import net.pms.store.StoreItem;

// a utility class, instances of which trigger start/stop callbacks before/after streaming a resource
public class StartStopListenerDelegate {
	private final String rendererId;
	private StoreItem resource;
	private boolean started = false;
	private boolean stopped = false;

	public StartStopListenerDelegate(String rendererId) {
		this.rendererId = rendererId;
	}

	// technically, these don't need to be synchronized as there should be
	// one thread per request/response, but it doesn't hurt to enforce the contract
	public synchronized void start(StoreItem resource) {
		assert this.resource == null;
		this.resource = resource;
		Format ext = resource.getFormat();
		// only trigger the start/stop events for audio and video
		if (!started && ext != null && (ext.isVideo() || ext.isAudio())) {
			resource.startPlaying(rendererId);
			started = true;
			Services.sleepManager().startPlaying();
		} else {
			Services.postponeSleep();
		}
	}

	public synchronized void stop() {
		if (started && !stopped) {
			resource.stopPlaying(rendererId);
			stopped = true;
			Services.sleepManager().stopPlaying();
		}
	}
}
