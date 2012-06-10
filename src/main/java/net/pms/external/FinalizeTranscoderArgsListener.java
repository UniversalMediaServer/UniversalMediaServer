package net.pms.external;

import java.util.List;

import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.encoders.Player;
import net.pms.io.OutputParams;

/**
 * Classes implementing this interface and packaged as pms plugins will have the
 * possibility to modify transcoding arguments when a resource is being
 * transcoded
 */
public interface FinalizeTranscoderArgsListener extends ExternalListener {
	/**
	 * Called before the transcoding of a resource starts to determine the list
	 * of commands to be used
	 * 
	 * @param player
	 *            the player being used
	 * @param filename
	 *            the name of the file being transcoded
	 * @param dlna
	 *            the DLNAResource being transcoded
	 * @param media
	 *            the DLNAMediaInfo for the file being transcoded
	 * @param params
	 *            the used OutputParams
	 * @param cmdList
	 *            the list of commands
	 * @return the exhaustive list of all commands. It will replace the ones
	 *         received as cmdList
	 */
	public List<String> finalizeTranscoderArgs(Player player, String filename,
			DLNAResource dlna, DLNAMediaInfo media, OutputParams params,
			List<String> cmdList);
}