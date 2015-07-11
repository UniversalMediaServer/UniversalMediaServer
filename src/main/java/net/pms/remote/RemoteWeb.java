package net.pms.remote;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import javax.net.ssl.*;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.newgui.DbgPacker;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteWeb {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWeb.class);
	private KeyStore ks;
	private KeyManagerFactory kmf;
	private TrustManagerFactory tmf;
	private HttpServer server;
	private SSLContext sslContext;
	private HashMap<String, String> users;
	private HashMap<String, String> tags;
	private Map<String, RootFolder> roots;
	private RemoteUtil.ResourceManager resources;
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private static final int defaultPort = configuration.getWebPort();
	

	public RemoteWeb() {
		this(defaultPort);
	}

	public RemoteWeb(int port) {
		if (port <= 0) {
			port = defaultPort;
		}

		users = new HashMap<>();
		tags = new HashMap<>();
		roots = Collections.synchronizedMap(new HashMap<String, RootFolder>());
		// Add "classpaths" for resolving web resources
		resources = new RemoteUtil.ResourceManager(
			"file:" + configuration.getProfileDirectory() + "/web/",
			"jar:file:" + configuration.getProfileDirectory() + "/web.zip!/",
			"file:" + configuration.getWebPath() + "/"
		);

		try {
			readCred();

			// Setup the socket address
			InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);

			// initialise the HTTP(S) server
			if (configuration.getWebHttps()) {
				try {
					server = httpsServer(address);
				} catch (Exception e) {
					LOGGER.warn("Error: Failed to start WEB interface on HTTPS: " + e);
					LOGGER.info("To enable HTTPS please generate a self-signed keystore file called 'UMS.jks' using the java 'keytool' commandline utility.");
					server = null;
				}
			} else {
				server = HttpServer.create(address, 0);
			}

			int threads = configuration.getWebThreads();

			// Add context handlers
			addCtx("/", new RemoteStartHandler(this));
			addCtx("/browse", new RemoteBrowseHandler(this));
			RemotePlayHandler playHandler = new RemotePlayHandler(this);
			addCtx("/play", playHandler);
			addCtx("/playstatus", playHandler);
			addCtx("/playlist", playHandler);
			addCtx("/media", new RemoteMediaHandler(this));
			addCtx("/fmedia", new RemoteMediaHandler(this, true));
			addCtx("/thumb", new RemoteThumbHandler(this));
			addCtx("/raw", new RemoteRawHandler(this));
			addCtx("/files", new RemoteFileHandler(this));
			addCtx("/doc", new RemoteDocHandler(this));
			addCtx("/poll", new RemotePollHandler(this));
			server.setExecutor(Executors.newFixedThreadPool(threads));
			server.start();
		} catch (Exception e) {
			LOGGER.debug("Couldn't start RemoteWEB " + e);
		}
	}

	private HttpServer httpsServer(InetSocketAddress address) throws Exception {
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

		HttpsServer server = HttpsServer.create(address, 0);
		sslContext = SSLContext.getInstance("TLS");
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
		if (root == null) {
			// Double-check for cookie errors
			WebRender valid = RemoteUtil.matchRenderer(user, t);
			if (valid != null) {
				// A browser of the same type and user is already connected at
				// this ip but for some reason we didn't get a cookie match.
				RootFolder validRoot = valid.getRootFolder();
				// Do a reverse lookup to see if it's been registered
				for (String c : roots.keySet()) {
					if (roots.get(c) == validRoot) {
						// Found
						root = validRoot;
						cookie = c;
						LOGGER.debug("Allowing browser connection without cookie match: {}: {}", valid.getRendererName(), t.getRemoteAddress().getAddress());
						break;
					}
				}
			}
		}
		if (!create || (root != null)) {
			t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/");
			return root;
		}
		synchronized (roots) {
			ArrayList<String> tag = new ArrayList<>();
			tag.add(user);
			if (!groupTag.equals(user)) {
				tag.add(groupTag);
			}

			tag.add(t.getRemoteAddress().getHostString());
			tag.add("web");
			root = new RootFolder(tag);
			try {
				WebRender render = new WebRender(user);
				root.setDefaultRenderer(render);
				render.setRootFolder(root);
				render.associateIP(t.getRemoteAddress().getAddress());
				render.associatePort(t.getRemoteAddress().getPort());
				if (configuration.useWebSubLang()) {
					render.setSubLang(StringUtils.join(RemoteUtil.getLangs(t), ","));
				}
//				render.setUA(t.getRequestHeaders().getFirst("User-agent"));
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
		}
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
			InputStream in;
			if (!configuration.isShowCodeThumbs() && !r.isCodeValid(r)) {
				// we shouldn't show the thumbs for coded objects
				// unless the code is entered
				in = r.getGenericThumbnailInputStream(null);
			} else {
				r.checkThumbnail();
				in = r.getThumbnailInputStream();
			}
			Headers hdr = t.getResponseHeaders();
			hdr.add("Content-Type", r.getThumbnailContentType());
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Connection", "keep-alive");
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
					File file = parent.getResources().getFile(filename);
					if (file != null) {
						filename = file.getName();
						HashMap<String, Object> vars = new HashMap<>();
						vars.put("title", filename);
						vars.put("brush", filename.endsWith("debug.log") ? "debug_log" :
							filename.endsWith(".log") ? "log" : "conf");
						vars.put("log", RemoteUtil.read(file).replace("<", "&lt;"));
						response = parent.getResources().getTemplate("util/log.html").execute(vars);
					} else {
						status = 404;
					}
				}
				mime = "text/html";

			} else if (parent.getResources().write(path.substring(7), t)) {
				// The resource manager found and sent the file, all done.
				return;

			} else {
				status = 404;
			}

			if (status == 404 && response == null) {
				response = "<html><body>404 - File Not Found: " + path + "</body></html>";
				mime = "text/html";
			}

			RemoteUtil.respond(t, response, status, mime);
		}
	}

	static class RemoteStartHandler implements HttpHandler {
		private static final Logger LOGGER = LoggerFactory.getLogger(RemoteStartHandler.class);
		@SuppressWarnings("unused")
		private final static String CRLF = "\r\n";
		private RemoteWeb parent;

		public RemoteStartHandler(RemoteWeb parent) {
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

			HashMap<String, Object> vars = new HashMap<>();
			vars.put("serverName", configuration.getServerName());
			vars.put("profileName", configuration.getProfileName());

			String response = parent.getResources().getTemplate("start.html").execute(vars);
			RemoteUtil.respond(t, response, 200, "text/html");
		}
	}

	static class RemoteDocHandler implements HttpHandler {
		private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDocHandler.class);
		@SuppressWarnings("unused")
		private final static String CRLF = "\r\n";

		private RemoteWeb parent;

		public RemoteDocHandler(RemoteWeb parent) {
			this.parent = parent;
			// Make sure logs are available right away
			getLogs(false);
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

			HashMap<String, Object> vars = new HashMap<>();
			vars.put("logs", getLogs(true));
			if (configuration.getUseCache()) {
				vars.put("cache", "http://" + PMS.get().getServer().getHost() + ":" + PMS.get().getServer().getPort() + "/console/home");
			}

			String response = parent.getResources().getTemplate("doc.html").execute(vars);
			RemoteUtil.respond(t, response, 200, "text/html");
		}

		private ArrayList<HashMap<String, String>> getLogs(boolean asList) {
			Set<File> files = new DbgPacker().getItems();
			ArrayList<HashMap<String, String>> logs = asList ? new ArrayList<HashMap<String, String>>() : null;
			for (File f : files) {
				if (f.exists()) {
					String id = String.valueOf(parent.getResources().add(f));
					if (asList) {
						HashMap<String, String> item = new HashMap<>();
						item.put("filename", f.getName());
						item.put("id", id);
						logs.add(item);
					}
				}
			}
			return logs;
		}
	}

	public RemoteUtil.ResourceManager getResources() {
		return resources;
	}

	public String getUrl() {
		if (server != null) {
			return (server instanceof HttpsServer ? "https://" : "http://") +
				PMS.get().getServer().getHost() + ":" + server.getAddress().getPort();
		}
		return null;
	}

	static class RemotePollHandler implements HttpHandler {
		@SuppressWarnings("unused")
		private static final Logger LOGGER = LoggerFactory.getLogger(RemotePollHandler.class);
		@SuppressWarnings("unused")
		private final static String CRLF = "\r\n";

		private RemoteWeb parent;

		public RemotePollHandler(RemoteWeb parent) {
			this.parent = parent;
		}

		@Override
		public void handle(HttpExchange t) throws IOException {
			//LOGGER.debug("poll req " + t.getRequestURI());
			if (RemoteUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			@SuppressWarnings("unused")
			String p = t.getRequestURI().getPath();
			RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
			WebRender renderer = (WebRender) root.getDefaultRenderer();
			String json = renderer.getPushData();
			RemoteUtil.respond(t, json, 200, "text");
		}
	}
}
