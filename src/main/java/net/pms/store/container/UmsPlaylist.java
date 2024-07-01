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

import java.io.File;
import java.util.List;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.item.VirtualVideoAction;
import net.pms.store.item.VirtualVideoActionLocalized;
import net.pms.store.utils.IOList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmsPlaylist extends StoreContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(UmsPlaylist.class);
	protected IOList list;
	protected int maxSize;

	public UmsPlaylist(Renderer renderer, String name) {
		this(renderer, name, null, 0, IOList.AUTOSAVE);
	}

	public UmsPlaylist(Renderer renderer, String name, String filename) {
		this(renderer, name, filename, 0, IOList.AUTOSAVE);
	}

	public UmsPlaylist(Renderer renderer, String name, String filename, int maxSize, int mode) {
		super(renderer, name, "images/store/folder.png");
		this.maxSize = maxSize > 0 ? maxSize : 0;
		list = new IOList(renderer, filename, mode);
		list.save();
	}

	public File getFile()  {
		return list.getFile();
	}

	public void add(StoreResource resource) {
		StoreResource res1;
		LOGGER.debug("Adding \"{}\" to playlist \"{}\"", resource.getDisplayName(), getName());
		if (resource instanceof VirtualVideoAction) {
			// don't add these
			return;
		}
		if (resource.getParent() == this) {
			res1 = resource; // best guess
			for (StoreResource r : list) {
				if (
					r.getName().equals(resource.getName()) &&
					r.getSystemName().equals(resource.getSystemName())
				) {
					res1 = r;
					break;
				}
			}
		} else {
			res1 = resource.clone();
			if (resource instanceof StoreItem item) {
				((StoreItem) res1).setResume(item.getResume());
			}
		}
		list.remove(res1);
		if (maxSize > 0 && list.size() == maxSize) {
			list.remove(maxSize - 1);
		}
		list.add(0, res1);
		update();
	}

	public void remove(StoreResource res) {
		LOGGER.debug("Removing \"{}\" from playlist \"{}\"", res.getName(), getName());
		list.remove(res);
		update();
	}

	public void clear() {
		LOGGER.debug("Clearing playlist \"{}\": {} items", getName(), list.size());
		list.clear();
		update();
	}

	public boolean isMode(int m) {
		return list.isMode(m);
	}

	@Override
	public void discoverChildren() {
		if (!list.isEmpty()) {
			final UmsPlaylist self = this;
			// Save
			if (!isMode(IOList.AUTOSAVE)) {
				addChild(new VirtualVideoActionLocalized(renderer, "Save", true, null) {
					@Override
					public boolean enable() {
						self.save();
						return true;
					}
				});
			}
			// Clear
			addChild(new VirtualVideoActionLocalized(renderer, "Clear", true, null) {
				@Override
				public boolean enable() {
					self.clear();
					return true;
				}
			});
		}
		for (StoreResource r : list) {
			addChild(r);
			if (r instanceof StoreItem item && item.isResume()) {
				// add this non resume after
				StoreItem clone = item.clone();
				clone.setResume(null);
				addChild(clone);
			}
		}
	}

	public List<StoreResource> getList() {
		return list;
	}

	public void update() {
		if (isMode(IOList.AUTOSAVE)) {
			save();
		}
		getChildren().clear();
		setDiscovered(false);
		if (list.size() < 1 && !isMode(IOList.PERMANENT)) {
			// Self-delete if empty
			getParent().getChildren().remove(this);
		}
	}

	public void save() {
		list.save();
	}
}
