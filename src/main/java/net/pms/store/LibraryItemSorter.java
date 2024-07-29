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
package net.pms.store;

import java.util.Comparator;
import java.util.List;
import net.pms.encoders.Engine;

/**
 * Helper class to take care of sorting the resources correctly. Resources
 * are sorted by engine, then by audio track, then by subtitle.
 */
public class LibraryItemSorter implements Comparator<StoreItem> {
	private final List<Engine> engines;

	public LibraryItemSorter(List<Engine> engines) {
		this.engines = engines;
	}

	@Override
	public int compare(StoreItem resource1, StoreItem resource2) {
		Integer engineIndex1 = engines.indexOf(resource1.isTranscoded() ? resource1.getTranscodingSettings().getEngine() : null);
		Integer engineIndex2 = engines.indexOf(resource2.isTranscoded() ? resource2.getTranscodingSettings().getEngine() : null);

		if (!engineIndex1.equals(engineIndex2)) {
			return engineIndex1.compareTo(engineIndex2);
		}

		int cmpAudioLang = compareLanguage(getMediaAudioLanguage(resource1),
			getMediaAudioLanguage(resource2));

		if (cmpAudioLang != 0) {
			return cmpAudioLang;
		}

		return compareLanguage(getMediaSubtitleLanguage(resource1),
			getMediaSubtitleLanguage(resource2));
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

	private static String getMediaAudioLanguage(StoreItem resource) {
		return resource.getMediaAudio() == null ? null : resource.getMediaAudio().getLang();
	}

	private static String getMediaSubtitleLanguage(StoreItem resource) {
		return resource.getMediaSubtitle() == null ? null : resource.getMediaSubtitle().getLang();
	}

}
