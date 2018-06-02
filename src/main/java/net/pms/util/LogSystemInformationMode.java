package net.pms.util;

import java.util.Locale;

/**
 * This is a representation of the different modes of system information logging.
 *
 * @author Nadahar
 */
public enum LogSystemInformationMode {
	/** Never log system information */
	NEVER,

	/** Only log system information when the log level is trace */
	TRACE_ONLY,

	/** Always log system information */
	ALWAYS;

	/**
	 * Tries to parse a {@link String} value into a
	 * {@link LogSystemInformationMode}. If the parsing fails, {@code null} is
	 * returned.
	 *
	 * @param logMode the {@link String} representing the mode of system
	 *            information logging.
	 * @return The corresponding {@link LogSystemInformationMode} or
	 *         {@code null}.
	 */
	public static LogSystemInformationMode typeOf(String logMode) {
		if (logMode == null) {
			return null;
		}
		logMode = logMode.trim().toLowerCase(Locale.ROOT);
		switch (logMode) {
			case "never":
			case "off":
			case "none":
			case "no":
			case "false":
				return NEVER;
			case "trace":
			case "trace only":
			case "trace_only":
				return TRACE_ONLY;
			case "always":
			case "on":
			case "yes":
			case "true":
				return ALWAYS;
			default: return null;
		}
	}

	/**
	 * @return the {@link Enum} value as a {@link String}.
	 */
	public String getValue() {
		return super.toString();
	}

	@Override
	public String toString() {
		switch (this) {
			case NEVER:
				return "Never";
			case TRACE_ONLY:
				return "Trace only";
			case ALWAYS:
				return "Always";
			default:
				throw new IllegalStateException("Unimplemented enum value: " + super.toString());
		}
	}
}
