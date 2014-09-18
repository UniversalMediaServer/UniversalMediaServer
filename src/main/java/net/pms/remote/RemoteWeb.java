package net.pms.remote;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.UUID;
import java.util.concurrent.Executors;
import javax.net.ssl.*;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.logging.LoggingConfigFileLoader;
import net.pms.newgui.DbgPacker;
import net.pms.newgui.DummyFrame;
import net.pms.newgui.LooksFrame;
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
	private HashSet<File> files;
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private final static String CRLF = "\r\n";

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
		files = new HashSet<>();

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
			addCtx("/files", new RemoteFileHandler(this));
			addCtx("/doc", new RemoteDocHandler(this));
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

	public String getTag(String user) {
		String tag = tags.get(user);
		if (tag == null) {
			return user;
		}
		return tag;
	}

	public String getAddress() {
		return PMS.get().getServer().getHost() + ":" + server.getAddress().getPort();
	}

	public RootFolder getRoot(String user, HttpExchange t) {
		return getRoot(user, false, t);
	}

	public RootFolder getRoot(String user, boolean create, HttpExchange t) {
		String groupTag = getTag(user);
		String cookie = RemoteUtil.getCookie("UMS", t);
		RootFolder root = roots.get(cookie);
		if (!create || (root != null)) {
			t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/");
			return root;
		}
		ArrayList<String> tag = new ArrayList<>();
		tag.add(user);
		if (!groupTag.equals(user)) {
			tag.add(groupTag);
		}
		if (t != null) {
			tag.add(t.getRemoteAddress().getHostString());
		}
		tag.add("web");
		root = new RootFolder(tag);
		try {
			WebRender render = new WebRender(user);
			root.setDefaultRenderer(render);
			render.setRootFolder(root);
			render.associateIP(t.getRemoteAddress().getAddress());
			render.associatePort(t.getRemoteAddress().getPort());
//			render.setUA(t.getRequestHeaders().getFirst("User-agent"));
			render.setBrowserInfo(RemoteUtil.getCookie("UMSINFO", t), t.getRequestHeaders().getFirst("User-agent"));
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

	public HttpServer getServer() {
		return server;
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
			final DLNAResource r = root.getDLNAResource(id, root.getDefaultRenderer());
			if (r == null) {
				// another error
				LOGGER.debug("media unknown");
				throw new IOException("Bad id");
			}
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
		private RemoteWeb parent;

		public RemoteFileHandler(RemoteWeb parent) {
			this.parent = parent;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			LOGGER.debug("file req " + t.getRequestURI());

			String path = t.getRequestURI().getPath();
			File file = null;
			String response = null;
			String mime = null;
			int status = 200;

			if (path.contains("crossdomain.xml")) {
				response = "<?xml version=\"1.0\"?>" +
					"<!-- http://www.bitsontherun.com/crossdomain.xml -->" +
					"<cross-domain-policy>" +
					"<allow-access-from domain=\"*\" />" +
					"</cross-domain-policy>";
				mime = "text/xml";

			} else if (path.startsWith("/files/log/")) {
				String filename = path.substring(11);
				if (filename.equals("info")) {
					String log = PMS.get().getFrame().getLog();
					log = log.replaceAll("\n", "<br>");
					String fullLink = "<br><a href=\"/files/log/full\">Full log</a><br><br>";
					String x = fullLink + log;
					if (StringUtils.isNotEmpty(log)) {
						x = x + fullLink;
					}
					response = "<html><title>UMS LOG</title><body>" + x + "</body></html>";
				} else {
					// Retrieve by hash
					file = parent.getFile(filename);
					if (file != null) {
						filename = file.getName();
						String log = RemoteUtil.read(file).replace("<", "&lt;");
						String brush = filename.endsWith("debug.log") ? "debug_log" :
							filename.endsWith(".log") ? "log" : "conf";
						StringBuilder sb = new StringBuilder();
						sb.append("<!DOCTYPE html>").append(CRLF);
							sb.append("<head>").append(CRLF);
								sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">").append(CRLF);
								sb.append("<link rel=\"stylesheet\" href=\"/files/util/sh.css\" type=\"text/css\">").append(CRLF);
								sb.append("<script src=\"/files/jquery.min.js\"></script>").append(CRLF);
								sb.append("<script src=\"/files/util/shCore.js\"></script>").append(CRLF);
								sb.append("<script src=\"/files/util/sh.js\"></script>").append(CRLF);
								sb.append("<title>" + filename + "</title>").append(CRLF);
							sb.append("</head>").append(CRLF);
							sb.append("<body>").append(CRLF);
								sb.append("<pre id=\"rawtext\" class=\"brush: " + brush + "\">").append(CRLF);
									sb.append(log);
								sb.append("</pre>");
							sb.append("</body>");
						sb.append("</html>");
						response = sb.toString();
					} else {
						status = 404;
					}
				}
				mime = "text/html";

			} else {
				// A regular file request
				String filename = path.substring(7);
				// Assume it's a web file
				file = configuration.getWebFile(filename);
				if (! file.exists()) {
					// Not a web file, see if it's a hash request
					file = parent.getFile(filename);
				}
				if (file != null) {
					filename = file.getName();
					// Add content type headers for IE
					// Thx to speedy8754
					if(filename.endsWith(".css")) {
						mime = "text/css";
					}
					else if(filename.endsWith(".js")) {
						mime = "text/javascript";
					}
				} else {
					status = 404;
				}
			}

			if (status == 404 && response == null) {
				response = "<html><body>404 - File Not Found: " + path + "</body></html>";
				mime = "text/html";
			}

			if (mime != null) {
				Headers hdr = t.getResponseHeaders();
				hdr.add("Content-Type", mime);
			}

			if (response != null) {
				byte[] bytes = response.getBytes();
				t.sendResponseHeaders(status, bytes.length);
				try (OutputStream os = t.getResponseBody()) {
					os.write(bytes);
					os.close();
				} catch (Exception e) {
					LOGGER.debug("Error sending response: " + e);
				}
			} else if (file != null) {
				RemoteUtil.dumpFile(file, t);
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
					sb.append(WebRender.umsInfoScript).append(CRLF);
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

	static class RemoteDocHandler implements HttpHandler {
		private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDocHandler.class);
		private final static String CRLF = "\r\n";

		private RemoteWeb parent;

		public RemoteDocHandler(RemoteWeb parent) {
			this.parent = parent;

		}

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
				sb.append("<body id=\"ContentPage\" class=\"Doc\">").append(CRLF);
					sb.append("<div id=\"Menu\">").append(CRLF);
						sb.append("<a href=\"/browse/0\" id=\"HomeButton\"></a>").append(CRLF);
					sb.append("</div>").append(CRLF);
					sb.append("<div id=\"Container\">").append(CRLF);
						sb.append("<h1>Tools</h1>").append(CRLF);
						sb.append("<br/>").append(CRLF);
						sb.append("<ul>").append(CRLF);
						sb.append("<li><a href=\"/files/util/logviewer.html\" title=\"Open general purpose log viewer\">View</a> logs and Confs:").append(CRLF).append(getLogs()).append("</li>").append(CRLF);
						if (configuration.getUseCache()) {
							sb.append("<li><a href=\"http://" + PMS.get().getServer().getHost() + ":" + PMS.get().getServer().getPort() + "/console/home\">Manage cache.</a></li>").append(CRLF);
						}
						sb.append("</ul>").append(CRLF);
						sb.append("<br/><br/>").append(CRLF);
						sb.append("<h1>Documentation</h1>").append(CRLF);
						sb.append("<br/>").append(CRLF);
						sb.append("<ul>").append(CRLF);
						sb.append("<li><a href=\"/bump\">Browser-to-UMS Media Player Setup.</a></li>").append(CRLF);
						sb.append("</ul>").append(CRLF);
					sb.append("</div>");
				sb.append("</body>");
			sb.append("</html>");

			String response = sb.toString();
			t.sendResponseHeaders(200, response.length());
			try (OutputStream os = t.getResponseBody()) {
				os.write(response.getBytes());
			}
		}

		private String getLogs() {
			StringBuilder sb = new StringBuilder();
			sb.append("<ul>").append(CRLF);
			for (File f : new DbgPacker().getItems()) {
				if (f.exists()) {
					sb.append("<li><a href=\"/files/log/" + parent.addFile(f) + "\">" + f.getName() + "</a></li>").append(CRLF);
				}
			}
			sb.append("</ul>").append(CRLF);
			return sb.toString();
		}
	}

	public int addFile(File f) {
		files.add(f);
		return f.hashCode();
	}

	public File getFile(String hash) {
		try {
			int h = Integer.valueOf(hash);
			for (File f : files) {
				if (f.hashCode() == h) {
					return f;
				}
			}
		} catch (NumberFormatException e) {
		}
		return null;
	}

	public String getUrl() {
		return (server instanceof HttpsServer ? "https://" : "http://") +
			PMS.get().getServer().getHost() + ":" + server.getAddress().getPort();
	}
}
