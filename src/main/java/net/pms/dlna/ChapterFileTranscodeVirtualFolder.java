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

import net.pms.dlna.virtual.VirtualFolder;

/**
 * The ChapterFileTranscodeVirtualFolder is a {@link DLNAResource} container that
 * examines the media to be transcoded and creates several virtual children. This
 * is done by taking the full length of the media and creating virtual chapters
 * by means of a specified interval length. These virtual chapters are presented
 * to the user in the "#Transcode#" folder when the option "Chapter #Transcode#
 * folder support" is activated in the settings.
 */
public class ChapterFileTranscodeVirtualFolder extends VirtualFolder {
	private boolean resolved;
	private final int interval;

	/**
	 * Constructor for a {@link ChapterFileTranscodeVirtualFolder}. The constructor
	 * does not create the children for this instance, it only sets the name, the
	 * icon for a thumbnail and the interval at which chapter markers must be placed
	 * when the children are created by {@link #resolve()}. 
	 * @param name The name of this instance.
	 * @param thumbnailIcon The thumbnail for this instance.
	 * @param interval The interval (in minutes) at which a chapter marker will be
	 * 			placed.
	 */
	public ChapterFileTranscodeVirtualFolder(String name, String thumbnailIcon, int interval) {
		super(name, thumbnailIcon);
		this.interval = interval;
	}

	/* (non-Javadoc)
	 * @see net.pms.dlna.DLNAResource#resolve()
	 */
	@Override
	public void resolve() {
		super.resolve();

		if (!resolved && getChildren().size() == 1) { //OK
			DLNAResource child = getChildren().get(0);
			child.resolve();
			int nbMinutes = (int) (child.getMedia().getDurationInSeconds() / 60);
			int nbIntervals = nbMinutes / interval;

			for (int i = 1; i <= nbIntervals; i++) {
				// TODO: Remove clone(), instead create a new object from scratch to avoid unwanted cross references.
				DLNAResource newChildNoSub = child.clone();
				newChildNoSub.setPlayer(child.getPlayer());
				newChildNoSub.setMedia(child.getMedia());
				newChildNoSub.setNoName(true);
				newChildNoSub.setMediaAudio(child.getMediaAudio());
				newChildNoSub.setMediaSubtitle(child.getMediaSubtitle());
				newChildNoSub.setSplitRange(new Range.Time(60.0 * i * interval, newChildNoSub.getMedia().getDurationInSeconds()));

				addChildInternal(newChildNoSub);
			}
		}
		resolved = true;
	}

}
