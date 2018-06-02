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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.pms.Messages;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class populates the file-specific transcode folder with content.
 */
public class FileTranscodeVirtualFolder extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileTranscodeVirtualFolder.class);

	// FIXME unused
	@Deprecated
	public FileTranscodeVirtualFolder(String name, String thumbnailIcon, boolean copy) {
		super(name, thumbnailIcon);
	}

	public FileTranscodeVirtualFolder(String name, String thumbnailIcon) { // XXX thumbnailIcon is always null
		super(name, thumbnailIcon);
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
	private DLNAResource createResourceWithAudioSubtitlePlayer(
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

		private String getMediaAudioLanguage(DLNAResource dlna) {
			return dlna.getMediaAudio() == null ? null : dlna.getMediaAudio().getLang();
		}

		private String getMediaSubtitleLanguage(DLNAResource dlna) {
			return dlna.getMediaSubtitle() == null ? null : dlna.getMediaSubtitle().getLang();
		}

		private int compareLanguage(String lang1, String lang2) {
			if (lang1 == null && lang2 == null) {
				return 0;
			} else if (lang1 != null && lang2 != null) {
				return lang1.compareToIgnoreCase(lang2);
			} else {
				if (lang1 == null) {
					return -1;
				} else {
					return 1;
				}
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

	private boolean isSeekable(DLNAResource dlna) {
		Player player = dlna.getPlayer();

		if ((player == null) || player.isTimeSeekable()) {
			return true;
		} else {
			return false;
		}
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

	/**
	 * This populates the file-specific transcode folder with all combinations of players,
	 * audio tracks and subtitles.
	 */
	@Override
	protected void resolveOnce() {
		if (getChildren().size() == 1) { // OK
			DLNAResource child = getChildren().get(0);
			child.syncResolve();

			RendererConfiguration renderer = null;
			if (this.getParent() != null) {
				renderer = this.getParent().getDefaultRenderer();
			}

			// create copies of the audio/subtitle track lists as we're making (local)
			// modifications to them
			List<DLNAMediaAudio> audioTracks = new ArrayList<>(child.getMedia().getAudioTracksList());
			List<DLNAMediaSubtitle> subtitleTracks = new ArrayList<>(child.getMedia().getSubtitleTracksList());

			// assemble copies for each combination of audio, subtitle and player
			ArrayList<DLNAResource> entries = new ArrayList<>();

			// First, add the option to simply stream the resource.
			if (renderer != null) {
				LOGGER.trace(
					"Duplicating {} for direct streaming to renderer: {}",
					child.getName(),
					renderer.getRendererName()
				);
			}

			DLNAResource noTranscode = createResourceWithAudioSubtitlePlayer(child, null, null, null);
			addChildInternal(noTranscode);
			addChapterFolder(noTranscode);

			// add options for renderer capable to handle streamed subtitles
			if (!configuration.isDisableSubtitles() && renderer != null && renderer.isSubtitlesStreamingSupported()) {
				for (DLNAMediaSubtitle subtitle : subtitleTracks) {
					// only add the option if the renderer supports the given format
					if (subtitle.isExternal()) { // do not check for embedded subs
						if (renderer.isExternalSubtitlesFormatSupported(subtitle, child.getMedia())) {
							DLNAResource copy = createResourceWithAudioSubtitlePlayer(child, null, subtitle, null);
							copy.getMediaSubtitle().setSubsStreamable(true);
							entries.add(copy);
							LOGGER.trace(
								"Duplicating {} for direct streaming subtitles {}",
								child.getName(),
								subtitle.toString()
							);
						}
					}
				}
			}

			/*
			 we add (or may add) a null entry to the audio list and/or subtitle list
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

			 Note: this is the only place in the codebase where mediaAudio and mediaSubtitle
			 are assigned (ignoring the trivial clone operation in ChapterFileTranscodeVirtualFolder),
			 so setting one or both of them to null is a no-op as they're already null.
			 */

			if (audioTracks.isEmpty()) {
				audioTracks.add(null);
			}

			if (subtitleTracks.isEmpty()) {
				subtitleTracks.add(null);
			} else {
				// if there are subtitles, make sure a no-subtitle option is added
				// for each player
				DLNAMediaSubtitle noSubtitle = new DLNAMediaSubtitle();
				noSubtitle.setId(-1);
				subtitleTracks.add(noSubtitle);
			}

			for (DLNAMediaAudio audio : audioTracks) {
				// Create combinations of all audio tracks, subtitles and players.
				for (DLNAMediaSubtitle subtitle : subtitleTracks) {
					// Create a temporary copy of the child with the audio and
					// subtitle modified in order to be able to match players to it.
					DLNAResource temp = createResourceWithAudioSubtitlePlayer(child, audio, subtitle, null);

					// Determine which players match this audio track and subtitle
					ArrayList<Player> players = PlayerFactory.getPlayers(temp);

					// create a copy for each compatible player
					for (Player player : players) {
						DLNAResource copy = createResourceWithAudioSubtitlePlayer(child, audio, subtitle, player);
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
}
