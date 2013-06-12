package net.pms.network;

import com.sun.net.httpserver.HttpServer;
import net.pms.PMS;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.HashMap;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class UploadServer implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadServer.class);
    private static final int DEFAULT_PORT = 9999;
    private final static String CRLF = "\r\n";

    private HttpServer server;
    private KeyStore ks;
    private KeyManagerFactory kmf;
    private TrustManagerFactory tmf;
    private SSLContext sslContext;
    private HashMap<String, String> users;

    public UploadServer() {
        this(DEFAULT_PORT);
    }

    public UploadServer(int port) {
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        try {

            users = new HashMap<String, String>();
            readCred();

            // Setup the socket address
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);

            // Initialize the HTTP(S) server
            if (PMS.getConfiguration().getExtHttps()) {
                server = httpsServer(address);
            } else {
                server = HttpServer.create(address, 0);
            }

            // Add context handlers
           // addCtx("/file", new UploadFile(this));
            addCtx("/url", new UploadFile(this));
            addCtx("/file", new UploadFile(this));
            addCtx("/", this);
            //addCtx("/jwplayer", new RemoteFileHandler());
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            LOGGER.debug("Couldn't start RemoteWEB " + e);
        }
    }

    private HttpServer httpsServer(InetSocketAddress address) throws Exception {
        HttpsServer server = HttpsServer.create(address, 0);

        sslContext = SSLContext.getInstance("TLS");

        // Initialize the keystore
        char[] password = "umsums".toCharArray();
        ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream("UMS.jks");
        ks.load(fis, password);

        // Setup the key manager factory
        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // Setup the trust manager factory
        tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    // initialise the SSL context
                    SSLContext c = SSLContext.getDefault();
                    SSLEngine engine = c.createSSLEngine();
                    params.setNeedClientAuth(true);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // get the default parameters
                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (Exception e) {
                    LOGGER.debug("https configure error  " + e);
                }
            }
        });
        return server;
    }

    private void addCtx(String path, HttpHandler h) {
        HttpContext ctx = server.createContext(path, h);
        ctx.setAuthenticator(new BasicAuthenticator("") {
            @Override
            public boolean checkCredentials(String user, String pwd) {
                //LOGGER.debug("authenticate " + user + " pwd " + pwd);
                return pwd.equals(users.get(user));
            }
        });
    }

    private void readCred() throws IOException {
        String cPath = (String) PMS.getConfiguration().getCustomProperty("cred.path");
        if (StringUtils.isEmpty(cPath)) {
            return;
        }
        File f = new File(cPath);
        if (!f.exists()) {
            return;
        }
        BufferedReader in;
        in = new BufferedReader(new FileReader(f));
        String str;
        while ((str = in.readLine()) != null) {
            str = str.trim();
            if (StringUtils.isEmpty(str) || str.startsWith("#")) {
                continue;
            }
            String[] s = str.split("\\s*=\\s*", 2);
            if (s.length < 2) {
                continue;
            }
            if (!s[0].startsWith("upload")) {
                continue;
            }
            String[] s2 = s[1].split(",", 2);
            if (s2.length < 2) {
                continue;
            }
            // s2[0] == usr s2[1] == pwd
            users.put(s2[0], s2[1]);
        }
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:og=\"http://opengraphprotocol.org/schema/\">");
        sb.append(CRLF);
        sb.append("<head>");
        sb.append(CRLF);
        sb.append("<meta charset=\"utf-8\">");
        sb.append(CRLF);
        sb.append("<title>");
        sb.append("UMS Upload service");
        sb.append("</title></head><body>");
        sb.append(CRLF);
        sb.append("<form action=\"/url\" >");
        sb.append("URL: <input type=\"text\" name=\"u\"><br>");
        sb.append("Name: <input type=\"text\" name=\"n\"><br>");
        sb.append("<input type=\"radio\" name=\"ty\" value=\"tmpurl\">Temporary URL");
        sb.append("<input type=\"radio\" name=\"ty\" value=\"url\" checked>Normal URL");
        sb.append("<input type=\"radio\" name=\"ty\" value=\"fileurl\">File URL");
        sb.append("<br><input type=\"submit\" value=\"Submit\">");
        sb.append("</form>");
        sb.append(CRLF);
        sb.append("</body></html>");
        sb.append(CRLF);
        String page = sb.toString();
        t.sendResponseHeaders(200, page.length());
        OutputStream os = t.getResponseBody();
        os.write(page.getBytes());
        t.close();
    }
}
