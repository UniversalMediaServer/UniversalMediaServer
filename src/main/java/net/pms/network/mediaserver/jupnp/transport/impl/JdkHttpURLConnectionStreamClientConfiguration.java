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

import java.util.concurrent.ExecutorService;
import org.jupnp.transport.spi.AbstractStreamClientConfiguration;

public class JdkHttpURLConnectionStreamClientConfiguration extends AbstractStreamClientConfiguration {

	private boolean usePersistentConnections = false;

	public JdkHttpURLConnectionStreamClientConfiguration(ExecutorService timeoutExecutorService) {
		super(timeoutExecutorService);
	}

	public JdkHttpURLConnectionStreamClientConfiguration(ExecutorService timeoutExecutorService, int timeoutSeconds) {
		super(timeoutExecutorService, timeoutSeconds);
	}

	public boolean isUsePersistentConnections() {
		return usePersistentConnections;
	}

	public void setUsePersistentConnections(boolean usePersistentConnections) {
		this.usePersistentConnections = usePersistentConnections;
	}

}
