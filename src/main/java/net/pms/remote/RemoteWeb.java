package net.pms.remote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import net.pms.newgui.LooksFrame;
import net.pms.util.PropertiesUtil;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class RemoteWeb {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWeb.class);
	private static final int DEFAULT_PORT = 9001;
	
	private KeyStore ks;
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	
	private HttpsServer server;
	//private HttpServer server1;
	private SSLContext sslContext;
	
	private HashMap<String,String> users;
	private HashMap<String,String> tags;
	private HashMap<String,RootFolder> roots;
	
	public RemoteWeb() {
		this(DEFAULT_PORT);
	}
	
	public RemoteWeb(int port) {
		if (port <= 0) {
			port = DEFAULT_PORT;
		}
		
		users = new HashMap<String,String>();
		tags = new HashMap<String,String>();
		roots = new HashMap<String,RootFolder>();
		
		try {
			readCred();
            // setup the socket address
            InetSocketAddress address = new InetSocketAddress ( InetAddress.getByName("0.0.0.0"), port );

            // initialise the HTTPS server
            server = HttpsServer.create ( address, 0 );
          //  server1 = HttpServer.create(address,0);
            sslContext = SSLContext.getInstance ( "TLS" );

            // initialise the keystore
            char[] password = "umsums".toCharArray ();
            ks = KeyStore.getInstance ( "JKS" );
            FileInputStream fis = new FileInputStream ( "UMS.jks" );
            ks.load ( fis, password );

            // setup the key manager factory
            kmf = KeyManagerFactory.getInstance ( "SunX509" );
            kmf.init ( ks, password );

            // setup the trust manager factory
            tmf = TrustManagerFactory.getInstance ( "SunX509" );
            tmf.init ( ks );

            // setup the HTTPS context and parameters
            sslContext.init ( kmf.getKeyManagers (), tmf.getTrustManagers (), null );
            // Add context handlers
            addCtx("/", new RemoteStartHandler());
            addCtx("/browse", new RemoteBrowseHandler(this));
            addCtx("/play", new RemotePlayHandler(this));
            addCtx("/media", new RemoteMediaHandler(this));
            addCtx("/thumb", new RemoteThumbHandler(this));
            addCtx("/raw", new RemoteRawHandler(this));
            //addCtx("/jwplayer", new RemoteFileHandler());
            server.setExecutor(null);
            server.setHttpsConfigurator ( new HttpsConfigurator( sslContext )
            {
                public void configure ( HttpsParameters params )
                {
                    try
                    {
                        // initialise the SSL context
                        SSLContext c = SSLContext.getDefault ();
                        SSLEngine engine = c.createSSLEngine ();
                        params.setNeedClientAuth ( true );
                        params.setCipherSuites ( engine.getEnabledCipherSuites () );
                        params.setProtocols ( engine.getEnabledProtocols () );

                        // get the default parameters
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters ();
                        params.setSSLParameters ( defaultSSLParameters );
                    }
                    catch ( Exception e ) {
                    	LOGGER.debug("https configure error  "+e);
                    }
                }
            } );
            server.start();
        } catch ( Exception e ) {
        	LOGGER.debug("Couldn't start RemoteWEB "+e);
        }	
	}
	
	public String getTag(String name) {
		String tag = tags.get(name);
		if (tag == null) {
			return name;
		}
		return tag;
	}
	
	public RootFolder getRoot(String name) {
		return getRoot(name, false);
	}
	
	public RootFolder getRoot(String name, boolean create) {
		RootFolder root = roots.get(name);
		if (!create || (root != null)) {
			return root;
		}
		root = new RootFolder(getTag(name));
		root.setDefaultRenderer(RendererConfiguration.getDefaultConf());
		//root.setDefaultRenderer(RendererConfiguration.getRendererConfigurationByName("web"));
		root.discoverChildren();
		roots.put(name, root);
		return root;
	}
	
	private void addCtx(String path,HttpHandler h) {
		HttpContext ctx=server.createContext(path, h);
		ctx.setAuthenticator(new BasicAuthenticator("") {
        	public boolean checkCredentials(String user, String pwd) {
        		LOGGER.debug("authenticate "+user+" pwd "+pwd);
        		//return pwd.equals(users.get(user));
        		return true;
        	}
        });
	}
	
	private void readCred() throws IOException {
		String cPath = (String) PMS.getConfiguration().getCustomProperty("cred.path");
		if(StringUtils.isEmpty(cPath))
			return ;
		File f=new File(cPath);
		if(!f.exists())
			return ;
		BufferedReader in;
		in = new BufferedReader(new FileReader(f));
		String str;
		while ((str = in.readLine()) != null) {
			str=str.trim();
			if(StringUtils.isEmpty(str)||str.startsWith("#")) {
				continue;
			}
			String[] s=str.split("\\s*=\\s*",2);
			if(s.length<2) {
				continue;
			}
			if(!s[0].startsWith("web")) {
				continue;
			}
			String[] s1 = s[0].split(".",2);
			String[] s2 = s[1].split(",",2);
			if(s2.length < 2) {
				continue;
			}
			// s2[0] == usr s2[1] == pwd s1[1] == tag
			users.put(s2[0], s2[1]);
			if(s1.length > 1) {
				// there is a tag here
				tags.put(s2[0], s1[1]);
			}
		}
	}
	
	static class RemoteThumbHandler implements HttpHandler {
		private RemoteWeb parent;
		
		public RemoteThumbHandler(RemoteWeb parent) {
			this.parent = parent;
		}
		
		public void handle(HttpExchange t) throws IOException {
			if(RemoteUtil.deny(t)) {
	    		throw new IOException("Access denied");
	    	}
			String id = RemoteUtil.getId("thumb/", t);
			if(id.contains("logo")) {
				RemoteUtil.sendLogo(t);
				return;
			}
			RootFolder root = parent.getRoot(t.getPrincipal().getUsername());
			if(root == null) {
				LOGGER.debug("weird root in thumb req");
				throw new IOException("Unknown root");
			}
			List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				throw new IOException("Bad id");
			}	
			Headers hdr = t.getResponseHeaders();
			hdr.add("Content-Type" , res.get(0).getThumbnailContentType());
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Connection", "keep-alive");
			InputStream in = res.get(0).getThumbnailInputStream();
			t.sendResponseHeaders(200, in.available());
			OutputStream os = t.getResponseBody();
			LOGGER.debug("input is "+in+" out "+os);
			if (root.getDefaultRenderer().isMediaParserV2()) {
				res.get(0).checkThumbnail();
			}
			RemoteUtil.dump(in,os);
		}
	}
	
	static class RemoteFileHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			LOGGER.debug("file req "+t.getRequestURI());
			if(t.getRequestURI().getPath().contains("crossdomain.xml")) {
				String data="<?xml version=\"1.0\"?>" +
				"<!-- http://www.bitsontherun.com/crossdomain.xml -->" +
				"<cross-domain-policy>" +
				"<allow-access-from domain=\"*\" />"+
				"</cross-domain-policy>";
				t.sendResponseHeaders(200, data.length());
				OutputStream os = t.getResponseBody();
				os.write(data.getBytes());
				os.close();
				return;
			}
			if(t.getRequestURI().getPath().contains("player.swf")) {
				LOGGER .debug("fetch player.swf");
				Headers hdr = t.getResponseHeaders();
				hdr.add("Accept-Ranges", "bytes");
				hdr.add("Server", PMS.get().getServerName());
				//hdr.add("Content-Type", "application/javascript; charset=utf-8");
				RemoteUtil.dumpFile("player.swf",t);
				return;
			}
			if(t.getRequestURI().getPath().contains("jwplayer.js")) {
				LOGGER .debug("fetch jwplayer.js");
				Headers hdr = t.getResponseHeaders();
				hdr.add("Content-Type", "application/x-javascript");
				hdr.add("Accept-Ranges", "bytes");
				hdr.add("Server", PMS.get().getServerName());
				RemoteUtil.dumpFile("jwplayer.js",t);
				return;
			}
		}
	}
	
	static class RemoteStartHandler implements HttpHandler {
		private static final Logger LOGGER = LoggerFactory.getLogger(RemoteStartHandler.class);
		private final static String CRLF = "\r\n";
		
		@Override
		public void handle(HttpExchange t) throws IOException {
			LOGGER.debug("root req "+t.getRequestURI());
			if(RemoteUtil.deny(t)) {
	    		throw new IOException("Access denied");
	    	}
			if(t.getRequestURI().getPath().contains("favicon")) {
				RemoteUtil.sendLogo(t);
				return;
			}
			StringBuilder sb = new StringBuilder();
			sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:og=\"http://opengraphprotocol.org/schema/\">");
			sb.append(CRLF);
			sb.append("<head><title>Universal Media Server</title></head><body>");
			sb.append(CRLF);
			sb.append("<h2><b>Universal Media Server</b></h2><br><br>");
			sb.append(CRLF);
			sb.append("<a href=\"/browse/0\"><img src=\"/thumb/logo\"/></a><br><br>");
			sb.append(CRLF);
			sb.append("<h2><b>");
			sb.append(PMS.getConfiguration().getProfileName());
			sb.append("</h2></b><br>");
			sb.append("</body></html>");
			String response = sb.toString();
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}		
	}
}
