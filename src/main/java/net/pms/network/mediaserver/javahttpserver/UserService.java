package net.pms.network.mediaserver.javahttpserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import at.favre.lib.crypto.bcrypt.BCrypt;
import net.pms.util.LoginDetails;
import java.util.List;

public class UserService {
	public static final String TABLE_NAME = "USERS";
	private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	public static LoginDetails getUserByUsername(final Connection connection, final String username) {
		LoginDetails result;
		LOGGER.info("Finding user: {} ", username);
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
				insertStatement.setString(2, left(hashPassword(password), 255));
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
			LOGGER.info("Updating password for {} to: {}", username, newPassword);
			Statement statement = connection.createStatement();
			String sql = "UPDATE " + TABLE_NAME + " " +
			"SET PASSWORD='" + hashPassword(newPassword) + "' " +
			"WHERE USERNAME='" + username + "'";
			statement.executeUpdate(sql);
		} catch (Exception e) {
			LOGGER.error("Error updatePassword:{}", e.getMessage());
		}
	}

	public static Boolean isLoggedIn(HttpExchange exchange) {
		final List<String> authHeader = exchange.getRequestHeaders().get("Authorization");
		final String token = authHeader.get(0).replace("Bearer ", "");
		try {
			Algorithm algorithm = Algorithm.HMAC256("secret"); //use more secure key
			JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer("UMS")
				.build(); //Reusable verifier instance
			DecodedJWT jwt = verifier.verify(token);
			return true;
		} catch (JWTVerificationException exception){
			LOGGER.error("Error verifying JWT: {}", exception.getMessage());
			return false;
		}
	}
    // TODO this could potentially decode the JWT rather than verifying it, assuming HTTP exchange has already been checked
	public static String getUsernameFromRequestJWT(HttpExchange exchange) {
		final List<String> authHeader = exchange.getRequestHeaders().get("Authorization");
		final String token = authHeader.get(0).replace("Bearer ", "");
		try {
			Algorithm algorithm = Algorithm.HMAC256("secret"); //use more secure key
			JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer("UMS")
				.build(); //Reusable verifier instance
			DecodedJWT jwt = verifier.verify(token);
			String jwtUser = jwt.getClaim("username").asString();
			return jwtUser;
		} catch (JWTVerificationException exception){
			LOGGER.error("Error verifying JWT: {}", exception.getMessage());
		}
		return null;
	}
}
