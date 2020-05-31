package net.pms.network;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpException extends Exception {
	private static final long serialVersionUID = -4952529888336004924L;
	private HttpResponseStatus status;

	HttpException(HttpResponseStatus status) {
		this.status = status;
	}

	HttpException(HttpResponseStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpResponseStatus getStatus() {
		return this.status;
	}
}
