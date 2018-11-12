package net.pms.configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import net.pms.encoders.ExecutableErrorType;
import net.pms.util.Version;


/**
 * A class to hold basic information about a FFmpeg executable and its
 * availability status.
 *
 * @author Nadahar
 */
@Immutable
public class FFmpegExecutableInfo extends ExecutableInfo {

	/**
	 * The {@link List} of {@link String} codes for the supported protocols
	 */
	@Nullable
	protected final List<String> protocols;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param available {@code true} if the executable is tested and found
	 *            available, {@code false} if the executable is tested and found
	 *            unavailable or {@code null} if the executable is untested.
	 * @param path the {@link Path} to the executable.
	 * @param version the {@link Version} of the executable or {@code null} if
	 *            unknown.
	 * @param errorType the {@link ExecutableErrorType} if {@code available} is
	 *            {@code false}, {@code null} otherwise.
	 * @param errorText the localized error text if {@code available} is
	 *            {@code false}, {@code null} otherwise.
	 * @param protocols a {@link List} of {@link String}s containing codes for
	 *            the supported protocols for this executable.
	 */
	public FFmpegExecutableInfo(
		@Nullable Boolean available,
		@Nonnull Path path,
		@Nullable Version version,
		@Nullable ExecutableErrorType errorType,
		@Nullable String errorText,
		@Nullable List<String> protocols
	) {
		super(available, path, version, errorType, errorText);
		this.protocols = protocols == null ? null : Collections.unmodifiableList(new ArrayList<>(protocols));
	}

	/**
	 * @return A new {@link FFmpegExecutableInfoBuilder} initialized with the
	 *         values of this {@link FFmpegExecutableInfo}. When done modifying,
	 *         convert the {@link FFmpegExecutableInfoBuilder} into a new
	 *         {@link FFmpegExecutableInfo} instance with
	 *         {@link FFmpegExecutableInfoBuilder#build()}.
	 */
	@Override
	public FFmpegExecutableInfoBuilder modify() {
		return new FFmpegExecutableInfoBuilder(this);
	}

	/**
	 * Returns a new {@link FFmpegExecutableInfoBuilder} for the specified
	 * executable. Use {@link FFmpegExecutableInfoBuilder#build()} to create a
	 * new {@link FFmpegExecutableInfo} instance once the values are set.
	 *
	 * @param executablePath the {@link Path} to the executable.
	 * @return The new {@link FFmpegExecutableInfoBuilder} instance.
	 */
	public static FFmpegExecutableInfoBuilder build(Path executablePath) {
		return new FFmpegExecutableInfoBuilder(executablePath);
	}

	/**
	 * @return The {@link List} of {@link String} codes for the supported
	 *         protocols.
	 */
	@Nullable
	public List<String> getProtocols() {
		return protocols;
	}

	@Override
	public String toString() {
		return
			"FFmpegExecutableInfo [executablePath=" + executablePath + ", available=" + available +
			", version=" + version + ", errorType=" + errorType + ", errorText=" + errorText +
			", protocols=" + protocols + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((protocols == null) ? 0 : protocols.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof FFmpegExecutableInfo)) {
			return false;
		}
		FFmpegExecutableInfo other = (FFmpegExecutableInfo) obj;
		if (protocols == null) {
			if (other.protocols != null) {
				return false;
			}
		} else if (!protocols.equals(other.protocols)) {
			return false;
		}
		return true;
	}

	/**
	 * A builder class to build {@link ExecutableInfo} instances by setting
	 * individual values.
	 */
	public static class FFmpegExecutableInfoBuilder extends ExecutableInfoBuilder {

		/**
		 * The {@link List} of {@link String}s with codes for the supported
		 * protocols
		 */
		@Nullable
		protected List<String> protocols;

		/**
		 * Creates a new {@link FFmpegExecutableInfoBuilder} with no values set.
		 */
		public FFmpegExecutableInfoBuilder() {
		}

		/**
		 * Creates a new {@link FFmpegExecutableInfoBuilder} for the specified
		 * executable.
		 *
		 * @param executablePath the {@link Path} to the executable.
		 */
		public FFmpegExecutableInfoBuilder(Path executablePath) {
			this.executablePath = executablePath;
		}

		/**
		 * Creates a new {@link FFmpegExecutableInfoBuilder} whose values is
		 * initialized by the specified {@link ExecutableInfo}.
		 *
		 * @param executableInfo the {@link ExecutableInfo} whose values to use.
		 */
		public FFmpegExecutableInfoBuilder(ExecutableInfo executableInfo) {
			this.available = executableInfo.available;
			this.executablePath = executableInfo.executablePath;
			this.version = executableInfo.version;
			this.errorType = executableInfo.errorType;
			this.errorText = executableInfo.errorText;
			if (executableInfo instanceof FFmpegExecutableInfo) {
				this.protocols = ((FFmpegExecutableInfo) executableInfo).protocols;
			}
		}

		/**
		 * Creates a {@link FFmpegExecutableInfo} instance from this
		 * {@link FFmpegExecutableInfoBuilder}.
		 *
		 * @return The new {@link FFmpegExecutableInfo} instance.
		 */
		@Override
		@Nonnull
		public FFmpegExecutableInfo build() {
			return new FFmpegExecutableInfo(available, executablePath, version, errorType, errorText, protocols);
		}

		@Override
		@Nonnull
		public FFmpegExecutableInfoBuilder executablePath(Path executablePath) {
			this.executablePath = executablePath;
			return this;
		}

		@Override
		@Nonnull
		public FFmpegExecutableInfoBuilder available(Boolean available) {
			this.available = available;
			return this;
		}

		@Override
		@Nonnull
		public FFmpegExecutableInfoBuilder version(Version version) {
			this.version = version;
			return this;
		}

		@Override
		@Nonnull
		public FFmpegExecutableInfoBuilder errorType(ExecutableErrorType errorType) {
			this.errorType = errorType;
			return this;
		}

		@Override
		public FFmpegExecutableInfoBuilder errorText(String errorText) {
			this.errorText = errorText;
			return this;
		}

		/**
		 * @return The {@link List} of {@link String}s with codes for the
		 *         supported protocols.
		 */
		@Nullable
		public List<String> protocols() {
			return protocols;
		}

		/**
		 * Sets the {@link List} of {@link String}s with codes for the supported
		 * protocols.
		 *
		 * @param protocols the {@link List} of protocol codes to set.
		 * @return The {@link FFmpegExecutableInfoBuilder} instance.
		 */
		public FFmpegExecutableInfoBuilder protocols(@Nullable List<String> protocols) {
			this.protocols = protocols;
			return this;
		}
	}
}
