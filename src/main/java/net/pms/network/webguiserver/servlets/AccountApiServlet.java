/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.webguiserver.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.sql.Connection;
import java.util.Base64;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.database.UserDatabase;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.Group;
import net.pms.iam.Permissions;
import net.pms.iam.User;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create/modify/view accounts Api Handler.
 */
@WebServlet(name = "AccountApiServlet", urlPatterns = {"/v1/api/account"}, displayName = "Account Api Servlet")
public class AccountApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountApiServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/accounts")) {
				//get account list that the user can view/modify
				Account account = AuthService.getAccountLoggedIn(req);
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
					if (account.havePermission(Permissions.USERS_MANAGE | Permissions.GROUPS_MANAGE)) {
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
					jObject.add("enabled", new JsonPrimitive(AuthService.isEnabled()));
					jObject.add("localhost", new JsonPrimitive(AuthService.isLocalhostAsAdmin()));
					WebGuiServletHelper.respond(req, resp, jObject.toString(), 200, "application/json");
				} else {
					WebGuiServletHelper.respondUnauthorized(req, resp);
				}
			} else {
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AccountApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/action")) {
				//action requested on account (create/modify)
				Account account = AuthService.getAccountLoggedIn(req);
				if (account != null) {
					JsonObject action = WebGuiServletHelper.getJsonObjectFromBody(req);
					if (action == null || !action.has("operation") || !action.get("operation").isJsonPrimitive()) {
						WebGuiServletHelper.respondBadRequest(req, resp);
						return;
					}
					String operation = action.get("operation").getAsString();
					if (operation != null) {
						Connection connection = UserDatabase.getConnectionIfAvailable();
						if (connection != null) {
							switch (operation) {
								case "authentication" -> {
									//we need enabled
									if (action.has("enabled") && account.havePermission(Permissions.SETTINGS_MODIFY)) {
										boolean enabled = action.get("enabled").getAsBoolean();
										AuthService.setEnabled(enabled);
										WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
									} else {
										LOGGER.trace("User '{}' try to change authentication service", account.toString());
										WebGuiServletHelper.respondForbidden(req, resp);
									}
								}
								case "localhost" -> {
									//we need enabled
									if (action.has("enabled") && account.havePermission(Permissions.SETTINGS_MODIFY)) {
										boolean enabled = action.get("enabled").getAsBoolean();
										AuthService.setLocalhostAsAdmin(enabled);
										WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
									} else {
										LOGGER.trace("User '{}' try to change localhost auto admin", account.toString());
										WebGuiServletHelper.respondForbidden(req, resp);
									}
								}
								case "changelogin" -> {
									//we need username, password
									//optional userid
									if (action.has("username") && action.has("password")) {
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
											WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
										} else {
											LOGGER.trace("User '{}' try to change password for user id: {}", account.toString(), clUserId);
											WebGuiServletHelper.respondForbidden(req, resp);
										}
									} else {
										WebGuiServletHelper.respondBadRequest(req, resp);
									}
								}
								case "createuser" -> {
									if (account.havePermission(Permissions.USERS_MANAGE)) {
										//we need at least user, password
										//optional : name, groupid
										if (action.has("username") && action.has("password")) {
											String cuUsername = action.get("username").getAsString();
											String cuPassword = action.get("password").getAsString();
											String cuName;
											if (action.has("displayname")) {
												cuName = action.get("displayname").getAsString();
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
											WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
											SseApiServlet.setUpdateAccounts();
										} else {
											WebGuiServletHelper.respondBadRequest(req, resp);
										}
									} else {
										LOGGER.trace("User '{}' try to create a user", account.toString());
										WebGuiServletHelper.respondForbidden(req, resp);
									}
								}
								case "modifyuser" -> {
									//we need userid
									//optional : name, groupid, avatar, pincode
									if (action.has("userid")) {
										int muUserId = action.get("userid").getAsInt();
										if (muUserId == account.getUser().getId() || account.havePermission(Permissions.USERS_MANAGE)) {
											User muUser = AccountService.getUserById(muUserId);
											if (muUser != null) {
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
												Image muAvatar = null;
												if (action.has("avatar")) {
													String muAvatarBase64 = action.get("avatar").getAsString();
													if (muAvatarBase64.contains("data:image") && muAvatarBase64.contains(";base64,")) {
														muAvatarBase64 = muAvatarBase64.substring(muAvatarBase64.indexOf(";base64,") + 8);
														muAvatar = getAvatarFromBase64(muAvatarBase64);
													}
													if (muAvatar == null && !"".equals(muAvatarBase64)) {
														//something went wrong or this is not an image
														muAvatar = muUser.getAvatar();
													}
												} else {
													muAvatar = muUser.getAvatar();
												}
												String muPinCode;
												if (action.has("pincode")) {
													muPinCode = action.get("pincode").getAsString();
												} else {
													muPinCode = muUser.getPinCode();
												}
												//if no granted to manage groups, only allow current group
												if (!account.havePermission(Permissions.GROUPS_MANAGE) && muGroupId != muUser.getGroupId()) {
													if (!muName.equals(muUser.getDisplayName())) {
														//the user had changed more field than group, revert back the group id change
														muGroupId = muUser.getGroupId();
													} else {
														//request only a group change, send a 403
														LOGGER.trace("User '{}' try to modify group of user id: {}", account.toString(), muUserId);
														WebGuiServletHelper.respondForbidden(req, resp);
														//don't forget to close the db
														UserDatabase.close(connection);
														return;
													}
												}
												AccountService.updateUser(connection, muUserId, muName, muGroupId, muAvatar, muPinCode);
												WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
												SseApiServlet.setRefreshSession(muUserId);
												SseApiServlet.setUpdateAccounts();
											} else {
												//user does not exists
												WebGuiServletHelper.respondBadRequest(req, resp);
											}
										} else {
											LOGGER.trace("User '{}' try to modify account of user id {}", account.toString(), muUserId);
											WebGuiServletHelper.respondForbidden(req, resp);
										}
									} else {
										WebGuiServletHelper.respondBadRequest(req, resp);
									}
								}
								case "deleteuser" -> {
									//we need only userid
									if (action.has("userid")) {
										int duUserId = action.get("userid").getAsInt();
										if (account.havePermission(Permissions.USERS_MANAGE)) {
											AccountService.deleteUser(connection, duUserId);
											WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
											SseApiServlet.setRefreshSession(duUserId);
											SseApiServlet.setUpdateAccounts();
										} else {
											LOGGER.trace("User '{}' try to delete the user with id {}", account.toString(), duUserId);
											WebGuiServletHelper.respondForbidden(req, resp);
										}
									} else {
										WebGuiServletHelper.respondBadRequest(req, resp);
									}
								}
								case "creategroup" -> {
									if (account.havePermission(Permissions.GROUPS_MANAGE)) {
										//we need name
										if (action.has("name")) {
											String cgName = action.get("name").getAsString();
											AccountService.createGroup(connection, cgName, 0);
											WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
											SseApiServlet.setUpdateAccounts();
										} else {
											WebGuiServletHelper.respondBadRequest(req, resp);
										}
									} else {
										LOGGER.trace("User '{}' try to create a group", account.toString());
										WebGuiServletHelper.respondForbidden(req, resp);
									}
								}
								case "modifygroup" -> {
									if (account.havePermission(Permissions.GROUPS_MANAGE)) {
										//we need groupid, name
										if (action.has("groupid") && action.has("name")) {
											int mgGroupId = action.get("groupid").getAsInt();
											String mgName = action.get("name").getAsString();
											List<Integer> userIds = AccountService.getUserIdsForGroup(mgGroupId);
											AccountService.updateGroup(connection, mgGroupId, mgName);
											WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
											SseApiServlet.setRefreshSessions(userIds);
											SseApiServlet.setUpdateAccounts();
										} else {
											WebGuiServletHelper.respondBadRequest(req, resp);
										}
									} else {
										LOGGER.trace("User '{}' try to modify a group", account.toString());
										WebGuiServletHelper.respondForbidden(req, resp);
									}
								}
								case "deletegroup" -> {
									if (account.havePermission(Permissions.GROUPS_MANAGE)) {
										//we need groupid
										if (action.has("groupid")) {
											int dgGroupId = action.get("groupid").getAsInt();
											List<Integer> userIds = AccountService.getUserIdsForGroup(dgGroupId);
											AccountService.deleteGroup(connection, dgGroupId);
											WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
											SseApiServlet.setRefreshSessions(userIds);
											SseApiServlet.setUpdateAccounts();
										} else {
											WebGuiServletHelper.respondBadRequest(req, resp);
										}
									} else {
										LOGGER.trace("User '{}' try to delete a group", account.toString());
										WebGuiServletHelper.respondForbidden(req, resp);
									}
								}
								case "updatepermission" -> {
									if (account.havePermission(Permissions.GROUPS_MANAGE)) {
										//we need groupid, permissions
										if (action.has("groupid") && action.has("permissions")) {
											int upGroupId = action.get("groupid").getAsInt();
											int upPermissions = action.get("permissions").getAsInt();
											List<Integer> userIds = AccountService.getUserIdsForGroup(upGroupId);
											AccountService.updatePermissions(connection, upGroupId, upPermissions);
											WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
											SseApiServlet.setRefreshSessions(userIds);
											SseApiServlet.setUpdateAccounts();
										} else {
											WebGuiServletHelper.respondBadRequest(req, resp);
										}
									} else {
										LOGGER.trace("User '{}' try to update permissions", account.toString());
										WebGuiServletHelper.respondForbidden(req, resp);
									}
								}
								default -> WebGuiServletHelper.respondBadRequest(req, resp, "Operation not configured");
							}
							UserDatabase.close(connection);
						} else {
							LOGGER.error("User database not available");
							WebGuiServletHelper.respondInternalServerError(req, resp, "User database not available");
						}
					} else {
						WebGuiServletHelper.respondBadRequest(req, resp);
					}
				} else {
					WebGuiServletHelper.respondUnauthorized(req, resp);
				}
			} else {
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AccountApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
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

	private static Image getAvatarFromBase64(String imageString) {
		try {
			byte[] avatarBytes = Base64.getDecoder().decode(imageString);
			return Image.toImage(avatarBytes, 640, 480, ImagesUtil.ScaleType.MAX, ImageFormat.JPEG, false);
		} catch (IllegalArgumentException e) {
			LOGGER.trace("Avatar seems to not be a valid Base64: {}", e);
		} catch (IOException e) {
			LOGGER.trace("Avatar seems to not be a valid image: {}", e);
		}
		return null;
	}

}
