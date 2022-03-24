package net.pms.network.mediaserver.handlers.api.playlist;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class PlaylistService implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class.getName());
	public static final String PATH_MATCH = "playlist";

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		return null;
	}

}
