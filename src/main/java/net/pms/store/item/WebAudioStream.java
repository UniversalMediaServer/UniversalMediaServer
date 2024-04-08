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
package net.pms.store.item;

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.formats.Format;
import net.pms.renderers.Renderer;
import net.pms.store.StoreResource;

public class WebAudioStream extends WebStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebAudioStream.class);
	private String mime = null;

	public WebAudioStream(Renderer renderer, String fluxName, String url, String thumbURL) {
		super(renderer, fluxName, url, thumbURL, Format.AUDIO);
		try {
			fetchThumbnailInputStream();
		} catch (IOException e) {
			LOGGER.error("cannot retrieve album art for resource {}", url, e);
		}
	}

	@Override
	public boolean isCodeValid(StoreResource r) {
		return false;
	}

	public void setMimeType(String mime) {
		this.mime = mime;
	}

	@Override
	public String getRendererMimeType() {
		if (StringUtils.isAllBlank(mime)) {
			return super.getRendererMimeType();
		} else {
			return mime;
		}
	}
}
