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

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.http.HttpHeaders;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFailedLookups;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.external.tmdb.TMDB;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.media.video.metadata.TvSeriesMetadata;
import net.pms.parsers.FFmpegParser;
import net.pms.parsers.Parser;
import net.pms.parsers.WebStreamParser;
import net.pms.util.FileNameMetadata;
import net.pms.util.FileUtil;
import net.pms.util.InputFile;
import net.pms.util.ResourceIdentifier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaInfoStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfoStore.class);

	private enum MetadataPhase {
		LOCK, STAT, CACHE, CONNECTION, TRANSACTION_SETUP, READ, HASH, PARSE, PARSE_WAIT, WRITE, COMMIT, OTHER
	}

	/** Per-call timings; phases are exclusive so nested parsing is not counted twice. */
	private static final class MetadataLoadTimer {
		private final long startedAt = System.nanoTime();
		private final long[] nanos = new long[MetadataPhase.values().length];
		private long phaseStartedAt = startedAt;
		private MetadataPhase current = MetadataPhase.OTHER;
		private String source = "unavailable";
		private String parser;

		private void phase(MetadataPhase next) {
			long now = System.nanoTime();
			nanos[current.ordinal()] += now - phaseStartedAt;
			phaseStartedAt = now;
			current = next;
		}

		private long ms(MetadataPhase phase) {
			return nanos[phase.ordinal()] / 1_000_000;
		}

		private void finish(String filename) {
			phase(MetadataPhase.OTHER);
			long totalMs = (phaseStartedAt - startedAt) / 1_000_000;
			if (totalMs >= 50) {
				LOGGER.info("Slow metadata load for \"{}\": {} ms total, source {}, parser {}; lock {} ms, file stat {} ms, memory cache {} ms, connection {} ms, transaction setup {} ms, database read {} ms, file hash {} ms, parse {} ms, parser wait {} ms, database write {} ms, commit/close {} ms, other {} ms",
					filename, totalMs, source, parser, ms(MetadataPhase.LOCK), ms(MetadataPhase.STAT), ms(MetadataPhase.CACHE),
					ms(MetadataPhase.CONNECTION), ms(MetadataPhase.TRANSACTION_SETUP), ms(MetadataPhase.READ), ms(MetadataPhase.HASH),
					ms(MetadataPhase.PARSE), ms(MetadataPhase.PARSE_WAIT), ms(MetadataPhase.WRITE),
					ms(MetadataPhase.COMMIT), ms(MetadataPhase.OTHER));
			}
		}
	}

	private static final Map<String, WeakReference<MediaInfo>> STORE = new HashMap<>();
	private static final Map<Long, WeakReference<TvSeriesMetadata>> TV_SERIES_STORE = new HashMap<>();

	private static final ReferenceQueue<MediaInfo> STORE_QUEUE = new ReferenceQueue<>();
	private static final ReferenceQueue<TvSeriesMetadata> TV_SERIES_QUEUE = new ReferenceQueue<>();

	// A weak reference that remembers its key
	private static final class KeyedReference<V> extends WeakReference<V> {

		private final Object key;
		private final long lastModified;

		private KeyedReference(Object key, V value, ReferenceQueue<? super V> queue) {
			this(key, value, queue, UNKNOWN_MODIFIED);
		}

		private KeyedReference(Object key, V value, ReferenceQueue<? super V> queue, long lastModified) {
			super(value, queue);
			this.key = key;
			this.lastModified = lastModified;
		}
	}

	private static final long UNKNOWN_MODIFIED = -1;

	// One lock per file or url
	private static final Map<String, CountedLock> LOCKS = new HashMap<>();

	// A per key lock that knows how many callers still need it.
	private static final class CountedLock {
		private int users;
	}

	private MediaInfoStore() {
		//should not be instantiated
	}

	private static CountedLock acquireLock(String key) {
		synchronized (LOCKS) {
			CountedLock lock = LOCKS.computeIfAbsent(key, k -> new CountedLock());
			lock.users++;
			return lock;
		}
	}

	private static void releaseLock(String key, CountedLock lock) {
		synchronized (LOCKS) {
			lock.users--;
			if (lock.users <= 0) {
				LOCKS.remove(key);
			}
		}
	}

	private static MediaInfo getMediaInfoStored(String filename) {
		return getMediaInfoStored(filename, UNKNOWN_MODIFIED);
	}

	private static MediaInfo getMediaInfoStored(String filename, long lastModified) {
		synchronized (STORE) {
			purgeStore();
			WeakReference<MediaInfo> reference = STORE.get(filename);
			if (reference == null) {
				return null;
			}
			if (lastModified != UNKNOWN_MODIFIED &&
					reference instanceof KeyedReference<MediaInfo> keyed &&
					keyed.lastModified != UNKNOWN_MODIFIED &&
					keyed.lastModified != lastModified) {
				LOGGER.debug("Stored media info for {} is from another version of the file, parsing it again", filename);
				STORE.remove(filename, reference);
				return null;
			}
			return reference.get();
		}
	}

	/**
	 * Drops the entries whose media info has been collected.
	 */
	private static void purgeStore() {
		Reference<? extends MediaInfo> reference;
		while ((reference = STORE_QUEUE.poll()) != null) {
			if (reference instanceof KeyedReference<?> keyed) {
				STORE.remove(keyed.key, reference);
			}
		}
	}

	private static void purgeTvSeriesStore() {
		Reference<? extends TvSeriesMetadata> reference;
		while ((reference = TV_SERIES_QUEUE.poll()) != null) {
			if (reference instanceof KeyedReference<?> keyed) {
				TV_SERIES_STORE.remove(keyed.key, reference);
			}
		}
	}

	/**
	 * Returns the thumbnail id of the media info that is still in memory
	 */
	public static Long getStoredThumbnailId(String filename) {
		MediaInfo mediaInfo = getMediaInfoStored(filename);
		return mediaInfo != null ? mediaInfo.getThumbnailId() : null;
	}

	/**
	 * Returns the thumbnail id of the media info that is still in memory.
	 */
	public static Long getStoredUserThumbnailId(String filename) {
		MediaInfo mediaInfo = getMediaInfoStored(filename);
		return mediaInfo != null && mediaInfo.getThumbnailSource() == ThumbnailSource.USER ?
			mediaInfo.getThumbnailId() : null;
	}

	/**
	 * Hands a thumbnail made after the file was parsed to the media info that is still in memory.
	 */
	public static void updateThumbnail(String filename, Long thumbnailId, ThumbnailSource thumbnailSource) {
		MediaInfo mediaInfo = getMediaInfoStored(filename);
		if (mediaInfo != null) {
			mediaInfo.setThumbnailId(thumbnailId);
			mediaInfo.setThumbnailSource(thumbnailSource);
			mediaInfo.setThumbnailPending(false);
		}
	}

	private static void storeMediaInfo(String filename, MediaInfo mediaInfo) {
		storeMediaInfo(filename, mediaInfo, UNKNOWN_MODIFIED);
	}

	private static void storeMediaInfo(String filename, MediaInfo mediaInfo, long lastModified) {
		synchronized (STORE) {
			purgeStore();
			STORE.put(filename, new KeyedReference<>(filename, mediaInfo, STORE_QUEUE, lastModified));
		}
	}

	public static MediaInfo getMediaInfo(String filename) {
		CountedLock lock = acquireLock(filename);
		try {
			synchronized (lock) {
				MediaInfo mediaInfo = getMediaInfoStored(filename);
				if (mediaInfo != null) {
					return mediaInfo;
				}
				Connection connection = null;
				try {
					connection = MediaDatabase.getConnectionIfAvailable();
					if (connection != null) {
						File file = new File(filename);
						long fileModified = file.lastModified();
						mediaInfo = MediaTableFiles.getMediaInfo(connection, filename, fileModified);
						if (mediaInfo != null && mediaInfo.isMediaParsed() && mediaInfo.getMimeType() != null) {
							storeMediaInfo(filename, mediaInfo, fileModified);
						}
						return mediaInfo;
					}
				} catch (IOException | SQLException e) {
					LOGGER.debug("Error while getting cached information about {}: {}", filename, e.getMessage());
					LOGGER.trace("", e);
				} finally {
					MediaDatabase.close(connection);
				}
			}
		} finally {
			releaseLock(filename, lock);
		}

		return null;
	}

	public static MediaInfo getMediaInfo(String filename, File file, Format format, int type) {
		MetadataLoadTimer timer = new MetadataLoadTimer();
		// Published cache entries can be read without joining the per-file load queue.
		timer.phase(MetadataPhase.STAT);
		long cachedModified = file.lastModified();
		timer.phase(MetadataPhase.CACHE);
		MediaInfo cached = getMediaInfoStored(filename, cachedModified);
		if (cached != null) {
			timer.source = "memory";
			timer.parser = cached.getMediaParser();
			timer.finish(filename);
			return cached;
		}
		timer.phase(MetadataPhase.LOCK);
		CountedLock lock = acquireLock(filename);
		try {
			synchronized (lock) {
				timer.phase(MetadataPhase.STAT);
				long lastModified = file.lastModified();
				timer.phase(MetadataPhase.CACHE);
				MediaInfo mediaInfo = getMediaInfoStored(filename, lastModified);
				if (mediaInfo != null) {
					timer.source = "memory";
					timer.parser = mediaInfo.getMediaParser();
					return mediaInfo;
				}
				timer.phase(MetadataPhase.OTHER);
				LOGGER.trace("Store does not yet contain MediaInfo for {}", filename);
				Connection connection = null;
				InputFile input = new InputFile();
				input.setFile(file);
				try {
					timer.phase(MetadataPhase.CONNECTION);
					connection = MediaDatabase.getConnectionIfAvailable();
					timer.phase(MetadataPhase.TRANSACTION_SETUP);
					if (connection != null) {
						connection.setAutoCommit(false);
						try {
							timer.phase(MetadataPhase.READ);
							mediaInfo = MediaTableFiles.getMediaInfo(connection, filename, lastModified);
							timer.phase(MetadataPhase.OTHER);
							if (mediaInfo != null) {
								timer.source = "database";
								if (!mediaInfo.isMediaParsed()) {
									timer.source = "database+repair";
									timer.phase(MetadataPhase.PARSE);
									Parser.parse(mediaInfo, input, format, type);
									timer.phase(MetadataPhase.WRITE);
									MediaTableFiles.insertOrUpdateData(connection, filename, lastModified, type, mediaInfo);
									timer.phase(MetadataPhase.OTHER);
								}
								//ensure we have the mime type
								if (mediaInfo.getMimeType() == null) {
									timer.source = "database+repair";
									timer.phase(MetadataPhase.PARSE);
									Parser.postParse(mediaInfo, type);
									timer.phase(MetadataPhase.WRITE);
									MediaTableFiles.insertOrUpdateData(connection, filename, lastModified, type, mediaInfo);
									timer.phase(MetadataPhase.OTHER);
								}
								//ensure we have the ruid
								if (mediaInfo.getResourceId() == null) {
									timer.source = "database+repair";
									timer.phase(MetadataPhase.HASH);
									String resourceHash = ResourceIdentifier.getResourceIdentifier(filename);
									mediaInfo.setResourceId(resourceHash);
									timer.phase(MetadataPhase.WRITE);
									MediaTableFiles.insertOrUpdateData(connection, filename, lastModified, type, mediaInfo);
									timer.phase(MetadataPhase.OTHER);
								}
							}
						} catch (IOException | SQLException e) {
							LOGGER.debug("Error while getting cached information about {}, reparsing information: {}", filename, e.getMessage());
							LOGGER.trace("", e);
						}
					}

					if (mediaInfo == null) {
						mediaInfo = updateMediaInfoFromFile(filename, file, format, type, connection, input, timer);
					}
				} catch (Exception e) {
					LOGGER.error("Error in RealFile.resolve: {}", e.getMessage());
					LOGGER.trace("", e);
				} finally {
					timer.phase(MetadataPhase.COMMIT);
					try {
						if (connection != null) {
							connection.commit();
							connection.setAutoCommit(true);
						}
					} catch (SQLException e) {
						LOGGER.error("Error in commit in RealFile.resolve: {}", e.getMessage());
						LOGGER.trace("", e);
					}
					MediaDatabase.close(connection);
					timer.phase(MetadataPhase.OTHER);
				}
				if (mediaInfo != null) {
					timer.parser = mediaInfo.getMediaParser();
					storeMediaInfo(filename, mediaInfo, lastModified);
				}
				return mediaInfo;
			}
		} finally {
			releaseLock(filename, lock);
			timer.finish(filename);
		}

	}

	public static MediaInfo updateMediaInfoFromFile(String filename, File file, Format format, int type, Connection connection, InputFile input) {
		MetadataLoadTimer timer = new MetadataLoadTimer();
		try {
			return updateMediaInfoFromFile(filename, file, format, type, connection, input, timer);
		} finally {
			timer.finish(filename);
		}
	}

	private static MediaInfo updateMediaInfoFromFile(String filename, File file, Format format, int type, Connection connection, InputFile input, MetadataLoadTimer timer) {
		timer.source = "file";
		timer.phase(MetadataPhase.HASH);
		MediaInfo mediaInfo;
		mediaInfo = new MediaInfo();
		String resourceHash = ResourceIdentifier.getResourceIdentifier(filename);
		mediaInfo.setResourceId(resourceHash);
		timer.phase(MetadataPhase.PARSE);
		if (format != null) {
			Parser.parse(mediaInfo, input, format, type);
		} else {
			// Don't think that will ever happen
			FFmpegParser.parse(mediaInfo, input, format, type);
		}

		timer.phase(MetadataPhase.PARSE_WAIT);
		mediaInfo.waitMediaParsing(5);
		timer.parser = mediaInfo.getMediaParser();

		if (connection == null) {
			timer.phase(MetadataPhase.CONNECTION);
			connection = MediaDatabase.getConnectionIfAvailable();
		}
		timer.phase(MetadataPhase.OTHER);
		try {
			if (connection != null && mediaInfo.isMediaParsed()) {
				try {
					timer.phase(MetadataPhase.WRITE);
					MediaTableFiles.insertOrUpdateData(connection, filename, file.lastModified(), type, mediaInfo);
					timer.phase(MetadataPhase.OTHER);
					if (mediaInfo.isAudio() && mediaInfo.getThumbnailId() == null) {
						// The file carries no cover, so it has to come from Cover Art Archive. That is an HTTP
						// round trip and is done in the background, now that the file row exists.
						AudioCoverResolver.enqueue(file);
					}
				} catch (SQLException e) {
					LOGGER.error(
						"Database error while trying to add parsed information for \"{}\" to the cache: {}",
						filename,
						e.getMessage());
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("SQL error code: {}", e.getErrorCode());
						if (
							e.getCause() instanceof SQLException &&
							((SQLException) e.getCause()).getErrorCode() != e.getErrorCode()
						) {
							LOGGER.trace("Cause SQL error code: {}", ((SQLException) e.getCause()).getErrorCode());
						}
						LOGGER.trace("", e);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("update media info error.", e);
		}
		return mediaInfo;
	}

	public static MediaInfo getWebStreamMediaInfo(String url, int type) {
		return getWebStreamMediaInfo(url, type, null);
	}

	public static MediaInfo getWebStreamMediaInfo(String url, int type, HttpHeaders probeHeaders) {
		CountedLock lock = acquireLock(url);
		try {
			synchronized (lock) {
				MediaInfo mediaInfo = getMediaInfoStored(url);
				if (mediaInfo != null) {
					return mediaInfo;
				}
				LOGGER.trace("Store does not yet contain MediaInfo for {}", url);
				try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
					mediaInfo = MediaTableFiles.getMediaInfo(connection, url, 0);
					if (mediaInfo == null) {
						mediaInfo = new MediaInfo();
					}
					//ensure we have the ruid
					if (mediaInfo.getResourceId() == null) {
						String resourceHash = ResourceIdentifier.getResourceIdentifier(url);
						mediaInfo.setResourceId(resourceHash);
						MediaTableFiles.insertOrUpdateData(connection, url, 0, type, mediaInfo);
					}
					if (!mediaInfo.isMediaParsed()) {
						WebStreamParser.parse(mediaInfo, url, type, probeHeaders);
						MediaTableFiles.insertOrUpdateData(connection, url, 0, type, mediaInfo);
					}
				} catch (Exception e) {
					LOGGER.error("Error while trying to add parsed information for \"{}\" to the cache: {}", url, e.getMessage());
					LOGGER.trace("", e);
				}
				if (mediaInfo != null) {
					storeMediaInfo(url, mediaInfo);
				}
				return mediaInfo;
			}
		} finally {
			releaseLock(url, lock);
		}

	}

	public static MediaVideoMetadata getMediaVideoMetadata(String filename) {
		//check on store
		MediaInfo mediaInfo = getMediaInfoStored(filename);
		if (mediaInfo != null) {
			return mediaInfo.getVideoMetadata();
		}
		//parse db
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				return MediaTableVideoMetadata.getVideoMetadataByFilename(connection, filename);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return null;
	}

	private static TvSeriesMetadata getTvSeriesMetadataStored(Long tvSeriesId) {
		synchronized (TV_SERIES_STORE) {
			purgeTvSeriesStore();
			WeakReference<TvSeriesMetadata> reference = TV_SERIES_STORE.get(tvSeriesId);
			return reference != null ? reference.get() : null;
		}
	}

	private static void storeTvSeriesMetadata(Long tvSeriesId, TvSeriesMetadata tvSeriesMetadata) {
		synchronized (TV_SERIES_STORE) {
			purgeTvSeriesStore();
			TV_SERIES_STORE.put(tvSeriesId, new KeyedReference<>(tvSeriesId, tvSeriesMetadata, TV_SERIES_QUEUE));
		}
	}

	public static TvSeriesMetadata getTvSeriesMetadata(Long tvSeriesId) {
		//check on store
		TvSeriesMetadata tvSeriesMetadata = getTvSeriesMetadataStored(tvSeriesId);
		if (tvSeriesMetadata != null || !MediaDatabase.isAvailable()) {
			return tvSeriesMetadata;
		}
		//parse db
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				tvSeriesMetadata = MediaTableTVSeries.getTvSeriesMetadata(connection, tvSeriesId);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		if (tvSeriesMetadata != null) {
			storeTvSeriesMetadata(tvSeriesId, tvSeriesMetadata);
		}
		return tvSeriesMetadata;
	}

	public static void updateTvSeriesMetadata(final TvSeriesMetadata tvSeriesMetadata, final Long tvSeriesId) {
		if (tvSeriesId == null || tvSeriesId < 0) {
			return;
		}
		if (tvSeriesMetadata == null) {
			LOGGER.warn("Couldn't update Tv Series Metadata for \"{}\" because there is no media information", tvSeriesId);
			return;
		}
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableTVSeries.updateAPIMetadata(connection, tvSeriesMetadata, tvSeriesId);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		//update referenced objects
		TvSeriesMetadata storedTvSeriesMetadata = getTvSeriesMetadataStored(tvSeriesId);
		if (storedTvSeriesMetadata != null) {
			storedTvSeriesMetadata.update(tvSeriesMetadata);
		}
	}

	public static void updateTvEpisodesTvSeriesId(final Long oldTvSeriesId, Long tvSeriesId) {
		if (oldTvSeriesId == null || tvSeriesId == null) {
			return;
		}
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				TvSeriesMetadata tvSeriesMetadata = getTvSeriesMetadata(tvSeriesId);
				List<String> filenames = MediaTableVideoMetadata.getTvEpisodesFilesByTvSeriesId(connection, oldTvSeriesId);
				for (String filename : filenames) {
					//remove FailedLookups entry on db if exists
					MediaTableFailedLookups.remove(connection, filename, false);
					MediaInfo mediaInfo = getMediaInfo(filename);
					if (mediaInfo != null && mediaInfo.hasVideoMetadata()) {
						mediaInfo.getVideoMetadata().setSeriesMetadata(tvSeriesMetadata);
						if ((tvSeriesMetadata != null && tvSeriesMetadata.getTmdbId() != null &&
								!tvSeriesMetadata.getTmdbId().equals(mediaInfo.getVideoMetadata().getTmdbTvId())) ||
								!tvSeriesId.equals(mediaInfo.getVideoMetadata().getTvSeriesId())) {
							//changed, remove old values to lookup for new metadata
							mediaInfo.getVideoMetadata().setTvSeriesId(tvSeriesId);
							mediaInfo.getVideoMetadata().setTmdbId(null);
							mediaInfo.getVideoMetadata().setIMDbID(null);
							try {
								MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, mediaInfo.getFileId(), mediaInfo, true);
							} catch (SQLException ex) {
							}
							File file = new File(filename);
							mediaInfo.setLastExternalLookup(0);
							MediaTableFailedLookups.remove(connection, file.getAbsolutePath(), true);
							TMDB.backgroundLookupAndAddMetadata(file, mediaInfo);
						}
					}
				}
				//cleanup MediaTableTVSeries
				MediaTableTVSeries.cleanup(connection);
			}
		} finally {
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Populates the mediaInfo Title, Year, Edition, TVSeason, TVEpisodeNumber and
	 * TVEpisodeName parsed from the mediaInfo file name and if enabled insert them
	 * to the database.
	 *
	 * @param file
	 * @param mediaInfo
	 */
	public static void setMetadataFromFileName(final File file, MediaInfo mediaInfo) {
		FilenameMetadataTimer timer = new FilenameMetadataTimer();
		try {
			setMetadataFromFileName(file, mediaInfo, timer);
		} finally {
			timer.finish(file);
		}
	}

	private static final class FilenameMetadataTimer {
		private final long started = System.nanoTime();
		private final long[] nanos = new long[5];
		private long phaseStarted = started;
		private int current;
		private boolean populated;

		private void phase(int next) {
			long now = System.nanoTime();
			nanos[current] += now - phaseStarted;
			phaseStarted = now;
			current = next;
		}

		private void finish(File file) {
			phase(4);
			long totalMs = (phaseStarted - started) / 1_000_000;
			if (totalMs >= 20) {
				LOGGER.info("Slow filename metadata for \"{}\": {} ms total; checks {} ms, name parse/apply {} ms, database {} ms, background lookup preparation {} ms, other {} ms; populated {}",
					file, totalMs, nanos[0] / 1_000_000, nanos[1] / 1_000_000, nanos[2] / 1_000_000,
					nanos[3] / 1_000_000, nanos[4] / 1_000_000, populated);
			}
		}
	}

	private static void setMetadataFromFileName(final File file, MediaInfo mediaInfo, FilenameMetadataTimer timer) {
		String absolutePath = file.getAbsolutePath();
		if (absolutePath == null ||
			(Platform.isMac() &&
			// skip metadata extraction and API lookups for live photos (little MP4s) backed up from iPhones
			absolutePath.contains("Photos Library.photoslibrary"))
		) {
			return;
		}

		// If the in-memory mediaInfo has not already been populated with filename metadata, we attempt it
		try {
			if (mediaInfo.isVideo() && !mediaInfo.hasVideoMetadata()) {
				timer.phase(1);
				MediaVideoMetadata videoMetadata = new MediaVideoMetadata();
				FileNameMetadata metadataFromFilename = FileUtil.getFileNameMetadata(file.getName(), absolutePath);
				String titleFromFilename = metadataFromFilename.getMovieOrShowName();

				// Apply the metadata from the filename.
				if (StringUtils.isNotBlank(titleFromFilename) && metadataFromFilename.isTvEpisode()) {
					TvSeriesMetadata tvSeriesMetadata = new TvSeriesMetadata();
					tvSeriesMetadata.setTitle(titleFromFilename);
					tvSeriesMetadata.setStartYear(metadataFromFilename.getYear());
					videoMetadata.setTvSeason(metadataFromFilename.getTvSeasonNumber());
					videoMetadata.setTvEpisodeNumber(metadataFromFilename.getTvEpisodeNumber());
					if (StringUtils.isNotBlank(metadataFromFilename.getTvEpisodeName())) {
						videoMetadata.setTitle(metadataFromFilename.getTvEpisodeName());
					}
					videoMetadata.setSeriesMetadata(tvSeriesMetadata);
					videoMetadata.setIsTvEpisode(true);
				} else {
					videoMetadata.setTitle(titleFromFilename);
					videoMetadata.setYear(metadataFromFilename.getYear());
				}
				if (metadataFromFilename.getExtraInformation() != null) {
					videoMetadata.setExtraInformation(metadataFromFilename.getExtraInformation());
				}
				videoMetadata.setIsSample(metadataFromFilename.isSample());

				mediaInfo.setVideoMetadata(videoMetadata);
				timer.populated = true;
				timer.phase(2);

				if (MediaDatabase.isAvailable()) {
					try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
						if (connection != null) {
							if (videoMetadata.isTvEpisode() && videoMetadata.getTvSeriesId() == null) {
								String tvSeriesTitle = videoMetadata.getSeriesMetadata().getTitle();
								Integer tvSeriesYear = videoMetadata.getSeriesMetadata().getStartYear();
								Long tvSeriesId = MediaTableTVSeries.getIdBySimilarTitle(connection, tvSeriesTitle, tvSeriesYear);
								if (tvSeriesId == null) {
									// Creates a minimal TV series row with just the title, that
									// might be enhanced later by the API
									tvSeriesId = MediaTableTVSeries.set(connection, tvSeriesTitle, tvSeriesYear);
								}
								TvSeriesMetadata tvSeriesMetadata = getTvSeriesMetadata(tvSeriesId);
								videoMetadata.setSeriesMetadata(tvSeriesMetadata);
								videoMetadata.setTvSeriesId(tvSeriesId);
							}
							MediaTableVideoMetadata.insertVideoMetadata(connection, absolutePath, file.lastModified(), mediaInfo);
						}
					}
				}
			}
		} catch (SQLException e) {
			timer.phase(4);
			LOGGER.error("Could not update the database with information from the filename for \"{}\": {}", file.getAbsolutePath(),
				e.getMessage());
			LOGGER.trace("", e);
		} catch (Exception e) {
			timer.phase(4);
			LOGGER.debug("", e);
		} finally {
			// Attempt to enhance the metadata via our API.
			timer.phase(3);
			TMDB.backgroundLookupAndAddMetadata(file, mediaInfo);
			timer.phase(4);
		}
	}

	public static boolean removeMediaEntriesInFolder(String pathToFolder) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFiles.removeMediaEntriesInFolder(connection, pathToFolder);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		boolean removed = false;
		synchronized (STORE) {
			Iterator<String> filenames = STORE.keySet().iterator();
			while (filenames.hasNext()) {
				if (filenames.next().startsWith(pathToFolder)) {
					filenames.remove();
					removed = true;
				}
			}
		}
		removed = MediaStatusStore.removeMediaEntriesInFolder(pathToFolder) || removed;
		return removed;
	}

	public static boolean removeMediaEntry(String filename) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFiles.removeMediaEntry(connection, filename, true);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		boolean removed = false;
		synchronized (STORE) {
			if (STORE.remove(filename) != null) {
				removed = true;
			}
		}
		removed = MediaStatusStore.removeMediaEntry(filename) || removed;
		return removed;
	}

	public static void removeMediaEntryFromCache(String filename) {
		synchronized (STORE) {
			STORE.remove(filename);
		}
	}

	public static void clear() {
		synchronized (STORE) {
			STORE.clear();
		}
	}

}
