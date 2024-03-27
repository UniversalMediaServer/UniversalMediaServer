/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dlna;

/**
 * MM flags-param (flags parameter).
 *
 * Many DLNA binary-value flags that belong in the fourth field are encapsulated
 * in this parameter.
 *
 * The primary-flags token shall be exactly 8 hexadecimal digits, and it shall
 * represent a value composed of 32 binary bits. Each bit shall represent a
 * binary flag. The least significant bit corresponds to bit-0 and the most
 * significant bit corresponds to bit-31 (e.g. 10000000000000000000000000000000b
 * = 0x80000000 where bit-31 is the only bit set to true).
 *
 * @author Surf@ceS
 */
@SuppressWarnings({ "checkstyle:TypeName" })
public class ORG_FLAGS {

	private static final String CI_PARAM_DELIM = ";";
	private static final String CI_PARAM_TOKEN = "DLNA.ORG_FLAGS";
	private static final String RESERVED_DATA = "000000000000000000000000";
	private Integer flags = 0;

	@Override
	public String toString() {
		return CI_PARAM_TOKEN + "=" + String.format("%08X", flags) + RESERVED_DATA;
	}

	public String getParam() {
		return flags == 0 ? "" : CI_PARAM_DELIM + toString();
	}

	public boolean isSenderPaced() {
		return isBitSet(31);
	}

	/**
	 * Sender Paced flag.
	 *
	 * The Sender Paced flag indicates if the Content Source will act as the
	 * Clock Source, for the context of the protocolInfo.
	 *
	 * @param value {@code false} if the Content Source is not the Content Clock
	 * Source, {@code true} if the Content Source is the Content Clock Source
	 */
	public void setSenderPaced(boolean value) {
		setBit(31, value);
	}

	public boolean isLimitedTimeBasedSeek() {
		return isBitSet(30);
	}

	/**
	 * Limited Operations flag: Time-Based Seek.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an inferred
	 * value of false.
	 *
	 * The TimeBasedSeek flag indicates that the transport layer supports random
	 * access on a limited range of npt playback positions.
	 *
	 * @param value
	 */
	public void setLimitedTimeBasedSeek(boolean value) {
		setBit(30, value);
	}

	public boolean isLimitedByteBasedSeek() {
		return isBitSet(29);
	}

	/**
	 * Limited Operations flag: Byte-Based Seek.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an inferred
	 * value of false because this flag applies only to DMR devices and the DLNA
	 * PlayContainer URI operation is optional for DMR devices.
	 *
	 * The ByteBasedSeek flag indicate that the transport layer supports random
	 * access on a limited range of byte positions.
	 *
	 * @param value
	 */
	public void setLimitedByteBasedSeek(boolean value) {
		setBit(29, value);
	}

	public boolean isDLNAPlayContainer() {
		return isBitSet(28);
	}

	/**
	 * DLNA PlayContainer flag.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an inferred
	 * value of false because this flag applies only to DMR devices and the DLNA
	 * PlayContainer URI operation is optional for DMR devices.
	 *
	 * @param value
	 */
	public void setDLNAPlayContainer(boolean value) {
		setBit(28, value);
	}

	public boolean isBeginningDataRangeIncreasing() {
		return isBitSet(27);
	}

	/**
	 * Beginning Data Range Increasing flag.
	 *
	 * Uniform Client Data Availability Model s0 Increasing flag.
	 *
	 * If the dlna-v1.5-flag is false, then the s0-increasing flag shall have an
	 * inferred value of unknown.
	 *
	 * If false, the content does have a fixed beginning (i.e. npt=0 and
	 * byte-pos=0 map to the beginning).
	 *
	 * If true, then the content does not have a fixed beginning, and the ORG_OP
	 * param shall be omitted.
	 *
	 * @param value {@code false} if the beginning data boundary is fixed,
	 * {@code true} if the beginning data boundary increases with time.
	 */
	public void setBeginningDataRangeIncreasing(boolean value) {
		setBit(27, value);
	}

	public boolean isEndingDataRangeIncreasing() {
		return isBitSet(26);
	}

	/**
	 * Ending Data Range Increasing flag.
	 *
	 * Uniform Client Data Availability Model sN Increasing flag.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an inferred
	 * value of unknown because previous versions of the DLNA guidelines permit
	 * content that grows with time or has a fixed ending.
	 *
	 * @param value If {@code true}, then the content does not have a fixed
	 * ending. Otherwise, the content has a fixed ending.
	 */
	public void setEndingDataRangeIncreasing(boolean value) {
		setBit(26, value);
	}

	public boolean isRtspPauseSupport() {
		return isBitSet(25);
	}

	/**
	 * Pause media operation support for RTP Serving Endpoints flag.
	 *
	 * Applies only to RTP Media Transport.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an inferred
	 * value of false because previous versions of the DLNA guidelines do not
	 * support the RTP Media Transport.
	 *
	 * @param value
	 */
	public void setRtspPauseSupport(boolean value) {
		setBit(25, value);
	}

	public boolean isStreamingMode() {
		return isBitSet(24);
	}

	/**
	 * Streaming mode flag.
	 *
	 * AV and Audio Media Class content shall set at least the Streaming mode
	 * flag equal to true.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an inferred
	 * value of true only for Audio-only and AV content. For all other content,
	 * the inferred value is false.
	 *
	 * @param value
	 */
	public void setStreamingMode(boolean value) {
		setBit(24, value);
	}

	public boolean isInteractiveMode() {
		return isBitSet(23);
	}

	/**
	 * Interactive mode flag.
	 *
	 * Image Media Class content shall set at least the Interactive mode flag
	 * equal to true.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an inferred
	 * value of true only for Image content and media collection files. For all
	 * other content, the inferred value shall be false.
	 *
	 * @param value
	 */
	public void setInteractiveMode(boolean value) {
		setBit(23, value);
	}

	public boolean isBackgroundMode() {
		return isBitSet(22);
	}

	/**
	 * Background mode flag.
	 *
	 * If the dlna-v1.5-flag is false, then this flag shall have an unknown
	 * value.
	 *
	 * @param value
	 */
	public void setBackgroundMode(boolean value) {
		setBit(22, value);
	}

	public boolean isHttpConnectionStalling() {
		return isBitSet(21);
	}

	/**
	 * HTTP Connection Stalling flag.
	 *
	 * Applies only to the HTTP Media Transport.
	 *
	 * @param value
	 */
	public void setHttpConnectionStalling(boolean value) {
		setBit(21, value);
	}

	public boolean isDLNAv15() {
		return isBitSet(20);
	}

	/**
	 * DLNA v1.5 versioning flag.
	 *
	 * @param value
	 */
	public void setDLNAv15(boolean value) {
		setBit(20, value);
	}

	public boolean isLinkProtectedContent() {
		return isBitSet(16);
	}

	/**
	 * Link Protected content flag.
	 *
	 * If the Cleartext Byte Full Data Seek flag or the Cleartext Byte Full Data
	 * Seek flag are set then this flag shall be set to true.
	 *
	 * @param value
	 */
	public void setLinkProtectedContent(boolean value) {
		setBit(16, value);
	}

	public boolean isCleartextByteFullDataSeek() {
		return isBitSet(15);
	}

	/**
	 * Cleartext Byte Full Data Seek flag.
	 *
	 * If the content described by this protocolInfo does not use a Link
	 * Protection system (i.e. the LP-flag is false or omitted), the Cleartext
	 * Byte Full Data Seek flag shall be omitted or set to false.
	 *
	 * @param value
	 */
	public void setCleartextByteFullDataSeek(boolean value) {
		setBit(15, value);
	}

	public boolean isCleartextLimitedDataSeek() {
		return isBitSet(14);
	}

	/**
	 * Cleartext Limited Data Seek flag.
	 *
	 * If the content described by this protocolInfo does not use a Link
	 * Protection System (i.e. the LP-flag is false or omitted), the
	 * lop-cleartextbytes flag shall be omitted or set to false.
	 *
	 * If the dlna-v1.5 flag is false, then the Cleartext Limited Data Seek flag
	 * shall have a value of false.
	 *
	 * @param value
	 */
	public void setCleartextLimitedDataSeek(boolean value) {
		setBit(14, value);
	}

	private void setBit(int bit, boolean value) {
		flags = value ?
				flags | (1 << bit) :
				flags & ~(1 << bit);
	}

	private boolean isBitSet(int bit) {
		return ((flags & (1 << bit)) != 0);
	}

}
