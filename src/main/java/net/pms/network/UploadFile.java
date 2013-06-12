package net.pms.network;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.WebStream;
import net.pms.formats.Format;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;

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

    private String unescape(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (Exception e) {
        }
        return str;
    }

    private String escape(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
        }
        return str;
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

    private void reply(String id, String name, HttpExchange t) throws IOException {
        LOGGER.debug("upload got id " + id);
        String resp = "http://" + PMS.get().getServer().getHost() + ":" +
                PMS.get().getServer().getPort() + "/get/" + id +
                "/" + escape(name);
        t.sendResponseHeaders(200, resp.length());
        OutputStream os = t.getResponseBody();
        os.write(resp.getBytes());
        t.close();
    }

    public void handle(HttpExchange t) throws IOException {
        LOGGER.debug("Got a upload request " + t.getRequestURI());
        if (t.getRequestURI().getPath().startsWith("/file/")) {
            String name = t.getRequestURI().getPath().substring(6);
            if (StringUtils.isEmpty(name)) {
                name = fallbackName("");
            }
            String fName = PMS.getConfiguration().getUploadFile(name);
            uploadFile(fName, t.getRequestBody());
            RealFile rf = new RealFile(new File(fName));
            PMS.get().upload(rf);
            reply(rf.getResourceId(), name, t);
            return;
        }
        // set fallbacks
        String url = t.getRequestURI().getPath();
        String name = null;
        String thumb = null;
        String type = "url";
        int format = Format.VIDEO;
        String query = "";
        query = t.getRequestURI().getQuery();
        String[] tmp = query.split("&");

        for (int i=0; i < tmp.length; i++) {
            String s = tmp[i];
            if (s.split("=").length < 2) {
                // skip these
                continue;
            }
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
            if (s.startsWith("fo=")) {
                String f = s.substring(3);
                if (f.equalsIgnoreCase("video")) {
                    format = Format.VIDEO;
                }
                if (f.equalsIgnoreCase("audio")) {
                    format = Format.AUDIO;
                }
                continue;
            }
        }
        if (StringUtils.isEmpty(name)) {
           name = fallbackName(url);
        }
        name = unescape(name);
        if (type.equalsIgnoreCase("fileurl")) {
            final String url1 = url;
            final String n1 = name;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        URL u = new URL(url1);
                        uploadFile(n1, u.openStream());
                    } catch (Exception e) {
                    }
                }
            };
            new Thread(r).start();
            return;
        }
        WebStream obj = new WebStream(name, url, thumb, format);
        PMS.get().upload(obj);
        if (type.equalsIgnoreCase("url")) {
            WebStream obj1 = new WebStream(name, url, thumb, format);
            PMS.get().addToWeb(obj1, thumb, format);
        }
        reply(obj.getResourceId(), name, t);
    }
}
