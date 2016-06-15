package net.pms.update;

public class UpdateException extends Exception {
	private static final long serialVersionUID = 661674274433241720L;

	UpdateException(String message) {
		super(message);
	}

	UpdateException(String message, Throwable cause) {
		super(message, cause);
	}
}
