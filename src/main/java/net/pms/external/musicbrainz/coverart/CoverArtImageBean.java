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

import java.util.List;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for JDK11+ HttpClient
 */
class CoverArtImageBean {

	private CoverArtImageThumbnailsBean thumbnails;
	private long id;
	private long edit;
	private List<String> types;
	private String image;
	private String comment;
	private boolean approved;
	private boolean front;
	private boolean back;

	public boolean isFront() {
		return front;
	}

	public void setFront(boolean front) {
		this.front = front;
	}

	public boolean isBack() {
		return back;
	}

	public void setBack(boolean back) {
		this.back = back;
	}

	public List<String> getTypes() {
		return types;
	}

	public CoverArtImageThumbnailsBean getThumbnails() {
		return thumbnails;
	}

	public void setThumbnails(CoverArtImageThumbnailsBean thumbnails) {
		this.thumbnails = thumbnails;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public long getEdit() {
		return edit;
	}

	public void setEdit(long edit) {
		this.edit = edit;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isApproved() {
		return approved;
	}

	public void setApproved(boolean approved) {
		this.approved = approved;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}