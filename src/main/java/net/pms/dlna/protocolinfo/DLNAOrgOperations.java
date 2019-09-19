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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fourthline.cling.support.model.Protocol;
import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;
import net.pms.util.ParseException;

/**
 * This class represents {@code DLNA.ORG_OP} attributes. This can be used for
 * both DLNA and non-DLNA content.
 *
 * @author Nadahar
 */
public abstract class DLNAOrgOperations implements ProtocolInfoAttribute {

	private static final long serialVersionUID = 1L;

	/** The static attribute name always used for this class */
	public static final ProtocolInfoAttributeName NAME = KnownProtocolInfoAttributeName.DLNA_ORG_OP;

	/**
	 * The static factory singleton instance used to retrieve
	 * {@link DLNAOrgOperations} instances.
	 */
	public static final DLNAOrgOperationsFactory FACTORY = new DLNAOrgOperationsFactory();

	/** The state */
	protected final byte state;

	/**
	 * For internal use only, use {@link #FACTORY} to get instances.
	 *
	 * @param flagA flag A.
	 * @param flagB flag B.
	 */
	protected DLNAOrgOperations(boolean flagA, boolean flagB) {
		state = packFlags(flagA, flagB);
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
		return state > 0 ? ((state & 2) > 0 ? "1" : "0") + ((state & 1) > 0 ? "1" : "0") : "";
	}

	@Override
	public String getAttributeString() {
		String result = getValue();
		return isBlank(result) ? "" : NAME + "=" + result;
	}

	/**
	 * Validates if this {@link DLNAOrgOperations} instance can be used together
	 * with a given {@link DLNAOrgFlags} instance without breaking DLNA rules.
	 *
	 * @param flags the {@link DLNAOrgFlags} instance to validate against.
	 * @return {@code true} if validation succeeded or {@code flags} was
	 *         {@code null}, {@code false} otherwise.
	 */
	public boolean validate(DLNAOrgFlags flags) {
		// Assuming that "null" means no FLAGS parameter
		if (flags != null && state > 0) {
			if (flags.isS0Increasing() || flags.isLimitedOperationsTimeBasedSeek() || flags.isLimitedOperationsByteBasedSeek()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Validates if this {@link DLNAOrgOperations} instance can be used in a
	 * given {@link ProtocolInfo} instance without breaking DLNA rules.
	 * <p>
	 * <b>Note:</b> Currently {@code res@size} and {@code res@duration} isn't
	 * implemented in {@link ProtocolInfo}, which means that the related
	 * requirements can't be verified.
	 *
	 * @param protocolInfo the {@link ProtocolInfo} instance to verify for.
	 * @return {@code true} if the validation succeeded, false otherwise.
	 */
	public boolean validate(ProtocolInfo protocolInfo) {
		return validate(protocolInfo.getFlags());
		// XXX When implemented in ProtocolInfo, verify that Size is given if B
		// flag is true
		// XXX When implemented in ProtocolInfo, verify that Duration is given
		// if A flag is true
	}

	/**
	 * For internal use only, packs the flags into bits in a {@code byte}.
	 *
	 * @param flagA flag A.
	 * @param flagB flag B.
	 * @return A byte with the corresponding bits set.
	 */
	protected static byte packFlags(boolean flagA, boolean flagB) {
		return (byte) ((flagA ? 2 : 0) | (flagB ? 1 : 0));
	}

	/**
	 * A factory for retrieving static {@link DLNAOrgOperations} instances.
	 */
	public static class DLNAOrgOperationsFactory {

		/** Internal regex pattern for validation of strings for parsing */
		protected static final Pattern STRING_PATTERN = Pattern.compile("^\\s*([01])([01])\\s*$");

		/**
		 * For internal use only, use {@link DLNAOrgOperations#FACTORY} to get
		 * the singleton instance.
		 */
		protected DLNAOrgOperationsFactory() {
		}

		/**
		 * Retrieves the correct static {@link DLNAOrgOperationsHTTP} or
		 * {@link DLNAOrgOperationsRTP} instance by parsing a
		 * {@code DLNA.ORG_OP} string value. Only the form {@code "nn"} where
		 * {@code n} is either {@code 0} or {@code 1} is valid.
		 * <p>
		 * If {@code protocol} is anything but {@link Protocol#HTTP_GET} or
		 * {@link Protocol#RTSP_RTP_UDP}, {@code null} is returned. If
		 * {@code value} is {@code null} or blank, it is parsed as if it were
		 * {@code "00"}.
		 *
		 * @param protocol the {@link Protocol} for which this
		 *            {@link DLNAOrgOperations} instance applies.
		 * @param value the {@code DLNA.ORG_OP} string value.
		 * @return The corresponding static {@link DLNAOrgOperationsHTTP} or
		 *         {@link DLNAOrgOperationsRTP} instance, or {@code null} if
		 *         none could be found.
		 * @throws ParseException if {@code value} can't be parsed.
		 */
		public DLNAOrgOperations getOperations(Protocol protocol, String value) throws ParseException {
			if (protocol == null) {
				return null;
			}
			if (isBlank(value)) {
				return getOperations(protocol, false, false);
			}
			Matcher matcher = STRING_PATTERN.matcher(value);
			if (!matcher.find()) {
				throw new ParseException("Unable to parse \"" + value + "\" as a DLNA operations value");
			}

			return getOperations(protocol, "1".equals(matcher.group(1)), "1".equals(matcher.group(2)));
		}

		/**
		 * Retrieves the correct static {@link DLNAOrgOperationsHTTP} or
		 * {@link DLNAOrgOperationsRTP} instance based on {@code flags}.
		 * {@link DLNAOrgOperationsFlags#TIME_SEEK} is ignored for
		 * {@link Protocol#RTSP_RTP_UDP}.
		 * <p>
		 * If {@code protocol} is anything but {@link Protocol#HTTP_GET} or
		 * {@link Protocol#RTSP_RTP_UDP}, {@code null} is returned. If
		 * {@code flags} is {@code null}, it is parsed as if it were empty.
		 *
		 * @param protocol the {@link Protocol} for which this
		 *            {@link DLNAOrgOperations} instance applies.
		 * @param flags the {@link DLNAOrgOperationsFlags} to set.
		 * @return The corresponding static {@link DLNAOrgOperationsHTTP} or
		 *         {@link DLNAOrgOperationsRTP} instance, or {@code null} if
		 *         none could be found.
		 */
		public DLNAOrgOperations getOperations(Protocol protocol, DLNAOrgOperationsFlags... flags) {
			boolean flagA = false;
			boolean flagB = false;
			if (flags != null && protocol == Protocol.HTTP_GET) {
				for (DLNAOrgOperationsFlags flag : flags) {
					flagA = flag == DLNAOrgOperationsFlags.TIME_SEEK;
					flagB = flag == DLNAOrgOperationsFlags.HEADER;
				}
			} else if (flags != null && protocol == Protocol.RTSP_RTP_UDP) {
				for (DLNAOrgOperationsFlags flag : flags) {
					flagA = flag == DLNAOrgOperationsFlags.HEADER;
				}
			} else {
				return null;
			}
			return getOperations(protocol, flagA, flagB);
		}

		/**
		 * Retrieves the correct static {@link DLNAOrgOperationsHTTP} or
		 * {@link DLNAOrgOperationsRTP} instance based on {@code flagA} and
		 * {@code flagB}. {@code flagB} is ignored for
		 * {@link Protocol#RTSP_RTP_UDP}.
		 * <p>
		 * If {@code protocol} is anything but {@link Protocol#HTTP_GET} or
		 * {@link Protocol#RTSP_RTP_UDP}, {@code null} is returned.
		 *
		 * @param protocol the {@link Protocol} for which this
		 *            {@link DLNAOrgOperations} instance applies.
		 * @param flagA the first flag.
		 * @param flagB the second flag.
		 * @return The corresponding static {@link DLNAOrgOperationsHTTP} or
		 *         {@link DLNAOrgOperationsRTP} instance, or {@code null} if
		 *         none could be found.
		 */
		public DLNAOrgOperations getOperations(Protocol protocol, boolean flagA, boolean flagB) {
			if (protocol == Protocol.HTTP_GET) {
				switch (packFlags(flagA, flagB)) {
					case 1:
						return DLNAOrgOperationsHTTP.HTTP_HEADER;
					case 2:
						return DLNAOrgOperationsHTTP.HTTP_TIME_SEEK;
					case 3:
						return DLNAOrgOperationsHTTP.HTTP_BOTH;
					default:
						return DLNAOrgOperationsHTTP.NONE;
				}
			} else if (protocol == Protocol.RTSP_RTP_UDP) {
				switch (packFlags(flagA, flagB)) {
					case 2:
						return DLNAOrgOperationsRTP.RTP_HEADER;
					default:
						return DLNAOrgOperationsRTP.NONE;
				}
			}
			return null;
		}

	}

	/**
	 * This class represents {@code DLNA.ORG_OP} attributes for HTTP transports.
	 * This can be used for both DLNA and non-DLNA content.
	 *
	 * @author Nadahar
	 */
	public static class DLNAOrgOperationsHTTP extends DLNAOrgOperations {

		private static final long serialVersionUID = 1L;

		/** Neither flag is set, corresponds to {@code "00"} */
		public static final DLNAOrgOperations NONE = new DLNAOrgOperationsHTTP(false, false);

		/**
		 * {@code TimeSeekRange.dlna.org} HTTP header support is set, corresponds
		 * to {@code "10"}
		 */
		public static final DLNAOrgOperations HTTP_TIME_SEEK = new DLNAOrgOperationsHTTP(true, false);


		/** Range HTTP header support is set, corresponds to {@code "01"} */
		public static final DLNAOrgOperations HTTP_HEADER = new DLNAOrgOperationsHTTP(false, true);

		/**
		 * Both {@code TimeSeekRange.dlna.org} HTTP header and Range HTTP header
		 * support is set, corresponds to {@code "11"}
		 */
		public static final DLNAOrgOperations HTTP_BOTH = new DLNAOrgOperationsHTTP(true, true);

		/**
		 * For internal use only, use that static instances or
		 * {@link DLNAOrgOperations#FACTORY} to retrieve instances.
		 *
		 * @see #NONE
		 * @see #HTTP_TIME_SEEK
		 * @see #HTTP_HEADER
		 * @see #HTTP_BOTH
		 *
		 * @param timeSeekRange the {@code TimeSeekRange.dlna.org} HTTP header
		 *            flag.
		 * @param rangeHttpHeader the Range HTTP header flag.
		 */
		protected DLNAOrgOperationsHTTP(boolean timeSeekRange, boolean rangeHttpHeader) {
			super(timeSeekRange, rangeHttpHeader);
		}
	}

	/**
	 * This class represents {@code DLNA.ORG_OP} attributes for RTP transports.
	 * This can be used for both DLNA and non-DLNA content.
	 *
	 * @author Nadahar
	 */
	public static class DLNAOrgOperationsRTP extends DLNAOrgOperations {

		private static final long serialVersionUID = 1L;

		/** Neither flag is set, corresponds to {@code "00"} */
		public static final DLNAOrgOperations NONE = new DLNAOrgOperationsRTP(false);

		/** Range header support is set, corresponds to {@code "10"} */
		public static final DLNAOrgOperations RTP_HEADER = new DLNAOrgOperationsRTP(true);

		/**
		 * For internal use only, use that static instances or
		 * {@link DLNAOrgOperations#FACTORY} to retrieve instances.
		 *
		 * @see #NONE
		 * @see #RTP_HEADER
		 *
		 * @param rangeHeader the Range header flag.
		 */
		protected DLNAOrgOperationsRTP(boolean rangeHeader) {
			super(rangeHeader, false);
		}
	}

	/**
	 * This {@code enum} represents the individual {@code DLNA.ORG_OP} flags.
	 */
	public enum DLNAOrgOperationsFlags {
		/** {@code TimeSeekRange.dlna.org} HTTP header */
		TIME_SEEK,

		/** Range HTTP header or Range header depending on the {@link Protocol} */
		HEADER;
	}
}
