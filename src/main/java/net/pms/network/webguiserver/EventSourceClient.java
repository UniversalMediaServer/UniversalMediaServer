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
package net.pms.network.webguiserver;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class EventSourceClient implements IEventSourceClient, Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceClient.class);
	private static final byte[] CRLF = new byte[]{'\r', '\n'};
	private static final byte[] EVENT_FIELD = "event: ".getBytes(StandardCharsets.UTF_8);
	private static final byte[] DATA_FIELD = "data: ".getBytes(StandardCharsets.UTF_8);
	private static final byte[] COMMENT_FIELD = ": ".getBytes(StandardCharsets.UTF_8);

	public static final int DEFAULT_HEART_BEAT_PERIOD = 10;

	private final int heartBeatPeriod;
	private final ScheduledExecutorService scheduler;
	private final AsyncContext async;
	private final ServletOutputStream output;

	private Future<?> heartBeat;
	private boolean closed;

	public EventSourceClient(AsyncContext async) throws IOException {
		this(async, DEFAULT_HEART_BEAT_PERIOD, null);
	}

	public EventSourceClient(AsyncContext async, int heartBeatPeriod) throws IOException {
		this(async, heartBeatPeriod, null);
	}

	public EventSourceClient(AsyncContext async, Runnable callback) throws IOException {
		this(async, DEFAULT_HEART_BEAT_PERIOD, callback);
	}

	public EventSourceClient(AsyncContext async, int heartBeatPeriod, Runnable callback) throws IOException {
		this.heartBeatPeriod = heartBeatPeriod;
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.async = async;
		this.output = async.getResponse().getOutputStream();
		if (callback != null) {
			async.addListener(new AsyncListener() {
				@Override
				public void onComplete(AsyncEvent event) throws IOException {
					new Thread(callback).start();
				}
				@Override
				public void onTimeout(AsyncEvent event) throws IOException {
					new Thread(callback).start();
				}
				@Override
				public void onError(AsyncEvent event) throws IOException {
					new Thread(callback).start();
				}
				@Override
				public void onStartAsync(AsyncEvent event) throws IOException {
					//nothing to do
				}
			});
		}
		scheduleHeartBeat();
	}

	private void event(String name, String data) throws IOException {
		synchronized (this) {
			output.write(EVENT_FIELD);
			output.write(name.getBytes(StandardCharsets.UTF_8));
			output.write(CRLF);
			data(data);
		}
	}

	private void data(String data) throws IOException {
		synchronized (this) {
			BufferedReader reader = new BufferedReader(new StringReader(data));
			String line;
			while ((line = reader.readLine()) != null) {
				output.write(DATA_FIELD);
				output.write(line.getBytes(StandardCharsets.UTF_8));
				output.write(CRLF);
			}
			output.write(CRLF);
			flush();
		}
	}

	private void comment(String comment) throws IOException {
		synchronized (this) {
			output.write(COMMENT_FIELD);
			output.write(comment.getBytes(StandardCharsets.UTF_8));
			output.write(CRLF);
			output.write(CRLF);
			flush();
		}
	}

	@Override
	public boolean sendMessage(String message) {
		return sendMessage(message, true);
	}

	public boolean sendMessage(String message, boolean log) {
		if (log) {
			LOGGER.trace("ServerSentEvents send message: {}", message);
		}
		try {
			event("message", message);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean sendComment(String comment, boolean log) {
		if (log) {
			LOGGER.trace("ServerSentEvents send comment: {}", comment);
		}
		try {
			comment(comment);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	protected void flush() throws IOException {
		async.getResponse().flushBuffer();
	}

	@Override
	public void close() {
		synchronized (this) {
			closed = true;
			if (heartBeat != null) {
				heartBeat.cancel(false);
			}
		}
		if (scheduler != null) {
			scheduler.shutdown();
		}
		async.complete();
	}

	private void scheduleHeartBeat() {
		synchronized (this) {
			if (!closed) {
				heartBeat = scheduler.schedule(this, heartBeatPeriod, TimeUnit.SECONDS);
			}
		}
	}

	@Override
	public boolean isOpened() {
		return !closed;
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void run() {
		try {
			event("ping", String.valueOf(System.currentTimeMillis()));
			scheduleHeartBeat();
		} catch (IOException e) {
			// The other peer closed the connection
			close();
		}
	}

}
