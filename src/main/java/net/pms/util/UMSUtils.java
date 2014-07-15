package net.pms.util;

import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;

import java.util.List;

public class UMSUtils {

	public static void postSearch(List<DLNAResource> files, String searchCriteria) {
		if(files == null || searchCriteria == null)   {
			return;
		}
		searchCriteria = searchCriteria.toLowerCase();
		for (int i = files.size() - 1; i >= 0; i--) {
			DLNAResource res = files.get(i);

			if (res.isSearched()) {
				continue;
			}

			boolean keep = res.getName().toLowerCase().indexOf(searchCriteria) != -1;
			final DLNAMediaInfo media = res.getMedia();

			if (!keep && media!=null && media.getAudioTracksList() != null) {
				for (int j = 0;j < media.getAudioTracksList().size(); j++) {
					DLNAMediaAudio audio = media.getAudioTracksList().get(j);
					if (audio.getAlbum() != null) {
						keep |= audio.getAlbum().toLowerCase().indexOf(searchCriteria) != -1;
					}
					if (audio.getArtist() != null) {
						keep |= audio.getArtist().toLowerCase().indexOf(searchCriteria) != -1;
					}
					if (audio.getSongname() != null) {
						keep |= audio.getSongname().toLowerCase().indexOf(searchCriteria) != -1;
					}
				}
			}

			if (!keep) { // dump it
				files.remove(i);
			}
		}
	}
}
