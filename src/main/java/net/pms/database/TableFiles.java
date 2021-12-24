/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.database;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.dlna.*;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImageInfo;
import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang3.StringUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.util.APIUtils;
import net.pms.util.FileUtil;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class TableFiles extends TableHelper {

	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock(true);
	private static final Logger LOGGER = LoggerFactory.getLogger(TableFiles.class);
	public static final String TABLE_NAME = "FILES";
	
	public static final String NONAME = "###";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 * 
	 * Version notes:
	 * - 18: Introduced ALBUMARTIST field
	 * - 19: Introduced EXTERNALFILE and CHARSET fields
	 *       Released in versions 8.0.0-a1 and a2
	 * - 20: No db changes, bumped version because a parsing bug was fixed
	 *       Released in version 8.0.0-b1
	 * - 21: No db changes, bumped version because a parsing bug was fixed
	 *       Released in version 8.0.0
	 * - 22: No db changes, bumped version because h2database was reverted
	 *       to 1.4.196 because 1.4.197 broke audio metadata being
	 *       inserted/updated
	 * - 23: Store aspect ratios as strings again
	 * - 24: Added VERSION column to FILES table which keeps track of which
	 *       API metadata version is saved for that file
	 */
	private static final int TABLE_VERSION = 25;

	// Database column sizes
	private static final int SIZE_CODECV = 32;
	private static final int SIZE_FRAMERATE = 32;
	private static final int SIZE_AVCLEVEL = 3;
	private static final int SIZE_CONTAINER = 32;
	private static final int SIZE_IMDBID = 16;
	private static final int SIZE_MATRIX_COEFFICIENTS = 16;
	private static final int SIZE_MUXINGMODE = 32;
	private static final int SIZE_FRAMERATEMODE = 16;

	private static final int SIZE_SAMPLEFREQ = 16;
	private static final int SIZE_CODECA = 32;
	private static final int SIZE_GENRE = 64;
	private static final int SIZE_YEAR = 4;
	private static final int SIZE_TVSEASON = 4;
	private static final int SIZE_TVEPISODENUMBER = 8;

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		TABLE_LOCK.writeLock().lock();
		try {
			if (tableExists(connection, TABLE_NAME)) {
				Integer version = TableTablesVersions.getTableVersion(connection, TABLE_NAME);
				if (version == null) {
					// Moving sql from DLNAMediaDatabase to this class.
					version = 24;
				}
				if (version < TABLE_VERSION) {
					upgradeTable(connection, version);
				} else if (version > TABLE_VERSION) {
					LOGGER.warn("Database table \"" + TABLE_NAME + "\" is from a newer version of UMS.");
				}
			} else {
				createTable(connection);
				TableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * This method <strong>MUST</strong> be updated if the table definition are
	 * altered. The changes for each version in the form of
	 * <code>ALTER TABLE</code> must be implemented here.
	 *
	 * @param connection the {@link Connection} to use
	 * @param currentVersion the version to upgrade <strong>from</strong>
	 *
	 * @throws SQLException
	 */
	@SuppressFBWarnings("IIL_PREPARE_STATEMENT_IN_LOOP")
	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		boolean force = false;
		TABLE_LOCK.writeLock().lock();
		try {
			for (int version = currentVersion; version < TABLE_VERSION; version++) {
				LOGGER.trace("Upgrading table {} from version {} to {}", TABLE_NAME, version, version + 1);
				switch (version) {
					case 23:
						try (Statement statement = connection.createStatement()) {
							StringBuilder sb = new StringBuilder();
							sb.append("ALTER TABLE ").append(TABLE_NAME).append(" ADD VERSION VARCHAR2(").append(SIZE_MAX).append(')');
							statement.execute(sb.toString());

							/*
							 * Since the last release, 10.12.0, we fixed some bugs with TV episode filename parsing
							 * so here we clear any cached data for non-episodes.
							 */
							sb = new StringBuilder();
							sb
								.append("UPDATE ")
									.append("FILES ")
								.append("SET ")
									.append("IMDBID = NULL, ")
									.append("`YEAR` = NULL, ")
									.append("MOVIEORSHOWNAME = NULL, ")
									.append("MOVIEORSHOWNAMESIMPLE = NULL, ")
									.append("TVSEASON = NULL, ")
									.append("TVEPISODENUMBER = NULL, ")
									.append("TVEPISODENAME = NULL, ")
									.append("ISTVEPISODE = NULL, ")
									.append("EXTRAINFORMATION = NULL ")
								.append("WHERE ")
									.append("NOT ISTVEPISODE");
							statement.execute(sb.toString());

							statement.execute("CREATE INDEX FILENAME_MODIFIED_VERSION_IMDBID on FILES (FILENAME, MODIFIED, VERSION, IMDBID)");

							statement.execute("CREATE UNIQUE INDEX IDX_KEY ON METADATA(`KEY`)");
						}
						version++;
						LOGGER.trace("Updated {} table from version {} to {}", TABLE_NAME, currentVersion, version);
						break;
					case 24:
						//Rename sql reserved words
						
						LOGGER.trace("Deleting index TYPE_ISTV");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV");
						LOGGER.trace("Deleting index TYPE_ISTV_SIMPLENAME");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_SIMPLENAME");
						LOGGER.trace("Deleting index TYPE_ISTV_NAME");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_NAME");
						LOGGER.trace("Deleting index TYPE_ISTV_NAME_SEASON");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_NAME_SEASON");
						LOGGER.trace("Deleting index TYPE_ISTV_YEAR_STEREOSCOPY");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_YEAR_STEREOSCOPY");
						LOGGER.trace("Deleting index TYPE_WIDTH_HEIGHT");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_WIDTH_HEIGHT");
						LOGGER.trace("Deleting index TYPE_MODIFIED");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_MODIFIED");
						if (isColumnExist(connection, TABLE_NAME, "TYPE")) {
							LOGGER.trace("Renaming column name TYPE to FORMAT_TYPE");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `TYPE` RENAME TO FORMAT_TYPE");
						}
						if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
							LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
						}
						if (isColumnExist(connection, TABLE_NAME, "SIZE")) {
							LOGGER.trace("Renaming column name SIZE to MEDIA_SIZE");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `SIZE` RENAME TO MEDIA_SIZE");
						}
						LOGGER.trace("Creating index FORMAT_TYPE");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE on FILES (FORMAT_TYPE)");
						LOGGER.trace("Creating index FORMAT_TYPE_ISTV");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_ISTV on FILES (FORMAT_TYPE, ISTVEPISODE)");
						LOGGER.trace("Creating index FORMAT_TYPE_ISTV_SIMPLENAME");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_ISTV_SIMPLENAME on FILES (FORMAT_TYPE, ISTVEPISODE, MOVIEORSHOWNAMESIMPLE)");
						LOGGER.trace("Creating index FORMAT_TYPE_ISTV_NAME");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_ISTV_NAME on FILES (FORMAT_TYPE, ISTVEPISODE, MOVIEORSHOWNAME)");
						LOGGER.trace("Creating index FORMAT_TYPE_ISTV_NAME_SEASON");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_ISTV_NAME_SEASON on FILES (FORMAT_TYPE, ISTVEPISODE, MOVIEORSHOWNAME, TVSEASON)");
						LOGGER.trace("Creating index FORMAT_TYPE_ISTV_YEAR_STEREOSCOPY");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_ISTV_YEAR_STEREOSCOPY on FILES (FORMAT_TYPE, ISTVEPISODE, MEDIA_YEAR, STEREOSCOPY)");
						LOGGER.trace("Creating index FORMAT_TYPE_WIDTH_HEIGHT");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_WIDTH_HEIGHT on FILES (FORMAT_TYPE, WIDTH, HEIGHT)");
						LOGGER.trace("Creating index FORMAT_TYPE_MODIFIED");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_MODIFIED on FILES (FORMAT_TYPE, MODIFIED)");
						break;
					default:
						// Do the dumb way
						force = true;
				}
			}
			try {
				TableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			} catch (SQLException e) {
				LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
				throw new SQLException(e);
			}
		} catch (Exception se) {
			LOGGER.error("Error updating tables: " + se.getMessage());
			LOGGER.trace("", se);
			force = true;
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
		if (force) {
			LOGGER.debug("Database will be (re)initialized");
			try {
				MediasDatabase.dropTable(connection, TABLE_NAME);
				createTable(connection);
				TableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			} catch (SQLException se) {
				LOGGER.error("SQL error while (re)initializing tables: {}", se.getMessage());
				LOGGER.trace("", se);
			}
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE " + TABLE_NAME + " (");
			sb.append("  ID                      INT AUTO_INCREMENT PRIMARY KEY");
			sb.append(", THUMBID                 BIGINT");
			sb.append(", FILENAME                VARCHAR2(1024)   NOT NULL");
			sb.append(", MODIFIED                TIMESTAMP        NOT NULL");
			sb.append(", FORMAT_TYPE             INT");
			sb.append(", DURATION                DOUBLE");
			sb.append(", BITRATE                 INT");
			sb.append(", WIDTH                   INT");
			sb.append(", HEIGHT                  INT");
			sb.append(", MEDIA_SIZE              NUMERIC");
			sb.append(", CODECV                  VARCHAR2(").append(SIZE_CODECV).append(')');
			sb.append(", FRAMERATE               VARCHAR2(").append(SIZE_FRAMERATE).append(')');
			sb.append(", ASPECTRATIODVD          VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", ASPECTRATIOCONTAINER    VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", ASPECTRATIOVIDEOTRACK   VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", REFRAMES                TINYINT");
			sb.append(", AVCLEVEL                VARCHAR2(").append(SIZE_AVCLEVEL).append(')');
			sb.append(", IMAGEINFO               OTHER");
			sb.append(", CONTAINER               VARCHAR2(").append(SIZE_CONTAINER).append(')');
			sb.append(", MUXINGMODE              VARCHAR2(").append(SIZE_MUXINGMODE).append(')');
			sb.append(", FRAMERATEMODE           VARCHAR2(").append(SIZE_FRAMERATEMODE).append(')');
			sb.append(", STEREOSCOPY             VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", MATRIXCOEFFICIENTS      VARCHAR2(").append(SIZE_MATRIX_COEFFICIENTS).append(')');
			sb.append(", TITLECONTAINER          VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", TITLEVIDEOTRACK         VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", VIDEOTRACKCOUNT         INT");
			sb.append(", IMAGECOUNT              INT");
			sb.append(", BITDEPTH                INT");
			sb.append(", PIXELASPECTRATIO        VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", SCANTYPE                OTHER");
			sb.append(", SCANORDER               OTHER");
			sb.append(", IMDBID                  VARCHAR2(").append(SIZE_IMDBID).append(')');
			sb.append(", MEDIA_YEAR              VARCHAR2(").append(SIZE_YEAR).append(')');
			sb.append(", MOVIEORSHOWNAME         VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", MOVIEORSHOWNAMESIMPLE   VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", TVSEASON                VARCHAR2(").append(SIZE_TVSEASON).append(')');
			sb.append(", TVEPISODENUMBER         VARCHAR2(").append(SIZE_TVEPISODENUMBER).append(')');
			sb.append(", TVEPISODENAME           VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", ISTVEPISODE             BOOLEAN");
			sb.append(", EXTRAINFORMATION        VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", VERSION                 VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(")");
			LOGGER.trace("Creating table FILES with:\n\n{}\n", sb.toString());

			statement.execute(sb.toString());

			LOGGER.trace("Creating index IDX_FILE");
			statement.execute("CREATE UNIQUE INDEX IDX_FILE ON FILES(FILENAME, MODIFIED)");
			LOGGER.trace("Creating index IDX_FILENAME_MODIFIED_IMDBID");
			statement.execute("CREATE INDEX IDX_FILENAME_MODIFIED_IMDBID ON FILES(FILENAME, MODIFIED, IMDBID)");

			LOGGER.trace("Creating index FILENAME_MODIFIED_VERSION_IMDBID");
			statement.execute("CREATE INDEX FILENAME_MODIFIED_VERSION_IMDBID on FILES (FILENAME, MODIFIED, VERSION, IMDBID)");

			LOGGER.trace("Creating index FORMAT_TYPE");
			statement.execute("CREATE INDEX FORMAT_TYPE on FILES (FORMAT_TYPE)");

			LOGGER.trace("Creating index FORMAT_TYPE_ISTV");
			statement.execute("CREATE INDEX FORMAT_TYPE_ISTV on FILES (FORMAT_TYPE, ISTVEPISODE)");

			LOGGER.trace("Creating index FORMAT_TYPE_ISTV_SIMPLENAME");
			statement.execute("CREATE INDEX FORMAT_TYPE_ISTV_SIMPLENAME on FILES (FORMAT_TYPE, ISTVEPISODE, MOVIEORSHOWNAMESIMPLE)");

			LOGGER.trace("Creating index FORMAT_TYPE_ISTV_NAME");
			statement.execute("CREATE INDEX FORMAT_TYPE_ISTV_NAME on FILES (FORMAT_TYPE, ISTVEPISODE, MOVIEORSHOWNAME)");

			LOGGER.trace("Creating index FORMAT_TYPE_ISTV_NAME_SEASON");
			statement.execute("CREATE INDEX FORMAT_TYPE_ISTV_NAME_SEASON on FILES (FORMAT_TYPE, ISTVEPISODE, MOVIEORSHOWNAME, TVSEASON)");

			LOGGER.trace("Creating index FORMAT_TYPE_ISTV_YEAR_STEREOSCOPY");
			statement.execute("CREATE INDEX FORMAT_TYPE_ISTV_YEAR_STEREOSCOPY on FILES (FORMAT_TYPE, ISTVEPISODE, MEDIA_YEAR, STEREOSCOPY)");

			LOGGER.trace("Creating index FORMAT_TYPE_WIDTH_HEIGHT");
			statement.execute("CREATE INDEX FORMAT_TYPE_WIDTH_HEIGHT on FILES (FORMAT_TYPE, WIDTH, HEIGHT)");

			LOGGER.trace("Creating index FORMAT_TYPE_MODIFIED");
			statement.execute("CREATE INDEX FORMAT_TYPE_MODIFIED on FILES (FORMAT_TYPE, MODIFIED)");
		}
	}

	/**
	 * Drops (deletes) the current table. Use with caution, there is no undo.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static final void dropTable(final Connection connection) throws SQLException {
		LOGGER.debug("Dropping database table if it exists \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
		}
	}

	/**
	 * Checks whether a row representing a {@link DLNAMediaInfo} instance for
	 * the given media exists in the database.
	 *
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return {@code true} if the data exists for this media, {@code false}
	 *         otherwise.
	 */
	public static boolean isDataExists(String name, long modified) {
		boolean found = false;
		try (Connection connection = DATABASE.getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (PreparedStatement statement = connection.prepareStatement("SELECT ID FROM " + TABLE_NAME + " WHERE FILENAME = ? AND MODIFIED = ? LIMIT 1")) {
				statement.setString(1, name);
				statement.setTimestamp(2, new Timestamp(modified));
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						found = true;
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException se) {
			LOGGER.error("An SQL error occurred when trying to check if data exists for \"{}\": {}", name, se.getMessage());
			LOGGER.trace("", se);
			return false;
		}
		return found;
	}

	/**
	 * Checks whether the latest data from our API has been written to the
	 * database for this video.
	 * If we could not fetch the latest version from the API, it will check
	 * whether any version exists in the database.
	 *
	 * @param name the full path of the video.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return whether the latest API metadata exists for this video.
	 */
	public static boolean doesLatestApiMetadataExist(String name, long modified) {
		boolean found = false;
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ID FROM " + TABLE_NAME + " WHERE FILENAME = ? AND MODIFIED = ? ");
		String latestVersion = null;
		if (CONFIGURATION.getExternalNetwork()) {
			latestVersion = APIUtils.getApiDataVideoVersion();
			if (latestVersion != null) {
				sql.append("AND VERSION = ? ");
			}
		}
		sql.append("AND IMDBID IS NOT NULL LIMIT 1");

		try (Connection connection = DATABASE.getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
				statement.setString(1, name);
				statement.setTimestamp(2, new Timestamp(modified));
				if (latestVersion != null) {
					statement.setString(3, latestVersion);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						found = true;
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (Exception se) {
			LOGGER.error(
				"An error occurred when trying to check if API metadata exists for \"{}\": {}",
				name,
				se.getMessage()
			);
			LOGGER.trace("", se);
			return false;
		}

		return found;
	}

	/**
	 * Gets a row of {@link DLNAMediaDatabase} from the database and returns it
	 * as a {@link DLNAMediaInfo} instance, along with thumbnails, status and tracks.
	 *
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return The {@link DLNAMediaInfo} instance matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public static DLNAMediaInfo getData(String name, long modified) throws IOException, SQLException {
		DLNAMediaInfo media = null;
		ArrayList<String> externalFileReferencesToRemove = new ArrayList();
		try (Connection conn = DATABASE.getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (
				PreparedStatement stmt = conn.prepareStatement(
					"SELECT * FROM " + TABLE_NAME + " " +
					"LEFT JOIN " + TableThumbnails.TABLE_NAME + " ON " + TABLE_NAME + ".THUMBID=" + TableThumbnails.TABLE_NAME + ".ID " +
					"WHERE " + TABLE_NAME + ".FILENAME = ? AND " + TABLE_NAME + ".MODIFIED = ? " +
					"LIMIT 1"
				);
			) {
				stmt.setString(1, name);
				stmt.setTimestamp(2, new Timestamp(modified));
				try (
					ResultSet rs = stmt.executeQuery();
					PreparedStatement audios = conn.prepareStatement("SELECT * FROM " + TableAudiotracks.TABLE_NAME + " WHERE FILEID = ?");
					PreparedStatement subs = conn.prepareStatement("SELECT * FROM " + TableSubtracks.TABLE_NAME + " WHERE FILEID = ?");
					PreparedStatement status = conn.prepareStatement("SELECT * FROM " + TableFilesStatus.TABLE_NAME + " WHERE FILENAME = ? LIMIT 1");
				) {
					if (rs.next()) {
						media = new DLNAMediaInfo();
						int id = rs.getInt("ID");
						media.setDuration(toDouble(rs, "DURATION"));
						media.setBitrate(rs.getInt("BITRATE"));
						media.setWidth(rs.getInt("WIDTH"));
						media.setHeight(rs.getInt("HEIGHT"));
						media.setSize(rs.getLong("MEDIA_SIZE"));
						media.setCodecV(rs.getString("CODECV"));
						media.setFrameRate(rs.getString("FRAMERATE"));
						media.setAspectRatioDvdIso(rs.getString("ASPECTRATIODVD"));
						media.setAspectRatioContainer(rs.getString("ASPECTRATIOCONTAINER"));
						media.setAspectRatioVideoTrack(rs.getString("ASPECTRATIOVIDEOTRACK"));
						media.setReferenceFrameCount(rs.getByte("REFRAMES"));
						media.setAvcLevel(rs.getString("AVCLEVEL"));
						media.setImageInfo((ImageInfo) rs.getObject("IMAGEINFO"));
						media.setThumb((DLNAThumbnail) rs.getObject("THUMBNAIL"));
						media.setContainer(rs.getString("CONTAINER"));
						media.setMuxingMode(rs.getString("MUXINGMODE"));
						media.setFrameRateMode(rs.getString("FRAMERATEMODE"));
						media.setStereoscopy(rs.getString("STEREOSCOPY"));
						media.setMatrixCoefficients(rs.getString("MATRIXCOEFFICIENTS"));
						media.setFileTitleFromMetadata(rs.getString("TITLECONTAINER"));
						media.setVideoTrackTitleFromMetadata(rs.getString("TITLEVIDEOTRACK"));
						media.setVideoTrackCount(rs.getInt("VIDEOTRACKCOUNT"));
						media.setImageCount(rs.getInt("IMAGECOUNT"));
						media.setVideoBitDepth(rs.getInt("BITDEPTH"));
						media.setPixelAspectRatio(rs.getString("PIXELASPECTRATIO"));
						media.setScanType((DLNAMediaInfo.ScanType) rs.getObject("SCANTYPE"));
						media.setScanOrder((DLNAMediaInfo.ScanOrder) rs.getObject("SCANORDER"));
						media.setIMDbID(rs.getString("IMDBID"));
						media.setYear(rs.getString("MEDIA_YEAR"));
						media.setMovieOrShowName(rs.getString("MOVIEORSHOWNAME"));
						media.setSimplifiedMovieOrShowName(rs.getString("MOVIEORSHOWNAMESIMPLE"));
						media.setExtraInformation(rs.getString("EXTRAINFORMATION"));

						if (rs.getBoolean("ISTVEPISODE")) {
							media.setTVSeason(rs.getString("TVSEASON"));
							media.setTVEpisodeNumber(rs.getString("TVEPISODENUMBER"));
							media.setTVEpisodeName(rs.getString("TVEPISODENAME"));
							media.setIsTVEpisode(true);
						} else {
							media.setIsTVEpisode(false);
						}
						media.setMediaparsed(true);
						audios.setInt(1, id);
						try (ResultSet elements = audios.executeQuery()) {
							while (elements.next()) {
								DLNAMediaAudio audio = new DLNAMediaAudio();
								audio.setId(elements.getInt("ID"));
								audio.setLang(elements.getString("LANG"));
								audio.setAudioTrackTitleFromMetadata(elements.getString("TITLE"));
								audio.getAudioProperties().setNumberOfChannels(elements.getInt("NRAUDIOCHANNELS"));
								audio.setSampleFrequency(elements.getString("SAMPLEFREQ"));
								audio.setCodecA(elements.getString("CODECA"));
								audio.setBitsperSample(elements.getInt("BITSPERSAMPLE"));
								audio.setAlbum(elements.getString("ALBUM"));
								audio.setArtist(elements.getString("ARTIST"));
								audio.setAlbumArtist(elements.getString("ALBUMARTIST"));
								audio.setSongname(elements.getString("SONGNAME"));
								audio.setGenre(elements.getString("GENRE"));
								audio.setYear(elements.getInt("MEDIA_YEAR"));
								audio.setTrack(elements.getInt("TRACK"));
								audio.setDisc(elements.getInt("DISC"));
								audio.getAudioProperties().setAudioDelay(elements.getInt("DELAY"));
								audio.setMuxingModeAudio(elements.getString("MUXINGMODE"));
								audio.setBitRate(elements.getInt("BITRATE"));
								audio.setMbidRecord(elements.getString("MBID_RECORD"));
								audio.setMbidTrack(elements.getString("MBID_TRACK"));
								media.getAudioTracksList().add(audio);
							}
						}

						subs.setLong(1, id);
						try (ResultSet elements = subs.executeQuery()) {
							while (elements.next()) {
								String fileName = elements.getString("EXTERNALFILE");
								File externalFile = isNotBlank(fileName) ? new File(fileName) : null;
								if (externalFile != null && !externalFile.exists()) {
									externalFileReferencesToRemove.add(externalFile.getPath());
									continue;
								}

								DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
								sub.setId(elements.getInt("ID"));
								sub.setLang(elements.getString("LANG"));
								sub.setSubtitlesTrackTitleFromMetadata(elements.getString("TITLE"));
								sub.setType(SubtitleType.valueOfStableIndex(elements.getInt("FORMAT_TYPE")));
								sub.setExternalFileOnly(externalFile);
								sub.setSubCharacterSet(elements.getString("CHARSET"));
								LOGGER.trace("Adding subtitles from the database for {}: {}", name, sub.toString());
								media.addSubtitlesTrack(sub);
							}
						}

						status.setString(1, name);
						try (ResultSet elements = status.executeQuery()) {
							if (elements.next()) {
								media.setPlaybackCount(elements.getInt("PLAYCOUNT"));
								media.setLastPlaybackTime(elements.getString("DATELASTPLAY"));
								media.setLastPlaybackPosition(elements.getDouble("LASTPLAYBACKPOSITION"));
							}
						}
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();

				// This needs to happen outside of the readLock because deleteRowsInTable has a writeLock
				if (!externalFileReferencesToRemove.isEmpty()) {
					for (String externalFileReferenceToRemove : externalFileReferencesToRemove) {
						LOGGER.trace("Deleting cached external subtitles from database because the file \"{}\" doesn't exist", externalFileReferenceToRemove);
						deleteRowsInTable(TableSubtracks.TABLE_NAME, "EXTERNALFILE", externalFileReferenceToRemove, false);
						externalFileReferencesToRemove.add(externalFileReferenceToRemove);
					}
				}
			}
		} catch (SQLException se) {
			if (se.getCause() != null && se.getCause() instanceof IOException) {
				throw (IOException) se.getCause();
			}
			throw se;
		}
		return media;
	}

	/**
	 * Gets a row of {@link DLNAMediaDatabase} from the database and returns it
	 * as a {@link DLNAMediaInfo} instance.
	 * This is the same as getData above, but is a much smaller query because it
	 * does not fetch thumbnails, status and tracks, and does not require a
	 * modified value to be passed, which means we can avoid touching the filesystem
	 * in the caller.
	 *
	 * @param name the full path of the media.
	 * @return The {@link DLNAMediaInfo} instance matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public static DLNAMediaInfo getFileMetadata(String name) throws IOException, SQLException {
		DLNAMediaInfo media = null;
		try (Connection conn = DATABASE.getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (
				PreparedStatement stmt = conn.prepareStatement(
					"SELECT * FROM " + TABLE_NAME + " " +
					"WHERE " + TABLE_NAME + ".FILENAME = ? " +
					"LIMIT 1"
				);
			) {
				stmt.setString(1, name);
				try (
					ResultSet rs = stmt.executeQuery();
				) {
					if (rs.next()) {
						media = new DLNAMediaInfo();
						media.setIMDbID(rs.getString("IMDBID"));
						media.setYear(rs.getString("MEDIA_YEAR"));
						media.setMovieOrShowName(rs.getString("MOVIEORSHOWNAME"));
						media.setSimplifiedMovieOrShowName(rs.getString("MOVIEORSHOWNAMESIMPLE"));
						media.setExtraInformation(rs.getString("EXTRAINFORMATION"));

						if (rs.getBoolean("ISTVEPISODE")) {
							media.setTVSeason(rs.getString("TVSEASON"));
							media.setTVEpisodeNumber(rs.getString("TVEPISODENUMBER"));
							media.setTVEpisodeName(rs.getString("TVEPISODENAME"));
							media.setIsTVEpisode(true);
						} else {
							media.setIsTVEpisode(false);
						}
						media.setMediaparsed(true);
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException se) {
			if (se.getCause() != null && se.getCause() instanceof IOException) {
				throw (IOException) se.getCause();
			}
			throw se;
		}
		return media;
	}

	/**
	 * Inserts or updates a database row representing an {@link DLNAMediaInfo}
	 * instance. If the row already exists, it will be updated with the
	 * information given in {@code media}. If it doesn't exist, a new will row
	 * be created using the same information.
	 *
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @param type the integer constant from {@link Format} indicating the type
	 *            of media.
	 * @param media the {@link DLNAMediaInfo} row to update.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertOrUpdateData(String name, long modified, int type, DLNAMediaInfo media) throws SQLException {
		try (
			Connection connection = DATABASE.getConnection()
		) {
			connection.setAutoCommit(false);
			long fileId = -1;
			TABLE_LOCK.writeLock().lock();
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"ID, FILENAME, MODIFIED, FORMAT_TYPE, DURATION, BITRATE, WIDTH, HEIGHT, MEDIA_SIZE, CODECV, FRAMERATE, " +
					"ASPECTRATIODVD, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, " +
					"CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
					"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH, PIXELASPECTRATIO, SCANTYPE, SCANORDER, " +
					"IMDBID, MEDIA_YEAR, MOVIEORSHOWNAME, MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION " +
				"FROM " + TABLE_NAME + " " +
				"WHERE " +
					"FILENAME = ? " +
				"LIMIT 1",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, name);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						fileId = rs.getLong("ID");
						rs.updateTimestamp("MODIFIED", new Timestamp(modified));
						rs.updateInt("FORMAT_TYPE", type);
						if (media != null) {
							if (media.getDuration() != null) {
								rs.updateDouble("DURATION", media.getDurationInSeconds());
							} else {
								rs.updateNull("DURATION");
							}

							if (type != Format.IMAGE) {
								if (media.getBitrate() == 0) {
									LOGGER.debug("Could not parse the bitrate for: " + name);
								}
								rs.updateInt("BITRATE", media.getBitrate());
							} else {
								rs.updateInt("BITRATE", 0);
							}
							rs.updateInt("WIDTH", media.getWidth());
							rs.updateInt("HEIGHT", media.getHeight());
							rs.updateLong("MEDIA_SIZE", media.getSize());
							rs.updateString("CODECV", left(media.getCodecV(), SIZE_CODECV));
							rs.updateString("FRAMERATE", left(media.getFrameRate(), SIZE_FRAMERATE));
							rs.updateString("ASPECTRATIODVD", left(media.getAspectRatioDvdIso(), SIZE_MAX));
							rs.updateString("ASPECTRATIOCONTAINER", left(media.getAspectRatioContainer(), SIZE_MAX));
							rs.updateString("ASPECTRATIOVIDEOTRACK", left(media.getAspectRatioVideoTrack(), SIZE_MAX));
							rs.updateByte("REFRAMES", media.getReferenceFrameCount());
							rs.updateString("AVCLEVEL", left(media.getAvcLevel(), SIZE_AVCLEVEL));
							updateSerialized(rs, media.getImageInfo(), "IMAGEINFO");
							if (media.getImageInfo() != null) {
								rs.updateObject("IMAGEINFO", media.getImageInfo());
							} else {
								rs.updateNull("IMAGEINFO");
							}
							rs.updateString("CONTAINER", left(media.getContainer(), SIZE_CONTAINER));
							rs.updateString("MUXINGMODE", left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
							rs.updateString("FRAMERATEMODE", left(media.getFrameRateMode(), SIZE_FRAMERATEMODE));
							rs.updateString("STEREOSCOPY", left(media.getStereoscopy(), SIZE_MAX));
							rs.updateString("MATRIXCOEFFICIENTS", left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
							rs.updateString("TITLECONTAINER", left(media.getFileTitleFromMetadata(), SIZE_MAX));
							rs.updateString("TITLEVIDEOTRACK", left(media.getVideoTrackTitleFromMetadata(), SIZE_MAX));
							rs.updateInt("VIDEOTRACKCOUNT", media.getVideoTrackCount());
							rs.updateInt("IMAGECOUNT", media.getImageCount());
							rs.updateInt("BITDEPTH", media.getVideoBitDepth());
							rs.updateString("PIXELASPECTRATIO", left(media.getPixelAspectRatio(), SIZE_MAX));
							updateSerialized(rs, media.getScanType(), "SCANTYPE");
							updateSerialized(rs, media.getScanOrder(), "SCANORDER");
							rs.updateString("IMDBID", left(media.getIMDbID(), SIZE_IMDBID));
							rs.updateString("MEDIA_YEAR", left(media.getYear(), SIZE_YEAR));
							rs.updateString("MOVIEORSHOWNAME", left(media.getMovieOrShowName(), SIZE_MAX));
							rs.updateString("MOVIEORSHOWNAMESIMPLE", left(media.getSimplifiedMovieOrShowName(), SIZE_MAX));
							rs.updateString("TVSEASON", left(media.getTVSeason(), SIZE_TVSEASON));
							rs.updateString("TVEPISODENUMBER", left(media.getTVEpisodeNumber(), SIZE_TVEPISODENUMBER));
							rs.updateString("TVEPISODENAME", left(media.getTVEpisodeName(), SIZE_MAX));
							rs.updateBoolean("ISTVEPISODE", media.isTVEpisode());
							rs.updateString("EXTRAINFORMATION", left(media.getExtraInformation(), SIZE_MAX));
						}
						rs.updateRow();
					}
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
			if (fileId < 0) {
				// No fileId means it didn't exist
				String columns = "FILENAME, MODIFIED, FORMAT_TYPE, DURATION, BITRATE, WIDTH, HEIGHT, MEDIA_SIZE, CODECV, " +
					"FRAMERATE, ASPECTRATIODVD, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, " +
					"CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
					"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH, PIXELASPECTRATIO, SCANTYPE, SCANORDER, IMDBID, MEDIA_YEAR, MOVIEORSHOWNAME, " +
					"MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION";

				TABLE_LOCK.writeLock().lock();
				try (
					PreparedStatement ps = connection.prepareStatement(
						"INSERT INTO " + TABLE_NAME + " (" + columns + ")" +
						createDefaultValueForInsertStatement(columns),
						Statement.RETURN_GENERATED_KEYS
					)
				) {
					int databaseColumnIterator = 0;

					ps.setString(++databaseColumnIterator, name);
					ps.setTimestamp(++databaseColumnIterator, new Timestamp(modified));
					ps.setInt(++databaseColumnIterator, type);
					if (media != null) {
						if (media.getDuration() != null) {
							ps.setDouble(++databaseColumnIterator, media.getDurationInSeconds());
						} else {
							ps.setNull(++databaseColumnIterator, Types.DOUBLE);
						}

						int databaseBitrate = 0;
						if (type != Format.IMAGE) {
							databaseBitrate = media.getBitrate();
							if (databaseBitrate == 0) {
								LOGGER.debug("Could not parse the bitrate for: " + name);
							}
						}
						ps.setInt(++databaseColumnIterator, databaseBitrate);

						ps.setInt(++databaseColumnIterator, media.getWidth());
						ps.setInt(++databaseColumnIterator, media.getHeight());
						ps.setLong(++databaseColumnIterator, media.getSize());
						ps.setString(++databaseColumnIterator, left(media.getCodecV(), SIZE_CODECV));
						ps.setString(++databaseColumnIterator, left(media.getFrameRate(), SIZE_FRAMERATE));
						ps.setString(++databaseColumnIterator, left(media.getAspectRatioDvdIso(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getAspectRatioContainer(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getAspectRatioVideoTrack(), SIZE_MAX));
						ps.setByte(++databaseColumnIterator, media.getReferenceFrameCount());
						ps.setString(++databaseColumnIterator, left(media.getAvcLevel(), SIZE_AVCLEVEL));
						if (media.getImageInfo() != null) {
							ps.setObject(++databaseColumnIterator, media.getImageInfo());
						} else {
							ps.setNull(++databaseColumnIterator, Types.OTHER);
						}
						ps.setString(++databaseColumnIterator, left(media.getContainer(), SIZE_CONTAINER));
						ps.setString(++databaseColumnIterator, left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
						ps.setString(++databaseColumnIterator, left(media.getFrameRateMode(), SIZE_FRAMERATEMODE));
						ps.setString(++databaseColumnIterator, left(media.getStereoscopy(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
						ps.setString(++databaseColumnIterator, left(media.getFileTitleFromMetadata(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getVideoTrackTitleFromMetadata(), SIZE_MAX));
						ps.setInt(++databaseColumnIterator, media.getVideoTrackCount());
						ps.setInt(++databaseColumnIterator, media.getImageCount());
						ps.setInt(++databaseColumnIterator, media.getVideoBitDepth());
						ps.setString(++databaseColumnIterator, left(media.getPixelAspectRatio(), SIZE_MAX));
						insertSerialized(ps, media.getScanType(), ++databaseColumnIterator);
						insertSerialized(ps, media.getScanOrder(), ++databaseColumnIterator);
						ps.setString(++databaseColumnIterator, left(media.getIMDbID(), SIZE_IMDBID));
						ps.setString(++databaseColumnIterator, left(media.getYear(), SIZE_YEAR));
						ps.setString(++databaseColumnIterator, left(media.getMovieOrShowName(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getSimplifiedMovieOrShowName(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getTVSeason(), SIZE_TVSEASON));
						ps.setString(++databaseColumnIterator, left(media.getTVEpisodeNumber(), SIZE_TVEPISODENUMBER));
						ps.setString(++databaseColumnIterator, left(media.getTVEpisodeName(), SIZE_MAX));
						ps.setBoolean(++databaseColumnIterator, media.isTVEpisode());
						ps.setString(++databaseColumnIterator, left(media.getExtraInformation(), SIZE_MAX));
					} else {
						ps.setString(++databaseColumnIterator, null);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setLong(++databaseColumnIterator, 0);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setByte(++databaseColumnIterator, (byte) -1);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.OTHER);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.OTHER);
						ps.setNull(++databaseColumnIterator, Types.OTHER);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setBoolean(++databaseColumnIterator, false);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
					}
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						if (rs.next()) {
							fileId = rs.getLong(1);
						}
					}
				} finally {
					TABLE_LOCK.writeLock().unlock();
				}
			}

			if (media != null && fileId > -1) {
				TableAudiotracks.insertOrUpdateAudioTracks(connection, fileId, media);
				TableSubtracks.insertOrUpdateSubtitleTracks(connection, fileId, media);
			}

			connection.commit();
		} catch (SQLException se) {
			if (se.getErrorCode() == 23505) {
				throw new SQLException(String.format(
					"Duplicate key while adding \"%s\" to the cache: %s",
					name,
					se.getMessage()
				), se);
			}
			throw se;
		} finally {
			if (media != null && media.getThumb() != null) {
				TableThumbnails.setThumbnail(media.getThumb(), name, -1);
			}
		}
	}

	/**
	 * Updates the name of a movie or TV series for existing entries in the database.
	 *
	 * @param oldName the existing movie or show name.
	 * @param newName the new movie or show name.
	 */
	public static void updateMovieOrShowName(String oldName, String newName) {
		try {
			updateRowsInFilesTable(oldName, newName, "MOVIEORSHOWNAME", SIZE_MAX, true);
		} catch (SQLException e) {
			LOGGER.error(
				"Failed to update MOVIEORSHOWNAME from \"{}\" to \"{}\": {}",
				oldName,
				newName,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Updates an existing row with information either extracted from the filename
	 * or from our API.
	 *
	 * @param path the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @param media the {@link DLNAMediaInfo} row to update.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertVideoMetadata(String path, long modified, DLNAMediaInfo media) throws SQLException {
		if (StringUtils.isBlank(path)) {
			LOGGER.warn("Couldn't write metadata for \"{}\" to the database because the media cannot be identified", path);
			return;
		}
		if (media == null) {
			LOGGER.warn("Couldn't write metadata for \"{}\" to the database because there is no media information", path);
			return;
		}

		try (Connection connection = DATABASE.getConnection()) {
			connection.setAutoCommit(false);
			TABLE_LOCK.writeLock().lock();
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"ID, IMDBID, MEDIA_YEAR, MOVIEORSHOWNAME, MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION, VERSION " +
				"FROM " + TABLE_NAME + " " +
				"WHERE " +
					"FILENAME = ? AND MODIFIED = ? " +
				"LIMIT 1",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, path);
				ps.setTimestamp(2, new Timestamp(modified));
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						rs.updateString("IMDBID", left(media.getIMDbID(), SIZE_IMDBID));
						rs.updateString("MEDIA_YEAR", left(media.getYear(), SIZE_YEAR));
						rs.updateString("MOVIEORSHOWNAME", left(media.getMovieOrShowName(), SIZE_MAX));
						rs.updateString("MOVIEORSHOWNAMESIMPLE", left(media.getSimplifiedMovieOrShowName(), SIZE_MAX));
						rs.updateString("TVSEASON", left(media.getTVSeason(), SIZE_TVSEASON));
						rs.updateString("TVEPISODENUMBER", left(media.getTVEpisodeNumber(), SIZE_TVEPISODENUMBER));
						rs.updateString("TVEPISODENAME", left(media.getTVEpisodeName(), SIZE_MAX));
						rs.updateBoolean("ISTVEPISODE", media.isTVEpisode());
						rs.updateString("EXTRAINFORMATION", left(media.getExtraInformation(), SIZE_MAX));
						rs.updateString("VERSION", left(APIUtils.getApiDataVideoVersion(), SIZE_MAX));
						rs.updateRow();
					} else {
						LOGGER.trace("Couldn't find \"{}\" in the database when trying to store metadata", path);
						return;
					}
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
			connection.commit();
		}
	}

	/**
	 * Updates a row or rows in the FILES table.
	 *
	 * @param oldValue the value to match, can be {@code null}.
	 * @param newValue the value to store, can be {@code null}.
	 * @param columnName the column to update.
	 * @param size the maximum size of the data if {@code isString} is
	 *            {@code true}.
	 * @param isString whether or not the value is a SQL char/string and should
	 *            be quoted and length limited.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void updateRowsInFilesTable(String oldValue, String newValue, String columnName, int size, boolean isString) throws SQLException {
		if (isString && size < 1) {
			throw new IllegalArgumentException("size must be positive");
		}
		if (StringUtils.isEmpty(columnName)) {
			LOGGER.error("Couldn't update rows in " + TABLE_NAME + " table because columnName ({}) is blank", columnName);
			return;
		}

		// Sanitize values
		oldValue = isString ? sqlQuote(oldValue) : sqlEscape(oldValue);
		newValue = isString ? sqlQuote(newValue) : sqlEscape(newValue);

		LOGGER.trace(
			"Updating rows in " + TABLE_NAME + " table from \"{}\" to \"{}\" in column {}",
			oldValue,
			newValue,
			columnName
		);
		try (Connection connection = DATABASE.getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows = statement.executeUpdate(
					"UPDATE " + TABLE_NAME + " SET " +
						columnName +  " = " + (newValue == null ? "NULL" : (isString ? left(newValue, size) : newValue)) +
					" WHERE " +
						columnName + (oldValue == null ? " IS NULL" : " = " + (isString ? left(oldValue, size) : oldValue))
				);
				LOGGER.trace("Updated {} rows in " + TABLE_NAME + " table", rows);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		}
	}

	/**
	 * Removes a single media file from the database.
	 *
	 * @param pathToFile the full path to the file to remove.
	 */
	public static void removeMediaEntry(String pathToFile) {
		try {
			removeMedia(pathToFile, false);
		} catch (SQLException e) {
			LOGGER.error(
				"An error occurred while trying to remove \"{}\" from the database: {}",
				pathToFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes all media files in a folder from the database.
	 *
	 * @param pathToFolder the full path to the folder whose children should be
	 *            removed.
	 */
	public static void removeMediaEntriesInFolder(String pathToFolder) {
		try {
			removeMedia(sqlLikeEscape(pathToFolder) + "%", true);
		} catch (SQLException e) {
			LOGGER.error(
				"An error occurred while trying to remove files matching \"{}\" from the database: {}",
				pathToFolder,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes row(s) in our other tables representing matching media. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @see TableTables#sqlLikeEscape(String)
	 *
	 * @param filename the filename(s) to remove.
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void removeMedia(String filename, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(filename)) {
			return;
		}

		deleteRowsInFilesTable(filename, useLike);
		TableFilesStatus.remove(filename, useLike);
		TableVideoMetadataActors.remove(filename, useLike);
		TableVideoMetadataAwards.remove(filename, useLike);
		TableVideoMetadataCountries.remove(filename, useLike);
		TableVideoMetadataDirectors.remove(filename, useLike);
		TableVideoMetadataGenres.remove(filename, useLike);
		TableVideoMetadataPosters.remove(filename, useLike);
		TableVideoMetadataProduction.remove(filename, useLike);
		TableVideoMetadataRated.remove(filename, useLike);
		TableVideoMetadataRatings.remove(filename, useLike);
		TableVideoMetadataReleased.remove(filename, useLike);
	}

	/**
	 * Deletes a row or rows in the FILES table. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @see TableTables#sqlLikeEscape(String)
	 *
	 * @param filename the filename to delete
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void deleteRowsInFilesTable(String filename, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(filename)) {
			return;
		}

		LOGGER.trace("Deleting rows from " + TABLE_NAME + " table where the filename is \"{}\"", filename);
		try (Connection connection = DATABASE.getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows;
				if (useLike) {
					rows = statement.executeUpdate("DELETE FROM " + TABLE_NAME + " WHERE FILENAME LIKE " + sqlQuote(filename));
				} else {
					rows = statement.executeUpdate("DELETE FROM " + TABLE_NAME + " WHERE FILENAME = " + sqlQuote(filename));
				}
				LOGGER.trace("Deleted {} rows from " + TABLE_NAME, rows);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		}
	}

	/**
	 * Deletes a row or rows in the given {@code tableName} for the given {@code condition}. If {@code useLike} is
	 * {@code true}, the {@code condition} must be properly escaped.
	 *
	 * @see TableTables#sqlLikeEscape(String)
	 *
	 * @param tableName the table name in which a row or rows will be deleted
	 * @param column the column where the {@code condition} will be queried
	 * @param condition the condition for which rows will be deleted
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void deleteRowsInTable(String tableName, String column, String condition, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(condition)) {
			return;
		}

		LOGGER.trace("Deleting rows from \"{}\" table for given column \"{}\" and condition \"{}\"", tableName, column, condition);
		try (Connection connection = DATABASE.getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows;
				if (useLike) {
					rows = statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + column + " LIKE " + sqlQuote(condition));
				} else {
					rows = statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + column + " = " + sqlQuote(condition));
				}
				LOGGER.trace("Deleted {} rows from " + tableName, rows);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		}
	}

	public static void updateThumbnailId(String fullPathToFile, int thumbId) {
		try (Connection conn = DATABASE.getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (
				PreparedStatement ps = conn.prepareStatement(
					"UPDATE " + TABLE_NAME + " SET THUMBID = ? WHERE FILENAME = ?"
				);
			) {
				ps.setInt(1, thumbId);
				ps.setString(2, fullPathToFile);
				ps.executeUpdate();
				LOGGER.trace("THUMBID updated to {} for {}", thumbId, fullPathToFile);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException se) {
			LOGGER.error("Error updating cached thumbnail for \"{}\": {}", se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static ArrayList<String> getStrings(String sql) {
		ArrayList<String> list = new ArrayList<>();
		HashSet<String> set = new HashSet<>();
		try (Connection connection = DATABASE.getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (
				PreparedStatement ps = connection.prepareStatement((sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with")) ? sql : ("SELECT FILENAME FROM " + TABLE_NAME + " WHERE " + sql));
				ResultSet rs = ps.executeQuery()
			) {
				while (rs.next()) {
					String str = rs.getString(1);
					if (isBlank(str)) {
						set.add(NONAME);
					} else {
						set.add(str);
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
			return null;
		}
		list.addAll(set);
		return list;
	}

	public static synchronized void cleanup() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DATABASE.getConnection();

			/*
			 * Cleanup of FILES table
			 *
			 * Removes entries that are not on the hard drive anymore, and
			 * ones that are no longer shared.
			 */
			ps = conn.prepareStatement("SELECT COUNT(*) FROM " + TABLE_NAME);
			rs = ps.executeQuery();
			int dbCount = 0;

			if (rs.next()) {
				dbCount = rs.getInt(1);
			}

			rs.close();
			ps.close();
			PMS.get().getFrame().setStatusLine(Messages.getString("DLNAMediaDatabase.2") + " 0%");
			int i = 0;
			int oldpercent = 0;

			if (dbCount > 0) {
				ps = conn.prepareStatement("SELECT FILENAME, MODIFIED, ID FROM " + TABLE_NAME, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				rs = ps.executeQuery();

				List<Path> sharedFolders = CONFIGURATION.getSharedFolders();
				boolean isFileStillShared = false;

				while (rs.next()) {
					String filename = rs.getString("FILENAME");
					long modified = rs.getTimestamp("MODIFIED").getTime();
					File file = new File(filename);
					if (!file.exists() || file.lastModified() != modified) {
						LOGGER.trace("Removing the file {} from our database because it is no longer on the hard drive", filename);
						rs.deleteRow();
					} else {
						// the file exists on the hard drive, but now check if we are still sharing it
						for (Path folder : sharedFolders) {
							if (filename.contains(folder.toString())) {
								isFileStillShared = true;
								break;
							}
						}

						if (!isFileStillShared) {
							LOGGER.trace("Removing the file {} from our database because it is no longer shared", filename);
							rs.deleteRow();
						}
					}

					i++;
					int newpercent = i * 100 / dbCount;
					if (newpercent > oldpercent) {
						PMS.get().getFrame().setStatusLine(Messages.getString("DLNAMediaDatabase.2") + newpercent + "%");
						oldpercent = newpercent;
					}
				}

				PMS.get().getFrame().setStatusLine(null);
			}

			/*
			 * Cleanup of THUMBNAILS table
			 *
			 * Removes entries that are not referenced by any rows in the FILES table.
			 */
			ps = conn.prepareStatement(
				"DELETE FROM " + TableThumbnails.TABLE_NAME + " " +
				"WHERE NOT EXISTS (" +
					"SELECT ID FROM " + TABLE_NAME + " " +
					"WHERE " + TABLE_NAME + ".THUMBID = " + TableThumbnails.TABLE_NAME + ".ID " +
					"LIMIT 1" +
				") AND NOT EXISTS (" +
					"SELECT ID FROM " + TableTVSeries.TABLE_NAME + " " +
					"WHERE " + TableTVSeries.TABLE_NAME + ".THUMBID = " + TableThumbnails.TABLE_NAME + ".ID " +
					"LIMIT 1" +
				");"
			);
			ps.execute();

			/*
			 * Cleanup of FILES_STATUS table
			 *
			 * Removes entries that are not referenced by any rows in the FILES table.
			 */
			ps = conn.prepareStatement(
				"DELETE FROM " + TableFilesStatus.TABLE_NAME + " " +
				"WHERE NOT EXISTS (" +
					"SELECT ID FROM " + TABLE_NAME + " " +
					"WHERE " + TABLE_NAME + ".FILENAME = FILES_STATUS.FILENAME" +
				");"
			);
			ps.execute();

			/*
			 * Cleanup of TV_SERIES table
			 *
			 * Removes entries that are not referenced by any rows in the FILES table.
			 */
			ps = conn.prepareStatement(
				"DELETE FROM " + TableTVSeries.TABLE_NAME + " " +
				"WHERE NOT EXISTS (" +
					"SELECT MOVIEORSHOWNAMESIMPLE FROM " + TABLE_NAME + " " +
					"WHERE " + TABLE_NAME + ".MOVIEORSHOWNAMESIMPLE = " + TableTVSeries.TABLE_NAME + ".SIMPLIFIEDTITLE " +
					"LIMIT 1" +
				");"
			);
			ps.execute();

			/*
			 * Cleanup of metadata tables
			 *
			 * Now that the TV_SERIES table is clean, remove metadata
			 * that does not correspond to any TV series or files
			 */
			String[] metadataTables = {
				TableVideoMetadataActors.TABLE_NAME,
				TableVideoMetadataAwards.TABLE_NAME,
				TableVideoMetadataCountries.TABLE_NAME,
				TableVideoMetadataDirectors.TABLE_NAME,
				TableVideoMetadataIMDbRating.TABLE_NAME,
				TableVideoMetadataGenres.TABLE_NAME,
				TableVideoMetadataPosters.TABLE_NAME,
				TableVideoMetadataProduction.TABLE_NAME,
				TableVideoMetadataRated.TABLE_NAME,
				TableVideoMetadataRatings.TABLE_NAME,
				TableVideoMetadataReleased.TABLE_NAME
			};
			for (String table : metadataTables) {
				ps = conn.prepareStatement(
					"DELETE FROM " + table + " " +
					"WHERE NOT EXISTS (" +
						"SELECT FILENAME FROM " + TABLE_NAME + " " +
						"WHERE " + TABLE_NAME + ".FILENAME = " + table + ".FILENAME " +
						"LIMIT 1" +
					") AND NOT EXISTS (" +
						"SELECT ID FROM " + TableTVSeries.TABLE_NAME + " " +
						"WHERE " + TableTVSeries.TABLE_NAME + ".ID = " + table + ".TVSERIESID " +
						"LIMIT 1" +
					");"
				);
				ps.execute();
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
		} finally {
			close(rs);
			close(ps);
			close(conn);
			PMS.get().getFrame().setStatusLine(null);
		}
	}

	public static ArrayList<File> getFiles(String sql) {
		ArrayList<File> list = new ArrayList<>();
		try (Connection connection = DATABASE.getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (
				PreparedStatement ps = connection.prepareStatement(
					sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with") ? sql : ("SELECT FILENAME, MODIFIED FROM " + TABLE_NAME + " WHERE " + sql)
				);
				ResultSet rs = ps.executeQuery();
			) {
				while (rs.next()) {
					String filename = rs.getString("FILENAME");
					long modified = rs.getTimestamp("MODIFIED").getTime();
					File file = new File(filename);
					if (file.exists() && file.lastModified() == modified) {
						list.add(file);
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
			return null;
		}
		return list;
	}

	/**
	 * @param filename
	 * @return all data across all tables for a video file, if it has an IMDb ID stored.
	 */
	public static List<HashMap<String, Object>> getAPIResultsByFilenameIncludingExternalTables(final String filename) {
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = DATABASE.getConnection()) {
			String query = "SELECT * " +
				"FROM " + TABLE_NAME + " " +
				"LEFT JOIN " + TableVideoMetadataActors.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataActors.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataAwards.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataAwards.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataCountries.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataCountries.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataDirectors.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataDirectors.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataGenres.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataGenres.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataProduction.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataProduction.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataPosters.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataPosters.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataRated.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataRated.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataRatings.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataRatings.TABLE_NAME + ".FILENAME " +
				"LEFT JOIN " + TableVideoMetadataReleased.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataReleased.TABLE_NAME + ".FILENAME " +
				"WHERE " + TABLE_NAME + ".FILENAME = " + sqlQuote(filename) + " and IMDBID != ''";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					return convertResultSetToList(resultSet);
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", filename, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * @param title
	 * @return a thumbnail based on title.
	 */
	public static DLNAThumbnail getThumbnailByTitle(final String title) {
		boolean trace = LOGGER.isTraceEnabled();

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);

		try (Connection connection = PMS.get().getDatabase().getConnection()) {
			String query = "SELECT THUMBNAIL " +
				"FROM " + TABLE_NAME + " " +
				"LEFT JOIN " + TableThumbnails.TABLE_NAME + " ON " + TABLE_NAME + ".THUMBID = " + TableThumbnails.TABLE_NAME + ".ID " +
				"WHERE MOVIEORSHOWNAMESIMPLE = " + sqlQuote(simplifiedTitle) + " LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					LOGGER.info("executed query for " + title);
					if (resultSet.next()) {
						LOGGER.info("got result for " + title);
						return (DLNAThumbnail) resultSet.getObject("THUMBNAIL");
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", title, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}
}
