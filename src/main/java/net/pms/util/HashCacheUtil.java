package net.pms.util;

import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableSubtitleHashCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HashCacheUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HashCacheUtil.class);
    private static final MediaDatabase mediaDatabase = PMS.get().getMediaDatabase();

    /**
     * Gets the cached hash of a subtitle file from the datastore.
     * @param file the {@link Path} for which to get the hash.
     * @return The saved Hash or {@code null}.
     */
    public static String getHashFromCache(Path file) {
        try (Connection connection = mediaDatabase.getConnection()) {
            String sqlStatement = "SELECT " + MediaTableSubtitleHashCache.COL_HASH + " FROM " +
                    MediaTableSubtitleHashCache.TABLE_NAME + " WHERE " +
                    MediaTableSubtitleHashCache.COL_FILE_NAME + "=?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
                preparedStatement.setString(1, file.getFileName().toString());
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString(1);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.debug("An exception occurred while reading hash from cache: ", e);
        }

        return null;
    }

    /**
     * Saves the computed hash to the datastore to avoid repeated computations.
     * @param file the {@link Path} for which to get the hash.
     * @param hash the hash computed through the
     * computeHash method.
     */
    public static void addHashToCache(Path file, String hash) {
        try (Connection connection = mediaDatabase.getConnection()) {
            String sql = "INSERT INTO " + MediaTableSubtitleHashCache.TABLE_NAME + " (" +
                    MediaTableSubtitleHashCache.COL_FILE_NAME + "," + MediaTableSubtitleHashCache.COL_HASH +
                    ") VALUES (?,?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, file.getFileName().toString());
                preparedStatement.setString(2, hash);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.debug("An exception occurred while saving hash to cache: ",e);
        }
    }
}
