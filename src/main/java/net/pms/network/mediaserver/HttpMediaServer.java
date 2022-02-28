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
package net.pms.network.mediaserver;

import java.io.IOException;
import java.net.InetAddress;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;

public abstract class HttpMediaServer {
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	protected final int port;

	protected String hostname;
	protected InetAddress serverInetAddress;
	protected int localPort = 0;
	protected boolean isSecure = false;

	public HttpMediaServer(InetAddress inetAddress, int port) {
		this.serverInetAddress = inetAddress;
		this.port = port;
		hostname = serverInetAddress.getHostAddress();
	}

	public int getPort() {
		return localPort != 0 ? localPort : port;
	}

	public boolean isHTTPS() {
		return isSecure;
	}

	public abstract boolean start() throws IOException;

	// avoid a NPE when a) switching HTTP Engine versions and b) restarting the HTTP server
	// by cleaning up based on what's in use (not null) rather than the config state, which
	// might be inconsistent.
	//
	// NOTE: there's little in the way of cleanup to do here as PMS.reset() discards the old
	// server and creates a new one
	public abstract void stop();

}
