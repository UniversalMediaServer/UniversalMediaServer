package net.pms.network.mediaserver.jupnp.support.umsservice;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.ErrorCode;


public class UmsExtendedServicesException extends ActionException {

	private static final long serialVersionUID = 1L;

	public UmsExtendedServicesException(ErrorCode errorCode) {
		super(errorCode);
	}

	public UmsExtendedServicesException(int errorCode, String message) {
		super(errorCode, message);
	}

	public UmsExtendedServicesException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public UmsExtendedServicesException(int errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}

	public UmsExtendedServicesException(ErrorCode errorCode, String message, boolean concatMessages) {
		super(errorCode, message, concatMessages);
	}

	public UmsExtendedServicesException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}

}
