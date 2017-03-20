package net.pms.io.iokit;

import net.pms.io.iokit.CoreFoundation.CFDictionaryRef;
import net.pms.io.iokit.CoreFoundation.CFStringRef;
import net.pms.io.iokit.CoreFoundation.CFTypeRef;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

/**
 * Mapping of IOKit Power Assertions from {@code IOKit/pwr_mgt/IOPMLib.h}.
 *
 * @author Nadahar
 */

public interface IOKit extends Library {
    public IOKit INSTANCE = (IOKit) Native.loadLibrary("IOKit", IOKit.class);

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
	public final String kIOPMAssertPreventUserIdleSystemSleep = "PreventUserIdleSystemSleep";

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
	public final String kIOPMAssertPreventUserIdleDisplaySleep = "PreventUserIdleDisplaySleep";

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
	public final String kIOPMAssertPreventDiskIdle = "PreventDiskIdle";

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
	public final String kIOPMAssertNetworkClientActive = "NetworkClientActive";

	/** This value represents a non-initialized assertion ID */
	public final int kIOPMNullAssertionID = 0;

	/** Level for a disabled assertion */
	public final int kIOPMAssertionLevelOff = 0;

	/** Level for an enabled assertion */
	public final int kIOPMAssertionLevelOn = 255;

	/** User is local on the system */
	public final int kIOPMUserActiveLocal = 0;

	/** Remote User connected to the system */
	public final int kIOPMUserActiveRemote = 1;

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
	public int IOPMAssertionCreateWithDescription(CFStringRef assertionType, CFStringRef name, CFStringRef details,
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
	public int IOPMAssertionCreateWithProperties(
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
	public int IOPMAssertionDeclareUserActivity(
		CFStringRef assertionName,
		int userType,
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
	public int IOPMDeclareNetworkClientActivity(
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
	public void IOPMAssertionRetain(int theAssertion);

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
	public int IOPMAssertionRelease(IntByReference assertionID);

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
	public CFDictionaryRef IOPMAssertionCopyProperties(int theAssertion);

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
	public int IOPMAssertionSetProperty(
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
	public int IOPMCopyAssertionsByProcess(CFDictionaryRef assertionsByPID);

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
	public int IOPMCopyAssertionsStatus(CFDictionaryRef assertionsStatus);

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
	IOReturn IOPMAssertionCreate(
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
	public int IOPMAssertionCreateWithName(
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
	public final String kIOPMAssertionTimeoutKey = "TimeoutSeconds";

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
	public final String kIOPMAssertionTimeoutActionKey = "TimeoutAction";

	/**
	 * A potential value for {@link #kIOPMAssertionTimeoutActionKey}.
	 *
	 * When this timeout action is specified, PM will log the timeout event but
	 * will not turn off or affect the setting of the assertion in any way.
	 */
	public final String kIOPMAssertionTimeoutActionLog = "TimeoutActionLog";

	/**
	 * When a timeout expires with this action, Power Management will log the
	 * timeout event, and will set the assertion's level to
	 * {@link #kIOPMAssertionLevelOff}.
	 */
	public final String kIOPMAssertionTimeoutActionTurnOff = "TimeoutActionTurnOff";

	/**
	 * When a timeout expires with this action, Power Management will log the
	 * timeout event, and will release the assertion.
	 */
	public final String kIOPMAssertionTimeoutActionRelease = "TimeoutActionRelease";

	/**
	 * Reflects the {@link CoreFoundation}-style retain count on this assertion.
	 * Creating or retaining an assertion increments its retain count. Release
	 * an assertion decrements its retain count. When the retain count
	 * decrements to zero, the OS will destroy the object.
	 * <p>
	 * This key can be found in the dictionary returned from
	 * {@link #IOPMAssertionCopyProperties}.
	 */
	public final String kIOPMAssertionRetainCountKey = "RetainCount";

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
	public final String kIOPMAssertionNameKey = "AssertName";

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
	public final String kIOPMAssertionDetailsKey = "Details";

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
	public final String kIOPMAssertionHumanReadableReasonKey = "HumanReadableReason";

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
	public final String kIOPMAssertionLocalizationBundlePathKey = "BundlePath";

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
	public final String kIOPMAssertionFrameworkIDKey = "FrameworkBundleID";

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
	public final String kIOPMAssertionPlugInIDKey = "PlugInBundleID";

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
	public final String kIOPMAssertionTypeKey = "AssertType";

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
	public final String kIOPMAssertionLevelKey = "AssertLevel";

	/**
	 * This assertion type is identical to
	 * {@link #kIOPMAssertPreventUserIdleSystemSleep} Please use that instead.
	 */
	public final String kIOPMAssertionTypePreventUserIdleSystemSleep = kIOPMAssertPreventUserIdleSystemSleep;

	/**
	 * This assertion type is identical to
	 * {@link #kIOPMAssertPreventUserIdleDisplaySleep} Please use that instead.
	 */
	public final String kIOPMAssertionTypePreventUserIdleDisplaySleep = kIOPMAssertPreventUserIdleDisplaySleep;


}
