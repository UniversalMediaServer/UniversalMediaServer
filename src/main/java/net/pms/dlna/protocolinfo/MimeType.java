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
package net.pms.dlna.protocolinfo;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.util.ParseException;

/**
 * This immutable class represents a mime-type.
 *
 * @author Nadahar
 */
public class MimeType implements Comparable<MimeType>, Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MimeType.class);
	private static final long serialVersionUID = 1L;

	/** The wildcard character */
	public static final String ANY = "*";

	/** A mime-type that represent any type and any subtype */
	public static final MimeType ANYANY = new MimeType();

	/** The type. */
	protected final String type;

	/** The subtype. */
	protected final String subtype;

	/** The {@link Map} of parameters. */
	protected final Map<String, String> parameters;

	/** The cached {@link #toString()} value */
	protected final String stringValue;

	/**
	 * Creates a new {@link MimeType} instance with both {@code type} and
	 * {@code subtype} set to {@link MimeType#ANY}.
	 */
	protected MimeType() {
		this(ANY, ANY);
	}

	/**
	 * Creates a new {@link MimeType} instance using the given values.
	 *
	 * @param type the first part of the mime-type.
	 * @param subtype the second part of the mime-type.
	 */
	public MimeType(String type, String subtype) {
		this(type, subtype, null);
	}

	/**
	 * Creates a new {@link MimeType} instance using the given values.
	 *
	 * @param type the first part of the mime-type.
	 * @param subtype the second part of the mime-type.
	 * @param parameters a {@link Map} of additional parameters for this
	 *            mime-type.
	 */
	public MimeType(String type, String subtype, Map<String, String> parameters) {
		this.type = type == null ? ANY : type;
		this.subtype = subtype == null ? ANY : subtype;
		if (parameters == null) {
			this.parameters = Collections.EMPTY_MAP;
		} else {
			TreeMap<String, String> map = new TreeMap<String, String>(new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}

			});
			for (Entry<String, String> entry : parameters.entrySet()) {
				map.put(entry.getKey(), entry.getValue());
			}
			this.parameters = Collections.unmodifiableSortedMap(map);
		}
		this.stringValue = generateStringValue();
	}

	/**
	 * @return The {@code type} (first) part of this {@link MimeType}.
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return Whether the {@code type} (first) part of this
	 *         {@link MimeType} matches anything.
	 */
	public boolean isAnyType() {
		return isBlank(type) || ANY.equals(type);
	}

	/**
	 * @return The {@code subtype} (second) part of this {@link MimeType}.
	 */
	public String getSubtype() {
		return subtype;
	}

	/**
	 * @return Whether the {@code subtype} (second) part of this
	 *         {@link MimeType} matches anything.
	 */
	public boolean isAnySubtype() {
		return ANY.equals(subtype);
	}

	/**
	 *
	 * @return A {@link Map} containing the {@code parameters} of this
	 *         {@link MimeType}. If no parameters exist, an empty
	 *         {@link Map} is returned, not {@code null}.
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Determines whether this and {@code other} is compatible. That means being
	 * either equal or having {@link #ANY} in {@code type} or {@code subtype} is
	 * such a way that they can describe the same content without taking
	 * parameters into account.
	 *
	 * @param other the {@link MimeType} to check compatibility against.
	 * @return {@code true} of this and {@code other} is compatible,
	 *         {@code false} otherwise.
	 */
	public boolean isCompatible(MimeType other) {
		if (other == null) {
			return false;
		}
		if (
			(isBlank(type) || ANY.equals(type)) &&
			isBlank(subtype) ||
			(isBlank(other.type) || ANY.equals(other.type)) &&
			isBlank(other.subtype)
		) {
			return true;
		} else if (isBlank(type) || (isBlank(other.type))) {
			return
				isBlank(subtype) ||
				isBlank(other.subtype) ||
				ANY.equals(subtype) ||
				ANY.equals(other.subtype) ||
				subtype.toLowerCase(Locale.ROOT).equals(other.subtype.toLowerCase(Locale.ROOT));
		} else if (
			type.toLowerCase(Locale.ROOT).equals(other.type.toLowerCase(Locale.ROOT)) &&
			(
				isBlank(subtype) ||
				ANY.equals(subtype) ||
				isBlank(other.subtype) ||
				ANY.equals(other.subtype)
			)
		) {
			return true;
		} else if (isBlank(subtype) || isBlank(other.subtype)) {
			return false;
		} else {
			return
				type.toLowerCase(Locale.ROOT).equals(other.type.toLowerCase(Locale.ROOT)) &&
				subtype.toLowerCase(Locale.ROOT).equals(other.subtype.toLowerCase(Locale.ROOT));
		}
	}

	/**
	 * Creates a new {@link MimeType} by attempting to parse
	 * {@code stringValue}.
	 *
	 * @param stringValue the {@link String} to parse.
	 * @return The new {@link MimeType}.
	 * @throws ParseException If {@code stringValue} isn't a valid mime-type.
	 */
	public static MimeType valueOf(String stringValue) throws ParseException {
		if (isBlank(stringValue)) {
			return ANYANY;
		}

		String[] parts = stringValue.trim().split("\\s*;\\s*");
		String[] elements = parts[0].split("\\s*/\\s*");

		String type = null;
		String subtype = null;

		if (elements.length < 2) {
			if (parts[0].equals(ANY) || isBlank(parts[0])) {
				type = ANY;
				subtype = ANY;
			} else {
				type = elements[0];
				subtype = ANY;
			}
		} else if (elements.length == 2) {
			type = elements[0];
			subtype = elements[1];
		} else if (elements.length > 2) {
			throw new ParseException("Error parsing mimetype \"" + parts[0] + "\" from \"" + stringValue + "\"");
		}

		if (parts.length > 1) {
			HashMap<String, String> parameterMap = new HashMap<>();
			for (int i = 1; i < parts.length; i++) {
				if (isBlank(parts[i])) {
					continue;
				}
				String[] parameter = parts[i].trim().split("\\s*=\\s*");
				if (parameter.length == 2 && isNotBlank(parameter[0])) {
					parameterMap.put(parameter[0], parameter[1]);
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("MimeType: Unable to parse parameter \"{}\" - it will be ignored", parts[i]);
				}
			}
			return new MimeType(type, subtype, parameterMap);
		}
		return new MimeType(type, subtype);
	}

	/**
	 * Determines if this {@link MimeType} is a Digital Rights Management
	 * mime-type.
	 *
	 * @return {@code true} if this {@link MimeType} represents DRM
	 *         mime-type, {@code false} otherwise.
	 */
	public boolean isDRM() {
		return isDTCP();
	}

	/**
	 * Determines if this {@link MimeType} is in the special DLNA-DRM format
	 * defined in the Digital Transmission Content Protection IP specification
	 * {@code DTCP-IP}.
	 *
	 * @return {@code true} if this {@link MimeType} represents a DTCP
	 *         mime-type, {@code false} otherwise.
	 */
	public boolean isDTCP() {
		return "application".equalsIgnoreCase(type) && "x-dtcp1".equalsIgnoreCase(subtype);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MimeType)) {
			return false;
		}
		MimeType other = (MimeType) obj;
		if (parameters == null) {
			if (other.parameters != null) {
				return false;
			}
		} else if (!parameters.equals(other.parameters)) {
			return false;
		}
		if (subtype == null) {
			if (other.subtype != null) {
				return false;
			}
		} else if (other.subtype == null) {
			return false;
		} else if (!subtype.toLowerCase(Locale.ROOT).equals(other.subtype.toLowerCase(Locale.ROOT))) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (other.type == null) {
			return false;
		} else if (!type.toLowerCase(Locale.ROOT).equals(other.type.toLowerCase(Locale.ROOT))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		result = prime * result + ((subtype == null) ? 0 : subtype.toLowerCase(Locale.ROOT).hashCode());
		result = prime * result + ((type == null) ? 0 : type.toLowerCase(Locale.ROOT).hashCode());
		return result;
	}

	@Override
	public String toString() {
		return stringValue;
	}

	/**
	 * Generates the {@link String} value for caching of the {@link #toString()}
	 * value.
	 *
	 * @return The generated {@link String} value.
	 */
	protected String generateStringValue() {
		StringBuilder sb = new StringBuilder(toStringWithoutParameters());
		if (parameters != null && parameters.size() > 0) {
			for (Entry<String, String> parameter : parameters.entrySet()) {
				sb.append(";").append(parameter.getKey()).append("=").append(parameter.getValue());
			}
		}
		return sb.toString();
	}

	/**
	 * Creates a {@link org.seamless.util.MimeType} from this instance.
	 *
	 * @return The new {@link org.seamless.util.MimeType} instance.
	 */
	public org.seamless.util.MimeType toSeamlessMimeType() {
		return new org.seamless.util.MimeType(type, subtype, parameters);
	}

	/**
	 * The same as {@link #toString()} but without any trailing parameters.
	 *
	 * @return The "basic mime-type" formatted {@link String}.
	 */
	public String toStringWithoutParameters() {
		return type + "/" + subtype;
	}

	@Override
	public int compareTo(MimeType other) {
		if (other == null) {
			return -1;
		}
		if (stringValue == null && other.stringValue != null) {
			return 1;
		}
		if (stringValue != null && other.stringValue == null) {
			return -1;
		}
		if (stringValue != null && other.stringValue != null) {
			return stringValue.compareTo(other.stringValue);
		}
		return 0;
	}
}
