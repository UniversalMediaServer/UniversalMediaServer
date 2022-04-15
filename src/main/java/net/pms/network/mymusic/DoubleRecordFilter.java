package net.pms.network.mymusic;

import java.util.HashSet;
import java.util.Set;

public class DoubleRecordFilter {

	private Set<MusicBrainzAlbum> albumObjects = new HashSet<>();

	public void addAlbum(MusicBrainzAlbum album) {
		albumObjects.add(album);
	}

	public Set<MusicBrainzAlbum> getUniqueAlbumSet() {
		return albumObjects;
	}
}
