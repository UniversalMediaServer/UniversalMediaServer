package net.pms.platform.windows;

import javax.annotation.Nonnull;
import com.sun.jna.Pointer;


/**
 * A simple extension of {@link com.sun.jna.platform.win32.Guid.GUID} that
 * overrides {@link #toString()}.
 */
public class GUID extends com.sun.jna.platform.win32.Guid.GUID {

	/**
	 * Instantiates a new {@link GUID}.
	 */
	public GUID() {
		super();
	}

	/**
	 * Instantiates a copy of the specified {@link GUID}.
	 *
	 * @param guid the {@link GUID} to copy.
	 */
	public GUID(@Nonnull GUID guid) {
		super(guid);
	}

	/**
	 * Instantiates a new {@link GUID} from the specified {@link String}.
	 *
	 * @param guid the string representation.
	 */
	public GUID(@Nonnull String guid) {
		super(guid);
	}

	/**
	 * Instantiates a new {@link GUID} from a byte array of 16 bytes.
	 *
	 * @param data the 16 byte array.
	 */
	public GUID(@Nonnull byte[] data) {
		super(data);
	}

	/**
	 * Instantiates a new {@link GUID} from the specified {@link Pointer} - only
	 * use if the memory pointed to is known to contain a valid GUID.
	 *
	 * @param pointer the {@link Pointer} to the existing GUID structure.
	 */
	public GUID(Pointer pointer) {
		super(pointer);
	}

	@Override
	public String toString() {
		return super.toGuidString();
	}
}
