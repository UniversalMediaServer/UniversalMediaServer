package net.pms.external.audioaddict.mapper;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response of {@code POST /v1/{network}/playlists/{id}/play} - a window of upcoming tracks of
 * the play session including freshly signed, time limited content URLs.
 */
public class PlaylistPlayResponse {

	public int id;
	public List<PlaylistTrackJson> tracks;
	@JsonProperty("last_tracks")
	public boolean lastTracks;
	@JsonProperty("current_progress")
	public PlaylistProgress currentProgress;
}
