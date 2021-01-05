package net.pms.util;

/**
 * An {@link Exception} indicating a problem with the argument(s).
 *
 * @author Nadahar
 */
public class InvalidArgumentException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidArgumentException() {
	}

	public InvalidArgumentException(String message) {
		super(message);
	}

	public InvalidArgumentException(Throwable cause) {
		super(cause);
	}

	public InvalidArgumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
