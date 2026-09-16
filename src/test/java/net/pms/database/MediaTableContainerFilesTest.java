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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MediaTableContainerFilesTest {
	@BeforeAll
	static void initialize() throws Exception {
		TestHelper.setLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	void freshSchemaSupportsSharedFilesAndPreservesPairUniqueness() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:container_fresh")) {
			MediaTableTablesVersions.checkTable(connection);
			MediaTableContainerFiles.checkTable(connection);
			try (var statement = connection.createStatement()) {
				statement.execute("INSERT INTO CONTAINER_FILES VALUES (1, 10), (2, 10), (1, 20)");
				assertThrows(SQLException.class, () -> statement.execute("INSERT INTO CONTAINER_FILES VALUES (1, 10)"));
			}
			assertEquals(2, MediaTableTablesVersions.getTableVersion(connection, "CONTAINER_FILES"));
			assertTrue(MediaTableContainerFiles.isInContainer(connection, 10L));
			assertFalse(MediaTableContainerFiles.isInContainer(connection, 99L));
			assertNull(MediaTableContainerFiles.isInContainer(connection, null));
			assertEquals(java.util.List.of(10L, 20L), MediaTableContainerFiles.getContainerFileIds(connection, 1L));
			MediaTableContainerFiles.deleteEntry(connection, 10L);
			assertFalse(MediaTableContainerFiles.isInContainer(connection, 10L));
			assertTrue(MediaTableContainerFiles.isInContainer(connection, 20L));
			assertTrue(plan(connection, 99).contains("CONTAINER_FILES_FILEID_IDX"));
		}
	}

	@Test
	void migrationPreservesRowsAndRemovesFullScanForMissingFile() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:container_upgrade")) {
			legacySchema(connection);
			MediaTableTablesVersions.setTableVersion(connection, "CONTAINER_FILES", 1);
			String before = plan(connection, -1);
			MediaTableContainerFiles.checkTable(connection);
			MediaTableContainerFiles.checkTable(connection);
			String after = plan(connection, -1);
			assertTrue(after.contains("CONTAINER_FILES_FILEID_IDX"), after);
			long beforeScans = scans(before);
			long afterScans = scans(after);
			assertTrue(beforeScans >= 10000, before);
			assertTrue(afterScans < 10, after);
			System.out.printf("CONTAINER_INDEX rows=10000 missing-file scanCount before=%d after=%d%n", beforeScans, afterScans);
			try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM CONTAINER_FILES")) {
				assertTrue(rows.next());
				assertEquals(10000, rows.getInt(1));
			}
			assertTrue(MediaTableContainerFiles.isInContainer(connection, 10000L));
			assertFalse(MediaTableContainerFiles.isInContainer(connection, -1L));
			assertEquals(2, MediaTableTablesVersions.getTableVersion(connection, "CONTAINER_FILES"));
		}
	}

	@Test
	void unversionedLegacySchemaAndAlreadyCreatedIndexCanBeUpgraded() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:container_unversioned")) {
			legacySchema(connection);
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE INDEX CONTAINER_FILES_FILEID_IDX ON CONTAINER_FILES(FILEID)");
			}
			MediaTableContainerFiles.checkTable(connection);
			assertEquals(2, MediaTableTablesVersions.getTableVersion(connection, "CONTAINER_FILES"));
			assertTrue(MediaTableContainerFiles.isInContainer(connection, 1L));
		}
	}

	private static void legacySchema(Connection connection) throws Exception {
		MediaTableTablesVersions.checkTable(connection);
		try (var statement = connection.createStatement()) {
			statement.execute("CREATE TABLE CONTAINER_FILES(ID BIGINT, FILEID BIGINT)");
			statement.execute("CREATE UNIQUE INDEX CONTAINER_FILES_ID_FILEID_IDX ON CONTAINER_FILES(ID, FILEID)");
			statement.execute("INSERT INTO CONTAINER_FILES SELECT X, X FROM SYSTEM_RANGE(1, 10000)");
		}
	}

	private static String plan(Connection connection, long id) throws Exception {
		try (var statement = connection.prepareStatement("EXPLAIN ANALYZE SELECT * FROM CONTAINER_FILES WHERE FILEID = ? LIMIT 1")) {
			statement.setLong(1, id);
			try (var rows = statement.executeQuery()) {
				assertTrue(rows.next());
				return rows.getString(1);
			}
		}
	}

	private static long scans(String plan) {
		var matcher = Pattern.compile("scanCount: ([0-9]+)").matcher(plan);
		assertTrue(matcher.find(), plan);
		return Long.parseLong(matcher.group(1));
	}
}
