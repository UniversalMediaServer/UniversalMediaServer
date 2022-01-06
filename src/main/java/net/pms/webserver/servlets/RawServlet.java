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
package net.pms.webserver.servlets;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(RawServlet.class);

	public RawServlet(WebServerServlets parent) {
		super(parent);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			URI uri = URI.create(request.getRequestURI());
			LOGGER.debug("got a raw request " + uri);
			RootFolder root = parent.getRoot(request, response);
			if (root == null) {
				LOGGER.debug("root not found");
				response.sendError(401, "Unknown root");
				return;
			}
			String id = RemoteUtil.strip(RemoteUtil.getId("raw/", uri));
			LOGGER.debug("raw id " + id);
			List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				response.sendError(404, "Bad id");
				return;
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
				range = RemoteUtil.parseRange(request.getHeader("Range"), len);
				in = dlna.getInputStream(range, root.getDefaultRenderer());
				if (len == 0) {
					// For web resources actual length may be unknown until we open the stream
					len = dlna.length();
				}
				mime = root.getDefaultRenderer().getMimeType(dlna);
			}

			LOGGER.debug("Sending media \"{}\" with mime type \"{}\"", dlna, mime);
			response.setContentType(mime);
			response.addHeader("Accept-Ranges", "bytes");
			response.addHeader("Server", PMS.get().getServerName());
			response.addHeader("Connection", "keep-alive");
			response.addHeader("Transfer-Encoding", "chunked");
			if (in != null && in.available() != len) {
				response.addHeader("Content-Range", "bytes " + range.getStart() + "-" + in.available() + "/" + len);
				response.setContentLength(in.available());
				response.setStatus(206);
			} else {
				response.setStatus(200);
			}

			OutputStream os = new BufferedOutputStream(response.getOutputStream(), 512 * 1024);
			LOGGER.debug("start raw dump");
			RemoteUtil.dumpDirect(in, os);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RawServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
