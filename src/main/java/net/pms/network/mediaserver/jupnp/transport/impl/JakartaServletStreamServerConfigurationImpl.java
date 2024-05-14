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
package net.pms.network.mediaserver.jupnp.transport.impl;

import net.pms.network.mediaserver.jupnp.transport.spi.JakartaServletContainerAdapter;
import org.jupnp.transport.spi.StreamServerConfiguration;

/**
 * Settings for the async Servlet JakartaEE 9+ implementation.
 * <p>
 * If you are trying to integrate jUPnP with an existing/running servlet
 * container, implement {@link org.jupnp.transport.spi.ServletContainerAdapter}.
 * </p>
 *
 * @author Christian Bauer
 * @author Surf@ces - Adapt to JakartaEE 9+
 */
public class JakartaServletStreamServerConfigurationImpl implements StreamServerConfiguration {

	protected JakartaServletContainerAdapter servletContainerAdapter;
	protected int listenPort = 0;
	protected int asyncTimeoutSeconds = 60;

	/**
	 * Defaults to port '0', ephemeral.
	 */
	public JakartaServletStreamServerConfigurationImpl(JakartaServletContainerAdapter servletContainerAdapter) {
		this.servletContainerAdapter = servletContainerAdapter;
	}

	public JakartaServletStreamServerConfigurationImpl(JakartaServletContainerAdapter servletContainerAdapter, int listenPort) {
		this.servletContainerAdapter = servletContainerAdapter;
		this.listenPort = listenPort;
	}

	public JakartaServletStreamServerConfigurationImpl(JakartaServletContainerAdapter servletContainerAdapter, int listenPort,
			int asyncTimeoutSeconds) {
		this.servletContainerAdapter = servletContainerAdapter;
		this.listenPort = listenPort;
		this.asyncTimeoutSeconds = asyncTimeoutSeconds;
	}

	/**
	 * @return Defaults to <code>0</code>.
	 */
	@Override
	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	/**
	 * The time in seconds this server wait for the
	 * {@link org.jupnp.transport.Router} to execute a
	 * {@link org.jupnp.transport.spi.UpnpStream}.
	 *
	 * @return The default of 60 seconds.
	 */
	public int getAsyncTimeoutSeconds() {
		return asyncTimeoutSeconds;
	}

	public void setAsyncTimeoutSeconds(int asyncTimeoutSeconds) {
		this.asyncTimeoutSeconds = asyncTimeoutSeconds;
	}

	public JakartaServletContainerAdapter getServletContainerAdapter() {
		return servletContainerAdapter;
	}

	public void setServletContainerAdapter(JakartaServletContainerAdapter servletContainerAdapter) {
		this.servletContainerAdapter = servletContainerAdapter;
	}
}
