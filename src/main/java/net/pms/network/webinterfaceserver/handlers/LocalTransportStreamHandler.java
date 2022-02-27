/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.webinterfaceserver.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.HTTPResource;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves .ts segments that are referenced by dynamic HLS playlists.
 */
public class LocalTransportStreamHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalTransportStreamHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	public LocalTransportStreamHandler() {}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			LOGGER.debug("root req " + httpExchange.getRequestURI());
			if (WebInterfaceServerUtil.deny(httpExchange)) {
				throw new IOException("Access denied");
			}

			String uriPath = httpExchange.getRequestURI().getPath();
			String filename = uriPath.substring(4);
			if (!filename.endsWith(".ts")) {
				throw new Exception("File must be a transport stream");
			}
			String videoSectionPath = FileUtil.appendPathSeparator(CONFIGURATION.getTempFolder().getAbsolutePath()) + filename;
			File videoSection = new File(videoSectionPath);
			byte[] response = FileUtils.readFileToByteArray(videoSection);

			Headers headers = httpExchange.getResponseHeaders();
			String mimeType = HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME;
			headers.add("accept-ranges", "bytes");
			WebInterfaceServerUtil.respond(httpExchange, response, 200, mimeType);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in LocalTransportStreamHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
