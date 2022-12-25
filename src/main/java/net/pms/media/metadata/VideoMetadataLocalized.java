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
package net.pms.media.metadata;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class VideoMetadataLocalized {
	private String homepage;
	private String overview;
	private String poster;
	private String tagline;
	private String title;
	private Long tmdbID;

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String value) {
		this.homepage = value;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String value) {
		this.overview = value;
	}

	public String getPoster() {
		return poster;
	}

	public void setPoster(String value) {
		this.poster = value;
	}

	public String getTagline() {
		return tagline;
	}

	public void setTagline(String value) {
		this.tagline = value;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	public Long getTmdbID() {
		return tmdbID;
	}

	public void setTmdbID(Long value) {
		this.tmdbID = value;
	}

	public void localizeJsonObject(final JsonObject jsonObject) {
		if (StringUtils.isNotBlank(homepage)) {
			jsonObject.remove("homepage");
			jsonObject.addProperty("homepage", homepage);
		}
		if (StringUtils.isNotBlank(overview)) {
			jsonObject.remove("overview");
			jsonObject.addProperty("overview", overview);
		}
		if (StringUtils.isNotBlank(poster)) {
			jsonObject.remove("poster");
			jsonObject.addProperty("poster", poster);
		}
		if (StringUtils.isNotBlank(tagline)) {
			jsonObject.remove("tagline");
			jsonObject.addProperty("tagline", tagline);
		}
		if (StringUtils.isNotBlank(title)) {
			jsonObject.remove("title");
			jsonObject.addProperty("title", title);
		}
	}
}
