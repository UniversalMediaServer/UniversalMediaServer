/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.platform;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import net.pms.io.IPipeProcess;
import net.pms.io.OutputParams;
import net.pms.newgui.LooksFrame;
import net.pms.service.process.ProcessManager;
import net.pms.service.process.AbstractProcessTerminator;
import net.pms.service.sleep.AbstractSleepWorker;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import net.pms.util.Version;

public interface IPlatformUtils {

	public abstract File getAvsPluginsDir();

	public abstract File getKLiteFiltersDir();

	public abstract String getShortPathNameW(String longPathName);

	public abstract String getDiskLabel(File f);

	public abstract boolean isKerioFirewall();

	public abstract Path getVlcPath();

	public abstract Version getVlcVersion();

	public abstract boolean isAviSynthAvailable();

	public abstract boolean isAviSynthPlusAvailable();

	public abstract boolean isTsMuxeRCompatible();

	/**
	 * Open HTTP URLs in the default browser.
	 * @param uri URI string to open externally.
	 * @return false if not browsed
	 */
	public boolean browseURI(String uri);

	public boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException;

	public void addSystemTray(final LooksFrame frame, boolean updateAvailable);

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

	/** Move the given file to the system trash, if one is available.
	 * @param file file to move
	 * @throws IOException on failure.
	 */
	public abstract void moveToTrash(File file) throws IOException;

	/**
	 * Determines whether or not the program has admin/root permissions.
	 *
	 * @return true if the program has admin/root permissions
	 */
	public abstract boolean isAdmin();

	/**
	 * Enumerates the default shared folders.
	 *
	 * @return The default shared folders.
	 */
	public abstract List<Path> getDefaultFolders();

	/**
	 * Determines the file version of library or executable.
	 *
	 * @return The file version or null.
	 */
	public abstract Version getFileVersionInfo(String filePath);

	/**
	 * Returns the iTunes XML file.This file has all the information of the iTunes database.
	 *
	 * @return (String) Absolute path to the iTunes XML file.
	 * @throws java.io.IOException
	 * @throws java.net.URISyntaxException
	 */
	public abstract String getiTunesFile() throws IOException, URISyntaxException;

	/**
	 * The default {@link Charset} for console
	 * @return Charset
	 */
	public abstract Charset getDefaultCharset();

	public abstract String getDefaultFontPath();

	/**
	 * Checks whether system sleep prevention is supported for the current
	 * {@link Platform}.
	 *
	 * @return {@code true} if system sleep prevention is supported,
	 *         {@code false} otherwise.
	 */
	public abstract boolean isPreventSleepSupported();

	/**
	 * creates a sleep worker for the current {@link Platform}.
	 *
	 * @param owner SleepManager
	 * @param mode PreventSleepMode
	 * @return the created {@link AbstractSleepWorker}
	 * @throws IllegalStateException If no {@link AbstractSleepWorker}
	 *             implementation is available for this {@link Platform}.
	 */
	public abstract AbstractSleepWorker getSleepWorker(SleepManager owner, PreventSleepMode mode);

	public abstract AbstractProcessTerminator getProcessTerminator(ProcessManager processManager);

	public abstract IPipeProcess getPipeProcess(String pipeName, OutputParams params, String... extras);

	public abstract IPipeProcess getPipeProcess(String pipeName, String... extras);

	public abstract void appendErrorString(StringBuilder sb, int exitCode);

	public abstract List<String> getRestartCommand(boolean hasOptions);

	public abstract String getShutdownCommand();

	public abstract String getJvmExecutableName();

	public abstract void destroyProcess(final Process p);
}
