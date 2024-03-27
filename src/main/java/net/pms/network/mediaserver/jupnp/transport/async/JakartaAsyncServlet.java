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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import net.pms.network.HttpServletHelper;
import net.pms.network.UmsAsyncListener;
import net.pms.network.mediaserver.jupnp.transport.impl.JakartaServletStreamServerConfigurationImpl;
import org.jupnp.transport.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http servlet implementation that uses the {@link Router}'s executor to
 * process the current request and releases the current thread(asynchronous).
 *
 * @author Ivan Iliev
 * @author Surf@ceS - Adapt to JakartaEE 9+
 */
public class JakartaAsyncServlet extends HttpServletHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(JakartaAsyncServlet.class);
	private final Router router;

	private int mCounter = 0;

	private final JakartaServletStreamServerConfigurationImpl configuration;

	public JakartaAsyncServlet(Router router, JakartaServletStreamServerConfigurationImpl configuration) {
		this.router = router;
		this.configuration = configuration;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (deny(req)) {
			return;
		}
		final long startTime = System.currentTimeMillis();
		final int counter = mCounter++;
		LOGGER.trace("HttpServlet {}", String.format("HttpServlet.service(): id: %3d, request URI: %s", counter, req.getRequestURI()));
		LOGGER.trace("Handling Servlet request asynchronously: {}", req);

		AsyncContext async = req.startAsync();
		async.setTimeout(configuration.getAsyncTimeoutSeconds() * 1000);
		async.addListener(new UmsAsyncListener(startTime, counter));
		JakartaAsyncServletUpnpStream stream = new JakartaAsyncServletUpnpStream(router.getProtocolFactory(), async, req);

		router.received(stream);
	}
}
