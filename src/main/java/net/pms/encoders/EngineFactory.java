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
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.FormatFactory;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.ProgramExecutableType;
import net.pms.util.ProgramExecutableType.DefaultExecutableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class handles {@link Engine} creation and keeps a registry over
 * instances.
 */
public final class EngineFactory {
	/**
	 * Logger used for all logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatFactory.class);

	/**
	 * Must be used to lock all access to {@link #ENGINES}.
	 */
	private static final ReentrantReadWriteLock ENGINES_LOCK = new ReentrantReadWriteLock();

	/**
	 * An {@link ArrayList} of all registered {@link Engine} objects.
	 */
	private static final ArrayList<Engine> ENGINES = new ArrayList<>();

	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * This sorts {@link Engine}s according to their configured priorities.
	 */
	@SuppressWarnings("serial")
	private static class EngineSort implements Comparator<Engine>, Serializable {

		@Override
		public int compare(Engine engine1, Engine engine2) {
			Integer index1 = CONFIGURATION.getEnginePriority(engine1);
			Integer index2 = CONFIGURATION.getEnginePriority(engine2);

			// Not being in the priority list will sort the engine as last.
			if (index1 == -1) {
				index1 = 999;
			}

			if (index2 == -1) {
				index2 = 999;
			}

			return index1.compareTo(index2);
		}
	}

	private static boolean initialized = false;

	/**
	 * Not to be instantiated.
	 */
	private EngineFactory() {
	}

	/**
	 * Registers all engines based on the given configuration, frame and
	 * registry.
	 *
	 * @throws InterruptedException If the operation is interrupted.
	 */
	public static void initialize() throws InterruptedException {
		registerEngines();
		initialized = true;
	}

	public static boolean isInitialized() {
		return initialized;
	}

	/**
	 * Registers a known set transcoding engines.
	 *
	 * @throws InterruptedException
	 */
	private static void registerEngines() throws InterruptedException {
		if (Platform.isWindows()) {
			registerEngine(new AviSynthFFmpeg());
			registerEngine(new AviSynthMEncoder());
		}

		registerEngine(new FFmpegAudio());
		registerEngine(new MEncoderVideo());
		registerEngine(new FFMpegVideo());
		registerEngine(new VLCVideo());
		registerEngine(new FFmpegHlsVideo());
		registerEngine(new FFmpegWebVideo());
		registerEngine(new MEncoderWebVideo());
		registerEngine(new VLCWebVideo());
		registerEngine(new TsMuxeRVideo());
		registerEngine(new TsMuxeRAudio());
		registerEngine(new VideoLanAudioStreaming());
		registerEngine(new VideoLanVideoStreaming());
		registerEngine(new DCRaw());
		registerEngine(new YoutubeDl());
	}

	/**
	 * Adds a single {@link Engine} to the list of {@link Engine}s after making
	 * some checks.
	 *
	 * @param engine the {@link Engine} to be added.
	 * @throws InterruptedException If the operation is interrupted.
	 */
	public static void registerEngine(final Engine engine) throws InterruptedException {
		if (engine == null) {
			throw new IllegalArgumentException("engine cannot be null");
		}
		CONFIGURATION.capitalizeEngineId(engine);
		ENGINES_LOCK.writeLock().lock();
		try {
			if (isEngineRegistered(engine.getEngineId())) {
				LOGGER.debug("Transcoding engine {} already exists, skipping registration...", engine);
				return;
			}

			LOGGER.debug("Checking transcoding engine {}", engine);
			ENGINES.add(engine);
			engine.setEnabled(CONFIGURATION.isEngineEnabled(engine), false);

			ExternalProgramInfo programInfo = engine.getProgramInfo();
			ReentrantReadWriteLock programInfoLock = programInfo.getLock();
			// Lock for consistency during tests, need write in case setAvailabe() needs to modify or a custom path is set
			programInfoLock.writeLock().lock();
			try {
				if (CONFIGURATION.isCustomProgramPathsSupported()) {
					LOGGER.trace("Registering custom executable path for transcoding engine {}", engine);
					Path customPath = CONFIGURATION.getEngineCustomPath(engine);
					engine.initCustomExecutablePath(customPath);
				}

				for (ProgramExecutableType executableType : programInfo.getExecutableTypes()) {
					testEngineExecutableType(engine, executableType);
				}
				engine.determineCurrentExecutableType();
			} finally {
				programInfoLock.writeLock().unlock();
			}

			if (engine.isAvailable()) {
				LOGGER.info("Transcoding engine \"{}\" is available", engine);
			} else {
				LOGGER.warn("Transcoding engine \"{}\" is not available", engine);
			}

			// Sort the engines according to the configuration settings. This
			// will have to be after each registered engine in case a engine is
			// registered at a later stage than during initialization.
			sortEngines();

		} finally {
			ENGINES_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Runs tests on the executable of the specified
	 * {@link ProgramExecutableType} for the specified {@link Engine}.
	 *
	 * @param engine the {@link Engine} whose executable to test.
	 * @param executableType the {@link ProgramExecutableType} to test.
	 */
	protected static void testEngineExecutableType(
		@Nullable Engine engine,
		@Nullable ProgramExecutableType executableType
	) {
		if (engine == null || executableType == null || !engine.getProgramInfo().containsType(executableType, true)) {
			return;
		}
		if (!engine.testEngine(executableType) && engine.getProgramInfo().getExecutableInfo(executableType) != null) {
			// Unavailability can only be set it the ExecutableInfo in non-null.
			engine.setUnavailable(
				executableType,
				ExecutableErrorType.GENERAL,
				String.format(Messages.getString("TranscodingEngineXNotAvailableCouldntValidated"), engine)
			);
		}
	}

	/**
	 * Used to (re)sort {@link #ENGINES} every time either {@link #ENGINES} or
	 * {@link UmsConfiguration#enginesPriority} has changed so that
	 * {@link #ENGINES} are always sorted according to priority.
	 */
	public static void sortEngines() {
		ENGINES_LOCK.writeLock().lock();
		try {
			Collections.sort(ENGINES, new EngineSort());
		} finally {
			ENGINES_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Returns a copy of the list of all {@link Engine}s. This includes the ones
	 * not verified as being okay.
	 *
	 * @return A {@link List} of {@link Engine}s.
	 */
	public static List<Engine> getAllEngines() {
		ENGINES_LOCK.readLock().lock();
		try {
			return new ArrayList<>(ENGINES);
		} finally {
			ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Returns a {@link List} of {@link Engine}s according to the given filters
	 * ordered by priority.
	 *
	 * @param onlyEnabled whether or not to filter on enabled {@link Engine}s.
	 * @param onlyAvailable whether or not to filter on available {@link Engine}s.
	 * @return The {@link List} of {@link Engine}s.
	 */
	public static List<Engine> getEngines(boolean onlyEnabled, boolean onlyAvailable) {
		ENGINES_LOCK.readLock().lock();
		try {
			ArrayList<Engine> resultEngines = new ArrayList<>();
			for (Engine engine : ENGINES) {
				if ((!onlyAvailable || engine.isAvailable()) && (!onlyEnabled || engine.isEnabled())) {
					resultEngines.add(engine);
				}
			}
			return resultEngines;
		} finally {
			ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Returns the list of enabled and available engines ordered by priority.
	 *
	 * @return The list of engines.
	 */
	public static List<Engine> getEngines() {
		return getEngines(true, true);
	}

	/**
	 * Checks if a {@link Engine} of the given type is registered.
	 *
	 * @param engine the {@link Engine} type to check for.
	 * @return The result.
	 */
	public static boolean isEngineRegistered(Engine engine) {
		if (engine == null) {
			return false;
		}
		return isEngineRegistered(engine.getEngineId());
	}

	/**
	 * Checks if a {@link EngineId} is registered.
	 *
	 * @param id the {@link EngineId} to check for.
	 * @return The result.
	 */
	public static boolean isEngineRegistered(EngineId id) {
		if (id == null) {
			return false;
		}

		ENGINES_LOCK.readLock().lock();
		try {
			for (Engine engine : ENGINES) {
				if (id.equals(engine.getEngineId())) {
					return true;
				}
			}
			return false;
		} finally {
			ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Checks if a {@link Engine} of the given type is both available and
	 * enabled.
	 *
	 * @param id the {@link Engine} type to check for.
	 * @return The result.
	 */
	public static boolean isEngineActive(EngineId id) {
		if (id == null) {
			return false;
		}

		ENGINES_LOCK.readLock().lock();
		try {
			for (Engine engine : ENGINES) {
				if (id.equals(engine.getEngineId())) {
					return engine.isActive();
				}
			}
			return false;
		} finally {
			ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Checks if a {@link Engine} of the given type is both available.
	 *
	 * @param id the {@link Engine} type to check for.
	 * @return The result.
	 */
	public static boolean isEngineAvailable(EngineId id) {
		if (id == null) {
			return false;
		}

		ENGINES_LOCK.readLock().lock();
		try {
			for (Engine engine : ENGINES) {
				if (id.equals(engine.getEngineId())) {
					return engine.isAvailable();
				}
			}
			return false;
		} finally {
			ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Returns the {@link Engine} that matches the given {@code id} if it exists
	 * and is enabled and available. If no {@link Engine} is found or it isn't
	 * enabled and available, {@code null} is returned.
	 *
	 * @param id the {@link Engine} type to check for.
	 * @return The {@link Engine} if found or {@code null}.
	 */
	public static Engine getActiveEngine(EngineId id) {
		return getEngine(id, true, true);
	}

	/**
	 * Returns the {@link Engine} that matches the given {@code id} if it exists
	 * and matches the given criteria. If no {@link Engine} is found or the
	 * given criteria isn't met, {@code null} is returned.
	 *
	 * @param id the {@link Engine} type to check for.
	 * @param onlyEnabled whether or not to filter on enabled {@link Engine}s.
	 * @param onlyAvailable whether or not to filter on available {@link Engine}s.
	 * @return The {@link Engine} if found or {@code null}.
	 */
	@Nullable
	public static Engine getEngine(@Nullable EngineId id, boolean onlyEnabled, boolean onlyAvailable) {
		if (id == null) {
			return null;
		}
		ENGINES_LOCK.readLock().lock();
		try {
			for (Engine engine : ENGINES) {
				if (id.equals(engine.getEngineId())) {
					if ((!onlyAvailable || engine.isAvailable()) && (!onlyEnabled || engine.isEnabled())) {
						return engine;
					}
					return null;
				}
			}
			return null;
		} finally {
			ENGINES_LOCK.readLock().unlock();
		}
	}

	/**
	 * Gets the currently active executable for a registered {@link Engine} or
	 * {@code null} if the no such {@link Engine} is registered.
	 *
	 * @param id the {@link EngineId} to use for lookup.
	 * @return the executable {@link String} or {@code null}.
	 */
	public static String getEngineExecutable(final EngineId id) {
		if (id == null) {
			return null;
		}

		ENGINES_LOCK.readLock().lock();
		try {
			for (Engine engine : ENGINES) {
				if (id.equals(engine.getEngineId())) {
					return engine.getExecutable();
				}
			}
		} finally {
			ENGINES_LOCK.readLock().unlock();
		}
		return null;
	}

	/**
	 * {@link ExternalProgramInfo} instances can be shared among several
	 * {@link Engine} instances, typically when they use the same executable.
	 * <p>
	 * When a {@link ProgramExecutableType#CUSTOM} {@link Path} is changed, it
	 * affects all {@link Engine} instance that shares that
	 * {@link ExternalProgramInfo} instance. As a result, tests must be rerun
	 * and the current/effective {@link ProgramExecutableType} must be
	 * reevaluated for all affected {@link Engine} instances.
	 * <p>
	 * This method uses the specified {@link Engine} instance that
	 * triggered/initiated the change as a starting point and finds all affected
	 * {@link Engine} instances. It then sets the new default
	 * {@link ProgramExecutableType}, rerun the appropriate tests and
	 * reevaluates the current/effective {@link ProgramExecutableType} for all
	 * affected {@link Engine} instances.
	 *
	 * @param triggeringEngine the {@link Engine} whose executable to test.
	 * @return {@code true} if a test was performed, {@code false} otherwise.
	 * @throws InterruptedException If the operation is interrupted.
	 */
	public static void reEvaluateExecutable(
		@Nonnull Engine triggeringEngine,
		@Nullable ProgramExecutableType changedType,
		@Nullable DefaultExecutableType defaultType
	) {
		if (triggeringEngine == null) {
			throw new IllegalArgumentException("triggeringEngine cannot be null");
		}
		ExternalProgramInfo programInfo = triggeringEngine.getProgramInfo();
		ArrayList<Engine> affectedEngines = new ArrayList<>();
		ENGINES_LOCK.readLock().lock();
		try {
			for (Engine engine : ENGINES) {
				if (engine.getProgramInfo() == programInfo) {
					affectedEngines.add(engine);
				}
			}
		} finally {
			ENGINES_LOCK.readLock().unlock();
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

			for (Engine engine : affectedEngines) {
				for (ProgramExecutableType executableType : retestTypes) {
					engine.clearSpecificErrors(executableType);
					testEngineExecutableType(engine, executableType);
				}
				engine.determineCurrentExecutableType();
			}
		} finally {
			programInfoLock.writeLock().unlock();
		}
	}
}
