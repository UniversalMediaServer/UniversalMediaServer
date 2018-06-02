package net.pms.dlna;

import java.io.IOException;


/**
 * An {@link IOException} indicating a DLNA media format profile related issue.
 *
 * @author Nadahar
 */
public class DLNAProfileException extends IOException {

	private static final long serialVersionUID = 1L;

	public DLNAProfileException() {
	}

	public DLNAProfileException(String message) {
		super(message);
	}

	public DLNAProfileException(Throwable cause) {
		super(cause);
	}

	public DLNAProfileException(String message, Throwable cause) {
		super(message, cause);
	}
}
