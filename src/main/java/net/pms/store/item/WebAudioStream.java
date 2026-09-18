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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.pms.external.JavaHttpClient;
import net.pms.formats.Format;
import net.pms.renderers.Renderer;
import net.pms.store.IcyMetadataInputStream;
import net.pms.store.IcyMetadataReaderInputStream;
import net.pms.store.IcyMetadataSource;
import net.pms.store.IcyStreamTitleParser;
import net.pms.store.IcyStreamTitleParser.Order;
import net.pms.store.LiveStreamRelay;
import net.pms.store.LiveStreamRelay.Source;
import net.pms.store.NowPlayingInfo;
import net.pms.store.NowPlayingWatchInputStream;
import net.pms.store.container.PlaylistFolder;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAudioStream extends WebStream implements IcyMetadataSource {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebAudioStream.class);

	private static final Map<String, Order> ICY_ORDER_OVERRIDES = new ConcurrentHashMap<>();

	private volatile Order playlistOrder;

	/**
	 * An opened stream
	 */
	private record Connection(InputStream stream, Supplier<String> streamTitle, Supplier<NowPlayingInfo> nowPlaying) {
	}

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

	public Order getIcyTitleOrder() {
		Order fromPlaylist = getPlaylistOrder();
		Order override = ICY_ORDER_OVERRIDES.get(getUrl());
		if (override == null) {
			return fromPlaylist;
		}
		if (override == fromPlaylist) {
			// The playlist has caught up, so the override has done its job.
			ICY_ORDER_OVERRIDES.remove(getUrl());
		}
		return override;
	}

	public void setIcyTitleOrder(Order order) {
		ICY_ORDER_OVERRIDES.put(getUrl(), order == null ? Order.AUTO : order);
	}

	private Order getPlaylistOrder() {
		Order order = playlistOrder;
		if (order == null) {
			order = Order.of(getDirective(PlaylistFolder.DIRECTIVE_ICY_ORDER));
			playlistOrder = order;
		}
		return order;
	}

	@Override
	public InputStream getInputStream() {
		Connection connection = connect();
		return connection == null ? null : watched(connection.stream(), connection.nowPlaying());
	}

	@Override
	public InputStream getIcyInputStream(int metaInt) {
		Connection connection = connect();
		if (connection == null) {
			return null;
		}
		LOGGER.info("forwarding ICY metadata for {} (renderer interval {})", getUrl(), metaInt);
		return watched(new IcyMetadataInputStream(connection.stream(), metaInt, connection.streamTitle()), connection.nowPlaying());
	}

	/**
	 * Opens the station through the relay when it is an endless live stream
	 */
	private Connection connect() {
		if (isUnboundedLiveStream()) {
			// Only an endless stream is shared; a hosted file is served per request, with its ranges.
			LiveStreamRelay.Listener listener = LiveStreamRelay.listen(getUrl(), this::openSource);
			if (listener == null) {
				return null;
			}
			return connection(listener, listener.hasNowPlaying() ? listener::getNowPlaying : null);
		}
		try {
			Source source = openSource();
			if (source != null) {
				return connection(source.stream(), source.nowPlaying());
			}
		} catch (IOException e) {
			LOGGER.warn("cannot open the stream {} : {}", getUrl(), e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}


	/**
	 * Opens the bytes of the station and says where it announces what it is playing. A stream that
	 * knows its own playback - a playlist this server puts together itself - overrides this.
	 */
	protected Source openSource() throws IOException {
		IcyMetadataReaderInputStream reader = isIcyPassThrough() ? openIcyReader() : null;
		if (reader != null) {
			// The station announces a single line, which still has to be split into artist and title.
			IcyStreamTitleParser parser = newTitleParser();
			return new Source(reader, () -> parser.parse(reader.getStreamTitle()));
		}
		InputStream plain = super.getInputStream();
		return plain == null ? null : new Source(plain, this::getNowPlaying);
	}

	/**
	 * Binds the two consumers of what is playing: the ICY output towards the renderer and the push
	 * towards a control point. Both read the same source, so they can never disagree.
	 *
	 * @param nowPlaying what the opened stream announces, NULL when it announces nothing
	 */
	private Connection connection(InputStream stream, Supplier<NowPlayingInfo> nowPlaying) {
		Supplier<NowPlayingInfo> source = nowPlaying != null ? nowPlaying : this::getNowPlaying;
		return new Connection(stream, () -> streamTitleOf(source.get()), source);
	}

	private static String streamTitleOf(NowPlayingInfo info) {
		return info == null ? null : info.streamTitle;
	}

	private IcyStreamTitleParser newTitleParser() {
		return new IcyStreamTitleParser(this::getIcyTitleOrder, getName(), getUrl());
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
