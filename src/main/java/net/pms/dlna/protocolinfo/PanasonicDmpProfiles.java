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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.model.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.protocolinfo.PanasonicComProfileName.KnownPanasonicComProfileName;
import net.pms.util.ParseException;

/**
 * This class handles a Panasonic DMP's {@link ProfileName}s from the custom
 * {@code X-PANASONIC-DMP-Profile} HTTP header field.
 * <p>
 * The {@code X-PANASONIC-DMP-Profile} doesn't provide {@code protocolInfo}
 * information, it simply lists a set of supported DLNA and non-DLNA format
 * profiles. This class still makes {@link ProtocolInfo} instances that
 * "represent" the listed profiles when this can be "reasonably deducted". This
 * allows the information given in {@code X-PANASONIC-DMP-Profile} to be
 * accessed as a "drop in replacement" for the information gotten from renderers
 * with {@code GetProtocolInfo}.
 * <p>
 * Any instance of {@link PanasonicDmpProfiles} must be "linked" with a
 * {@link DeviceProtocolInfo} instance that represents the same device. The
 * {@link ProtocolInfo} instances parsed or added is stored in this "linked"
 * instance.
 * <p>
 * This class is thread-safe.
 *
 * @author Nadahar
 */
public class PanasonicDmpProfiles implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(PanasonicDmpProfiles.class);

	/** The static singleton {@code X-PANASONIC-DMP-Profile} identifier */
	public static final DeviceProtocolInfoSource<PanasonicDmpProfiles> PANASONIC_DMP = new PanasonicDmpProfileType();

	/** The populated status */
	protected volatile boolean populated;

	/** The linked {@link DeviceProtocolInfo} */
	protected final DeviceProtocolInfo deviceProtocolInfo;

	/**
	 * Creates a new empty instance.
	 *
	 * @param deviceProtocolInfo the {@link DeviceProtocolInfo} instance to link
	 *            with the new instance.
	 */
	public PanasonicDmpProfiles(DeviceProtocolInfo deviceProtocolInfo) {
		if (deviceProtocolInfo == null) {
			throw new IllegalArgumentException("deviceProtocolInfo cannot be null");
		}
		this.deviceProtocolInfo = deviceProtocolInfo;
	}

	/**
	 * Creates a new instance, tries to parse {@code dmpProfileString} and adds
	 * the resulting {@link ProtocolInfo} instances to the {@link Set} of
	 * {@link ProtocolInfo} in {@code deviceProtocolInfo}.
	 *
	 * @param deviceProtocolInfo the {@link DeviceProtocolInfo} instance to link
	 *            with the new instance.
	 * @param dmpProfilesString a comma separated string of {@code protocolInfo}
	 *            representations.
	 */
	public PanasonicDmpProfiles(DeviceProtocolInfo deviceProtocolInfo, String dmpProfilesString) {
		if (deviceProtocolInfo == null) {
			throw new IllegalArgumentException("deviceProtocolInfo cannot be null");
		}
		this.deviceProtocolInfo = deviceProtocolInfo;
		add(dmpProfilesString);
	}

	/**
	 * @return {@code true} if the {@link Set} of {@link ProtocolInfo} in
	 *         {@code deviceProtocolInfo} has already been populated with
	 *         information from a {@code X-PANASONIC-DMP-Profile} field,
	 *         {@code false} otherwise.
	 */
	public boolean isPopulated() {
		return populated;
	}

	/**
	 * Tries to parse {@code dmpProfilesString} and adds the resulting
	 * {@link ProtocolInfo} instances to the {@link Set} of type
	 * {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}.
	 *
	 * @param dmpProfilesString a space separated string of format profile
	 *            representations whose presence is to be ensured.
	 * @return {@code true} if the {@link Set} of {@link ProtocolInfo} in
	 *         {@code deviceProtocolInfo} changed as a result of the call.
	 *         Returns {@code false} this already contains the specified
	 *         elements.
	 */
	public boolean add(String dmpProfilesString) {
		if (StringUtils.isBlank(dmpProfilesString)) {
			return false;
		}

		dmpProfilesString = dmpProfilesString.replaceFirst("\\s*X-PANASONIC-DMP-Profile:\\s*", "").trim();
		String[] elements = dmpProfilesString.trim().split("\\s+");

		SortedSet<ProtocolInfo> protocolInfoSet = new TreeSet<>();
		for (String element : elements) {
			try {
				ProtocolInfo protocolInfo = dmpProfileToProtocolInfo(element);
				if (protocolInfo != null) {
					protocolInfoSet.add(protocolInfo);
				}
			} catch (ParseException e) {
				LOGGER.warn(
					"Unable to parse protocolInfo from \"{}\", this profile will not be registered: {}",
					element,
					e.getMessage());
				LOGGER.trace("", e);
			}
		}
		boolean result = false;
		if (!protocolInfoSet.isEmpty()) {
			result = deviceProtocolInfo.addAll(PANASONIC_DMP, protocolInfoSet);
		}

		populated |= result;
		return result;
	}

	// Standard java.util.Collection methods


	/**
	 * Returns the number of elements of type {@link PanasonicDmpProfileType} in
	 * {@code deviceProtocolInfo}. If this contains more than
	 * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
	 *
	 * @return The number of elements in the {@link Set} for
	 *         {@link PanasonicDmpProfiles}.
	 */
	public int size() {
		return deviceProtocolInfo.size(PANASONIC_DMP);
	}

	/**
	 * Checks if the {@link Set} of type {@link PanasonicDmpProfileType} in
	 * {@code deviceProtocolInfo} is empty.
	 *
	 * @return {@code true} if {@code deviceProtocolInfo} contains no elements
	 *         of type {@link PanasonicDmpProfileType}, {@code false} otherwise.
	 */
	public boolean isEmpty() {
		return deviceProtocolInfo.isEmpty(PANASONIC_DMP);
	}

	/**
	 * Returns {@code true} if the {@link Set} of type
	 * {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo} contains
	 * the specified element.
	 *
	 * @param protocolInfo the element whose presence is to be tested.
	 * @return {@code true} if the {@link Set} of type
	 *         {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}
	 *         contains the specified element, {@code false} otherwise.
	 */
	public boolean contains(ProtocolInfo protocolInfo) {
		return deviceProtocolInfo.contains(PANASONIC_DMP, protocolInfo);
	}

	/**
	 * Returns a sorted array containing all of the elements of the {@link Set}
	 * of type {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}.
	 * <p>
	 * The returned array will be "safe" in that no reference to it is
	 * maintained. (In other words, this method must allocate a new array). The
	 * caller is thus free to modify the returned array.
	 *
	 * @return An array containing the {@link ProtocolInfo} instances.
	 */
	public ProtocolInfo[] toArray() {
		return deviceProtocolInfo.toArray(PANASONIC_DMP);
	}


	/**
	 * Returns {@code true} if the {@link Set} of type
	 * {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo} contains
	 * all of the elements in the specified collection.
	 *
	 * @param collection a {@link Collection} to be checked for containment.
	 * @return {@code true} if the {@link Set} of type
	 *         {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}
	 *         collection contains all of the elements in {@code collection}.
	 *
	 * @see #contains(ProtocolInfo))
	 */
	public boolean containsAll(Collection<ProtocolInfo> collection) {
		return deviceProtocolInfo.containsAll(PANASONIC_DMP, collection);
	}


	/**
	 * Removes all elements from the {@link Set} of type
	 * {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}.
	 */
	public void clear() {
		deviceProtocolInfo.clear(PANASONIC_DMP);
		populated = false;
	}

	/**
	 * Ensures that the the {@link Set} of type {@link PanasonicDmpProfileType}
	 * in {@code deviceProtocolInfo} contains the specified element. Returns
	 * {@code true} if the {@link Set} changed as a result of the call. Returns
	 * {@code false} the {@link Set} already contains the specified element.
	 *
	 * @param protocolInfo element whose presence is to be ensured.
	 * @return {@code true} if the the {@link Set} of type
	 *         {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}
	 *         changed as a result of the call, {@code false} otherwise.
	 */
	public boolean add(ProtocolInfo protocolInfo) {
		if (deviceProtocolInfo.add(PANASONIC_DMP, protocolInfo)) {
			populated = true;
			return true;
		}
		return false;
	}

	/**
	 * Adds all of the elements in the specified collection to the {@link Set}
	 * of type {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}.
	 *
	 * @param collection a {@link Collection} containing the elements to be
	 *            added.
	 * @return {@code true} if the {@link Set} of type
	 *         {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}
	 *         changed as a result of the call, {@code false} otherwise.
	 *
	 * @see #add(ProtocolInfo)
	 */
	public boolean addAll(Collection<? extends ProtocolInfo> collection) {
		if (deviceProtocolInfo.addAll(PANASONIC_DMP, collection)) {
			populated = true;
			return true;
		}
		return false;
	}

	/**
	 * Removes a single instance of {@link ProtocolInfo} from the {@link Set} of
	 * type {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo}, if it
	 * is present. Returns {@code true} if the {@link Set} contained the
	 * specified element (or equivalently, if the {@link Set} of type
	 * {@link PanasonicDmpProfileType} in {@code deviceProtocolInfo} changed as
	 * a result of the call).
	 *
	 * @param protocolInfo element to be removed, if present.
	 * @return {@code true} if an element was removed as a result of this call,
	 *         {@code false} otherwise.
	 */
	public boolean remove(ProtocolInfo protocolInfo) {
		return deviceProtocolInfo.remove(PANASONIC_DMP, protocolInfo);
	}

	/**
	 * Removes all elements that are also contained in {@code collection} from
	 * the {@link Set} of type {@link PanasonicDmpProfileType} in
	 * {@code deviceProtocolInfo}.
	 *
	 * @param collection a {@link Collection} containing the elements to be
	 *            removed.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 */
	public boolean removeAll(Collection<ProtocolInfo> collection) {
		if (deviceProtocolInfo.removeAll(PANASONIC_DMP, collection)) {
			if (deviceProtocolInfo.isEmpty(PANASONIC_DMP)) {
				populated = false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Retains only the elements that are contained in {@code collection} in the
	 * {@link Set} of type {@link PanasonicDmpProfileType} in
	 * {@code deviceProtocolInfo}. In other words, removes all elements that are
	 * not contained in {@code collection}.
	 *
	 * @param collection a {@link Collection} containing the elements to be
	 *            retained.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 */
	public boolean retainAll(Collection<ProtocolInfo> collection) {
		if (deviceProtocolInfo.removeAll(collection)) {
			if (deviceProtocolInfo.isEmpty()) {
				populated = false;
			}
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return deviceProtocolInfo.toString(PANASONIC_DMP, false);
	}

	/**
	 * Returns a string representation of the {@link Set} of type
	 * {@link PanasonicDmpProfileType} in the linked {@link DeviceProtocolInfo}
	 * instance. If {@code debug} is {@code true}, verbose output is returned.
	 *
	 * @param debug whether or not verbose output should be generated.
	 * @return The string representation.
	 */
	public String toString(boolean debug) {
		return deviceProtocolInfo.toString(PANASONIC_DMP, debug);
	}


	// Static methods


	/**
	 * Creates a {@link DeviPanasonicDmpProfiles} instance for {@code renderer}
	 * as needed. Parses {@code dmpProfilesString} and store the results in the
	 * linked {@link DeviceProtocolInfo} instance. This method assumes that the
	 * renderer sends the same string every time.
	 *
	 * @param dmpProfilesString the {@code X-PANASONIC-DMP-Profile} string to
	 *            parse.
	 * @param renderer the {@link RendererConfiguration} for which to apply the
	 *            parsing results.
	 * @throws IllegalStateException If {@code renderer}'s
	 *             {@code deviceProtocolInfo} is {@code null}.
	 */
	public static void parsePanasonicDmpProfiles(String dmpProfilesString, RendererConfiguration renderer) {
		if (renderer == null) {
			return;
		}
		if (renderer.deviceProtocolInfo == null) {
			throw new IllegalStateException(
				"Panasonic DMP profiles cannot be parsed before the renderer's deviceProtocolInfo is instantiated"
			);
		}
		boolean added = false;
		if (renderer.panasonicDmpProfiles == null) {
			renderer.panasonicDmpProfiles = new PanasonicDmpProfiles(renderer.deviceProtocolInfo, dmpProfilesString);
			added = true;
		} else if (!renderer.panasonicDmpProfiles.isPopulated()) {
			added = renderer.panasonicDmpProfiles.add(dmpProfilesString);
		}
		if (added && LOGGER.isTraceEnabled() && !renderer.panasonicDmpProfiles.isEmpty()) {
			LOGGER.trace(
				"Received X-PANASONIC-DMP-Profiles from \"{}\":\n\n{}",
				renderer.getConfName(),
				renderer.panasonicDmpProfiles
			);
		}
	}

	/**
	 * Tries to convert an element from a {@code X-PANASONIC-DMP-Profile} string
	 * to a {@link ProtocolInfo} instance. This is a manual mapping which must
	 * be extended when new profiles are discovered.
	 *
	 * @param dmpProfile the {@code X-PANASONIC-DMP-Profile} string element.
	 * @return A new {@link ProtocolInfo} instance if one could be created, or
	 *         {@code null}.
	 * @throws ParseException If there is a problem while parsing
	 *             {@code dmpProfile}.
	 */
	public static ProtocolInfo dmpProfileToProtocolInfo(String dmpProfile) throws ParseException {
		if (isBlank(dmpProfile)) {
			return null;
		}
		dmpProfile = dmpProfile.trim();
		ProtocolInfoAttribute attribute = DLNAOrgProfileName.FACTORY.getProfileName(dmpProfile);
		if (attribute != null) {
			// Mime-types must be mapped manually, as this information is missing from X-PANASONIC-DMP-Profile
			MimeType mimeType;
			if (attribute.getValue().startsWith("JPEG")) {
				mimeType = new MimeType("image", "jpeg");
			} else if (attribute.getValue().startsWith("PNG")) {
				mimeType = new MimeType("image", "png");
			} else if (attribute.getValue().startsWith("GIF")) {
				mimeType = new MimeType("image", "gif");
			} else if (attribute.getValue().startsWith("MPEG")) {
				mimeType = new MimeType("video", "mpeg");
			} else if (attribute.getValue().startsWith("AC3")) {
				mimeType = new MimeType("audio", "vnd.dolby.dd-raw");
			} else if (attribute.getValue().startsWith("AMR")) {
				mimeType = new MimeType("audio", "3gpp");
			} else if (attribute.getValue().startsWith("LPCM")) {
				mimeType = new MimeType("audio", "L16");
			} else if (attribute.getValue().startsWith("MP2")) {
				mimeType = new MimeType("audio", "mpeg");
			} else if (attribute.getValue().startsWith("MP3")) {
				mimeType = new MimeType("audio", "mpeg");
			} else if (attribute.getValue().startsWith("AAC")) {
				mimeType = new MimeType("audio", "mp4");
			} else if (attribute.getValue().startsWith("WMA")) {
				mimeType = new MimeType("audio", "x-ms-wma");
			} else if (attribute.getValue().startsWith("MPEG4")) {
				mimeType = new MimeType("video", "mp4");
			} else if (attribute.getValue().startsWith("MPEG")) {
				mimeType = new MimeType("video", "mpeg");
			} else if (attribute.getValue().startsWith("WMV")) {
				mimeType = new MimeType("video", "x-ms-wmv");
			} else if (attribute.getValue().startsWith("VC1")) {
				mimeType = new MimeType("video", "mpeg");
			} else {
				throw new ParseException("Can't infer mime-type for \"" + attribute + "\"");
			}
			return new ProtocolInfo(
				Protocol.HTTP_GET,
				ProtocolInfo.WILDCARD,
				mimeType,
				Collections.singletonMap(attribute.getName(), attribute)
			);
		}
		attribute = PanasonicComProfileName.FACTORY.getProfileName(dmpProfile);
		if (attribute != null) {
			// Mime-types must be mapped manually, as this information is missing from X-PANASONIC-DMP-Profile
			if (attribute instanceof KnownPanasonicComProfileName) {
				MimeType mimeType;
				switch ((KnownPanasonicComProfileName) attribute) {
					case MPO_3D:
						mimeType = new MimeType("image", "mpo");
						break;
					case PV_DIVX_DIV3:
					case PV_DIVX_DIV4:
					case PV_DIVX_DIVX:
					case PV_DIVX_DX50:
						mimeType = new MimeType("video", "divx");
						break;
					case PV_DRM_DIVX_DIV3:
					case PV_DRM_DIVX_DIV4:
					case PV_DRM_DIVX_DIVX:
					case PV_DRM_DIVX_DX50:
						// No idea what mime-type they use for DRM, or how to handle them
						return null;
					default:
						throw new ParseException("Unimplemented PANASONIC.COM_PN profile \"" + attribute + "\"");
				}
				return new ProtocolInfo(
					Protocol.HTTP_GET,
					ProtocolInfo.WILDCARD,
					mimeType,
					Collections.singletonMap(attribute.getName(), attribute)
				);
			}
			throw new ParseException("Can't infer mime-type for \"" + attribute + "\"");
		}

		// No match found for known DLNA.ORG_PN or PANASONIC.COM_PN profiles
		LOGGER.debug("Warning: Unable to parse X-PANASONIC-DMP-Profile \"{}\"", dmpProfile);
		return null;
	}

	/**
	 * This is an implementation of {@link DeviceProtocolInfoSource} where
	 * {@link PanasonicDmpProfiles} is the parsing class.
	 *
	 * @author Nadahar
	 */
	public static class PanasonicDmpProfileType extends DeviceProtocolInfoSource<PanasonicDmpProfiles> {

		private static final long serialVersionUID = 1L;

		/**
		 * Not to be instantiated, use
		 * {@link PanasonicDmpProfiles#PANASONIC_DMP} instead.
		 */
		protected PanasonicDmpProfileType() {
		}

		@Override
		public Class<PanasonicDmpProfiles> getClazz() {
			return PanasonicDmpProfiles.class;
		}

		@Override
		public String getType() {
			return "X-PANASONIC-DMP-Profile";
		}
	}

}
