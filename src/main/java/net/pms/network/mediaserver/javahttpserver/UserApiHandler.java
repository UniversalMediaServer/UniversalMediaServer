package net.pms.network.mediaserver.javahttpserver;

import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.LoginDetails;
import net.pms.database.UserDatabase;

public class UserApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserApiHandler.class);

	private final Gson gson = new Gson();

    /**
	 * Handle API calls.
	 *
	 * @param exchange
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			try {
                // TODO check this is the right endpoint/path e.g. /changepassword
				if (exchange.getRequestMethod().equals("POST")) {
                    if (!UserService.isLoggedIn(exchange)) {
                        WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
                    }
                    String reqBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    LoginDetails data = gson.fromJson(reqBody, LoginDetails.class);
					Connection connection = null;
					connection = UserDatabase.getConnectionIfAvailable();
                    String loggedInUsername = UserService.getUsernameFromRequestJWT(exchange);
                    UserService.updatePassword(connection, data.getPassword(), loggedInUsername);
					WebInterfaceServerUtil.respond(exchange, "Ok", 200, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException: {}", e.getMessage());
				exchange.sendResponseHeaders(500, 0); //Internal Server Error
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConsoleHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
    }
}
