package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry of "/v1/{network}/track_history/channel/{id}", newest first. Unlike the global
 * "currently_playing" response this one carries the cover art.
 */
public class TrackHistoryJson {

	@JsonProperty("track_id")
	public Integer trackId;
	public String track;
	public String title;
	public String artist;
	@JsonProperty("display_title")
	public String displayTitle;
	@JsonProperty("display_artist")
	public String displayArtist;
	@JsonProperty("art_url")
	public String artUrl;

}
