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
package net.pms.network.mediaserver.jupnp.transport.async;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import net.pms.network.mediaserver.jupnp.transport.impl.JakartaServletConnection;
import net.pms.network.mediaserver.jupnp.transport.impl.JakartaServletUpnpStream;
import org.jupnp.model.message.Connection;
import org.jupnp.protocol.ProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on JakartaEE Servlet 9+ API.
 * <p>
 * Concrete implementations must provide a connection wrapper, as this wrapper
 * most likely has to access proprietary APIs to implement connection checking.
 * </p>
 *
 * @author Christian Bauer
 * @author Surf@ceS - Adapt to JakartaEE 9+
 */
public class JakartaAsyncServletUpnpStream extends JakartaServletUpnpStream implements AsyncListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(JakartaAsyncServletUpnpStream.class);

	protected final AsyncContext asyncContext;
	protected final HttpServletRequest request;

	protected JakartaAsyncServletUpnpStream(ProtocolFactory protocolFactory, AsyncContext asyncContext, HttpServletRequest request) {
		super(protocolFactory);
		this.asyncContext = asyncContext;
		this.request = request;
		asyncContext.addListener(this);
	}

	@Override
	protected Connection createConnection() {
		return new JakartaServletConnection(getRequest());
	}

	@Override
	protected HttpServletRequest getRequest() {
		return request;
	}

	@Override
	protected HttpServletResponse getResponse() {
		ServletResponse response = asyncContext.getResponse();
		if (response == null) {
			throw new IllegalStateException("Couldn't get response from asynchronous context, already timed out");
		}
		return (HttpServletResponse) response;
	}

	@Override
	protected void complete() {
		try {
			asyncContext.complete();
		} catch (IllegalStateException e) {
			// If Jetty's connection, for whatever reason, is in an illegal state, this will be thrown
			// and we can "probably" ignore it. The request is complete, no matter how it ended.
			LOGGER.trace("Error calling servlet container's AsyncContext#complete() method", e);
		}
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
		// This is a completely useless callback, it will only be called on request.startAsync() which
		// then immediately removes the listener... what were they thinking.
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		LOGGER.trace("Completed asynchronous processing of HTTP request: {}", event.getSuppliedRequest());
		responseSent(responseMessage);
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		LOGGER.trace("Asynchronous processing of HTTP request timed out: {}", event.getSuppliedRequest());
		responseException(new Exception("Asynchronous request timed out"));
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		LOGGER.trace("Asynchronous processing of HTTP request error", event.getThrowable());
		responseException(event.getThrowable());
	}

	@Override
	public String toString() {
		return "" + hashCode();
	}
}
