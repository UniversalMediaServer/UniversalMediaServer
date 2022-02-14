package net.pms.network.mediaserver.handlers.api;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

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
public class StarRating implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(StarRating.class.getName());
	private MediaDatabase db = PMS.get().getMediaDatabase();
	public static final String PATH_MATCH = "rating";

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		try (Connection connection = db.getConnection()) {
			if (connection == null) {
				output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
				return "database unavailable";
			}

			String sql = null;
			switch (uri) {
				case "setrating":
					RequestVO request = parseSetRatingRequest(content);
					setDatabaseRating(connection, request.stars, request.trackID);
					List<FilenameIdVO> dbSongs = getFilenameIdList(connection, request.trackID);
					if (PMS.getConfiguration().isAudioUpdateTag()) {
						for (FilenameIdVO dbSong : dbSongs) {
							setRatingInFile(request.stars, dbSong);
						}
					}
					break;
				case "getrating":
					sql = "Select distinct f.rating from FILES as f left outer join AUDIOTRACKS as a on F.ID = A.FILEID where a.MBID_TRACK = ?";
					try {
						PreparedStatement ps = connection.prepareStatement(sql);
						ps.setString(1, content);
						ResultSet rs = ps.executeQuery();
						if (rs.next()) {
							int ratingVal = rs.getInt(1);
							return Integer.toString(ratingVal);
						}
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
					}
					break;
				default:
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return "unknown api path : " + uri;
			}

			output.setStatus(HttpResponseStatus.OK);
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
			output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			return "OK";
		} catch (NumberFormatException e) {
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return "illegal rating. Set rating between 0 and 5 (inclusive)";
		} catch (Exception e) {
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return "ERROR : " + e.getMessage();
		}
	}

	private RequestVO parseSetRatingRequest(String content) {
		if (content.indexOf('/') < 0) {
			throw new RuntimeException("illegal API call");
		}

		String[] contentArray = content.split("/");
		RequestVO request = new RequestVO(contentArray[0], Integer.parseInt(contentArray[1]));
		if (request.stars < 0 || request.stars > 5) {
			throw new NumberFormatException("Rating value must be between 0 and 5 (including).");
		}
		if (StringUtils.isBlank(request.trackID)) {
			throw new NumberFormatException("musicBraintID shall not be null.");
		}
		return request;
	}

	public void setRatingInFile(int ratingInStars, FilenameIdVO dbSong) {
		AudioFile audioFile;
		try {
			audioFile = AudioFileIO.read(new File(dbSong.filename));
			Tag tag = audioFile.getTag();
			tag.setField(FieldKey.RATING, getRatingValue(tag, ratingInStars));
			audioFile.commit();
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException e) {
			LOG.warn("Error writing Tag info.", e);
		}
	}

	public void setDatabaseRating(Connection connection, int ratingInStars, String musicBrainzTrackId) {
		String sql;
		sql = "UPDATE FILES set rating = ? where ID in (select fileid from audiotracks where MBID_TRACK = ?)";
		try {
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setInt(1, ratingInStars);
			ps.setString(2, musicBrainzTrackId);
			ps.executeUpdate();
		} catch (SQLException e) {
			LOG.warn("error preparing statement", e);
		}
	}

	private List<FilenameIdVO> getFilenameIdList(Connection connection, String trackId) {
		ArrayList<FilenameIdVO> list = new ArrayList<>();
		String sql = "Select f.id, filename from FILES as f left outer join AUDIOTRACKS as a on F.ID = A.FILEID where a.MBID_TRACK = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql);) {
			ps.setString(1, trackId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new FilenameIdVO(rs.getInt(1), rs.getString(2)));
			}
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
		}
		if (list.isEmpty()) {
			throw new RuntimeException("musicbraint trackid not found : " + trackId);
		} else {
			return list;
		}
	}

	/**
	 * converts 0-5 star rating to format specific value.
	 *
	 * @param tag Tag to update
	 * @param stars number of stars (0 - 5)
	 * @return
	 */
	public String getRatingValue(Tag tag, int stars) {
		int num = 0;
		if (tag instanceof FlacTag || tag instanceof VorbisCommentTag) {
			num = convertStarsToVorbis(stars);
		} else if (tag instanceof AbstractID3v2Tag || tag instanceof ID3v11Tag) {
			num = convertStarsToID3(stars);
		} else {
			// Dont't know ... maybe we use vorbis tags by default
			num = convertStarsToVorbis(stars);
		}

		return "" + num;
	}

	/**
	 * Converts 0-5 stars to MP3 TAG value
	 * @param rating
	 * @return
	 */
	public int convertStarsToID3(int rating) {
		if (rating == 0) {
			return 0;
		} else if (rating == 1) {
			return 1;
		} else if (rating == 2) {
			return 64;
		} else if (rating == 3) {
			return 128;
		} else if (rating == 4) {
			return 196;
		} else {
			return 255;
		}
	}

	/**
	 * converts 0-5 stars to VORBIS TAG value
	 * @param rating
	 * @return
	 */
	public int convertStarsToVorbis(int rating) {
		return rating * 20;
	}

	/**
	 * Converts TAG values read from file to 0-5 stars
	 * @param tag
	 */
	public static int convertTagRatingToStar(Tag tag) {
		String value = tag.getFirst(FieldKey.RATING);
		if (!StringUtils.isBlank(value)) {
			int num = Integer.parseInt(value);
			if (tag instanceof FlacTag || tag instanceof VorbisCommentTag) {
				return convertVorbisToStars(num);
			} else if (tag instanceof AbstractID3v2Tag || tag instanceof ID3v11Tag) {
				return convertID3ToStars(num);
			} else {
				// Dont't know ... maybe we use vorbis tags by default
				return convertVorbisToStars(num);
			}
		}
		return 0;
	}

	public static int convertID3ToStars(int num) {
		if (num == 0) {
			return 0;
		} else if (num < 32) {
			return 1;
		} else if (num < 96) {
			return 2;
		} else if (num < 160) {
			return 3;
		} else if (num < 224) {
			return 4;
		} else {
			return 5;
		}
	}

	public static int convertVorbisToStars(int num) {
		if (num == 0) {
			return 0;
		} else if (num < 21) {
			return 1;
		} else if (num < 41) {
			return 2;
		} else if (num < 61) {
			return 3;
		} else if (num < 81) {
			return 4;
		} else {
			return 5;
		}
	}
}
