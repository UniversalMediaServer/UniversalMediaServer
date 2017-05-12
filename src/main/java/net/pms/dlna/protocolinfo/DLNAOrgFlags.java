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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import net.pms.dlna.MediaType;
import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName.KnownProtocolInfoAttributeName;

/**
 * This class is immutable and represents the {@code DLNA.ORG_FLAGS} parameter.
 * This can only be used for DLNA content.
 *
 * @author Nadahar
 */
public class DLNAOrgFlags implements ProtocolInfoAttribute {

	private static final long serialVersionUID = 1L;

	/** The first/most significant bits/flags. */
	protected final long high;

	/** The last/least significant bits/flags. */
	protected final long low;

	/** The static name of this attribute. */
	public static final ProtocolInfoAttributeName NAME = KnownProtocolInfoAttributeName.DLNA_ORG_FLAGS;

	/**
	 * A {@link DLNAOrgFlags} instance that represents the implied flags where
	 * all flags are 0 and results in an empty parameter string.
	 */
	public static final DLNAOrgFlags IMPLIED = new DLNAOrgFlags(0, 0);

	/**
	 * Calculates the "effective" flags for a {@link DLNAOrgFlags} instance by
	 * applying DLNA conditional rules for some flags. {@code null} can be
	 * passed to get the flags that represent an omitted {@code DLNA.ORG_FLAGS}
	 * parameter.
	 *
	 * @param flags the {@link DLNAOrgFlags} instance to process.
	 * @param mediaType the {@link MediaType} of the media for which this flag
	 *            applies.
	 * @return A new {@link DLNAOrgFlags} instance where illegal combinations
	 *         have been corrected.
	 */
	public static DLNAOrgFlags getEffectiveFlags(DLNAOrgFlags flags, MediaType mediaType) {
		long high = flags == null ? 0 : flags.high;

		if (flags == null || !flags.isDLNA15()) {
			high &= ~(1L << 63);
			high &= ~(1L << 62);
			high &= ~(1L << 61);
			high &= ~(1L << 60);
			high &= ~(1L << 59);
			high &= ~(1L << 58);
			high &= ~(1L << 57);
			if (mediaType == MediaType.AUDIO || mediaType == MediaType.VIDEO) {
				high |= 1L << 56;
			} else {
				high &= ~(1L << 56);
			}
			// XHTML Print documents and media collection files too, when/if there is a MediaType for those
			if (mediaType == MediaType.IMAGE) {
				high |= 1L << 55;
			} else {
				high &= ~(1L << 55);
			}
			high &= ~(1L << 54);
			if (flags != null) {
				if (flags.isCleartextByteFullDataSeek()) {
					high |= 1L << 48;
				}
			}
			high &= ~(1L << 46);
		} else {
			if (flags.isCleartextByteFullDataSeek() || flags.isCleartextLimitedDataSeek()) {
				high |= 1L << 48;
			}
		}
		if (high == 0 && (flags == null || flags.low == 0)) {
			return IMPLIED;
		}
		return new DLNAOrgFlags(high, flags == null ? 0 : flags.low);
	}

	/**
	 * Creates a new {@link DLNAOrgFlagsBuilder} instance which can be used
	 * to {@link #build} an {@link DLNAOrgFlags} instance.
	 *
	 * @param isDLNA15 Sets bit 20: dlna-v1.5-flag (DLNA v1.5 versioning
	 *            flag).
	 * @return The {@link DLNAOrgFlagsBuilder} instance.
	 */
	public static DLNAOrgFlagsBuilder builder(boolean isDLNA15) {
		return new DLNAOrgFlagsBuilder(isDLNA15);
	}

	/**
	 * Creates a new {@link DLNAOrgFlags} instance based on a string of
	 * hexadecimal digits. The argument can be either only the "primary-flags"
	 * (the first 8 digits) or the full flag (all 32 digits). values.
	 *
	 * @param hexValue the {@link String} containing only characters
	 *            representing hexadecimal values ({@code 0-9, a-f, A-F}).
	 */
	public DLNAOrgFlags(String hexValue) {
		if (StringUtils.isBlank(hexValue)) {
			throw new IllegalArgumentException("hexValue cannot be empty");
		}
		try {
			if (hexValue.length() == 8) {
				high = Long.parseLong(hexValue, 16) << 32;
				low = 0;
			} else if (hexValue.length() == 32) {
				high = Long.parseLong(hexValue.substring(0, 8), 16) << 32 + Long.parseLong(hexValue.substring(8, 16), 16); //Unsigned
				low  = Long.parseLong(hexValue.substring(16, 24), 16) << 32 + Long.parseLong(hexValue.substring(24), 16); //Unsigned
			} else {
				throw new IllegalArgumentException("hexValue must be 8 or 32 digits long");
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("hexValue must be a valid hexadecimal number with 8 or 32 digits", e);
		}
	}

	/**
	 * Creates a new {@link DLNAOrgFlags} instance based on the given bit
	 * values.
	 *
	 * @param highValue the first 64 bits/flags.
	 * @param lowValue the last 64 bits/flags.
	 */
	public DLNAOrgFlags(long highValue, long lowValue) {
		this.high = highValue;
		this.low = lowValue;
	}

	/**
	 * Bit-31: sp-flag (Sender Paced flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isSenderPaced() {
		return ((high >> 63) & 1) == 1;
	}

	/**
	 * Bit-30: lop-npt (Limited Operations flag: Time-Based Seek).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isLimitedOperationsTimeBasedSeek() {
		return ((high >> 62) & 1) == 1;
	}

	/**
	 * Bit-29: lop-bytes (Limited Operations flag: Byte-Based Seek).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isLimitedOperationsByteBasedSeek() {
		return ((high >> 61) & 1) == 1;
	}

	/**
	 * Bit-28: playcontainer-param (DLNA PlayContainer flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isPlayContainer() {
		return ((high >> 60) & 1) == 1;
	}

	/**
	 * Bit 27: s0-increasing (UCDAM s0 Increasing flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isS0Increasing() {
		return ((high >> 59) & 1) == 1;
	}

	/**
	 * Bit 26: sN-increasing (UCDAM sN Increasing flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isSNIncreasing() {
		return ((high >> 58) & 1) == 1;
	}

	/**
	 * Bit-25: rtsp-pause (Pause media operation support for RTP Serving
	 * Endpoints).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isRtspPause() {
		return ((high >> 57) & 1) == 1;
	}

	/**
	 * Bit 24: tm-s (Streaming mode flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isStreamingMode() {
		return ((high >> 56) & 1) == 1;
	}

	/**
	 * Bit 23: tm-i (Interactive mode flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isInteractive() {
		return ((high >> 55) & 1) == 1;
	}

	/**
	 * Bit 22: tm-b (Background mode flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isBackgroundMode() {
		return ((high >> 54) & 1) == 1;
	}

	/**
	 * Bit 21: http-stalling (HTTP Connection Stalling flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isHttpConnectionStalling() {
		return ((high >> 53) & 1) == 1;
	}

	/**
	 * Bit 20: dlna-v1.5-flag (DLNA v1.5 versioning flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isDLNA15() {
		return ((high >> 52) & 1) == 1;
	}

	/**
	 * Bit 16: LP-flag (Link Protected content flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isLinkProtectedContent() {
		return ((high >> 48) & 1) == 1;
	}

	/**
	 * Bit 15: cleartextbyteseek-full flag (Cleartext Byte Full Data Seek flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isCleartextByteFullDataSeek() {
		return ((high >> 47) & 1) == 1;
	}

	/**
	 * Bit 14: lop-cleartextbytes flag (Cleartext Limited Data Seek flag).
	 *
	 * @return {@code true} if the flag is {@code 1}, {@code false} if the flag
	 *         is {@code 0}.
	 */
	public boolean isCleartextLimitedDataSeek() {
		return ((high >> 46) & 1) == 1;
	}

	/**
	 * Calculates the "effective" flags for this {@link DLNAOrgFlags} instance
	 * by applying DLNA conditional rules for some flags.
	 *
	 * @param mediaType the {@link MediaType} of the media for which this
	 *            instance applies.
	 * @return The new {@link DLNAOrgFlags} instance where illegal combinations
	 *         have been corrected.
	 */
	public DLNAOrgFlags getEffectiveFlags(MediaType mediaType) {
		return DLNAOrgFlags.getEffectiveFlags(this, mediaType);
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
		DLNAOrgFlags flags = DLNAOrgFlags.getEffectiveFlags(this, MediaType.UNKNOWN);
		return String.format("%016x", flags.high) + String.format("%016x", flags.low);
	}

	/**
	 * Returns a formatted {@code DLNA.ORG_FLAGS} attribute string for use in
	 * {@code protocolInfo}. If this {@link DLNAOrgFlags} instance has an empty
	 * value, or represents the implied default
	 * {@code DLNA.ORG_FLAGS=00000000000000000000000000000000}, an empty string
	 * is returned.
	 *
	 * @return The formatted {@code DLNA.ORG_FLAGS} attribute or an empty
	 *         {@link String}.
	 */
	@Override
	public String getAttributeString() {
		DLNAOrgFlags flags = DLNAOrgFlags.getEffectiveFlags(this, MediaType.UNKNOWN);
		if (flags.high == 0 || (flags.high & ~(3L << 55)) == 0) {
			return "";
		}
		return
			NAME + "=" +
			String.format("%016x", flags.high) + String.format("%016x", flags.low);
	}

	/**
	 * Gets the first 32 bits representing the {@code primary-flags}.
	 *
	 * @return The most significant {@link Integer}.
	 */
	public int getPrimaryValue() {
		return (int) (high >> 32);
	}

	/**
	 * Gets the first 64 bits of the 128 bits in {@code DLNA.ORG_FLAGS}.
	 *
	 * @return The most significant {@link Long}.
	 */
	public long getHighValue() {
		return high;
	}

	/**
	 * Gets the last 64 bits of the 128 bits in {@code DLNA.ORG_FLAGS}.
	 *
	 * @return The least significant {@link Long}.
	 */
	public long getLowValue() {
		return low;
	}

	@Override
	public String toString() {
		List<String> enabledFlags = new ArrayList<>();
		if (isSenderPaced()) {
			enabledFlags.add("Sender Paced");
		}
		if (isLimitedOperationsTimeBasedSeek()) {
			enabledFlags.add("Limited Operations: Time Based Seek");
		}
		if (isLimitedOperationsByteBasedSeek()) {
			enabledFlags.add("Limited Operations: Byte Based Seek");
		}
		if (isS0Increasing()) {
			enabledFlags.add("UCDAM s0 Increasing");
		}
		if (isSNIncreasing()) {
			enabledFlags.add("UCDAM sN Increasing");
		}
		if (isRtspPause()) {
			enabledFlags.add("RTSP Pause");
		}
		if (isStreamingMode()) {
			enabledFlags.add("Streaming Mode");
		}
		if (isInteractive()) {
			enabledFlags.add("Interactive");
		}
		if (isBackgroundMode()) {
			enabledFlags.add("Background Mode");
		}
		if (isHttpConnectionStalling()) {
			enabledFlags.add("HTTP Connection Stalling");
		}
		if (isDLNA15()) {
			enabledFlags.add("DLNA 1.5");
		}
		if (isLinkProtectedContent()) {
			enabledFlags.add("Link Protected Content");
		}
		if (isCleartextByteFullDataSeek()) {
			enabledFlags.add("Cleartext Byte Full Data Seek");
		}
		if (isCleartextLimitedDataSeek()) {
			enabledFlags.add("Cleartext Limited Data Seek");
		}
		return NAME + " = " + enabledFlags.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (high ^ (high >>> 32));
		result = prime * result + (int) (low ^ (low >>> 32));
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
		if (!(obj instanceof DLNAOrgFlags)) {
			return false;
		}
		DLNAOrgFlags other = (DLNAOrgFlags) obj;
		if (high != other.high) {
			return false;
		}
		if (low != other.low) {
			return false;
		}
		return true;
	}

	/**
	 * A builder class to build {@link DLNAOrgFlags} instances by setting
	 * individual flags.
	 */
	public static class DLNAOrgFlagsBuilder {

		/** The first/most significant bits/flags. */
		protected long high;

		/** The last/least significant bits/flags. */
		protected long low;

		/**
		 * Creates a new {@link DLNAOrgFlagsBuilder} instance which can be used
		 * to {@link #build} an {@link DLNAOrgFlags} instance.
		 *
		 * @param isDLNA15 Sets bit 20: dlna-v1.5-flag (DLNA v1.5 versioning
		 *            flag).
		 */
		public DLNAOrgFlagsBuilder(boolean isDLNA15) {
			high = (isDLNA15 ? 1L : 0L) << 52;
			low = 0;
		}

		/**
		 * Creates a {@link DLNAOrgFlags} instance from this {@link DLNAOrgFlagsBuilder}.
		 *
		 * @return The new {@link DLNAOrgFlags} instance.
		 */
		public DLNAOrgFlags build() {
			return high == 0 && low == 0 ? IMPLIED : new DLNAOrgFlags(high, low);
		}

		/**
		 * Sets bit-31: sp-flag (Sender Paced flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder senderPaced() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 63 : 0;
			return this;
		}

		/**
		 * Sets bit-30: lop-npt (Limited Operations flag: Time-Based Seek).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder limitedOperationsTimeBasedSeek() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 62 : 0;
			return this;
		}

		/**
		 * Sets bit-29: lop-bytes (Limited Operations flag: Byte-Based Seek).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder limitedOperationsByteBasedSeek() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 61 : 0;
			return this;
		}

		/**
		 * Sets bit-28: playcontainer-param (DLNA PlayContainer flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder playContainer() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 60 : 0;
			return this;
		}

		/**
		 * Sets bit 27: s0-increasing (UCDAM s0 Increasing flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder s0Increasing() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 59 : 0;
			return this;
		}

		/**
		 * Sets bit 26: sN-increasing (UCDAM sN Increasing flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder sNIncreasing() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 58 : 0;
			return this;
		}

		/**
		 * Sets bit-25: rtsp-pause (Pause media operation support for RTP
		 * Serving Endpoints).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder rtspPause() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 57 : 0;
			return this;
		}

		/**
		 * Sets bit 24: tm-s (Streaming mode flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder streamingMode() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 56 : 0;
			return this;
		}

		/**
		 * Sets bit 23: tm-i (Interactive mode flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder interactive() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 55 : 0;
			return this;
		}

		/**
		 * Sets bit 22: tm-b (Background mode flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder backgroundMode() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 54 : 0;
			return this;
		}

		/**
		 * Sets bit 21: http-stalling (HTTP Connection Stalling flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder httpConnectionStalling() {
			high |= 1L << 53;
			return this;
		}

		/**
		 * Sets bit 16: LP-flag (Link Protected content flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder linkProtectedContent() {
			high |= 1L << 48;
			return this;
		}

		/**
		 * Sets bit 15: cleartextbyteseek-full flag (Cleartext Byte Full Data
		 * Seek flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder cleartextByteFullDataSeek() {
			high |= 1L << 47;
			high |= 1L << 48;
			return this;
		}

		/**
		 * Sets bit 14: lop-cleartextbytes flag (Cleartext Limited Data Seek
		 * flag).
		 *
		 * @return The {@link DLNAOrgFlagsBuilder} instance.
		 */
		public DLNAOrgFlagsBuilder cleartextLimitedDataSeek() {
			high |= ((high >> 52) & 1) == 1 ? 1L << 46 : 0;
			if (((high >> 46) & 1) == 1) {
				high |= 1L << 48;
			}
			return this;
		}
	}
}
