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
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;

public class FolderLimitLevel extends StoreContainer {
	private final int level;
	private StoreResource start;

	public FolderLimitLevel(Renderer renderer, int level) {
		super(renderer, "Level " + level, null);
		this.level = level;
		this.start = null;
	}

	public int level() {
		return level;
	}

	public void setStart(StoreResource r) {
		if (r.getParent() == null) {
			start = r.clone();
		} else {
			start = r.getParent().clone();
		}
		resolve();
	}

	@Override
	public synchronized void discoverChildren() {
		if (start != null) {
			addChild(start);
			sortChildrenIfNeeded();
		}
	}

	@Override
	public synchronized void resolve() {
		this.setDiscovered(false);
		this.getChildren().clear();
	}
}
