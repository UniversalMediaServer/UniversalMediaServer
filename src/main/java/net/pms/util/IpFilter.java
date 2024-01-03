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
package net.pms.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP Filter class, which supports multiple wildcards, ranges. For example :
 * 127.0.0.1,192.168.0-1.*
 *
 * @author zsombor
 *
 */
public class IpFilter {
	private static final String IP_FILTER_RULE_CHAR = "0123456789-.* ";
	private static final Pattern PATTERN = Pattern.compile("(([0-9]*)(-([0-9]*))?)");
	private static final Logger LOGGER = LoggerFactory.getLogger(IpFilter.class);

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
			return addr.getHostName().contains(name);
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
				if (max >= 0 && value > max) {
					return false;
				}
				return true;
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

		List<ByteRule> rules;

		public IpPredicate(String[] tags) {
			this.rules = new ArrayList<>(tags.length);
			for (String s : tags) {
				s = s.trim();
				rules.add(parseTag(s));
			}
		}

		ByteRule parseTag(String s) {
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
			for (int i = 0; i < rules.size() && i < b.length; i++) {
				int value = b[i] < 0 ? b[i] + 256 : b[i];
				if (!rules.get(i).match(value)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for (ByteRule r : rules) {
				if (b.length() > 0) {
					b.append('.');
				}
				b.append(r);
			}
			return b.toString();
		}

	}

	String rawFilter;
	List<Predicate> matchers = new ArrayList<>();
	Set<String> logged = new HashSet<>();

	public IpFilter() {
	}

	public IpFilter(String f) {
		setRawFilter(f);
	}

	public synchronized String getRawFilter() {
		return rawFilter;
	}

	public synchronized void setRawFilter(String rawFilter) {
		if (this.rawFilter != null && this.rawFilter.equals(rawFilter)) {
			return;
		}
		this.matchers.clear();
		this.logged.clear();
		this.rawFilter = rawFilter;
		if (rawFilter != null) {
			String[] rules = rawFilter.split("[,;]");
			for (String r : rules) {
				Predicate p = parse(r);
				if (p != null) {
					matchers.add(p);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "IpFilter:" + getNormalizedFilter();
	}

	public synchronized String getNormalizedFilter() {
		StringBuilder b = new StringBuilder();
		for (Predicate r : matchers) {
			if (b.length() > 0) {
				b.append(',');
			}
			b.append(r);
		}

		return b.toString();
	}

	public synchronized boolean allowed(InetAddress addr) {
		boolean log = isFirstDecision(addr);
		if (matchers.isEmpty()) {
			if (log) {
				LOGGER.trace("IP Filter: No IP filter specified, access granted to {}", addr.getHostAddress());
			}
			return true;
		}
		//if it is loopback address, it come from server
		if (addr.isLoopbackAddress()) {
			return true;
		}
		for (Predicate p : matchers) {
			if (p.match(addr)) {
				if (log) {
					LOGGER.trace("IP Filter: Access granted to {} with rule: {}", addr.getHostAddress(), p);
				}
				return true;
			}
		}
		if (log) {
			LOGGER.info("IP Filter: Access denied to {}", addr.getHostName());
		}
		return false;
	}

	Predicate parse(String rule) {
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

	private synchronized boolean isFirstDecision(InetAddress addr) {
		String ip = addr.getHostAddress();
		if (!logged.contains(ip)) {
			logged.add(ip);
			return true;
		}
		return false;
	}
}
