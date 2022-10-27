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
package net.pms.configuration.sharedcontent;

public abstract class Stream extends SharedContent {
	private String parent;
	private String name;
	private String uri;
	private String thumbnail;

	protected Stream(String parent, String name, String uri, String thumbnail) {
		this.parent = parent;
		this.name = name;
		this.uri = uri;
		this.thumbnail = thumbnail;
	}

	public void setParent(String value) {
		parent = value;
	}

	public String getParent() {
		return parent;
	}

	public void setName(String value) {
		name = value;
	}

	public String getName() {
		return name;
	}

	public void setUri(String value) {
		uri = value;
	}

	public String getUri() {
		return uri;
	}

	public void setThumbnail(String value) {
		thumbnail = value;
	}

	public String getThumbnail() {
		return thumbnail;
	}

}
