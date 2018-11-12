package net.pms.io;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import javax.annotation.Nullable;
import net.pms.newgui.LooksFrame;

public interface SystemUtils {

	public abstract File getAvsPluginsDir();

	public abstract File getKLiteFiltersDir();

	public abstract String getShortPathNameW(String longPathName);

	public abstract String getWindowsDirectory();

	public abstract String getDiskLabel(File f);

	public abstract boolean isKerioFirewall();

	/*
	 * Use getVlcPath() instead
	 */
	@Deprecated
	public abstract String getVlcp();

	/*
	 * Use getVlcVersion() instead
	 */
	@Deprecated
	public abstract String getVlcv();

	public abstract String getVlcPath();

	public abstract String getVlcVersion();

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

	String parsePingLine(String line);

	/**
	 * This is't an actual but an estimated value assuming default MTU size.
	 *
	 * @param packetSize the size of the packet in bytes.
	 * @return The estimated number of fragments.
	 */
	int getPingPacketFragments(int packetSize);

	/**
	 * @return The Windows (internal) version or {@code null} if the platform
	 *         isn't Windows or the value could not be parsed.
	 */
	@Nullable
	public Double getWindowsVersion();
}
