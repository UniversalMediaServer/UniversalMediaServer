package net.pms.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.SQLException;
import net.pms.database.UserTableUsers;

public class UserService {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	public static LoginDetails getUserByUsername(final Connection connection, final String username) {
		LoginDetails result;
		LOGGER.info("Finding user: {} ", username);
		try {
			String sql = "SELECT * " +
					"FROM " + UserTableUsers.TABLE_NAME + " " +
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
		} catch (SQLException e) {
			LOGGER.error("Error finding user: " + e);
		}
		return null;
	}

	public static void createUser(final Connection connection, final String username, final String password) {
		try {
			LOGGER.info("Creating user: {}", username);
			PreparedStatement insertStatement = connection.prepareStatement(
					"INSERT INTO " + UserTableUsers.TABLE_NAME + "(USERNAME, PASSWORD) " + "VALUES(?, ?)",
					Statement.RETURN_GENERATED_KEYS);

			insertStatement.clearParameters();
			insertStatement.setString(1, left(username, 255));
			insertStatement.setString(2, left(hashPassword(password), 255));
			insertStatement.executeUpdate();
			try (ResultSet rs2 = insertStatement.getGeneratedKeys()) {
				if (rs2.next()) {
					LOGGER.info("Created user successfully in " + UserTableUsers.TABLE_NAME);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("ERROR createUser" + e);
		}
	}

	public static boolean validatePassword(String password, String dbPasswordHash) {
		BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), dbPasswordHash);
		return result.verified;
	}

	public static String hashPassword(String passwordToHash) {
		String bcryptHashString = BCrypt.withDefaults().hashToString(12, passwordToHash.toCharArray());
		return bcryptHashString;
	}

	public static void updatePassword(final Connection connection, final String newPassword, final String username) {
		try {
			LOGGER.info("Updating password for {}", username);
			Statement statement = connection.createStatement();
			String sql = "UPDATE " + UserTableUsers.TABLE_NAME + " " +
					"SET PASSWORD='" + hashPassword(newPassword) + "' " +
					"WHERE USERNAME='" + username + "'";
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			LOGGER.error("Error updatePassword:{}", e.getMessage());
		}
	}
}
