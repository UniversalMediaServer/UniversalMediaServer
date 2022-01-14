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
package net.pms.network.mediaserver.jupnp;

import java.net.InetAddress;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.StreamServerConfiguration;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;

public class EmptyStreamServerImpl implements StreamServer<StreamServerConfiguration> {

	final protected StreamServerConfiguration configuration;

	public EmptyStreamServerImpl(StreamServerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	synchronized public void init(InetAddress bindAddress, Router router) throws InitializationException {
	}

	@Override
	synchronized public int getPort() {
		return configuration.getListenPort();
	}

	@Override
	public StreamServerConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	synchronized public void run() {
	}

	@Override
	synchronized public void stop() {
	}
}
