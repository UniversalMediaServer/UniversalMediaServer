package net.pms.store;

import java.sql.DriverManager;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MediaStoreIdsBatchTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void sharedAncestorsAndRepeatedNamesAreBumpedOnce() throws Exception {
		try (var db = DriverManager.getConnection("jdbc:h2:mem:"); var sql = db.createStatement()) {
			sql.execute("CREATE TABLE STORE_IDS (ID BIGINT PRIMARY KEY, PARENT_ID BIGINT, NAME VARCHAR, OBJECT_TYPE VARCHAR, UPDATE_ID BIGINT)");
			sql.execute("INSERT INTO STORE_IDS VALUES " +
				"(-1,0,'system','System',0),(1,0,'library','Folder',0),(2,1,'folder','Folder',0)," +
				"(3,2,'a.mp4','RealFile',0),(4,2,'b.mp4','RealFile',0)," +
				"(5,1,'recent','Folder',0),(6,5,'a.mp4','RealFile',0),(99,0,'unrelated','Folder',0)");
			long before = MediaStoreIds.getSystemUpdateId().getValue();
			MediaStoreIds.incrementUpdateIdsForFilenamesWithAncestors(db, List.of("a.mp4", "b.mp4", "a.mp4", "missing"));
			long after = MediaStoreIds.getSystemUpdateId().getValue();
			assertEquals(6, after - before, "Six distinct resources/ancestors, including both copies of a.mp4");
			try (var rows = sql.executeQuery("SELECT ID, UPDATE_ID FROM STORE_IDS")) {
				while (rows.next()) {
					long id = rows.getLong(1);
					long update = rows.getLong(2);
					if (id == 99) {
						assertEquals(0, update);
					} else if (id == -1) {
						assertEquals(after, update);
					} else {
						assertTrue(update > before && update <= after, "Changed ID " + id);
					}
				}
			}
			MediaStoreIds.incrementUpdateIdsForFilenamesWithAncestors(db, List.of());
			assertEquals(after, MediaStoreIds.getSystemUpdateId().getValue());
		} finally {
			MediaStoreIds.drainChangedIds();
		}
	}
}
