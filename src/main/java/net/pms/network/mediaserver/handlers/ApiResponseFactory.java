package net.pms.network.mediaserver.handlers;

import net.pms.network.mediaserver.handlers.api.FolderScanner;
import net.pms.network.mediaserver.handlers.api.LikeMusic;

public class ApiResponseFactory {

	public ApiResponseHandler getApiResponseHandler(String apiType) {
		switch (apiType) {
			case "folderscanner":
				return new FolderScanner();
			case "like":
				return new LikeMusic();
		}

		throw new RuntimeException("No api Handler found");
	}
}
