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
package net.pms.renderers.devices.players;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.network.mediaserver.UPNPControl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaylistItem extends PlayerItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistItem.class);
	private static final Matcher DC_TITLE = Pattern.compile("<dc:title>(.+)</dc:title>").matcher("");

	public PlaylistItem(String uri, String name, String metadata) {
		this.uri = uri;
		this.name = name;
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		if (StringUtils.isBlank(name)) {
			try {
				name = (!StringUtils.isEmpty(metadata) && DC_TITLE.reset(UPNPControl.unescape(metadata)).find()) ?
					DC_TITLE.group(1) :
					new File(StringUtils.substringBefore(UPNPControl.unescape(uri), "?")).getName();
			} catch (IllegalArgumentException e) {
				LOGGER.error("URL decoding error ", e);
			}
		}
		return name;
	}

	@Override
	public boolean equals(Object other) {
		return (
			other != null && (
				other == this ||
				(other instanceof PlaylistItem item && item.uri.equals(uri)) ||
				other.toString().equals(uri)
			)
		);
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}
}
