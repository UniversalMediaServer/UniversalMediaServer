package net.pms.network.webinterfaceserver.handlers;

import com.google.gson.Gson;
import java.util.HashMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.ProcessUtil;
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
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			if (WebInterfaceServerUtil.deny(ia)) {
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
                    String reqBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    HashMap<String,String> data = gson.fromJson(reqBody, HashMap.class);
                    String method = data.get("operation");
                    switch (method) {
                        case "Server.restart":
                            ProcessUtil.reboot();
                            break;
                        default:
                            WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Operation not configured\"}", 400, "application/json");
                    }
                    ProcessUtil.reboot();
				} else {
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in ActionsApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
			}
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in AuthApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
