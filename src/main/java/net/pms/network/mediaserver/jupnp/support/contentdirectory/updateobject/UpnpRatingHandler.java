package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import java.util.Objects;
import net.pms.store.StoreResource;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

/**
 * Handles the UPNP:RATING property of the CDS UPDATEOBJECT action.
 *
 * The rating is a user rating from 0 to 5 stars and is supported by any kind of
 * store resource : audio and video files as well as folders, playlists and web
 * streams. An empty NEWTAGVALUE removes the rating.
 */
public class UpnpRatingHandler extends BaseUpdateObjectHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpnpRatingHandler.class.getName());

	private static final int MIN_RATING = 0;
	private static final int MAX_RATING = 5;

	public UpnpRatingHandler(StoreResource objectResource, NodeList currentTagValue, NodeList newTagValue) {
		super(objectResource, currentTagValue, newTagValue);
	}

	@Override
	public void handle() throws ContentDirectoryException {
		Integer currentValue = parseRating(getNodeTextValue(getCurrentTagValue(), 0));
		Integer newValue = parseRating(getNodeTextValue(getNewTagValue(), 0));

		if (!Objects.equals(currentValue, getObjectResource().getRating())) {
			throw new ContentDirectoryException(702, "UpdateObject() failed because upnp:rating value listed in " +
				"the CurrentTagValue argument do not match the current state of the ContentDirectory service. " +
				"The specified data is likely out of date.");
		}
		if (newValue != null && (newValue > MAX_RATING || newValue < MIN_RATING)) {
			throw new ContentDirectoryException(703, "UpdateObject() failed because new upnp:rating value is out of bounds. " +
				"Value must be between 0 and 5 which is equavalent of a rating from 0 to 5 stars.");
		}

		try {
			//a null value removes the rating
			getObjectResource().setRating(newValue);
		} catch (RuntimeException e) {
			LOGGER.error("cannot handle update object request", e);
			throw new ContentDirectoryException(712, "UpdateObject() failed because the new upnp:rating value cannot be stored.");
		}
	}

	/**
	 * @param value the tag text content, or NULL if the tag is empty or absent
	 * @return the rating, or NULL if the rating is to be removed
	 */
	private static Integer parseRating(String value) throws ContentDirectoryException {
		if (value == null) {
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (NumberFormatException e) {
			throw new ContentDirectoryException(712, "UpdateObject() failed because some TextContent cannot be parsed.");
		}
	}
}
