package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single track of a playlist.
 */
public class PlaylistTrackJson {

	public long id;
	public int length;
	public String track;
	@JsonProperty("display_title")
	public String displayTitle;
	@JsonProperty("display_artist")
	public String displayArtist;
	@JsonProperty("asset_url")
	public String assetUrl;
	public PlaylistTrackContent content;
}
