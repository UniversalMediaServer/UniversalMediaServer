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

/**
 * Implementation based on org.jupnp.transport.impl.apache
 */
public class ApacheStreamClientConfiguration extends AbstractStreamClientConfiguration {

	protected int maxTotalConnections = 1024;
	protected int maxTotalPerRoute = 100;
	protected String contentCharset = "UTF-8"; // UDA spec says it's always UTF-8 entity content

	public ApacheStreamClientConfiguration(ExecutorService timeoutExecutorService) {
		super(timeoutExecutorService);
	}

	public ApacheStreamClientConfiguration(ExecutorService timeoutExecutorService, int timeoutSeconds) {
		super(timeoutExecutorService, timeoutSeconds);
	}

	/**
	 * Defaults to 1024.
	 *
	 * @return
	 */
	public int getMaxTotalConnections() {
		return maxTotalConnections;
	}

	public void setMaxTotalConnections(int maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}

	/**
	 * Defaults to 100.
	 *
	 * @return
	 */
	public int getMaxTotalPerRoute() {
		return maxTotalPerRoute;
	}

	public void setMaxTotalPerRoute(int maxTotalPerRoute) {
		this.maxTotalPerRoute = maxTotalPerRoute;
	}

	/**
	 * @return Character set of textual content, defaults to "UTF-8".
	 */
	public String getContentCharset() {
		return contentCharset;
	}

	public void setContentCharset(String contentCharset) {
		this.contentCharset = contentCharset;
	}

	/**
	 * @return By default <code>-1</code>, enabling HttpClient's default (8192
	 * bytes in version 4.1)
	 */
	public int getSocketBufferSize() {
		return -1;
	}

	/**
	 * @return By default <code>0</code>, use <code>-1</code> to enable
	 * HttpClient's default (3 retries in version 4.1)
	 */
	public int getRequestRetryCount() {
		return 0;
	}

}
