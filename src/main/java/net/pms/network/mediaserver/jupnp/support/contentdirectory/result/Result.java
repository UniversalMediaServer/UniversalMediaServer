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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result;

import java.util.ArrayList;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.BaseObject;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Desc;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;

public class Result {

	public static final String NAMESPACE_URI = "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/";

	protected List<Container> containers = new ArrayList<>();
	protected List<Item> items = new ArrayList<>();
	protected List<Desc> descriptions = new ArrayList<>();

	public Result addContainer(Container container) {
		containers.add(container);
		return this;
	}

	public List<Container> getContainers() {
		return containers;
	}

	public void setContainers(List<Container> containers) {
		this.containers = containers;
	}

	/**
	 * Adds {@link Item} or {@link Container} typed instances, ignores
	 * everything else.
	 */
	public Result addObject(BaseObject object) {
		if (object instanceof Item item) {
			addItem(item);
		} else if (object instanceof Container container) {
			addContainer(container);
		}
		return this;
	}

	public Result addItem(Item item) {
		items.add(item);
		return this;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public Result addDescription(Desc description) {
		this.descriptions.add(description);
		return this;
	}

	public List<Desc> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<Desc> descriptions) {
		this.descriptions = descriptions;
	}

	public long getCount() {
		return containers.size() + items.size();
	}

	@Override
	public String toString() {
		return new Generator().generate(this);
	}

}
