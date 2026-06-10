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

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableThumbnails;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.external.JavaHttpClient;

public class ThumbnailStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailStore.class.getName());

	private static final Map<Long, WeakReference<DLNAThumbnail>> STORE = new HashMap<>();
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

}
