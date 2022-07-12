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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import net.pms.dlna.DLNAMediaChapter;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DVDISOTitle;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.dlna.Range;
import net.pms.dlna.RealFile;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.encoders.HlsHelper;
import net.pms.encoders.ImagePlayer;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.StandardPlayerId;
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
import net.pms.util.FileUtil;
import net.pms.util.FullyPlayed;
import net.pms.util.PropertiesUtil;
import net.pms.util.SubtitleUtils;
import net.pms.util.UMSUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerApiHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Map<String, RootFolder> ROOTS = new HashMap<>();

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
				} else if (api.getIn("/raw/")) {
					String[] rawData = api.getEndpoint().split("/");
					if (rawData.length == 4) {
						RootFolder root = getRoot(exchange, rawData[2]);
						if (root != null && sendRawMedia(exchange, root, rawData[3], false)) {
							return;
						}
					}
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
				} else if (api.getIn("/media/")) {
					if (!sendMedia(exchange, api)) {
						WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
					}
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
			JsonArray mediaLibraryFolders = new JsonArray();
			JsonArray jMedias = new JsonArray();
			DLNAResource rootResource = id.equals("0") ? null : root.getDLNAResource(id, null);

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
			if (
				!resources.isEmpty() &&
				resources.get(0).getParent() != null &&
				resources.get(0).getParent().isFolder()
			) {
				DLNAResource thisResourceFromResources = resources.get(0).getParent();
				String thisName = thisResourceFromResources.getDisplayName();
				if (thisName.equals(Messages.getString("MediaLibrary"))) {
					for (DLNAResource resource : resources) {
						String faIcon = switch (resource.resumeName()) {
							case "Video" -> "fa-video";
							case "Audio" -> "fa-music";
							case "Photo" -> "fa-images";
							default -> "fa-folder";
						};
						hasFile = true;
						JsonObject jMedia = new JsonObject();
						jMedia.addProperty("id", resource.getResourceId());
						jMedia.addProperty("name", resource.resumeName());
						jMedias.add(jMedia);
					}
				}
				jBreadcrumbs = getBreadcrumbs(thisResourceFromResources);

				if (resources.get(0).getParent().getParent() != null) {
					DLNAResource parentFromResources = resources.get(0).getParent().getParent();
					JsonObject jFolder = new JsonObject();
					jFolder.addProperty("id", parentFromResources.getResourceId());
					jFolder.addProperty("name", "..");
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
							List<DLNAResource> videoSearchResults = root.getDLNAResources(resource.getId(), true, 0, 0, root.getDefaultRenderer(), Messages.getString("Video"));
							UMSUtils.filterResourcesByName(videoSearchResults, Messages.getString("Video"), true, true);
							DLNAResource videoFolder = videoSearchResults.get(0);
							JsonObject mediaLibraryFolder = new JsonObject();
							mediaLibraryFolder.addProperty("id", videoFolder.getResourceId());
							mediaLibraryFolder.addProperty("name", videoFolder.resumeName());
							mediaLibraryFolders.add(mediaLibraryFolder);

							List<DLNAResource> audioSearchResults = root.getDLNAResources(resource.getId(), true, 0, 0, root.getDefaultRenderer(), Messages.getString("Audio"));
							UMSUtils.filterResourcesByName(audioSearchResults, Messages.getString("Audio"), true, true);
							DLNAResource audioFolder = audioSearchResults.get(0);
							mediaLibraryFolder = new JsonObject();
							mediaLibraryFolder.addProperty("id", audioFolder.getResourceId());
							mediaLibraryFolder.addProperty("name", audioFolder.resumeName());
							mediaLibraryFolders.add(mediaLibraryFolder);

							List<DLNAResource> imageSearchResults = root.getDLNAResources(resource.getId(), true, 0, 0, root.getDefaultRenderer(), Messages.getString("Photo"));
							UMSUtils.filterResourcesByName(imageSearchResults, Messages.getString("Photo"), true, true);
							DLNAResource imagesFolder = imageSearchResults.get(0);
							mediaLibraryFolder = new JsonObject();
							mediaLibraryFolder.addProperty("id", imagesFolder.getResourceId());
							mediaLibraryFolder.addProperty("name", imagesFolder.resumeName());
							mediaLibraryFolders.add(mediaLibraryFolder);

							result.add("recentlyAdded", getMediaLibraryFolderChilds(videoFolder, root, Messages.getString("RecentlyAdded")));
							result.add("recentlyPlayed", getMediaLibraryFolderChilds(videoFolder, root, Messages.getString("RecentlyPlayed")));
							result.add("inProgress", getMediaLibraryFolderChilds(videoFolder, root, Messages.getString("InProgress")));
							result.add("mostPlayed", getMediaLibraryFolderChilds(videoFolder, root, Messages.getString("MostPlayed")));

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

			if (rootResource != null && rootResource instanceof MediaLibraryFolder) {
				MediaLibraryFolder folder = (MediaLibraryFolder) rootResource;
				if (
					folder.isTVSeries() &&
					CONFIGURATION.getUseCache()
				) {
					String apiMetadataAsJavaScriptVars = WebInterfaceServerUtil.getAPIMetadataAsJavaScriptVars(rootResource, "", true, root);
					if (apiMetadataAsJavaScriptVars != null) {
						//this should be JsonObject
						result.addProperty("apiData", apiMetadataAsJavaScriptVars);
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
					dlna = dbIdResourceLocator.locateResource(id); // id.substring(0, id.indexOf('/'))
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} else {
				dlna = root.getDLNAResource(id, null);
			}

			result.addProperty("umsversion", PropertiesUtil.getProjectProperties().get("project.version"));
			result.addProperty("name", id.equals("0") || dlna == null ? CONFIGURATION.getServerDisplayName() : dlna.getDisplayName());
			result.addProperty("hasFile", hasFile);
			result.addProperty("webControl", CONFIGURATION.useWebControl());
			result.add("breadcrumbs", jBreadcrumbs);
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
		RootFolder root,
		String folderName
	) throws IOException {
		List<DLNAResource> videoFolderChildren = videoFolder.getDLNAResources(videoFolder.getId(), true, 0, 0, root.getDefaultRenderer(), folderName);
		UMSUtils.filterResourcesByName(videoFolderChildren, folderName, true, true);
		if (videoFolderChildren.isEmpty()) {
			LOGGER.trace("The videoFolderChildren folder was empty after filtering for " + folderName);
			return null;
		}
		JsonArray jLibraryVideos = new JsonArray();
		DLNAResource librayFolder = videoFolderChildren.get(0);
		List<DLNAResource> libraryVideos = root.getDLNAResources(librayFolder.getId(), true, 0, 6, root.getDefaultRenderer());

		for (DLNAResource libraryVideo : libraryVideos) {
			// Skip the #--TRANSCODE--# entry
			if (libraryVideo.resumeName().equals(Messages.getString("Transcode_FolderName"))) {
				continue;
			}
			jLibraryVideos.add(getMediaJsonObject(libraryVideo));
		}
		return jLibraryVideos;
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

	private static boolean sendMedia(HttpExchange exchange, ApiHelper api) {
		String[] rawData = api.getEndpoint().split("/");
		if (rawData.length < 4) {
			return false;
		}
		String sessionId = rawData[2];
		String resourceId = rawData[3];
		String uri = exchange.getRequestURI().getPath();
		RootFolder root = getRoot(exchange, sessionId);
		RendererConfiguration renderer = root.getDefaultRenderer();
		DLNAResource resource = root.getDLNAResource(resourceId, renderer);
		if (resource == null) {
			// another error
			LOGGER.debug("media unkonwn");
			return false;
		}
		DLNAMediaSubtitle sid = null;
		String mimeType = renderer.getMimeType(resource);
		WebRender render = (WebRender) renderer;
		DLNAMediaInfo media = resource.getMedia();
		if (media == null) {
			media = new DLNAMediaInfo();
			resource.setMedia(media);
		}
		if (mimeType.equals(FormatConfiguration.MIMETYPE_AUTO) && media.getMimeType() != null) {
			mimeType = media.getMimeType();
		}
		int code = 200;
		resource.setDefaultRenderer(renderer);
		if (resource.getFormat().isVideo()) {
			if (!WebInterfaceServerUtil.directmime(mimeType) || WebInterfaceServerUtil.transMp4(mimeType, media)) {
				mimeType = render.getVideoMimeType();
				// TODO: Use normal engine priorities instead of the following hacks
				if (FileUtil.isUrl(resource.getSystemName())) {
					if (FFmpegWebVideo.isYouTubeURL(resource.getSystemName())) {
						resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.YOUTUBE_DL, false, false));
					} else {
						resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_WEB_VIDEO, false, false));
					}
				} else if (!(resource instanceof DVDISOTitle)) {
					resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_VIDEO, false, false));
				}
				//code = 206;
			}
			if (
				PMS.getConfiguration().getWebSubs() &&
				resource.getMediaSubtitle() != null &&
				resource.getMediaSubtitle().isExternal()
			) {
				// fetched on the side
				sid = resource.getMediaSubtitle();
				resource.setMediaSubtitle(null);
			}
		}

		if (!WebInterfaceServerUtil.directmime(mimeType) && resource.getFormat().isAudio()) {
			resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_AUDIO, false, false));
			code = 206;
		}

		try {
			//hls part
			if (resource.getFormat().isVideo() && render != null && HTTPResource.HLS_TYPEMIME.equals(render.getVideoMimeType())) {
				Headers headers = exchange.getResponseHeaders();
				headers.add("Server", PMS.get().getServerName());
				if (uri.endsWith("/chapters.vtt")) {
					String response = DLNAMediaChapter.getWebVtt(resource);
					WebInterfaceServerUtil.respond(exchange, response, 200, HTTPResource.WEBVTT_TYPEMIME);
				} else if (uri.endsWith("/chapters.json")) {
					String response = DLNAMediaChapter.getHls(resource);
					WebInterfaceServerUtil.respond(exchange, response, 200, HTTPResource.JSON_TYPEMIME);
				} else if (rawData.length > 5 && "hls".equals(rawData[4])) {
					if (rawData[5].endsWith(".m3u8")) {
						String rendition = rawData[5];
						rendition = rendition.replace(".m3u8", "");
						String response = HlsHelper.getHLSm3u8ForRendition(resource, renderer, BASE_PATH + "/media/" + sessionId + "/", rendition);
						WebInterfaceServerUtil.respond(exchange, response, 200, HTTPResource.HLS_TYPEMIME);
					} else {
						//we need to hls stream
						InputStream in = HlsHelper.getInputStream(uri, resource, renderer);

						if (in != null) {
							headers.add("Connection", "keep-alive");
							if (uri.endsWith(".ts")) {
								headers.add("Content-Type", HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME);
							} else if (uri.endsWith(".vtt")) {
								headers.add("Content-Type", HTTPResource.WEBVTT_TYPEMIME);
							}
							OutputStream os = exchange.getResponseBody();
							exchange.sendResponseHeaders(200, 0); //chunked
							((WebRender) renderer).start(resource);
							if (LOGGER.isTraceEnabled()) {
								WebInterfaceServerUtil.logMessageSent(exchange, null, in);
							}
							WebInterfaceServerUtil.dump(in, os);
						} else {
							exchange.sendResponseHeaders(500, -1);
						}
					}
				} else {
					String response = HlsHelper.getHLSm3u8(resource, root.getDefaultRenderer(), BASE_PATH + "/media/" + sessionId + "/");
					WebInterfaceServerUtil.respond(exchange, response, 200, HTTPResource.HLS_TYPEMIME);
				}
			} else {
				media.setMimeType(mimeType);
				Range.Byte range = WebInterfaceServerUtil.parseRange(exchange.getRequestHeaders(), resource.length());
				LOGGER.debug("Sending {} with mime type {} to {}", resource, mimeType, renderer);
				InputStream in = resource.getInputStream(range, root.getDefaultRenderer());
				if (range.getEnd() == 0) {
					// For web resources actual length may be unknown until we open the stream
					range.setEnd(resource.length());
				}
				Headers headers = exchange.getResponseHeaders();
				headers.add("Content-Type", mimeType);
				headers.add("Accept-Ranges", "bytes");
				long end = range.getEnd();
				long start = range.getStart();
				String rStr = start + "-" + end + "/*";
				headers.add("Content-Range", "bytes " + rStr);
				if (start != 0) {
					code = 206;
				}

				headers.add("Server", PMS.get().getServerName());
				headers.add("Connection", "keep-alive");
				exchange.sendResponseHeaders(code, 0);
				if (LOGGER.isTraceEnabled()) {
					WebInterfaceServerUtil.logMessageSent(exchange, null, in);
				}
				OutputStream os = exchange.getResponseBody();
				if (render != null) {
					render.start(resource);
				}
				if (sid != null) {
					resource.setMediaSubtitle(sid);
				}
				WebInterfaceServerUtil.dump(in, os);
			}
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

}
