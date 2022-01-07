/*
 * Universal Media Server, for streaming any medias to DLNA
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
package net.pms.webserver.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import net.pms.encoders.ImagePlayer;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.webserver.WebServerUtil;
import net.pms.webserver.WebServerHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RawHandler.class);

	private final WebServerHttpServer parent;

	public RawHandler(WebServerHttpServer parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			LOGGER.debug("got a raw request " + t.getRequestURI());
			if (WebServerUtil.deny(t)) {
				throw new IOException("Access denied");
			}

			RootFolder root = parent.getRoot(WebServerUtil.userName(t), t);
			if (root == null) {
				throw new IOException("Unknown root");
			}
			String id;
			id = WebServerUtil.strip(WebServerUtil.getId("raw/", t));
			LOGGER.debug("raw id " + id);
			List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				throw new IOException("Bad id");
			}
			DLNAResource dlna = res.get(0);
			long len;
			String mime;
			InputStream in;
			Range.Byte range;
			if (dlna.getMedia() != null && dlna.getMedia().isImage() && dlna.getMedia().getImageInfo() != null) {
				boolean supported = false;
				ImageInfo imageInfo = dlna.getMedia().getImageInfo();
				if (root.getDefaultRenderer() instanceof WebRender) {
					WebRender renderer = (WebRender) root.getDefaultRenderer();
					supported = renderer.isImageFormatSupported(imageInfo.getFormat());
				}
				mime = dlna.getFormat() != null ?
					dlna.getFormat().mimeType() :
					root.getDefaultRenderer().getMimeType(dlna);

				len = supported && imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ? imageInfo.getSize() : dlna.length();
				range = new Range.Byte(0L, len);
				if (supported) {
					in = dlna.getInputStream();
				} else {
					InputStream imageInputStream;
					if (dlna.getPlayer() instanceof ImagePlayer) {
						ProcessWrapper transcodeProcess = dlna.getPlayer().launchTranscode(
							dlna,
							dlna.getMedia(),
							new OutputParams(PMS.getConfiguration())
						);
						imageInputStream = transcodeProcess != null ? transcodeProcess.getInputStream(0) : null;
					} else {
						imageInputStream = dlna.getInputStream();
					}
					Image image = Image.toImage(imageInputStream, 3840, 2400, ScaleType.MAX, ImageFormat.JPEG, false);
					len = image == null ? 0 : image.getBytes(false).length;
					in = image == null ? null : new ByteArrayInputStream(image.getBytes(false));
				}
			} else {
				len = dlna.length();
				dlna.setPlayer(null);
				range = WebServerUtil.parseRange(t.getRequestHeaders(), len);
				in = dlna.getInputStream(range, root.getDefaultRenderer());
				if (len == 0) {
					// For web resources actual length may be unknown until we open the stream
					len = dlna.length();
				}
				mime = root.getDefaultRenderer().getMimeType(dlna);
			}

			Headers hdr = t.getResponseHeaders();
			LOGGER.debug("Sending media \"{}\" with mime type \"{}\"", dlna, mime);
			hdr.add("Content-Type", mime);
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Server", PMS.get().getServerName());
			hdr.add("Connection", "keep-alive");
			hdr.add("Transfer-Encoding", "chunked");
			if (in != null && in.available() != len) {
				hdr.add("Content-Range", "bytes " + range.getStart() + "-" + in.available() + "/" + len);
				t.sendResponseHeaders(206, in.available());
			} else {
				t.sendResponseHeaders(200, 0);
			}
			OutputStream os = new BufferedOutputStream(t.getResponseBody(), 512 * 1024);
			LOGGER.debug("start raw dump");
			WebServerUtil.dump(in, os);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RawHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
