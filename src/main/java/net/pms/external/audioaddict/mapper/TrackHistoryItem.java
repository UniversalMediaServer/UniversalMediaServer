package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrackHistoryItem {

	@JsonProperty("network_id")
	public int networkId;
	@JsonProperty("channel_id")
	public int channelId;
	public String artist;
	@JsonProperty("display_artist")
	public String displayArtist;
	public String title;
	@JsonProperty("display_title")
	public String displayTitle;
	public String track;
	public int length;
	public int duration;
	public int started;
	public String type;
	@JsonProperty("track_id")
	public int trackId;
	public Object release;
	@JsonProperty("art_url")
	public String artUrl;
	public Images images;
}
