package net.pms.network.mediaserver.nettyserver;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class HttpException extends Exception {

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
