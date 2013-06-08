package net.pms.network;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.WebVideoStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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

    private void reply(String id, String name, HttpExchange t) throws IOException {
        LOGGER.debug("upload got id " + id);
        String resp = "http://" + PMS.get().getServer().getHost() + ":" +
                PMS.get().getServer().getPort() + "/get/" + id +
                "/" + name;
        t.sendResponseHeaders(200, resp.length());
        OutputStream os = t.getResponseBody();
        os.write(resp.getBytes());
        t.close();
    }

    public void handle(HttpExchange t) throws IOException {
        LOGGER.debug("Got a upload request " + t.getRequestURI());
        if (t.getRequestURI().getPath().startsWith("/file/")) {
            String name = t.getRequestURI().getPath().substring(6);
            String fName = PMS.getConfiguration().getUploadFile(name);
            uploadFile(fName, t.getRequestBody());
            RealFile rf = new RealFile(new File(fName));
            PMS.get().upload(rf, false);
            reply(rf.getResourceId(), name, t);
            return;
        }
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
        if (StringUtils.isEmpty(name)) {
           name = fallbackName(url);
        }
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
        WebVideoStream obj = new WebVideoStream(name, url, thumb);
        PMS.get().upload(obj, type.equalsIgnoreCase("tmpurl"));
        reply(obj.getResourceId(), name, t);
    }
}
