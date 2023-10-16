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
package net.pms.store.container;

import net.pms.renderers.Renderer;
import net.pms.store.SearchObj;

public class SearchFolder extends LocalizedStoreContainer {

	private SearchObj sobj;

	public SearchFolder(Renderer renderer, SearchObj sobj) {
		this(renderer, "Search", sobj);
	}

	public SearchFolder(Renderer renderer, String i18nName, SearchObj sobj) {
		super(renderer, i18nName);
		this.sobj = sobj;
	}

	private void createSearcher(SearchObj obj, String initStr) {
		char i;
		Search s = new Search(renderer, obj, initStr);
		addChild(s);
		addChild(new SearchAction(renderer, s, '\0', "Clear"));
		addChild(new SearchAction(renderer, s, ' ', "Space"));
		addChild(new SearchAction(renderer, s, '\b', "Delete"));
		for (i = 'A'; i <= 'Z'; i++) {
			addChild(new SearchAction(renderer, s, i));
		}
		for (i = '0'; i <= '9'; i++) {
			addChild(new SearchAction(renderer, s, i));
		}
	}

	@Override
	public synchronized void resolve() {
		//this resource never resolve
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
		refreshChildren(null, null);
		return true;
	}

	@Override
	public boolean refreshChildren(String str, String lang) {
		if (str == null) {
			return false;
		}
		getChildren().clear();
		discoverChildren(str);
		return true;
	}

}
