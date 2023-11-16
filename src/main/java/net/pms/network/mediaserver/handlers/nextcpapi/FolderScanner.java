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
package net.pms.network.mediaserver.handlers.nextcpapi;

import net.pms.store.MediaScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME : this should not exists.
//if a bug exists on the scanner, it should be solved.
public class FolderScanner implements NextcpApiResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(FolderScanner.class);
	public static final String PATH_MATCH = "folderscanner";

	@Override
	public NextcpApiResponse handleRequest(String uri, String content) {
		NextcpApiResponse response = new NextcpApiResponse();
		response.setStatusCode(204);
		switch (uri) {
			case "rescan" -> rescanMediaStore();
			case "rescanFileOrFolder" -> MediaScanner.backgroundScanFileOrFolder(content);
			default -> {
				LOGGER.warn("Invalid nextcp API call. Unknown path : " + uri);
				response.setStatusCode(404);
			}
		}
		return response;
	}

	/**
	 * rescan MediaScanner store
	 */
	private void rescanMediaStore() {
		if (!MediaScanner.isMediaScanRunning()) {
			MediaScanner.startMediaScan();
		} else {
			LOGGER.warn("Media scan already in progress");
		}
	}

}
