/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item;

import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.StorageMediumValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * An audioBook instance represents audio content that is the narration of a
 * book (as opposed to, for example, a news broadcast or a song).
 *
 * It typically has at least one res property. This class is derived from the
 * audioItem class and inherits the properties defined by that class.
 */
public class AudioBook extends AudioItem {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_AUDIOITEM_AUDIOBOOK_TYPE);

	public AudioBook() {
		setUpnpClass(CLASS);
	}

	public AudioBook(Item other) {
		super(other);
	}

	public AudioBook(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, null, null, null, resource);
	}

	public AudioBook(String id, Container parent, String title, String creator, String producer, String contributor, String date, Res... resource) {
		this(id, parent.getId(), title, creator, producer, contributor, date, resource);
	}

	public AudioBook(String id, String parentID, String title, String creator, String producer, String contributor, String date, Res... resource) {
		super(id, parentID, title, creator, resource);
		setUpnpClass(CLASS);
		if (producer != null) {
			properties.add(new UPNP.Producer(producer));
		}
		if (contributor != null) {
			properties.add(new DC.Contributor(contributor));
		}
		if (date != null) {
			setDate(date);
		}
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getStorageMedium() {
		return properties.getValue(UPNP.StorageMedium.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public AudioBook setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public AudioBook setStorageMedium(StorageMediumValue storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstProducer() {
		return properties.getValue(UPNP.Producer.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getProducers() {
		List<String> list = properties.getValues(UPNP.Producer.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public AudioBook setProducers(String[] producers) {
		properties.remove(UPNP.Producer.class);
		for (String producer : producers) {
			properties.add(new UPNP.Producer(producer));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstContributor() {
		return properties.getValue(DC.Contributor.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getContributors() {
		List<String> list = properties.getValues(DC.Contributor.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public AudioBook setContributors(String[] contributors) {
		properties.remove(DC.Contributor.class);
		for (String contributor : contributors) {
			properties.add(new DC.Contributor(contributor));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getDate() {
		return properties.getValue(DC.Date.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public final AudioBook setDate(String date) {
		properties.set(new DC.Date(date));
		return this;
	}

}
