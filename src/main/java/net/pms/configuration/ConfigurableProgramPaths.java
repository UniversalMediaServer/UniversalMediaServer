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
package net.pms.configuration;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.platform.PlatformProgramPaths;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.FFmpegProgramInfo;
import net.pms.util.FileUtil;
import net.pms.util.ProgramExecutableType;
/**
 * This class adds configurable/custom paths to {@link PlatformProgramPaths}.
 *
 * @author Nadahar
 */
@ThreadSafe
public class ConfigurableProgramPaths extends PlatformProgramPaths {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableProgramPaths.class);

	/** The {@link Configuration} key for the MPlayer executable type. */
	public static final String KEY_MPLAYER_EXECUTABLE_TYPE = "mplayer_executable_type";

	/** The {@link Configuration} key for the custom MPlayer path. */
	public static final String KEY_MPLAYER_PATH     = "mplayer_path";

	/** The {@link Configuration} key for the FLAC executable type. */
	public static final String KEY_FLAC_EXECUTABLE_TYPE = "flac_executable_type";

	/** The {@link Configuration} key for the custom FLAC path. */
	public static final String KEY_FLAC_PATH        = "flac_path";

	/** The {@link Configuration} key for the Interframe executable type. */
	public static final String KEY_INTERFRAME_EXECUTABLE_TYPE = "interframe_executable_type";

	/** The {@link Configuration} key for the custom InterFrame path. */
	public static final String KEY_INTERFRAME_PATH  = "interframe_path";

	/** The {@link Configuration} key for the FFMS2 executable type. */
	public static final String KEY_FFMS2_EXECUTABLE_TYPE = "ffms2_executable_type";

	/** The {@link Configuration} key for the custom FFMS2 path. */
	public static final String KEY_FFMS2_PATH  = "ffms2_path";

	/** The {@link Configuration} key for the 2DTO3D executable type. */
	public static final String KEY_2DTO3D_EXECUTABLE_TYPE = "2DTO3D_executable_type";

	/** The {@link Configuration} key for the custom 2DTO3D path. */
	public static final String KEY_2DTO3D_PATH  = "2dTo3d_path";

	/** The {@link Configuration} key for the custom youtube-dl path. */
	public static final String KEY_YOUTUBEDL_PATH  = "youtubedl_path";

	private final Configuration configuration;
	private final PlatformProgramPaths platformPaths = PlatformProgramPaths.get();

	/**
	 * Not to be instantiated, get the {@link ExternalProgramInfo} instances
	 * from {@link UmsConfiguration} instead.
	 *
	 * @param configuration the {@link Configuration} to use for custom paths.
	 */
	protected ConfigurableProgramPaths(@Nullable Configuration configuration) {
		this.configuration = configuration;

		// Read configured paths for all configurable program paths not handled by a Player.
		setCustomPathFromConfiguration(getMPlayer(), KEY_MPLAYER_PATH);
		setCustomPathFromConfiguration(getFLAC(), KEY_FLAC_PATH);
		setCustomPathFromConfiguration(getInterFrame(), KEY_INTERFRAME_PATH);
		setCustomPathFromConfiguration(getYoutubeDl(), KEY_YOUTUBEDL_PATH);
	}

	@Override
	public final FFmpegProgramInfo getFFmpeg() {
		return platformPaths.getFFmpeg();
	}

	@Override
	public final ExternalProgramInfo getMPlayer() {
		return platformPaths.getMPlayer();
	}

	@Override
	public final ExternalProgramInfo getVLC() {
		return platformPaths.getVLC();
	}

	@Override
	public ExternalProgramInfo getMEncoder() {
		return platformPaths.getMEncoder();
	}

	@Override
	public final ExternalProgramInfo getTsMuxeR() {
		return platformPaths.getTsMuxeR();
	}

	@Override
	public final ExternalProgramInfo getFLAC() {
		return platformPaths.getFLAC();
	}

	@Override
	public final ExternalProgramInfo getDCRaw() {
		return platformPaths.getDCRaw();
	}

	@Override
	public final ExternalProgramInfo getAviSynth() {
		return platformPaths.getAviSynth();
	}

	@Override
	public final ExternalProgramInfo getInterFrame() {
		return platformPaths.getInterFrame();
	}

	@Override
	public final ExternalProgramInfo getFFMS2() {
		return platformPaths.getFFMS2();
	}

	@Override
	public final ExternalProgramInfo getDirectShowSource() {
		return platformPaths.getFFMS2();
	}

	@Override
	public final ExternalProgramInfo getMvtools2() {
		return platformPaths.getMvtools2();
	}

	@Override
	public final ExternalProgramInfo getMasktools2() {
		return platformPaths.getMasktools2();
	}

	@Override
	public final ExternalProgramInfo getConvert2dTo3d() {
		return platformPaths.getConvert2dTo3d();
	}

	@Override
	public final ExternalProgramInfo getYoutubeDl() {
		return platformPaths.getYoutubeDl();
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for MPlayer
	 * both in {@link #configuration} and the {@link ExternalProgramInfo}.
	 *
	 * @param path the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomMPlayerPath(@Nullable Path path) {
		setCustomProgramPath(path, platformPaths.getMPlayer(), KEY_MPLAYER_PATH, true);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for FLAC
	 * both in {@link #configuration} and the {@link ExternalProgramInfo}.
	 *
	 * @param path the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomFlacPath(@Nullable Path path) {
		setCustomProgramPath(path, platformPaths.getFLAC(), KEY_FLAC_PATH, true);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for
	 * Interframe both in {@link #configuration} and the
	 * {@link ExternalProgramInfo}.
	 *
	 * @param path the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomInterFramePath(@Nullable Path path) {
		setCustomProgramPath(path, platformPaths.getInterFrame(), KEY_INTERFRAME_PATH, true);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for
	 * FFMS2 both in {@link #configuration} and the
	 * {@link ExternalProgramInfo}.
	 *
	 * @param path the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomFFMS2Path(@Nullable Path path) {
		setCustomProgramPath(path, platformPaths.getFFMS2(), KEY_FFMS2_PATH, true);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for
	 * Convert2dTo3d both in {@link #configuration} and the
	 * {@link ExternalProgramInfo}.
	 *
	 * @param path the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomConvert2dTo3dPath(@Nullable Path path) {
		setCustomProgramPath(path, platformPaths.getConvert2dTo3d(), KEY_2DTO3D_PATH, true);
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} for youtube-dl
	 * both in {@link #configuration} and the {@link ExternalProgramInfo}.
	 *
	 * @param path the new {@link Path} or {@code null} to clear it.
	 */
	public void setCustomYoutubeDlPath(@Nullable Path path) {
		setCustomProgramPath(path, platformPaths.getYoutubeDl(), KEY_YOUTUBEDL_PATH, true);
	}

	/**
	 * Returns the configured {@link ProgramExecutableType} for the specified
	 * {@link ExternalProgramInfo} using the specified {@link Configuration}
	 * key. If the {@link Configuration} doesn't contain any value for the
	 * specified key, the default from the {@link ProgramExecutableType} is
	 * used.
	 *
	 * @param programInfo the {@link ExternalProgramInfo} to use for the default
	 *            value.
	 * @param configurationKey the {@link Configuration} key to use.
	 * @return The resulting {@link ProgramExecutableType}.
	 */
	@Nullable
	public ProgramExecutableType getConfiguredExecutableType(
		@Nullable ExternalProgramInfo programInfo,
		@Nullable String configurationKey
	) {
		if (configuration == null) {
			return null;
		}

		return getConfiguredExecutableType(configurationKey, programInfo == null ? null : programInfo.getDefault());
	}

	/**
	 * Returns the configured {@link ProgramExecutableType} from the specified
	 * {@link Configuration} key. If the {@link Configuration} doesn't contain
	 * any value for the specified key, the specified default is used.
	 *
	 * @param configurationKey the {@link Configuration} key to use.
	 * @param defaultExecutableType the default {@link ProgramExecutableType} if
	 *            the specified key has no value.
	 * @return The resulting {@link ProgramExecutableType}.
	 */
	@Nullable
	public ProgramExecutableType getConfiguredExecutableType(
		@Nullable String configurationKey,
		@Nullable ProgramExecutableType defaultExecutableType
	) {
		if (configuration == null || isBlank(configurationKey)) {
			return null;
		}
		return ProgramExecutableType.toProgramExecutableType(
			configuration.getString(configurationKey),
			defaultExecutableType
		);
	}

	/**
	 * Sets the {@link ProgramExecutableType#CUSTOM} path for the given
	 * {@link ExternalProgramInfo} to its configured value.
	 *
	 * @param programInfo the {@link ExternalProgramInfo} for which to set
	 *            the {@link ProgramExecutableType#CUSTOM} {@link Path}.
	 * @param configurationKey the {@link Configuration} key to read.
	 */
	private void setCustomPathFromConfiguration(
		@Nullable ExternalProgramInfo programInfo,
		@Nullable String configurationKey
	) {
		if (programInfo == null || configuration == null) {
			return;
		}
		if (isBlank(configurationKey)) {
			throw new IllegalArgumentException("configurationKey can't be blank");
		}

		Path customPath;
		try {
			customPath = getCustomProgramPath(configurationKey);
		} catch (ConfigurationException e) {
			customPath = null;
		}
		setCustomProgramPath(customPath, programInfo, configurationKey, false);
	}

	/**
	 * Gets the configured custom program {@link Path} from the
	 * {@link Configuration} using the specified key. If the specified key has
	 * no value, {@code null} is returned.
	 *
	 * @param configurationKey the {@link Configuration} key to use.
	 * @return The resulting {@link Path} or {@code null}.
	 * @throws ConfigurationException If the configured value can't be parsed as
	 *             a valid {@link Path}.
	 */
	@Nullable
	public Path getCustomProgramPath(@Nullable String configurationKey) throws ConfigurationException {
		if (isBlank(configurationKey) || configuration == null) {
			return null;
		}

		try {
			String configuredPath = configuration.getString(configurationKey);
			if (isBlank(configuredPath)) {
				return null;
			}
			return Paths.get(configuredPath);
		} catch (ConversionException | InvalidPathException e) {
			throw new ConfigurationException(
				"Invalid configured custom program path in \"" + configurationKey + "\": " + e.getMessage(),
				e
			);
		}
	}

	/**
	 * Sets a new {@link ProgramExecutableType#CUSTOM} {@link Path} in the
	 * {@link Configuration} for the specified key. No change is done to any
	 * {@link ExternalProgramInfo} instance. To set the {@link Path} both in the
	 * {@link Configuration} and in the {@link ExternalProgramInfo} instance in
	 * one operation, use {@link #setCustomProgramPath}.
	 *
	 * @param customPath the new {@link Path} or {@code null} to clear it.
	 * @param configurationKey the {@link Configuration} key under which to
	 *            store the {@code path}.
	 * @return {@code true} if a change was made, {@code false} otherwise.
	 */
	public boolean setCustomProgramPathConfiguration(@Nullable Path customPath, @Nonnull String configurationKey) {
		if (isBlank(configurationKey)) {
			throw new IllegalArgumentException("configurationKey can't be blank");
		}
		if (configuration == null) {
			return false;
		}
		if (customPath == null) {
			if (configuration.containsKey(configurationKey)) {
				configuration.clearProperty(configurationKey);
				return true;
			}
			return false;
		}
		boolean changed;
		try {
			String currentValue = configuration.getString(configurationKey);
			changed = !customPath.toString().equals(currentValue);
		} catch (ConversionException e) {
			changed = true;
		}
		if (changed) {
			configuration.setProperty(configurationKey, customPath.toString());
		}
		return changed;
	}

	/**
	 * Sets the {@link ProgramExecutableType#CUSTOM} path for the given
	 * {@link ExternalProgramInfo}.
	 *
	 * @param customPath the custom executable {@link Path}.
	 * @param programInfo the {@link ExternalProgramInfo} for which to set the
	 *            {@link ProgramExecutableType#CUSTOM} {@link Path}.
	 * @param configurationKey the {@link Configuration} key under which to
	 *            store the {@code path}. Cannot be {@code null} if
	 *            {@code setConfiguration} is {@code true}.
	 * @param setConfiguration whether or not {@code customPath} should also be
	 *            stored in the current {@link Configuration}.
	 */
	protected void setCustomProgramPath(
		@Nullable Path customPath,
		@Nullable ExternalProgramInfo programInfo,
		@Nullable String configurationKey,
		boolean setConfiguration
	) {
		if (programInfo == null || configuration == null) {
			return;
		}

		if (setConfiguration && isBlank(configurationKey)) {
			throw new IllegalArgumentException("configurationKey can't be blank if setConfiguration is true");
		}

		if (setConfiguration) {
			setCustomProgramPathConfiguration(customPath, configurationKey);
		}

		customPath = resolveCustomProgramPath(customPath);
		ReentrantReadWriteLock lock = programInfo.getLock();
		lock.writeLock().lock();
		try {
			if (customPath == null) {
				if (programInfo.getExecutableInfo(ProgramExecutableType.CUSTOM) != null) {
					programInfo.setExecutableInfo(ProgramExecutableType.CUSTOM, null);
					programInfo.setOriginalDefault();
					LOGGER.debug("Cleared custom {} path", programInfo.getName());
				}
			} else {
				ExecutableInfo executableInfo = programInfo.getExecutableInfo(ProgramExecutableType.CUSTOM);
				if (executableInfo == null || !customPath.equals(executableInfo.getPath())) {
					programInfo.setPath(ProgramExecutableType.CUSTOM, customPath);
					programInfo.setDefault(ProgramExecutableType.CUSTOM);
					LOGGER.debug("Set custom {} path \"{}\"", programInfo.getName(), customPath);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Resolves the path in that the existence of the specified {@link Path} is
	 * verified. It it doesn't exist and the {@link Path} is relative, an
	 * attempt to look it up in the OS {@code PATH}. If a match is found in the
	 * OS {@code PATH}, that {@link Path} is returned. If not, the input
	 * {@link Path} is returned.
	 *
	 * @param customPath the custom executable {@link Path} to resolve.
	 * @return The same instance as {@code customPath} it it exists or a match
	 *         isn't found in the OS {@code PATH}, otherwise the {@link Path} to
	 *         the matched executable found in the OS {@code PATH}.
	 */
	@Nullable
	public static Path resolveCustomProgramPath(@Nullable Path customPath) {
		if (customPath == null) {
			return null;
		}

		if (!Files.exists(customPath) && !customPath.isAbsolute()) {
			Path osPathCustom = FileUtil.findExecutableInOSPath(customPath);
			if (osPathCustom != null) {
				customPath = osPathCustom;
			}
		}

		return customPath;
	}
}
