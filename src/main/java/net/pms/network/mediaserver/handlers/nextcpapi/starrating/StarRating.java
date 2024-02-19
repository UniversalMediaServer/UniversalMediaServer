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
package net.pms.network.mediaserver.handlers.nextcpapi.starrating;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudioMetadata;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponse;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponseHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
//FIXME : this should be implemented under upnp, UpdateObject() -> metadata.
public class StarRating implements NextcpApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(StarRating.class.getName());
	public static final String PATH_MATCH = "rating";
	private final MediaDatabase db = PMS.get().getMediaDatabase();

	@Override
	public NextcpApiResponse handleRequest(String uri, String content) {
		NextcpApiResponse response = new NextcpApiResponse();
		try (Connection connection = db.getConnection()) {
			if (connection == null) {
				response.setStatusCode(503);
				response.setResponse("database unavailable");
				return response;
			}

			String uriLower = uri.toLowerCase();
			switch (uriLower) {
				case "setrating" -> {
					RequestVO request = parseSetRatingRequest(content);
					setDatabaseRatingByMusicbrainzTrackId(connection, request.getStars(), request.getTrackID());
					if (PMS.getConfiguration().isAudioUpdateTag()) {
						List<String> filenames = getFilenameListByMusicbrainzTrackId(connection, request.getTrackID());
						for (String filename : filenames) {
							setRatingInFile(request.getStars(), filename);
						}
					}
				}
				case "getrating" -> {
					Integer rating = MediaTableAudioMetadata.getRatingByMusicbrainzTrackId(connection, content);
					if (rating != null) {
						response.setResponse(Integer.toString(rating));
						return response;
					}
				}
				case "setratingbyaudiotrackid" -> {
					RequestVO request = parseSetRatingRequest(content);
					if (NumberUtils.isParsable(request.getTrackID())) {
						Integer audiotrackId = Integer.valueOf(request.getTrackID());
						MediaTableAudioMetadata.updateRatingByAudiotrackId(connection, request.getStars(), audiotrackId);
						if (PMS.getConfiguration().isAudioUpdateTag()) {
							String filename = getFilenameForAudiotrackId(connection, audiotrackId);
							setRatingInFile(request.getStars(), filename);
						}
					}
				}
				case "getratingbyaudiotrackid" -> {
					Integer rating = MediaTableAudioMetadata.getRatingByAudiotrackId(connection, Integer.valueOf(content));
					if (rating != null) {
						response.setResponse(Integer.toString(rating));
						return response;
					}
				}
				default -> {
					response.setStatusCode(404);
					response.setResponse("unknown api path : " + uri);
					return response;
				}
			}

			response.setStatusCode(200);
			response.setContentType("text/plain; charset=UTF-8");
			response.setConnection("keep-alive");
			response.setResponse("OK");
			return response;
		} catch (NumberFormatException e) {
			response.setStatusCode(503);
			response.setResponse("illegal rating. Set rating between 0 and 5 (inclusive)");
			return response;
		} catch (SQLException e) {
			response.setStatusCode(503);
			response.setResponse("database error : " + e.getMessage());
			return response;
		} catch (Exception e) {
			response.setStatusCode(503);
			response.setResponse("ERROR : " + e.getMessage());
			return response;
		}
	}

	private RequestVO parseSetRatingRequest(String content) {
		if (content.indexOf('/') < 0) {
			throw new RuntimeException("illegal API call");
		}

		String[] contentArray = content.split("/");
		if (contentArray.length < 3) {
			throw new RuntimeException("illegal API call : expected 3 parameters");
		}
		RequestVO request = new RequestVO(contentArray[0], contentArray[2], Integer.parseInt(contentArray[1]));
		if (!request.isStarsValid()) {
			throw new NumberFormatException("Rating value must be between 0 and 5 (including).");
		}
		if (StringUtils.isBlank(request.getTrackID())) {
			throw new NumberFormatException("musicBraintID shall not be null.");
		}
		return request;
	}

	private void setRatingInFile(int ratingInStars, String filename) {
		if (StringUtils.isEmpty(filename)) {
			return;
		}
		try {
			AudioFile audioFile = AudioFileIO.read(new File(filename));
			Tag tag = audioFile.getTag();
			tag.setField(FieldKey.RATING, getRatingValue(tag, ratingInStars));
			audioFile.commit();
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException e) {
			LOG.warn("Error writing Tag info.", e);
		}
	}

	private void setDatabaseRatingByMusicbrainzTrackId(Connection connection, int ratingInStars, String musicBrainzTrackId) throws SQLException {
		MediaTableAudioMetadata.updateRatingByMusicbrainzTrackId(connection, ratingInStars, musicBrainzTrackId);
	}

	private List<String> getFilenameListByMusicbrainzTrackId(Connection connection, String trackId) {
		if (trackId == null) {
			throw new RuntimeException("musicBrainz trackId shall not be empty.");
		}
		try {
			List<String> filenames = MediaTableAudioMetadata.getFilenamesByMusicbrainzTrackId(connection, trackId);
			if (filenames.isEmpty()) {
				throw new RuntimeException("musicbrainz trackid not found : " + trackId);
			}
			return filenames;
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
		}
	}

	private String getFilenameForAudiotrackId(Connection connection, Integer audiotrackId) {
		if (audiotrackId == null) {
			throw new RuntimeException("audiotrackId shall not be empty.");
		}
		try {
			String filename = MediaTableAudioMetadata.getFilenameByAudiotrackId(connection, audiotrackId);
			if (filename == null) {
				throw new RuntimeException("audiotrackId not found : " + audiotrackId);
			}
			return filename;
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
		}
	}

	/**
	 * converts 0-5 star rating to format specific value.
	 *
	 * @param tag Tag to update
	 * @param stars number of stars (0 - 5)
	 * @return
	 */
	private String getRatingValue(Tag tag, int stars) {
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

}
