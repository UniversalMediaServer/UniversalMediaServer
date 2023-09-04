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

import java.io.IOException;
import java.io.InputStream;
import org.apache.hc.core5.http.HttpEntity;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for Apache HttpClient5
 */
public class HttpUtil {

	/**
	 * Ensures that the entity content is fully consumed and the content stream,
	 * if exists, is closed.<br/>
	 * <br/>
	 * This method is copied from Apache HttpClient version 4.1
	 * {@link EntityUtils#consume(HttpEntity)}, in order to keep compatibility
	 * with lower HttpClient versions, such as the "pre-BETA snapshot" used in
	 * android.
	 *
	 * @param entity
	 * @throws IOException if an error occurs reading the input stream
	 * @since 4.1
	 */
	public static void consumeEntity(final HttpEntity entity) throws IOException {
		if (entity == null) {
			return;
		}
		if (entity.isStreaming()) {
			InputStream instream = entity.getContent();
			if (instream != null) {
				instream.close();
			}
		}
	}

}
