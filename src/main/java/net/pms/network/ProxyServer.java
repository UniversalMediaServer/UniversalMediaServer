/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
package net.pms.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyServer.class);
	private int port;

	public ProxyServer(int port) {
		this.port = port;
		start();
	}

	@Override
	public void run() {
		ServerSocket s;
		try {
			s = new ServerSocket(port);
			try {
				while (true) {
					Socket socket = s.accept();
					try {
						new Proxy(socket, false);
					} catch (IOException e) {
						//System.err.println("E1 " + Thread.currentThread().getName() + ": " + e.getMessage());
						socket.close();
					}
				}
			} finally {
				s.close();
			}
		} catch (IOException e1) {
			LOGGER.debug("Caught exception", e1);
		}
	}
}
