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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fourthline.cling.support.model.dlna.DLNAProfiles;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;

/**
 * This interface represents {@code DLNA.ORG_PN} attributes. This can only be
 * used for DLNA content.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface DLNAOrgProfileName extends ProfileName {

	/** The static {@code NONE} instance representing a blank/empty value */
	DLNAOrgProfileName NONE = new DefaultDLNAOrgProfileName("");

	/**
	 * The static factory singleton instance used to create and retrieve
	 * {@link DLNAOrgProfileName} instances.
	 */
	DLNAOrgProfileNameFactory FACTORY = new DLNAOrgProfileNameFactory();

	/** The static attribute name always used for this class */
	ProtocolInfoAttributeName NAME = KnownProtocolInfoAttributeName.DLNA_ORG_PN;

	/**
	 * A factory for creating, caching and retrieving {@link DLNAOrgProfileName}
	 * instances.
	 */
	public static class DLNAOrgProfileNameFactory extends AbstractProfileNameFactory<DLNAOrgProfileName> {

		/** The static pre-compiled JPEG_RES_H_V pattern */
		protected static final Pattern JPEG_RES_PATTERN = Pattern.compile("^JPEG_RES_(\\d+)_(\\d+)$");

		/**
		 * For internal use only, use {@link DLNAOrgProfileName#FACTORY} to get
		 * the singleton instance.
		 */
		protected DLNAOrgProfileNameFactory() {
			// Add the profiles already defined by Cling
			for (DLNAProfiles profile : DLNAProfiles.values()) {
				if (profile != DLNAProfiles.NONE) {
					instanceCache.add(new DefaultDLNAOrgProfileName(profile.getCode()));
				}
			}
			// Add the static instances from DLNAImageProfile
			instanceCache.add(DLNAImageProfile.GIF_LRG);
			instanceCache.add(DLNAImageProfile.JPEG_LRG);
			instanceCache.add(DLNAImageProfile.JPEG_MED);
			instanceCache.add(DLNAImageProfile.JPEG_RES_H_V);
			instanceCache.add(DLNAImageProfile.JPEG_SM);
			instanceCache.add(DLNAImageProfile.JPEG_TN);
			instanceCache.add(DLNAImageProfile.PNG_LRG);
			instanceCache.add(DLNAImageProfile.PNG_TN);
		}

		@Override
		protected DLNAOrgProfileName getNoneInstance() {
			return NONE;
		}

		@Override
		protected DLNAOrgProfileName searchKnownInstances(String value) {
			// Check for known instances
			for (KnownDLNAOrgProfileName knownAttribute : KnownDLNAOrgProfileName.values()) {
				if (value.equals(knownAttribute.getValue())) {
					return knownAttribute;
				}
			}
			return null;
		}

		@Override
		protected DLNAOrgProfileName getNewInstance(String value) {
			Matcher matcher = JPEG_RES_PATTERN.matcher(value);
			if (matcher.find()) {
				DLNAImageProfile.createJPEG_RES_H_V(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
			}
			switch (value) {
				// Return existing static instances if applicable
				case DLNAImageProfile.GIF_LRG_STRING:
					return DLNAImageProfile.GIF_LRG;
				case DLNAImageProfile.JPEG_LRG_STRING:
					return DLNAImageProfile.JPEG_LRG;
				case DLNAImageProfile.JPEG_MED_STRING:
					return DLNAImageProfile.JPEG_MED;
				case DLNAImageProfile.JPEG_RES_H_V_STRING:
					return DLNAImageProfile.JPEG_RES_H_V;
				case DLNAImageProfile.JPEG_SM_STRING:
					return DLNAImageProfile.JPEG_SM;
				case DLNAImageProfile.JPEG_TN_STRING:
					return DLNAImageProfile.JPEG_TN;
				case DLNAImageProfile.PNG_LRG_STRING:
					return DLNAImageProfile.PNG_LRG;
				case DLNAImageProfile.PNG_TN_STRING:
					return DLNAImageProfile.PNG_TN;
				default:
					if (value.startsWith("JPEG_RES")) {
						matcher = Pattern.compile("^JPEG_RES_(\\d+)[X_](\\d+)").matcher(value);
						if (matcher.find()) {
							return DLNAImageProfile.createJPEG_RES_H_V(
								Integer.parseInt(matcher.group(1)),
								Integer.parseInt(matcher.group(2))
							);
						}
					}

					return new DefaultDLNAOrgProfileName(value);
			}
		}
	}

	/**
	 * This is the default, immutable class implementing
	 * {@link DLNAOrgProfileName}. {@link DLNAOrgProfileNameFactory} creates and
	 * caches instances of this class.
	 */
	public static class DefaultDLNAOrgProfileName extends AbstractDefaultProfileName implements DLNAOrgProfileName {

		private static final long serialVersionUID = 1L;

		/**
		 * For internal use only, use
		 * {@link DLNAOrgProfileNameFactory#createProfileName} to create new
		 * instances.
		 *
		 * @param value the profile name.
		 */
		protected DefaultDLNAOrgProfileName(String value) {
			super(value);
		}

		@Override
		public ProtocolInfoAttributeName getName() {
			return NAME;
		}
	}
}
