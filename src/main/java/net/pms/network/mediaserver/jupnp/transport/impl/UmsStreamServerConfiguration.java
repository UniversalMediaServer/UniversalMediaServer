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
package net.pms.network.mediaserver.jupnp.transport.impl;

import org.jupnp.transport.spi.StreamServerConfiguration;

public class UmsStreamServerConfiguration implements StreamServerConfiguration {

	private int listenPort;
	private int tcpConnectionBacklog;

	/**
	 * Defaults to port '0', ephemeral.
	 */
	public UmsStreamServerConfiguration() {
		this(0);
	}

	public UmsStreamServerConfiguration(int listenPort) {
		this.listenPort = listenPort;
	}

	@Override
	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	/**
	 * @return Maximum number of queued incoming connections to allow on the
	 * listening socket, default is system default.
	 */
	public int getTcpConnectionBacklog() {
		return tcpConnectionBacklog;
	}

	public void setTcpConnectionBacklog(int tcpConnectionBacklog) {
		this.tcpConnectionBacklog = tcpConnectionBacklog;
	}

}
