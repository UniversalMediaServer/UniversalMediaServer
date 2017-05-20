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

import org.fourthline.cling.support.model.dlna.DLNAProfiles;
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
			return new DefaultDLNAOrgProfileName(value);
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
