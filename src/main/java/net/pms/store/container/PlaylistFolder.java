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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pms.PMS;
import net.pms.database.MediaTableContainerFiles;
import net.pms.database.MediaTableFiles;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.parsers.WebStreamParser;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.renderers.Renderer;
import net.pms.store.PlaylistManager;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.item.FeedItem;
import net.pms.store.item.RealFile;
import net.pms.store.item.WebAudioStream;
import net.pms.store.item.WebVideoStream;
import net.pms.store.utils.StoreResourceSorter;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlaylistFolder extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistFolder.class);

	private final String uri;
	private final boolean isweb;
	private final int defaultContent;

	public PlaylistFolder(Renderer renderer, String name, String uri, int type) {
		super(renderer, name, null);
		this.uri = uri;
		isweb = FileUtil.isUrl(uri);
		super.setLastModified(isweb ? 0 : new File(uri).lastModified());
		defaultContent = (type != 0 && type != Format.UNKNOWN) ? type : Format.VIDEO;
	}

	public PlaylistFolder(Renderer renderer, File f) {
		super(renderer, f.getName(), null);
		uri = f.getAbsolutePath();
		isweb = false;
		super.setLastModified(f.lastModified());
		defaultContent = Format.VIDEO;
	}

	public File getPlaylistfile() {
		return isweb ? null : new File(uri);
	}

	@Override
	public boolean allowScan() {
		return true;
	}

	@Override
	public String getSystemName() {
		return isweb ? uri : ProcessUtil.getShortFileNameIfWideChars(uri);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void discoverChildren() {
		resolve();
	}

	private BufferedReader getBufferedReader() throws IOException {
		String extension;
		Charset charset;
		if (FileUtil.isUrl(uri)) {
			extension = FileUtil.getUrlExtension(uri);
		} else {
			extension = FileUtil.getExtension(uri);
		}
		if (extension != null) {
			extension = extension.toLowerCase(PMS.getLocale());
		}
		if (extension != null && (extension.equals("m3u8") || extension.equals(".cue"))) {
			charset = StandardCharsets.UTF_8;
		} else {
			charset = StandardCharsets.ISO_8859_1;
		}
		if (FileUtil.isUrl(uri)) {
			BOMInputStream bi = BOMInputStream.builder().setInputStream(URI.create(uri).toURL().openStream()).get();
			return new BufferedReader(new InputStreamReader(bi, charset));
		} else {
			File playlistfile = new File(uri);
			if (playlistfile.length() < 10000000) {
				FileInputStream inputStream = new FileInputStream(playlistfile);
				BOMInputStream bi = BOMInputStream.builder().setInputStream(inputStream).get();
				return new BufferedReader(new InputStreamReader(bi, charset));
			}
		}
		return null;
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		File thumbnailImage;
		if (!isweb) {
			thumbnailImage = new File(FilenameUtils.removeExtension(uri) + ".png");
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				thumbnailImage = new File(FilenameUtils.removeExtension(uri) + ".jpg");
			}
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				thumbnailImage = new File(FilenameUtils.getFullPath(uri) + "folder.png");
			}
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				thumbnailImage = new File(FilenameUtils.getFullPath(uri) + "folder.jpg");
			}
			if (!thumbnailImage.exists() || thumbnailImage.isDirectory()) {
				return super.getThumbnailInputStream();
			}
			DLNAThumbnailInputStream result = null;
			try {
				LOGGER.debug("PlaylistFolder albumart path : " + thumbnailImage.getAbsolutePath());
				result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(thumbnailImage));
			} catch (IOException e) {
				LOGGER.debug("An error occurred while getting thumbnail for \"{}\", using generic thumbnail instead: {}", getName(),
						e.getMessage());
				LOGGER.trace("", e);
			}
			return result != null ? result : super.getThumbnailInputStream();
		}
		return null;
	}

	@Override
	public void resolve() {
		getChildren().clear();
		File playlistFile = getPlaylistfile();
		if (playlistFile != null) {
			setLastModified(playlistFile.lastModified());
		}
		resolveOnce();
	}

	@Override
	protected void resolveOnce() {
		Long containerId = MediaTableFiles.getOrInsertFileId(uri, getLastModified(), Format.PLAYLIST);

		List<Entry> entries = getPlaylistEntries();
		for (Entry entry : entries) {
			if (entry == null) {
				continue;
			}
			LOGGER.debug("Adding playlist entry: {}", entry);

			if (!isweb && !FileUtil.isUrl(entry.fileName)) {
				int type = defaultContent;
				String ext = FileUtil.getUrlExtension(entry.fileName);
				if (ext != null) {
					ext = "." + ext;
					Format f = FormatFactory.getAssociatedFormat(ext);
					if (f != null) {
						type = f.getType();
					}
				}
				File en = new File(FilenameUtils.concat(new File(uri).getParent(), entry.fileName));
				if (en.exists()) {
					if (type == Format.PLAYLIST) {
						addChild(new PlaylistFolder(renderer, en));
					} else {
						addChild(new RealFile(renderer, en, entry.title));
					}
				}
			} else {
				String u = FileUtil.urlJoin(uri, entry.fileName);
				Integer type = MediaTableFiles.getFormatType(u);
				if (type == null || type == 0) {
					type = WebStreamParser.getWebStreamType(entry.fileName, defaultContent);
				}
				StoreResource d = switch (type) {
					case Format.VIDEO -> new WebVideoStream(renderer, entry.title, u, null, entry.directives);
					case Format.AUDIO -> new WebAudioStream(renderer, entry.title, u, null, entry.directives);
					case Format.IMAGE -> new FeedItem(renderer, entry.title, u, null, null, Format.IMAGE);
					case Format.PLAYLIST -> PlaylistManager.getPlaylist(renderer, entry.title, u, 0);
					default -> null;
				};

				if (d != null) {
					addChild(d);
					Long entryId;
					if (d instanceof StoreContainer storeContainer) {
						entryId = MediaTableFiles.getOrInsertFileId(u, storeContainer.getLastModified(), type);
					} else {
						entryId = MediaTableFiles.getOrInsertFileId(u, 0L, type);
					}
					MediaTableContainerFiles.addContainerEntry(containerId, entryId);
				}
			}
		}

		if (renderer.getUmsConfiguration().getSortMethod(getPlaylistfile()) == StoreResourceSorter.SORT_RANDOM) {
			Collections.shuffle(getChildren());
		}

		for (StoreResource r : getChildren()) {
			r.syncResolve();
		}
	}

	private List<Entry> getPlaylistEntries() {
		List<Entry> entries = new ArrayList<>();
		boolean m3u = false;
		boolean pls = false;
		try (BufferedReader br = getBufferedReader()) {
			String line;
			while (!m3u && !pls && br != null && (line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#EXTM3U")) {
					m3u = true;
					LOGGER.debug("Reading m3u playlist: " + getName());
				} else if (line.length() > 0 && line.equals("[playlist]")) {
					pls = true;
					LOGGER.debug("Reading PLS playlist: " + getName());
				}
			}
			String fileName;
			String title = null;
			Map<String, String> directives = new HashMap<>();
			while (br != null &&  (line = br.readLine()) != null) {
				line = line.trim();
				if (pls) {
					if (line.length() > 0 && !line.startsWith("#")) {
						int eq = line.indexOf('=');
						if (eq != -1) {
							String value = line.substring(eq + 1);
							String valueType = line.substring(0, eq).toLowerCase();
							fileName = null;
							title = null;
							int index = 0;
							if (valueType.startsWith("file")) {
								index = Integer.parseInt(valueType.substring(4));
								fileName = value;
							} else if (valueType.startsWith("title")) {
								index = Integer.parseInt(valueType.substring(5));
								title = value;
							}
							if (index > 0) {
								while (entries.size() < index) {
									entries.add(null);
								}
								Entry entry = entries.get(index - 1);
								if (entry == null) {
									entry = new Entry();
									entries.set(index - 1, entry);
								}
								if (fileName != null) {
									entry.fileName = fileName;
								}
								if (title != null) {
									entry.title = title;
								}
							}
						}
					}
				} else if (m3u) {
					if (line.startsWith("#EXTINF:")) {
						line = line.substring(8).trim();
						if (line.matches("^-?\\d+,.+")) {
							title = line.substring(line.indexOf(',') + 1).trim();
						} else {
							title = line;
						}
					} else if (line.startsWith("#RADIOBROWSERUUID:")) {
						directives.put("RADIOBROWSERUUID", line.substring(18));
					} else if (!line.startsWith("#") && !line.matches("^\\s*$")) {
						// Non-comment and non-empty line contains the filename
						fileName = line;
						Entry entry = new Entry();
						entry.fileName = fileName;
						entry.title = title;
						entry.directives = directives;
						entries.add(entry);
						title = null;
						directives = new HashMap<>();
					}
				}
			}
		} catch (NumberFormatException | IOException e) {
			LOGGER.error(null, e);
		}
		return entries;
	}

	private static class Entry {

		private String fileName;
		private String title;
		private Map<String, String> directives;

		@Override
		public String toString() {
			return "[" + fileName + "," + title + "]";
		}
	}

	@Override
	public String getDisplayName(boolean withSuffix) {
		String displayName = super.getDisplayNameBase();
		if (displayName.contains(".")) {
			displayName = displayName.substring(0, displayName.lastIndexOf("."));
		}
		return displayName;
	}

}
