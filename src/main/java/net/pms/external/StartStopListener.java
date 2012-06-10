package net.pms.external;

import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;

/**
 * Classes implementing this interface and packaged as pms plugins will be
 * notified when a resources starts or stops being played
 */
public interface StartStopListener extends ExternalListener {
	/**
	 * Called when a resource starts playing
	 * @param media the DLNAMediaInfo for the resource being played
	 * @param resource the DLNAResource being played
	 */
	public void nowPlaying(DLNAMediaInfo media, DLNAResource resource);
	
	/**
	 * Called when a resource stops playing
	 * @param media the DLNAMediaInfo for the resource being stopped
	 * @param resource the DLNAResource being stopped
	 */
	public void donePlaying(DLNAMediaInfo media, DLNAResource resource);
}