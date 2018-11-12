/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.sql.Connection;
import java.sql.Statement;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import org.apache.commons.configuration.ConfigurationException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableFilesStatusTest {
	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@Before
	public final void setUp() throws ConfigurationException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
		PMS.get();
		PMS.setConfiguration(new PmsConfiguration(false));
	}

	/**
	 * This should get more specific, since for now it
	 * just makes sure the code completes without errors but
	 * doesn't really do anything.
	 */
	@Test
	public void testUpgrade() throws Exception {
		DLNAMediaDatabase database = PMS.get().getDatabase();
		try (Connection connection = database.getConnection()) {
			if (!Tables.tableExists(connection, "TABLES")) {
				Tables.createTablesTable(connection);
			}

			try (Statement statement = connection.createStatement()) {
				Tables.dropTable(connection, TableFilesStatus.TABLE_NAME);

				// Create version 7 of this table
				statement.execute(
					"CREATE TABLE " + TableFilesStatus.TABLE_NAME + "(" +
						"ID            IDENTITY PRIMARY KEY, " +
						"FILENAME      VARCHAR2(1024)        NOT NULL, " +
						"MODIFIED      DATETIME, " +
						"ISFULLYPLAYED BOOLEAN DEFAULT false, " +
						"CONSTRAINT filename_match FOREIGN KEY(FILENAME) " +
							"REFERENCES FILES(FILENAME) " +
							"ON DELETE CASCADE" +
					")"
				);
	
				statement.execute("CREATE UNIQUE INDEX FILENAME_IDX ON " + TableFilesStatus.TABLE_NAME + "(FILENAME)");
				statement.execute("CREATE INDEX ISFULLYPLAYED_IDX ON " + TableFilesStatus.TABLE_NAME + "(ISFULLYPLAYED)");

				Tables.setTableVersion(connection, TableFilesStatus.TABLE_NAME, 7);
			}

			/*
			 * Version 7 is created, so now we can update to the latest version
			 * and any errors that occur along the way will cause the test to fail.
			 */
			TableFilesStatus.checkTable(connection);
		}
	}

	@Test
	public void testIsFullyPlayed() throws Exception {
		TableFilesStatus.setFullyPlayed("FileThatHasBeenPlayed", true);
		TableFilesStatus.setFullyPlayed("FileThatHasBeenMarkedNotPlayed", false);
		assertThat(TableFilesStatus.isFullyPlayed("FileThatDoesntExist")).isNull();
		assertThat(TableFilesStatus.isFullyPlayed("FileThatHasBeenPlayed")).isTrue();
		assertThat(TableFilesStatus.isFullyPlayed("FileThatHasBeenMarkedNotPlayed")).isFalse();
	}
}
