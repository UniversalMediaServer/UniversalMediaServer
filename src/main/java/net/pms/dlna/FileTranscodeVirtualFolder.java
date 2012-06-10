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

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.encoders.MEncoderVideo;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.TSMuxerVideo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTranscodeVirtualFolder extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileTranscodeVirtualFolder.class);
	private boolean resolved;

	public FileTranscodeVirtualFolder(String name, String thumbnailIcon, boolean copy) {
		super(name, thumbnailIcon);
	}

	@Override
	public void resolve() {
		super.resolve();
		if (!resolved && getChildren().size() == 1) { //OK
			DLNAResource child = getChildren().get(0);
			child.resolve();
			if (child.getExt().getProfiles() != null) {
				DLNAResource ref = child;
				Player tsMuxer = null;
				for (int i = 0; i < child.getExt().getProfiles().size(); i++) {
					Player pl = PlayerFactory.getPlayer(child.getExt().getProfiles().get(i), child.getExt());
					if (pl != null && !child.getPlayer().equals(pl)) {
						DLNAResource avisnewChild = child.clone();
						avisnewChild.setPlayer(pl);
						avisnewChild.setNoName(true);
						avisnewChild.setMedia(child.getMedia());
						if (avisnewChild.getPlayer().id().equals(MEncoderVideo.ID)) {
							ref = avisnewChild;
						}
						if (avisnewChild.getPlayer().id().equals(TSMuxerVideo.ID)) {
							tsMuxer = pl;
						}
						addChildInternal(avisnewChild);
						addChapterFile(avisnewChild);
					}
				}
				for (int i = 0; i < child.getMedia().getAudioCodes().size(); i++) {
					DLNAResource newChildNoSub = ref.clone();
					newChildNoSub.setPlayer(ref.getPlayer());
					newChildNoSub.setMedia(ref.getMedia());
					newChildNoSub.setNoName(true);
					newChildNoSub.setMediaAudio(ref.getMedia().getAudioCodes().get(i));
					newChildNoSub.setMediaSubtitle(new DLNAMediaSubtitle());
					newChildNoSub.getMediaSubtitle().setId(-1);
					addChildInternal(newChildNoSub);

					addChapterFile(newChildNoSub);

					for (int j = 0; j < child.getMedia().getSubtitlesCodes().size(); j++) {
						DLNAResource newChild = ref.clone();
						newChild.setPlayer(ref.getPlayer());
						newChild.setMedia(ref.getMedia());
						newChild.setNoName(true);
						newChild.setMediaAudio(ref.getMedia().getAudioCodes().get(i));
						newChild.setMediaSubtitle(ref.getMedia().getSubtitlesCodes().get(j));
						addChildInternal(newChild);
						addChapterFile(newChild);

						LOGGER.debug("Duplicate " + ref.getName() + " with player: " + ref.getPlayer().toString() + " and aid: " + newChild.getMediaAudio().getId() + " and sid: " + newChild.getMediaSubtitle());
					}
				}

				if (tsMuxer != null) {
					for (int i = 0; i < child.getMedia().getAudioCodes().size(); i++) {
						DLNAResource newChildNoSub = ref.clone();
						newChildNoSub.setPlayer(tsMuxer);
						newChildNoSub.setMedia(ref.getMedia());
						newChildNoSub.setNoName(true);
						newChildNoSub.setMediaAudio(ref.getMedia().getAudioCodes().get(i));
						addChildInternal(newChildNoSub);
						addChapterFile(newChildNoSub);
					}
				}

				// meskibob: I think it'd be a good idea to add a "Stream" option (for PS3 compatible containers) to the #Transcode# folder in addition to the current options already in there.
				DLNAResource justStreamed = ref.clone();

				RendererConfiguration renderer = null;

				if (this.getParent() != null) {
					renderer = this.getParent().getDefaultRenderer();
				}

				if (justStreamed.getExt() != null && (justStreamed.getExt().isCompatible(ref.getMedia(), renderer) || justStreamed.isSkipTranscode())) {
					justStreamed.setPlayer(null);
					justStreamed.setMedia(ref.getMedia());
					justStreamed.setNoName(true);
					addChildInternal(justStreamed);
					addChapterFile(justStreamed);

					if (renderer != null) {
						LOGGER.debug("Duplicate " + ref.getName() + " for direct streaming to renderer: " + renderer.getRendererName());
					}
				}
			}
		}
		resolved = true;
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
