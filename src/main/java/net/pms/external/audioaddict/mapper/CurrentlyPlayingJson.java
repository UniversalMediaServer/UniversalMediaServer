package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry of the global "/v1/{network}/currently_playing" response
 */
public class CurrentlyPlayingJson {

	@JsonProperty("channel_id")
	public Integer channelId;
	@JsonProperty("channel_key")
	public String channelKey;
	public Track track;

	/**
	 * The currently playing track. AudioAddict usually fills "track" with an "Artist - Title" string
	 */
	public static class Track {
		public String track;
		public String title;
		public String artist;
		@JsonProperty("display_title")
		public String displayTitle;
		@JsonProperty("display_artist")
		public String displayArtist;
	}

}
