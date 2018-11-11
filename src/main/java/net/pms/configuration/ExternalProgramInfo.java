/*
 * Digital Media Server, for streaming digital media to DLNA compatible devices
 * based on www.ps3mediaserver.org and www.universalmediaserver.com.
 * Copyright (C) 2016 Digital Media Server developers.
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
package net.pms.configuration;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.lang.model.type.ExecutableType;
import net.pms.encoders.ExecutableErrorType;
import net.pms.util.Version;


/**
 * This class holds information about the different executable types stored for
 * a given external program. Callers can lock an instance when performing
 * multiple operations in an atomic manner using {@link #getLock()}.
 *
 * @author Nadahar
 */
@ThreadSafe
public class ExternalProgramInfo {

	/** The lock protecting all mutable class fields */
	protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	/** The default {@link ProgramExecutableType} for this external program. */
	@GuardedBy("lock")
	protected ProgramExecutableType defaultType;

	/** The default {@link ProgramExecutableType} set by the constructor. */
	protected final ProgramExecutableType originalDefaultType;

	/**
	 * A {@link HashMap} containing the paths to the different
	 * {@link ProgramExecutableType}s for this executable.
	 */
	@GuardedBy("lock")
	protected final HashMap<ProgramExecutableType, ExecutableInfo> executablesInfo;

	/** The human readable name of this external program. */
	protected final String programName;

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
	public ExternalProgramInfo(@Nullable String programName, @Nullable ProgramExecutableType defaultType) {
		this.programName = programName;
		this.defaultType = defaultType;
		this.originalDefaultType = defaultType;
		this.executablesInfo = new HashMap<>();
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
	public ExternalProgramInfo(
		@Nullable String programName,
		@Nullable ProgramExecutableType defaultType,
		@Nullable Map<ProgramExecutableType, ? extends ExecutableInfo> executablesInfo
	) {
		this.programName = programName;
		this.defaultType = defaultType;
		this.originalDefaultType = defaultType;
		if (executablesInfo != null) {
			this.executablesInfo = new HashMap<>(executablesInfo);
		} else {
			this.executablesInfo = new HashMap<>();
		}
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
	protected ExternalProgramInfo(
		@Nullable String programName,
		@Nullable ProgramExecutableType defaultType,
		@Nullable ProgramExecutableType originalDefaultType,
		@Nullable Map<ProgramExecutableType, ? extends ExecutableInfo> executablesInfo
	) {
		this.programName = programName;
		this.defaultType = defaultType;
		this.originalDefaultType = originalDefaultType;
		if (executablesInfo != null) {
			this.executablesInfo = new HashMap<>(executablesInfo);
		} else {
			this.executablesInfo = new HashMap<>();
		}
	}

	/**
	 * @return The lock protecting all mutable class fields of this instance,
	 *         use it for locking before iteration or other situations where no
	 *         concurrent should be allowed between operations.
	 */
	@Nonnull
	public ReentrantReadWriteLock getLock() {
		return lock;
	}

	/**
	 * @return The human readable name of this external program.
	 */
	@Nullable
	public String getName() {
		return programName;
	}

	/**
	 * Returns the {@link ProgramExecutableType} set by the constructor. The
	 * difference between this value and {@link #getDefault()} is that the
	 * latter can be changed with {@link #setDefault}, which is done for example
	 * when a {@link ProgramExecutableType#CUSTOM} is set. if no
	 * {@link ProgramExecutableType#CUSTOM} is set, this and
	 * {@link #getDefault()} will usually be equal. This is used primarily to
	 * set the default back to its original value if the overridden value is
	 * invalidated.
	 *
	 * @return The original default {@link ProgramExecutableType}.
	 */
	@Nullable
	public ProgramExecutableType getOriginalDefault() {
		return originalDefaultType;
	}

	/**
	 * @return The default {@link ProgramExecutableType}.
	 */
	@Nullable
	public ProgramExecutableType getDefault() {
		lock.readLock().lock();
		try {
			return defaultType;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Sets the default {@link ProgramExecutableType}.
	 *
	 * @param defaultType The default value.
	 */
	public void setDefault(@Nullable ProgramExecutableType defaultType) {
		lock.writeLock().lock();
		try {
			this.defaultType = defaultType;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Sets the default {@link ProgramExecutableType} to the original value set
	 * by the constructor.
	 */
	public void setOriginalDefault() {
		lock.writeLock().lock();
		try {
			defaultType = originalDefaultType;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @return The {@link ExecutableInfo} for the default
	 *         {@link ProgramExecutableType}.
	 */
	@Nullable
	public ExecutableInfo getDefaultExecutableInfo() {
		lock.readLock().lock();
		try {
			return defaultType == null ? null : executablesInfo.get(defaultType);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return The {@link Path} for the default {@link ProgramExecutableType}.
	 */
	@Nullable
	public Path getDefaultPath() {
		lock.readLock().lock();
		try {
			if (defaultType == null) {
				return null;
			}
			ExecutableInfo executableInfo = executablesInfo.get(defaultType);
			return executableInfo == null ? null : executableInfo.getPath();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Gets the {@link ExecutableInfo} for the specified
	 * {@link ProgramExecutableType}.
	 *
	 * @param executableType the {@link ProgramExecutableType} to get.
	 * @return The {@link ExecutableInfo} for the default
	 *         {@link ProgramExecutableType}.
	 */
	@Nullable
	public ExecutableInfo getExecutableInfo(@Nullable ProgramExecutableType executableType) {
		if (executableType == null) {
			return null;
		}
		lock.readLock().lock();
		try {
			return executablesInfo.get(executableType);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Sets the {@link ExecutableInfo} for the specified
	 * {@link ProgramExecutableType}.
	 *
	 * @param executableType the {@link ProgramExecutableType} for which to set.
	 * @param executableInfo the {@link ExecutableInfo} to set.
	 */
	public void setExecutableInfo(
		@Nonnull ProgramExecutableType executableType,
		@Nullable ExecutableInfo executableInfo
	) {
		if (executableType == null) {
			throw new IllegalArgumentException("executableType cannot be null");
		}
		lock.writeLock().lock();
		try {
			executablesInfo.put(executableType, executableInfo);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Gets the path for the specified {@link ProgramExecutableType}.
	 *
	 * @param executableType the {@link ProgramExecutableType} to get.
	 * @return The executable {@link Path}.
	 */
	@Nullable
	public Path getPath(@Nullable ProgramExecutableType executableType) {
		lock.readLock().lock();
		try {
			ExecutableInfo executableInfo = executablesInfo.get(executableType);
			return executableInfo == null ? null : executableInfo.getPath();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Sets the {@link Path} for a specified {@link ProgramExecutableType}.
	 * <p>
	 * <b>This resets all metadata for the executable unless {@code path}
	 * {@code equals()} the existing {@link Path}.</b>
	 *
	 * @param executableType the {@link ProgramExecutableType} whose path to
	 *            set.
	 * @param path the executable {@link Path} to set.
	 * @return {@code true} if a change was made, {@code false} otherwise.
	 */
	public boolean setPath(@Nonnull ProgramExecutableType executableType, @Nullable Path path) {
		if (executableType == null) {
			throw new IllegalArgumentException("executableType cannot be null");
		}
		lock.writeLock().lock();
		try {
			ExecutableInfo executableInfo = executablesInfo.get(executableType);
			if (
				(
					path == null &&
					executableInfo == null &&
					executablesInfo.containsKey(executableType)
				) || (
					path != null &&
					executableInfo != null &&
					path.equals(executableInfo.getPath())
				)
			) {
				return false;
			}
			ExecutableInfo newExecutableInfo;
			if (path == null) {
				newExecutableInfo = null;
			} else {
				newExecutableInfo = createExecutableInfo(path);
			}
			executablesInfo.put(executableType, newExecutableInfo);
			if (path == null && executableInfo == null) {
				// No actual change was made to the path, but a null entry for the ProgramExecutableType was inserted
				return false;
			}
		} finally {
			lock.writeLock().unlock();
		}
		return true;
	}

	/**
	 * Removes the specified {@link ProgramExecutableType} executable
	 * {@link Path} if present.
	 *
	 * @param executableType the {@link ProgramExecutableType} to remove.
	 */
	public void remove(@Nullable ProgramExecutableType executableType) {
		if (executableType == null) {
			return;
		}
		lock.writeLock().lock();
		try {
			executablesInfo.remove(executableType);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @return The number of executable paths registered.
	 */
	public int size() {
		lock.readLock().lock();
		try {
			return executablesInfo.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return {@code true} if no executable paths are registered, {@code false}
	 *         otherwise.
	 */
	public boolean isEmpty() {
		lock.readLock().lock();
		try {
			return executablesInfo.isEmpty();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Checks whether an entry for the given {@link ProgramExecutableType} is
	 * registered.
	 *
	 * @param executableType the {@link ProgramExecutableType} to check.
	 * @param includeNull whether or not to return {@code true} if
	 *            {@code executableType} is registered but the corresponding
	 *            {@link ExecutableInfo} is {@code null}.
	 * @return {@code true} if an {@link ExecutableInfo} is registered for
	 *         {@code executableType}, {@code false} otherwise.
	 */
	public boolean containsType(@Nullable ProgramExecutableType executableType, boolean includeNull) {
		if (executableType == null) {
			return false;
		}
		lock.readLock().lock();
		try {
			return
				executablesInfo.containsKey(executableType) &&
				(
					includeNull ||
					executablesInfo.get(executableType) != null
				);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Checks whether the given {@link ExecutableInfo} is registered.
	 *
	 * @param executableInfo the {@link ExecutableInfo} to look for.
	 * @return {@code true} if {@code ExecutableInfo} is registered,
	 *         {@code false} otherwise.
	 */
	public boolean containsExecutableInfo(@Nullable ExecutableInfo executableInfo) {
		if (executableInfo == null) {
			return false;
		}
		lock.readLock().lock();
		try {
			return executablesInfo.containsValue(executableInfo);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Checks whether the given {@link Path} is registered.
	 *
	 * @param path the {@link Path} to look for.
	 * @return {@code true} if {@code path} is registered, {@code false}
	 *         otherwise.
	 */
	public boolean containsPath(@Nullable Path path) {
		if (path == null) {
			return false;
		}
		lock.readLock().lock();
		try {
			for (ExecutableInfo executableInfo : executablesInfo.values()) {
				if (path.equals(executableInfo.getPath())) {
					return true;
				}
			}
			return false;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Removes all registered executable paths.
	 */
	public void clear() {
		lock.writeLock().lock();
		try {
			executablesInfo.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @return A {@link Set} of all registered {@link ProgramExecutableType}s.
	 */
	@Nonnull
	public Set<ProgramExecutableType> getExecutableTypes() {
		lock.readLock().lock();
		try {
			return new HashSet<>(executablesInfo.keySet());
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return An array of all registered {@link ProgramExecutableType}s.
	 */
	@Nonnull
	public ProgramExecutableType[] getExecutableTypesArray() {
		lock.readLock().lock();
		try {
			return executablesInfo.keySet().toArray(new ProgramExecutableType[executablesInfo.size()]);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return A copy of {@code executablesMap} containing all registered
	 *         executable entries.
	 */
	@Nonnull
	public Map<ProgramExecutableType, ExecutableInfo> getExecutablesInfo() {
		lock.readLock().lock();
		try {
			return new HashMap<>(executablesInfo);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Sets the executable available status and a related error text for the
	 * specified {@link ExecutableType}.
	 *
	 * @param available whether or not the executable is available.
	 * @param executableType the {@link ProgramExecutableType} for which to set
	 *            availability.
	 * @param version the {@link Version} of the executable if known or
	 *            {@code null} if unknown.
	 * @param errorType the {@link ExecutableErrorType} if {@code available} is
	 *            {@code false} or {@code null} if {@code available} is
	 *            {@code true}.
	 * @param errorText a localized description of the current error if
	 *            {@code available} is {@code false} or {@code null} if the
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
			throw new IllegalArgumentException("errorType and errorType can only be null if available is true or null");
		}
		if (errorType == ExecutableErrorType.SPECIFIC) {
			throw new IllegalArgumentException("SPECIFIC error type isn't allowed");
		}
		lock.writeLock().lock();
		try {
			ExecutableInfo executableInfo = executablesInfo.get(executableType);
			if (executableInfo == null) {
				throw new IllegalStateException(
					"Cannot set availability for executable type " +
					executableType + " since its path is undefined"
				);
			}
			executableInfo = executableInfo.modify()
				.available(Boolean.valueOf(available)).version(version)
				.errorType(errorType).errorText(errorText).build();
			executablesInfo.put(executableType, executableInfo);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		lock.readLock().lock();
		try {
			if (programName != null) {
				sb.append(" ").append(programName);
			}
			boolean first = true;
			sb.append(": [");
			for (Entry<ProgramExecutableType, ExecutableInfo> entry : executablesInfo.entrySet()) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				if (entry.getKey() == defaultType) {
					sb.append("(*)");
				}
				sb.append(entry.getKey()).append(" = \"").append(entry.getValue()).append("\"");
			}
			sb.append("]");
		} finally {
			lock.readLock().unlock();
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		lock.readLock().lock();
		try {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((defaultType == null) ? 0 : defaultType.hashCode());
			result = prime * result + ((programName == null) ? 0 : programName.hashCode());
			result = prime * result + ((executablesInfo == null) ? 0 : executablesInfo.hashCode());
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ExternalProgramInfo)) {
			return false;
		}
		ExternalProgramInfo other = (ExternalProgramInfo) obj;
		ReentrantReadWriteLock otherLock = other.getLock();
		lock.readLock().lock();
		try {
			otherLock.readLock().lock();
			try {
				if (defaultType == null) {
					if (other.defaultType != null) {
						return false;
					}
				} else if (!defaultType.equals(other.defaultType)) {
					return false;
				}
				if (programName == null) {
					if (other.programName != null) {
						return false;
					}
				} else if (!programName.equals(other.programName)) {
					return false;
				}
				if (executablesInfo == null) {
					if (other.executablesInfo != null) {
						return false;
					}
				} else if (!executablesInfo.equals(other.executablesInfo)) {
					return false;
				}
				return true;
			} finally {
				otherLock.readLock().unlock();
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Creates a new {@link ExecutableInfo} of the correct type. Override to
	 * create custom types.
	 *
	 * @param executablePath The {@link Path} to the executable.
	 * @return The new instance.
	 */
	@Nonnull
	protected ExecutableInfo createExecutableInfo(@Nonnull Path executablePath) {
		return ExecutableInfo.build(executablePath).build();
	}

	/**
	 * Returns a "deep-clone" of this instance where mutable objects are copied.
	 *
	 * @return The new {@link ExternalProgramInfo}.
	 */
	@Nonnull
	public ExternalProgramInfo copy() {
		return new ExternalProgramInfo(programName, defaultType, originalDefaultType, executablesInfo);
	}
}
