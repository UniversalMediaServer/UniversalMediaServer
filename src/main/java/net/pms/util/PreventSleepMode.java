package net.pms.util;

import java.util.Locale;
import net.pms.Messages;

/**
 * This is a representation of the different modes of system sleep prevention.
 *
 * @author Nadahar
 */
public enum PreventSleepMode {
	/** Never manipulate OS sleep mode */
	NEVER,

	/** Prevent sleep during playback */
	PLAYBACK,

	/** Prevent sleep while UMS is running */
	RUNNING;

	/**
	 * Tries to parse a {@link String} value into a {@link PreventSleepMode}. If
	 * the parsing fails, {@code null} is returned.
	 *
	 * @param sleepMode the {@link String} representing the mode of sleep
	 *            prevention.
	 * @return The corresponding {@link PreventSleepMode} or {@code null}.
	 */
	public static PreventSleepMode typeOf(String sleepMode) {
		if (sleepMode == null) {
			return null;
		}
		sleepMode = sleepMode.trim().toLowerCase(Locale.ROOT);
		switch (sleepMode) {
			case "never":
			case "off":
			case "none":
			case "no":
				return NEVER;
			case "playback":
			case "during playback":
			case "while playing":
			case "while streaming":
				return PLAYBACK;
			case "running":
			case "while running":
			case "when running":
			case "yes":
				return RUNNING;
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
				return Messages.getString("NetworkTab.PreventSleepNever");
			case PLAYBACK:
				return Messages.getString("NetworkTab.PreventSleepDuringPlayback");
			case RUNNING:
				return Messages.getString("NetworkTab.PreventSleepWhileRunning");
			default:
				throw new IllegalStateException("Unimplemented enum value: " + super.toString());
		}
	}
}
