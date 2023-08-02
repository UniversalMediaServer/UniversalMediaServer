/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.webguiserver.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.List;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.dlna.ByteRange;
import net.pms.dlna.CodeEnter;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DVDISOTitle;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.dlna.RealFile;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.encoders.HlsHelper;
import net.pms.encoders.ImageEngine;
import net.pms.encoders.StandardEngineId;
import net.pms.formats.Format;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.HTTPResource;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.ServerSentEvents;
import net.pms.network.webguiserver.WebGuiServletHelper;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.WebGuiRenderer;
import net.pms.renderers.devices.players.WebGuiPlayer;
import net.pms.util.APIUtils;
import net.pms.util.FileUtil;
import net.pms.util.FullyPlayed;
import net.pms.util.PropertiesUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "PlayerApiServlet", urlPatterns = {"/v1/api/player"}, displayName = "Player Api Servlet")
public class PlayerApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerApiServlet.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/")) {
				Account account = AuthService.getPlayerAccountLoggedIn(req);
				if (account == null) {
					WebGuiServletHelper.respondUnauthorized(req, resp);
					return;
				}
				if (!account.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
					WebGuiServletHelper.respondForbidden(req, resp);
					return;
				}
				String uuid = ConnectedRenderers.getRandomUUID();
				WebGuiServletHelper.respond(req, resp, "{\"uuid\":\"" + uuid + "\"}", 200, "application/json");
			} else if (path.startsWith("/sse/")) {
				String[] sseData = path.split("/");
				if (sseData.length == 3) {
					WebGuiRenderer renderer = getRenderer(req, sseData[2]);
					if (renderer != null && renderer.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
						resp.setHeader("Server", PMS.get().getServerName());
						resp.setHeader("Connection", "keep-alive");
						resp.setHeader("Cache-Control", "no-transform");
						resp.setHeader("Charset", "UTF-8");
						resp.setContentType("text/event-stream");
						AsyncContext async = req.startAsync();
						ServerSentEvents sse = new ServerSentEvents(async, () -> {
							try {
								Thread.sleep(2000);
								renderer.updateServerSentEventsActive();
							} catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
							}
						});
						renderer.setActive(true);
						renderer.addServerSentEvents(sse);
						return;
					}
				}
				WebGuiServletHelper.respondBadRequest(req, resp);
			} else if (path.startsWith("/thumb/")) {
				String[] thumbData = path.split("/");
				if (thumbData.length == 4) {
					WebGuiRenderer renderer = getRenderer(req, thumbData[2]);
					if (renderer != null && renderer.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
						DLNAResource resource = renderer.getRootFolder().getDLNAResource(thumbData[3], null);
						AsyncContext async = req.startAsync();
						DLNAThumbnailInputStream thumb = getMediaThumbImage(resource);
						if (thumb != null) {
							resp.setContentType(ImageFormat.PNG.equals(thumb.getFormat()) ? HTTPResource.PNG_TYPEMIME : HTTPResource.JPEG_TYPEMIME);
							resp.setHeader("Accept-Ranges", "bytes");
							resp.setHeader("Connection", "keep-alive");
							resp.setStatus(200);
							resp.setContentLengthLong(thumb.getSize());
							OutputStream os = resp.getOutputStream();
							WebGuiServletHelper.copyStreamAsync(thumb, os, async);
							return;
						}
					}
				}
				WebGuiServletHelper.respondBadRequest(req, resp);
			} else if (path.startsWith("/image/")) {
				String[] imageData = path.split("/");
				if (imageData.length == 4) {
					WebGuiRenderer renderer = getRenderer(req, imageData[2]);
					if (renderer != null  && renderer.havePermission(Permissions.WEB_PLAYER_BROWSE) && sendImageMedia(req, resp, renderer, imageData[3])) {
						return;
					}
				}
				WebGuiServletHelper.respondBadRequest(req, resp);
			} else if (path.startsWith("/raw/")) {
				String[] rawData = path.split("/");
				if (rawData.length == 4) {
					WebGuiRenderer renderer = getRenderer(req, rawData[2]);
					if (renderer != null && renderer.havePermission(Permissions.WEB_PLAYER_BROWSE) && sendRawMedia(req, resp, renderer, rawData[3], false)) {
						return;
					}
				}
				WebGuiServletHelper.respondBadRequest(req, resp);
			} else if (path.startsWith("/download/")) {
				String[] rawData = path.split("/");
				if (rawData.length == 4) {
					WebGuiRenderer renderer = getRenderer(req, rawData[2]);
					if (renderer != null && renderer.havePermission(Permissions.WEB_PLAYER_DOWNLOAD) && sendDownloadMedia(req, resp, renderer, rawData[3])) {
						return;
					}
				}
				WebGuiServletHelper.respondBadRequest(req, resp);
			} else if (path.startsWith("/media/")) {
				if (!sendMedia(req, resp, path)) {
					WebGuiServletHelper.respondBadRequest(req, resp);
				}
			} else {
				LOGGER.trace("PlayerApiServlet request not available : {}", path);
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in PlayerApiServlet: {}", e.getMessage());
			LOGGER.trace("{}", e);
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			JsonObject action = WebGuiServletHelper.getJsonObjectFromBody(req);
			if (!action.has("uuid")) {
				WebGuiServletHelper.respondUnauthorized(req, resp);
				return;
			}
			String uuid = action.get("uuid").getAsString();
			WebGuiRenderer renderer = getRenderer(req, uuid);
			if (renderer == null || !renderer.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
				WebGuiServletHelper.respondForbidden(req, resp);
				return;
			}
			renderer.setActive(true);
			var path = req.getPathInfo();
			switch (path) {
				case "/browse" -> {
					if (action.has("id")) {
						String id = action.get("id").getAsString();
						String search = action.has("search") ? action.get("search").getAsString() : null;
						String lang = action.has("lang") ? action.get("lang").getAsString() : null;
						JsonObject browse = getBrowsePage(renderer, id, search, lang);
						if (browse != null) {
							WebGuiServletHelper.respond(req, resp, browse.toString(), 200, "application/json");
							return;
						}
					}
					WebGuiServletHelper.respondBadRequest(req, resp);
				}
				case "/logout" -> {
					ConnectedRenderers.removeWebPlayerRenderer(uuid);
					WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
				}
				case "/play" -> {
					if (action.has("id")) {
						String id = action.get("id").getAsString();
						String lang = action.has("lang") ? action.get("lang").getAsString() : null;
						JsonObject play = getPlayPage(renderer, id, lang);
						if (play != null) {
							WebGuiServletHelper.respond(req, resp, play.toString(), 200, "application/json");
							return;
						}
					}
					WebGuiServletHelper.respondBadRequest(req, resp);
				}
				case "/show" -> {
					if (action.has("id")) {
						String id = action.get("id").getAsString();
						String lang = action.has("lang") ? action.get("lang").getAsString() : null;
						JsonObject show = getShowPage(renderer, id, lang);
						if (show != null) {
							WebGuiServletHelper.respond(req, resp, show.toString(), 200, "application/json");
							return;
						}
					}
					WebGuiServletHelper.respondBadRequest(req, resp);
				}
				case "/status" -> {
					if (action.has("uuid")) {
						((WebGuiPlayer) renderer.getPlayer()).setDataFromJson(action.toString());
						WebGuiServletHelper.respond(req, resp, "", 200, "application/json");
					} else {
						WebGuiServletHelper.respondBadRequest(req, resp);
					}
				}
				default -> {
					LOGGER.trace("PlayerApiServlet request not available : {}", path);
					WebGuiServletHelper.respondNotFound(req, resp);
				}

			}
		} catch (RuntimeException | InterruptedException e) {
			LOGGER.error("Exception in PlayerApiServlet: {}", e.getMessage());
			LOGGER.trace("{}", e);
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	private static WebGuiRenderer getRenderer(HttpServletRequest req, String uuid) {
		if (ConnectedRenderers.hasWebPlayerRenderer(uuid)) {
			return ConnectedRenderers.getWebPlayerRenderer(uuid);
		}
		if (ConnectedRenderers.isValidUUID(uuid)) {
			createRenderer(req, uuid);
			if (ConnectedRenderers.hasWebPlayerRenderer(uuid)) {
				return ConnectedRenderers.getWebPlayerRenderer(uuid);
			}
		}
		return null;
	}

	private static void createRenderer(HttpServletRequest req, String uuid) {
		Account account = AuthService.getPlayerAccountLoggedIn(req);
		if (account == null || !account.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
			return;
		}
		try {
			int userId = account.getUser().getId();
			String userAgent = req.getHeader("User-agent");
			String langs = WebGuiServletHelper.getLangs(req);
			WebGuiRenderer renderer = new WebGuiRenderer(uuid, userId, userAgent, langs);
			renderer.associateIP(WebGuiServletHelper.getInetAddress(req.getRemoteAddr()));
			renderer.setActive(true);
			renderer.getRootFolder().discoverChildren();
			ConnectedRenderers.addWebPlayerRenderer(renderer);
		} catch (ConfigurationException | InterruptedException ex) {
			LOGGER.info("Error in loading configuration of WebPlayerRenderer");
		}
	}

	private JsonObject getBrowsePage(Renderer renderer, String id, String search, String lang) throws IOException, InterruptedException {
		PMS.REALTIME_LOCK.lockInterruptibly();
		try {
			LOGGER.debug("Make browse page " + id);
			JsonObject result = new JsonObject();
			result.addProperty("goal", "browse");
			JsonArray jBreadcrumbs = new JsonArray();
			JsonArray jFolders = new JsonArray();
			JsonArray mediaLibraryFolders = new JsonArray();
			JsonArray jMedias = new JsonArray();
			DLNAResource rootResource = id.equals("0") ? null : renderer.getRootFolder().getDLNAResource(id, null);

			List<DLNAResource> resources = renderer.getRootFolder().getDLNAResources(id, true, 0, 0, renderer, search);
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
			if (
				!resources.isEmpty() &&
				resources.get(0).getParent() != null &&
				resources.get(0).getParent().isFolder()
			) {
				DLNAResource thisResourceFromResources = resources.get(0).getParent();
				String thisName = thisResourceFromResources.getDisplayName();
				if (thisName.equals(Messages.getString("MediaLibrary"))) {
					for (DLNAResource resource : resources) {
						String icon = switch (resource.resumeName()) {
							case "Video" -> "video";
							case "Audio" -> "audio";
							case "Photo" -> "image";
							default -> "folder";
						};
						hasFile = true;
						JsonObject jMedia = new JsonObject();
						jMedia.addProperty("id", resource.getResourceId());
						jMedia.addProperty("name", resource.resumeName());
						jMedia.addProperty("icon", icon);
						jMedias.add(jMedia);
					}
				}
				jBreadcrumbs = getBreadcrumbs(thisResourceFromResources);

				if (resources.get(0).getParent().getParent() != null) {
					DLNAResource parentFromResources = resources.get(0).getParent().getParent();
					JsonObject jFolder = new JsonObject();
					jFolder.addProperty("id", parentFromResources.getResourceId());
					jFolder.addProperty("name", "..");
					jFolder.addProperty("icon", "back");
					jFolders.add(jFolder);
				}
			}
			if (resources.isEmpty() && rootResource != null && rootResource.isFolder()) {
				jBreadcrumbs = getBreadcrumbs(rootResource);
				if (rootResource.getParent() != null) {
					DLNAResource parentFromResources = rootResource.getParent();
					JsonObject jFolder = new JsonObject();
					jFolder.addProperty("id", parentFromResources.getResourceId());
					jFolder.addProperty("name", "..");
					jFolder.addProperty("icon", "back");
					jFolders.add(jFolder);
				}
			}

			// Generate innerHtml snippets for folders and media items
			for (DLNAResource resource : resources) {
				if (resource == null) {
					continue;
				}
				if (resource instanceof VirtualVideoAction) {
					// Let's take the VVA real early
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
						resource.getParent().getDisplayName().equals(Messages.getString("TvShows")) ||
						resource.getParent().getDisplayName().equals(Messages.getString("Recommendations")) ||
						(
							resource.getParent().getParent() != null &&
							resource.getParent().getParent().getDisplayName().equals(Messages.getString("FilterByProgress"))
						) ||
						(
							resource.getParent().getParent() != null &&
							resource.getParent().getParent().getParent() != null &&
							resource.getParent().getParent().getParent().getDisplayName().equals(Messages.getString("FilterByInformation"))
						)
					) {
						isDisplayFoldersAsThumbnails = true;
					}

					if (!isDisplayFoldersAsThumbnails || !(isDisplayFoldersAsThumbnails && (resource instanceof MediaLibraryFolder))) {
						boolean addFolderToFoldersListOnLeft = true;

						// Populate the front page
						if (id.equals("0") && resource.getName().equals(Messages.getString("MediaLibrary"))) {
							List<DLNAResource> videoSearchResults = renderer.getRootFolder().getDLNAResources(resource.getId(), true, 0, 0, renderer, Messages.getString("Video"));
							UMSUtils.filterResourcesByName(videoSearchResults, Messages.getString("Video"), true, true);
							DLNAResource videoFolder = videoSearchResults.get(0);
							JsonObject mediaLibraryFolder = new JsonObject();
							mediaLibraryFolder.addProperty("id", videoFolder.getResourceId());
							mediaLibraryFolder.addProperty("name", videoFolder.resumeName());
							mediaLibraryFolder.addProperty("icon", "video");
							mediaLibraryFolders.add(mediaLibraryFolder);

							List<DLNAResource> audioSearchResults = renderer.getRootFolder().getDLNAResources(resource.getId(), true, 0, 0, renderer, Messages.getString("Audio"));
							UMSUtils.filterResourcesByName(audioSearchResults, Messages.getString("Audio"), true, true);
							DLNAResource audioFolder = audioSearchResults.get(0);
							mediaLibraryFolder = new JsonObject();
							mediaLibraryFolder.addProperty("id", audioFolder.getResourceId());
							mediaLibraryFolder.addProperty("name", audioFolder.resumeName());
							mediaLibraryFolder.addProperty("icon", "audio");
							mediaLibraryFolders.add(mediaLibraryFolder);

							List<DLNAResource> imageSearchResults = renderer.getRootFolder().getDLNAResources(resource.getId(), true, 0, 0, renderer, Messages.getString("Photo"));
							UMSUtils.filterResourcesByName(imageSearchResults, Messages.getString("Photo"), true, true);
							DLNAResource imagesFolder = imageSearchResults.get(0);
							mediaLibraryFolder = new JsonObject();
							mediaLibraryFolder.addProperty("id", imagesFolder.getResourceId());
							mediaLibraryFolder.addProperty("name", imagesFolder.resumeName());
							mediaLibraryFolder.addProperty("icon", "image");
							mediaLibraryFolders.add(mediaLibraryFolder);

							JsonObject jMediasSelections = new JsonObject();
							jMediasSelections.add("recentlyAdded", getMediaLibraryFolderChilds(videoFolder, renderer, Messages.getString("RecentlyAdded")));
							jMediasSelections.add("recentlyPlayed", getMediaLibraryFolderChilds(videoFolder, renderer, Messages.getString("RecentlyPlayed")));
							jMediasSelections.add("inProgress", getMediaLibraryFolderChilds(videoFolder, renderer, Messages.getString("InProgress")));
							jMediasSelections.add("mostPlayed", getMediaLibraryFolderChilds(videoFolder, renderer, Messages.getString("MostPlayed")));
							result.add("mediasSelections", jMediasSelections);

							addFolderToFoldersListOnLeft = false;
						}

						if (addFolderToFoldersListOnLeft) {
							// The resource is a folder
							JsonObject jFolder = new JsonObject();
							jFolder.addProperty("id", resource.getResourceId());
							jFolder.addProperty("name", resource.resumeName());
							jFolders.add(jFolder);
						}
					}
				} else {
					// The resource is a media file
					hasFile = true;
					jMedias.add(getMediaJsonObject(resource));
				}
			}

			if (rootResource instanceof MediaLibraryFolder folder) {
				if (
					folder.isTVSeries() &&
					CONFIGURATION.getUseCache()
				) {
					JsonObject metadata = getMetadataAsJsonObject(rootResource, true, renderer, lang);
					if (metadata != null) {
						result.add("metadata", metadata);
					}
				}

				// Check whether this resource is expected to contain folders that display as big thumbnails
				if (
					folder.getDisplayName().equals(Messages.getString("TvShows")) ||
					folder.getDisplayName().equals(Messages.getString("Recommendations")) ||
					(
						folder.getParent() != null &&
						folder.getParent().getDisplayName().equals(Messages.getString("FilterByProgress"))
					) ||
					(
						folder.getParent() != null &&
						folder.getParent().getParent() != null &&
						folder.getParent().getParent().getDisplayName().equals(Messages.getString("FilterByInformation"))
					)
				) {
					for (DLNAResource resource : resources) {
						if (resource instanceof MediaLibraryFolder) {
							hasFile = true;
							jMedias.add(getMediaJsonObject(resource));
						}
					}
				}
			}

			DLNAResource dlna = null;
			if (id.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
				try {
					dlna = DbIdResourceLocator.locateResource(id, renderer); // id.substring(0, id.indexOf('/'))
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} else {
				dlna = renderer.getRootFolder().getDLNAResource(id, null);
			}

			result.addProperty("umsversion", PropertiesUtil.getProjectProperties().get("project.version"));
			result.addProperty("name", id.equals("0") || dlna == null ? CONFIGURATION.getServerDisplayName() : dlna.getDisplayName());
			result.addProperty("hasFile", hasFile);
			result.addProperty("useWebControl", CONFIGURATION.useWebPlayerControls());
			result.add("breadcrumbs", jBreadcrumbs);
			result.add("mediaLibraryFolders", mediaLibraryFolders);
			result.add("folders", jFolders);
			result.add("medias", jMedias);
			return result;
		} finally {
			PMS.REALTIME_LOCK.unlock();
		}
	}

	private JsonObject getMediaJsonObject(DLNAResource resource) {
		JsonObject jMedia = new JsonObject();
		if (resource.isFolder()) {
			jMedia.addProperty("goal", "browse");
		} else if (resource.getFormat() != null && resource.getFormat().isVideo()) {
			jMedia.addProperty("goal", "show");
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

	private JsonArray getMediaLibraryFolderChilds(
		DLNAResource videoFolder,
		Renderer renderer,
		String folderName
	) throws IOException {
		List<DLNAResource> videoFolderChildren = videoFolder.getDLNAResources(videoFolder.getId(), true, 0, 0, renderer, folderName);
		UMSUtils.filterResourcesByName(videoFolderChildren, folderName, true, true);
		if (videoFolderChildren.isEmpty()) {
			LOGGER.trace("The videoFolderChildren folder was empty after filtering for " + folderName);
			return null;
		}
		JsonArray jLibraryVideos = new JsonArray();
		DLNAResource librayFolder = videoFolderChildren.get(0);
		List<DLNAResource> libraryVideos = renderer.getRootFolder().getDLNAResources(librayFolder.getId(), true, 0, 6, renderer);

		for (DLNAResource libraryVideo : libraryVideos) {
			// Skip the #--TRANSCODE--# entry
			if (libraryVideo.resumeName().equals(Messages.getString("Transcode_FolderName"))) {
				continue;
			}
			jLibraryVideos.add(getMediaJsonObject(libraryVideo));
		}
		return jLibraryVideos;
	}

	private JsonObject getShowPage(WebGuiRenderer renderer, String id, String lang) throws IOException, InterruptedException {
		JsonObject result = getPlayPage(renderer, id, lang);
		if (result != null) {
			result.remove("goal");
			result.addProperty("goal", "show");
		}
		return result;
	}

	private JsonObject getPlayPage(WebGuiRenderer renderer, String id, String lang) throws IOException, InterruptedException {
		PMS.REALTIME_LOCK.lockInterruptibly();
		try {
			LOGGER.debug("Make play page " + id);
			JsonObject result = new JsonObject();
			result.addProperty("goal", "play");
			JsonArray jFolders = new JsonArray();
			JsonArray medias = new JsonArray();
			JsonObject media = new JsonObject();

			DLNAResource rootResource = renderer.getRootFolder().getDLNAResource(id, renderer);
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

			String mime = renderer.getMimeType(rootResource);
			media.addProperty("mediaType", isVideo ? "video" : isAudio ? "audio" : isImage ? "image" : "");
			if (isVideo) {
				if (CONFIGURATION.getUseCache()) {
					JsonObject metadata = getMetadataAsJsonObject(rootResource, false, renderer, lang);
					media.add("metadata", metadata);
				}
				media.addProperty("isVideoWithChapters", rootResource.getMedia() != null && rootResource.getMedia().hasChapters());
				if (mime.equals(FormatConfiguration.MIMETYPE_AUTO)) {
					if (rootResource.getMedia() != null && rootResource.getMedia().getMimeType() != null) {
						mime = rootResource.getMedia().getMimeType();
					}
				}

				if (!directmime(mime) || transMp4(mime, rootResource.getMedia()) || rootResource.isResume()) {
					mime = renderer.getVideoMimeType();
				}
				if (rootResource.getMediaStatus() != null && rootResource.getMediaStatus().getLastPlaybackPosition() != null && rootResource.getMediaStatus().getLastPlaybackPosition() > 0) {
					media.addProperty("resumePosition", rootResource.getMediaStatus().getLastPlaybackPosition().intValue());
				}
			}

			// Controls whether to use the browser's native audio player
			// Audio types that are natively supported by all major browsers:
			if (isAudio) {
				media.addProperty("isNativeAudio", mime.equals(HTTPResource.AUDIO_MP3_TYPEMIME));
			}

			media.addProperty("name", rootResource.resumeName());
			media.addProperty("id", id);
			media.addProperty("autoContinue", CONFIGURATION.getWebPlayerAutoCont(format));
			media.addProperty("isDynamicPls", CONFIGURATION.isDynamicPls());
			media.addProperty("isDownload", renderer.havePermission(Permissions.WEB_PLAYER_DOWNLOAD) && CONFIGURATION.useWebPlayerDownload());

			media.add("surroundMedias", getSurroundingByType(rootResource));

			if (isImage) {
				// do this like this to simplify the code
				// skip all player crap since img tag works well
				int delay = CONFIGURATION.getWebPlayerImgSlideDelay() * 1000;
				if (delay > 0 && CONFIGURATION.getWebPlayerAutoCont(format)) {
					media.addProperty("delay", delay);
				}
			} else {
				media.addProperty("mime", mime);
			}

			medias.add(media);
			result.add("medias", medias);
			result.add("folders", jFolders);
			result.add("breadcrumbs", getBreadcrumbs(rootResource));
			result.addProperty("useWebControl", CONFIGURATION.useWebPlayerControls());
			return result;
		} finally {
			PMS.REALTIME_LOCK.unlock();
		}
	}

	private static JsonObject getSurroundingByType(DLNAResource resource) {
		JsonObject result = new JsonObject();
		List<DLNAResource> children = resource.getParent().getChildren();
		boolean looping = CONFIGURATION.getWebPlayerAutoLoop(resource.getFormat());
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

	private boolean sendRawMedia(HttpServletRequest req, HttpServletResponse resp, WebGuiRenderer renderer, String id, boolean isDownload) {
		List<DLNAResource> res;
		try {
			res = renderer.getRootFolder().getDLNAResources(id, false, 0, 0, renderer);
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				return false;
			}
			DLNAResource dlna = res.get(0);
			long len = dlna.length();
			dlna.setEngine(null);
			ByteRange range = parseRange(req, len);
			AsyncContext async = req.startAsync();
			InputStream in = dlna.getInputStream(range, renderer);
			if (len == 0) {
				// For web resources actual length may be unknown until we open the stream
				len = dlna.length();
			}
			String mime = renderer.getMimeType(dlna);
			resp.setContentType(mime);
			resp.setHeader("Accept-Ranges", "bytes");
			resp.setHeader("Server", PMS.get().getServerName());
			resp.setHeader("Connection", "keep-alive");

			if (isDownload) {
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + new File(dlna.getFileName()).getName() + "\"");
			}
			if (in != null) {
				if (in.available() != len) {
					resp.setHeader("Content-Range", "bytes " + range.getStart() + "-" + in.available() + "/" + len);
					resp.setStatus(206);
					resp.setContentLength(in.available());
				} else {
					resp.setStatus(200);
					resp.setContentLength(in.available());
				}
				if (LOGGER.isTraceEnabled()) {
					WebGuiServletHelper.logHttpServletResponse(req, resp, null, in);
				}
				OutputStream os = new BufferedOutputStream(resp.getOutputStream(), 512 * 1024);
				LOGGER.debug("start raw dump");
				WebGuiServletHelper.copyStreamAsync(in, os, async);
			} else {
				resp.setStatus(500);
				resp.setContentLength(0);
				async.complete();
			}
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	private boolean sendDownloadMedia(HttpServletRequest req, HttpServletResponse resp, WebGuiRenderer renderer, String id) {
		List<DLNAResource> res;
		try {
			res = renderer.getRootFolder().getDLNAResources(id, false, 0, 0, renderer);
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				return false;
			}
			DLNAResource dlna = res.get(0);
			File media = new File(dlna.getFileName());
			String mime = renderer.getMimeType(dlna);
			resp.setContentType(mime);
			resp.setHeader("Server", PMS.get().getServerName());
			resp.setHeader("Connection", "keep-alive");
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + media.getName() + "\"");
			resp.setStatus(200);
			resp.setContentLengthLong(media.length());
			InputStream in = dlna.getInputStream();
			if (LOGGER.isTraceEnabled()) {
				WebGuiServletHelper.logHttpServletResponse(req, resp, null, in);
			}
			AsyncContext async = req.startAsync();
			OutputStream os = resp.getOutputStream();
			WebGuiServletHelper.copyStreamAsync(in, os, async);
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	private boolean sendImageMedia(HttpServletRequest req, HttpServletResponse resp, WebGuiRenderer renderer, String id) {
		List<DLNAResource> res;
		try {
			res = renderer.getRootFolder().getDLNAResources(id, false, 0, 0, renderer);
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				return false;
			}
			DLNAResource dlna = res.get(0);
			String mime;
			InputStream in;
			long len;
			ByteRange range;
			if (dlna.getMedia() != null && dlna.getMedia().isImage() && dlna.getMedia().getImageInfo() != null) {
				ImageInfo imageInfo = dlna.getMedia().getImageInfo();
				boolean supported = renderer.isImageFormatSupported(imageInfo.getFormat());
				mime = dlna.getFormat() != null ?
					dlna.getFormat().mimeType() :
					renderer.getMimeType(dlna);

				len = supported && imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ? imageInfo.getSize() : dlna.length();

				if (supported) {
					in = dlna.getInputStream();
				} else {
					InputStream imageInputStream;
					if (dlna.getEngine() instanceof ImageEngine) {
						ProcessWrapper transcodeProcess = dlna.getEngine().launchTranscode(
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
				range = new ByteRange(0L, len);
			} else {
				return false;
			}
			AsyncContext async = req.startAsync();
			resp.setContentType(mime);
			resp.setHeader("Accept-Ranges", "bytes");
			resp.setHeader("Server", PMS.get().getServerName());
			resp.setHeader("Connection", "keep-alive");
			if (in != null) {
				if (in.available() != len) {
					resp.setHeader("Content-Range", "bytes " + range.getStart() + "-" + in.available() + "/" + len);
					resp.setStatus(206);
					resp.setContentLength(in.available());
				} else {
					resp.setStatus(200);
					resp.setContentLength(in.available());
				}
				if (LOGGER.isTraceEnabled()) {
					WebGuiServletHelper.logHttpServletResponse(req, resp, null, in);
				}
				OutputStream os = new BufferedOutputStream(resp.getOutputStream(), 512 * 1024);
				WebGuiServletHelper.copyStreamAsync(in, os, async);
			} else {
				resp.setStatus(500);
				resp.setContentLength(0);
				async.complete();
			}
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	private static boolean sendMedia(HttpServletRequest req, HttpServletResponse resp, String path) {
		String[] rawData = path.split("/");
		if (rawData.length < 4) {
			return false;
		}
		String sessionId = rawData[2];
		String resourceId = rawData[3];
		String uri = req.getRequestURI();
		WebGuiRenderer renderer = getRenderer(req, sessionId);
		if (renderer == null || !renderer.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
			return false;
		}
		DLNAResource resource = renderer.getRootFolder().getDLNAResource(resourceId, renderer);
		if (resource == null) {
			// another error
			LOGGER.debug("media unkonwn");
			return false;
		}
		MediaSubtitle sid = null;
		String mimeType = renderer.getMimeType(resource);
		MediaInfo media = resource.getMedia();
		if (media == null) {
			media = new MediaInfo();
			resource.setMedia(media);
		}
		if (mimeType.equals(FormatConfiguration.MIMETYPE_AUTO) && media.getMimeType() != null) {
			mimeType = media.getMimeType();
		}
		resource.setDefaultRenderer(renderer);
		if (resource.getFormat().isVideo()) {
			if (!directmime(mimeType) || transMp4(mimeType, media)) {
				mimeType = renderer.getVideoMimeType();
				// TODO: Use normal engine priorities instead of the following hacks
				if (FileUtil.isUrl(resource.getSystemName())) {
					if (FFmpegWebVideo.isYouTubeURL(resource.getSystemName())) {
						resource.setEngine(EngineFactory.getEngine(StandardEngineId.YOUTUBE_DL, false, false));
					} else {
						resource.setEngine(EngineFactory.getEngine(StandardEngineId.FFMPEG_WEB_VIDEO, false, false));
					}
				} else if (!(resource instanceof DVDISOTitle)) {
					resource.setEngine(EngineFactory.getEngine(StandardEngineId.FFMPEG_VIDEO, false, false));
				}
			}
			if (
				PMS.getConfiguration().getWebPlayerSubs() &&
				resource.getMediaSubtitle() != null &&
				resource.getMediaSubtitle().isExternal()
			) {
				// fetched on the side
				sid = resource.getMediaSubtitle();
				resource.setMediaSubtitle(null);
			}
		}

		if (!directmime(mimeType) && resource.getFormat().isAudio()) {
			resource.setEngine(EngineFactory.getEngine(StandardEngineId.FFMPEG_AUDIO, false, false));
		}

		try {
			//hls part
			if (resource.getFormat().isVideo() && HTTPResource.HLS_TYPEMIME.equals(renderer.getVideoMimeType())) {
				resp.setHeader("Server", PMS.get().getServerName());
				if (uri.endsWith("/chapters.vtt")) {
					String response = MediaChapter.getWebVtt(resource);
					WebGuiServletHelper.respond(req, resp, response, 200, HTTPResource.WEBVTT_TYPEMIME);
				} else if (uri.endsWith("/chapters.json")) {
					String response = MediaChapter.getHls(resource);
					WebGuiServletHelper.respond(req, resp, response, 200, HTTPResource.JSON_TYPEMIME);
				} else if (rawData.length > 5 && "hls".equals(rawData[4])) {
					if (rawData[5].endsWith(".m3u8")) {
						String rendition = rawData[5];
						rendition = rendition.replace(".m3u8", "");
						String response = HlsHelper.getHLSm3u8ForRendition(resource, renderer, req.getServletPath() + "/media/" + sessionId + "/", rendition);
						WebGuiServletHelper.respond(req, resp, response, 200, HTTPResource.HLS_TYPEMIME);
					} else {
						//we need to hls stream
						AsyncContext async = req.startAsync();
						InputStream in = HlsHelper.getInputStream(uri, resource, renderer);

						if (in != null) {
							resp.setHeader("Connection", "keep-alive");
							if (uri.endsWith(".ts")) {
								resp.setContentType(HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME);
							} else if (uri.endsWith(".vtt")) {
								resp.setContentType(HTTPResource.WEBVTT_TYPEMIME);
							}
							resp.setStatus(200);
							resp.setContentLength(in.available());
							renderer.start(resource);
							if (LOGGER.isTraceEnabled()) {
								WebGuiServletHelper.logHttpServletResponse(req, resp, null, in);
							}
							OutputStream os = resp.getOutputStream();
							WebGuiServletHelper.copyStreamAsync(in, os, async);
						} else {
							resp.setStatus(500);
							resp.setContentLength(0);
							async.complete();
						}
					}
				} else {
					String response = HlsHelper.getHLSm3u8(resource, renderer, req.getServletPath() + "/media/" + sessionId + "/");
					WebGuiServletHelper.respond(req, resp, response, 200, HTTPResource.HLS_TYPEMIME);
				}
			} else {
				AsyncContext async = req.startAsync();
				media.setMimeType(mimeType);
				ByteRange range = parseRange(req, resource.length());
				LOGGER.debug("Sending {} with mime type {} to {}", resource, mimeType, renderer);
				InputStream in = resource.getInputStream(range, renderer);
				long len = resource.length();
				boolean isTranscoding = len == MediaInfo.TRANS_SIZE;
				resp.setContentType(mimeType);
				resp.setHeader("Server", PMS.get().getServerName());
				resp.setHeader("Connection", "keep-alive");
				if (in != null) {
					if (isTranscoding) {
						resp.setHeader("Transfer-Encoding", "chunked");
						resp.setStatus(200);
					} else if (in.available() != len) {
						range.setEnd(range.getStart() + in.available());
						if (in.available() == 0) {
							len = range.getEnd() + 1;
						}
						resp.setHeader("Content-Range", "bytes " + range.getStart() + "-" + range.getEnd() + "/" + len);
						resp.setContentLength(in.available());
						resp.setStatus(206);
					} else {
						resp.setContentLength(in.available());
						resp.setStatus(200);
					}
					if (LOGGER.isTraceEnabled()) {
						WebGuiServletHelper.logHttpServletResponse(req, resp, null, in);
					}
					renderer.start(resource);
					if (sid != null) {
						resource.setMediaSubtitle(sid);
					}
					OutputStream os = new BufferedOutputStream(resp.getOutputStream(), 512 * 1024);
					WebGuiServletHelper.copyStreamAsync(in, os, async);
				} else {
					resp.setStatus(500);
					resp.setContentLength(0);
					async.complete();
				}
			}
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Gets metadata from our database, which may be there from our API, for
	 * this resource, which could be a TV series, TV episode, or movie.
	 *
	 * @param resource
	 * @param isTVSeries whether this is a TV series, or an episode/movie
	 * @param rootFolder the root folder, used for looking up IDs
	 * @return a JsonObject to be used by a web browser which includes
	 *         metadata names and when applicable, associated IDs, or null
	 *         when there is no metadata
	 */
	private static JsonObject getMetadataAsJsonObject(DLNAResource resource, boolean isTVSeries, Renderer renderer, String lang) {
		JsonObject result = null;
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection != null) {
				if (isTVSeries) {
					String simplifiedTitle = resource.getDisplayName() != null ? FileUtil.getSimplifiedShowName(resource.getDisplayName()) : resource.getName();
					result = MediaTableTVSeries.getTvSeriesMetadataAsJsonObject(connection, simplifiedTitle, lang);
				} else {
					result = MediaTableVideoMetadata.getVideoMetadataAsJsonObject(connection, resource.getFileName(), lang);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while getting metadata for web interface");
			LOGGER.debug("", e);
		}
		if (result == null) {
			return null;
		}
		DLNAResource actorsFolder = null;
		DLNAResource countriesFolder = null;
		DLNAResource directorsFolder = null;
		DLNAResource genresFolder = null;
		DLNAResource ratedFolder = null;
		if (CONFIGURATION.isShowMediaLibraryFolder()) {
			// prepare to get IDs of certain metadata resources, to make them clickable
			List<DLNAResource> rootFolderChildren = renderer.getRootFolder().getDLNAResources("0", true, 0, 0, renderer, Messages.getString("MediaLibrary"));
			UMSUtils.filterResourcesByName(rootFolderChildren, Messages.getString("MediaLibrary"), true, true);
			if (rootFolderChildren.isEmpty()) {
				return null;
			}
			DLNAResource mediaLibraryFolder = rootFolderChildren.get(0);
			List<DLNAResource> mediaLibraryChildren = mediaLibraryFolder.getDLNAResources(mediaLibraryFolder.getId(), true, 0, 0, renderer, Messages.getString("Video"));
			UMSUtils.filterResourcesByName(mediaLibraryChildren, Messages.getString("Video"), true, true);
			DLNAResource videoFolder = mediaLibraryChildren.get(0);

			boolean isRelatedToTV = isTVSeries || resource.isEpisodeWithinSeasonFolder() || resource.isEpisodeWithinTVSeriesFolder();
			String folderName = isRelatedToTV ? Messages.getString("TvShows") : Messages.getString("Movies");
			List<DLNAResource> videoFolderChildren = videoFolder.getDLNAResources(videoFolder.getId(), true, 0, 0, renderer, folderName);
			UMSUtils.filterResourcesByName(videoFolderChildren, folderName, true, true);
			DLNAResource tvShowsOrMoviesFolder = videoFolderChildren.get(0);

			List<DLNAResource> tvShowsOrMoviesChildren = tvShowsOrMoviesFolder.getDLNAResources(tvShowsOrMoviesFolder.getId(), true, 0, 0, renderer, Messages.getString("FilterByInformation"));
			UMSUtils.filterResourcesByName(tvShowsOrMoviesChildren, Messages.getString("FilterByInformation"), true, true);
			DLNAResource filterByInformationFolder = tvShowsOrMoviesChildren.get(0);

			List<DLNAResource> filterByInformationChildren = filterByInformationFolder.getDLNAResources(filterByInformationFolder.getId(), true, 0, 0, renderer, Messages.getString("Genres"));

			for (int filterByInformationChildrenIterator = 0; filterByInformationChildrenIterator < filterByInformationChildren.size(); filterByInformationChildrenIterator++) {
				DLNAResource filterByInformationChild = filterByInformationChildren.get(filterByInformationChildrenIterator);
				if (filterByInformationChild.getDisplayName().equals(Messages.getString("Actors"))) {
					actorsFolder = filterByInformationChild;
				} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Country"))) {
					countriesFolder = filterByInformationChild;
				} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Director"))) {
					directorsFolder = filterByInformationChild;
				} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Genres"))) {
					genresFolder = filterByInformationChild;
				} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Rated"))) {
					ratedFolder = filterByInformationChild;
				}
			}
		}
		addJsonArrayDlnaIds(result, "actors", actorsFolder, renderer);
		addJsonArrayDlnaIds(result, "countries", countriesFolder, renderer);
		addJsonArrayDlnaIds(result, "directors", directorsFolder, renderer);
		addJsonArrayDlnaIds(result, "genres", genresFolder, renderer);
		addStringDlnaId(result, "rated", ratedFolder, renderer);
		result.addProperty("imageBaseURL", APIUtils.getApiImageBaseURL());

		return result;
	}

	private static void addJsonArrayDlnaIds(final JsonObject object, final String memberName, final DLNAResource folder, final Renderer renderer) {
		if (object.has(memberName)) {
			JsonElement element = object.remove(memberName);
			if (element.isJsonArray()) {
				JsonArray array = element.getAsJsonArray();
				if (!array.isEmpty()) {
					JsonArray dlnaChilds = new JsonArray();
					for (JsonElement child : array) {
						if (child.isJsonPrimitive()) {
							String value = child.getAsString();
							JsonObject dlnaChild = new JsonObject();
							dlnaChild.addProperty("name", value);
							if (folder != null) {
								List<DLNAResource> folderChildren = folder.getDLNAResources(folder.getId(), true, 0, 0, renderer, value);
								UMSUtils.filterResourcesByName(folderChildren, value, true, true);
								if (!folderChildren.isEmpty()) {
									dlnaChild.addProperty("id", folderChildren.get(0).getId());
								}
							}
							dlnaChilds.add(dlnaChild);
						}
					}
					object.add(memberName, dlnaChilds);
				}
			}
		}
	}

	private static void addStringDlnaId(final JsonObject object, final String memberName, final DLNAResource folder, final Renderer renderer) {
		if (object.has(memberName)) {
			JsonElement element = object.remove(memberName);
			if (element.isJsonPrimitive()) {
				String value = element.getAsString();
				JsonObject dlnaChild = new JsonObject();
				dlnaChild.addProperty("name", value);
				if (folder != null) {
					List<DLNAResource> folderChildren = folder.getDLNAResources(folder.getId(), true, 0, 0, renderer, value);
					UMSUtils.filterResourcesByName(folderChildren, value, true, true);
					if (!folderChildren.isEmpty()) {
						dlnaChild.addProperty("id", folderChildren.get(0).getId());
					}
				}
				object.add(memberName, dlnaChild);
			}
		}
	}

	private static ByteRange parseRange(HttpServletRequest req, long len) {
		String range = req.getHeader("Range");
		if (range == null || "".equals(range)) {
			return new ByteRange(0L, len);
		}
		String[] tmp = range.split("=")[1].split("-");
		long start = Long.parseLong(tmp[0]);
		long end = tmp.length == 1 ? len : Long.parseLong(tmp[1]);
		return new ByteRange(start, end);
	}

	/**
	 * Whether the MIME type is supported by all browsers.
	 * Note: This is a flawed approach because while browsers
	 * may support the container format, they may not support
	 * the codecs within. For example, most browsers support
	 * MP4 with H.264, but do not support it with H.265 (HEVC)
	 *
	 * @param mime
	 * @return
	 * @todo refactor to be more specific
	 */
	private static boolean directmime(String mime) {
		return mime != null &&
		(
			mime.equals(HTTPResource.MP4_TYPEMIME) ||
			mime.equals(HTTPResource.WEBM_TYPEMIME) ||
			mime.equals(HTTPResource.OGG_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_M4A_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_MP3_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_OGA_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_WAV_TYPEMIME) ||
			mime.equals(HTTPResource.BMP_TYPEMIME) ||
			mime.equals(HTTPResource.PNG_TYPEMIME) ||
			mime.equals(HTTPResource.JPEG_TYPEMIME) ||
			mime.equals(HTTPResource.GIF_TYPEMIME)
		);
	}

	private static boolean transMp4(String mime, MediaInfo media) {
		LOGGER.debug("mp4 profile " + media.getH264Profile());
		return mime.equals(HTTPResource.MP4_TYPEMIME) && (PMS.getConfiguration().isWebPlayerMp4Trans() || media.getAvcAsInt() >= 40);
	}

}
