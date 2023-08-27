/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.mediaserver.handlers.api;

import net.pms.library.LibraryScanner;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderScanner implements ApiResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(FolderScanner.class);
	public static final String PATH_MATCH = "folderscanner";

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "0");
		output.setStatus(HttpResponseStatus.NO_CONTENT);
		switch (uri) {
			case "rescan" -> rescanLibrary();
			case "rescanFileOrFolder" -> LibraryScanner.scanFileOrFolder(content);
			default -> {
				LOGGER.warn("Invalid API call. Unknown path : " + uri);
				output.setStatus(HttpResponseStatus.NOT_FOUND);
			}
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
