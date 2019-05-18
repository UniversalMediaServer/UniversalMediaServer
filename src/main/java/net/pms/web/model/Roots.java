package net.pms.web.model;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.web.resources.ResourceUtil;

@Singleton
public class Roots {
	private static final Logger LOGGER = LoggerFactory.getLogger(Roots.class);

	private Map<String, RootFolder> roots = new HashMap<>();

	private PmsConfiguration configuration;

	@Inject
	public Roots(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public RootFolder getRoot(String user, HttpServletRequest req) throws InterruptedException {
		return getRoot(user, false, req);
	}

	public RootFolder getRoot(String user, boolean create, HttpServletRequest req) throws InterruptedException {
		String cookie = req.getSession(true).getId();
		InetAddress address = ResourceUtil.getAddress(req);
		RootFolder root;
		synchronized (roots) {
			root = roots.get(cookie);
			if (root == null) {
				// Double-check for cookie errors
				WebRender valid = ResourceUtil.matchRenderer(user, req);
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
							LOGGER.debug("Allowing browser connection without cookie match: {}: {}",
									valid.getRendererName(), address);
							break;
						}
					}
				}
			}

			if (!create || (root != null)) {
				return root;
			}

			root = new RootFolder();
			try {
				WebRender render = new WebRender(user);
				root.setDefaultRenderer(render);
				render.setRootFolder(root);
				render.associateIP(address);
				render.associatePort(req.getRemotePort());
				if (configuration.useWebSubLang()) {
					render.setSubLang(StringUtils.join(ResourceUtil.getLangs(req.getHeader("Accept-language")), ","));
				}
//				render.setUA(t.getRequestHeaders().getFirst("User-agent"));
				render.setBrowserInfo(ResourceUtil.getCookie(req, "UMSINFO"), req.getHeader("User-agent"));
				PMS.get().setRendererFound(render);
			} catch (ConfigurationException e) {
				root.setDefaultRenderer(RendererConfiguration.getDefaultConf());
			}
			// root.setDefaultRenderer(RendererConfiguration.getRendererConfigurationByName("web"));
			root.discoverChildren();
			roots.put(cookie, root);
		}
		return root;
	}
}
