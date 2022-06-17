package net.pms.network.webinterfaceserver.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import net.pms.PMS;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webinterfaceserver.WebInterfaceServer;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionsApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionsApiHandler.class);
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
			if (WebInterfaceServerUtil.deny(exchange)) {
				exchange.close();
				return;
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(exchange, "");
			}
			/**
			 * Helpers for HTTP methods and paths.
			 */
			var api = new Object() {
				private String getEndpoint() {
					String endpoint = "/";
					int pos = exchange.getRequestURI().getPath().indexOf("/v1/api/actions/");
					if (pos != -1) {
						endpoint = exchange.getRequestURI().getPath().substring(pos + "/v1/api/actions/".length());
					}
					return endpoint;
				}
				/**
				 * @return whether this was a GET request for the specified path.
				 */
				public Boolean get(String path) {
					return exchange.getRequestMethod().equals("GET") && getEndpoint().equals(path);
				}
				/**
				 * @return whether this was a POST request for the specified path.
				 */
				public Boolean post(String path) {
					return exchange.getRequestMethod().equals("POST") && getEndpoint().equals(path);
				}
			};
			try {
				if (api.post("/")) {
					if (!AuthService.isLoggedIn(exchange.getRequestHeaders().get("Authorization"))) {
						WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
					}
					int loggedInUserId = AuthService.getUserIdFromJWT(exchange.getRequestHeaders().get("Authorization"));
					Account account = WebInterfaceServer.getAccountByUserId(loggedInUserId);
					if (account != null) {
						String reqBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
						HashMap<String, String> data = gson.fromJson(reqBody, HashMap.class);
						String operation = data.get("operation");
						switch (operation) {
							case "Server.Restart":
								if (account.havePermission(Permissions.SERVER_RESTART)) {
									PMS.get().reset();
									WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
								} else {
									WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
								}
								break;
							default:
								WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Operation not configured\"}", 400, "application/json");
						}
					} else {
						WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
					}
				} else {
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in ActionsApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
			}
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ActionsApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
