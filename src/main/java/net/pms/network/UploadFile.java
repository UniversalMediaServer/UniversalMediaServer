package net.pms.network;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.WebVideoStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class UploadFile implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadFile.class);
    private final static String CRLF = "\r\n";
    private UploadServer parent;

    public UploadFile(UploadServer p) {
        parent = p;
    }

    private String fallbackName(String url) {
        int pos = url.lastIndexOf("/");
        if (pos == -1) {
            // Bad stuff
            return"Upload_" + System.currentTimeMillis();
        }
        else {
           return url.substring(pos + 1);
        }
    }

    private void uploadFile(String name, InputStream in) throws IOException {
        FileOutputStream out = new FileOutputStream(name);
        byte[] buf = new byte[4096];
        int len;
        while((len=in.read(buf)) != -1)  {
            out.write(buf, 0, len);
        }
        out.flush();
        out.close();
    }

    public void handle(HttpExchange t) throws IOException {
        LOGGER.debug("Got a upload request " + t.getRequestURI());
        // set fallbacks
        String url = t.getRequestURI().getPath();
        String name = null;
        String thumb = null;
        String type = "url";
        String query = "";
        query = t.getRequestURI().getQuery();
        String[] tmp = query.split("&");

        for (int i=0; i < tmp.length; i++) {
            String s = tmp[i];
            if (s.startsWith("u=")) {
              url = s.substring(2);
              continue;
            }
            if (s.startsWith("n=")) {
                name = s.substring(2);
                continue;
            }
            if (s.startsWith("t=")) {
                thumb = s.substring(2);
                continue;
            }
            if (s.startsWith("ty=")) {
                type = s.substring(3);
                continue;
            }
        }
        if (name == null) {
           name = fallbackName(url);
        }
        WebVideoStream obj = new WebVideoStream(name, url, thumb);
        PMS.get().upload(obj, type.equalsIgnoreCase("tmpurl"));
        LOGGER.debug("upload got id "+obj.getResourceId());
        String resp = "http://" + PMS.get().getServer().getHost() + ":" +
                    PMS.get().getServer().getPort() + "/get/" + obj.getResourceId() +
                    "/" + name;
        t.sendResponseHeaders(200, resp.length());
        OutputStream os = t.getResponseBody();
        os.write(resp.getBytes());
        t.close();
    }
}
