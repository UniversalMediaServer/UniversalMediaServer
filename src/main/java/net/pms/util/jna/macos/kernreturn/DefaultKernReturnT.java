/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
package net.pms.util.jna.macos.kernreturn;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import net.pms.util.jna.macos.kernreturn.IOReturn.IOReturnSubsystem;


/**
 * Maps OS X {@code kern_return_t} integer values to their defined names as defined
 * in {@code error.h}.
 * Use {@link DefaultKernReturnT#getValue} to convert an {@link DefaultKernReturnT} to its integer
 * value. Use {@link DefaultKernReturnT#typeOf} to convert an integer value to an
 * {@link DefaultKernReturnT}.
 *
 * @author Nadahar
 */
@SuppressWarnings({"checkstyle:ConstantName", "checkstyle:MethodName"})
public class DefaultKernReturnT implements KernReturnT {

	/** Success / No error / Routine {@code null} */
	public static final DefaultKernReturnT SUCCESS = new DefaultKernReturnT(0);

	/** Alias for {@link #SUCCESS} */
	public static final DefaultKernReturnT none = SUCCESS;

	/** Alias for {@link #SUCCESS} */
	public static final DefaultKernReturnT ROUTINE_NIL = SUCCESS;

	/** Alias for {@link #SUCCESS} */
	public static final DefaultKernReturnT KERN_SUCCESS = SUCCESS;

	/** Alias for {@link #SUCCESS} */
	public static final DefaultKernReturnT kIOReturnSuccess = SUCCESS;

	/** Alias for {@link #SUCCESS} */
	public static final DefaultKernReturnT MACH_MSG_SUCCESS = SUCCESS;

	private static final Object unmappedSubsystemSetLock = new Object();
	private static final HashSet<WeakReference<UnmappedSubsystem>> unmappedSubsystemSet = new HashSet<>();

	private static final Object unmappedKernReturnSetLock = new Object();
	private static final HashSet<WeakReference<DefaultKernReturnT>> unmappedKernReturnSet = new HashSet<>();

	final int code;

	/**
	 * Constructor.
	 *
	 * @param code the {@code kern_return_t} integer value for the new instance.
	 */
	private DefaultKernReturnT(int code) {
		this.code = code;
	}

	@Override
	public int getValue() {
		return code;
	}

	@Override
	public String toString() {
		return toStandardString();
	}

	@Override
	public String toStandardString() {
		if (code == 0) {
			return "Success (0)";
		}
		return "DefaultKernReturnT (0x" +  Long.toHexString(code & 0xFFFFFFFFL) + ")";
	}

	/**
	 * Returns a {@link DefaultKernReturnT} instance representing
	 * {@code kernReturn}. If a {@link DefaultKernReturnT} mapping for this code
	 * already exists, that instance is returned. If not, a new
	 * {@link DefaultKernReturnT} will be created and a {@link WeakReference}
	 * will be held to avoid duplicates. This should make sure only one
	 * {@link DefaultKernReturnT} instance exists for one
	 * {@code kernel_return_t} code.
	 *
	 * @param kernReturn the {@code kernel_return_t} code
	 * @return A {@link DefaultKernReturnT} instance representing the
	 *         {@code kernel_return_t} code.
	 */
	public static DefaultKernReturnT getDefaultKernReturnTFromCode(int kernReturn) {
		DefaultKernReturnT result = null;
		synchronized (unmappedKernReturnSetLock) {
			for (Iterator<WeakReference<DefaultKernReturnT>> iterator = unmappedKernReturnSet.iterator(); iterator.hasNext();) {
				WeakReference<DefaultKernReturnT> weakReference = iterator.next();
				DefaultKernReturnT element = weakReference.get();
				if (element == null) {
					iterator.remove();
				} else if (element.getValue() == kernReturn) {
					result = element;
					// Do not break, complete the iteration to remove all nulled references
				}
			}
			if (result == null) {
				result = new DefaultKernReturnT(kernReturn);
				unmappedKernReturnSet.add(new WeakReference<DefaultKernReturnT>(result));
			}
		}
		return result;
	}

	/**
	 * Represents the macro {@code err_get_system(err)} in {@code error.h}.
	 *
	 * @param kernReturn the {@link KernReturnT} instance.
	 * @return The extracted {@code system} part of a {@link KernReturnT} return
	 *         code.
	 */
	public static int getSystemCode(KernReturnT kernReturn) {
		return getSystemCode(kernReturn.getValue());
	}

	/**
	 * Represents the macro {@code err_get_system(err)} in {@code error.h}.
	 *
	 * @param kernReturn the {@link KernReturnT} return code.
	 * @return The extracted {@code system} part of a {@link KernReturnT} return
	 *         code.
	 */
	public static int getSystemCode(int kernReturn) {
		return (kernReturn >> 26) & 0x3f;
	}

	/**
	 * Translates the {@code system} part of a {@link KernReturnT} return code
	 * into a corresponding {@link Enum} if known. If the code isn't recognized,
	 * {@code null} is returned. Use {@link #getSystemCode} to get the integer
	 * code.
	 *
	 * @param kernReturn the {@link KernReturnT} instance.
	 * @return An {@link Enum} representing the {@code system} part of a
	 *         {@link KernReturnT} return code or {@code null} if the code isn't
	 *         recognized.
	 */
	public static System getSystem(KernReturnT kernReturn) {
		return System.typeOf(getSystemCode(kernReturn.getValue()));
	}

	/**
	 * Represents the macro {@code err_system(x)} in {@code error.h}. Converts a
	 * {@link System} value to a {@link KernReturnT} integer value.
	 *
	 * @param value the {@link System} value to convert.
	 * @return The {@link KernReturnT} value.
	 */
	public static int convertToSystem(System value) {
		return convertToSystem(value.getValue());
	}

	/**
	 * Represents the macro {@code err_system(x)} in {@code error.h}. Converts a
	 * {@link System} integer value to a {@link KernReturnT} integer value.
	 *
	 * @param value the {@link System} integer value to convert.
	 * @return The {@link KernReturnT} value.
	 */
	public static int convertToSystem(int value) {
		return (value & 0x3f) << 26;
	}

	/**
	 * Represents the macro {@code err_sub(x)} in {@code error.h}. Converts a
	 * {@link Subsystem} value to a {@link KernReturnT} integer value.
	 *
	 * @param value the {@link System} value to convert.
	 * @return The {@link KernReturnT} value.
	 */
	public static int convertToSubsystem(Subsystem value) {
		return convertToSubsystem(value.getValue());
	}

	/**
	 * Represents the macro {@code err_sub(x)} in {@code error.h}. Converts a
	 * {@link Subsystem} integer value to a {@link KernReturnT} integer value.
	 *
	 * @param value the {@link System} integer value to convert.
	 * @return The {@link KernReturnT} value.
	 */
	public static int convertToSubsystem(int value) {
		return (value & 0xfff) << 14;
	}

	/**
	 * Represents the macro {@code err_get_sub(err)} in {@code error.h}.
	 *
	 * @param kernReturn the {@link KernReturnT} instance.
	 * @return The extracted {@code subsystem} part of a {@link KernReturnT}
	 *         return code.
	 */
	public static int getSubsystemCode(KernReturnT kernReturn) {
		return getSubsystemCode(kernReturn.getValue());
	}

	/**
	 * Represents the macro {@code err_get_sub(err)} in {@code error.h}.
	 *
	 * @param kernReturn the {@link KernReturnT} return code.
	 * @return The extracted {@code subsystem} part of a {@link KernReturnT}
	 *         return code.
	 */
	public static int getSubsystemCode(int kernReturn) {
		return (kernReturn >> 14) & 0xfff;
	}

	/**
	 * Returns a {@link Subsystem} instance representing the subsystem part of
	 * {@code value}.
	 * <p>
	 * If a {@link Subsystem} mapping for this code already exists, that
	 * {@link Subsystem} instance is returned. If not, a new
	 * {@link UnmappedSubsystem} instance will be created and a
	 * {@link WeakReference} will be held to avoid duplicates. This should make
	 * sure only one {@link UnmappedSubsystem} instance exists for one subsystem
	 * code, but other {@link Subsystem} instances from other classes/
	 * {@link Enum}s with the same value might exist.
	 * <p>
	 * Use {@link #getSubsystemCode} to get the integer code.
	 *
	 * @param kernReturn the {@link KernReturnT} instance.
	 * @return An {@link Subsystem} instance representing the {@code subsystem}
	 *         part of a {@link KernReturnT} return code.
	 */
	public static Subsystem getSubsystem(KernReturnT kernReturn) {
		Subsystem result = null;
		if (kernReturn instanceof IOReturn) {
			result = IOReturnSubsystem.typeOf(getSubsystemCode(kernReturn));
		}

		// This must come after the type specific mappings
		if (result == null) {
			result = DefaultSubsystem.typeOf(getSubsystemCode(kernReturn));
		}
		return result != null ? result : getUnmappedSubsystemFromCode(getSubsystemCode(kernReturn));
	}

	/**
	 * Returns a {@link UnmappedSubsystem} instance representing the subsystem
	 * code {@code value}. If a {@link UnmappedSubsystem} mapping for this code
	 * already exists, that instance is returned. If not, a new
	 * {@link UnmappedSubsystem} will be created and a {@link WeakReference} to
	 * it created to avoid duplicates. This should make sure only one
	 * {@link UnmappedSubsystem} instance exists for one subsystem code.
	 *
	 * @param value the subsystem integer code
	 * @return A {@link UnmappedSubsystem} instance representing the subsystem
	 *         code {@code value}.
	 */
	public static UnmappedSubsystem getUnmappedSubsystemFromCode(int value) {
		UnmappedSubsystem result = null;
		synchronized (unmappedSubsystemSetLock) {
			for (Iterator<WeakReference<UnmappedSubsystem>> iterator = unmappedSubsystemSet.iterator(); iterator.hasNext();) {
				WeakReference<UnmappedSubsystem> weakReference = iterator.next();
				UnmappedSubsystem element = weakReference.get();
				if (element == null) {
					iterator.remove();
				} else if (element.getValue() == value) {
					result = element;
					// Do not break, complete the iteration to remove all nulled references
				}
			}
			if (result == null) {
				result = new UnmappedSubsystem(value);
				unmappedSubsystemSet.add(new WeakReference<UnmappedSubsystem>(result));
			}
		}
		return result;
	}

	/**
	 * Represents the macro {@code err_get_code(err)} in {@code error.h}.
	 *
	 * @param kernReturn the {@link KernReturnT} instance.
	 * @return The extracted {@code error code} part of a {@link KernReturnT}
	 *         return code.
	 */
	public static int getErrorCode(KernReturnT kernReturn) {
		return getErrorCode(kernReturn.getValue());
	}

	/**
	 * Represents the macro {@code err_get_code(err)} in {@code error.h}.
	 *
	 * @param kernReturn the {@link KernReturnT} return code.
	 * @return The extracted {@code error code} part of a {@link KernReturnT}
	 *         return code.
	 */
	public static int getErrorCode(int kernReturn) {
		return kernReturn & 0x3fff;
	}

	/**
	 * Returns a {@link KernReturnT} instance representing the
	 * {@code kern_return_t} integer value {@code kernReturn}.
	 * <p>
	 * If a known mapping for this return code exists, that instance is
	 * returned. If not, a new {@link DefaultKernReturnT} instance will be
	 * created and a {@link WeakReference} to it will be created to avoid
	 * duplicates. This should make sure that only one {@link KernReturnT}
	 * instance exists for any given {@code kern_return_t} integer value.
	 *
	 * @param kernReturn the {@code kern_return_t} integer code.
	 * @return A {@link KernReturnT} instance representing the
	 *         {@code kern_return_t} integer value.
	 */
	public static KernReturnT typeOf(int kernReturn) {
		if (kernReturn == 0) {
			return SUCCESS;
		}

		System system = System.typeOf(getSystemCode(kernReturn));
		KernReturnT result = null;

		// Map to known systems
		if (system == System.sys_iokit) {
			result = IOReturn.typeOf(kernReturn);
		}

		// Generic mapping
		if (result == null) {
			result = Message.typeOf(kernReturn);
		}
		if (result == null) {
			result = KernReturn.typeOf(kernReturn);
		}

		// Create unmapped instance
		if (result == null) {
			result = getDefaultKernReturnTFromCode(kernReturn);
		}
		return result;
	}

	/**
	 * Maps the OS X {@code system} part of a {@link KernReturnT} error to their
	 * integer values.
	 * <p>
	 * Use {@link System#getValue} to convert an {@link System} to its integer
	 * value. Use {@link System#typeOf} to convert an integer value to an
	 * {@link System}.
	 *
	 * @author Nadahar
	 */
	public enum System {

		/** Kernel */
		err_kern(0x0),

		/** User space library */
		err_us(0x1),

		/** User space servers */
		err_server(0x2),

		/** Old ipc errors */
		err_ipc(0x3),

		/** Mach-ipc errors */
		err_mach_ipc(0x4),

		/** Distributed ipc */
		err_dipc(0x7),

		/** IOKit */
		sys_iokit(0x38),

		/** User defined errors */
		err_local(0x3e),

		/** Mach-ipc errors (compatibility) */
		err_ipc_compat(0x3f),

		/** System max */
		err_max_system(0x3f);

		private final int value;

		private System(int value) {
			this.value = value;
		}

		/**
		 * @return The system error code value of this {@link System}.
		 */
		public int getValue() {
			return value;
		}

		/**
		 * @param systemCode the {@code System} integer value.
		 * @return The corresponding {@link System}.
		 */
		public static System typeOf(int systemCode) {
			for (System entry : System.values()) {
				if (entry.getValue() == systemCode) {
					return entry;
				}
			}
			return null;
		}
	}

	/**
	 * Maps the OS X {@code Subsystem} part of a {@link KernReturnT} error to
	 * their integer values.
	 * <p>
	 * Use {@link Subsystem#getValue} to convert an {@link Subsystem} to its
	 * integer value.
	 */
	public interface Subsystem {

		/**
		 * @return The integer subsystem code.
		 */
		public int getValue();
	}

	/**
	 * Maps the OS X {@code Subsystem} part of a {@link KernReturnT} error to
	 * their integer values.
	 * <p>
	 * Use {@link DefaultSubsystem#getValue} to convert an {@link Subsystem} to its
	 * integer value. Use {@link DefaultSubsystem#typeOf} to convert an integer value
	 * to an {@link Subsystem}.
	 */
	public enum DefaultSubsystem implements Subsystem {

		/** No subsystem */
		none(0),

		/** Unix error */
		unix_err(0x3);

		private final int value;

		private DefaultSubsystem(int value) {
			this.value = value;
		}

		/**
		 * @return The subsystem error code value of this {@link Subsystem}.
		 */
		@Override
		public int getValue() {
			return value;
		}

		/**
		 * @param subSystemCode the {@code Subsystem} integer value.
		 * @return The corresponding {@link Subsystem}.
		 */
		public static Subsystem typeOf(int subSystemCode) {
			for (DefaultSubsystem entry : DefaultSubsystem.values()) {
				if (entry.getValue() == subSystemCode) {
					return entry;
				}
			}
			return null;
		}
	}

	/**
	 * Maps the OS X {@code Subsystem} part of a {@link KernReturnT} error to
	 * their integer values.
	 * <p>
	 * Use {@link DefaultSubsystem#getValue} to convert an {@link Subsystem} to its
	 * integer value. Use {@link DefaultSubsystem#typeOf} to convert an integer value
	 * to an {@link Subsystem}.
	 */
	public static class UnmappedSubsystem implements Subsystem {

		private final int value;

		private UnmappedSubsystem(int value) {
			this.value = value;
		}

		@Override
		public int getValue() {
			return value;
		}

	}
}
