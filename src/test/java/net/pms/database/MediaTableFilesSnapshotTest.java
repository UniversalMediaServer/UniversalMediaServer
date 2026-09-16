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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MediaTableFilesSnapshotTest {
	@TempDir
	Path directory;

	@BeforeEach
	void setUp() throws Exception {
		TestHelper.setLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	void oneQueryPreservesUnfilteredFirstColumnAndValidFileDuplicates() throws Exception {
		File valid = Files.writeString(directory.resolve("valid"), "data").toFile();
		File changed = Files.writeString(directory.resolve("changed"), "data").toFile();
		File missing = directory.resolve("missing").toFile();
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:file_snapshot")) {
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE TABLE FILES (LABEL VARCHAR, FILENAME VARCHAR, MODIFIED TIMESTAMP)");
			}
			try (var statement = connection.prepareStatement("INSERT INTO FILES VALUES (?, ?, ?)")) {
				File[] files = {valid, missing, changed, valid};
				String[] labels = {"first", "", "changed", "first"};
				for (int i = 0; i < files.length; i++) {
					statement.setString(1, labels[i]);
					statement.setString(2, files[i].getAbsolutePath());
					statement.setTimestamp(3, new Timestamp(files[i].lastModified() + (i == 2 ? 1000 : 0)));
					statement.executeUpdate();
				}
			}
			String sql = "SELECT LABEL, FILENAME, MODIFIED FROM FILES ORDER BY FILENAME";
			List<File> expectedFiles = MediaTableFiles.getFiles(connection, sql);
			List<String> expectedSnapshot = MediaTableFiles.getStrings(connection, sql);
			AtomicInteger queries = new AtomicInteger();
			Connection counting = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
					new Class<?>[]{Connection.class}, (proxy, method, args) -> {
						if (method.getName().equals("prepareStatement")) {
							queries.incrementAndGet();
						}
						try {
							return method.invoke(connection, args);
						} catch (InvocationTargetException e) {
							throw e.getCause();
						}
					});
			var result = MediaTableFiles.getFilesWithSnapshot(counting, sql);
			assertEquals(1, queries.get());
			assertEquals(List.of(valid, valid), result.files());
			assertEquals(expectedFiles, result.files());
			assertEquals(expectedSnapshot, result.snapshot());
			assertEquals(3, result.snapshot().size());
			var implicitQuery = MediaTableFiles.getFilesWithSnapshot(connection, "1=1");
			assertEquals(MediaTableFiles.getFiles(connection, "1=1"), implicitQuery.files());
			assertEquals(MediaTableFiles.getStrings(connection, "1=1"), implicitQuery.snapshot());
		}
	}
}
