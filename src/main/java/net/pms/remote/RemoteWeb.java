package net.pms.remote;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.util.ArrayList;
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
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.RealFile;
import net.pms.dlna.RootFolder;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImageFormat;
import net.pms.network.HTTPResource;
import net.pms.newgui.DbgPacker;
import net.pms.util.FileUtil;
import net.pms.util.FullyPlayed;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RemoteWeb {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWeb.class);
	private KeyStore keyStore;
	private KeyManagerFactory keyManagerFactory;
	private TrustManagerFactory trustManagerFactory;
	private HttpServer server;
	private SSLContext sslContext;
	private Map<String, RootFolder> roots;
	private RemoteUtil.ResourceManager resources;
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private static final int defaultPort = configuration.getWebPort();

	public RemoteWeb() throws IOException {
		this(defaultPort);
	}

	public RemoteWeb(int port) throws IOException {
		if (port <= 0) {
			port = defaultPort;
		}

		roots = new HashMap<>();
		// Add "classpaths" for resolving web resources
		resources = AccessController.doPrivileged(new PrivilegedAction<RemoteUtil.ResourceManager>() {

			@Override
			public RemoteUtil.ResourceManager run() {
				return new RemoteUtil.ResourceManager(
					"file:" + configuration.getProfileDirectory() + "/web/",
					"jar:file:" + configuration.getProfileDirectory() + "/web.zip!/",
					"file:" + configuration.getWebPath() + "/"
				);
			}
		});

		//readCred();

		// Setup the socket address
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);

		// Initialize the HTTP(S) server
		if (configuration.getWebHttps()) {
			try {
				server = httpsServer(address);
			} catch (IOException e) {
				LOGGER.error("Failed to start WEB interface on HTTPS: {}", e.getMessage());
				LOGGER.trace("", e);
				if (e.getMessage().contains("UMS.jks")) {
					LOGGER.info(
						"To enable HTTPS please generate a self-signed keystore file " +
						"called \"DMS.jks\" with password \"dmsdms\" using the java " +
						"'keytool' commandline utility, and place it in the profile folder"
					);				}
			} catch (GeneralSecurityException e) {
				LOGGER.error("Failed to start WEB interface on HTTPS due to a security error: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		} else {
			server = HttpServer.create(address, 0);
		}

		if (server != null) {
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
		}
	}

	private HttpServer httpsServer(InetSocketAddress address) throws IOException, GeneralSecurityException {
		// Initialize the keystore
		char[] password = "umsums".toCharArray();
		keyStore = KeyStore.getInstance("JKS");
		try (
			FileInputStream fis = new FileInputStream(
				FileUtil.appendPathSeparator(configuration.getProfileDirectory()) + "DMS.jks"
			)
		) {
			keyStore.load(fis, password);
		}

		// Setup the key manager factory
		keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keyStore, password);

		// Setup the trust manager factory
		trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(keyStore);

		HttpsServer server = HttpsServer.create(address, 0);
		sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

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
		String tag = PMS.getCredTag("web", user);
		//tags.get(user);
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
		RootFolder root;
		synchronized (roots) {
			root = roots.get(cookie);
			if (root == null) {
				// Double-check for cookie errors
				WebRender valid = RemoteUtil.matchRenderer(user, t);
				if (valid != null) {
					// A browser of the same type and user is already connected at
					// this ip but for some reason we didn't get a cookie match.
					RootFolder validRoot = valid.getRootFolder();
					// Do a reverse lookup to see if it's been registered
					for (Map.Entry<String, RootFolder> entry : roots.entrySet()) {
						if (entry.getValue() == validRoot) {
							// Found
							root = validRoot;
							cookie = entry.getKey();
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

	public void associate(HttpExchange t, WebRender webRenderer) {
		webRenderer.associateIP(t.getRemoteAddress().getAddress());
		webRenderer.associatePort(t.getRemoteAddress().getPort());
	}

	private void addCtx(String path, HttpHandler h) {
		HttpContext ctx = server.createContext(path, h);
		if (configuration.isWebAuthenticate()) {
			ctx.setAuthenticator(new BasicAuthenticator("") {
				@Override
				public boolean checkCredentials(String user, String pwd) {
					LOGGER.debug("authenticate " + user);
					return PMS.verifyCred("web", PMS.getCredTag("web", user), user, pwd);
					//return pwd.equals(users.get(user));
					//return true;
				}
			});
		}
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
			DLNAThumbnailInputStream in;
			if (!configuration.isShowCodeThumbs() && !r.isCodeValid(r)) {
				// we shouldn't show the thumbs for coded objects
				// unless the code is entered
				in = r.getGenericThumbnailInputStream(null);
			} else {
				r.checkThumbnail();
				in = r.fetchThumbnailInputStream();
			}
			BufferedImageFilterChain filterChain = null;
			if (r instanceof RealFile && FullyPlayed.isFullyPlayedMark(((RealFile) r).getFile())) {
				filterChain = new BufferedImageFilterChain(FullyPlayed.getOverlayFilter());
			}
			filterChain = r.addFlagFilters(filterChain);
			if (filterChain != null) {
				in = in.transcode(in.getDLNAImageProfile(), false, filterChain);
			}
			Headers hdr = t.getResponseHeaders();
			hdr.add("Content-Type", ImageFormat.PNG.equals(in.getFormat()) ? HTTPResource.PNG_TYPEMIME : HTTPResource.JPEG_TYPEMIME);
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Connection", "keep-alive");
			t.sendResponseHeaders(200, in.getSize());
			OutputStream os = t.getResponseBody();
			LOGGER.trace("Web thumbnail: Input is {} output is {}", in, os);
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
			LOGGER.debug("Handling web interface file request \"{}\"", t.getRequestURI());

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
			vars.put("serverName", configuration.getServerDisplayName());

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
			RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
			WebRender renderer = (WebRender) root.getDefaultRenderer();
			String json = renderer.getPushData();
			RemoteUtil.respond(t, json, 200, "text");
		}
	}
}
