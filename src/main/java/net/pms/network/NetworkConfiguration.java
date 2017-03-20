/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2011  G.Zsombor
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network;

import java.net.*;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class stores the network configuration information: which network
 * interfaces belong to which IP addresses, etc.
 * <p>
 * This class is a bit awkward to test, because it is largely dependent on the
 * {@link NetworkInterface} class which happens to be <code>final</code>. This
 * means it is not possible to provide mock network interface setups to the
 * class constructor and have those tested.
 *
 * @author zsombor
 *
 */
public class NetworkConfiguration {

	public static class InterfaceAssociation {
		String parentName;
		InetAddress addr;
		NetworkInterface iface;

		public InterfaceAssociation(InetAddress addr, NetworkInterface iface, String parentName) {
			super();
			this.addr = addr;
			this.iface = iface;
			this.parentName = parentName;
		}

		/**
		 * @return the addr
		 */
		public InetAddress getAddr() {
			return addr;
		}

		/**
		 * @return the iface
		 */
		public NetworkInterface getIface() {
			return iface;
		}

		/**
		 * Returns the name of the parent of the interface association.
		 *
		 * @return The name of the parent.
		 */
		public String getParentName() {
			return parentName;
		}

		/**
		 * Returns the name of the interface association.
		 *
		 * @return The name.
		 */
		public String getShortName() {
			return iface.getName();
		}

		/**
		 * Returns the display name of the interface association.
		 *
		 * @return The name.
		 */
		public String getDisplayName() {
			String displayName = iface.getDisplayName();

			if (displayName != null) {
				displayName = displayName.trim();
			} else {
				displayName = iface.getName();
			}

			if (addr != null) {
				displayName += " (" + addr.getHostAddress() + ")";
			}

			return displayName;
		}
		
		@Override
		public String toString() {
			return "InterfaceAssociation(addr=" + addr + ", iface=" + iface + ", parent=" + parentName + ')';
		}
	}

	/**
	 * The logger.
	 */
	private final static Logger LOGGER = LoggerFactory.getLogger(NetworkConfiguration.class);

	/**
	 * Singleton instance of this class.
	 */
	private static NetworkConfiguration config;

	/**
	 * The list of discovered network interfaces.
	 */
	private List<InterfaceAssociation> interfaces = new ArrayList<>();

	/**
	 * The map of discovered default IP addresses belonging to a network interface.
	 */
	private Map<String, InterfaceAssociation> mainAddress = new HashMap<>();

	/**
	 * The map of IP addresses connected to an interface name.
	 */
	private Map<String, Set<InetAddress>> addressMap = new HashMap<>();

	/**
	 * The list of configured network interface names that should be skipped.
	 * 
	 * @see PmsConfiguration#getSkipNetworkInterfaces()
	 */
	private List<String> skipNetworkInterfaces = PMS.getConfiguration().getSkipNetworkInterfaces();

	/**
	 * Default constructor. However, this is a singleton class: use
	 * {@link #getInstance()} to retrieve an instance.
	 */
	private NetworkConfiguration(Enumeration<NetworkInterface> networkInterfaces) {
		System.setProperty("java.net.preferIPv4Stack", "true");

		checkNetworkInterface(networkInterfaces, null);
	}

	/**
	 * Collect all of the relevant addresses for the given network interface,
	 * add them to the global address map and return them.
	 * 
	 * @param networkInterface
	 *            The network interface.
	 * @return The available addresses.
	 */
	private Set<InetAddress> addAvailableAddresses(NetworkInterface networkInterface) {
		Set<InetAddress> addrSet = new HashSet<>();
		LOGGER.trace("available addresses for {} is: {}", networkInterface.getName(), Collections.list(networkInterface.getInetAddresses()));

		/**
		 * networkInterface.getInterfaceAddresses() returns 'null' on some adapters if 
		 * the parameter 'java.net.preferIPv4Stack=true' is passed to the JVM
		 * Use networkInterface.getInetAddresses() instead
		 */
		for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
			if (address != null) {
				if (isRelevantAddress(address)) {
					addrSet.add(address);
				}
			}
		}

		LOGGER.trace("non loopback/ipv4 addresses: {}", addrSet);

		// Store the addresses
		addressMap.put(networkInterface.getName(), addrSet);

		return addrSet;
	}

	/**
	 * Returns true if the provided address is relevant, i.e. when the address
	 * is not an IPv6 address or a loopback address.
	 * 
	 * @param address
	 *            The address to test.
	 * @return True when the address is relevant, false otherwise.
	 */
	private boolean isRelevantAddress(InetAddress address) {
		return !(address instanceof Inet6Address || address.isLoopbackAddress());
	}

	/**
	 * Discovers the list of relevant network interfaces based on the provided
	 * list of network interfaces. The parent name is passed on for logging and
	 * identification purposes, it can be <code>null</code>.
	 * 
	 * @param networkInterfaces
	 *            The network interface list to check.
	 * @param parentName
	 *            The name of the parent network interface.
	 */
	private void checkNetworkInterface(Enumeration<NetworkInterface> networkInterfaces, String parentName) {
		if (networkInterfaces == null) {
			return;
		}

		LOGGER.trace("checkNetworkInterface(parent = {}, child interfaces = {})", parentName, networkInterfaces);

		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface ni = networkInterfaces.nextElement();

			if (!skipNetworkInterface(ni.getName(), ni.getDisplayName())) {
				// check for interface has at least one IP address.
				checkNetworkInterface(ni, parentName);
			} else {
				LOGGER.trace("child network interface ({},{}) skipped, because skip_network_interfaces='{}'",
					new Object[] { ni.getName(), ni.getDisplayName(), skipNetworkInterfaces });
			}
		}

		LOGGER.trace("checkNetworkInterface(parent = {}) finished", parentName);
	}

	/**
	 * Returns the list of discovered available addresses for the provided list
	 * of network interfaces.
	 * 
	 * @param networkInterfaces
	 *            The list of network interfaces.
	 * @return The list of addresses.
	 */
	private Set<InetAddress> getAllAvailableAddresses(Enumeration<NetworkInterface> networkInterfaces) {
		Set<InetAddress> addrSet = new HashSet<>();

		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface ni = networkInterfaces.nextElement();
			Set<InetAddress> set = addressMap.get(ni.getName());

			if (set != null) {
				addrSet.addAll(set);
			}
		}

		return addrSet;
	}

	/**
	 * Discover the list of relevant addresses for a single network interface,
	 * taking into account that a network interface can have sub interfaces that
	 * might also have relevant addresses. Discovery is therefore handled
	 * recursively. The parent name is passed on for identification and logging
	 * purposes, it can be <code>null</code>.
	 * 
	 * @param networkInterface
	 *            The network interface to check.
	 * @param parentName
	 *            The name of the parent interface.
	 */
	private void checkNetworkInterface(NetworkInterface networkInterface, String parentName) {
		LOGGER.trace("checking {}, display name: {}",networkInterface.getName(), networkInterface.getDisplayName());
		addAvailableAddresses(networkInterface);
		checkNetworkInterface(networkInterface.getSubInterfaces(), networkInterface.getName());

		// Create address / iface pairs which are not IP address of the child iface too
		Set<InetAddress> subAddress = getAllAvailableAddresses(networkInterface.getSubInterfaces());
		LOGGER.trace("sub address for {} is {}", networkInterface.getName(), subAddress);
		boolean foundAddress = false;

		// networkInterface.getInterfaceAddresses() returns 'null' on some adapters if
		// the parameter 'java.net.preferIPv4Stack=true' is passed to the JVM
		// Use networkInterface.getInetAddresses() instead
		for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
			if (address != null) {
				LOGGER.trace("checking {} on {}", new Object[] { address, networkInterface.getName() });

				if (isRelevantAddress(address)) {
					// Avoid adding duplicates
					if (!subAddress.contains(address)) {
						LOGGER.trace("found {} -> {}", networkInterface.getName(), address.getHostAddress());
						final InterfaceAssociation ia = new InterfaceAssociation(address, networkInterface, parentName);
						interfaces.add(ia);
						mainAddress.put(networkInterface.getName(), ia);
						foundAddress = true;
					}
				} else {
					LOGGER.trace("has {}, which is skipped, because loopback={}, ipv6={}", new Object[] {
						address, address.isLoopbackAddress(), (address instanceof Inet6Address)} );
				}
			}
		}

		if (!foundAddress) {
			interfaces.add(new InterfaceAssociation(null, networkInterface, parentName));
			LOGGER.trace("found {}, without valid address", networkInterface.getName());
		}
	}

	/**
	 * Returns the list of discovered interface names.
	 *
	 * @return The interface names.
	 */
	public List<String> getKeys() {
		List<String> result = new ArrayList<>(interfaces.size());

		for (InterfaceAssociation i : interfaces) {
			result.add(i.getShortName());
		}

		return result;
	}

	/**
	 * Returns the list of user friendly name names of interfaces with their IP
	 * address.
	 *
	 * @return The list of names.
	 */
	public List<String> getDisplayNames() {
		List<String> result = new ArrayList<>(interfaces.size());

		for (InterfaceAssociation i : interfaces) {
				result.add(i.getDisplayName());
		}

		return result;
	}

	/**
	 * Returns the default IP address associated with the default network interface.
	 * This is the first network interface that does not have a parent. This
	 * should avoid alias interfaces being returned. If no interfaces were discovered,
	 * <code>null</code> is returned.
	 *
	 * @return The address.
	 */
	public InterfaceAssociation getDefaultNetworkInterfaceAddress() {
		LOGGER.trace("default network interface address from {}", interfaces);
		InterfaceAssociation association = getFirstInterfaceWithAddress();

		if (association != null) {
			if (association.getParentName() != null) {
				InterfaceAssociation ia = getAddressForNetworkInterfaceName(association.getParentName());
				LOGGER.trace("first association has parent: {} -> {}", association, ia);
				return ia;
			} else {
				LOGGER.trace("first network interface: {}", association);
				return association;
			}
		}

		return null;
	}

	/**
	 * Returns the first interface from the list of discovered interfaces that
	 * has an address. If no such interface can be found or if no interfaces
	 * were discovered, <code>null</code> is returned.
	 * 
	 * @return The interface.
	 */
	private InterfaceAssociation getFirstInterfaceWithAddress() {
		for (InterfaceAssociation ia : interfaces) {
			if (ia.getAddr() != null) {
				return ia;
			}
		}

		return null;
	}

	/**
	 * Returns the default IP address associated with the the given interface
	 * name, or <code>null</code> if it has not been discovered.
	 * 
	 * @param name
	 *            The name of the interface.
	 * @return The IP address.
	 */
	public InterfaceAssociation getAddressForNetworkInterfaceName(String name) {
		return mainAddress.get(name);
	}

	/**
	 * Returns true if the name or displayname match the configured interfaces
	 * to skip, false otherwise.
	 * 
	 * @param name
	 *            The name of the interface.
	 * @param displayName
	 *            The display name of the interface.
	 * @return True if the interface should be skipped, false otherwise.
	 */
	private boolean skipNetworkInterface(String name, String displayName) {
		for (String current : skipNetworkInterfaces) {
			if (current != null) {
				// We expect the configured network interface names to skip to be
				// defined with the start of the interface name, e.g. "tap" to
				// to skip "tap0", "tap1" and "tap2", but not "etap0".
				if (name != null && name.toLowerCase().startsWith(current.toLowerCase())) {
					return true;
				}
	
				if (displayName != null && displayName.toLowerCase().startsWith(current.toLowerCase())) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns the network interface for the servername configured in PMS, or
	 * <code>null</code> if no servername is configured.
	 * 
	 * @return The network interface.
	 * @throws SocketException
	 *             If an I/O error occurs.
	 * @throws UnknownHostException
	 *             If no IP address for the server name could be found.
	 */
	public NetworkInterface getNetworkInterfaceByServerName() throws SocketException, UnknownHostException {
		String hostname = PMS.getConfiguration().getServerHostname();

		if (hostname != null) {
			LOGGER.trace("Searching network interface for " + hostname);
			return NetworkInterface.getByInetAddress(InetAddress.getByName(hostname));
		}

		return null;
	}

	/**
	 * Returns a configured NetworkConfiguration object, or <code>null</code>
	 * when an I/O error occurs.
	 *
	 * @return The network configuration.
	 */
	public static synchronized NetworkConfiguration getInstance() {
		if (config == null) {
			try {
				config = new NetworkConfiguration(NetworkInterface.getNetworkInterfaces());
			} catch (SocketException e) {
				LOGGER.error("Inspecting the network failed: " + e.getMessage(), e);
			}
		}

		return config;
	}

	/**
	 * Forget the cached configuration.
	 */
	public static synchronized void forgetConfiguration() {
		config = null;
	}
}
