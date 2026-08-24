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
package net.pms.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableThumbnails;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAProfileException;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.external.JavaHttpClient;
import net.pms.image.BufferedImageFilterChain;
import net.openhft.hashing.LongHashFunction;
import net.pms.network.HTTPResource;

public class ThumbnailStore {

	/**
	 * The thumbnail generated for an image file, together with the file stamps it was generated from.
	 */
	private record CachedFileThumbnail(long lastModified, long length, long id) {
	}

	/**
	 * Access ordered map that drops the least recently used variant once it is full.
	 */
	private static class TranscodedThumbnailCache extends LinkedHashMap<String, DLNAThumbnail> {

		private static final long serialVersionUID = 1L;

		TranscodedThumbnailCache() {
			super(16, 0.75f, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, DLNAThumbnail> eldest) {
			return size() > MAX_TRANSCODED_THUMBNAILS;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailStore.class.getName());

	private static final Map<Long, WeakReference<DLNAThumbnail>> STORE = new HashMap<>();

	// Thumbnails already generated for a given remote image URL.
	private static final Map<String, Long> URL_THUMBNAIL_IDS = new ConcurrentHashMap<>();

	// Thumbnails already generated for a given image file, by absolute path.
	private static final Map<String, CachedFileThumbnail> FILE_THUMBNAIL_IDS = new ConcurrentHashMap<>();

	// Thumbnails already generated for cover bytes kept under a key, e.g. a MusicBrainz release id.
	private static final Map<String, Long> KEY_THUMBNAIL_IDS = new ConcurrentHashMap<>();

	// A JPEG_TN variant is around 15 kB, the large profiles are a lot bigger, so this is a few tens of MB in the worst case.
	private static final int MAX_TRANSCODED_THUMBNAILS = 500;

	private static final LongHashFunction THUMBNAIL_HASH = LongHashFunction.xx3();

	// Variants of a thumbnail converted to the DLNA profile a renderer asked for.
	private static final Map<String, DLNAThumbnail> TRANSCODED_THUMBNAILS = Collections.synchronizedMap(new TranscodedThumbnailCache());

	private static final BlockingQueue<ThumbnailUpdateRequest> THUMBNAIL_UPDATE_QUEUE = new LinkedBlockingQueue<>();
	private static final AtomicBoolean QUEUE_WORKER_RUNNING = new AtomicBoolean(false);
	private static final String QUEUE_WORKER_THREAD_NAME = "thumbnail-update-worker";

	private static Long tempId = Long.MAX_VALUE;

	public record ThumbnailUpdateRequest(String uri, String filePath, ThumbnailSource thumbnailSource) { }


	private ThumbnailStore() {
		//should not be instantiated
	}

	public static Long getId(DLNAThumbnail thumbnail) {
		if (thumbnail == null) {
			return null;
		}
		Connection connection = null;
		Long id = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				id = MediaTableThumbnails.setThumbnail(connection, thumbnail);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		if (id != null) {
			synchronized (STORE) {
				STORE.put(id, new WeakReference<>(thumbnail));
			}
		}
		return id;
	}

	public static void enqueueThumbnailUpdate(ThumbnailUpdateRequest request) {
		if (request == null) {
			return;
		}
		THUMBNAIL_UPDATE_QUEUE.offer(request);
		startQueueWorkerIfNeeded();
	}

	public static void enqueueThumbnailUpdate(String uri, String filePath, ThumbnailSource thumbnailSource) {
		enqueueThumbnailUpdate(new ThumbnailUpdateRequest(uri, filePath, thumbnailSource));
	}

	private static void startQueueWorkerIfNeeded() {
		if (QUEUE_WORKER_RUNNING.compareAndSet(false, true)) {
			Thread workerThread = new Thread(ThumbnailStore::runQueueWorker, QUEUE_WORKER_THREAD_NAME);
			workerThread.setDaemon(true);
			workerThread.start();
		}
	}

	private static void runQueueWorker() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				ThumbnailUpdateRequest request = THUMBNAIL_UPDATE_QUEUE.take();
				try {
					updateThumbnailByURI(request.uri(), request.filePath(), request.thumbnailSource());
				} catch (Exception e) {
					LOGGER.debug("Error while processing thumbnail update request for filePath: {}", request.filePath(), e);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			QUEUE_WORKER_RUNNING.set(false);
		}
	}

	private static Long updateThumbnailByURI(String uri, String filePath, ThumbnailSource thumbnailSource) {
		if (uri == null || filePath == null || thumbnailSource == null) {
			LOGGER.debug("Cannot update thumbnail because uri/filePath/thumbnailSource is null");
			return null;
		}
		return updateFileThumbnail(filePath, JavaHttpClient.getThumbnail(uri), thumbnailSource);
	}

	/**
	 * Stores a thumbnail that was resolved after the file had already been parsed and makes renderers
	 * notice it.
	 */
	public static Long updateFileThumbnail(String filePath, DLNAThumbnail thumbnail, ThumbnailSource thumbnailSource) {
		if (filePath == null || thumbnail == null || thumbnailSource == null) {
			return null;
		}
		Long fileId = MediaTableFiles.getFileId(filePath);
		Long id = getId(thumbnail);
		if (id == null || fileId == null) {
			LOGGER.debug("id : {} or fileId : {} is null", id, fileId);
			return null;
		}
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFiles.updateThumbnailId(connection, fileId, id, thumbnailSource.toString());
				MediaStoreIds.incrementUpdateIdForFilename(connection, filePath);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return id;
	}

	public static Long getId(DLNAThumbnail thumbnail, Long fileId, ThumbnailSource thumbnailSource) {
		Long id = getId(thumbnail);
		if (id != null && fileId != null) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					MediaTableFiles.updateThumbnailId(connection, fileId, id, thumbnailSource.toString());
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return id;
	}

	public static Long getIdForTvSeries(DLNAThumbnail thumbnail, long tvSeriesId, ThumbnailSource thumbnailSource) {
		Long id = getId(thumbnail);
		if (id != null) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					MediaTableTVSeries.updateThumbnailId(connection, tvSeriesId, id, thumbnailSource.toString());
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return id;
	}

	public static Long getTempId(DLNAThumbnail thumbnail) {
		if (thumbnail == null) {
			return null;
		}
		synchronized (STORE) {
			//resume/temp thumbnail
			Long id = tempId--;
			STORE.put(id, new WeakReference<>(thumbnail));
			return id;
		}
	}

	public static DLNAThumbnail getThumbnail(Long id) {
		if (id == null) {
			return null;
		}
		synchronized (STORE) {
			if (STORE.containsKey(id) && STORE.get(id).get() != null) {
				return STORE.get(id).get();
			}
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					DLNAThumbnail thumbnail = MediaTableThumbnails.getThumbnail(connection, id);
					if (thumbnail != null) {
						STORE.put(id, new WeakReference<>(thumbnail));
						return thumbnail;
					}
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return null;
	}

	public static DLNAThumbnailInputStream getThumbnailInputStream(Long id) {
		DLNAThumbnail thumbnail = getThumbnail(id);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	public static void resetLanguage() {
		synchronized (STORE) {
			STORE.clear();
			TRANSCODED_THUMBNAILS.clear();
			tempId = Long.MAX_VALUE;
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					MediaTableFiles.resetLocalizedThumbnail(connection);
					MediaTableTVSeries.resetLocalizedThumbnail(connection);
					MediaTableThumbnails.cleanup(connection);
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
	}

	/**
	 * Returns a thumbnail for a remote image URL, generating it on first use and reusing it
	 * afterwards. The thumbnail is stored (DB-backed) and remembered per URL.
	 */
	public static DLNAThumbnailInputStream getThumbnailInputStreamForUrl(String url) throws IOException {
		if (url == null) {
			return null;
		}
		Long cachedId = URL_THUMBNAIL_IDS.get(url);
		if (cachedId != null) {
			DLNAThumbnailInputStream cached = getThumbnailInputStream(cachedId);
			if (cached != null) {
				return cached;
			}
		}
		long start = System.currentTimeMillis();
		DLNAThumbnailInputStream generated = DLNAThumbnailInputStream.toThumbnailInputStream(HTTPResource.downloadAndSend(url, true));
		if (generated != null) {
			try {
				Long id = getId(generated.getThumbnail());
				if (id != null) {
					URL_THUMBNAIL_IDS.put(url, id);
				}
			} catch (DLNAProfileException e) {
				LOGGER.trace("Could not cache thumbnail for {}: {}", url, e.getMessage());
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Prepared thumbnail from {} in {} ms", url, System.currentTimeMillis() - start);
			}
		}
		return generated;
	}

	/**
	 * Returns the id of the thumbnail already made for a key
	 */
	public static Long getCachedIdForKey(String key) {
		return key == null ? null : KEY_THUMBNAIL_IDS.get(key);
	}

	/**
	 * Returns a thumbnail for cover bytes that are kept under a key, decoding them only once.
	 */
	public static DLNAThumbnailInputStream getThumbnailInputStreamForKey(String key, Supplier<byte[]> coverSupplier) throws IOException {
		if (key == null || coverSupplier == null) {
			return null;
		}
		Long cachedId = KEY_THUMBNAIL_IDS.get(key);
		if (cachedId != null) {
			DLNAThumbnailInputStream cached = getThumbnailInputStream(cachedId);
			if (cached != null) {
				return cached;
			}
		}
		byte[] cover = coverSupplier.get();
		if (cover == null || cover.length == 0) {
			return null;
		}
		DLNAThumbnailInputStream generated = DLNAThumbnailInputStream.toThumbnailInputStream(cover);
		if (generated != null) {
			try {
				Long id = getId(generated.getThumbnail());
				if (id != null) {
					KEY_THUMBNAIL_IDS.put(key, id);
				}
			} catch (DLNAProfileException e) {
				LOGGER.trace("Could not cache thumbnail for {}: {}", key, e.getMessage());
			}
		}
		return generated;
	}

	/**
	 * Returns a thumbnail for an image file.
	 */
	public static DLNAThumbnailInputStream getThumbnailInputStreamForFile(File file) throws IOException {
		if (file == null || !file.isFile()) {
			return null;
		}
		String path = file.getAbsolutePath();
		long lastModified = file.lastModified();
		long length = file.length();
		CachedFileThumbnail cached = FILE_THUMBNAIL_IDS.get(path);
		if (cached != null && cached.lastModified() == lastModified && cached.length() == length) {
			DLNAThumbnailInputStream stored = getThumbnailInputStream(cached.id());
			if (stored != null) {
				return stored;
			}
		}
		long start = System.currentTimeMillis();
		DLNAThumbnailInputStream generated;
		try (InputStream inputStream = new FileInputStream(file)) {
			generated = DLNAThumbnailInputStream.toThumbnailInputStream(inputStream);
		}
		if (generated != null) {
			try {
				Long id = getId(generated.getThumbnail());
				if (id != null) {
					FILE_THUMBNAIL_IDS.put(path, new CachedFileThumbnail(lastModified, length, id));
				}
			} catch (DLNAProfileException e) {
				LOGGER.trace("Could not cache thumbnail for {}: {}", path, e.getMessage());
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Prepared thumbnail from {} in {} ms", path, System.currentTimeMillis() - start);
			}
		}
		return generated;
	}

	/**
	 * Converts a thumbnail to the DLNA profile a renderer asked for and keeps the result for the next request.
	 */
	public static DLNAThumbnailInputStream getTranscodedThumbnailInputStream(
		DLNAThumbnailInputStream source,
		DLNAImageProfile outputProfile,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		if (source == null) {
			return null;
		}
		if (filterChain != null || outputProfile == null) {
			return source.transcode(outputProfile, padToSize, filterChain);
		}
		String key = Long.toHexString(THUMBNAIL_HASH.hashBytes(source.getBytes(false))) + "|" + outputProfile +
				"|" + outputProfile.getH() + "x" + outputProfile.getV() + "|" + padToSize;
		DLNAThumbnail cached = TRANSCODED_THUMBNAILS.get(key);
		if (cached != null) {
			return new DLNAThumbnailInputStream(cached);
		}
		DLNAThumbnailInputStream transcoded = source.transcode(outputProfile, padToSize, null);
		if (transcoded != null) {
			try {
				TRANSCODED_THUMBNAILS.put(key, transcoded.getThumbnail());
			} catch (DLNAProfileException e) {
				LOGGER.trace("Could not cache the thumbnail converted to {} : {}", outputProfile, e.getMessage());
			}
		}
		return transcoded;
	}

	/**
	 * Deletes all cached thumbnails, both from this in-memory store and from the database (also
	 * clearing the references to them from the FILES and TV_SERIES tables), so thumbnails are
	 * regenerated on demand.
	 */
	public static void deleteAll() {
		synchronized (STORE) {
			STORE.clear();
			// the ids these remember are about to be deleted
			URL_THUMBNAIL_IDS.clear();
			FILE_THUMBNAIL_IDS.clear();
			TRANSCODED_THUMBNAILS.clear();
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					MediaTableThumbnails.deleteAll(connection);
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
	}
}
