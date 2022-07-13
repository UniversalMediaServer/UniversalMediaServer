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
package net.pms.util.jna.macos.iokit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.vdurmont.semver4j.Semver;
import net.pms.util.jna.macos.corefoundation.CoreFoundation;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableDictionaryRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableDictionaryRefByReference;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFNumberRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFNumberType;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFTypeRef;
import net.pms.util.jna.macos.iokit.IOKit.IOPMUserActiveType;
import net.pms.util.jna.macos.kernreturn.DefaultKernReturnT;
import net.pms.util.jna.macos.kernreturn.KernReturnT;
import net.pms.util.jna.macos.types.IOIteratorTRef;
import net.pms.util.jna.macos.types.IORegistryEntryT;

/**
 * This is a utility class for {@link IOKitUtils}.
 * <p>
 * It currently primarily contains methods for handling idle sleep.
 *
 * @author Nadahar
 */
public class IOKitUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(IOKitUtils.class);

	private static IOKit ioKit = IOKit.INSTANCE;
	private static CoreFoundation cf = CoreFoundation.INSTANCE;

	protected static final Semver MAC_OS_VERSION;

	private IOKitUtils() {
	}

	/**
	 * Determines if the current macOS version is of a version equal or greater
	 * to the argument.
	 *
	 * @param version the version to evaluate.
	 * @return whether the current version is at least the specified version.
	 */
	public static boolean isMacOsVersionEqualOrGreater(String version) {
		return MAC_OS_VERSION.isGreaterThanOrEqualTo(version);
	}

	/**
	 * Queries the OS for the current value of the idle timer and returns the
	 * results.
	 *
	 * @return The number of milliseconds since the last user activity.
	 * @throws IOKitException If an error occurred while querying the OS.
	 */
	@SuppressWarnings("null")
	public static long getSystemIdleTimeMS() throws IOKitException {
		IOIteratorTRef iteratorRef = new IOIteratorTRef(true);
		KernReturnT ioReturn = ioKit.IOServiceGetMatchingServices(null, ioKit.IOServiceMatching("IOHIDSystem"), iteratorRef);
		try {
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				IORegistryEntryT entry = IORegistryEntryT.toIORegistryT(ioKit.IOIteratorNext(iteratorRef.getValue()));
				if (entry != null) {
					try {
						CFMutableDictionaryRefByReference dictionaryRef = new CFMutableDictionaryRefByReference();
						ioReturn = ioKit.IORegistryEntryCreateCFProperties(entry, dictionaryRef, CoreFoundation.ALLOCATOR, 0);
						if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
							CFMutableDictionaryRef dictionary = dictionaryRef.getCFMutableDictionaryRef();
							try {
								CFTypeRef cfType = cf.CFDictionaryGetValue(
									dictionaryRef.getCFMutableDictionaryRef(),
									CFStringRef.toCFStringRef("HIDIdleTime")
								);
								if (cfType != null) {
									CFNumberRef cfNumber = new CFNumberRef(cfType);
									LongByReference nanoSeconds = new LongByReference();
									if (cf.CFNumberGetValue(cfNumber, CFNumberType.kCFNumberSInt64Type, nanoSeconds)) {
										return nanoSeconds.getValue() >> 20;
									}
									throw new IOKitException("HIDIdleTime out of range");
								}
								throw new IOKitException("HIDIdleTime not found");
							} finally {
								cf.CFRelease(dictionary);
							}
						}
						throw new IOKitException(
							"IORegistryEntryCreateCFProperties failed with error code: " +
							ioReturn.toStandardString()
						);
					} finally {
						ioKit.IOObjectRelease(entry);
					}
				}
				throw new IOKitException("IOHIDSystem not found");
			}
			throw new IOKitException("IOServiceGetMatchingServices failed with error code: " + ioReturn.toStandardString());
		} finally {
			// Even though Java doesn't understand it, this can be null because IOServiceGetMatchingServices() can return null.
			if (iteratorRef != null) {
				ioKit.IOObjectRelease(iteratorRef.getValue());
			}
		}

	}

	/**
	 * Tells the OS to prevent system sleep until further notice by creating an
	 * assertion.
	 *
	 * @param assertionName the name of the assertion to create.
	 * @param assertionDetails the details of the assertion to create.
	 * @return The assertion id if an assertion is created. This value is needed
	 *         when calling {@link #enableGoToSleep} to cancel the assertion.
	 * @throws IOKitException If an error occurs during the operation.
	 */
	public static int disableGoToSleep(String assertionName, String assertionDetails) throws IOKitException {
		IntByReference assertionIdRef = new IntByReference();
		CFStringRef assertionType = CFStringRef.toCFStringRef(IOKit.kIOPMAssertPreventUserIdleSystemSleep);
		CFStringRef name = CFStringRef.toCFStringRef(assertionName);
		CFStringRef details = CFStringRef.toCFStringRef(assertionDetails);

		if (isMacOsVersionEqualOrGreater("10.7.0")) {
			KernReturnT ioReturn = ioKit.IOPMAssertionCreateWithDescription(
				assertionType,
				name,
				details,
				null,
				null,
				0,
				null,
				assertionIdRef
			);
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				return assertionIdRef.getValue();
			}
			throw new IOKitException("IOPMAssertionCreateWithDescription failed with error code: " + ioReturn.toStandardString());
		} else if (isMacOsVersionEqualOrGreater("10.6.0")) {
			KernReturnT ioReturn = ioKit.IOPMAssertionCreateWithName(assertionType, IOKit.kIOPMAssertionLevelOn, name, assertionIdRef);
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				return assertionIdRef.getValue();
			}
			throw new IOKitException("IOPMAssertionCreateWithName failed with error code: " + ioReturn.toStandardString());
		} else if (isMacOsVersionEqualOrGreater("10.5.0")) {
			@SuppressWarnings("deprecation")
			KernReturnT ioReturn = ioKit.IOPMAssertionCreate(assertionType, IOKit.kIOPMAssertionLevelOn, assertionIdRef);
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				return assertionIdRef.getValue();
			}
			throw new IOKitException("IOPMAssertionCreate failed with error code: " + ioReturn.toStandardString());
		}
		throw new IOKitException(
			"Unable to disable sleep mode; maxOS " +
			System.getProperty("os.version") +
			"doesn't support sleep prevention"
		);
	}

	/**
	 * Allows the OS to sleep by canceling an existing assertion.
	 *
	 * @param assertionId the assertion id for the assertion to cancel. This
	 *            value is returned from {@link #disableGoToSleep}.
	 * @throws IOKitException If an error occurs during the operation.
	 */
	public static void enableGoToSleep(int assertionId) throws IOKitException {
		if (isMacOsVersionEqualOrGreater("10.5.0")) {
			KernReturnT ioReturn = ioKit.IOPMAssertionRelease(assertionId);
			if (ioReturn != DefaultKernReturnT.KERN_SUCCESS) {
				throw new IOKitException("IOPMAssertionRelease failed with error code: " + ioReturn.toStandardString());
			}
		} else {
			throw new IOKitException(
				"Unable to enable sleep mode; macOS " +
				System.getProperty("os.version") +
				"doesn't support sleep prevention"
			);
		}
	}

	/**
	 * Creates an assertion preventing sleep with a timeout value equal to the
	 * idle sleep time. This has the same effect as if a user activity occurred
	 * so that the idle timer was reset.
	 *
	 * @param assertionName the name of the assertion to create.
	 * @param assertionId the assertion id if you want to extend the time of an
	 *            existing assertion or {@code 0} to create a new assertion.
	 * @return The assertionId of the created assertion. This may or may not be
	 *         the same as {@code assertionId}.
	 * @throws IOKitException If an error occurs during the operation.
	 */
	public static int resetIdleTimer(String assertionName, int assertionId) throws IOKitException {
		if (isMacOsVersionEqualOrGreater("10.7.3")) {
			IntByReference assertionIdRef = new IntByReference(assertionId > 0 ? assertionId : 0);
			CFStringRef name = CFStringRef.toCFStringRef(assertionName);
			KernReturnT ioReturn = ioKit.IOPMAssertionDeclareUserActivity(name, IOPMUserActiveType.kIOPMUserActiveLocal, assertionIdRef);
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				return assertionIdRef.getValue();
			}
			throw new IOKitException("IOPMAssertionDeclareUserActivity failed with error code: " + ioReturn.toStandardString());
		}
		LOGGER.warn("Unable to reset sleep timer; not supported by maxOS version {}", System.getProperty("os.version"));
		return -1;
	}

	static {
		int dotCount = 0;
		String ver = System.getProperty("os.version");
		for (int i = 0; i < ver.length(); i++) {
			if (ver.charAt(i) == '.') {
				dotCount++;
			}
		}
		if (dotCount == 1) {
			ver += ".0";
		}
		MAC_OS_VERSION = new Semver(ver);
	}
}
