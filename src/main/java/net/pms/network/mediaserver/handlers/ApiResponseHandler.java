package net.pms.network.mediaserver.handlers;

import org.jboss.netty.handler.codec.http.HttpResponse;

public interface ApiResponseHandler {

	public String handleRequest(String uri, String content, HttpResponse output);
}
