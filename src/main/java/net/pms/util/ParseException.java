package net.pms.util;

import java.io.IOException;


/**
 * An {@link IOException} indicating some form of parsing issue.
 *
 * @author Nadahar
 */
public class ParseException extends IOException {

	private static final long serialVersionUID = 1L;

	public ParseException() {
	}

	public ParseException(String message) {
		super(message);
	}

	public ParseException(Throwable cause) {
		super(cause);
	}

	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
