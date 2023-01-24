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
package net.pms.iam;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pms.database.UserDatabase;
import net.pms.database.UserTableGroups;
import net.pms.database.UserTableUsers;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountService.class);
	public static final String DEFAULT_ADMIN_GROUP = "admin";
	public static final String DEFAULT_ADMIN_USERNAME = "ums";
	public static final String DEFAULT_ADMIN_PASSWORD = "initialpassword";
	public static final int MAX_LOGIN_FAIL_BEFORE_LOCK = 3;	//3 tries
	public static final int LOGIN_FAIL_LOCK_TIME = 30000;	//30 sec
	private static final Map<Integer, User> USERS = new HashMap<>();
	private static final Map<Integer, Group> GROUPS = new HashMap<>();
	private static final Account FAKE_ADMIN_ACCOUNT = setFakeAdminAccount();

	/**
	 * This class is not meant to be instantiated.
	 */
	private AccountService() {
	}

	public static Account getAccountByUserId(final int userId) {
		if (userId == Integer.MAX_VALUE) {
			return FAKE_ADMIN_ACCOUNT;
		}
		Account result = new Account();
		result.setUser(getUserById(userId));
		return fillGroupAndPermissions(result);
	}

	public static User getUserById(final int userId) {
		if (USERS.containsKey(userId)) {
			return USERS.get(userId);
		} else {
			Connection connection = UserDatabase.getConnectionIfAvailable();
			if (connection != null) {
				User user = UserTableUsers.getUserByUserId(connection, userId);
				if (user != null) {
					USERS.put(userId, user);
					UserDatabase.close(connection);
					return user;
				}
				UserDatabase.close(connection);
			}
		}
		return null;
	}

	private static Group getGroupById(final int groupId) {
		if (GROUPS.containsKey(groupId)) {
			return GROUPS.get(groupId);
		} else {
			Connection connection = UserDatabase.getConnectionIfAvailable();
			if (connection != null) {
				Group group = UserTableGroups.getGroupById(connection, groupId);
				//here, group id may have falled back to no group (0)
				if (group.getId() == groupId) {
					GROUPS.put(group.getId(), group);
				}
				UserDatabase.close(connection);
				return group;
			}
		}
		return null;
	}

	private static Account fillGroupAndPermissions(final Account account) {
		if (account == null || account.getUser() == null) {
			return null;
		}
		int groupId = account.getUser().getGroupId();
		account.setGroup(getGroupById(groupId));
		if (account.getUser().getGroupId() != account.getGroup().getId() && account.getGroup().getId() == 0) {
			LOGGER.info("User '{}' refer to an unknown group that fall back to no group.", account.getUser().getUsername());
			//update the user groupId to prevent message flood
			account.getUser().setGroupId(account.getGroup().getId());
		}
		return account;
	}

	public static Account getAccountByUsername(final Connection connection, final String username) {
		Account account = new Account();
		account.setUser(UserTableUsers.getUserByUsername(connection, username));
		if (account.getUser() == null) {
			return null;
		}
		if (!USERS.containsKey(account.getUser().getId())) {
			USERS.put(account.getUser().getId(), account.getUser());
		}
		int groupId = account.getUser().getGroupId();
		if (GROUPS.containsKey(groupId)) {
			account.setGroup(GROUPS.get(groupId));
		} else {
			Group group = UserTableGroups.getGroupById(connection, groupId);
			//here, group id may have falled back to no group (0)
			GROUPS.put(group.getId(), group);
			account.setGroup(group);
			if (account.getUser().getGroupId() != account.getGroup().getId() && account.getGroup().getId() == 0) {
				LOGGER.info("User '{}' refer to an unknown group that fall back to no group.", account.getUser().getUsername());
				//update the user groupId to prevent message flood
				account.getUser().setGroupId(account.getGroup().getId());
			}
		}
		return account;
	}

	public static void setUserLogged(final Connection connection, final User user) {
		LOGGER.info("User {} logged", user.getUsername());
		long timestamp = System.currentTimeMillis();
		user.setLastLoginTime(timestamp);
		user.setLoginFailedTime(0);
		user.setLoginFailedCount(0);
		UserTableUsers.setLoginTime(connection, user.getId(), timestamp);
	}

	public static void setUserLoginFailed(final Connection connection, final User user) {
		LOGGER.info("User login failed for {}", user.getUsername());
		long timestamp = System.currentTimeMillis();
		user.setLoginFailedTime(timestamp);
		user.setLoginFailedCount(user.getLoginFailedCount() + 1);
		UserTableUsers.setLoginFailed(connection, user.getId(), timestamp);
	}

	public static void createUser(final Connection connection, final String username, final String password) {
		createUser(connection, username, password, username, 0);
	}

	public static void createUser(final Connection connection, final String username, final String password, final String displayName) {
		createUser(connection, username, password, displayName, 0);
	}

	public static void createUser(final Connection connection, final String username, final String password, final int groupId) {
		createUser(connection, username, password, username, groupId);
	}

	public static void createUser(final Connection connection, final String username, final String password, final String displayName, final int groupId) {
		LOGGER.info("Creating user: {}", username);
		UserTableUsers.addUser(connection, left(username, 255), left(hashPassword(password), 255), left(displayName, 255), groupId);
	}

	public static void updateUser(final Connection connection, final int userId, final String displayName, final int groupId) {
		LOGGER.info("Updating user id : {}", userId);
		if (UserTableUsers.updateUser(connection, userId, displayName, groupId) && USERS.containsKey(userId)) {
			USERS.get(userId).setDisplayName(displayName);
			USERS.get(userId).setGroupId(groupId);
		}
	}

	public static void deleteUser(final Connection connection, final int userId) {
		LOGGER.info("Deleting user id : {}", userId);
		UserTableUsers.deleteUser(connection, userId);
		if (USERS.containsKey(userId)) {
			USERS.remove(userId);
		}
	}

	public static void updateLogin(final Connection connection, final int userId, final String username, final String newPassword) {
		LOGGER.info("Updating username/password for user id {}", userId);
		String password = hashPassword(newPassword);
		if (UserTableUsers.updateLogin(connection, userId, username, password) && USERS.containsKey(userId)) {
			USERS.get(userId).setUsername(username);
			USERS.get(userId).setPassword(password);
		}
	}

	public static void checkUserUnlock(final Connection connection, final User user) {
		if (user != null && user.getLoginFailedCount() > 0 && System.currentTimeMillis() -  user.getLoginFailedTime() > LOGIN_FAIL_LOCK_TIME) {
			LOGGER.trace("Unlocking account for {}",  user.getUsername());
			user.setLoginFailedCount(0);
			UserTableUsers.resetLoginFailCount(connection, user.getId());
		}
	}

	public static boolean isUserLocked(final User user) {
		return (user == null || user.getLoginFailedCount() >= MAX_LOGIN_FAIL_BEFORE_LOCK);
	}

	public static boolean validatePassword(String password, String bcryptHash) {
		BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), bcryptHash);
		return result.verified;
	}

	public static String hashPassword(String password) {
		return BCrypt.withDefaults().hashToString(12, password.toCharArray());
	}

	public static void createGroup(final Connection connection, final String name, final int permissions) {
		LOGGER.info("Creating group: {}", name);
		UserTableGroups.addGroup(connection, name, permissions);
	}

	public static void updateGroup(final Connection connection, final int groupId, final String name) {
		LOGGER.info("Updating group: {}", groupId);
		if (UserTableGroups.updateGroupName(connection, groupId, name) && GROUPS.containsKey(groupId)) {
			GROUPS.get(groupId).setDisplayName(name);
		}
	}

	public static void deleteGroup(final Connection connection, final int groupId) {
		LOGGER.info("Deleting group: {}", groupId);
		if (UserTableGroups.removeGroup(connection, groupId) && GROUPS.containsKey(groupId)) {
			GROUPS.remove(groupId);
		}
	}

	public static void updatePermissions(final Connection connection, final int groupId, final int permissions) {
		LOGGER.info("Updating permissions to group id {}", groupId);
		if (UserTableGroups.updateGroupPermissions(connection, groupId, permissions) && GROUPS.containsKey(groupId)) {
			GROUPS.get(groupId).setPermissions(permissions);
		}
	}

	public static boolean hasNoAdmin(final Connection connection) {
		LOGGER.info("Checking user table have admin");
		return UserTableUsers.hasNoAdmin(connection);
	}

	public static Collection<User> getAllUsers() {
		//ensure all users are in static Map
		Connection connection = UserDatabase.getConnectionIfAvailable();
		if (connection != null) {
			List<User> users = UserTableUsers.getAllUsers(connection);
			for (User user : users) {
				if (!USERS.containsKey(user.getId())) {
					USERS.put(user.getId(), user);
				}
			}
			UserDatabase.close(connection);
		}
		return USERS.values();
	}

	public static Collection<Group> getAllGroups() {
		//ensure all groups are in static Map
		Connection connection = UserDatabase.getConnectionIfAvailable();
		if (connection != null) {
			List<Group> groups = UserTableGroups.getAllGroups(connection);
			for (Group group : groups) {
				if (!GROUPS.containsKey(group.getId())) {
					//load the perms
					GROUPS.put(group.getId(), group);
				}
			}
			UserDatabase.close(connection);
		}
		return GROUPS.values();
	}

	public static List<Integer> getUserIdsForGroup(int groupId) {
		List<Integer> userIds = new ArrayList<>();
		for (User user : USERS.values()) {
			if (user.getGroupId() == groupId) {
				userIds.add(user.getId());
			}
		}
		return userIds;
	}

	public static Account getFakeAdminAccount() {
		return FAKE_ADMIN_ACCOUNT;
	}

	private static Account setFakeAdminAccount() {
		Account account = new Account();
		Group group = new Group();
		group.setPermissions(Permissions.ALL);
		group.setId(Integer.MAX_VALUE);
		account.setGroup(group);
		User user = new User();
		user.setId(Integer.MAX_VALUE);
		account.setUser(user);
		return account;
	}
}
