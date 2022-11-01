/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
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
package net.pms.network.webguiserver;

import net.pms.dlna.RootFolder;
import net.pms.iam.Account;
import net.pms.iam.AccountService;

public class WebPlayerRootFolder extends RootFolder {
	private final int userId;

	public WebPlayerRootFolder(int userId) {
		super();
		this.userId = userId;
	}

	public int getUserId() {
		return userId;
	}

	public boolean havePermission(int permission) {
		if (userId == Integer.MAX_VALUE) {
			return true;
		} else {
			Account account = AccountService.getAccountByUserId(userId);
			return (account != null && account.havePermission(permission));
		}
	}
}
