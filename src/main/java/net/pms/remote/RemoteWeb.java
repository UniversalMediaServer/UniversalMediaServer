package net.pms.remote;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import javax.net.ssl.*;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteWeb {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWeb.class);
	private static final int DEFAULT_PORT = 9001;
	private KeyStore ks;
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	private HttpServer server;
	private SSLContext sslContext;
	private HashMap<String, String> users;
	private HashMap<String, String> tags;
	private HashMap<String, RootFolder> roots;
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	public RemoteWeb() {
		this(DEFAULT_PORT);
	}

	public RemoteWeb(int port) {
		if (port <= 0) {
			port = DEFAULT_PORT;
		}

		users = new HashMap<>();
		tags = new HashMap<>();
		roots = new HashMap<>();

		try {
			readCred();

			// Setup the socket address
			InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);

			// initialise the HTTP(S) server
			if (configuration.getWebHttps()) {
				server = httpsServer(address);
			} else {
				server = HttpServer.create(address, 0);
			}

			int threads = configuration.getWebThreads();

			// Add context handlers
			addCtx("/", new RemoteStartHandler());
			addCtx("/browse", new RemoteBrowseHandler(this));
			addCtx("/play", new RemotePlayHandler(this));
			addCtx("/media", new RemoteMediaHandler(this));
			addCtx("/fmedia", new RemoteMediaHandler(this, true));
			addCtx("/thumb", new RemoteThumbHandler(this));
			addCtx("/raw", new RemoteRawHandler(this));
			addCtx("/files", new RemoteFileHandler());
			addCtx("/subs", new RemoteFileHandler());
			server.setExecutor(Executors.newFixedThreadPool(threads));
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

	public String getTag(String name) {
		String tag = tags.get(name);
		if (tag == null) {
			return name;
		}
		return tag;
	}

	private String getCookie(String cstr) {
		if (StringUtils.isEmpty(cstr)) {
			return null;
		}
		String[] tmp = cstr.split(";");
		for (String str: tmp) {
			str = str.trim();
			if (str.startsWith("UMS=")) {
				return str.substring(4);
			}
		}
		return null;
	}

	public RootFolder getRoot(String name, HttpExchange t) {
		return getRoot(name, false, t);
	}

	public RootFolder getRoot(String name, boolean create, HttpExchange t) {
		String groupTag = getTag(name);
		String cookie = getCookie(t.getRequestHeaders().getFirst("Cookie"));
		RootFolder root = roots.get(cookie);
		if (!create || (root != null)) {
			t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/");
			return root;
		}
		ArrayList<String> tag = new ArrayList<>();
		tag.add(name);
		if (!groupTag.equals(name)) {
			tag.add(groupTag);
		}

		tag.add(t.getRemoteAddress().getHostString());
		tag.add("web");
		root = new RootFolder(tag);
		try {
			WebRender render = new WebRender(name);
			root.setDefaultRenderer(render);
			render.associateIP(t.getRemoteAddress().getAddress());
			render.associatePort(t.getRemoteAddress().getPort());
			render.setUA(t.getRequestHeaders().getFirst("User-agent"));
			PMS.get().setRendererFound(render);
		} catch (ConfigurationException e) {
			root.setDefaultRenderer(RendererConfiguration.getDefaultConf());
		}
		//root.setDefaultRenderer(RendererConfiguration.getRendererConfigurationByName("web"));
		root.discoverChildren();
		cookie = UUID.randomUUID().toString();
		t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/");
		roots.put(cookie, root);
		return root;
	}

	public void associate(HttpExchange t, RendererConfiguration r) {
		WebRender wr = (WebRender) r;
		wr.associateIP(t.getRemoteAddress().getAddress());
		wr.associatePort(t.getRemoteAddress().getPort());
	}

	private void addCtx(String path, HttpHandler h) {
		HttpContext ctx = server.createContext(path, h);
		if (configuration.isWebAuthenticate()) {
			ctx.setAuthenticator(new BasicAuthenticator("") {
				@Override
				public boolean checkCredentials(String user, String pwd) {
					LOGGER.debug("authenticate " + user);
					return pwd.equals(users.get(user));
					//return true;
				}
			});
		}
	}

	private void readCred() throws IOException {
		String cPath = (String) configuration.getCustomProperty("cred.path");
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
			if (!s[0].startsWith("web")) {
				continue;
			}
			String[] s1 = s[0].split("\\.", 2);
			String[] s2 = s[1].split(",", 2);
			if (s2.length < 2) {
				continue;
			}
			// s2[0] == usr s2[1] == pwd s1[1] == tag
			users.put(s2[0], s2[1]);
			if (s1.length > 1) {
				// there is a tag here
				tags.put(s2[0], s1[1]);
			}
		}
		in.close();
	}

	static class RemoteThumbHandler implements HttpHandler {
		private RemoteWeb parent;

		public RemoteThumbHandler(RemoteWeb parent) {
			this.parent = parent;

		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			if (RemoteUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			String id = RemoteUtil.getId("thumb/", t);
			LOGGER.trace("web thumb req " + id);
			if (id.contains("logo")) {
				RemoteUtil.sendLogo(t);
				return;
			}
			RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
			if (root == null) {
				LOGGER.debug("weird root in thumb req");
				throw new IOException("Unknown root");
			}
			final List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				throw new IOException("Bad id");
			}
			DLNAResource r = res.get(0);
			r.checkThumbnail();
			Headers hdr = t.getResponseHeaders();
			hdr.add("Content-Type", r.getThumbnailContentType());
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Connection", "keep-alive");
			InputStream in = r.getThumbnailInputStream();
			t.sendResponseHeaders(200, in.available());
			OutputStream os = t.getResponseBody();
			LOGGER.debug("input is " + in + " out " + os);
			RemoteUtil.dump(in, os);
		}
	}

	static class RemoteFileHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			LOGGER.debug("file req " + t.getRequestURI());
			if (t.getRequestURI().getPath().contains("crossdomain.xml")) {
				String data = "<?xml version=\"1.0\"?>" +
					"<!-- http://www.bitsontherun.com/crossdomain.xml -->" +
					"<cross-domain-policy>" +
					"<allow-access-from domain=\"*\" />" +
					"</cross-domain-policy>";
				t.sendResponseHeaders(200, data.length());
				try (OutputStream os = t.getResponseBody()) {
					os.write(data.getBytes());
				}
				return;
			}
			if (t.getRequestURI().getPath().startsWith("/files/")) {
				// Add content type headers for IE
				// Thx to speedy8754
				if( t.getRequestURI().getPath().endsWith(".css") ) {
					Headers hdr = t.getResponseHeaders();
					hdr.add("Content-Type", "text/css");
				}
				else if( t.getRequestURI().getPath().endsWith(".js") ) {
					Headers hdr = t.getResponseHeaders();
					hdr.add("Content-Type", "text/javascript");
				}
				File f = configuration.getWebFile(t.getRequestURI().getPath().substring(7));
				RemoteUtil.dumpFile(f, t);
				return;
			}
			if (t.getRequestURI().getPath().startsWith("/subs/")) {
				File f = new File(t.getRequestURI().getPath().substring(6));
				RemoteUtil.dumpFile(f, t);
			}
		}
	}

	static class RemoteStartHandler implements HttpHandler {
		private static final Logger LOGGER = LoggerFactory.getLogger(RemoteStartHandler.class);
		private final static String CRLF = "\r\n";

		@Override
		public void handle(HttpExchange t) throws IOException {
			LOGGER.debug("root req " + t.getRequestURI());
			if (RemoteUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			if (t.getRequestURI().getPath().contains("favicon")) {
				RemoteUtil.sendLogo(t);
				return;
			}

			// Front page HTML
			StringBuilder sb = new StringBuilder();
			sb.append("<!DOCTYPE html>").append(CRLF);
				sb.append("<head>").append(CRLF);
					sb.append("<link rel=\"stylesheet\" href=\"/files/reset.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
					sb.append("<link rel=\"stylesheet\" href=\"/files/web.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
					sb.append("<link rel=\"stylesheet\" href=\"/files/web-narrow.css\" type=\"text/css\" media=\"screen and (max-width: 1080px)\">").append(CRLF);
					sb.append("<link rel=\"stylesheet\" href=\"/files/web-wide.css\" type=\"text/css\" media=\"screen and (min-width: 1081px)\">").append(CRLF);
					sb.append("<link rel=\"icon\" href=\"/files/favicon.ico\" type=\"image/x-icon\">").append(CRLF);
					sb.append("<title>Universal Media Server</title>").append(CRLF);
				sb.append("</head>").append(CRLF);
				sb.append("<body id=\"FrontPage\">").append(CRLF);
					sb.append("<div id=\"Container\">").append(CRLF);
						sb.append("<div id=\"Menu\">").append(CRLF);
							sb.append("<a href=\"/browse/0\" id=\"Logo\" title=\"Browse Media\">").append(CRLF);
								sb.append("<h3>");
									sb.append("Browse the media on ").append(configuration.getProfileName());
								sb.append("</h3>");
							sb.append("</a>").append(CRLF);
						sb.append("</div>").append(CRLF);
					sb.append("</div>");
				sb.append("</body>");
			sb.append("</html>");

			String response = sb.toString();
			Headers hdr = t.getResponseHeaders();
			hdr.add("Content-Type", "text/html");
			t.sendResponseHeaders(200, response.length());
			try (OutputStream os = t.getResponseBody()) {
				os.write(response.getBytes());
			}
		}
	}

	public String getUrl() {
		return (server instanceof HttpsServer ? "https://" : "http://") +
			PMS.get().getServer().getHost() + ":" + server.getAddress().getPort();
	}
}
