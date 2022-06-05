package net.pms.network.mediaserver.javahttpserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.util.LoginDetails;

public class UserService {
	public static final String TABLE_NAME = "USERS";
	private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	public static LoginDetails getUserByUsername(final Connection connection, final String username) {
		LoginDetails result;
		LOGGER.info("Finding user: username: {} ", username);
		try {
			String sql = "SELECT * " +
				"FROM " + TABLE_NAME + " " +
				"WHERE USERNAME" + "='" + username + "' " +
				"LIMIT 1";
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				while (resultSet.next()) {
					String password = resultSet.getString("PASSWORD");
					result = new LoginDetails();
					result.setUsername(username);
					result.setPassword(password);
					return result;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error finding user: " + e);
		}

		return null;
	}

	public static void createUser(final Connection connection, final String username, final String password) {
		try {
			LOGGER.info("Creating user: {}", username);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO " + TABLE_NAME + "(USERNAME, PASSWORD) " + "VALUES(?, ?)",
				Statement.RETURN_GENERATED_KEYS);

				insertStatement.clearParameters();
				insertStatement.setString(1, left(username, 255));
				insertStatement.setString(2, left(password, 255));
				insertStatement.executeUpdate();
				try (ResultSet rs2 = insertStatement.getGeneratedKeys()) {
					if (rs2.next()) {
						LOGGER.info("Created user successfully in " + TABLE_NAME);
					}
				}
		} catch (Exception e) {
			LOGGER.error("ERROR createUser" + e);
		}
		return;
	}
}
