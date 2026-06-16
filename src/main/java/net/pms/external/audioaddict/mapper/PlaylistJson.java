package net.pms.external.audioaddict.mapper;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single curated playlist as returned by the playlists list resource.
 */
public class PlaylistJson {

	public int id;
	public String name;
	public String slug;
	public String description;
	public String duration;
	@JsonProperty("track_count")
	public int trackCount;
	public Map<String, String> images;
}
