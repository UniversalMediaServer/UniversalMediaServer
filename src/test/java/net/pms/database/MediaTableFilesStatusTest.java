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
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MediaTableFilesStatusTest {
	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 * @throws InterruptedException
	 */
	@BeforeEach
	public final void setUp() throws ConfigurationException, InterruptedException {
		TestHelper.SetLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	/**
	 * Ensures that the table updates properly.
	 * todo: This should get more specific, since for now it
	 * just makes sure the code completes without errors but
	 * doesn't really do anything.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testUpgrade() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			//remove all tables to cleanup db
			MediaDatabase.dropAllTables(connection);
			database.checkTables(true);
			MediaDatabase.dropTableAndConstraint(connection, MediaTableFilesStatus.TABLE_NAME);
			MediaDatabase.execute(connection,
				"CREATE TABLE " + MediaTableFilesStatus.TABLE_NAME + "(" +
					"ID            IDENTITY PRIMARY KEY, " +
					"FILENAME      VARCHAR(1024)        NOT NULL UNIQUE, " +
					"MODIFIED      DATETIME, " +
					"ISFULLYPLAYED BOOLEAN DEFAULT false, " +
					"CONSTRAINT filename_match FOREIGN KEY(FILENAME) " +
						"REFERENCES " + MediaTableFiles.TABLE_NAME + "(FILENAME) " +
						"ON DELETE CASCADE" +
				")",
				"CREATE UNIQUE INDEX FILENAME_IDX ON " + MediaTableFilesStatus.TABLE_NAME + "(FILENAME)",
				"CREATE INDEX ISFULLYPLAYED_IDX ON " + MediaTableFilesStatus.TABLE_NAME + "(ISFULLYPLAYED)"
			);
			MediaTableTablesVersions.setTableVersion(connection, MediaTableFilesStatus.TABLE_NAME, 7);

			/*
			 * Version 7 is created, so now we can update to the latest version
			 * and any errors that occur along the way will cause the test to fail.
			 */
			database.checkTables(true);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}

	@Test
	public void testIsFullyPlayed() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			MediaTableFilesStatus.setFullyPlayed(connection, "FileThatHasBeenPlayed", 0, true);
			MediaTableFilesStatus.setFullyPlayed(connection, "FileThatHasBeenMarkedNotPlayed", 0, false);
			assertNull(MediaTableFilesStatus.isFullyPlayed(connection, "FileThatDoesntExist", 0));
			assertTrue(MediaTableFilesStatus.isFullyPlayed(connection, "FileThatHasBeenPlayed", 0));
			assertNull(MediaTableFilesStatus.isFullyPlayed(connection, "FileThatHasBeenPlayed", 1));
			assertFalse(MediaTableFilesStatus.isFullyPlayed(connection, "FileThatHasBeenMarkedNotPlayed", 0));
		}
	}
}
