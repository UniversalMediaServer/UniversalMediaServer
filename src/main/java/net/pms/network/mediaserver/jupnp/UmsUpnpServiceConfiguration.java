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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.jupnp.transport.impl.JdkHttpServerStreamServer;
import net.pms.network.mediaserver.jupnp.transport.impl.JdkHttpURLConnectionStreamClient;
import net.pms.network.mediaserver.jupnp.transport.impl.JdkHttpURLConnectionStreamClientConfiguration;
import net.pms.network.mediaserver.jupnp.transport.impl.NettyStreamServer;
import net.pms.network.mediaserver.jupnp.transport.impl.Ums2DatagramProcessor;
import net.pms.network.mediaserver.jupnp.transport.impl.UmsDatagramIO;
import net.pms.network.mediaserver.jupnp.transport.impl.UmsMulticastReceiver;
import net.pms.network.mediaserver.jupnp.transport.impl.UmsNetworkAddressFactory;
import net.pms.network.mediaserver.jupnp.transport.impl.UmsStreamServerConfiguration;
import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.transport.impl.DatagramIOConfigurationImpl;
import org.jupnp.transport.impl.MulticastReceiverConfigurationImpl;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmsUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final int CORE_THREAD_POOL_SIZE = 16;
	private static final int THREAD_POOL_SIZE = 200;
	private static final int THREAD_QUEUE_SIZE = 1000;
	private static final boolean THREAD_POOL_CORE_TIMEOUT = true;

	private final ExecutorService multicastReceiverExecutorService;
	private final ExecutorService datagramIOExecutorService;
	private final ExecutorService streamServerExecutorService;
	private final ExecutorService syncProtocolExecutorService;
	private final ExecutorService asyncProtocolExecutorService;
	private final ExecutorService remoteListenerExecutorService;
	private final ExecutorService registryListenerExecutorService;
	private final UpnpHeaders umsHeaders;

	private boolean ownHttpServer = false;

	public UmsUpnpServiceConfiguration(boolean ownHttpServer) {
		super();
		this.ownHttpServer = ownHttpServer;
		umsHeaders = new UpnpHeaders();
		umsHeaders.add(UpnpHeader.Type.USER_AGENT.getHttpName(), "UMS/" + PMS.getVersion() + " UPnP/1.0 DLNADOC/1.50 (" + System.getProperty("os.name").replace(" ", "_") + ")");
		multicastReceiverExecutorService = createDefaultExecutorService("multicast-receiver");
		datagramIOExecutorService = createDefaultExecutorService("datagram-io");
		streamServerExecutorService = createDefaultExecutorService("stream-server");
		syncProtocolExecutorService = createDefaultExecutorService("sync-protocol");
		asyncProtocolExecutorService = createDefaultExecutorService("async-protocol");
		remoteListenerExecutorService = createDefaultExecutorService("remote-listener");
		registryListenerExecutorService = createDefaultExecutorService("registry-listener");
	}

	@Override
	public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity identity) {
		return umsHeaders;
	}

	@Override
	public int getAliveIntervalMillis() {
		return CONFIGURATION.getAliveDelay() != 0 ? CONFIGURATION.getAliveDelay() : 30000;
	}

	@Override
	public StreamClient createStreamClient() {
		return new JdkHttpURLConnectionStreamClient(
				new JdkHttpURLConnectionStreamClientConfiguration(
						getSyncProtocolExecutorService()
				)
		);
	}

	public boolean useOwnHttpServer() {
		return ownHttpServer;
	}

	public void setOwnHttpServer(boolean ownHttpServer) {
		this.ownHttpServer = ownHttpServer;
	}

	@Override
	public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
		if (ownHttpServer) {
			int engineVersion = CONFIGURATION.getServerEngine();
			if (engineVersion == 0 || !MediaServer.VERSIONS.containsKey(engineVersion)) {
				engineVersion = MediaServer.DEFAULT_VERSION;
			}
			switch (engineVersion) {
				case 4:
					return new NettyStreamServer(
							new UmsStreamServerConfiguration(
									networkAddressFactory.getStreamListenPort()
							)
					);
				case 5:
					return new JdkHttpServerStreamServer(
							new UmsStreamServerConfiguration(
									networkAddressFactory.getStreamListenPort()
							)
					);
			}
		}
		return new JdkHttpServerStreamServer(
				new UmsStreamServerConfiguration()
		);
	}

	@Override
	public NetworkAddressFactory createNetworkAddressFactory() {
		return new UmsNetworkAddressFactory();
	}

	@Override
	protected DatagramProcessor createDatagramProcessor() {
		return new Ums2DatagramProcessor();
	}

	@Override
	public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
		return new UmsMulticastReceiver(
				new MulticastReceiverConfigurationImpl(
						networkAddressFactory.getMulticastGroup(),
						networkAddressFactory.getMulticastPort()
				)
		);
	}

	@Override
	public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
		return new UmsDatagramIO(new DatagramIOConfigurationImpl());
	}

	//use defaultExecutorService for registryMaintainer
	@Override
	protected ExecutorService createDefaultExecutorService() {
		return new JUPnPExecutor("registry-maintainer");
	}

	private ExecutorService createDefaultExecutorService(String name) {
		return new JUPnPExecutor(name);
	}

	@Override
	public ExecutorService getMulticastReceiverExecutor() {
		return multicastReceiverExecutorService;
	}

	@Override
	public ExecutorService getDatagramIOExecutor() {
		return datagramIOExecutorService;
	}

	@Override
	public ExecutorService getAsyncProtocolExecutor() {
		return asyncProtocolExecutorService;
	}

	@Override
	public ExecutorService getSyncProtocolExecutorService() {
		return syncProtocolExecutorService;
	}

	@Override
	public ExecutorService getStreamServerExecutorService() {
		return streamServerExecutorService;
	}

	@Override
	public Executor getRegistryMaintainerExecutor() {
		return getDefaultExecutorService();
	}

	@Override
	public Executor getRegistryListenerExecutor() {
		return registryListenerExecutorService;
	}

	@Override
	public Executor getRemoteListenerExecutor() {
		return remoteListenerExecutorService;
	}

	@Override
	public void shutdown() {
		LOGGER.trace("Shutting down registry maintainer executor service");
		getDefaultExecutorService().shutdownNow();
		LOGGER.trace("Shutting down multicast receiver executor service");
		multicastReceiverExecutorService.shutdownNow();
		LOGGER.trace("Shutting down registry maintainer executor service");
		datagramIOExecutorService.shutdownNow();
		LOGGER.trace("Shutting down datagram IO executor service");
		streamServerExecutorService.shutdownNow();
		LOGGER.trace("Shutting down sync protocol executor service");
		syncProtocolExecutorService.shutdownNow();
		LOGGER.trace("Shutting down async protocol executor service");
		asyncProtocolExecutorService.shutdownNow();
		LOGGER.trace("Shutting down remote listener executor service");
		remoteListenerExecutorService.shutdownNow();
		LOGGER.trace("Shutting down registry listener executor service");
		registryListenerExecutorService.shutdownNow();
	}

	public static class JUPnPExecutor extends ThreadPoolExecutor {

		public JUPnPExecutor(String name) {
			this(new JUPnPThreadFactory(name),
					new ThreadPoolExecutor.DiscardPolicy() {
				// The pool is bounded and rejections will happen during shutdown
				@Override
				public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
					// Log and discard
					LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class).warn("Thread pool rejected execution of " + runnable.getClass());
					super.rejectedExecution(runnable, threadPoolExecutor);
				}
			}
			);
		}

		public JUPnPExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
			// This is the same as Executors.newCachedThreadPool
			super(CORE_THREAD_POOL_SIZE,
					THREAD_POOL_SIZE,
					10L,
					TimeUnit.SECONDS,
					new ArrayBlockingQueue<Runnable>(THREAD_QUEUE_SIZE),
					threadFactory,
					rejectedHandler
			);
			allowCoreThreadTimeOut();
		}

		private void allowCoreThreadTimeOut() {
			allowCoreThreadTimeOut(THREAD_POOL_CORE_TIMEOUT);
		}

		@Override
		protected void afterExecute(Runnable runnable, Throwable throwable) {
			super.afterExecute(runnable, throwable);
			if (throwable != null) {
				Throwable cause = Exceptions.unwrap(throwable);
				if (cause instanceof InterruptedException) {
					// Ignore this, might happen when we shutdownNow() the executor. We can't
					// log at this point as the logging system might be stopped already (e.g.
					// if it's a CDI component).
					return;
				}
				// Log only
				LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class).warn("Thread terminated " + runnable + " abruptly with exception: " + throwable);
				LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class).warn("Root cause: " + cause);
			}
		}
	}

	// Executors.DefaultThreadFactory is package visibility (...no touching, you unworthy JDK user!)
	public static class JUPnPThreadFactory implements ThreadFactory {

		protected final ThreadGroup group;
		protected final AtomicInteger threadNumber = new AtomicInteger(1);
		protected final String namePrefix;

		public JUPnPThreadFactory(String name) {
			namePrefix = "jupnp-" + name + "-";
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(
					group, r,
					namePrefix + threadNumber.getAndIncrement(),
					0
			);
			if (t.isDaemon()) {
				t.setDaemon(false);
			}
			if (t.getPriority() != Thread.NORM_PRIORITY) {
				t.setPriority(Thread.NORM_PRIORITY);
			}

			return t;
		}
	}

}
