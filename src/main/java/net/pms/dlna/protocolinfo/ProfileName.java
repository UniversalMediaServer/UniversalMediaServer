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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface represents {@code protocolInfo} profile name attributes like
 * {@code DLNA.ORG_PN} or {@code SONY.COM_PN}.
 *
 * @author Nadahar
 */
public interface ProfileName extends ProtocolInfoAttribute {

	/**
	 * An abstract factory for creating, caching and retrieving
	 * {@link ProfileName} instances.
	 *
	 * @param <E> the instance type
	 */
	public abstract static class AbstractProfileNameFactory<E extends ProfileName> {

		/** The logger. */
		private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProfileNameFactory.class);

		/** The instance cache lock. */
		protected final ReentrantReadWriteLock instanceCacheLock = new ReentrantReadWriteLock();

		/** The instance cache. */
		protected final HashSet<E> instanceCache = new HashSet<>();

		/**
		 * Gets the static {@code NONE} instance used for representing
		 * blank/empty values.
		 *
		 * @return the {@code NONE} instance.
		 */
		protected abstract E getNoneInstance();

		/**
		 * Searches through the predefined "known" instances, if any. Returns
		 * the instance if a match is found, otherwise {@code null}.
		 *
		 * @param value the value to search for.
		 * @return The found {@link ProfileName} or {@code null}.
		 */
		protected abstract E searchKnownInstances(String value);

		/**
		 * Creates a new instance of the concrete type {@link E}.
		 *
		 * @param value the value to use for the new instance.
		 * @return The new instance of {@link E}.
		 */
		protected abstract E getNewInstance(String value);

		/**
		 * Retrieves an instance of {@link E} representing the profile name
		 * {@code value} from the predefined instances or the cache. If no such
		 * {@link E} instance exists, {@code null} is returned.
		 *
		 * @param value the profile name value.
		 * @return The profile name instance of {@link E} or {@code null}.
		 */
		public E getProfileName(String value) {

			// Check for static instances
			if (isBlank(value)) {
				return getNoneInstance();
			}
			value = value.trim().toUpperCase(Locale.ROOT);

			// Check for known instances
			E instance = searchKnownInstances(value);
			if (instance != null) {
				return instance;
			}

			// Check for cached instances
			instanceCacheLock.readLock().lock();
			try {
				for (E cachedAttribute : instanceCache) {
					if (value.equals(cachedAttribute.getValue())) {
						return cachedAttribute;
					}
				}
			} finally {
				instanceCacheLock.readLock().unlock();
			}
			return null;
		}

		/**
		 * Creates or retrieves an instance of {@link E} representing the
		 * profile name {@code value}. Any existing predefined or cached
		 * instances are first checked, and if none are found for {@code value}
		 * a new instance is created.
		 *
		 * @param value the profile name value.
		 * @return The profile name instance of {@link E}.
		 */
		public E createProfileName(String value) {

			// Check if an instance for this value already exists
			E instance = getProfileName(value);
			if (instance != null) {
				return instance;
			}

			// Null values will already have been handled above
			value = value.trim().toUpperCase(Locale.ROOT);

			// Prepare to create a new instance
			instanceCacheLock.writeLock().lock();
			try {
				// Check cache again as it could have been added while the
				// lock was released
				for (E cachedAttribute : instanceCache) {
					if (value.equals(cachedAttribute.getValue())) {
						return cachedAttribute;
					}
				}

				// None was found, create the instance
				instance = getNewInstance(value);
				instanceCache.add(instance);
				LOGGER.trace("{} added unknown profile \"{}\"", getClass().getSimpleName(), value);
				return instance;
			} finally {
				instanceCacheLock.writeLock().unlock();
			}
		}
	}

	/**
	 * An abstract, immutable class of a default profile name implementation.
	 * Default profile name classes are used by the {@code ProfileNameFactories}
	 * when creating new instances not handled by any other class implementing
	 * that interface.
	 */
	public abstract static class AbstractDefaultProfileName implements ProfileName {

		private static final long serialVersionUID = 1L;

		/** The profile name value. */
		protected final String value;

		/**
		 * Instantiates a new {@link AbstractDefaultProfileName}. For use by
		 * subclasses only.
		 *
		 * @param value the profile name value.
		 */
		protected AbstractDefaultProfileName(String value) {
			this.value = value;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			return getName() + " = " + value;
		}

		@Override
		public abstract ProtocolInfoAttributeName getName();

		@Override
		public String getNameString() {
			return getName() == null ? "" : getName().getName();
		}

		@Override
		public String getAttributeString() {
			return
				isBlank(getNameString()) || isBlank(value) ?
					"" :
					getNameString() + "=" + value;
		}
	}

	/**
	 * A factory for creating, caching and retrieving
	 * {@link DefaultGenericProfileName} instances.
	 */
	public static class DefaultGenericProfileNameFactory {

		/** The logger. */
		private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProfileNameFactory.class);

		/** The instance cache lock. */
		protected final ReentrantReadWriteLock instanceCacheLock = new ReentrantReadWriteLock();

		/** The instance cache. */
		protected final HashMap<ProtocolInfoAttributeName, HashSet<DefaultGenericProfileName>> instanceCache = new HashMap<>();

		/**
		 * For internal use only, use {@link DefaultGenericProfileName#FACTORY}
		 * instead.
		 */
		protected DefaultGenericProfileNameFactory() {
		}

		/**
		 * Retrieves an instance of {@link DefaultGenericProfileName}
		 * representing attribute name {@code name} and profile name
		 * {@code value} from the cache. If no such
		 * {@link DefaultGenericProfileName} instance exists, {@code null} is
		 * returned.
		 *
		 * @param name the attribute name.
		 * @param value the profile name value.
		 * @return The {@link DefaultGenericProfileName} instance or
		 *         {@code null}.
		 */
		public DefaultGenericProfileName getProfileName(String name, String value) {
			return getProfileName(ProtocolInfoAttributeName.FACTORY.createAttributeName(name), value);
		}

		/**
		 * Retrieves an instance of {@link DefaultGenericProfileName}
		 * representing attribute name {@code name} and profile name
		 * {@code value} from the cache. If no such
		 * {@link DefaultGenericProfileName} instance exists, {@code null} is
		 * returned.
		 *
		 * @param name the {@link ProtocolInfoAttributeName}.
		 * @param value the profile name value.
		 * @return The {@link DefaultGenericProfileName} instance or
		 *         {@code null}.
		 */
		public DefaultGenericProfileName getProfileName(ProtocolInfoAttributeName name, String value) {

			// Make all blank values the same
			if (isBlank(value)) {
				value = "";
			} else {
				value = value.trim().toUpperCase(Locale.ROOT);
			}

			// Check for cached instances
			instanceCacheLock.readLock().lock();
			try {
				HashSet<DefaultGenericProfileName> set = instanceCache.get(name);
				if (set != null) {
					for (DefaultGenericProfileName cachedProfileName : set) {
						if (value.equals(cachedProfileName.getValue())) {
							return cachedProfileName;
						}
					}
				}
			} finally {
				instanceCacheLock.readLock().unlock();
			}
			return null;
		}

		/**
		 * Creates or retrieves an instance of {@link DefaultGenericProfileName}
		 * representing the profile name {@code value}. All cached instances are
		 * checked first, and if none are found for {@code name} and
		 * {@code value} a new instance is created and cached.
		 *
		 * @param name the attribute name.
		 * @param value the profile name value.
		 * @return The {@link DefaultGenericProfileName} instance.
		 */
		public DefaultGenericProfileName createProfileName(String name, String value) {
			return createProfileName(ProtocolInfoAttributeName.FACTORY.createAttributeName(name), value);
		}

		/**
		 * Creates or retrieves an instance of {@link DefaultGenericProfileName}
		 * representing the profile name {@code value}. All cached instances are
		 * checked first, and if none are found for {@code name} and
		 * {@code value} a new instance is created and cached.
		 *
		 * @param name the {@link ProtocolInfoAttributeName}.
		 * @param value the profile name value.
		 * @return The {@link DefaultGenericProfileName} instance.
		 */
		public DefaultGenericProfileName createProfileName(ProtocolInfoAttributeName name, String value) {

			// Check if an instance for this value already exists
			DefaultGenericProfileName instance = getProfileName(name, value);
			if (instance != null) {
				return instance;
			}

			// Make all blank values the same
			if (isBlank(value)) {
				value = "";
			} else {
				value = value.trim().toUpperCase(Locale.ROOT);
			}

			// Prepare to create a new instance
			instanceCacheLock.writeLock().lock();
			try {
				// Check cache again as it could have been added while the
				// lock was released
				HashSet<DefaultGenericProfileName> set = instanceCache.get(name);
				if (set != null) {
					for (DefaultGenericProfileName cachedProfileName : set) {
						if (value.equals(cachedProfileName.getValue())) {
							return cachedProfileName;
						}
					}
				} else {
					// Store this set
					set = new HashSet<>();
					instanceCache.put(name, set);
				}

				// None was found, create the instance
				instance = new DefaultGenericProfileName(name, value);
				set.add(instance);
				LOGGER.trace("Adding unknown generic \"{}\" profile name \"{}\"", name, value);
				return instance;
			} finally {
				instanceCacheLock.writeLock().unlock();
			}
		}
	}

	/**
	 * This is the default, immutable class implementing generic
	 * {@link ProfileName}, that is {@link ProfileName} types that doesn't have
	 * a specific implementation. At this time, that is any {@code _PN}
	 * attributes except {@code DLNA.ORG_PN}.
	 * <p>
	 * {@link DefaultGenericProfileNameFactory} creates and caches instances of
	 * this class.
	 */
	public static class DefaultGenericProfileName extends AbstractDefaultProfileName {

		private static final long serialVersionUID = 1L;

		/**
		 * The static factory singleton instance used to create and retrieve
		 * {@link DefaultGenericProfileName} instances.
		 */
		public static final DefaultGenericProfileNameFactory FACTORY = new DefaultGenericProfileNameFactory();

		/** The attribute name */
		protected final ProtocolInfoAttributeName name;

		/**
		 * For internal use only, use {@link #FACTORY} to create new instances.
		 *
		 * @param name the {@link ProtocolInfoAttributeName}.
		 * @param value the profile name.
		 */
		protected DefaultGenericProfileName(ProtocolInfoAttributeName name, String value) {
			super(value);
			this.name = name;
		}

		@Override
		public ProtocolInfoAttributeName getName() {
			return name;
		}

	}
}
