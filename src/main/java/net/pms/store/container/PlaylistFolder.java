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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableContainerFiles;
import net.pms.database.MediaTableFiles;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.image.ImageFormat;
import net.pms.parsers.WebStreamParser;
import net.pms.renderers.Renderer;
import net.pms.store.MediaInfoStore;
import net.pms.store.MediaStore;
import net.pms.store.MediaStoreIds;
import net.pms.store.PlaylistManager;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.SystemFilesHelper;
import net.pms.store.ThumbnailStore;
import net.pms.store.item.FeedItem;
import net.pms.store.item.RealFile;
import net.pms.store.item.WebAudioStream;
import net.pms.store.item.WebStream;
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
	public static final String DIRECTIVE_RATING = "#EXTRATING:";
	public static final String DIRECTIVE_PLAYLIST_RATING = "#EXTPLAYLISTRATING:";

	// How long the first browse of a web playlist waits for its entries.
	private static final long WEB_ENTRY_BUDGET_SECONDS = 2;

	// How long it waits when there is still nothing to show at all.
	private static final long WEB_ENTRY_EMPTY_BUDGET_SECONDS = 15;

	// Set once the browse has been answered, so entries that resolve later make renderers refresh.
	private final AtomicBoolean webEntriesResponded = new AtomicBoolean(false);

	private static final ExecutorService WEB_ENTRY_EXECUTOR =
		Executors.newFixedThreadPool(Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())), r -> {
			Thread thread = new Thread(r, "web-playlist-entry");
			thread.setDaemon(true);
			return thread;
		});

	// A nested playlist must not wait for entries that need the pool it is running on.
	private static final ThreadLocal<Boolean> IN_WEB_ENTRY_POOL = ThreadLocal.withInitial(() -> false);

	// Reads the files of a playlist ahead of the sequential handling below.
	private static final ExecutorService ENTRY_PREFETCH_EXECUTOR =
		Executors.newFixedThreadPool(Math.max(4, Math.min(8, Runtime.getRuntime().availableProcessors() * 2)),
			runnable -> {
				Thread thread = new Thread(runnable, "playlist-entry-prefetch");
				thread.setDaemon(true);
				return thread;
			});

	private final String uri;
	private File uriAsFile;
	private final boolean isweb;
	private final int defaultContent;
	private boolean utf8 = false;
	private String playlistRatingDirective = null;

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

	/**
	 * GETSYSTEMNAME() returns a file path or an URL, which identifies this
	 * container globally, so GETRATINGKEY() can use it as is.
	 */
	@Override
	protected boolean hasGlobalRatingKey() {
		return true;
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

	private Charset getCharset() {
		return utf8 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1;
	}

	private BufferedReader getBufferedReader(boolean utf8) throws IOException {
		Charset charset = utf8 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1;
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

	/**
	 * The playlist's picture, most specific source first:
	 *
	 * 1. an image named after the playlist
	 * 2. a cover set through UpdateObject
	 * 3. the folder.* picture of the directory
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (!isweb) {
			DLNAThumbnailInputStream named = readDiskThumbnail(
				SystemFilesHelper.getFolderThumbnail(uriAsFile.getParentFile(), FilenameUtils.removeExtension(uriAsFile.getName()))
			);
			if (named != null) {
				return named;
			}
		}

		DLNAThumbnailInputStream userThumbnail = getUserThumbnailInputStream();
		if (userThumbnail != null) {
			return userThumbnail;
		}

		if (!isweb) {
			DLNAThumbnailInputStream folderImage = readDiskThumbnail(SystemFilesHelper.getFolderThumbnail(uriAsFile.getParentFile()));
			if (folderImage != null) {
				return folderImage;
			}
		}
		return super.getThumbnailInputStream();
	}

	private static DLNAThumbnailInputStream readDiskThumbnail(File diskThumbnail) {
		if (diskThumbnail == null) {
			return null;
		}
		try {
			return ThumbnailStore.getThumbnailInputStreamForFile(diskThumbnail);
		} catch (IOException e) {
			LOGGER.trace("getThumbnailInputStream", e);
			return null;
		}
	}

	/**
	 * The thumbnail a user set for this playlist
	 */
	private DLNAThumbnailInputStream getUserThumbnailInputStream() {
		Long thumbnailId = MediaInfoStore.getStoredUserThumbnailId(getFileName());
		if (thumbnailId == null) {
			// The playlist file usually has no media info in memory, so the stored id is the only source.
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				thumbnailId = MediaTableFiles.getUserThumbnailId(connection, getFileName());
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return thumbnailId != null ? ThumbnailStore.getThumbnailInputStream(thumbnailId) : null;
	}

	/**
	 * Stores a cover next to the playlist file.
	 */
	public File writeCoverFile(DLNAThumbnail thumbnail) throws IOException {
		File playlistFile = getPlaylistfile();
		if (playlistFile == null || thumbnail == null) {
			return null;
		}
		String extension = thumbnailExtension(thumbnail.getFormat());
		if (extension == null) {
			LOGGER.debug("not storing the cover of {} : {} is not a thumbnail format", playlistFile, thumbnail.getFormat());
			return null;
		}
		if (SystemFilesHelper.isFolderThumbnail(playlistFile, false)) {
			LOGGER.warn("not storing the cover of {} : the name is reserved for the folder thumbnail", playlistFile);
			return null;
		}
		File coverFile = FileUtil.replaceExtension(playlistFile, extension, false, true);
		if (coverFile == null) {
			return null;
		}
		Files.write(coverFile.toPath(), thumbnail.getBytes(false));
		removeStaleCoverFiles(playlistFile, coverFile);
		LOGGER.debug("stored the cover of {} as {}", playlistFile, coverFile);
		return coverFile;
	}

	/**
	 * Drops the cover files of this playlist that the new one replaces.
	 */
	private static void removeStaleCoverFiles(File playlistFile, File coverFile) {
		for (String extension : SystemFilesHelper.THUMBNAIL_EXTENSIONS) {
			File stale = FileUtil.replaceExtension(playlistFile, extension, true, true);
			if (stale != null && !stale.equals(coverFile) && !stale.delete()) {
				LOGGER.warn("cannot remove the previous cover {} of {}", stale, playlistFile);
			}
		}
	}

	/**
	 * The file extension a picture.
	 */
	private static String thumbnailExtension(ImageFormat format) {
		if (format == null) {
			return null;
		}
		return switch (format) {
			case JPEG -> "jpg";
			case PNG -> "png";
			case GIF -> "gif";
			case BMP -> "bmp";
			case WEBP -> "webp";
			default -> null;
		};
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

	/**
	 * Parses the files of this playlist in parallel, so the loop below finds their metadata ready.
	 *
	 * Nothing is added to the tree here: MediaInfoStore keeps what it read, and the sequential pass
	 * then only picks it up. Failures are ignored on purpose - whatever did not work here is simply
	 * done again, and reported, where the entry is actually added.
	 *
	 * @param entries the entries of this playlist
	 */
	private void prefetchLocalEntries(List<Entry> entries) {
		if (entries == null || entries.size() < 2) {
			return;
		}
		String parent = new File(uri).getParent();
		if (isweb || parent == null) {
			return;
		}
		List<CompletableFuture<Void>> pending = new ArrayList<>();
		for (Entry entry : entries) {
			if (entry == null || FileUtil.isUrl(entry.fileName())) {
				continue;
			}
			File file = new File(FilenameUtils.concat(parent, entry.fileName()));
			if (!file.isFile()) {
				continue;
			}
			String ext = FileUtil.getUrlExtension(entry.fileName());
			Format format = ext != null ? FormatFactory.getAssociatedFormat("." + ext) : null;
			if (format == null || format.getType() == Format.PLAYLIST) {
				continue;
			}
			pending.add(CompletableFuture.runAsync(MediaStore.asRequestWork(() -> {
				try {
					MediaInfoStore.getMediaInfo(file.getAbsolutePath(), file, format, format.getType());
				} catch (Exception e) {
					LOGGER.trace("Could not read \"{}\" ahead of time: {}", file.getName(), e.getMessage());
				}
			}), ENTRY_PREFETCH_EXECUTOR));
		}
		if (pending.isEmpty()) {
			return;
		}
		long started = System.currentTimeMillis();
		CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).join();
		LOGGER.debug("Read {} playlist entries of \"{}\" ahead in {} ms",
			pending.size(), getName(), System.currentTimeMillis() - started);
	}

	@Override
	protected void resolveOnce() {
		Long containerId = MediaTableFiles.getOrInsertFileId(uri, getLastModified(), Format.PLAYLIST);

		List<Entry> entries = getPlaylistEntries();
		applyPlaylistRatingDirective();
		prefetchLocalEntries(entries);
		webEntriesResponded.set(false);
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
					webEntryFutures.add(CompletableFuture.supplyAsync(() -> resolveWebEntry(entry), WEB_ENTRY_EXECUTOR)
						.whenComplete((resolved, failure) -> addResolvedWebEntry(containerId, resolved, failure)));
				}
			}
		}

		awaitWebEntries(webEntryFutures);
		// Everything still in flight announces itself through the update id from here on.
		webEntriesResponded.set(true);

		if (renderer.getUmsConfiguration().getSortMethod(getPlaylistfile()) == StoreResourceSorter.SORT_RANDOM) {
			Collections.shuffle(getChildren());
		}

		for (StoreResource r : new ArrayList<>(getChildren())) {
			r.syncResolve();
		}
	}

	/**
	 * Gives the entries of a web playlist a short moment to arrive, so the first answer is not empty.
	 */
	private void awaitWebEntries(List<CompletableFuture<ResolvedWebEntry>> webEntryFutures) {
		if (webEntryFutures.isEmpty()) {
			return;
		}
		if (Boolean.TRUE.equals(IN_WEB_ENTRY_POOL.get())) {
			LOGGER.debug("Not waiting for {} web playlist entries, this is already a resolving thread", webEntryFutures.size());
			return;
		}
		CompletableFuture<Void> all = CompletableFuture.allOf(webEntryFutures.toArray(new CompletableFuture[0]));
		if (awaitWebEntries(all, WEB_ENTRY_BUDGET_SECONDS) || !getChildren().isEmpty()) {
			return;
		}
		LOGGER.debug("{} web playlist entries are not resolved within {} s and there is nothing to show, waiting up to {} s",
			webEntryFutures.size(), WEB_ENTRY_BUDGET_SECONDS, WEB_ENTRY_EMPTY_BUDGET_SECONDS);
		awaitWebEntries(all, WEB_ENTRY_EMPTY_BUDGET_SECONDS - WEB_ENTRY_BUDGET_SECONDS);
	}

	/**
	 * Waits the given number of seconds for the entries. Returns false when they are still in flight.
	 */
	private boolean awaitWebEntries(CompletableFuture<Void> all, long seconds) {
		try {
			all.get(seconds, TimeUnit.SECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			// Individual failures are reported by the completion handler
		}
		return true;
	}

	/**
	 * Adds a resolved web entry and stores it. A late entry bumps the update id of this folder, so renderers ask again and see it.
	 */
	private void addResolvedWebEntry(Long containerId, ResolvedWebEntry resolved, Throwable failure) {
		if (failure != null) {
			Throwable cause = (failure instanceof CompletionException && failure.getCause() != null) ? failure.getCause() : failure;
			if (cause instanceof WebEntryResolveException re && re.entry != null) {
				LOGGER.warn("Web playlist entry resolve failed for: {}", re.entry);
			} else {
				LOGGER.debug("Web playlist entry resolve failed", cause);
			}
			return;
		}
		if (resolved == null || resolved.resource() == null) {
			return;
		}
		try {
			LOGGER.debug("Web playlist entry async resolved: {} type {}", resolved.url(), resolved.type());
			addChild(resolved.resource());
			resolved.resource().syncResolve();
			Long entryId;
			if (resolved.resource() instanceof StoreContainer storeContainer) {
				entryId = MediaTableFiles.getOrInsertFileId(resolved.url(), storeContainer.getLastModified(), resolved.type());
			} else {
				entryId = MediaTableFiles.getOrInsertFileId(resolved.url(), 0L, resolved.type());
			}
			MediaTableContainerFiles.addContainerEntry(containerId, entryId);
			if (webEntriesResponded.get()) {
				// The browse is already answered, so the renderer only learns about this entry through the update id.
				MediaStoreIds.incrementUpdateId(getLongId());
			}
		} catch (Exception e) {
			LOGGER.debug("Could not add the resolved web playlist entry {}: {}", resolved.url(), e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private ResolvedWebEntry resolveWebEntry(Entry entry) {
		IN_WEB_ENTRY_POOL.set(true);
		String u = FileUtil.urlJoin(uri, entry.fileName());
		int type = WebStreamParser.getWebStreamType(entry.fileName(), defaultContent);
		StoreResource d = createWebResource(entry, u, type);
		if (d == null) {
			throw new WebEntryResolveException(entry, "Unsupported web stream type for entry");
		}
		if (d instanceof WebStream) {
			MediaInfoStore.getWebStreamMediaInfo(u, type);
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

	/**
	 * Appends a web resource.The url has to come last!
	 */
	public synchronized boolean addWebEntry(String url, String title, String albumArtUri, String radioBrowserUuid) {
		if (StringUtils.isBlank(url)) {
			return false;
		}
		if (!isM3uPlaylist()) {
			LOGGER.debug("adding entries is only possible for m3u or m3u8 files. This playlist is {}", getExtension());
			return false;
		}
		String entryUrl = url.trim();
		try (BufferedReader br = getBufferedReader(utf8)) {
			StringBuilder out = new StringBuilder();
			String nl = System.getProperty("line.separator");
			boolean firstline = true;
			String line;
			while (br != null && (line = br.readLine()) != null) {
				line = line.trim();
				if (firstline) {
					firstline = false;
					if (!"#EXTM3U".equals(line)) {
						LOGGER.debug("adding missing #EXTM3U directive.");
						out.append("#EXTM3U").append(nl).append(nl);
					}
				}
				if (line.equalsIgnoreCase(entryUrl)) {
					LOGGER.debug("{} is already in this playlist", entryUrl);
					return false;
				}
				out.append(line).append(nl);
			}
			if (firstline) {
				// the file was empty
				out.append("#EXTM3U").append(nl).append(nl);
			}
			out.append(nl);
			out.append("#EXTINF:-1,").append(StringUtils.isBlank(title) ? entryUrl : title.trim()).append(nl);
			if (StringUtils.isNotBlank(radioBrowserUuid)) {
				out.append(DIRECTIVE_RADIOBROWSERUUID).append(radioBrowserUuid.trim()).append(nl);
			}
			if (StringUtils.isNotBlank(albumArtUri)) {
				out.append(DIRECTIVE_ALBUMART_URI).append(albumArtUri.trim()).append(nl);
			}
			out.append(entryUrl).append(nl);
			writeContentToFile(out);
		} catch (Exception er) {
			LOGGER.error("cannot add {} to playlist", entryUrl, er);
			return false;
		}
		warmWebEntry(entryUrl);
		// the children were built from the old file, so the next browse has to read it again
		setDiscovered(false);
		// let the renderers know the playlist changed
		MediaStoreIds.incrementUpdateIdForFilename(getFileName());
		return true;
	}

	/**
	 * Reads a newly added web entry once and records its format, so resolving the playlist afterwards
	 * finds it in the database and does not have to wait for the network again.
	 */
	private void warmWebEntry(String url) {
		try {
			int type = WebStreamParser.getWebStreamType(url, defaultContent);
			MediaInfoStore.getWebStreamMediaInfo(url, type);
			MediaTableFiles.getOrInsertFileId(url, 0L, type);
		} catch (Exception e) {
			LOGGER.debug("could not resolve the new playlist entry {} up front: {}", url, e.getMessage());
		}
	}

	public void updateAlbumArtUriDirective(String url, String externalAlbumArtUri) {
		updateDirective(url, DIRECTIVE_ALBUMART_URI, externalAlbumArtUri);
	}

	/**
	 * A NULL rating removes the directive, so an entry that is no longer rated does not keep an old value in the file.
	 */
	public void updateRatingDirective(String url, Integer rating) {
		updateDirective(url, DIRECTIVE_RATING, rating == null ? null : rating.toString());
	}

	/**
	 * Stores the rating of the playlist itself in its header.
	 */
	public synchronized void updatePlaylistRatingDirective(Integer rating) {
		if (!isM3uPlaylist()) {
			LOGGER.debug("updating {} is only possible for m3u or m3u8 files. This playlist is {}",
				DIRECTIVE_PLAYLIST_RATING, getExtension());
			return;
		}
		String value = rating == null ? null : rating.toString();
		try (BufferedReader br = getBufferedReader(utf8)) {
			StringBuilder out = new StringBuilder();
			String nl = System.getProperty("line.separator");
			out.append("#EXTM3U").append(nl);
			if (value != null) {
				out.append(DIRECTIVE_PLAYLIST_RATING).append(value).append(nl);
			}
			String line;
			boolean firstline = true;
			while (br != null && (line = br.readLine()) != null) {
				String trimmed = line.trim();
				if (firstline) {
					firstline = false;
					if ("#EXTM3U".equals(trimmed)) {
						continue;
					}
					LOGGER.debug("adding missing #EXTM3U directive.");
				}
				if (trimmed.toUpperCase().startsWith(DIRECTIVE_PLAYLIST_RATING)) {
					continue;
				}
				out.append(trimmed).append(nl);
			}
			playlistRatingDirective = value;
			writeContentToFile(out);
		} catch (Exception er) {
			LOGGER.error("cannot update {} in playlist", DIRECTIVE_PLAYLIST_RATING, er);
		}
	}

	private void applyPlaylistRatingDirective() {
		if (playlistRatingDirective == null || getRating() != null) {
			return;
		}
		try {
			setRating(Integer.valueOf(playlistRatingDirective));
		} catch (NumberFormatException e) {
			LOGGER.debug("Ignoring the playlist rating directive \"{}\" of {} because it is not a number",
				playlistRatingDirective, getFileName());
		}
	}

	/**
	 * Rewrites the playlist with a new value for one directive of one entry.
	 */
	private synchronized void updateDirective(String url, String directive, String value) {
		boolean firstline = true;
		try (BufferedReader br = getBufferedReader(utf8)) {
			StringBuilder out = new StringBuilder();
			if (!isM3uPlaylist()) {
				LOGGER.debug("updating {} is only possible for m3u or m3u8 files. This playlist is {}", directive, getExtension());
				return;
			}
			String line = "";
			StringBuilder lastEntry = new StringBuilder();
			String lastDirective = null;
			while (br != null &&  (line = br.readLine()) != null) {
				line = line.trim();
				if (firstline) {
					firstline = false;
					if (!"#EXTM3U".equals(line.trim())) {
						LOGGER.debug("adding missing #EXTM3U directive.");
						lastEntry.append("#EXTM3U\n\n");
					}
				}
				if (line.toUpperCase().startsWith(directive)) {
					lastDirective = line.substring(directive.length());
				} else if (line.startsWith("#")) {
					lastEntry.append(line).append(System.getProperty("line.separator"));
				} else if (StringUtils.isAllBlank(line)) {
					lastEntry.append(System.getProperty("line.separator"));
				} else {
					// Parsing finished. This line is the filename.
					if (line.equalsIgnoreCase(url)) {
						if (value != null) {
							lastEntry.append(directive).append(value).append(System.getProperty("line.separator"));
						}
					} else if (lastDirective != null) {
						lastEntry.append(directive).append(lastDirective).append(System.getProperty("line.separator"));
					}
					lastEntry.append(line).append(System.getProperty("line.separator"));
					out.append(lastEntry);
					lastEntry = new StringBuilder();
					lastDirective = null;
				}
			}
			// whatever follows the last entry - a trailing comment for example - would be lost otherwise
			out.append(lastEntry);

			writeContentToFile(out);
		} catch (Exception er) {
			LOGGER.error("cannot update {} in playlist", directive, er);
		}
	}

	private void writeContentToFile(StringBuilder out) throws IOException {
		File file = new File(getFileName());
		BufferedWriter writer = null;
		try {
			LOGGER.debug("writing playlist file ...");
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), getCharset()));
			writer.append(out);
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
					} else if (line.toUpperCase().startsWith(DIRECTIVE_PLAYLIST_RATING)) {
						playlistRatingDirective = line.substring(DIRECTIVE_PLAYLIST_RATING.length()).trim();
					} else if (line.toUpperCase().startsWith(DIRECTIVE_RATING)) {
						directives.put(DIRECTIVE_RATING, line.substring(DIRECTIVE_RATING.length()));
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
