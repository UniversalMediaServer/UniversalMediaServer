package net.pms.store;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableResourceRatings;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.parsers.JaudiotaggerParser;
import net.pms.util.ResourceIdentifier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the user rating (0 - 5 stars) of any StoreResource, items as well as containers. Ratings are global,
 * there is one rating per resource.
 *
 * Ratings are stored in the RESOURCE_RATINGS table. The key comes from StoreResource.getRatingKey(). That table is the only place a rating lives.
 *
 * The cache is kept here and not on the StoreResource. A rating set through one renderer must be visible to all others.
 */
public class StoreResourceRatings {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreResourceRatings.class);

	private static final int MIN_RATING = 0;
	private static final int MAX_RATING = 5;

	/**
	 * Rating key to rating. Holds NULL values to remember that a resource is known to be unrated.
	 */
	private static final Map<String, Integer> CACHE = new HashMap<>();

	private StoreResourceRatings() {
	}

	/**
	 * Returns the user rating of a resource.
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
	 * Copies the rating into the audio metadata and if enabled into the tag of the file itself.
	 */
	private static void updateAudioMetadata(Connection connection, StoreResource resource, Integer rating) {
		MediaAudioMetadata audioMetadata = getAudioMetadata(resource);
		if (audioMetadata == null) {
			return;
		}
		audioMetadata.setRating(rating);
		if (PMS.getConfiguration().isAudioUpdateTag() && JaudiotaggerParser.writeRatingToFile(resource.getFileName(), rating)) {
			refreshResourceId(connection, resource);
		}
	}

	private static void refreshResourceId(Connection connection, StoreResource resource) {
		String filename = resource.getFileName();
		String resourceId = ResourceIdentifier.getResourceIdentifier(filename);
		if (resourceId == null) {
			return;
		}
		MediaInfo mediaInfo = resource.getMediaInfo();
		if (mediaInfo != null) {
			mediaInfo.setResourceId(resourceId);
		}
		MediaTableFiles.updateResourceUidForFilename(connection, filename, resourceId);
	}

	private static MediaAudioMetadata getAudioMetadata(StoreResource resource) {
		MediaInfo mediaInfo = resource.getMediaInfo();
		if (mediaInfo == null || !mediaInfo.hasAudioMetadata()) {
			return null;
		}
		return mediaInfo.getAudioMetadata();
	}
}


