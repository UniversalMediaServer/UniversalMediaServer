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
package net.pms.dlna.virtual;

import java.util.List;
import net.pms.dlna.MediaResource;
import net.pms.renderers.Renderer;
import org.apache.commons.lang3.StringUtils;

/**
 * A general-purpose free-floating folder
 */
public class UnattachedFolder extends VirtualFolder {

	public UnattachedFolder(Renderer renderer, String name) {
		super(renderer, name, null);
		setId(name);
	}

	public MediaResource add(MediaResource d) {
		if (d != null) {
			addChild(d);
			d.setId(d.getId() + "$" + getId());
			return d;
		}

		return null;
	}

	public MediaResource add(String uri, String name, Renderer r) {
		MediaResource d = autoMatch(r, uri, name);
		if (d != null) {
			// Now add the item and resolve its rendering details
			add(d);
			d.syncResolve();
		}

		return d;
	}

	public int getIndex(String objectId) {
		return getIndex(objectId, null);
	}

	public int getIndex(String objectId, Renderer r) {
		int index = indexOf(objectId);
		if (index == -1 && r != null) {
			index = indexOf(recreate(objectId, null, r).getResourceId());
		}

		return index;
	}

	public MediaResource get(String objectId, Renderer r) {
		int index = getIndex(objectId, r);
		return index > -1 ? getChildren().get(index) : null;
	}

	public List<MediaResource> asList(String objectId) {
		int index = getIndex(objectId);
		return index > -1 ? getChildren().subList(index, index + 1) : null;
	}

	// Try to recreate a lost item from a previous session
	// using its objectId's trailing uri, if any
	public MediaResource recreate(String objectId, String name, Renderer r) {
		try {
			return add(StringUtils.substringAfter(objectId, "/"), name, r);
		} catch (Exception e) {
			return null;
		}
	}

}
