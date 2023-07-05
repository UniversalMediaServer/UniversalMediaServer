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
package net.pms.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network device filter class, which supports multiple wildcards, ranges.
 * For example : 10.0.0.1,192.168.0-1.*
 */
public class NetworkDeviceFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkDeviceFilter.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String IP_FILTER_RULE_CHAR = "0123456789-.* ";
	private static final Pattern PATTERN = Pattern.compile("((\\d*)(-(\\d*))?)");

	private static final Map<String, Predicate> MATCHERS = new HashMap<>();
	private static final Set<String> LOGGED = new HashSet<>();
	private static final Map<String, InetAddressSeen> LAST_SEEN = new HashMap<>();

	private NetworkDeviceFilter() {
		throw new IllegalStateException("Static class");
	}

	public static synchronized void reset() {
		MATCHERS.clear();
		LOGGED.clear();
		String rawFilter = CONFIGURATION.getNetworkDevicesFilter();
		if (rawFilter != null) {
			String[] rules = rawFilter.split("[,;]");
			for (String r : rules) {
				Predicate p = parse(r);
				if (p != null) {
					MATCHERS.put(p.toString(), p);
				}
			}
		}
	}

	public static synchronized boolean isAllowed(InetAddress addr) {
		return isAllowed(addr, true);
	}

	/**
	 * Updates the allowlist or denylist to allow or block the IP address.
	 *
	 * @param rule the rule to modify (can be regex)
	 * @param isAllowed
	 */
	public static synchronized void setAllowed(String rule, boolean isAllowed) {
		boolean allowedByDefault = !CONFIGURATION.isNetworkDevicesBlockedByDefault();
		if (allowedByDefault == isAllowed) {
			MATCHERS.remove(rule);
		} else {
			Predicate p = parse(rule);
			if (p != null) {
				MATCHERS.put(p.toString(), p);
			}
		}
		// persist the change
		CONFIGURATION.setNetworkDevicesFilter(getNormalizedFilter());
	}

	public static synchronized boolean getBlockedByDefault() {
		return CONFIGURATION.isNetworkDevicesBlockedByDefault();
	}

	public static synchronized void setBlockedByDefault(boolean value) {
		if (value != CONFIGURATION.isNetworkDevicesBlockedByDefault()) {
			CONFIGURATION.setNetworkDevicesFilter("");
			CONFIGURATION.setNetworkDevicesBlockedByDefault(value);
			reset();
		}
	}

	public static synchronized JsonArray getNetworkDevicesAsJsonArray() {
		JsonArray result = new JsonArray();
		boolean isAllowed = !PMS.getConfiguration().isNetworkDevicesBlockedByDefault();
		JsonObject rule = new JsonObject();
		rule.addProperty("name", "localhost");
		rule.addProperty("isDefault", true);
		rule.addProperty("isAllowed", true);
		JsonArray devices = new JsonArray();
		for (InetAddressSeen addrSeen : LAST_SEEN.values()) {
			if (addrSeen.addr.isLoopbackAddress()) {
				JsonObject device = new JsonObject();
				device.addProperty("hostName", addrSeen.addr.getHostName());
				device.addProperty("ipAddress", addrSeen.addr.getHostAddress());
				device.addProperty("lastSeen", addrSeen.seen);
				devices.add(device);
			}
		}
		if (!devices.isEmpty()) {
			rule.add("devices", devices);
			result.add(rule);
		}
		for (Predicate predicate : MATCHERS.values()) {
			rule = new JsonObject();
			rule.addProperty("name", predicate.toString());
			rule.addProperty("isDefault", false);
			rule.addProperty("isAllowed", !isAllowed);
			devices = new JsonArray();
			for (InetAddressSeen addrSeen : LAST_SEEN.values()) {
				if (!addrSeen.addr.isLoopbackAddress() && predicate.match(addrSeen.addr)) {
					JsonObject device = new JsonObject();
					device.addProperty("hostName", addrSeen.addr.getHostName());
					device.addProperty("ipAddress", addrSeen.addr.getHostAddress());
					device.addProperty("lastSeen", addrSeen.seen);
					devices.add(device);
				}
			}
			rule.add("devices", devices);
			result.add(rule);
		}

		for (InetAddressSeen addrSeen : LAST_SEEN.values()) {
			if (!addrSeen.addr.isLoopbackAddress() && isAllowed(addrSeen.addr, false) == isAllowed) {
				rule = new JsonObject();
				rule.addProperty("name", addrSeen.addr.getHostAddress());
				rule.addProperty("isDefault", false);
				rule.addProperty("isAllowed", isAllowed);
				devices = new JsonArray();
				JsonObject device = new JsonObject();
				device.addProperty("hostName", addrSeen.addr.getHostName());
				device.addProperty("ipAddress", addrSeen.addr.getHostAddress());
				device.addProperty("lastSeen", addrSeen.seen);
				devices.add(device);
				rule.add("devices", devices);
				result.add(rule);
			}
		}
		return result;
	}

	private static synchronized boolean isAllowed(InetAddress addr, boolean logging) {
		boolean log = logging && isFirstDecision(addr);
		// if it is loopback address, it come from server
		if (addr.isLoopbackAddress()) {
			if (log) {
				LOGGER.trace("IP Filter: Do not filter loopback address, access granted to {}", addr.getHostAddress());
			}
			return true;
		}
		boolean allowedByDefault = !CONFIGURATION.isNetworkDevicesBlockedByDefault();
		for (Predicate p : MATCHERS.values()) {
			if (p.match(addr)) {
				if (allowedByDefault) {
					if (log) {
						LOGGER.trace("IP Filter: Access denied to {} with rule: {} because it matched the denylist", addr.getHostAddress(), p);
					}
					return false;
				} else {
					if (log) {
						LOGGER.trace("IP Filter: Access granted to {} with rule: {} because it matched the allowlist", addr.getHostAddress(), p);
					}
					return true;
				}
			}
		}

		if (log) {
			if (allowedByDefault) {
				LOGGER.info("IP Filter: Access allowed to {}", addr.getHostName());
			} else {
				LOGGER.info("IP Filter: Access denied to {}", addr.getHostName());
			}
		}

		return allowedByDefault;
	}

	private static synchronized String getNormalizedFilter() {
		StringBuilder b = new StringBuilder();
		for (Predicate r : MATCHERS.values()) {
			if (b.length() > 0) {
				b.append(',');
			}
			b.append(r);
		}

		return b.toString();
	}

	private static synchronized boolean isFirstDecision(InetAddress addr) {
		String ip = addr.getHostAddress();
		LAST_SEEN.put(ip, new InetAddressSeen(addr));
		if (!LOGGED.contains(ip)) {
			LOGGED.add(ip);
			return true;
		}
		return false;
	}

	private static Predicate parse(String rule) {
		rule = rule.trim();
		if (rule.length() == 0) {
			return null;
		}
		for (int i = 0; i < rule.length(); i++) {
			if (IP_FILTER_RULE_CHAR.indexOf(rule.charAt(i)) == -1) {
				return new HostNamePredicate(rule);
			}
		}
		String[] tags = rule.split("\\.");
		return new IpPredicate(tags);
	}

	interface Predicate {

		boolean match(InetAddress addr);
	}

	static class HostNamePredicate implements Predicate {

		String name;

		public HostNamePredicate(String n) {
			this.name = n.toLowerCase().trim();
		}

		@Override
		public boolean match(InetAddress addr) {
			return addr.getHostName().toLowerCase().contains(name);
		}

		@Override
		public String toString() {
			return name;
		}

	}

	static class IpPredicate implements Predicate {

		static class ByteRule {

			int min;
			int max;

			public ByteRule(int a, int b) {
				this.min = a;
				this.max = b;

				if (b > -1 && a > -1 && b < a) {
					this.max = a;
					this.min = b;
				}
			}

			boolean match(int value) {
				if (min >= 0 && value < min) {
					return false;
				}
				return !(max >= 0 && value > max);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see java.lang.Object#toString()
			 */
			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				if (min >= 0) {
					builder.append(min);
					if (max > min) {
						builder.append('-').append(max);
					} else {
						if (max < 0) {
							builder.append('-');
						}
					}
				} else {
					if (max >= 0) {
						builder.append('-').append(max);
					} else {
						builder.append('*');
					}
				}
				return builder.toString();
			}

		}

		List<ByteRule> byteRules;

		public IpPredicate(String[] tags) {
			this.byteRules = new ArrayList<>(tags.length);
			for (String s : tags) {
				s = s.trim();
				byteRules.add(parseTag(s));
			}
		}

		private static ByteRule parseTag(String s) {
			if ("*".equals(s)) {
				return new ByteRule(-1, -1);
			} else {
				Matcher matcher = PATTERN.matcher(s);
				if (matcher.matches()) {
					String start = matcher.group(2);
					String middle = matcher.group(3);
					String ending = matcher.group(4);
					if (valid(start)) {
						// x , x-, x-y
						int x = Integer.parseInt(start);
						if (valid(ending)) {
							// x-y
							return new ByteRule(x, Integer.parseInt(ending));
						} else {
							if (valid(middle)) {
								// x -
								return new ByteRule(x, -1);
							} else {
								// x
								return new ByteRule(x, x);
							}
						}
					} else {
						// -y
						if (valid(ending)) {
							return new ByteRule(-1, Integer.parseInt(ending));
						}
					}
				}
			}

			throw new IllegalArgumentException("Tag is not understood:" + s);

		}

		private static boolean valid(String s) {
			return s != null && s.length() > 0;
		}

		@Override
		public boolean match(InetAddress addr) {
			byte[] b = addr.getAddress();
			for (int i = 0; i < byteRules.size() && i < b.length; i++) {
				int value = b[i] < 0 ? b[i] + 256 : b[i];
				if (!byteRules.get(i).match(value)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for (ByteRule r : byteRules) {
				if (b.length() > 0) {
					b.append('.');
				}
				b.append(r);
			}
			return b.toString();
		}

	}

	static class InetAddressSeen {

		InetAddress addr;
		long seen;

		public InetAddressSeen(InetAddress addr) {
			this.addr = addr;
			seen = System.currentTimeMillis();
		}
	}

}
