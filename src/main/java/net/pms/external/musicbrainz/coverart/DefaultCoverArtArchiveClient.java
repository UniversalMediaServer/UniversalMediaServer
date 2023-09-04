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

import fm.last.musicbrainz.coverart.CoverArt;
import fm.last.musicbrainz.coverart.CoverArtArchiveClient;
import fm.last.musicbrainz.coverart.CoverArtException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for Apache HttpClient5
 */
public class DefaultCoverArtArchiveClient implements CoverArtArchiveClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCoverArtArchiveClient.class);

	private static final String API_DOMAIN = "coverartarchive.org/";
	private static final String API_ROOT = "http://" + API_DOMAIN;
	private static final String API_ROOT_HTTPS = "https://" + API_DOMAIN;

	private final HttpClient client;
	private final ProxiedCoverArtFactory factory = new ProxiedCoverArtFactory(this);

	private final HttpClientResponseHandler<String> fetchJsonListingHandler = FetchJsonListingResponseHandler.INSTANCE;
	private final HttpClientResponseHandler<InputStream> fetchImageDataHandler = FetchImageDataResponseHandler.INSTANCE;

	private boolean useHttps;

	/**
	 * Creates a client that communicates using (unsecured) HTTP. To use HTTPS,
	 * see the other constructors.
	 */
	public DefaultCoverArtArchiveClient() {
		this(false);
	}

	/**
	 * Creates a client that explicitly communicates with or without secure
	 * HTTP.
	 *
	 * @param useHttps <code>true</code> to use HTTPS to connect to
	 * coverartarchive.<br/>
	 * <i>Note:</i> this only applies to communication with coverartarchive.org.
	 * They might return plain HTTP links to image files. You might want to
	 * handle this yourself using {@link CoverArtImage}.
	 * <code>getXYZUrl()</code> methods, such as
	 * {@link CoverArtImage#getImageUrl()}.
	 */
	public DefaultCoverArtArchiveClient(boolean useHttps) {
		this(useHttps, null);
	}

	/**
	 * Allows for using a custom HTTP client. This might be necessary when the
	 * default client used here does not suit the needs. For example to replace
	 * the outdated version shipped with android.
	 *
	 * @param useHttps <code>true</code> uses HTTPS to connect to
	 * coverartarchive. <br/>
	 * <i>Note:</i> this only applies to communication with coverartarchive.org.
	 * They might return plain HTTP links to image files. You might want to
	 * handle this yourself using {@link CoverArtImage}.
	 * <code>getXYZUrl()</code> methods, such as
	 * {@link CoverArtImage#getImageUrl()}.
	 * @param client custom client. <code>null</code> results in using the
	 * default client
	 */
	public DefaultCoverArtArchiveClient(boolean useHttps, HttpClient client) {
		if (client == null) {
			this.client = HttpClients.createDefault();
		} else {
			this.client = client;
		}
		this.useHttps = useHttps;
	}

	public boolean isUsingHttps() {
		return useHttps;
	}

	@Override
	public CoverArt getByMbid(UUID mbid) throws CoverArtException {
		return getByMbid(CoverArtArchiveEntity.RELEASE, mbid);
	}

	@Override
	public CoverArt getReleaseGroupByMbid(UUID mbid) throws CoverArtException {
		return getByMbid(CoverArtArchiveEntity.RELEASE_GROUP, mbid);
	}

	private CoverArt getByMbid(CoverArtArchiveEntity entity, UUID mbid) {
		LOGGER.info("mbid={}", mbid);
		HttpGet getRequest = getJsonGetRequest(entity, mbid);
		CoverArt coverArt = null;
		try {
			String json = client.execute(getRequest, fetchJsonListingHandler);
			coverArt = factory.valueOf(json);
		} catch (IOException e) {
			throw new CoverArtException(e);
		}
		return coverArt;
	}

	InputStream getImageData(String location) throws IOException {
		LOGGER.info("location={}", location);
		HttpGet getRequest = getJpegGetRequest(location);
		return client.execute(getRequest, fetchImageDataHandler);
	}

	private HttpGet getJpegGetRequest(String location) {
		HttpGet getRequest = new HttpGet(location);
		getRequest.addHeader("accept", "image/jpeg");
		return getRequest;
	}

	private HttpGet getJsonGetRequest(CoverArtArchiveEntity entity, UUID mbid) {
		String url;
		if (useHttps) {
			url = API_ROOT_HTTPS;
		} else {
			url = API_ROOT;
		}
		url += entity.getUrlParam() + mbid;
		HttpGet getRequest = new HttpGet(url);
		getRequest.addHeader("accept", "application/json");
		return getRequest;
	}

}
