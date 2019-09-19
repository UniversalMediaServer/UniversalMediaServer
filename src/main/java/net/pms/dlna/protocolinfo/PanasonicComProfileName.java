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

import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;

/**
 * This interface represents {@code PANASONIC.COM_PN} attributes.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface PanasonicComProfileName extends ProfileName {

	/** The static {@code NONE} instance representing a blank/empty value */
	PanasonicComProfileName NONE = new DefaultPanasonicComProfileName("");

	/**
	 * The static factory singleton instance used to create and retrieve
	 * {@link PanasonicComProfileName} instances.
	 */
	PanasonicComProfileNameFactory FACTORY = new PanasonicComProfileNameFactory();

	/** The static attribute name always used for this class */
	ProtocolInfoAttributeName NAME = KnownProtocolInfoAttributeName.PANASONIC_COM_PN;

	/**
	 * A factory for creating, caching and retrieving {@link PanasonicComProfileName}
	 * instances.
	 */
	public static class PanasonicComProfileNameFactory extends AbstractProfileNameFactory<PanasonicComProfileName> {

		/**
		 * For internal use only, use {@link PanasonicComProfileName#FACTORY} to
		 * get the singleton instance.
		 */
		protected PanasonicComProfileNameFactory() {
		}

		@Override
		protected PanasonicComProfileName getNoneInstance() {
			return NONE;
		}

		@Override
		protected PanasonicComProfileName searchKnownInstances(String value) {
			// Check for known instances
			for (KnownPanasonicComProfileName knownAttribute : KnownPanasonicComProfileName.values()) {
				if (value.equals(knownAttribute.getValue())) {
					return knownAttribute;
				}
			}
			return null;
		}

		@Override
		protected PanasonicComProfileName getNewInstance(String value) {
			return new DefaultPanasonicComProfileName(value);
		}
	}

	/**
	 * This contains predefined {@code PANASONIC.COM_PN} values.
	 */
	public enum KnownPanasonicComProfileName implements PanasonicComProfileName {
		//XXX Add profiles

		/** Undefined image profile. */
		MPO_3D,

		/** Undefined video profile. */
		PV_DIVX_DIV3,

		/** Undefined video profile. */
		PV_DIVX_DIV4,

		/** Undefined video profile. */
		PV_DIVX_DIVX,

		/** Undefined video profile. */
		PV_DIVX_DX50,

		/** Undefined DRM video profile. */
		PV_DRM_DIVX_DIV3,

		/** Undefined DRM video profile. */
		PV_DRM_DIVX_DIV4,

		/** Undefined DRM video profile. */
		PV_DRM_DIVX_DIVX,

		/** Undefined DRM video profile. */
		PV_DRM_DIVX_DX50;

		@Override
		public String getValue() {
			return super.toString();
		}

		@Override
		public String toString() {
			return NAME + " = " + super.toString();
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
		public String getAttributeString() {
			return NAME + "=" + super.toString();
		}

	}

	/**
	 * This is the default, immutable class implementing
	 * {@link PanasonicComProfileName}. {@link PanasonicComProfileNameFactory}
	 * creates and caches instances of this class.
	 */
	public static class DefaultPanasonicComProfileName extends AbstractDefaultProfileName implements PanasonicComProfileName {

		private static final long serialVersionUID = 1L;

		/**
		 * For internal use only, use
		 * {@link PanasonicComProfileNameFactory#createProfileName} to create
		 * new instances.
		 *
		 * @param value the profile name.
		 */
		protected DefaultPanasonicComProfileName(String value) {
			super(value);
		}

		@Override
		public ProtocolInfoAttributeName getName() {
			return NAME;
		}
	}
}
