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
import net.pms.dlna.DLNAThumbnail;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImageInfo;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.lang3.StringUtils.*;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
	private final String latestVersion = "9";

	// Database column sizes
	private final int SIZE_CODECV = 32;
	private final int SIZE_FRAMERATE = 32;
	private final int SIZE_ASPECTRATIO_DVDISO = 32;
	private final int SIZE_ASPECTRATIO_CONTAINER = 5;
	private final int SIZE_ASPECTRATIO_VIDEOTRACK = 5;
	private final int SIZE_VIDEO_FORMAT_PROFILE = 40;
	private final int SIZE_CONTAINER = 32;
	private final int SIZE_MATRIX_COEFFICIENTS = 16;
	private final int SIZE_MUXINGMODE = 32;
	private final int SIZE_FRAMERATE_MODE = 16;
	private final int SIZE_STEREOSCOPY = 255;
	private final int SIZE_LANG = 3;
	private final int SIZE_TITLE = 255;
	private final int SIZE_SAMPLEFREQ = 16;
	private final int SIZE_CODECA = 32;
	private final int SIZE_ALBUM = 255;
	private final int SIZE_ARTIST = 255;
	private final int SIZE_SONGNAME = 255;
	private final int SIZE_GENRE = 64;

	public DLNAMediaDatabase(String name) {
		dbName = name;
		File profileDirectory = new File(configuration.getProfileDirectory());
		dbDir = new File(profileDirectory.isDirectory() ? configuration.getProfileDirectory() : null, "database").getAbsolutePath();
		url = Constants.START_URL + dbDir + File.separator + dbName;
		LOGGER.debug("Using database URL: " + url);
		LOGGER.info("Using database located at: " + dbDir);

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
		} else {
			return dbDir + File.separator + dbName;
		}
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

	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
	public synchronized void init(boolean force) {
		dbCount = -1;
		String version = null;
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
				version = rs.getString(1);
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

		// Recreate database if it is not the latest version.
		boolean force_reinit = !latestVersion.equals(version);
		if (force || dbCount == -1 || force_reinit) {
			LOGGER.debug("Database will be (re)initialized");
			try {
				conn = getConnection();
				executeUpdate(conn, "DROP TABLE FILES");
				executeUpdate(conn, "DROP TABLE METADATA");
				executeUpdate(conn, "DROP TABLE REGEXP_RULES");
				executeUpdate(conn, "DROP TABLE AUDIOTRACKS");
				executeUpdate(conn, "DROP TABLE SUBTRACKS");
			} catch (SQLException se) {
				if (se.getErrorCode() != 42102) { // Don't log exception "Table "FILES" not found" which will be corrected in following step
					LOGGER.error(null, se);
				}
			}
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE FILES (");
				sb.append("  ID                      INT AUTO_INCREMENT");
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
				sb.append(", ASPECT                  VARCHAR2(").append(SIZE_ASPECTRATIO_DVDISO).append(')');
				sb.append(", ASPECTRATIOCONTAINER    VARCHAR2(").append(SIZE_ASPECTRATIO_CONTAINER).append(')');
				sb.append(", ASPECTRATIOVIDEOTRACK   VARCHAR2(").append(SIZE_ASPECTRATIO_VIDEOTRACK).append(')');
				sb.append(", REFRAMES                TINYINT");
				sb.append(", VIDEOFORMATPROFILE      VARCHAR2(").append(SIZE_VIDEO_FORMAT_PROFILE).append(')');
				sb.append(", IMAGEINFO               OTHER");
				sb.append(", THUMB                   OTHER");
				sb.append(", CONTAINER               VARCHAR2(").append(SIZE_CONTAINER).append(')');
				sb.append(", MUXINGMODE              VARCHAR2(").append(SIZE_MUXINGMODE).append(')');
				sb.append(", FRAMERATEMODE           VARCHAR2(").append(SIZE_FRAMERATE_MODE).append(')');
				sb.append(", STEREOSCOPY             VARCHAR2(").append(SIZE_STEREOSCOPY).append(')');
				sb.append(", MATRIXCOEFFICIENTS      VARCHAR2(").append(SIZE_MATRIX_COEFFICIENTS).append(')');
				sb.append(", TITLECONTAINER          VARCHAR2(").append(SIZE_TITLE).append(')');
				sb.append(", TITLEVIDEOTRACK         VARCHAR2(").append(SIZE_TITLE).append(')');
				sb.append(", VIDEOTRACKCOUNT         INT");
				sb.append(", IMAGECOUNT              INT");
				sb.append(", BITDEPTH                INT");
				sb.append(", constraint PK1 primary key (FILENAME, MODIFIED, ID))");
				executeUpdate(conn, sb.toString());
				sb = new StringBuilder();
				sb.append("CREATE TABLE AUDIOTRACKS (");
				sb.append("  FILEID            INT              NOT NULL");
				sb.append(", ID                INT              NOT NULL");
				sb.append(", LANG              VARCHAR2(").append(SIZE_LANG).append(')');
				sb.append(", TITLE             VARCHAR2(").append(SIZE_TITLE).append(')');
				sb.append(", NRAUDIOCHANNELS   NUMERIC");
				sb.append(", SAMPLEFREQ        VARCHAR2(").append(SIZE_SAMPLEFREQ).append(')');
				sb.append(", CODECA            VARCHAR2(").append(SIZE_CODECA).append(')');
				sb.append(", BITSPERSAMPLE     INT");
				sb.append(", ALBUM             VARCHAR2(").append(SIZE_ALBUM).append(')');
				sb.append(", ARTIST            VARCHAR2(").append(SIZE_ARTIST).append(')');
				sb.append(", SONGNAME          VARCHAR2(").append(SIZE_SONGNAME).append(')');
				sb.append(", GENRE             VARCHAR2(").append(SIZE_GENRE).append(')');
				sb.append(", YEAR              INT");
				sb.append(", TRACK             INT");
				sb.append(", DELAY             INT");
				sb.append(", MUXINGMODE        VARCHAR2(").append(SIZE_MUXINGMODE).append(')');
				sb.append(", BITRATE           INT");
				sb.append(", constraint PKAUDIO primary key (FILEID, ID))");
				executeUpdate(conn, sb.toString());
				sb = new StringBuilder();
				sb.append("CREATE TABLE SUBTRACKS (");
				sb.append("  FILEID   INT              NOT NULL");
				sb.append(", ID       INT              NOT NULL");
				sb.append(", LANG     VARCHAR2(").append(SIZE_LANG).append(')');
				sb.append(", TITLE    VARCHAR2(").append(SIZE_TITLE).append(')');
				sb.append(", TYPE     INT");
				sb.append(", constraint PKSUB primary key (FILEID, ID))");

				executeUpdate(conn, sb.toString());
				executeUpdate(conn, "CREATE TABLE METADATA (KEY VARCHAR2(255) NOT NULL, VALUE VARCHAR2(255) NOT NULL)");
				executeUpdate(conn, "INSERT INTO METADATA VALUES ('VERSION', '" + latestVersion + "')");
				executeUpdate(conn, "CREATE INDEX IDXARTIST on AUDIOTRACKS (ARTIST asc);");
				executeUpdate(conn, "CREATE INDEX IDXALBUM on AUDIOTRACKS (ALBUM asc);");
				executeUpdate(conn, "CREATE INDEX IDXGENRE on AUDIOTRACKS (GENRE asc);");
				executeUpdate(conn, "CREATE INDEX IDXYEAR on AUDIOTRACKS (YEAR asc);");
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
				LOGGER.info("Error in table creation: " + se.getMessage());
			} finally {
				close(conn);
			}
		} else {
			LOGGER.debug("Database file count: " + dbCount);
			LOGGER.debug("Database version: " + latestVersion);
		}
	}

	private void executeUpdate(Connection conn, String sql) throws SQLException {
		if (conn != null) {
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(sql);
			}
		}
	}

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
			LOGGER.error(null, se);
			return false;
		} finally {
			close(rs);
			close(stmt);
			close(conn);
		}
		return found;
	}

	public synchronized ArrayList<DLNAMediaInfo> getData(String name, long modified) throws IOException, SQLException {
		ArrayList<DLNAMediaInfo> list = new ArrayList<>();
		try (
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM FILES WHERE FILENAME = ? AND MODIFIED = ?");
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
					media.setImageInfo((ImageInfo) rs.getObject("IMAGEINFO"));
					media.setWidth(rs.getInt("WIDTH"));
					media.setHeight(rs.getInt("HEIGHT"));
					media.setSize(rs.getLong("SIZE"));
					media.setCodecV(rs.getString("CODECV"));
					media.setFrameRate(rs.getString("FRAMERATE"));
					media.setAspectRatioDvdIso(rs.getString("ASPECT"));
					media.setAspectRatioContainer(rs.getString("ASPECTRATIOCONTAINER"));
					media.setAspectRatioVideoTrack(rs.getString("ASPECTRATIOVIDEOTRACK"));
					media.setReferenceFrameCount(rs.getByte("REFRAMES"));
					media.setVideoFormatProfile(rs.getString("VIDEOFORMATPROFILE"));
					media.setThumb((DLNAThumbnail) rs.getObject("THUMB"));
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
					media.setMediaparsed(true);

					ResultSet elements;
					audios.setInt(1, id);
					elements = audios.executeQuery();
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
					elements.close();
					subs.setInt(1, id);
					elements = subs.executeQuery();
					while (elements.next()) {
						DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
						sub.setId(elements.getInt("ID"));
						sub.setLang(elements.getString("LANG"));
						sub.setSubtitlesTrackTitleFromMetadata(elements.getString("TITLE"));
						sub.setType(SubtitleType.valueOfStableIndex(elements.getInt("TYPE")));
						media.getSubtitleTracksList().add(sub);
					}
					elements.close();

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

	private Double toDouble(ResultSet rs, String column) throws SQLException {
		Object obj = rs.getObject(column);
		if (obj instanceof Double) {
			return (Double) obj;
		}
		return null;
	}

	private void insertOrUpdateSubtitleTracks(Connection connection, int fileId, DLNAMediaInfo media) throws SQLException {
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
				updateStatment.setInt(1, fileId);
				updateStatment.setInt(2, subtitleTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						rs.updateString("LANG", left(subtitleTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_TITLE));
						rs.updateInt("TYPE", subtitleTrack.getType().getStableIndex());
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setInt(1, fileId);
						insertStatement.setInt(2, subtitleTrack.getId());
						insertStatement.setString(3, left(subtitleTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_TITLE));
						insertStatement.setInt(5, subtitleTrack.getType().getStableIndex());
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}

	private void insertOrUpdateAudioTracks(Connection connection, int fileId, DLNAMediaInfo media) throws SQLException {
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
				updateStatment.setInt(1, fileId);
				updateStatment.setInt(2, audioTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						rs.updateString("LANG", left(audioTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_TITLE));
						rs.updateInt("NRAUDIOCHANNELS", audioTrack.getAudioProperties().getNumberOfChannels());
						rs.updateString("SAMPLEFREQ", left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
						rs.updateString("CODECA", left(audioTrack.getCodecA(), SIZE_CODECA));
						rs.updateInt("BITSPERSAMPLE", audioTrack.getBitsperSample());
						rs.updateString("ALBUM", left(trimToEmpty(audioTrack.getAlbum()), SIZE_ALBUM));
						rs.updateString("ARTIST", left(trimToEmpty(audioTrack.getArtist()), SIZE_ARTIST));
						rs.updateString("SONGNAME", left(trimToEmpty(audioTrack.getSongname()), SIZE_SONGNAME));
						rs.updateString("GENRE", left(trimToEmpty(audioTrack.getGenre()), SIZE_GENRE));
						rs.updateInt("YEAR", audioTrack.getYear());
						rs.updateInt("TRACK", audioTrack.getTrack());
						rs.updateInt("DELAY", audioTrack.getAudioProperties().getAudioDelay());
						rs.updateString("MUXINGMODE", left(trimToEmpty(audioTrack.getMuxingModeAudio()), SIZE_MUXINGMODE));
						rs.updateInt("BITRATE", audioTrack.getBitRate());
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setInt(1, fileId);
						insertStatement.setInt(2, audioTrack.getId());
						insertStatement.setString(3, left(audioTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_TITLE));
						insertStatement.setInt(5, audioTrack.getAudioProperties().getNumberOfChannels());
						insertStatement.setString(6, left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
						insertStatement.setString(7, left(audioTrack.getCodecA(), SIZE_CODECA));
						insertStatement.setInt(8, audioTrack.getBitsperSample());
						insertStatement.setString(9, left(trimToEmpty(audioTrack.getAlbum()), SIZE_ALBUM));
						insertStatement.setString(10, left(trimToEmpty(audioTrack.getArtist()), SIZE_ARTIST));
						insertStatement.setString(11, left(trimToEmpty(audioTrack.getSongname()), SIZE_SONGNAME));
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

	public synchronized void insertOrUpdateData(String name, long modified, int type, DLNAMediaInfo media) throws SQLException {
		try (
			Connection connection = getConnection()
		) {
			connection.setAutoCommit(false);
			int fileId = -1;
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"ID, FILENAME, MODIFIED, TYPE, DURATION, BITRATE, WIDTH, HEIGHT, SIZE, CODECV, FRAMERATE, " +
					"ASPECT, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, VIDEOFORMATPROFILE, IMAGEINFO, THUMB, " +
					"CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
					"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH " +
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
						fileId = rs.getInt("ID");
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
							rs.updateString("ASPECT", left(media.getAspectRatioDvdIso(), SIZE_ASPECTRATIO_DVDISO));
							rs.updateString("ASPECTRATIOCONTAINER", left(media.getAspectRatioContainer(), SIZE_ASPECTRATIO_CONTAINER));
							rs.updateString("ASPECTRATIOVIDEOTRACK", left(media.getAspectRatioVideoTrack(), SIZE_ASPECTRATIO_VIDEOTRACK));
							rs.updateByte("REFRAMES", media.getReferenceFrameCount());
							rs.updateString("VIDEOFORMATPROFILE", left(media.getVideoFormatProfile(), SIZE_VIDEO_FORMAT_PROFILE));
							if (media.getImageInfo() != null) {
								rs.updateObject("IMAGEINFO", media.getImageInfo());
							} else {
								rs.updateNull("IMAGEINFO");
							}
							if (media.getThumb() != null) {
								rs.updateObject("THUMB", media.getThumb());
							} else {
								rs.updateNull("THUMB");
							}
							rs.updateString("CONTAINER", left(media.getContainer(), SIZE_CONTAINER));
							rs.updateString("MUXINGMODE", left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
							rs.updateString("FRAMERATEMODE", left(media.getFrameRateMode(), SIZE_FRAMERATE_MODE));
							rs.updateString("STEREOSCOPY", left(media.getStereoscopy(), SIZE_STEREOSCOPY));
							rs.updateString("MATRIXCOEFFICIENTS", left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
							rs.updateString("TITLECONTAINER", left(media.getFileTitleFromMetadata(), SIZE_TITLE));
							rs.updateString("TITLEVIDEOTRACK", left(media.getVideoTrackTitleFromMetadata(), SIZE_TITLE));
							rs.updateInt("VIDEOTRACKCOUNT", media.getVideoTrackCount());
							rs.updateInt("IMAGECOUNT", media.getImageCount());
							rs.updateInt("BITDEPTH", media.getVideoBitDepth());
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
						"FRAMERATE, ASPECT, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, VIDEOFORMATPROFILE, IMAGEINFO, " +
						"THUMB, CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
						"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH) VALUES "+
						"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
				) {
					ps.setString(1, name);
					ps.setTimestamp(2, new Timestamp(modified));
					ps.setInt(3, type);
					if (media != null) {
						if (media.getDuration() != null) {
							ps.setDouble(4, media.getDurationInSeconds());
						} else {
							ps.setNull(4, Types.DOUBLE);
						}

						int databaseBitrate = 0;
						if (type != Format.IMAGE) {
							databaseBitrate = media.getBitrate();
							if (databaseBitrate == 0) {
								LOGGER.debug("Could not parse the bitrate for: " + name);
							}
						}
						ps.setInt(5, databaseBitrate);

						ps.setInt(6, media.getWidth());
						ps.setInt(7, media.getHeight());
						ps.setLong(8, media.getSize());
						ps.setString(9, left(media.getCodecV(), SIZE_CODECV));
						ps.setString(10, left(media.getFrameRate(), SIZE_FRAMERATE));
						ps.setString(11, left(media.getAspectRatioDvdIso(), SIZE_ASPECTRATIO_DVDISO));
						ps.setString(12, left(media.getAspectRatioContainer(), SIZE_ASPECTRATIO_CONTAINER));
						ps.setString(13, left(media.getAspectRatioVideoTrack(), SIZE_ASPECTRATIO_VIDEOTRACK));
						ps.setByte(14, media.getReferenceFrameCount());
						ps.setString(15, left(media.getVideoFormatProfile(), SIZE_VIDEO_FORMAT_PROFILE));
						if (media.getImageInfo() != null) {
							ps.setObject(16, media.getImageInfo());
						} else {
							ps.setNull(16, Types.OTHER);
						}
						if (media.getThumb() != null) {
							ps.setObject(17, media.getThumb());
						} else {
							ps.setNull(17, Types.OTHER);
						}
						ps.setString(18, left(media.getContainer(), SIZE_CONTAINER));
						ps.setString(19, left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
						ps.setString(20, left(media.getFrameRateMode(), SIZE_FRAMERATE_MODE));
						ps.setString(21, left(media.getStereoscopy(), SIZE_STEREOSCOPY));
						ps.setString(22, left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
						ps.setString(23, left(media.getFileTitleFromMetadata(), SIZE_TITLE));
						ps.setString(24, left(media.getVideoTrackTitleFromMetadata(), SIZE_TITLE));
						ps.setInt(25, media.getVideoTrackCount());
						ps.setInt(26, media.getImageCount());
						ps.setInt(27, media.getVideoBitDepth());
					} else {
						ps.setString(4, null);
						ps.setInt(5, 0);
						ps.setInt(6, 0);
						ps.setInt(7, 0);
						ps.setLong(8, 0);
						ps.setNull(9, Types.VARCHAR);
						ps.setNull(10, Types.VARCHAR);
						ps.setNull(11, Types.VARCHAR);
						ps.setNull(12, Types.VARCHAR);
						ps.setNull(13, Types.VARCHAR);
						ps.setByte(14, (byte) -1);
						ps.setNull(15, Types.VARCHAR);
						ps.setNull(16, Types.OTHER);
						ps.setNull(17, Types.OTHER);
						ps.setNull(18, Types.VARCHAR);
						ps.setNull(19, Types.VARCHAR);
						ps.setNull(20, Types.VARCHAR);
						ps.setNull(21, Types.VARCHAR);
						ps.setNull(22, Types.VARCHAR);
						ps.setNull(23, Types.VARCHAR);
						ps.setNull(24, Types.VARCHAR);
						ps.setInt(25, 0);
						ps.setInt(26, 0);
						ps.setInt(27, 0);
					}
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						if (rs.next()) {
							fileId = rs.getInt(1);
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
		}
	}

	public synchronized void updateThumbnail(String name, long modified, int type, DLNAMediaInfo media) {
		try (
			Connection conn = getConnection();
			PreparedStatement ps = conn.prepareStatement(
				"UPDATE FILES SET THUMB = ? WHERE FILENAME = ? AND MODIFIED = ?"
			);
		) {

			ps.setString(2, name);
			ps.setTimestamp(3, new Timestamp(modified));
			if (media != null && media.getThumb() != null) {
				ps.setObject(1, media.getThumb());
			} else {
				ps.setNull(1, Types.OTHER);
			}
			ps.executeUpdate();
		} catch (SQLException se) {
			LOGGER.error("Error updating cached thumbnail for \"{}\": {}", se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public synchronized ArrayList<String> getStrings(String sql) {
		ArrayList<String> list = new ArrayList<>();
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			conn = getConnection();
			ps = conn.prepareStatement(sql);
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
			close(conn);
		}
		return list;
	}

	public synchronized void cleanup() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnection();
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
		} catch (SQLException se) {
			LOGGER.error(null, se);
		} finally {
			close(rs);
			close(ps);
			close(conn);
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

	private void close(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	private void close(Statement ps) {
		try {
			if (ps != null) {
				ps.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	private void close(Connection conn) {
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
			scanner.start();
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
