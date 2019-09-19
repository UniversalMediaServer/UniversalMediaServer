package net.pms.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import net.pms.encoders.ExecutableErrorType;
import net.pms.util.Version;


/**
 * A class to hold basic information about an external executable and its
 * availability status.
 *
 * @author Nadahar
 */
@Immutable
public class ExecutableInfo {

	/** The {@link Path} to this executable */
	@Nonnull
	protected final Path executablePath;

	/** Whether or not {@link #executablePath} exists */
	protected final boolean pathExists;

	/** The availability of this executable */
	@Nullable
	protected final Boolean available;

	/** The {@link Version} of this executable */
	@Nullable
	protected final Version version;

	/** The {@link ExecutableErrorType} for this executable, if any */
	@Nullable
	protected final ExecutableErrorType errorType;

	/** The localized error text for this executable, if any */
	@Nullable
	protected final String errorText;

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
	 */
	public ExecutableInfo(
		@Nullable Boolean available,
		@Nonnull Path path,
		@Nullable Version version,
		@Nullable ExecutableErrorType errorType,
		@Nullable String errorText
	) {
		if (path == null) {
			throw new IllegalArgumentException("path cannot be null");
		}
		if (available != null && !available.booleanValue() && (errorType == null || errorText == null)) {
			throw new IllegalArgumentException("errorType and errorText must be specified in available is false");
		}
		this.available = available;
		this.executablePath = path;
		this.version = version;
		this.errorType = errorType;
		this.errorText = errorText;
		this.pathExists = Files.exists(path);
	}

	/**
	 * @return The {@link Path} to this executable.
	 */
	@Nonnull
	public Path getPath() {
		return executablePath;
	}

	/**
	 * @return {@code true} if the executable existed at the time of this
	 *         {@link ExecutableInfo}'s creation, {@code false} otherwise.
	 */
	public boolean isPathExisting() {
		return pathExists;
	}

	/**
	 * @return The available status of this executable.
	 */
	@Nullable
	public Boolean getAvailable() {
		return available;
	}

	/**
	 * @return The {@link Version} of this executable or {@code null} if
	 *         unknown.
	 */
	@Nullable
	public Version getVersion() {
		return version;
	}

	/**
	 * @return The {@link ExecutableErrorType} for this executable if relevant,
	 *         {@code null} otherwise.
	 */
	@Nullable
	public ExecutableErrorType getErrorType() {
		return errorType;
	}

	/**
	 * @return The localized error text for this executable if relevant,
	 *         {@code null} otherwise.
	 */
	@Nullable
	public String getErrorText() {
		return errorText;
	}

	/**
	 * @return A new {@link ExecutableInfoBuilder} initialized with the values
	 *         of this {@link ExecutableInfo}. When done modifying, convert the
	 *         {@link ExecutableInfoBuilder} into a new {@link ExecutableInfo}
	 *         instance with {@link ExecutableInfoBuilder#build()}.
	 */
	public ExecutableInfoBuilder modify() {
		return new ExecutableInfoBuilder(this);
	}

	/**
	 * Returns a new {@link ExecutableInfoBuilder} for the specified executable.
	 * Use {@link ExecutableInfoBuilder#build()} to create a new
	 * {@link ExecutableInfo} instance once the values are set.
	 *
	 * @param executablePath the {@link Path} to the executable.
	 * @return The new {@link ExecutableInfoBuilder} instance.
	 */
	public static ExecutableInfoBuilder build(Path executablePath) {
		return new ExecutableInfoBuilder(executablePath);
	}

	@Override
	public String toString() {
		return
			"ExecutableInfo [executablePath=" + executablePath + ", available=" + available +
			", version=" + version + ", errorType=" + errorType + ", errorText=" + errorText + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((available == null) ? 0 : available.hashCode());
		result = prime * result + ((errorText == null) ? 0 : errorText.hashCode());
		result = prime * result + ((errorType == null) ? 0 : errorType.hashCode());
		result = prime * result + executablePath.hashCode();
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ExecutableInfo)) {
			return false;
		}
		ExecutableInfo other = (ExecutableInfo) obj;
		if (available == null) {
			if (other.available != null) {
				return false;
			}
		} else if (!available.equals(other.available)) {
			return false;
		}
		if (errorText == null) {
			if (other.errorText != null) {
				return false;
			}
		} else if (!errorText.equals(other.errorText)) {
			return false;
		}
		if (errorType != other.errorType) {
			return false;
		}
		if (!executablePath.equals(other.executablePath)) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		return true;
	}

	/**
	 * A builder class to build {@link ExecutableInfo} instances by setting
	 * individual values.
	 */
	public static class ExecutableInfoBuilder {

		/** The {@link Path} to this executable */
		protected Path executablePath;

		/** The availability of this executable */
		protected Boolean available;

		/** The {@link Version} of this executable */
		protected Version version;

		/** The {@link ExecutableErrorType} for this executable, if any */
		protected ExecutableErrorType errorType;

		/** The localized error text for this executable, if any */
		protected String errorText;

		/**
		 * Creates a new {@link ExecutableInfoBuilder} with no values set.
		 */
		public ExecutableInfoBuilder() {
		}

		/**
		 * Creates a new {@link ExecutableInfoBuilder} for the specified
		 * executable.
		 *
		 * @param executablePath the {@link Path} to the executable.
		 */
		public ExecutableInfoBuilder(Path executablePath) {
			this.executablePath = executablePath;
		}

		/**
		 * Creates a new {@link ExecutableInfoBuilder} whose values is
		 * initialized by the specified {@link ExecutableInfo}.
		 *
		 * @param executableInfo the {@link ExecutableInfo} whose values to use.
		 */
		public ExecutableInfoBuilder(ExecutableInfo executableInfo) {
			this.available = executableInfo.available;
			this.executablePath = executableInfo.executablePath;
			this.version = executableInfo.version;
			this.errorType = executableInfo.errorType;
			this.errorText = executableInfo.errorText;
		}

		/**
		 * Creates a {@link ExecutableInfo} instance from this
		 * {@link ExecutableInfoBuilder}.
		 *
		 * @return The new {@link ExecutableInfo} instance.
		 */
		@Nonnull
		public ExecutableInfo build() {
			return new ExecutableInfo(available, executablePath, version, errorType, errorText);
		}

		/**
		 * @return {@code true} if calling {@link #build()} will succeed,
		 *         {@code false} if it will result in an
		 *         {@link IllegalArgumentException}.
		 */
		public boolean isValid() {
			return
				executablePath != null && (
					(
						available == null ||
						available.booleanValue()
					) || (
						errorType != null &&
						errorText != null
					)
				);
		}

		/**
		 * @return The {@link Path} to this executable.
		 */
		@Nullable
		public Path executablePath() {
			return executablePath;
		}

		/**
		 * Sets the {@link Path} to this executable.
		 *
		 * @param executablePath the {@link Path} to set.
		 * @return The {@link ExecutableInfoBuilder} instance.
		 */
		@Nonnull
		public ExecutableInfoBuilder executablePath(Path executablePath) {
			this.executablePath = executablePath;
			return this;
		}

		/**
		 * @return The available status of this executable.
		 */
		@Nullable
		public Boolean available() {
			return available;
		}

		/**
		 * Sets the available status of this executable.
		 *
		 * @param available the available status to set.
		 * @return The {@link ExecutableInfoBuilder} instance.
		 */
		@Nonnull
		public ExecutableInfoBuilder available(Boolean available) {
			this.available = available;
			return this;
		}

		/**
		 * @return The {@link Version} of this executable.
		 */
		@Nullable
		public Version version() {
			return version;
		}

		/**
		 * Sets the {@link Version} for this executable.
		 *
		 * @param version the {@link Version} to set.
		 * @return The {@link ExecutableInfoBuilder} instance.
		 */
		@Nonnull
		public ExecutableInfoBuilder version(Version version) {
			this.version = version;
			return this;
		}

		/**
		 * @return The {@link ExecutableErrorType} for this executable.
		 */
		@Nullable
		public ExecutableErrorType errorType() {
			return errorType;
		}

		/**
		 * Sets the {@link ExecutableErrorType} for this executable.
		 *
		 * @param errorType the {@link ExecutableErrorType} to set.
		 * @return The {@link ExecutableInfoBuilder} instance.
		 */
		@Nonnull
		public ExecutableInfoBuilder errorType(ExecutableErrorType errorType) {
			this.errorType = errorType;
			return this;
		}

		/**
		 * @return The localized error text for this executable.
		 */
		@Nullable
		public String errorText() {
			return errorText;
		}

		/**
		 * Sets the localized error text for this executable.
		 *
		 * @param errorText the error text to set.
		 * @return The {@link ExecutableInfoBuilder} instance.
		 */
		public ExecutableInfoBuilder errorText(String errorText) {
			this.errorText = errorText;
			return this;
		}
	}
}
