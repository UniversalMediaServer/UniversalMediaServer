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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.iam;

import java.util.HashMap;

public class Account {
	private User user;
	private Group group;
	private HashMap<String, Boolean> permissions;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public HashMap<String, Boolean> getPermissions() {
		return permissions;
	}

	public void setPermissions(HashMap<String, Boolean> permissions) {
		this.permissions = permissions;
	}

	public String getUsername() {
		return user.getUsername();
	}

	public boolean havePermission(String name) {
		return (this.permissions != null &&
				((this.permissions.containsKey(Permissions.ANY) && this.permissions.get(Permissions.ANY)) ||
				(this.permissions.containsKey(name) && this.permissions.get(name))
				));
	}

}
