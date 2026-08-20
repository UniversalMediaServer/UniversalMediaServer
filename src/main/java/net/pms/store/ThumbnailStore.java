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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableThumbnails;
import net.pms.dlna.DLNAProfileException;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.external.JavaHttpClient;
import net.pms.network.HTTPResource;

public class ThumbnailStore {

	/**
	 * The thumbnail generated for an image file, together with the file stamps it was generated from.
	 */
	private record CachedFileThumbnail(long lastModified, long length, long id) {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailStore.class.getName());

	private static final Map<Long, WeakReference<DLNAThumbnail>> STORE = new HashMap<>();
	// Thumbnails already generated for a given remote image URL.
	private static final Map<String, Long> URL_THUMBNAIL_IDS = new ConcurrentHashMap<>();
	// Thumbnails already generated for a given image file, by absolute path.
	private static final Map<String, CachedFileThumbnail> FILE_THUMBNAIL_IDS = new ConcurrentHashMap<>();

	private static Long tempId = Long.MAX_VALUE;

	private ThumbnailStore() {
		//should not be instantiated
	}

	public static Long getId(DLNAThumbnail thumbnail) {
		if (thumbnail == null) {
			return null;
		}
		synchronized (STORE) {
			Connection connection = null;
			Long id = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					id = MediaTableThumbnails.setThumbnail(connection, thumbnail);
					if (id != null) {
						STORE.put(id, new WeakReference<>(thumbnail));
					}
				}
			} finally {
				MediaDatabase.close(connection);
			}
			return id;
		}
	}

	public static Long updateThumbnailByURI(String uri, String filePath, ThumbnailSource thumbnailSource) {
		DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(uri);
		Long fileId = MediaTableFiles.getFileId(filePath);

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
		} else {
			LOGGER.debug("id : {} or fileId : {} is null", id, fileId);
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
