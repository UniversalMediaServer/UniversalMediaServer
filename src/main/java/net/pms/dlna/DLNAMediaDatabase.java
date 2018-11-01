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
import net.pms.util.Rational;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import static net.pms.database.Tables.sqlLikeEscape;
import static org.apache.commons.lang3.StringUtils.*;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.newgui.NavigationShareTab;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class DLNAMediaDatabase implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaDatabase.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	private String url;
	private String dbDir;
	private String dbName;
	public static final String NONAME = "###";
	private Thread scanner;
	private JdbcConnectionPool cp;
	private int dbCount;

	/**
	 * The database version should be incremented when we change anything to
	 * do with the database since the last released version.
	 */
	private final int latestVersion = 17;

	// Database column sizes
	private final int SIZE_CODECV = 32;
	private final int SIZE_FRAMERATE = 32;
	private final int SIZE_AVC_LEVEL = 3;
	private final int SIZE_CONTAINER = 32;
	private final int SIZE_IMDBID = 16;
	private final int SIZE_MATRIX_COEFFICIENTS = 16;
	private final int SIZE_MUXINGMODE = 32;
	private final int SIZE_FRAMERATE_MODE = 16;
	private final int SIZE_LANG = 3;
	private final int SIZE_SAMPLEFREQ = 16;
	private final int SIZE_CODECA = 32;
	private final int SIZE_GENRE = 64;
	private final int SIZE_YEAR = 4;
	private final int SIZE_TVSEASON = 4;
	private final int SIZE_TVEPISODENUMBER = 8;

	// Generic constant for the maximum string size: 255 chars
	private final int SIZE_MAX = 255;

	/**
	 * Initializes the database connection pool for the current profile.
	 *
	 * Will create the "UMS-tests" profile directory and put the database
	 * in there if it doesn't exist, in order to prevent overwriting
	 * real databases.
	 */
	public DLNAMediaDatabase(String name) {
		dbName = name;
		File profileDirectory = new File(configuration.getProfileDirectory());
		dbDir = new File(PMS.isRunningTests() || profileDirectory.isDirectory() ? configuration.getProfileDirectory() : null, "database").getAbsolutePath();
		boolean logDB = configuration.getDatabaseLogging();
		url = Constants.START_URL + dbDir + File.separator + dbName + (logDB ? ";TRACE_LEVEL_FILE=4" : "");
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
		boolean trace = LOGGER.isTraceEnabled();

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
							JOptionPane.ERROR_MESSAGE);
					}
					LOGGER.error("Damaged cache can't be deleted. Stop the program and delete the folder \"" + dbDir + "\" manually");
					PMS.get().getRootFolder(null).stopScan();
					configuration.setUseCache(false);
					return;
				}
			} else {
				LOGGER.error("Database connection error: " + se.getMessage());
				LOGGER.trace("", se);
				RootFolder rootFolder = PMS.get().getRootFolder(null);
				if (rootFolder != null) {
					rootFolder.stopScan();
				}
				configuration.setUseCache(false);
				return;
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
		if (currentVersion != -1 && latestVersion != currentVersion) {
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
				LOGGER.trace("DROPPING TABLE AUDIOTRACKS");
				executeUpdate(conn, "DROP TABLE AUDIOTRACKS");
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
				sb.append(", ASPECTRATIODVD          OTHER");
				sb.append(", ASPECTRATIOCONTAINER    OTHER");
				sb.append(", ASPECTRATIOVIDEOTRACK   OTHER");
				sb.append(", REFRAMES                TINYINT");
				sb.append(", AVCLEVEL                VARCHAR2(").append(SIZE_AVC_LEVEL).append(')');
				sb.append(", IMAGEINFO               OTHER");
				sb.append(", CONTAINER               VARCHAR2(").append(SIZE_CONTAINER).append(')');
				sb.append(", MUXINGMODE              VARCHAR2(").append(SIZE_MUXINGMODE).append(')');
				sb.append(", FRAMERATEMODE           VARCHAR2(").append(SIZE_FRAMERATE_MODE).append(')');
				sb.append(", STEREOSCOPY             VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", MATRIXCOEFFICIENTS      VARCHAR2(").append(SIZE_MATRIX_COEFFICIENTS).append(')');
				sb.append(", TITLECONTAINER          VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", TITLEVIDEOTRACK         VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", VIDEOTRACKCOUNT         INT");
				sb.append(", IMAGECOUNT              INT");
				sb.append(", BITDEPTH                INT");
				sb.append(", PIXELASPECTRATIO        OTHER");
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
				sb.append(", EXTRAINFORMATION        VARCHAR2(").append(SIZE_MAX).append("))");
				if (trace) {
					LOGGER.trace("Creating table FILES with:\n\n{}\n", sb.toString());
				}
				executeUpdate(conn, sb.toString());
				sb = new StringBuilder();
				sb.append("CREATE TABLE AUDIOTRACKS (");
				sb.append("  ID                INT              NOT NULL");
				sb.append(", FILEID            BIGINT           NOT NULL");
				sb.append(", LANG              VARCHAR2(").append(SIZE_LANG).append(')');
				sb.append(", TITLE             VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", NRAUDIOCHANNELS   NUMERIC");
				sb.append(", SAMPLEFREQ        VARCHAR2(").append(SIZE_SAMPLEFREQ).append(')');
				sb.append(", CODECA            VARCHAR2(").append(SIZE_CODECA).append(')');
				sb.append(", BITSPERSAMPLE     INT");
				sb.append(", ALBUM             VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", ARTIST            VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", SONGNAME          VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", GENRE             VARCHAR2(").append(SIZE_GENRE).append(')');
				sb.append(", YEAR              INT");
				sb.append(", TRACK             INT");
				sb.append(", DELAY             INT");
				sb.append(", MUXINGMODE        VARCHAR2(").append(SIZE_MUXINGMODE).append(')');
				sb.append(", BITRATE           INT");
				sb.append(", constraint PKAUDIO primary key (FILEID, ID)");
				sb.append(", FOREIGN KEY(FILEID)");
				sb.append("    REFERENCES FILES(ID)");
				sb.append("    ON DELETE CASCADE");
				sb.append(')');
				if (trace) {
					LOGGER.trace("Creating table AUDIOTRACKS with:\n\n{}\n", sb.toString());
				}
				executeUpdate(conn, sb.toString());
				sb = new StringBuilder();
				sb.append("CREATE TABLE SUBTRACKS (");
				sb.append("  ID       INT              NOT NULL");
				sb.append(", FILEID   BIGINT           NOT NULL");
				sb.append(", LANG     VARCHAR2(").append(SIZE_LANG).append(')');
				sb.append(", TITLE    VARCHAR2(").append(SIZE_MAX).append(')');
				sb.append(", TYPE     INT");
				sb.append(", constraint PKSUB primary key (FILEID, ID)");
				sb.append(", FOREIGN KEY(FILEID)");
				sb.append("    REFERENCES FILES(ID)");
				sb.append("    ON DELETE CASCADE");
				sb.append(')');

				if (trace) {
					LOGGER.trace("Creating table SUBTRACKS with:\n\n{}\n", sb.toString());
				}
				executeUpdate(conn, sb.toString());

				LOGGER.trace("Creating table METADATA");
				executeUpdate(conn, "CREATE TABLE METADATA (KEY VARCHAR2(255) NOT NULL, VALUE VARCHAR2(255) NOT NULL)");
				executeUpdate(conn, "INSERT INTO METADATA VALUES ('VERSION', '" + latestVersion + "')");

				LOGGER.trace("Creating index IDX_FILE");
				executeUpdate(conn, "CREATE UNIQUE INDEX IDX_FILE ON FILES(FILENAME, MODIFIED)");

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

				LOGGER.trace("Creating index IDXARTIST");
				executeUpdate(conn, "CREATE INDEX IDXARTIST on AUDIOTRACKS (ARTIST asc);");

				LOGGER.trace("Creating index IDXALBUM");
				executeUpdate(conn, "CREATE INDEX IDXALBUM on AUDIOTRACKS (ALBUM asc);");

				LOGGER.trace("Creating index IDXGENRE");
				executeUpdate(conn, "CREATE INDEX IDXGENRE on AUDIOTRACKS (GENRE asc);");

				LOGGER.trace("Creating index IDXYEAR");
				executeUpdate(conn, "CREATE INDEX IDXYEAR on AUDIOTRACKS (YEAR asc);");

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
			LOGGER.debug("Database version: " + latestVersion);
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
	public synchronized boolean isDataExists(String name, long modified) {
		boolean found = false;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("SELECT * FROM FILES WHERE FILENAME = ? AND MODIFIED = ?");
			stmt.setString(1, name);
			stmt.setTimestamp(2, new Timestamp(modified));
			rs = stmt.executeQuery();
			while (rs.next()) {
				found = true;
			}
		} catch (SQLException se) {
			LOGGER.error("An SQL error occurred when trying to check if data exists for \"{}\": {}", name, se.getMessage());
			LOGGER.trace("", se);
			return false;
		} finally {
			close(rs);
			close(stmt);
			close(conn);
		}
		return found;
	}

	/**
	 * Checks whether data from OpenSubtitles has been written to the database
	 * for this media.
	 *
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return {@code true} if OpenSubtitles metadata exists for this media,
	 *         {@code false} otherwise.
	 */
	public synchronized boolean isOpenSubtitlesMetadataExists(String name, long modified) {
		boolean found = false;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("SELECT * FROM FILES WHERE FILENAME = ? AND MODIFIED = ? AND IMDBID IS NOT NULL");
			stmt.setString(1, name);
			stmt.setTimestamp(2, new Timestamp(modified));
			rs = stmt.executeQuery();
			while (rs.next()) {
				found = true;
			}
		} catch (SQLException se) {
			LOGGER.error(
				"An SQL error occurred when trying to check if OpenSubtitles metadata exists for \"{}\": {}",
				name,
				se.getMessage()
			);
			LOGGER.trace("", se);
			return false;
		} finally {
			close(rs);
			close(stmt);
			close(conn);
		}
		return found;
	}

	/**
	 * Gets rows of {@link DLNAMediaDatabase} from the database and returns them
	 * as a {@link List} of {@link DLNAMediaInfo} instances.
	 *
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return The {@link List} of {@link DLNAMediaInfo} instances matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public synchronized ArrayList<DLNAMediaInfo> getData(String name, long modified) throws IOException, SQLException {
		ArrayList<DLNAMediaInfo> list = new ArrayList<>();
		try (
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement(
				"SELECT * FROM FILES LEFT JOIN " + TableThumbnails.TABLE_NAME + " ON FILES.THUMBID=" + TableThumbnails.TABLE_NAME + ".ID " +
				"WHERE FILENAME = ? AND FILES.MODIFIED = ?"
			);
		) {
			stmt.setString(1, name);
			stmt.setTimestamp(2, new Timestamp(modified));
			try (
				ResultSet rs = stmt.executeQuery();
				PreparedStatement audios = conn.prepareStatement("SELECT * FROM AUDIOTRACKS WHERE FILEID = ?");
				PreparedStatement subs = conn.prepareStatement("SELECT * FROM SUBTRACKS WHERE FILEID = ?")
			) {
				while (rs.next()) {
					DLNAMediaInfo media = new DLNAMediaInfo();
					int id = rs.getInt("ID");
					media.setDuration(toDouble(rs, "DURATION"));
					media.setBitrate(rs.getInt("BITRATE"));
					media.setWidth(rs.getInt("WIDTH"));
					media.setHeight(rs.getInt("HEIGHT"));
					media.setSize(rs.getLong("SIZE"));
					media.setCodecV(rs.getString("CODECV"));
					media.setFrameRate(rs.getString("FRAMERATE"));
					media.setAspectRatioDvdIso((Rational) rs.getObject("ASPECTRATIODVD"));
					media.setAspectRatioContainer((Rational) rs.getObject("ASPECTRATIOCONTAINER"));
					media.setAspectRatioVideoTrack((Rational) rs.getObject("ASPECTRATIOVIDEOTRACK"));
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
					media.setPixelAspectRatio((Rational) rs.getObject("PIXELASPECTRATIO"));
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
							audio.setSongname(elements.getString("SONGNAME"));
							audio.setGenre(elements.getString("GENRE"));
							audio.setYear(elements.getInt("YEAR"));
							audio.setTrack(elements.getInt("TRACK"));
							audio.getAudioProperties().setAudioDelay(elements.getInt("DELAY"));
							audio.setMuxingModeAudio(elements.getString("MUXINGMODE"));
							audio.setBitRate(elements.getInt("BITRATE"));
							media.getAudioTracksList().add(audio);
						}
					}
					subs.setInt(1, id);
					try (ResultSet elements = subs.executeQuery()) {
						while (elements.next()) {
							DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
							sub.setId(elements.getInt("ID"));
							sub.setLang(elements.getString("LANG"));
							sub.setSubtitlesTrackTitleFromMetadata(elements.getString("TITLE"));
							sub.setType(SubtitleType.valueOfStableIndex(elements.getInt("TYPE")));
							media.getSubtitleTracksList().add(sub);
						}
					}

					list.add(media);
				}
			}
		} catch (SQLException se) {
			if (se.getCause() != null && se.getCause() instanceof IOException) {
				throw (IOException) se.getCause();
			}
			throw se;
		}
		return list;
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

		/* XXX This is flawed, multiple subtitle tracks with the same language will
		 * overwrite each other.
		 */
		try (
			PreparedStatement updateStatment = connection.prepareStatement(
				"SELECT " +
					"FILEID, ID, LANG, TITLE, TYPE " +
				"FROM SUBTRACKS " +
				"WHERE " +
					"FILEID = ? AND ID = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO SUBTRACKS (" +
					"FILEID, ID, LANG, TITLE, TYPE " +
				") VALUES (" +
					"?, ?, ?, ?, ?" +
				")"
			);
		) {
			for (DLNAMediaSubtitle subtitleTrack : media.getSubtitleTracksList()) {
				updateStatment.setLong(1, fileId);
				updateStatment.setInt(2, subtitleTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						rs.updateString("LANG", left(subtitleTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						rs.updateInt("TYPE", subtitleTrack.getType().getStableIndex());
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setLong(1, fileId);
						insertStatement.setInt(2, subtitleTrack.getId());
						insertStatement.setString(3, left(subtitleTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						insertStatement.setInt(5, subtitleTrack.getType().getStableIndex());
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}

	private void insertOrUpdateAudioTracks(Connection connection, long fileId, DLNAMediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getAudioTrackCount() < 1) {
			return;
		}

		/* XXX This is flawed, multiple audio tracks with the same language will
		 * overwrite each other.
		 */
		try (
			PreparedStatement updateStatment = connection.prepareStatement(
				"SELECT " +
					"FILEID, ID, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, " +
					"BITSPERSAMPLE, ALBUM, ARTIST, SONGNAME, GENRE, YEAR, TRACK, " +
					"DELAY, MUXINGMODE, BITRATE " +
				"FROM AUDIOTRACKS " +
				"WHERE " +
					"FILEID = ? AND ID = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO AUDIOTRACKS (" +
					"FILEID, ID, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, BITSPERSAMPLE, " +
					"ALBUM, ARTIST, SONGNAME, GENRE, YEAR, TRACK, DELAY, MUXINGMODE, BITRATE" +
				") VALUES (" +
					"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				")"
			);
		) {
			for (DLNAMediaAudio audioTrack : media.getAudioTracksList()) {
				updateStatment.setLong(1, fileId);
				updateStatment.setInt(2, audioTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						rs.updateString("LANG", left(audioTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_MAX));
						rs.updateInt("NRAUDIOCHANNELS", audioTrack.getAudioProperties().getNumberOfChannels());
						rs.updateString("SAMPLEFREQ", left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
						rs.updateString("CODECA", left(audioTrack.getCodecA(), SIZE_CODECA));
						rs.updateInt("BITSPERSAMPLE", audioTrack.getBitsperSample());
						rs.updateString("ALBUM", left(trimToEmpty(audioTrack.getAlbum()), SIZE_MAX));
						rs.updateString("ARTIST", left(trimToEmpty(audioTrack.getArtist()), SIZE_MAX));
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
						insertStatement.setString(11, left(trimToEmpty(audioTrack.getSongname()), SIZE_MAX));
						insertStatement.setString(12, left(trimToEmpty(audioTrack.getGenre()), SIZE_GENRE));
						insertStatement.setInt(13, audioTrack.getYear());
						insertStatement.setInt(14, audioTrack.getTrack());
						insertStatement.setInt(15, audioTrack.getAudioProperties().getAudioDelay());
						insertStatement.setString(16, left(trimToEmpty(audioTrack.getMuxingModeAudio()), SIZE_MUXINGMODE));
						insertStatement.setInt(17, audioTrack.getBitRate());
						insertStatement.executeUpdate();
					}
				}
			}
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
	public synchronized void insertOrUpdateData(String name, long modified, int type, DLNAMediaInfo media) throws SQLException {
		try (
			Connection connection = getConnection()
		) {
			connection.setAutoCommit(false);
			long fileId = -1;
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"ID, FILENAME, MODIFIED, TYPE, DURATION, BITRATE, WIDTH, HEIGHT, SIZE, CODECV, FRAMERATE, " +
					"ASPECTRATIODVD, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, " +
					"CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
					"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH, PIXELASPECTRATIO, SCANTYPE, SCANORDER, " +
					"IMDBID, YEAR, MOVIEORSHOWNAME, MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION " +
				"FROM FILES " +
				"WHERE " +
					"FILENAME = ?",
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
							updateSerialized(rs, media.getAspectRatioDvdIso(), "ASPECTRATIODVD");
							updateSerialized(rs, media.getAspectRatioContainer(), "ASPECTRATIOCONTAINER");
							updateSerialized(rs, media.getAspectRatioVideoTrack(), "ASPECTRATIOVIDEOTRACK");
							rs.updateByte("REFRAMES", media.getReferenceFrameCount());
							rs.updateString("AVCLEVEL", left(media.getAvcLevel(), SIZE_AVC_LEVEL));
							updateSerialized(rs, media.getImageInfo(), "IMAGEINFO");
							if (media.getImageInfo() != null) {
								rs.updateObject("IMAGEINFO", media.getImageInfo());
							} else {
								rs.updateNull("IMAGEINFO");
							}
							rs.updateString("CONTAINER", left(media.getContainer(), SIZE_CONTAINER));
							rs.updateString("MUXINGMODE", left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
							rs.updateString("FRAMERATEMODE", left(media.getFrameRateMode(), SIZE_FRAMERATE_MODE));
							rs.updateString("STEREOSCOPY", left(media.getStereoscopy(), SIZE_MAX));
							rs.updateString("MATRIXCOEFFICIENTS", left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
							rs.updateString("TITLECONTAINER", left(media.getFileTitleFromMetadata(), SIZE_MAX));
							rs.updateString("TITLEVIDEOTRACK", left(media.getVideoTrackTitleFromMetadata(), SIZE_MAX));
							rs.updateInt("VIDEOTRACKCOUNT", media.getVideoTrackCount());
							rs.updateInt("IMAGECOUNT", media.getImageCount());
							rs.updateInt("BITDEPTH", media.getVideoBitDepth());
							updateSerialized(rs, media.getPixelAspectRatio(), "PIXELASPECTRATIO");
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
			}
			if (fileId < 0) {
				// No fileId means it didn't exist
				try (
					PreparedStatement ps = connection.prepareStatement(
						"INSERT INTO FILES (FILENAME, MODIFIED, TYPE, DURATION, BITRATE, WIDTH, HEIGHT, SIZE, CODECV, " +
						"FRAMERATE, ASPECTRATIODVD, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, " +
						"CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
						"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH, PIXELASPECTRATIO, SCANTYPE, SCANORDER, IMDBID, YEAR, MOVIEORSHOWNAME, " +
						"MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION) VALUES " +
						"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
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
						insertSerialized(ps, media.getAspectRatioDvdIso(), ++databaseColumnIterator);
						insertSerialized(ps, media.getAspectRatioContainer(), ++databaseColumnIterator);
						insertSerialized(ps, media.getAspectRatioVideoTrack(), ++databaseColumnIterator);
						ps.setByte(++databaseColumnIterator, media.getReferenceFrameCount());
						ps.setString(++databaseColumnIterator, left(media.getAvcLevel(), SIZE_AVC_LEVEL));
						if (media.getImageInfo() != null) {
							ps.setObject(++databaseColumnIterator, media.getImageInfo());
						} else {
							ps.setNull(++databaseColumnIterator, Types.OTHER);
						}
						ps.setString(++databaseColumnIterator, left(media.getContainer(), SIZE_CONTAINER));
						ps.setString(++databaseColumnIterator, left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
						ps.setString(++databaseColumnIterator, left(media.getFrameRateMode(), SIZE_FRAMERATE_MODE));
						ps.setString(++databaseColumnIterator, left(media.getStereoscopy(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
						ps.setString(++databaseColumnIterator, left(media.getFileTitleFromMetadata(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, left(media.getVideoTrackTitleFromMetadata(), SIZE_MAX));
						ps.setInt(++databaseColumnIterator, media.getVideoTrackCount());
						ps.setInt(++databaseColumnIterator, media.getImageCount());
						ps.setInt(++databaseColumnIterator, media.getVideoBitDepth());
						insertSerialized(ps, media.getPixelAspectRatio(), ++databaseColumnIterator);
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
						ps.setNull(++databaseColumnIterator, Types.OTHER);
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
			if (media.getThumb() != null) {
				TableThumbnails.setThumbnail(media.getThumb(), name);
			}
		}
	}

	/**
	 * Updates an existing row with information either extracted from the filename
	 * or from OpenSubtitles.
	 *
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @param media the {@link DLNAMediaInfo} row to update.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public synchronized void insertVideoMetadata(String name, long modified, DLNAMediaInfo media) throws SQLException {
		if (StringUtils.isBlank(name)) {
			LOGGER.warn(
				"Couldn't write OpenSubtitles data for \"{}\" to the database because the media cannot be identified",
				name
			);
			return;
		}
		if (media == null) {
			LOGGER.warn("Couldn't write OpenSubtitles data for \"{}\" to the database because there is no media information",
				name
			);
			return;
		}

		try (Connection connection = getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"ID, IMDBID, YEAR, MOVIEORSHOWNAME, MOVIEORSHOWNAMESIMPLE, TVSEASON, TVEPISODENUMBER, TVEPISODENAME, ISTVEPISODE, EXTRAINFORMATION " +
				"FROM FILES " +
				"WHERE " +
					"FILENAME = ? AND MODIFIED = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, name);
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
						LOGGER.error("Couldn't find \"{}\" in the database when trying to store data from OpenSubtitles", name);
					}
				}
			}
			connection.commit();
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
	public synchronized void updateRowsInFilesTable(String oldValue, String newValue, String columnName, int size, boolean isString) throws SQLException {
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
			try (Statement statement = connection.createStatement()) {
				int rows = statement.executeUpdate(
					"UPDATE FILES SET " +
						columnName +  " = " + (newValue == null ? "NULL" : (isString ? left(newValue, size) : newValue)) +
					" WHERE " +
						columnName + (oldValue == null ? " IS NULL" : " = " + (isString ? left(oldValue, size) : oldValue))
				);
				LOGGER.trace("Updated {} rows in FILES table", rows);
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
	 * Removes row(s) in the both FILES and FILES_STATUS tables representing matching media. If {@code useLike} is
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
	public synchronized void deleteRowsInFilesTable(String filename, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(filename)) {
			return;
		}

		LOGGER.trace("Deleting rows from FILES table where the filename is \"{}\"", filename);
		try (Connection connection = getConnection()) {
			try (Statement statement = connection.createStatement()) {
				int rows;
				if (useLike) {
					rows = statement.executeUpdate("DELETE FROM FILES WHERE FILENAME LIKE " + Tables.sqlQuote(filename));
				} else {
					rows = statement.executeUpdate("DELETE FROM FILES WHERE FILENAME = " + Tables.sqlQuote(filename));
				}
				LOGGER.trace("Deleted {} rows from FILES", rows);
			}
		}
	}

	public synchronized void updateThumbnailId(String fullPathToFile, int thumbId) {
		try (
			Connection conn = getConnection();
			PreparedStatement ps = conn.prepareStatement(
				"UPDATE FILES SET THUMBID = ? WHERE FILENAME = ?"
			);
		) {
			ps.setInt(1, thumbId);
			ps.setString(2, fullPathToFile);
			ps.executeUpdate();
			LOGGER.trace("THUMBID updated to {} for {}", thumbId, fullPathToFile);
		} catch (SQLException se) {
			LOGGER.error("Error updating cached thumbnail for \"{}\": {}", se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public synchronized ArrayList<String> getStrings(String sql) {
		ArrayList<String> list = new ArrayList<>();
		Connection connection = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			connection = getConnection();
			ps = connection.prepareStatement(sql);
			rs = ps.executeQuery();
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
		} catch (SQLException se) {
			LOGGER.error(null, se);
			return null;
		} finally {
			close(rs);
			close(ps);
			close(connection);
		}
		return list;
	}

	public synchronized void cleanup() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnection();

			/**
			 * Cleanup of FILES table
			 *
			 * Removes entries that are not on the hard drive anymore.
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
				while (rs.next()) {
					String filename = rs.getString("FILENAME");
					long modified = rs.getTimestamp("MODIFIED").getTime();
					File file = new File(filename);
					if (!file.exists() || file.lastModified() != modified) {
						rs.deleteRow();
					}
					i++;
					int newpercent = i * 100 / dbCount;
					if (newpercent > oldpercent) {
						PMS.get().getFrame().setStatusLine(Messages.getString("DLNAMediaDatabase.2") + newpercent + "%");
						oldpercent = newpercent;
					}
				}
			}

			/**
			 * Cleanup of THUMBNAILS table
			 *
			 * Removes entries that are not referenced by any rows in the FILES table.
			 */
			ps = conn.prepareStatement(
				"DELETE FROM THUMBNAILS " +
				"WHERE NOT EXISTS (" +
					"SELECT ID FROM FILES " +
					"WHERE FILES.THUMBID = THUMBNAILS.ID" +
				");"
			);
			ps.execute();

			/**
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
		} catch (SQLException se) {
			LOGGER.error(null, se);
		} finally {
			close(rs);
			close(ps);
			close(conn);
			PMS.get().getFrame().setStatusLine(null);
		}
	}

	public synchronized ArrayList<File> getFiles(String sql) {
		ArrayList<File> list = new ArrayList<>();
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			conn = getConnection();
			ps = conn.prepareStatement(sql.toLowerCase().startsWith("select") ? sql : ("SELECT FILENAME, MODIFIED FROM FILES WHERE " + sql));
			rs = ps.executeQuery();
			while (rs.next()) {
				String filename = rs.getString("FILENAME");
				long modified = rs.getTimestamp("MODIFIED").getTime();
				File file = new File(filename);
				if (file.exists() && file.lastModified() == modified) {
					list.add(file);
				}
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
			return null;
		} finally {
			close(rs);
			close(ps);
			close(conn);
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
			scanner.setPriority(scanner.MIN_PRIORITY);
			scanner.start();
			NavigationShareTab.setScanLibraryBusy();
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
}
