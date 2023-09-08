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

import java.util.concurrent.Callable;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.transport.spi.AbstractStreamClient;
import org.jupnp.transport.spi.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdkStreamClients extends AbstractStreamClient<JdkStreamClientConfiguration, JdkStreamClient> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdkStreamClients.class);
	private final JdkStreamClientConfiguration configuration;

	public JdkStreamClients(JdkStreamClientConfiguration configuration) throws InitializationException {
		this.configuration = configuration;
	}

	@Override
	public JdkStreamClientConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	protected JdkStreamClient createRequest(StreamRequestMessage requestMessage) {
		return new JdkStreamClient(requestMessage, configuration);
	}

	@Override
	protected Callable<StreamResponseMessage> createCallable(StreamRequestMessage requestMessage, JdkStreamClient client) {
		return () -> {
			LOGGER.trace("Sending HTTP request: " + requestMessage);
			if (LOGGER.isTraceEnabled()) {
				StreamsLoggerHelper.logStreamClientRequestMessage(requestMessage);
			}
			return client.getResponse(requestMessage);
		};
	}

	@Override
	protected void abort(JdkStreamClient client) {
		client.cancel();
	}

	@Override
	protected boolean logExecutionException(Throwable t) {
		return false;
	}

	@Override
	public void stop() {
		LOGGER.trace("Shutting down HTTP client connection manager/pool");
		configuration.getRequestExecutorService().shutdown();
	}

}
