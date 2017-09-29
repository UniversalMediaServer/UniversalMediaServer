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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;
import net.pms.util.Rational;

/**
 * This class is immutable and represents {@code DLNA.ORG_PS} attributes. This
 * can be used for both DLNA and non-DLNA content.
 *
 * @author Nadahar
 */
public class DLNAOrgPlaySpeeds implements ProtocolInfoAttribute {

	private static final long serialVersionUID = 1L;

	/** The static {@code NONE} instance representing a blank/empty value */
	public static final DLNAOrgPlaySpeeds NONE = new DLNAOrgPlaySpeeds(new Rational[0]);

	/**
	 * The static factory used to create and retrieve {@link DLNAOrgPlaySpeeds}
	 * instances.
	 */
	public static final DLNAOrgPlaySpeedsFactory FACTORY = new DLNAOrgPlaySpeedsFactory();

	/** The static attribute name always used for this class */
	public static final ProtocolInfoAttributeName NAME = KnownProtocolInfoAttributeName.DLNA_ORG_PS;

	/** The {@link Set} of {@link Rational} play speeds */
	protected final SortedSet<Rational> speeds;

	/** The cached {@link String} representation. */
	protected final String stringValue;

	/** The cached {@code hashCode} */
	protected final int hashCode;

	/**
	 * For internal use only, use {@link #FACTORY} to create new instances.
	 *
	 * @param speeds the play-speed values.
	 */
	private DLNAOrgPlaySpeeds(Rational... speeds) {
		TreeSet<Rational> speedSet = new TreeSet<>();
		for (Rational speed : speeds) {
			if (speed != null && !Rational.ONE.equals(speed)) {
				speedSet.add(speed);
			}
		}
		this.speeds = Collections.unmodifiableSortedSet(speedSet);
		this.stringValue = generateStringValue(this.speeds);
		this.hashCode = calculateHashCode();
	}

	/**
	 * For internal use only, use {@link #FACTORY} to create new instances.
	 *
	 * @param speeds the play-speed values.
	 */
	private DLNAOrgPlaySpeeds(SortedSet<Rational> speedSet) {
		TreeSet<Rational> newSpeedsSet = new TreeSet<>();
		for (Rational speed : speedSet) {
			if (speed != null && !Rational.ONE.equals(speed)) {
				newSpeedsSet.add(speed);
			}
		}
		speeds = Collections.unmodifiableSortedSet(newSpeedsSet);
		stringValue = generateStringValue(speeds);
		hashCode = calculateHashCode();
	}


	/**
	 * @return The unmodifiable {@link Set} or {@link Rational} play speeds for
	 *         this {@link DLNAOrgPlaySpeeds} instance.
	 */
	public SortedSet<Rational> getSpeeds() {
		return speeds;
	}

	@Override
	public ProtocolInfoAttributeName getName() {
		return NAME;
	}

	@Override
	public String getNameString() {
		return NAME.getName();
	}

	@Override
	public String getValue() {
		return stringValue;
	}

	@Override
	public String getAttributeString() {
		return isBlank(stringValue) ? "" : NAME + "=" + stringValue;
	}

	@Override
	public String toString() {
		return stringValue;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (!(object instanceof DLNAOrgPlaySpeeds)) {
			return false;
		}
		DLNAOrgPlaySpeeds other = (DLNAOrgPlaySpeeds) object;
		if (speeds == null) {
			return other.speeds == null;
		} else if (speeds.size() != other.speeds.size()) {
			return false;
		}
		// Using the cached hashCode for performance
		return hashCode == other.hashCode;
	}

	/**
	 * Generates a string representation of a {@link Set} of {@link Rational}
	 * play speeds. Used for generating the cached string representation.
	 *
	 * @param speeds the {@link Set} of {@link Rational} play speeds.
	 * @return The {@link String} representation.
	 */
	protected static String generateStringValue(SortedSet<Rational> speeds) {
		StringBuilder sb = new StringBuilder();
		for (Rational speed : speeds) {
			if (speed != null && !Rational.ONE.equals(speed)) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(speed);
			}
		}
		return sb.toString();
	}

	/**
	 * Calculates the cached {@code hashCode}.
	 *
	 * @return the calculated {@code hashCode}.
	 */
	protected int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		if (speeds == null) {
			result = prime * result;
		} else {
			for (Rational speed : speeds) {
				if (speed != null) {
					result = prime * result + speed.hashCode();
				}
			}
		}
		return result;
	}

	/**
	 * A factory for creating, caching and retrieving {@link DLNAOrgPlaySpeeds}
	 * instances.
	 */
	public static class DLNAOrgPlaySpeedsFactory {

		/** The instance cache lock. */
		protected final ReentrantReadWriteLock instanceCacheLock = new ReentrantReadWriteLock();

		/** The instance cache. */
		protected final HashSet<DLNAOrgPlaySpeeds> instanceCache = new HashSet<>();

		/**
		 * For internal use only, use {@link DLNAOrgPlaySpeeds#FACTORY} to get
		 * the singleton instance.
		 */
		private DLNAOrgPlaySpeedsFactory() {
		}

		/**
		 * Retrieves an instance of {@link DLNAOrgPlaySpeeds} representing the
		 * comma separated list of play speeds in {@code values} from the
		 * predefined instances or the cache. If no such
		 * {@link DLNAOrgPlaySpeeds} instance exists, {@code null} is returned.
		 *
		 * @param values a comma separated list of play speeds in rational form.
		 * @return The {@link DLNAOrgPlaySpeeds} instance or {@code null}.
		 * @throws NumberFormatException If {@code values} cannot be parsed.
		 */
		public DLNAOrgPlaySpeeds getPlaySpeeds(String values) {

			// Check for static instances
			if (isBlank(values)) {
				return NONE;
			}

			// Parse the values
			values = values.trim();
			String[] valueArray = values.split("\\s*,\\s*");
			TreeSet<Rational> valueSet = new TreeSet<>();
			for (String value : valueArray) {
				if (isNotBlank(value)) {
					valueSet.add(Rational.valueOf(value));
				}
			}
			return getPlaySpeeds(valueSet);
		}

		/**
		 * Retrieves an instance of {@link DLNAOrgPlaySpeeds} representing the
		 * given {@link Rational} play speed values from the predefined
		 * instances or the cache. If no such {@link DLNAOrgPlaySpeeds} instance
		 * exists, {@code null} is returned.
		 *
		 * @param values the {@link Rational} play speed values.
		 * @return The {@link DLNAOrgPlaySpeeds} instance or {@code null}.
		 */
		public DLNAOrgPlaySpeeds getPlaySpeeds(Rational... values) {

			// Check for static instances
			if (values == null || values.length == 0) {
				return NONE;
			}

			// Check for cached instances
			return getPlaySpeeds(new TreeSet<Rational>(Arrays.asList(values)));
		}

		/**
		 * Retrieves an instance of {@link DLNAOrgPlaySpeeds} representing the
		 * {@link SortedSet} of {@link Rational} play speeds in {@code speedSet}
		 * from the predefined instances or the cache. If no such
		 * {@link DLNAOrgPlaySpeeds} instance exists, {@code null} is returned.
		 *
		 * @param speedSet a {@link SortedSet} of {@link Rational} play speeds.
		 * @return The {@link DLNAOrgPlaySpeeds} instance or {@code null}.
		 */
		public DLNAOrgPlaySpeeds getPlaySpeeds(SortedSet<Rational> speedSet) {

			// Check for static instances
			if (speedSet == null || speedSet.isEmpty()) {
				return NONE;
			}

			// Check for cached instances
			String stringValue = generateStringValue(speedSet);
			instanceCacheLock.readLock().lock();
			try {
				for (DLNAOrgPlaySpeeds cachedAttribute : instanceCache) {
					if (stringValue.equals(cachedAttribute.getValue())) {
						return cachedAttribute;
					}
				}
			} finally {
				instanceCacheLock.readLock().unlock();
			}
			return null;
		}

		/**
		 * Creates or retrieves an instance of {@link DLNAOrgPlaySpeeds}
		 * representing the comma separated list of play speeds in
		 * {@code values}. Any existing predefined or cached instances are first
		 * checked, and if none are found for {@code values} a new instance is
		 * created.
		 *
		 * @param values a comma separated list of play speeds in rational form.
		 * @return The {@link DLNAOrgPlaySpeeds} instance.
		 * @throws NumberFormatException If {@code values} cannot be parsed.
		 */
		public DLNAOrgPlaySpeeds createPlaySpeeds(String values) {

			// Check for static instances
			if (isBlank(values)) {
				return NONE;
			}

			// Parse the values
			values = values.trim();
			String[] valueArray = values.split("\\s*,\\s*");
			TreeSet<Rational> valueSet = new TreeSet<>();
			for (String value : valueArray) {
				if (isNotBlank(value)) {
					valueSet.add(Rational.valueOf(value));
				}
			}

			return createPlaySpeeds(valueSet);
		}

		/**
		 * Creates or retrieves an instance of {@link DLNAOrgPlaySpeeds}
		 * representing the given {@link Rational} play speed values. Any
		 * existing predefined or cached instances are first checked, and if
		 * none are found for {@code values} a new instance is created.
		 *
		 * @param values the {@link Rational} play speed values.
		 * @return The {@link DLNAOrgPlaySpeeds} instance.
		 */
		public DLNAOrgPlaySpeeds createPlaySpeeds(Rational... values) {
			// Check for static instances
			if (values == null || values.length == 0) {
				return NONE;
			}

			// Remove any "1" values from the set, they are prohibited by DLNA
			SortedSet<Rational> cleanedSpeedSet = new TreeSet<>(Arrays.asList(values));
			if (cleanedSpeedSet.contains(Rational.ONE)) {
				cleanedSpeedSet.remove(Rational.ONE);
			}

			return createPlaySpeeds(cleanedSpeedSet);
		}

		/**
		 * Creates or retrieves an instance of {@link DLNAOrgPlaySpeeds}
		 * representing {@link SortedSet} of {@link Rational} play speeds in
		 * {@code speedSet}. Any existing predefined or cached instances are
		 * first checked, and if none are found for {@code speedSet} a new
		 * instance is created.
		 *
		 * @param speedSet a {@link SortedSet} of {@link Rational} play speeds.
		 * @return The {@link DLNAOrgPlaySpeeds} instance.
		 */
		public DLNAOrgPlaySpeeds createPlaySpeeds(SortedSet<Rational> speedSet) {

			// Check for static instances
			if (speedSet == null || speedSet.isEmpty()) {
				return NONE;
			}

			// Remove any "1" values from the set, they are prohibited by DLNA
			SortedSet<Rational> cleanedSpeedSet;
			if (speedSet.contains(Rational.ONE)) {
				// Don't modify the argument (we don't even know if it's mutable)
				cleanedSpeedSet = new TreeSet<>(speedSet);
				cleanedSpeedSet.remove(Rational.ONE);
			} else {
				cleanedSpeedSet = speedSet;
			}

			// Check if an instance for this value already exists
			DLNAOrgPlaySpeeds instance = getPlaySpeeds(cleanedSpeedSet);
			if (instance != null) {
				return instance;
			}

			// Prepare to create a new instance
			instanceCacheLock.writeLock().lock();
			try {
				// Check cache again as it could have been added while the
				// lock was released
				instance = getPlaySpeeds(cleanedSpeedSet);
				if (instance != null) {
					return instance;
				}

				// None was found, create the instance
				instance = new DLNAOrgPlaySpeeds(cleanedSpeedSet);
				instanceCache.add(instance);
				return instance;
			} finally {
				instanceCacheLock.writeLock().unlock();
			}
		}
	}
}
