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

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.tree.TreePath;
import net.pms.PMS;
import net.pms.newgui.components.NICTreeNode;
import net.pms.util.ConstantList;
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

	public static class InterfaceAssociation implements Comparable<InterfaceAssociation> {

		private final NetworkInterface parentInterface;
		private final InetAddress address;
		private final NetworkInterface networkInterface;

		public InterfaceAssociation(InetAddress address, NetworkInterface networkInterface, NetworkInterface parentInterface) {
			super();
			this.address = address;
			this.networkInterface = networkInterface;
			this.parentInterface = parentInterface;
		}

		/**
		 * @return the addr
		 */
		public InetAddress getAddress() {
			return address;
		}

		/**
		 * @return the iface
		 */
		public NetworkInterface getInterface() {
			return networkInterface;
		}

		/**
		 * Returns the name of the parent of the interface association.
		 *
		 * @return The name of the parent.
		 */
		public NetworkInterface getParentInterface() {
			return parentInterface;
		}

		/**
		 * Returns the name of the {@link NetworkInterface}.
		 *
		 * @return The name.
		 */
		public String getName() {
			return networkInterface.getName();
		}

		/**
		 * Returns the display name of this {@link InterfaceAssociation}.
		 *
		 * @return The name.
		 */
		public String getDisplayName() {
			String displayName = networkInterface.getDisplayName();

			if (displayName != null) {
				displayName = displayName.trim();
			} else {
				displayName = networkInterface.getName();
			}

			if (address != null) {
				displayName += " (" + address.getHostAddress() + ")";
			}

			return displayName;
		}

		@Override
		public String toString() {
			return "InterfaceAssociation(addr = " + address + ", iface = " + networkInterface + ", parent = " + parentInterface + ')';
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((address == null) ? 0 : address.hashCode());
			result = prime * result + ((networkInterface == null) ? 0 : networkInterface.hashCode());
			result = prime * result + ((parentInterface == null) ? 0 : parentInterface.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof InterfaceAssociation)) {
				return false;
			}
			InterfaceAssociation other = (InterfaceAssociation) obj;
			if (address == null) {
				if (other.address != null) {
					return false;
				}
			} else if (!address.equals(other.address)) {
				return false;
			}
			if (networkInterface == null) {
				if (other.networkInterface != null) {
					return false;
				}
			} else if (!networkInterface.equals(other.networkInterface)) {
				return false;
			}
			if (parentInterface == null) {
				if (other.parentInterface != null) {
					return false;
				}
			} else if (!parentInterface.equals(other.parentInterface)) {
				return false;
			}
			return true;
		}

		@Override
		public int compareTo(InterfaceAssociation o) {
			if (o.getParentInterface() != null && o.getParentInterface().equals(this)) {
				return -1;
			}
			if (parentInterface != null && parentInterface.equals(o)) {
				return 1;
			}
			if (networkInterface.getDisplayName() == null || o.getInterface().getDisplayName() == null) {
				return networkInterface.getName().compareTo(o.getInterface().getName());
			}
			return networkInterface.getDisplayName().compareTo(o.getInterface().getDisplayName());
		}
	}

	/**
	 * The logger.
	 */
	private final static Logger LOGGER = LoggerFactory.getLogger(NetworkConfiguration.class);

	private final static Object instanceLock = new Object();

	/**
	 * Singleton instance of this class. All access must be protected by {@link #instanceLock}.
	 */
	private static NetworkConfiguration instance;

	/**
	 * The list of discovered network interfaces.
	 */
	private final List<InterfaceAssociation> allInterfaceAssociations;

	private final ReentrantReadWriteLock selectedLock = new ReentrantReadWriteLock();
	private final List<InterfaceAssociation> selectedInterfaceAssociations = new ArrayList<>();

	/**
	 * The map of discovered default IP addresses belonging to a network interface.
	 */
	private Map<NetworkInterface, InterfaceAssociation> mainAddress = new HashMap<>();

	/**
	 * The map of IP addresses connected to an interface name.
	 */
	private Map<String, Set<InetAddress>> addressMap = new HashMap<>();

	/**
	 * Default constructor. However, this is a singleton class: use {@link #get()} to retrieve an instance.
	 *
	 * @throws NetworkConfigurationException if a viable network configuration can't be found.
	 */
	private NetworkConfiguration() throws SocketException, NetworkConfigurationException {
		System.setProperty("java.net.preferIPv4Stack", "true");

		List<InterfaceAssociation> allInterfaceAssociations = new ArrayList<>();
		enumerateNetworkInterfaces(NetworkInterface.getNetworkInterfaces(), null, allInterfaceAssociations);
		this.allInterfaceAssociations = new ConstantList<NetworkConfiguration.InterfaceAssociation>(allInterfaceAssociations);
	}

	/**
	 * Collect all {@link InetAddress}es for the given {@link NetworkInterface}.
	 *
	 * @param networkInterface the {@link NetworkInterface} for which to enumerate addresses.
	 * @return The {@link InetAddress}es.
	 */
	private List<InetAddress> enumerateInterfaceAddresses(NetworkInterface networkInterface) {
		// networkInterface.getInterfaceAddresses() returns 'null' on some adapters if
		// the parameter 'java.net.preferIPv4Stack=true' is passed to the JVM
		// Use networkInterface.getInetAddresses() instead
		return Collections.list(networkInterface.getInetAddresses());
	}

	/**
	 * Collect all of the relevant addresses for the given network interface, add them to the global address map and
	 * return them.
	 *
	 * @param networkInterface the network interface.
	 * @return The available addresses.
	 */
	private Set<InetAddress> addAvailableAddresses(NetworkInterface networkInterface) {
		Set<InetAddress> addrSet = new HashSet<>();
		LOGGER.trace("Available addresses for \"{}\" are: {}", networkInterface.getName(),
			Collections.list(networkInterface.getInetAddresses()));

		/**
		 * networkInterface.getInterfaceAddresses() returns 'null' on some adapters if the parameter
		 * 'java.net.preferIPv4Stack=true' is passed to the JVM Use networkInterface.getInetAddresses() instead
		 */
		for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
			if (address != null) {
				if (isRelevantAddress(address)) {
					addrSet.add(address);
				}
			}
		}

		LOGGER.trace("Non loopback/IPv4 addresses for \"{}\" are: {}", networkInterface.getName(), addrSet);

		// Store the addresses
		addressMap.put(networkInterface.getName(), addrSet);

		return addrSet;
	}

	/**
	 * Returns true if the provided address is relevant, i.e. when the address is not an IPv6 address or a loopback
	 * address.
	 *
	 * @param address The address to test.
	 * @return True when the address is relevant, false otherwise.
	 */
	private boolean isRelevantAddress(InetAddress address) {
		return !(address instanceof Inet6Address || address.isLoopbackAddress());
	}

	/**
	 * Discovers the list of relevant network interfaces based on the provided list of network interfaces. The parent
	 * name is passed on for logging and identification purposes, it can be <code>null</code>.
	 *
	 * @param networkInterfaces the network interface list to check.
	 * @param parentInterface the name of the parent network interface.
	 * @return A {@link List} of all {@link InetAddress}es found on the given {@link NetworkInterface}s or an empty
	 *         {@link List} if none is found. Never returns {@code null}.
	 *
	 * @throws NetworkConfigurationException if a viable network configuration can't be found.
	 */
	private List<InetAddress> enumerateNetworkInterfaces(Enumeration<NetworkInterface> networkInterfaces, NetworkInterface parentInterface,
		List<InterfaceAssociation> interfaceAssociations) throws NetworkConfigurationException {
		List<InetAddress> addresses = new ArrayList<>();
		if (networkInterfaces == null) {
			return addresses;
		}

		List<NetworkInterface> interfaces = Collections.list(networkInterfaces);
		if (interfaces.isEmpty()) {
			if (parentInterface != null) {
				return addresses;
			}
			throw new NetworkConfigurationException("No network interfaces found");
		}

		if (LOGGER.isTraceEnabled()) {
			StringBuilder stringBuilder = new StringBuilder();
			for (NetworkInterface networkInterface : interfaces) {
				if (stringBuilder.length() > 0) {
					stringBuilder.append(", ").append(networkInterface.getDisplayName());
				} else {
					stringBuilder.append(networkInterface.getDisplayName());
				}
			}
			if (LOGGER.isTraceEnabled()) {
				if (parentInterface != null) {
					LOGGER.trace("Checking network sub interfaces for \"{}\": {}", parentInterface.getDisplayName(), stringBuilder);
				} else {
					LOGGER.trace("Checking network interfaces: {}", stringBuilder);
				}
			}
		}

		for (NetworkInterface networkInterface : interfaces) {
			if (!skipNetworkInterface(networkInterface)) {
				// check for interface has at least one IP address.
				addresses.addAll(enumerateNetworkInterface(networkInterface, parentInterface, interfaceAssociations));
			} else {
				LOGGER.trace("Child network interface ({},{}) skipped, because skip_network_interfaces='{}'", new Object[] {
						networkInterface.getName(), networkInterface.getDisplayName(), PMS.getConfiguration().getSkipNetworkInterfaces() });
			}
		}

		if (LOGGER.isTraceEnabled()) {
			if (parentInterface != null) {
				LOGGER.trace("Network sub interfaces check for \"{}\" completed", parentInterface.getDisplayName());
			} else {
				LOGGER.trace("Network interfaces check completed");
			}
		}

		// Only sort the full list
		if (parentInterface == null) {
			Collections.sort(interfaceAssociations);
		}

		return addresses;
	}

	/**
	 * Returns the list of discovered available addresses for the provided list of network interfaces.
	 *
	 * @param networkInterfaces The list of network interfaces.
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
	 * Discover the list of relevant addresses for a single network interface, taking into account that a network
	 * interface can have sub interfaces that might also have relevant addresses. Discovery is therefore handled
	 * recursively. The parent name is passed on for identification and logging purposes, it can be <code>null</code>.
	 *
	 * @param networkInterface The network interface to check.
	 * @param parentInterface The name of the parent interface.
	 * @return A {@link List} of all {@link InetAddress}es found on the given {@link NetworkInterface}s or an empty
	 *         {@link List} if none is found. Never returns {@code null}.
	 * @throws NetworkConfigurationException if a viable network configuration can't be found.
	 */
	private List<InetAddress> enumerateNetworkInterface(NetworkInterface networkInterface, NetworkInterface parentInterface,
		List<InterfaceAssociation> interfaceAssociations) throws NetworkConfigurationException {
		List<InetAddress> addresses = new ArrayList<>();

		LOGGER.trace("Checking \"{}\", display name: \"{}\"", networkInterface.getName(), networkInterface.getDisplayName());

		// Gather sub interface addresses first so they can be excluded
		List<InetAddress> subAddresses = enumerateNetworkInterfaces(networkInterface.getSubInterfaces(), networkInterface,
			interfaceAssociations);

		if (!subAddresses.isEmpty()) {
			LOGGER.trace("Sub addresses for \"{}\" is {}", networkInterface.getName(), subAddresses);
		}

		for (InetAddress address : enumerateInterfaceAddresses(networkInterface)) {
			if (address != null && !subAddresses.contains(address)) {
				addresses.add(address);
				LOGGER.trace("Checking \"{}\" on \"{}\"", address.getHostAddress(), networkInterface.getName());

				if (isRelevantAddress(address)) {
					LOGGER.trace("Found \"{}\" -> \"{}\"", networkInterface.getName(), address.getHostAddress());
					final InterfaceAssociation interfaceAssociation = new InterfaceAssociation(address, networkInterface, parentInterface);
					interfaceAssociations.add(interfaceAssociation);
					mainAddress.put(networkInterface, interfaceAssociation);
				} else if (LOGGER.isTraceEnabled()) {
					if (address.isLoopbackAddress()) {
						LOGGER.trace("Skipping \"{}\" because it's a loopback address", address.getHostAddress());
					} else {
						LOGGER.trace("Skipping \"{}\" because it's IPv6", address.getHostAddress());
					}
				}
			}
		}

		if (addresses.isEmpty()) {
			interfaceAssociations.add(new InterfaceAssociation(null, networkInterface, parentInterface));
			LOGGER.trace("Network interface \"{}\" has no valid address", networkInterface.getName());
		}

		return addresses;
	}

	/**
	 * @return All registered {@link NetworkInterface}s.
	 */
	public List<NetworkInterface> getNetworkInterfaces() {
		List<NetworkInterface> foundInterfaces = new ArrayList<>();
		for (InterfaceAssociation interfaceAssociation : allInterfaceAssociations) {
			foundInterfaces.add(interfaceAssociation.getInterface());
		}

		return foundInterfaces;
	}

	/**
	 * @return All registered {@link NetworkInterface}s that has at least one relevant address as defined by
	 *         {@link #isRelevantAddress(InetAddress)}.
	 */
	public List<NetworkInterface> getRelevantNetworkInterfaces() {
		List<NetworkInterface> foundInterfaces = new ArrayList<>();
		for (InterfaceAssociation interfaceAssociation : allInterfaceAssociations) {
			if (interfaceAssociation.getAddress() != null) {
				foundInterfaces.add(interfaceAssociation.getInterface());
			}
		}

		return foundInterfaces;
	}

	/**
	 * @return All {@link NetworkInterface}s registered with the given {@link InetAddress}
	 */
	public List<NetworkInterface> getNetworkInterfaces(InetAddress inetAddress) {
		if (inetAddress == null) {
			return null;
		}
		List<NetworkInterface> foundInterfaces = new ArrayList<>();
		for (InterfaceAssociation interfaceAssociation : allInterfaceAssociations) {
			if (inetAddress.equals(interfaceAssociation.getAddress())) {
				foundInterfaces.add(interfaceAssociation.getInterface());
			}
		}

		return foundInterfaces;
	}

	/**
	 * @param networkInterface the {@link NetworkInterface} for which to return {@link InetAddress}es.
	 * @return An array of relevant (as defined by {@link #isRelevantAddress(InetAddress)}) addresses for the given
	 *         {@link NetworkInterface} or {@code null} if none is found.
	 */
	public InetAddress[] getRelevantInterfaceAddresses(NetworkInterface networkInterface) {
		if (networkInterface == null) {
			return null;
		}

		List<InetAddress> inetAddresses = new ArrayList<>();
		for (InterfaceAssociation interfaceAssociation : allInterfaceAssociations) {
			if (interfaceAssociation.getAddress() != null && interfaceAssociation.getInterface().equals(networkInterface)) {
				inetAddresses.add(interfaceAssociation.getAddress());
			}
		}

		if (inetAddresses.size() > 0) {
			return inetAddresses.toArray(new InetAddress[inetAddresses.size()]);
		}

		return null;
	}

	/**
	 * @return An array of relevant (as defined by {@link #isRelevantAddress(InetAddress)}) addresses for the all
	 *         {@link NetworkInterface}s or {@code null} if none is found.
	 */
	public InetAddress[] getRelevantInterfaceAddresses() {
		List<InetAddress> inetAddresses = new ArrayList<>();
		for (InterfaceAssociation interfaceAssociation : allInterfaceAssociations) {
			if (interfaceAssociation.getAddress() != null) {
				inetAddresses.add(interfaceAssociation.getAddress());
			}
		}

		if (inetAddresses.size() > 0) {
			return inetAddresses.toArray(new InetAddress[inetAddresses.size()]);
		}

		return null;
	}

	/**
	 * Returns the first interface from the list of discovered interfaces that has an address. If no such interface can
	 * be found or if no interfaces were discovered, <code>null</code> is returned.
	 *
	 * @return The interface.
	 */
	//TODO: Remove
	private InterfaceAssociation getFirstInterfaceWithAddress() {
		for (InterfaceAssociation ia : allInterfaceAssociations) {
			if (ia.getAddress() != null) {
				return ia;
			}
		}

		return null;
	}

	/**
	 * Returns the default IP address associated with the the given interface name, or <code>null</code> if it has not
	 * been discovered.
	 *
	 * @param networkInterface The name of the interface.
	 * @return The IP address.
	 */
	//TODO: Remove
	public InterfaceAssociation getAddressForNetworkInterfaceName(String networkInterfaceName) {
		for (InterfaceAssociation ia : allInterfaceAssociations) {
			if (ia.getName().equalsIgnoreCase(networkInterfaceName) && ia.getAddress() != null) {
				return ia;
			}
		}
		return null;
	}

	/**
	 * Returns {@code true} if the name or display name of the argument {@link NetworkInterface} match the configured
	 * interfaces to skip, false otherwise.
	 *
	 * @param networkInterface the {@link NetworkInterface} to check.
	 * @return {@code true} if the argument {@link NetworkInterface} should be skipped, {@code false} otherwise.
	 */
	private boolean skipNetworkInterface(NetworkInterface networkInterface) {
		if (StringUtils.isBlank(networkInterface.getName())) {
			return false;
		}

		for (String interfaceName : PMS.getConfiguration().getSkipNetworkInterfaces()) {
			if (interfaceName != null) {
				// We expect the configured network interface names to skip to be
				// defined with the start of the interface name, e.g. "tap" to
				// to skip "tap0", "tap1" and "tap2", but not "etap0".
				if (networkInterface.getName().toLowerCase(Locale.ROOT).startsWith(interfaceName.toLowerCase(Locale.ROOT))
					|| (networkInterface.getDisplayName() != null && networkInterface.getDisplayName().toLowerCase(Locale.ROOT)
						.startsWith(interfaceName.toLowerCase(Locale.ROOT)))) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns the network interface for the servername configured in PMS, or <code>null</code> if no servername is
	 * configured.
	 *
	 * @return The network interface.
	 * @throws SocketException If an I/O error occurs.
	 * @throws UnknownHostException If no IP address for the server name could be found.
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
	 * Creates or returns the {@link NetworkConfiguration} singleton instance.
	 *
	 * @return The {@link NetworkConfiguration} instance.
	 */
	public static NetworkConfiguration get() {
		synchronized (instanceLock) {
			if (instance == null) {
				try {
					instance = new NetworkConfiguration();
				} catch (SocketException | NetworkConfigurationException e) {
					LOGGER.error("Fatal error when trying to detect network configuration: {}", e.getMessage());
					LOGGER.error("No network services will be available");
					LOGGER.trace("", e);
					instance = null;
				}
			}

			return instance;
		}
	}

	/**
	 * Attempts to get the name of the local computer.
	 */
	public static String getDefaultHostName() {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = InetAddress.getLoopbackAddress().getHostName();
		}
		return hostname;
	}

	/**
	 * Reinitializes the {@link NetworkConfiguration} singleton instance.
	 */
	public static void reinitialize() {
		synchronized (instanceLock) {
			try {
				instance = new NetworkConfiguration();
			} catch (SocketException | NetworkConfigurationException e) {
				LOGGER.error("Fatal error when trying to detect network configuration: {}", e.getMessage());
				LOGGER.error("No network services will be available");
				LOGGER.trace("", e);
				instance = null;
			}
		}
	}

	public void setSelected(TreePath[] selections) {
		// TODO: Update internal information
		selectedLock.writeLock().lock();
		try {
			selectedInterfaceAssociations.clear();
			for (TreePath path : selections) {
				if (path.getLastPathComponent() instanceof NICTreeNode) {
					NICTreeNode node = (NICTreeNode) path.getLastPathComponent();
					if (node.isInterface()) {
						NICTreeNode parentNode = node.getParent() instanceof NICTreeNode ? (NICTreeNode) node.getParent() : null;
						if (parentNode != null && parentNode.isInterface()) {
							// TODO: error
						}
					}
				}
			}
			Collections.sort(selectedInterfaceAssociations);
		} finally {
			selectedLock.writeLock().unlock();
		}

		// TODO: Update configuration
	}

	public static class NetworkConfigurationException extends IOException {

		private static final long serialVersionUID = 455847507700087670L;

		public NetworkConfigurationException() {
			super();
		}

		public NetworkConfigurationException(String message) {
			super(message);
		}

		public NetworkConfigurationException(String message, Throwable cause) {
			super(message, cause);
		}

		public NetworkConfigurationException(Throwable cause) {
			super(cause);
		}

	}
}
