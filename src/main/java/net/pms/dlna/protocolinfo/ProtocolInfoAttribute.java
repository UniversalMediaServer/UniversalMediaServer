/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
package net.pms.dlna.protocolinfo;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.io.Serializable;
import org.fourthline.cling.support.model.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.protocolinfo.ProfileName.DefaultGenericProfileName;
import net.pms.util.ParseException;

/**
 * This interface represents attributes found in the {@code additionalInfo} part
 * of {@code protocolInfo}.
 *
 * @author Nadahar
 */
public interface ProtocolInfoAttribute extends Serializable {

	/**
	 * The static factory singleton instance for creating or retrieve
	 * predefined and cached {@link ProtocolInfoAttribute} instances.
	 */
	ProtocolInfoAttributeFactory FACTORY = new ProtocolInfoAttributeFactory();

	/**
	 * @return The {@link ProtocolInfoAttributeName} instance for this
	 *         {@link ProtocolInfoAttribute}.
	 */
	ProtocolInfoAttributeName getName();

	/**
	 * @return The {@link String} value of this {@link ProtocolInfoAttribute}'s
	 *         {@link ProtocolInfoAttributeName} instance.
	 */
	String getNameString();

	/**
	 * @return The {@link String} value of this {@link ProtocolInfoAttribute}.
	 */
	String getValue();

	/**
	 * Returns a formatted attribute string for use in {@code protocolInfo}. If
	 * this {@link ProtocolInfoAttribute} has an empty name or value, or
	 * represents the implied default, an empty string is returned.
	 *
	 * @return The formatted attribute or an empty {@link String}.
	 */
	String getAttributeString();

	/**
	 * A factory for creating or retrieving cached
	 * {@link ProtocolInfoAttribute} instances.
	 */
	public static class ProtocolInfoAttributeFactory {

		/** The logger. */
		private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolInfoAttributeFactory.class);

		/**
		 * For internal use only, use {@link ProtocolInfoAttribute#FACTORY}
		 * instead.
		 */
		protected ProtocolInfoAttributeFactory() {
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, {@code null}
		 * is returned.
		 * <p>
		 * <b>{@link DLNAOrgOperations} can't be retrieved with this method and
		 * will throw an {@link IllegalArgumentException}</b>, use
		 * {@link #getAttribute(String, String, Protocol)} instead.
		 * <p>
		 * <b>Note:</b> {@link DLNAOrgFlags} isn't predefined or cached and
		 * will always return {@code null}.
		 * <p>
		 * This method can only retrieve values in their {@link String} form.
		 * Use the factory methods of the individual classes to retrieve
		 * instances for values of other types.
		 *
		 * @param attributeName the attribute name {@link String} value.
		 * @param attributeValue the attribute {@link String} value.
		 * @return The existing instance or {@code null}.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 * @throws IllegalArgumentException If {@code attributeName} is
		 *             {@code "DLNA.ORG_OP"}.
		 */
		public ProtocolInfoAttribute getAttribute(
			String attributeName,
			String attributeValue
		) throws ParseException {
			return getAttribute(
				ProtocolInfoAttributeName.FACTORY.createAttributeName(attributeName),
				attributeValue
			);
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, {@code null}
		 * is returned.
		 * <p>
		 * <b>{@link DLNAOrgOperations} can't be retrieved with this method and
		 * will throw an {@link IllegalArgumentException}</b>, use
		 * {@link #getAttribute(ProtocolInfoAttributeName, String, Protocol)}
		 * instead.
		 * <p>
		 * <b>Note:</b> {@link DLNAOrgFlags} isn't predefined or cached and
		 * will always return {@code null}.
		 * <p>
		 * This method can only retrieve values in their {@link String} form.
		 * Use the factory methods of the individual classes to retrieve
		 * instances for values of other types.
		 *
		 * @param attributeName the {@link ProtocolInfoAttributeName}.
		 * @param attributeValue the attribute {@link String} value.
		 * @return The existing instance or {@code null}.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 * @throws IllegalArgumentException If {@code attributeName} is
		 *             {@link DLNAOrgOperations#NAME}.
		 */
		public ProtocolInfoAttribute getAttribute(
			ProtocolInfoAttributeName attributeName,
			String attributeValue
		) throws ParseException {
			if (DLNAOrgOperations.NAME.equals(attributeName)) {
				throw new IllegalArgumentException(
					"Cannot get DLNA.ORG_FLAGS instance because protocol information is needed " +
					"- use an overloaded version of this method which takes a protocol argument");
			}
			return getAttribute(attributeName, attributeValue, null);
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, {@code null}
		 * is returned.
		 * <p>
		 * <b>Note:</b> {@link DLNAOrgFlags} isn't predefined or cached and
		 * will always return {@code null}.
		 * <p>
		 * This method can only retrieve values in their {@link String} form.
		 * Use the factory methods of the individual classes to retrieve
		 * instances for values of other types.
		 *
		 * @param attributeName the attribute name {@link String} value.
		 * @param attributeValue the attribute {@link String} value.
		 * @param protocol the {@link Protocol} of the {@code protocolInfo} this
		 *            {@link ProtocolInfoAttribute} belongs to. This parameter
		 *            is only used when retrieving {@link DLNAOrgOperations}
		 *            instances and can be {@code null} for other attribute
		 *            types.
		 * @return The existing instance or {@code null}.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 */
		public ProtocolInfoAttribute getAttribute(
			String attributeName,
			String attributeValue,
			Protocol protocol
		) throws ParseException {
			return getAttribute(
				ProtocolInfoAttributeName.FACTORY.createAttributeName(attributeName),
				attributeValue,
				protocol
			);
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, {@code null}
		 * is returned.
		 * <p>
		 * <b>Note:</b> {@link DLNAOrgFlags} isn't predefined or cached and
		 * will always return {@code null}.
		 * <p>
		 * This method can only retrieve values in their {@link String} form.
		 * Use the factory methods of the individual classes to retrieve
		 * instances for values of other types.
		 *
		 * @param attributeName the {@link ProtocolInfoAttributeName}.
		 * @param attributeValue the attribute {@link String} value.
		 * @param protocol the {@link Protocol} of the {@code protocolInfo} this
		 *            {@link ProtocolInfoAttribute} belongs to. This parameter
		 *            is only used when retrieving {@link DLNAOrgOperations}
		 *            instances and can be {@code null} for other attribute
		 *            types.
		 * @return The existing instance or {@code null}.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 */
		public ProtocolInfoAttribute getAttribute(
			ProtocolInfoAttributeName attributeName,
			String attributeValue,
			Protocol protocol
		) throws ParseException {
			if (attributeName == null) {
				return null;
			}

			/* Check predefined and cached types, the following classes don't have
			 * predefined or cached types and will always return null:
			 *
			 * - DLNAOrgFlags
			 *
			 */
			if (DLNAOrgConversionIndicator.NAME.equals(attributeName)) {
				return DLNAOrgConversionIndicator.FACTORY.getConversionIndicator(attributeValue);
			} else if (DLNAOrgOperations.NAME.equals(attributeName)) {
				return DLNAOrgOperations.FACTORY.getOperations(protocol, attributeValue);
			} else if (DLNAOrgPlaySpeeds.NAME.equals(attributeName)) {
				return DLNAOrgPlaySpeeds.FACTORY.getPlaySpeeds(attributeValue);
			} else if (DLNAOrgProfileName.NAME.equals(attributeName)) {
				return DLNAOrgProfileName.FACTORY.getProfileName(attributeValue);
			} else if (PanasonicComProfileName.NAME.equals(attributeName)) {
				return PanasonicComProfileName.FACTORY.getProfileName(attributeValue);
			} else if (AribOrJpProfileName.NAME.equals(attributeName)) {
				return AribOrJpProfileName.FACTORY.getProfileName(attributeValue);
			} else if (attributeName.getName().contains("_PN")) {
				return DefaultGenericProfileName.FACTORY.getProfileName(attributeName, attributeValue);
			}
			return null;
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, a new will
		 * be created.
		 * <p>
		 * <b>{@link DLNAOrgOperations} can't be retrieved with this method and
		 * will throw an {@link IllegalArgumentException}</b>, use
		 * {@link #createAttribute(String, String, Protocol)} instead.
		 * <p>
		 * This method can only retrieve or create values in their
		 * {@link String} form. Use the factory methods of the individual
		 * classes to retrieve or create instances for values of other types.
		 *
		 * @param attributeName the attribute name {@link String} value.
		 * @param attributeValue the attribute {@link String} value.
		 * @return The existing or created instance.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 * @throws IllegalArgumentException If {@code attributeName} is
		 *             {@code "DLNA.ORG_OP"}.
		 */
		public ProtocolInfoAttribute createAttribute(
			String attributeName,
			String attributeValue
		) throws ParseException {
			return createAttribute(
				ProtocolInfoAttributeName.FACTORY.createAttributeName(attributeName),
				attributeValue
			);
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, a new will
		 * be created.
		 * <p>
		 * <b>{@link DLNAOrgOperations} can't be retrieved with this method and
		 * will throw an {@link IllegalArgumentException}</b>, use
		 * {@link #createAttribute(ProtocolInfoAttributeName, String, Protocol)}
		 * instead.
		 * <p>
		 * This method can only retrieve or create values in their
		 * {@link String} form. Use the factory methods of the individual
		 * classes to retrieve or create instances for values of other types.
		 *
		 * @param attributeName the {@link ProtocolInfoAttributeName}.
		 * @param attributeValue the attribute {@link String} value.
		 * @return The existing or created instance.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 * @throws IllegalArgumentException If {@code attributeName} is
		 *             {@link DLNAOrgOperations#NAME}.
		 */
		public ProtocolInfoAttribute createAttribute(
			ProtocolInfoAttributeName attributeName,
			String attributeValue
		) throws ParseException {
			if (DLNAOrgOperations.NAME.equals(attributeName)) {
				throw new IllegalArgumentException(
					"Cannot create DLNA.ORG_FLAGS instance because protocol information is needed " +
					"- use an overloaded version of this method which takes a protocol argument");
			}
			return createAttribute(attributeName, attributeValue, null);
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, a new will
		 * be created.
		 * <p>
		 * This method can only retrieve or create values in their {@link String} form.
		 * Use the factory methods of the individual classes to retrieve or create
		 * instances for values of other types.
		 *
		 * @param attributeName the attribute name {@link String} value.
		 * @param attributeValue the attribute {@link String} value.
		 * @param protocol the {@link Protocol} of the {@code protocolInfo} this
		 *            {@link ProtocolInfoAttribute} belongs to. This parameter
		 *            is only used when retrieving {@link DLNAOrgOperations}
		 *            instances and can be {@code null} for other attribute
		 *            types.
		 * @return The existing or created instance.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 */
		public ProtocolInfoAttribute createAttribute(
			String attributeName,
			String attributeValue,
			Protocol protocol
		) throws ParseException {
			return createAttribute(
				ProtocolInfoAttributeName.FACTORY.createAttributeName(attributeName),
				attributeValue,
				protocol
			);
		}

		/**
		 * Retrieves a predefined or cached {@link ProtocolInfoAttribute}
		 * instance if it exists. If no matching instance is found, a new will
		 * be created.
		 * <p>
		 * This method can only retrieve or create values in their {@link String} form.
		 * Use the factory methods of the individual classes to retrieve or create
		 * instances for values of other types.
		 *
		 * @param attributeName the {@link ProtocolInfoAttributeName}.
		 * @param attributeValue the attribute {@link String} value.
		 * @param protocol the {@link Protocol} of the {@code protocolInfo} this
		 *            {@link ProtocolInfoAttribute} belongs to. This parameter
		 *            is only used when retrieving {@link DLNAOrgOperations}
		 *            instances and can be {@code null} for other attribute
		 *            types.
		 * @return The existing or created instance.
		 * @throws ParseException If {@code attributeValue} can't be parsed.
		 */
		public ProtocolInfoAttribute createAttribute(
			ProtocolInfoAttributeName attributeName,
			String attributeValue,
			Protocol protocol
		) throws ParseException {
			if (attributeName == null) {
				return null;
			}

			/* Check predefined and cached types, the following classes only has predefined instances:
			 *
			 * - DLNAOrgConversionIndicator
			 * - DLNAOrgOperations
			 *
			 */
			ProtocolInfoAttribute instance = getAttribute(attributeName, attributeValue, protocol);
			if (instance != null) {
				return instance;
			}

			if (DLNAOrgPlaySpeeds.NAME.equals(attributeName)) {
				return DLNAOrgPlaySpeeds.FACTORY.createPlaySpeeds(attributeValue);
			} else if (DLNAOrgFlags.NAME.equals(attributeName)) {
				DLNAOrgFlags flags = new DLNAOrgFlags(attributeValue);
				if (DLNAOrgFlags.IMPLIED.equals(flags)) {
					return DLNAOrgFlags.IMPLIED;
				}
				return flags;
			} else if (attributeName == DLNAOrgProfileName.NAME) {
				return DLNAOrgProfileName.FACTORY.createProfileName(attributeValue);
			} else if (PanasonicComProfileName.NAME.equals(attributeName)) {
				return PanasonicComProfileName.FACTORY.createProfileName(attributeValue);
			} else if (AribOrJpProfileName.NAME.equals(attributeName)) {
				return AribOrJpProfileName.FACTORY.createProfileName(attributeValue);
			} else if (attributeName.getName().contains("_PN")) {
				return DefaultGenericProfileName.FACTORY.createProfileName(attributeName, attributeValue);
			}

			// Create a new "generic" instance
			LOGGER.trace("Creating unknown ProtocolInfoAttribute \"{}\" with value \"{}\"", attributeName, attributeValue);
			return new StringAttribute(attributeName, attributeValue);
		}
	}

	/**
	 * The most basic {@link ProtocolInfoAttribute} implementation where the
	 * value is simply a {@link String}. Immutable.
	 */
	public static class StringAttribute implements ProtocolInfoAttribute {

		private static final long serialVersionUID = 1L;

		/** The attribute name */
		protected final ProtocolInfoAttributeName name;

		/** The {@link String} value */
		protected final String value;

		/**
		 * Creates a new instance with the given values.
		 *
		 * @param name the attribute name.
		 * @param value the attribute value.
		 */
		public StringAttribute(ProtocolInfoAttributeName name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public ProtocolInfoAttributeName getName() {
			return name;
		}

		@Override
		public String getNameString() {
			return name == null ? null : name.getName();
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			return name + " = " + value;
		}

		@Override
		public String getAttributeString() {
			return
				name == null || isBlank(name.getName()) || isBlank(value) ?
					"" :
					name.getName() + "=" + value;
		}
	}

}
