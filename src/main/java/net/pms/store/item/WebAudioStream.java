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
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Map;
import net.pms.external.JavaHttpClient;
import net.pms.formats.Format;
import net.pms.renderers.Renderer;
import net.pms.store.IcyMetadataInputStream;
import net.pms.store.IcyMetadataReaderInputStream;
import net.pms.store.IcyMetadataSource;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAudioStream extends WebStream implements IcyMetadataSource {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebAudioStream.class);

	public WebAudioStream(Renderer renderer, String fluxName, String url, String thumbURL) {
		this(renderer, fluxName, url, thumbURL, null);
	}

	public WebAudioStream(Renderer renderer, String fluxName, String url, String thumbURL, Map<String, String> directives) {
		super(renderer, fluxName, url, thumbURL, Format.AUDIO, directives);
	}

	/**
	 * Only an endless stream carries live titles; a hosted audio file has its metadata in the DIDL.
	 */
	@Override
	public boolean isIcyMetadataEnabled() {
		return isUnboundedLiveStream();
	}

	/**
	 * Passes the station's own ICY metadata.
	 */
	@Override
	public InputStream getIcyInputStream(int metaInt) {
		InputStream upstream = null;
		try {
			HttpResponse<InputStream> response = JavaHttpClient.getLiveStreamResponse(getUrl(), Map.of("Icy-MetaData", "1"));
			upstream = response.body();
			int upstreamMetaInt = NumberUtils.toInt(response.headers().firstValue("icy-metaint").orElse(null), 0);
			if (upstreamMetaInt > 0) {
				LOGGER.info("forwarding ICY metadata for {} (station interval {}, renderer interval {})", getUrl(), upstreamMetaInt, metaInt);
				IcyMetadataReaderInputStream reader = new IcyMetadataReaderInputStream(upstream, upstreamMetaInt);
				return new IcyMetadataInputStream(reader, metaInt, reader::getStreamTitle);
			}
			LOGGER.debug("web stream {} announces no icy-metaint; forwarding audio without live titles", getUrl());
			return new IcyMetadataInputStream(upstream, metaInt, () -> null);
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("cannot open ICY stream from {} : {}", getUrl(), e.getMessage());
			LOGGER.trace("", e);
			closeQuietly(upstream);
		}
		InputStream plain = getInputStream();
		return plain != null ? new IcyMetadataInputStream(plain, metaInt, () -> null) : null;
	}

	private static void closeQuietly(InputStream stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (IOException e) {
			LOGGER.trace("closing the failed ICY stream", e);
		}
	}

}
