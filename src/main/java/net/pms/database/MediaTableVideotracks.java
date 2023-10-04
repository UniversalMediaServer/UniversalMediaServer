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
package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.pms.media.MediaInfo;
import net.pms.media.video.MediaVideo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the VideoTracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableVideotracks extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideotracks.class);
	protected static final String TABLE_NAME = "VIDEOTRACKS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	protected static final String COL_FILEID = MediaTableFiles.CHILD_ID;
	private static final String COL_LANG = "LANG";
	private static final String COL_STREAMID = "STREAMID";
	private static final String COL_DEFAULT_FLAG = "DEFAULT_FLAG";
	private static final String COL_FORCED_FLAG = "FORCED_FLAG";
	private static final String COL_OPTIONALID = "OPTIONALID";
	private static final String COL_WIDTH = "WIDTH";
	private static final String COL_HEIGHT = "HEIGHT";
	private static final String COL_DISPLAYASPECTRATIO = "DISPLAYASPECTRATIO";
	private static final String COL_ODISPLAYASPECTRATIO = "ODISPLAYASPECTRATIO";
	private static final String COL_CODEC = "CODEC";
	private static final String COL_FRAMERATE = "FRAMERATE";
	private static final String COL_FORMATPROFILE = "FORMATPROFILE";
	private static final String COL_FORMATLEVEL = "FORMATLEVEL";
	private static final String COL_FORMATTIER = "FORMATTIER";
	private static final String COL_MATRIXCOEFFICIENTS = "MATRIXCOEFFICIENTS";
	private static final String COL_MUXINGMODE = "MUXINGMODE";
	private static final String COL_BITDEPTH = "BITDEPTH";
	private static final String COL_REFRAMES = "REFRAMES";
	private static final String COL_HDRFORMAT = "HDRFORMAT";
	private static final String COL_HDRFORMATCOMPATIBILITY = "HDRFORMATCOMPATIBILITY";
	private static final String COL_PIXELASPECTRATIO = "PIXELASPECTRATIO";
	private static final String COL_SCANTYPE = "SCANTYPE";
	private static final String COL_SCANORDER = "SCANORDER";
	private static final String COL_MULTIVIEW_LAYOUT = "MULTIVIEW_LAYOUT";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_DURATION = "DURATION";
	private static final String COL_BITRATE = "BITRATE";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	private static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	private static final String TABLE_COL_WIDTH = TABLE_NAME + "." + COL_WIDTH;
	private static final String TABLE_COL_HEIGHT = TABLE_NAME + "." + COL_HEIGHT;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_BY_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_BY_FILEID_ID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_DELETE_BY_FILEID_ID_GREATER_OR_EQUAL = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + TABLE_COL_ID + GREATER_OR_EQUAL_THAN + PARAMETER;
	public static final String SQL_GET_FILEID_BY_VIDEOHD = SELECT + TABLE_COL_FILEID + FROM + TABLE_NAME + WHERE + TABLE_COL_WIDTH + " > 864" + OR + TABLE_COL_HEIGHT + " > 576";
	public static final String SQL_GET_FILEID_BY_VIDEOSD = SELECT + TABLE_COL_FILEID + FROM + TABLE_NAME + WHERE + TABLE_COL_WIDTH + " < 865" + AND + TABLE_COL_HEIGHT + " < 577";
	public static final String SQL_GET_FILEID_BY_IS3D = SELECT + TABLE_COL_FILEID + FROM + TABLE_NAME + WHERE + COL_MULTIVIEW_LAYOUT + " != ''";

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		if (tableExists(connection, TABLE_NAME)) {
			Integer version = MediaTableTablesVersions.getTableVersion(connection, TABLE_NAME);
			if (version == null) {
				version = 1;
			}
			if (version < TABLE_VERSION) {
				upgradeTable(connection, version);
			} else if (version > TABLE_VERSION) {
				LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB, DATABASE_NAME, TABLE_NAME, DATABASE.getDatabaseFilename());
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				default -> {
					throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
				}
			}
		}
		try {
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		} catch (SQLException e) {
			LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
			LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_ID                      + INTEGER               + NOT_NULL               + COMMA +
				COL_FILEID                  + BIGINT                + NOT_NULL               + COMMA +
				COL_LANG                    + VARCHAR                                        + COMMA +
				COL_STREAMID                + INTEGER                                        + COMMA +
				COL_OPTIONALID              + BIGINT                                         + COMMA +
				COL_DEFAULT_FLAG            + BOOLEAN               + DEFAULT + FALSE        + COMMA +
				COL_FORCED_FLAG             + BOOLEAN               + DEFAULT + FALSE        + COMMA +
				COL_TITLE                   + VARCHAR                                        + COMMA +
				COL_WIDTH                   + INTEGER                                        + COMMA +
				COL_HEIGHT                  + INTEGER                                        + COMMA +
				COL_CODEC                   + VARCHAR                                        + COMMA +
				COL_FORMATPROFILE           + VARCHAR                                        + COMMA +
				COL_FORMATLEVEL             + VARCHAR                                        + COMMA +
				COL_FORMATTIER              + VARCHAR                                        + COMMA +
				COL_DURATION                + DOUBLE_PRECISION                               + COMMA +
				COL_BITRATE                 + INTEGER                                        + COMMA +
				COL_BITDEPTH                + INTEGER                                        + COMMA +
				COL_FRAMERATE               + DOUBLE_PRECISION                               + COMMA +
				COL_MATRIXCOEFFICIENTS      + VARCHAR                                        + COMMA +
				COL_MUXINGMODE              + VARCHAR                                        + COMMA +
				COL_HDRFORMAT               + VARCHAR                                        + COMMA +
				COL_HDRFORMATCOMPATIBILITY  + VARCHAR                                        + COMMA +
				COL_PIXELASPECTRATIO        + DOUBLE_PRECISION                               + COMMA +
				COL_MULTIVIEW_LAYOUT        + VARCHAR                                        + COMMA +
				COL_REFRAMES                + TINYINT                                        + COMMA +
				COL_DISPLAYASPECTRATIO      + VARCHAR                                        + COMMA +
				COL_ODISPLAYASPECTRATIO     + VARCHAR                                        + COMMA +
				COL_SCANTYPE                + VARCHAR                                        + COMMA +
				COL_SCANORDER               + VARCHAR                                        + COMMA +
				CONSTRAINT + TABLE_NAME + PK_MARKER + PRIMARY_KEY + "(" + COL_FILEID + COMMA + COL_ID + ")" + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableFiles.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_WIDTH + CONSTRAINT_SEPARATOR + COL_HEIGHT + IDX_MARKER + ON + TABLE_NAME + "(" + COL_WIDTH + COMMA + COL_HEIGHT + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MULTIVIEW_LAYOUT + IDX_MARKER + ON + TABLE_NAME + "(" + COL_MULTIVIEW_LAYOUT + ")"
		);
	}

	protected static void insertOrUpdateVideoTracks(Connection connection, long fileId, MediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || !media.hasVideoTrack()) {
			return;
		}

		int trackCount = media.getVideoTrackCount();
		try (
			PreparedStatement updateStatment = connection.prepareStatement(SQL_DELETE_BY_FILEID_ID_GREATER_OR_EQUAL);
		) {
			updateStatment.setLong(1, fileId);
			updateStatment.setInt(2, trackCount);
			updateStatment.executeUpdate();
		}

		if (trackCount == 0) {
			return;
		}

		try (
			PreparedStatement updateStatement = connection.prepareStatement(SQL_GET_ALL_BY_FILEID_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			for (MediaVideo videoTrack : media.getVideoTracks()) {
				updateStatement.clearParameters();
				updateStatement.setLong(1, fileId);
				updateStatement.setInt(2, videoTrack.getId());
				try (ResultSet result = updateStatement.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						result.moveToInsertRow();
						result.updateLong(COL_FILEID, fileId);
						result.updateInt(COL_ID, videoTrack.getId());
					}
					updateVideoTrack(result, videoTrack);
					if (isCreatingNewRecord) {
						result.insertRow();
					} else {
						result.updateRow();
					}
				}
			}
		}
	}

	private static void updateVideoTrack(ResultSet result, MediaVideo videoTrack) throws SQLException {
		result.updateString(COL_LANG, StringUtils.left(videoTrack.getLang(), SIZE_LANG));
		updateInteger(result, COL_STREAMID, videoTrack.getStreamOrder());
		updateLong(result, COL_OPTIONALID, videoTrack.getOptionalId());
		result.updateBoolean(COL_DEFAULT_FLAG, videoTrack.isDefault());
		result.updateBoolean(COL_FORCED_FLAG, videoTrack.isForced());
		result.updateInt(COL_WIDTH, videoTrack.getWidth());
		result.updateInt(COL_HEIGHT, videoTrack.getHeight());
		updateDouble(result, COL_DURATION, videoTrack.getDuration());
		result.updateString(COL_DISPLAYASPECTRATIO, videoTrack.getDisplayAspectRatio());
		result.updateString(COL_ODISPLAYASPECTRATIO, videoTrack.getOriginalDisplayAspectRatio());
		updateDouble(result, COL_PIXELASPECTRATIO, videoTrack.getPixelAspectRatio());
		result.updateString(COL_CODEC, videoTrack.getCodec());
		result.updateString(COL_FORMATPROFILE, videoTrack.getFormatProfile());
		result.updateString(COL_FORMATLEVEL, videoTrack.getFormatLevel());
		result.updateString(COL_FORMATTIER, videoTrack.getFormatTier());
		updateDouble(result, COL_FRAMERATE, videoTrack.getFrameRate());
		result.updateInt(COL_BITRATE, videoTrack.getBitRate());
		result.updateInt(COL_BITDEPTH, videoTrack.getBitDepth());
		result.updateString(COL_MATRIXCOEFFICIENTS, videoTrack.getMatrixCoefficients());
		result.updateString(COL_MUXINGMODE, videoTrack.getMuxingMode());
		result.updateByte(COL_REFRAMES, videoTrack.getReferenceFrameCount());
		result.updateString(COL_HDRFORMAT, videoTrack.getHDRFormat());
		result.updateString(COL_HDRFORMATCOMPATIBILITY, videoTrack.getHDRFormatCompatibility());
		MediaVideo.ScanType scanType = videoTrack.getScanType();
		result.updateString(COL_SCANTYPE, scanType != null ? scanType.toString() : null);
		MediaVideo.ScanOrder scanOrder = videoTrack.getScanOrder();
		result.updateString(COL_SCANORDER, scanOrder != null ? scanOrder.toString() : null);
		result.updateString(COL_MULTIVIEW_LAYOUT, videoTrack.getMultiViewLayout());
		result.updateString(COL_TITLE, videoTrack.getTitle());
	}

	protected static List<MediaVideo> getVideoTracks(Connection connection, long fileId) {
		List<MediaVideo> result = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_BY_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet resultset = stmt.executeQuery()) {
				while (resultset.next()) {
					MediaVideo videoTrack = getVideoTrack(resultset);
					LOGGER.trace("Adding video from the database: {}", videoTrack.toString());
					result.add(videoTrack);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	private static MediaVideo getVideoTrack(ResultSet resultset) throws SQLException {
		MediaVideo result = new MediaVideo();
		result.setId(resultset.getInt(COL_ID));
		result.setLang(resultset.getString(COL_LANG));
		result.setStreamOrder(toInteger(resultset, COL_STREAMID));
		result.setOptionalId(toLong(resultset, COL_OPTIONALID));
		result.setDefault(resultset.getBoolean(COL_DEFAULT_FLAG));
		result.setForced(resultset.getBoolean(COL_FORCED_FLAG));
		result.setWidth(resultset.getInt(COL_WIDTH));
		result.setHeight(resultset.getInt(COL_HEIGHT));
		result.setDisplayAspectRatio(resultset.getString(COL_DISPLAYASPECTRATIO));
		result.setOriginalDisplayAspectRatio(resultset.getString(COL_ODISPLAYASPECTRATIO));
		result.setCodec(resultset.getString(COL_CODEC));
		result.setFormatProfile(resultset.getString(COL_FORMATPROFILE));
		result.setFormatLevel(resultset.getString(COL_FORMATLEVEL));
		result.setFormatTier(resultset.getString(COL_FORMATTIER));
		result.setFrameRate(toDouble(resultset, COL_FRAMERATE));
		result.setMatrixCoefficients(resultset.getString(COL_MATRIXCOEFFICIENTS));
		result.setMuxingMode(resultset.getString(COL_MUXINGMODE));
		result.setBitDepth(resultset.getInt(COL_BITDEPTH));
		result.setReferenceFrameCount(resultset.getByte(COL_REFRAMES));
		result.setHDRFormat(resultset.getString(COL_HDRFORMAT));
		result.setHDRFormatCompatibility(resultset.getString(COL_HDRFORMATCOMPATIBILITY));
		result.setPixelAspectRatio(toDouble(resultset, COL_PIXELASPECTRATIO));
		result.setScanType(resultset.getString(COL_SCANTYPE));
		result.setScanOrder(resultset.getString(COL_SCANORDER));
		result.setMultiViewLayout(resultset.getString(COL_MULTIVIEW_LAYOUT));
		result.setTitle(resultset.getString(COL_TITLE));
		result.setDuration(toDouble(resultset, COL_DURATION));
		result.setBitRate(resultset.getInt(COL_BITRATE));
		return result;
	}

}
