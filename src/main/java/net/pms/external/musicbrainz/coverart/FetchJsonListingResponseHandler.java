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
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for Apache HttpClient5
 */
enum FetchJsonListingResponseHandler implements HttpClientResponseHandler<String> {
	/* */
	INSTANCE;

	@Override
	public String handleResponse(ClassicHttpResponse response) throws IOException {
		HttpEntity entity = response.getEntity();

		if (response.getCode() == HttpStatus.SC_OK) {
			try {
				return EntityUtils.toString(entity);
			} catch (ParseException ex) {
				throw new IOException("header elements cannot be parsed");
			}
		} else if (response.getCode() == HttpStatus.SC_NOT_FOUND) {
			return null;
		}
		HttpUtil.consumeEntity(entity);
		throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
	}

}
