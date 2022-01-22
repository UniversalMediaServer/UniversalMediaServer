package net.pms.network.webinterfaceserver;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSentEvents {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerSentEvents.class);

	private final Object osLock = new Object();
	private final String language;

	private Thread pingThread;
	private OutputStream os;

	public ServerSentEvents(OutputStream os, String language) {
		addEventStream(os);
		this.language = language;
	}

	public final void addEventStream(OutputStream os) {
		//clean current OutputStream in case of....
		stopPing();
		close();
		this.os = os;
		LOGGER.debug("ServerSentEvents OutputStream was set");
		startPing();
	}

	public boolean isOpened() {
		return this.os != null;
	}

	public boolean sendMessage(String message) {
		String response = "event: message\n";
		response += "data: " + message + "\n\n";
		LOGGER.trace("ServerSentEvents send message: {}", message);
		return send(response);
	}

	public void close() {
		if (os != null) {
			LOGGER.debug("ServerSentEvents close OutputStream");
			try {
				os.close();
			} catch (IOException ex1) {
			}
			os = null;
		}
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

	public String getMsgString(String key) {
		return WebInterfaceServerUtil.getMsgString(key, language);
	}
}
