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
import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;
import net.pms.util.ParseException;

/**
 * This class is immutable and represents the {@code DLNA.ORG_FLAGS} parameter.
 * This can be used for both DLNA and non-DLNA content.
 *
 * @author Nadahar
 */
public final class DLNAOrgConversionIndicator implements ProtocolInfoAttribute {

	private static final long serialVersionUID = 1L;

	/** The static attribute name always used for this class */
	public static final ProtocolInfoAttributeName NAME = KnownProtocolInfoAttributeName.DLNA_ORG_CI;

	/**
	 * The static factory singleton instance used to retrieve static
	 * {@link DLNAOrgConversionIndicator} instances.
	 */
	public static final DLNAOrgConversionIndicatorFactory FACTORY = new DLNAOrgConversionIndicatorFactory();

	/**
	 * The static {@code TRUE} instance representing converted/transcoded
	 * content with the value 1.
	 */
	public static final DLNAOrgConversionIndicator TRUE = new DLNAOrgConversionIndicator(true);

	/**
	 * The static {@code YES} instance representing converted/transcoded content
	 * with the value 1.
	 */
	public static final DLNAOrgConversionIndicator YES = TRUE;

	/**
	 * The static {@code FALSE} instance representing original/non-transcoded
	 * content with the value 0.
	 */
	public static final DLNAOrgConversionIndicator FALSE = new DLNAOrgConversionIndicator(false);

	/**
	 * The static {@code NO} instance representing original/non-transcoded
	 * content with the value 0.
	 */
	public static final DLNAOrgConversionIndicator NO = FALSE;

	/** The state */
	protected final boolean state;

	/**
	 * For internal use only, use one of the static instances instead.
	 *
	 * @see #TRUE
	 * @see #FALSE
	 * @see #YES
	 * @see #NO
	 *
	 * @param state the state.
	 */
	protected DLNAOrgConversionIndicator(boolean state) {
		this.state = state;
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
		return state ? "1" : "0";
	}

	/*
	 * XXX This currently returns blank for 0/false. While this isn't
	 * technically correct, it avoids a confusion with some old Panasonic Viera
	 * renderers which will pick any resource with CI=0 without considering the
	 * media type. Since the implied value also is CI=0, this shouldn't pose a
	 * problem.
	 */
	@Override
	public String getAttributeString() {
		return state ? NAME + "=1" : "";
	}

	/**
	 * A factory used to retrieve static {@link DLNAOrgConversionIndicator}
	 * instances.
	 */
	public static class DLNAOrgConversionIndicatorFactory {

		/**
		 * For internal use only, use {@link DLNAOrgConversionIndicator#FACTORY}
		 * instead.
		 */
		protected DLNAOrgConversionIndicatorFactory() {
		}

		/**
		 * Retrieves a static {@link DLNAOrgConversionIndicator} instance by
		 * parsing {@code value}. Valid values are {@code "0"} or {@code "1"}
		 * only. If {@code value} is {@code null} or blank,
		 * {@link DLNAOrgConversionIndicator#FALSE} is returned.
		 *
		 * @param value the {@code DLNA.ORG_CI} attribute value to parse.
		 * @return The corresponding {@link DLNAOrgConversionIndicator}
		 *         instance.
		 * @throws ParseException if {@code value} can't be parsed.
		 */
		public DLNAOrgConversionIndicator getConversionIndicator(String value) throws ParseException {
			if (isBlank(value)) {
				return FALSE;
			}

			value = value.trim();
			switch (value) {
				case "0":
					return FALSE;
				case "1":
					return TRUE;
				default:
					throw new ParseException("Cannot parse DLNA conversion indicator value \"" + value + "\"");
			}
		}

		/**
		 * Retrieves a static {@link DLNAOrgConversionIndicator} instance
		 * corresponding to the given argument.
		 *
		 * @param ci the {@code DLNA.ORG_CI} flag value.
		 * @return The corresponding {@link DLNAOrgConversionIndicator}
		 *         instance.
		 */
		public DLNAOrgConversionIndicator getConversionIndicator(boolean ci) {
			return ci ? TRUE : FALSE;
		}
	}

}
