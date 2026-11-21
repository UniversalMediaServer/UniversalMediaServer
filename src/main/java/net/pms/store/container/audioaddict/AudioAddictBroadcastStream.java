package net.pms.store.container.audioaddict;

import net.pms.renderers.Renderer;
import net.pms.store.IcyMetadataSource;
import net.pms.store.item.WebAudioStream;

/**
 * Base for the AudioAddict playable streams that are continuous live broadcasts: curated
 * playlists (DI.fm), radio channels and events (DI.fm). All of them are served radio-style, so radio-aware renderers
 * may request ICY in-band metadata and can display the live track title.
 * <p>
 * The only thing that differs between the three types is where the live title comes from.
 */
public abstract class AudioAddictBroadcastStream extends WebAudioStream implements IcyMetadataSource {

	protected AudioAddictBroadcastStream(Renderer renderer, String fluxName, String url, String thumbURL) {
		super(renderer, fluxName, url, thumbURL);
	}

	/**
	 * Endless, non-seekable live stream.
	 */
	@Override
	public boolean isUnboundedLiveStream() {
		return true;
	}

	/**
	 * Advertise these continuous streams (playlist + radio) as internet radio
	 * "object.item.audioItem.audioBroadcast" so an OpenHome control points can play them via
	 * the renderer's Radio source (where ICY metadata is consumed), rather than its Playlist source.
	 */
	@Override
	public boolean isAudioBroadcast() {
		return true;
	}

	/**
	 * The live title comes from the AudioAddict API, not from the stream, so the bytes are passed
	 * through untouched.
	 */
	@Override
	protected boolean isIcyPassThrough() {
		return false;
	}

	/**
	 * ICY metadata is gated behind the "audio_addict_icy_metadata" setting so it can be turned
	 * off for renderers that do not cope with the in-band metadata.
	 */
	@Override
	public boolean isIcyMetadataEnabled() {
		return renderer.getUmsConfiguration().isAudioAddictIcyMetadata();
	}

}
