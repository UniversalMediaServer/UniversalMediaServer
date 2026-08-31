package net.pms.store;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudioMetadata;
import net.pms.database.MediaTableResourceRatings;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.parsers.JaudiotaggerParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the user rating (0 - 5 stars) of any StoreResource, items
 * as well as containers. Ratings are global, there is one rating per resource.
 *
 * Ratings are stored in the RESOURCE_RATINGS table, keyed on
 * StoreResource.getRatingKey(). For audio files the rating is mirrored into
 * AUDIO_METADATA.RATING so that searching on upnp:rating keeps working.
 *
 * The cache is kept here and not on the StoreResource because every renderer
 * has its own MediaStore and therefore its own resource instances : a rating
 * set through one renderer must be visible to all others.
 */
public class StoreResourceRatings {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreResourceRatings.class);

	private static final int MIN_RATING = 0;
	private static final int MAX_RATING = 5;

	/**
	 * Rating key to rating. Holds NULL values to remember that a resource is
	 * known to be unrated.
	 */
	private static final Map<String, Integer> CACHE = new HashMap<>();

	/**
	 * This class is not meant to be instantiated.
	 */
	private StoreResourceRatings() {
	}

	/**
	 * Returns the user rating of a resource.
	 *
	 * For audio files that have no rating stored yet, the rating parsed from the
	 * file tag is adopted and stored, so ratings set by earlier UMS versions are
	 * migrated on first access.
	 *
	 * @param resource the resource
	 * @return the rating (0 - 5 stars) or NULL if the resource is not rated
	 */
	public static synchronized Integer getRating(StoreResource resource) {
		if (resource == null) {
			return null;
		}
		String ratingKey = resource.getRatingKey();
		if (StringUtils.isBlank(ratingKey)) {
			return null;
		}
		if (CACHE.containsKey(ratingKey)) {
			return CACHE.get(ratingKey);
		}
		Integer rating = null;
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection == null) {
				//without database we cannot tell, do not cache that
				return null;
			}
			rating = MediaTableResourceRatings.getRating(connection, ratingKey);
			if (rating == null) {
				rating = getAudioTagRating(resource);
				if (rating != null) {
					//migrate the rating read from the audio file tag
					MediaTableResourceRatings.setRating(connection, ratingKey, resource.getClass().getSimpleName(), rating);
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		CACHE.put(ratingKey, rating);
		return rating;
	}

	/**
	 * Sets the user rating of a resource.
	 *
	 * @param rating the rating (0 - 5 stars), or NULL to remove the rating
	 */
	public static synchronized void setRating(StoreResource resource, Integer rating) {
		if (resource == null) {
			return;
		}
		if (rating != null && (rating < MIN_RATING || rating > MAX_RATING)) {
			throw new IllegalArgumentException("rating must be between " + MIN_RATING + " and " + MAX_RATING);
		}
		String ratingKey = resource.getRatingKey();
		if (StringUtils.isBlank(ratingKey)) {
			LOGGER.warn("cannot store the rating of \"{}\", it has no rating key", resource.getDisplayName());
			return;
		}
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection == null) {
				LOGGER.warn("cannot store the rating of \"{}\", database is not available", resource.getDisplayName());
				return;
			}
			MediaTableResourceRatings.setRating(connection, ratingKey, resource.getClass().getSimpleName(), rating);
			CACHE.put(ratingKey, rating);
			updateAudioMetadata(connection, resource, rating);
			MediaStoreIds.incrementUpdateIdForFilenameWithAncestors(connection, ratingKey);
		} finally {
			MediaDatabase.close(connection);
		}
		resource.notifyRefresh();
	}

	public static synchronized void clearCache() {
		CACHE.clear();
	}

	/**
	 * Mirrors the rating of an audio file into the audio metadata, the AUDIO_METADATA table and, if enabled, the tag of the file itself.
	 */
	private static void updateAudioMetadata(Connection connection, StoreResource resource, Integer rating) {
		MediaAudioMetadata audioMetadata = getAudioMetadata(resource);
		if (audioMetadata == null) {
			return;
		}
		audioMetadata.setRating(rating);
		try {
			MediaTableAudioMetadata.updateRatingByAudiotrackId(connection, rating, audioMetadata.getAudiotrackId());
		} catch (SQLException e) {
			LOGGER.error("cannot mirror the rating of \"{}\" into the audio metadata: {}", resource.getDisplayName(), e.getMessage());
			LOGGER.trace("", e);
		}
		if (PMS.getConfiguration().isAudioUpdateTag()) {
			JaudiotaggerParser.writeRatingToFile(resource.getFileName(), rating);
		}
	}

	/**
	 * @return the rating held by the audio metadata of a resource
	 */
	private static Integer getAudioTagRating(StoreResource resource) {
		MediaAudioMetadata audioMetadata = getAudioMetadata(resource);
		return audioMetadata == null ? null : audioMetadata.getRating();
	}

	private static MediaAudioMetadata getAudioMetadata(StoreResource resource) {
		MediaInfo mediaInfo = resource.getMediaInfo();
		if (mediaInfo == null || !mediaInfo.hasAudioMetadata()) {
			return null;
		}
		return mediaInfo.getAudioMetadata();
	}
}


