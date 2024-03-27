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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item;

import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.StorageMediumValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * An imageItem instance represents a still image object.
 *
 * It typically has at least one res property. This class is derived from the
 * item class and inherits the properties defined by that class.
 */
public class ImageItem extends Item {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_IMAGEITEM_TYPE);

	public ImageItem() {
		setUpnpClass(CLASS);
	}

	public ImageItem(Item other) {
		super(other);
	}

	public ImageItem(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public ImageItem(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, CLASS);
		if (resource != null) {
			setResources(resource);
		}
	}

	public String getLongDescription() {
		return properties.getValue(UPNP.LongDescription.class);
	}

	public ImageItem setLongDescription(String description) {
		properties.set(new UPNP.LongDescription(description));
		return this;
	}

	public String getStorageMedium() {
		return properties.getValue(UPNP.StorageMedium.class);
	}

	public ImageItem setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	public ImageItem setStorageMedium(StorageMediumValue storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	public String getRating() {
		return properties.getValue(UPNP.Rating.class);
	}

	public ImageItem setRating(String rating) {
		properties.set(new UPNP.Rating(rating));
		return this;
	}

	public String getDescription() {
		return properties.getValue(DC.Description.class);
	}

	public ImageItem setDescription(String description) {
		properties.set(new DC.Description(description));
		return this;
	}

	public String getFirstPublisher() {
		return properties.getValue(DC.Publisher.class);
	}

	public String[] getPublishers() {
		List<String> list = properties.getValues(DC.Publisher.class);
		return list.toArray(String[]::new);
	}

	public ImageItem setPublishers(String[] publishers) {
		properties.remove(DC.Publisher.class);
		for (String publisher : publishers) {
			properties.add(new DC.Publisher(publisher));
		}
		return this;
	}

	public String getDate() {
		return properties.getValue(DC.Date.class);
	}

	public ImageItem setDate(String date) {
		properties.set(new DC.Date(date));
		return this;
	}

	public String getFirstRights() {
		return properties.getValue(DC.Rights.class);
	}

	public String[] getRights() {
		List<String> list = properties.getValues(DC.Rights.class);
		return list.toArray(String[]::new);
	}

	public ImageItem setRights(String[] rights) {
		properties.remove(DC.Rights.class);
		for (String right : rights) {
			properties.add(new DC.Rights(right));
		}
		return this;
	}

}
