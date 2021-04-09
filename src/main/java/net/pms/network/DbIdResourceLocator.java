package net.pms.network;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.PlaylistFolder;
import net.pms.dlna.RealFileDbId;
import net.pms.dlna.virtual.MediaLibraryFolder;

public class DbIdResourceLocator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbIdResourceLocator.class);
	private DLNAMediaDatabase database;

	public DbIdResourceLocator() {
		this.database = PMS.get().getDatabase();
	}

	public DLNAResource locateResource(Long id) {
		return getDLNAResourceFromSQL(id);
	}

	private DLNAResource getDLNAResourceFromSQL(Long id) {
		DLNAResource res = null;
		try (Connection connection = database.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(String.format("select FILENAME, TYPE from files where id = %d", id))) {
					if (resultSet.next()) {
						switch (resultSet.getInt("TYPE")) {
							case MediaLibraryFolder.MOVIE_FOLDERS:
								res = new PlaylistFolder(new File(resultSet.getString("FILENAME")));
								break;
							default:
								res = new RealFileDbId(new File(resultSet.getString("FILENAME")));
								break;
						}
						res.resolve();
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("", e);
		}
		return res;
	}
}
