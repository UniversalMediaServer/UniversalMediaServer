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

import com.google.common.net.InetAddresses;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import org.jupnp.model.message.Connection;

/**
 * UPNP Connection implementation using a {@link HttpServletRequest}.
 *
 * @author Christian Bauer
 * @author Surf@ceS - adapt to Jakarta
 */
public class JakartaServletConnection implements Connection {

	protected HttpServletRequest request;

	public JakartaServletConnection(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	@Override
	public boolean isOpen() {
		return isConnectionOpen(getRequest());
	}

	@Override
	public InetAddress getRemoteAddress() {
		try {
			return InetAddresses.forString(getRequest().getRemoteAddr());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Failed to parse request remote address", e);
		}
	}

	@Override
	public InetAddress getLocalAddress() {
		try {
			return InetAddresses.forString(getRequest().getLocalAddr());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Failed to parse request local address", e);
		}
	}

	/**
	 * Override this method if you can check, at a low level, if the client
	 * connection is still open for the given request. This will likely require
	 * access to proprietary APIs of your servlet container to obtain the
	 * socket/channel for the given request.
	 *
	 * @return By default <code>true</code>.
	 */
	protected boolean isConnectionOpen(HttpServletRequest request) {
		return true;
	}
}
