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
package net.pms.network.mediaserver.handlers.api.starrating;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
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
import net.pms.dlna.DLNAResource;
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
	public static final String PATH_MATCH = "rating";
	private final MediaDatabase db = PMS.get().getMediaDatabase();

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		try (Connection connection = db.getConnection()) {
			if (connection == null) {
				output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
				return "database unavailable";
			}

			String uriLower = uri.toLowerCase();
			String sql;
			switch (uriLower) {
				case "setrating" -> {
					RequestVO request = parseSetRatingRequest(content);
					setDatabaseRatingByMusicbrainzId(connection, request.getStars(), request.getTrackID());
					List<FilenameIdVO> dbSongs = getFilenameIdList(connection, request.getTrackID());
					if (PMS.getConfiguration().isAudioUpdateTag()) {
						for (FilenameIdVO dbSong : dbSongs) {
							setRatingInFile(request.getStars(), dbSong);
						}
					}
				}
				case "getrating" -> {
					sql = "Select distinct rating from FILES as f left outer join AUDIOTRACKS as a on F.ID = A.FILEID where a.MBID_TRACK = ?";
					try (PreparedStatement ps = connection.prepareStatement(sql)) {
						ps.setString(1, content);
						try (ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								int ratingVal = rs.getInt(1);
								return Integer.toString(ratingVal);
							}
						}
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
					}
				}
				case "setratingbyaudiotrackid" -> {
					RequestVO request = parseSetRatingRequest(content);
					if (NumberUtils.isParsable(request.getTrackID())) {
						Integer audiotrackId = Integer.valueOf(request.getTrackID());
						setDatabaseRatingByAudiotracksId(connection, request.getStars(), audiotrackId);
						updateGlobalRepoCache(request);
						if (PMS.getConfiguration().isAudioUpdateTag()) {
							FilenameIdVO dbSong = getFilenameIdForAudiotrackId(connection, audiotrackId);
							setRatingInFile(request.getStars(), dbSong);
						}
					}
				}
				case "getratingbyaudiotrackid" -> {
					sql = "Select distinct rating from FILES as f left outer join AUDIOTRACKS as a on F.ID = A.FILEID where a.AUDIOTRACK_ID = ?";
					try (PreparedStatement ps = connection.prepareStatement(sql)) {
						ps.setString(1, content);
						try (ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								int ratingVal = rs.getInt(1);
								return Integer.toString(ratingVal);
							}
						}
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
					}
				}
				default -> {
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return "unknown api path : " + uri;
				}
			}

			output.setStatus(HttpResponseStatus.OK);
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
			output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			return "OK";
		} catch (NumberFormatException e) {
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return "illegal rating. Set rating between 0 and 5 (inclusive)";
		} catch (SQLException e) {
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return "database error : " + e.getMessage();
		} catch (Exception e) {
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return "ERROR : " + e.getMessage();
		}
	}

	private void updateGlobalRepoCache(RequestVO request) {
		try {
			DLNAResource cachedObj = PMS.getGlobalRepo().get("" + (request.getGlobalID()));
			if (cachedObj != null) {
				cachedObj.getMedia().getFirstAudioTrack().setRating(request.getStars());
				LOG.trace("updated GlobalRepo cache object successfully.");
			}
		} catch (NullPointerException e) {
			LOG.trace("couldn't update rating info in cache.");
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

	public void setRatingInFile(int ratingInStars, FilenameIdVO dbSong) {
		AudioFile audioFile;
		try {
			audioFile = AudioFileIO.read(new File(dbSong.getFilename()));
			Tag tag = audioFile.getTag();
			tag.setField(FieldKey.RATING, getRatingValue(tag, ratingInStars));
			audioFile.commit();
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException e) {
			LOG.warn("Error writing Tag info.", e);
		}
	}

	public void setDatabaseRatingByMusicbrainzId(Connection connection, int ratingInStars, String musicBrainzTrackId) throws SQLException {
		String sql;
		sql = "UPDATE AUDIOTRACKS set rating = ? where MBID_TRACK = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, ratingInStars);
			ps.setString(2, musicBrainzTrackId);
			ps.executeUpdate();
			connection.commit();
		}
	}

	public void setDatabaseRatingByAudiotracksId(Connection connection, int ratingInStars, Integer audiotracksId) throws SQLException {
		String sql;
		sql = "UPDATE AUDIOTRACKS set rating = ? where AUDIOTRACK_ID = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, ratingInStars);
			if (audiotracksId == null) {
				ps.setNull(2, Types.INTEGER);
			} else {
				ps.setInt(2, audiotracksId);
			}
			ps.executeUpdate();
			connection.commit();
		}
	}

	private List<FilenameIdVO> getFilenameIdList(Connection connection, String trackId) {
		if (trackId == null) {
			throw new RuntimeException("musicBrainz trackId shall not be empty.");
		}

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
			throw new RuntimeException("musicbrainz trackid not found : " + trackId);
		} else {
			return list;
		}
	}

	private FilenameIdVO getFilenameIdForAudiotrackId(Connection connection, Integer audiotrackId) {
		if (audiotrackId == null) {
			throw new RuntimeException("audiotrackId shall not be empty.");
		}

		String sql = "Select f.id, filename from FILES as f left outer join AUDIOTRACKS as a on F.ID = A.FILEID where a.AUDIOTRACK_ID = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql);) {
			ps.setInt(1, audiotrackId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new FilenameIdVO(rs.getInt(1), rs.getString(2));
			} else {
				throw new RuntimeException("audiotrackId not found : " + audiotrackId);
			}
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
	public String getRatingValue(Tag tag, int stars) {
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
	public int convertStarsToID3(int rating) {
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
	public int convertStarsToVorbis(int rating) {
		return rating * 20;
	}

	/**
	 * Converts TAG values read from file to 0-5 stars
	 *
	 * @param tag
	 */
	public static Integer convertTagRatingToStar(Tag tag) {
		try {
			if (tag == null) {
				return null;
			}

			String value = tag.getFirst(FieldKey.RATING);
			if (!StringUtils.isBlank(value)) {
				int num = Integer.parseInt(value);
				if (tag instanceof FlacTag || tag instanceof VorbisCommentTag) {
					return convertVorbisToStars(num);
				} else if (tag instanceof AbstractID3v2Tag || tag instanceof ID3v11Tag) {
					return convertID3ToStars(num);
				} else {
					// Don't know ... maybe we use vorbis tags by default
					return convertVorbisToStars(num);
				}
			}
		} catch (NumberFormatException | KeyNotFoundException e) {
			// Value couldn't be read.
			LOG.trace("conversion error", e);
		}
		return null;
	}

	public static Integer convertID3ToStars(Integer num) {
		if (num == null) {
			return null;
		}
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

	public static Integer convertVorbisToStars(Integer num) {
		if (num == null) {
			return null;
		}
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
