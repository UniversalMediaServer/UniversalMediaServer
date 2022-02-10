package net.pms.network.mediaserver.handlers.api;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.LibraryScanner;
import net.pms.dlna.RootFolder;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class FolderScanner implements ApiResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(FolderScanner.class);

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "0");
		output.setStatus(HttpResponseStatus.NO_CONTENT);
		switch (uri) {
			case "rescan":
				rescanLibrary();
				break;
			case "rescanFileOrFolder":
				RootFolder.rescanLibraryFileOrFolder(content);
				break;
			default:
				LOGGER.warn("Invalid API call. Unknown path : " + uri);
				output.setStatus(HttpResponseStatus.NOT_FOUND);
				break;
		}
		return null;
	}

	/**
	 * rescan library
	 */
	private void rescanLibrary() {
		if (!LibraryScanner.isScanLibraryRunning()) {
			LibraryScanner.scanLibrary();
		} else {
			LOGGER.warn("library scan already in progress");
		}
	}

}
