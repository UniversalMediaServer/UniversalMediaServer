package net.pms.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

public class RemoteUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteUtil.class);
	
	public static void dumpFile(String file, HttpExchange t) throws IOException {
		File f = new File(file);
		LOGGER.debug("file "+f+" "+f.length());
		if(!f.exists()) {
			throw new IOException("no file");
		}
		t.sendResponseHeaders(200, f.length());
		dump(new FileInputStream(f), t.getResponseBody());
		LOGGER.debug("dump of "+file+" done");
	}
	
	public static void dump(InputStream in,OutputStream os) throws IOException {
		byte[] buffer = new byte[32 * 1024];
		int bytes;
		int sendBytes = 0;

		try {
			while ((bytes = in.read(buffer)) != -1) {
				sendBytes += bytes;
				os.write(buffer, 0, bytes);
			}
		} catch (IOException e) {
			LOGGER.trace("Sending stream with premature end: " + sendBytes + " bytes. Reason: " + e.getMessage());
		}
		finally {
			in.close();
		}
		os.close();
	}
	
	public static String getId(String path, HttpExchange t) {
		String id = "0";
		int pos = t.getRequestURI().getPath().indexOf(path);
    	if(pos != -1) {    		
    		id = t.getRequestURI().getPath().substring(pos + path.length());
    	}
    	return id;
	}

}
