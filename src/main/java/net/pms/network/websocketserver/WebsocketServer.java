package net.pms.network.websocketserver;

import java.io.IOException;
import java.io.FileInputStream;
import java.net.*;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.FileUtil;
import net.pms.PMS;

import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * A simple WebSocketServer implementation
 */
public class WebsocketServer extends WebSocketServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketServer.class);
  private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();


  public WebsocketServer(int port) throws UnknownHostException {
    super(new InetSocketAddress(port));
  }

  public WebsocketServer(InetSocketAddress address) {
    super(address);
  }

  public WebsocketServer(int port, Draft_6455 draft) {
    super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    conn.send("UMS Server online");
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    broadcast(message);
    System.out.println(conn + ": " + message);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {

  }

  public static WebsocketServer createServer(int port) throws Exception {
    try {
        WebsocketServer s = new WebsocketServer(port);
        if (CONFIGURATION.getWebSocketHttps()) {
          final KeyStore keyStore;
          final KeyManagerFactory keyManagerFactory;
          final TrustManagerFactory trustManagerFactory;
          final SSLContext sslContext;
          // Initialize the keystore
          char[] password = "umsums".toCharArray();
          keyStore = KeyStore.getInstance("JKS");
          try (FileInputStream fis = new FileInputStream(FileUtil.appendPathSeparator(CONFIGURATION.getProfileDirectory()) + "UMS_WEBSOCKETS.jks")) {
            keyStore.load(fis, password);
          }
          // Setup the key manager factory
          keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
          keyManagerFactory.init(keyStore, password);

          // Setup the trust manager factory
          trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
          trustManagerFactory.init(keyStore);

          sslContext = SSLContext.getInstance("TLS");
          sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
          s.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        }
        s.start();
        return s;
    } catch (IOException e) {
        LOGGER.error("Failed to start websocket server: {}", e.getMessage());
        if (e.getMessage().contains("UMS_WEBSOCKETS.jks")) {
					LOGGER.info(
							"To enable HTTPS please generate a self-signed keystore file " +
							"called \"UMS_WEBSOCKETS.jks\" with password \"umsums\" using the java " +
							"'keytool' commandline utility, and place it in the profile folder"
					);
				}
        return null;
    }
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    LOGGER.error("Websocket error: {}", ex.getMessage());
  }

  @Override
  public void onStart() {
    LOGGER.info("Websocket server started");
    setConnectionLostTimeout(0);
    setConnectionLostTimeout(100);
  }

}