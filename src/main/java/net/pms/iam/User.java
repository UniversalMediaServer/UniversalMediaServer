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

import net.pms.image.Image;

public class User extends UsernamePassword {
	private int id;
	private String displayName;
	private int groupId;
	private Image avatar;
	private String pinCode;
	private long lastLoginTime;
	private long loginFailedTime;
	private int loginFailedCount;
	private boolean libraryHidden;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String name) {
		this.displayName = name;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public Image getAvatar() {
		return avatar;
	}

	public void setAvatar(Image avatar) {
		this.avatar = avatar;
	}

	public String getPinCode() {
		return pinCode;
	}

	public void setPinCode(String pinCode) {
		this.pinCode = pinCode;
	}

	public long getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(long lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public long getLoginFailedTime() {
		return loginFailedTime;
	}

	public void setLoginFailedTime(long loginFailedTime) {
		this.loginFailedTime = loginFailedTime;
	}

	public int getLoginFailedCount() {
		return loginFailedCount;
	}

	public void setLoginFailedCount(int loginFailedCount) {
		this.loginFailedCount = loginFailedCount;
	}

	public boolean isLibraryHidden() {
		return libraryHidden;
	}

	public boolean isLibraryChoice() {
		return !libraryHidden;
	}

	public void setLibraryHidden(boolean libraryHidden) {
		this.libraryHidden = libraryHidden;
	}

	@Override
	public String toString() {
		return displayName != null && !"".equals(displayName) ? displayName : getUsername();
	}
}
