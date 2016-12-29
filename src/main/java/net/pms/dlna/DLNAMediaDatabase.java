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
import org.apache.commons.io.FileUtils;
import static org.apache.commons.lang3.StringUtils.*;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final String latestVersion = "8";

	// Database column sizes
	private final int SIZE_CODECV = 32;
	private final int SIZE_FRAMERATE = 32;
	private final int SIZE_ASPECTRATIO_DVDISO = 32;
	private final int SIZE_ASPECTRATIO_CONTAINER = 5;
	private final int SIZE_ASPECTRATIO_VIDEOTRACK = 5;
	private final int SIZE_AVC_LEVEL = 3;
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

	public void init(boolean force) {
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
				sb.append(", AVCLEVEL                VARCHAR2(").append(SIZE_AVC_LEVEL).append(')');
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

	public boolean isDataExists(String name, long modified) {
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

	public ArrayList<DLNAMediaInfo> getData(String name, long modified) {
		ArrayList<DLNAMediaInfo> list = new ArrayList<>();
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
				DLNAMediaInfo media = new DLNAMediaInfo();
				int id = rs.getInt("ID");
				media.setDuration(toDouble(rs, "DURATION"));
				media.setBitrate(rs.getInt("BITRATE"));
				media.setWidth(rs.getInt("WIDTH"));
				media.setHeight(rs.getInt("HEIGHT"));
				media.setSize(rs.getLong("SIZE"));
				media.setCodecV(rs.getString("CODECV"));
				media.setFrameRate(rs.getString("FRAMERATE"));
				media.setAspectRatioDvdIso(rs.getString("ASPECT"));
				media.setAspectRatioContainer(rs.getString("ASPECTRATIOCONTAINER"));
				media.setAspectRatioVideoTrack(rs.getString("ASPECTRATIOVIDEOTRACK"));
				media.setReferenceFrameCount(rs.getByte("REFRAMES"));
				media.setAvcLevel(rs.getString("AVCLEVEL"));
				media.setImageInfo((ImageInfo) rs.getObject("IMAGEINFO"));
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
				ResultSet subrs;
				try (PreparedStatement audios = conn.prepareStatement("SELECT * FROM AUDIOTRACKS WHERE FILEID = ?")) {
					audios.setInt(1, id);
					subrs = audios.executeQuery();
					while (subrs.next()) {
						DLNAMediaAudio audio = new DLNAMediaAudio();
						audio.setId(subrs.getInt("ID"));
						audio.setLang(subrs.getString("LANG"));
						audio.setAudioTrackTitleFromMetadata(subrs.getString("TITLE"));
						audio.getAudioProperties().setNumberOfChannels(subrs.getInt("NRAUDIOCHANNELS"));
						audio.setSampleFrequency(subrs.getString("SAMPLEFREQ"));
						audio.setCodecA(subrs.getString("CODECA"));
						audio.setBitsperSample(subrs.getInt("BITSPERSAMPLE"));
						audio.setAlbum(subrs.getString("ALBUM"));
						audio.setArtist(subrs.getString("ARTIST"));
						audio.setSongname(subrs.getString("SONGNAME"));
						audio.setGenre(subrs.getString("GENRE"));
						audio.setYear(subrs.getInt("YEAR"));
						audio.setTrack(subrs.getInt("TRACK"));
						audio.getAudioProperties().setAudioDelay(subrs.getInt("DELAY"));
						audio.setMuxingModeAudio(subrs.getString("MUXINGMODE"));
						audio.setBitRate(subrs.getInt("BITRATE"));
						media.getAudioTracksList().add(audio);
					}
					subrs.close();
				}
				try (PreparedStatement subs = conn.prepareStatement("SELECT * FROM SUBTRACKS WHERE FILEID = ?")) {
					subs.setInt(1, id);
					subrs = subs.executeQuery();
					while (subrs.next()) {
						DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
						sub.setId(subrs.getInt("ID"));
						sub.setLang(subrs.getString("LANG"));
						sub.setSubtitlesTrackTitleFromMetadata(subrs.getString("TITLE"));
						sub.setType(SubtitleType.valueOfStableIndex(subrs.getInt("TYPE")));
						media.getSubtitleTracksList().add(sub);
					}
					subrs.close();
				}

				list.add(media);
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
			return null;
		} finally {
			close(rs);
			close(stmt);
			close(conn);
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

	public synchronized void insertData(String name, long modified, int type, DLNAMediaInfo media) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getConnection();
			ps = conn.prepareStatement(
				"INSERT INTO FILES(FILENAME, MODIFIED, TYPE, DURATION, BITRATE, WIDTH, HEIGHT, SIZE, CODECV, " +
				"FRAMERATE, ASPECT, ASPECTRATIOCONTAINER, ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, " +
				"THUMB, CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, " +
				"TITLEVIDEOTRACK, VIDEOTRACKCOUNT, IMAGECOUNT, BITDEPTH) VALUES "+
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
						LOGGER.debug("Could not parse the bitrate from: " + name);
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
				ps.setString(15, left(media.getAvcLevel(), SIZE_AVC_LEVEL));
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
				ps.setString(9, null);
				ps.setString(10, null);
				ps.setString(11, null);
				ps.setString(12, null);
				ps.setString(13, null);
				ps.setByte(14, (byte) -1);
				ps.setString(15, null);
				ps.setInt(16, 0);
				ps.setNull(17, Types.OTHER);
				ps.setString(18, null);
				ps.setString(19, null);
				ps.setString(20, null);
				ps.setString(21, null);
				ps.setString(22, null);
				ps.setString(23, null);
				ps.setString(24, null);
				ps.setInt(25, 0);
				ps.setInt(26, 0);
				ps.setInt(27, 0);
			}
			ps.executeUpdate();
			int id;
			try (ResultSet rs = ps.getGeneratedKeys()) {
				id = -1;
				while (rs.next()) {
					id = rs.getInt(1);
				}
			}
			if (media != null && id > -1) {
				PreparedStatement insert = null;
				if (media.getAudioTracksList().size() > 0) {
					insert = conn.prepareStatement("INSERT INTO AUDIOTRACKS VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					for (DLNAMediaAudio audio : media.getAudioTracksList()) {
						insert.clearParameters();
						insert.setInt(1, id);
						insert.setInt(2, audio.getId());
						insert.setString(3, left(audio.getLang(), SIZE_LANG));
						insert.setString(4, left(audio.getAudioTrackTitleFromMetadata(), SIZE_TITLE));
						insert.setInt(5, audio.getAudioProperties().getNumberOfChannels());
						insert.setString(6, left(audio.getSampleFrequency(), SIZE_SAMPLEFREQ));
						insert.setString(7, left(audio.getCodecA(), SIZE_CODECA));
						insert.setInt(8, audio.getBitsperSample());
						insert.setString(9, left(trimToEmpty(audio.getAlbum()), SIZE_ALBUM));
						insert.setString(10, left(trimToEmpty(audio.getArtist()), SIZE_ARTIST));
						insert.setString(11, left(trimToEmpty(audio.getSongname()), SIZE_SONGNAME));
						insert.setString(12, left(trimToEmpty(audio.getGenre()), SIZE_GENRE));
						insert.setInt(13, audio.getYear());
						insert.setInt(14, audio.getTrack());
						insert.setInt(15, audio.getAudioProperties().getAudioDelay());
						insert.setString(16, left(trimToEmpty(audio.getMuxingModeAudio()), SIZE_MUXINGMODE));
						insert.setInt(17, audio.getBitRate());

						try {
							insert.executeUpdate();
						} catch (SQLException e) {
							if (e.getErrorCode() == 23505) {
								LOGGER.debug("A duplicate key error occurred while trying to store the following file's audio information in the database: " + name);
							} else {
								LOGGER.debug("An error occurred while trying to store the following file's audio information in the database: " + name);
							}
							LOGGER.debug("The error given by jdbc was: " + e);
						}
					}
				}

				if (media.getSubtitleTracksList().size() > 0) {
					insert = conn.prepareStatement("INSERT INTO SUBTRACKS VALUES (?, ?, ?, ?, ?)");
					for (DLNAMediaSubtitle sub : media.getSubtitleTracksList()) {
						if (sub.getExternalFile() == null) { // no save of external subtitles
							insert.clearParameters();
							insert.setInt(1, id);
							insert.setInt(2, sub.getId());
							insert.setString(3, left(sub.getLang(), SIZE_LANG));
							insert.setString(4, left(sub.getSubtitlesTrackTitleFromMetadata(), SIZE_TITLE));
							insert.setInt(5, sub.getType().getStableIndex());
							try {
								insert.executeUpdate();
							} catch (SQLException e) {
								if (e.getErrorCode() == 23505) {
									LOGGER.debug("A duplicate key error occurred while trying to store the following file's subtitle information in the database: " + name);
								} else {
									LOGGER.debug("An error occurred while trying to store the following file's subtitle information in the database: " + name);
								}
								LOGGER.debug("The error given by jdbc was: " + e);
							}
						}
					}
				}

				close(insert);
			}
		} catch (SQLException se) {
			if (se.getErrorCode() == 23505) {
				LOGGER.debug("Duplicate key while inserting this entry: " + name + " into the database: " + se.getMessage());
			} else {
				LOGGER.error(null, se);
			}
		} finally {
			close(ps);
			close(conn);
		}
	}

	public synchronized void updateThumbnail(String name, long modified, int type, DLNAMediaInfo media) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getConnection();
			ps = conn.prepareStatement("UPDATE FILES SET THUMB = ? WHERE FILENAME = ? AND MODIFIED = ?");
			ps.setString(2, name);
			ps.setTimestamp(3, new Timestamp(modified));
			if (media != null && media.getThumb() != null) {
				ps.setObject(1, media.getThumb());
			} else {
				ps.setNull(1, Types.OTHER);
			}
			ps.executeUpdate();
		} catch (SQLException se) {
			if (se.getErrorCode() == 23001) {
				LOGGER.debug("Duplicate key while inserting this entry: " + name + " into the database: " + se.getMessage());
			} else {
				LOGGER.error(null, se);
			}
		} finally {
			close(ps);
			close(conn);
		}
	}

	public ArrayList<String> getStrings(String sql) {
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

	public void cleanup() {
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

	public ArrayList<File> getFiles(String sql) {
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
