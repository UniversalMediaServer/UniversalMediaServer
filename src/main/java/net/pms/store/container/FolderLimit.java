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

import java.util.ArrayList;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderLimit extends StoreContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(FolderLimit.class);
	private final ArrayList<FolderLimitLevel> levels;
	private boolean discover;

	public FolderLimit(Renderer renderer) {
		super(renderer, "Folder Limit", null);
		discover = false;
		levels = new ArrayList<>();
		levels.add(new FolderLimitLevel(renderer, 0)); // create level 0
	}

	public void setStart(StoreResource res) {
		LOGGER.debug("setting folder lim " + res);
		if (res == null) {
			return;
		}
		if (discover) {
			return;
		}
		int level = -1;
		StoreResource tmp = res;
		while (tmp != null) {
			if (tmp instanceof FolderLimit) {
				return;
			}
			if (tmp instanceof FolderLimitLevel folderLimitLevel) {
				level = folderLimitLevel.level();
				break;
			}
			tmp = tmp.getParent();
		}
		try {
			FolderLimitLevel fll = levels.get(level + 1);
			fll.setStart(res);
			if ((fll.level() == 0) && (levels.size() > 1)) {
				// we want to remove all levels 1+ so we clear all
				// and read level 0, its easier
				levels.clear();
				levels.add(fll);
			}
		} catch (IndexOutOfBoundsException e) { // create new level
			FolderLimitLevel fll = new FolderLimitLevel(renderer, level + 1);
			fll.setStart(res);
			levels.add(fll);
		}
	}

	@Override
	public void discoverChildren() {
		discover = true;
		for (StoreResource res : levels) {
			addChild(res);
		}
		discover = false;
	}

	@Override
	public synchronized void resolve() {
		this.setDiscovered(false);
		this.getChildren().clear();
	}
}
