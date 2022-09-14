/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.webguiserver;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import javax.servlet.AsyncContext;
import net.pms.network.IServerSentEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSentEvents implements IServerSentEvents {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerSentEvents.class);

	private final Object osLock = new Object();
	private final AsyncContext context;
	private final Runnable callback;
	private OutputStream os;
	private Thread pingThread;

	public ServerSentEvents(AsyncContext context) {
		this(context, null);
	}

	public ServerSentEvents(AsyncContext context, Runnable callback) {
		this.context = context;
		this.callback = callback;
		try {
			addEventStream(this.context.getResponse().getOutputStream());
		} catch (IOException ex) {
			close();
		}
	}

	@Override
	public boolean isOpened() {
		return this.os != null;
	}

	@Override
	public boolean sendMessage(String message) {
		return sendMessage(message, true);
	}

	public boolean sendMessage(String message, boolean log) {
		String response = "event: message\n";
		response += "data: " + message + "\n\n";
		if (log) {
			LOGGER.trace("ServerSentEvents send message: {}", message);
		}
		return send(response);
	}

	@Override
	public final void close() {
		synchronized (osLock) {
			if (os != null) {
				LOGGER.debug("ServerSentEvents close OutputStream");
				try {
					os.close();
				} catch (IOException ex) {
				}
				if (context != null) {
					context.complete();
				}
				os = null;
				if (callback != null) {
					callback.run();
				}
			}
		}
	}

	private void addEventStream(OutputStream os) {
		synchronized (osLock) {
			this.os = os;
		}
		LOGGER.debug("ServerSentEvents OutputStream was set");
		startPing();
	}

	private boolean send(String response) {
		return send(response.getBytes());
	}

	private boolean send(byte[] response) {
		try {
			if (isOpened()) {
				synchronized (osLock) {
					os.write(response);
					os.flush();
				}
				return true;
			}
		} catch (IOException ex) {
			LOGGER.debug("ServerSentEvents OutputStream seems closed");
			stopPing();
			close();
		}
		return false;
	}

	private void startPing() {
		if (pingThread != null && pingThread.isAlive()) {
			LOGGER.trace("ServerSentEvents pingThread already running");
			return;
		}
		Runnable rPing = () -> {
			while (isOpened()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					close();
					return;
				}
				sendPing();
			}
		};
		pingThread = new Thread(rPing, "ServerSentEvents ping");
		pingThread.start();
	}

	private void stopPing() {
		if (pingThread != null) {
			pingThread.interrupt();
		}
	}

	private boolean sendPing() {
		String response = "event: ping\n";
		response += "data: " + new Timestamp(System.currentTimeMillis()) + "\n\n";
		return send(response);
	}

}
