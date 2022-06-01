package net.pms.network.mediaserver.handlers.api.playlist;


/**
 * Represents a server playlist entry
 */
public class PlaylistIdentVO {

	public String playlistName = "";
	public Integer playlistId = null;

	public PlaylistIdentVO() {
	}


	public PlaylistIdentVO(String playlistName, Integer playlistId) {
		super();
		this.playlistName = playlistName;
		this.playlistId = playlistId;
	}
}
