package net.pms.io;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;

import net.pms.newgui.LooksFrame;

public interface SystemUtils {

	public abstract void disableGoToSleep();

	public abstract void reenableGoToSleep();

	public abstract File getAvsPluginsDir();

	public abstract String getShortPathNameW(String longPathName);

	public abstract String getWindowsDirectory();

	public abstract String getDiskLabel(File f);

	public abstract boolean isKerioFirewall();

	public abstract String getVlcp();

	public abstract String getVlcv();

	public abstract boolean isAvis();

	/**
	 * Open HTTP URLs in the default browser.
	 * @param uri URI string to open externally.
	 */
	public void browseURI(String uri);

	public boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException;

	public void addSystemTray(final LooksFrame frame);

	/**
	 * Fetch the hardware address for a network interface.
	 * 
	 * @param ni Interface to fetch the mac address for
	 * @return the mac address as bytes, or null if it couldn't be fetched.
	 * @throws SocketException
	 *             This won't happen on Mac OS, since the NetworkInterface is
	 *             only used to get a name.
	 */
	public byte[] getHardwareAddress(NetworkInterface ni) throws SocketException;

	/**
	 * Return the platform specific ping command for the given host address,
	 * ping count and packet size.
	 *
	 * @param hostAddress The host address.
	 * @param count The ping count.
	 * @param packetSize The packet size.
	 * @return The ping command.
	 */
	String[] getPingCommand(String hostAddress, int count, int packetSize);
}
