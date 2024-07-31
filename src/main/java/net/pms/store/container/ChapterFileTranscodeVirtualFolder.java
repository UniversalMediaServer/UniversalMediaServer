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

import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.util.TimeRange;

/**
 * The ChapterFileTranscodeVirtualFolder is a {@link StoreResource} container that
 * examines the media to be transcoded and creates several virtual children. This
 * is done by taking the full length of the media and creating virtual chapters
 * by means of a specified interval length. These virtual chapters are presented
 * to the user in the "#Transcode#" folder when the option "Chapter #Transcode#
 * folder support" is activated in the settings.
 */
public class ChapterFileTranscodeVirtualFolder extends LocalizedStoreContainer {
	private final int interval;

	/**
	 * Constructor for a {@link ChapterFileTranscodeVirtualFolder}. The constructor
	 * does not create the children for this instance, it only sets the name, the
	 * icon for a thumbnail and the interval at which chapter markers must be placed
	 * when the children are created by {@link #syncResolve()}.
	 * @param name The name of this instance.
	 * @param child The chapter folder for this instance.
	 * @param interval The interval (in minutes) at which a chapter marker will be
	 * 			placed.
	 */
	public ChapterFileTranscodeVirtualFolder(Renderer renderer, String name, StoreResource child, int interval) {
		super(renderer, "ChapterX", null, name);
		this.interval = interval;
		addChildInternal(child);
	}

	/* (non-Javadoc)
	 * @see net.pms.store.StoreResource#resolve()
	 */
	@Override
	protected void resolveOnce() {
		if (getChildren().size() == 1) {
			// OK
			StoreResource child = getChildren().get(0);
			if (child instanceof StoreItem item) {
				item.syncResolve();
				int nbMinutes = (int) (item.getMediaInfo().getDurationInSeconds() / 60);
				int nbIntervals = nbMinutes / interval;

				for (int i = 1; i <= nbIntervals; i++) {
					// TODO: Remove clone(), instead create a new object from scratch to avoid unwanted cross references.
					StoreItem newChildNoSub = item.clone();
					newChildNoSub.setTranscodingSettings(item.getTranscodingSettings());
					newChildNoSub.setMediaInfo(item.getMediaInfo());
					newChildNoSub.setNoName(true);
					newChildNoSub.setMediaAudio(item.getMediaAudio());
					newChildNoSub.setMediaSubtitle(item.getMediaSubtitle());
					newChildNoSub.setSplitRange(new TimeRange(60.0 * i * interval, newChildNoSub.getMediaInfo().getDurationInSeconds()));

					addChildInternal(newChildNoSub);
				}
			}
		}
	}
}
