package net.pms.iam;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.Connection;
import net.pms.database.UserTableGroups;
import net.pms.database.UserTablePermissions;
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

	public static Account getAccountByUsername(final Connection connection, final String username) {
		Account result = new Account();
		result.setUser(UserTableUsers.getUserByUsername(connection, username));
		if (result.getUser() == null) {
			return null;
		}
		int groupId = result.getUser().getGroupId();
		result.setGroup(UserTableGroups.getGroupById(connection, groupId));
		result.setPermissions(UserTablePermissions.getPermissionsForGroupId(connection, groupId));
		return result;
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
		createUser(connection, username, password, username, -1);
	}

	public static void createUser(final Connection connection, final String username, final String password, final String name) {
		createUser(connection, username, password, name, -1);
	}

	public static void createUser(final Connection connection, final String username, final String password, final int groupId) {
		createUser(connection, username, password, username, groupId);
	}

	public static void createUser(final Connection connection, final String username, final String password, final String name, final int groupId) {
		LOGGER.info("Creating user: {}", username);
		UserTableUsers.addUser(connection, left(username, 255), left(hashPassword(password), 255), left(name, 255), groupId);
	}

	public static void updatePassword(final Connection connection, final String newPassword, final User user) {
		LOGGER.info("Updating password for {}", user.getUsername());
		String password = hashPassword(newPassword);
		user.setPassword(password);
		UserTableUsers.updatePassword(connection, user.getId(), password);
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

	public static boolean validatePassword(String password, String dbPasswordHash) {
		BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), dbPasswordHash);
		return result.verified;
	}

	public static String hashPassword(String passwordToHash) {
		String bcryptHashString = BCrypt.withDefaults().hashToString(12, passwordToHash.toCharArray());
		return bcryptHashString;
	}

	public static void createGroup(final Connection connection, final String name) {
		LOGGER.info("Creating group: {}", name);
		UserTableGroups.addGroup(connection, name);
	}

	public static void allowPermission(final Connection connection, final Account account, final String name) {
		LOGGER.info("Allowing permission '{}' to group '{}'", name, account.getGroup().getName());
		account.getPermissions().put(name, true);
		UserTablePermissions.insertOrUpdate(connection, account.getGroup().getId(), name, true);
	}

	public static void denyPermission(final Connection connection, final Account account, final String name) {
		LOGGER.info("Denying permission '{}' to group '{}'", name, account.getGroup().getName());
		account.getPermissions().put(name, false);
		UserTablePermissions.insertOrUpdate(connection, account.getGroup().getId(), name, false);
	}

}
