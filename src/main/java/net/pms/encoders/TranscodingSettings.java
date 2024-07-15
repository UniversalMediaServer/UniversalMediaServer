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
package net.pms.encoders;

import java.util.ArrayList;
import java.util.List;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author SurfaceS
 */
public class TranscodingSettings {

	private static final Logger LOGGER = LoggerFactory.getLogger(FormatFactory.class);

	private final Engine engine;
	private final EncodingFormat encodingFormat;

	public TranscodingSettings(Engine engine, EncodingFormat encodingFormat) {
		this.engine = engine;
		this.encodingFormat = encodingFormat;
	}

	public Engine getEngine() {
		return engine;
	}

	public EncodingFormat getEncodingFormat() {
		return encodingFormat;
	}

	/**
	 * Determine the mime type specific for this renderer, given a generic mime
	 * type by item. This translation takes into account all configured "Supported"
	 * lines and mime type aliases for this renderer.
	 *
	 * @param item The item with the generic mime type.
	 * @return The renderer specific mime type for given item. If the generic mime
	 * type given by item is <code>null</code> this method returns <code>null</code>.
	 */
	public String getMimeType(StoreItem item) {
		String mimeType = getEngine().getMimeType();
		if (mimeType == null) {
			return null;
		}

		String matchedMimeType = null;

		if (item.getDefaultRenderer().isUseMediaInfo() && (HTTPResource.VIDEO_TRANSCODE.equals(mimeType) || HTTPResource.AUDIO_TRANSCODE.equals(mimeType))) {
			// Use the supported information in the configuration to determine the transcoding mime type.
			matchedMimeType = item.getDefaultRenderer().getFormatConfiguration().getMatchedMIMEtype(getEncodingFormat().getTranscodingContainer(), getEncodingFormat().getTranscodingVideoCodec(), getEncodingFormat().getTranscodingAudioCodec());
		}

		if (matchedMimeType == null) {
			// No match found in the renderer config, try our defaults
			if (HTTPResource.VIDEO_TRANSCODE.equals(mimeType)) {
				if (getEncodingFormat().isTranscodeToWMV()) {
					matchedMimeType = HTTPResource.WMV_TYPEMIME;
				} else if (getEncodingFormat().isTranscodeToMPEGTS()) {
					// Default video transcoding mime type
					matchedMimeType = HTTPResource.MPEGTS_TYPEMIME;
				} else if (getEncodingFormat().isTranscodeToMP4()) {
					// Default video transcoding mime type
					matchedMimeType = HTTPResource.MP4_TYPEMIME;
				} else {
					// Default video transcoding mime type
					matchedMimeType = HTTPResource.MPEG_TYPEMIME;
				}
			} else if (HTTPResource.AUDIO_TRANSCODE.equals(mimeType)) {
				if (getEncodingFormat().isTranscodeToWAV()) {
					matchedMimeType = HTTPResource.AUDIO_WAV_TYPEMIME;
				} else if (getEncodingFormat().isTranscodeToMP3()) {
					matchedMimeType = HTTPResource.AUDIO_MP3_TYPEMIME;
				} else {
					// Default audio transcoding mime type
					matchedMimeType = HTTPResource.AUDIO_LPCM_TYPEMIME;
				}
			}
		}
		if (HTTPResource.AUDIO_TRANSCODE.equals(mimeType) && HTTPResource.AUDIO_LPCM_TYPEMIME.equals(matchedMimeType)) {
			MediaInfo mediaInfo = item.getMediaInfo();
			if (item.getDefaultRenderer().getUmsConfiguration().isAudioResample()) {
				if (item.getDefaultRenderer().isTranscodeAudioTo441()) {
					matchedMimeType += ";rate=44100;channels=2";
				} else {
					matchedMimeType += ";rate=48000;channels=2";
				}
			} else if (mediaInfo != null && mediaInfo.getDefaultAudioTrack() != null) {
				MediaAudio audio = mediaInfo.getDefaultAudioTrack();
				if (audio.getSampleRate() > 0) {
					matchedMimeType += ";rate=" + Integer.toString(audio.getSampleRate());
				}
				if (audio.getNumberOfChannels() > 0) {
					matchedMimeType += ";channels=" + Integer.toString(audio.getNumberOfChannels());
				}
			}
		}

		if (matchedMimeType == null) {
			matchedMimeType = mimeType;
		}

		// Apply renderer specific mime type aliases
		if (item.getDefaultRenderer().getMimeTranslations().containsKey(matchedMimeType)) {
			return item.getDefaultRenderer().getMimeTranslations().get(matchedMimeType);
		}

		return matchedMimeType;
	}

	public String getId() {
		return engine.getEngineId() + "|" + encodingFormat;
	}

	@Override
	public String toString() {
		return engine + " (" + encodingFormat + ")";
	}

	/**
	 * Returns the first {@link Engine} that matches the given
	 * {@link StoreItem}.
	 *
	 * Each of the available {@link Engine} instances are passed the provided
	 * information and the first that reports that it is compatible will be
	 * returned.
	 *
	 * @param item the {@link StoreItem} to match.
	 * @return The {@link Engine} if a match could be found, {@code null}
	 * otherwise.
	 */
	public static TranscodingSettings getBestTranscodingSettings(final StoreItem item) {
		if (item == null) {
			LOGGER.warn("Invalid resource (null): no engine found");
			return null;
		}
		LOGGER.trace("Getting engine for resource \"{}\"", item.getName());
		boolean isImage = item.getMediaInfo() != null && item.getMediaInfo().isImage();
		List<EncodingFormat> encodingFormats = item.getDefaultRenderer().getTranscodingFormats();
		List<Engine> engines = EngineFactory.getEngines();

		for (EncodingFormat encodingFormat : encodingFormats) {
			for (Engine engine : engines) {
				if (isImage && !(engine instanceof ImageEngine)) {
					continue;
				}
				boolean compatible = engine.isCompatible(encodingFormat) && engine.isCompatible(item);
				if (compatible) {
					// Engine is enabled and compatible
					LOGGER.trace("Returning compatible engine \"{}\"", engine.getName());
					return new TranscodingSettings(engine, encodingFormat);
				} else if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Engine \"{}\" is incompatible", engine.getName());
				}
			}
		}

		LOGGER.trace("No engine found for {}", item.getName());

		return null;
	}

	/**
	 * Returns all {@link TranscodingSettings}s that match the given resource and are
	 * enabled.
	 *
	 * Each of the available {@link Engine}s are passed the provided information
	 * and each {@link TranscodingSettings} that reports that it is compatible will be
	 * returned.
	 *
	 * @param item the {@link StoreItem} to match.
	 * @return A list of compatible {@link TranscodingSettings}s if a match could be found,
	 * {@code null} otherwise.
	 */
	public static List<TranscodingSettings> getTranscodingsSettings(final StoreItem item) {
		List<TranscodingSettings> compatibleEngines = new ArrayList<>();
		if (item == null) {
			return compatibleEngines;
		}
		List<EncodingFormat> encodingFormats = item.getDefaultRenderer().getTranscodingFormats();
		List<Engine> engines = EngineFactory.getEngines();
		for (EncodingFormat encodingFormat : encodingFormats) {
			for (Engine engine : engines) {
				if (engine.isCompatible(encodingFormat) && engine.isCompatible(item)) {
					// Engine is enabled, available and compatible
					LOGGER.trace("Engine {} is compatible with resource \"{}\"", engine.getName(), item.getName());
					compatibleEngines.add(new TranscodingSettings(engine, encodingFormat));
				}
			}
		}
		return compatibleEngines;
	}

	/**
	 * Returns the list of enabled and available TranscodingSettings ordered by priority.
	 *
	 * @return The list of TranscodingSettings.
	 */
	public static List<TranscodingSettings> getTranscodingsSettings(final Renderer renderer) {
		List<TranscodingSettings> availableEngines = new ArrayList<>();
		if (renderer == null) {
			return availableEngines;
		}
		List<EncodingFormat> encodingFormats = renderer.getTranscodingFormats();
		List<Engine> engines = EngineFactory.getEngines();
		for (EncodingFormat encodingFormat : encodingFormats) {
			for (Engine engine : engines) {
				availableEngines.add(new TranscodingSettings(engine, encodingFormat));
			}
		}
		return availableEngines;
	}

	/**
	 * Returns the TranscodingSettings from string.
	 *
	 * @return The TranscodingSettings.
	 */
	public static TranscodingSettings getTranscodingSettings(final String transcodingId) {
		if (StringUtils.isBlank(transcodingId) || !transcodingId.contains("|")) {
			return null;
		}
		String[] transcodingParts = transcodingId.split("\\|");
		String engineIdString = transcodingParts[0].trim();
		String encodingFormatName = transcodingParts[1].trim();
		EngineId engineId = StandardEngineId.toEngineID(engineIdString);
		EncodingFormat encodingFormat = EncodingFormat.getEncodingFormat(encodingFormatName);
		if (engineId != null && encodingFormat != null) {
			Engine engine = EngineFactory.getEngine(engineId, false, false);
			return new TranscodingSettings(engine, encodingFormat);
		}
		return null;
	}

}
