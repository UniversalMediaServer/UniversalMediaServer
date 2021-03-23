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
package net.pms.dlna;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.database.TableFilesStatus;
import net.pms.database.TableThumbnails;
import net.pms.database.Tables;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImageInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import static net.pms.database.Tables.sqlLikeEscape;
import static org.apache.commons.lang3.StringUtils.*;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.CharMatcher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import net.pms.database.TableVideoMetadataActors;
import net.pms.database.TableVideoMetadataAwards;
import net.pms.database.TableVideoMetadataCountries;
import net.pms.database.TableVideoMetadataDirectors;
import net.pms.database.TableVideoMetadataGenres;
import net.pms.database.TableVideoMetadataPosters;
import net.pms.database.TableVideoMetadataProduction;
import net.pms.database.TableVideoMetadataRated;
import net.pms.database.TableVideoMetadataRatings;
import net.pms.database.TableVideoMetadataReleased;
import static net.pms.database.Tables.convertResultSetToList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.database.TableTVSeries;
import net.pms.database.TableVideoMetadataIMDbRating;
import static net.pms.database.Tables.sqlQuote;
import net.pms.newgui.SharedContentTab;
import net.pms.util.FileUtil;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class DLNAMediaDatabase implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaDatabase.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock(true);

	private String url;
	private String dbDir;
	private String dbName;
	public static final String NONAME = "###";
	private Thread scanner;
	private final JdbcConnectionPool cp;
	private int dbCount;

	public static final String TABLE_NAME = "FILES";

	/**
	 * The database version should be incremented when we change anything to
	 * do with the database since the last released version.
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
	 */
	private static final int LATEST_VERSION = 23;

	// Database column sizes
	private static final int SIZE_CODECV = 32;
	private static final int SIZE_FRAMERATE = 32;
	private static final int SIZE_AVCLEVEL = 3;
	private static final int SIZE_CONTAINER = 32;
	private static final int SIZE_IMDBID = 16;
	private static final int SIZE_MATRIX_COEFFICIENTS = 16;
	private static final int SIZE_MUXINGMODE = 32;
	private static final int SIZE_FRAMERATEMODE = 16;
	private static final int SIZE_LANG = 3;
	private static final int SIZE_SAMPLEFREQ = 16;
	private static final int SIZE_CODECA = 32;
	private static final int SIZE_GENRE = 64;
	private static final int SIZE_YEAR = 4;
	private static final int SIZE_TVSEASON = 4;
	private static final int SIZE_TVEPISODENUMBER = 8;
	private static final int SIZE_EXTERNALFILE = 1000;

	// Generic constant for the maximum string size: 255 chars
	private static final int SIZE_MAX = 255;

	/**
	 * Initializes the database connection pool for the current profile.
	 *
	 * Will create the "UMS-tests" profile directory and put the database
	 * in there if it doesn't exist, in order to prevent overwriting
	 * real databases.
	 *
	 * @param name the database name
	 */
	public DLNAMediaDatabase(String name) {
		dbName = name;
		File profileDirectory = new File(CONFIGURATION.getProfileDirectory());
		dbDir = new File(PMS.isRunningTests() || profileDirectory.isDirectory() ? CONFIGURATION.getProfileDirectory() : null, "database").getAbsolutePath();
		boolean logDB = CONFIGURATION.getDatabaseLogging();
		url = Constants.START_URL + dbDir + File.separator + dbName + (logDB ? ";TRACE_LEVEL_FILE=3" : "");
		LOGGER.debug("Using database URL: {}", url);
		LOGGER.info("Using database located at: \"{}\"", dbDir);
		if (logDB) {
			LOGGER.info("Database logging is enabled");
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Database logging is disabled");
		}

		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			LOGGER.error(null, e);
		}

		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL(url);
		ds.setUser("sa");
		ds.setPassword("");
		cp = JdbcConnectionPool.create(ds);
	}

	/**
	 * Gets the name of the database file
	 *
	 * @return The filename
	 */
	public String getDatabaseFilename() {
		if (dbName == null || dbDir == null) {
			return null;
		}
		return dbDir + File.separator + dbName;
	}

	/**
	 * Gets the database path
	 *
	 * @return The database path
	 */
	public String getDatabasePath() {
		if (dbDir == null) {
			return null;
		}

		return dbDir;
	}

	/**
	 * Gets a new connection from the connection pool if one is available. If
	 * not waits for a free slot until timeout.<br>
	 * <br>
	 * <strong>Important: Every connection must be closed after use</strong>
	 *
	 * @return the new connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		return cp.getConnection();
	}

	/**
	 * Initialized the database for use, performing checks and creating a new
	 * database if necessary.
	 *
	 * @param force whether to recreate the database regardless of necessity.
	 */
	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
	public synchronized void init(boolean force) {
		dbCount = -1;
		int currentVersion = -1;
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;

		try {
			conn = getConnection();
		} catch (SQLException se) {
			final File dbFile = new File(dbDir + File.separator + dbName + ".data.db");
			final File dbDirectory = new File(dbDir);
			if (dbFile.exists() || (se.getErrorCode() == 90048)) { // Cache is corrupt or a wrong version, so delete it
				FileUtils.deleteQuietly(dbDirectory);
				if (!dbDirectory.exists()) {
					LOGGER.info("The database has been deleted because it was corrupt or had the wrong version");
				} else {
					if (!net.pms.PMS.isHeadless()) {
						JOptionPane.showMessageDialog(
							SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
							String.format(Messages.getString("DLNAMediaDatabase.5"), dbDir),
							Messages.getString("Dialog.Error"),
							JOptionPane.ERROR_MESSAGE
						);
					}
					LOGGER.error("Damaged cache can't be deleted. Stop the program and delete the folder \"" + dbDir + "\" manually");
					PMS.get().getRootFolder(null).stopScan();
					CONFIGURATION.setUseCache(false);
					return;
				}
			} else {
				LOGGER.debug("Database connection error, retrying in 10 seconds");

				try {
					Thread.sleep(10000);
					conn = getConnection();
				} catch (InterruptedException | SQLException se2) {
					if (!net.pms.PMS.isHeadless()) {
						try {
							JOptionPane.showMessageDialog(
								SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
								String.format(Messages.getString("DLNAMediaDatabase.ConnectionError"), dbDir),
								Messages.getString("Dialog.Error"),
								JOptionPane.ERROR_MESSAGE
							);
						} catch (NullPointerException e) {
							LOGGER.debug("Failed to show database connection error message, probably because GUI is not initialized yet. Error was {}", e);
						}
					}
					LOGGER.debug("", se2);
					RootFolder rootFolder = PMS.get().getRootFolder(null);
					if (rootFolder != null) {
						rootFolder.stopScan();
					}
					return;
				}
			}
		} finally {
			close(conn);
		}

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT count(*) FROM FILES");
			if (rs.next()) {
				dbCount = rs.getInt(1);
			}
			rs.close();
			stmt.close();

			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT VALUE FROM METADATA WHERE KEY = 'VERSION'");
			if (rs.next()) {
				currentVersion = Integer.parseInt(rs.getString(1));
			}
		} catch (SQLException se) {
			if (se.getErrorCode() != 42102) { // Don't log exception "Table "FILES" not found" which will be corrected in following step
				LOGGER.error(null, se);
			}
		} finally {
			close(rs);
			close(stmt);
			close(conn);
		}

		/**
		 * In here we could update tables with our changes, but our database
		 * gets bloated often which causes the update to fail, so do the
		 * dumb way for now until we have better ways to keep the database
		 * smaller.
		 */
		if (currentVersion != -1 && LATEST_VERSION != currentVersion) {
			force = true;
		}

		if (force || dbCount == -1) {
			LOGGER.debug("Database will be (re)initialized");
			try {
				conn = getConnection();
				LOGGER.trace("DROPPING TABLE FILES");
				executeUpdate(conn, "DROP TABLE FILES");
				LOGGER.trace("DROPPING TABLE METADATA");
				executeUpdate(conn, "DROP TABLE METADATA");
				LOGGER.trace("DROPPING TABLE REGEXP_RULES");
				executeUpdate(conn, "DROP TABLE REGEXP_RULES");
				LOGGER.trace("DROPPING TABLE SUBTRACKS");
				executeUpdate(conn, "DROP TABLE SUBTRACKS");
			} catch (SQLException se) {
				if (se.getErrorCode() != 42102) { // Don't log exception "Table "FILES" not found" which will be corrected in following step
					LOGGER.error("SQL error while dropping tables: {}", se.getMessage());
					LOGGER.trace("", se);
				}
			}
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE FILES (");
				sb.append("  ID                      INT AUTO_INCREMENT");
				sb.append(", THUMBID                 BIGINT");
				sb.append(", FILENAME                VARCHAR2(1024)   NOT NULL");
				sb.append(", MODIFIED                TIMESTAMP        NOT NULL");
				sb.append(", TYPE                    INT");
				sb.append(", DURATION                DOUBLE");
				sb.append(", BITRATE                 INT");
				sb.append(", WIDTH                   INT");
				sb.append(", HEIGHT                  INT");
				sb.append(", SIZE                    NUMERIC");
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
				sb.append(", YEAR                    VARCHAR2(").append(SIZE_YEAR).append(')');
				sb.append(", MOVIEORSHOWNAME         VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", MOVIEORSHOWNAMESIMPLE   VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", TVSEASON                VARCHAR2(").append(SIZE_TVSEASON).append(')');
				sb.append(", TVEPISODENUMBER         VARCHAR2(").append(SIZE_TVEPISODENUMBER).append(')');
				sb.append(", TVEPISODENAME           VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", ISTVEPISODE             BOOLEAN");
				sb.append(", EXTRAINFORMATION        VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(")");
				LOGGER.trace("Creating table FILES with:\n\n{}\n", sb.toString());
				executeUpdate(conn, sb.toString());
				sb = new StringBuilder();
				sb.append("CREATE TABLE SUBTRACKS (");
				sb.append("  ID       INT              NOT NULL");
				sb.append(", FILEID   BIGINT           NOT NULL");
				sb.append(", LANG     VARCHAR2(").append(SIZE_LANG).append(')');
				sb.append(", TITLE    VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", TYPE     INT");
				sb.append(", EXTERNALFILE VARCHAR2(").append(SIZE_EXTERNALFILE).append(") NOT NULL default ''");
				sb.append(", CHARSET VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", constraint PKSUB primary key (FILEID, ID, EXTERNALFILE)");
				sb.append(", FOREIGN KEY(FILEID)");
				sb.append("    REFERENCES FILES(ID)");
				sb.append("    ON DELETE CASCADE");
				sb.append(')');
				LOGGER.trace("Creating table SUBTRACKS with:\n\n{}\n", sb.toString());
				executeUpdate(conn, sb.toString());

				LOGGER.trace("Creating table METADATA");
				executeUpdate(conn, "CREATE TABLE METADATA (KEY VARCHAR2(255) NOT NULL, VALUE VARCHAR2(255) NOT NULL)");
				executeUpdate(conn, "INSERT INTO METADATA VALUES ('VERSION', '" + LATEST_VERSION + "')");

				LOGGER.trace("Creating index IDX_FILE");
				executeUpdate(conn, "CREATE UNIQUE INDEX IDX_FILE ON FILES(FILENAME, MODIFIED)");

				LOGGER.trace("Creating index IDX_FILENAME_MODIFIED_IMDBID");
				executeUpdate(conn, "CREATE INDEX IDX_FILENAME_MODIFIED_IMDBID ON FILES(FILENAME, MODIFIED, IMDBID)");

				LOGGER.trace("Creating index TYPE");
				executeUpdate(conn, "CREATE INDEX TYPE on FILES (TYPE)");

				LOGGER.trace("Creating index TYPE_ISTV");
				executeUpdate(conn, "CREATE INDEX TYPE_ISTV on FILES (TYPE, ISTVEPISODE)");

				LOGGER.trace("Creating index TYPE_ISTV_SIMPLENAME");
				executeUpdate(conn, "CREATE INDEX TYPE_ISTV_SIMPLENAME on FILES (TYPE, ISTVEPISODE, MOVIEORSHOWNAMESIMPLE)");

				LOGGER.trace("Creating index TYPE_ISTV_NAME");
				executeUpdate(conn, "CREATE INDEX TYPE_ISTV_NAME on FILES (TYPE, ISTVEPISODE, MOVIEORSHOWNAME)");

				LOGGER.trace("Creating index TYPE_ISTV_NAME_SEASON");
				executeUpdate(conn, "CREATE INDEX TYPE_ISTV_NAME_SEASON on FILES (TYPE, ISTVEPISODE, MOVIEORSHOWNAME, TVSEASON)");

				LOGGER.trace("Creating index TYPE_ISTV_YEAR_STEREOSCOPY");
				executeUpdate(conn, "CREATE INDEX TYPE_ISTV_YEAR_STEREOSCOPY on FILES (TYPE, ISTVEPISODE, YEAR, STEREOSCOPY)");

				LOGGER.trace("Creating index TYPE_WIDTH_HEIGHT");
				executeUpdate(conn, "CREATE INDEX TYPE_WIDTH_HEIGHT on FILES (TYPE, WIDTH, HEIGHT)");

				LOGGER.trace("Creating index TYPE_MODIFIED");
				executeUpdate(conn, "CREATE INDEX TYPE_MODIFIED on FILES (TYPE, MODIFIED)");

				LOGGER.trace("Creating table REGEXP_RULES");
				executeUpdate(conn, "CREATE TABLE REGEXP_RULES ( ID VARCHAR2(255) PRIMARY KEY, RULE VARCHAR2(255), ORDR NUMERIC);");
				executeUpdate(conn, "INSERT INTO REGEXP_RULES VALUES ( '###', '(?i)^\\W.+', 0 );");
				executeUpdate(conn, "INSERT INTO REGEXP_RULES VALUES ( '0-9', '(?i)^\\d.+', 1 );");

				// Retrieve the alphabet property value and split it
				String[] chars = Messages.getString("DLNAMediaDatabase.1").split(",");

				for (int i = 0; i < chars.length; i++) {
					// Create regexp rules for characters with a sort order based on the property value
					executeUpdate(conn, "INSERT INTO REGEXP_RULES VALUES ( '" + chars[i] + "', '(?i)^" + chars[i] + ".+', " + (i + 2) + " );");
				}

				LOGGER.debug("Database initialized");
			} catch (SQLException se) {
				LOGGER.error("Error creating tables: " + se.getMessage());
				LOGGER.trace("", se);
			} finally {
				close(conn);
			}
		} else {
			LOGGER.debug("Database file count: " + dbCount);
			LOGGER.debug("Database version: " + LATEST_VERSION);
		}
	}

	private static void executeUpdate(Connection conn, String sql) throws SQLException {
		if (conn != null) {
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(sql);
			}
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
	public boolean isDataExists(String name, long modified) {
		boolean found = false;
		try (Connection connection = getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM FILES WHERE FILENAME = ? AND MODIFIED = ? LIMIT 1")) {
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
	 * Checks whether data from our API has been written to the database
	 * for this video.
	 *
	 * @param name the full path of the video.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return whether API metadata exists for this video.
	 */
	public boolean isAPIMetadataExists(String name, long modified) {
		boolean found = false;
		try (Connection connection = getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM FILES WHERE FILENAME = ? AND MODIFIED = ? AND IMDBID IS NOT NULL LIMIT 1")) {
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
			LOGGER.error(
				"An SQL error occurred when trying to check if OpenSubtitles metadata exists for \"{}\": {}",
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
	 * as a {@link DLNAMediaInfo} instance.
	 *
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return The {@link DLNAMediaInfo} instance matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public DLNAMediaInfo getData(String name, long modified) throws IOException, SQLException {
		DLNAMediaInfo media = null;
		ArrayList<String> externalFileReferencesToRemove = new ArrayList();
		try (Connection conn = getConnection()) {
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
					PreparedStatement audios = conn.prepareStatement("SELECT * FROM AUDIOTRACKS WHERE FILEID = ?");
					PreparedStatement subs = conn.prepareStatement("SELECT * FROM SUBTRACKS WHERE FILEID = ?");
					PreparedStatement status = conn.prepareStatement("SELECT * FROM " + TableFilesStatus.TABLE_NAME + " WHERE FILENAME = ? LIMIT 1");
				) {
					if (rs.next()) {
						media = new DLNAMediaInfo();
						int id = rs.getInt("ID");
						media.setDuration(toDouble(rs, "DURATION"));
						media.setBitrate(rs.getInt("BITRATE"));
						media.setWidth(rs.getInt("WIDTH"));
						media.setHeight(rs.getInt("HEIGHT"));
						media.setSize(rs.getLong("SIZE"));
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
						media.setYear(rs.getString("YEAR"));
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
								audio.setYear(elements.getInt("YEAR"));
								audio.setTrack(elements.getInt("TRACK"));
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
								sub.setType(SubtitleType.valueOfStableIndex(elements.getInt("TYPE")));
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
						deleteRowsInTable("SUBTRACKS", "EXTERNALFILE", externalFileReferenceToRemove, false);
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

	private static Double toDouble(ResultSet rs, String column) throws SQLException {
		Object obj = rs.getObject(column);
		if (obj instanceof Double) {
			return (Double) obj;
		}
		return null;
	}

	private void insertOrUpdateSubtitleTracks(Connection connection, long fileId, DLNAMediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getSubTrackCount() < 1) {
			return;
		}

		String columns = "FILEID, ID, LANG, TITLE, TYPE, EXTERNALFILE, CHARSET ";

		TABLE_LOCK.writeLock().lock();
		try (
			PreparedStatement updateStatement = connection.prepareStatement(
				"SELECT " +
					"FILEID, ID, LANG, TITLE, TYPE, EXTERNALFILE, CHARSET " +
				"FROM SUBTRACKS " +
				"WHERE " +
					"FILEID = ? AND ID = ? AND EXTERNALFILE = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO SUBTRACKS (" + columns +	")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			for (DLNAMediaSubtitle subtitleTrack : media.getSubtitlesTracks()) {
				updateStatement.setLong(1, fileId);
				updateStatement.setInt(2, subtitleTrack.getId());
				if (subtitleTrack.getExternalFile() != null) {
					updateStatement.setString(3, subtitleTrack.getExternalFile().getPath());
				} else {
					updateStatement.setString(3, "");
				}
				try (ResultSet rs = updateStatement.executeQuery()) {
					if (rs.next()) {
						rs.updateString("LANG", left(subtitleTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						rs.updateInt("TYPE", subtitleTrack.getType().getStableIndex());
						if (subtitleTrack.getExternalFile() != null) {
							rs.updateString("EXTERNALFILE", left(subtitleTrack.getExternalFile().getPath(), SIZE_EXTERNALFILE));
						} else {
							rs.updateString("EXTERNALFILE", "");
						}
						rs.updateString("CHARSET", left(subtitleTrack.getSubCharacterSet(), SIZE_MAX));
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setLong(1, fileId);
						insertStatement.setInt(2, subtitleTrack.getId());
						insertStatement.setString(3, left(subtitleTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						insertStatement.setInt(5, subtitleTrack.getType().getStableIndex());
						if (subtitleTrack.getExternalFile() != null) {
							insertStatement.setString(6, left(subtitleTrack.getExternalFile().getPath(), SIZE_EXTERNALFILE));
						} else {
							insertStatement.setString(6, "");
						}
						insertStatement.setString(7, left(subtitleTrack.getSubCharacterSet(), SIZE_MAX));
						insertStatement.executeUpdate();
					}
				}
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	private void insertOrUpdateAudioTracks(Connection connection, long fileId, DLNAMediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getAudioTrackCount() < 1) {
			return;
		}

		String columns = "FILEID, ID, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, BITSPERSAMPLE, " +
			"ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, YEAR, TRACK, DELAY, MUXINGMODE, BITRATE, MBID_RECORD, MBID_TRACK";

		TABLE_LOCK.writeLock().lock();
		try (
			PreparedStatement updateStatment = connection.prepareStatement(
				"SELECT " +
					"FILEID, ID, MBID_RECORD, MBID_TRACK, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, " +
					"BITSPERSAMPLE, ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, YEAR, TRACK, " +
					"DELAY, MUXINGMODE, BITRATE " +
				"FROM AUDIOTRACKS " +
				"WHERE " +
					"FILEID = ? AND ID = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO AUDIOTRACKS (" + columns + ")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			for (DLNAMediaAudio audioTrack : media.getAudioTracksList()) {
				updateStatment.setLong(1, fileId);
				updateStatment.setInt(2, audioTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						rs.updateString("MBID_RECORD", left(trimToEmpty(audioTrack.getMbidRecord()), SIZE_MAX));
						rs.updateString("MBID_TRACK", left(trimToEmpty(audioTrack.getMbidTrack()), SIZE_MAX));
						rs.updateString("LANG", left(audioTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_MAX));
						rs.updateInt("NRAUDIOCHANNELS", audioTrack.getAudioProperties().getNumberOfChannels());
						rs.updateString("SAMPLEFREQ", left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
						rs.updateString("CODECA", left(audioTrack.getCodecA(), SIZE_CODECA));
						rs.updateInt("BITSPERSAMPLE", audioTrack.getBitsperSample());
						rs.updateString("ALBUM", left(trimToEmpty(audioTrack.getAlbum()), SIZE_MAX));
						rs.updateString("ARTIST", left(trimToEmpty(audioTrack.getArtist()), SIZE_MAX));

						//Special case for album artist. If it's empty, we want to insert NULL (for quicker retrieval)
						String albumartist = left(trimToEmpty(audioTrack.getAlbumArtist()), SIZE_MAX);
						if (albumartist.isEmpty()) {
							rs.updateNull("ALBUMARTIST");
						} else {
							rs.updateString("ALBUMARTIST", albumartist);
						}

						rs.updateString("SONGNAME", left(trimToEmpty(audioTrack.getSongname()), SIZE_MAX));
						rs.updateString("GENRE", left(trimToEmpty(audioTrack.getGenre()), SIZE_GENRE));
						rs.updateInt("YEAR", audioTrack.getYear());
						rs.updateInt("TRACK", audioTrack.getTrack());
						rs.updateInt("DELAY", audioTrack.getAudioProperties().getAudioDelay());
						rs.updateString("MUXINGMODE", left(trimToEmpty(audioTrack.getMuxingModeAudio()), SIZE_MUXINGMODE));
						rs.updateInt("BITRATE", audioTrack.getBitRate());
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setLong(1, fileId);
						insertStatement.setInt(2, audioTrack.getId());
						insertStatement.setString(3, left(audioTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_MAX));
						insertStatement.setInt(5, audioTrack.getAudioProperties().getNumberOfChannels());
						insertStatement.setString(6, left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
						insertStatement.setString(7, left(audioTrack.getCodecA(), SIZE_CODECA));
						insertStatement.setInt(8, audioTrack.getBitsperSample());
						insertStatement.setString(9, left(trimToEmpty(audioTrack.getAlbum()), SIZE_MAX));
						insertStatement.setString(10, left(trimToEmpty(audioTrack.getArtist()), SIZE_MAX));

						//Special case for album artist. If it's empty, we want to insert NULL (for quicker retrieval)
						String albumartist = left(trimToEmpty(audioTrack.getAlbumArtist()), SIZE_MAX);
						if (albumartist.isEmpty()) {
							insertStatement.setNull(11, Types.VARCHAR);
						} else {
							insertStatement.setString(11, albumartist);
						}

						insertStatement.setString(12, left(trimToEmpty(audioTrack.getSongname()), SIZE_MAX));
						insertStatement.setString(13, left(trimToEmpty(audioTrack.getGenre()), SIZE_GENRE));
						insertStatement.setInt(14, audioTrack.getYear());
						insertStatement.setInt(15, audioTrack.getTrack());
						insertStatement.setInt(16, audioTrack.getAudioProperties().getAudioDelay());
						insertStatement.setString(17, left(trimToEmpty(audioTrack.getMuxingModeAudio()), SIZE_MUXINGMODE));
						insertStatement.setInt(18, audioTrack.getBitRate());
						insertStatement.setString(19, audioTrack.getMbidRecord());
						insertStatement.setString(20, audioTrack.getMbidTrack());
						insertStatement.executeUpdate();
					}
				}
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	protected void updateSerialized(ResultSet rs, Object x, String columnLabel) throws SQLException {
		if (x != null) {
			rs.updateObject(columnLabel, x);
		} else {
			rs.updateNull(columnLabel);
		}
	}

	protected void insertSerialized(PreparedStatement ps, Object x, int parameterIndex) throws SQLException {
		if (x != null) {
			ps.setObject(parameterIndex, x);
		} else {
			ps.setNull(parameterIndex, Types.OTHER);
		}
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
	public void insertOrUpdateData(String name, long modified, int type, DLNAMediaInfo media) throws SQLException {
		try (
			Connection connection = getConnection()
		) {
			connection.setAutoCommit(false);
			long fileId = -1;
			TABLE_LOCK.writeLock().lock();
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"ID, FILENAME, MODIFIED, TYPE, DURATION, BITRATE, WIDTH, HEIGHT, SIZE, CODECV, FRAMERATE, " +
					"ASPECTRATIODVD, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, " +
					"CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
					"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH, PIXELASPECTRATIO, SCANTYPE, SCANORDER, " +
					"IMDBID, YEAR, MOVIEORSHOWNAME, MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION " +
				"FROM FILES " +
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
						rs.updateInt("TYPE", type);
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
							rs.updateLong("SIZE", media.getSize());
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
							rs.updateString("YEAR", left(media.getYear(), SIZE_YEAR));
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
				String columns = "FILENAME, MODIFIED, TYPE, DURATION, BITRATE, WIDTH, HEIGHT, SIZE, CODECV, " +
					"FRAMERATE, ASPECTRATIODVD, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, " +
					"CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
					"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH, PIXELASPECTRATIO, SCANTYPE, SCANORDER, IMDBID, YEAR, MOVIEORSHOWNAME, " +
					"MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION";

				TABLE_LOCK.writeLock().lock();
				try (
					PreparedStatement ps = connection.prepareStatement(
						"INSERT INTO FILES (" + columns + ")" +
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
				insertOrUpdateAudioTracks(connection, fileId, media);
				insertOrUpdateSubtitleTracks(connection, fileId, media);
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
	 * Updates the name of a TV series for existing entries in the database.
	 *
	 * @param oldName the existing movie or show name.
	 * @param newName the new movie or show name.
	 */
	public void updateMovieOrShowName(String oldName, String newName) {
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
	public void insertVideoMetadata(String path, long modified, DLNAMediaInfo media) throws SQLException {
		if (StringUtils.isBlank(path)) {
			LOGGER.warn("Couldn't write metadata for \"{}\" to the database because the media cannot be identified", path);
			return;
		}
		if (media == null) {
			LOGGER.warn("Couldn't write metadata for \"{}\" to the database because there is no media information", path);
			return;
		}

		try (Connection connection = getConnection()) {
			connection.setAutoCommit(false);
			TABLE_LOCK.writeLock().lock();
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"ID, IMDBID, YEAR, MOVIEORSHOWNAME, MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION " +
				"FROM FILES " +
				"WHERE " +
					"FILENAME = ? AND MODIFIED = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, path);
				ps.setTimestamp(2, new Timestamp(modified));
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						rs.updateString("IMDBID", left(media.getIMDbID(), SIZE_IMDBID));
						rs.updateString("YEAR", left(media.getYear(), SIZE_YEAR));
						rs.updateString("MOVIEORSHOWNAME", left(media.getMovieOrShowName(), SIZE_MAX));
						rs.updateString("MOVIEORSHOWNAMESIMPLE", left(media.getSimplifiedMovieOrShowName(), SIZE_MAX));
						rs.updateString("TVSEASON", left(media.getTVSeason(), SIZE_TVSEASON));
						rs.updateString("TVEPISODENUMBER", left(media.getTVEpisodeNumber(), SIZE_TVEPISODENUMBER));
						rs.updateString("TVEPISODENAME", left(media.getTVEpisodeName(), SIZE_MAX));
						rs.updateBoolean("ISTVEPISODE", media.isTVEpisode());
						rs.updateString("EXTRAINFORMATION", left(media.getExtraInformation(), SIZE_MAX));
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
	public void updateRowsInFilesTable(String oldValue, String newValue, String columnName, int size, boolean isString) throws SQLException {
		if (isString && size < 1) {
			throw new IllegalArgumentException("size must be positive");
		}
		if (StringUtils.isEmpty(columnName)) {
			LOGGER.error("Couldn't update rows in FILES table because columnName ({}) is blank", columnName);
			return;
		}

		// Sanitize values
		oldValue = isString ? Tables.sqlQuote(oldValue) : Tables.sqlEscape(oldValue);
		newValue = isString ? Tables.sqlQuote(newValue) : Tables.sqlEscape(newValue);

		LOGGER.trace(
			"Updating rows in FILES table from \"{}\" to \"{}\" in column {}",
			oldValue,
			newValue,
			columnName
		);
		try (Connection connection = getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows = statement.executeUpdate(
					"UPDATE FILES SET " +
						columnName +  " = " + (newValue == null ? "NULL" : (isString ? left(newValue, size) : newValue)) +
					" WHERE " +
						columnName + (oldValue == null ? " IS NULL" : " = " + (isString ? left(oldValue, size) : oldValue))
				);
				LOGGER.trace("Updated {} rows in FILES table", rows);
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
	public void removeMediaEntry(String pathToFile) {
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
	public void removeMediaEntriesInFolder(String pathToFolder) {
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
	 * @see Tables#sqlLikeEscape(String)
	 *
	 * @param filename the filename(s) to remove.
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public void removeMedia(String filename, boolean useLike) throws SQLException {
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
	 * @see Tables#sqlLikeEscape(String)
	 *
	 * @param filename the filename to delete
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public void deleteRowsInFilesTable(String filename, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(filename)) {
			return;
		}

		LOGGER.trace("Deleting rows from FILES table where the filename is \"{}\"", filename);
		try (Connection connection = getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows;
				if (useLike) {
					rows = statement.executeUpdate("DELETE FROM FILES WHERE FILENAME LIKE " + Tables.sqlQuote(filename));
				} else {
					rows = statement.executeUpdate("DELETE FROM FILES WHERE FILENAME = " + Tables.sqlQuote(filename));
				}
				LOGGER.trace("Deleted {} rows from FILES", rows);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		}
	}

	/**
	 * Deletes a row or rows in the given {@code tableName} for the given {@code condition}. If {@code useLike} is
	 * {@code true}, the {@code condition} must be properly escaped.
	 *
	 * @see Tables#sqlLikeEscape(String)
	 *
	 * @param tableName the table name in which a row or rows will be deleted
	 * @param column the column where the {@code condition} will be queried
	 * @param condition the condition for which rows will be deleted
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public void deleteRowsInTable(String tableName, String column, String condition, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(condition)) {
			return;
		}

		LOGGER.trace("Deleting rows from \"{}\" table for given column \"{}\" and condition \"{}\"", tableName, column, condition);
		try (Connection connection = getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows;
				if (useLike) {
					rows = statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + column + " LIKE " + Tables.sqlQuote(condition));
				} else {
					rows = statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + column + " = " + Tables.sqlQuote(condition));
				}
				LOGGER.trace("Deleted {} rows from SUBTRACKS", rows);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		}
	}

	public void updateThumbnailId(String fullPathToFile, int thumbId) {
		try (Connection conn = getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (
				PreparedStatement ps = conn.prepareStatement(
					"UPDATE FILES SET THUMBID = ? WHERE FILENAME = ?"
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

	public ArrayList<String> getStrings(String sql) {
		ArrayList<String> list = new ArrayList<>();
		try (Connection connection = getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (
				PreparedStatement ps = connection.prepareStatement((sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with")) ? sql : ("SELECT FILENAME FROM FILES WHERE " + sql));
				ResultSet rs = ps.executeQuery()
			) {
				while (rs.next()) {
					String str = rs.getString(1);
					if (isBlank(str)) {
						if (!list.contains(NONAME)) {
							list.add(NONAME);
						}
					} else if (!list.contains(str)) {
						list.add(str);
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

	public synchronized void cleanup() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnection();

			/*
			 * Cleanup of FILES table
			 *
			 * Removes entries that are not on the hard drive anymore, and
			 * ones that are no longer shared.
			 */
			ps = conn.prepareStatement("SELECT COUNT(*) FROM FILES");
			rs = ps.executeQuery();
			dbCount = 0;

			if (rs.next()) {
				dbCount = rs.getInt(1);
			}

			rs.close();
			ps.close();
			PMS.get().getFrame().setStatusLine(Messages.getString("DLNAMediaDatabase.2") + " 0%");
			int i = 0;
			int oldpercent = 0;

			if (dbCount > 0) {
				ps = conn.prepareStatement("SELECT FILENAME, MODIFIED, ID FROM FILES", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
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
				"DELETE FROM FILES_STATUS " +
				"WHERE NOT EXISTS (" +
					"SELECT ID FROM FILES " +
					"WHERE FILES.FILENAME = FILES_STATUS.FILENAME" +
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
					"SELECT MOVIEORSHOWNAMESIMPLE FROM FILES " +
					"WHERE FILES.MOVIEORSHOWNAMESIMPLE = " + TableTVSeries.TABLE_NAME + ".SIMPLIFIEDTITLE " +
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

	public ArrayList<File> getFiles(String sql) {
		ArrayList<File> list = new ArrayList<>();
		try (Connection connection = getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (
				PreparedStatement ps = connection.prepareStatement(
					sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with") ? sql : ("SELECT FILENAME, MODIFIED FROM FILES WHERE " + sql)
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

	private static void close(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	private static void close(Statement ps) {
		try {
			if (ps != null) {
				ps.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	private static void close(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	public boolean isScanLibraryRunning() {
		return scanner != null && scanner.isAlive();
	}

	public void scanLibrary() {
		if (isScanLibraryRunning()) {
			LOGGER.info("Cannot start library scanner: A scan is already in progress");
		} else {
			scanner = new Thread(this, "Library Scanner");
			scanner.setPriority(Thread.MIN_PRIORITY);
			scanner.start();
			SharedContentTab.setScanLibraryBusy();
		}
	}

	public void stopScanLibrary() {
		if (isScanLibraryRunning()) {
			PMS.get().getRootFolder(null).stopScan();
		}
	}

	@Override
	public void run() {
		try {
			PMS.get().getRootFolder(null).scan();
		} catch (Exception e) {
			LOGGER.error("Unhandled exception during library scan: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Returns the VALUES {@link String} for the SQL request.
	 * It fills the {@link String} with {@code " VALUES (?,?,?, ...)"}.<p>
	 * The number of the "?" is calculated from the columns and not need to be hardcoded which
	 * often causes mistakes when columns are deleted or added.<p>
	 * Possible implementation:
	 * <blockquote><pre>
	 * String columns = "FILEID, ID, LANG, TITLE, NRAUDIOCHANNELS";
	 * PreparedStatement insertStatement = connection.prepareStatement(
	 *    "INSERT INTO AUDIOTRACKS (" + columns + ")" +
	 *    createDefaultValueForInsertStatement(columns)
	 * );
	 * </pre></blockquote><p
	 *
	 * @param columns the SQL parameters string
	 * @return The " VALUES (?,?,?, ...)" string
	 *
	 */
	private String createDefaultValueForInsertStatement(String columns) {
		int count = CharMatcher.is(',').countIn(columns);
		StringBuilder sb = new StringBuilder();
		sb.append(" VALUES (").append(StringUtils.repeat("?,", count)).append("?)");
		return sb.toString();
	}

	/**
	 * @param filename
	 * @return all data across all tables for a video file, if it has an IMDb ID stored.
	 */
	public static List<HashMap<String, Object>> getAPIResultsByFilenameIncludingExternalTables(final String filename) {
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = PMS.get().getDatabase().getConnection()) {
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
