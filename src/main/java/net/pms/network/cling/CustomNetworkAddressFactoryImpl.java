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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Locale;
import java.util.logging.Logger;
import net.pms.PMS;
import org.fourthline.cling.transport.impl.NetworkAddressFactoryImpl;
import org.fourthline.cling.transport.spi.InitializationException;


public class CustomNetworkAddressFactoryImpl extends NetworkAddressFactoryImpl {

    private static Logger log = Logger.getLogger(CustomNetworkAddressFactoryImpl.class.getName());

    /**
     * Defaults to an ephemeral port.
     */
    public CustomNetworkAddressFactoryImpl() throws InitializationException {
        super(DEFAULT_TCP_HTTP_LISTEN_PORT);
    }

    public CustomNetworkAddressFactoryImpl(int streamListenPort) throws InitializationException {
    	super(streamListenPort);
    }

    @Override
    protected boolean isUsableNetworkInterface(NetworkInterface iface) throws Exception {
        if (!iface.isUp()) {
            log.finer("Skipping network interface (down): " + iface.getDisplayName());
            return false;
        }

        if (!PMS.get().getServer().getNetworkInterfaces().contains(iface)) {
        	log.finer("Skipping network interface because it isn't among configured interfaces");
        	return false;
        }

        if (getInetAddresses(iface).size() == 0) {
            log.finer("Skipping network interface without bound IP addresses: " + iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ROOT).startsWith("ppp")) {
            log.finer("Skipping network interface (PPP): " + iface.getDisplayName());
            return false;
        }

        if (iface.isLoopback()) {
            log.finer("Skipping network interface (ignoring loopback): " + iface.getDisplayName());
            return false;
        }

        if (useInterfaces.size() > 0 && !useInterfaces.contains(iface.getName())) {
            log.finer("Skipping unwanted network interface (-D" + SYSTEM_PROPERTY_NET_IFACES + "): " + iface.getName());
            return false;
        }

        if (!iface.supportsMulticast()) {
            log.warning("Network interface isn't multicast capable: "  + iface.getDisplayName());
            return false;
        }

        return true;
    }

    @Override
    protected boolean isUsableAddress(NetworkInterface networkInterface, InetAddress address) {
    	if (!super.isUsableAddress(networkInterface, address)) {
    		return false;
    	}
    	InetAddress httpServerAddress = PMS.get().getServer().getSocketAddress().getAddress();
    	if (!httpServerAddress.isAnyLocalAddress() && !httpServerAddress.equals(address)) {
            log.finer("Skipping IP address because of configuration: " + address);
    		return false;
    	}

        return true;
    }


}
