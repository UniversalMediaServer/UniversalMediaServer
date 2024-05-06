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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpExchangeAsyncContext implements AsyncContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpExchangeAsyncContext.class);

	private final HttpExchangeServletRequest servletRequest;
	private final HttpExchangeServletResponse servletResponse;
	private final List<AsyncListener> listeners = new ArrayList<>();
	private long timeout = 30000;
	private Timer timeoutTimer;
	private Thread thread;

	public HttpExchangeAsyncContext(HttpExchangeServletRequest servletRequest, HttpExchangeServletResponse servletResponse) {
		this.servletRequest = servletRequest;
		this.servletResponse = servletResponse;
	}

	@Override
	public ServletRequest getRequest() {
		return servletRequest;
	}

	@Override
	public ServletResponse getResponse() {
		return servletResponse;
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		return true;
	}

	@Override
	public void dispatch() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void dispatch(String path) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void dispatch(ServletContext context, String path) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void complete() {
		if (servletResponse != null) {
			servletResponse.close();
		}
		unsetTimeoutThread();
		synchronized (listeners) {
			for (AsyncListener listener : listeners) {
				try {
					listener.onComplete(new AsyncEvent(this));
				} catch (IOException e) {
					LOGGER.trace("Exception in AsyncListener#onComplete:", e);
				}
			}
		}
	}

	@Override
	public void start(Runnable run) {
		thread = new Thread(onStartRunnable(run), Thread.currentThread().getName() + "-async");
		thread.start();
	}

	private Runnable onStartRunnable(Runnable run) {
		return () -> {
			onStartAsync();
			setTimeoutThread();
			try {
				run.run();
			} catch (Throwable e) {
				onError(e);
			}
			unsetTimeoutThread();
		};
	}

	private void onStartAsync() {
		synchronized (listeners) {
			for (AsyncListener listener : listeners) {
				try {
					listener.onStartAsync(new AsyncEvent(this));
				} catch (IOException e) {
					LOGGER.trace("Exception in AsyncListener#onStartAsync:", e);
				}
			}
		}
	}

	private void onError(Throwable throwable) {
		synchronized (listeners) {
			for (AsyncListener listener : listeners) {
				try {
					listener.onError(new AsyncEvent(this, throwable));
				} catch (IOException e) {
					LOGGER.trace("Exception in AsyncListener#onError:", e);
				}
			}
		}
	}

	private void setTimeoutThread() {
		if (timeout < 1) {
			return;
		}
		timeoutTimer = new Timer();
		timeoutTimer.schedule(
				new TimerTask() {
			@Override
			public void run() {
				onTimeout();
			}
		},
				timeout);
	}

	private void unsetTimeoutThread() {
		if (timeoutTimer != null) {
			timeoutTimer.cancel();
			timeoutTimer = null;
		}
	}

	private void onTimeout() {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
			synchronized (listeners) {
				for (AsyncListener listener : listeners) {
					try {
						listener.onTimeout(new AsyncEvent(this));
					} catch (IOException e) {
						LOGGER.trace("Exception in AsyncListener#onTimeout:", e);
					}
				}
			}
		}
	}

	@Override
	public void addListener(AsyncListener listener) {
		if (thread != null) {
			throw new IllegalStateException("Method is called after the container-initiated dispatch, during which one of the {@link ServletRequest#startAsync} methods was called, has returned to the container");
		}
		if (listener != null) {
			synchronized (listeners) {
				listeners.add(listener);
			}
		}
	}

	@Override
	public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
		AsyncListener wrappedListener = new WrappedAsyncListener(listener, servletRequest, servletResponse);
		addListener(wrappedListener);
	}

	@Override
	public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
		ServletContext contextHandler = servletRequest.getServletContext();
		if (contextHandler != null) {
			return contextHandler.createListener(clazz);
		}
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void setTimeout(long timeout) {
		if (thread != null) {
			throw new IllegalStateException("Method is called after the container-initiated dispatch, during which one of the {@link ServletRequest#startAsync} methods was called, has returned to the container");
		}
		this.timeout = timeout;
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

}
