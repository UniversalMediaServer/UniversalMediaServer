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
package net.pms.network.configuration;

import com.google.gson.JsonArray;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.network.mediaserver.MediaServerNetworkConfigurationListener;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
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

	/**
	 * The logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkConfiguration.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * Interval for checking network configuration.
	 */
	private static final int CHECK_DELAY = 5000;

	/**
	 * The list of discovered network INTERFACES.
	 */
	private static final Map<Integer, NetworkInterfaceAssociation> INTERFACES_ASSOCIATIONS = new LinkedHashMap<>();

	/**
	 * The insertion-ordered map of discovered default IP addresses belonging to a network interface.
	 * This assumes that interfaces order in the OS is made upon their importance.
	 */
	private static final List<Integer> INTERFACES_WITH_ASSOCIATED_ADDRESS = new ArrayList<>();

	/**
	 * The map of IP addresses connected to an interface name.
	 */
	private static final Map<Integer, Set<InetAddress>> ADDRESS_MAP = new HashMap<>();

	/**
	 * The list of configured network interface names that should be skipped.
	 *
	 * @see UmsConfiguration#getSkipNetworkInterfaces()
	 */
	private static final List<String> SKIP_NETWORK_INTERFACES = PMS.getConfiguration().getSkipNetworkInterfaces();
	private static final List<NetworkConfigurationListenerInterface> LISTENERS = new ArrayList<>();
	private static final List<NetworkConfigurationListenerInterface> LISTENERS_REMOVED = new ArrayList<>();

	private static boolean isInitiated = false;
	private static boolean isRunning = false;
	// The watcher thread.
	private static Thread watcherThread;

	/**
	 * This class is not meant to be instantiated.
	 */
	private NetworkConfiguration() {
	}

	public static void init() {
		LOGGER.trace("Initializing the network scanner");
		System.setProperty("java.net.preferIPv4Stack", "true");
		checkNetworkInterfaces(true);
		//add MediaServer listener
		LISTENERS.add(new MediaServerNetworkConfigurationListener());
		isInitiated = true;
	}

	public static void start() {
		if (!isInitiated) {
			init();
		}
		isRunning = true;
		if (watcherThread == null) {
			watcherThread = new Thread(run(), "NetworkConfiguration Watcher");
			watcherThread.start();
		}
	}

	public static void stop() {
		isRunning = false;
		if (watcherThread != null) {
			LOGGER.trace("Stopping the network scanner");
			watcherThread.interrupt();
			watcherThread = null;
		}
	}

	private static Runnable run() {
		return () -> {
			LOGGER.trace("Starting the network scanner");
			while (isRunning) {
				UMSUtils.sleep(CHECK_DELAY);
				LOGGER.trace("Checking network configuration changes");
				checkKnownValues();
				checkNetworkInterfaces(false);
			}
		};
	}

	public static void addListener(NetworkConfigurationListenerInterface listener) {
		cleanListeners();
		synchronized (LISTENERS) {
			LISTENERS.add(listener);
		}
	}

	public static void removeListener(NetworkConfigurationListenerInterface listener) {
		LISTENERS_REMOVED.add(listener);
	}

	public static void cleanListeners() {
		synchronized (LISTENERS) {
			if (!LISTENERS_REMOVED.isEmpty()) {
				for (NetworkConfigurationListenerInterface listener : LISTENERS_REMOVED) {
					LISTENERS.remove(listener);
				}
				LISTENERS_REMOVED.clear();
			}
		}
	}

	private static void checkKnownValues() {
		synchronized (INTERFACES_ASSOCIATIONS) {
			for (NetworkInterfaceAssociation netInterface : INTERFACES_ASSOCIATIONS.values()) {
				if (netInterface.statusChanged()) {
					if (netInterface.wasUp()) {
						LOGGER.trace("interface {} with associated address {}: Interface is down", netInterface.iface.getName(), netInterface.iface.getIndex(), netInterface.getAddr());
						for (NetworkConfigurationListenerInterface listener : LISTENERS) {
							listener.networkInterfaceDown(netInterface);
						}
						cleanListeners();
					} else {
						LOGGER.trace("interface {} with associated address {}: Interface is up", netInterface.iface.getName(), netInterface.iface.getIndex(), netInterface.getAddr());
						for (NetworkConfigurationListenerInterface listener : LISTENERS) {
							listener.networkInterfaceUp(netInterface);
						}
						cleanListeners();
					}
					netInterface.updateStatus();
				}
			}
		}
	}

	//Store the addresses
	private static void addressMapStore(NetworkInterface networkInterface, Set<InetAddress> addrSet) {
		synchronized (ADDRESS_MAP) {
			int interfaceIndex = networkInterface.getIndex();
			if (ADDRESS_MAP.containsKey(interfaceIndex)) {
				Set<InetAddress> oldAddrSet = ADDRESS_MAP.get(interfaceIndex);
				Set<InetAddress> removedAddresses = new HashSet<>();
				Set<InetAddress> addedAddresses = new HashSet<>();
				for (InetAddress address : oldAddrSet) {
					if (!addrSet.contains(address)) {
						removedAddresses.add(address);
						LOGGER.trace("address {} is no more mapped to the interface {}({})", address, networkInterface.getName(), interfaceIndex);
					}
				}
				for (InetAddress address : addrSet) {
					if (!oldAddrSet.contains(address)) {
						addedAddresses.add(address);
						LOGGER.trace("new found address {} is mapped to the interface {}({})", address, networkInterface.getName(), interfaceIndex);
					}
				}
				if (!removedAddresses.isEmpty() || !addedAddresses.isEmpty()) {
					ADDRESS_MAP.put(interfaceIndex, addrSet);
					//now advise the listeners
					for (InetAddress address : removedAddresses) {
						for (NetworkConfigurationListenerInterface listener : LISTENERS) {
							listener.networkInterfaceAddressRemoved(networkInterface, address);
						}
						cleanListeners();
					}
					for (InetAddress address : addedAddresses) {
						for (NetworkConfigurationListenerInterface listener : LISTENERS) {
							listener.networkInterfaceAddressAdded(networkInterface, address);
						}
						cleanListeners();
					}
				}
			} else {
				//newly interface
				ADDRESS_MAP.put(interfaceIndex, addrSet);
				//now advise the listeners
				for (InetAddress address : addrSet) {
					for (NetworkConfigurationListenerInterface listener : LISTENERS) {
						listener.networkInterfaceAddressAdded(networkInterface, address);
					}
					cleanListeners();
				}
			}
		}
	}

	//Store the NetworkInterfaceAssociation
	private static void interfacesStore(int interfaceIndex, NetworkInterfaceAssociation ia) {
		synchronized (INTERFACES_ASSOCIATIONS) {
			if (INTERFACES_ASSOCIATIONS.containsKey(interfaceIndex)) {
				NetworkInterfaceAssociation oldIa = INTERFACES_ASSOCIATIONS.get(interfaceIndex);
				if ((oldIa.addr == null && ia.addr != null) ||
						(oldIa.addr != null && !oldIa.addr.equals(ia.addr)) ||
						(oldIa.parentName == null && ia.parentName != null) ||
						(oldIa.parentName != null && !oldIa.parentName.equals(ia.parentName))) {
					INTERFACES_ASSOCIATIONS.put(interfaceIndex, ia);
					LOGGER.trace("changed interface {}({}) with newly associated address {}", ia.iface.getName(), interfaceIndex, ia.getAddr());
					//now advise the listeners
					for (NetworkConfigurationListenerInterface listener : LISTENERS) {
						listener.networkInterfaceDefaultAddressChanged(ia.iface, ia.addr);
					}
					cleanListeners();
				}
			} else {
				INTERFACES_ASSOCIATIONS.put(interfaceIndex, ia);
				//now advise the listeners
				for (NetworkConfigurationListenerInterface listener : LISTENERS) {
					listener.networkInterfaceAdded(ia.iface, ia.addr);
				}
				cleanListeners();
			}
		}
	}

	//Store the INTERFACES_WITH_ASSOCIATED_ADDRESS
	private static void interfacesWithAssociatedAddressStore(int interfaceIndex) {
		boolean changed = false;
		synchronized (INTERFACES_WITH_ASSOCIATED_ADDRESS) {
			if (!INTERFACES_WITH_ASSOCIATED_ADDRESS.contains(interfaceIndex)) {
				//newly multicast interface found
				LOGGER.trace("available multicast interface with address #{}", interfaceIndex);
				INTERFACES_WITH_ASSOCIATED_ADDRESS.add(interfaceIndex);
				changed = true;
			}
		}
		if (changed) {
			//now advise the listeners
			for (NetworkConfigurationListenerInterface listener : LISTENERS) {
				listener.networkInterfaceWithAddressAdded();
			}
		}
	}

	/**
	 * Collect all of the relevant addresses for the given network interface,
	 * add them to the global address map and return them.
	 *
	 * @param networkInterface
	 *            The network interface.
	 * @return The available addresses.
	 */
	private static Set<InetAddress> addAvailableAddresses(NetworkInterface networkInterface, boolean showTrace) {
		Set<InetAddress> addrSet = new HashSet<>();
		if (showTrace) {
			LOGGER.trace("available addresses for {} is: {}", networkInterface.getName(), Collections.list(networkInterface.getInetAddresses()));
		}

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

		if (showTrace) {
			LOGGER.trace("non loopback/ipv4 addresses: {}", addrSet);
		}

		// Store the addresses
		addressMapStore(networkInterface, addrSet);

		return addrSet;
	}

	/**
	 * Discovers the list of relevant network INTERFACES for the system.
	 */
	private static void checkNetworkInterfaces(boolean showTrace) {
		try {
			checkNetworkInterface(NetworkInterface.getNetworkInterfaces(), null, showTrace);
		} catch (SocketException ex) {
			if (showTrace) {
				LOGGER.error("Inspecting the network failed: {}", ex.getMessage(), ex);
			}
		}
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
	private static void checkNetworkInterface(Enumeration<NetworkInterface> networkInterfaces, String parentName, boolean showTrace) {
		if (networkInterfaces == null) {
			return;
		}

		if (showTrace) {
			LOGGER.trace("checkNetworkInterface(parent = {}, child interfaces = {})", parentName, networkInterfaces);
		}

		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface ni = networkInterfaces.nextElement();

			if (!skipNetworkInterface(ni.getName(), ni.getDisplayName())) {
				// check for interface has at least one IP address.
				checkNetworkInterface(ni, parentName, showTrace);
			} else {
				if (showTrace) {
					synchronized (SKIP_NETWORK_INTERFACES) {
						LOGGER.trace("child network interface ({},{}) skipped, because skip_network_interfaces='{}'",
							new Object[] {ni.getName(), ni.getDisplayName(), SKIP_NETWORK_INTERFACES});
					}
				}
			}
		}

		if (showTrace) {
			LOGGER.trace("checkNetworkInterface(parent = {}) finished", parentName);
		}
	}

	/**
	 * Returns the list of discovered available addresses for the provided list
 of network INTERFACES.
	 *
	 * @param networkInterfaces
	 *            The list of network INTERFACES.
	 * @return The list of addresses.
	 */
	private static Set<InetAddress> getAllAvailableAddresses(Enumeration<NetworkInterface> networkInterfaces) {
		Set<InetAddress> addrSet = new HashSet<>();

		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface ni = networkInterfaces.nextElement();
			synchronized (ADDRESS_MAP) {
				Set<InetAddress> set = ADDRESS_MAP.get(ni.getIndex());
				if (set != null) {
					addrSet.addAll(set);
				}
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
	private static void checkNetworkInterface(NetworkInterface networkInterface, String parentName, boolean showTrace) {
		final int interfaceIndex = networkInterface.getIndex();
		if (showTrace) {
			LOGGER.trace("checking {}({}), display name: {}", networkInterface.getName(), interfaceIndex, networkInterface.getDisplayName());
		}
		addAvailableAddresses(networkInterface, showTrace);
		checkNetworkInterface(networkInterface.getSubInterfaces(), networkInterface.getName(), showTrace);

		// Create address / iface pairs which are not IP address of the child iface too
		Set<InetAddress> subAddress = getAllAvailableAddresses(networkInterface.getSubInterfaces());
		if (showTrace) {
			LOGGER.trace("sub address for {}({}) is {}", networkInterface.getName(), interfaceIndex, subAddress);
		}
		boolean foundAddress = false;

		// networkInterface.getInterfaceAddresses() returns 'null' on some adapters if
		// the parameter 'java.net.preferIPv4Stack=true' is passed to the JVM
		// Use networkInterface.getInetAddresses() instead
		for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
			if (address != null) {
				if (showTrace) {
					LOGGER.trace("checking {} on {}({})", address, networkInterface.getName(), interfaceIndex);
				}
				if (isRelevantAddress(address)) {
					// Avoid adding duplicates
					if (!subAddress.contains(address)) {
						if (showTrace) {
							LOGGER.trace("found {}({}) -> {}", networkInterface.getName(), interfaceIndex, address.getHostAddress());
						}

						final NetworkInterfaceAssociation ia = new NetworkInterfaceAssociation(address, networkInterface, parentName);
						interfacesStore(interfaceIndex, ia);
						try {
							// Add network interface to the list of INTERFACES with associated address only
							// if interface is supporting multicast.
							if (networkInterface.supportsMulticast()) {
								interfacesWithAssociatedAddressStore(interfaceIndex);
							} else {
								if (showTrace) {
									LOGGER.trace("Ignoring interface {}({}) with associated address {} because it does not seem to support multicast", networkInterface.getName(), interfaceIndex, ia.getAddr());
								}
							}
						} catch (SocketException e) {
							if (showTrace) {
								LOGGER.trace("Interface {}({}) raised exception when checking the multicast capability", networkInterface.getName(), interfaceIndex);
							}
						}

						foundAddress = true;
					}
				} else {
					if (showTrace) {
						LOGGER.trace("has {}, which is skipped, because loopback={}, ipv6={}", new Object[] {
							address, address.isLoopbackAddress(), (address instanceof Inet6Address)});
					}
				}
			}
		}

		if (!foundAddress) {
			interfacesStore(interfaceIndex, new NetworkInterfaceAssociation(null, networkInterface, parentName));
			if (showTrace) {
				LOGGER.trace("found {}({}), without valid address", networkInterface.getName(), interfaceIndex);
			}
		}
	}

	/**
	 * Returns the list of user friendly names of INTERFACES with their IP address.
	 *
	 * @return The list of names.
	 */
	public static List<String> getDisplayNamesWithAddress() {
		synchronized (INTERFACES_ASSOCIATIONS) {
			List<String> result = new ArrayList<>(INTERFACES_ASSOCIATIONS.size());

			for (NetworkInterfaceAssociation i : INTERFACES_ASSOCIATIONS.values()) {
				result.add(i.getDisplayNameWithAddress());
			}
			return result;
		}
	}

	/**
	 * Returns the list of user friendly name names of INTERFACES with their IP address.
	 *
	 * @return The list of names.
	 */
	public static List<String> getDisplayNames() {
		synchronized (INTERFACES_ASSOCIATIONS) {
			List<String> result = new ArrayList<>(INTERFACES_ASSOCIATIONS.size());

			for (NetworkInterfaceAssociation i : INTERFACES_ASSOCIATIONS.values()) {
				result.add(i.getDisplayName());
			}

			return result;
		}
	}

	/**
	 * @return available network interfaces as a JSON array
	 */
	public static synchronized JsonArray getNetworkInterfacesAsJsonArray() {
		List<String> values = getDisplayNames();
		List<String> labels = getDisplayNamesWithAddress();
		values.add(0, "");
		labels.add(0, "i18n@AutoDetect");
		return UMSUtils.getListsAsJsonArrayOfObjects(values, labels, null);
	}

	/**
	 * Returns the default IP address associated with the default network interface.
	 * This is the first network interface that does not have a parent. This
	 * should avoid alias interfaces being returned. If no interfaces were discovered,
	 * <code>null</code> is returned.
	 *
	 * @return The address.
	 */
	public static NetworkInterfaceAssociation getDefaultNetworkInterfaceAddress() {
		synchronized (INTERFACES_ASSOCIATIONS) {
			LOGGER.trace("default network interface address from {}", INTERFACES_ASSOCIATIONS);
		}
		NetworkInterfaceAssociation association = getFirstInterfaceWithAddress();

		if (association != null) {
			if (association.getParentName() != null) {
				NetworkInterfaceAssociation ia = getAddressForNetworkInterfaceName(association.getParentName());
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
	 * Returns the first interface from the list of discovered interfaces whose
	 * have an IPV4 address and are supporting multicast. If the real interface
	 * is not found the first virtual interface is returned.
	 * If no such interface can be found or if no interfaces were discovered the
	 * <code>null</code> is returned.
	 *
	 * @return The first interface with the associated IP address.
	 */
	private static NetworkInterfaceAssociation getFirstInterfaceWithAddress() {
		synchronized (INTERFACES_WITH_ASSOCIATED_ADDRESS) {
			if (INTERFACES_WITH_ASSOCIATED_ADDRESS.isEmpty()) {
				return null;
			}

			List<Integer> virtualInterfaces = new ArrayList<>();
			for (Integer interfaceIndex : INTERFACES_WITH_ASSOCIATED_ADDRESS) {
				if (INTERFACES_ASSOCIATIONS.get(interfaceIndex).getDisplayName().toLowerCase().contains("virtual")) {
					virtualInterfaces.add(interfaceIndex);
					continue;
				}

				return INTERFACES_ASSOCIATIONS.get(interfaceIndex);
			}

			// We did not find any non-virtual INTERFACES, so choose the first virtual one if it exists
			if (!virtualInterfaces.isEmpty()) {
				return INTERFACES_ASSOCIATIONS.get(virtualInterfaces.get(0));
			}

			return null;
		}
	}

	/**
	 * Returns true if the name or displayname match the configured INTERFACES
	 * to skip, false otherwise.
	 *
	 * @param name
	 *            The name of the interface.
	 * @param displayName
	 *            The display name of the interface.
	 * @return True if the interface should be skipped, false otherwise.
	 */
	private static boolean skipNetworkInterface(String name, String displayName) {
		synchronized (SKIP_NETWORK_INTERFACES) {
			for (String current : SKIP_NETWORK_INTERFACES) {
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
	}

	/**
	 * Returns the network interface for the servername configured in UMS, or
	 * <code>null</code> if no servername is configured.
	 *
	 * @return The network interface.
	 * @throws SocketException
	 *             If an I/O error occurs.
	 * @throws UnknownHostException
	 *             If no IP address for the server name could be found.
	 */
	public static NetworkInterface getNetworkInterfaceByServerName() throws SocketException, UnknownHostException {
		String hostname = CONFIGURATION.getServerHostname();

		if (hostname != null) {
			LOGGER.trace("Searching network interface for {}", hostname);
			return NetworkInterface.getByInetAddress(InetAddress.getByName(hostname));
		}

		return null;
	}

	public static NetworkInterfaceAssociation getNetworkInterfaceAssociationFromConfig() {
		boolean forcedNetworkInterface = StringUtils.isNotEmpty(CONFIGURATION.getNetworkInterface());
		boolean forcedInetAddress = StringUtils.isNotBlank(CONFIGURATION.getServerHostname());
		NetworkInterfaceAssociation ia = null;
		if (forcedNetworkInterface) {
			LOGGER.debug("Using forced network interface: {}", CONFIGURATION.getNetworkInterface());
			ia = NetworkConfiguration.getAddressForNetworkInterfaceName(CONFIGURATION.getNetworkInterface());
			if (ia == null) {
				LOGGER.error("Forced network interface {} not found on this system", CONFIGURATION.getNetworkInterface().trim().replace('\n', ' '));
				return null;
			}
			if (!ia.isUp()) {
				LOGGER.error("Forced network interface {} is down", CONFIGURATION.getNetworkInterface().trim().replace('\n', ' '));
				return null;
			}
		}
		String hostname = CONFIGURATION.getServerHostname();
		//look for ip on forced address
		if (forcedInetAddress) {
			LOGGER.debug("Using forced address: {}", hostname);
			InetAddress inetAddress = null;
			try {
				inetAddress = InetAddress.getByName(hostname);
			} catch (UnknownHostException ex) {
				//handle after
			}
			if (inetAddress == null) {
				LOGGER.error("Forced address {} is unknowned on this system", hostname);
				if (ia != null) {
					Set<InetAddress> set = ADDRESS_MAP.get(ia.iface.getIndex());
					LOGGER.info("Good addresses for the requested network interface are: {}", set);
				}
				return null;
			} else {
				//cleanup the ip
				hostname = inetAddress.getHostAddress();
				try {
					NetworkInterface tmpNetworkInterface = NetworkInterface.getByInetAddress(inetAddress);
					if (ia == null) {
						//mean not forcedNetworkInterface
						if (tmpNetworkInterface.isUp()) {
							return new NetworkInterfaceAssociation(inetAddress, tmpNetworkInterface, null);
						} else {
							LOGGER.error("Forced address {} network interface {} is down", hostname, tmpNetworkInterface.getName().trim().replace('\n', ' '));
							return null;
						}
					} else {
						//mean forcedNetworkInterface
						Set<InetAddress> set = ADDRESS_MAP.get(ia.iface.getIndex());
						if (set.contains(inetAddress)) {
							return new NetworkInterfaceAssociation(inetAddress, ia.iface, ia.parentName);
						} else {
							LOGGER.error("Forced address {} is unknowned on the requested network interface", hostname);
							LOGGER.info("Good addresses for the requested network interface {} are:{}", CONFIGURATION.getNetworkInterface().trim().replace('\n', ' '), set);
							return null;
						}
					}
				} catch (SocketException ex) {
					LOGGER.error("Forced address {} network interface not found on this system", hostname);
					return null;
				}
			}
		}
		//look for ip on forced network interface
		if (ia != null) {
			if (ia.addr != null) {
				return ia;
			} else {
				LOGGER.error("Forced network interface {} don't have any IP address assigned", ia.iface.getName().trim().replace('\n', ' '));
				return null;
			}
		}
		//mean nothing forced from config
		//look for ip on default network interface
		ia = NetworkConfiguration.getDefaultNetworkInterfaceAddress();
		if (ia != null) {
			return ia;
		} else {
			LOGGER.error("No default network interface found on this system");
		}
		return null;
	}

	/**
	 * for backwards-compatibility check if the short network interface name is used
	 *
	 * @param interfaceName
	 * @return the standard display name
	 */
	public static String replaceShortInterfaceNameByDisplayName(String interfaceName) {
		if (StringUtils.isNotBlank(interfaceName)) {
			synchronized (INTERFACES_ASSOCIATIONS) {
				for (NetworkInterfaceAssociation netInterface : INTERFACES_ASSOCIATIONS.values()) {
					if (netInterface.getShortName().equals(interfaceName)) {
						interfaceName = netInterface.getDisplayName();
						break;
					}
				}
			}
		}
		return interfaceName;
	}

	/**
	 * Get Interface Index From DisplayName
	 *
	 * @param interfaceName
	 * @return the interface index
	 */
	public static NetworkInterfaceAssociation getInterfaceAssociationFromDisplayName(String interfaceName) {
		interfaceName = replaceShortInterfaceNameByDisplayName(interfaceName);
		if (StringUtils.isNotBlank(interfaceName)) {
			synchronized (INTERFACES_ASSOCIATIONS) {
				for (NetworkInterfaceAssociation netInterface : INTERFACES_ASSOCIATIONS.values()) {
					if (netInterface.getDisplayName().equals(interfaceName)) {
						return netInterface;
					}
				}
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
	public static NetworkInterfaceAssociation getAddressForNetworkInterfaceName(String name) {
		// for backwards-compatibility check if the short network interface name is used
		NetworkInterfaceAssociation ia = getInterfaceAssociationFromDisplayName(name);
		if (ia != null) {
			synchronized (INTERFACES_WITH_ASSOCIATED_ADDRESS) {
				if (INTERFACES_WITH_ASSOCIATED_ADDRESS.contains(ia.iface.getIndex())) {
					return ia;
				}
			}
		}
		return null;
	}

	/**
	 * Returns true if the provided address is relevant, i.e. when the address
	 * is not an IPv6 address or a loopback address.
	 *
	 * @param address
	 *            The address to test.
	 * @return True when the address is relevant, false otherwise.
	 */
	private static boolean isRelevantAddress(InetAddress address) {
		return !(address == null || address instanceof Inet6Address || address.isLoopbackAddress());
	}

}
