package net.pms.network.mediaserver.jupnp.support.umsservice;

/**
 * Album identifier containing MusicBrainz and Discogs release IDs.
 */
public class AlbumId {

	/**
	 * MusicBrainz release ID
	 */
	public String musicBrainzId;

	/**
	 * Discogs release ID
	 */
	public Long discogsId;

	public AlbumId() {
	}

	public AlbumId(String musicBrainzId, Long discogsId) {
		this.musicBrainzId = musicBrainzId;
		this.discogsId = discogsId;
	}
}
