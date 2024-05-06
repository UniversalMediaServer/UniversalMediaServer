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
package net.pms.platform.mac.iokit;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import net.pms.platform.mac.MacUtils;
import net.pms.platform.mac.corefoundation.CoreFoundation;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFMutableDictionaryRef;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFMutableDictionaryRefByReference;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFNumberRef;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFNumberType;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFStringRef;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFTypeRef;
import net.pms.platform.mac.iokit.IOKit.IOPMUserActiveType;
import net.pms.platform.mac.kernreturn.DefaultKernReturnT;
import net.pms.platform.mac.kernreturn.KernReturnT;
import net.pms.platform.mac.types.IOIteratorTRef;
import net.pms.platform.mac.types.IORegistryEntryT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility class for {@link IOKitUtils}.
 * <p>
 * It currently primarily contains methods for handling idle sleep.
 *
 * @author Nadahar
 */
public class IOKitUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(IOKitUtils.class);

	private static final IOKit IOKIT = IOKit.INSTANCE;
	private static final CoreFoundation CF = CoreFoundation.INSTANCE;


	private IOKitUtils() {
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
		KernReturnT ioReturn = IOKIT.IOServiceGetMatchingServices(null, IOKIT.IOServiceMatching("IOHIDSystem"), iteratorRef);
		try {
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				IORegistryEntryT entry = IORegistryEntryT.toIORegistryT(IOKIT.IOIteratorNext(iteratorRef.getValue()));
				if (entry != null) {
					try {
						CFMutableDictionaryRefByReference dictionaryRef = new CFMutableDictionaryRefByReference();
						ioReturn = IOKIT.IORegistryEntryCreateCFProperties(entry, dictionaryRef, CoreFoundation.ALLOCATOR, 0);
						if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
							CFMutableDictionaryRef dictionary = dictionaryRef.getCFMutableDictionaryRef();
							try {
								CFTypeRef cfType = CF.CFDictionaryGetValue(
									dictionaryRef.getCFMutableDictionaryRef(),
									CFStringRef.toCFStringRef("HIDIdleTime")
								);
								if (cfType != null) {
									CFNumberRef cfNumber = new CFNumberRef(cfType);
									LongByReference nanoSeconds = new LongByReference();
									if (CF.CFNumberGetValue(cfNumber, CFNumberType.kCFNumberSInt64Type, nanoSeconds)) {
										return nanoSeconds.getValue() >> 20;
									}
									throw new IOKitException("HIDIdleTime out of range");
								}
								throw new IOKitException("HIDIdleTime not found");
							} finally {
								CF.CFRelease(dictionary);
							}
						}
						throw new IOKitException(
							"IORegistryEntryCreateCFProperties failed with error code: " +
							ioReturn.toStandardString()
						);
					} finally {
						IOKIT.IOObjectRelease(entry);
					}
				}
				throw new IOKitException("IOHIDSystem not found");
			}
			throw new IOKitException("IOServiceGetMatchingServices failed with error code: " + ioReturn.toStandardString());
		} finally {
			// Even though Java doesn't understand it, this can be null because IOServiceGetMatchingServices() can return null.
			if (iteratorRef != null) {
				IOKIT.IOObjectRelease(iteratorRef.getValue());
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

		if (MacUtils.isMacOsVersionEqualOrGreater("10.7.0")) {
			KernReturnT ioReturn = IOKIT.IOPMAssertionCreateWithDescription(
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
		} else if (MacUtils.isMacOsVersionEqualOrGreater("10.6.0")) {
			KernReturnT ioReturn = IOKIT.IOPMAssertionCreateWithName(assertionType, IOKit.kIOPMAssertionLevelOn, name, assertionIdRef);
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				return assertionIdRef.getValue();
			}
			throw new IOKitException("IOPMAssertionCreateWithName failed with error code: " + ioReturn.toStandardString());
		} else if (MacUtils.isMacOsVersionEqualOrGreater("10.5.0")) {
			@SuppressWarnings("deprecation")
			KernReturnT ioReturn = IOKIT.IOPMAssertionCreate(assertionType, IOKit.kIOPMAssertionLevelOn, assertionIdRef);
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
		if (MacUtils.isMacOsVersionEqualOrGreater("10.5.0")) {
			KernReturnT ioReturn = IOKIT.IOPMAssertionRelease(assertionId);
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
		if (MacUtils.isMacOsVersionEqualOrGreater("10.7.3")) {
			IntByReference assertionIdRef = new IntByReference(assertionId > 0 ? assertionId : 0);
			CFStringRef name = CFStringRef.toCFStringRef(assertionName);
			KernReturnT ioReturn = IOKIT.IOPMAssertionDeclareUserActivity(name, IOPMUserActiveType.kIOPMUserActiveLocal, assertionIdRef);
			if (ioReturn == DefaultKernReturnT.KERN_SUCCESS) {
				return assertionIdRef.getValue();
			}
			throw new IOKitException("IOPMAssertionDeclareUserActivity failed with error code: " + ioReturn.toStandardString());
		}
		LOGGER.warn("Unable to reset sleep timer; not supported by maxOS version {}", System.getProperty("os.version"));
		return -1;
	}

}
