package net.pms.network.mymusic;

import java.util.Objects;

public class MusicBrainzAlbum {

	public String mbReleaseid = null;
	public String album = null;
	public String artist = null;
	public Integer year = null;

	public MusicBrainzAlbum(String mbReleaseid, String album, String artist, Integer year) {
		super();
		this.mbReleaseid = mbReleaseid;
		this.album = album;
		this.artist = artist;
		this.year = year;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mbReleaseid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MusicBrainzAlbum other = (MusicBrainzAlbum) obj;
		return Objects.equals(mbReleaseid, other.mbReleaseid);
	}

}
