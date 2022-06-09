package net.pms.network.websocketserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

/**
 * A simple WebSocketServer implementation
 */
public class WebsocketServer extends WebSocketServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketServer.class);

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
    conn.send("Welcome to the server!");
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
        s.start();
        return s;
    } catch (IOException e) {
        LOGGER.error("Failed to start websocket server: {}", e.getMessage());
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