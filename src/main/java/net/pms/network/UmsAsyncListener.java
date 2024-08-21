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
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class UmsAsyncListener implements AsyncListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsAsyncListener.class);
	private static final int ADVISE_MS_TRIGGER = 1000;

	private final long startTime;
	private final int counter;

	private long bytesSent = 0;
	private long nextBytesAdvise = System.currentTimeMillis() + ADVISE_MS_TRIGGER;

	public UmsAsyncListener(long startTime, int counter) {
		this.startTime = startTime;
		this.counter = counter;
	}

	@Override
	public void onTimeout(AsyncEvent asyncEvent) throws IOException {
		log(asyncEvent, "Timed out", true);
	}

	@Override
	public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
		// useless
		log(asyncEvent, "Start", false);
	}

	@Override
	public void onError(AsyncEvent asyncEvent) throws IOException {
		log(asyncEvent, "Error", true);
	}

	@Override
	public void onComplete(AsyncEvent asyncEvent) throws IOException {
		log(asyncEvent, "Complete", true);
	}

	public void onPrematureEnd(String state) {
		log(null, String.format("Premature end: %s", state), true);
	}

	public void log(AsyncEvent asyncEvent, String state, boolean extended) {
		String data = String.format("Stream Async %s", state);
		if (counter > 0) {
			data += String.format(", id: %d", counter);
		}
		if (asyncEvent != null && asyncEvent.getSuppliedRequest() instanceof HttpServletRequest request) {
			data += String.format(", uri: %s", request.getRequestURI());
			request.getRequestURI();
		}
		if (extended) {
			long duration = System.currentTimeMillis() - startTime;
			data += String.format(", duration: %d ms", duration);
			if (bytesSent > 0) {
				data += String.format(", sent: %d bytes", bytesSent);
			}
		}
		LOGGER.trace(data);
	}

	public void setBytesSent(long bytesSent) {
		this.bytesSent = bytesSent;
		if (LOGGER.isTraceEnabled() && System.currentTimeMillis() > nextBytesAdvise) {
			nextBytesAdvise = System.currentTimeMillis() + ADVISE_MS_TRIGGER;
			LOGGER.trace("Stream Async progress: {} bytes sent", bytesSent);
		}
	}

}
