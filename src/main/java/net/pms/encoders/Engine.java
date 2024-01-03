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
package net.pms.encoders;

import com.google.gson.JsonArray;
import com.sun.jna.Platform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.ConfigurableProgramPaths;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.subtitle.MediaOnDemandSubtitle;
import net.pms.renderers.Renderer;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExecutableInfo.ExecutableInfoBuilder;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.FilePermissions;
import net.pms.util.FilePermissions.FileFlag;
import net.pms.util.ProgramExecutableType;
import net.pms.util.ProgramExecutableType.DefaultExecutableType;
import net.pms.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base class for all transcoding engines.
 */
@ThreadSafe
public abstract class Engine {
	private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

	public static final int VIDEO_SIMPLEFILE_ENGINE = 0;
	public static final int AUDIO_SIMPLEFILE_ENGINE = 1;
	public static final int VIDEO_WEBSTREAM_ENGINE = 2;
	public static final int AUDIO_WEBSTREAM_ENGINE = 3;
	public static final int MISC_ENGINE = 4;

	/** The final {@link ExternalProgramInfo} instance set in the constructor */
	@Nonnull
	protected final ExternalProgramInfo programInfo;

	public abstract int purpose();

	public abstract EngineId getEngineId();

	/**
	 * @return The {@link Configuration} key for this {@link Engine}'s custom
	 *         executable path.
	 */
	public abstract String getConfigurablePathKey();

	public abstract String getName();

	public abstract int type();

	/**
	 * Must be used to control all access to {@link #available}
	 */
	public abstract String getExecutableTypeKey();

	/**
	 * Used to store if this {@link Engine} can be used, e.g if the binary is
	 * accessible. All access must be guarded with {@link #availableLock}.
	 */
	protected final ReentrantReadWriteLock specificErrorsLock = new ReentrantReadWriteLock();

	/**
	 * Used to store a localized error text if the {@link Engine} is
	 * unavailable. All access must be guarded with {@link #availableLock}.
	 */
	protected volatile ProgramExecutableType currentExecutableType;

	/**
	 * Used to store the executable version if the {@link Engine} is available
	 * and the information could be parsed. All access must be guarded with
	 * {@link #availableLock}.
	 */
	@GuardedBy("specificErrorsLock")
	protected final HashMap<ProgramExecutableType, String> specificErrors = new HashMap<>();

	/**
	 * Must be used to control all access to {@link #enabled}
	 */
	protected final ReentrantReadWriteLock enabledLock = new ReentrantReadWriteLock();

	/**
	 * Used to store if this {@link Engine} is enabled in the configuration. All
	 * access must be guarded with {@link #enabledLock}.
	 */
	@GuardedBy("enabledLock")
	protected boolean enabled = false;

	/**
	 * Abstract constructor that sets the final {@code programInfo} variable.
	 */
	protected Engine(ExternalProgramInfo programInfo) {
		this.programInfo = programInfo;
		if (programInfo == null) {
			throw new IllegalStateException(
				"Can't instantiate " + this.getClass().getSimpleName() + "because executables() returns null"
			);
		}
	}

	/**
	 * Gets the <i>current</i> {@link ProgramExecutableType} for this
	 * {@link Engine}. For an explanation of the concept, see
	 * {@link #currentExecutableType}.
	 *
	 * @return The current {@link ProgramExecutableType}
	 */
	@Nullable
	public ProgramExecutableType getCurrentExecutableType() {
		return currentExecutableType;
	}

	/**
	 * Sets the current {@link ProgramExecutableType} for this {@link Engine}.
	 * For an explanation of the concept, see {@link #currentExecutableType}.
	 *
	 * @param executableType the new {@link ProgramExecutableType}.
	 */
	public void setCurrentExecutableType(ProgramExecutableType executableType) {
		currentExecutableType = executableType;
	}

	/**
	 * Determines and sets the current {@link ProgramExecutableType} for this
	 * {@link Engine}. The determination starts out with the configured
	 * {@link ProgramExecutableType}.
	 * <p>
	 * For an explanation of the concept, see {@link #currentExecutableType}.
	 */
	public void determineCurrentExecutableType() {
		determineCurrentExecutableType(configuration.getEngineExecutableType(this));
	}

	/**
	 * Determines and sets the current {@link ProgramExecutableType} for this
	 * {@link Engine}. The determination starts out with the specified
	 * {@link ProgramExecutableType}.
	 * <p>
	 * For an explanation of the concept, see {@link #currentExecutableType}.
	 *
	 * @param newExecutableType the preferred
	 *            {@link ProgramExecutableType}.
	 */
	public void determineCurrentExecutableType(@Nullable ProgramExecutableType newExecutableType) {
		// Find the best executable type to use, first try the configured type
		if (!isAvailable(newExecutableType)) {
			// Set the platform default if that's available
			ProgramExecutableType tmpExecutableType = programInfo.getDefault();
			if (isAvailable(tmpExecutableType)) {
				newExecutableType = tmpExecutableType;
			} else {
				// Set the first one that is available, if any
				for (ProgramExecutableType executableType : programInfo.getExecutableTypes()) {
					if (isAvailable(executableType)) {
						newExecutableType = executableType;
						break;
					}
				}
			}
			// Leave it to the configured type if no other is available
		}

		// If null, just pick one that exists if possible.
		if (newExecutableType == null) {
			if (currentExecutableType != null) {
				return;
			}
			for (ProgramExecutableType executableType : programInfo.getExecutableTypes()) {
				if (executableType != null) {
					newExecutableType = executableType;
					break;
				}
			}
		}
		currentExecutableType = newExecutableType;
	}

	public abstract String mimeType();

	/**
	 * @return The {@link ExternalProgramInfo} instance.
	 */
	@Nonnull
	public ExternalProgramInfo getProgramInfo() {
		return programInfo;
	}

	/**
	 * @return The path to the currently configured
	 *         {@link ProgramExecutableType} for this {@link Engine} or
	 *         {@code null} if undefined.
	 */
	@Nullable
	public String getExecutable() {
		Path executable = getProgramInfo().getPath(currentExecutableType);
		return executable == null ? null : executable.toString();
	}

	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	protected UmsConfiguration configuration = CONFIGURATION;

	public boolean isAviSynthEngine() {
		return false;
	}

	public abstract boolean excludeFormat(Format extension);

	public abstract boolean isEngineCompatible(Renderer renderer);

	public boolean isInternalSubtitlesSupported() {
		return true;
	}

	public boolean isExternalSubtitlesSupported() {
		return true;
	}

	public boolean isTimeSeekable() {
		return false;
	}

	/**
	 * Used to determine if this {@link Engine} can be used, e.g if the binary
	 * is accessible.
	 *
	 * @return {@code true} if this is available, {@code false} otherwise.
	 */
	public boolean isAvailable() {
		return isAvailable(currentExecutableType);
	}

	/**
	 * Checks whether this {@link Engine} can be used, e.g if the binary is
	 * accessible for the specified {@link ProgramExecutableType}.
	 *
	 * @param executableType the {@link ProgramExecutableType} to get the status
	 *            text for.
	 * @return {@code true} if this {@link Engine} is available, {@code false}
	 *         otherwise.
	 */
	public boolean isAvailable(@Nullable ProgramExecutableType executableType) {
		if (executableType == null) {
			return false;
		}
		ExecutableInfo executableInfo = programInfo.getExecutableInfo(executableType);
		if (executableInfo == null) {
			return false;
		}
		Boolean result = executableInfo.getAvailable();
		if (result == null || !result) {
			return false;
		}
		specificErrorsLock.readLock().lock();
		try {
			String specificError = specificErrors.get(executableType);
			return specificError == null;
		} finally {
			specificErrorsLock.readLock().unlock();
		}
	}

	/**
	 * Returns the current engine status (enabled, available) as a localized
	 * text. If there is an error, a generic text is returned.
	 *
	 * @return The localized status text.
	 */
	public String getStatusText(ProgramExecutableType executableType) {
		return getStatusText(executableType, false);
	}

	/**
	 * Returns the current engine status (enabled, available) as a localized
	 * text. If there is an error, the full error text is returned.
	 *
	 * @return The localized status text.
	 */
	public String getStatusTextFull(ProgramExecutableType executableType) {
		return getStatusText(executableType, true);
	}

	/**
	 * Returns the current engine status (enabled, available) as a localized
	 * text.
	 *
	 * @param fullText if {@code true} the full error text is returned in case
	 *            of an error, if {@code false} only a generic text is returned
	 *            in case of an error.
	 * @return The localized status text.
	 */
	public String getStatusText(ProgramExecutableType executableType, boolean fullText) {
		if (executableType == null) {
			return null;
		}
		ExecutableInfo executableInfo = programInfo.getExecutableInfo(executableType);
		if (executableInfo == null) {
			return String.format(Messages.getString("TheXExecutableNotDefined"), getName());
		}
		if (executableInfo.getAvailable() == null || executableInfo.getAvailable()) {
			// Generally available or unknown, check for Engine specific failures
			specificErrorsLock.readLock().lock();
			try {
				String specificError = specificErrors.get(executableType);
				if (specificError != null) {
					return fullText ? specificError : String.format(Messages.getString("ThereIsProblemTranscodingEngineX"), getName());
				}
			} finally {
				specificErrorsLock.readLock().unlock();
			}
			if (executableInfo.getAvailable() == null) {
				return String.format(Messages.getString("StatusTranscodingEngineXUnknown"), getName());
			}
		}
		if (executableInfo.getAvailable()) {
			if (isEnabled()) {
				if (executableInfo.getVersion() != null) {
					return String.format(Messages.getString("TranscodingEngineXYEnabled"), getName(), executableInfo.getVersion());
				}
				return String.format(Messages.getString("TranscodingEngineXEnabled"), getName());
			}
			return String.format(Messages.getString("TranscodingEngineXDisabled"), getName());
		}
		if (executableInfo.getErrorText() == null) {
			return Messages.getString("UnknownError");
		}
		return fullText ? executableInfo.getErrorText() : String.format(Messages.getString("ThereIsProblemTranscodingEngineX"), getName());
	}

	public JsonArray getStatusTextAsJsonArray(ProgramExecutableType executableType, boolean fullText) {
		if (executableType == null) {
			return null;
		}
		JsonArray array = new JsonArray();
		ExecutableInfo executableInfo = programInfo.getExecutableInfo(executableType);
		if (executableInfo == null) {
			array.add("i18n@TheXExecutableNotDefined");
			array.add(getName());
			return array;
		}
		if (executableInfo.getAvailable() == null || executableInfo.getAvailable()) {
			// Generally available or unknown, check for Player specific failures
			specificErrorsLock.readLock().lock();
			try {
				String specificError = specificErrors.get(executableType);
				if (specificError != null) {
					if (fullText) {
						array.add(specificError);
					} else {
						array.add("i18n@ThereIsProblemTranscodingEngineX");
						array.add(getName());
					}
					return array;
				}
			} finally {
				specificErrorsLock.readLock().unlock();
			}
			if (executableInfo.getAvailable() == null) {
				array.add("i18n@StatusTranscodingEngineXUnknown");
				array.add(getName());
				return array;
			}
		}
		if (executableInfo.getAvailable()) {
			if (isEnabled()) {
				Version version = executableInfo.getVersion();
				if (version != null) {
					array.add("i18n@TranscodingEngineXYEnabled");
					array.add(getName());
					array.add(version.toString());
					return array;
				}
				array.add("i18n@TranscodingEngineXEnabled");
				array.add(getName());
				return array;
			}
			array.add("i18n@TranscodingEngineXDisabled");
			array.add(getName());
			return array;
		}
		if (executableInfo.getErrorText() == null) {
			array.add("i18n@UnknownError");
			return array;
		}
		if (fullText) {
			array.add(executableInfo.getErrorText());
			return array;
		}
		array.add("i18n@ThereIsProblemTranscodingEngineX");
		array.add(getName());
		return array;
	}

	/**
	 * Sets the engine available status and a related text.
	 * Note that {@code getStatusText} has a "dual function".
	 *
	 * @return if {@code available} is {@code true}, the executable
	 *            version or {@code null} the version if unknown. If
	 *            {@code available} is {@code false}, a localized description of
	 *            the current error.
	 */
	public String getStatusText() {
		return getStatusText(currentExecutableType, false);
	}

	/**
	 * Returns the current engine status (enabled, available) as a localized
	 * text for the current {@link ProgramExecutableType}. If there is an error,
	 * the full error text is returned.
	 *
	 * @return The localized status text.
	 */
	public String getStatusTextFull() {
		return getStatusText(currentExecutableType, true);
	}

	public JsonArray getStatusTextFullAsJsonArray() {
		return getStatusTextAsJsonArray(currentExecutableType, true);
	}

	/**
	 * Marks the engine as available for use.
	 *
	 * @param versionText the parsed version string for the executable, or
	 *            {@code null} if the version is unknown.
	 */
	public void setAvailable(@Nonnull ProgramExecutableType executableType, @Nullable Version version) {
		setAvailable(true, executableType, version, null, null);
	}

	/**
	 * Marks the engine as unavailable.
	 *
	 * @param errorText the localized error description.
	 */
	public void setUnavailable(
		@Nonnull ProgramExecutableType executableType,
		@Nonnull ExecutableErrorType errorType,
		@Nullable String errorText
	) {
		setAvailable(false, executableType, null, errorType, errorText);
	}

	/**
	 * Marks the engine as unavailable.
	 *
	 * @param executableType the {@link ProgramExecutableType} for which to set
	 *            availability.
	 * @param version the {@link Version} of the executable if known or
	 *            {@code null} if unknown.
	 * @param errorType the {@link ExecutableErrorType}.
	 * @param errorText the localized error description.
	 */
	public void setUnavailable(
		@Nonnull ProgramExecutableType executableType,
		@Nullable Version version,
		@Nonnull ExecutableErrorType errorType,
		@Nonnull String errorText
	) {
		setAvailable(false, executableType, version, errorType, errorText);
	}

	/**
	 * Sets the engine available status and a related error text.
	 *
	 * @param available whether or not the {@link Engine} is available.
	 * @param executableType the {@link ProgramExecutableType} for which to set
	 *            availability.
	 * @param version the {@link Version} of the executable if known or
	 *            {@code null} if unknown.
	 * @param errorType the {@link ExecutableErrorType} if {@code available} is
	 *            {@code false}. Can be {@code null} if {@code available} is
	 *            {@code true}.
	 * @param errorText a localized description of the current error if
	 *            {@code available} is {@code false}, or {@code null} if the
	 *            executable is available.
	 */
	public void setAvailable(
		boolean available,
		@Nonnull ProgramExecutableType executableType,
		@Nullable Version version,
		@Nullable ExecutableErrorType errorType,
		@Nullable String errorText
	) {
		if (executableType == null) {
			throw new IllegalArgumentException("executableType cannot be null or unknown");
		}
		if (!available && (errorType == null || errorText == null)) {
			throw new IllegalArgumentException("errorType and errorText can only be null if available is true");
		}
		if (errorType == ExecutableErrorType.SPECIFIC) {
			/*
			 * Although most probably the case, we can't assume that a Engine
			 * specific error means that the executable is generally available.
			 * Thus, only set the local specific error and not the global
			 * availability for this executable. If it's used by another engine
			 * it will be tested again.
			 */
			specificErrorsLock.writeLock().lock();
			try {
				specificErrors.put(executableType, errorText);
			} finally {
				specificErrorsLock.writeLock().unlock();
			}
		} else {
			// Set the global general status
			ExecutableInfo executableInfo = programInfo.getExecutableInfo(executableType);
			if (executableInfo == null) {
				throw new IllegalStateException(
					"Cannot set availability for " + executableType + " " + getName() + " because it is undefined"
				);
			}
			ExecutableInfoBuilder builder = executableInfo.modify();
			builder.available(available);
			if (version != null) {
				builder.version(version);
			}
			if (errorType != null || errorText != null) {
				builder.errorType(errorType).errorText(errorText);
			}
			programInfo.setExecutableInfo(executableType, builder.build());
		}
	}

	/**
	 * Sets the custom executable {@link Path} and the default
	 * {@link ProgramExecutableType} type, but won't run tests or perform other
	 * tasks normally needed after such a change.
	 * <p>
	 * <b>This should normally only be called from
	 * {@link EngineFactory#registerEngine(Engine)}</b> to set the configured
	 * {@link Path} before other registration tasks are performed.
	 *
	 * @param customPath The custom executable {@link Path}.
	 */
	public void initCustomExecutablePath(@Nullable Path customPath) {
		customPath = ConfigurableProgramPaths.resolveCustomProgramPath(customPath);
		programInfo.setPath(ProgramExecutableType.CUSTOM, customPath);
		if (customPath == null) {
			programInfo.setOriginalDefault();
		} else {
			programInfo.setDefault(ProgramExecutableType.CUSTOM);
			LOGGER.debug("Custom executable path for {} was initialized to \"{}\"", programInfo, customPath);
		}
	}

	/**
	 * Sets or clears the {@link ProgramExecutableType#CUSTOM} executable
	 * {@link Path} for the underlying {@link ExternalProgramInfo}. This will
	 * impact all engines sharing the same {@link ExternalProgramInfo}.
	 * <p>
	 * A changed {@link Path} will result in a rerun of tests and a reevaluation
	 * of the current {@link ExecutableInfo} for all affected {@link Engine}s.
	 * As this is a costly operations, no changes will be made if the specified
	 * {@link Path} is equal to the existing {@link Path} or if both are
	 * {@code null}.
	 *
	 * @param customPath the new custom {@link Path} or {@code null} to clear.
	 * @param setConfiguration whether or not the {@link Path} should also be
	 *            stored in {@link UmsConfiguration}.
	 * @return {@code true} if any changes were made as a result of this call,
	 *         {@code false} otherwise.
	 */
	public boolean setCustomExecutablePath(@Nullable Path customPath, boolean setConfiguration) {
		boolean configurationChanged = false;
		if (setConfiguration) {
			try {
				configurationChanged = configuration.setEngineCustomPath(this, customPath);
			} catch (IllegalStateException e) {
				configurationChanged = false;
				LOGGER.warn("Failed to set custom executable path for {}: {}", getName(), e.getMessage());
				LOGGER.trace("", e);
			}
		}

		customPath = ConfigurableProgramPaths.resolveCustomProgramPath(customPath);
		boolean changed = programInfo.setPath(ProgramExecutableType.CUSTOM, customPath);
		if (changed) {
			DefaultExecutableType defaultType;
			if (customPath == null) {
				defaultType = DefaultExecutableType.ORIGINAL;
				if (setConfiguration && LOGGER.isDebugEnabled()) {
					LOGGER.debug("Custom executable path for {} was cleared", programInfo);
				}
			} else {
				defaultType = DefaultExecutableType.CUSTOM;
				if (setConfiguration && LOGGER.isDebugEnabled()) {
					LOGGER.debug("Custom executable path for {} was set to \"{}\"", programInfo, customPath);
				}
			}
			EngineFactory.reEvaluateExecutable(this, ProgramExecutableType.CUSTOM, defaultType);
		}
		return changed || configurationChanged;
	}

	/**
	 * Clears any registered {@link ExecutableErrorType#SPECIFIC} for the
	 * specified {@link ProgramExecutableType}.
	 *
	 * @param executableType the {@link ProgramExecutableType} for which to
	 *            clear registered {@link ExecutableErrorType#SPECIFIC} errors.
	 */
	public void clearSpecificErrors(@Nullable ProgramExecutableType executableType) {
		if (executableType == null && !isSpecificTest()) {
			return;
		}
		specificErrorsLock.writeLock().lock();
		try {
			specificErrors.remove(executableType);
		} finally {
			specificErrorsLock.writeLock().unlock();
		}
	}

	/**
	 * Used to determine if this {@link Engine} is enabled in the configuration.
	 *
	 * @return {@code true} if this is enabled, {@code false} otherwise.
	 */
	public boolean isEnabled() {
		enabledLock.readLock().lock();
		try {
			return enabled;
		} finally {
			enabledLock.readLock().unlock();
		}
	}

	/**
	 * Sets the enabled status for this {@link Engine}.
	 *
	 * @param enabled {@code true} if this {@link Engine} is enabled,
	 *            {@code false} otherwise.
	 */
	public void setEnabled(boolean enabled, boolean setConfiguration) {
		enabledLock.writeLock().lock();
		try {
			this.enabled = enabled;
			if (setConfiguration) {
				CONFIGURATION.setEngineEnabled(getEngineId(), enabled);
			}
		} finally {
			enabledLock.writeLock().unlock();
		}
	}

	/**
	 * Toggles the enabled status for this {@link Engine}.
	 */
	public void toggleEnabled(boolean setConfiguration) {
		enabledLock.writeLock().lock();
		try {
			enabled = !enabled;
			if (setConfiguration) {
				CONFIGURATION.setEngineEnabled(getEngineId(), enabled);
			}
		} finally {
			enabledLock.writeLock().unlock();
		}
	}

	/**
	 * Convenience method to check if this {@link Engine} is both available and
	 * enabled for the specified {@link ProgramExecutableType}.
	 *
	 * @param executableType the {@link ProgramExecutableType} for which to
	 *            check availability.
	 * @return {@code true} if this {@link Engine} is both available and
	 *         enabled, {@code false} otherwise.
	 *
	 */
	public boolean isActive(ProgramExecutableType executableType) {
		return isAvailable(executableType) && isEnabled();
	}

	/**
	 * Convenience method to check if this {@link Engine} is both available and
	 * enabled for the current {@link ProgramExecutableType}.
	 *
	 * @return {@code true} if this {@link Engine} is both available and
	 *         enabled, {@code false} otherwise.
	 */
	public boolean isActive() {
		return isAvailable(currentExecutableType) && isEnabled();
	}

	/**
	 * Returns whether or not this {@link Engine} supports GPU acceleration.
	 * <p>
	 * Each {@link Engine} capable of video hardware acceleration must override
	 * this method and return {@code true}.
	 *
	 * @return {@code true} if GPU acceleration is supported, {@code false}
	 *         otherwise.
	 */
	public boolean isGPUAccelerationReady() {
		return false;
	}

	public abstract ProcessWrapper launchTranscode(
		DLNAResource dlna,
		MediaInfo media,
		OutputParams params
	) throws IOException;

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * This method populates the supplied {@link OutputParams} object with the
	 * correct audio track (aid) and subtitles (sid) based on resource
	 * information and configuration settings.
	 *
	 * @param fileName The file name used to determine the availability of
	 *            subtitles.
	 * @param media The MediaInfo metadata for the file.
	 * @param params The parameters to populate.
	 */
	public static void setAudioAndSubs(DLNAResource resource, OutputParams params) {
		if (resource == null || params == null || resource.getMedia() == null) {
			return;
		}

		if (params.getAid() == null) {
			params.setAid(resource.resolveAudioStream());
		}

		if (params.getSid() != null && params.getSid().getId() == MediaLang.DUMMY_ID) {
			LOGGER.trace("Don't want subtitles!");
			params.setSid(null);
		} else if (params.getSid() instanceof MediaOnDemandSubtitle dLNAMediaOnDemandSubtitle) {
			// Download/fetch live subtitles
			if (params.getSid().getExternalFile() == null) {
				if (!dLNAMediaOnDemandSubtitle.fetch()) {
					LOGGER.error("Failed to fetch on-demand subtitles \"{}\"", params.getSid().getName());
				}
				if (params.getSid().getExternalFile() == null) {
					params.setSid(null);
				}
			}
		} else if (params.getSid() == null) {
			params.setSid(resource.resolveSubtitlesStream(params.getMediaRenderer(), params.getAid() == null ? null : params.getAid().getLang(), true));
		}
	}

	/**
	 * Convert number to be divisible by mod.
	 *
	 * @param number the number to convert
	 * @param mod the number to divide by
	 * @return the number divisible by mod
	 */
	public static int convertToModX(int number, int mod) {
		if (number % mod != 0) {
			number -= (number % mod);
		}

		return number;
	}

	/**
	 * Returns whether or not this {@link Engine} can handle a given
	 * {@link DLNAResource}. If {@code resource} is {@code null} {@code false}
	 * will be returned.
	 *
	 * @param resource the {@link DLNAResource} to be matched.
	 * @return {@code true} if {@code resource} can be handled, {@code false}
	 *         otherwise.
	 */
	public abstract boolean isCompatible(DLNAResource resource);

	protected abstract boolean isSpecificTest();

	/**
	 * Does basic file tests of the specified executable, checking that it
	 * exists and has the required permissions.
	 *
	 * @param executableInfo the {@link ExecutableInfo} whose executable to
	 *            test.
	 * @return The resulting {@link ExecutableInfo} instance.
	 */
	@Nonnull
	protected ExecutableInfo testExecutableFile(@Nonnull ExecutableInfo executableInfo) {
		try {
			FilePermissions permissions = new FilePermissions(executableInfo.getPath());
			Set<FileFlag> flags = permissions.getFlags(FileFlag.FILE, FileFlag.EXECUTE, FileFlag.READ);
			if (!flags.contains(FileFlag.FILE) || !flags.contains(FileFlag.READ) || !flags.contains(FileFlag.EXECUTE)) {
				LOGGER.warn(
					"Insufficient permission to execute \"{}\" for transcoding engine {}",
					executableInfo.getPath(),
					this
				);
				executableInfo = executableInfo.modify()
					.available(Boolean.FALSE)
					.errorType(ExecutableErrorType.GENERAL)
					.errorText(
						String.format(Messages.getString("InsufficientPermissionExecuteTranscodingEngine"), executableInfo.getPath(), this)
					).build();
			}
		} catch (FileNotFoundException e) {
			LOGGER.warn(
				"Executable \"{}\" of transcoding engine {} not found: {}",
				executableInfo.getPath(),
				this,
				e.getMessage()
			);
			executableInfo = executableInfo.modify()
				.available(Boolean.FALSE)
				.errorType(ExecutableErrorType.GENERAL)
				.errorText(
					String.format(Messages.getString("ExecutableXTranscodingEngineNotFound"), executableInfo.getPath(), this)
				).build();
		}
		return executableInfo;
	}

	/**
	 * Tests a specific executable and returns the results. If the executable
	 * has already has been tested, the previous results are used.
	 * <p>
	 * <b>This method must be implemented unless {@link #testEngine} is
	 * overridden in such a way that this method is never called or no test can
	 * be performed on this executable</b> If the method isn't implemented,
	 * simply make it return {@code null}, which is interpreted by
	 * {@link #testEngine} as if no test was performed.
	 *
	 * @param executableInfo the {@link ExecutableInfo} whose executable to
	 *            test.
	 * @return The resulting {@link ExecutableInfo} instance.
	 */
	@Nullable
	protected abstract ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo);

	/**
	 * Tests the executable(s) for this {@link Engine} and stores the results.
	 * If the executable has already been tested by another {@link Engine} or
	 * {@link ProgramExecutableType}, the previous results are used.
	 *
	 * @param executableType the {@link ProgramExecutableType} to test. Invalid
	 *            {@link ProgramExecutableType}s for this {@link Engine} will
	 *            throw an {@link Exception}.
	 * @return {@code true} if a test was or previously has been performed,
	 *         {@code false} otherwise.
	 */
	public boolean testEngine(@Nonnull ProgramExecutableType executableType) {
		if (executableType == null) {
			throw new IllegalArgumentException("executableType cannot be null");
		}
		ReentrantReadWriteLock programInfoLock = programInfo.getLock();
		programInfoLock.writeLock().lock();
		try {
			ExecutableInfo executableInfo = programInfo.getExecutableInfo(executableType);
			if (executableInfo == null) {
				return false;
			}
			if (isAviSynthEngine()) {
				//TODO: adapt for linux/Mac, then remove this
				if (!Platform.isWindows()) {
					LOGGER.debug(
						"Skipping transcoding engine {} ({}) as it's not compatible with this platform",
						this,
						executableType
					);
					setUnavailable(
						executableType,
						ExecutableErrorType.SPECIFIC,
						String.format(Messages.getString("TranscodingEngineNotAvailableOperatingSystem"), this)
					);
					return true;
				}
				String aviSynthPath = configuration.getAviSynthPath();
				if (aviSynthPath == null || !new File(aviSynthPath).exists()) {
					LOGGER.debug(
						"Transcoding engine {} ({}) is unavailable since AviSynth couldn't be found",
						this,
						executableType
					);
					setUnavailable(
						executableType,
						ExecutableErrorType.SPECIFIC,
						String.format(Messages.getString("TranscodingEngineXNotAvailableAvisynth"), this)
					);
					return true;
				}
			}

			if (
				executableInfo.getAvailable() != null &&
				(
					!executableInfo.getAvailable() ||
					!isSpecificTest()
				)
			) {
				// Executable has already been tested
				return true;
			}
			specificErrorsLock.writeLock().lock();
			try {
				if (specificErrors.get(executableType) != null) {
					// Executable Engine specific failures has already been tested
					return true;
				}

				ExecutableInfo result = testExecutable(executableInfo);
				if (result == null) {
					// Executable test not implemented
					return false;
				}
				if (result.getAvailable() == null) {
					throw new AssertionError("Engine test for " + getName() + " failed to return availability");
				}
				if (!result.equals(executableInfo)) {
					// The test resulted in a change
					setAvailable(
						result.getAvailable(),
						executableType,
						result.getVersion(),
						result.getErrorType(),
						result.getErrorText()
					);
				}
				return true;
			} finally {
				specificErrorsLock.writeLock().unlock();
			}
		} finally {
			programInfoLock.writeLock().unlock();
		}
	}

	/**
	 * Checks if {@code object} is a {@link Engine} and has the same
	 * {@link #getEngineId()} as this.
	 *
     * @param object the reference object with which to compare.
	 * @return {@code true} if {@code object} is a {@link Engine} and the IDs
	 *         match, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof Engine other) {
			return (this == object ||
				(getEngineId() == null && other.getEngineId() == null) ||
				(getEngineId() != null && getEngineId().equals(other.getEngineId()))
			);
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getEngineId() == null) ? 0 : getEngineId().hashCode());
		return result;
	}
}
