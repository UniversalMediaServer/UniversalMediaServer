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

/**
 * Adding new permissions should be duplicated on the react side.
 * integer allows 32 permissions, that should be enough for our purposes.
 * otherwise we can easily switch to long (64 bits).
 */
public class Permissions {
	//Identity and Access Management
	public static final int USERS_MANAGE = 1;
	public static final int GROUPS_MANAGE = 1 << 1;
	//Settings
	public static final int SETTINGS_VIEW = 1 << 10;
	public static final int SETTINGS_MODIFY = 1 << 11;
	public static final int DEVICES_CONTROL = 1 << 12;
	//Actions
	public static final int SERVER_RESTART = 1 << 20;
	public static final int APPLICATION_RESTART = 1 << 21;
	public static final int APPLICATION_SHUTDOWN = 1 << 22;
	public static final int COMPUTER_SHUTDOWN = 1 << 23;
	//Web player
	public static final int WEB_PLAYER_BROWSE = 1 << 25;
	public static final int WEB_PLAYER_DOWNLOAD = 1 << 26;

	public static final int ALL = 0xFFFFFFFF;

	private int value;
	public Permissions(int permissions) {
		value = permissions;
	}

	public void setValue(int permissions) {
		value = permissions;
	}

	public int getValue() {
		return value;
	}

	public void addPermission(int permission) {
		value |= permission;
	}

	public void removePermission(int permission) {
		value |= permission;
	}

	public boolean havePermission(int permission) {
		return (value & permission) != 0;
	}

}
