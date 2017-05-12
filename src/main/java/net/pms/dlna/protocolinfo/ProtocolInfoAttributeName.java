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
import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface represents {@code protocolInfo} attribute names like
 * {@code DLNA.ORG_OP}, {@code DLNA.ORG_PS} and {@code DLNA.ORG_FLAGS}.
 *
 * @author Nadahar
 */
public interface ProtocolInfoAttributeName extends Serializable {

	/** The static {@code NONE} instance representing a blank/empty name */
	ProtocolInfoAttributeName NONE = new GenericProtocolInfoAttributeName("");

	/**
	 * The static {@code WILDCARD} instance representing an asterisk
	 * ({@code *}).
	 */
	ProtocolInfoAttributeName WILDCARD = new GenericProtocolInfoAttributeName("*");

	/**
	 * The static factory used to create and retrieve
	 * {@link ProtocolInfoAttributeName} instances.
	 */
	ProtocolInfoAttributeNameFactory FACTORY = new ProtocolInfoAttributeNameFactory();

	/**
	 * Get the attribute name as a {@link String} value.
	 *
	 * @return The attribute name {@link String}.
	 */
	String getName();

	/**
	 * A factory for creating, caching and retrieving
	 * {@link ProtocolInfoAttributeName} instances.
	 */
	public static class ProtocolInfoAttributeNameFactory {

		/** The logger. */
		private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolInfoAttributeNameFactory.class);

		/** The instance cache lock. */
		private final ReentrantReadWriteLock instanceCacheLock = new ReentrantReadWriteLock();

		/** The instance cache. */
		private final HashSet<ProtocolInfoAttributeName> instanceCache = new HashSet<>();

		/**
		 * For internal use only, use {@link ProtocolInfoAttributeName#FACTORY}
		 * to get the singleton instance.
		 */
		protected ProtocolInfoAttributeNameFactory() {
		}

		/**
		 * Retrieves a {@link ProtocolInfoAttributeName} instance representing
		 * the attribute name {@code name} from the predefined instances or the
		 * cache. If no such {@link ProtocolInfoAttributeName} instance exists,
		 * {@code null} is returned.
		 *
		 * @param name the attribute name.
		 * @return The {@link ProtocolInfoAttributeName} instance or
		 *         {@code null}.
		 */
		public ProtocolInfoAttributeName getAttributeName(String name) {

			// Check for static instances
			if (isBlank(name)) {
				return NONE;
			}
			name = name.trim().toUpperCase(Locale.ROOT);

			if ("*".equals(name)) {
				return WILDCARD;
			}

			// Check for known instances
			for (KnownProtocolInfoAttributeName knownAttribute : KnownProtocolInfoAttributeName.values()) {
				if (name.equals(knownAttribute.getName())) {
					return knownAttribute;
				}
			}

			// Check for cached instances
			instanceCacheLock.readLock().lock();
			try {
				for (ProtocolInfoAttributeName cachedAttribute : instanceCache) {
					if (name.equals(cachedAttribute.getName())) {
						return cachedAttribute;
					}
				}
			} finally {
				instanceCacheLock.readLock().unlock();
			}
			return null;
		}

		/**
		 * Creates or retrieves a {@link ProtocolInfoAttributeName} instance
		 * representing the attribute name {@code name}. Any existing predefined
		 * or cached instances are first checked, and if none are found for
		 * {@code value} a new instance is created.
		 *
		 * @param name the profile name.
		 * @return The {@link ProtocolInfoAttributeName} instance.
		 */
		public ProtocolInfoAttributeName createAttributeName(String name) {

			ProtocolInfoAttributeName instance = getAttributeName(name);
			if (instance != null) {
				return instance;
			}

			// Null values will already have been handled above
			name = name.trim().toUpperCase(Locale.ROOT);

			// Prepare to create a new instance
			instanceCacheLock.writeLock().lock();
			try {
				// Check cache again as it could have been added while the lock
				// was released
				for (ProtocolInfoAttributeName cachedAttribute : instanceCache) {
					if (name.equals(cachedAttribute.getName())) {
						return cachedAttribute;
					}
				}

				// None was found, create the instance
				instance = new GenericProtocolInfoAttributeName(name);
				instanceCache.add(instance);
				LOGGER.trace("ProtocolInfoAttributeNameFactory added unknown attribute name \"{}\"", name);
				return instance;
			} finally {
				instanceCacheLock.writeLock().unlock();
			}
		}
	}

	/**
	 * This contains predefined, known {@code protocolInfo} attribute names.
	 */
	public enum KnownProtocolInfoAttributeName implements ProtocolInfoAttributeName {
		// The order is important for protocolInfo

		/** DLNA.ORG_PN */
		DLNA_ORG_PN("DLNA.ORG_PN"),

		/** ARIB.OR.JP_PN */
		ARIB_OR_JP_PN("ARIB.OR.JP_PN"),

		/** DTV_MVP_PN */
		DTV_MVP_PN("DTV_MVP_PN"),

		/** PANASONIC.COM_PN */
		PANASONIC_COM_PN("PANASONIC.COM_PN"),

		/** MICROSOFT.COM_PN */
		MICROSOFT_COM_PN("MICROSOFT.COM_PN"),

		/** SHARP.COM_PN */
		SHARP_COM_PN("SHARP.COM_PN"),

		/** SONY.COM_PN */
		SONY_COM_PN("SONY.COM_PN"),

		/** DLNA.ORG_OP */
		DLNA_ORG_OP("DLNA.ORG_OP"),

		/** DLNA.ORG_PS */
		DLNA_ORG_PS("DLNA.ORG_PS"),

		/** DLNA.ORG_CI */
		DLNA_ORG_CI("DLNA.ORG_CI"),

		/** DLNA.ORG_FLAGS */
		DLNA_ORG_FLAGS("DLNA.ORG_FLAGS"),

		/** DLNA.ORG_MAXSP */
		DLNA_ORG_MAXSP("DLNA.ORG_MAXSP");

		private final String name;

		private KnownProtocolInfoAttributeName(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * This is the default/generic class implementing
	 * {@link ProtocolInfoAttributeName}.
	 * {@link ProtocolInfoAttributeNameFactory} creates and caches instances of
	 * this class. Immutable.
	 */
	public static class GenericProtocolInfoAttributeName implements ProtocolInfoAttributeName {

		private static final long serialVersionUID = 1L;

		/** The attribute name value. */
		protected final String name;

		/**
		 * For internal use only, use
		 * {@link ProtocolInfoAttributeNameFactory#createAttributeName} to
		 * create new instances.
		 *
		 * @param name the attribute name.
		 */
		protected GenericProtocolInfoAttributeName(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
