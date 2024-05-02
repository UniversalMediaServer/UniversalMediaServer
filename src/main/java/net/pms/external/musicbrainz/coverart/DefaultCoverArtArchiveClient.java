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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (C) 2012-2018 Last.fm
 *
 * Adapted for JDK11+ HttpClient
 */
public class DefaultCoverArtArchiveClient implements CoverArtArchiveClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCoverArtArchiveClient.class);

	private static final String API_DOMAIN = "coverartarchive.org/";
	private static final String API_ROOT = "http://" + API_DOMAIN;
	private static final String API_ROOT_HTTPS = "https://" + API_DOMAIN;

	private final HttpClient client;
	private final ProxiedCoverArtFactory factory = new ProxiedCoverArtFactory(this);

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
			this.client = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build();
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
		try {
			return getByMbid(CoverArtArchiveEntity.RELEASE, mbid);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Override
	public CoverArt getReleaseGroupByMbid(UUID mbid) throws CoverArtException {
		try {
			return getByMbid(CoverArtArchiveEntity.RELEASE_GROUP, mbid);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private CoverArt getByMbid(CoverArtArchiveEntity entity, UUID mbid) throws InterruptedException, CoverArtResponseException {
		LOGGER.info("mbid={}", mbid);
		HttpRequest getRequest = getJsonGetRequest(entity, mbid);
		CoverArt coverArt = null;
		try {
			HttpResponse<String> response = client.send(getRequest, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new CoverArtResponseException(response.statusCode(), response.body());
			}
			String json = response.body();
			coverArt = factory.valueOf(json);
		} catch (IOException e) {
			throw new CoverArtException(e);
		}
		return coverArt;
	}

	InputStream getImageData(String location) throws IOException, InterruptedException {
		LOGGER.info("location={}", location);
		HttpRequest getRequest = getJpegGetRequest(location);
		return client.send(getRequest, BodyHandlers.ofInputStream()).body();
	}

	private HttpRequest getJpegGetRequest(String location) {
		try {
			URI uri = new URI(location);
			return HttpRequest.newBuilder()
					.uri(uri)
					.setHeader("accept", "image/jpeg")
					.GET()
					.build();
		} catch (URISyntaxException ex) {
			return null;
		}
	}

	private HttpRequest getJsonGetRequest(CoverArtArchiveEntity entity, UUID mbid) {
		String url;
		if (useHttps) {
			url = API_ROOT_HTTPS;
		} else {
			url = API_ROOT;
		}
		url += entity.getUrlParam() + mbid;
		URI uri;
		try {
			uri = new URI(url);
			return HttpRequest.newBuilder()
					.uri(uri)
					.setHeader("accept", "application/json")
					.GET()
					.build();
		} catch (URISyntaxException ex) {
			return null;
		}
	}

}
