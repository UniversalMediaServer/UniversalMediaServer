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
package net.pms.network;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class UmsAsyncListener implements AsyncListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsAsyncListener.class);

	private final long startTime;
	private final int counter;

	private long bytesSent = 0;

	public UmsAsyncListener(long startTime, int counter) {
		this.startTime = startTime;
		this.counter = counter;
	}

	@Override
	public void onTimeout(AsyncEvent asyncEvent) throws IOException {
		log(asyncEvent, "timed out", true);
	}

	@Override
	public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
		// useless
		log(asyncEvent, "start", false);
	}

	@Override
	public void onError(AsyncEvent asyncEvent) throws IOException {
		log(asyncEvent, "Error", true);
	}

	@Override
	public void onComplete(AsyncEvent asyncEvent) throws IOException {
		log(asyncEvent, "Complete", true);
	}

	public void log(AsyncEvent asyncEvent, String state, boolean extended) {
		if (extended) {
			long duration = System.currentTimeMillis() - startTime;
			String data = "";
			if (counter > 0) {
				data += String.format(", request: %3d", counter);
			}
			if (bytesSent > 0) {
				data += String.format(", sent: %d bytes", bytesSent);
			}
			LOGGER.trace("Stream Async {}: duration: {}, request: {}{}", state, duration, asyncEvent.getSuppliedRequest(), data);
		} else {
			LOGGER.trace("Stream Async {}: request: {}", state, asyncEvent.getSuppliedRequest());
		}
	}

	public void setBytesSent(long bytesSent) {
		this.bytesSent = bytesSent;
	}

}
