/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.TranscodeVirtualFolder;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class populates the file-specific transcode folder with content.
 */
public class FileTranscodeVirtualFolder extends TranscodeVirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileTranscodeVirtualFolder.class);
	private final DLNAResource originalResource;

	public FileTranscodeVirtualFolder(DLNAResource resource) {
		super(resource.getDisplayNameBase(), (String) null);
		originalResource = resource;
	}

	/**
	 * Create a copy of the provided original resource and optionally set
	 * the copy's audio track, subtitle track and player.
	 *
	 * @param original The original {@link DLNAResource} to create a copy of.
	 * @param audio The audio track to use.
	 * @param subtitle The subtitle track to use.
	 * @param player The player to use.
	 * @return The copy.
	 */
	private static DLNAResource createResourceWithAudioSubtitlePlayer(
		DLNAResource original,
		DLNAMediaAudio audio,
		DLNAMediaSubtitle subtitle,
		Player player) {
		// FIXME clone is broken. should be e.g. original.newInstance()
		DLNAResource copy = original.clone();
		copy.setMedia(original.getMedia());
		copy.setNoName(true);
		copy.setMediaAudio(audio);
		copy.setMediaSubtitle(subtitle);
		copy.setPlayer(player);

		return copy;
	}

	/**
	 * Helper class to take care of sorting the resources correctly. Resources
	 * are sorted by player, then by audio track, then by subtitle.
	 */
	private static class ResourceSort implements Comparator<DLNAResource> {

		private ArrayList<Player> players;

		ResourceSort(ArrayList<Player> players) {
			this.players = players;
		}

		private static String getMediaAudioLanguage(DLNAResource dlna) {
			return dlna.getMediaAudio() == null ? null : dlna.getMediaAudio().getLang();
		}

		private static String getMediaSubtitleLanguage(DLNAResource dlna) {
			return dlna.getMediaSubtitle() == null ? null : dlna.getMediaSubtitle().getLang();
		}

		private static int compareLanguage(String lang1, String lang2) {
			if (lang1 == null && lang2 == null) {
				return 0;
			} else if (lang1 != null && lang2 != null) {
				return lang1.compareToIgnoreCase(lang2);
			} else {
				if (lang1 == null) {
					return -1;
				}
				return 1;
			}
		}

		@Override
		public int compare(DLNAResource dlna1, DLNAResource dlna2) {
			Integer playerIndex1 = players.indexOf(dlna1.getPlayer());
			Integer playerIndex2 = players.indexOf(dlna2.getPlayer());

			if (!playerIndex1.equals(playerIndex2)) {
				return playerIndex1.compareTo(playerIndex2);
			}

			int cmpAudioLang = compareLanguage(
				getMediaAudioLanguage(dlna1),
				getMediaAudioLanguage(dlna2));

			if (cmpAudioLang != 0) {
				return cmpAudioLang;
			}

			int cmpSubtitleLang = compareLanguage(
				getMediaSubtitleLanguage(dlna1),
				getMediaSubtitleLanguage(dlna2));

			return cmpSubtitleLang;
		}
	}

	private static boolean isSeekable(DLNAResource dlna) {
		Player player = dlna.getPlayer();

		if ((player == null) || player.isTimeSeekable()) {
			return true;
		}
		return false;
	}

	private void addChapterFolder(DLNAResource dlna) {
		if (!dlna.getFormat().isVideo()) {
			return;
		}

		int chapterInterval = configuration.isChapterSupport()
			? configuration.getChapterInterval()
			: -1;

		if ((chapterInterval > 0) && isSeekable(dlna)) {
			// don't add a chapter folder if the duration of the video
			// is less than the chapter length.
			double duration = dlna.getMedia().getDurationInSeconds(); // 0 if the duration is unknown
			if (duration != 0 && duration <= (chapterInterval * 60)) {
				return;
			}

			ChapterFileTranscodeVirtualFolder chapterFolder = new ChapterFileTranscodeVirtualFolder(
				String.format(
				Messages.getString("FileTranscodeVirtualFolder.1"),
				dlna.getDisplayName()),
				null,
				chapterInterval);
			DLNAResource copy = dlna.clone();
			copy.setNoName(true);
			chapterFolder.addChildInternal(copy);
			addChildInternal(chapterFolder);
		}
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		try {
			return originalResource.getThumbnailInputStream();
		} catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}

	@Override
	public void checkThumbnail() {
		originalResource.checkThumbnail();
	}

	/**
	 * This populates the file-specific transcode folder with all combinations of players,
	 * audio tracks and subtitles.
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void resolveOnce() {
		if (getChildren().isEmpty()) { // OK
			originalResource.syncResolve();
			if (originalResource.getMedia() != null && originalResource.getMedia().isVideo()) {
				originalResource.registerExternalSubtitles(true);
			}

			RendererConfiguration renderer = null;
			if (this.getParent() != null) {
				renderer = this.getParent().getDefaultRenderer();
			}

			// create copies of the audio/subtitle track lists as we're making (local)
			// modifications to them
			List<DLNAMediaAudio> audioTracks = new ArrayList<>(originalResource.getMedia().getAudioTracksList());
			List<DLNAMediaSubtitle> subtitlesTracks;
			if (media_subtitle != null) {
				// Transcode folder of live subtitles folder
				subtitlesTracks = Collections.singletonList(media_subtitle);
			} else {
				subtitlesTracks = new ArrayList<>(originalResource.getMedia().getSubtitleTracksList());
			}

			// If there is a single audio track, set that as audio track
			// for non-transcoded entries to show the correct language
			DLNAMediaAudio singleAudioTrack = audioTracks.size() == 1 ? audioTracks.get(0) : null;

			// assemble copies for each combination of audio, subtitle and player
			ArrayList<DLNAResource> entries = new ArrayList<>();

			// First, add the option to simply stream the resource.
			if (renderer != null) {
				LOGGER.trace(
					"Duplicating {} for direct streaming to renderer: {}",
					originalResource.getName(),
					renderer.getRendererName()
				);
			}

			DLNAResource noTranscode = createResourceWithAudioSubtitlePlayer(originalResource, singleAudioTrack, null, null);
			addChildInternal(noTranscode);
			addChapterFolder(noTranscode);

			/*
			 We add (or may add) a null entry to the audio list and/or subtitle list
			 to ensure the inner loop is always entered:

			 for audio in audioTracks:
			 for subtitle in subtitleTracks:
			 for player in players:
			 newResource(audio, subtitle, player)

			 there are 4 different scenarios:

			 1) a file with audio tracks and no subtitles (subtitle == null): in that case we want
			 to assign a player for each audio track

			 2) a file with subtitles and no audio tracks (audio == null): in that case we want
			 to assign a player for each subtitle track

			 3) a file with no audio tracks (audio == null) and no subtitles (subtitle == null)
			 e.g. an audio file, a video with no sound and no subtitles or a web audio/video file:
			 in that case we still want to provide a selection of players e.g. FFmpeg Web Video
			 and VLC Web Video for a web video or FFmpeg Audio and MPlayer Audio for an audio file

			 4) one or more audio tracks AND one or more subtitle tracks: this is the case this code
			 used to handle when it solely dealt with (local) video files: assign a player
			 for each combination of audio track and subtitle track

			 If a null audio or subtitle track is passed to createResourceWithAudioSubtitlePlayer,
			 it sets the copy's corresponding mediaAudio (AKA params.aid) or mediaSubtitle
			 (AKA params.sid) value to null.
			 */

			if (audioTracks.isEmpty()) {
				audioTracks.add(null);
			}

			if (media_subtitle == null) {
				if (subtitlesTracks.isEmpty()) {
					subtitlesTracks.add(null);
				} else {
					// if there are subtitles, make sure a no-subtitle option is added
					// for each player
					DLNAMediaSubtitle noSubtitle = new DLNAMediaSubtitle();
					noSubtitle.setId(DLNAMediaLang.DUMMY_ID);
					subtitlesTracks.add(noSubtitle);
				}
			}

			for (DLNAMediaAudio audio : audioTracks) {
				// Create combinations of all audio tracks, subtitles and players.
				for (DLNAMediaSubtitle subtitle : subtitlesTracks) {
					// Create a temporary copy of the child with the audio and
					// subtitle modified in order to be able to match players to it.
					DLNAResource temp = createResourceWithAudioSubtitlePlayer(originalResource, audio, subtitle, null);

					// Determine which players match this audio track and subtitle
					ArrayList<Player> players = PlayerFactory.getPlayers(temp);

					// create a copy for each compatible player
					for (Player player : players) {
						DLNAResource copy = createResourceWithAudioSubtitlePlayer(originalResource, audio, subtitle, player);
						entries.add(copy);
					}
				}
			}

			if (renderer != null && renderer.isSubtitlesStreamingSupported()) {
				// Add a no-transcode entry for each streamable external subtitles
				for (DLNAMediaSubtitle subtitlesTrack : subtitlesTracks) {
					if (
						subtitlesTrack != null && subtitlesTrack.isExternal() &&
						renderer.isExternalSubtitlesFormatSupported(subtitlesTrack, originalResource.getMedia())
					) {
						DLNAResource copy = createResourceWithAudioSubtitlePlayer(originalResource, singleAudioTrack, subtitlesTrack, null);
						entries.add(copy);
					}
				}
			}

			// Sort the list of combinations
			Collections.sort(entries, new ResourceSort(PlayerFactory.getPlayers()));

			// Now add the sorted list of combinations to the folder
			for (DLNAResource dlna : entries) {
				LOGGER.trace(
					"Adding {}: audio: {}, subtitle: {}, player: {}",
					new Object[]{
					dlna.getName(),
					dlna.getMediaAudio(),
					dlna.getMediaSubtitle(),
					(dlna.getPlayer() != null ? dlna.getPlayer().name() : null),});

				addChildInternal(dlna);
				addChapterFolder(dlna);
			}
		}
	}

	@Override
	protected String getDisplayNameEngine(PmsConfiguration configuration) {
		return null;
	}
}
