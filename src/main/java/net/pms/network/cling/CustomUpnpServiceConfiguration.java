/*
 * Universal Media Server, for streaming any medias to DLNA
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
package net.pms.network.cling;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.transport.impl.MulticastReceiverConfigurationImpl;
import org.fourthline.cling.transport.spi.MulticastReceiver;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;

public class CustomUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

    /**
     * Defaults to port '0', ephemeral.
     */
    public CustomUpnpServiceConfiguration() {
        super(CustomNetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT);
    }

    public CustomUpnpServiceConfiguration(int streamListenPort) {
        super(streamListenPort, true);
    }

    @Override
    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
        return new CustomNetworkAddressFactoryImpl(streamListenPort);
    }

    @Override
    public MulticastReceiver<?> createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new CustomMulticastReceiverImpl(
                new MulticastReceiverConfigurationImpl(
                        networkAddressFactory.getMulticastGroup(),
                        networkAddressFactory.getMulticastPort()
                )
        );
    }
}
