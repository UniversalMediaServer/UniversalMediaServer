package net.pms.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * This represent the different H264 predefined levels. Add further levels if
 * more are defined.
 */
public enum H264Level {

	/** Level 1 */
	L1,

	/** Level 1b */
	L1b,

	/** Level 1.1 */
	L1_1,

	/** Level 1.2 */
	L1_2,

	/** Level 1.3 */
	L1_3,

	/** Level 2 */
	L2,

	/** Level 2.1 */
	L2_1,

	/** Level 2.2 */
	L2_2,

	/** Level 3 */
	L3,

	/** Level 3.1 */
	L3_1,

	/** Level 3.2 */
	L3_2,

	/** Level 4 */
	L4,

	/** Level 4.1 */
	L4_1,

	/** Level 4.2 */
	L4_2,

	/** Level 5 */
	L5,

	/** Level 5.1 */
	L5_1,

	/** Level 5.2 */
	L5_2;

	/*
	 * Example values:
	 *
	 * High@L3.0
	 * High@L4.0
	 * High@L4.1
	 * Stereo High@L4.1 / High@L4.1
	 */

	protected static final Pattern pattern = Pattern.compile(
		"^\\s*(?:\\w[^@]*@)?(?:L|LEVEL)?\\s*([\\db]+(?:\\.\\d+|,\\d+)?)(?:@\\S.*\\S)?\\s*(?:/|$)",
		Pattern.CASE_INSENSITIVE
	);

	/**
	 * @param other the {@link H264Level} to compare to.
	 * @return {@code true} if this has a H264 level equal to or greater (
	 *         {@code >=}) than {@code other}, {@code false} otherwise.
	 */
	public boolean isGreaterOrEqual(H264Level other) {
		return compareTo(other) >= 0;
	}

	/**
	 * @param other the {@link H264Level} to compare to.
	 * @return {@code true} if this has a H264 level equal to or smaller (
	 *         {@code <=}) than {@code other}, {@code false} otherwise.
	 */
	public boolean isSmallerOrEqual(H264Level other) {
		return compareTo(other) <= 0;
	}

	/**
	 * Tries to convert {@code value} into a {@link H264Level}. Returns
	 * {@code null} if the conversion fails.
	 *
	 * @param value the {@link String} describing a H264 level.
	 * @return The {@link H264Level} corresponding to {@code value} or
	 *         {@code null}.
	 */
	public static H264Level typeOf(String value) {
		return typeOf(value, null);
	}

	/**
	 * Tries to convert {@code value} into a {@link H264Level}. Returns
	 * {@code defaultValue} if the conversion fails.
	 *
	 * @param value the {@link String} describing a H264 level.
	 * @param defaultValue the default {@link H264Level} to return if the
	 *            conversion fails.
	 * @return The {@link H264Level} corresponding to {@code value} or
	 *         {@code defaultValue}.
	 */
	public static H264Level typeOf(String value, H264Level defaultValue) {
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}

		Matcher matcher = pattern.matcher(value);
		if (matcher.find()) {
			String level = matcher.group(1).replaceAll(",", "\\.").toLowerCase(Locale.ROOT);
			switch (level) {
				case "1":
				case "1.0":
					return L1;
				case "1.1":
					return L1_1;
				case "1.2":
					return L1_2;
				case "1.3":
					return L1_3;
				case "1b":
					return L1b;
				case "2":
				case "2.0":
					return L2;
				case "2.1":
					return L2_1;
				case "2.2":
					return L2_2;
				case "3":
				case "3.0":
					return L3;
				case "3.1":
					return L3_1;
				case "3.2":
					return L3_2;
				case "4":
				case "4.0":
					return L4;
				case "4.1":
					return L4_1;
				case "4.2":
					return L4_2;
				case "5":
				case "5.0":
					return L5;
				case "5.1":
					return L5_1;
				case "5.2":
					return L5_2;
			}
		}

		return defaultValue;
	}

	@Override
	public String toString() {
		switch (this) {
			case L1:
				return "Level 1";
			case L1_1:
				return "Level 1.1";
			case L1_2:
				return "Level 1.2";
			case L1_3:
				return "Level 1.3";
			case L1b:
				return "Level 1b";
			case L2:
				return "Level 2";
			case L2_1:
				return "Level 2.1";
			case L2_2:
				return "Level 2.2";
			case L3:
				return "Level 3";
			case L3_1:
				return "Level 3.1";
			case L3_2:
				return "Level 3.2";
			case L4:
				return "Level 4";
			case L4_1:
				return "Level 4.1";
			case L4_2:
				return "Level 4.2";
			case L5:
				return "Level 5";
			case L5_1:
				return "Level 5.1";
			case L5_2:
				return "Level 5.2";
			default:
				throw new IllegalStateException("Unimplemented enum value: " + super.toString());
		}
	}
}
