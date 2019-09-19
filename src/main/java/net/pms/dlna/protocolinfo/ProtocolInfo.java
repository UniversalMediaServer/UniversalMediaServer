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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.dlna.DLNAAttribute;
import org.fourthline.cling.support.model.dlna.DLNAProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;
import net.pms.util.ParseException;


/**
 * This immutable class represents a {@code protocolInfo} element.
 *
 * @author Nadahar
 */
public class ProtocolInfo implements Comparable<ProtocolInfo>, Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolInfo.class);

	/** The wildcard character {@code "*"} */
	public static final String WILDCARD = "*";

	/** A static instance of an empty attribute map */
	public static final SortedMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> EMPTYMAP =
		Collections.unmodifiableSortedMap(createEmptyAttributesMap());

	/** The protocol (first field) of {@code protocolInfo} */
	protected final Protocol protocol;

	/** The network (second field) of {@code protocolInfo} */
	protected final String network;

	/** The contentType (third field) of {@code protocolInfo} */
	protected final MimeType mimeType;

	/** The {@code additionalInfo} (fourth field) of {@code protocolInfo} */
	protected final String additionalInfo;

	/** The attributes parsed from {@code additionalInfo}. */
	protected final SortedMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> attributes;

	/** The cached string representation of {@link #attributes}. */
	protected final String attributesString;

	/** The cached string representation. */
	protected final String stringValue;

	/**
	 * Creates a new instance by parsing a {@code ProtocolInfo} string.
	 *
	 * @param protocolInfoString the {@link String} to parse.
	 * @throws ParseException If {@code protocolInfoString} can't be parsed.
	 */
	public ProtocolInfo(String protocolInfoString) throws ParseException {
		String tmpNetwork = WILDCARD;
		MimeType tmpMimeType = MimeType.ANYANY;
		String tmpAdditionalInfo = WILDCARD;

		if (isBlank(protocolInfoString)) {
			protocol = Protocol.ALL;
		} else {
			protocolInfoString = protocolInfoString.trim();
			String[] elements = protocolInfoString.split("\\s*:\\s*");
			protocol = Protocol.value(elements[0]);
			if (elements.length > 1) {
				tmpNetwork = elements[1];
			}
			if (elements.length > 2) {
				tmpMimeType = createMimeType(elements[2]);
			}
			if (elements.length > 3) {
				tmpAdditionalInfo = elements[3];
			}
			if (elements.length > 4) {
				throw new ParseException("Invalid protocolInfo string \"" + protocolInfoString + "\"");
			}
		}

		network = tmpNetwork;
		mimeType = tmpMimeType;
		additionalInfo = tmpAdditionalInfo;
		attributes = Collections.unmodifiableSortedMap(parseAdditionalInfo());
		attributesString = generateAttributesString();
		stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance using the provided information.
	 *
	 * @param protocol the {@link Protocol} for the new instance. Use
	 *            {@code null} for "any".
	 * @param network the network for the new instance. Use {@code null} or
	 *            blank for "any".
	 * @param contentFormat the content format for the new instance. Use
	 *            {@code null} or blank for "any".
	 * @param additionalInfo the additional information for the new instance.
	 */
	public ProtocolInfo(Protocol protocol, String network, String contentFormat, String additionalInfo) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = isBlank(network) ? WILDCARD : network;
		this.mimeType = createMimeType(contentFormat);
		this.additionalInfo = isBlank(additionalInfo) ? WILDCARD : additionalInfo;
		this.attributes = Collections.unmodifiableSortedMap(parseAdditionalInfo());
		this.attributesString = generateAttributesString();
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance using the provided information.
	 *
	 * @param protocol the {@link Protocol} for the new instance. Use
	 *            {@code null} for "any".
	 * @param network the network for the new instance. Use {@code null} or
	 *            blank for "any".
	 * @param mimeType the mime-type for the new instance. Use {@code null} or
	 *            {@link MimeType#ANYANY} for "any".
	 * @param additionalInfo the additional information for the new instance.
	 */
	public ProtocolInfo(Protocol protocol, String network, MimeType mimeType, String additionalInfo) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = isBlank(network) ? WILDCARD : network;
		this.mimeType = mimeType == null ? MimeType.ANYANY : mimeType;
		this.additionalInfo = isBlank(additionalInfo) ? WILDCARD : additionalInfo;
		this.attributes = Collections.unmodifiableSortedMap(parseAdditionalInfo());
		this.attributesString = generateAttributesString();
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance using the provided information.
	 *
	 * @param protocol the {@link Protocol} for the new instance. Use
	 *            {@code null} for "any".
	 * @param network the network for the new instance. Use {@code null} or
	 *            blank for "any".
	 * @param contentFormat the content format for the new instance. Use
	 *            {@code null} or blank for "any".
	 * @param attributes a {@link Map} of {@link ProtocolInfoAttributeName} and
	 *            {@link ProtocolInfoAttribute} pairs for the new instance.
	 */
	public ProtocolInfo(
		Protocol protocol,
		String network,
		String contentFormat,
		Map<ProtocolInfoAttributeName, ProtocolInfoAttribute> attributes
	) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = isBlank(network) ? WILDCARD : network;
		this.mimeType = createMimeType(contentFormat);
		TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> tmpAttributes = createEmptyAttributesMap();
		tmpAttributes.putAll(attributes);
		this.attributes = Collections.unmodifiableSortedMap(tmpAttributes);
		this.attributesString = generateAttributesString();
		this.additionalInfo = this.attributesString;
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance using the provided information.
	 *
	 * @param protocol the {@link Protocol} for the new instance. Use
	 *            {@code null} for "any".
	 * @param network the network for the new instance. Use {@code null} or
	 *            blank for "any".
	 * @param mimeType the mime-type for the new instance. Use {@code null} or
	 *            {@link MimeType#ANYANY} for "any".
	 * @param attributes a {@link Map} of {@link ProtocolInfoAttributeName} and
	 *            {@link ProtocolInfoAttribute} pairs for the new instance.
	 */
	public ProtocolInfo(
		Protocol protocol,
		String network,
		MimeType mimeType,
		Map<ProtocolInfoAttributeName, ProtocolInfoAttribute> attributes
	) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = isBlank(network) ? WILDCARD : network;
		this.mimeType = mimeType == null ? MimeType.ANYANY : mimeType;
		TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> tmpAttributes = createEmptyAttributesMap();
		tmpAttributes.putAll(attributes);
		this.attributes = Collections.unmodifiableSortedMap(tmpAttributes);
		this.attributesString = generateAttributesString();
		this.additionalInfo = this.attributesString;
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance using the provided information.
	 *
	 * @param protocol the {@link Protocol} for the new instance. Use
	 *            {@code null} for "any".
	 * @param network the network for the new instance. Use {@code null} or
	 *            blank for "any".
	 * @param contentFormat the content format for the new instance. Use
	 *            {@code null} or blank for "any".
	 * @param attributes an {@link EnumMap} with {@link DLNAAttribute}s the new
	 *            instance.
	 */
	public ProtocolInfo(
		Protocol protocol,
		String network,
		String contentFormat,
		EnumMap<DLNAAttribute.Type, DLNAAttribute<?>> attributes
	) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = isBlank(network) ? WILDCARD : network;
		this.mimeType = createMimeType(contentFormat);
		this.attributes = Collections.unmodifiableSortedMap(dlnaAttributesToAttributes(attributes));
		this.attributesString = generateAttributesString();
		this.additionalInfo = this.attributesString;
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance using the provided information.
	 *
	 * @param protocol the {@link Protocol} for the new instance. Use
	 *            {@code null} for "any".
	 * @param network the network for the new instance. Use {@code null} or
	 *            blank for "any".
	 * @param mimeType the mime-type for the new instance. Use {@code null} or
	 *            {@link MimeType#ANYANY} for "any".
	 * @param attributes an {@link EnumMap} with {@link DLNAAttribute}s for the
	 *            new instance.
	 */
	public ProtocolInfo(
		Protocol protocol,
		String network,
		MimeType mimeType,
		EnumMap<DLNAAttribute.Type, DLNAAttribute<?>> attributes
	) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = isBlank(network) ? WILDCARD : network;
		this.mimeType = mimeType == null ? MimeType.ANYANY : mimeType;
		this.attributes = Collections.unmodifiableSortedMap(dlnaAttributesToAttributes(attributes));
		this.attributesString = generateAttributesString();
		this.additionalInfo = this.attributesString;
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance based on a {@link DLNAProfiles} profile.
	 *
	 * @param protocol the {@link Protocol} for the new instance.
	 * @param profile the {@link DLNAProfiles} profile for the new instance.
	 */
	public ProtocolInfo(Protocol protocol, DLNAProfiles profile) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = WILDCARD;
		this.mimeType = createMimeType(profile.getContentFormat());
		SortedMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> tmpAttributes = createEmptyAttributesMap();
		DLNAOrgProfileName profileName = DLNAOrgProfileName.FACTORY.createProfileName(profile.getCode());
		tmpAttributes.put(profileName.getName(), profileName);
		this.attributes = Collections.unmodifiableSortedMap(tmpAttributes);
		this.attributesString = generateAttributesString();
		this.additionalInfo = this.attributesString;
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance based on a {@link DLNAProfiles} profile and
	 * additional {@link DLNAAttribute}s.
	 *
	 * @param protocol the {@link Protocol} for the new instance.
	 * @param profile the {@link DLNAProfiles} profile for the new instance.
	 * @param dlnaAttributes an {@link EnumMap} with {@link DLNAAttribute}s for
	 *            the new instance.
	 */
	public ProtocolInfo(
		Protocol protocol,
		DLNAProfiles profile,
		EnumMap<DLNAAttribute.Type, DLNAAttribute<?>> dlnaAttributes
	) {
		this.protocol = protocol == null ? Protocol.ALL : protocol;
		this.network = WILDCARD;
		this.mimeType = createMimeType(profile.getContentFormat());
		TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> tmpAttributes =
			dlnaAttributesToAttributes(dlnaAttributes);
		DLNAOrgProfileName profileName = DLNAOrgProfileName.FACTORY.createProfileName(profile.getCode());
		tmpAttributes.put(profileName.getName(), profileName);
		this.attributes = Collections.unmodifiableSortedMap(tmpAttributes);
		this.attributesString = generateAttributesString();
		this.additionalInfo = this.attributesString;
		this.stringValue = generateStringValue();
	}

	/**
	 * Creates a new instance from a {@link org.fourthline.cling.support.model.ProtocolInfo} instance.
	 *
	 * @param template the {@link org.fourthline.cling.support.model.ProtocolInfo} instance.
	 */
	public ProtocolInfo(org.fourthline.cling.support.model.ProtocolInfo template) {
		this(template.getProtocol(),
			template.getNetwork(),
			template.getContentFormat(),
			template.getAdditionalInfo()
		);
	}

	/**
	 * @return The {@code DLNA.ORG_PN} (DLNA media format profile) for this
	 *         {@link ProtocolInfo} or {@code null} if it isn't defined.
	 */
	public DLNAOrgProfileName getDLNAProfileName() {
		ProtocolInfoAttribute pnAttribute =
			attributes.get(KnownProtocolInfoAttributeName.DLNA_ORG_PN);
		return pnAttribute instanceof DLNAOrgProfileName ?
			(DLNAOrgProfileName) pnAttribute :
			null;
	}

	/**
	 * @return The {@code DLNA.ORG_OP} of this {@link ProtocolInfo} or
	 *         {@code null} if it isn't defined.
	 */
	public DLNAOrgOperations getDLNAOperations() {
		ProtocolInfoAttribute operationsAttribute =
			attributes.get(KnownProtocolInfoAttributeName.DLNA_ORG_OP);
		return operationsAttribute instanceof DLNAOrgOperations ?
			(DLNAOrgOperations) operationsAttribute :
			null;
	}

	/**
	 * @return The {@code DLNA.ORG_PS} of this {@link ProtocolInfo} or
	 *         {@code null} if it isn't defined.
	 */
	public DLNAOrgPlaySpeeds getDLNAPlaySpeeds() {
		ProtocolInfoAttribute playSpeedsAttribute =
			attributes.get(KnownProtocolInfoAttributeName.DLNA_ORG_PS);
		return playSpeedsAttribute instanceof DLNAOrgPlaySpeeds ?
			(DLNAOrgPlaySpeeds) playSpeedsAttribute :
			null;
	}

	/**
	 * @return The {@code DLNA.ORG_CI} of this {@link ProtocolInfo} or
	 *         {@code null} if it isn't defined.
	 */
	public DLNAOrgConversionIndicator getDLNAConversionIndicator() {
		ProtocolInfoAttribute conversionIndicatorAttribute =
			attributes.get(KnownProtocolInfoAttributeName.DLNA_ORG_CI);
		return conversionIndicatorAttribute instanceof DLNAOrgConversionIndicator ?
			(DLNAOrgConversionIndicator) conversionIndicatorAttribute :
			null;
	}

	/**
	 * @return The {@code DLNA.ORG_FLAGS} of this {@link ProtocolInfo} or
	 *         {@code null} if it isn't defined.
	 */
	public DLNAOrgFlags getFlags() {
		ProtocolInfoAttribute flagsAttribute =
			attributes.get(KnownProtocolInfoAttributeName.DLNA_ORG_FLAGS);
		return flagsAttribute instanceof DLNAOrgFlags ?
			(DLNAOrgFlags) flagsAttribute :
			null;
	}

	/**
	 * Searches for a {@link ProfileName} (any attribute name ending with
	 * {@code "_PN"} among the attributes, and returns the first one found.
	 * There is supposed to be zero or one {@link ProfileName} for any given
	 * instance of {@link ProtocolInfo}. If none is found, {@code null} is
	 * returned.
	 * <p>
	 * <b>Note: This will return any {@link ProfileName}, not just
	 * {@link DLNAOrgProfileName}s</b>. If you're looking for a
	 * {@code DLNA.ORG_PN}, use {@link #getDLNAProfileName} instead.
	 *
	 * @return The {@code DLNA.ORG_PN} (DLNA media format profile) for this
	 *         {@link ProtocolInfo} or {@code null} if it isn't defined.
	 */
	public ProfileName getProfileName() {
		for (ProtocolInfoAttribute attribute : attributes.values()) {
			if (attribute instanceof ProfileName) {
				return (ProfileName) attribute;
			}
		}
		return null;
	}

	/**
	 * Gets the cached {@link String} generated from the attributes map. This
	 * should be identical to {@link #getAdditionalInfo()}.
	 *
	 * @return The {@link String} representation of {@link #attributes}.
	 */
	public String getAttributesString() {
		return attributesString;
	}

	/**
	 * For internal use only. Generates a {@link String} representation of the
	 * {@link ProtocolInfoAttribute}s in {@link #attributes}.
	 *
	 * @return The {@link String} representation.
	 */
	protected String generateAttributesString() {
		if (attributes == null || attributes.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (ProtocolInfoAttribute attribute : attributes.values()) {
			String attributeString = attribute.getAttributeString();
			if (isNotBlank(attributeString)) {
				if (sb.length() > 0) {
					sb.append(";");
				}
				sb.append(attributeString);
			}
		}
		return sb.toString();
	}

	/**
	 * Gets the {@link MimeType} created from the content format of this
	 * {@link ProtocolInfo}.
	 *
	 * @return The {@link MimeType}.
	 */
	public MimeType getMimeType() {
		return mimeType;
	}

	/**
	 * For internal use only, creates the {@link MimeType} that is stored in
	 * {@code this.mimeType}.
	 *
	 * @param contentFormat the {@code protocolInfo} {@code contentFormat} to
	 *            parse.
	 * @return A new {@link MimeType} instance.
	 */
	protected MimeType createMimeType(String contentFormat) {
		try {
			return MimeType.valueOf(contentFormat);
		} catch (ParseException e) {
			LOGGER.error("Error parsing MimeType from \"{}\": {}", contentFormat, e.getMessage());
			LOGGER.trace("", e);
		}
		return MimeType.ANYANY;
	}

	/**
	 * Creates a new {@link org.seamless.util.MimeType} from the
	 * {@link MimeType} of this {@link ProtocolInfo}. To get the
	 * {@link MimeType}, use {@link #getMimeType()} instead.
	 *
	 * @return The corresponding {@link org.seamless.util.MimeType}.
	 * @throws IllegalArgumentException if
	 *             {@link org.seamless.util.MimeType#valueOf()} can't parse this
	 *             {@link MimeType}.
	 * @see #getMimeType()
	 */
	public org.seamless.util.MimeType getSeamlessMimeType() throws IllegalArgumentException {
		return org.seamless.util.MimeType.valueOf(mimeType.toString());
	}

	/**
	 * Parses {@code additionalInfo}.
	 *
	 * @return The {@link SortedMap} of parsed {@link ProtocolInfoAttribute}s.
	 */
	protected SortedMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> parseAdditionalInfo() {
		if (isBlank(additionalInfo) || WILDCARD.equals(additionalInfo.trim())) {
			return EMPTYMAP;
		}
		TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> result = createEmptyAttributesMap();
		String[] attributeStrings = additionalInfo.trim().toUpperCase(Locale.ROOT).split("\\s*;\\s*");
		for (String attributeString : attributeStrings) {
			if (isBlank(attributeString)) {
				continue;
			}
			String[] attributeEntry = attributeString.split("\\s*=\\s*");
			if (attributeEntry.length == 2) {
				try {
					ProtocolInfoAttribute attribute = ProtocolInfoAttribute.FACTORY.createAttribute(
						attributeEntry[0],
						attributeEntry[1],
						protocol
					);

					if (attribute != null) {
						result.put(attribute.getName(), attribute);
					} else {
						LOGGER.debug("Failed to parse attribute \"{}\"", attributeString);
					}
				} catch (ParseException e) {
					LOGGER.debug("Failed to parse attribute \"{}\": {}", attributeString, e.getMessage());
					LOGGER.trace("", e);
				}
			} else {
				LOGGER.debug("Invalid ProtocolInfo attribute \"{}\"", attributeString);
			}
		}
		return result;
	}

	/**
	 * @return the {@link Protocol} of this {@link ProtocolInfo}.
	 */
	public Protocol getProtocol() {
		return protocol;
	}

	/**
	 * @return the {@code network} of this {@link ProtocolInfo}.
	 */
	public String getNetwork() {
		return network;
	}

	/**
	 * @return the {@code contentFormat} of this {@link ProtocolInfo}.
	 */
	public String getContentFormat() {
		return mimeType.toString();
	}

	/**
	 * @return the {@code additionalInfo} of this {@link ProtocolInfo}.
	 */
	public String getAdditionalInfo() {
		return additionalInfo;
	}


	/**
	 * The attributes are the content of {@code additionalInfo} in parsed form.
	 *
	 * @return the attributes of this {@link ProtocolInfo}.
	 */
	public SortedMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> getAttributes() {
		return attributes;
	}

	/**
	 * Returns a debug string representation of this {@link ProtocolInfo}.
	 *
	 * @return The debug {@link String} representation.
	 */
	public String toDebugString() {
		StringBuilder sb = new StringBuilder();

		sb	.append("Protocol: ").append(protocol)
			.append(", Network: ").append(network)
			.append(", ContentFormat/MimeType: ").append(mimeType);

		if (isNotBlank(additionalInfo)) {
			sb.append(", AdditionalInfo: ").append(additionalInfo);
		}

		if (!mimeType.toString().equals(mimeType.toStringWithoutParameters())) {
			sb.append(", Simple MimeType: ").append(mimeType.toStringWithoutParameters());
		}
		if (mimeType.isDRM()) {
			sb.append(", DRM");
		}
		if (!mimeType.getParameters().isEmpty()) {
			sb.append(", MimeType Parameters: ").append(mimeType.getParameters());
		}

		if (attributes != null && !attributes.isEmpty()) {
			sb.append(", Attributes: ").append(attributes);
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return stringValue;
	}

	/**
	 * For internal use only, generates the string representation of this
	 * {@link ProtocolInfo} for use for the cached {@link #toString()} value.
	 *
	 * @return The string representation.
	 */
	protected String generateStringValue() {
		StringBuilder sb = new StringBuilder();
		sb	.append(protocol == null ? WILDCARD : protocol).append(":")
			.append(isBlank(network) ? WILDCARD : network).append(":")
			.append(mimeType == null ? MimeType.ANYANY : mimeType).append(":")
			.append(isBlank(attributesString) ? WILDCARD : attributesString);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((additionalInfo == null) ? 0 : additionalInfo.hashCode());
		result = prime * result + ((attributesString == null) ? 0 : attributesString.hashCode());
		result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
		result = prime * result + ((network == null) ? 0 : network.hashCode());
		result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
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
		if (!(obj instanceof ProtocolInfo)) {
			return false;
		}
		ProtocolInfo other = (ProtocolInfo) obj;
		if (additionalInfo == null) {
			if (other.additionalInfo != null) {
				return false;
			}
		} else if (!additionalInfo.equals(other.additionalInfo)) {
			return false;
		}
		if (attributesString == null) {
			if (other.attributesString != null) {
				return false;
			}
		} else if (!attributesString.equals(other.attributesString)) {
			return false;
		}
		if (mimeType == null) {
			if (other.mimeType != null) {
				return false;
			}
		} else if (!mimeType.equals(other.mimeType)) {
			return false;
		}
		if (network == null) {
			if (other.network != null) {
				return false;
			}
		} else if (!network.equals(other.network)) {
			return false;
		}
		if (protocol != other.protocol) {
			return false;
		}
		return true;
	}

	/**
	 * Converts an {@link EnumMap} of
	 * {@link org.fourthline.cling.support.model.dlna.DLNAAttribute}s to a
	 * {@link TreeMap} of {@link ProtocolInfoAttribute}s.
	 *
	 * @param dlnaAttributes the {@link EnumMap} of
	 *            {@link org.fourthline.cling.support.model.dlna.DLNAAttribute}s
	 *            to convert.
	 * @return A {@link TreeMap} containing the converted
	 *         {@link ProtocolInfoAttribute}s.
	 */
	public static TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> dlnaAttributesToAttributes(
		EnumMap<DLNAAttribute.Type,
		DLNAAttribute<?>> dlnaAttributes
	) {
		TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> attributes = createEmptyAttributesMap();
		for (Entry<DLNAAttribute.Type, DLNAAttribute<?>> entry: dlnaAttributes.entrySet()) {
			try {
				ProtocolInfoAttribute attribute = ProtocolInfoAttribute.FACTORY.createAttribute(
					entry.getKey().getAttributeName(),
					entry.getValue().getString(),
					Protocol.HTTP_GET
				);
				if (attribute != null) {
					attributes.put(attribute.getName(), attribute);
				}
			} catch (ParseException e) {
				LOGGER.debug(
					"Couldn't parse DLNAAttribute \"{}\" = \"{}\": {}",
					entry.getKey().getAttributeName(),
					entry.getValue().getString(), e.getMessage()
				);
				LOGGER.trace("", e);
			}
		}
		return attributes;
	}

	/**
	 * A convenience method to create an empty {@link TreeMap} of
	 * {@link ProtocolInfoAttribute}s with the correct {@link Comparator}
	 * {@link AttributeComparator}.
	 *
	 * @return The empty attributes map.
	 */
	public static TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> createEmptyAttributesMap() {
		return new TreeMap<>(new AttributeComparator());
	}

	/**
	 * DLNA requires {@code protocolInfo} attributes to appear in a certain
	 * order. Any {@link SortedMap} that is initialized with this
	 * {@link Comparator} will automatically sort its elements according to this
	 * custom order.
	 *
	 * It is vital that any {@link Map} used to store
	 * {@link ProtocolInfoAttribute}s in relation to {@link ProtocolInfo} use
	 * this class as it's {@link Comparator}.
	 *
	 * @author Nadahar
	 */
	public static class AttributeComparator implements Comparator<ProtocolInfoAttributeName>, Serializable {

		private static final long serialVersionUID = 1L;
		/** Defines the sort order for known attributes */
		public static final List<ProtocolInfoAttributeName> DEFINED_ORDER =
			Collections.unmodifiableList(Arrays.asList(new ProtocolInfoAttributeName[] {
			KnownProtocolInfoAttributeName.DLNA_ORG_PN,
			KnownProtocolInfoAttributeName.DLNA_ORG_OP,
			KnownProtocolInfoAttributeName.DLNA_ORG_PS,
			KnownProtocolInfoAttributeName.DLNA_ORG_CI,
			KnownProtocolInfoAttributeName.DLNA_ORG_FLAGS,
			KnownProtocolInfoAttributeName.ARIB_OR_JP_PN,
			KnownProtocolInfoAttributeName.DTV_MVP_PN,
			KnownProtocolInfoAttributeName.PANASONIC_COM_PN,
			KnownProtocolInfoAttributeName.MICROSOFT_COM_PN,
			KnownProtocolInfoAttributeName.SHARP_COM_PN,
			KnownProtocolInfoAttributeName.SONY_COM_PN
		}));

		@Override
		public int compare(ProtocolInfoAttributeName o1, ProtocolInfoAttributeName o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			int o1Index = DEFINED_ORDER.indexOf(o1);
			int o2Index = DEFINED_ORDER.indexOf(o2);

			// Sort by defined order if both arguments are defined
			if (o1Index >= 0 && o2Index >= 0) {
				return o1Index - o2Index;
			}

			// Sort by string value if none of the arguments are defined
			if (o1Index < 0 && o2Index < 0) {
				return o1.getName().compareTo(o2.getName());
			}

			// Sort defined arguments before undefined arguments
			if (o1Index < 0) {
				return 1;
			}
			if (o2Index < 0) {
				return -1;
			}

			// Sort alphabetically by name
			String o1Name = o1.getName();
			String o2Name = o2.getName();

			if (o1Name == null && o2Name == null) {
				return 0;
			}
			if (o1Name == null) {
				return 1;
			}
			if (o2Name == null) {
				return -1;
			}
			return o1Name.compareTo(o2Name);
		}
	}

	/**
	 * Compares {@link ProtocolInfo} instances for sorting. Sorting is done in
	 * this order: {@code protocol}, {@code network}, {@code contentFormat} and
	 * {@code additionalInfo}.
	 *
	 * @param other the {@link ProtocolInfo} instance to compare to.
	 */
	@Override
	public int compareTo(ProtocolInfo other) {

		if (other == null) {
			return -1;
		}

		// Protocol
		if (protocol == null && other.protocol != null) {
			return 1;
		}
		if (protocol != null && other.protocol == null) {
			return -1;
		}
		int result;
		if (protocol != null && other.protocol != null) {
			result = protocol.compareTo(other.protocol);
			if (result != 0) {
				return result;
			}
		}

		// Network
		if (network == null && other.network != null) {
			return 1;
		}
		if (network != null && other.network == null) {
			return -1;
		}
		if (network != null && other.network != null) {
			result = network.compareTo(other.network);
			if (result != 0) {
				return result;
			}
		}

		// ContentFormat/MimeType
		if (mimeType == null && other.mimeType != null) {
			return 1;
		}
		if (mimeType != null && other.mimeType == null) {
			return -1;
		}
		if (mimeType != null && other.mimeType != null) {
			result = mimeType.compareTo(other.mimeType);
			if (result != 0) {
				return result;
			}
		}

		// AdditionalInfo
		if (additionalInfo == null && other.additionalInfo != null) {
			return 1;
		}
		if (additionalInfo != null && other.additionalInfo == null) {
			return -1;
		}
		if (additionalInfo != null && other.additionalInfo != null) {
			return additionalInfo.compareTo(other.additionalInfo);
		}

		return 0;
	}
}
