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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaTableContainerFiles;
import net.pms.database.MediaTableFiles;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.parsers.WebStreamParser;
import net.pms.renderers.Renderer;
import net.pms.store.PlaylistManager;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.SystemFilesHelper;
import net.pms.store.ThumbnailStore;
import net.pms.store.item.FeedItem;
import net.pms.store.item.RealFile;
import net.pms.store.item.WebAudioStream;
import net.pms.store.item.WebVideoStream;
import net.pms.store.utils.StoreResourceSorter;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;

public final class PlaylistFolder extends StoreContainer {

	private record ResolvedWebEntry(Entry entry, String url, int type, StoreResource resource) {
	}

	private static final class WebEntryResolveException extends RuntimeException {
		private final Entry entry;

		private WebEntryResolveException(Entry entry, String message) {
			super(message);
			this.entry = entry;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistFolder.class);

	public static final String DIRECTIVE_ALBUMART_URI = "#EXTIMG:";
	public static final String DIRECTIVE_RADIOBROWSERUUID = "#RADIOBROWSERUUID:";
	private static final ExecutorService WEB_ENTRY_EXECUTOR =
		Executors.newFixedThreadPool(Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())));

	private final String uri;
	private File uriAsFile;
	private final boolean isweb;
	private final int defaultContent;
	private boolean utf8 = false;

	public PlaylistFolder(Renderer renderer, String name, String uri, int type) {
		super(renderer, name, null);
		this.uri = uri;
		this.uriAsFile = new File(uri);
		isweb = FileUtil.isUrl(uri);
		super.setLastModified(isweb ? 0 : new File(uri).lastModified());
		defaultContent = (type != 0 && type != Format.UNKNOWN) ? type : Format.VIDEO;
		utf8 = "m3u8".equalsIgnoreCase(getExtension().toLowerCase());
	}

	public PlaylistFolder(Renderer renderer, File f) {
		super(renderer, f.getName(), null);
		uri = f.getAbsolutePath();
		this.uriAsFile = f;
		isweb = false;
		super.setLastModified(f.lastModified());
		defaultContent = Format.VIDEO;
		utf8 = "m3u8".equalsIgnoreCase(getExtension().toLowerCase());
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
		return isweb ? uri : ProcessUtil.getSystemPathName(uri);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void discoverChildren() {
		resolve();
	}

	private boolean isM3uPlaylist() {
		if (getExtension() != null) {
			// m3u is not required to have a header, if there is an extension and it matches known m3u, assume m3u
			switch (getExtension()) {
				case "m3u" -> {
					return true;
				}
				case "m3u8" -> {
					return true;
				}
			}
		}
		return false;
	}

	/** Attempts to return a lowercase file extension from uri (excluding the dot) or null */
	private String getExtension() {
		String extension;
		if (FileUtil.isUrl(uri)) {
			extension = FileUtil.getUrlExtension(uri);
		} else {
			extension = FileUtil.getExtension(uri);
		}
		if (extension != null) {
			extension = extension.toLowerCase(PMS.getLocale());
		}
		return extension;
	}

	private BufferedReader getBufferedReader(boolean utf8) throws IOException {
		Charset charset;
		if (utf8) {
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
		if (!isweb) {
			File diskThumbnail = SystemFilesHelper.getFolderThumbnail(uriAsFile.getParentFile(), FilenameUtils.removeExtension(uriAsFile.getName()));
			DLNAThumbnailInputStream result = null;
			try {
				if (diskThumbnail != null) {
					result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(diskThumbnail));
				}
			} catch (IOException e) {
				LOGGER.trace("getThumbnailInputStream", e);
			}

			// Just use folder image as Thumbnail is available
			if (result == null) {
				diskThumbnail = SystemFilesHelper.getFolderThumbnail(uriAsFile.getParentFile());
				if (diskThumbnail != null) {
					try {
						result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(diskThumbnail));
					} catch (IOException e) {
						LOGGER.trace("getThumbnailInputStream", e);
					}
				}
			}

			return result != null ? result : super.getThumbnailInputStream();
		} else {
			DLNAThumbnailInputStream storeThum = ThumbnailStore.getThumbnailInputStream(getLongId());
			return storeThum != null ? storeThum : super.getThumbnailInputStream();
		}
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
		List<CompletableFuture<ResolvedWebEntry>> webEntryFutures = new ArrayList<>();
		for (Entry entry : entries) {
			if (entry == null) {
				continue;
			}
			LOGGER.debug("Adding playlist entry: {}", entry);

			if (!isweb && !FileUtil.isUrl(entry.fileName())) {
				int type = defaultContent;
				String ext = FileUtil.getUrlExtension(entry.fileName());
				if (ext != null) {
					ext = "." + ext;
					Format f = FormatFactory.getAssociatedFormat(ext);
					if (f != null) {
						type = f.getType();
					}
				}
				File en = new File(FilenameUtils.concat(new File(uri).getParent(), entry.fileName()));
				if (en.exists()) {
					if (type == Format.PLAYLIST) {
						addChild(new PlaylistFolder(renderer, en));
					} else {
						addChild(new RealFile(renderer, en, entry.title()));
					}
				}
			} else {
				String u = FileUtil.urlJoin(uri, entry.fileName());
				Integer type = MediaTableFiles.getFormatType(u);
				StoreResource cachedResource = null;
				if (type != null && type != 0 && type != Format.UNKNOWN) {
					LOGGER.debug("Web playlist entry DB hit for {}: type {}", u, type);
					cachedResource = createWebResource(entry, u, type);
				}

				if (cachedResource != null) {
					addChild(cachedResource);
					Long entryId;
					if (cachedResource instanceof StoreContainer storeContainer) {
						entryId = MediaTableFiles.getOrInsertFileId(u, storeContainer.getLastModified(), type);
					} else {
						entryId = MediaTableFiles.getOrInsertFileId(u, 0L, type);
					}
					MediaTableContainerFiles.addContainerEntry(containerId, entryId);
				} else {
					LOGGER.debug("Web playlist entry queued for async resolve: {}", u);
					webEntryFutures.add(CompletableFuture.supplyAsync(() -> resolveWebEntry(entry), WEB_ENTRY_EXECUTOR));
				}
			}
		}

		collectWebFutures(containerId, webEntryFutures);

		if (renderer.getUmsConfiguration().getSortMethod(getPlaylistfile()) == StoreResourceSorter.SORT_RANDOM) {
			Collections.shuffle(getChildren());
		}

		for (StoreResource r : new ArrayList<>(getChildren())) {
			r.syncResolve();
		}
	}

	/**
	 * Asynchronously collects resolved web entries, adds them as children and updates the database. Waits max. up to 3 seconds for resolution,
	 * then adds any remaining unresolved entries to the failedEntries list for later retry or other error handling.
	 *
	 * @param containerId
	 * @param webEntryFutures
	 */
	private void collectWebFutures(Long containerId, List<CompletableFuture<ResolvedWebEntry>> webEntryFutures) {
		if (webEntryFutures.isEmpty()) {
			return;
		}

		try {
			CompletableFuture.allOf(webEntryFutures.toArray(new CompletableFuture[0]))
				.get(3, TimeUnit.SECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			LOGGER.debug("Web playlist entry resolution timed out — collecting completed entries");
		} catch (Exception e) {
			// Individual failures are handled per-future below
		}

		// Collect whichever futures completed within the budget
		List<ResolvedWebEntry> resolvedWebEntries = new ArrayList<>();
		for (CompletableFuture<ResolvedWebEntry> future : webEntryFutures) {
			if (!future.isDone() || future.isCompletedExceptionally() || future.isCancelled()) {
				continue;
			}
			try {
				ResolvedWebEntry resolved = future.getNow(null);
				if (resolved != null && resolved.resource() != null) {
					resolvedWebEntries.add(resolved);
				}
			} catch (Exception e) {
				Throwable cause = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
				LOGGER.debug("Web playlist entry resolve failed", cause);
			}
		}

		for (ResolvedWebEntry resolved : resolvedWebEntries) {
			LOGGER.debug("Web playlist entry async resolved: {} type {}", resolved.url(), resolved.type());
			addChild(resolved.resource());
			Long entryId;
			if (resolved.resource() instanceof StoreContainer storeContainer) {
				entryId = MediaTableFiles.getOrInsertFileId(resolved.url(), storeContainer.getLastModified(), resolved.type());
			} else {
				entryId = MediaTableFiles.getOrInsertFileId(resolved.url(), 0L, resolved.type());
			}
			MediaTableContainerFiles.addContainerEntry(containerId, entryId);
		}

		for (CompletableFuture<ResolvedWebEntry> future : webEntryFutures) {
			if (!future.isDone()) {
				continue;
			}
			if (future.isCompletedExceptionally()) {
				try {
					future.join();
				} catch (CompletionException e) {
					if (e.getCause() instanceof WebEntryResolveException re && re.entry != null) {
						LOGGER.warn("Web playlist entry resolve failed for: {}", re.entry);
					}
				}
			}
		}
	}

	private ResolvedWebEntry resolveWebEntry(Entry entry) {
		String u = FileUtil.urlJoin(uri, entry.fileName());
		int type = WebStreamParser.getWebStreamType(entry.fileName(), defaultContent);
		StoreResource d = createWebResource(entry, u, type);
		if (d == null) {
			throw new WebEntryResolveException(entry, "Unsupported web stream type for entry");
		}
		return new ResolvedWebEntry(entry, u, type, d);
	}

	private StoreResource createWebResource(Entry entry, String url, int type) {
		return switch (type) {
			case Format.VIDEO -> new WebVideoStream(renderer, entry.title(), url, null, entry.directives());
			case Format.AUDIO -> new WebAudioStream(renderer, entry.title(), url, null, entry.directives());
			case Format.IMAGE -> new FeedItem(renderer, entry.title(), url, null, null, Format.IMAGE);
			case Format.PLAYLIST -> PlaylistManager.getPlaylist(renderer, entry.title(), url, 0);
			default -> null;
		};
	}

	public boolean deleteEntry(String url) {
		try (BufferedReader br = getBufferedReader(utf8)) {
			boolean entryRemoved = false;
			StringBuilder out = new StringBuilder();
			StringBuilder lastEntry = new StringBuilder();
			if (!isM3uPlaylist()) {
				LOGGER.debug("deleting playlists entries active only for m3u or m3u8 files. This playlist is {}", getExtension());
				return false;
			}
			out.append(lastEntry);
			String line = "";
			lastEntry = new StringBuilder();
			while (br != null &&  (line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#")) {
					lastEntry.append(line).append(System.getProperty("line.separator"));
				} else if (StringUtils.isAllBlank(line)) {
					lastEntry.append(System.getProperty("line.separator"));
				} else {
					// Parsing finished. This line is the filename.
					if (!line.equalsIgnoreCase(url)) {
						lastEntry.append(line).append(System.getProperty("line.separator"));
						out.append(lastEntry);
					} else {
						entryRemoved = true;
					}
					lastEntry = new StringBuilder();
				}
			}

			if (entryRemoved) {
				writeContentToFile(out);
			}
			return entryRemoved;
		} catch (Exception er) {
			LOGGER.error("deleteEntry", er);
			throw new RuntimeException("deleteEntry", er);
		}
	}

	public void updateAlbumArtUriDirective(String url, String externalAlbumArtUri) {
		boolean firstline = true;
		try (BufferedReader br = getBufferedReader(utf8)) {
			StringBuilder out = new StringBuilder();
			if (!isM3uPlaylist()) {
				LOGGER.debug("updating album art is only possible for m3u or m3u8 files. This playlist is {}", getExtension());
				return;
			}
			String line = "";
			StringBuilder lastEntry = new StringBuilder();
			String lastAlbumArtDirective = null;
			while (br != null &&  (line = br.readLine()) != null) {
				line = line.trim();
				if (firstline) {
					firstline = false;
					if (!"#EXTM3U".equals(line.trim())) {
						LOGGER.debug("adding missing #EXTM3U directive.");
						lastEntry.append("#EXTM3U\n\n");
					}
				}
				if (line.startsWith(DIRECTIVE_ALBUMART_URI)) {
					lastAlbumArtDirective = line.substring(DIRECTIVE_ALBUMART_URI.length());
				} else if (line.startsWith("#")) {
					lastEntry.append(line).append(System.getProperty("line.separator"));
				} else if (StringUtils.isAllBlank(line)) {
					lastEntry.append(System.getProperty("line.separator"));
				} else {
					// Parsing finished. This line is the filename.
					if (line.equalsIgnoreCase(url)) {
						lastEntry.append(DIRECTIVE_ALBUMART_URI).append(externalAlbumArtUri).append(System.getProperty("line.separator"));
					} else if (lastAlbumArtDirective != null) {
						lastEntry.append(DIRECTIVE_ALBUMART_URI).append(lastAlbumArtDirective).append(System.getProperty("line.separator"));
					}
					lastEntry.append(line).append(System.getProperty("line.separator"));
					out.append(lastEntry);
					lastEntry = new StringBuilder();
					lastAlbumArtDirective = null;
				}
			}

			writeContentToFile(out);
			resolve();
		} catch (Exception er) {
			LOGGER.error("updateAlbumArtUriDirective", er);
		}
	}

	private void writeContentToFile(StringBuilder out) throws IOException {
		File file = new File(getFileName());
		BufferedWriter writer = null;
		try {
			LOGGER.debug("writing playlist file ...");
			writer = new BufferedWriter(new FileWriter(file));
			writer.append(out);
			file.setLastModified(System.currentTimeMillis());
		} catch (Exception e) {
			LOGGER.warn("cannot update playlist", e);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private List<Entry> getPlaylistEntries() {
		List<Entry> entries = new ArrayList<>();
		try (BufferedReader br = getBufferedReader(utf8)) {
			String line;
			String title = null;
			String playlistType = getExtension();
			Map<String, String> directives = new HashMap<>();
			// For PLS: accumulate fileName and title per index before creating records
			Map<Integer, String[]> plsData = new HashMap<>();
			while (br != null && (line = br.readLine()) != null) {
				line = line.trim();
				if ("pls".equals(playlistType)) {
					if (line.length() > 0 && !line.startsWith("#")) {
						int eq = line.indexOf('=');
						if (eq != -1) {
							String value = line.substring(eq + 1);
							String valueType = line.substring(0, eq).toLowerCase();
							int index = 0;
							if (valueType.startsWith("file")) {
								index = Integer.parseInt(valueType.substring(4));
								plsData.computeIfAbsent(index, k -> new String[2])[0] = value;
							} else if (valueType.startsWith("title")) {
								index = Integer.parseInt(valueType.substring(5));
								plsData.computeIfAbsent(index, k -> new String[2])[1] = value;
							}
						}
					}
				} else if (isM3uPlaylist()) {
					if (line.startsWith("#EXTINF:")) {
						line = line.substring(8).trim();
						if (line.matches("^-?\\d+,.+")) {
							title = line.substring(line.indexOf(',') + 1).trim();
						} else {
							title = line;
						}
					} else if (line.toUpperCase().startsWith(DIRECTIVE_RADIOBROWSERUUID)) {
						directives.put(DIRECTIVE_RADIOBROWSERUUID, line.substring(DIRECTIVE_RADIOBROWSERUUID.length()));
					} else if (line.toUpperCase().startsWith(DIRECTIVE_ALBUMART_URI)) {
						directives.put(DIRECTIVE_ALBUMART_URI, line.substring(DIRECTIVE_ALBUMART_URI.length()));
					} else if (!line.startsWith("#") && !line.matches("^\\s*$")) {
						entries.add(new Entry(line, title, directives));
						title = null;
						directives = new HashMap<>();
					}
				}
			}
			// Build PLS entries sorted by index
			plsData.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(e -> entries.add(new Entry(e.getValue()[0], e.getValue()[1], null)));
		} catch (NumberFormatException | IOException e) {
			LOGGER.error(null, e);
		}
		return entries;
	}

	private record Entry(String fileName, String title, Map<String, String> directives) {
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
