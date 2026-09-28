package net.pms.external.audioaddict.mapper;

import java.util.List;

/**
 * Response of {@code GET /v1/{network}/playlists} - the list of curated playlists.
 */
public class PlaylistsResponse {

	public List<PlaylistJson> results;
}
