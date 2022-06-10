package net.pms.network.webinterfaceserver;

import java.util.ArrayList;
import java.util.Iterator;
import net.pms.iam.Account;

public class WebInterfaceAccount extends Account {
	private final ArrayList<ServerSentEvents> sses;

	private boolean needClean;

	public WebInterfaceAccount(Account account) {
		this.setUser(account.getUser());
		this.setGroup(account.getGroup());
		this.setPermissions(account.getPermissions());
		sses = new ArrayList<>();
	}

	/**
	 * Link a Server Sent Events Stream to this account.
	 * @param see the Server Sent Events Stream
	 */
	public void addServerSentEvents(ServerSentEvents see) {
		sses.add(see);
	}

	/**
	 * Broadcast a message to all Server Sent Events Streams linked to this account.
	 * @param message
	 */
	public void broadcastMessage(String message) {
		sses.forEach(sse -> {
			sse.sendMessage(message);
			if (!sse.isOpened()) {
				needClean = true;
			}
		});
		if (needClean) {
			cleanServerSendEvents();
		}
	}

	/**
	 * Clean all Server Sent Events Streams closed.
	 */
	public void cleanServerSendEvents() {
		needClean = false;
		Iterator i = sses.iterator();
		while (i.hasNext()) {
			ServerSentEvents see = (ServerSentEvents) i.next();
			if (!see.isOpened()) {
				i.remove();
			}
		}
	}

}
