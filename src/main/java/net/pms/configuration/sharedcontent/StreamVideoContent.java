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

import net.pms.formats.Format;

public class StreamVideoContent extends StreamContent {
	protected static final String TYPE = "StreamVideo";

	public StreamVideoContent(String parent, String uri) {
		super(parent, null, uri, null);
	}

	public StreamVideoContent(String parent, String name, String uri) {
		super(parent, name, uri, null);
	}

	public StreamVideoContent(String parent, String name, String uri, String thumbnail) {
		super(parent, name, uri, thumbnail);
	}

	@Override
	public int getFormat() {
		return Format.VIDEO;
	}

	@Override
	public String getType() {
		return TYPE;
	}
}
