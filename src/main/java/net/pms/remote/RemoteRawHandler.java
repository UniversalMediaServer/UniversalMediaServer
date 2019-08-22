package net.pms.remote;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RemoteRawHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRawHandler.class);
	private RemoteWeb parent;

	public RemoteRawHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			LOGGER.debug("got a raw request " + t.getRequestURI());
			if (RemoteUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
			if (root == null) {
				throw new IOException("Unknown root");
			}
			String id;
			id = RemoteUtil.strip(RemoteUtil.getId("raw/", t));
			LOGGER.debug("raw id " + id);
			List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				throw new IOException("Bad id");
			}
			DLNAResource dlna = res.get(0);
			long len;
			String mime = null;
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
					root.getDefaultRenderer().getMimeType(dlna.mimeType(), dlna.getMedia());

				len = supported && imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ? imageInfo.getSize() : dlna.length();
				range = new Range.Byte(0l, len);
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
				range = RemoteUtil.parseRange(t.getRequestHeaders(), len);
				in = dlna.getInputStream(range, root.getDefaultRenderer());
				if (len == 0) {
					// For web resources actual length may be unknown until we open the stream
					len = dlna.length();
				}
				mime = root.getDefaultRenderer().getMimeType(dlna.mimeType(), dlna.getMedia());
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
			RemoteUtil.dump(in, os);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteRawHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
