package net.pms.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacSystemUtils extends BasicSystemUtils {
	private final static Logger LOGGER = LoggerFactory.getLogger(MacSystemUtils.class); 

	public MacSystemUtils() { }

	@Override
	public void browseURI(String uri) {
		try {
			// On OS X, open the given URI with the "open" command.
			// This will open HTTP URLs in the default browser.
			Runtime.getRuntime().exec(new String[] { "open", uri });
			
		} catch (IOException e) {
			LOGGER.trace("Unable to open the given URI: {}", uri);
		}
	}

	@Override
	public boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException {
		return false;
	}

	/**
	 * Fetch the hardware address for a network interface.
	 * 
	 * @param ni Interface to fetch the MAC address for
	 * @return the MAC address as bytes, or null if it couldn't be fetched.
	 * @throws SocketException
	 *         This won't happen on OS X, since the NetworkInterface is
	 *         only used to get a name.
	 */
	@Override
	public byte[] getHardwareAddress(NetworkInterface ni) throws SocketException {
		// On Mac OS X, fetch the hardware address from the command line tool "ifconfig".
		byte[] aHardwareAddress = null;
		InputStream inputStream = null;

		try {
			Process process = Runtime.getRuntime().exec(new String[] { "ifconfig", ni.getName(), "ether" });
			inputStream = process.getInputStream();
			List<String> lines = IOUtils.readLines(inputStream);
			String aMacStr = null;
			Pattern aMacPattern = Pattern.compile("\\s*ether\\s*([a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2})");

			for (String line : lines) {
				Matcher aMacMatcher = aMacPattern.matcher(line);

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
			LOGGER.warn("Failed to execute ifconfig", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		return aHardwareAddress;
	}
}
