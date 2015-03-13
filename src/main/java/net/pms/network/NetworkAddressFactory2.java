/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package net.pms.network;

import org.fourthline.cling.model.Constants;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.NoNetworkException;
import org.seamless.util.Iterators;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of network interface and address configuration/discovery.
 * <p
 * This implementation has been tested on Windows XP, Windows Vista, Mac OS X 10.8,
 * and whatever kernel ships in Ubuntu 9.04. This implementation does not support IPv6.
 * </p>
 *
 * @author Christian Bauer
 */
public class NetworkAddressFactory2 implements NetworkAddressFactory {

	// Logger ids to write messages to the logs.
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAddressFactory2.class);
	// Ephemeral port is the default
	public static final int DEFAULT_TCP_HTTP_LISTEN_PORT = 0;

	final protected Set<String> useInterfaces = new HashSet<String>();
	final protected Set<String> useAddresses = new HashSet<String>();

	final protected List<NetworkInterface> networkInterfaces = new ArrayList<NetworkInterface>();
	final protected List<InetAddress> bindAddresses = new ArrayList<InetAddress>();

	protected int streamListenPort;

	public NetworkAddressFactory2() {
	}
		
	/**
	 * Defaults to an ephemeral port.
	 */
	public void init() {
		init(DEFAULT_TCP_HTTP_LISTEN_PORT);
	}

	public void init(int streamListenPort) throws InitializationException {
		System.setProperty("java.net.preferIPv4Stack", "true");

		String useInterfacesString = System.getProperty(SYSTEM_PROPERTY_NET_IFACES);
		if (useInterfacesString != null) {
			String[] userInterfacesStrings = useInterfacesString.split(",");
			useInterfaces.addAll(Arrays.asList(userInterfacesStrings));
		}

		String useAddressesString = System.getProperty(SYSTEM_PROPERTY_NET_ADDRESSES);
		if (useAddressesString != null) {
			String[] useAddressesStrings = useAddressesString.split(",");
			useAddresses.addAll(Arrays.asList(useAddressesStrings));
		}

		discoverNetworkInterfaces();
		discoverBindAddresses();

		if ((networkInterfaces.size() == 0 || bindAddresses.size() == 0)) {
			LOGGER.debug("No usable network interface or addresses found");
			if(requiresNetworkInterface()) {
				throw new NoNetworkException(
					"Could not discover any usable network interfaces and/or addresses"
				);
			}
		}
		logInterfaceInformation();
	}


	/**
	 * @return <code>true</code> (the default) if a <code>MissingNetworkInterfaceException</code> should be thrown
	 */
	protected boolean requiresNetworkInterface() {
		return true;
	}

	public void logInterfaceInformation() {
		synchronized (networkInterfaces) {
			if(networkInterfaces.isEmpty()) {
				LOGGER.info("No network interface to display!");
				return ;
			}
			for(NetworkInterface networkInterface : networkInterfaces) {
				try {
					LOGGER.info("Usable network interfaces found: {}", networkInterfaces.size());
					LOGGER.debug("---------------------------------------------------------------------------------");
					logInterfaceInformation(networkInterface);
				} catch (SocketException ex) {
					LOGGER.warn("Exception while logging network interface information", ex);
				}
			}
		}
	}

	public InetAddress getMulticastGroup() {
		try {
			return InetAddress.getByName(Constants.IPV4_UPNP_MULTICAST_GROUP);
		} catch (UnknownHostException ex) {
			throw new RuntimeException(ex);
		}
	}

	public int getMulticastPort() {
		return Constants.UPNP_MULTICAST_PORT;
	}

	public int getStreamListenPort() {
		return streamListenPort;
	}

	public Iterator<NetworkInterface> getNetworkInterfaces() {
		return new Iterators.Synchronized<NetworkInterface>(networkInterfaces) {
			@Override
			protected void synchronizedRemove(int index) {
				synchronized (networkInterfaces) {
					networkInterfaces.remove(index);
				}
			}
		};
	}

	public Iterator<InetAddress> getBindAddresses() {
		return new Iterators.Synchronized<InetAddress>(bindAddresses) {
			@Override
			protected void synchronizedRemove(int index) {
				synchronized (bindAddresses) {
					bindAddresses.remove(index);
				}
			}
		};
	}

	public boolean hasUsableNetwork() {
		return networkInterfaces.size() > 0 && bindAddresses.size() > 0;
	}

	public byte[] getHardwareAddress(InetAddress inetAddress) {
		try {
			NetworkInterface iface = NetworkInterface.getByInetAddress(inetAddress);
			return iface != null ? iface.getHardwareAddress() : null;
		} catch (Throwable ex) {
			LOGGER.warn("Cannot get hardware address for: " + inetAddress, ex);
			// On Win32: java.lang.Error: IP Helper Library GetIpAddrTable function failed

			// On Android 4.0.3 NullPointerException with inetAddress != null

			// On Android "SocketException: No such device or address" when
			// switching networks (mobile -> WiFi)
			return null;
		}
	}

	public InetAddress getBroadcastAddress(InetAddress inetAddress) {
		synchronized (networkInterfaces) {
			for (NetworkInterface iface : networkInterfaces) {
				for (InterfaceAddress interfaceAddress : getInterfaceAddresses(iface)) {
					if (interfaceAddress != null && interfaceAddress.getAddress().equals(inetAddress)) {
						return interfaceAddress.getBroadcast();
					}
				}
			}
		}
		return null;
	}

	public Short getAddressNetworkPrefixLength(InetAddress inetAddress) {
		synchronized (networkInterfaces) {
			for (NetworkInterface iface : networkInterfaces) {
				for (InterfaceAddress interfaceAddress : getInterfaceAddresses(iface)) {
					if (interfaceAddress != null && interfaceAddress.getAddress().equals(inetAddress)) {
						short prefix = interfaceAddress.getNetworkPrefixLength();
						if(prefix > 0 && prefix < 32) return prefix; // some network cards return -1
						return null;
					}
				}
			}
		}
		return null;
	}

	public InetAddress getLocalAddress(NetworkInterface networkInterface, boolean isIPv6, InetAddress remoteAddress) {

		// First try to find a local IP that is in the same subnet as the remote IP
		InetAddress localIPInSubnet = getBindAddressInSubnetOf(remoteAddress);
		if (localIPInSubnet != null) return localIPInSubnet;

		// There are two reasons why we end up here:
		//
		// - Windows Vista returns a 64 or 128 CIDR prefix if you ask it for the network prefix length of an IPv4 address!
		//
		// - We are dealing with genuine IPv6 addresses
		//
		// - Something is really wrong on the LAN and we received a multicast datagram from a source we can't reach via IP
		LOGGER.trace("Could not find local bind address in same subnet as: " + remoteAddress.getHostAddress());

		// Next, just take the given interface (which is really totally random) and get the first address that we like
		for (InetAddress interfaceAddress: getInetAddresses(networkInterface)) {
			if (isIPv6 && interfaceAddress instanceof Inet6Address)
				return interfaceAddress;
			if (!isIPv6 && interfaceAddress instanceof Inet4Address)
				return interfaceAddress;
		}
		throw new IllegalStateException("Can't find any IPv4 or IPv6 address on interface: " + networkInterface.getDisplayName());
	}

	protected List<InterfaceAddress> getInterfaceAddresses(NetworkInterface networkInterface) {
		return networkInterface.getInterfaceAddresses();
	}

	protected List<InetAddress> getInetAddresses(NetworkInterface networkInterface) {
		return Collections.list(networkInterface.getInetAddresses());
	}

	protected InetAddress getBindAddressInSubnetOf(InetAddress inetAddress) {
		synchronized (networkInterfaces) {
			for (NetworkInterface iface : networkInterfaces) {
				for (InterfaceAddress ifaceAddress : getInterfaceAddresses(iface)) {

					synchronized (bindAddresses) {
						if (ifaceAddress == null || !bindAddresses.contains(ifaceAddress.getAddress())) {
							continue;
						}
					}

					if (isInSubnet(
							inetAddress.getAddress(),
							ifaceAddress.getAddress().getAddress(),
							ifaceAddress.getNetworkPrefixLength())
							) {
						return ifaceAddress.getAddress();
					}
				}

			}
		}
		return null;
	}

	protected boolean isInSubnet(byte[] ip, byte[] network, short prefix) {
		if (ip.length != network.length) {
			return false;
		}

		if (prefix / 8 > ip.length) {
			return false;
		}

		int i = 0;
		while (prefix >= 8 && i < ip.length) {
			if (ip[i] != network[i]) {
				return false;
			}
			i++;
			prefix -= 8;
		}
		if(i == ip.length) return true;
		final byte mask = (byte) ~((1 << 8 - prefix) - 1);

		return (ip[i] & mask) == (network[i] & mask);
	}

	protected void discoverNetworkInterfaces() throws InitializationException {
		try {

			Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface iface : Collections.list(interfaceEnumeration)) {
				//displayInterfaceInformation(iface);

				LOGGER.trace("Analyzing network interface: " + iface.getDisplayName());
				if (isUsableNetworkInterface(iface)) {
					LOGGER.trace("Discovered usable network interface: " + iface.getDisplayName());
					synchronized (networkInterfaces) {
						networkInterfaces.add(iface);
					}
				} else {
					LOGGER.trace("Ignoring non-usable network interface: " + iface.getDisplayName());
				}
			}

		} catch (Exception ex) {
			throw new InitializationException("Could not not analyze local network interfaces: " + ex, ex);
		}
	}

	/**
	 * Validation of every discovered network interface.
	 * <p>
	 * Override this method to customize which network interfaces are used.
	 * </p>
	 * <p>
	 * The given implementation ignores interfaces which are
	 * </p>
	 * <ul>
	 * <li>loopback (yes, we do not bind to lo0)</li>
	 * <li>down</li>
	 * <li>have no bound IP addresses</li>
	 * <li>named "vmnet*" (OS X VMWare does not properly stop interfaces when it quits)</li>
	 * <li>named "vnic*" (OS X Parallels interfaces should be ignored as well)</li>
	 * <li>named "*virtual*" (VirtualBox interfaces, for example</li>
	 * <li>named "ppp*"</li>
	 * </ul>
	 *
	 * @param iface The interface to validate.
	 * @return True if the given interface matches all validation criteria.
	 * @throws Exception If any validation test failed with an un-recoverable error.
	 */
	protected boolean isUsableNetworkInterface(NetworkInterface iface) throws Exception {
		if (!iface.isUp()) {
			LOGGER.trace("Skipping network interface (down): " + iface.getDisplayName());
			return false;
		}

		if (getInetAddresses(iface).size() == 0) {
			LOGGER.trace("Skipping network interface without bound IP addresses: " + iface.getDisplayName());
			return false;
		}

		if (iface.getName().toLowerCase(Locale.ROOT).startsWith("vmnet") ||
				(iface.getDisplayName() != null &&  iface.getDisplayName().toLowerCase(Locale.ROOT).contains("vmnet"))) {
			LOGGER.trace("Skipping network interface (VMWare): " + iface.getDisplayName());
			return false;
		}

		if (iface.getName().toLowerCase(Locale.ROOT).startsWith("vnic")) {
			LOGGER.trace("Skipping network interface (Parallels): " + iface.getDisplayName());
			return false;
		}

		if (iface.getName().toLowerCase(Locale.ROOT).contains("virtual")) {
			LOGGER.trace("Skipping network interface (named '*virtual*'): " + iface.getDisplayName());
			return false;
		}

		if (iface.getName().toLowerCase(Locale.ROOT).startsWith("ppp")) {
			LOGGER.trace("Skipping network interface (PPP): " + iface.getDisplayName());
			return false;
		}

		if (iface.isLoopback()) {
			LOGGER.trace("Skipping network interface (ignoring loopback): " + iface.getDisplayName());
			return false;
		}

		if (useInterfaces.size() > 0 && !useInterfaces.contains(iface.getName())) {
			LOGGER.trace("Skipping unwanted network interface (-D" + SYSTEM_PROPERTY_NET_IFACES + "): " + iface.getName());
			return false;
		}

		if (skipNetworkInterface(iface.getName(), iface.getDisplayName())) {
			LOGGER.trace("Skipping network interface (user setting): " + iface.getDisplayName());
			return false;
		}

		if (!iface.supportsMulticast())
			LOGGER.debug("Network interface may not be multicast capable: "  + iface.getDisplayName());

		return true;
	}

	protected boolean skipNetworkInterface(String name, String displayName) {
		return false;
	}

	protected void discoverBindAddresses() throws InitializationException {
		try {

			synchronized (networkInterfaces) {
				Iterator<NetworkInterface> it = networkInterfaces.iterator();
				while (it.hasNext()) {
					NetworkInterface networkInterface = it.next();

					LOGGER.trace("Discovering addresses of interface: " + networkInterface.getDisplayName());
					int usableAddresses = 0;
					for (InetAddress inetAddress : getInetAddresses(networkInterface)) {
						if (inetAddress == null) {
							LOGGER.debug("Network has a null address: " + networkInterface.getDisplayName());
							continue;
						}

						if (isUsableAddress(networkInterface, inetAddress)) {
							LOGGER.trace("Discovered usable network interface address: " + inetAddress.getHostAddress());
							usableAddresses++;
							synchronized (bindAddresses) {
								bindAddresses.add(inetAddress);
								interfaceFound(networkInterface, inetAddress);
							}
						} else {
							LOGGER.trace("Ignoring non-usable network interface address: " + inetAddress.getHostAddress());
						}
					}

					if (usableAddresses == 0) {
						LOGGER.trace("Network interface has no usable addresses, removing: " + networkInterface.getDisplayName());
						it.remove();
					}
				}
			}

		} catch (Exception ex) {
			throw new InitializationException("Could not not analyze local network interfaces: " + ex, ex);
		}
	}
	
	public void interfaceFound(NetworkInterface ni, InetAddress ia) {
	}

	/**
	 * Validation of every discovered local address.
	 * <p>
	 * Override this method to customize which network addresses are used.
	 * </p>
	 * <p>
	 * The given implementation ignores addresses which are
	 * </p>
	 * <ul>
	 * <li>not IPv4</li>
	 * <li>the local loopback (yes, we ignore 127.0.0.1)</li>
	 * </ul>
	 *
	 * @param networkInterface The interface to validate.
	 * @param address The address of this interface to validate.
	 * @return True if the given address matches all validation criteria.
	 */
	protected boolean isUsableAddress(NetworkInterface networkInterface, InetAddress address) {
		if (!(address instanceof Inet4Address)) {
			LOGGER.trace("Skipping unsupported non-IPv4 address: " + address);
			return false;
		}

		if (address.isLoopbackAddress()) {
			LOGGER.trace("Skipping loopback address: " + address);
			return false;
		}

		if (useAddresses.size() > 0 && !useAddresses.contains(address.getHostAddress())) {
			LOGGER.trace("Skipping unwanted address: " + address);
			return false;
		}

		return true;
	}

	protected void logInterfaceInformation(NetworkInterface networkInterface) throws SocketException {
		LOGGER.debug(String.format("Interface display name: %s", networkInterface.getDisplayName()));
		if (networkInterface.getParent() != null)
			LOGGER.debug(String.format("Parent Info: %s", networkInterface.getParent()));
		LOGGER.info(String.format("Name: %s", networkInterface.getName()));

		Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

		for (InetAddress inetAddress : Collections.list(inetAddresses)) {
			LOGGER.info(String.format("InetAddress: %s", inetAddress));
		}

		List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();

		for (InterfaceAddress interfaceAddress : interfaceAddresses) {
			if (interfaceAddress == null) {
				LOGGER.debug("Skipping null InterfaceAddress!");
				continue;
			}
			LOGGER.debug(" Interface Address");
			LOGGER.debug("  Address: " + interfaceAddress.getAddress());
			LOGGER.debug("  Broadcast: " + interfaceAddress.getBroadcast());
			LOGGER.debug("  Prefix length: " + interfaceAddress.getNetworkPrefixLength());
		}

		Enumeration<NetworkInterface> subIfs = networkInterface.getSubInterfaces();

		for (NetworkInterface subIf : Collections.list(subIfs)) {
			if (subIf == null) {
				LOGGER.debug("Skipping null NetworkInterface sub-interface");
				continue;
			}
			LOGGER.debug(String.format("\tSub Interface Display name: %s", subIf.getDisplayName()));
			LOGGER.debug(String.format("\tSub Interface Name: %s", subIf.getName()));
		}
		LOGGER.debug(String.format("Up? %s", networkInterface.isUp()));
		LOGGER.debug(String.format("Loopback? %s", networkInterface.isLoopback()));
		LOGGER.debug(String.format("PointToPoint? %s", networkInterface.isPointToPoint()));
		LOGGER.debug(String.format("Supports multicast? %s", networkInterface.supportsMulticast()));
		LOGGER.debug(String.format("Virtual? %s", networkInterface.isVirtual()));
		LOGGER.debug(String.format("Hardware address: %s", Arrays.toString(networkInterface.getHardwareAddress())));
		LOGGER.debug(String.format("MTU: %s", networkInterface.getMTU()));
		LOGGER.debug("---------------------------------------------------------------------------------");
	}
}
