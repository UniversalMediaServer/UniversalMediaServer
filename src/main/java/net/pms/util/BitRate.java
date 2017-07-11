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
package net.pms.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.io.Serializable;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a media bitrate. It's type can be one of the following:
 * <ul>
 * <li>Constant with a defined bitrate</li>
 * <li>Variable without a defined bitrate</li>
 * <li>Unknown with no defined values</li>
 * <li></li>
 * </ul>
 * The getters without arguments will return default values if the type is
 * unknown.
 *
 * @author Nadahar
 */
@SuppressWarnings("serial")
public class BitRate implements Serializable, Comparable<BitRate> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitRate.class);

	/** The default audio bitrate in bits per second */
	public static final int DEFAULT_AUDIO_VALUE = 8000;

	/** The default audio {@link BitRateMode} */
	public static final BitRateMode DEFAULT_AUDIO_MODE = BitRateMode.CONSTANT;

	/** The default audio {@link BitRate} */
	public static final BitRate DEFAULT_AUDIO = new BitRate(DEFAULT_AUDIO_MODE, DEFAULT_AUDIO_VALUE, null);

	/** The {@link BitRateMode} for this instance */
	protected BitRateMode mode;

	/** The bitrate integer value for this instance if the type is constant */
	protected int bitRate;

	/** The default {@link BitRate} to use if the actual value is unknown */
	protected BitRate defaultInstance;

	/**
	 * Creates a new unknown type.
	 *
	 * @param defaultInstance the default {@link BitRate} instance to use if the
	 *            actual value is unknown. Use {@code null} for no default or
	 *            {@link #DEFAULT_AUDIO} for audio media.
	 */
	public BitRate(BitRate defaultInstance) {
		this.defaultInstance = defaultInstance;
	}

	/**
	 * Creates a new instance.
	 *
	 * @param mode the {@link BitRateMode} for the new instance or {@code null}
	 *            if unknown.
	 * @param bitRate the bitrate integer value for the new instance if the type
	 *            is constant, or {@code 0} if unknown.
	 * @param defaultInstance the default {@link BitRate} instance to use if the
	 *            actual value is unknown. Use {@code null} for no default or
	 *            {@link #DEFAULT_AUDIO} for audio media.
	 */
	public BitRate(BitRateMode mode, int bitRate, BitRate defaultInstance) {
		this.mode = mode;
		this.bitRate = bitRate;
		this.defaultInstance = defaultInstance;
	}

	/**
	 * Creates a new instance by trying to parse the {@link String} arguments.
	 * If the parsing fails, the new instance will be of unknown type.
	 *
	 * @param mode the bitrate mode to parse.
	 * @param bitRate the bitrate value to parse if {@code mode} parses to
	 *            {@link BitRateMode#CONSTANT}. Can be anything if {@code mode}
	 *            parses to something else.
	 * @param defaultInstance the default {@link BitRate} instance to use if the
	 *            actual value is unknown. Use {@code null} for no default or
	 *            {@link #DEFAULT_AUDIO} for audio media.
	 */
	public BitRate(String mode, String bitRate, BitRate defaultInstance) {
		this.defaultInstance = defaultInstance;
		this.mode = BitRateMode.typeOf(mode);
		if (this.mode == BitRateMode.CONSTANT) {
			if (bitRate != null) {
				bitRate = bitRate.toLowerCase(Locale.ROOT).trim();
			}
			if (isBlank(bitRate) || "Unknown".equals(bitRate)) {
				this.bitRate = 0;
			} else {
				try {
					this.bitRate = Integer.parseInt(bitRate);
				} catch (NumberFormatException e) {
					LOGGER.warn("Unable to parse bitrate from \"{}\"", bitRate);
					this.bitRate = 0;
				}
			}
		}
	}

	/**
	 * Returns the bitrate mode, using the default bitrate mode if the actual
	 * value is unknown.
	 *
	 * @return The {@link BitRateMode} or the default bitrate mode.
	 */
	public BitRateMode getMode() {
		return getMode(true);
	}

	/**
	 * Returns the bitrate mode.
	 *
	 * @param returnDefault Whether or not to return the default bitrate mode if
	 *            this instance {@link #isUnknown()}.
	 * @return The {@link BitRateMode}, the default bitrate mode or {@code null}.
	 */
	public BitRateMode getMode(boolean returnDefault) {
		if (returnDefault && isUnknown() && defaultInstance != null) {
			return defaultInstance.getMode();
		}
		return mode;
	}

	/**
	 * Sets the bitrate mode. Use {@code null} for unknown.
	 *
	 * @param mode the {@link BitRateMode} to set.
	 */
	public void setMode(BitRateMode mode) {
		this.mode = mode;
	}

	/**
	 * Returns the bitrate, using the default bitrate if the actual value is
	 * unknown.
	 *
	 * @return The bitrate integer value or the default bitrate.
	 */
	public int getBitRateValue() {
		return getBitRateValue(true);
	}

	/**
	 * Returns the {@code "res@bitRate"} value which is actually
	 * <b>bytes/second</b> rounded upwards.
	 *
	 * @return The {@code "res@bitRate"} value for this {@link BitRate}.
	 */
	public int getDIDLBitRate() { //TODO: (Nad) Handle variable bitrate
		return (int) Math.ceil(getBitRateValue(true) / 8.0);
	}

	/**
	 * Returns the bitrate.
	 *
	 * @param returnDefault Whether or not to return the default bitrate if this
	 *            instance {@link #isUnknown()}.
	 * @return The bitrate integer value, the default bitrate or {@code 0}.
	 */
	public int getBitRateValue(boolean returnDefault) {
		if (mode == BitRateMode.VARIABLE) {
			throw new IllegalStateException("The bitrate value for variable bitrate is undefined");
		}
		if (returnDefault && isUnknown() && defaultInstance != null) {
			if (defaultInstance.isVariable() || defaultInstance.isUnknown()) {
				throw new IllegalStateException("The bitrate value for the default bitrate is undefined");
			}
			return defaultInstance.getBitRateValue();
		}
		return bitRate < 0 ? 0 : bitRate;
	}

	/**
	 * Sets the bitrate value. Use {@code 0} for unknown.
	 *
	 * @param bitRate the bitrate integer value to set.
	 */
	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}

	/**
	 * @return {@code true} if {@code mode} is {@code null} or type is constant
	 *         and {@code bitRate} is less than 1, {@code false} otherwise.
	 */
	public boolean isUnknown() {
		return mode == null || mode == BitRateMode.CONSTANT && bitRate < 1;
	}

	/**
	 * Returns whether this {@link BitRate} is variable or not, using the
	 * default if the actual value is unknown.
	 *
	 * @return {@code true} if the bitrate mode is {@link BitRateMode#VARIABLE},
	 *         false otherwise.
	 */
	public boolean isVariable() {
		return isVariable(true);
	}

	/**
	 * Returns whether this {@link BitRate} is variable or not.
	 *
	 * @param returnDefault Whether or not to evaluate the default if this
	 *            instance {@link #isUnknown()}.
	 * @return {@code true} if the bitrate mode is {@link BitRateMode#VARIABLE},
	 *         false otherwise.
	 */
	public boolean isVariable(boolean returnDefault) {
		if (isUnknown() && returnDefault && defaultInstance != null) {
			return defaultInstance.isVariable();
		}
		return mode == BitRateMode.VARIABLE;
	}

	/**
	 * Returns whether this {@link BitRate} is constant or not, using the
	 * default if the actual value is unknown.
	 *
	 * @return {@code true} if the bitrate mode is {@link BitRateMode#CONSTANT},
	 *         false otherwise.
	 */
	public boolean isConstant() {
		return isConstant(true);
	}

	/**
	 * Returns whether this {@link BitRate} is constant or not.
	 *
	 * @param returnDefault Whether or not to evaluate the default if this
	 *            instance {@link #isUnknown()}.
	 * @return {@code true} if the bitrate mode is {@link BitRateMode#CONSTANT},
	 *         false otherwise.
	 */
	public boolean isConstant(boolean returnDefault) {
		if (isUnknown() && returnDefault && defaultInstance != null) {
			return defaultInstance.isConstant();
		}
		return mode == BitRateMode.CONSTANT;
	}

	@Override
	public int compareTo(BitRate other) {
		if (other == null) {
			return 1;
		}
		if (mode != other.mode) {
			if (mode == null) {
				return -1;
			}
			if (other.mode == null) {
				return 1;
			}
			return mode.compareTo(other.mode);
		}
		return (bitRate < other.bitRate) ? -1 : ((bitRate == other.bitRate) ? 0 : 1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bitRate;
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
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
		if (!(obj instanceof BitRate)) {
			return false;
		}
		BitRate other = (BitRate) obj;
		if (bitRate != other.bitRate) {
			return false;
		}
		if (mode != other.mode) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		if (isUnknown()) {
			if (defaultInstance == null) {
				return "Unknown";
			}
			return "Unknown (" + defaultInstance + ")";
		}
		if (isVariable(false)) {
			return "Variable";
		}
		return "Constant " + StringUtil.formatBits(bitRate, false) + "/s";
	}
}
