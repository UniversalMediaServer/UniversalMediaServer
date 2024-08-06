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
package net.pms.store.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.TranscodingSettings;
import net.pms.image.BufferedImageFilterChain;
import net.pms.media.MediaLang;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.renderers.Renderer;
import net.pms.store.LibraryItemSorter;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class populates the file-specific transcode folder with content.
 */
public class FileTranscodeVirtualFolder extends TranscodeVirtualFolder {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileTranscodeVirtualFolder.class);
	private final StoreItem originalResource;

	public FileTranscodeVirtualFolder(Renderer renderer, StoreItem resource) {
		super(renderer, resource.getDisplayNameBase());
		originalResource = resource;
	}

	/**
	 * Create a copy of the provided original resource and optionally set the
	 * copy's audio track, subtitle track and engine.
	 *
	 * @param original The original {@link StoreResource} to create a copy of.
	 * @param audio The audio track to use.
	 * @param subtitle The subtitle track to use.
	 * @param engine The engine to use.
	 * @return The copy.
	 */
	private static StoreItem createResourceWithAudioSubtitleEngine(
			StoreItem original,
			MediaAudio audio,
			MediaSubtitle subtitle,
			TranscodingSettings transcodingSettings) {
		// FIXME clone is broken. should be e.g. original.newInstance()
		StoreItem copy = original.clone();
		copy.setMediaInfo(original.getMediaInfo());
		copy.setNoName(true);
		copy.setMediaAudio(audio);
		copy.setMediaSubtitle(subtitle);
		copy.setTranscodingSettings(transcodingSettings);

		return copy;
	}

	private void addChapterFolder(StoreItem resource) {
		if (!resource.getFormat().isVideo()) {
			return;
		}

		int chapterInterval = renderer.getUmsConfiguration().isChapterSupport() ?
				renderer.getUmsConfiguration().getChapterInterval() : -1;

		if ((chapterInterval > 0) && resource.isTimeSeekable()) {
			// don't add a chapter folder if the duration of the video
			// is less than the chapter length.
			double duration = resource.getMediaInfo().getDurationInSeconds(); // 0 if the duration is unknown
			if (duration != 0 && duration <= (chapterInterval * 60)) {
				return;
			}

			StoreResource copy = resource.clone();
			copy.setNoName(true);
			ChapterFileTranscodeVirtualFolder chapterFolder = new ChapterFileTranscodeVirtualFolder(
					renderer,
					resource.getDisplayName(),
					copy,
					chapterInterval);

			addChildInternal(chapterFolder);
		}
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		try {
			return originalResource.getThumbnailInputStream();
		} catch (IOException e) {
			return super.getThumbnailInputStream();
		}
	}

	@Override
	public void checkThumbnail() {
		originalResource.checkThumbnail();
	}

	/**
	 * This populates the file-specific transcode folder with all combinations
	 * of engines, audio tracks and subtitles.
	 */
	@Override
	protected void resolveOnce() {
		if (getChildren().isEmpty()) { // OK
			originalResource.syncResolve();
			if (originalResource.getMediaInfo() != null && originalResource.getMediaInfo().isVideo()) {
				originalResource.registerExternalSubtitles(true);
			}

			// create copies of the audio/subtitle track lists as we're making (local)
			// modifications to them
			List<MediaAudio> audioTracks = new ArrayList<>(originalResource.getMediaInfo().getAudioTracks());
			List<MediaSubtitle> subtitlesTracks;
			if (originalResource.getMediaSubtitle() != null) {
				// Transcode folder of live subtitles folder
				subtitlesTracks = Collections.singletonList(originalResource.getMediaSubtitle());
			} else {
				subtitlesTracks = new ArrayList<>(originalResource.getMediaInfo().getSubtitlesTracks());
			}

			// If there is a single audio track, set that as audio track
			// for non-transcoded entries to show the correct language
			MediaAudio singleAudioTrack = audioTracks.size() == 1 ? audioTracks.get(0) : null;

			// assemble copies for each combination of audio, subtitle and engine
			ArrayList<StoreItem> entries = new ArrayList<>();

			// First, add the option to simply stream the resource.
			LOGGER.trace(
					"Duplicating {} for direct streaming to renderer: {}",
					originalResource.getName(),
					renderer.getRendererName()
			);

			StoreItem noTranscode = createResourceWithAudioSubtitleEngine(originalResource, singleAudioTrack, null, null);
			addChildInternal(noTranscode);
			addChapterFolder(noTranscode);

			/*
			 We add (or may add) a null entry to the audio list and/or subtitle list
			 to ensure the inner loop is always entered:

			 for audio in audioTracks:
			 for subtitle in subtitleTracks:
			 for engine in engines:
			 newResource(audio, subtitle, engine)

			 there are 4 different scenarios:

			 1) a file with audio tracks and no subtitles (subtitle == null): in that case we want
			 to assign a engine for each audio track

			 2) a file with subtitles and no audio tracks (audio == null): in that case we want
			 to assign a engine for each subtitle track

			 3) a file with no audio tracks (audio == null) and no subtitles (subtitle == null)
			 e.g. an audio file, a video with no sound and no subtitles or a web audio/video file:
			 in that case we still want to provide a selection of engines e.g. FFmpeg Web Video
			 and VLC Web Video for a web video or FFmpeg Audio and MPlayer Audio for an audio file

			 4) one or more audio tracks AND one or more subtitle tracks: this is the case this code
			 used to handle when it solely dealt with (local) video files: assign a engine
			 for each combination of audio track and subtitle track

			 If a null audio or subtitle track is passed to createResourceWithAudioSubtitleEngine,
			 it sets the copy's corresponding mediaAudio (AKA params.aid) or mediaSubtitle
			 (AKA params.sid) value to null.
			 */
			if (audioTracks.isEmpty()) {
				audioTracks.add(null);
			}

			if (originalResource.getMediaSubtitle() == null) {
				if (subtitlesTracks.isEmpty()) {
					subtitlesTracks.add(null);
				} else {
					// if there are subtitles, make sure a no-subtitle option is added
					// for each engine
					MediaSubtitle noSubtitle = new MediaSubtitle();
					noSubtitle.setId(MediaLang.DUMMY_ID);
					subtitlesTracks.add(noSubtitle);
				}
			}

			for (MediaAudio audio : audioTracks) {
				// Create combinations of all audio tracks, subtitles and engines.
				for (MediaSubtitle subtitle : subtitlesTracks) {
					// Create a temporary copy of the child with the audio and
					// subtitle modified in order to be able to match engines to it.
					StoreItem temp = createResourceWithAudioSubtitleEngine(originalResource, audio, subtitle, null);

					// Determine which transcodingSettings match this audio track and subtitle
					List<TranscodingSettings> transcodingsSettings = TranscodingSettings.getTranscodingsSettings(temp);

					// create a copy for each compatible engine
					for (TranscodingSettings transcodingSettings : transcodingsSettings) {
						StoreItem copy = createResourceWithAudioSubtitleEngine(originalResource, audio, subtitle, transcodingSettings);
						entries.add(copy);
					}
				}
			}

			if (renderer.isSubtitlesStreamingSupportedForAllFiletypes()) {
				// Add a no-transcode entry for each streamable external subtitles
				for (MediaSubtitle subtitlesTrack : subtitlesTracks) {
					if (subtitlesTrack != null && subtitlesTrack.isExternal() &&
							renderer.isExternalSubtitlesFormatSupported(subtitlesTrack, originalResource)) {
						StoreItem copy = createResourceWithAudioSubtitleEngine(originalResource, singleAudioTrack, subtitlesTrack, null);
						entries.add(copy);
					}

				}
			}

			// Sort the list of combinations
			Collections.sort(entries, new LibraryItemSorter(EngineFactory.getEngines()));

			// Now add the sorted list of combinations to the folder
			for (StoreItem resource : entries) {
				LOGGER.trace("Adding {}: audio: {}, subtitle: {}, engine: {}",
						resource.getName(),
						resource.getMediaAudio(),
						resource.getMediaSubtitle(),
						resource.getTranscodingSettings() != null ? resource.getTranscodingSettings().toString() : null);

				addChildInternal(resource);
				addChapterFolder(resource);
			}
		}
	}

	/**
	 * Adds audio and subtitles "flag" filters to the specified
	 * {@link BufferedImageFilterChain} if they should be applied. If
	 * {@code filterChain} is {@code null} and a "flag" filter is added, a new
	 * {@link BufferedImageFilterChain} is created.
	 *
	 * @param filterChain the {@link BufferedImageFilterChain} to modify.
	 * @return The resulting {@link BufferedImageFilterChain} or {@code null}.
	 */
	@Override
	public BufferedImageFilterChain addFlagFilters(BufferedImageFilterChain filterChain) {
		// Show audio and subtitles language flags in the TRANSCODE folder only
		// for video files
		if (getParent() instanceof TranscodeVirtualFolder && originalResource.getMediaSubtitle() != null) {
			filterChain = originalResource.addSubtitlesFlagFilter(filterChain);
		}
		return filterChain;
	}

}
