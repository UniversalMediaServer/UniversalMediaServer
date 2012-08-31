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
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class populates the TRANSCODE folder with content. 
 */
public class FileTranscodeVirtualFolder extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileTranscodeVirtualFolder.class);
	private boolean resolved;

	/**
	 * Class to take care of sorting the resources properly. Resources
	 * are sorted by player, then by audio track, then by subtitle.
	 */
	private class ResourceSort implements Comparator<DLNAResource> {
		private ArrayList<Player> players;

		ResourceSort(ArrayList<Player> players) {
			this.players = players;
		}

		@Override
		public int compare(DLNAResource resource1, DLNAResource resource2) {
			Integer playerIndex1 = players.indexOf(resource1.getPlayer());
			Integer playerIndex2 = players.indexOf(resource2.getPlayer());
			
			if (playerIndex1.equals(playerIndex2)) {
				String audioLang1 = resource1.getMediaAudio().getLang();
				String audioLang2 = resource2.getMediaAudio().getLang();

				if (audioLang1.equals(audioLang2)) {
					String subtitle1 = resource1.getMediaSubtitle().getLang();
					String subtitle2 = resource2.getMediaSubtitle().getLang();

					if (subtitle1 != null && subtitle2 != null) {
						return subtitle1.compareToIgnoreCase(subtitle2);
					} else {
						if (subtitle1 == null && subtitle2 == null) {
							return 0;
						} else {
							if (subtitle1 == null) {
								return -1;
							} else {
								return 1;
							}
						}
					}
				} else {
					return audioLang1.compareToIgnoreCase(audioLang2);
				}
			} else {
				return playerIndex1.compareTo(playerIndex2);
			}
		}
		
	}

	public FileTranscodeVirtualFolder(String name, String thumbnailIcon, boolean copy) {
		super(name, thumbnailIcon);
	}

	/**
	 * This populates the TRANSCODE folder with all combinations of players,
	 * audio tracks and subtitles.
	 */
	@Override
	public void resolve() {
		super.resolve();
		if (!resolved && getChildren().size() == 1) { // OK
			DLNAResource child = getChildren().get(0);
			child.resolve();

			// First, add the option to simply stream the resource
			DLNAResource justStreamed = child.clone();

			RendererConfiguration renderer = null;

			if (this.getParent() != null) {
				renderer = this.getParent().getDefaultRenderer();
			}

			// Only add the option if the renderer is compatible with the format
			if (justStreamed.getFormat() != null
					&& (justStreamed.getFormat().isCompatible(child.getMedia(),
							renderer) || justStreamed.isSkipTranscode())) {
				justStreamed.setPlayer(null);
				justStreamed.setMedia(child.getMedia());
				justStreamed.setNoName(true);
				addChildInternal(justStreamed);
				addChapterFile(justStreamed);

				if (renderer != null) {
					LOGGER.debug("Duplicate " + child.getName()
							+ " for direct streaming to renderer: "
							+ renderer.getRendererName());
				}
			}

			// List holding all combinations
			ArrayList<DLNAResource> combos = new ArrayList<DLNAResource>();

			ArrayList<DLNAMediaAudio> audioTracks = child.getMedia().getAudioTracksList();
			ArrayList<DLNAMediaSubtitle> subtitles = child.getMedia().getSubtitleTracksList();

			// Make sure a combo with no subtitles will be added
			DLNAMediaSubtitle noSubtitle = new DLNAMediaSubtitle();
			noSubtitle.setId(-1);
			subtitles.add(noSubtitle);

			// Create combinations of all audio tracks, subtitles and players.
			for (DLNAMediaAudio audio : audioTracks) {
				for (DLNAMediaSubtitle subtitle : subtitles) {
					// Create a temporary copy of the child with the audio and
					// subtitle modified in order to be able to match players to it.
					DLNAResource tempModifiedCopy = createModifiedResource(child, audio, subtitle);
			
					// Determine which players match this audio track and subtitle
					ArrayList<Player> players = PlayerFactory.getEnabledPlayers(tempModifiedCopy);

					for (Player player : players) {
						// Create a copy based on this combination
						DLNAResource combo = createComboResource(child, audio, subtitle, player);
						combos.add(combo);
					}
				}
			}

			// Sort the list of combinations
			Collections.sort(combos, new ResourceSort(PlayerFactory.getAllPlayers()));

			// Now add the sorted list of combinations to the folder
			for (DLNAResource combo : combos) {
				LOGGER.trace("Adding " + combo.toString() + " - "
						+ combo.getPlayer().name() + " - "
						+ combo.getMediaAudio().toString() + " - "
						+ combo.getMediaSubtitle().toString());

				addChildInternal(combo);
				addChapterFile(combo);
			}
		}

		resolved = true;
	}

	/**
	 * Create a copy of the provided original resource with the given audio
	 * track, subtitles and player.
	 *
	 * @param original The original {@link DLNAResource} to create a copy of.
	 * @param audio The audio track to use.
	 * @param subtitle The subtitle track to use.
	 * @param player The player to use.
	 * @return The copy.
	 */
	private DLNAResource createComboResource(DLNAResource original,
			DLNAMediaAudio audio, DLNAMediaSubtitle subtitle, Player player) {

		// FIXME: Use new DLNAResource() instead of clone(). Clone is bad, mmmkay?
		DLNAResource copy = original.clone();

		copy.setMedia(original.getMedia());
		copy.setNoName(true);
		copy.setMediaAudio(audio);
		copy.setMediaSubtitle(subtitle);
		copy.setPlayer(player);

		return copy;
	}

	/**
	 * Create a copy of the provided original resource and modifies it with
	 * the given audio track and subtitles.
	 *
	 * @param original The original {@link DLNAResource} to create a copy of.
	 * @param audio The audio track to use.
	 * @param subtitle The subtitle track to use.
	 * @return The copy.
	 */
	private DLNAResource createModifiedResource(DLNAResource original,
			DLNAMediaAudio audio, DLNAMediaSubtitle subtitle) {

		// FIXME: Use new DLNAResource() instead of clone(). Clone is bad, mmmkay?
		DLNAResource copy = original.clone();

		copy.setMedia(original.getMedia());
		copy.setNoName(true);
		copy.setMediaAudio(audio);
		copy.setMediaSubtitle(subtitle);
		return copy;
	}

	private void addChapterFile(DLNAResource source) {
		if (PMS.getConfiguration().getChapterInterval() > 0 && PMS.getConfiguration().isChapterSupport()) {
			ChapterFileTranscodeVirtualFolder chapterFolder = new ChapterFileTranscodeVirtualFolder("Chapters:" + source.getDisplayName(), null, PMS.getConfiguration().getChapterInterval());
			DLNAResource newSeekChild = source.clone();
			newSeekChild.setNoName(true);
			chapterFolder.addChildInternal(newSeekChild);
			addChildInternal(chapterFolder);
		}
	}

	public FileTranscodeVirtualFolder(String name, String thumbnailIcon) {
		super(name, thumbnailIcon);
	}
}
