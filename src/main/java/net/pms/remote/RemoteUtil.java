package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.pms.PMS;
import net.pms.dlna.Range;
import net.pms.newgui.LooksFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteUtil.class);

    public static void dumpFile(String file, HttpExchange t) throws IOException {
        File f = new File(file);
        dumpFile(f, t);
    }

	public static void dumpFile(File f, HttpExchange t) throws IOException {
		LOGGER.debug("file " + f + " " + f.length());
		if (!f.exists()) {
			throw new IOException("no file");
		}
		t.sendResponseHeaders(200, f.length());
		dump(new FileInputStream(f), t.getResponseBody());
		LOGGER.debug("dump of " + f.getName() + " done");
	}

	public static void dump(final InputStream in, final OutputStream os) throws IOException {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				byte[] buffer = new byte[32 * 1024];
				int bytes;
				int sendBytes = 0;

				try {
					while ((bytes = in.read(buffer)) != -1) {
						sendBytes += bytes;
						os.write(buffer, 0, bytes);
						os.flush();
					}
				} catch (IOException e) {
					LOGGER.trace("Sending stream with premature end: " + sendBytes + " bytes. Reason: " + e.getMessage());
				} finally {
					try {
						in.close();
					} catch (IOException e) {
					}
				}
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		};
		new Thread(r).start();
	}

	public static String getId(String path, HttpExchange t) {
		String id = "0";
		int pos = t.getRequestURI().getPath().indexOf(path);
		if (pos != -1) {
			id = t.getRequestURI().getPath().substring(pos + path.length());
		}
		return id;
	}

	public static String strip(String id) {
		int pos = id.lastIndexOf(".");
		if (pos != -1) {
			return id.substring(0, pos);
		}
		return id;
	}

	public static boolean deny(HttpExchange t) {
		return !PMS.getConfiguration().getIpFiltering().allowed(t.getRemoteAddress().getAddress());
	}

	private static Range nullRange(long len) {
		return Range.create(0, len, 0.0, 0.0);
	}

	public static Range parseRange(Headers hdr, long len) {
		if (hdr == null) {
			return nullRange(len);
		}
		List<String> r = hdr.get("Range");
		if (r == null) { // no range
			return nullRange(len);
		}
		// assume only one
		String range = r.get(0);
		String[] tmp = range.split("=")[1].split("-");
		long start = Long.parseLong(tmp[0]);
		long end = tmp.length == 1 ? len : Long.parseLong(tmp[1]);
		return Range.create(start, end, 0.0, 0.0);
	}

	public static void sendLogo(HttpExchange t) throws IOException {
		InputStream in = LooksFrame.class.getResourceAsStream("/resources/images/logo.png");
		t.sendResponseHeaders(200, 0);
		OutputStream os = t.getResponseBody();
		dump(in, os);
	}

	public static boolean directmime(String mime) {
		return (mime.equals("video/mp4") || mime.equals("video/webm") || mime.equals("video/ogg"));
	}
}
