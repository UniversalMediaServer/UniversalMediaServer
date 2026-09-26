package net.pms.external.audioaddict.mapper;

import java.util.List;

/**
 * The playable content of a track. {@link #assets} holds one entry per available
 * quality/format; the URL is signed, IP bound and expires.
 */
public class PlaylistTrackContent {

	public List<PlaylistTrackAsset> assets;
}
