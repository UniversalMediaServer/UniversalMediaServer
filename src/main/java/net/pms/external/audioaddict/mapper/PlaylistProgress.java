package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Playback progress of a playlist play session.
 */
public class PlaylistProgress {

	@JsonProperty("played_tracks")
	public int playedTracks;
	@JsonProperty("remaining_tracks")
	public int remainingTracks;
	@JsonProperty("percent_complete")
	public double percentComplete;
}
