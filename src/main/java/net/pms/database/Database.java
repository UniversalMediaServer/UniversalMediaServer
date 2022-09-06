/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
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

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import net.pms.Messages;
import net.pms.gui.GuiManager;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public abstract class Database extends DatabaseHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

	private final Boolean embedded;
	private final HikariDataSource ds;
	private final String dbName;

	protected DatabaseStatus status;

	/**
	 * Initializes the database connection pool for the current profile.
	 *
	 * Will create the "UMS-tests" profile directory and put the database
	 * in there if it doesn't exist, in order to prevent overwriting
	 * real databases.
	 *
	 * @param name the database name
	 */
	public Database(String name) {
		dbName = name;
		status = DatabaseStatus.CLOSED;
		embedded = true;
		String jdbcUrl = DatabaseEmbedded.getJdbcUrl(name);
		ds = new HikariDataSource();
		ds.setJdbcUrl(jdbcUrl);
		ds.setUsername(DatabaseEmbedded.getDbUser());
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
		return ds.getConnection();
	}

	public int getActiveConnections() throws SQLException {
		return ds.isRegisterMbeans() ? ds.getHikariPoolMXBean().getActiveConnections() : 0;
	}

	public boolean isOpened() {
		return status == DatabaseStatus.OPENED;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	/**
	 * Initialized the database for use, performing checks and creating a new
	 * database if necessary.
	 *
	 * @param force whether to recreate the database regardless of necessity.
	 */
	public synchronized void init(boolean force) {
		if (embedded) {
			DatabaseEmbedded.deleteDatabaseLock(dbName);
		}
		open();
		switch (status) {
			case OPENED -> onOpening(force);
			case OPENFAILED -> onOpeningFail(force);
		}
	}

	private void open() {
		status = DatabaseStatus.OPENING;
		Connection conn = null;
		boolean needRetry;
		try {
			conn = getConnection();
			status = DatabaseStatus.OPENED;
		} catch (SQLException se) {
			status = DatabaseStatus.OPENFAILED;
			if (embedded) {
				needRetry = DatabaseEmbedded.openFailed(dbName, se);
			} else {
				LOGGER.debug("Database connection error, retrying in 10 seconds");
				UMSUtils.sleep(10000);
				needRetry = true;
			}
			if (needRetry) {
				try {
					conn = getConnection();
					status = DatabaseStatus.OPENED;
				} catch (SQLException se2) {
					showMessageDialog("TheLocalCacheCouldNotStarted", null);
					LOGGER.debug("", se2);
				}
			}
		} finally {
			if (embedded && conn != null) {
				DatabaseEmbedded.checkTableStorageType(conn, dbName);
			}
			close(conn);
		}
	}

	abstract void onOpening(boolean force);

	abstract void onOpeningFail(boolean force);

	public void close() {
		status = DatabaseStatus.CLOSING;
		try {
			Thread.sleep(50);
			int activeConnections = getActiveConnections();
			int maxAttempts = 10;
			int curAttempts = 1;
			//allow threads to finish
			while (activeConnections > 0 && curAttempts <= maxAttempts) {
				LOGGER.trace("Database shutdown waiting 500 ms ({}/{}) for {} connections to close", curAttempts, maxAttempts, activeConnections);
				Thread.sleep(500);
				activeConnections = getActiveConnections();
				curAttempts++;
			}
			if (activeConnections > 0) {
				LOGGER.debug("Database shutdown will kill remaining connections ({}), db errors may occurs", activeConnections);
			}
		} catch (SQLException e) {
			LOGGER.error("Waiting DB connections", e);
		} catch (InterruptedException e) {
			LOGGER.debug("Interrupted while shutting down database..");
			LOGGER.trace("", e);
		}

		if (embedded) {
			try {
				Connection con = getConnection();
				DatabaseEmbedded.shutdown(con);
			} catch (SQLException ex) {
				LOGGER.error("shutdown DB ", ex);
			}
		}
		ds.close();
		status = DatabaseStatus.CLOSED;
	}

	public String getDatabaseFilename() {
		return embedded ? DatabaseEmbedded.getDatabaseFilename(dbName) : "";
	}

	public void createDatabaseReport() {
		if (embedded && CONFIGURATION.getDatabaseLogging()) {
			DatabaseEmbedded.createDatabaseReport(dbName);
		}
	}

	public static void showMessageDialog(String message, String dbDir) {
		GuiManager.showErrorMessage(String.format(Messages.getString(message), dbDir), Messages.getString("Error"));
	}

	public enum DatabaseStatus {
		OPENING,
		OPENED,
		OPENFAILED,
		CLOSING,
		CLOSED
	}
}
