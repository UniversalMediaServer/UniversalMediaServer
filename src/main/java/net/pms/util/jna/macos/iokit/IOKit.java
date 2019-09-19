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
package net.pms.util.jna.macos.iokit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.pms.util.jna.JnaIntEnum;
import net.pms.util.jna.JnaIntEnumConverter;
import net.pms.util.jna.JnaLongEnum;
import net.pms.util.jna.JnaLongEnumConverter;
import net.pms.util.jna.macos.corefoundation.CoreFoundation;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFAllocatorRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFDictionaryRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFDictionaryRefByReference;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableDictionaryRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableDictionaryRefByReference;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFTypeRef;
import net.pms.util.jna.macos.kernreturn.DefaultKernReturnT;
import net.pms.util.jna.macos.kernreturn.IOReturn;
import net.pms.util.jna.macos.kernreturn.KernReturnT;
import net.pms.util.jna.macos.kernreturn.KernReturnTConverter;
import net.pms.util.jna.macos.types.IOConnectT;
import net.pms.util.jna.macos.types.IOConnectTRef;
import net.pms.util.jna.macos.types.IOIteratorT;
import net.pms.util.jna.macos.types.IOIteratorTRef;
import net.pms.util.jna.macos.types.IONameT;
import net.pms.util.jna.macos.types.IOObjectT;
import net.pms.util.jna.macos.types.IORegistryEntryT;
import net.pms.util.jna.macos.types.IORegistryEntryTRef;
import net.pms.util.jna.macos.types.IOServiceT;
import net.pms.util.jna.macos.types.IOServiceTRef;
import net.pms.util.jna.macos.types.IOStringT;
import net.pms.util.jna.macos.types.MachPortT;
import net.pms.util.jna.macos.types.MachPortTRef;
import net.pms.util.jna.macos.types.TaskPortT;
import com.sun.jna.DefaultTypeMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;


/**
 * Partial mapping of IOKit:
 * <ul>
 *   <li>Power Assertions from {@code IOKit/pwr_mgt/IOPMLib.h}</li>
 *   <li>Some constants from {@code IOKit/pwr_mgt/IOPM.h}</li>
 *   <li>Some functions from {@code IOKitLib.h}</li>
 * </ul>
 *
 * @author Nadahar
 */
@SuppressWarnings({
	"checkstyle:ConstantName",
	"checkstyle:MethodName",
	"checkstyle:ParameterName",
	"checkstyle:JavadocVariable"
})
public interface IOKit extends Library {
	/**
	 * The options to pass to {@link Native#loadLibrary}.
	 */
	Map<String, Object> OPTIONS = Collections.unmodifiableMap(new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new DefaultTypeMapper() {
				{
					addTypeConverter(JnaIntEnum.class, new JnaIntEnumConverter());
					addTypeConverter(JnaLongEnum.class, new JnaLongEnumConverter());
					addTypeConverter(KernReturnT.class, new KernReturnTConverter());
				}
			});
		}
	});
	/**
	 * The instance of this interface.
	 */
	IOKit INSTANCE = (IOKit) Native.loadLibrary("IOKit", IOKit.class, OPTIONS);

	/*
     * Power Assertions from {@code IOKit/pwr_mgt/IOPMLib.h}
     */

	/**
	 * Prevents the system from sleeping automatically due to a lack of user
	 * activity.
	 * <p>
	 * When asserted and set to level {@link #kIOPMAssertionLevelOn}, will
	 * prevent the system from sleeping due to a period of idle user activity.
	 * Create this assertion with {@link #IOPMAssertionCreateWithName} or
	 * {@link #IOPMAssertionCreateWithDescription}.
	 * <p>
	 * The display may dim and idle sleep while
	 * {@link #kIOPMAssertPreventUserIdleSystemSleep} is enabled, but the system
	 * may not idle sleep. The system may still sleep for lid close, Apple menu,
	 * low battery, or other sleep reasons.
	 * <p>
	 * This assertion has no effect if the system is in Dark Wake.
	 */
	String kIOPMAssertPreventUserIdleSystemSleep = "PreventUserIdleSystemSleep";

	/**
	 * Prevents the display from dimming automatically.
	 * <p>
	 * Create this assertion with {@link #IOPMAssertionCreateWithName} or
	 * {@link #IOPMAssertionCreateWithDescription}.
	 * <p>
	 * When asserted and set to level {@link #kIOPMAssertionLevelOn}, will
	 * prevent the display from turning off due to a period of idle user
	 * activity. Note that the display may still sleep for other reasons, like a
	 * user closing a portable's lid or the machine sleeping. If the display is
	 * already off, this assertion does not light up the display. If display
	 * needs to be turned on, then consider calling function
	 * {@link #IOPMAssertionDeclareUserActivity}.
	 * <p>
	 * While the display is prevented from dimming, the system cannot go into
	 * idle sleep.
	 * <p>
	 * This assertion has no effect if the system is in Dark Wake.
	 */
	String kIOPMAssertPreventUserIdleDisplaySleep = "PreventUserIdleDisplaySleep";

	/**
	 * Prevent attached disks from idling into lower power states.
	 * <p>
	 * When asserted and set to level {@link #kIOPMAssertionLevelOn}, will
	 * prevent attached disks and optical media from idling into lower power
	 * states. Create this assertion with {@link #IOPMAssertionCreateWithName}
	 * or {@link #IOPMAssertionCreateWithDescription}.
	 * <p>
	 * Applications who rely on real-time access to disks should create this
	 * assertion to avoid latencies caused by disks changing power states. For
	 * example, audio and video performance or recording applications may
	 * benefit from this assertion. Most applications should not take this
	 * assertion; preventing disk idle consumes battery life, and most
	 * applications don't require the low latency disk access that this
	 * provides.
	 * <p>
	 * This assertion doesn't increase a disk's power state (it just prevents
	 * that device from idling). After creating this assertion, the caller
	 * should perform disk I/O on the necessary drives to ensure that they're in
	 * a usable power state.
	 * <p>
	 * The system may still sleep while this assertion is active. Callers should
	 * also take {@link #kIOPMAssertPreventUserIdleSystemSleep} if necessary, to
	 * prevent idle system sleep.
	 */
	String kIOPMAssertPreventDiskIdle = "PreventDiskIdle";

	/**
	 * Keeps the system awake while OS X serves active network clients.
	 *
	 * When asserted and set to level {@link #kIOPMAssertionLevelOn}, will keep
	 * the computer awake. Create this assertion with
	 * {@link #IOPMAssertionCreateWithName} or
	 * {@link #IOPMAssertionCreateWithDescription}.
	 * <p>
	 * Instead of taking this assertion, most callers should instead use:
	 * {@link #IOPMDeclareNetworkClientActivity} it takes the assertion, but
	 * with a built-in timeout.
	 * <p>
	 * This assertion keeps the system awake in dark or full wake, as long as
	 * the system is on AC power. On battery, this assertion can prevent system
	 * from going into idle sleep. IOKit power assertions are suggestions and OS
	 * X may not honor them under battery, thermal, or user circumstances.
	 * <p>
	 * This assertion provides CPU, disk, and network connectivity. If the
	 * network is no longer available, this assertion may stop working and allow
	 * the system to go to sleep.
	 * <p>
	 * Callers should take this assertion when they have remote clients
	 * connected and active. Please {@link #IOPMAssertionRelease} this assertion
	 * if remote clients become inactive, idle, or disconnected. If your process
	 * already manages user timeouts and tracks activity, you can take this
	 * assertion directly with {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * IOKit can manage remote client idleness for you if you call
	 * {@link #IOPMDeclareNetworkClientActivity} upon every remote access.
	 * <p>
	 * This assertion is a suggestion; Mac OS X may need to sleep the system
	 * even if this assertion is active.
	 */
	String kIOPMAssertNetworkClientActive = "NetworkClientActive";

	/** This value represents a non-initialized assertion ID */
	int kIOPMNullAssertionID = 0;

	/** Level for a disabled assertion */
	int kIOPMAssertionLevelOff = 0;

	/** Level for an enabled assertion */
	int kIOPMAssertionLevelOn = 255;

	/**
	 * Creates an IOPMAssertion.
	 * <p>
	 * It allows the caller to specify the Name, Details, and
	 * HumanReadableReason at creation time. There are other keys that can
	 * further describe an assertion, but most developers don't need to use
	 * them. Use {@link #IOPMAssertionSetProperty} or
	 * {@link #IOPMAssertionCreateWithProperties} if you need to specify
	 * properties that aren't available here.
	 *
	 * @param assertionType An assertion type constant. Caller must specify this
	 *            argument.
	 *
	 * @param name A {@code CFString} value to correspond to key
	 *            {@link #kIOPMAssertionNameKey}. Caller must specify this
	 *            argument.
	 *
	 * @param details A {@code CFString} value to correspond to key
	 *            {@link #kIOPMAssertionDetailsKey}. Caller may pass
	 *            {@code null}, but it helps power users and administrators
	 *            identify the reasons for this assertion.
	 *
	 * @param humanReadableReason A {@code CFString} value to correspond to key
	 *            {@link #kIOPMAssertionHumanReadableReasonKey}. Caller may pass
	 *            {@code null}, but if it's specified OS X may display it to
	 *            users to describe the active assertions on their system.
	 *
	 * @param localizationBundlePath A {@code CFString} value to correspond to
	 *            key {@link #kIOPMAssertionLocalizationBundlePathKey} . This
	 *            bundle path should include a localization for the string
	 *            {@code HumanReadableReason}. The caller may pass {@code null},
	 *            but this argument is required if the caller specifies
	 *            {@code HumanReadableReason}.
	 *
	 * @param timeout Specifies a timeout for this assertion in seconds. Pass 0
	 *            for no timeout.
	 *
	 * @param timeoutAction Specifies a timeout action. Caller my pass
	 *            {@code null}. If a timeout is specified but a
	 *            {@code timeoutAction} is not, the default timeout action is
	 *            {@link #kIOPMAssertionTimeoutActionTurnOff}.
	 *
	 * @param assertionID (Output) On successful return, contains a unique
	 *            reference to a PM assertion.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.7
	 */
	KernReturnT IOPMAssertionCreateWithDescription(CFStringRef assertionType, CFStringRef name, CFStringRef details,
		CFStringRef humanReadableReason, CFStringRef localizationBundlePath, double timeout,
		CFStringRef timeoutAction, IntByReference assertionID
	);

	/**
	 * Creates an IOPMAssertion with more flexibility than
	 * {@link #IOPMAssertionCreateWithDescription}
	 * .
	 *
	 * Create a new PM assertion - the caller must specify the type of
	 * assertion, initial level, and its properties as
	 * {@code IOPMAssertionDictionaryKeys} keys in the
	 * {@code assertionProperties} dictionary. The following keys are recommend
	 * and/or required to be specified in the {@code assertionProperties}
	 * dictionary argument.
	 * <ul>
	 *   <li>REQUIRED: {@link #kIOPMAssertionTypeKey} define the assertion type.
	 *   <li>REQUIRED: {@link #kIOPMAssertionNameKey} Caller must describe the
	 *   name for the activity that requires the change in behavior provided by
	 *   the assertion.
	 *   <li>OPTIONAL: {@link #kIOPMAssertionLevelKey} define an initial value. If
	 *   not set, assertion is turned on after creation.
	 *   <li>OPTIONAL: {@link #kIOPMAssertionDetailsKey} Caller may describe
	 *   context-specific data about the assertion.
	 *   <li>OPTIONAL: {@link #kIOPMAssertionHumanReadableReasonKey} Caller may
	 *   describe the reason for creating the assertion in a localizable
	 *   {@code CFString}. This should be a human readable phrase that describes
	 *   the actions the calling process is taking while the assertion is held,
	 *   like "Downloading TV episodes", or "Compiling Projects"
	 *   <li>OPTIONAL: {@link #kIOPMAssertionLocalizationBundlePathKey} Caller may
	 *   provide its bundle's path, where OS X can localize for GUI display the
	 *   {@code CFString} specified by
	 *   {@link #kIOPMAssertionHumanReadableReasonKey}.
	 *   <li>OPTIONAL: {@code kIOPMAssertionPlugInIDKey} if the caller is a plugin
	 *   with a different identity than the process it's loaded in.
	 *   <li>OPTIONAL: {@code kIOPMAssertionFrameworkIDKey} if the caller is a
	 *   framework acting on behalf of a process.
	 *   <li>OPTIONAL: The caller may specify a timeout in seconds.
	 * </ul>
	 *
	 * @param assertionProperties Dictionary providing the properties of the
	 *            assertion that need to be created.
	 * @param assertionID (Output) On successful return, contains a unique
	 *            reference to a PM assertion.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.7
	 */
	KernReturnT IOPMAssertionCreateWithProperties(
		CFDictionaryRef assertionProperties,
		IntByReference assertionID
	);

	/**
	 * Declares that the user is active on the system.
	 * <p>
	 * This causes the display to power on and postpone display sleep, up to the
	 * user's display sleep Energy Saver settings.
	 * <p>
	 * If you need to hold the display awake for a longer period and you know
	 * how long you'd like to hold it, consider taking assertion
	 * {@link #kIOPMAssertPreventUserIdleDisplaySleep} using
	 * {@link #IOPMAssertionCreateWithDescription} API instead.
	 * <p>
	 * No special privileges are necessary to make this call - any process may
	 * call this API. Caller must specify an {@code assertionName} -
	 * {@code null} is not a valid input.
	 *
	 * @param assertionName A string that describes the name of the caller and
	 *            the activity being handled by this assertion (e.g. "Mail
	 *            Compacting Mailboxes"). Name may be no longer than 128
	 *            characters.
	 *
	 * @param userType This parameter specifies if the active user is located
	 *            locally in front of the system or connected to the system over
	 *            the network. Various components of the system are maintained
	 *            at different power levels depending on user location. Possible
	 *            values: {@link #kIOPMUserActiveLocal},
	 *            {@link #kIOPMUserActiveRemote}.
	 *
	 * @param assertionID On Success, unique id will be returned in this
	 *            parameter. Caller may call this function again with the unique
	 *            id returned previously to report continuous user activity. The
	 *            unique id returned by this function may change on each call
	 *            depending on how frequently this function call is repeated and
	 *            the current display sleep timer value. If you make this call
	 *            more than once, track the returned value for assertionID, and
	 *            pass it in as an argument on each call.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.7.3
	 */
	KernReturnT IOPMAssertionDeclareUserActivity(
		CFStringRef assertionName,
		IOPMUserActiveType userType,
		IntByReference assertionID
	);

	/**
	 * A convenience function for handling remote network clients; this is a
	 * wrapper for holding {@link #kIOPMAssertNetworkClientActive}.
	 * <p>
	 * Call this whenever you detect activity from your remote network clients.
	 * This call generates an IPC call, and may block.
	 * <p>
	 * On the first invocation, this will populate parameter {@code assertionID}
	 * with a new assertion ID. You should pass in this returned assertion ID on
	 * every access.
	 * <p>
	 * When system is on AC power, every call to
	 * {@link #IOPMDeclareNetworkClientActivity} prevents system from idle
	 * sleeping and from demand sleeping for the duration of system sleep timer.
	 * When system is on Battery power, every call to
	 * {@link #IOPMDeclareNetworkClientActivity} prevents system from idle
	 * sleeping for the duration of system sleep timer.
	 * <p>
	 * Assertion created by this interface is valid only for the duration of
	 * system sleep timer from the last call. IOKit will disable
	 * {@code assertionID} after that duration.
	 * <p>
	 * If you detect that your remote client is no longer active, please
	 * immediately call {@link #IOPMAssertionRelease}. Do not wait for the
	 * timeout.
	 * <p>
	 * If your process can detect when remote clients are active and idle, you
	 * can skip this API and directly create
	 * {@link #kIOPMAssertNetworkClientActive} yourself.
	 * <p>
	 * If your remote clients require access to the framebuffer or the GPU, then
	 * this isn't the appropriate call for you. Please see
	 * {@link #IOPMAssertionDeclareUserActivity} and pass in argument
	 * {@link #kIOPMUserActiveRemote}.
	 *
	 * @param assertionName A string that describes the name of the caller and
	 *            the activity being handled by this assertion (e.g.
	 *            "Serving a podcast"). The name must be less than 128
	 *            characters.
	 * @param assertionID (Output) On Success, an unique id will be returned in
	 *            this parameter. The caller may call this function again with
	 *            the unique id returned previously to report additional user
	 *            activity. The unique id returned by this function may change
	 *            on each call depending on how frequently this function call is
	 *            repeated and the current system sleep timer value. If you make
	 *            this call more than once, track the returned value for
	 *            assertionID, and pass it in as an argument on each call.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.9
	 */
	KernReturnT IOPMDeclareNetworkClientActivity(
		CFStringRef assertionName,
		IntByReference assertionID
	);

	/**
	 * Increments the assertion's retain count.
	 * <p>
	 * Increments the retain count according to {@link CoreFoundation} style
	 * retain/release semantics. Retain count can be inspected in the
	 * assertion's info dictionary at key {@link #kIOPMAssertionRetainCountKey}.
	 *
	 * @param theAssertion The assertion ID to retain.
	 *
	 * @since OS X 10.7
	 */
	void IOPMAssertionRetain(int theAssertion);

	/**
	 * Decrements the assertion's retain count.
	 * <p>
	 * If the retain count becomes zero, then this also frees and deactivates
	 * the assertion referred to by <code>assertionID</code>
	 * <p>
	 * Calls to {@link #IOPMAssertionCreateWithDescription},
	 * {@link #IOPMAssertionCreateWithName},
	 * {@link #IOPMAssertionCreateWithProperties} or
	 * {@link #IOPMAssertionRetain} must each be paired with calls to
	 * {@link #IOPMAssertionRelease}.
	 *
	 * @param assertionID The assertion_id, returned from
	 *            {@link #IOPMAssertionCreateWithDescription},
	 *            {@link #IOPMAssertionCreateWithName} or
	 *            {@link #IOPMAssertionCreateWithProperties}, to cancel.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOPMAssertionRelease(int assertionID);

	/**
	 * Copies details about an {@code IOPMAssertion}. Returns a dictionary
	 * describing an {@code IOPMAssertion}'s specifications and current state.
	 *
	 * @param theAssertion The assertion ID to copy info about.
	 * @return A dictionary describing the assertion with keys specified as an
	 *         {@code IOPMAssertionDictionaryKey}. It's the caller's
	 *         responsibility to release this dictionary.
	 * @since OS X 10.7
	 */
	CFDictionaryRef IOPMAssertionCopyProperties(int theAssertion);

	/**
	 * Sets a property in the assertion.
	 * <p>
	 * Only the process that created an assertion may change its properties.
	 *
	 * @param theAssertion The {@code IOPMAssertionID} of the assertion to
	 *            modify.
	 * @param theProperty The {@code CFString} key, from
	 *            {@code IOPMAssertionDictionaryKeys} to modify.
	 * @param theValue The property to set. The value must match the
	 *            {@code CF type} expected for the specified key.
	 * @return An {@link IOReturn} return code. Returns:
	 *         <ul>
	 *           <li>{@link IOReturn#kIOReturnNotPrivileged} if the caller
	 *           doesn't have permission to modify this assertion.</li>
	 *           <li>{@link IOReturn#kIOReturnNotFound} if PM can't locate this
	 *           assertion.</li>
	 *           <li>{@link IOReturn#kIOReturnError} upon an unidentified
	 *           error.</li>
	 *           <li>{@link IOReturn#kIOReturnSuccess} otherwise.</li>
	 *         </ul>
	 */
	KernReturnT IOPMAssertionSetProperty(
		int theAssertion,
		CFStringRef theProperty,
		CFTypeRef theValue
	);

	/**
	 * Returns a dictionary listing all assertions, grouped by their owning
	 * process.
	 * <p>
	 * Notes: One process may have multiple assertions. Several processes may
	 * have asserted the same assertion to different levels.
	 *
	 * @param assertionsByPID (Output) On success, this returns a dictionary of
	 *            assertions per process. At the top level, keys to the
	 *            {@code CFDictionary} are pids stored as {@code CFNumbers}
	 *            (integer). The value associated with each {@code CFNumber} pid
	 *            is a {@code CFArray} of active assertions. Each entry in the
	 *            {@code CFArray} is an assertion represented as a
	 *            {@code CFDictionary}. See the keys
	 *            {@link #kIOPMAssertionTypeKey} and
	 *            {@link #kIOPMAssertionLevelKey}. Caller must
	 *            {@link CoreFoundation#CFRelease} this dictionary when done.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOPMCopyAssertionsByProcess(CFDictionaryRefByReference assertionsByPID);

	/**
	 * Returns a list of available assertions and their system-wide levels.
	 * <p>
	 * The system-wide level is the maximum of all individual assertions'
	 * levels.
	 *
	 * @param assertionsStatus (Output) On success, this returns a
	 *            {@code CFDictionary} of all assertions currently available.
	 *            The keys in the dictionary are the assertion types, and the
	 *            value of each is a {@code CFNumber} that represents the
	 *            aggregate level for that assertion. Caller must
	 *            {@link CoreFoundation#CFRelease} this dictionary when done.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOPMCopyAssertionsStatus(CFDictionaryRefByReference assertionsStatus);

	/**
	 * This is a deprecated call to create a power assertion.
	 * <p>
	 * No special privileges necessary to make this call - any process may
	 * activate a power assertion.
	 *
	 * @param assertionType The {@code CFString} assertion type to request from
	 *            the PM system.
	 * @param assertionLevel Pass {@link #kIOPMAssertionLevelOn} or
	 *            {@link #kIOPMAssertionLevelOff}.
	 * @param assertionID (Output) On success, a unique id will be returned in
	 *            this parameter.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.5
	 *
	 * @deprecated IOPMAssertionCreate is deprecated in favor of
	 *             {@link #IOPMAssertionCreateWithProperties}. Please use that
	 *             version of this API instead.
	 */
	@Deprecated
	KernReturnT IOPMAssertionCreate(
		CFStringRef assertionType,
		int assertionLevel,
		IntByReference assertionID
	);

	/**
	 * The simplest API to create a power assertion.
	 * <p>
	 * No special privileges are necessary to make this call - any process may
	 * activate a power assertion. Caller must specify an {@code assertionName}
	 * - {@code null} is not a valid input.
	 *
	 * @param assertionType The {@code CFString} assertion type to request from
	 *            the PM system.
	 * @param assertionLevel Pass {@link #kIOPMAssertionLevelOn} or
	 *            {@link #kIOPMAssertionLevelOff}.
	 * @param assertionName A string that describes the name of the caller and
	 *            the activity being handled by this assertion (e.g. "Mail
	 *            Compacting Mailboxes"). Name may be no longer than 128
	 *            characters.
	 *
	 * @param assertionID (Output) On success, a unique id will be returned in
	 *            this parameter.
	 *
	 * @return An {@link IOReturn} return code.
	 *
	 * @since OS X 10.6
	 */
	KernReturnT IOPMAssertionCreateWithName(
		CFStringRef assertionType,
		int assertionLevel,
		CFStringRef assertionName,
		IntByReference assertionID
	);

	/**
	 * Specifies an outer bound, in seconds, that this assertion should be
	 * asserted.
	 * <p>
	 * If your application hangs, or is unable to complete its assertion task in
	 * a reasonable amount of time, specifying a timeout allows PM to disable
	 * your assertion so the system can resume normal activity. Once a timeout
	 * with the {@link #kIOPMAssertionTimeoutActionTurnOff} assertion fires, the
	 * level will be set to {@link #kIOPMAssertionTimeoutActionTurnOff}. The
	 * assertion may be re-armed by calling {@link #IOPMAssertionSetProperty}
	 * and setting a new value for for {@link #kIOPMAssertionTimeoutKey}.
	 * <p>
	 * This key may be specified in the dictionary passed to
	 * {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * This key may be present in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 */
	String kIOPMAssertionTimeoutKey = "TimeoutSeconds";

	/**
	 * Specifies the action to take upon timeout expiration.
	 * <p>
	 * Specifying the timeout action only has meaning if you also specify an
	 * {@link #kIOPMAssertionTimeoutKey}. If the caller does not specify a
	 * timeout action, the default action is
	 * {@link #kIOPMAssertionTimeoutActionTurnOff}.
	 * <p>
	 * This key may be specified in the dictionary passed to
	 * {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * This key may be present in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 */
	String kIOPMAssertionTimeoutActionKey = "TimeoutAction";

	/**
	 * A potential value for {@link #kIOPMAssertionTimeoutActionKey}.
	 *
	 * When this timeout action is specified, PM will log the timeout event but
	 * will not turn off or affect the setting of the assertion in any way.
	 */
	String kIOPMAssertionTimeoutActionLog = "TimeoutActionLog";

	/**
	 * When a timeout expires with this action, Power Management will log the
	 * timeout event, and will set the assertion's level to
	 * {@link #kIOPMAssertionLevelOff}.
	 */
	String kIOPMAssertionTimeoutActionTurnOff = "TimeoutActionTurnOff";

	/**
	 * When a timeout expires with this action, Power Management will log the
	 * timeout event, and will release the assertion.
	 */
	String kIOPMAssertionTimeoutActionRelease = "TimeoutActionRelease";

	/**
	 * Reflects the {@link CoreFoundation}-style retain count on this assertion.
	 * Creating or retaining an assertion increments its retain count. Release
	 * an assertion decrements its retain count. When the retain count
	 * decrements to zero, the OS will destroy the object.
	 * <p>
	 * This key can be found in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 */
	String kIOPMAssertionRetainCountKey = "RetainCount";

	/**
	 * The {@code CFDictionary} key for assertion name. Setting this key is
	 * required when you're creating an assertion.
	 * <p>
	 * Describes the the activity the assertion is protecting. The creator
	 * should specify a {@code CFString} value for this key in the dictionary
	 * passed to
	 * {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * The assertion name is separate from the assertion type's behavior -
	 * specify a {@code CFString} like "Checking mail" or "Compiling" that
	 * describes the task that this assertion protects.
	 * <p>
	 * The {@code CFString} you associate with this key does not have to be
	 * localizable (OS X will not attempt to localize it.)
	 * <p>
	 * Describe your assertion as thoroughly as possible. See these other keys
	 * that can you can also set to add explanation to an assertion:
	 * <ul>
	 * <li>OPTIONAL {@link #kIOPMAssertionDetailsKey}
	 * <li>OPTIONAL {@link #kIOPMAssertionHumanReadableReasonKey}
	 * <li>OPTIONAL {@link #kIOPMAssertionLocalizationBundlePathKey}
	 * </ul>
	 */
	String kIOPMAssertionNameKey = "AssertName";

	/**
	 * You may provide extra, contextual information about an assertion for
	 * admins and for debugging in this key. Setting this key in an assertion
	 * dictionary is optional.
	 * <p>
	 * Please name your assertion something unique with
	 * {@link #kIOPMAssertionNameKey} first. If you have more data to describe
	 * this assertion, put it here as a {@code CFString}.
	 * <p>
	 * EXAMPLE: OS X creates an assertion named
	 * {@code com.apple.powermanagement.tty} to prevent sleep for remote-logged
	 * in users. To identify the cause for these assertions, OS X sets
	 * {@link #kIOPMAssertionDetailsKey} to the {@code CFString} device path of
	 * the active remote session(s), e.g. "/dev/ttys000 /dev/ttys004"
	 * <p>
	 * The {@code CFString} you associate with this key does not have to be
	 * localizable (OS X will not attempt to localize it.)
	 *
	 * Describe your assertion as thoroughly as possible. See these other keys
	 * that can you can set to add explanation to an assertion:
	 * <ul>
	 *   <li>REQUIRED {@link #kIOPMAssertionNameKey}
	 *   <li>OPTIONAL {@link #kIOPMAssertionHumanReadableReasonKey}
	 *   <li>OPTIONAL {@link #kIOPMAssertionLocalizationBundlePathKey}
	 * </ul>
	 */
	String kIOPMAssertionDetailsKey = "Details";

	/**
	 *
	 * Optional key that provides a localizable string for OS X to display PM
	 * Assertions in the GUI.
	 * <p>
	 * The caller should specify this string in
	 * {@link #IOPMAssertionCreateWithProperties}. If present, OS X may display
	 * this string, localized to the user's language, to explain changes in
	 * system behavior caused by the assertion.
	 * <p>
	 * If set, the caller must also specify a bundle path for the key
	 * {@link #kIOPMAssertionLocalizationBundlePathKey}. The bundle at that path
	 * should contain localization info for the specified string.
	 * <p>
	 * This key may be specified in the dictionary passed to
	 * {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * This key may be present in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 * <p>
	 * Describe your assertion as thoroughly as possible. See these other keys
	 * that can you can set to add explanation to an assertion:
	 * <ul>
	 *   <li>REQUIRED {@link #kIOPMAssertionNameKey}
	 *   <li>OPTIONAL {@link #kIOPMAssertionDetailsKey}
	 * </ul>
	 */
	String kIOPMAssertionHumanReadableReasonKey = "HumanReadableReason";

	/**
	 * Refers to a {@code CFURL}, as a {@code CFString}, identifying the path to
	 * the caller's bundle, which contains localization info.
	 * <p>
	 * The bundle must contain localizations for
	 * {@link #kIOPMAssertionHumanReadableReasonKey}.
	 * <p>
	 * This key may be specified in the dictionary passed to
	 * {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * This key may be present in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 */
	String kIOPMAssertionLocalizationBundlePathKey = "BundlePath";

	/**
	 * Specify if the assertion creator is a framework.
	 * <p>
	 * If the code that creates the assertion resides in a framework or library,
	 * the caller should specify a {@code CFBundleIdentifier}, as a
	 * {@code CFString}, identifying that bundle here. This info helps
	 * developers and administrators determine the source of an assertion.
	 * <p>
	 * This key may be specified in the dictionary passed to
	 * {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * This key may be present in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 */
	String kIOPMAssertionFrameworkIDKey = "FrameworkBundleID";

	/**
	 * Specify if the assertion creator is a plugin.
	 * <p>
	 * If the code that creates the assertion resides in a plugin, the caller
	 * should specify a {@code CFBundleIdentifier}, as a {@code CFString},
	 * identifying the plugin's bundle here. This info helps developers and
	 * administrators determine the source of an assertion.
	 * <p>
	 * This key may be specified in the dictionary passed to
	 * {@link #IOPMAssertionCreateWithProperties}.
	 * <p>
	 * This key may be present in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 */
	String kIOPMAssertionPlugInIDKey = "PlugInBundleID";

	/**
	 * The {@code CFDictionary} key for assertion type in an assertion info
	 * dictionary.
	 * <p>
	 * The value for this key will be a {@link CFStringRef}, with the value of
	 * the assertion type specified at creation time. Note that OS X may
	 * substitute a support assertion type string if the caller specifies a
	 * deprecated assertion type; in that case the value for this key could
	 * differ from the caller-provided assertion type.
	 */
	String kIOPMAssertionTypeKey = "AssertType";

	/**
	 * The {@code CFDictionary} key for assertion level in an assertion info
	 * dictionary.
	 *
	 * The value for this key will be a {@code CFNumber},
	 * {@code kCFNumberIntType} with value {@link #kIOPMAssertionLevelOff} or
	 * {@link #kIOPMAssertionLevelOn}. The level reflects the assertion's level
	 * set at creation, or adjusted via
	 * {@link #IOPMAssertionSetProperty}.
	 */
	String kIOPMAssertionLevelKey = "AssertLevel";

	/**
	 * This assertion type is identical to
	 * {@link #kIOPMAssertPreventUserIdleSystemSleep} Please use that instead.
	 */
	String kIOPMAssertionTypePreventUserIdleSystemSleep = kIOPMAssertPreventUserIdleSystemSleep;

	/**
	 * This assertion type is identical to
	 * {@link #kIOPMAssertPreventUserIdleDisplaySleep} Please use that instead.
	 */
	String kIOPMAssertionTypePreventUserIdleDisplaySleep = kIOPMAssertPreventUserIdleDisplaySleep;

	/**
	 * Represents {@code IOPMUserActiveType} in {@code IOPMLib.h}.
	 */
	public enum IOPMUserActiveType implements JnaIntEnum<IOPMUserActiveType> {
		/** User is local on the system */
		kIOPMUserActiveLocal(0),

		/** Remote User connected to the system */
		kIOPMUserActiveRemote(1);

		private final int value;

		private IOPMUserActiveType(int value) {
			this.value = value;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public IOPMUserActiveType typeForValue(int value) {
			return IOPMUserActiveType.typeOf(value);
		}

		/**
		 * @param iopmUserActiveType the {@code IOPMUserActiveType} integer value.
		 * @return The corresponding {@link IOPMUserActiveType}.
		 */
		public static IOPMUserActiveType typeOf(int iopmUserActiveType) {
			for (IOPMUserActiveType entry : IOPMUserActiveType.values()) {
				if (entry.getValue() == iopmUserActiveType) {
					return entry;
				}
			}
			return null;
		}
	}

	/*
	 * Some constants from {@code IOKit/pwr_mgt/IOPM.h}
	 */

	/**
	 * Power Source state is published as properties to the {@code IORegistry} under these keys.
	 */
	String kIOPMPSExternalConnectedKey     = "ExternalConnected";
	String kIOPMPSExternalChargeCapableKey = "ExternalChargeCapable";
	String kIOPMPSBatteryInstalledKey      = "BatteryInstalled";
	String kIOPMPSIsChargingKey            = "IsCharging";
	String kIOPMFullyChargedKey            = "FullyCharged";
	String kIOPMPSAtWarnLevelKey           = "AtWarnLevel";
	String kIOPMPSAtCriticalLevelKey       = "AtCriticalLevel";
	String kIOPMPSCurrentCapacityKey       = "CurrentCapacity";
	String kIOPMPSMaxCapacityKey           = "MaxCapacity";
	String kIOPMPSDesignCapacityKey        = "DesignCapacity";
	String kIOPMPSTimeRemainingKey         = "TimeRemaining";
	String kIOPMPSAmperageKey              = "Amperage";
	String kIOPMPSVoltageKey               = "Voltage";
	String kIOPMPSCycleCountKey            = "CycleCount";
	String kIOPMPSMaxErrKey                = "MaxErr";
	String kIOPMPSAdapterInfoKey           = "AdapterInfo";
	String kIOPMPSLocationKey              = "Location";
	String kIOPMPSErrorConditionKey        = "ErrorCondition";
	String kIOPMPSManufacturerKey          = "Manufacturer";
	String kIOPMPSManufactureDateKey       = "ManufactureDate";
	String kIOPMPSModelKey                 = "Model";
	String kIOPMPSSerialKey                = "Serial";
	String kIOPMDeviceNameKey              = "DeviceName";
	String kIOPMPSLegacyBatteryInfoKey     = "LegacyBatteryInfo";
	String kIOPMPSBatteryHealthKey         = "BatteryHealth";
	String kIOPMPSHealthConfidenceKey      = "HealthConfidence";
	String kIOPMPSCapacityEstimatedKey     = "CapacityEstimated";
	String kIOPMPSBatteryChargeStatusKey   = "ChargeStatus";
	String kIOPMPSBatteryTemperatureKey    = "Temperature";
	String kIOPMPSAdapterDetailsKey        = "AdapterDetails";
	String kIOPMPSChargerConfigurationKey  = "ChargerConfiguration";

	/*
	 * Some functions from {@code IOKitLib.h}
	 */

	/**
	 * The default mach port used to initiate communication with {@code IOKit}.
	 * <p>
	 * When specifying a master port to {@code IOKit} functions, the
	 * {@code null} argument indicates "use the default". This is a synonym for
	 * {@code null}, if you'd rather use a named constant.
	 */
	MachPortT kIOMasterPortDefault = null;

	/**
	 * Returns the mach port used to initiate communication with {@code IOKit}.
	 *
	 * Functions that don't specify an existing object require the
	 * {@code IOKit master port} to be passed. This function obtains that port.
	 *
	 * @param bootstrapPort pass {@code null} for the default.
	 * @param masterPort (Output) The returned master port.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOMasterPort(MachPortT bootstrapPort, MachPortTRef masterPort);

	/**
	 * Releases an object handle previously returned by {@code IOKitLib}.
	 * <p>
	 * All objects returned by {@code IOKitLib} should be released with this
	 * function when access to them is no longer needed. Using the object after
	 * it has been released may or may not return an error, depending on how
	 * many references the task has to the same object in the kernel.
	 *
	 * @param object the {@code IOKit object} to release.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOObjectRelease(IOObjectT object);

	/**
	 * Retains an object handle previously returned by IOKitLib.
	 * <p>
	 * Gives the caller an additional reference to an existing object handle
	 * previously returned by {@code IOKitLib}.
	 *
	 * @param object the {@code IOKit object} to retain.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOObjectRetain(IOObjectT object);

	/**
	 * Return the class name of an IOKit object.
	 * <p>
	 * This function uses the {@code OSMetaClass} system in the kernel to derive
	 * the name of the class the object is an instance of.
	 *
	 * @param object the {@code IOKit} object.
	 * @param className caller allocated buffer to receive the name string.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOObjectGetClass(IOObjectT object, IONameT className);

	/**
	 * Return the class name of an {@code IOKit} object.
	 * <p>
	 * This function does the same thing as {@link #IOObjectGetClass}, but
	 * returns the result as a {@link CFStringRef}.
	 *
	 * @param object the {@code IOKit} object.
	 * @return The resulting {@link CFStringRef}. <b>This should be released by
	 *         the caller with {@link #IOObjectRelease}</b>. If a valid object
	 *         is not passed in, {@code null} is returned.
	 *
	 * @since OS X 10.4
	 */
	CFStringRef IOObjectCopyClass(IOObjectT object);

	/**
	 * Return the superclass name of the given class.
	 * <p>
	 * This function uses the {@code OSMetaClass} system in the kernel to derive
	 * the name of the superclass of the class.
	 *
	 * @param classname the name of the class as a {@code CFString}.
	 * @return The resulting {@link CFStringRef}. <b>This should be released by
	 *         the caller with {@link #IOObjectRelease}</b> If there is no
	 *         superclass, or a valid class name is not passed in, {@code null}
	 *         is returned.
	 *
	 * @since OS X 10.4
	 */
	CFStringRef IOObjectCopySuperclassForClass(CFStringRef classname);

	/**
	 * Performs an {@code OSDynamicCast} operation on an {@code IOKit} object.
	 * <p>
	 * This function uses the {@code OSMetaClass} system in the kernel to
	 * determine if the object will dynamic cast to a class, specified as a
	 * C-string. In other words, if the object is of that class or a subclass.
	 *
	 * @param object an {@code IOKit} object.
	 * @param className the name of the class, as a C-string.
	 * @return If the {@code object} is valid, and represents an object in the
	 *         kernel that dynamic casts to the class {@code true} is returned,
	 *         otherwise {@code false}.
	 */
	boolean IOObjectConformsTo(IOObjectT object, IONameT className);

	/**
	 * Checks two object handles to see if they represent the same kernel
	 * object.
	 * <p>
	 * If two object handles are returned by {@code IOKitLib} functions, this
	 * function will compare them to see if they represent the same kernel
	 * object.
	 *
	 * @param object an {@code IOKit} object.
	 * @param anObject another {@code IOKit} object.
	 * @return If both object handles are valid, and represent the same object
	 *         in the kernel {@code true} is returned, otherwise {@code false}.
	 */
	boolean IOObjectIsEqualTo(IOObjectT object, IOObjectT anObject);

	/**
	 * Returns the next object in an iteration.
	 * <p>
	 * This function returns the next object in an iteration, or zero if no more
	 * remain or the iterator is invalid.
	 *
	 * @param iterator an {@code IOKit iterator} handle.
	 * @return If the iterator handle is valid, the next element in the
	 *         iteration is returned, otherwise zero is returned. <b>The element
	 *         should be released by the caller by calling
	 *         {@link #IOObjectRelease} when it is finished</b>.
	 */
	IOObjectT IOIteratorNext(IOIteratorT iterator);

	/**
	 * Resets an iteration back to the beginning.
	 * <p>
	 * If an iterator is invalid, or if the caller wants to start over,
	 * {@link #IOIteratorReset} will set the iteration back to the beginning.
	 *
	 * @param iterator an {@code IOKit iterator} handle.
	 */
	void IOIteratorReset(IOIteratorT iterator);

	/**
	 * Checks an iterator is still valid.
	 * <p>
	 * Some iterators will be made invalid if changes are made to the structure
	 * they are iterating over. This function checks the iterator is still valid
	 * and should be called when {@link #IOIteratorNext} returns zero. An
	 * invalid iterator can be reset and the iteration restarted.
	 *
	 * @param iterator an {@code IOKit iterator} handle.
	 * @return {@code true} if the iterator handle is valid, otherwise
	 *         {@code false} is returned.
	 */
	boolean IOIteratorIsValid(IOIteratorT iterator);

	/**
	 * Look up a registered {@code IOService} object that matches the
	 * {@code matching} dictionary.
	 * <p>
	 * This is the preferred method of finding {@code IOService} objects
	 * currently registered by {@code IOKit} (that is, objects that have had
	 * their {@code registerService()} methods invoked). To find
	 * {@code IOService} objects that aren't yet registered, use an iterator as
	 * created by {@link #IORegistryEntryCreateIterator}.
	 * {@code IOServiceAddMatchingNotification} can also supply this information
	 * and install a notification of new {@code IOServices}. The matching
	 * information used in the matching dictionary may vary depending on the
	 * class of service being looked up.
	 *
	 * @param masterPort the master port obtained from {@link #IOMasterPort}.
	 *            Pass {@link #kIOMasterPortDefault} to look up the default
	 *            master port.
	 * @param matching ({@code CF_RELEASES_ARGUMENT}) a {@code CFDictionary}
	 *            containing matching information, <b>of which one reference is
	 *            always consumed by this function (
	 *            {@link CoreFoundation#CFRelease} is called once)</b>. Note:
	 *            prior to the Tiger release there was a small chance that the
	 *            dictionary might not be released if there was an error
	 *            attempting to serialize the dictionary.
	 *            <p>
	 *            {@code IOKitLib} can construct matching dictionaries for
	 *            common criteria with helper functions such as
	 *            {@link #IOServiceMatching}, {@link #IOServiceNameMatching},
	 *            {@link #IOBSDNameMatching}.
	 * @return The first service matched is returned on success. <b>The service
	 *         must be released by the caller with {@link #IOObjectRelease}</b>.
	 */
	IOServiceT IOServiceGetMatchingService(MachPortT masterPort, CFDictionaryRef matching);

	/**
	 * Look up registered {@code IOService} objects that match a matching
	 * dictionary.
	 * <p>
	 * This is the preferred method of finding {@code IOService} objects
	 * currently registered by {@code IOKit} (that is, objects that have had
	 * their {@code registerService()} methods invoked). To find
	 * {@code IOService} objects that aren't yet registered, use an iterator as
	 * created by {@link #IORegistryEntryCreateIterator}.
	 * {@code IOServiceAddMatchingNotification} can also supply this information
	 * and install a notification of new {@code IOServices}. The matching
	 * information used in the matching dictionary may vary depending on the
	 * class of service being looked up.
	 *
	 * @param masterPort the master port obtained from {@link #IOMasterPort}.
	 *            Pass {@link #kIOMasterPortDefault} to look up the default
	 *            master port.
	 * @param matching ({@code CF_RELEASES_ARGUMENT}) a {@code CFDictionary}
	 *            containing matching information, <b>of which one reference is
	 *            always consumed by this function (
	 *            {@link CoreFoundation#CFRelease} is called once)</b>. Note:
	 *            prior to the Tiger release there was a small chance that the
	 *            dictionary might not be released if there was an error
	 *            attempting to serialize the dictionary.
	 *            <p>
	 *            {@code IOKitLib} can construct matching dictionaries for
	 *            common criteria with helper functions such as
	 *            {@link #IOServiceMatching}, {@link #IOServiceNameMatching},
	 *            {@link #IOBSDNameMatching}.
	 * @param existing an iterator handle, or {@code null}, is returned on
	 *            success, and should be released by the caller when the
	 *            iteration is finished. If {@code null} is returned, the
	 *            iteration was successful but found no matching services.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOServiceGetMatchingServices(MachPortT masterPort, CFDictionaryRef matching, IOIteratorTRef existing);

	/**
	 * Returns the {@code busyState} of an {@code IOService}.
	 * <p>
	 * Many activities in {@code IOService} are asynchronous. When registration,
	 * matching, or termination is in progress on an {@code IOService}, its
	 * {@code busyState} is increased by one. Change in {@code busyState} to or
	 * from zero also changes the {@code IOService}'s provider's
	 * {@code busyState} by one, which means that an {@code IOService} is marked
	 * busy when any of the above activities is occurring on it or any of its
	 * clients.
	 *
	 * @param service the {@code IOService} whose {@code busyState} to return.
	 * @param busyState the {@code busyState} count is returned.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOServiceGetBusyState(IOServiceT service, IntByReference busyState);

	/**
	 * A request to create a connection to an {@code IOService}.
	 * <p>
	 * A non kernel client may request a connection be opened via the
	 * {@link #IOServiceOpen} library function, which will call { @code
	 * IOService::newUserClient } in the kernel. The rules & capabilities of
	 * user level clients are family dependent, the default {@code IOService}
	 * implementation returns {@link IOReturn#kIOReturnUnsupported}.
	 *
	 * @param service the {@code IOService} object to open a connection to,
	 *            usually obtained via the {@link #IOServiceGetMatchingServices}
	 *            or {@code IOServiceAddNotification} APIs.
	 * @param owningTask the mach task requesting the connection.
	 * @param type a constant specifying the type of connection to be created,
	 *            interpreted only by the {@code IOService}'s family.
	 * @param connect an {@link IOConnectT} handle is returned on success, to be
	 *            used with the {@code IOConnectXXX} APIs. It should be
	 *            destroyed with {@link #IOServiceClose}.
	 * @return A return code generated by {@code IOService::newUserClient}.
	 */
	KernReturnT IOServiceOpen(IOServiceT service, TaskPortT owningTask, int type, IOConnectTRef connect);

	/**
	 * A request to rescan a bus for device changes.
	 * <p>
	 * A non kernel client may request a bus or controller rescan for added or
	 * removed devices, if the bus family does automatically notice such
	 * changes. For example, SCSI bus controllers do not notice device changes.
	 * The implementation of this routine is family dependent, and the default
	 * {@code IOService} implementation returns
	 * {@link IOReturn#kIOReturnUnsupported}.
	 *
	 * @param service the {@code IOService} object to request a rescan, usually
	 *            obtained via the {@link #IOServiceGetMatchingServices} or
	 *            {@code IOServiceAddNotification} APIs.
	 * @param options an options mask, interpreted only by the {@code IOService}
	 *            's family.
	 * @return A return code generated by {@code IOService::requestProbe}.
	 */
	KernReturnT IOServiceRequestProbe(IOServiceT service, int options);

	/**
	 * Close a connection to an {@code IOService} and destroy the connect
	 * handle.
	 * <p>
	 * A connection created with the {@link #IOServiceOpen} should be closed
	 * when the connection is no longer to be used with {@link #IOServiceClose}.
	 *
	 * @param connect the {@link IOConnectT} handle created by
	 *            {@link #IOServiceOpen}. It will be destroyed by this function,
	 *            and should not be released with {@link #IOObjectRelease}.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOServiceClose(IOConnectT connect);

	/**
	 * Adds a reference to the connect handle.
	 *
	 * @param connect the {@link IOConnectT} handle created by
	 *            {@link #IOServiceOpen}.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOConnectAddRef(IOConnectT connect);

	/**
	 * Remove a reference to the connect handle. If the last reference is
	 * removed an implicit {@link #IOServiceClose} is performed.
	 *
	 * @param connect the {@link IOConnectT} handle created by
	 *            {@link #IOServiceOpen}.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOConnectRelease(IOConnectT connect);

	/**
	 * Returns the IOService a connect handle was opened on.
	 * <p>
	 * Finds the service object a connection was opened on.
	 *
	 * @param connect the {@link IOConnectT} handle created by
	 *            {@link #IOServiceOpen}.
	 * @param service on success, the {@link IOServiceT} handle the connection
	 *            was opened on, which should be released with
	 *            {@link #IOObjectRelease}.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOConnectGetService(IOConnectT connect, IOServiceTRef service);

	/**
	 * Set a port to receive family specific notifications.
	 * <p>
	 * This is a generic method to pass a mach port send right to be be used by
	 * family specific notifications.
	 *
	 * @param connect the connect handle created by {@link #IOServiceOpen}.
	 * @param type the type of notification requested, not interpreted by
	 *            {@code IOKit} and family defined.
	 * @param port the {@link MachPortT} to which to send notifications.
	 * @param reference some families may support passing a reference parameter
	 *            for the callers use with the notification.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IOConnectSetNotificationPort(
		IOConnectT connect,
		int type,
		MachPortT port,
		Pointer reference
	);

	/**
	 * Set CF container based properties on a connection.
	 * <p>
	 * This is a generic method to pass a CF container of properties to the
	 * connection. The properties are interpreted by the family and commonly
	 * represent configuration settings, but may be interpreted as anything.
	 *
	 * @param connect the connect handle created by {@link #IOServiceOpen}.
	 * @param properties a CF container - commonly a {@code CFDictionary} but
	 *            this is not enforced. The container should consist of objects
	 *            which are understood by {@code IOKit} - these are currently :
	 *            {@code CFDictionary}, {@code CFArray}, {@code CFSet},
	 *            {@code CFString}, {@code CFData}, {@code CFNumber} and
	 *            {@code CFBoolean}, and are passed in the kernel as the
	 *            corresponding {@code OSDictionary} etc. objects.
	 * @return A {@link KernReturnT} error code returned by the family.
	 */
	KernReturnT IOConnectSetCFProperties(IOConnectT connect, CFTypeRef properties);

	/**
	 * Set a CF container based property on a connection.
	 * <p>
	 * This is a generic method to pass a CF property to the connection. The
	 * property is interpreted by the family and commonly represent
	 * configuration settings, but may be interpreted as anything.
	 *
	 * @param connect the connect handle created by {@link #IOServiceOpen}.
	 * @param propertyName the name of the property as a {@code CFString}.
	 * @param property a CF container - should consist of objects which are
	 *            understood by {@code IOKit} - these are currently :
	 *            {@code CFDictionary}, {@code CFArray}, {@code CFSet},
	 *            {@code CFString}, {@code CFData}, {@code CFNumber} and
	 *            {@code CFBoolean}, and are passed in the kernel as the
	 *            corresponding {@code OSDictionary} etc. objects.
	 * @return A {@link KernReturnT} error code returned by the object.
	 */
	KernReturnT IOConnectSetCFProperty(IOConnectT connect, CFStringRef propertyName, CFTypeRef property);

	/**
	 * This method isn't documented by Apple.
	 *
	 * @param connection the connection.
	 * @param selector the selector.
	 * @param input the input.
	 * @param inputCnt the input count.
	 * @param inputStruct the input struct.
	 * @param inputStructCnt the input struct count.
	 * @param output (Output) the output.
	 * @param outputCnt (Input/Output) the output count.
	 * @param outputStruct (Output) the output struct.
	 * @param outputStructCnt (Input/Output) the output struct count.
	 * @return A {@link KernReturnT} error code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOConnectCallMethod(
		MachPortT connection, // In
		int selector, // In
		LongByReference input, // In
		int inputCnt, // In
		Structure inputStruct, // In
		NativeLong inputStructCnt, // In
		LongByReference output, // Out
		IntByReference outputCnt, // In/Out
		Structure outputStruct, // Out
		NativeLongByReference outputStructCnt // In/Out
	);

	/**
	 * This method isn't documented by Apple.
	 *
	 * @param connection the connection.
	 * @param selector the selector.
	 * @param wake_port the wake port.
	 * @param reference the reference.
	 * @param referenceCnt the reference count.
	 * @param input the input.
	 * @param inputCnt the input count.
	 * @param inputStruct the input struct.
	 * @param inputStructCnt the input struct count.
	 * @param output (Output) the output.
	 * @param outputCnt (Input/Output) the output count.
	 * @param outputStruct (Output) the output struct.
	 * @param outputStructCnt (Input/Output) the output struct count.
	 * @return A {@link KernReturnT} error code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOConnectCallAsyncMethod(
		MachPortT connection, // In
		int selector, // In
		MachPortT wake_port, // In
		LongByReference reference, // In
		int referenceCnt, // In
		LongByReference input, // In
		int inputCnt, // In
		Structure inputStruct, // In
		NativeLong inputStructCnt, // In
		LongByReference output, // Out
		IntByReference outputCnt, // In/Out
		Structure outputStruct, // Out
		NativeLongByReference outputStructCnt // In/Out
	);

	/**
	 * This method isn't documented by Apple.
	 *
	 * @param connection the connection.
	 * @param selector the selector.
	 * @param inputStruct the input struct.
	 * @param inputStructCnt the input struct count.
	 * @param outputStruct (Output) the output struct.
	 * @param outputStructCnt (Input/Output) the output struct count.
	 * @return A {@link KernReturnT} error code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOConnectCallStructMethod(
		MachPortT connection, // In
		int selector, // In
		Structure inputStruct, // In
		NativeLong inputStructCnt, // In
		Structure outputStruct, // Out
		NativeLongByReference outputStructCnt // In/Out
	);

	/**
	 * This method isn't documented by Apple.
	 *
	 * @param connection the connection.
	 * @param selector the selector.
	 * @param wake_port the wake port.
	 * @param reference the reference.
	 * @param referenceCnt the reference count.
	 * @param inputStruct the input struct.
	 * @param inputStructCnt the input struct count.
	 * @param outputStruct (Output) the output struct.
	 * @param outputStructCnt (Input/Output) the output struct count.
	 * @return A {@link KernReturnT} error code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOConnectCallAsyncStructMethod(
		MachPortT connection, // In
		int selector, // In
		MachPortT wake_port, // In
		LongByReference reference, // In
		int referenceCnt, // In
		Structure inputStruct, // In
		NativeLong inputStructCnt, // In
		Structure outputStruct, // Out
		NativeLongByReference outputStructCnt // In/Out
	);

	/**
	 * This method isn't documented by Apple.
	 *
	 * @param connection the connection.
	 * @param selector the selector.
	 * @param input the input.
	 * @param inputCnt the input count.
	 * @param output (Output) the output.
	 * @param outputCnt (Input/Output) the output count.
	 * @return A {@link KernReturnT} error code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOConnectCallScalarMethod(
		MachPortT connection, // In
		int selector, // In
		LongByReference input, // In
		int inputCnt, // In
		LongByReference output, // Out
		IntByReference outputCnt // In/Out
	);

	/**
	 * This method isn't documented by Apple.
	 *
	 * @param connection the connection.
	 * @param selector the selector.
	 * @param wake_port the wake port.
	 * @param reference the reference.
	 * @param referenceCnt the reference count.
	 * @param input the input.
	 * @param inputCnt the input count.
	 * @param output (Output) the output.
	 * @param outputCnt (Input/Output) the output count.
	 * @return A {@link KernReturnT} error code.
	 *
	 * @since OS X 10.5
	 */
	KernReturnT IOConnectCallAsyncScalarMethod(
		MachPortT connection, // In
		int selector, // In
		MachPortT wake_port, // In
		LongByReference reference, // In
		int referenceCnt, // In
		LongByReference input, // In
		int inputCnt, // In
		LongByReference output, // Out
		IntByReference outputCnt // In/Out
	);

	/**
	 * Inform a connection of a second connection.
	 * <p>
	 * This is a generic method to inform a family connection of a second
	 * connection, and is rarely used.
	 *
	 * @param connect the connect handle created by {@link #IOServiceOpen}.
	 * @param client another connect handle created by {@link #IOServiceOpen}.
	 * @return A {@link KernReturnT} error code returned by the family.
	 */
	KernReturnT IOConnectAddClient(IOConnectT connect, IOConnectT client);

	/**
	 * Return a handle to the registry root.
	 * <p>
	 * This method provides an accessor to the root of the registry for the
	 * machine. The root may be passed to a registry iterator when iterating a
	 * plane, and contains properties that describe the available planes, and
	 * diagnostic information for IOKit.
	 *
	 * @param masterPort the master port obtained from {@link #IOMasterPort}.
	 *            Pass {@link #kIOMasterPortDefault} to look up the default
	 *            master port.
	 * @return A handle to the {@link IORegistryEntryT} root instance, to be
	 *         released with {@link #IOObjectRelease} by the caller, or
	 *         {@link MachPortT#MACH_PORT_NULL} on failure.
	 */
	IORegistryEntryT IORegistryGetRootEntry(MachPortT masterPort);

	/**
	 * Looks up a registry entry by path.
	 * <p>
	 * This function parses paths to lookup registry entries. The path should
	 * begin with '{@code <plane name>:}' If there are characters remaining
	 * unparsed after an entry has been looked up, this is considered an invalid
	 * lookup. Paths are further documented in {@code IORegistryEntry.h}.
	 *
	 * @param masterPort the master port obtained from {@link #IOMasterPort}.
	 *            Pass {@link #kIOMasterPortDefault} to look up the default
	 *            master port.
	 * @param path a C-string path.
	 * @return A handle to the {@link IORegistryEntryT} witch was found with the
	 *         path, to be released with {@link #IOObjectRelease} by the caller,
	 *         or {@link MachPortT#MACH_PORT_NULL} on failure.
	 */
	IORegistryEntryT IORegistryEntryFromPath(MachPortT masterPort, IOServiceT path);

	/**
	 * Looks up a registry entry by path.
	 * <p>
	 * This function parses paths to lookup registry entries. The path should
	 * begin with '{@code <plane name>:}' If there are characters remaining
	 * unparsed after an entry has been looked up, this is considered an invalid
	 * lookup. Paths are further documented in {@code IORegistryEntry.h}.
	 *
	 * @param masterPort the master port obtained from {@link #IOMasterPort}.
	 *            Pass {@link #kIOMasterPortDefault} to look up the default
	 *            master port.
	 * @param path a {@code CFString} path.
	 * @return A handle to the {@link IORegistryEntryT} witch was found with the
	 *         path, to be released with {@link #IOObjectRelease} by the caller,
	 *         or {@link MachPortT#MACH_PORT_NULL} on failure.
	 *
	 * @since OS X 10.11
	 */
	IORegistryEntryT IORegistryEntryCopyFromPath(MachPortT masterPort, CFStringRef path);

	/**
	 * Option for {@link #IORegistryCreateIterator},
	 * {@link #IORegistryEntryCreateIterator},
	 * {@link #IORegistryEntrySearchCFProperty}.
	 */
	int kIORegistryIterateRecursively = 1;

	/**
	 * Option for {@link #IORegistryCreateIterator},
	 * {@link #IORegistryEntryCreateIterator},
	 * {@link #IORegistryEntrySearchCFProperty}.
	 */
	int kIORegistryIterateParents = 2;

	/**
	 * Create an iterator rooted at the registry root.
	 * <p>
	 * This method creates an {@code IORegistryIterator} in the kernel that is
	 * set up with options to iterate children of the registry root entry, and
	 * to recurse automatically into entries as they are returned, or only when
	 * instructed with calls to {@link #IORegistryIteratorEnterEntry}. The
	 * iterator object keeps track of entries that have been recursed into
	 * previously to avoid loops.
	 *
	 * @param masterPort the master port obtained from {@link #IOMasterPort}.
	 *            Pass {@link #kIOMasterPortDefault} to look up the default
	 *            master port.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param options {@link #kIORegistryIterateRecursively} may be set to
	 *            recurse automatically into each entry as it is returned from
	 *            {@link #IOIteratorNext} calls on the registry iterator.
	 * @param iterator a created {@link IOIteratorT} handle, to be released by
	 *            the caller when it has finished with it with
	 *            {@link #IOObjectRelease}.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryCreateIterator(MachPortT masterPort, IONameT plane, int options, IOIteratorTRef iterator);

	/**
	 * Create an iterator rooted at a given registry entry.
	 * <p>
	 * This method creates an {@code IORegistryIterator} in the kernel that is
	 * set up with options to iterate children or parents of a root entry, and
	 * to recurse automatically into entries as they are returned, or only when
	 * instructed with calls to {@link #IORegistryIteratorEnterEntry}. The
	 * iterator object keeps track of entries that have been recursed into
	 * previously to avoid loops.
	 *
	 * @param entry the root entry to begin the iteration at.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param options {@link #kIORegistryIterateRecursively} may be set to
	 *            recurse automatically into each entry as it is returned from
	 *            {@link #IOIteratorNext} calls on the registry iterator.
	 *            {@link #kIORegistryIterateParents} may be set to iterate the
	 *            parents of each entry, by default the children are iterated.
	 * @param iterator a created {@link IOIteratorT} handle, to be released by
	 *            the caller when it has finished with it with
	 *            {@link #IOObjectRelease}.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryCreateIterator(
		IORegistryEntryT entry,
		IONameT plane,
		int options,
		IOIteratorTRef iterator
	);

	/**
	 * Recurse into the current entry in the registry iteration.
	 * <p>
	 * This method makes the current entry, i.e. the last entry returned by
	 * {@link #IOIteratorNext}, the root in a new level of recursion.
	 *
	 * @param iterator the iterator.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryIteratorEnterEntry(IOIteratorT iterator);

	/**
	 * Exits a level of recursion, restoring the current entry.
	 * <p>
	 * This method undoes an {@link #IORegistryIteratorEnterEntry}, restoring
	 * the current entry. If there are no more levels of recursion to exit
	 * {@link IOReturn#kIOReturnNoDevice} is returned, otherwise
	 * {@link DefaultKernReturnT#kIOReturnSuccess} is returned.
	 *
	 * @param iterator the iterator.
	 * @return {@link DefaultKernReturnT#kIOReturnSuccess} if a level of
	 *         recursion was undone, {@link IOReturn#kIOReturnNoDevice} if no
	 *         recursive levels are left in the iteration.
	 */
	KernReturnT IORegistryIteratorExitEntry(IOIteratorT	iterator);

	/**
	 * Returns a C-string name assigned to a registry entry.
	 * <p>
	 * Registry entries can be named in a particular plane, or globally. This
	 * function returns the entry's global name. The global name defaults to the
	 * entry's meta class name if it has not been named.
	 *
	 * @param entry the registry entry handle whose name to look up.
	 * @param name the caller's buffer to receive the name.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryGetName(IORegistryEntryT entry, IONameT name);

	/**
	 * Returns a C-string name assigned to a registry entry, in a specified
	 * plane.
	 * <p>
	 * Registry entries can be named in a particular plane, or globally. This
	 * function returns the entry's name in the specified plane or global name
	 * if it has not been named in that plane. The global name defaults to the
	 * entry's meta class name if it has not been named.
	 *
	 * @param entry the registry entry handle whose name to look up.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param name the caller's buffer to receive the name.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryGetNameInPlane(
		IORegistryEntryT entry,
		IONameT plane,
		IONameT name
	);

	/**
	 * Returns a C-string location assigned to a registry entry, in a specified
	 * plane.
	 * <p>
	 * Registry entries can given a location string in a particular plane, or
	 * globally. If the entry has had a location set in the specified plane that
	 * location string will be returned, otherwise the global location string is
	 * returned. If no global location string has been set, an error is
	 * returned.
	 *
	 * @param entry the registry entry handle whose name to look up.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param location the caller's buffer to receive the location string.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryGetLocationInPlane(
		IORegistryEntryT entry,
		IONameT plane,
		IONameT location
	);

	/**
	 * Create a path for a registry entry.
	 * <p>
	 * The path for a registry entry is copied to the caller's buffer. The path
	 * describes the entry's attachment in a particular plane, which must be
	 * specified. The path begins with the plane name followed by a colon, and
	 * then followed by '{@code /}' separated path components for each of the
	 * entries between the root and the registry entry. An alias may also exist
	 * for the entry, and will be returned if available.
	 *
	 * @param entry the registry entry handle whose path to look up.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param path a char buffer allocated by the caller.
	 * @return {@link #IORegistryEntryGetPath} will fail if the entry is not
	 *         attached in the plane, or if the buffer is not large enough to
	 *         contain the path.
	 */
	KernReturnT IORegistryEntryGetPath(
		IORegistryEntryT entry,
		IONameT plane,
		IOStringT path
	);

	/**
	 * Create a path for a registry entry.
	 * <p>
	 * The path for a registry entry is returned as a {@code CFString} The path
	 * describes the entry's attachment in a particular plane, which must be
	 * specified. The path begins with the plane name followed by a colon, and
	 * then followed by '{@code /}' separated path components for each of the
	 * entries between the root and the registry entry. An alias may also exist
	 * for the entry, and will be returned if available.
	 *
	 * @param entry the registry entry handle whose path to look up.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @return An instance of {@code CFString} on success, to be released by the
	 *         caller with {@link CoreFoundation#CFRelease}.
	 *         {@link #IORegistryEntryCopyPath} will fail if the entry is not
	 *         attached in the plane.
	 *
	 * @since OS X 10.11
	 */
	CFStringRef IORegistryEntryCopyPath(IORegistryEntryT entry, IONameT plane);

	/**
	 * Returns an ID for the registry entry that is global to all tasks.
	 * <p>
	 * The entry ID returned by {@link #IORegistryEntryGetRegistryEntryID} can
	 * be used to identify a registry entry across all tasks. A registry entry
	 * may be looked up by its {@code entryID} by creating a matching dictionary
	 * with {@link #IORegistryEntryIDMatching} to be used with the {@code IOKit}
	 * matching functions. The ID is valid only until the machine reboots.
	 *
	 * @param entry the registry entry handle whose ID to look up.
	 * @param entryID (Output) the resulting ID.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryGetRegistryEntryID(IORegistryEntryT entry, LongByReference entryID);

	/**
	 * Creates a {@code CFDictionary} representation of a registry entry's
	 * property table.
	 * <p>
	 * This function creates an instantaneous snapshot of a registry entry's
	 * property table, creating a {@code CFDictionary} analog in the caller's
	 * task. Not every object available in the kernel is represented as a CF
	 * container. Currently {@code OSDictionary}, {@code OSArray},
	 * {@code OSSet}, {@code OSSymbol}, {@code OSString}, {@code OSData},
	 * {@code OSNumber} and {@code OSBoolean} are created as their CF
	 * counterparts.
	 *
	 * @param entry the registry entry handle whose property table to copy.
	 * @param properties (Output) a {@code CFDictionary} is created and
	 *            returned to the caller on success. The caller should release
	 *            with {@link CoreFoundation#CFRelease}.
	 * @param allocator the {@code CFAllocator} to use when creating the CF
	 *            containers.
	 * @param options no options are currently defined.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryCreateCFProperties(
		IORegistryEntryT entry,
		CFMutableDictionaryRefByReference properties,
		CFAllocatorRef allocator,
		int options
	);

	/**
	 * Create a CF representation of a registry entry's property.
	 * <p>
	 * This function creates an instantaneous snapshot of a registry entry
	 * property, creating a CF container analog in the caller's task. Not every
	 * object available in the kernel is represented as a CF container.
	 * Currently {@code OSDictionary}, {@code OSArray}, {@code OSSet},
	 * {@code OSSymbol}, {@code OSString}, {@code OSData}, {@code OSNumber} and
	 * {@code OSBoolean} are created as their CF counterparts.
	 *
	 * @param entry the registry entry handle whose property to copy.
	 * @param key a {@code CFString} specifying the property name.
	 * @param allocator the {@code CFAllocator} to use when creating the CF
	 *            container.
	 * @param options no options are currently defined.
	 * @return A CF container is created and returned the caller on success. The
	 *         caller should release this with {@link CoreFoundation#CFRelease}.
	 */
	CFTypeRef IORegistryEntryCreateCFProperty(
		IORegistryEntryT entry,
		CFStringRef key,
		CFAllocatorRef allocator,
		int options
	);

	/**
	 * Create a CF representation of a registry entry's property. This function
	 * creates an instantaneous snapshot of a registry entry property, creating
	 * a CF container analog in the caller's task. Not every object available in
	 * the kernel is represented as a CF container; currently
	 * {@code OSDictionary}, {@code OSArray}, {@code OSSet}, {@code OSSymbol},
	 * {@code OSString}, {@code OSData}, {@code OSNumber} and {@code OSBoolean}
	 * are created as their CF counterparts.
	 * <p>
	 * This function will search for a property, starting first with specified
	 * registry entry's property table, then iterating recursively through
	 * either the parent registry entries or the child registry entries of this
	 * entry. Once the first occurrence is found, it will lookup and return the
	 * value of the property, using the same semantics as
	 * {@link #IORegistryEntryCreateCFProperty}. The iteration keeps track of
	 * entries that have been recursed into previously to avoid loops.
	 *
	 * @param entry the registry entry at which to start the search.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param key a {@code CFString} specifying the property name.
	 * @param allocator The {@code CFAllocator} to use when creating the CF
	 *            container.
	 * @param options {@link #kIORegistryIterateRecursively} may be set to
	 *            recurse automatically into the registry hierarchy. Without
	 *            this option, this method degenerates into the standard
	 *            {@link #IORegistryEntryCreateCFProperty} call.
	 *            {@link #kIORegistryIterateParents} may be set to iterate the
	 *            parents of the entry, in place of the children.
	 * @return ({@code CF_RETURNS_RETAINED}) A CF container is created and
	 *         returned the caller on success. The caller should release with
	 *         {@link CoreFoundation#CFRelease}.
	 */
	CFTypeRef IORegistryEntrySearchCFProperty(
		IORegistryEntryT entry,
		IONameT plane,
		CFStringRef key,
		CFAllocatorRef allocator,
		int options
	);

	/**
	 * Set CF container based properties in a registry entry.
	 * <p>
	 * This is a generic method to pass a CF container of properties to an
	 * object in the registry. Setting properties in a registry entry is not
	 * generally supported, it is more common to support
	 * {@link #IOConnectSetCFProperties} for connection based property setting.
	 * The properties are interpreted by the object.
	 *
	 * @param entry the registry entry whose properties to set.
	 * @param properties a CF container - commonly a {@code CFDictionary} but
	 *            this is not enforced. The container should consist of objects
	 *            which are understood by {@code IOKit} - these are currently :
	 *            {@code CFDictionary}, {@code CFArray}, {@code CFSet},
	 *            {@code CFString}, {@code CFData}, {@code CFNumber},
	 *            {@code CFBoolean}, and are passed in the kernel as the
	 *            corresponding {@code OSDictionary} etc. objects.
	 * @return A {@link KernReturnT} error code returned by the object.
	 */
	KernReturnT IORegistryEntrySetCFProperties(IORegistryEntryT entry, CFTypeRef properties);

	/**
	 * Set a CF container based property in a registry entry.
	 * <p>
	 * This is a generic method to pass a CF container as a property to an
	 * object in the registry. Setting properties in a registry entry is not
	 * generally supported, it is more common to support
	 * {@link #IOConnectSetCFProperty} for connection based property setting.
	 * The property is interpreted by the object.
	 *
	 * @param entry the registry entry whose property to set.
	 * @param propertyName the name of the property as a {@code CFString}.
	 * @param property a CF container - should consist of objects which are
	 *            understood by {@code IOKit} - these are currently :
	 *            {@code CFDictionary}, {@code CFArray}, {@code CFSet},
	 *            {@code CFString}, {@code CFData}, {@code CFNumber},
	 *            {@code CFBoolean}, and are passed in the kernel as the
	 *            corresponding {@code OSDictionary} etc. objects.
	 * @return A {@link KernReturnT} error code returned by the object.
	 */
	KernReturnT IORegistryEntrySetCFProperty(
		IORegistryEntryT entry,
		CFStringRef propertyName,
		CFTypeRef property
	);

	/**
	 * Returns an iterator over an registry entry's child entries in a plane.
	 * <p>
	 * This method creates an iterator which will return each of a registry
	 * entry's child entries in a specified plane.
	 *
	 * @param entry The registry entry whose children to iterate over.
	 * @param plane The name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param iterator The created iterator over the children of the entry, on
	 *            success. The iterator must be released with
	 *            {@link #IOObjectRelease} when the iteration is finished.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryGetChildIterator(
		IORegistryEntryT entry,
		IONameT plane,
		IOIteratorTRef iterator
	);

	/**
	 * Returns the first child of a registry entry in a plane.
	 * <p>
	 * This function will return the child which first attached to a registry
	 * entry in a plane.
	 *
	 * @param entry the registry entry whose child to look up.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in {@code IOKitKeys.h}, e.g. {@code kIOServicePlane}.
	 * @param child the first child of the registry entry, on success. The child
	 *            must be released with {@link #IOObjectRelease} by the caller.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryGetChildEntry(
		IORegistryEntryT entry,
		IONameT plane,
		IORegistryEntryTRef child
	);

	/**
	 * Returns an iterator over an registry entry's parent entries in a plane.
	 * <p>
	 * This method creates an iterator which will return each of a registry
	 * entry's parent entries in a specified plane.
	 *
	 * @param entry the registry entry whose parents to iterate over.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in IOKitKeys.h, e.g. {@code kIOServicePlane}.
	 * @param iterator the created iterator over the parents of the entry, on
	 *            success. The iterator must be released with
	 *            {@link #IOObjectRelease} when the iteration is finished.
	 * @return A {@link KernReturnT} error.
	 */
	KernReturnT IORegistryEntryGetParentIterator(
		IORegistryEntryT entry,
		IONameT plane,
		IOIteratorTRef iterator
	);

	/**
	 * Returns the first parent of a registry entry in a plane.
	 * <p>
	 * This function will return the parent to which the registry entry was
	 * first attached in a plane.
	 *
	 * @param entry the registry entry whose parent to look up.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in IOKitKeys.h, e.g. {@code kIOServicePlane}.
	 * @param parent the first parent of the registry entry, on success. The
	 *            parent must be released with {@link #IOObjectRelease} by the
	 *            caller.
	 * @return A {@link KernReturnT} error code.
	 */
	KernReturnT IORegistryEntryGetParentEntry(
		IORegistryEntryT entry,
		IONameT plane,
		IORegistryEntryTRef parent
	);

	/**
	 * Determines if the registry entry is attached in a plane.
	 * <p>
	 * This method determines if the entry is attached in a plane to any other
	 * entry.
	 *
	 * @param entry the registry entry.
	 * @param plane the name of an existing registry plane. Plane names are
	 *            defined in IOKitKeys.h, e.g. {@code kIOServicePlane}.
	 * @return If the entry has a parent in the plane, {@code true} is returned,
	 *         otherwise {@code false} is returned.
	 */
	boolean IORegistryEntryInPlane(IORegistryEntryT entry, IONameT plane);

	/**
	 * Create a matching dictionary that specifies an {@code IOService} class
	 * match.
	 * <p>
	 * A very common matching criteria for {@code IOService} is based on its
	 * class. {@link #IOServiceMatching} will create a matching dictionary that
	 * specifies any {@code IOService} of a class, or its subclasses. The class
	 * is specified by C-string name.
	 *
	 * @param name the class name, as a {@code const} C-string. Class matching
	 *            is successful on {@code IOService}'s of this class or any
	 *            subclass.
	 * @return ({@code CF_RETURNS_RETAINED}) The matching dictionary created, is
	 *         returned on success, or zero on failure. The dictionary is
	 *         commonly passed to {@link #IOServiceGetMatchingServices} or
	 *         {@code IOServiceAddNotification} which will consume a reference,
	 *         otherwise it should be released with
	 *         {@link CoreFoundation#CFRelease} by the caller.
	 */
	CFMutableDictionaryRef IOServiceMatching(String	name);

	/**
	 * Create a matching dictionary that specifies an {@code IOService} name
	 * match.
	 * <p>
	 * A common matching criteria for {@code IOService} is based on its name.
	 * {@link #IOServiceNameMatching} will create a matching dictionary that
	 * specifies an {@code IOService} with a given name. Some {@code IOService}s
	 * created from the device tree will perform name matching on the standard
	 * compatible, name, model properties.
	 *
	 * @param name the {@code IOService} name, as a {@code const} C-string.
	 * @return ({@code CF_RETURNS_RETAINED}) The matching dictionary created, is
	 *         returned on success, or zero on failure. The dictionary is
	 *         commonly passed to {@link #IOServiceGetMatchingServices} or
	 *         {@code IOServiceAddNotification} which will consume a reference,
	 *         otherwise it should be released with
	 *         {@link CoreFoundation#CFRelease} by the caller.
	 */
	CFMutableDictionaryRef IOServiceNameMatching(String name);

	/**
	 * Create a matching dictionary that specifies an {@code IOService} match
	 * based on BSD device name.
	 * <p>
	 * IOServices that represent BSD devices have an associated BSD name. This
	 * function creates a matching dictionary that will match {@code IOService}
	 * 's with a given BSD name.
	 *
	 * @param masterPort the master port obtained from {@link #IOMasterPort}.
	 *            Pass {@link #kIOMasterPortDefault} to look up the default
	 *            master port.
	 * @param options no options are currently defined.
	 * @param bsdName the BSD name, as a C-string.
	 * @return ({@code CF_RETURNS_RETAINED}) The matching dictionary created, is
	 *         returned on success, or zero on failure. The dictionary is
	 *         commonly passed to {@link #IOServiceGetMatchingServices} or
	 *         {@code IOServiceAddNotification} which will consume a reference,
	 *         otherwise it should be released with
	 *         {@link CoreFoundation#CFRelease} by the caller.
	 */
	CFMutableDictionaryRef IOBSDNameMatching(MachPortT masterPort, int options, String bsdName);

	/**
	 * Create a matching dictionary that specifies an {@code IOService} match
	 * based on a registry entry ID.
	 * <p>
	 * This function creates a matching dictionary that will match a registered,
	 * active {@code IOService} found with the given registry entry ID. The
	 * entry ID for a registry entry is returned by
	 * {@link #IORegistryEntryGetRegistryEntryID}.
	 *
	 * @param entryID the registry entry ID to be found.
	 * @return ({@code CF_RETURNS_RETAINED}) The matching dictionary created, is
	 *         returned on success, or zero on failure. The dictionary is
	 *         commonly passed to {@link #IOServiceGetMatchingServices} or
	 *         {@code IOServiceAddNotification} which will consume a reference,
	 *         otherwise it should be released with
	 *         {@link CoreFoundation#CFRelease} by the caller.
	 */
	CFMutableDictionaryRef IORegistryEntryIDMatching(long entryID);
}
