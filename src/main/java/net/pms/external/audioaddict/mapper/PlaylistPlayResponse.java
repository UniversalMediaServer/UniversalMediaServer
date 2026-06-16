package net.pms.external.audioaddict.mapper;

import java.util.List;

/**
 * Response of {@code POST /v1/{network}/playlists/{id}/play} - the ordered tracks of a
 * playlist including freshly signed, time limited content URLs.
 */
public class PlaylistPlayResponse {

	public int id;
	public List<PlaylistTrackJson> tracks;
}
