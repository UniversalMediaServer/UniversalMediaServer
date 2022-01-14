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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.jupnp.QueueingThreadPoolExecutor;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.RecoveringUDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.RecoveringUDA10ServiceDescriptorBinderSAXImpl;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.model.Namespace;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.ServiceType;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.DatagramIOConfigurationImpl;
import org.jupnp.transport.impl.DatagramIOImpl;
import org.jupnp.transport.impl.DatagramProcessorImpl;
import org.jupnp.transport.impl.GENAEventProcessorImpl;
import org.jupnp.transport.impl.MulticastReceiverConfigurationImpl;
import org.jupnp.transport.impl.MulticastReceiverImpl;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UpnpServiceConfiguration with a StreamClient but not StreamServer. UMS will
 * handle http server, jupnp the datagram and RemoteDevices
 */
public class CustomUpnpServiceConfiguration implements UpnpServiceConfiguration {

	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomUpnpServiceConfiguration.class);
	private static final int THREAD_POOL_SIZE = 50;

	private final ExecutorService defaultExecutorService;

	private final DatagramProcessor datagramProcessor;
	private final SOAPActionProcessor soapActionProcessor;
	private final GENAEventProcessor genaEventProcessor;

	private final DeviceDescriptorBinder deviceDescriptorBinderUDA10;
	private final ServiceDescriptorBinder serviceDescriptorBinderUDA10;

	private final Namespace namespace;

	private final TransportConfiguration transportConfiguration;
	private final UpnpHeaders umsHeaders = new UpnpHeaders();
	private final int aliveIntervalMillis;

	protected CustomUpnpServiceConfiguration() {
		umsHeaders.add(UpnpHeader.Type.USER_AGENT.getHttpName(), "UMS/" + PMS.getVersion() + " " + new ServerClientTokens());
		transportConfiguration = new CustomTransportConfiguration();

		defaultExecutorService = QueueingThreadPoolExecutor.createInstance("upnp-remote", THREAD_POOL_SIZE);

		datagramProcessor = new DatagramProcessorImpl();
		soapActionProcessor = new SOAPActionProcessorImpl();
		genaEventProcessor = new GENAEventProcessorImpl();

		deviceDescriptorBinderUDA10 = new RecoveringUDA10DeviceDescriptorBinderImpl();
		serviceDescriptorBinderUDA10 = new RecoveringUDA10ServiceDescriptorBinderSAXImpl();

		namespace = new Namespace();
		aliveIntervalMillis = CONFIGURATION.getAliveDelay() != 0 ? CONFIGURATION.getAliveDelay() : 30000;
	}

	protected ExecutorService getDefaultExecutorService() {
		return defaultExecutorService;
	}

	@Override
	public NetworkAddressFactory createNetworkAddressFactory() {
		return new CustomNetworkAddressFactoryImpl();
	}

	@Override
	public DatagramProcessor getDatagramProcessor() {
		return datagramProcessor;
	}

	@Override
	public SOAPActionProcessor getSoapActionProcessor() {
		return soapActionProcessor;
	}

	@Override
	public GENAEventProcessor getGenaEventProcessor() {
		return genaEventProcessor;
	}

	@Override
	public StreamClient createStreamClient() {
		return transportConfiguration.createStreamClient(getSyncProtocolExecutorService(), null);
	}

	@Override
	public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
		return new MulticastReceiverImpl(
				new MulticastReceiverConfigurationImpl(
						networkAddressFactory.getMulticastGroup(),
						networkAddressFactory.getMulticastPort()
				)
		);
	}

	@Override
	public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
		return new DatagramIOImpl(new DatagramIOConfigurationImpl());
	}

	@Override
	public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
		return transportConfiguration.createStreamServer(networkAddressFactory.getStreamListenPort());
	}

	@Override
	public Executor getMulticastReceiverExecutor() {
		return getDefaultExecutorService();
	}

	@Override
	public Executor getDatagramIOExecutor() {
		return getDefaultExecutorService();
	}

	@Override
	public ExecutorService getStreamServerExecutorService() {
		return getDefaultExecutorService();
	}

	@Override
	public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
		return deviceDescriptorBinderUDA10;
	}

	@Override
	public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
		return serviceDescriptorBinderUDA10;
	}

	@Override
	public ServiceType[] getExclusiveServiceTypes() {
		return new ServiceType[0];
	}

	@Override
	public int getRegistryMaintenanceIntervalMillis() {
		return aliveIntervalMillis;
	}

	@Override
	public int getAliveIntervalMillis() {
		return aliveIntervalMillis;
	}

	@Override
	public boolean isReceivedSubscriptionTimeoutIgnored() {
		return false;
	}

	@Override
	public Integer getRemoteDeviceMaxAgeSeconds() {
		return null;
	}

	@Override
	public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity rdi) {
		return umsHeaders;
	}

	@Override
	public UpnpHeaders getEventSubscriptionHeaders(RemoteService rs) {
		return null;
	}

	@Override
	public Executor getAsyncProtocolExecutor() {
		return getDefaultExecutorService();
	}

	@Override
	public ExecutorService getSyncProtocolExecutorService() {
		return getDefaultExecutorService();
	}

	@Override
	public Namespace getNamespace() {
		return namespace;
	}

	@Override
	public Executor getRegistryMaintainerExecutor() {
		return getDefaultExecutorService();
	}

	@Override
	public Executor getRegistryListenerExecutor() {
		return getDefaultExecutorService();
	}

	@Override
	public Executor getRemoteListenerExecutor() {
		return getDefaultExecutorService();
	}

	@Override
	public void shutdown() {
		LOGGER.trace("Shutting down default executor service");
		getDefaultExecutorService().shutdownNow();
	}

}
