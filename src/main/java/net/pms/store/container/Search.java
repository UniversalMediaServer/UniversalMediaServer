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
import net.pms.store.StoreContainer;

public class Search extends StoreContainer {
	private SearchObj sobj;
	private StringBuilder sb;
	private boolean searched;

	public Search(Renderer renderer, SearchObj obj) {
		this(renderer, obj, "");
	}

	public Search(Renderer renderer, SearchObj obj, String str) {
		super(renderer, str, null);
		this.sobj = obj;
		this.sb = new StringBuilder(str);
		searched = false;
	}

	public SearchObj getSearchObj() {
		return sobj;
	}

	@Override
	public String getName() {
		return sb.toString();
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	@Override
	public synchronized void resolve() {
		setDiscovered(false);
	}

	public synchronized void append(char ch) {
		switch (ch) {
			case '\0' -> {
				sb = new StringBuilder();
			}
			case '\b' -> {
				if (sb.length() != 0) {
					sb.deleteCharAt(sb.length() - 1);
				}
			}
			default -> sb.append(ch);
		}
	}

	@Override
	public synchronized void discoverChildren() {
		if (searched) {
			getChildren().clear();
		}
		sobj.search(sb.toString(), this);
		searched = true;
	}
}
