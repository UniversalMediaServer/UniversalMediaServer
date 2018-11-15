package net.pms.configuration;

import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;


/**
 * This class holds information about the different executable types stored for
 * {@code FFmpeg}. Callers can lock an instance when performing multiple
 * operations in an atomic manner using {@link #getLock()}.
 *
 * @author Nadahar
 */
@ThreadSafe
public class FFmpegProgramInfo extends ExternalProgramInfo {

	/**
	 * Creates a new instance with the given arguments.
	 *
	 * @param programName the human readable name for the program to which the
	 *            new {@link ExternalProgramInfo} applies, this is not the
	 *            filename of a particular executable, but the general name of
	 *            the program.
	 * @param defaultType the default {@link ProgramExecutableType} for this
	 *            external program.
	 */
	public FFmpegProgramInfo(@Nullable String programName, @Nullable ProgramExecutableType defaultType) {
		super(programName, defaultType);
	}

	/**
	 * Creates a new instance with the given arguments.
	 *
	 * @param programName the human readable name for the program to which the
	 *            new {@link ExternalProgramInfo} applies, this is not the
	 *            filename of a particular executable, but the general name of
	 *            the program.
	 * @param defaultType the default {@link ProgramExecutableType} for this
	 *            external program.
	 * @param executablesInfo a {@link Map} of {@link ProgramExecutableType}s
	 *            with their corresponding {@link ExecutableInfo}s.
	 */
	public FFmpegProgramInfo(
		@Nullable String programName,
		@Nullable ProgramExecutableType defaultType,
		@Nullable Map<ProgramExecutableType, FFmpegExecutableInfo> executablesInfo
	) {
		super(programName, defaultType, executablesInfo);
	}

	/**
	 * Copy constructor, creates a "deep-clone" in that {@link #executablesInfo}
	 * and {@link #lock} are new instances.
	 *
	 * @param programName the human readable name for the program to which the
	 *            new {@link ExternalProgramInfo} applies, this is not the
	 *            filename of a particular executable, but the general name of
	 *            the program.
	 * @param defaultType the default {@link ProgramExecutableType} for this
	 *            external program.
	 * @param originalDefaultType the default {@link ProgramExecutableType} set
	 *            by the original constructor.
	 * @param executablesInfo a {@link Map} of {@link ProgramExecutableType}s
	 *            with their corresponding {@link ExecutableInfo}s.
	 */
	protected FFmpegProgramInfo(
		@Nullable String programName,
		@Nullable ProgramExecutableType defaultType,
		@Nullable ProgramExecutableType originalDefaultType,
		@Nullable Map<ProgramExecutableType, ? extends ExecutableInfo> executablesInfo
	) {
		super(programName, defaultType, originalDefaultType, executablesInfo);
	}

	/**
	 * Sets the {@link FFmpegExecutableInfo} for the specified
	 * {@link ProgramExecutableType}.
	 *
	 * @param executableType the {@link ProgramExecutableType} for which to set.
	 * @param executableInfo the {@link FFmpegExecutableInfo} to set.
	 * @throws IllegalArgumentException If {@code executableInfo} is not
	 *             {@code null} and is not an instance of
	 *             {@link FFmpegExecutableInfo}.
	 */
	@Override
	public void setExecutableInfo(
		@Nonnull ProgramExecutableType executableType,
		@Nullable ExecutableInfo executableInfo
	) {
		if (executableInfo == null || executableInfo instanceof FFmpegExecutableInfo) {
			super.setExecutableInfo(executableType, executableInfo);
		} else {
			throw new IllegalArgumentException("executableInfo must be an instance of FFmpegExecutableInfo");
		}
	}

	@Override
	@Nonnull
	protected FFmpegExecutableInfo createExecutableInfo(@Nonnull Path executablePath) {
		return FFmpegExecutableInfo.build(executablePath).build();
	}
	/**
	 * Returns a "deep-clone" of this instance where mutable objects are copied.
	 *
	 * @return The new {@link FFmpegProgramInfo}.
	 */
	@Override
	@Nonnull
	public FFmpegProgramInfo copy() {
		return new FFmpegProgramInfo(programName, defaultType, originalDefaultType, executablesInfo);
	}
}
