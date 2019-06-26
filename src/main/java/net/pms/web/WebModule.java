package net.pms.web;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;
import com.google.inject.name.Names;
import net.pms.PMS;
import net.pms.configuration.IpFilter;
import net.pms.configuration.PmsConfiguration;
import net.pms.remote.RemoteUtil;
import net.pms.remote.RemoteUtil.ResourceManager;
import net.pms.web.filters.AuthenticationFilter;
import net.pms.web.resources.BrowseResource;
import net.pms.web.resources.DocResource;
import net.pms.web.resources.FileResource;
import net.pms.web.resources.PlayListResource;
import net.pms.web.resources.PlayResource;
import net.pms.web.resources.PlayerControlResource;
import net.pms.web.resources.PlayerStatusResource;
import net.pms.web.resources.PollResource;
import net.pms.web.resources.RawResource;
import net.pms.web.resources.RemoteFlashMediaResource;
import net.pms.web.resources.RemoteMediaResource;
import net.pms.web.resources.StartResource;
import net.pms.web.resources.ThumbResource;
import net.pms.web.resources.UmsResource;
import net.pms.web.resources.WebFileResource;
import net.pms.web.services.TemplateService;

public class WebModule extends RequestScopeModule {

	@Override
	protected void configure() {
		super.configure();

		final PmsConfiguration configuration = getConfiguration();
		bind(PmsConfiguration.class).toInstance(configuration);
		bind(ResourceManager.class)
				.toInstance(AccessController.doPrivileged(new PrivilegedAction<RemoteUtil.ResourceManager>() {

					@Override
					public RemoteUtil.ResourceManager run() {
						return new RemoteUtil.ResourceManager("file:" + configuration.getProfileDirectory() + "/web/",
								"jar:file:" + configuration.getProfileDirectory() + "/web.zip!/",
								"file:" + configuration.getWebPath() + "/");
					}
				}));

		if (configuration.isWebAuthenticate()) {
			bind(AuthenticationFilter.class);
		}
		bind(IpFilter.class);

		// Resources
		bind(BrowseResource.class);
		bind(DocResource.class);
		bind(FileResource.class);
		bind(PlayerStatusResource.class);
		bind(PlayListResource.class);
		bind(PlayResource.class);
		bind(PollResource.class);
		bind(RawResource.class);
		bind(RemoteFlashMediaResource.class);
		bind(RemoteMediaResource.class);
		bind(StartResource.class);
		bind(ThumbResource.class);
		bind(UmsResource.class);
		bind(WebFileResource.class);
		bind(String.class).annotatedWith(Names.named("ums.web.root")).toInstance("/resources/web/");

		bind(TemplateService.class);

		if (!"false".equals(configuration.getBumpAddress().toLowerCase())) {
			bind(PlayerControlResource.class);
//				LOGGER.debug("Attached http player control handler to web server");
		}
	}

	private PmsConfiguration getConfiguration() {
		final PmsConfiguration configuration = PMS.getConfiguration();

		if (configuration != null) {
			return configuration;
		}
		try {
			return new PmsConfiguration();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
