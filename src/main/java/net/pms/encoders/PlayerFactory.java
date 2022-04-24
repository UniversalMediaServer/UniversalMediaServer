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

import com.sun.jna.Platform;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.ExternalProgramInfo;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.ProgramExecutableType;
import net.pms.configuration.ProgramExecutableType.DefaultExecutableType;
import net.pms.dlna.DLNAResource;
import net.pms.formats.FormatFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class handles {@link Player} creation and keeps a registry over
 * instances.
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

	private static PmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * This sorts {@link Player}s according to their configured priorities.
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
	 * Not to be instantiated.
	 */
	private PlayerFactory() {
	}

	/**
	 * Registers all players based on the given configuration, frame and
	 * registry.
	 *
	 * @throws InterruptedException If the operation is interrupted.
	 */
	public static void initialize() throws InterruptedException {
		registerPlayers();
	}

	/**
	 * Registers a known set transcoding engines.
	 *
	 * @throws InterruptedException
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
		registerPlayer(new YoutubeDl());
	}

	/**
	 * Adds a single {@link Player} to the list of {@link Player}s after making
	 * some checks.
	 *
	 * @param player the {@link Player} to be added.
	 * @throws InterruptedException If the operation is interrupted.
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
			player.setEnabled(configuration.isEngineEnabled(player), false);

			ExternalProgramInfo programInfo = player.getProgramInfo();
			ReentrantReadWriteLock programInfoLock = programInfo.getLock();
			// Lock for consistency during tests, need write in case setAvailabe() needs to modify or a custom path is set
			programInfoLock.writeLock().lock();
			try {
				if (configuration.isCustomProgramPathsSupported()) {
					LOGGER.trace("Registering custom executable path for transcoding engine {}", player);
					Path customPath = configuration.getPlayerCustomPath(player);
					player.initCustomExecutablePath(customPath);
				}

				for (ProgramExecutableType executableType : programInfo.getExecutableTypes()) {
					testPlayerExecutableType(player, executableType);
				}
				player.determineCurrentExecutableType();
			} finally {
				programInfoLock.writeLock().unlock();
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
	 * Runs tests on the executable of the specified
	 * {@link ProgramExecutableType} for the specified {@link Player}.
	 *
	 * @param player the {@link Player} whose executable to test.
	 * @param executableType the {@link ProgramExecutableType} to test.
	 */
	protected static void testPlayerExecutableType(
		@Nullable Player player,
		@Nullable ProgramExecutableType executableType
	) {
		if (player == null || executableType == null || !player.getProgramInfo().containsType(executableType, true)) {
			return;
		}
		if (!player.testPlayer(executableType) && player.getProgramInfo().getExecutableInfo(executableType) != null) {
			// Unavailability can only be set it the ExecutableInfo in non-null.
			player.setUnavailable(
				executableType,
				ExecutableErrorType.GENERAL,
				String.format(Messages.getString("Engine.NotTested"), player)
			);
		}
	}

	/**
	 * Used to (re)sort {@link #PLAYERS} every time either {@link #PLAYERS} or
	 * {@link PmsConfiguration#enginesPriority} has changed so that
	 * {@link #PLAYERS} are always sorted according to priority.
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
	 * Returns a copy of the list of all {@link Player}s. This includes the ones
	 * not verified as being okay.
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
	public static boolean isPlayerRegistered(Player player) {
		if (player == null) {
			return false;
		}
		return isPlayerRegistered(player.id());
	}

	/**
	 * Checks if a {@link PlayerId} is registered.
	 *
	 * @param id the {@link PlayerId} to check for.
	 * @return The result.
	 */
	public static boolean isPlayerRegistered(PlayerId id) {
		if (id == null) {
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
	public static boolean isPlayerActive(PlayerId id) {
		if (id == null) {
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
	 * Checks if a {@link Player} of the given type is both available.
	 *
	 * @param id the {@link Player} type to check for.
	 * @return The result.
	 */
	public static boolean isPlayerAvailable(PlayerId id) {
		if (id == null) {
			return false;
		}

		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (id.equals(player.id())) {
					return player.isAvailable();
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
	public static Player getActivePlayer(PlayerId id) {
		return getPlayer(id, true, true);
	}

	/**
	 * Returns the {@link Player} that matches the given {@code id} if it exists
	 * and matches the given criteria. If no {@link Player} is found or the
	 * given criteria isn't met, {@code null} is returned.
	 *
	 * @param id the {@link Player} type to check for.
	 * @param onlyEnabled whether or not to filter on enabled {@link Player}s.
	 * @param onlyAvailable whether or not to filter on available {@link Player}s.
	 * @return The {@link Player} if found or {@code null}.
	 */
	@Nullable
	public static Player getPlayer(@Nullable PlayerId id, boolean onlyEnabled, boolean onlyAvailable) {
		if (id == null) {
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
	 * Returns the first {@link Player} that matches the given
	 * {@link DLNAResource}. Each of the available {@link Player} instances are
	 * passed the provided information and the first that reports that it is
	 * compatible will be returned.
	 *
	 * @param resource the {@link DLNAResource} to match.
	 * @return The {@link Player} if a match could be found, {@code null}
	 *         otherwise.
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
	 * Gets the currently active executable for a registered {@link Player} or
	 * {@code null} if the no such {@link Player} is registered.
	 *
	 * @param id the {@link PlayerId} to use for lookup.
	 * @return the executable {@link String} or {@code null}.
	 */
	public static String getPlayerExecutable(final PlayerId id) {
		if (id == null) {
			return null;
		}

		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (id.equals(player.id())) {
					return player.getExecutable();
				}
			}
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
		return null;
	}

	/**
	 * Returns all {@link Player}s that match the given resource and are
	 * enabled. Each of the available {@link Player}s are passed the provided
	 * information and each {@link Player} that reports that it is compatible
	 * will be returned.
	 *
	 * @param resource the {@link DLNAResource} to match.
	 * @return A list of compatible {@link Player}s if a match could be found,
	 *         {@code null} otherwise.
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
	 * {@link ExternalProgramInfo} instances can be shared among several
	 * {@link Player} instances, typically when they use the same executable.
	 * <p>
	 * When a {@link ProgramExecutableType#CUSTOM} {@link Path} is changed, it
	 * affects all {@link Player} instance that shares that
	 * {@link ExternalProgramInfo} instance. As a result, tests must be rerun
	 * and the current/effective {@link ProgramExecutableType} must be
	 * reevaluated for all affected {@link Player} instances.
	 * <p>
	 * This method uses the specified {@link Player} instance that
	 * triggered/initiated the change as a starting point and finds all affected
	 * {@link Player} instances. It then sets the new default
	 * {@link ProgramExecutableType}, rerun the appropriate tests and
	 * reevaluates the current/effective {@link ProgramExecutableType} for all
	 * affected {@link Player} instances.
	 *
	 * @param player the {@link Player} whose executable to test.
	 * @return {@code true} if a test was performed, {@code false} otherwise.
	 * @throws InterruptedException If the operation is interrupted.
	 */
	public static void reEvaluateExecutable(
		@Nonnull Player triggeringPlayer,
		@Nullable ProgramExecutableType changedType,
		@Nullable DefaultExecutableType defaultType
	) {
		if (triggeringPlayer == null) {
			throw new IllegalArgumentException("triggeringPlayer cannot be null");
		}
		ExternalProgramInfo programInfo = triggeringPlayer.getProgramInfo();
		ArrayList<Player> affectedPlayers = new ArrayList<>();
		PLAYERS_LOCK.readLock().lock();
		try {
			for (Player player : PLAYERS) {
				if (player.getProgramInfo() == programInfo) {
					affectedPlayers.add(player);
				}
			}
		} finally {
			PLAYERS_LOCK.readLock().unlock();
		}
		ReentrantReadWriteLock programInfoLock = programInfo.getLock();
		// Lock for consistency during reevaluation
		programInfoLock.writeLock().lock();
		try {
			if (defaultType != null) {
				switch (defaultType) {
					case CUSTOM:
						programInfo.setDefault(ProgramExecutableType.CUSTOM);
						break;
					case ORIGINAL:
						programInfo.setOriginalDefault();
						break;
					case NONE:
					default:
						break;
				}
			}

			Set<ProgramExecutableType> retestTypes;
			if (changedType == null) {
				retestTypes = EnumSet.allOf(ProgramExecutableType.class);
			} else {
				retestTypes = Collections.singleton(changedType);
			}

			for (Player player : affectedPlayers) {
				for (ProgramExecutableType executableType : retestTypes) {
					player.clearSpecificErrors(executableType);
					testPlayerExecutableType(player, executableType);
				}
				player.determineCurrentExecutableType();
			}
		} finally {
			programInfoLock.writeLock().unlock();
		}
	}
}
