package net.pms.store.container.audioaddict;

import java.io.InputStream;
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
	 * ICY metadata is gated behind the "audio_addict_icy_metadata" setting so it can be turned
	 * off for renderers that do not cope with the in-band metadata.
	 */
	@Override
	public boolean isIcyMetadataEnabled() {
		return renderer.getUmsConfiguration().isAudioAddictIcyMetadata();
	}

	/**
	 * Wraps the regular stream with ICY in-band metadata. Subclasses whose title is bound to a specific source instance
	 * (the playlist) override this to bind the supplier to that instance instead.
	 */
	@Override
	public InputStream getIcyInputStream(int metaInt) {
		return new IcyMetadataInputStream(getInputStream(), metaInt, this::getStreamTitle);
	}

	/**
	 * @return the currently playing track as "Artist - Title" for ICY metadata, or NULL when
	 * the live title is unknown (treated as "unchanged" by the metadata layer).
	 */
	protected String getStreamTitle() {
		return null;
	}

	/**
	 * Diagnostic: when {@code audio_addict_capture_stream} is enabled, wraps the stream so the first
	 * chunk served is dumped to a capture file (see {@link CapturingInputStream}); otherwise returns
	 * the stream unchanged.
	 */
	protected InputStream maybeCapture(InputStream in, String label) {
		if (renderer != null && renderer.getUmsConfiguration().isAudioAddictCaptureStream()) {
			return CapturingInputStream.wrap(in, label);
		}
		return in;
	}

}
