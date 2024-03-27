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
package net.pms.network.httpserverservletcontainer;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author Surf@ceS
 */
public class WrappedAsyncListener implements AsyncListener {

	private final AsyncListener listener;
	private final ServletRequest servletRequest;
	private final ServletResponse servletResponse;

	public WrappedAsyncListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
		this.listener = listener;
		this.servletRequest = servletRequest;
		this.servletResponse = servletResponse;
	}

	public AsyncListener getListener() {
		return listener;
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		listener.onTimeout(new AsyncEvent(event.getAsyncContext(), servletRequest, servletResponse, event.getThrowable()));
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
		listener.onStartAsync(new AsyncEvent(event.getAsyncContext(), servletRequest, servletResponse, event.getThrowable()));
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		listener.onError(new AsyncEvent(event.getAsyncContext(), servletRequest, servletResponse, event.getThrowable()));
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		listener.onComplete(new AsyncEvent(event.getAsyncContext(), servletRequest, servletResponse, event.getThrowable()));
	}

}
