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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fm.last.musicbrainz.coverart.CoverArtImage;
import fm.last.musicbrainz.coverart.CoverArtType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for JDK11+ HttpClient
 */
class ProxiedCoverArtImageBeanDecorator implements CoverArtImage {

	private final CoverArtImageBean delegate;
	private final DefaultCoverArtArchiveClient client;

	public ProxiedCoverArtImageBeanDecorator(CoverArtImageBean delegate, DefaultCoverArtArchiveClient client) {
		this.delegate = delegate;
		this.client = client;
	}

	@Override
	public long getId() {
		return delegate.getId();
	}

	@Override
	public long getEdit() {
		return delegate.getEdit();
	}

	@Override
	public Set<CoverArtType> getTypes() {
		List<String> types = delegate.getTypes();
		return Sets.newHashSet(Lists.transform(types, CoverArtTypeStringToEnumValue.INSTANCE));
	}

	@Override
	public InputStream getImage() throws IOException {
		try {
			return client.getImageData(delegate.getImage());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Override
	public String getImageUrl() {
		return delegate.getImage();
	}

	@Override
	public boolean isFront() {
		return delegate.isFront();
	}

	@Override
	public boolean isBack() {
		return delegate.isBack();
	}

	@Override
	public String getComment() {
		return delegate.getComment();
	}

	@Override
	public boolean isApproved() {
		return delegate.isApproved();
	}

	@Override
	public InputStream getLargeThumbnail() throws IOException {
		try {
			return client.getImageData(delegate.getThumbnails().getLarge());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Override
	public String getLargeThumbnailUrl() {
		return delegate.getThumbnails().getLarge();
	}

	@Override
	public InputStream getSmallThumbnail() throws IOException {
		try {
			return client.getImageData(delegate.getThumbnails().getSmall());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Override
	public String getSmallThumbnailUrl() {
		return delegate.getThumbnails().getSmall();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProxiedCoverArtImage [id=");
		builder.append(getId());
		builder.append(", edit=");
		builder.append(getEdit());
		builder.append(", types=");
		builder.append(getTypes());
		builder.append(", front=");
		builder.append(isFront());
		builder.append(", back=");
		builder.append(isBack());
		builder.append(", comment=");
		builder.append(getComment());
		builder.append(", approved=");
		builder.append(isApproved());
		builder.append(", image=");
		builder.append(delegate.getImage());
		builder.append("]");
		return builder.toString();
	}

}