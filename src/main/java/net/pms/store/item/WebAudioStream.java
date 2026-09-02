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
import java.util.function.Supplier;
import net.pms.external.JavaHttpClient;
import net.pms.formats.Format;
import net.pms.renderers.Renderer;
import net.pms.store.IcyMetadataInputStream;
import net.pms.store.IcyMetadataReaderInputStream;
import net.pms.store.IcyMetadataSource;
import net.pms.store.NowPlayingInfo;
import net.pms.store.NowPlayingWatchInputStream;
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
	 * Whether the live title has to be read out of the stream itself. True for a plain internet
	 * radio; a stream whose titles come from an API of its own opts out, so its bytes stay
	 * untouched.
	 */
	protected boolean isIcyPassThrough() {
		return isUnboundedLiveStream();
	}

	/**
	 * The single place a stream type says what it is playing. How that is found out differs - ICY
	 * blocks, an API lookup, internal playback state - but both consumers, the ICY output towards a
	 * renderer and the push towards a control point, read it from here.
	 *
	 * @return NULL when unknown, which both consumers treat as "unchanged".
	 */
	protected NowPlayingInfo getNowPlaying() {
		return null;
	}

	@Override
	public InputStream getInputStream() {
		IcyMetadataReaderInputStream reader = isIcyPassThrough() ? openIcyReader() : null;
		if (reader != null) {
			return watched(reader, () -> NowPlayingInfo.ofStreamTitle(reader.getStreamTitle()));
		}
		InputStream plain = super.getInputStream();
		return plain == null ? null : watched(plain, this::getNowPlaying);
	}

	@Override
	public InputStream getIcyInputStream(int metaInt) {
		IcyMetadataReaderInputStream reader = isIcyPassThrough() ? openIcyReader() : null;
		if (reader != null) {
			LOGGER.info("forwarding ICY metadata for {} (renderer interval {})", getUrl(), metaInt);
			return watched(new IcyMetadataInputStream(reader, metaInt, reader::getStreamTitle),
					() -> NowPlayingInfo.ofStreamTitle(reader.getStreamTitle()));
		}
		InputStream plain = super.getInputStream();
		if (plain == null) {
			return null;
		}
		return watched(new IcyMetadataInputStream(plain, metaInt, this::currentStreamTitle), this::getNowPlaying);
	}

	private String currentStreamTitle() {
		NowPlayingInfo info = getNowPlaying();
		return info == null ? null : info.streamTitle;
	}

	private InputStream watched(InputStream stream, Supplier<NowPlayingInfo> supplier) {
		return new NowPlayingWatchInputStream(stream, getResourceId(), supplier);
	}

	/**
	 * @return a reader over the station's stream. No metadata: Fallback to the plain stream.
	 */
	private IcyMetadataReaderInputStream openIcyReader() {
		InputStream upstream = null;
		try {
			HttpResponse<InputStream> response = JavaHttpClient.getLiveStreamResponse(getUrl(), Map.of("Icy-MetaData", "1"));
			upstream = response.body();
			int upstreamMetaInt = NumberUtils.toInt(response.headers().firstValue("icy-metaint").orElse(null), 0);
			if (upstreamMetaInt > 0) {
				return new IcyMetadataReaderInputStream(upstream, upstreamMetaInt);
			}
			LOGGER.debug("web stream {} announces no icy-metaint", getUrl());
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("cannot open ICY stream from {} : {}", getUrl(), e.getMessage());
			LOGGER.trace("", e);
		}
		closeQuietly(upstream);
		return null;
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
