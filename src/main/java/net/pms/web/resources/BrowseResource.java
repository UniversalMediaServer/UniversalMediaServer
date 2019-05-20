package net.pms.web.resources;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.configuration.WebRender;
import net.pms.dlna.CodeEnter;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.formats.Format;
import net.pms.util.UMSUtils;
import net.pms.web.model.Folder;
import net.pms.web.model.Media;
import net.pms.web.services.RootService;

@Singleton
@Path("browse")
public class BrowseResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BrowseResource.class);

	@Context
	private SecurityContext securityContext;

	private RootService roots;

	@Inject
	public BrowseResource(RootService roots) {
		this.roots = roots;
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> getFolders(@PathParam("id") String id, @QueryParam("search") String search, @Context HttpServletRequest request) throws Exception {
		LOGGER.debug("Make browse page " + id);
		String user = ResourceUtil.getUserName(securityContext);
		RootFolder root = roots.getRoot(user, true, request);

		List<Folder> folders = new ArrayList<>();
		List<Media> media = new ArrayList<>();

		Map<String, Object> result = new HashMap<>();
		result.put("media", media);
		result.put("folders", folders);

		List<DLNAResource> resources = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), search);
		if (!resources.isEmpty() && resources.get(0).getParent() != null
				&& (resources.get(0).getParent() instanceof CodeEnter)) {
			// this is a code folder the search string is entered code
			CodeEnter ce = (CodeEnter) resources.get(0).getParent();
			ce.setEnteredCode(search);
			if (!ce.validCode(ce)) {
				// invalid code throw error
				throw new IOException("Auth error");
			}
		}
		if (StringUtils.isNotEmpty(search) && !(resources instanceof CodeEnter)) {
			UMSUtils.postSearch(resources, search);
		}

		// calculate parent
		if (!resources.isEmpty() && resources.get(0).getParent() != null
				&& resources.get(0).getParent().getParent() != null
				&& resources.get(0).getParent().getParent().isFolder()) {
			String newId = resources.get(0).getParent().getParent().getResourceId();
			String idForWeb = URLEncoder.encode(newId, "UTF-8"); // TODO should be done on the client
			result.put("parentId", idForWeb);
		}

		// Calculate folders and media items
		for (DLNAResource resource : resources) {
			String newId = resource.getResourceId();
			String idForWeb = URLEncoder.encode(newId, "UTF-8"); // TODO this should be done on the client
			String name = resource.resumeName();

			if (resource instanceof VirtualVideoAction) {
				media.add(new Media(id, name, false, false, true));
			}
			else if (resource.isFolder()) {
				folders.add(new Folder(idForWeb, name));
			}
			else {
				// The resource is a media file
				media.add(new Media(idForWeb, name,
						!(WebRender.supports(resource) || resource.isResume() || resource.getType() == Format.IMAGE),
						resource.getParent() instanceof Playlist, false));
			}
		}


		return result;
	}
}
