package net.pms.database;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class MediaTableFilesQueryTest {
	@org.junit.jupiter.api.BeforeAll
	public static void configure() throws Exception {
		System.setProperty(net.pms.PMS.PROPERTY_RUNNING_TESTS, "true");
		net.pms.PMS.setConfiguration(new net.pms.configuration.UmsConfiguration(false));
	}

	@Test
	public void oneQueryPreservesFilesAndUnfilteredSnapshot() throws Exception {
		var file = Files.createTempFile("ums-query-", ".mp4");
		try (Connection db = DriverManager.getConnection("jdbc:h2:mem:folder_query")) {
			db.createStatement().execute("CREATE TABLE FILES(ID INT, FILENAME VARCHAR, MODIFIED TIMESTAMP)");
			try (var insert = db.prepareStatement("INSERT INTO FILES VALUES(?, ?, ?)")) {
				for (int id = 1; id <= 4; id++) {
					insert.setInt(1, id);
					insert.setString(2, id == 3 ? file + ".missing" : file.toString());
					insert.setTimestamp(3, new Timestamp(file.toFile().lastModified() + (id == 4 ? 10000 : 0)));
					insert.executeUpdate();
				}
			}
			AtomicInteger queries = new AtomicInteger();
			Connection counted = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
				if (method.getName().equals("prepareStatement")) {
					queries.incrementAndGet();
				}
				try {
					return method.invoke(db, args);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			});
			for (String sql : new String[]{"SELECT * FROM FILES ORDER BY ID", "SELECT CASE WHEN ID < 3 THEN 'same' WHEN ID = 3 THEN NULL ELSE ' ' END AS SNAP, FILENAME, MODIFIED FROM FILES ORDER BY ID", "ID > 0", "SELECT * FROM FILES WHERE ID = 99"}) {
				var expectedFiles = MediaTableFiles.getFiles(db, sql);
				var expectedSnapshot = MediaTableFiles.getStrings(db, sql);
				queries.set(0);
				var result = MediaTableFiles.getFilesAndSnapshot(counted, sql);
				assertEquals(expectedFiles, result.files(), sql);
				assertEquals(expectedSnapshot, result.snapshot(), sql);
				assertEquals(1, queries.get(), sql);
			}
			var result = MediaTableFiles.getFilesAndSnapshot(db, "SELECT * FROM FILES ORDER BY ID");
			assertEquals(2, result.files().size(), "Valid duplicate files remain in order");
			assertEquals(4, result.snapshot().size(), "Missing and modified files remain in refresh snapshot");
		} finally {
			Files.deleteIfExists(file);
		}
	}
}
