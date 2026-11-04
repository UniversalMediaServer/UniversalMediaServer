package net.pms.external.audioaddict;

import java.util.ArrayList;
import java.util.List;

/**
 * One window of a playlist play session: the next tracks plus the session progress flags.
 */
public class AudioAddictPlayWindow {

	public List<AudioAddictTrackDto> tracks = new ArrayList<>();
	public boolean lastTracks;
	public int remainingTracks;
}
