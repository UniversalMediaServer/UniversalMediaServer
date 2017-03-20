/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shagrath
 * Derived from the mpeg4ip project and the ToNMT tool
 *
 * @deprecated now we are using {@link net.pms.dlna.LibMediaInfoParser} for extracting AVC stream info.
 */
@Deprecated
public class AVCHeader {
	private static final Logger LOGGER = LoggerFactory.getLogger(AVCHeader.class);

	private byte[] buffer;
	private int currentBit;
	private int profile;
	private int level;
	private int ref_frames;
	
	/**
	 *  Fix for ArrayOutOfBoundsException in getBit(); keeping an eye on the
	 *  buffer size. This will be set to true when the buffer is smaller than
	 *  expected.
	 *  See: http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=12933
	 */
	private boolean parseFailed = false;

	/**
	 * Constructor for the AVC header parser, allows to retrieve information
	 * from the header.
	 *
	 * @param buffer The byte buffer to parse.
	 */
	public AVCHeader(byte buffer[]) {
		this.buffer = buffer;
		currentBit = 0;
	}

	/**
	 * Parses the AVC header buffer passed to {@link #AVCHeader(byte[])}.
	 */
	public void parse() {
		profile = getValue(8);
		skipBit(8);
		level = getValue(8);
		if (profile == 100 || profile == 110 || profile == 122 || profile == -112) {
			if (getExpGolombCode() == 3) {
				getExpGolombCode();
			}
			getExpGolombCode();
			getExpGolombCode();
			getBit();
			if (getBit() == 1) {
				for (int i = 0; i < 8; i++) {
					int seqScalingListPresentFlag = getBit();
					if (seqScalingListPresentFlag == 1) {
						int lastScale = 8, nextScale = 8;
						int scalingListSize = i < 6 ? 16 : 64;
						for (int pos = 0; pos < scalingListSize; pos++) {
							if (nextScale != 0) {
								int deltaScale = getExpGolombCode();
								nextScale = (lastScale + deltaScale + 256) % 256;
							}
							lastScale = nextScale;
						}
					}
				}
			}
			getExpGolombCode();
			int picOrderCntType = getExpGolombCode();
			if (picOrderCntType == 0) {
				getExpGolombCode();
			} else if (picOrderCntType == 1) {
				getBit();
				getExpGolombCode();
				getExpGolombCode();
				int n = getExpGolombCode();
				for (int i = 0; i < n; i++) {
					getExpGolombCode();
				}
			}

			if (!parseFailed) {
				ref_frames = getExpGolombCode();
			} else {
				ref_frames = -1;
			}
		}
	}

	/**
	 * Returns the value of one particular bit in the header buffer and
	 * automatically increases the bit counter field to point at the next bit.
	 * <p>
	 * If the bit counter is too high and the buffer is too small to retrieve
	 * a proper bit value, <code>0</code> is returned.
	 *
	 * @return The bit value.
	 */
	private int getBit() {
		int pos = currentBit / 8;
		int modulo = currentBit % 8;
		currentBit++;
		
		if (buffer != null && pos < buffer.length) {
			return (buffer[pos] & (1 << (7 - modulo))) >> (7 - modulo);
		} else {
			if (!parseFailed) {
				LOGGER.debug("Cannot parse AVC header, buffer length is " + buffer.length);

				// Do not log consecutive errors.
				parseFailed = true;
			}

			return 0;
		}
	}

	/**
	 * Increase the bit counter field to skip a number of bits.
	 * 
	 * @param number The number of bits to skip.
	 */
	private void skipBit(int number) {
		currentBit += number;
	}

	/**
	 * Returns the integer value determined by a number of bits forming a
	 * binary string at the bit counter field position.
	 *
	 * @param length The number of bits that together form the binary value.
	 * @return The integer value.
	 */
	private int getValue(int length) {
		int total = 0;
		for (int i = 0; i < length; i++) {
			total += getBit() << (length - i - 1);
		}
		return total;
	}

	/**
	 * Returns the ExpGolombCode.
	 *
	 * @return the ExpGolombCode.
	 */
	private int getExpGolombCode() {
		int bits = 0;

		while (getBit() == 0 && !parseFailed) {
			bits++;
		}
		
		if (bits > 0 && !parseFailed) {
			return (1 << bits) - 1 + getValue(bits);
		} else {
			return 0;
		}
	}

	/**
	 * Returns the AVC profile parsed from the header.
	 *
	 * @return The profile
	 */
	public int getProfile() {
		return profile;
	}

	/**
	 * Returns the AVC compliancy level parsed from the header.
	 * 
	 * @return The level.
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Returns the AVC ref frames parsed from the header.
	 *
	 * @return The ref frames
	 */
	public int getRef_frames() {
		return ref_frames;
	}
}
