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
package net.pms.dlna;

import net.pms.Messages;
import net.pms.dlna.virtual.VirtualFolder;

public class SearchFolder extends VirtualFolder {
	private SearchObj sobj;

	public SearchFolder(SearchObj sobj) {
		this(Messages.getString("Search"), sobj);
	}

	public SearchFolder(String name, SearchObj sobj) {
		super(name, null);
		this.sobj = sobj;
	}

	private void createSearcher(SearchObj obj, String initStr) {
		char i;
		Search s = new Search(obj, initStr);
		addChild(s);
		addChild(new SearchAction(s, '\0', "Clear"));
		addChild(new SearchAction(s, ' ', "Space"));
		addChild(new SearchAction(s, '\b', "Delete"));
		for (i = 'A'; i <= 'Z'; i++) {
			addChild(new SearchAction(s, i));
		}
		for (i = '0'; i <= '9'; i++) {
			addChild(new SearchAction(s, i));
		}
	}

	@Override
	public synchronized void resolve() {
	}

	@Override
	public void discoverChildren(String str) {
		if (str == null) {
			discoverChildren();
		} else {
			sobj.search(str, this);
		}
	}

	@Override
	public boolean isSearched() {
		return true;
	}

	@Override
	public void discoverChildren() {
		createSearcher(sobj, "");
	}

	@Override
	public boolean isRefreshNeeded() {
		return true;
	}

	@Override
	public boolean refreshChildren() {
		refreshChildren(null);
		return true;
	}

	@Override
	public boolean refreshChildren(String str) {
		if (str == null) {
			return false;
		}
		getChildren().clear();
		discoverChildren(str);
		return true;
	}
}
