package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.CodeEnter;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.formats.Format;
import net.pms.util.PropertiesUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RemoteBrowseHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBrowseHandler.class);
	private final RemoteWeb parent;
	private final static PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	public RemoteBrowseHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private final HashMap<String, Object> mustacheVars = new HashMap<>();

	/**
	 * @param resource
	 * @param idForWeb
	 * @param name
	 * @param thumb
	 * @param t
	 * @param isFolder
	 * @return a set of HTML strings to display a clickable thumbnail
	 */
	private HashMap<String, String> getMediaHTML(DLNAResource resource, String idForWeb, String name, String thumb, HttpExchange t) {
		boolean upnpAllowed = RemoteUtil.bumpAllowed(t);
		boolean upnpControl = RendererConfiguration.hasConnectedControlPlayers();
		String pageTypeUri = "/play/";
		if (resource.isFolder()) {
			pageTypeUri = "/browse/";
		}

		StringBuilder bumpHTML = new StringBuilder();
		HashMap<String, String> item = new HashMap<>();
		if (!resource.isFolder() && upnpAllowed) {
			if (upnpControl) {
				bumpHTML.append("<a class=\"bumpIcon\" href=\"javascript:bump.start('//")
					.append(parent.getAddress()).append("','/play/").append(idForWeb).append("','")
					.append(name.replace("'", "\\'")).append("')\" title=\"")
					.append(RemoteUtil.getMsgString("Web.1", t)).append("\"></a>");
			} else {
				bumpHTML.append("<a class=\"bumpIcon icondisabled\" href=\"javascript:notify('warn','")
					.append(RemoteUtil.getMsgString("Web.2", t))
					.append("')\" title=\"").append(RemoteUtil.getMsgString("Web.3", t)).append("\"></a>");
			}

			if (resource.getParent() instanceof Playlist) {
				bumpHTML.append("\n<a class=\"playlist_del\" href=\"#\" onclick=\"umsAjax('/playlist/del/")
					.append(idForWeb).append("', true);return false;\" title=\"")
					.append(RemoteUtil.getMsgString("Web.4", t)).append("\"></a>");
			} else {
				bumpHTML.append("\n<a class=\"playlist_add\" href=\"#\" onclick=\"umsAjax('/playlist/add/")
					.append(idForWeb).append("', false);return false;\" title=\"")
					.append(RemoteUtil.getMsgString("Web.5", t)).append("\"></a>");
			}
		} else {
			// ensure that we got a string
			bumpHTML.append("");
		}

		bumpHTML.append("\n<a class=\"download\" href=\"/m3u8/").append(idForWeb).append(".m3u8\" title=\"")
			.append(RemoteUtil.getMsgString("Web.DownloadAsPlaylist", t)).append("\"></a>");

		item.put("actions", bumpHTML.toString());

        if (
			resource.isFolder() ||
			resource.isResume() ||
			(
				resource.getFormat() != null &&
				(
					resource.getFormat().isVideo() ||
					resource.getFormat().isAudio() ||
					resource.getFormat().isImage()
				)
			)
		) {
			StringBuilder thumbHTML = new StringBuilder();
			thumbHTML.append("<a href=\"").append(pageTypeUri).append(idForWeb)
				.append("\" title=\"").append(name).append("\">")
				.append("<img class=\"thumb\" loading=\"lazy\" src=\"").append(thumb).append("\" alt=\"").append(name).append("\">")
				.append("</a>");
			item.put("thumb", thumbHTML.toString());

			StringBuilder captionHTML = new StringBuilder();
			captionHTML.append("<a href=\"").append(pageTypeUri).append(idForWeb)
				.append("\" title=\"").append(name).append("\">")
				.append("<span class=\"caption\">").append(name).append("</span>")
				.append("</a>");
			item.put("caption", captionHTML.toString());
		}

		return item;
	}

	private String mkBrowsePage(String id, HttpExchange t) throws IOException, InterruptedException {
		LOGGER.debug("Make browse page " + id);
		String user = RemoteUtil.userName(t);
		RootFolder root = parent.getRoot(user, true, t);
		DLNAResource rootResource = id.equals("0") ? null : root.getDLNAResource(id, null);
		String search = RemoteUtil.getQueryVars(t.getRequestURI().getQuery(), "str");

		String enterSearchStringText = RemoteUtil.getMsgString("Web.8", t);

		List<DLNAResource> resources = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), search);
		if (
			!resources.isEmpty() &&
			resources.get(0).getParent() != null &&
			(resources.get(0).getParent() instanceof CodeEnter)
		) {
			// this is a code folder the search string is  entered code
			CodeEnter ce = (CodeEnter) resources.get(0).getParent();
			ce.setEnteredCode(search);
			if (!ce.validCode(ce)) {
				// invalid code throw error
				throw new IOException("Auth error");
			}
			DLNAResource real = ce.getResource();
			if (!real.isFolder()) {
				// no folder   -> redirect
				Headers hdr = t.getResponseHeaders();
				hdr.add("Location", "/play/" + real.getId());
				RemoteUtil.respond(t, "", 302, "text/html");
				// return null here to avoid multiple responses
				return null;
			}
			// redirect to ourself
			Headers hdr = t.getResponseHeaders();
			hdr.add("Location", "/browse/" + real.getResourceId());
			RemoteUtil.respond(t, "", 302, "text/html");
			return null;
		}
		if (StringUtils.isNotEmpty(search) && !(resources instanceof CodeEnter)) {
			UMSUtils.filterResourcesByName(resources, search, false, false);
		}

		boolean hasFile = false;

		ArrayList<String> breadcrumbs = new ArrayList<>();
		ArrayList<String> folders = new ArrayList<>();

		// Will contain the direct child of Media Library, for shortcuts
		ArrayList<String> mediaLibraryFolders = new ArrayList<>();

		ArrayList<HashMap<String, String>> media = new ArrayList<>();
		StringBuilder backLinkHTML = new StringBuilder();
		Boolean isShowBreadcrumbs = false;

		if (
			!resources.isEmpty() &&
			resources.get(0).getParent() != null &&
			resources.get(0).getParent().isFolder()
		) {
			DLNAResource thisResourceFromResources = resources.get(0).getParent();
			String thisName = thisResourceFromResources.getDisplayName();
			if (thisName.equals(Messages.getString("PMS.MediaLibrary"))) {
				for (DLNAResource resource : resources) {
					String newId = resource.getResourceId();
					String idForWeb = URLEncoder.encode(newId, "UTF-8");
					StringBuilder thumbHTML = new StringBuilder();
					String name = StringEscapeUtils.escapeHtml4(resource.resumeName());
					HashMap<String, String> item = new HashMap<>();
					String faIcon;
					switch (name) {
						case "Video":
							faIcon = "fa-video";
							break;
						case "Audio":
							faIcon = "fa-music";
							break;
						case "Photo":
							faIcon = "fa-images";
							break;
						default:
							faIcon = "fa-folder";
						}
					thumbHTML.append("<a href=\"/browse/").append(idForWeb);
					thumbHTML.append("\" title=\"").append(name).append("\">");
					thumbHTML.append("<i class=\"fas ").append(faIcon).append(" fa-5x\"></i>");
					thumbHTML.append("</a>");
					item.put("thumb", thumbHTML.toString());

					StringBuilder captionHTML = new StringBuilder();
					captionHTML.append("<a href=\"/browse/").append(idForWeb);
					captionHTML.append("\" title=\"").append(name).append("\">");
					captionHTML.append("<span class=\"caption\">").append(name).append("</span>");
					captionHTML.append("</a>");

					item.put("caption", captionHTML.toString());
					item.put("actions", "<span class=\"floatRight\"></span>");
					media.add(item);
					hasFile = true;
				}
			}
			breadcrumbs.add("<li class=\"active\">" + thisName + "</li>");
			while (thisResourceFromResources.getParent() != null && thisResourceFromResources.getParent().isFolder()) {
				thisResourceFromResources = thisResourceFromResources.getParent();
				String ancestorName = thisResourceFromResources.getDisplayName().equals("root") ? Messages.getString("Web.Home") : thisResourceFromResources.getDisplayName();
				String ancestorID = thisResourceFromResources.getResourceId();
				String ancestorIDForWeb = URLEncoder.encode(ancestorID, "UTF-8");
				String ancestorUri = "/browse/" + ancestorIDForWeb;
				breadcrumbs.add(0, "<li><a href=\"" + ancestorUri + "\">" + ancestorName + "</a></li>");
				isShowBreadcrumbs = true;
			}

			if (resources.get(0).getParent().getParent() != null) {
				DLNAResource parentFromResources = resources.get(0).getParent().getParent();
				String parentID = parentFromResources.getResourceId();
				String parentIDForWeb = URLEncoder.encode(parentID, "UTF-8");
				String backUri = "/browse/" + parentIDForWeb;
				backLinkHTML.append("<a href=\"").append(backUri).append("\" title=\"").append(RemoteUtil.getMsgString("Web.10", t)).append("\">");
				backLinkHTML.append("<span><i class=\"fa fa-angle-left\"></i> ").append(RemoteUtil.getMsgString("Web.10", t)).append("</span>");
				backLinkHTML.append("</a>");
				folders.add(backLinkHTML.toString());
			} else {
				folders.add("");
			}
		}
		mustacheVars.put("isShowBreadcrumbs", isShowBreadcrumbs);
		mustacheVars.put("breadcrumbs", breadcrumbs);
		mustacheVars.put("javascriptVarsScript", "");
		mustacheVars.put("recentlyPlayed", "");
		mustacheVars.put("recentlyPlayedLink", "");
		mustacheVars.put("hasRecentlyPlayed", false);
		mustacheVars.put("inProgress", "");
		mustacheVars.put("inProgressLink", "");
		mustacheVars.put("isTVSeriesWithAPIData", false);
		mustacheVars.put("hasInProgress", false);
		mustacheVars.put("recentlyAdded", "");
		mustacheVars.put("recentlyAddedLink", "");
		mustacheVars.put("hasRecentlyAdded", false);
		mustacheVars.put("mostPlayed", "");
		mustacheVars.put("mostPlayedLink", "");
		mustacheVars.put("hasMostPlayed", false);
		mustacheVars.put("mediaLibraryFolders", "");
		mustacheVars.put("isFrontPage", false);

		// Generate innerHtml snippets for folders and media items
		for (DLNAResource resource : resources) {
			String newId = resource.getResourceId();
			String idForWeb = URLEncoder.encode(newId, "UTF-8");
			String thumbnailUri = "/thumb/" + idForWeb;
			String name = StringEscapeUtils.escapeHtml4(resource.resumeName());

			if (resource instanceof VirtualVideoAction) {
				// Let's take the VVA real early
				StringBuilder thumbHTML = new StringBuilder();
				HashMap<String, String> item = new HashMap<>();
				thumbHTML.append("<a href=\"#\" onclick=\"umsAjax('/play/").append(idForWeb)
						.append("', true);return false;\" title=\"").append(name).append("\">")
						.append("<img class=\"thumb\" loading=\"lazy\" src=\"").append(thumbnailUri).append("\" alt=\"").append(name).append("\">")
						.append("</a>");
				item.put("thumb", thumbHTML.toString());

				StringBuilder captionHTML = new StringBuilder();
				captionHTML.append("<a href=\"#\" onclick=\"umsAjax('/play/").append(idForWeb)
						.append("', true);return false;\" title=\"").append(name).append("\">")
						.append("<span class=\"caption\">").append(name).append("</span>")
						.append("</a>");
				item.put("caption", captionHTML.toString());
				item.put("actions", "<span class=\"floatRight\"></span>");
				media.add(item);
				hasFile = true;
				continue;
			}

			if (resource.isFolder()) {
				Boolean isDisplayFoldersAsThumbnails = false;
				/*
				 * Display folders as thumbnails instead of down the left side if:
				 * - The parent is TV Shows, or
				 * - This is a filtered metadata folder within TV shows, or
				 * - This is Recommendations
				 */
				if (
					resource.getParent().getDisplayName().equals(Messages.getString("VirtualFolder.4")) ||
					resource.getParent().getDisplayName().equals(Messages.getString("MediaLibrary.Recommendations")) ||
					(
						resource.getParent().getParent() != null &&
						resource.getParent().getParent().getDisplayName().equals(Messages.getString("VirtualFolder.FilterByProgress"))
					) ||
					(
						resource.getParent().getParent() != null &&
						resource.getParent().getParent().getParent() != null &&
						resource.getParent().getParent().getParent().getDisplayName().equals(Messages.getString("VirtualFolder.FilterByInformation"))
					)
				) {
					isDisplayFoldersAsThumbnails = true;
				}

				if (!isDisplayFoldersAsThumbnails || !(isDisplayFoldersAsThumbnails && (resource instanceof MediaLibraryFolder))) {
					boolean addFolderToFoldersListOnLeft = true;

					// Populate the front page
					if (id.equals("0") && resource.getName().equals(Messages.getString("PMS.MediaLibrary"))) {
						mustacheVars.put("isFrontPage", true);

						List<DLNAResource> videoSearchResults = root.getDLNAResources(resource.getId(), true, 0, 0, root.getDefaultRenderer(), Messages.getString("PMS.34"));
						UMSUtils.filterResourcesByName(videoSearchResults, Messages.getString("PMS.34"), true, true);
						DLNAResource videoFolder = videoSearchResults.get(0);

						List<DLNAResource> audioSearchResults = root.getDLNAResources(resource.getId(), true, 0, 0, root.getDefaultRenderer(), Messages.getString("PMS.1"));
						UMSUtils.filterResourcesByName(audioSearchResults, Messages.getString("PMS.1"), true, true);
						DLNAResource audioFolder = audioSearchResults.get(0);

						List<DLNAResource> imageSearchResults = root.getDLNAResources(resource.getId(), true, 0, 0, root.getDefaultRenderer(), Messages.getString("PMS.31"));
						UMSUtils.filterResourcesByName(imageSearchResults, Messages.getString("PMS.31"), true, true);
						DLNAResource imagesFolder = imageSearchResults.get(0);

						mediaLibraryFolders.add(addMediaLibraryChildToMustacheVars(videoFolder, enterSearchStringText));
						mediaLibraryFolders.add(addMediaLibraryChildToMustacheVars(audioFolder, enterSearchStringText));
						mediaLibraryFolders.add(addMediaLibraryChildToMustacheVars(imagesFolder, enterSearchStringText));

						addMediaLibraryFolderToFrontPage(videoFolder, root, "MediaLibrary.RecentlyAdded", "Web.RecentlyAddedVideos", "hasRecentlyAdded", "recentlyAdded", t);
						addMediaLibraryFolderToFrontPage(videoFolder, root, "VirtualFolder.1", "Web.RecentlyPlayedVideos", "hasRecentlyPlayed", "recentlyPlayed", t);
						addMediaLibraryFolderToFrontPage(videoFolder, root, "MediaLibrary.InProgress", "Web.InProgressVideos", "hasInProgress", "inProgress", t);
						addMediaLibraryFolderToFrontPage(videoFolder, root, "MediaLibrary.MostPlayed", "Web.MostPlayedVideos", "hasMostPlayed", "mostPlayed", t);

						addFolderToFoldersListOnLeft = false;
					}

					if (addFolderToFoldersListOnLeft) {
						StringBuilder folderHTML = new StringBuilder();
						// The resource is a folder
						String resourceUri = "/browse/" + idForWeb;
						boolean code = (resource instanceof CodeEnter);
						if (code) {
							enterSearchStringText = RemoteUtil.getMsgString("Web.9", t);
						}
						if (resource.getClass().getName().contains("SearchFolder") || code) {
							// search folder add a prompt
							// NOTE!!!
							// Yes doing getClass.getname is REALLY BAD, but this
							// is to make legacy plugins utilize this function as well
							folderHTML.append("<a href=\"javascript:void(0);\" onclick=\"searchFun('").append(resourceUri).append("','")
							.append(enterSearchStringText).append("');\" title=\"").append(name).append("\">");
						} else {
							folderHTML.append("<a href=\"").append(resourceUri).append("\" oncontextmenu=\"searchFun('").append(resourceUri)
							.append("','").append(enterSearchStringText).append("');\" title=\"").append(name).append("\">");
						}
						folderHTML.append("<div class=\"folder-thumbnail\" style=\"background-image:url(").append(thumbnailUri).append(")\"></div>");
						folderHTML.append("<span>").append(name).append("</span>");
						folderHTML.append("</a>");
						folders.add(folderHTML.toString());
					}
				}
			} else {
				// The resource is a media file
				media.add(getMediaHTML(resource, idForWeb, name, thumbnailUri, t));
				hasFile = true;
			}
		}

		if (rootResource != null && rootResource instanceof MediaLibraryFolder) {
			MediaLibraryFolder folder = (MediaLibraryFolder) rootResource;
			if (
				folder.isTVSeries() &&
				CONFIGURATION.getUseCache()
			) {
				String apiMetadataAsJavaScriptVars = RemoteUtil.getAPIMetadataAsJavaScriptVars(rootResource, t, true, root);
				if (apiMetadataAsJavaScriptVars != null) {
					mustacheVars.put("isTVSeriesWithAPIData", true);
					mustacheVars.put("javascriptVarsScript", apiMetadataAsJavaScriptVars);
				}
			}

			// Check whether this resource is expected to contain folders that display as big thumbnails
			if (
				folder.getDisplayName().equals(Messages.getString("VirtualFolder.4")) ||
				folder.getDisplayName().equals(Messages.getString("MediaLibrary.Recommendations")) ||
				(
					folder.getParent() != null &&
					folder.getParent().getDisplayName().equals(Messages.getString("VirtualFolder.FilterByProgress"))
				) ||
				(
					folder.getParent() != null &&
					folder.getParent().getParent() != null &&
					folder.getParent().getParent().getDisplayName().equals(Messages.getString("VirtualFolder.FilterByInformation"))
				)
			) {
				for (DLNAResource resource : resources) {
					if (resource instanceof MediaLibraryFolder) {
						String newId = resource.getResourceId();
						String idForWeb = URLEncoder.encode(newId, "UTF-8");
						String thumb = "/thumb/" + idForWeb;
						String name = StringEscapeUtils.escapeHtml4(resource.resumeName());

						media.add(getMediaHTML(resource, idForWeb, name, thumb, t));
						hasFile = true;
					}
				}
			}
		}

		if (CONFIGURATION.useWebControl()) {
			mustacheVars.put("push", true);
		}
		if (hasFile) {
			mustacheVars.put("folderId", id);
			mustacheVars.put("downloadFolderTooltip", RemoteUtil.getMsgString("Web.DownloadFolderAsPlaylist", t));
		}

		mustacheVars.put("name", id.equals("0") ? CONFIGURATION.getServerDisplayName() : StringEscapeUtils.escapeHtml4(root.getDLNAResource(id, null).getDisplayName()));
		mustacheVars.put("hasFile", hasFile);
		mustacheVars.put("folders", folders);
		mustacheVars.put("mediaLibraryFolders", mediaLibraryFolders);
		mustacheVars.put("media", media);
		mustacheVars.put("umsversion", PropertiesUtil.getProjectProperties().get("project.version"));

		return parent.getResources().getTemplate("browse.html").execute(mustacheVars);
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			if (RemoteUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			String id = RemoteUtil.getId("browse/", t);
			LOGGER.debug("Got a browse request found id " + id);
			String response = mkBrowsePage(id, t);
			LOGGER.trace("Browse page:\n{}", response);
			RemoteUtil.respond(t, response, 200, "text/html");
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteBrowseHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private void addMediaLibraryFolderToFrontPage(
		DLNAResource videoFolder,
		RootFolder root,
		String folderNameKey,
		String headingKey,
		String hasFolderVarName,
		String childrenVarName,
		HttpExchange t
	) throws IOException {
		int i = 0;
		List<DLNAResource> videoFolderChildren = videoFolder.getDLNAResources(videoFolder.getId(), true, 0, 0, root.getDefaultRenderer(), Messages.getString(folderNameKey));
		UMSUtils.filterResourcesByName(videoFolderChildren, Messages.getString(folderNameKey), true, true);
		if (videoFolderChildren.isEmpty()) {
			LOGGER.trace("The videoFolderChildren folder was empty after filtering for " + Messages.getString(folderNameKey));
			return;
		}
		DLNAResource recentlyPlayedFolder = videoFolderChildren.get(0);

		ArrayList<HashMap<String, String>> recentlyPlayedVideosHTML = new ArrayList<>();
		List<DLNAResource> recentlyPlayedVideos = root.getDLNAResources(recentlyPlayedFolder.getId(), true, 0, 6, root.getDefaultRenderer());

		for (DLNAResource recentlyPlayedResource : recentlyPlayedVideos) {
			String recentlyPlayedId = recentlyPlayedResource.getResourceId();
			String recentlyPlayedIdForWeb = URLEncoder.encode(recentlyPlayedId, "UTF-8");
			String recentlyPlayedThumb = "/thumb/" + recentlyPlayedIdForWeb;
			String recentlyPlayedName = StringEscapeUtils.escapeHtml4(recentlyPlayedResource.resumeName());

			// Skip the #--TRANSCODE--# entry
			if (recentlyPlayedName.equals(Messages.getString("TranscodeVirtualFolder.0"))) {
				continue;
			}

			recentlyPlayedVideosHTML.add(getMediaHTML(recentlyPlayedResource, recentlyPlayedIdForWeb, recentlyPlayedName, recentlyPlayedThumb, t));
			i++;

			if (i == 1) {
				mustacheVars.put(hasFolderVarName, true);
				StringBuilder recentlyPlayedLink = new StringBuilder();
				String recentlyPlayedFolderId = recentlyPlayedFolder.getResourceId();
				String recentlyPlayedFolderidForWeb = URLEncoder.encode(recentlyPlayedFolderId, "UTF-8");
				recentlyPlayedLink.append("<a href=\"/browse/").append(recentlyPlayedFolderidForWeb).append("\">");
				recentlyPlayedLink.append(Messages.getString(headingKey)).append(":");
				recentlyPlayedLink.append("</a>");

				String linkVarName = childrenVarName + "Link";
				mustacheVars.put(linkVarName, recentlyPlayedLink.toString());
			}
		}
		mustacheVars.put(childrenVarName, recentlyPlayedVideosHTML);
	}

	private String addMediaLibraryChildToMustacheVars(
		DLNAResource childFolder,
		String enterSearchStringText
	) throws UnsupportedEncodingException {
		String mediaLibraryChildId = childFolder.getResourceId();
		String mediaLibraryChildIdForWeb = URLEncoder.encode(mediaLibraryChildId, "UTF-8");
		String mediaLibraryChildUri = "/browse/" + mediaLibraryChildIdForWeb;
		String mediaLibraryChildThumbUri = "/thumb/" + mediaLibraryChildIdForWeb;
		String mediaLibraryChildname = StringEscapeUtils.escapeHtml4(childFolder.resumeName());
		StringBuilder mediaLibraryFolderHTML = new StringBuilder();
		mediaLibraryFolderHTML
				.append("<a href=\"").append(mediaLibraryChildUri).append("\" oncontextmenu=\"searchFun('").append(mediaLibraryChildUri)
				.append("','").append(enterSearchStringText).append("');\">")
					.append("<div class=\"folder-thumbnail\" style=\"background-image:url(").append(mediaLibraryChildThumbUri).append(")\"></div>")
					.append("<span>").append(mediaLibraryChildname).append("</span>")
				.append("</a>");
		return mediaLibraryFolderHTML.toString();
	}
}
