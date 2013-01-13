package net.pms.io;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacSystemUtils extends BasicSystemUtils {
	private final static Logger LOGGER = LoggerFactory.getLogger(MacSystemUtils.class); 

	public MacSystemUtils() {
	}

	@Override
	public void browseURI(String uri) {
		try {
			// On OSX, open the given URI with the "open" command.
			// This will open HTTP URLs in the default browser.
			Runtime.getRuntime().exec(new String[] { "open", uri });
			
		} catch (IOException e) {
			LOGGER.trace("Unable to open the given URI: " + uri + ".");
		}
	}
	
	@Override
	public boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException {
		return false;
	}

	/**
	 * Fetch the hardware address for a network interface.
	 * 
	 * @param ni Interface to fetch the mac address for
	 * @return the mac address as bytes, or null if it couldn't be fetched.
	 * @throws SocketException
	 *             This won't happen on Mac OS, since the NetworkInterface is
	 *             only used to get a name.
	 */
	@Override
	public byte[] getHardwareAddress(NetworkInterface ni) throws SocketException {
		// On Mac OS, fetch the hardware address from the command line tool "ifconfig".
		byte[] aHardwareAddress = null;

		try {
			Process aProc = Runtime.getRuntime().exec(new String[] { "ifconfig", ni.getName(), "ether" });
			aProc.waitFor();
			OutputTextConsumer aConsumer = new OutputTextConsumer(aProc.getInputStream(), false);
			aConsumer.run();
			List<String> aLines = aConsumer.getResults();
			String aMacStr = null;
			Pattern aMacPattern = Pattern.compile("\\s*ether\\s*([a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2})");

			for (String aLine : aLines) {
				Matcher aMacMatcher = aMacPattern.matcher(aLine);

				if (aMacMatcher.find()) {
					aMacStr = aMacMatcher.group(1);
					break;
				}
			}

			if (aMacStr != null) {
				String[] aComps = aMacStr.split(":");
				aHardwareAddress = new byte[aComps.length];

				for (int i = 0; i < aComps.length; i++) {
					String aComp = aComps[i];
					aHardwareAddress[i] = (byte) Short.valueOf(aComp, 16).shortValue();
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Failed to execute ifconfig", e);
		} catch (InterruptedException e) {
			LOGGER.debug("Interrupted while waiting for ifconfig", e);
			Thread.interrupted(); // XXX work around a Java bug - see ProcessUtil.waitFor()
		}
		return aHardwareAddress;
	}	

}
