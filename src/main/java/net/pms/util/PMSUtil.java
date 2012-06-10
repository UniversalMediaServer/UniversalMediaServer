package net.pms.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;

import net.pms.PMS;
import net.pms.newgui.LooksFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PMSUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(PMSUtil.class);

	@Deprecated
	public static <T> T[] copyOf(T[] original, int newLength) {
		LOGGER.info("deprecated PMSUtil.copyOf called");
		return Arrays.copyOf(original, newLength);
	}

	/**
	 * Open HTTP URLs in the default browser.
	 * @param uri URI string to open externally.
	 * @deprecated call SystemUtils.browseURI
	 */
	@Deprecated
	public static void browseURI(String uri) {
		LOGGER.info("deprecated PMSUtil.browseURI called");
		PMS.get().getRegistry().browseURI(uri);
	}

	@Deprecated
	public static void addSystemTray(final LooksFrame frame) {
		LOGGER.info("deprecated PMSUtil.addSystemTray called");
		PMS.get().getRegistry().addSystemTray(frame);
	}

	@Deprecated
	public static boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException {
		LOGGER.info("deprecated PMSUtil.isNetworkInterfaceLoopback called");
		return PMS.get().getRegistry().isNetworkInterfaceLoopback(ni);
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
	@Deprecated
	public static byte[] getHardwareAddress(NetworkInterface ni) throws SocketException {
		LOGGER.info("deprecated PMSUtil.getHardwareAddress called");
		return PMS.get().getRegistry().getHardwareAddress(ni);
	}
}
