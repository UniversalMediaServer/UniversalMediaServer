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

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

/**
 * A case-insensitive key-sorted map of headers that can join its values
 * into a combined string or regex.
 */
public class SortedHeaderMap extends TreeMap<String, String> {
	private static final long serialVersionUID = -5090333053981045429L;
	/**
	 * A case-insensitive string comparator
	 */
	public static final Comparator<String> CASE_INSENSITIVE_COMPARATOR = (String s1, String s2) -> s1.compareToIgnoreCase(s2);

	String headers = null;

	public SortedHeaderMap() {
		super(CASE_INSENSITIVE_COMPARATOR);
	}

	public SortedHeaderMap(Collection<Map.Entry<String, String>> headers) {
		this();
		for (Map.Entry<String, String> h : headers) {
			put(h.getKey(), h.getValue());
		}
	}

	@Override
	public final String put(String key, String value) {
		if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
			headers = null; // i.e. mark as changed
			return super.put(key.trim(), value.trim());
		}
		return null;
	}

	public String put(String raw) {
		return put(StringUtils.substringBefore(raw, ":"), StringUtils.substringAfter(raw, ":"));
	}

	public String joined() {
		if (headers == null) {
			headers = StringUtils.join(values(), " ");
		}
		return headers;
	}

	public String toRegex() {
		int size = size();
		return (size > 1 ? "(" : "") + StringUtils.join(values(), ").*(") + (size > 1 ? ")" : "");
	}
}

