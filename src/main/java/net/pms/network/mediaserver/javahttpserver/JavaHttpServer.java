/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.network.mediaserver.javahttpserver;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.network.mediaserver.HttpMediaServer;

public class JavaHttpServer extends HttpMediaServer {
	private HttpServer server;
	private ExecutorService executorService;

	public JavaHttpServer(int port) {
		super(port);
	}

	@Override
	public boolean start() throws IOException {
		InetSocketAddress address = getSocketAddress();
		server = HttpServer.create(address, 0);
		if (server != null) {
			server.createContext("/", new RequestHandler());
			server.createContext("/api", new ApiHandler());
			server.createContext("/console", new ConsoleHandler());
			executorService = Executors.newCachedThreadPool(new HttpServerThreadFactory());
			server.setExecutor(executorService);
			server.start();
		}
		if (hostname == null && iafinal != null) {
			hostname = iafinal.getHostAddress();
		} else if (hostname == null) {
			hostname = InetAddress.getLocalHost().getHostAddress();
		}
		return true;
	}

	/**
	 * A {@link ThreadFactory} that creates HttpServer requests threads.
	 */
	static class HttpServerThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);

		HttpServerThreadFactory() {
			group = new ThreadGroup("HttpServer requests group");
			group.setDaemon(false);
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(group, runnable, "HTTPv3 Request Handler " + threadNumber.getAndIncrement());
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}
}
