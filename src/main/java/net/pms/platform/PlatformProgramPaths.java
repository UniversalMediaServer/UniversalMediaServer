/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.platform;

import com.sun.jna.Platform;
import java.io.FileNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.platform.linux.LinuxProgramPaths;
import net.pms.platform.mac.MacProgramPaths;
import net.pms.platform.windows.WindowsProgramPaths;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.FilePermissions;
import net.pms.util.FFmpegProgramInfo;
import net.pms.util.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of paths to external programs.
 *
 * @author Nadahar
 */
public abstract class PlatformProgramPaths {

	/**
	 * @return The {@link FFmpegProgramInfo} for FFmpeg.
	 */
	@Nullable
	public abstract FFmpegProgramInfo getFFmpeg();

	/**
	 * @return The {@link ExternalProgramInfo} for MPlayer.
	 */
	@Nullable
	public abstract ExternalProgramInfo getMPlayer();

	/**
	 * @return The {@link ExternalProgramInfo} for VLC.
	 */
	@Nullable
	public abstract ExternalProgramInfo getVLC();

	/**
	 * @return The {@link ExternalProgramInfo} for MEncoder.
	 */
	@Nullable
	public abstract ExternalProgramInfo getMEncoder();

	/**
	 * @return The {@link ExternalProgramInfo} for tsMuxeR.
	 */
	@Nullable
	public abstract ExternalProgramInfo getTsMuxeR();

	/**
	 * @return The {@link ExternalProgramInfo} for tsMuxeRNew.
	 */
	@Nullable
	public abstract ExternalProgramInfo getTsMuxeRNew();

	/**
	 * @return The {@link ExternalProgramInfo} for FLAC.
	 */
	@Nullable
	public abstract ExternalProgramInfo getFLAC();

	/**
	 * @return The {@link ExternalProgramInfo} for DCRaw.
	 */
	@Nullable
	public abstract ExternalProgramInfo getDCRaw();

	/**
	 * @return The {@link ExternalProgramInfo} for InterFrame.
	 */
	@Nullable
	public abstract ExternalProgramInfo getInterFrame();

	/**
	 * @return The {@link ExternalProgramInfo} for InterFrame.
	 */
	@Nullable
	public abstract ExternalProgramInfo getYoutubeDl();

	/**
	 * @return The {@link Path} for {@code ctrlsender.exe} for Windows.
	 */
	@Nullable
	public Path getCtrlSender() {
		return null;
	}

	/**
	 * @return The {@link Path} for {@code taskkill.exe} for Windows.
	 */
	@Nullable
	public Path getTaskKill() {
		return null;
	}

	/** Singleton {@link PlatformProgramPaths} instance */
	@Nonnull
	private static final PlatformProgramPaths INSTANCE;

	/** The {@link Path} to {@code project.binaries.dir}. */
	protected static final Path BINARIES_FOLDER = getBinariesFolder();

	/** The {@link Path} to the bundled binaries for the current platform. */
	protected static final Path PLATFORM_BINARIES_FOLDER;

	/** The {@link Path} to the downloaded development binaries for the current platform. */
	protected static final Path PLATFORM_DEVELOPMENT_BINARIES_FOLDER;

	static {
		String subPath;
		if (Platform.isWindows()) {
			subPath = "windows";
		} else if (Platform.isMac()) {
			subPath = "osx";
		} else {
			subPath = "linux";
		}
		PLATFORM_BINARIES_FOLDER = BINARIES_FOLDER.resolve(subPath).toAbsolutePath();
		Path developmentBinaryFolder = Paths.get("target/bin", subPath);
		try {
			FilePermissions permission = new FilePermissions(developmentBinaryFolder);
			if (permission.isBrowsable()) {
				developmentBinaryFolder = developmentBinaryFolder.toAbsolutePath();
			} else {
				developmentBinaryFolder = null;
			}
		} catch (FileNotFoundException e) {
			developmentBinaryFolder = null;
		}
		PLATFORM_DEVELOPMENT_BINARIES_FOLDER = developmentBinaryFolder;

		if (Platform.isWindows()) {
			INSTANCE = new WindowsProgramPaths();
		} else if (Platform.isMac()) {
			INSTANCE = new MacProgramPaths();
		} else {
			INSTANCE = new LinuxProgramPaths();
		}
	}

	/**
	 * Returns a platform dependent {@link PlatformProgramPaths} instance.
	 * <p>
	 * <b>Note:</b> The returned instance does not support customizable program
	 * paths. Use {@link PmsConfiguration} to retrieve customizable
	 * {@link ExternalProgramInfo} instances.
	 *
	 * @return The platform dependent {@link PlatformProgramPaths} instance.
	 */
	@Nonnull
	public static final PlatformProgramPaths get() {
		return INSTANCE;
	}

	/**
	 * Returns the (relative) {@link Path} where binaries can be found. This
	 * {@link Path} differs between the build phase and the test phase.
	 *
	 * @return The path to the binaries folder.
	 */
	@Nonnull
	protected static Path getBinariesFolder() {
		String path = PropertiesUtil.getProjectProperties().get("project.binaries.dir");

		try {
			if (StringUtils.isNotBlank(path)) {
				return Paths.get(path);
			}
		} catch (InvalidPathException e) {
			System.err.println("Invalid \"project.binaries.dir\":" + e);
		}
		return Paths.get("");
	}

}
