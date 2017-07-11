package net.pms.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.util.Locale;

/**
 * This {@code enum} represents the possible bit rate "modes".
 */
public enum BitRateMode {

	/** Constant bit rate mode (CBR) */
	CONSTANT,

	/** Variable bit rate mode (VBR) */
	VARIABLE;

	@Override
	public String toString() {
		switch (this) {
			case CONSTANT:
				return "Constant";
			case VARIABLE:
				return "Variable";
			default:
				return super.toString();
		}
	}

	/**
	 * Tries to parse {@code value} and return the corresponding
	 * {@link BitRateMode}. Returns {@code null} if the parsing fails.
	 *
	 * @param value the {@link String} to parse.
	 * @return The corresponding {@link BitRateMode} or {@code null}.
	 */
	public static BitRateMode typeOf(String value) {
		if (isBlank(value)) {
			return null;
		}
		value = value.toUpperCase(Locale.ROOT).trim();
		switch (value) {
			case "CBR":
			case "CONSTANT":
				return CONSTANT;
			case "VBR":
			case "VARIABLE":
				return VARIABLE;
			default:
				return null;
		}
	}
}
