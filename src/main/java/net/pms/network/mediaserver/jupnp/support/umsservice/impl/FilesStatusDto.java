package net.pms.network.mediaserver.jupnp.support.umsservice.impl;


public class FilesStatusDto {

	public String filename = "";
	public Integer userid = null;
	public Boolean isFullyPlayed = null;
	public Integer bookmark = null;
	public Integer playcount = null;
	public Double lastPlaybackPos = null;

	public FilesStatusDto() {
	}

	@Override
	public String toString() {
		return "FilesStatusDto [filename=" + filename + ", userid=" + userid + ", isFullyPlayed=" + isFullyPlayed + ", bookmark=" +
			bookmark + ", playcount=" + playcount + ", lastPlaybackPos=" + lastPlaybackPos + "]";
	}

}
