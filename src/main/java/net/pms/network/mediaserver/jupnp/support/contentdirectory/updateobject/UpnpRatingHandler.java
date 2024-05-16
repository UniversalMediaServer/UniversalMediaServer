package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudioMetadata;
import net.pms.store.StoreResource;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

public class UpnpRatingHandler extends BaseUpdateObjectHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpnpRatingHandler.class.getName());

	public UpnpRatingHandler(StoreResource objectResource, NodeList currentTagValue, NodeList newTagValue) {
		super(objectResource, currentTagValue, newTagValue);
	}

	@Override
	public void handle() throws ContentDirectoryException {
		try {
			Integer currentValue = getNodeTextValue(getCurrentTagValue(), 0) != null ? Integer.parseInt(getNodeTextValue(getCurrentTagValue(), 0)) : null;
			Integer newValue = getNodeTextValue(getNewTagValue(), 0) != null ? Integer.parseInt(getNodeTextValue(getNewTagValue(), 0)) : null;
			if (!isModelValueEqual(currentValue)) {
				throw new ContentDirectoryException(702, "UpdateObject() failed because upnp:rating value listed in " +
					"the CurrentTagValue argument do not match the current state of the ContentDirectory service. " +
					"The specified data is likely out of date.");
			}
			if (newValue > 5 || newValue < 0) {
				throw new ContentDirectoryException(703, "UpdateObject() failed because new upnp:rating value is out of bounds. " +
					"Value must be between 0 and 5 which is equavalent of a rating from 0 to 5 stars.");
			}
			getObjectResource().getMediaInfo().getAudioMetadata().setRating(newValue);
			updateDatabase();

			if (PMS.getConfiguration().isAudioUpdateTag()) {
				String filename = getObjectResource().getFileName();
				setRatingInFile(newValue, filename);
			}
		} catch (NullPointerException e) {
			LOGGER.error("cannot handle update object request", e);
			throw new ContentDirectoryException(712, "UpdateObject() failed because some TextContent cannot be parsed.");
		}
	}

	private boolean isModelValueEqual(Integer oldValue) {
		if (oldValue == null && getObjectResource().getMediaInfo().getAudioMetadata().getRating() == null) {
			return true;
		}
		return getObjectResource().getMediaInfo().getAudioMetadata().getRating().equals(oldValue);
	}

	/**
	 * <pre>
	 *
	 * ID3v2 Tags support:
	 * =======================================
	 *
	 * There is a "Popularimeter" frame in the ID3v2 specification meant for this purpose.
	 * The frame is called POPM and Windows Explorer, Windows Media Player, Winamp, foobar2000, MediaMonkey,
	 * and other software all map roughly the same ranges of 0–255 to a 0–5 stars value for display.
	 *
	 * The following list details how Windows Explorer reads and writes the POPM frame:
	 *
	 * 224–255 = 5 stars when READ with Windows Explorer, writes 255
	 * 160–223 = 4 stars when READ with Windows Explorer, writes 196
	 * 096-159 = 3 stars when READ with Windows Explorer, writes 128
	 * 032-095 = 2 stars when READ with Windows Explorer, writes 64
	 * 001-031 = 1 star when READ with Windows Explorer, writes 1
	 *
	 *
	 * Vorbis
	 * =======================================
	 *
	 *  Ratings are usually mapped as 1-5 stars with 20,40,60,80,100 as the actual string values.
	 *
	 * </pre>
	 *
	 */
	private void setRatingInFile(Integer ratingInStars, String filename) {
		if (StringUtils.isEmpty(filename)) {
			LOGGER.warn("cannot update rating in file. Filename is empty or NULL");
			return;
		}
		try {
			AudioFile audioFile = AudioFileIO.read(new File(filename));
			Tag tag = audioFile.getTag();
			if (ratingInStars == null) {
				tag.deleteField(FieldKey.RATING);
			} else {
				tag.setField(FieldKey.RATING, getRatingValue(tag, ratingInStars));
			}
			audioFile.commit();
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException e) {
			LOGGER.warn("Error writing Tag info.", e);
		}
	}

	/**
	 * converts 0-5 star rating to format specific value.
	 *
	 * @param tag Tag to update
	 * @param stars number of stars (0 - 5)
	 * @return
	 */
	private String getRatingValue(Tag tag, Integer stars) {
		if (stars == null) {
			return null;
		}

		int num;
		if (tag instanceof FlacTag || tag instanceof VorbisCommentTag) {
			num = convertStarsToVorbis(stars);
		} else if (tag instanceof AbstractID3v2Tag || tag instanceof ID3v11Tag) {
			num = convertStarsToID3(stars);
		} else {
			// Don't know ... maybe we use vorbis tags by default
			num = convertStarsToVorbis(stars);
		}

		return "" + num;
	}

	/**
	 * Converts 0-5 stars to MP3 TAG value
	 *
	 * @param rating
	 * @return
	 */
	private int convertStarsToID3(int rating) {
		return switch (rating) {
			case 0 -> 0;
			case 1 -> 1;
			case 2 -> 64;
			case 3 -> 128;
			case 4 -> 196;
			default -> 255;
		};
	}

	/**
	 * converts 0-5 stars to VORBIS TAG value
	 *
	 * @param rating
	 * @return
	 */
	private int convertStarsToVorbis(int rating) {
		return rating * 20;
	}

	private void updateDatabase() throws ContentDirectoryException {
		try {
			MediaTableAudioMetadata.updateRatingByAudiotrackId(
				MediaDatabase.getConnectionIfAvailable(),
				getObjectResource().getMediaInfo().getAudioMetadata().getRating(),
				getObjectResource().getMediaInfo().getAudioMetadata().getAudiotrackId()
				);
		} catch (SQLException e) {
			throw new ContentDirectoryException(712, "UpdateObject() failed because of SQL exception.");
		}
	}
}
