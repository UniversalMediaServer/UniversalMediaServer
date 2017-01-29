package net.pms.util;

/**
 * A checked {@link Exception} indicating that something is not as expected.
 *
 * @author Nadahar
 */

public class InvalidStateException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidStateException() {
	}

	public InvalidStateException(String message) {
		super(message);
	}

	public InvalidStateException(Throwable cause) {
		super(cause);
	}

	public InvalidStateException(String message, Throwable cause) {
		super(message, cause);
	}
}
