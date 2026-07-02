package net.pms.external.audioaddict.mapper;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single upcoming event (a scheduled show episode). The playable episode is held in
 * {@link #tracks} and uses the same track structure as playlists.
 */
public class EventJson {

	public int id;
	public String name;
	public String slug;
	public String subtitle;
	@JsonProperty("artists_tagline")
	public String artistsTagline;
	@JsonProperty("start_at")
	public String startAt;
	@JsonProperty("end_at")
	public String endAt;
	public Show show;
	public List<PlaylistTrackJson> tracks;
}
