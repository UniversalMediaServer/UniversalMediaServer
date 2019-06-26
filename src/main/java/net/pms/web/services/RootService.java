package net.pms.web.services;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.web.resources.ResourceUtil;

@Singleton
public class RootService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RootService.class);

	private Map<String, RootFolder> roots = new HashMap<>();

	private PmsConfiguration configuration;

	@Inject
	public RootService(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public RootFolder getRoot(String user, HttpHeaders headers, ChannelHandlerContext chc) throws InterruptedException {
		return getRoot(user, false, headers, chc);
	}

	public RootFolder getRoot(String user, boolean create, HttpHeaders headers, ChannelHandlerContext chc) throws InterruptedException {
		String userAgent = headers.getHeaderString(HttpHeaders.USER_AGENT);
		String umsInfo = headers.getCookies().get("UMSINFO") != null ? headers.getCookies().get("UMSINFO").getValue() : null;
		String cookie = "foo"; // TODO formly used sessions
		InetAddress address = ResourceUtil.getAddress(chc);

		RootFolder root;
		synchronized (roots) {
			root = roots.get(cookie);
			if (root == null) {
				// Double-check for cookie errors
				WebRender valid = ResourceUtil.matchRenderer(user, headers, chc);
				if (valid != null) {
					// A browser of the same type and user is already connected
					// at
					// this ip but for some reason we didn't get a cookie match.
					RootFolder validRoot = valid.getRootFolder();
					// Do a reverse lookup to see if it's been registered
					for (Map.Entry<String, RootFolder> entry : roots.entrySet()) {
						if (entry.getValue() == validRoot) {
							// Found
							root = validRoot;
							cookie = entry.getKey();
							LOGGER.debug("Allowing browser connection without cookie match: {}: {}", valid.getRendererName(), address);
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
				render.associatePort(((InetSocketAddress) chc.getChannel().getRemoteAddress()).getPort());
				if (configuration.useWebSubLang()) {
					String acceptLanguage = headers.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);
					render.setSubLang(StringUtils.join(ResourceUtil.getLangs(acceptLanguage), ","));
				}
				render.setUA(userAgent);
				render.setBrowserInfo(umsInfo, userAgent);
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
