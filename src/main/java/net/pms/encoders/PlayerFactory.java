/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package net.pms.encoders;

import static org.apache.commons.lang3.StringUtils.isBlank;
import com.sun.jna.Platform;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.formats.FormatFactory;
import net.pms.io.ListProcessWrapperResult;
import net.pms.io.SimpleProcessWrapper;
import net.pms.io.SystemUtils;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles players. Creating an instance will initialize the list of
 * known players.
 *
 * @since 1.51.0
 */
public final class PlayerFactory {
	/**
	 * Logger used for all logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatFactory.class);

	/**
	 * Must be used to lock all access to {@link #PLAYERS}.
	 */
	private static final ReentrantReadWriteLock PLAYERS_LOCK = new ReentrantReadWriteLock();

	/**
	 * An {@link ArrayList} of all registered {@link Player} objects.
	 */
	private static final ArrayList<Player> PLAYERS = new ArrayList<>();

	/**
	 * Interface to Windows specific functions, like Windows Registry. The
	 * registry is set by the constructor.
	 */
	private static SystemUtils utils;

	private static PmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * This takes care of sorting the players by the given DMS configuration.
	 */
	@SuppressWarnings("serial")
	private static class PlayerSort implements Comparator<Player>, Serializable {

		@Override
		public int compare(Player player1, Player player2) {
			Integer index1 = configuration.getEnginePriority(player1);
			Integer index2 = configuration.getEnginePriority(player2);

			// Not being in the priority list will sort the player as last.
			if (index1 == -1) {
				index1 = 999;
			}

			if (index2 == -1) {
				index2 = 999;
			}

			return index1.compareTo(index2);
		}
	}

	/**
	 * This class is not meant to be instantiated.
	 */
	private PlayerFactory() {
	}

	/**
	 * Constructor that registers all players based on the given configuration,
	 * frame and registry.
	 */
	public static void initialize() throws InterruptedException {
		utils = PMS.get().getRegistry();
		registerPlayers();
	}

	/**
	 * Register a known set of audio or video transcoders.
	 */
	private static void registerPlayers() throws InterruptedException {
		if (Platform.isWindows()) {
			registerPlayer(new AviSynthFFmpeg());
			registerPlayer(new AviSynthMEncoder());
		}

		registerPlayer(new FFmpegAudio());
		registerPlayer(new MEncoderVideo());
		registerPlayer(new FFMpegVideo());
		registerPlayer(new VLCVideo());
		registerPlayer(new FFmpegWebVideo());
		registerPlayer(new MEncoderWebVideo());
		registerPlayer(new VLCWebVideo());
		registerPlayer(new TsMuxeRVideo());
		registerPlayer(new TsMuxeRAudio());
		registerPlayer(new VideoLanAudioStreaming());
		registerPlayer(new VideoLanVideoStreaming());
		registerPlayer(new DCRaw());
	}

	/**
	 * Adds a single {@link Player} to the list of Players. Before the player is
	 * added to the list, it is verified to be okay.
	 *
	 * @param player Player to be added to the list.
	 */
	public static void registerPlayer(final Player player) throws InterruptedException {
		if (player == null) {
			throw new IllegalArgumentException("player cannot be null");
		}
		configuration.capitalizeEngineId(player);
		PLAYERS_LOCK.writeLock().lock();
		try {
			if (isPlayerRegistered(player.id())) {
				LOGGER.debug("Transcoding engine {} already exists, skipping registration...", player);
				return;
			}

			LOGGER.debug("Checking transcoding engine {}", player);
			PLAYERS.add(player);
			player.setEnabled(configuration.isEngineEnabled(player));

			if (player.getExecutable() == null) {
				player.setUnavailable(String.format(Messages.getString("Engine.ExecutableNotDefined"), player));
				LOGGER.warn("Executable of transcoding engine {} is undefined", player);
				return;
			}

			Path executable;
			if (Platform.isWindows()) {
				String[] validExtensions = {"exe", "com", "bat"};
				String extension = FileUtil.getExtension(player.getExecutable());
				if (extension == null || !Arrays.asList(validExtensions).contains(extension.toLowerCase())) {
					executable = Paths.get(player.getExecutable() + ".exe");
				} else {
					executable = Paths.get(player.getExecutable());
				}
			} else if (player.avisynth()) {
				LOGGER.debug("Skipping transcoding engine {} as it's not compatible with this platform", player);
				player.setUnavailable(String.format(Messages.getString("Engine.ExecutablePlatformIncompatible"), player));
				return;
			} else {
				executable = Paths.get(player.getExecutable());
			}

			try {
				FilePermissions permissions = new FilePermissions(executable);
				if (!permissions.isExecutable()) {
					LOGGER.warn(
						"Insufficient permission to execute \"{}\" for transcoding engine {}",
						executable.toAbsolutePath(),
						player
					);
					player.setUnavailable(
						String.format(Messages.getString("Engine.MissingExecutePermission"), executable.toAbsolutePath(), player)
					);
					return;
				} else if (Platform.isWindows() && player.avisynth() && !utils.isAviSynthAvailable()) {
					LOGGER.warn("Transcoding engine {} is unavailable since AviSynth couldn't be found", player);
					player.setUnavailable(String.format(Messages.getString("Engine.AviSynthNotFound"), player));
					return;
				} else if (!playerTest(player, executable)) {
					// Only set available if this isn't already done by the test to avoid overwriting the status
					player.setAvailable(null);
				}
			} catch (FileNotFoundException e) {
				LOGGER.warn(
					"Executable \"{}\" of transcoding engine {} not found: {}",
					executable.toAbsolutePath(),
					player,
					e.getMessage()
				);
				player.setUnavailable(
					String.format(Messages.getString("Engine.ExecutableNotFound"), executable.toAbsolutePath(), player)
				);
				return;
			}

			if (player.isAvailable()) {
				LOGGER.info("Transcoding engine \"{}\" is available", player);
			} else {
				LOGGER.warn("Transcoding engine \"{}\" is not available", player);
			}

			// Sort the players according to the configuration settings. This
			// will have to be after each registered player in case a player is
			// registered at a later stage than during initialization.
			sortPlayers();

		} finally {
			PLAYERS_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Used to (re)sort {@link #PLAYERS} every time either {@link #PLAYERS}
	 * or {@link PmsConfiguration#enginesPriority} has changed so that
	 * {@link #PLAYERS} always is sorted according to priority.
	 */
	public static void sortPlayers() {
		PLAYERS_LOCK.writeLock().lock();
		try {
			Collections.sort(PLAYERS, new PlayerSort());
		} finally {
			PLAYERS_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Returns the a copy of the list of all {@link Player}s. This includes the
	 * ones not verified as being okay.
	 *
	 * @return A {@link List} of {@link Player}s.
	 */
	public static ArrayList<Player> getAllPlayers() {
		PLAYERS_LOCK.readLock().lock();
		try {
			return new ArrayList<Player>(PLAYERS);
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
	}

	/**
	 * Returns a {@link List} of {@link Player}s according to the given filters
	 * ordered by priority.
	 *
	 * @param onlyEnabled whether or not to filter on enabled {@link Player}s.
	 * @param onlyAvailable whether or not to filter on available {@link Player}s.
	 * @return The {@link List} of {@link Player}s.
	 */
	public static ArrayList<Player> getPlayers(boolean onlyEnabled, boolean onlyAvailable) {
		PLAYERS_LOCK.readLock().lock();
		try {
			ArrayList<Player> resultPlayers = new ArrayList<>();
			for (Player player : PLAYERS) {
				if ((!onlyAvailable || player.isAvailable()) && (!onlyEnabled || player.isEnabled())) {
					resultPlayers.add(player);
				}
			}
			return resultPlayers;
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
	}

	/**
	 * Returns the list of enabled and available players ordered by priority.
	 *
	 * @return The list of players.
	 */
	public static ArrayList<Player> getPlayers() {
		return getPlayers(true, true);
	}

	/**
	 * Checks if a {@link Player} of the given type is registered.
	 *
	 * @param id the {@link Player} type to check for.
	 * @return The result.
	 */
	public static boolean isPlayerRegistered(String id) {
		if (isBlank(id)) {
			return false;
		}

		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (id.equals(player.id())) {
					return true;
				}
			}
			return false;
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
	}

	/**
	 * Checks if a {@link Player} of the given type is both available and
	 * enabled.
	 *
	 * @param id the {@link Player} type to check for.
	 * @return The result.
	 */
	public static boolean isPlayerActive(String id) {
		if (isBlank(id)) {
			return false;
		}

		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (id.equals(player.id())) {
					return player.isActive();
				}
			}
			return false;
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
	}

	/**
	 * Returns the {@link Player} that matches the given {@code id} if it exists
	 * and is enabled and available. If no {@link Player} is found or it isn't
	 * enabled and available, {@code null} is returned.
	 *
	 * @param id the {@link Player} type to check for.
	 * @return The {@link Player} if found or {@code null}.
	 */
	public static Player getActivePlayer(String id) {
		return getPlayer(id, true, true);
	}

	/**
	 * Returns the {@link Player} that matches the given {@code id} if it exists
	 * and matches the given criteria. If no {@link Player} is found or the
	 * given criteria isn't met, {@code null} is returned.
	 *
	 * @param id the {@link Player} type to check for.
	 * @param onlyEnabled whether or not to filter on enabled {@link Player}s.
	 * @param onlyAvailable whether or not to filter on available
	 *        {@link Player}s.
	 * @return The {@link Player} if found or {@code null}.
	 */
	public static Player getPlayer(String id, boolean onlyEnabled, boolean onlyAvailable) {
		if (isBlank(id)) {
			return null;
		}
		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (id.equals(player.id())) {
					if ((!onlyAvailable || player.isAvailable()) && (!onlyEnabled || player.isEnabled())) {
						return player;
					}
					return null;
				}
			}
			return null;
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
	}

	/**
	 * Returns the first {@link Player} that matches the given mediaInfo or
	 * format. Each of the available players is passed the provided information
	 * and the first that reports it is compatible will be returned.
	 *
	 * @param resource
	 *            The {@link DLNAResource} to match
	 * @return The player if a match could be found, <code>null</code>
	 *         otherwise.
	 * @since 1.60.0
	 */
	public static Player getPlayer(final DLNAResource resource) {
		if (resource == null) {
			LOGGER.warn("Invalid resource (null): no player found");
			return null;
		}
		LOGGER.trace("Getting player for resource \"{}\"", resource.getName());
		boolean isImage = resource.getMedia() != null ? resource.getMedia().isImage() : false;

		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (isImage && !(player instanceof ImagePlayer)) {
					continue;
				}
				boolean enabled = player.isEnabled();
				boolean available = player.isAvailable();
				if (enabled && available) {
					boolean compatible = player.isCompatible(resource);
					if (compatible) {
						// Player is enabled and compatible
						LOGGER.trace("Returning compatible player \"{}\"", player.name());
						return player;
					} else if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Player \"{}\" is incompatible", player.name());
					}
				} else if (LOGGER.isTraceEnabled()) {
					if (available) {
						LOGGER.trace("Player \"{}\" is disabled", player.name());
					} else if (enabled) {
						LOGGER.trace("Player \"{}\" isn't available", player.name());
					} else {
						LOGGER.trace("Player \"{}\" is neither available nor enabled", player.name());
					}
				}
			}
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}

		LOGGER.trace("No player found for {}", resource.getName());

		return null;
	}

	/**
	 * Returns all {@link Player}s that match the given resource and are
	 * enabled. Each of the available players is passed the provided information
	 * and each player that reports it is compatible will be returned.
	 *
	 * @param resource
	 *        The {@link DLNAResource} to match
	 * @return The list of compatible players if a match could be found,
	 *         <code>null</code> otherwise.
	 * @since 1.60.0
	 */
	public static ArrayList<Player> getPlayers(final DLNAResource resource) {
		if (resource == null) {
			return null;
		}

		ArrayList<Player> compatiblePlayers = new ArrayList<>();
		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (player.isEnabled() && player.isAvailable() && player.isCompatible(resource)) {
					// Player is enabled, available and compatible
					LOGGER.trace("Player {} is compatible with resource \"{}\"", player.name(), resource.getName());
					compatiblePlayers.add(player);
				}
			}
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
		return compatiblePlayers;
	}

	/**
	 * Protected by {@link #playersLock}.
	 */
	private static final List<PlayerTestRecord> TEST_RECORDS = new ArrayList<>();

	/**
	 * Must only be called when a lock is held on {@link #playersLock}.
	 *
	 * @param player the {@link Player} whose executable to test.
	 * @return {@code true} if a test was performed, {@code false} otherwise.
	 * @throws InterruptedException If the operation is interrupted.
	 */
	private static boolean playerTest(Player player, Path executable) throws InterruptedException {
		if (executable == null) {
			return false;
		}

		for (PlayerTestRecord testRecord : TEST_RECORDS) {
			if (executable.equals(testRecord.executable)) {
				player.setAvailable(testRecord.pass, testRecord.status);
				return true;
			}
		}

		// Return true if a test is performed and availability is set
		if (player instanceof FFMpegVideo) {
			final String arg = "-version";
			String status = null;
			ListProcessWrapperResult result = SimpleProcessWrapper.runProcessListOutput(
				1000,
				500,
				executable.toString(),
				arg
			);
			if (result.getError() != null) {
				LOGGER.warn("\"{}\" failed with error: {}", executable.toString(), result.getError().getMessage());
				LOGGER.trace("", result.getError());
				status = String.format(Messages.getString("Engine.Error"), player) + " \n" + result.getError().getMessage();
				player.setUnavailable(status);
				return true;
			}
			if (result.getExitCode() == 0) {
				if (result.getOutput() != null && result.getOutput().size() > 0) {
					Pattern pattern = Pattern.compile("^ffmpeg version\\s+(.*?)\\s+Copyright", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(result.getOutput().get(0));
					if (matcher.find()) {
						status = matcher.group(1);
						player.setAvailable(status);
					} else {
						player.setAvailable(null);
					}
				} else {
					player.setAvailable(null);
				}
			} else {
				if (result.getOutput().size() > 2) {
					status =
						String.format(Messages.getString("Engine.Error"), player) + " \n" +
						result.getOutput().get(result.getOutput().size() - 2) + " " +
						result.getOutput().get(result.getOutput().size() - 1);
					player.setUnavailable(status);
				} else if (result.getOutput().size() > 1) {
					status =
						String.format(Messages.getString("Engine.Error"), player) + " \n" +
						result.getOutput().get(result.getOutput().size() - 1);
					player.setUnavailable(status);
				} else {
					status = String.format(Messages.getString("Engine.Error"), player) + Messages.getString("General.3");
					player.setUnavailable(status);
				}
			}
			TEST_RECORDS.add(new PlayerTestRecord(executable, player.isAvailable(), status));
			return true;
		} else if (player instanceof MEncoderVideo || player instanceof MEncoderWebVideo) {
			final String arg = "-info:help";
			String status = null;
			ListProcessWrapperResult result = SimpleProcessWrapper.runProcessListOutput(
				1000,
				500,
				executable.toString(),
				arg
			);
			if (result.getError() != null) {
				LOGGER.warn("\"{}\" failed with error: {}", executable.toString(), result.getError().getMessage());
				LOGGER.trace("", result.getError());
				status = String.format(Messages.getString("Engine.Error"), player) + " \n" + result.getError().getMessage();
				player.setUnavailable(status);
				return true;
			}
			if (result.getExitCode() == 0) {
				if (result.getOutput() != null && result.getOutput().size() > 0) {
					Pattern pattern = Pattern.compile("^MEncoder\\s+(.*?)\\s+\\(C\\)", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(result.getOutput().get(0));
					if (matcher.find()) {
						status = matcher.group(1);
						player.setAvailable(status);
					} else {
						player.setAvailable(null);
					}
				} else {
					player.setAvailable(null);
				}
			} else {
				if (result.getOutput() != null &&
					result.getOutput().size() > 3 &&
					StringUtil.hasValue(result.getOutput().get(result.getOutput().size() - 1)) &&
					!StringUtil.hasValue(result.getOutput().get(result.getOutput().size() - 2)) &&
					StringUtil.hasValue(result.getOutput().get(result.getOutput().size() - 3))
				) {
					status =
						String.format(Messages.getString("Engine.Error"), player) + " \n" +
						result.getOutput().get(result.getOutput().size() - 3);
					player.setUnavailable(status);
				} else {
					status = String.format(Messages.getString("Engine.Error"), player) + Messages.getString("General.3");
					player.setUnavailable(status);
				}
			}
			TEST_RECORDS.add(new PlayerTestRecord(executable, player.isAvailable(), status));
			return true;
		} else if (player instanceof TsMuxeRVideo) {
			final String arg = "-v";
			String status = null;
			ListProcessWrapperResult result = SimpleProcessWrapper.runProcessListOutput(
				1000,
				500,
				executable.toString(),
				arg
			);
			if (result.getError() != null) {
				LOGGER.warn("\"{}\" failed with error: {}", executable.toString(), result.getError().getMessage());
				LOGGER.trace("", result.getError());
				status = String.format(Messages.getString("Engine.Error"), player) + " \n" + result.getError().getMessage();
				player.setUnavailable(status);
				return true;
			}
			if (result.getExitCode() == 0) {
				if (result.getOutput() != null && result.getOutput().size() > 0) {
					Pattern pattern = Pattern.compile("tsMuxeR\\.\\s+Version\\s(\\S+)\\s+", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(result.getOutput().get(0));
					if (matcher.find()) {
						status = matcher.group(1);
						player.setAvailable(status);
					} else {
						player.setAvailable(null);
					}
				} else {
					player.setAvailable(null);
				}
			} else {
				status = String.format(Messages.getString("Engine.ExitCode"), player, result.getExitCode());
				if (Platform.isLinux() && Platform.is64Bit()) {
					status += ". \n" + Messages.getString("Engine.tsMuxerErrorLinux");
				}
				player.setUnavailable(status);
			}
			TEST_RECORDS.add(new PlayerTestRecord(executable, player.isAvailable(), status));
			return true;
		} else if (player instanceof DCRaw) {
			String status = null;
			ListProcessWrapperResult result = SimpleProcessWrapper.runProcessListOutput(
				1000,
				500,
				executable.toString()
			);
			if (result.getError() != null) {
				LOGGER.warn("\"{}\" failed with error: {}", executable.toString(), result.getError().getMessage());
				LOGGER.trace("", result.getError());
				status = String.format(Messages.getString("Engine.Error"), player) + " \n" + result.getError().getMessage();
				player.setUnavailable(status);
				return true;
			}
			if (!StringUtil.hasValue(result.getOutput().get(0))) {
				if (result.getOutput() != null && result.getOutput().size() > 1) {
					Pattern pattern = Pattern.compile("decoder\\s\"dcraw\"\\s(\\S+)", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(result.getOutput().get(1));
					if (matcher.find()) {
						status = matcher.group(1);
						player.setAvailable(status);
					} else {
						player.setAvailable(null);
					}
				} else {
					player.setAvailable(null);
				}
			} else if (result.getOutput().size() > 0) {
				status =
					String.format(Messages.getString("Engine.Error"), player) + " \n" +
					result.getOutput().get(0);
				player.setUnavailable(status);
			} else {
				status = String.format(Messages.getString("Engine.Error"), player) + Messages.getString("General.3");
				player.setUnavailable(status);
			}
			TEST_RECORDS.add(new PlayerTestRecord(executable, player.isAvailable(), status));
			return true;
		}
		// No test has been made for VLC, found no way to get feedback on stdout: https://forum.videolan.org/viewtopic.php?t=73665

		return false;
	}

	/**
	 * @deprecated Use {@link #getPlayers(DLNAResource)} instead.
	 *
	 * @param resource the resource to match
	 * @return The list of players if a match could be found, null otherwise.
	 */
	@Deprecated
	public static ArrayList<Player> getEnabledPlayers(final DLNAResource resource) {
		return getPlayers(resource);
	}

	private static class PlayerTestRecord {
		public final Path executable;
		public final boolean pass;
		public final String status;

		public PlayerTestRecord(Path executable, boolean pass, String status) {
			this.executable = executable;
			this.pass = pass;
			this.status = status;
		}
	}
}
