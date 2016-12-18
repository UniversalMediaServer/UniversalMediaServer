/*
 * Universal Media Server, for streaming any medias to DLNA
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
package net.pms.dlna;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.util.ImagesUtil.ImageFormat;


/**
 * Definition of the different DLNA media profiles for images. If more
 * are added, corresponding changes need to be made in
 * {@link ImageFormat#fromImageProfile}.
 *
 * Please not: Only the profile constant (e.g {@code JPEG_TN} is taken into
 * consideration in {@link #equals(Object)} and {@link #hashCode()}, metadata
 * like {@code H} and {@code V} aren't taken into account.
 */
public class DLNAImageProfile {

	public static final int GIF_LRG_INT = 1;
	public static final int JPEG_LRG_INT = 2;
	public static final int JPEG_MED_INT = 3;
	public static final int JPEG_RES_H_V_INT = 4;
	public static final int JPEG_SM_INT = 5;
	public static final int JPEG_TN_INT = 6;
	public static final int PNG_LRG_INT = 7;
	public static final int PNG_TN_INT = 8;

	public static final Integer GIF_LRG_INTEGER = GIF_LRG_INT;
	public static final Integer JPEG_LRG_INTEGER = JPEG_LRG_INT;
	public static final Integer JPEG_MED_INTEGER = JPEG_MED_INT;
	public static final Integer JPEG_RES_H_V_INTEGER = JPEG_RES_H_V_INT;
	public static final Integer JPEG_SM_INTEGER = JPEG_SM_INT;
	public static final Integer JPEG_TN_INTEGER = JPEG_TN_INT;
	public static final Integer PNG_LRG_INTEGER = PNG_LRG_INT;
	public static final Integer PNG_TN_INTEGER = PNG_TN_INT;

	/**
	 * {@code GIF_LRG} maximum resolution 1600 x 1200.
	 */
	public static final DLNAImageProfile GIF_LRG = new DLNAImageProfile(GIF_LRG_INT, "GIF_LRG");

	/**
	 * {@code JPEG_LRG} maximum resolution 4096 x 4096.
	 */
	public static final DLNAImageProfile JPEG_LRG = new DLNAImageProfile(JPEG_LRG_INT, "JPEG_LRG");

	/**
	 * {@code JPEG_MED} maximum resolution 1024 x 768.
	 */
	public static final DLNAImageProfile JPEG_MED = new DLNAImageProfile(JPEG_MED_INT, "JPEG_MED");

	/**
	 * {@code JPEG_RES_H_V} exact resolution H x V.<br>
	 * <br>
	 * <b>This constant creates an invalid profile, only for use with
	 * {@link #equals(Object)}.</b><br>
	 * <br>
	 * To create a valid profile, use {@link #getJPEG_RES_H_V(int, int)} instead.
	 */
	public static final DLNAImageProfile JPEG_RES_H_V = new DLNAImageProfile(JPEG_RES_H_V_INT, "JPEG_RES_H_V");

	/**
	 * {@code JPEG_SM} maximum resolution 640 x 480.
	 */
	public static final DLNAImageProfile JPEG_SM = new DLNAImageProfile(JPEG_SM_INT, "JPEG_SM");

	/**
	 * {@code JPEG_TN} maximum resolution 160 x 160.
	 */
	public static final DLNAImageProfile JPEG_TN = new DLNAImageProfile(JPEG_TN_INT, "JPEG_TN");

	/**
	 * {@code PNG_LRG} maximum resolution 4096 x 4096.
	 */
	public static final DLNAImageProfile PNG_LRG = new DLNAImageProfile(PNG_LRG_INT, "PNG_LRG");

	/**
	 * {@code PNG_TN} maximum resolution 160 x 160.
	 */
	public static final DLNAImageProfile PNG_TN = new DLNAImageProfile(PNG_TN_INT, "PNG_TN");

	/**
	 * {@code JPEG_RES_H_V} defines an exact resolution H x V. Set {@code H}
	 * and {@code V} for the {@code JPEG_RES_H_V} profile. Not applicable for
	 * other profiles.
	 *
	 * @param horizontal the {@code H} value.
	 * @param vertical the {@code V} value.
	 */
	public static DLNAImageProfile getJPEG_RES_H_V(int horizontal, int vertical) {
		return new DLNAImageProfile(
			JPEG_RES_H_V_INT,
			"JPEG_RES_" + Integer.toString(horizontal) + "_" + Integer.toString(vertical),
			horizontal,
			vertical
		);
	}

	public final int imageProfileInt;
	public final String imageProfileStr;
	private final int horizontal;
	private final int vertical;

	/**
	 * Instantiate a {@link DLNAImageProfile} object.
	 */
	private DLNAImageProfile(int imageProfileInt, String imageProfileStr) {
		this.imageProfileInt = imageProfileInt;
		this.imageProfileStr = imageProfileStr;
		this.horizontal = -1;
		this.vertical = -1;
	}

	/**
	 * Instantiate a {@link DLNAImageProfile#JPEG_RES_H_V} object.
	 */
	private DLNAImageProfile(int imageProfileInt, String imageProfileStr, int horizontal, int vertical) {
		this.imageProfileInt = imageProfileInt;
		this.imageProfileStr = imageProfileStr;
		this.horizontal = horizontal;
		this.vertical = vertical;
	}

	/**
	 * Returns the string representation of this {@link DLNAImageProfile}.
	 */
	@Override
	public String toString() {
		return imageProfileStr;
	}

	/**
	 * Returns the integer representation of this {@link DLNAImageProfile}.
	 */
	public int toInt() {
		return imageProfileInt;
	}

	/**
	 * Converts a {@link DLNAImageProfile} to an {@link Integer} object.
	 *
	 * @return This {@link DLNAImageProfile}'s {@link Integer} mapping.
	 */
	public Integer toInteger() {
		switch (imageProfileInt) {
			case GIF_LRG_INT:
				return GIF_LRG_INTEGER;
			case JPEG_LRG_INT:
				return JPEG_LRG_INTEGER;
			case JPEG_MED_INT:
				return JPEG_MED_INTEGER;
			case JPEG_RES_H_V_INT:
				return JPEG_RES_H_V_INTEGER;
			case JPEG_SM_INT:
				return JPEG_SM_INTEGER;
			case JPEG_TN_INT:
				return JPEG_TN_INTEGER;
			case PNG_LRG_INT:
				return PNG_LRG_INTEGER;
			case PNG_TN_INT:
				return PNG_TN_INTEGER;
			default:
				throw new IllegalStateException("DLNAImageProfile " + imageProfileStr + ", " + imageProfileInt + " is unknown.");
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link DLNAImageProfile}. If the conversion fails, this method returns
	 * {@code null}.
	 */
	public static DLNAImageProfile toDLNAImageProfile(String argument) {
		return toDLNAImageProfile(argument, null);
	}

	/**
	 * Converts the integer passed as argument to a {@link DLNAImageProfile}.
	 * If the conversion fails, this method returns {@code null}.
	 */
	public static DLNAImageProfile toDLNAImageProfile(int value) {
		return toDLNAImageProfile(value, null);
	}

	/**
	 * Converts the integer passed as argument to a {@link DLNAImageProfile}. If the
	 * conversion fails, this method returns the specified default.
	 */
	public static DLNAImageProfile toDLNAImageProfile(int value, DLNAImageProfile defaultImageProfile) {
		switch (value) {
			case GIF_LRG_INT:
				return GIF_LRG;
			case JPEG_LRG_INT:
				return JPEG_LRG;
			case JPEG_MED_INT:
				return JPEG_MED;
			case JPEG_RES_H_V_INT:
				return JPEG_RES_H_V;
			case JPEG_SM_INT:
				return JPEG_SM;
			case JPEG_TN_INT:
				return JPEG_TN;
			case PNG_LRG_INT:
				return PNG_LRG;
			case PNG_TN_INT:
				return PNG_TN;
			default:
				return defaultImageProfile;
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link DLNAImageProfile}. If the conversion fails, this method returns
	 * the specified default.
	 */
	public static DLNAImageProfile toDLNAImageProfile(String argument, DLNAImageProfile defaultImageProfile) {
		if (argument == null) {
			return defaultImageProfile;
		}

		argument = argument.toUpperCase(Locale.ROOT);
		switch (argument) {
			case "GIF_LRG":
				return DLNAImageProfile.GIF_LRG;
			case "JPEG_LRG":
				return DLNAImageProfile.JPEG_LRG;
			case "JPEG_MED":
				return DLNAImageProfile.JPEG_MED;
			case "JPEG_RES_H_V":
				return DLNAImageProfile.JPEG_RES_H_V;
			case "JPEG_SM":
				return DLNAImageProfile.JPEG_SM;
			case "JPEG_TN":
				return DLNAImageProfile.JPEG_TN;
			case "PNG_LRG":
				return DLNAImageProfile.PNG_LRG;
			case "PNG_TN":
				return DLNAImageProfile.PNG_TN;
			default:
				if (argument.startsWith("JPEG_RES")) {
					Matcher matcher = Pattern.compile("^JPEG_RES_?(\\d+)[X_](\\d+)").matcher(argument);
					if (matcher.find()) {
						return new DLNAImageProfile(
							JPEG_RES_H_V_INT,
							"JPEG_RES_" + matcher.group(1) + "_" + matcher.group(2),
							Integer.parseInt(matcher.group(1)),
							Integer.parseInt(matcher.group(2))
						);
					}
				}
				return defaultImageProfile;
		}
	}

	public String getMimeType() {
		switch (imageProfileInt) {
			case GIF_LRG_INT:
				return "image/gif";
			case JPEG_LRG_INT:
			case JPEG_MED_INT:
			case JPEG_RES_H_V_INT:
			case JPEG_SM_INT:
			case JPEG_TN_INT:
				return "image/jpeg";
			case PNG_LRG_INT:
			case PNG_TN_INT:
				return "image/png";
		}
		return "image/unknown";
	}

	/**
	 * Get {@code H} for the {@code JPEG_RES_H_V} profile. Not applicable
	 * for other profiles.
	 *
	 * @return The {@code H} value.
	 */
	public int getH() {
		return horizontal;
	}

	/**
	 * Get {@code V} for the {@code JPEG_RES_H_V} profile. Not applicable
	 * for other profiles.
	 *
	 * @return The {@code V} value.
	 */
	public int getV() {
		return vertical;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + imageProfileInt;
		return result;
	}

	/**
	 * Only the profile constant is considered when comparing, metadata like
	 * {@code H} and {@code V} aren't taken into account.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DLNAImageProfile)) {
			return false;
		}
		DLNAImageProfile other = (DLNAImageProfile) obj;
		if (imageProfileInt != other.imageProfileInt) {
			return false;
		}
		return true;
	}
}
