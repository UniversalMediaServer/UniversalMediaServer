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

import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;

/**
 * This interface represents {@code ARIB.OR.JP_PN} attributes.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface AribOrJpProfileName extends ProfileName {

	/** The static {@code NONE} instance representing a blank/empty value */
	AribOrJpProfileName NONE = new DefaultAribOrJpProfileName("");

	/**
	 * The static factory singleton instance used to create and retrieve
	 * {@link AribOrJpProfileName} instances.
	 */
	AribOrJpProfileNameFactory FACTORY = new AribOrJpProfileNameFactory();

	/** The static attribute name always used for this class */
	ProtocolInfoAttributeName NAME = KnownProtocolInfoAttributeName.ARIB_OR_JP_PN;

	/**
	 * A factory for creating, caching and retrieving
	 * {@link AribOrJpProfileName} instances.
	 */
	public static class AribOrJpProfileNameFactory extends AbstractProfileNameFactory<AribOrJpProfileName> {

		/**
		 * For internal use only, use {@link AribOrJpProfileName#FACTORY} to get
		 * the singleton instance.
		 */
		protected AribOrJpProfileNameFactory() {
		}

		@Override
		protected AribOrJpProfileName getNoneInstance() {
			return NONE;
		}

		@Override
		protected AribOrJpProfileName searchKnownInstances(String value) {
			// Check for known instances
			for (KnownAribOrJpProfileName knownAttribute : KnownAribOrJpProfileName.values()) {
				if (value.equals(knownAttribute.getValue())) {
					return knownAttribute;
				}
			}
			return null;
		}

		@Override
		protected AribOrJpProfileName getNewInstance(String value) {
			return new DefaultAribOrJpProfileName(value);
		}
	}

	/**
	 * This contains predefined {@code ARIB.OR.JP_PN} values.
	 */
	public enum KnownAribOrJpProfileName implements AribOrJpProfileName {

		/**
		 * MPEG2-Video ({@code ISO/IEC 13818-2}) and MPEG2-AAC (
		 * {@code ISO/IEC 13818-7}) partial transport stream format with time
		 * stamp with mime-type
		 * {@code application/X-arib-cp;CONTENTFORMAT=<content-mimetype>}.
		 * <p>
		 * For full specification, see <a href=
		 * "http://www.arib.or.jp/english/html/overview/doc/6-STD-B21v4_6-E1.pdf"
		 * >ARIB STD – B21</a> chapter <b>9.2.4</b>.
		 */
		MPEG_TTS_CP;

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
	 * {@link AribOrJpProfileName}. {@link AribOrJpProfileNameFactory} creates
	 * and caches instances of this class.
	 */
	public static class DefaultAribOrJpProfileName extends AbstractDefaultProfileName implements AribOrJpProfileName {

		private static final long serialVersionUID = 1L;

		/**
		 * For internal use only, use
		 * {@link AribOrJpProfileNameFactory#createProfileName} to create new
		 * instances.
		 *
		 * @param value the profile name.
		 */
		protected DefaultAribOrJpProfileName(String value) {
			super(value);
		}

		@Override
		public ProtocolInfoAttributeName getName() {
			return NAME;
		}
	}
}
