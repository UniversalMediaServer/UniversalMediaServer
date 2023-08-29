/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
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
package net.pms.library;

import com.sun.jna.Platform;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.sharedcontent.FeedAudioContent;
import net.pms.configuration.sharedcontent.FeedImageContent;
import net.pms.configuration.sharedcontent.FeedVideoContent;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.configuration.sharedcontent.SharedContentWithPath;
import net.pms.configuration.sharedcontent.StreamAudioContent;
import net.pms.configuration.sharedcontent.StreamContent;
import net.pms.configuration.sharedcontent.StreamVideoContent;
import net.pms.configuration.sharedcontent.VirtualFolderContent;
import net.pms.iam.AccountService;
import net.pms.io.StreamGobbler;
import net.pms.library.virtual.CodeEnter;
import net.pms.library.virtual.DynamicPlaylist;
import net.pms.library.virtual.FileSearch;
import net.pms.library.virtual.FolderLimit;
import net.pms.library.virtual.MediaLibrary;
import net.pms.library.virtual.MediaMonitor;
import net.pms.library.virtual.Playlist;
import net.pms.library.virtual.SearchFolder;
import net.pms.library.virtual.UnattachedFolder;
import net.pms.library.virtual.UserVirtualFolder;
import net.pms.library.virtual.VirtualFile;
import net.pms.library.virtual.VirtualFolder;
import net.pms.library.virtual.VirtualFolderDbId;
import net.pms.library.virtual.VirtualVideoAction;
import net.pms.media.DbIdResourceLocator;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.util.CodeDb;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.SimpleThreadFactory;
import net.pms.util.UMSUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xmlwise.Plist;
import xmlwise.XmlParseException;

public class RootFolder extends LibraryResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(RootFolder.class);

	// A temp folder for non-xmb items
	private final UnattachedFolder tempFolder;
	private final MediaLibrary mediaLibrary;
	private DynamicPlaylist dynamicPls;
	private FolderLimit lim;
	private MediaMonitor mon;
	/**
	 * List of children objects backuped when discoverChildren.
	 */
	private final List<LibraryResource> backupChildren = new ArrayList<>();;

	public RootFolder(Renderer renderer) {
		super(renderer);
		tempFolder = new UnattachedFolder(renderer, "Temp");
		mediaLibrary = new MediaLibrary(renderer);
		setIndexId(0);
		addVirtualMyMusicFolder();
	}

	public UnattachedFolder getTemp() {
		return tempFolder;
	}

	public Playlist getDynamicPls() {
		if (dynamicPls == null) {
			dynamicPls = new DynamicPlaylist(renderer, Messages.getString("DynamicPlaylist"),
				renderer.getUmsConfiguration().getDynamicPlsSavePath(),
				(renderer.getUmsConfiguration().isDynamicPlsAutoSave() ? UMSUtils.IOList.AUTOSAVE : 0) | UMSUtils.IOList.PERMANENT);
		}
		return dynamicPls;
	}

	/**
	 * Returns the MediaLibrary.
	 *
	 * @return The current {@link MediaLibrary}.
	 */
	public MediaLibrary getLibrary() {
		return mediaLibrary;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public String getName() {
		return "root";
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public synchronized void discoverChildren() {
		if (isDiscovered()) {
			return;
		}

		//clear childrens but keep copy until discovered
		backupChildren.clear();
		for (LibraryResource libraryResource : getChildren()) {
			backupChildren.add(libraryResource);
		}
		getChildren().clear();

		if (renderer.getUmsConfiguration().isShowMediaLibraryFolder() && mediaLibrary.isEnabled()) {
			if (backupChildren.contains(mediaLibrary)) {
				addChildInternal(mediaLibrary, false);
				backupChildren.remove(mediaLibrary);
			} else {
				addChild(mediaLibrary, true);
			}
		}

		if (mon != null) {
			mon.clearChildren();
		}
		if (renderer.getUmsConfiguration().getUseCache()) {
			List<File> foldersMonitored = SharedContentConfiguration.getMonitoredFolders();
			if (!foldersMonitored.isEmpty()) {
				File[] dirs = foldersMonitored.toArray(File[]::new);
				mon = new MediaMonitor(renderer, dirs);
			}
		}

		if (
			renderer.getUmsConfiguration().getFolderLimit() &&
			renderer.isLimitFolders()
		) {
			lim = new FolderLimit(renderer);
			addChild(lim, true);
		}

		if (renderer.getUmsConfiguration().isDynamicPls()) {
			if (dynamicPls != null && backupChildren.contains(dynamicPls)) {
				addChildInternal(dynamicPls, false);
				backupChildren.remove(dynamicPls);
			} else {
				addChild(getDynamicPls(), true);
			}

			if (!renderer.getUmsConfiguration().isHideSavedPlaylistFolder()) {
				File plsdir = new File(renderer.getUmsConfiguration().getDynamicPlsSavePath());
				RealFile realFile = findRealFileInResources(backupChildren, plsdir);
				if (realFile != null) {
					addChildInternal(realFile, false);
					backupChildren.remove(realFile);
				} else {
					addChild(new RealFile(renderer, plsdir, Messages.getString("SavedPlaylists")), true);
				}
			}
		}

		setSharedContents();

		int osType = Platform.getOSType();
		if (osType == Platform.MAC) {
			if (renderer.getUmsConfiguration().isShowIphotoLibrary()) {
				LibraryResource iPhotoRes = findVirtualFolderInResources(backupChildren, "iPhoto Library");
				if (iPhotoRes != null) {
					addChildInternal(iPhotoRes, false);
					backupChildren.remove(iPhotoRes);
				} else {
					iPhotoRes = getiPhotoFolder();
					if (iPhotoRes != null) {
						addChild(iPhotoRes);
					}
				}
			}
			if (renderer.getUmsConfiguration().isShowApertureLibrary()) {
				LibraryResource apertureRes = findVirtualFolderInResources(backupChildren, "Aperture libraries");
				if (apertureRes != null) {
					addChildInternal(apertureRes, false);
					backupChildren.remove(apertureRes);
				} else {
					apertureRes = getApertureFolder();
					if (apertureRes != null) {
						addChild(apertureRes);
					}
				}
			}
		}
		if (osType == Platform.MAC || osType == Platform.WINDOWS) {
			if (renderer.getUmsConfiguration().isShowItunesLibrary()) {
				LibraryResource iTunesRes = findVirtualFolderInResources(backupChildren, "iTunes Library");
				if (iTunesRes != null) {
					addChildInternal(iTunesRes, false);
					backupChildren.remove(iTunesRes);
				} else {
					iTunesRes = getiTunesFolder();
					if (iTunesRes != null) {
						addChild(iTunesRes);
					}
				}
			}
		}

		if (renderer.getUmsConfiguration().isShowServerSettingsFolder()) {
			LibraryResource serverSettingsRes = findVirtualFolderInResources(backupChildren, Messages.getString("ServerSettings"));
			if (serverSettingsRes != null) {
				addChildInternal(serverSettingsRes, false);
				backupChildren.remove(serverSettingsRes);
			} else {
				addAdminFolder();
			}
		}

		//now remove old children from globalids
		for (LibraryResource backupChild : backupChildren) {
			renderer.getGlobalRepo().delete(backupChild);
			backupChild.clearChildren();
		}
		backupChildren.clear();
		setDiscovered(true);
	}

	private RealFile findRealFileInResources(List<LibraryResource> resources, File file) {
		if (file == null) {
			return null;
		}
		for (LibraryResource resource : resources) {
			if (resource instanceof RealFile realFile && file.equals(realFile.getFile())) {
				return realFile;
			}
		}
		return null;
	}

	private VirtualFolder findVirtualFolderInResources(List<LibraryResource> resources, String name) {
		if (name == null) {
			return null;
		}
		for (LibraryResource resource : resources) {
			if (resource instanceof VirtualFolder virtualFolder && name.equals(virtualFolder.getName())) {
				return virtualFolder;
			}
		}
		return null;
	}

	public void setFolderLim(LibraryResource r) {
		if (lim != null) {
			lim.setStart(r);
		}
	}

	private void setSharedContents() {
		List<RealFile> realFiles = new ArrayList<>();
		List<SharedContent> sharedContents = SharedContentConfiguration.getSharedContentArray();
		boolean setExternalContent = CONFIGURATION.getExternalNetwork() && renderer.getUmsConfiguration().getExternalNetwork();
		for (SharedContent sharedContent : sharedContents) {
			if (sharedContent instanceof FolderContent folder &&
				folder.getFile() != null &&
				folder.isActive() &&
				folder.isGroupAllowed(renderer.getAccountGroupId())
			) {
				RealFile realFile = findRealFileInResources(backupChildren, folder.getFile());
				if (realFile != null) {
					addChildInternal(realFile, false);
					backupChildren.remove(realFile);
				} else {
					realFile = new RealFile(renderer, folder.getFile());
					addChild(realFile, true, true);
				}
				realFiles.add(realFile);
			} else if (sharedContent instanceof VirtualFolderContent virtualFolder && virtualFolder.isActive()) {
				LibraryResource parent = getSharedContentParent(virtualFolder.getParent());
				parent.addChild(new VirtualFile(renderer, virtualFolder));
			} else if (setExternalContent && sharedContent instanceof SharedContentWithPath sharedContentWithPath && sharedContentWithPath.isExternalContent() && sharedContentWithPath.isActive()) {
				LibraryResource parent = getSharedContentParent(sharedContentWithPath.getParent());
				// Handle web playlists stream
				if (sharedContent instanceof StreamContent streamContent) {
					LibraryResource playlist = PlaylistFolder.getPlaylist(renderer, streamContent.getName(), streamContent.getUri(), streamContent.getFormat());
					if (playlist != null) {
						parent.addChild(playlist);
						continue;
					}
				}
				if (sharedContent instanceof FeedAudioContent feedAudioContent) {
					parent.addChild(new AudiosFeed(renderer, feedAudioContent.getUri()));
				} else if (sharedContent instanceof FeedImageContent feedImageContent) {
					parent.addChild(new ImagesFeed(renderer, feedImageContent.getUri()));
				} else if (sharedContent instanceof FeedVideoContent feedVideoContent) {
					parent.addChild(new VideosFeed(renderer, feedVideoContent.getUri()));
				} else if (sharedContent instanceof StreamAudioContent streamAudioContent) {
					parent.addChild(new WebAudioStream(renderer, streamAudioContent.getName(), streamAudioContent.getUri(), streamAudioContent.getThumbnail()));
				} else if (sharedContent instanceof StreamVideoContent streamVideoContent) {
					parent.addChild(new WebVideoStream(renderer, streamVideoContent.getName(), streamVideoContent.getUri(), streamVideoContent.getThumbnail()));
				}
				setLastModified(1);
			}
		}

		if (renderer.getUmsConfiguration().getSearchFolder()) {
			SearchFolder sf = new SearchFolder(renderer, Messages.getString("SearchDiscFolders"), new FileSearch(realFiles));
			addChild(sf);
		}
	}

	/**
	 * Creates, populates and returns a virtual folder mirroring the
	 * contents of the system's iPhoto folder.
	 * Mac OS X only.
	 *
	 * @return iPhotoVirtualFolder the populated <code>VirtualFolder</code>, or null if one couldn't be created.
	 */
	private LibraryResource getiPhotoFolder() {
		VirtualFolder iPhotoVirtualFolder = null;

		if (Platform.isMac()) {
			LOGGER.debug("Adding iPhoto folder");
			Process process;
			try {
				// This command will show the XML files for recently opened iPhoto databases
				process = Runtime.getRuntime().exec("defaults read com.apple.iApps iPhotoRecentDatabases");
			} catch (IOException e1) {
				LOGGER.error("Something went wrong with the iPhoto Library scan: ", e1);
				return null;
			}

			try (InputStream inputStream = process.getInputStream()) {
				List<String> lines = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
				LOGGER.debug("iPhotoRecentDatabases: {}", lines);

				if (lines.size() >= 2) {
					// we want the 2nd line
					String line = lines.get(1);

					// Remove extra spaces
					line = line.trim();

					// Remove quotes
					line = line.substring(1, line.length() - 1);

					URI uri = new URI(line);
					URL url = uri.toURL();
					File file = FileUtils.toFile(url);
					LOGGER.debug("Resolved URL to file: {} -> {}", url, file.getAbsolutePath());

					// Load the properties XML file.
					Map<String, Object> iPhotoLib = Plist.load(file);

					// The list of all photos
					Map<?, ?> photoList = (Map<?, ?>) iPhotoLib.get("Master Image List");

					// The list of events (rolls)
					List<Map<?, ?>> listOfRolls = (List<Map<?, ?>>) iPhotoLib.get("List of Rolls");

					iPhotoVirtualFolder = new VirtualFolder(renderer, "iPhoto Library", null);

					for (Map<?, ?> roll : listOfRolls) {
						Object rollName = roll.get("RollName");

						if (rollName != null) {
							VirtualFolder virtualFolder = new VirtualFolder(renderer, rollName.toString(), null);

							// List of photos in an event (roll)
							List<?> rollPhotos = (List<?>) roll.get("KeyList");

							for (Object photo : rollPhotos) {
								Map<?, ?> photoProperties = (Map<?, ?>) photoList.get(photo);

								if (photoProperties != null) {
									Object imagePath = photoProperties.get("ImagePath");

									if (imagePath != null) {
										RealFile realFile = new RealFile(renderer, new File(imagePath.toString()));
										virtualFolder.addChild(realFile);
									}
								}
							}

							iPhotoVirtualFolder.addChild(virtualFolder);
						}
					}
				} else {
					LOGGER.info("iPhoto folder not found");
				}
			} catch (XmlParseException | URISyntaxException | IOException e) {
				LOGGER.error("Something went wrong with the iPhoto Library scan: ", e);
			}
		}

		return iPhotoVirtualFolder;
	}

	/**
	 * Returns Aperture folder. Used by manageRoot, so it is usually used as
	 * a folder at the root folder. Only works when UMS is run on Mac OS X.
	 * TODO: Requirements for Aperture.
	 */
	private LibraryResource getApertureFolder() {
		VirtualFolder res = null;

		if (Platform.isMac()) {
			Process process = null;

			try {
				process = Runtime.getRuntime().exec("defaults read com.apple.iApps ApertureLibraries");
				try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					// Every line entry is one aperture library. We want all of them as a dlna folder.
					String line;
					res = new VirtualFolder(renderer, "Aperture libraries", null);

					while ((line = in.readLine()) != null) {
						if (line.startsWith("(") || line.startsWith(")")) {
							continue;
						}

						line = line.trim(); // remove extra spaces
						line = line.substring(1, line.lastIndexOf('"')); // remove quotes and spaces
						VirtualFolder apertureLibrary = createApertureDlnaLibrary(line);

						if (apertureLibrary != null) {
							res.addChild(apertureLibrary);
						}
					}
				}
			} catch (IOException | XmlParseException | URISyntaxException e) {
				LOGGER.error("Something went wrong with the aperture library scan: ", e);
			} finally {
				// Avoid zombie processes, or open stream failures
				if (process != null) {
					try {
						// The process seems to always finish, so we can wait for it.
						// If the result code is not read by parent. The process might turn into a zombie (they are real!)
						process.waitFor();
					} catch (InterruptedException e) {
						LOGGER.warn("Interrupted while waiting for stream for process");
						Thread.currentThread().interrupt();
					}

					try {
						process.getErrorStream().close();
					} catch (IOException e) {
						LOGGER.warn("Could not close process output stream: {}", e.getMessage());
						LOGGER.trace("", e);
					}

					try {
						process.getInputStream().close();
					} catch (IOException e) {
						LOGGER.warn("Could not close stream for output process", e);
					}

					try {
						process.getOutputStream().close();
					} catch (IOException e) {
						LOGGER.warn("Could not close stream for output process", e);
					}
				}
			}
		}

		return res;
	}

	private VirtualFolder createApertureDlnaLibrary(String url) throws XmlParseException, IOException, URISyntaxException {
		VirtualFolder res = null;

		if (url != null) {
			Map<String, Object> iPhotoLib;
			// every project is a album, too
			List<?> listOfAlbums;
			Map<?, ?> album;
			Map<?, ?> photoList;

			URI tURI = new URI(url);
			iPhotoLib = Plist.load(URLDecoder.decode(tURI.toURL().getFile(), System.getProperty("file.encoding"))); // loads the (nested) properties.
			photoList = (Map<?, ?>) iPhotoLib.get("Master Image List"); // the list of photos
			final Object mediaPath = iPhotoLib.get("Archive Path");
			String mediaName;

			if (mediaPath != null) {
				mediaName = mediaPath.toString();

				if (mediaName != null && mediaName.lastIndexOf('/') != -1 && mediaName.lastIndexOf(".aplibrary") != -1) {
					mediaName = mediaName.substring(mediaName.lastIndexOf('/'), mediaName.lastIndexOf(".aplibrary"));
				} else {
					mediaName = "unknown library";
				}
			} else {
				mediaName = "unknown library";
			}

			LOGGER.info("Going to parse aperture library: " + mediaName);
			res = new VirtualFolder(renderer, mediaName, null);
			listOfAlbums = (List<?>) iPhotoLib.get("List of Albums"); // the list of events (rolls)

			for (Object item : listOfAlbums) {
				album = (Map<?, ?>) item;

				if (album.get("Parent") == null) {
					VirtualFolder vAlbum = createApertureAlbum(photoList, album, listOfAlbums);
					res.addChild(vAlbum);
				}
			}
		} else {
			LOGGER.info("No Aperture library found.");
		}
		return res;
	}

	private VirtualFolder createApertureAlbum(
		Map<?, ?> photoList,
		Map<?, ?> album, List<?> listOfAlbums
	) {

		List<?> albumPhotos;
		int albumId = (Integer) album.get("AlbumId");
		VirtualFolder vAlbum = new VirtualFolder(renderer, album.get("AlbumName").toString(), null);

		for (Object item : listOfAlbums) {
			Map<?, ?> sub = (Map<?, ?>) item;

			if (sub.get("Parent") != null) {
				// recursive album creation
				int parent = (Integer) sub.get("Parent");

				if (parent == albumId) {
					VirtualFolder subAlbum = createApertureAlbum(photoList, sub, listOfAlbums);
					vAlbum.addChild(subAlbum);
				}
			}
		}

		albumPhotos = (List<?>) album.get("KeyList");

		if (albumPhotos == null) {
			return vAlbum;
		}

		boolean firstPhoto = true;

		for (Object photoKey : albumPhotos) {
			Map<?, ?> photo = (Map<?, ?>) photoList.get(photoKey);

			if (firstPhoto) {
				Object x = photoList.get("ThumbPath");

				if (x != null) {
					vAlbum.setThumbnail(x.toString());
				}

				firstPhoto = false;
			}

			RealFile file = new RealFile(renderer, new File(photo.get("ImagePath").toString()));
			vAlbum.addChild(file);
		}

		return vAlbum;
	}

	/**
	 * Returns the iTunes XML file. This file has all the information of the
	 * iTunes database. The methods used in this function depends on whether
	 * UMS runs on Mac OS X or Windows.
	 *
	 * @return (String) Absolute path to the iTunes XML file.
	 * @throws Exception
	 */
	private String getiTunesFile() throws Exception {
		String customUserPath = renderer.getUmsConfiguration().getItunesLibraryPath();

		if (!"".equals(customUserPath)) {
			return customUserPath;
		}
		return PlatformUtils.INSTANCE.getiTunesFile();
	}

	/**
	 * Returns iTunes folder. Used by manageRoot, so it is usually used as a
	 * folder at the root folder. Only works on Mac OS X or Windows.
	 *
	 * The iTunes XML is parsed fully when this method is called, so it can
	 * take some time for larger (+1000 albums) databases.
	 *
	 * This method does not support genius playlists and does not provide a
	 * media library.
	 *
	 * @see RootFolder#getiTunesFile()
	 */
	private LibraryResource getiTunesFolder() {
		LibraryResource res = null;

		if (Platform.isMac() || Platform.isWindows()) {
			Map<String, Object> iTunesLib;
			List<?> playlists;
			Map<?, ?> playlist;
			Map<?, ?> tracks;
			Map<?, ?> track;
			List<?> playlistTracks;

			try {
				String iTunesFile = getiTunesFile();

				if (iTunesFile != null && (new File(iTunesFile)).exists()) {
					iTunesLib = Plist.load(URLDecoder.decode(iTunesFile, System.getProperty("file.encoding"))); // loads the (nested) properties.
					tracks = (Map<?, ?>) iTunesLib.get("Tracks"); // the list of tracks
					playlists = (List<?>) iTunesLib.get("Playlists"); // the list of Playlists
					res = new VirtualFolder(renderer, "iTunes Library", null);

					VirtualFolder playlistsFolder = null;

					for (Object item : playlists) {
						playlist = (Map<?, ?>) item;

						if (playlist.containsKey("Visible") && playlist.get("Visible").equals(Boolean.FALSE)) {
							continue;
						}

						if (playlist.containsKey("Music") && playlist.get("Music").equals(Boolean.TRUE)) {
							// Create virtual folders for artists, albums and genres

							VirtualFolder musicFolder = new VirtualFolder(renderer, playlist.get("Name").toString(), null);
							res.addChild(musicFolder);

							VirtualFolder virtualFolderArtists = new VirtualFolder(renderer, Messages.getString("BrowseByArtist"), null);
							VirtualFolder virtualFolderAlbums = new VirtualFolder(renderer, Messages.getString("BrowseByAlbum"), null);
							VirtualFolder virtualFolderGenres = new VirtualFolder(renderer, Messages.getString("BrowseByGenre"), null);
							VirtualFolder virtualFolderAllTracks = new VirtualFolder(renderer, Messages.getString("AllAudioTracks"), null);
							playlistTracks = (List<?>) playlist.get("Playlist Items"); // list of tracks in a playlist

							String artistName;
							String albumName;
							String genreName;

							if (playlistTracks != null) {
								for (Object t : playlistTracks) {
									Map<?, ?> td = (Map<?, ?>) t;
									track = (Map<?, ?>) tracks.get(td.get("Track ID").toString());

									if (
										track != null &&
										track.get("Location") != null &&
										track.get("Location").toString().startsWith("file://")
									) {
										String name = Normalizer.normalize((String) track.get("Name"), Normalizer.Form.NFC);
										// remove dots from name to prevent media renderer from trimming
										name = name.replace('.', '-');

										if (track.containsKey("Protected") && track.get("Protected").equals(Boolean.TRUE)) {
											name = name + "-" + Messages.getString("Protected_lowercase");
										}

										boolean isCompilation = (track.containsKey("Compilation") && track.get("Compilation").equals(Boolean.TRUE));

										artistName = (String) track.get("Artist");
										if (isCompilation) {
											artistName = "Compilation";
										} else if (track.containsKey("Album Artist")) {
											artistName = (String) track.get("Album Artist");
										}
										albumName = (String) track.get("Album");
										genreName = (String) track.get("Genre");

										if (artistName == null) {
											artistName = "Unknown Artist";
										} else {
											artistName = Normalizer.normalize(artistName, Normalizer.Form.NFC);
										}

										if (albumName == null) {
											albumName = "Unknown Album";
										} else {
											albumName = Normalizer.normalize(albumName, Normalizer.Form.NFC);
										}

										if (genreName == null || "".equals(genreName.replaceAll("[^a-zA-Z]", ""))) {
											// This prevents us from adding blank or numerical genres
											genreName = "Unknown Genre";
										} else {
											genreName = Normalizer.normalize(genreName, Normalizer.Form.NFC);
										}

										// Replace &nbsp with space and then trim
										artistName = artistName.replace('\u0160', ' ').trim();
										albumName  = albumName.replace('\u0160', ' ').trim();
										genreName  = genreName.replace('\u0160', ' ').trim();

										URI tURI2 = new URI(track.get("Location").toString());
										File refFile = new File(URLDecoder.decode(tURI2.toURL().getFile(), StandardCharsets.UTF_8));
										RealFile file = new RealFile(renderer, refFile, name);

										// Put the track into the artist's album folder and the artist's "All tracks" folder
										VirtualFolder individualArtistFolder = null;
										VirtualFolder individualArtistAllTracksFolder;
										VirtualFolder individualArtistAlbumFolder = null;

										for (LibraryResource artist : virtualFolderArtists.getChildren()) {
											if (areNamesEqual(artist.getName(), artistName)) {
												individualArtistFolder = (VirtualFolder) artist;
												for (LibraryResource album : individualArtistFolder.getChildren()) {
													if (areNamesEqual(album.getName(), albumName)) {
														individualArtistAlbumFolder = (VirtualFolder) album;
													}
												}
												break;
											}
										}

										if (individualArtistFolder == null) {
											individualArtistFolder = new VirtualFolder(renderer, artistName, null);
											virtualFolderArtists.addChild(individualArtistFolder);
											individualArtistAllTracksFolder = new VirtualFolder(renderer, Messages.getString("AllAudioTracks"), null);
											individualArtistFolder.addChild(individualArtistAllTracksFolder);
										} else {
											individualArtistAllTracksFolder = (VirtualFolder) individualArtistFolder.getChildren().get(0);
										}

										if (individualArtistAlbumFolder == null) {
											individualArtistAlbumFolder = new VirtualFolder(renderer, albumName, null);
											individualArtistFolder.addChild(individualArtistAlbumFolder);
										}

										individualArtistAlbumFolder.addChild(file.clone());
										individualArtistAllTracksFolder.addChild(file);

										// Put the track into its album folder
										if (!isCompilation) {
											albumName += " - " + artistName;
										}

										VirtualFolder individualAlbumFolder = null;
										for (LibraryResource album : virtualFolderAlbums.getChildren()) {
											if (areNamesEqual(album.getName(), albumName)) {
												individualAlbumFolder = (VirtualFolder) album;
												break;
											}
										}
										if (individualAlbumFolder == null) {
											individualAlbumFolder = new VirtualFolder(renderer, albumName, null);
											virtualFolderAlbums.addChild(individualAlbumFolder);
										}
										individualAlbumFolder.addChild(file.clone());

										// Put the track into its genre folder
										VirtualFolder individualGenreFolder = null;
										for (LibraryResource genre : virtualFolderGenres.getChildren()) {
											if (areNamesEqual(genre.getName(), genreName)) {
												individualGenreFolder = (VirtualFolder) genre;
												break;
											}
										}
										if (individualGenreFolder == null) {
											individualGenreFolder = new VirtualFolder(renderer, genreName, null);
											virtualFolderGenres.addChild(individualGenreFolder);
										}
										individualGenreFolder.addChild(file.clone());

										// Put the track into the global "All tracks" folder
										virtualFolderAllTracks.addChild(file.clone());
									}
								}
							}

							musicFolder.addChild(virtualFolderArtists);
							musicFolder.addChild(virtualFolderAlbums);
							musicFolder.addChild(virtualFolderGenres);
							musicFolder.addChild(virtualFolderAllTracks);

							// Sort the virtual folders alphabetically
							Collections.sort(virtualFolderArtists.getChildren(), (LibraryResource o1, LibraryResource o2) -> {
								VirtualFolder a = (VirtualFolder) o1;
								VirtualFolder b = (VirtualFolder) o2;
								return a.getName().compareToIgnoreCase(b.getName());
							});

							Collections.sort(virtualFolderAlbums.getChildren(), (LibraryResource o1, LibraryResource o2) -> {
								VirtualFolder a = (VirtualFolder) o1;
								VirtualFolder b = (VirtualFolder) o2;
								return a.getName().compareToIgnoreCase(b.getName());
							});

							Collections.sort(virtualFolderGenres.getChildren(), (LibraryResource o1, LibraryResource o2) -> {
								VirtualFolder a = (VirtualFolder) o1;
								VirtualFolder b = (VirtualFolder) o2;
								return a.getName().compareToIgnoreCase(b.getName());
							});
						} else {
							// Add all playlists
							VirtualFolder pf = new VirtualFolder(renderer, playlist.get("Name").toString(), null);
							playlistTracks = (List<?>) playlist.get("Playlist Items"); // list of tracks in a playlist

							if (playlistTracks != null) {
								for (Object t : playlistTracks) {
									Map<?, ?> td = (Map<?, ?>) t;
									track = (Map<?, ?>) tracks.get(td.get("Track ID").toString());

									if (
										track != null &&
										track.get("Location") != null &&
										track.get("Location").toString().startsWith("file://")
									) {
										String name = Normalizer.normalize(track.get("Name").toString(), Normalizer.Form.NFC);
										// remove dots from name to prevent media renderer from trimming
										name = name.replace('.', '-');

										if (track.containsKey("Protected") && track.get("Protected").equals(Boolean.TRUE)) {
											name = name + "-" + Messages.getString("Protected_lowercase");
										}

										URI tURI2 = new URI(track.get("Location").toString());
										RealFile file = new RealFile(renderer, new File(URLDecoder.decode(tURI2.toURL().getFile(), StandardCharsets.UTF_8)), name);
										pf.addChild(file);
									}
								}
							}

							int kind = playlist.containsKey("Distinguished Kind") ? ((Number) playlist.get("Distinguished Kind")).intValue() : -1;
							if (kind >= 0 && kind != 17 && kind != 19 && kind != 20) {
								// System folder, but not voice memos (17) and purchased items (19 & 20)
								res.addChild(pf);
							} else {
								// User playlist or playlist folder
								if (playlistsFolder == null) {
									playlistsFolder = new VirtualFolder(renderer, "Playlists", null);
									res.addChild(playlistsFolder);
								}
								playlistsFolder.addChild(pf);
							}
						}
					}
				} else {
					LOGGER.info("Could not find the iTunes file");
				}
			} catch (Exception e) {
				LOGGER.error("Something went wrong with the iTunes Library scan: ", e);
			}
		}

		return res;
	}

	private void addAdminFolder() {
		LibraryResource res = new VirtualFolder(renderer, Messages.getString("ServerSettings"), null);
		LibraryResource vsf = getVideoSettingsFolder();

		if (vsf != null) {
			res.addChild(vsf);
		}

		if (renderer.getUmsConfiguration().getScriptDir() != null) {
			final File scriptDir = new File(renderer.getUmsConfiguration().getScriptDir());

			if (scriptDir.exists()) {
				res.addChild(new VirtualFolder(renderer, Messages.getString("Scripts"), null) {
					@Override
					public void discoverChildren() {
						File[] files = scriptDir.listFiles();
						if (files != null) {
							for (File file : files) {
								String childrenName = file.getName().replace("_", " ");
								int pos = childrenName.lastIndexOf('.');

								if (pos != -1) {
									childrenName = childrenName.substring(0, pos);
								}

								final File f = file;

								addChild(new VirtualVideoAction(renderer, childrenName, true, null) {
									@Override
									public boolean enable() {
										try {
											ProcessBuilder pb = new ProcessBuilder(f.getAbsolutePath());
											pb.redirectErrorStream(true);
											Process pid = pb.start();
											// consume the error and output process streams
											StreamGobbler.consume(pid.getInputStream());
											pid.waitFor();
										} catch (IOException e) {
											//continue
										} catch (InterruptedException e) {
											Thread.currentThread().interrupt();
										}

										return true;
									}
								});
							}
						}
					}
				});
			}
		}

		// Resume file management
		if (renderer.getUmsConfiguration().isResumeEnabled()) {
			res.addChild(new VirtualFolder(renderer, Messages.getString("ManageResumeFiles"), null) {
				@Override
				public void discoverChildren() {
					final File[] files = ResumeObj.resumeFiles();
					addChild(new VirtualVideoAction(renderer, Messages.getString("DeleteAllFiles"), true, null) {
						@Override
						public boolean enable() {
							for (File f : files) {
								f.delete();
							}
							getParent().getChildren().remove(this);
							return false;
						}
					});
					for (final File f : files) {
						String childrenName = FileUtil.getFileNameWithoutExtension(f.getName());
						childrenName = childrenName.replaceAll(ResumeObj.CLEAN_REG, "");
						addChild(new VirtualVideoAction(renderer, childrenName, false, null) {
							@Override
							public boolean enable() {
								f.delete();
								getParent().getChildren().remove(this);
								return false;
							}
						});
					}
				}
			});
		}

		// Restart UMS
		res.addChild(new VirtualVideoAction(renderer, Messages.getString("RestartUms"), true, "images/icon-videothumbnail-restart.png") {
			@Override
			public boolean enable() {
				ProcessUtil.reboot();
				// Reboot failed if we get here
				return false;
			}
		});

		// Shut down computer
		res.addChild(new VirtualVideoAction(renderer, Messages.getString("ShutDownComputer"), true, "images/icon-videothumbnail-shutdown.png") {
			@Override
			public boolean enable() {
				ProcessUtil.shutDownComputer();
				// Shutdown failed if we get here
				return false;
			}
		});

		addChild(res);
	}

	/**
	 * Returns Video Settings folder. Used by manageRoot, so it is usually
	 * used as a folder at the root folder. Child objects are created when
	 * this folder is created.
	 */
	private LibraryResource getVideoSettingsFolder() {
		LibraryResource res = null;

		if (renderer.getUmsConfiguration().isShowServerSettingsFolder()) {
			res = new VirtualFolder(renderer, Messages.getString("VideoSettings_FolderName"), null);
			VirtualFolder vfSub = new VirtualFolder(renderer, Messages.getString("Subtitles"), null);
			res.addChild(vfSub);

			if (renderer.getUmsConfiguration().useCode() && !PMS.get().masterCodeValid() &&
				StringUtils.isNotEmpty(PMS.get().codeDb().lookup(CodeDb.MASTER))) {
				// if the master code is valid we don't add this
				VirtualVideoAction vva = new VirtualVideoAction(renderer, "MasterCode", true, null) {
					@Override
					public boolean enable() {
						CodeEnter ce = (CodeEnter) getParent();
						if (ce.validCode(this)) {
							PMS.get().setMasterCode(ce);
							return true;
						}
						return false;
					}
				};
				CodeEnter ce1 = new CodeEnter(vva);
				ce1.setCode(CodeDb.MASTER);
				res.addChild(ce1);
			}

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("AvSyncAlternativeMethod"), renderer.getUmsConfiguration().isMencoderNoOutOfSync(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderNoOutOfSync(!renderer.getUmsConfiguration().isMencoderNoOutOfSync());
					return renderer.getUmsConfiguration().isMencoderNoOutOfSync();
				}
			});

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("DefaultH264RemuxMencoder"), renderer.getUmsConfiguration().isMencoderMuxWhenCompatible(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderMuxWhenCompatible(!renderer.getUmsConfiguration().isMencoderMuxWhenCompatible());

					return renderer.getUmsConfiguration().isMencoderMuxWhenCompatible();
				}
			});

			res.addChild(new VirtualVideoAction(renderer, "  !!-- Fix 23.976/25fps A/V Mismatch --!!", renderer.getUmsConfiguration().isFix25FPSAvMismatch(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderForceFps(!renderer.getUmsConfiguration().isFix25FPSAvMismatch());
					renderer.getUmsConfiguration().setFix25FPSAvMismatch(!renderer.getUmsConfiguration().isFix25FPSAvMismatch());
					return renderer.getUmsConfiguration().isFix25FPSAvMismatch();
				}
			});

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("DeinterlaceFilter"), renderer.getUmsConfiguration().isMencoderYadif(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setMencoderYadif(!renderer.getUmsConfiguration().isMencoderYadif());

					return renderer.getUmsConfiguration().isMencoderYadif();
				}
			});

			vfSub.addChild(new VirtualVideoAction(renderer, Messages.getString("DisableSubtitles"), renderer.getUmsConfiguration().isDisableSubtitles(), null) {
				@Override
				public boolean enable() {
					boolean oldValue = renderer.getUmsConfiguration().isDisableSubtitles();
					boolean newValue = !oldValue;
					renderer.getUmsConfiguration().setDisableSubtitles(newValue);
					return newValue;
				}
			});

			vfSub.addChild(new VirtualVideoAction(renderer, Messages.getString("AutomaticallyLoadSrtSubtitles"), renderer.getUmsConfiguration().isAutoloadExternalSubtitles(), null) {
				@Override
				public boolean enable() {
					boolean oldValue = renderer.getUmsConfiguration().isAutoloadExternalSubtitles();
					boolean newValue = !oldValue;
					renderer.getUmsConfiguration().setAutoloadExternalSubtitles(newValue);
					return newValue;
				}
			});

			vfSub.addChild(new VirtualVideoAction(renderer, Messages.getString("UseEmbeddedStyle"), renderer.getUmsConfiguration().isUseEmbeddedSubtitlesStyle(), null) {
				@Override
				public boolean enable() {
					boolean oldValue = renderer.getUmsConfiguration().isUseEmbeddedSubtitlesStyle();
					boolean newValue = !oldValue;
					renderer.getUmsConfiguration().setUseEmbeddedSubtitlesStyle(newValue);
					return newValue;
				}
			});

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("SkipLoopFilterDeblocking"), renderer.getUmsConfiguration().getSkipLoopFilterEnabled(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setSkipLoopFilterEnabled(!renderer.getUmsConfiguration().getSkipLoopFilterEnabled());
					return renderer.getUmsConfiguration().getSkipLoopFilterEnabled();
				}
			});

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("KeepDtsTracks"), renderer.getUmsConfiguration().isAudioEmbedDtsInPcm(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setAudioEmbedDtsInPcm(!renderer.getUmsConfiguration().isAudioEmbedDtsInPcm());
					return renderer.getUmsConfiguration().isAudioEmbedDtsInPcm();
				}
			});

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("SaveConfiguration"), true, null) {
				@Override
				public boolean enable() {
					try {
						renderer.getUmsConfiguration().save();
					} catch (ConfigurationException e) {
						LOGGER.debug("Caught exception", e);
					}
					return true;
				}
			});

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("RestartServer"), true, null) {
				@Override
				public boolean enable() {
					PMS.get().resetMediaServer();
					return true;
				}
			});

			res.addChild(new VirtualVideoAction(renderer, Messages.getString("ShowLiveSubtitlesFolder"), renderer.getUmsConfiguration().isShowLiveSubtitlesFolder(), null) {
				@Override
				public boolean enable() {
					renderer.getUmsConfiguration().setShowLiveSubtitlesFolder(renderer.getUmsConfiguration().isShowLiveSubtitlesFolder());
					return renderer.getUmsConfiguration().isShowLiveSubtitlesFolder();
				}
			});
		}

		return res;
	}

	/**
	 * TODO: move that under the media library as it should (like tv series)
	 */
	private void addVirtualMyMusicFolder() {
		DbIdTypeAndIdent myAlbums = new DbIdTypeAndIdent(DbIdMediaType.TYPE_MYMUSIC_ALBUM, null);
		VirtualFolderDbId myMusicFolder = new VirtualFolderDbId(renderer, Messages.getString("MyAlbums"), myAlbums, "");
		if (PMS.getConfiguration().displayAudioLikesInRootFolder()) {
			if (!getChildren().contains(myMusicFolder)) {
				myMusicFolder.setFakeParentId("0");
				addChild(myMusicFolder, true, false);
				LOGGER.debug("adding My Music folder to root");
			}
		} else {
			if (
				mediaLibrary.isEnabled() &&
				mediaLibrary.getAudioFolder() != null &&
				mediaLibrary.getAudioFolder().getChildren() != null &&
				!mediaLibrary.getAudioFolder().getChildren().contains(myMusicFolder)
			) {
				myMusicFolder.setFakeParentId(mediaLibrary.getAudioFolder().getId());
				mediaLibrary.getAudioFolder().addChild(myMusicFolder, true, false);
				LOGGER.debug("adding My Music folder to 'Audio' folder");
			} else {
				LOGGER.debug("couldn't add 'My Music' folder because the media library is not initialized.");
			}
		}
	}

	@Override
	public String toString() {
		return "RootFolder[" + getChildren() + "]";
	}

	public void reset() {
		setDiscovered(false);
	}

	public void stopPlaying(LibraryResource res) {
		if (mon != null) {
			mon.stopped(res);
		}
	}

	// Returns the LibraryResource pointed to by the uri if it exists
	// or else a new Temp item (or null)
	public LibraryResource getValidResource(String uri, String name) {
		LOGGER.debug("Validating URI \"{}\"", uri);
		String objectId = parseObjectId(uri);
		if (objectId != null) {
			if (objectId.startsWith("Temp$")) {
				int index = tempFolder.indexOf(objectId);
				return index > -1 ? tempFolder.getChildren().get(index) : tempFolder.recreate(objectId, name);
			}
			return getLibraryResource(objectId);
		}
		return tempFolder.add(uri, name);
	}

	public synchronized LibraryResource getLibraryResource(String objectId) {
		// this method returns exactly ONE (1) LibraryResource
		// it's used when someone requests playback of mediaInfo. The mediaInfo must
		// have been discovered by someone first (unless it's a Temp item)

		if (objectId.startsWith("$LogIn/")) {
			String loginstring = StringUtils.substringAfter(objectId, "/");
			Integer userId = UserVirtualFolder.decrypt(loginstring);
			if (userId != null) {
				renderer.setAccount(AccountService.getAccountByUserId(userId));
				getChildren().clear();
				reset();
				discoverChildren();
				UmsContentDirectoryService.bumpSystemUpdateId();
			}
			return this;
		}

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp/")) {
			return getTemp().get(objectId);
		}

		// Now strip off the filename
		objectId = StringUtils.substringBefore(objectId, "/");

		String[] ids = objectId.split("\\.");
		if (objectId.equals("0")) {
			return this;
		} else {
			// only allow the last one here
			return renderer.getGlobalRepo().get(ids[ids.length - 1]);
		}
	}

	/**
	 * First thing it does it searches for an item matching the given objectID.
	 * If children is false, then it returns the found object as the only object
	 * in the list.
	 *
	 * TODO: (botijo) This function does a lot more than this!
	 *
	 * @param objectId ID to search for.
	 * @param children State if you want all the children in the returned list.
	 * @param start
	 * @param count
	 * @param renderer Renderer for which to do the actions.
	 * @return List of LibraryResource items.
	 * @throws IOException
	 */
	public synchronized List<LibraryResource> getLibraryResources(String objectId, boolean children, int start, int count) throws IOException {
		return getLibraryResources(objectId, children, start, count, null);
	}

	public synchronized List<LibraryResource> getLibraryResources(String objectId, boolean returnChildren, int start, int count,
		String searchStr) {
		ArrayList<LibraryResource> resources = new ArrayList<>();

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp/")) {
			List<LibraryResource> items = getTemp().asList(objectId);
			return items != null ? items : resources;
		}

		// Now strip off the filename
		objectId = StringUtils.substringBefore(objectId, "/");

		LibraryResource resource = null;
		String[] ids = objectId.split("\\.");
		if (objectId.equals("0")) {
			resource = this;
		} else {
			if (objectId.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
				try {
					resource = DbIdResourceLocator.locateResource(renderer, objectId);
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} else {
				resource = renderer.getGlobalRepo().get(ids[ids.length - 1]);
			}
		}

		if (resource == null) {
			// nothing in the cache do a traditional search
			resource = search(ids);
			// resource = search(objectId, count, searchStr);
		}

		if (resource != null) {
			if (!(resource instanceof CodeEnter) && !isCodeValid(resource)) {
				LOGGER.debug("code is not valid any longer");
				return resources;
			}

			if (!isRendererAllowed()) {
				LOGGER.debug("renderer does not have access to this ressource");
				return resources;
			}

			if (!returnChildren) {
				resources.add(resource);
				resource.refreshChildrenIfNeeded(searchStr);
			} else {
				resource.discover(count, true, searchStr);

				if (count == 0) {
					count = resource.getChildren().size();
				}

				if (count > 0) {
					String systemName = resource.getSystemName();
					ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(count);

					int nParallelThreads = 3;
					if (resource instanceof DVDISOFile) {
						// Some DVD drives die with 3 parallel threads
						nParallelThreads = 1;
					}

					ThreadPoolExecutor tpe = new ThreadPoolExecutor(Math.min(count, nParallelThreads), count, 20, TimeUnit.SECONDS, queue,
						new SimpleThreadFactory("LibraryResource resolver thread", true));

					if (shouldDoAudioTrackSorting(resource)) {
						sortChildrenWithAudioElements(resource);
					}
					for (int i = start; i < start + count && i < resource.getChildren().size(); i++) {
						final LibraryResource child = resource.getChildren().get(i);
						if (child != null) {
							tpe.execute(child);
							resources.add(child);
						} else {
							LOGGER.warn("null child at index {} in {}", i, systemName);
						}
					}

					try {
						tpe.shutdown();
						tpe.awaitTermination(20, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						LOGGER.error("error while shutting down thread pool executor for " + systemName, e);
						Thread.currentThread().interrupt();
					}

					LOGGER.trace("End of analysis for " + systemName);
				}
			}
		}

		return resources;
	}

	private LibraryResource search(String[] searchIds) {
		LibraryResource resource;
		for (String searchId : searchIds) {
			if (searchId.equals("0")) {
				resource = this;
			} else {
				resource = renderer.getGlobalRepo().get(searchId);
			}

			if (resource == null) {
				LOGGER.debug("Bad id {} found in path", searchId);
				return null;
			}

			resource.discover(0, false, null);
		}

		return renderer.getGlobalRepo().get(searchIds[searchIds.length - 1]);
	}

	private static boolean areNamesEqual(String aThis, String aThat) {
		Collator collator = Collator.getInstance(Locale.getDefault());
		collator.setStrength(Collator.PRIMARY);
		int comparison = collator.compare(aThis, aThat);

		return (comparison == 0);
	}

	/**
	 * Check if all audio child elements belong to the same album. Here the Album string is matched. Another more strict alternative
	 * implementation could match the MBID record id (not implemented).
	 *
	 * @param resource Folder containing child objects of any kind
	 *
	 * @return
	 * 	TRUE, if AudioTrackSorting is not disabled, all audio child objects belong to the same album and the majority of files are audio.
	 */
	private static boolean shouldDoAudioTrackSorting(LibraryResource resource) {
		if (!PMS.getConfiguration().isSortAudioTracksByAlbumPosition()) {
			LOGGER.trace("shouldDoAudioTrackSorting : {}", PMS.getConfiguration().isSortAudioTracksByAlbumPosition());
			return false;
		}

		String album = null;
		String mbReleaseId = null;
		int numberOfAudioFiles = 0;
		int numberOfOtherFiles = 0;

		boolean audioExists = false;
		for (LibraryResource res : resource.getChildren()) {
			if (res.getFormat() != null && res.getFormat().isAudio()) {
				if (res.getMediaInfo() == null || !res.getMediaInfo().hasAudioMetadata()) {
					LOGGER.warn("Audio resource has no AudioMetadata : {}", res.getDisplayName());
					continue;
				}
				MediaAudioMetadata metadata = res.getMediaInfo().getAudioMetadata();
				numberOfAudioFiles++;
				if (album == null) {
					audioExists = true;
					album = metadata.getAlbum() != null ? metadata.getAlbum() : "";
					mbReleaseId = metadata.getMbidRecord();
					if (StringUtils.isAllBlank(album) && StringUtils.isAllBlank(mbReleaseId)) {
						return false;
					}
				} else {
					if (mbReleaseId != null && !StringUtils.isAllBlank(mbReleaseId)) {
						// First check musicbrainz ReleaseID
						if (!mbReleaseId.equals(metadata.getMbidRecord())) {
							return false;
						}
					} else if (!album.equals(metadata.getAlbum())) {
						return false;
					}
				}
			} else {
				numberOfOtherFiles++;
			}
		}
		return audioExists && (numberOfAudioFiles > numberOfOtherFiles);
	}

	private static void sortChildrenWithAudioElements(LibraryResource resource) {
		Collections.sort(resource.getChildren(), (LibraryResource o1, LibraryResource o2) -> {
			if (getDiscNum(o1) == null || getDiscNum(o2) == null || getDiscNum(o1).equals(getDiscNum(o2))) {
				if (o1.getFormat() != null && o1.getFormat().isAudio()) {
					if (o2.getFormat() != null && o2.getFormat().isAudio()) {
						return getTrackNum(o1).compareTo(getTrackNum(o2));
					} else {
						return o1.getDisplayNameBase().compareTo(o2.getDisplayNameBase());
					}
				} else {
					return o1.getDisplayNameBase().compareTo(o2.getDisplayNameBase());
				}
			} else {
				return getDiscNum(o1).compareTo(getDiscNum(o2));
			}
		});
	}

	private static Integer getTrackNum(LibraryResource res) {
		if (res != null && res.getMediaInfo() != null && res.getMediaInfo().hasAudioMetadata()) {
			return res.getMediaInfo().getAudioMetadata().getTrack();
		}
		return 0;
	}

	private static Integer getDiscNum(LibraryResource res) {
		if (res != null && res.getMediaInfo() != null && res.getMediaInfo().hasAudioMetadata()) {
			return res.getMediaInfo().getAudioMetadata().getDisc();
		}
		return 0;
	}

}
