package net.pms.network.websocketserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * A simple WebSocketServer implementation
 */
public class WebsocketServer extends WebSocketServer {

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
    conn.send("Welcome to the server!"); //This method sends a message to the new client
    broadcast("new connection: " + handshake
        .getResourceDescriptor()); //This method sends a message to all clients connected
    System.out.println(
        conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    broadcast(conn + " has left the room!");
    System.out.println(conn + " has left the room!");
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    broadcast(message);
    System.out.println(conn + ": " + message);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {
    broadcast(message.array());
    System.out.println(conn + ": " + message);
  }

  public static WebsocketServer createServer(int port) throws Exception {
    try {
        WebsocketServer s = new WebsocketServer(port);
        s.start();
        //setup(s);
        System.out.println("WebsocketServer started on port: " + s.getPort());
        return s;
        
    } catch (IOException e) {
        return null;
    }
  }
  private static void setup(WebsocketServer s) {
    try {
        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
        //while (true) {
          String in = sysin.readLine();
          s.broadcast(in);
          if (in.equals("exit")) {
            s.stop(1000);
            //break;
          }
        //}
    } catch (Exception e) {
      return;
    }

  }
//   public static void main(String[] args) throws InterruptedException, IOException {
//     int port = 8887; // 843 flash policy port
//     try {
//       port = Integer.parseInt(args[0]);
//     } catch (Exception ex) {
//     }
//     WebsocketServer s = new WebsocketServer(port);
//     s.start();
//     System.out.println("WebsocketServer started on port: " + s.getPort());

//     BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
//     while (true) {
//       String in = sysin.readLine();
//       s.broadcast(in);
//       if (in.equals("exit")) {
//         s.stop(1000);
//         break;
//       }
//     }
//   }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    ex.printStackTrace();
    if (conn != null) {
      // some errors like port binding failed may not be assignable to a specific websocket
    }
  }

  @Override
  public void onStart() {
    System.out.println("Server started!");
    setConnectionLostTimeout(0);
    setConnectionLostTimeout(100);
  }

}