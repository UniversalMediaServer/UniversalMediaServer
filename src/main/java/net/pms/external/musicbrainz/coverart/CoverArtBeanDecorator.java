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
package net.pms.external.musicbrainz.coverart;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import fm.last.musicbrainz.coverart.CoverArt;
import fm.last.musicbrainz.coverart.CoverArtImage;
import java.util.Collection;
import java.util.List;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for JDK11+ HttpClient
 */
class CoverArtBeanDecorator implements CoverArt {

	private final CoverArtBean delegate;
	private final DefaultCoverArtArchiveClient client;
	private final List<CoverArtImage> coverArtImages = Lists.newArrayList();

	public CoverArtBeanDecorator(CoverArtBean delegate, DefaultCoverArtArchiveClient client) {
		this.delegate = delegate;
		this.client = client;
	}

	@Override
	public List<CoverArtImage> getImages() {
		return getProxiedCoverArtImages();
	}

	@Override
	public String getMusicBrainzReleaseUrl() {
		return delegate.getRelease();
	}

	@Override
	public CoverArtImage getImageById(long id) {
		return getImageOrNull(new IsImageWithId(id));
	}

	@Override
	public CoverArtImage getFrontImage() {
		return getImageOrNull(IsFrontImage.INSTANCE);
	}

	@Override
	public CoverArtImage getBackImage() {
		return getImageOrNull(IsBackImage.INSTANCE);
	}

	private CoverArtImage getImageOrNull(Predicate<CoverArtImage> filter) {
		Collection<CoverArtImage> filtered = Collections2.filter(getProxiedCoverArtImages(), filter);
		if (filtered.isEmpty()) {
			return null;
		}
		return filtered.iterator().next();
	}

	private List<CoverArtImage> getProxiedCoverArtImages() {
		if (coverArtImages.isEmpty()) {
			for (CoverArtImageBean image : delegate.getImages()) {
				coverArtImages.add(new ProxiedCoverArtImageBeanDecorator(image, client));
			}
		}
		return coverArtImages;
	}

}
