/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.webinterfaceserver.configuration.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.ToNumberPolicy;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.CodeEnter;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.dlna.Playlist;
import net.pms.dlna.Range;
import net.pms.dlna.RealFile;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.ImagePlayer;
import net.pms.encoders.Player;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.network.HTTPResource;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.configuration.ApiHelper;
import net.pms.util.FullyPlayed;
import net.pms.util.PropertiesUtil;
import net.pms.util.SubtitleUtils;
import net.pms.util.UMSUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerApiHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Map<String, RootFolder> ROOTS = new HashMap<>();
	private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

	public static final String BASE_PATH = "/v1/api/player";

	private final DbIdResourceLocator dbIdResourceLocator = new DbIdResourceLocator();

	/**
	 * Handle API calls.
	 *
	 * @param exchange
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(exchange)) {
				exchange.close();
				return;
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(exchange, "");
			}
			var api = new ApiHelper(exchange, BASE_PATH);
			try {
				if (api.get("/")) {
					String token = createRoot(exchange, null);
					WebInterfaceServerUtil.respond(exchange, "{\"token\":\"" + token + "\"}", 200, "application/json");
				} else if (api.post("/browse")) {
					JsonObject action = WebInterfaceServerUtil.getJsonObjectFromPost(exchange);
					if (action.has("token") && action.has("id")) {
						String token = action.get("token").getAsString();
						RootFolder root = getRoot(exchange, token);
						if (root != null) {
							String id = action.get("id").getAsString();
							String search = action.has("search") ? action.get("search").getAsString() : null;
							JsonObject browse = getBrowsePage(root, id, search);
							if (browse != null) {
								WebInterfaceServerUtil.respond(exchange, browse.toString(), 200, "application/json");
							}
						}
					}
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
				} else if (api.post("/play")) {
					JsonObject action = WebInterfaceServerUtil.getJsonObjectFromPost(exchange);
					if (action.has("token") && action.has("id")) {
						String token = action.get("token").getAsString();
						RootFolder root = getRoot(exchange, token);
						if (root != null) {
							String id = action.get("id").getAsString();
							String language = action.has("language") ? action.get("language").getAsString() : "";
							JsonObject play = getPlayPage(root, id, language);
							if (play != null) {
								WebInterfaceServerUtil.respond(exchange, play.toString(), 200, "application/json");
							}
						}
					}
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
				} else if (api.getIn("/thumb/")) {
					String[] thumbData = api.getEndpoint().split("/");
					if (thumbData.length == 4) {
						RootFolder root = getRoot(exchange, thumbData[2]);
						if (root != null) {
							DLNAResource resource = root.getDLNAResource(thumbData[3], null);
							DLNAThumbnailInputStream thumb = getMediaThumbImage(resource);
							if (thumb != null) {
								Headers hdr = exchange.getResponseHeaders();
								hdr.add("Content-Type", ImageFormat.PNG.equals(thumb.getFormat()) ? HTTPResource.PNG_TYPEMIME : HTTPResource.JPEG_TYPEMIME);
								hdr.add("Accept-Ranges", "bytes");
								hdr.add("Connection", "keep-alive");
								exchange.sendResponseHeaders(200, thumb.getSize());
								OutputStream os = exchange.getResponseBody();
								WebInterfaceServerUtil.dump(thumb, os);
								return;
							}
						}
					}
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
				} else if (api.getIn("/image/")) {
					String[] imageData = api.getEndpoint().split("/");
					if (imageData.length == 4) {
						RootFolder root = getRoot(exchange, imageData[2]);
						if (root != null && sendImageMedia(exchange, root, imageData[3])) {
							return;
						}
					}
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
				} else if (api.getIn("/raw")) {
					String[] rawData = api.getEndpoint().split("/");
					if (rawData.length == 4) {
						RootFolder root = getRoot(exchange, rawData[2]);
						if (root != null && sendRawMedia(exchange, root, rawData[3], false)) {
							return;
						}
					}
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
				} else {
					LOGGER.trace("PlayerApiHandler request not available : {}", api.getEndpoint());
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in PlayerApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
			}
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in PlayerApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	static private RootFolder getRoot(HttpExchange exchange, String token) {
		synchronized (ROOTS) {
			if (ROOTS.containsKey(token)) {
				return ROOTS.get(token);
			}
		}
		if (isValidToken(token)) {
			createRoot(exchange, token);
			synchronized (ROOTS) {
				if (ROOTS.containsKey(token)) {
					return ROOTS.get(token);
				}
			}
		}
		return null;
	}

	static private boolean isValidToken(String token) {
		try {
			UUID.fromString(token);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	static private String createRoot(HttpExchange exchange, String givenToken) {
		RootFolder root = new RootFolder();
		String token = null;
		if (givenToken != null && isValidToken(givenToken)) {
			synchronized (ROOTS) {
				if (!ROOTS.containsKey(token)) {
					token = givenToken;
				} else {
					return givenToken;
				}
			}
		}
		if (token == null) {
			synchronized (ROOTS) {
				token = UUID.randomUUID().toString();
				while (ROOTS.containsKey(token)) {
					token = UUID.randomUUID().toString();
				}
			}
		}
		try {
			WebRender render = new WebRender("");
			root.setDefaultRenderer(render);
			render.setRootFolder(root);
			render.associateIP(exchange.getRemoteAddress().getAddress());
			render.associatePort(exchange.getRemoteAddress().getPort());
			if (CONFIGURATION.useWebSubLang()) {
				render.setSubLang(StringUtils.join(WebInterfaceServerUtil.getLangs(exchange), ","));
			}
			render.setBrowserInfo(WebInterfaceServerUtil.getCookie("UMSINFO", exchange), exchange.getRequestHeaders().getFirst("User-agent"));
			PMS.get().setRendererFound(render);
		} catch (ConfigurationException | InterruptedException e) {
			root.setDefaultRenderer(RendererConfiguration.getDefaultConf());
		}
		root.discoverChildren();
		synchronized (ROOTS) {
			ROOTS.put(token, root);
		}
		return token;
	}

	private JsonObject getBrowsePage(RootFolder root, String id, String search) throws IOException, InterruptedException {
		PMS.REALTIME_LOCK.lock();
		try {
			LOGGER.debug("Make browse page " + id);
			JsonObject result = new JsonObject();
			result.addProperty("goal", "browse");
			JsonArray jBreadcrumbs = new JsonArray();
			JsonArray jFolders = new JsonArray();
			JsonArray jMedias = new JsonArray();
			DLNAResource rootResource = id.equals("0") ? null : root.getDLNAResource(id, null);
			String enterSearchStringText = "Web.8";

			List<DLNAResource> resources = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), search);
			if (
				!resources.isEmpty() &&
				resources.get(0).getParent() != null &&
				(resources.get(0).getParent() instanceof CodeEnter)
			) {
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
						JsonObject jMedia = new JsonObject();
						jMedia.addProperty("id", resource.getResourceId());
						jMedia.addProperty("name", resource.resumeName());
						jMedias.add(jMedia);
					}
				}
				jBreadcrumbs = getBreadcrumbs(thisResourceFromResources);
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
					backLinkHTML.append("<a href=\"").append(backUri).append("\" title=\"").append("Web.10").append("\">");
					backLinkHTML.append("<span><i class=\"fa fa-angle-left\"></i> ").append("Web.10").append("</span>");
					backLinkHTML.append("</a>");
					folders.add(backLinkHTML.toString());
					JsonObject jFolder = new JsonObject();
					jFolder.addProperty("id", parentFromResources.getResourceId());
					jFolder.addProperty("name", "..");
					jFolders.add(jFolder);
				} else {
					folders.add("");
				}
			}
			HashMap<String, Object> mustacheVars = new HashMap<>();
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
						JsonObject jMedia = new JsonObject();
						jMedia.addProperty("id", resource.getResourceId());
						jMedia.addProperty("name", resource.resumeName());
						jMedias.add(jMedia);
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

							addMediaLibraryFolderToFrontPage(videoFolder, root, "MediaLibrary.RecentlyAdded", "Web.RecentlyAddedVideos", "hasRecentlyAdded", "recentlyAdded", mustacheVars);
							addMediaLibraryFolderToFrontPage(videoFolder, root, "VirtualFolder.1", "Web.RecentlyPlayedVideos", "hasRecentlyPlayed", "recentlyPlayed", mustacheVars);
							addMediaLibraryFolderToFrontPage(videoFolder, root, "MediaLibrary.InProgress", "Web.InProgressVideos", "hasInProgress", "inProgress", mustacheVars);
							addMediaLibraryFolderToFrontPage(videoFolder, root, "MediaLibrary.MostPlayed", "Web.MostPlayedVideos", "hasMostPlayed", "mostPlayed", mustacheVars);

							addFolderToFoldersListOnLeft = false;
						}

						if (addFolderToFoldersListOnLeft) {
							StringBuilder folderHTML = new StringBuilder();
							// The resource is a folder
							String resourceUri = "/browse/" + idForWeb;
							boolean code = (resource instanceof CodeEnter);
							if (code) {
								enterSearchStringText = "Web.9";
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
							JsonObject jFolder = new JsonObject();
							jFolder.addProperty("id", resource.getResourceId());
							jFolder.addProperty("name", resource.resumeName());
							jFolders.add(jFolder);
						}
					}
				} else {
					// The resource is a media file
					media.add(getMediaHTML(resource, idForWeb, name, thumbnailUri));
					hasFile = true;
					jMedias.add(getMediaJsonObject(resource));
				}
			}

			if (rootResource != null && rootResource instanceof MediaLibraryFolder) {
				MediaLibraryFolder folder = (MediaLibraryFolder) rootResource;
				if (
					folder.isTVSeries() &&
					CONFIGURATION.getUseCache()
				) {
					String apiMetadataAsJavaScriptVars = WebInterfaceServerUtil.getAPIMetadataAsJavaScriptVars(rootResource, "", true, root);
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

							media.add(getMediaHTML(resource, idForWeb, name, thumb));
							hasFile = true;
							jMedias.add(getMediaJsonObject(resource));
						}
					}
				}
			}

			if (CONFIGURATION.useWebControl()) {
				mustacheVars.put("push", true);
			}
			if (hasFile) {
				mustacheVars.put("folderId", id);
				mustacheVars.put("downloadFolderTooltip", "Web.DownloadFolderAsPlaylist");
			}

			DLNAResource dlna = null;
			if (id.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
				try {
					dlna = dbIdResourceLocator.locateResource(id); // id.substring(0, id.indexOf('/'))
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} else {
				dlna = root.getDLNAResource(id, null);
			}
			mustacheVars.put("name", id.equals("0") ? CONFIGURATION.getServerDisplayName() : StringEscapeUtils.escapeHtml4(dlna.getDisplayName()));
			mustacheVars.put("hasFile", hasFile);
			//mustacheVars.put("folders", folders);
			mustacheVars.put("mediaLibraryFolders", mediaLibraryFolders);
			mustacheVars.put("media", media);
			mustacheVars.put("umsversion", PropertiesUtil.getProjectProperties().get("project.version"));

			result.add("breadcrumbs", jBreadcrumbs);
			result.add("folders", jFolders);
			result.add("medias", jMedias);
			result.add("other", GSON.toJsonTree(mustacheVars).getAsJsonObject());
			return result;
		} finally {
			PMS.REALTIME_LOCK.unlock();
		}
	}


	private JsonObject getMediaJsonObject(DLNAResource resource) {
		JsonObject jMedia = new JsonObject();
		if (resource.isFolder()) {
			jMedia.addProperty("goal", "browse");
		} else {
			jMedia.addProperty("goal", "play");
		}
		jMedia.addProperty("id", resource.getResourceId());
		jMedia.addProperty("name", resource.resumeName());
		return jMedia;
	}

	private JsonArray getBreadcrumbs(DLNAResource resource) {
		JsonArray jBreadcrumbs = new JsonArray();
		JsonObject jBreadcrumb = new JsonObject();
		jBreadcrumb.addProperty("id", "");
		jBreadcrumb.addProperty("name", resource.getDisplayName());
		jBreadcrumbs.add(jBreadcrumb);
		DLNAResource thisResourceFromResources = resource;
		while (thisResourceFromResources.getParent() != null && thisResourceFromResources.getParent().isFolder()) {
			thisResourceFromResources = thisResourceFromResources.getParent();
			jBreadcrumb = new JsonObject();
			jBreadcrumb.addProperty("id", thisResourceFromResources.getResourceId());
			jBreadcrumb.addProperty("name", thisResourceFromResources.getDisplayName());
			jBreadcrumbs.add(jBreadcrumb);
		}
		JsonArray jBreadcrumbsInverted = new JsonArray();
		for (int i = jBreadcrumbs.size(); i > 0; i--) {
			jBreadcrumbsInverted.add(jBreadcrumbs.get(i - 1));
		}
		return jBreadcrumbsInverted;
	}

	private JsonObject getPlayPage(RootFolder root, String id, String language) throws IOException, InterruptedException {
		PMS.REALTIME_LOCK.lock();
		try {
			LOGGER.debug("Make play page " + id);
			JsonObject result = new JsonObject();
			result.addProperty("goal", "play");
			JsonArray jFolders = new JsonArray();
			JsonArray medias = new JsonArray();
			JsonObject media = new JsonObject();


			WebRender renderer = (WebRender) root.getDefaultRenderer();
			DLNAResource rootResource = root.getDLNAResource(id, renderer);
			if (rootResource == null) {
				LOGGER.debug("Bad web play id: " + id);
				throw new IOException("Bad Id");
			}

			if (
				rootResource.getParent() != null &&
				rootResource.getParent().isFolder()
			) {
				JsonObject jFolder = new JsonObject();
				jFolder.addProperty("id", rootResource.getParent().getResourceId());
				jFolder.addProperty("name", "..");
				jFolders.add(jFolder);
			}

			Format format = rootResource.getFormat();
			boolean isImage = format.isImage();
			boolean isVideo = format.isVideo();
			boolean isAudio = format.isAudio();

			// hack here to ensure we got a root folder to use for recently played etc.
			root.getDefaultRenderer().setRootFolder(root);
			String mime = root.getDefaultRenderer().getMimeType(rootResource);
			media.addProperty("mediaType", isVideo ? "video" : isAudio ? "audio" : isImage ? "image" : "");
			if (isVideo) {
				if (CONFIGURATION.getUseCache()) {
					String apiMetadataAsJavaScriptVars = WebInterfaceServerUtil.getAPIMetadataAsJavaScriptVars(rootResource, language, false, root);
					media.addProperty("metadatas", apiMetadataAsJavaScriptVars);
				}
				media.addProperty("isVideoWithChapters", rootResource.getMedia() != null && rootResource.getMedia().hasChapters());
				if (mime.equals(FormatConfiguration.MIMETYPE_AUTO)) {
					if (rootResource.getMedia() != null && rootResource.getMedia().getMimeType() != null) {
						mime = rootResource.getMedia().getMimeType();
					}
				}

				if (!WebInterfaceServerUtil.directmime(mime) || WebInterfaceServerUtil.transMp4(mime, rootResource.getMedia()) || rootResource.isResume()) {
					WebRender render = (WebRender) rootResource.getDefaultRenderer();
					mime = render != null ? render.getVideoMimeType() : WebInterfaceServerUtil.transMime();
				}
				if (rootResource.getMedia() != null && rootResource.getMedia().getLastPlaybackPosition() != null && rootResource.getMedia().getLastPlaybackPosition() > 0) {
					media.addProperty("resumePosition", rootResource.getMedia().getLastPlaybackPosition().intValue());
				}

			}

			// Controls whether to use the browser's native audio player
			// Audio types that are natively supported by all major browsers:
			if (isAudio) {
				media.addProperty("isNativeAudio", mime.equals(HTTPResource.AUDIO_MP3_TYPEMIME));
			}

			media.addProperty("name", rootResource.resumeName());
			media.addProperty("id", id);
			media.addProperty("autoContinue", CONFIGURATION.getWebAutoCont(format));
			media.addProperty("isDynamicPls", CONFIGURATION.isDynamicPls());
			media.add("surroundMedias", getSurroundingByType(rootResource));
			media.addProperty("useWebControl", CONFIGURATION.useWebControl());

			if (isImage) {
				// do this like this to simplify the code
				// skip all player crap since img tag works well
				int delay = CONFIGURATION.getWebImgSlideDelay() * 1000;
				if (delay > 0 && CONFIGURATION.getWebAutoCont(format)) {
					media.addProperty("delay", delay);
				}
			} else {
				media.addProperty("mime", mime);
				media.addProperty("width", renderer.getVideoWidth());
				media.addProperty("height", renderer.getVideoHeight());
			}

			if (isVideo && CONFIGURATION.getWebSubs()) {
				// only if subs are requested as <track> tags
				// otherwise we'll transcode them in
				boolean isFFmpegFontConfig = CONFIGURATION.isFFmpegFontConfig();
				if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer subs
					CONFIGURATION.setFFmpegFontConfig(false);
				}
				OutputParams p = new OutputParams(CONFIGURATION);
				p.setSid(rootResource.getMediaSubtitle());
				Player.setAudioAndSubs(rootResource, p);
				if (p.getSid() != null && p.getSid().getType().isText()) {
					try {
						File subFile = SubtitleUtils.getSubtitles(rootResource, rootResource.getMedia(), p, CONFIGURATION, SubtitleType.WEBVTT);
						LOGGER.debug("subFile " + subFile);
						if (subFile != null) {
							media.addProperty("sub", subFile.getName());
						}
					} catch (IOException e) {
						LOGGER.debug("error when doing sub file " + e);
					}
				}
				CONFIGURATION.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
			}
			medias.add(media);
			result.add("medias", medias);
			result.add("folders", jFolders);
			result.add("breadcrumbs", getBreadcrumbs(rootResource));
			return result;
		} finally {
			PMS.REALTIME_LOCK.unlock();
		}
	}

	private static JsonObject getSurroundingByType(DLNAResource resource) {
		JsonObject result = new JsonObject();
		List<DLNAResource> children = resource.getParent().getChildren();
		boolean looping = CONFIGURATION.getWebAutoLoop(resource.getFormat());
		int type = resource.getType();
		int size = children.size();
		int mod = looping ? size : 9999;
		int self = children.indexOf(resource);
		for (int step = -1; step < 2; step += 2) {
			int i = self;
			int offset = (step < 0 && looping) ? size : 0;
			DLNAResource next = null;
			while (true) {
				i = (offset + i + step) % mod;
				if (i >= size || i < 0 || i == self) {
					break; // Not found
				}
				next = children.get(i);
				if (next.getType() == type && !next.isFolder()) {
					break; // Found
				}
				next = null;
			}
			if (next != null) {
				JsonObject jMedia = new JsonObject();
				jMedia.addProperty("id", next.getResourceId());
				jMedia.addProperty("name", next.resumeName());
				result.add(step > 0 ? "next" : "prev", jMedia);
			}
		}
		return result;
	}

	private DLNAThumbnailInputStream getMediaThumbImage(DLNAResource resource) {
		if (resource == null) {
			return null;
		}
		DLNAThumbnailInputStream in;
		resource.checkThumbnail();
		try {
			in = resource.fetchThumbnailInputStream();
			if (in == null) {
				// if r is null for some reason, default to generic thumb
				in = resource.getGenericThumbnailInputStream(null);
			}
		} catch (IOException ex) {
			return null;
		}
		BufferedImageFilterChain filterChain = null;
		if (
			(
				resource instanceof RealFile &&
				FullyPlayed.isFullyPlayedFileMark(((RealFile) resource).getFile())
			) ||
			(
				resource instanceof MediaLibraryFolder &&
				((MediaLibraryFolder) resource).isTVSeries() &&
				FullyPlayed.isFullyPlayedTVSeriesMark(((MediaLibraryFolder) resource).getName())
			)
		) {
			filterChain = new BufferedImageFilterChain(FullyPlayed.getOverlayFilter());
		}
		filterChain = resource.addFlagFilters(filterChain);
		if (filterChain != null) {
			try {
				in = in.transcode(in.getDLNAImageProfile(), false, filterChain);
			} catch (IOException ex) {
			}
		}
		return in;
	}

	private boolean sendRawMedia(HttpExchange exchange, RootFolder root, String id, boolean isDownload) {
		List<DLNAResource> res;
		try {
			res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				return false;
			}
			DLNAResource dlna = res.get(0);
			long len = dlna.length();
			dlna.setPlayer(null);
			Range.Byte range = WebInterfaceServerUtil.parseRange(exchange.getRequestHeaders(), len);
			InputStream in = dlna.getInputStream(range, root.getDefaultRenderer());
			if (len == 0) {
				// For web resources actual length may be unknown until we open the stream
				len = dlna.length();
			}
			String mime = root.getDefaultRenderer().getMimeType(dlna);
			Headers hdr = exchange.getResponseHeaders();
			hdr.add("Content-Type", mime);
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Server", PMS.get().getServerName());
			hdr.add("Connection", "keep-alive");
			hdr.add("Transfer-Encoding", "chunked");
			if (isDownload) {
				hdr.add("Content-Disposition", "attachment; filename=\"" + dlna.getFileName() + "\"");
			}
			if (in != null && in.available() != len) {
				hdr.add("Content-Range", "bytes " + range.getStart() + "-" + in.available() + "/" + len);
				exchange.sendResponseHeaders(206, in.available());
			} else {
				exchange.sendResponseHeaders(200, 0);
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageSent(exchange, null, in);
			}
			OutputStream os = new BufferedOutputStream(exchange.getResponseBody(), 512 * 1024);
			LOGGER.debug("start raw dump");
			WebInterfaceServerUtil.dump(in, os);
		} catch (IOException ex) {
			return false;
		}
		return true;
	}


	private boolean sendImageMedia(HttpExchange exchange, RootFolder root, String id) {
		List<DLNAResource> res;
		try {
			res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				return false;
			}
			DLNAResource dlna = res.get(0);
			String mime;
			InputStream in;
			long len;
			Range.Byte range;
			if (dlna.getMedia() != null && dlna.getMedia().isImage() && dlna.getMedia().getImageInfo() != null) {
				boolean supported = false;
				ImageInfo imageInfo = dlna.getMedia().getImageInfo();
				if (root.getDefaultRenderer() instanceof WebRender) {
					WebRender renderer = (WebRender) root.getDefaultRenderer();
					supported = renderer.isImageFormatSupported(imageInfo.getFormat());
				}
				mime = dlna.getFormat() != null ?
					dlna.getFormat().mimeType() :
					root.getDefaultRenderer().getMimeType(dlna);

				len = supported && imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ? imageInfo.getSize() : dlna.length();

				if (supported) {
					in = dlna.getInputStream();
				} else {
					InputStream imageInputStream;
					if (dlna.getPlayer() instanceof ImagePlayer) {
						ProcessWrapper transcodeProcess = dlna.getPlayer().launchTranscode(
							dlna,
							dlna.getMedia(),
							new OutputParams(PMS.getConfiguration())
						);
						imageInputStream = transcodeProcess != null ? transcodeProcess.getInputStream(0) : null;
					} else {
						imageInputStream = dlna.getInputStream();
					}
					Image image = Image.toImage(imageInputStream, 3840, 2400, ImagesUtil.ScaleType.MAX, ImageFormat.JPEG, false);
					len = image == null ? 0 : image.getBytes(false).length;
					in = image == null ? null : new ByteArrayInputStream(image.getBytes(false));
				}
				range = new Range.Byte(0L, len);
			} else {
				return false;
			}
			Headers hdr = exchange.getResponseHeaders();
			hdr.add("Content-Type", mime);
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Server", PMS.get().getServerName());
			hdr.add("Connection", "keep-alive");
			hdr.add("Transfer-Encoding", "chunked");
			if (in != null && in.available() != len) {
				hdr.add("Content-Range", "bytes " + range.getStart() + "-" + in.available() + "/" + len);
				exchange.sendResponseHeaders(206, in.available());
			} else {
				exchange.sendResponseHeaders(200, 0);
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageSent(exchange, null, in);
			}
			OutputStream os = new BufferedOutputStream(exchange.getResponseBody(), 512 * 1024);
			WebInterfaceServerUtil.dump(in, os);
		} catch (IOException ex) {
			return false;
		}
		return true;
	}



	/**
	 * @param resource
	 * @param idForWeb
	 * @param name
	 * @param thumb
	 * @param t
	 * @param isFolder
	 * @return a set of HTML strings to display a clickable thumbnail
	 */
	private HashMap<String, String> getMediaHTML(DLNAResource resource, String idForWeb, String name, String thumb) {
		boolean upnpAllowed = false;
		String pageTypeUri = "/play/";
		if (resource.isFolder()) {
			pageTypeUri = "/browse/";
		}

		StringBuilder bumpHTML = new StringBuilder();
		HashMap<String, String> item = new HashMap<>();
		if (!resource.isFolder() && upnpAllowed) {
			if (resource.getParent() instanceof Playlist) {
				bumpHTML.append("\n<a class=\"playlist_del\" href=\"#\" onclick=\"umsAjax('/playlist/del/")
					.append(idForWeb).append("', true);return false;\" title=\"")
					.append("Web.4").append("\"></a>");
			} else {
				bumpHTML.append("\n<a class=\"playlist_add\" href=\"#\" onclick=\"umsAjax('/playlist/add/")
					.append(idForWeb).append("', false);return false;\" title=\"")
					.append("Web.5").append("\"></a>");
			}
		} else {
			// ensure that we got a string
			bumpHTML.append("");
		}

		bumpHTML.append("\n<a class=\"download\" href=\"/m3u8/").append(idForWeb).append(".m3u8\" title=\"")
			.append("Web.DownloadAsPlaylist").append("\"></a>");

		item.put("actions", bumpHTML.toString());

		if (
			resource.isFolder() ||
			resource.isResume() ||
			resource instanceof VirtualVideoAction ||
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

	private void addMediaLibraryFolderToFrontPage(
		DLNAResource videoFolder,
		RootFolder root,
		String folderNameKey,
		String headingKey,
		String hasFolderVarName,
		String childrenVarName,
		final HashMap<String, Object> mustacheVars
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

			recentlyPlayedVideosHTML.add(getMediaHTML(recentlyPlayedResource, recentlyPlayedIdForWeb, recentlyPlayedName, recentlyPlayedThumb));
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

	private static String addMediaLibraryChildToMustacheVars(
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
