/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ALL WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.webinterfaceserver.configuration.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import net.pms.database.UserDatabase;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.Group;
import net.pms.iam.Permissions;
import net.pms.iam.User;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.configuration.ApiHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create/modify/view accounts Api Handler.
 */
public class AccountApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountApiHandler.class);
	private static final Gson GSON = new Gson();

	public static final String BASE_PATH = "/v1/api/account";


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
			var api = new ApiHelper(exchange, BASE_PATH);
			try {
				if (api.get("/accounts")) {
					//get account list that the user can view/modify
					Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
					if (account != null) {
						JsonObject jObject = new JsonObject();
						JsonArray jUsers = new JsonArray();
						if (account.havePermission(Permissions.USERS_MANAGE)) {
							jObject.add("usersManage", new JsonPrimitive(true));
							for (User user : AccountService.getAllUsers()) {
								jUsers.add(userToJsonObject(user));
							}
						} else {
							//can see only his user
							jObject.add("usersManage", new JsonPrimitive(false));
							jUsers.add(userToJsonObject(account.getUser()));
						}
						jObject.add("users", jUsers);
						JsonArray jGroups = new JsonArray();
						if (account.havePermission(Permissions.USERS_MANAGE) || account.havePermission(Permissions.GROUPS_MANAGE)) {
							jObject.add("groupsManage", new JsonPrimitive(true));
							for (Group group : AccountService.getAllGroups()) {
								jGroups.add(groupToJsonObject(group));
							}
						} else {
							//can see only his groups
							jObject.add("groupsManage", new JsonPrimitive(false));
							if (account.getGroup().getId() > 0) {
								jGroups.add(groupToJsonObject(account.getGroup()));
							}
						}
						jObject.add("groups", jGroups);
					} else {
						WebInterfaceServerUtil.respond(exchange, null, 401, "application/json");
					}
				} else if (api.post("/action")) {
					//action requested on account (create/modify)
					Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
					if (account != null) {
						String reqBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
						JsonObject action = jsonObjectFromString(reqBody);
						if (action == null || !action.has("operation") || !action.get("operation").isJsonPrimitive()) {
							WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
							return;
						}
						String operation = action.get("operation").getAsString();
						if (operation != null) {
							Connection connection = UserDatabase.getConnectionIfAvailable();
							if (connection != null) {
								switch (operation) {
									case "changelogin":
										//we need username, password
										//optional userid
										if (!action.has("username") || !action.has("password")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										int clUserId;
										// without userid member, we fall back to self user
										if (action.has("userid")) {
											clUserId = action.get("userid").getAsInt();
										} else {
											clUserId = account.getUser().getId();
										}
										String clUsername = action.get("username").getAsString();
										String clPassword = action.get("password").getAsString();
										//user changing his own password or have permissions to
										if (clUserId == account.getUser().getId() || account.havePermission(Permissions.USERS_MANAGE)) {
											AccountService.updateLogin(connection, clUserId, clUsername, clPassword);
											WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
										} else {
											LOGGER.trace("User '{}' try to change password for user id: {}", account.toString(), clUserId);
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
										}
										break;
									case "createuser":
										if (!account.havePermission(Permissions.USERS_MANAGE)) {
											LOGGER.trace("User '{}' try to create a user", account.toString());
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
											return;
										}
										//we need at least user, password
										//optional : name, groupid
										if (!action.has("username") || !action.has("password")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										String cuUsername = action.get("username").getAsString();
										String cuPassword = action.get("password").getAsString();
										String cuName;
										if (action.has("name")) {
											cuName = action.get("name").getAsString();
										} else {
											cuName = cuUsername;
										}
										int cuGroupId;
										if (action.has("groupid")) {
											cuGroupId = action.get("groupid").getAsInt();
										} else {
											cuGroupId = 0;
										}
										//if no granted to manage groups, only allow self group or none
										if (cuGroupId != 0 && !account.havePermission(Permissions.GROUPS_MANAGE) && cuGroupId != account.getGroup().getId()) {
											cuGroupId = 0;
										}
										AccountService.createUser(connection, cuUsername, cuPassword, cuName, cuGroupId);
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
										break;
									case "modifyuser":
										//we need userid
										//optional : name, groupid
										if (!action.has("userid")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										int muUserId = action.get("userid").getAsInt();
										if (muUserId != account.getUser().getId() && !account.havePermission(Permissions.USERS_MANAGE)) {
											LOGGER.trace("User '{}' try to modify account of user id {}", account.toString(), muUserId);
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
											return;
										}
										User muUser = AccountService.getUserById(muUserId);
										if (muUser == null) {
											//user does not exists
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										String muName;
										if (action.has("name")) {
											muName = action.get("name").getAsString();
										} else {
											muName = muUser.getDisplayName();
										}
										int muGroupId;
										if (action.has("groupid")) {
											muGroupId = action.get("groupid").getAsInt();
										} else {
											muGroupId = muUser.getGroupId();
										}
										//if no granted to manage groups, only allow current group
										if (!account.havePermission(Permissions.GROUPS_MANAGE) && muGroupId != muUser.getGroupId()) {
											if (!muName.equals(muUser.getDisplayName())) {
												//the user had changed more field than group, revert back the group id change
												muGroupId = muUser.getGroupId();
											} else {
												//request only a group change, send a 403
												LOGGER.trace("User '{}' try to modify group of user id: {}", account.toString(), muUserId);
												WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
												return;
											}
										}
										AccountService.updateUser(connection, muUserId, muName, muGroupId);
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
										break;
									case "deleteuser":
										//we need only userid
										if (!action.has("userid")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										int duUserId = action.get("userid").getAsInt();
										if (!account.havePermission(Permissions.USERS_MANAGE)) {
											LOGGER.trace("User '{}' try to delete the user with id {}", account.toString(), duUserId);
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
											return;
										}
										AccountService.deleteUser(connection, duUserId);
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
										break;
									case "creategroup":
										if (!account.havePermission(Permissions.GROUPS_MANAGE)) {
											LOGGER.trace("User '{}' try to create a group", account.toString());
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
											return;
										}
										//we need name
										if (!action.has("name")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										String cgName = action.get("name").getAsString();
										AccountService.createGroup(connection, cgName);
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
										break;
									case "modifygroup":
										if (!account.havePermission(Permissions.GROUPS_MANAGE)) {
											LOGGER.trace("User '{}' try to modify a group", account.toString());
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
											return;
										}
										//we need groupid, name
										if (!action.has("groupid") || !action.has("name")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										int mgGroupId = action.get("groupid").getAsInt();
										String mgName = action.get("name").getAsString();
										AccountService.updateGroup(connection, mgGroupId, mgName);
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
										break;
									case "deletegroup":
										if (!account.havePermission(Permissions.GROUPS_MANAGE)) {
											LOGGER.trace("User '{}' try to delete a group", account.toString());
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
											return;
										}
										//we need groupid
										if (!action.has("groupid")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										int dgGroupId = action.get("groupid").getAsInt();
										AccountService.deleteGroup(connection, dgGroupId);
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
										break;
									case "updatepermission":
										if (!account.havePermission(Permissions.GROUPS_MANAGE)) {
											LOGGER.trace("User '{}' try to update permissions", account.toString());
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
											return;
										}
										//we need groupid, permissions
										if (!action.has("groupid") || !action.has("permissions")) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										int upGroupId = action.get("groupid").getAsInt();
										List<String> upPermissions = null;
										if (action.get("permissions").isJsonArray()) {
											upPermissions = new ArrayList<>();
											JsonArray jPermissions = action.get("permissions").getAsJsonArray();
											for (JsonElement jPermission : jPermissions) {
												upPermissions.add(jPermission.getAsString());
											}
										}
										if (upPermissions == null) {
											WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
											return;
										}
										AccountService.updatePermission(connection, upGroupId, upPermissions);
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
									default:
										WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Operation not configured\"}", 400, "application/json");
								}
							} else {
								LOGGER.error("User database not available");
								WebInterfaceServerUtil.respond(exchange, "{\"error\": \"User database not available\"}", 500, "application/json");
							}
						} else {
							WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
						}
					} else {
						WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Unauthorized\"}", 401, "application/json");
					}
				} else {
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in UserApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in UserApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private static JsonObject jsonObjectFromString(String str) {
		JsonObject jObject = null;
		try {
			JsonElement jElem = GSON.fromJson(str, JsonElement.class);
			if (jElem.isJsonObject()) {
				jObject = jElem.getAsJsonObject();
			}
		} catch (JsonSyntaxException je) {
		}
		return jObject;
	}

	public static JsonObject accountToJsonObject(Account account) {
		JsonElement jElement = GSON.toJsonTree(account);
		JsonObject jAccount = jElement.getAsJsonObject();
		jAccount.getAsJsonObject("user").remove("password");
		return jAccount;
	}

	private static JsonObject userToJsonObject(User user) {
		JsonElement jElement = GSON.toJsonTree(user);
		JsonObject jUser = jElement.getAsJsonObject();
		jUser.remove("password");
		return jUser;
	}

	private static JsonObject groupToJsonObject(Group group) {
		JsonElement jElement = GSON.toJsonTree(group);
		return jElement.getAsJsonObject();
	}

}
