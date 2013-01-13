/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
package net.pms.dlna;

import com.sun.jna.Platform;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.DownloadPlugins;
import net.pms.configuration.MapFileConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.external.AdditionalFolderAtRoot;
import net.pms.external.AdditionalFoldersAtRoot;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.gui.IFrame;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xmlwise.Plist;
import xmlwise.XmlParseException;

public class RootFolder extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(RootFolder.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private boolean running;
	private FolderLimit lim;

	public RootFolder() {
		setIndexId(0);
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
	public void discoverChildren() {
		if (isDiscovered()) {
			return;
		}

		if (configuration.getFolderLimit()) {
			lim = new FolderLimit();
			addChild(lim);
		}

		for (DLNAResource r : getConfiguredFolders()) {
			addChild(r);
		}

		for (DLNAResource r : getVirtualFolders()) {
			addChild(r);
		}

		if (configuration.getSearchFolder()) {
			SearchFolder sf = new SearchFolder("Search disc folders", new FileSearch(getConfiguredFolders()));
			addChild(sf);
		}

		File webConf = new File(configuration.getProfileDirectory(), "WEB.conf");
		if (webConf.exists() && configuration.getExternalNetwork()) {
			addWebFolder(webConf);
		}

		if (Platform.isMac() && configuration.getIphotoEnabled()) {
			DLNAResource iPhotoRes = getiPhotoFolder();
			if (iPhotoRes != null) {
				addChild(iPhotoRes);
			}
		}

		if (Platform.isMac() && configuration.getApertureEnabled()) {
			DLNAResource apertureRes = getApertureFolder();
			if (apertureRes != null) {
				addChild(apertureRes);
			}
		}

		if ((Platform.isMac() || Platform.isWindows()) && configuration.getItunesEnabled()) {
			DLNAResource iTunesRes = getiTunesFolder();
			if (iTunesRes != null) {
				addChild(iTunesRes);
			}
		}

		if (!configuration.isHideMediaLibraryFolder()) {
			DLNAResource libraryRes = PMS.get().getLibrary();
			if (libraryRes != null) {
				addChild(libraryRes);
			}
		}

		for (DLNAResource r : getAdditionalFoldersAtRoot()) {
			addChild(r);
		}

		if (!configuration.getHideVideoSettings()) {
			addAdminFolder();
		}

		setDiscovered(true);
	}

	public void setFolderLim(DLNAResource r) {
		if (lim != null) {
			lim.setStart(r);
		}
	}

	/**
	 * Returns whether or not a scan is running.
	 *
	 * @return <code>true</code> if a scan is running, <code>false</code>
	 * otherwise.
	 */
	private synchronized boolean isRunning() {
		return running;
	}

	/**
	 * Sets whether or not a scan is running.
	 *
	 * @param running Set to <code>true</code> if the scan is running, or to
	 * <code>false</code> when the scan has stopped.
	 */
	private synchronized void setRunning(boolean running) {
		this.running = running;
	}

	public void scan() {
		setRunning(true);

		if (!isDiscovered()) {
			discoverChildren();
		}

		setDefaultRenderer(RendererConfiguration.getDefaultConf());
		scan(this);
		IFrame frame = PMS.get().getFrame();
		frame.setScanLibraryEnabled(true);
		PMS.get().getDatabase().cleanup();
		frame.setStatusLine(null);
	}

	/*
	 * @deprecated Use {@link #stopScan()} instead.
	 */
	public void stopscan() {
		stopScan();
	}

	public void stopScan() {
		setRunning(false);
	}

	private synchronized void scan(DLNAResource resource) {
		if (isRunning()) {
			for (DLNAResource child : resource.getChildren()) {
				if (isRunning() && child.allowScan()) {
					child.setDefaultRenderer(resource.getDefaultRenderer());
					String trace = null;

					if (child instanceof RealFile) {
						trace = Messages.getString("DLNAMediaDatabase.4") + " " + child.getName();
					}

					if (trace != null) {
						LOGGER.debug(trace);
						PMS.get().getFrame().setStatusLine(trace);
					}

					if (child.isDiscovered()) {
						child.refreshChildren();
					} else {
						if (child instanceof DVDISOFile || child instanceof DVDISOTitle) { // ugly hack
							child.resolve();
						}
						child.discoverChildren();
						child.analyzeChildren(-1);
						child.setDiscovered(true);
					}

					int count = child.getChildren().size();

					if (count == 0) {
						continue;
					}

					scan(child);
					child.getChildren().clear();
				}
			}
		}
	}

	private List<RealFile> getConfiguredFolders() {
		List<RealFile> res = new ArrayList<RealFile>();
		File[] files = PMS.get().getFoldersConf();

		if (files == null || files.length == 0) {
			files = File.listRoots();
		}

		for (File f : files) {
			res.add(new RealFile(f));
		}

		return res;
	}

	private List<DLNAResource> getVirtualFolders() {
		List<DLNAResource> res = new ArrayList<DLNAResource>();
		List<MapFileConfiguration> mapFileConfs = MapFileConfiguration.parse(configuration.getVirtualFolders());

		if (mapFileConfs != null) {
			for (MapFileConfiguration f : mapFileConfs) {
				res.add(new MapFile(f));
			}
		}

		return res;
	}

	private void addWebFolder(File webConf) {
		if (webConf.exists()) {
			try {
				LineNumberReader br = new LineNumberReader(new InputStreamReader(new FileInputStream(webConf), "UTF-8"));
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();

					if (line.length() > 0 && !line.startsWith("#") && line.indexOf("=") > -1) {
						String key = line.substring(0, line.indexOf("="));
						String value = line.substring(line.indexOf("=") + 1);
						String[] keys = parseFeedKey(key);

						try {
							if (
								keys[0].equals("imagefeed") ||
								keys[0].equals("audiofeed") ||
								keys[0].equals("videofeed") ||
								keys[0].equals("audiostream") ||
								keys[0].equals("videostream")
							) {
								String[] values = parseFeedValue(value);
								DLNAResource parent = null;

								if (keys[1] != null) {
									StringTokenizer st = new StringTokenizer(keys[1], ",");
									DLNAResource currentRoot = this;

									while (st.hasMoreTokens()) {
										String folder = st.nextToken();
										parent = currentRoot.searchByName(folder);

										if (parent == null) {
											parent = new VirtualFolder(folder, "");
											currentRoot.addChild(parent);
										}

										currentRoot = parent;
									}
								}

								if (parent == null) {
									parent = this;
								}

								if (keys[0].equals("imagefeed")) {
									parent.addChild(new ImagesFeed(values[0]));
								} else if (keys[0].equals("videofeed")) {
									parent.addChild(new VideosFeed(values[0]));
								} else if (keys[0].equals("audiofeed")) {
									parent.addChild(new AudiosFeed(values[0]));
								} else if (keys[0].equals("audiostream")) {
									parent.addChild(new WebAudioStream(values[0], values[1], values[2]));
								} else if (keys[0].equals("videostream")) {
									parent.addChild(new WebVideoStream(values[0], values[1], values[2]));
								}
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							// catch exception here and go with parsing
							LOGGER.info("Error at line " + br.getLineNumber() + " of WEB.conf: " + e.getMessage());
							LOGGER.debug(null, e);
						}
					}
				}

				br.close();
			} catch (IOException e) {
				LOGGER.info("Unexpected error in WEB.conf" + e.getMessage());
				LOGGER.debug(null, e);
			}
		}
	}

	/**
	 * Splits the first part of a WEB.conf spec into a pair of Strings
	 * representing the resource type and its DLNA folder.
	 *
	 * @param spec (String) to be split
	 * @return Array of (String) that represents the tokenized entry.
	 */
	private String[] parseFeedKey(String spec) {
		String[] pair = StringUtils.split(spec, ".", 2);

		if (pair == null || pair.length < 2) {
			pair = new String[2];
		}

		if (pair[0] == null) {
			pair[0] = "";
		}

		return pair;
	}

	/**
	 * Splits the second part of a WEB.conf spec into a triple of Strings
	 * representing the DLNA path, resource URI and optional thumbnail URI.
	 *
	 * @param spec (String) to be split
	 * @return Array of (String) that represents the tokenized entry.
	 */
	private String[] parseFeedValue(String spec) {
		StringTokenizer st = new StringTokenizer(spec, ",");
		String[] triple = new String[3];
		int i = 0;

		while (st.hasMoreTokens()) {
			triple[i++] = st.nextToken();
		}

		return triple;
	}

	/**
	 * Creates, populates and returns a virtual folder mirroring the
	 * contents of the system's iPhoto folder.
	 * Mac OS X only.
	 *
	 * @return iPhotoVirtualFolder the populated <code>VirtualFolder</code>, or null if one couldn't be created.
	 */
	private DLNAResource getiPhotoFolder() {
		VirtualFolder iPhotoVirtualFolder = null;

		if (Platform.isMac()) {
			LOGGER.debug("Adding iPhoto folder");
			InputStream inputStream = null;

			try {
				// This command will show the XML files for recently opened iPhoto databases
				Process process = Runtime.getRuntime().exec("defaults read com.apple.iApps iPhotoRecentDatabases");
				inputStream = process.getInputStream();
				List<String> lines = IOUtils.readLines(inputStream);
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

					iPhotoVirtualFolder = new VirtualFolder("iPhoto Library", null);

					for (Map<?, ?> roll : listOfRolls) {
						Object rollName = roll.get("RollName");

						if (rollName != null) {
							VirtualFolder virtualFolder = new VirtualFolder(rollName.toString(), null);

							// List of photos in an event (roll)
							List<?> rollPhotos = (List<?>) roll.get("KeyList");

							for (Object photo : rollPhotos) {
								Map<?, ?> photoProperties = (Map<?, ?>) photoList.get(photo);

								if (photoProperties != null) {
									Object imagePath = photoProperties.get("ImagePath");

									if (imagePath != null) {
										RealFile realFile = new RealFile(new File(imagePath.toString()));
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
			} catch (XmlParseException e) {
				LOGGER.error("Something went wrong with the iPhoto Library scan: ", e);
			} catch (URISyntaxException e) {
				LOGGER.error("Something went wrong with the iPhoto Library scan: ", e);
			} catch (IOException e) {
				LOGGER.error("Something went wrong with the iPhoto Library scan: ", e);
			} finally {
				IOUtils.closeQuietly(inputStream);
			}
		}

		return iPhotoVirtualFolder;
	}

	/**
	 * Returns Aperture folder. Used by manageRoot, so it is usually used as
	 * a folder at the root folder. Only works when PMS is run on Mac OS X.
	 * TODO: Requirements for Aperture.
	 */
	private DLNAResource getApertureFolder() {
		VirtualFolder res = null;

		if (Platform.isMac()) {
			Process process = null;

			try {
				process = Runtime.getRuntime().exec("defaults read com.apple.iApps ApertureLibraries");
				BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
				// Every line entry is one aperture library. We want all of them as a dlna folder.
				String line;
				res = new VirtualFolder("Aperture libraries", null);

				while ((line = in.readLine()) != null) {
					if (line.startsWith("(") || line.startsWith(")")) {
						continue;
					}

					line = line.trim(); // remove extra spaces
					line = line.substring(1, line.lastIndexOf("\"")); // remove quotes and spaces
					VirtualFolder apertureLibrary = createApertureDlnaLibrary(line);

					if (apertureLibrary != null) {
						res.addChild(apertureLibrary);
					}
				}

				in.close();
			} catch (Exception e) {
				LOGGER.error("Something went wrong with the aperture library scan: ", e);
			} finally {
				// Avoid zombie processes, or open stream failures
				if (process != null) {
					try {
						// The process seems to always finish, so we can wait for it.
						// If the result code is not read by parent. The process might turn into a zombie (they are real!)
						process.waitFor();
					} catch (InterruptedException e) {
						// Can this thread be interrupted? Don't think so, or, and even when, what will happen?
						LOGGER.warn("Interrupted while waiting for stream for process" + e.getMessage());
					}

					try {
						process.getErrorStream().close();
					} catch (Exception e) {
						LOGGER.warn("Could not close stream for output process", e);
					}

					try {
						process.getInputStream().close();
					} catch (Exception e) {
						LOGGER.warn("Could not close stream for output process", e);
					}

					try {
						process.getOutputStream().close();
					} catch (Exception e) {
						LOGGER.warn("Could not close stream for output process", e);
					}
				}
			}
		}

		return res;
	}

	private VirtualFolder createApertureDlnaLibrary(String url) throws UnsupportedEncodingException, MalformedURLException, XmlParseException, IOException, URISyntaxException {
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

				if (mediaName != null && mediaName.lastIndexOf("/") != -1 && mediaName.lastIndexOf(".aplibrary") != -1) {
					mediaName = mediaName.substring(mediaName.lastIndexOf("/"), mediaName.lastIndexOf(".aplibrary"));
				} else {
					mediaName = "unknown library";
				}
			} else {
				mediaName = "unknown library";
			}

			LOGGER.info("Going to parse aperture library: " + mediaName);
			res = new VirtualFolder(mediaName, null);
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
		VirtualFolder vAlbum = new VirtualFolder(album.get("AlbumName").toString(), null);

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
			Map<?, ? > photo = (Map<?, ?>) photoList.get(photoKey);

			if (firstPhoto) {
				Object x = photoList.get("ThumbPath");

				if (x != null) {
					vAlbum.setThumbnail(x.toString());
				}

				firstPhoto = false;
			}

			RealFile file = new RealFile(new File(photo.get("ImagePath").toString()));
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
		String line;
		String iTunesFile = null;

		if (Platform.isMac()) {
			// the second line should contain a quoted file URL e.g.:
			// "file://localhost/Users/MyUser/Music/iTunes/iTunes%20Music%20Library.xml"
			Process process = Runtime.getRuntime().exec("defaults read com.apple.iApps iTunesRecentDatabases");
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

			// we want the 2nd line
			if ((line = in.readLine()) != null && (line = in.readLine()) != null) {
				line = line.trim(); // remove extra spaces
				line = line.substring(1, line.length() - 1); // remove quotes and spaces
				URI tURI = new URI(line);
				iTunesFile = URLDecoder.decode(tURI.toURL().getFile(), "UTF8");
			}

			if (in != null) {
				in.close();
			}
		} else if (Platform.isWindows()) {
			Process process = Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v \"My Music\"");
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String location = null;

			while ((line = in.readLine()) != null) {
				final String LOOK_FOR = "REG_SZ";
				if (line.contains(LOOK_FOR)) {
					location = line.substring(line.indexOf(LOOK_FOR) + LOOK_FOR.length()).trim();
				}
			}

			if (in != null) {
				in.close();
			}

			if (location != null) {
				// Add the iTunes folder to the end
				location = location + "\\iTunes\\iTunes Music Library.xml";
				iTunesFile = location;
			} else {
				LOGGER.info("Could not find the My Music folder");
			}
		}

		return iTunesFile;
	}

	/**
	 * Returns iTunes folder. Used by manageRoot, so it is usually used as a
	 * folder at the root folder. Only works when PMS is run on MacOsX or
	 * Windows.
	 * <p>
	 * The iTunes XML is parsed fully when this method is called, so it can
	 * take some time for larger (+1000 albums) databases.
	 * TODO: Check if only music is being added.
	 * <P>
	 * This method does not support genius playlists and does not provide a
	 * media library.
	 *
	 * @see RootFolder#getiTunesFile(boolean)
	 */
	private DLNAResource getiTunesFolder() {
		DLNAResource res = null;

		if (Platform.isMac() || Platform.isWindows()) {
			Map<String, Object> iTunesLib;
			List<?> Playlists;
			Map<?, ?> Playlist;
			Map<?, ?> Tracks;
			Map<?, ?> track;
			List<?> PlaylistTracks;

			try {
				String iTunesFile = getiTunesFile();

				if (iTunesFile != null && (new File(iTunesFile)).exists()) {
					iTunesLib = Plist.load(URLDecoder.decode(iTunesFile, System.getProperty("file.encoding"))); // loads the (nested) properties.
					Tracks = (Map<?, ?>) iTunesLib.get("Tracks"); // the list of tracks
					Playlists = (List<?>) iTunesLib.get("Playlists"); // the list of Playlists
					res = new VirtualFolder("iTunes Library", null);

					for (Object item : Playlists) {
						Playlist = (Map<?, ?>) item;
						VirtualFolder pf = new VirtualFolder(Playlist.get("Name").toString(), null);
						PlaylistTracks = (List<?>) Playlist.get("Playlist Items"); // list of tracks in a playlist

						if (PlaylistTracks != null) {
							for (Object t : PlaylistTracks) {
								Map<?, ?> td = (Map<?, ?>) t;
								track = (Map<?, ?>) Tracks.get(td.get("Track ID").toString());

								if (
									track != null
									&& track.get("Location") != null
									&& track.get("Location").toString().startsWith("file://")
								) {
									URI tURI2 = new URI(track.get("Location").toString());
									RealFile file = new RealFile(new File(URLDecoder.decode(tURI2.toURL().getFile(), "UTF-8")));
									pf.addChild(file);
								}
							}
						}

						res.addChild(pf);
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
		DLNAResource res = new VirtualFolder(Messages.getString("PMS.131"), null);
		DLNAResource vsf = getVideoSettingssFolder();

		if (vsf != null) {
			res.addChild(vsf);
		}

		res.addChild(new VirtualFolder(Messages.getString("NetworkTab.39"), null) {
			@Override
			public void discoverChildren() {
				final ArrayList<DownloadPlugins> plugins = DownloadPlugins.downloadList();
				for (final DownloadPlugins plugin : plugins) {
					addChild(new VirtualVideoAction(plugin.getName(), true) {
						@Override
						public boolean enable() {
							try {
								plugin.install(null);
							} catch (Exception e) {
							}

							return true;
						}
					});
				}
			}
		});

		if (configuration.getScriptDir() != null) {
			final File scriptDir = new File(configuration.getScriptDir());

			if (scriptDir.exists()) {
				res.addChild(new VirtualFolder(Messages.getString("PMS.132"), null) {
					@Override
					public void discoverChildren() {
						File[] files = scriptDir.listFiles();

						for (int i = 0; i < files.length; i++) {
							String name = files[i].getName().replaceAll("_", " ");
							int pos = name.lastIndexOf(".");

							if (pos != -1) {
								name = name.substring(0,pos);
							}

							final File f = files[i];

							addChild(new VirtualVideoAction(name, true) {
								@Override
								public boolean enable() {
									try {
										ProcessBuilder pb = new ProcessBuilder(f.getAbsolutePath());
										Process pid = pb.start();
										InputStream is = pid.getInputStream();
										InputStreamReader isr = new InputStreamReader(is);
										BufferedReader br = new BufferedReader(isr);
										while (br.readLine() != null) { 
										}
										pid.waitFor();
									} catch (Exception e) {
									}

									return true;
								}
							});
						}
					}
				});
			}
		}

		addChild(res);
	}

	/**
	 * Returns Video Settings folder. Used by manageRoot, so it is usually
	 * used as a folder at the root folder. Child objects are created when
	 * this folder is created.
	 */
	private DLNAResource getVideoSettingssFolder() {
		DLNAResource res = null;

		if (!configuration.getHideVideoSettings()) {
			res = new VirtualFolder(Messages.getString("PMS.37"), null);
			VirtualFolder vfSub = new VirtualFolder(Messages.getString("PMS.8"), null);
			res.addChild(vfSub);

			res.addChild(new VirtualVideoAction(Messages.getString("PMS.3"), configuration.isMencoderNoOutOfSync()) {
				@Override
				public boolean enable() {
					configuration.setMencoderNoOutOfSync(!configuration.isMencoderNoOutOfSync());
					return configuration.isMencoderNoOutOfSync();
				}
			});

			res.addChild(new VirtualVideoAction(Messages.getString("PMS.14"), configuration.isMencoderMuxWhenCompatible()) {
				@Override
				public boolean enable() {
					configuration.setMencoderMuxWhenCompatible(!configuration.isMencoderMuxWhenCompatible());

					return configuration.isMencoderMuxWhenCompatible();
				}
			});

			res.addChild(new VirtualVideoAction("  !!-- Fix 23.976/25fps A/V Mismatch --!!", configuration.isFix25FPSAvMismatch()) {
				@Override
				public boolean enable() {
					configuration.setMencoderForceFps(!configuration.isFix25FPSAvMismatch());
					configuration.setFix25FPSAvMismatch(!configuration.isFix25FPSAvMismatch());
					return configuration.isFix25FPSAvMismatch();
				}
			});

			res.addChild(new VirtualVideoAction(Messages.getString("PMS.4"), configuration.isMencoderYadif()) {
				@Override
				public boolean enable() {
					configuration.setMencoderYadif(!configuration.isMencoderYadif());

					return configuration.isMencoderYadif();
				}
			});

			vfSub.addChild(new VirtualVideoAction(Messages.getString("PMS.10"), configuration.isMencoderDisableSubs()) {
				@Override
				public boolean enable() {
					boolean oldValue = configuration.isMencoderDisableSubs();
					boolean newValue = !oldValue;
					configuration.setMencoderDisableSubs(newValue);
					return newValue;
				}
			});

			vfSub.addChild(new VirtualVideoAction(Messages.getString("PMS.6"), configuration.isAutoloadSubtitles()) {
				@Override
				public boolean enable() {
					boolean oldValue = configuration.isAutoloadSubtitles();
					boolean newValue = !oldValue;
					configuration.setAutoloadSubtitles(newValue);
					return newValue;
				}
			});

			vfSub.addChild(new VirtualVideoAction(Messages.getString("MEncoderVideo.36"), configuration.isMencoderAssDefaultStyle()) {
				@Override
				public boolean enable() {
					boolean oldValue = configuration.isMencoderAssDefaultStyle();
					boolean newValue = !oldValue;
					configuration.setMencoderAssDefaultStyle(newValue);
					return newValue;
				}
			});

			res.addChild(new VirtualVideoAction(Messages.getString("PMS.7"), configuration.getSkipLoopFilterEnabled()) {
				@Override
				public boolean enable() {
					configuration.setSkipLoopFilterEnabled(!configuration.getSkipLoopFilterEnabled());
					return configuration.getSkipLoopFilterEnabled();
				}
			});

			res.addChild(new VirtualVideoAction(Messages.getString("TrTab2.28"), configuration.isDTSEmbedInPCM()) {
				@Override
				public boolean enable() {
					configuration.setDTSEmbedInPCM(!configuration.isDTSEmbedInPCM());
					return configuration.isDTSEmbedInPCM();
				}
			});

			res.addChild(new VirtualVideoAction(Messages.getString("PMS.27"), true) {
				@Override
				public boolean enable() {
					try {
						configuration.save();
					} catch (ConfigurationException e) {
						LOGGER.debug("Caught exception", e);
					}
					return true;
				}
			});

			res.addChild(new VirtualVideoAction(Messages.getString("LooksFrame.12"), true) {
				@Override
				public boolean enable() {
					PMS.get().reset();
					return true;
				}
			});
		}

		return res;
	}

	/**
	 * Returns as many folders as plugins providing root folders are loaded
	 * into memory (need to implement AdditionalFolder(s)AtRoot)
	 */
	private List<DLNAResource> getAdditionalFoldersAtRoot() {
		List<DLNAResource> res = new ArrayList<DLNAResource>();

		for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
			if (listener instanceof AdditionalFolderAtRoot) {
				AdditionalFolderAtRoot afar = (AdditionalFolderAtRoot) listener;

				try {
					res.add(afar.getChild());
				} catch (Throwable t) {
					LOGGER.error(String.format("Failed to append AdditionalFolderAtRoot with name=%s, class=%s", afar.name(), afar.getClass()), t);
				}
			} else if (listener instanceof AdditionalFoldersAtRoot) {
				java.util.Iterator<DLNAResource> folders = ((AdditionalFoldersAtRoot) listener).getChildren();

				while (folders.hasNext()) {
					DLNAResource resource = folders.next();

					try {
						res.add(resource);
					} catch (Throwable t) {
						LOGGER.error(String.format("Failed to append AdditionalFolderAtRoots with class=%s for DLNAResource=%s", listener.getClass(), resource.getClass()), t);
					}
				}
			}
		}

		return res;
	}

	@Override
	public String toString() {
		return "RootFolder[" + getChildren() + "]";
	}

	public void reset() {
		setDiscovered(false);
	}
}
