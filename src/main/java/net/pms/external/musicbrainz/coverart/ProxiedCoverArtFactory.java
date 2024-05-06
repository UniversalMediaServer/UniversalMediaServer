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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import fm.last.musicbrainz.coverart.CoverArt;
import java.io.IOException;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for JDK11+ HttpClient
 */
public class ProxiedCoverArtFactory {

	private final DefaultCoverArtArchiveClient client;
	private final ObjectMapper mapper;

	public ProxiedCoverArtFactory(DefaultCoverArtArchiveClient client) {
		this.client = client;
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public CoverArt valueOf(String json) throws IOException {
		if (Strings.isNullOrEmpty(json)) {
			return null;
		}
		CoverArtBean coverArtBean = mapper.readValue(json, CoverArtBean.class);
		return new CoverArtBeanDecorator(coverArtBean, client);
	}

}
