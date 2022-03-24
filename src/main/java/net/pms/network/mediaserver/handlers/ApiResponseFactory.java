package net.pms.network.mediaserver.handlers;

import net.pms.network.mediaserver.handlers.api.FolderScanner;
import net.pms.network.mediaserver.handlers.api.LikeMusic;
import net.pms.network.mediaserver.handlers.api.starrating.StarRating;

public class ApiResponseFactory {

	public ApiResponseHandler getApiResponseHandler(String apiType) {
		switch (apiType) {
			case FolderScanner.PATH_MATCH:
				return new FolderScanner();
			case LikeMusic.PATH_MATCH:
				return new LikeMusic();
			case StarRating.PATH_MATCH:
				return new StarRating();
		}

		throw new RuntimeException("No api Handler found");
	}
}
