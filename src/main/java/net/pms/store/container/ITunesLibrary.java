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
package net.pms.store.container;

import com.sun.jna.Platform;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.pms.Messages;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.item.RealFile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xmlwise.Plist;

public class ITunesLibrary extends LocalizedStoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ITunesLibrary.class);

	public ITunesLibrary(Renderer renderer) {
		super(renderer, "ItunesLibrary");
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
	 * @see getiTunesFile(Renderer renderer)
	 */
	public static ITunesLibrary getiTunesFolder(Renderer renderer, String path) {
		ITunesLibrary res = null;

		if (Platform.isMac() || Platform.isWindows()) {
			Map<String, Object> iTunesLib;
			List<?> playlists;
			Map<?, ?> playlist;
			Map<?, ?> tracks;
			Map<?, ?> track;
			List<?> playlistTracks;

			try {
				String iTunesFile = getiTunesFile(path);

				if (iTunesFile != null && (new File(iTunesFile)).exists()) {
					iTunesLib = Plist.load(URLDecoder.decode(iTunesFile, System.getProperty("file.encoding"))); // loads the (nested) properties.
					tracks = (Map<?, ?>) iTunesLib.get("Tracks"); // the list of tracks
					playlists = (List<?>) iTunesLib.get("Playlists"); // the list of Playlists
					res = new ITunesLibrary(renderer);

					StoreContainer playlistsFolder = null;

					for (Object item : playlists) {
						playlist = (Map<?, ?>) item;

						if (playlist.containsKey("Visible") && playlist.get("Visible").equals(Boolean.FALSE)) {
							continue;
						}

						if (playlist.containsKey("Music") && playlist.get("Music").equals(Boolean.TRUE)) {
							// Create virtual folders for artists, albums and genres

							StoreContainer musicFolder = new StoreContainer(renderer, playlist.get("Name").toString(), null);
							res.addChild(musicFolder);

							LocalizedStoreContainer virtualFolderArtists = new LocalizedStoreContainer(renderer, "BrowseByArtist");
							LocalizedStoreContainer virtualFolderAlbums = new LocalizedStoreContainer(renderer, "BrowseByAlbum");
							LocalizedStoreContainer virtualFolderGenres = new LocalizedStoreContainer(renderer, "BrowseByGenre");
							LocalizedStoreContainer virtualFolderAllTracks = new LocalizedStoreContainer(renderer, "AllAudioTracks");
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
										StoreContainer individualArtistFolder = null;
										StoreContainer individualArtistAllTracksFolder;
										StoreContainer individualArtistAlbumFolder = null;

										for (StoreResource artist : virtualFolderArtists.getChildren()) {
											if (areNamesEqual(artist.getName(), artistName)) {
												individualArtistFolder = (StoreContainer) artist;
												for (StoreResource album : individualArtistFolder.getChildren()) {
													if (areNamesEqual(album.getName(), albumName)) {
														individualArtistAlbumFolder = (StoreContainer) album;
													}
												}
												break;
											}
										}

										if (individualArtistFolder == null) {
											individualArtistFolder = new StoreContainer(renderer, artistName, null);
											virtualFolderArtists.addChild(individualArtistFolder);
											individualArtistAllTracksFolder = new LocalizedStoreContainer(renderer, "AllAudioTracks");
											individualArtistFolder.addChild(individualArtistAllTracksFolder);
										} else {
											individualArtistAllTracksFolder = (StoreContainer) individualArtistFolder.getChildren().get(0);
										}

										if (individualArtistAlbumFolder == null) {
											individualArtistAlbumFolder = new StoreContainer(renderer, albumName, null);
											individualArtistFolder.addChild(individualArtistAlbumFolder);
										}

										individualArtistAlbumFolder.addChild(file.clone());
										individualArtistAllTracksFolder.addChild(file);

										// Put the track into its album folder
										if (!isCompilation) {
											albumName += " - " + artistName;
										}

										StoreContainer individualAlbumFolder = null;
										for (StoreResource album : virtualFolderAlbums.getChildren()) {
											if (areNamesEqual(album.getName(), albumName)) {
												individualAlbumFolder = (StoreContainer) album;
												break;
											}
										}
										if (individualAlbumFolder == null) {
											individualAlbumFolder = new StoreContainer(renderer, albumName, null);
											virtualFolderAlbums.addChild(individualAlbumFolder);
										}
										individualAlbumFolder.addChild(file.clone());

										// Put the track into its genre folder
										StoreContainer individualGenreFolder = null;
										for (StoreResource genre : virtualFolderGenres.getChildren()) {
											if (areNamesEqual(genre.getName(), genreName)) {
												individualGenreFolder = (StoreContainer) genre;
												break;
											}
										}
										if (individualGenreFolder == null) {
											individualGenreFolder = new StoreContainer(renderer, genreName, null);
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
							Collections.sort(virtualFolderArtists.getChildren(), (StoreResource o1, StoreResource o2) -> {
								StoreContainer a = (StoreContainer) o1;
								StoreContainer b = (StoreContainer) o2;
								return a.getName().compareToIgnoreCase(b.getName());
							});

							Collections.sort(virtualFolderAlbums.getChildren(), (StoreResource o1, StoreResource o2) -> {
								StoreContainer a = (StoreContainer) o1;
								StoreContainer b = (StoreContainer) o2;
								return a.getName().compareToIgnoreCase(b.getName());
							});

							Collections.sort(virtualFolderGenres.getChildren(), (StoreResource o1, StoreResource o2) -> {
								StoreContainer a = (StoreContainer) o1;
								StoreContainer b = (StoreContainer) o2;
								return a.getName().compareToIgnoreCase(b.getName());
							});
						} else {
							// Add all playlists
							StoreContainer pf = new StoreContainer(renderer, playlist.get("Name").toString(), null);
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
									playlistsFolder = new StoreContainer(renderer, "Playlists", null);
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

	/**
	 * Returns the iTunes XML file. This file has all the information of the
	 * iTunes database. The methods used in this function depends on whether
	 * UMS runs on Mac OS X or Windows.
	 *
	 * @return (String) Absolute path to the iTunes XML file.
	 * @throws Exception
	 */
	private static String getiTunesFile(String path) throws Exception {
		if (StringUtils.isNotBlank(path)) {
			return path;
		}
		return PlatformUtils.INSTANCE.getiTunesFile();
	}

	private static boolean areNamesEqual(String aThis, String aThat) {
		Collator collator = Collator.getInstance(Locale.getDefault());
		collator.setStrength(Collator.PRIMARY);
		int comparison = collator.compare(aThis, aThat);

		return (comparison == 0);
	}

}
