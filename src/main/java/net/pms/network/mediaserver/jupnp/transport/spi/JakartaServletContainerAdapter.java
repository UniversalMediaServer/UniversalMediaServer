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
package net.pms.network.mediaserver.jupnp.transport.spi;

import jakarta.servlet.Servlet;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * {@see #ServletContainerAdapter}
 * @author Surf@ceS
 */
public interface JakartaServletContainerAdapter {

	/**
	 * Might be called several times to integrate the servlet container with
	 * jUPnP's executor configuration. You can ignore this call if you want to
	 * configure the container's thread pooling independently from jUPnP. If you
	 * use the given jUPnP <code>ExecutorService</code>, make sure the Jetty
	 * container won't shut it down when {@link #stopIfRunning()} is called!
	 *
	 * @param executorService The service to use when spawning new servlet
	 * execution threads.
	 */
	void setExecutorService(ExecutorService executorService);

	/**
	 * Might be called several times to set up the connectors. This is the
	 * host/address and the port jUPnP expects to receive HTTP requests on. If
	 * you set up your HTTP server connectors elsewhere and ignore when jUPnP
	 * calls this method, make sure you configure jUPnP with the correct
	 * host/port of your servlet container.
	 *
	 * @param host The host address for the socket.
	 * @param port The port, might be <code>-1</code> to bind to an ephemeral
	 * port.
	 * @return The actual registered local port.
	 * @throws IOException If the connector couldn't be opened to retrieve the
	 * registered local port.
	 */
	int addConnector(String host, int port) throws IOException;

	/**
	 * Might be called several times to register (the same) handler for UPnP
	 * requests, should only register it once.
	 *
	 * @param contextPath The context path prefix for all UPnP requests.
	 * @param servlet The servlet handling all UPnP requests.
	 */
	void registerServlet(String contextPath, Servlet servlet);

	/**
	 * Start your servlet container if it isn't already running, might be called
	 * multiple times.
	 */
	void startIfNotRunning();

	/**
	 * Stop your servlet container if it's still running, might be called
	 * multiple times.
	 */
	void stopIfRunning();
}
