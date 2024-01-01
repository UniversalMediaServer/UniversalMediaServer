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
package net.pms.platform.windows;

import javax.annotation.Nullable;

/**
 * An {@code enum} representing the Windows NTStatus error codes.
 *
 * @author Nadahar
 */
public enum NTStatus {

	/** The operation completed successfully. */
	STATUS_SUCCESS(0x00000000L, "The operation completed successfully."),

	/**
	 * The caller specified WaitAny for WaitType and one of the dispatcher
	 * objects in the Object array has been set to the signaled state.
	 */
	STATUS_WAIT_0(0x00000000L,
		"The caller specified WaitAny for WaitType and one of the dispatcher " +
		"objects in the Object array has been set to the signaled state."),

	/**
	 * The caller specified WaitAny for WaitType and one of the dispatcher
	 * objects in the Object array has been set to the signaled state.
	 */
	STATUS_WAIT_1(0x00000001L,
		"The caller specified WaitAny for WaitType and one of the dispatcher " +
		"objects in the Object array has been set to the signaled state."),

	/**
	 * The caller specified WaitAny for WaitType and one of the dispatcher
	 * objects in the Object array has been set to the signaled state.
	 */
	STATUS_WAIT_2(0x00000002L,
		"The caller specified WaitAny for WaitType and one of the dispatcher " +
		"objects in the Object array has been set to the signaled state."),

	/**
	 * The caller specified WaitAny for WaitType and one of the dispatcher
	 * objects in the Object array has been set to the signaled state.
	 */
	STATUS_WAIT_3(0x00000003L,
		"The caller specified WaitAny for WaitType and one of the dispatcher " +
		"objects in the Object array has been set to the signaled state."),

	/**
	 * The caller specified WaitAny for WaitType and one of the dispatcher
	 * objects in the Object array has been set to the signaled state.
	 */
	STATUS_WAIT_63(0x0000003FL,
		"The caller specified WaitAny for WaitType and one of the dispatcher " +
		"objects in the Object array has been set to the signaled state."),

	/** The caller attempted to wait for a mutex that has been abandoned. */
	STATUS_ABANDONED(0x00000080L, "The caller attempted to wait for a mutex that has been abandoned."),

	/** The caller attempted to wait for a mutex that has been abandoned. */
	STATUS_ABANDONED_WAIT_0(0x00000080L, "The caller attempted to wait for a mutex that has been abandoned."),

	/** The caller attempted to wait for a mutex that has been abandoned. */
	STATUS_ABANDONED_WAIT_63(0x000000BFL, "The caller attempted to wait for a mutex that has been abandoned."),

	/** A user-mode APC was delivered before the given Interval expired. */
	STATUS_USER_APC(0x000000C0L, "A user-mode APC was delivered before the given Interval expired."),

	/** The delay completed because the thread was alerted. */
	STATUS_ALERTED(0x00000101L, "The delay completed because the thread was alerted."),

	/** The given Timeout interval expired. */
	STATUS_TIMEOUT(0x00000102L, "The given Timeout interval expired."),

	/** The operation that was requested is pending completion. */
	STATUS_PENDING(0x00000103L, "The operation that was requested is pending completion."),

	/**
	 * A reparse should be performed by the Object Manager because the name of
	 * the file resulted in a symbolic link.
	 */
	STATUS_REPARSE(0x00000104L,
		"A reparse should be performed by the Object Manager because the name of the file resulted in a symbolic link."),

	/**
	 * Returned by enumeration APIs to indicate more information is available to
	 * successive calls.
	 */
	STATUS_MORE_ENTRIES(0x00000105L, "Returned by enumeration APIs to indicate more information is available to successive calls."),

	/**
	 * Indicates not all privileges or groups that are referenced are assigned
	 * to the caller. This allows, for example, all privileges to be disabled
	 * without having to know exactly which privileges are assigned.
	 */
	STATUS_NOT_ALL_ASSIGNED(
		0x00000106L,
		"Indicates not all privileges or groups that are referenced are assigned to the caller. " +
		"This allows, for example, all privileges to be disabled without having to know exactly " +
		"which privileges are assigned."),

	/** Some of the information to be translated has not been translated. */
	STATUS_SOME_NOT_MAPPED(0x00000107L, "Some of the information to be translated has not been translated."),

	/**
	 * An open/create operation completed while an opportunistic lock (oplock)
	 * break is underway.
	 */
	STATUS_OPLOCK_BREAK_IN_PROGRESS(0x00000108L,
		"An open/create operation completed while an opportunistic lock (oplock) break is underway."),

	/** A new volume has been mounted by a file system. */
	STATUS_VOLUME_MOUNTED(0x00000109L, "A new volume has been mounted by a file system."),

	/**
	 * This success level status indicates that the transaction state already
	 * exists for the registry subtree but that a transaction commit was
	 * previously aborted. The commit has now been completed.
	 */
	STATUS_RXACT_COMMITTED(
		0x0000010AL,
		"This success level status indicates that the transaction state already exists for the " +
		"registry subtree but that a transaction commit was previously aborted. The commit has " +
		"now been completed."),

	/**
	 * Indicates that a notify change request has been completed due to closing
	 * the handle that made the notify change request.
	 */
	STATUS_NOTIFY_CLEANUP(0x0000010BL,
		"Indicates that a notify change request has been completed due to closing the handle that made the notify change request."),

	/**
	 * Indicates that a notify change request is being completed and that the
	 * information is not being returned in the caller's buffer. The caller now
	 * needs to enumerate the files to find the changes.
	 */
	STATUS_NOTIFY_ENUM_DIR(
		0x0000010CL,
		"Indicates that a notify change request is being completed and that the information is not being " +
		"returned in the caller's buffer. The caller now needs to enumerate the files to find the changes."),

	/**
	 * {No Quotas} No system quota limits are specifically set for this account.
	 */
	STATUS_NO_QUOTAS_FOR_ACCOUNT(0x0000010DL, "{No Quotas} No system quota limits are specifically set for this account."),

	/**
	 * {Connect Failure on Primary Transport} An attempt was made to connect to
	 * the remote server %hs on the primary transport, but the connection
	 * failed. The computer WAS able to connect on a secondary transport.
	 */
	STATUS_PRIMARY_TRANSPORT_CONNECT_FAILED(
		0x0000010EL,
		"{Connect Failure on Primary Transport} An attempt was made to connect to the remote server %hs on " +
		"the primary transport, but the connection failed. The computer WAS able to connect on a secondary transport."),

	/** The page fault was a transition fault. */
	STATUS_PAGE_FAULT_TRANSITION(0x00000110L, "The page fault was a transition fault."),

	/** The page fault was a demand zero fault. */
	STATUS_PAGE_FAULT_DEMAND_ZERO(0x00000111L, "The page fault was a demand zero fault."),

	/** The page fault was a demand zero fault. */
	STATUS_PAGE_FAULT_COPY_ON_WRITE(0x00000112L, "The page fault was a demand zero fault."),

	/** The page fault was a demand zero fault. */
	STATUS_PAGE_FAULT_GUARD_PAGE(0x00000113L, "The page fault was a demand zero fault."),

	/** The page fault was satisfied by reading from a secondary storage device. */
	STATUS_PAGE_FAULT_PAGING_FILE(0x00000114L, "The page fault was satisfied by reading from a secondary storage device."),

	/** The cached page was locked during operation. */
	STATUS_CACHE_PAGE_LOCKED(0x00000115L, "The cached page was locked during operation."),

	/** The crash dump exists in a paging file. */
	STATUS_CRASH_DUMP(0x00000116L, "The crash dump exists in a paging file."),

	/** The specified buffer contains all zeros. */
	STATUS_BUFFER_ALL_ZEROS(0x00000117L, "The specified buffer contains all zeros."),

	/**
	 * A reparse should be performed by the Object Manager because the name of
	 * the file resulted in a symbolic link.
	 */
	STATUS_REPARSE_OBJECT(0x00000118L,
		"A reparse should be performed by the Object Manager because the name of the file resulted in a symbolic link."),

	/**
	 * The device has succeeded a query-stop and its resource requirements have
	 * changed.
	 */
	STATUS_RESOURCE_REQUIREMENTS_CHANGED(0x00000119L, "The device has succeeded a query-stop and its resource requirements have changed."),

	/**
	 * The translator has translated these resources into the global space and
	 * no additional translations should be performed.
	 */
	STATUS_TRANSLATION_COMPLETE(0x00000120L,
		"The translator has translated these resources into the global space and no additional translations should be performed."),

	/**
	 * The directory service evaluated group memberships locally, because it was
	 * unable to contact a global catalog server.
	 */
	STATUS_DS_MEMBERSHIP_EVALUATED_LOCALLY(0x00000121L,
		"The directory service evaluated group memberships locally, because it was unable to contact a global catalog server."),

	/** A process being terminated has no threads to terminate. */
	STATUS_NOTHING_TO_TERMINATE(0x00000122L, "A process being terminated has no threads to terminate."),

	/** The specified process is not part of a job. */
	STATUS_PROCESS_NOT_IN_JOB(0x00000123L, "The specified process is not part of a job."),

	/** The specified process is part of a job. */
	STATUS_PROCESS_IN_JOB(0x00000124L, "The specified process is part of a job."),

	/** {Volume Shadow Copy Service} The system is now ready for hibernation. */
	STATUS_VOLSNAP_HIBERNATE_READY(0x00000125L, "{Volume Shadow Copy Service} The system is now ready for hibernation."),

	/**
	 * A file system or file system filter driver has successfully completed an
	 * FsFilter operation.
	 */
	STATUS_FSFILTER_OP_COMPLETED_SUCCESSFULLY(0x00000126L,
		"A file system or file system filter driver has successfully completed an FsFilter operation."),

	/** The specified interrupt vector was already connected. */
	STATUS_INTERRUPT_VECTOR_ALREADY_CONNECTED(0x00000127L, "The specified interrupt vector was already connected."),

	/** The specified interrupt vector is still connected. */
	STATUS_INTERRUPT_STILL_CONNECTED(0x00000128L, "The specified interrupt vector is still connected."),

	/** The current process is a cloned process. */
	STATUS_PROCESS_CLONED(0x00000129L, "The current process is a cloned process."),

	/** The file was locked and all users of the file can only read. */
	STATUS_FILE_LOCKED_WITH_ONLY_READERS(0x0000012AL, "The file was locked and all users of the file can only read."),

	/** The file was locked and at least one user of the file can write. */
	STATUS_FILE_LOCKED_WITH_WRITERS(0x0000012BL, "The file was locked and at least one user of the file can write."),

	/**
	 * The specified ResourceManager made no changes or updates to the resource
	 * under this transaction.
	 */
	STATUS_RESOURCEMANAGER_READ_ONLY(0x00000202L,
		"The specified ResourceManager made no changes or updates to the resource under this transaction."),

	/** An operation is blocked and waiting for an oplock. */
	STATUS_WAIT_FOR_OPLOCK(0x00000367L, "An operation is blocked and waiting for an oplock."),

	/** Debugger handled the exception. */
	DBG_EXCEPTION_HANDLED(0x00010001L, "Debugger handled the exception."),

	/** The debugger continued. */
	DBG_CONTINUE(0x00010002L, "The debugger continued."),

	/** The IO was completed by a filter. */
	STATUS_FLT_IO_COMPLETE(0x001C0001L, "The IO was completed by a filter."),

	/** The file is temporarily unavailable. */
	STATUS_FILE_NOT_AVAILABLE(0xC0000467L, "The file is temporarily unavailable."),

	/** The share is temporarily unavailable. */
	STATUS_SHARE_UNAVAILABLE(0xC0000480L, "The share is temporarily unavailable."),

	/**
	 * A threadpool worker thread entered a callback at thread affinity %p and
	 * exited at affinity %p.
	 * <p>
	 * This is unexpected, indicating that the callback missed restoring the
	 * priority.
	 */
	STATUS_CALLBACK_RETURNED_THREAD_AFFINITY(
		0xC0000721L,
		"A threadpool worker thread entered a callback at thread affinity %p and exited at affinity %p.\n\n" +
		"This is unexpected, indicating that the callback missed restoring the priority."),

	/**
	 * {Object Exists} An attempt was made to create an object but the object
	 * name already exists.
	 */
	STATUS_OBJECT_NAME_EXISTS(0x40000000L, "{Object Exists} An attempt was made to create an object but the object name already exists."),

	/**
	 * {Thread Suspended} A thread termination occurred while the thread was
	 * suspended. The thread resumed, and termination proceeded.
	 */
	STATUS_THREAD_WAS_SUSPENDED(0x40000001L,
		"{Thread Suspended} A thread termination occurred while the thread was suspended. The thread resumed, and termination proceeded."),

	/**
	 * {Working Set Range Error} An attempt was made to set the working set
	 * minimum or maximum to values that are outside the allowable range.
	 */
	STATUS_WORKING_SET_LIMIT_RANGE(0x40000002L,
		"{Working Set Range Error} An attempt was made to set the working set " +
		"minimum or maximum to values that are outside the allowable range."),

	/**
	 * {Image Relocated} An image file could not be mapped at the address that
	 * is specified in the image file. Local fixes must be performed on this
	 * image.
	 */
	STATUS_IMAGE_NOT_AT_BASE(
		0x40000003L,
		"{Image Relocated} An image file could not be mapped at the address that is " +
		"specified in the image file. Local fixes must be performed on this image."),

	/**
	 * This informational level status indicates that a specified registry
	 * subtree transaction state did not yet exist and had to be created.
	 */
	STATUS_RXACT_STATE_CREATED(0x40000004L,
		"This informational level status indicates that a specified registry " +
		"subtree transaction state did not yet exist and had to be created."),

	/**
	 * {Segment Load} A virtual DOS machine (VDM) is loading, unloading, or
	 * moving an MS-DOS or Win16 program segment image. An exception is raised
	 * so that a debugger can load, unload, or track symbols and breakpoints
	 * within these 16-bit segments.
	 */
	STATUS_SEGMENT_NOTIFICATION(
		0x40000005L,
		"{Segment Load} A virtual DOS machine (VDM) is loading, unloading, or moving an MS-DOS " +
		"or Win16 program segment image. An exception is raised so that a debugger can load, " +
		"unload, or track symbols and breakpoints within these 16-bit segments."),

	/**
	 * {Local Session Key} A user session key was requested for a local remote
	 * procedure call (RPC) connection. The session key that is returned is a
	 * constant value and not unique to this connection.
	 */
	STATUS_LOCAL_USER_SESSION_KEY(
		0x40000006L,
		"{Local Session Key} A user session key was requested for a local remote procedure " +
		"call (RPC) connection. The session key that is returned is a constant value and not " +
		"unique to this connection."),

	/**
	 * {Invalid Current Directory} The process cannot switch to the startup
	 * current directory %hs. Select OK to set the current directory to %hs, or
	 * select CANCEL to exit.
	 */
	STATUS_BAD_CURRENT_DIRECTORY(
		0x40000007L,
		"{Invalid Current Directory} The process cannot switch to the startup current directory %hs. " +
		"Select OK to set the current directory to %hs, or select CANCEL to exit."),

	/**
	 * {Serial IOCTL Complete} A serial I/O operation was completed by another
	 * write to a serial port. (The IOCTL_SERIAL_XOFF_COUNTER reached zero.)
	 */
	STATUS_SERIAL_MORE_WRITES(
		0x40000008L,
		"{Serial IOCTL Complete} A serial I/O operation was completed by another " +
		"write to a serial port. (The IOCTL_SERIAL_XOFF_COUNTER reached zero.)"),

	/**
	 * {Registry Recovery} One of the files that contains the system registry
	 * data had to be recovered by using a log or alternate copy. The recovery
	 * was successful.
	 */
	STATUS_REGISTRY_RECOVERED(
		0x40000009L,
		"{Registry Recovery} One of the files that contains the system registry data " +
		"had to be recovered by using a log or alternate copy. The recovery was successful."),

	/**
	 * {Redundant Read} To satisfy a read request, the Windows NT operating
	 * system fault-tolerant file system successfully read the requested data
	 * from a redundant copy. This was done because the file system encountered a
	 * failure on a member of the fault-tolerant volume but was unable to
	 * reassign the failing area of the device.
	 */
	STATUS_FT_READ_RECOVERY_FROM_BACKUP(
		0x4000000AL,
		"{Redundant Read} To satisfy a read request, the Windows NT operating system " +
		"fault-tolerant file system successfully read the requested data from a redundant copy. " +
		"This was done because the file system encountered a failure on a member of the " +
		"fault-tolerant volume but was unable to reassign the failing area of the device."),

	/**
	 * {Redundant Write} To satisfy a write request, the Windows NT
	 * fault-tolerant file system successfully wrote a redundant copy of the
	 * information. This was done because the file system encountered a failure
	 * on a member of the fault-tolerant volume but was unable to reassign the
	 * failing area of the device.
	 */
	STATUS_FT_WRITE_RECOVERY(
		0x4000000BL,
		"{Redundant Write} To satisfy a write request, the Windows NT fault-tolerant file " +
		"system successfully wrote a redundant copy of the information. This was done because " +
		"the file system encountered a failure on a member of the fault-tolerant volume but " +
		"was unable to reassign the failing area of the device."),

	/**
	 * {Serial IOCTL Timeout} A serial I/O operation completed because the
	 * time-out period expired. (The IOCTL_SERIAL_XOFF_COUNTER had not reached
	 * zero.)
	 */
	STATUS_SERIAL_COUNTER_TIMEOUT(
		0x4000000CL,
		"{Serial IOCTL Timeout} A serial I/O operation completed because the " +
		"time-out period expired. (The IOCTL_SERIAL_XOFF_COUNTER had not reached zero.)"),

	/**
	 * {Password Too Complex} The Windows password is too complex to be
	 * converted to a LAN Manager password. The LAN Manager password that
	 * returned is a NULL string.
	 */
	STATUS_NULL_LM_PASSWORD(
		0x4000000DL,
		"{Password Too Complex} The Windows password is too complex to be converted to a LAN " +
		"Manager password. The LAN Manager password that returned is a NULL string."),

	/**
	 * {Machine Type Mismatch} The image file %hs is valid but is for a machine
	 * type other than the current machine. Select OK to continue, or CANCEL to
	 * fail the DLL load.
	 */
	STATUS_IMAGE_MACHINE_TYPE_MISMATCH(
		0x4000000EL,
		"{Machine Type Mismatch} The image file %hs is valid but is for a machine type other " +
		"than the current machine. Select OK to continue, or CANCEL to fail the DLL load."),

	/**
	 * {Partial Data Received} The network transport returned partial data to
	 * its client. The remaining data will be sent later.
	 */
	STATUS_RECEIVE_PARTIAL(0x4000000FL,
		"{Partial Data Received} The network transport returned partial data to its client. The remaining data will be sent later."),

	/**
	 * {Expedited Data Received} The network transport returned data to its
	 * client that was marked as expedited by the remote system.
	 */
	STATUS_RECEIVE_EXPEDITED(0x40000010L,
		"{Expedited Data Received} The network transport returned data to its client that was marked as expedited by the remote system."),

	/**
	 * {Partial Expedited Data Received} The network transport returned partial
	 * data to its client and this data was marked as expedited by the remote
	 * system. The remaining data will be sent later.
	 */
	STATUS_RECEIVE_PARTIAL_EXPEDITED(
		0x40000011L,
		"{Partial Expedited Data Received} The network transport returned partial data to " +
		"its client and this data was marked as expedited by the remote system. " +
		"The remaining data will be sent later."),

	/** {TDI Event Done} The TDI indication has completed successfully. */
	STATUS_EVENT_DONE(0x40000012L, "{TDI Event Done} The TDI indication has completed successfully."),

	/** {TDI Event Pending} The TDI indication has entered the pending state. */
	STATUS_EVENT_PENDING(0x40000013L, "{TDI Event Pending} The TDI indication has entered the pending state."),

	/** Checking file system on %wZ. */
	STATUS_CHECKING_FILE_SYSTEM(0x40000014L, "Checking file system on %wZ."),

	/** {Fatal Application Exit} %hs */
	STATUS_FATAL_APP_EXIT(0x40000015L, "{Fatal Application Exit} %hs"),

	/** The specified registry key is referenced by a predefined handle. */
	STATUS_PREDEFINED_HANDLE(0x40000016L, "The specified registry key is referenced by a predefined handle."),

	/**
	 * {Page Unlocked} The page protection of a locked page was changed to 'No
	 * Access' and the page was unlocked from memory and from the process.
	 */
	STATUS_WAS_UNLOCKED(
		0x40000017L,
		"{Page Unlocked} The page protection of a locked page was changed to 'No Access' " +
		"and the page was unlocked from memory and from the process."),

	/** %hs */
	STATUS_SERVICE_NOTIFICATION(0x40000018L, "%hs"),

	/** {Page Locked} One of the pages to lock was already locked. */
	STATUS_WAS_LOCKED(0x40000019L, "{Page Locked} One of the pages to lock was already locked."),

	/** Application popup: %1 : %2 */
	STATUS_LOG_HARD_ERROR(0x4000001AL, "Application popup: %1 : %2"),

	/** A Win32 process already exists. */
	STATUS_ALREADY_WIN32(0x4000001BL, "A Win32 process already exists."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_UNSIMULATE(0x4000001CL, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_CONTINUE(0x4000001DL, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_SINGLE_STEP(0x4000001EL, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_BREAKPOINT(0x4000001FL, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_EXCEPTION_CONTINUE(0x40000020L, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_EXCEPTION_LASTCHANCE(0x40000021L, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_EXCEPTION_CHAIN(0x40000022L, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * {Machine Type Mismatch} The image file %hs is valid but is for a machine
	 * type other than the current machine.
	 */
	STATUS_IMAGE_MACHINE_TYPE_MISMATCH_EXE(0x40000023L,
		"{Machine Type Mismatch} The image file %hs is valid but is for a machine type other than the current machine."),

	/** A yield execution was performed and no thread was available to run. */
	STATUS_NO_YIELD_PERFORMED(0x40000024L, "A yield execution was performed and no thread was available to run."),

	/** The resume flag to a timer API was ignored. */
	STATUS_TIMER_RESUME_IGNORED(0x40000025L, "The resume flag to a timer API was ignored."),

	/** The arbiter has deferred arbitration of these resources to its parent. */
	STATUS_ARBITRATION_UNHANDLED(0x40000026L, "The arbiter has deferred arbitration of these resources to its parent."),

	/** The device has detected a CardBus card in its slot. */
	STATUS_CARDBUS_NOT_SUPPORTED(0x40000027L, "The device has detected a CardBus card in its slot."),

	/**
	 * An exception status code that is used by the Win32 x86 emulation
	 * subsystem.
	 */
	STATUS_WX86_CREATEWX86TIB(0x40000028L, "An exception status code that is used by the Win32 x86 emulation subsystem."),

	/**
	 * The CPUs in this multiprocessor system are not all the same revision
	 * level. To use all processors, the operating system restricts itself to
	 * the features of the least capable processor in the system. If problems
	 * occur with this system, contact the CPU manufacturer to see if this mix of
	 * processors is supported.
	 */
	STATUS_MP_PROCESSOR_MISMATCH(
		0x40000029L,
		"The CPUs in this multiprocessor system are not all the same revision level. " +
		"To use all processors, the operating system restricts itself to the features of " +
		"the least capable processor in the system. If problems occur with this system, " +
		"contact the CPU manufacturer to see if this mix of processors is supported."),

	/** The system was put into hibernation. */
	STATUS_HIBERNATED(0x4000002AL, "The system was put into hibernation."),

	/** The system was resumed from hibernation. */
	STATUS_RESUME_HIBERNATION(0x4000002BL, "The system was resumed from hibernation."),

	/**
	 * Windows has detected that the system firmware (BIOS) was updated
	 * [previous firmware date = %2, current firmware date %3].
	 */
	STATUS_FIRMWARE_UPDATED(0x4000002CL,
		"Windows has detected that the system firmware (BIOS) was updated [previous firmware date = %2, current firmware date %3]."),

	/**
	 * A device driver is leaking locked I/O pages and is causing system
	 * degradation. The system has automatically enabled the tracking code to
	 * try and catch the culprit.
	 */
	STATUS_DRIVERS_LEAKING_LOCKED_PAGES(
		0x4000002DL,
		"A device driver is leaking locked I/O pages and is causing system degradation. " +
		"The system has automatically enabled the tracking code to try and catch the culprit."),

	/**
	 * The ALPC message being canceled has already been retrieved from the queue
	 * on the other side.
	 */
	STATUS_MESSAGE_RETRIEVED(0x4000002EL, "The ALPC message being canceled has already been retrieved from the queue on the other side."),

	/** The system power state is transitioning from %2 to %3. */
	STATUS_SYSTEM_POWERSTATE_TRANSITION(0x4000002FL, "The system power state is transitioning from %2 to %3."),

	/**
	 * The receive operation was successful. Check the ALPC completion list for
	 * the received message.
	 */
	STATUS_ALPC_CHECK_COMPLETION_LIST(0x40000030L,
		"The receive operation was successful. Check the ALPC completion list for the received message."),

	/**
	 * The system power state is transitioning from %2 to %3 but could enter %4.
	 */
	STATUS_SYSTEM_POWERSTATE_COMPLEX_TRANSITION(0x40000031L, "The system power state is transitioning from %2 to %3 but could enter %4."),

	/** Access to %1 is monitored by policy rule %2. */
	STATUS_ACCESS_AUDIT_BY_POLICY(0x40000032L, "Access to %1 is monitored by policy rule %2."),

	/** A valid hibernation file has been invalidated and should be abandoned. */
	STATUS_ABANDON_HIBERFILE(0x40000033L, "A valid hibernation file has been invalidated and should be abandoned."),

	/** Business rule scripts are disabled for the calling application. */
	STATUS_BIZRULES_NOT_ENABLED(0x40000034L, "Business rule scripts are disabled for the calling application."),

	/** The system has awoken. */
	STATUS_WAKE_SYSTEM(0x40000294L, "The system has awoken."),

	/** The directory service is shutting down. */
	STATUS_DS_SHUTTING_DOWN(0x40000370L, "The directory service is shutting down."),

	/** Debugger will reply later. */
	DBG_REPLY_LATER(0x40010001L, "Debugger will reply later."),

	/** Debugger cannot provide a handle. */
	DBG_UNABLE_TO_PROVIDE_HANDLE(0x40010002L, "Debugger cannot provide a handle."),

	/** Debugger terminated the thread. */
	DBG_TERMINATE_THREAD(0x40010003L, "Debugger terminated the thread."),

	/** Debugger terminated the process. */
	DBG_TERMINATE_PROCESS(0x40010004L, "Debugger terminated the process."),

	/** Debugger obtained control of C. */
	DBG_CONTROL_C(0x40010005L, "Debugger obtained control of C."),

	/** Debugger printed an exception on control C. */
	DBG_PRINTEXCEPTION_C(0x40010006L, "Debugger printed an exception on control C."),

	/** Debugger received a RIP exception. */
	DBG_RIPEXCEPTION(0x40010007L, "Debugger received a RIP exception."),

	/** Debugger received a control break. */
	DBG_CONTROL_BREAK(0x40010008L, "Debugger received a control break."),

	/** Debugger command communication exception. */
	DBG_COMMAND_EXCEPTION(0x40010009L, "Debugger command communication exception."),

	/** A UUID that is valid only on this computer has been allocated. */
	RPC_NT_UUID_LOCAL_ONLY(0x40020056L, "A UUID that is valid only on this computer has been allocated."),

	/** Some data remains to be sent in the request buffer. */
	RPC_NT_SEND_INCOMPLETE(0x400200AFL, "Some data remains to be sent in the request buffer."),

	/** The Client Drive Mapping Service has connected on Terminal Connection. */
	STATUS_CTX_CDM_CONNECT(0x400A0004L, "The Client Drive Mapping Service has connected on Terminal Connection."),

	/**
	 * The Client Drive Mapping Service has disconnected on Terminal Connection.
	 */
	STATUS_CTX_CDM_DISCONNECT(0x400A0005L, "The Client Drive Mapping Service has disconnected on Terminal Connection."),

	/**
	 * A kernel mode component is releasing a reference on an activation
	 * context.
	 */
	STATUS_SXS_RELEASE_ACTIVATION_CONTEXT(0x4015000DL, "A kernel mode component is releasing a reference on an activation context."),

	/**
	 * The transactional resource manager is already consistent. Recovery is not
	 * needed.
	 */
	STATUS_RECOVERY_NOT_NEEDED(0x40190034L, "The transactional resource manager is already consistent. Recovery is not needed."),

	/** The transactional resource manager has already been started. */
	STATUS_RM_ALREADY_STARTED(0x40190035L, "The transactional resource manager has already been started."),

	/** The log service encountered a log stream with no restart area. */
	STATUS_LOG_NO_RESTART(0x401A000CL, "The log service encountered a log stream with no restart area."),

	/**
	 * {Display Driver Recovered From Failure} The %hs display driver has
	 * detected a failure and recovered from it. Some graphical operations might
	 * have failed. The next time you restart the machine, a dialog box appears,
	 * giving you an opportunity to upload data about this failure to Microsoft.
	 */
	STATUS_VIDEO_DRIVER_DEBUG_REPORT_REQUEST(
		0x401B00ECL,
		"{Display Driver Recovered From Failure} The %hs display driver has detected a " +
		"failure and recovered from it. Some graphical operations might have failed. The " +
		"next time you restart the machine, a dialog box appears, giving you an opportunity " +
		"to upload data about this failure to Microsoft."),

	/**
	 * The specified buffer is not big enough to contain the entire requested
	 * dataset. Partial data is populated up to the size of the buffer.
	 * <p>
	 * The caller needs to provide a buffer of the size as specified in the
	 * partially populated buffer's content (interface specific).
	 */
	STATUS_GRAPHICS_PARTIAL_DATA_POPULATED(
		0x401E000AL,
		"The specified buffer is not big enough to contain the entire requested dataset. " +
		"Partial data is populated up to the size of the buffer.\n\nThe caller needs to " +
		"provide a buffer of the size as specified in the partially populated buffer's " +
		"content (interface specific)."),

	/**
	 * The kernel driver detected a version mismatch between it and the user
	 * mode driver.
	 */
	STATUS_GRAPHICS_DRIVER_MISMATCH(0x401E0117L, "The kernel driver detected a version mismatch between it and the user mode driver."),

	/** No mode is pinned on the specified VidPN source/target. */
	STATUS_GRAPHICS_MODE_NOT_PINNED(0x401E0307L, "No mode is pinned on the specified VidPN source/target."),

	/**
	 * The specified mode set does not specify a preference for one of its
	 * modes.
	 */
	STATUS_GRAPHICS_NO_PREFERRED_MODE(0x401E031EL, "The specified mode set does not specify a preference for one of its modes."),

	/**
	 * The specified dataset (for example, mode set, frequency range set,
	 * descriptor set, or topology) is empty.
	 */
	STATUS_GRAPHICS_DATASET_IS_EMPTY(0x401E034BL,
		"The specified dataset (for example, mode set, frequency range set, descriptor set, or topology) is empty."),

	/**
	 * The specified dataset (for example, mode set, frequency range set,
	 * descriptor set, or topology) does not contain any more elements.
	 */
	STATUS_GRAPHICS_NO_MORE_ELEMENTS_IN_DATASET(0x401E034CL,
		"The specified dataset (for example, mode set, frequency range set, " +
		"descriptor set, or topology) does not contain any more elements."),

	/**
	 * The specified content transformation is not pinned on the specified VidPN
	 * present path.
	 */
	STATUS_GRAPHICS_PATH_CONTENT_GEOMETRY_TRANSFORMATION_NOT_PINNED(0x401E0351L,
		"The specified content transformation is not pinned on the specified VidPN present path."),

	/** The child device presence was not reliably detected. */
	STATUS_GRAPHICS_UNKNOWN_CHILD_STATUS(0x401E042FL, "The child device presence was not reliably detected."),

	/**
	 * Starting the lead adapter in a linked configuration has been temporarily
	 * deferred.
	 */
	STATUS_GRAPHICS_LEADLINK_START_DEFERRED(0x401E0437L,
		"Starting the lead adapter in a linked configuration has been temporarily deferred."),

	/**
	 * The display adapter is being polled for children too frequently at the
	 * same polling level.
	 */
	STATUS_GRAPHICS_POLLING_TOO_FREQUENTLY(0x401E0439L,
		"The display adapter is being polled for children too frequently at the same polling level."),

	/** Starting the adapter has been temporarily deferred. */
	STATUS_GRAPHICS_START_DEFERRED(0x401E043AL, "Starting the adapter has been temporarily deferred."),

	/** The request will be completed later by an NDIS status indication. */
	STATUS_NDIS_INDICATION_REQUIRED(0x40230001L, "The request will be completed later by an NDIS status indication."),

	/**
	 * {EXCEPTION} Guard Page Exception A page of memory that marks the end of a
	 * data structure, such as a stack or an array, has been accessed.
	 */
	STATUS_GUARD_PAGE_VIOLATION(0x80000001L,
		"{EXCEPTION} Guard Page Exception A page of memory that marks the end " +
		"of a data structure, such as a stack or an array, has been accessed."),

	/**
	 * {EXCEPTION} Alignment Fault A data type misalignment was detected in a
	 * load or store instruction.
	 */
	STATUS_DATATYPE_MISALIGNMENT(0x80000002L,
		"{EXCEPTION} Alignment Fault A data type misalignment was detected in a load or store instruction."),

	/** {EXCEPTION} Breakpoint A breakpoint has been reached. */
	STATUS_BREAKPOINT(0x80000003L, "{EXCEPTION} Breakpoint A breakpoint has been reached."),

	/**
	 * {EXCEPTION} Single Step A single step or trace operation has just been
	 * completed.
	 */
	STATUS_SINGLE_STEP(0x80000004L, "{EXCEPTION} Single Step A single step or trace operation has just been completed."),

	/**
	 * {Buffer Overflow} The data was too large to fit into the specified
	 * buffer.
	 */
	STATUS_BUFFER_OVERFLOW(0x80000005L, "{Buffer Overflow} The data was too large to fit into the specified buffer."),

	/**
	 * {No More Files} No more files were found which match the file
	 * specification.
	 */
	STATUS_NO_MORE_FILES(0x80000006L, "{No More Files} No more files were found which match the file specification."),

	/**
	 * {Kernel Debugger Awakened} The system debugger was awakened by an
	 * interrupt.
	 */
	STATUS_WAKE_SYSTEM_DEBUGGER(0x80000007L, "{Kernel Debugger Awakened} The system debugger was awakened by an interrupt."),

	/**
	 * {Handles Closed} Handles to objects have been automatically closed
	 * because of the requested operation.
	 */
	STATUS_HANDLES_CLOSED(0x8000000AL,
		"{Handles Closed} Handles to objects have been automatically closed because of the requested operation."),

	/**
	 * {Non-Inheritable ACL} An access control list (ACL) contains no components
	 * that can be inherited.
	 */
	STATUS_NO_INHERITANCE(0x8000000BL, "{Non-Inheritable ACL} An access control list (ACL) contains no components that can be inherited."),

	/**
	 * {GUID Substitution} During the translation of a globally unique
	 * identifier (GUID) to a Windows security ID (SID), no administratively
	 * defined GUID prefix was found. A substitute prefix was used, which will
	 * not compromise system security. However, this might provide a more
	 * restrictive access than intended.
	 */
	STATUS_GUID_SUBSTITUTION_MADE(
		0x8000000CL,
		"{GUID Substitution} During the translation of a globally unique identifier " +
		"(GUID) to a Windows security ID (SID), no administratively defined GUID prefix " +
		"was found. A substitute prefix was used, which will not compromise system security. " +
		"However, this might provide a more restrictive access than intended."),

	/**
	 * Because of protection conflicts, not all the requested bytes could be
	 * copied.
	 */
	STATUS_PARTIAL_COPY(0x8000000DL, "Because of protection conflicts, not all the requested bytes could be copied."),

	/** {Out of Paper} The printer is out of paper. */
	STATUS_DEVICE_PAPER_EMPTY(0x8000000EL, "{Out of Paper} The printer is out of paper."),

	/** {Device Power Is Off} The printer power has been turned off. */
	STATUS_DEVICE_POWERED_OFF(0x8000000FL, "{Device Power Is Off} The printer power has been turned off."),

	/** {Device Offline} The printer has been taken offline. */
	STATUS_DEVICE_OFF_LINE(0x80000010L, "{Device Offline} The printer has been taken offline."),

	/** {Device Busy} The device is currently busy. */
	STATUS_DEVICE_BUSY(0x80000011L, "{Device Busy} The device is currently busy."),

	/** {No More EAs} No more extended attributes (EAs) were found for the file. */
	STATUS_NO_MORE_EAS(0x80000012L, "{No More EAs} No more extended attributes (EAs) were found for the file."),

	/**
	 * {Illegal EA} The specified extended attribute (EA) name contains at least
	 * one illegal character.
	 */
	STATUS_INVALID_EA_NAME(0x80000013L, "{Illegal EA} The specified extended attribute (EA) name contains at least one illegal character."),

	/** {Inconsistent EA List} The extended attribute (EA) list is inconsistent. */
	STATUS_EA_LIST_INCONSISTENT(0x80000014L, "{Inconsistent EA List} The extended attribute (EA) list is inconsistent."),

	/** {Invalid EA Flag} An invalid extended attribute (EA) flag was set. */
	STATUS_INVALID_EA_FLAG(0x80000015L, "{Invalid EA Flag} An invalid extended attribute (EA) flag was set."),

	/**
	 * {Verifying Disk} The media has changed and a verify operation is in
	 * progress; therefore, no reads or writes can be performed to the device,
	 * except those that are used in the verify operation.
	 */
	STATUS_VERIFY_REQUIRED(
		0x80000016L,
		"{Verifying Disk} The media has changed and a verify operation is in progress; therefore, " +
		"no reads or writes can be performed to the device, except those that are used in the verify operation."),

	/**
	 * {Too Much Information} The specified access control list (ACL) contained
	 * more information than was expected.
	 */
	STATUS_EXTRANEOUS_INFORMATION(0x80000017L,
		"{Too Much Information} The specified access control list (ACL) contained more information than was expected."),

	/**
	 * This warning level status indicates that the transaction state already
	 * exists for the registry subtree, but that a transaction commit was
	 * previously aborted. The commit has NOT been completed but has not been
	 * rolled back either; therefore, it can still be committed, if needed.
	 */
	STATUS_RXACT_COMMIT_NECESSARY(
		0x80000018L,
		"This warning level status indicates that the transaction state already exists for the " +
		"registry subtree, but that a transaction commit was previously aborted. The commit has NOT " +
		"been completed but has not been rolled back either; therefore, it can still be committed, if needed."),

	/**
	 * {No More Entries} No more entries are available from an enumeration
	 * operation.
	 */
	STATUS_NO_MORE_ENTRIES(0x8000001AL, "{No More Entries} No more entries are available from an enumeration operation."),

	/** {Filemark Found} A filemark was detected. */
	STATUS_FILEMARK_DETECTED(0x8000001BL, "{Filemark Found} A filemark was detected."),

	/** {Media Changed} The media has changed. */
	STATUS_MEDIA_CHANGED(0x8000001CL, "{Media Changed} The media has changed."),

	/** {I/O Bus Reset} An I/O bus reset was detected. */
	STATUS_BUS_RESET(0x8000001DL, "{I/O Bus Reset} An I/O bus reset was detected."),

	/** {End of Media} The end of the media was encountered. */
	STATUS_END_OF_MEDIA(0x8000001EL, "{End of Media} The end of the media was encountered."),

	/** The beginning of a tape or partition has been detected. */
	STATUS_BEGINNING_OF_MEDIA(0x8000001FL, "The beginning of a tape or partition has been detected."),

	/** {Media Changed} The media might have changed. */
	STATUS_MEDIA_CHECK(0x80000020L, "{Media Changed} The media might have changed."),

	/** A tape access reached a set mark. */
	STATUS_SETMARK_DETECTED(0x80000021L, "A tape access reached a set mark."),

	/** During a tape access, the end of the data written is reached. */
	STATUS_NO_DATA_DETECTED(0x80000022L, "During a tape access, the end of the data written is reached."),

	/** The redirector is in use and cannot be unloaded. */
	STATUS_REDIRECTOR_HAS_OPEN_HANDLES(0x80000023L, "The redirector is in use and cannot be unloaded."),

	/** The server is in use and cannot be unloaded. */
	STATUS_SERVER_HAS_OPEN_HANDLES(0x80000024L, "The server is in use and cannot be unloaded."),

	/** The specified connection has already been disconnected. */
	STATUS_ALREADY_DISCONNECTED(0x80000025L, "The specified connection has already been disconnected."),

	/** A long jump has been executed. */
	STATUS_LONGJUMP(0x80000026L, "A long jump has been executed."),

	/** A cleaner cartridge is present in the tape library. */
	STATUS_CLEANER_CARTRIDGE_INSTALLED(0x80000027L, "A cleaner cartridge is present in the tape library."),

	/** The Plug and Play query operation was not successful. */
	STATUS_PLUGPLAY_QUERY_VETOED(0x80000028L, "The Plug and Play query operation was not successful."),

	/** A frame consolidation has been executed. */
	STATUS_UNWIND_CONSOLIDATE(0x80000029L, "A frame consolidation has been executed."),

	/**
	 * {Registry Hive Recovered} The registry hive (file): %hs was corrupted and
	 * it has been recovered. Some data might have been lost.
	 */
	STATUS_REGISTRY_HIVE_RECOVERED(0x8000002AL,
		"{Registry Hive Recovered} The registry hive (file): %hs was corrupted and it has been recovered. Some data might have been lost."),

	/**
	 * The application is attempting to run executable code from the module %hs.
	 * This might be insecure. An alternative, %hs, is available. Should the
	 * application use the secure module %hs?
	 */
	STATUS_DLL_MIGHT_BE_INSECURE(
		0x8000002BL,
		"The application is attempting to run executable code from the module %hs. This might be insecure. " +
		"An alternative, %hs, is available. Should the application use the secure module %hs?"),

	/**
	 * The application is loading executable code from the module %hs. This is
	 * secure but might be incompatible with previous releases of the operating
	 * system. An alternative, %hs, is available. Should the application use the
	 * secure module %hs?
	 */
	STATUS_DLL_MIGHT_BE_INCOMPATIBLE(
		0x8000002CL,
		"The application is loading executable code from the module %hs. This is secure but might be " +
		"incompatible with previous releases of the operating system. An alternative, %hs, is available. " +
		"Should the application use the secure module %hs?"),

	/** The create operation stopped after reaching a symbolic link. */
	STATUS_STOPPED_ON_SYMLINK(0x8000002DL, "The create operation stopped after reaching a symbolic link."),

	/** The device has indicated that cleaning is necessary. */
	STATUS_DEVICE_REQUIRES_CLEANING(0x80000288L, "The device has indicated that cleaning is necessary."),

	/**
	 * The device has indicated that its door is open. Further operations
	 * require it closed and secured.
	 */
	STATUS_DEVICE_DOOR_OPEN(0x80000289L, "The device has indicated that its door is open. " +
		"Further operations require it closed and secured."),

	/**
	 * Windows discovered a corruption in the file %hs. This file has now been
	 * repaired. Check if any data in the file was lost because of the
	 * corruption.
	 */
	STATUS_DATA_LOST_REPAIR(
		0x80000803L,
		"Windows discovered a corruption in the file %hs. This file has now been repaired. " +
		"Check if any data in the file was lost because of the corruption."),

	/** Debugger did not handle the exception. */
	DBG_EXCEPTION_NOT_HANDLED(0x80010001L, "Debugger did not handle the exception."),

	/** The cluster node is already up. */
	STATUS_CLUSTER_NODE_ALREADY_UP(0x80130001L, "The cluster node is already up."),

	/** The cluster node is already down. */
	STATUS_CLUSTER_NODE_ALREADY_DOWN(0x80130002L, "The cluster node is already down."),

	/** The cluster network is already online. */
	STATUS_CLUSTER_NETWORK_ALREADY_ONLINE(0x80130003L, "The cluster network is already online."),

	/** The cluster network is already offline. */
	STATUS_CLUSTER_NETWORK_ALREADY_OFFLINE(0x80130004L, "The cluster network is already offline."),

	/** The cluster node is already a member of the cluster. */
	STATUS_CLUSTER_NODE_ALREADY_MEMBER(0x80130005L, "The cluster node is already a member of the cluster."),

	/** The log could not be set to the requested size. */
	STATUS_COULD_NOT_RESIZE_LOG(0x80190009L, "The log could not be set to the requested size."),

	/** There is no transaction metadata on the file. */
	STATUS_NO_TXF_METADATA(0x80190029L, "There is no transaction metadata on the file."),

	/** The file cannot be recovered because there is a handle still open on it. */
	STATUS_CANT_RECOVER_WITH_HANDLE_OPEN(0x80190031L, "The file cannot be recovered because there is a handle still open on it."),

	/**
	 * Transaction metadata is already present on this file and cannot be
	 * superseded.
	 */
	STATUS_TXF_METADATA_ALREADY_PRESENT(0x80190041L, "Transaction metadata is already present on this file and cannot be superseded."),

	/**
	 * A transaction scope could not be entered because the scope handler has
	 * not been initialized.
	 */
	STATUS_TRANSACTION_SCOPE_CALLBACKS_NOT_SET(0x80190042L,
		"A transaction scope could not be entered because the scope handler has not been initialized."),

	/**
	 * {Display Driver Stopped Responding and recovered} The %hs display driver
	 * has stopped working normally. The recovery had been performed.
	 */
	STATUS_VIDEO_HUNG_DISPLAY_DRIVER_THREAD_RECOVERED(0x801B00EBL,
		"{Display Driver Stopped Responding and recovered} The %hs display driver " +
		"has stopped working normally. The recovery had been performed."),

	/**
	 * {Buffer too small} The buffer is too small to contain the entry. No
	 * information has been written to the buffer.
	 */
	STATUS_FLT_BUFFER_TOO_SMALL(0x801C0001L,
		"{Buffer too small} The buffer is too small to contain the entry. No information has been written to the buffer."),

	/** Volume metadata read or write is incomplete. */
	STATUS_FVE_PARTIAL_METADATA(0x80210001L, "Volume metadata read or write is incomplete."),

	/**
	 * BitLocker encryption keys were ignored because the volume was in a
	 * transient state.
	 */
	STATUS_FVE_TRANSIENT_STATE(0x80210002L, "BitLocker encryption keys were ignored because the volume was in a transient state."),

	/** {Operation Failed} The requested operation was unsuccessful. */
	STATUS_UNSUCCESSFUL(0xC0000001L, "{Operation Failed} The requested operation was unsuccessful."),

	/** {Not Implemented} The requested operation is not implemented. */
	STATUS_NOT_IMPLEMENTED(0xC0000002L, "{Not Implemented} The requested operation is not implemented."),

	/**
	 * {Invalid Parameter} The specified information class is not a valid
	 * information class for the specified object.
	 */
	STATUS_INVALID_INFO_CLASS(0xC0000003L,
		"{Invalid Parameter} The specified information class is not a valid information class for the specified object."),

	/**
	 * The specified information record length does not match the length that is
	 * required for the specified information class.
	 */
	STATUS_INFO_LENGTH_MISMATCH(0xC0000004L,
		"The specified information record length does not match the length that is required for the specified information class."),

	/**
	 * The instruction at 0x%08lx referenced memory at 0x%08lx. The memory could
	 * not be %s.
	 */
	STATUS_ACCESS_VIOLATION(0xC0000005L, "The instruction at 0x%08lx referenced memory at 0x%08lx. The memory could not be %s."),

	/**
	 * The instruction at 0x%08lx referenced memory at 0x%08lx. The required
	 * data was not placed into memory because of an I/O error status of
	 * 0x%08lx.
	 */
	STATUS_IN_PAGE_ERROR(
		0xC0000006L,
		"The instruction at 0x%08lx referenced memory at 0x%08lx. The required data " +
		"was not placed into memory because of an I/O error status of 0x%08lx."),

	/** The page file quota for the process has been exhausted. */
	STATUS_PAGEFILE_QUOTA(0xC0000007L, "The page file quota for the process has been exhausted."),

	/** An invalid HANDLE was specified. */
	STATUS_INVALID_HANDLE(0xC0000008L, "An invalid HANDLE was specified."),

	/** An invalid initial stack was specified in a call to NtCreateThread. */
	STATUS_BAD_INITIAL_STACK(0xC0000009L, "An invalid initial stack was specified in a call to NtCreateThread."),

	/**
	 * An invalid initial start address was specified in a call to
	 * NtCreateThread.
	 */
	STATUS_BAD_INITIAL_PC(0xC000000AL, "An invalid initial start address was specified in a call to NtCreateThread."),

	/** An invalid client ID was specified. */
	STATUS_INVALID_CID(0xC000000BL, "An invalid client ID was specified."),

	/**
	 * An attempt was made to cancel or set a timer that has an associated APC
	 * and the specified thread is not the thread that originally set the timer
	 * with an associated APC routine.
	 */
	STATUS_TIMER_NOT_CANCELED(
		0xC000000CL,
		"An attempt was made to cancel or set a timer that has an associated APC and the " +
		"specified thread is not the thread that originally set the timer with an associated APC routine."),

	/** An invalid parameter was passed to a service or function. */
	STATUS_INVALID_PARAMETER(0xC000000DL, "An invalid parameter was passed to a service or function."),

	/** A device that does not exist was specified. */
	STATUS_NO_SUCH_DEVICE(0xC000000EL, "A device that does not exist was specified."),

	/** {File Not Found} The file %hs does not exist. */
	STATUS_NO_SUCH_FILE(0xC000000FL, "{File Not Found} The file %hs does not exist."),

	/** The specified request is not a valid operation for the target device. */
	STATUS_INVALID_DEVICE_REQUEST(0xC0000010L, "The specified request is not a valid operation for the target device."),

	/**
	 * The end-of-file marker has been reached. There is no valid data in the
	 * file beyond this marker.
	 */
	STATUS_END_OF_FILE(0xC0000011L, "The end-of-file marker has been reached. There is no valid data in the file beyond this marker."),

	/**
	 * {Wrong Volume} The wrong volume is in the drive. Insert volume %hs into
	 * drive %hs.
	 */
	STATUS_WRONG_VOLUME(0xC0000012L, "{Wrong Volume} The wrong volume is in the drive. Insert volume %hs into drive %hs."),

	/** {No Disk} There is no disk in the drive. Insert a disk into drive %hs. */
	STATUS_NO_MEDIA_IN_DEVICE(0xC0000013L, "{No Disk} There is no disk in the drive. Insert a disk into drive %hs."),

	/**
	 * {Unknown Disk Format} The disk in drive %hs is not formatted properly.
	 * Check the disk, and reformat it, if needed.
	 */
	STATUS_UNRECOGNIZED_MEDIA(0xC0000014L,
		"{Unknown Disk Format} The disk in drive %hs is not formatted properly. Check the disk, and reformat it, if needed."),

	/** {Sector Not Found} The specified sector does not exist. */
	STATUS_NONEXISTENT_SECTOR(0xC0000015L, "{Sector Not Found} The specified sector does not exist."),

	/**
	 * {Still Busy} The specified I/O request packet (IRP) cannot be disposed of
	 * because the I/O operation is not complete.
	 */
	STATUS_MORE_PROCESSING_REQUIRED(0xC0000016L,
		"{Still Busy} The specified I/O request packet (IRP) cannot be disposed of because the I/O operation is not complete."),

	/**
	 * {Not Enough Quota} Not enough virtual memory or paging file quota is
	 * available to complete the specified operation.
	 */
	STATUS_NO_MEMORY(0xC0000017L,
		"{Not Enough Quota} Not enough virtual memory or paging file quota is available to complete the specified operation."),

	/**
	 * {Conflicting Address Range} The specified address range conflicts with
	 * the address space.
	 */
	STATUS_CONFLICTING_ADDRESSES(0xC0000018L, "{Conflicting Address Range} The specified address range conflicts with the address space."),

	/** The address range to unmap is not a mapped view. */
	STATUS_NOT_MAPPED_VIEW(0xC0000019L, "The address range to unmap is not a mapped view."),

	/** The virtual memory cannot be freed. */
	STATUS_UNABLE_TO_FREE_VM(0xC000001AL, "The virtual memory cannot be freed."),

	/** The specified section cannot be deleted. */
	STATUS_UNABLE_TO_DELETE_SECTION(0xC000001BL, "The specified section cannot be deleted."),

	/** An invalid system service was specified in a system service call. */
	STATUS_INVALID_SYSTEM_SERVICE(0xC000001CL, "An invalid system service was specified in a system service call."),

	/**
	 * {EXCEPTION} Illegal Instruction An attempt was made to execute an illegal
	 * instruction.
	 */
	STATUS_ILLEGAL_INSTRUCTION(0xC000001DL, "{EXCEPTION} Illegal Instruction An attempt was made to execute an illegal instruction."),

	/**
	 * {Invalid Lock Sequence} An attempt was made to execute an invalid lock
	 * sequence.
	 */
	STATUS_INVALID_LOCK_SEQUENCE(0xC000001EL, "{Invalid Lock Sequence} An attempt was made to execute an invalid lock sequence."),

	/**
	 * {Invalid Mapping} An attempt was made to create a view for a section that
	 * is bigger than the section.
	 */
	STATUS_INVALID_VIEW_SIZE(0xC000001FL,
		"{Invalid Mapping} An attempt was made to create a view for a section that is bigger than the section."),

	/**
	 * {Bad File} The attributes of the specified mapping file for a section of
	 * memory cannot be read.
	 */
	STATUS_INVALID_FILE_FOR_SECTION(0xC0000020L,
		"{Bad File} The attributes of the specified mapping file for a section of memory cannot be read."),

	/** {Already Committed} The specified address range is already committed. */
	STATUS_ALREADY_COMMITTED(0xC0000021L, "{Already Committed} The specified address range is already committed."),

	/**
	 * {Access Denied} A process has requested access to an object but has not
	 * been granted those access rights.
	 */
	STATUS_ACCESS_DENIED(0xC0000022L,
		"{Access Denied} A process has requested access to an object but has not been granted those access rights."),

	/**
	 * {Buffer Too Small} The buffer is too small to contain the entry. No
	 * information has been written to the buffer.
	 */
	STATUS_BUFFER_TOO_SMALL(0xC0000023L,
		"{Buffer Too Small} The buffer is too small to contain the entry. No information has been written to the buffer."),

	/**
	 * {Wrong Type} There is a mismatch between the type of object that is
	 * required by the requested operation and the type of object that is
	 * specified in the request.
	 */
	STATUS_OBJECT_TYPE_MISMATCH(
		0xC0000024L,
		"{Wrong Type} There is a mismatch between the type of object that is required by " +
		"the requested operation and the type of object that is specified in the request."),

	/** {EXCEPTION} Cannot Continue Windows cannot continue from this exception. */
	STATUS_NONCONTINUABLE_EXCEPTION(0xC0000025L, "{EXCEPTION} Cannot Continue Windows cannot continue from this exception."),

	/** An invalid exception disposition was returned by an exception handler. */
	STATUS_INVALID_DISPOSITION(0xC0000026L, "An invalid exception disposition was returned by an exception handler."),

	/** Unwind exception code. */
	STATUS_UNWIND(0xC0000027L, "Unwind exception code."),

	/**
	 * An invalid or unaligned stack was encountered during an unwind operation.
	 */
	STATUS_BAD_STACK(0xC0000028L, "An invalid or unaligned stack was encountered during an unwind operation."),

	/** An invalid unwind target was encountered during an unwind operation. */
	STATUS_INVALID_UNWIND_TARGET(0xC0000029L, "An invalid unwind target was encountered during an unwind operation."),

	/** An attempt was made to unlock a page of memory that was not locked. */
	STATUS_NOT_LOCKED(0xC000002AL, "An attempt was made to unlock a page of memory that was not locked."),

	/** A device parity error on an I/O operation. */
	STATUS_PARITY_ERROR(0xC000002BL, "A device parity error on an I/O operation."),

	/** An attempt was made to decommit uncommitted virtual memory. */
	STATUS_UNABLE_TO_DECOMMIT_VM(0xC000002CL, "An attempt was made to decommit uncommitted virtual memory."),

	/**
	 * An attempt was made to change the attributes on memory that has not been
	 * committed.
	 */
	STATUS_NOT_COMMITTED(0xC000002DL, "An attempt was made to change the attributes on memory that has not been committed."),

	/**
	 * Invalid object attributes specified to NtCreatePort or invalid port
	 * attributes specified to NtConnectPort.
	 */
	STATUS_INVALID_PORT_ATTRIBUTES(0xC000002EL,
		"Invalid object attributes specified to NtCreatePort or invalid port attributes specified to NtConnectPort."),

	/**
	 * The length of the message that was passed to NtRequestPort or
	 * NtRequestWaitReplyPort is longer than the maximum message that is allowed
	 * by the port.
	 */
	STATUS_PORT_MESSAGE_TOO_LONG(
		0xC000002FL,
		"The length of the message that was passed to NtRequestPort or NtRequestWaitReplyPort " +
		"is longer than the maximum message that is allowed by the port."),

	/** An invalid combination of parameters was specified. */
	STATUS_INVALID_PARAMETER_MIX(0xC0000030L, "An invalid combination of parameters was specified."),

	/** An attempt was made to lower a quota limit below the current usage. */
	STATUS_INVALID_QUOTA_LOWER(0xC0000031L, "An attempt was made to lower a quota limit below the current usage."),

	/**
	 * {Corrupt Disk} The file system structure on the disk is corrupt and
	 * unusable. Run the Chkdsk utility on the volume %hs.
	 */
	STATUS_DISK_CORRUPT_ERROR(0xC0000032L,
		"{Corrupt Disk} The file system structure on the disk is corrupt and unusable. Run the Chkdsk utility on the volume %hs."),

	/** The object name is invalid. */
	STATUS_OBJECT_NAME_INVALID(0xC0000033L, "The object name is invalid."),

	/** The object name is not found. */
	STATUS_OBJECT_NAME_NOT_FOUND(0xC0000034L, "The object name is not found."),

	/** The object name already exists. */
	STATUS_OBJECT_NAME_COLLISION(0xC0000035L, "The object name already exists."),

	/**
	 * An attempt was made to send a message to a disconnected communication
	 * port.
	 */
	STATUS_PORT_DISCONNECTED(0xC0000037L, "An attempt was made to send a message to a disconnected communication port."),

	/**
	 * An attempt was made to attach to a device that was already attached to
	 * another device.
	 */
	STATUS_DEVICE_ALREADY_ATTACHED(0xC0000038L, "An attempt was made to attach to a device that was already attached to another device."),

	/** The object path component was not a directory object. */
	STATUS_OBJECT_PATH_INVALID(0xC0000039L, "The object path component was not a directory object."),

	/** {Path Not Found} The path %hs does not exist. */
	STATUS_OBJECT_PATH_NOT_FOUND(0xC000003AL, "{Path Not Found} The path %hs does not exist."),

	/** The object path component was not a directory object. */
	STATUS_OBJECT_PATH_SYNTAX_BAD(0xC000003BL, "The object path component was not a directory object."),

	/** {Data Overrun} A data overrun error occurred. */
	STATUS_DATA_OVERRUN(0xC000003CL, "{Data Overrun} A data overrun error occurred."),

	/** {Data Late} A data late error occurred. */
	STATUS_DATA_LATE_ERROR(0xC000003DL, "{Data Late} A data late error occurred."),

	/** {Data Error} An error occurred in reading or writing data. */
	STATUS_DATA_ERROR(0xC000003EL, "{Data Error} An error occurred in reading or writing data."),

	/** {Bad CRC} A cyclic redundancy check (CRC) checksum error occurred. */
	STATUS_CRC_ERROR(0xC000003FL, "{Bad CRC} A cyclic redundancy check (CRC) checksum error occurred."),

	/** {Section Too Large} The specified section is too big to map the file. */
	STATUS_SECTION_TOO_BIG(0xC0000040L, "{Section Too Large} The specified section is too big to map the file."),

	/** The NtConnectPort request is refused. */
	STATUS_PORT_CONNECTION_REFUSED(0xC0000041L, "The NtConnectPort request is refused."),

	/** The type of port handle is invalid for the operation that is requested. */
	STATUS_INVALID_PORT_HANDLE(0xC0000042L, "The type of port handle is invalid for the operation that is requested."),

	/** A file cannot be opened because the share access flags are incompatible. */
	STATUS_SHARING_VIOLATION(0xC0000043L, "A file cannot be opened because the share access flags are incompatible."),

	/** Insufficient quota exists to complete the operation. */
	STATUS_QUOTA_EXCEEDED(0xC0000044L, "Insufficient quota exists to complete the operation."),

	/** The specified page protection was not valid. */
	STATUS_INVALID_PAGE_PROTECTION(0xC0000045L, "The specified page protection was not valid."),

	/**
	 * An attempt to release a mutant object was made by a thread that was not
	 * the owner of the mutant object.
	 */
	STATUS_MUTANT_NOT_OWNED(0xC0000046L,
		"An attempt to release a mutant object was made by a thread that was not the owner of the mutant object."),

	/**
	 * An attempt was made to release a semaphore such that its maximum count
	 * would have been exceeded.
	 */
	STATUS_SEMAPHORE_LIMIT_EXCEEDED(0xC0000047L,
		"An attempt was made to release a semaphore such that its maximum count would have been exceeded."),

	/**
	 * An attempt was made to set the DebugPort or ExceptionPort of a process,
	 * but a port already exists in the process, or an attempt was made to set
	 * the CompletionPort of a file but a port was already set in the file, or an
	 * attempt was made to set the associated completion port of an ALPC port but
	 * it is already set.
	 */
	STATUS_PORT_ALREADY_SET(
		0xC0000048L,
		"An attempt was made to set the DebugPort or ExceptionPort of a process, but a port " +
		"already exists in the process, or an attempt was made to set the CompletionPort of a " +
		"file but a port was already set in the file, or an attempt was made to set the " +
		"associated completion port of an ALPC port but it is already set."),

	/**
	 * An attempt was made to query image information on a section that does not
	 * map an image.
	 */
	STATUS_SECTION_NOT_IMAGE(0xC0000049L, "An attempt was made to query image information on a section that does not map an image."),

	/**
	 * An attempt was made to suspend a thread whose suspend count was at its
	 * maximum.
	 */
	STATUS_SUSPEND_COUNT_EXCEEDED(0xC000004AL, "An attempt was made to suspend a thread whose suspend count was at its maximum."),

	/** An attempt was made to suspend a thread that has begun termination. */
	STATUS_THREAD_IS_TERMINATING(0xC000004BL, "An attempt was made to suspend a thread that has begun termination."),

	/**
	 * An attempt was made to set the working set limit to an invalid value (for
	 * example, the minimum greater than maximum).
	 */
	STATUS_BAD_WORKING_SET_LIMIT(0xC000004CL,
		"An attempt was made to set the working set limit to an invalid value (for example, the minimum greater than maximum)."),

	/**
	 * A section was created to map a file that is not compatible with an
	 * already existing section that maps the same file.
	 */
	STATUS_INCOMPATIBLE_FILE_MAP(0xC000004DL,
		"A section was created to map a file that is not compatible with an already existing section that maps the same file."),

	/**
	 * A view to a section specifies a protection that is incompatible with the
	 * protection of the initial view.
	 */
	STATUS_SECTION_PROTECTION(0xC000004EL,
		"A view to a section specifies a protection that is incompatible with the protection of the initial view."),

	/**
	 * An operation involving EAs failed because the file system does not
	 * support EAs.
	 */
	STATUS_EAS_NOT_SUPPORTED(0xC000004FL, "An operation involving EAs failed because the file system does not support EAs."),

	/** An EA operation failed because the EA set is too large. */
	STATUS_EA_TOO_LARGE(0xC0000050L, "An EA operation failed because the EA set is too large."),

	/** An EA operation failed because the name or EA index is invalid. */
	STATUS_NONEXISTENT_EA_ENTRY(0xC0000051L, "An EA operation failed because the name or EA index is invalid."),

	/** The file for which EAs were requested has no EAs. */
	STATUS_NO_EAS_ON_FILE(0xC0000052L, "The file for which EAs were requested has no EAs."),

	/** The EA is corrupt and cannot be read. */
	STATUS_EA_CORRUPT_ERROR(0xC0000053L, "The EA is corrupt and cannot be read."),

	/** A requested read/write cannot be granted due to a conflicting file lock. */
	STATUS_FILE_LOCK_CONFLICT(0xC0000054L, "A requested read/write cannot be granted due to a conflicting file lock."),

	/** A requested file lock cannot be granted due to other existing locks. */
	STATUS_LOCK_NOT_GRANTED(0xC0000055L, "A requested file lock cannot be granted due to other existing locks."),

	/**
	 * A non-close operation has been requested of a file object that has a
	 * delete pending.
	 */
	STATUS_DELETE_PENDING(0xC0000056L, "A non-close operation has been requested of a file object that has a delete pending."),

	/**
	 * An attempt was made to set the control attribute on a file. This
	 * attribute is not supported in the destination file system.
	 */
	STATUS_CTL_FILE_NOT_SUPPORTED(0xC0000057L,
		"An attempt was made to set the control attribute on a file. This attribute is not supported in the destination file system."),

	/**
	 * Indicates a revision number that was encountered or specified is not one
	 * that is known by the service. It might be a more recent revision than the
	 * service is aware of.
	 */
	STATUS_UNKNOWN_REVISION(
		0xC0000058L,
		"Indicates a revision number that was encountered or specified is not one that " +
		"is known by the service. It might be a more recent revision than the service is aware of."),

	/** Indicates that two revision levels are incompatible. */
	STATUS_REVISION_MISMATCH(0xC0000059L, "Indicates that two revision levels are incompatible."),

	/**
	 * Indicates a particular security ID cannot be assigned as the owner of an
	 * object.
	 */
	STATUS_INVALID_OWNER(0xC000005AL, "Indicates a particular security ID cannot be assigned as the owner of an object."),

	/**
	 * Indicates a particular security ID cannot be assigned as the primary
	 * group of an object.
	 */
	STATUS_INVALID_PRIMARY_GROUP(0xC000005BL, "Indicates a particular security ID cannot be assigned as the primary group of an object."),

	/**
	 * An attempt has been made to operate on an impersonation token by a thread
	 * that is not currently impersonating a client.
	 */
	STATUS_NO_IMPERSONATION_TOKEN(0xC000005CL,
		"An attempt has been made to operate on an impersonation token by a thread that is not currently impersonating a client."),

	/** A mandatory group cannot be disabled. */
	STATUS_CANT_DISABLE_MANDATORY(0xC000005DL, "A mandatory group cannot be disabled."),

	/** No logon servers are currently available to service the logon request. */
	STATUS_NO_LOGON_SERVERS(0xC000005EL, "No logon servers are currently available to service the logon request."),

	/**
	 * A specified logon session does not exist. It might already have been
	 * terminated.
	 */
	STATUS_NO_SUCH_LOGON_SESSION(0xC000005FL, "A specified logon session does not exist. It might already have been terminated."),

	/** A specified privilege does not exist. */
	STATUS_NO_SUCH_PRIVILEGE(0xC0000060L, "A specified privilege does not exist."),

	/** A required privilege is not held by the client. */
	STATUS_PRIVILEGE_NOT_HELD(0xC0000061L, "A required privilege is not held by the client."),

	/** The name provided is not a properly formed account name. */
	STATUS_INVALID_ACCOUNT_NAME(0xC0000062L, "The name provided is not a properly formed account name."),

	/** The specified account already exists. */
	STATUS_USER_EXISTS(0xC0000063L, "The specified account already exists."),

	/** The specified account does not exist. */
	STATUS_NO_SUCH_USER(0xC0000064L, "The specified account does not exist."),

	/** The specified group already exists. */
	STATUS_GROUP_EXISTS(0xC0000065L, "The specified group already exists."),

	/** The specified group does not exist. */
	STATUS_NO_SUCH_GROUP(0xC0000066L, "The specified group does not exist."),

	/**
	 * The specified user account is already in the specified group account.
	 * Also used to indicate a group cannot be deleted because it contains a
	 * member.
	 */
	STATUS_MEMBER_IN_GROUP(
		0xC0000067L,
		"The specified user account is already in the specified group account. Also " +
		"used to indicate a group cannot be deleted because it contains a member."),

	/**
	 * The specified user account is not a member of the specified group
	 * account.
	 */
	STATUS_MEMBER_NOT_IN_GROUP(0xC0000068L, "The specified user account is not a member of the specified group account."),

	/**
	 * Indicates the requested operation would disable or delete the last
	 * remaining administration account. This is not allowed to prevent creating
	 * a situation in which the system cannot be administrated.
	 */
	STATUS_LAST_ADMIN(
		0xC0000069L,
		"Indicates the requested operation would disable or delete the last remaining " +
		"administration account. This is not allowed to prevent creating a situation in " +
		"which the system cannot be administrated."),

	/**
	 * When trying to update a password, this return status indicates that the
	 * value provided as the current password is not correct.
	 */
	STATUS_WRONG_PASSWORD(0xC000006AL,
		"When trying to update a password, this return status indicates that the value provided as the current password is not correct."),

	/**
	 * When trying to update a password, this return status indicates that the
	 * value provided for the new password contains values that are not allowed
	 * in passwords.
	 */
	STATUS_ILL_FORMED_PASSWORD(
		0xC000006BL,
		"When trying to update a password, this return status indicates that the value " +
		"provided for the new password contains values that are not allowed in passwords."),

	/**
	 * When trying to update a password, this status indicates that some
	 * password update rule has been violated. For example, the password might
	 * not meet length criteria.
	 */
	STATUS_PASSWORD_RESTRICTION(
		0xC000006CL,
		"When trying to update a password, this status indicates that some password update " +
		"rule has been violated. For example, the password might not meet length criteria."),

	/**
	 * The attempted logon is invalid. This is either due to a bad username or
	 * authentication information.
	 */
	STATUS_LOGON_FAILURE(0xC000006DL, "The attempted logon is invalid. This is either due to a bad username or authentication information."),

	/**
	 * Indicates a referenced user name and authentication information are
	 * valid, but some user account restriction has prevented successful
	 * authentication (such as time-of-day restrictions).
	 */
	STATUS_ACCOUNT_RESTRICTION(
		0xC000006EL,
		"Indicates a referenced user name and authentication information are valid, but " +
		"some user account restriction has prevented successful authentication " +
		"(such as time-of-day restrictions)."),

	/**
	 * The user account has time restrictions and cannot be logged onto at this
	 * time.
	 */
	STATUS_INVALID_LOGON_HOURS(0xC000006FL, "The user account has time restrictions and cannot be logged onto at this time."),

	/**
	 * The user account is restricted so that it cannot be used to log on from
	 * the source workstation.
	 */
	STATUS_INVALID_WORKSTATION(0xC0000070L,
		"The user account is restricted so that it cannot be used to log on from the source workstation."),

	/** The user account password has expired. */
	STATUS_PASSWORD_EXPIRED(0xC0000071L, "The user account password has expired."),

	/** The referenced account is currently disabled and cannot be logged on to. */
	STATUS_ACCOUNT_DISABLED(0xC0000072L, "The referenced account is currently disabled and cannot be logged on to."),

	/** None of the information to be translated has been translated. */
	STATUS_NONE_MAPPED(0xC0000073L, "None of the information to be translated has been translated."),

	/**
	 * The number of LUIDs requested cannot be allocated with a single
	 * allocation.
	 */
	STATUS_TOO_MANY_LUIDS_REQUESTED(0xC0000074L, "The number of LUIDs requested cannot be allocated with a single allocation."),

	/** Indicates there are no more LUIDs to allocate. */
	STATUS_LUIDS_EXHAUSTED(0xC0000075L, "Indicates there are no more LUIDs to allocate."),

	/** Indicates the sub-authority value is invalid for the particular use. */
	STATUS_INVALID_SUB_AUTHORITY(0xC0000076L, "Indicates the sub-authority value is invalid for the particular use."),

	/** Indicates the ACL structure is not valid. */
	STATUS_INVALID_ACL(0xC0000077L, "Indicates the ACL structure is not valid."),

	/** Indicates the SID structure is not valid. */
	STATUS_INVALID_SID(0xC0000078L, "Indicates the SID structure is not valid."),

	/** Indicates the SECURITY_DESCRIPTOR structure is not valid. */
	STATUS_INVALID_SECURITY_DESCR(0xC0000079L, "Indicates the SECURITY_DESCRIPTOR structure is not valid."),

	/** Indicates the specified procedure address cannot be found in the DLL. */
	STATUS_PROCEDURE_NOT_FOUND(0xC000007AL, "Indicates the specified procedure address cannot be found in the DLL."),

	/**
	 * {Bad Image} %hs is either not designed to run on Windows or it contains
	 * an error. Try installing the program again using the original
	 * installation media or contact your system administrator or the software
	 * vendor for support.
	 */
	STATUS_INVALID_IMAGE_FORMAT(
		0xC000007BL,
		"{Bad Image} %hs is either not designed to run on Windows or it contains an error. " +
		"Try installing the program again using the original installation media or contact " +
		"your system administrator or the software vendor for support."),

	/**
	 * An attempt was made to reference a token that does not exist. This is
	 * typically done by referencing the token that is associated with a thread
	 * when the thread is not impersonating a client.
	 */
	STATUS_NO_TOKEN(
		0xC000007CL,
		"An attempt was made to reference a token that does not exist. This is typically " +
		"done by referencing the token that is associated with a thread when the thread " +
		"is not impersonating a client."),

	/**
	 * Indicates that an attempt to build either an inherited ACL or ACE was not
	 * successful. This can be caused by a number of things. One of the more
	 * probable causes is the replacement of a CreatorId with a SID that did not
	 * fit into the ACE or ACL.
	 */
	STATUS_BAD_INHERITANCE_ACL(
		0xC000007DL,
		"Indicates that an attempt to build either an inherited ACL or ACE was not successful. " +
		"This can be caused by a number of things. One of the more probable causes is the " +
		"replacement of a CreatorId with a SID that did not fit into the ACE or ACL."),

	/** The range specified in NtUnlockFile was not locked. */
	STATUS_RANGE_NOT_LOCKED(0xC000007EL, "The range specified in NtUnlockFile was not locked."),

	/** An operation failed because the disk was full. */
	STATUS_DISK_FULL(0xC000007FL, "An operation failed because the disk was full."),

	/** The GUID allocation server is disabled at the moment. */
	STATUS_SERVER_DISABLED(0xC0000080L, "The GUID allocation server is disabled at the moment."),

	/** The GUID allocation server is enabled at the moment. */
	STATUS_SERVER_NOT_DISABLED(0xC0000081L, "The GUID allocation server is enabled at the moment."),

	/** Too many GUIDs were requested from the allocation server at once. */
	STATUS_TOO_MANY_GUIDS_REQUESTED(0xC0000082L, "Too many GUIDs were requested from the allocation server at once."),

	/**
	 * The GUIDs could not be allocated because the Authority Agent was
	 * exhausted.
	 */
	STATUS_GUIDS_EXHAUSTED(0xC0000083L, "The GUIDs could not be allocated because the Authority Agent was exhausted."),

	/** The value provided was an invalid value for an identifier authority. */
	STATUS_INVALID_ID_AUTHORITY(0xC0000084L, "The value provided was an invalid value for an identifier authority."),

	/**
	 * No more authority agent values are available for the particular
	 * identifier authority value.
	 */
	STATUS_AGENTS_EXHAUSTED(0xC0000085L, "No more authority agent values are available for the particular identifier authority value."),

	/** An invalid volume label has been specified. */
	STATUS_INVALID_VOLUME_LABEL(0xC0000086L, "An invalid volume label has been specified."),

	/** A mapped section could not be extended. */
	STATUS_SECTION_NOT_EXTENDED(0xC0000087L, "A mapped section could not be extended."),

	/** Specified section to flush does not map a data file. */
	STATUS_NOT_MAPPED_DATA(0xC0000088L, "Specified section to flush does not map a data file."),

	/** Indicates the specified image file did not contain a resource section. */
	STATUS_RESOURCE_DATA_NOT_FOUND(0xC0000089L, "Indicates the specified image file did not contain a resource section."),

	/** Indicates the specified resource type cannot be found in the image file. */
	STATUS_RESOURCE_TYPE_NOT_FOUND(0xC000008AL, "Indicates the specified resource type cannot be found in the image file."),

	/** Indicates the specified resource name cannot be found in the image file. */
	STATUS_RESOURCE_NAME_NOT_FOUND(0xC000008BL, "Indicates the specified resource name cannot be found in the image file."),

	/** {EXCEPTION} Array bounds exceeded. */
	STATUS_ARRAY_BOUNDS_EXCEEDED(0xC000008CL, "{EXCEPTION} Array bounds exceeded."),

	/** {EXCEPTION} Floating-point denormal operand. */
	STATUS_FLOAT_DENORMAL_OPERAND(0xC000008DL, "{EXCEPTION} Floating-point denormal operand."),

	/** {EXCEPTION} Floating-point division by zero. */
	STATUS_FLOAT_DIVIDE_BY_ZERO(0xC000008EL, "{EXCEPTION} Floating-point division by zero."),

	/** {EXCEPTION} Floating-point inexact result. */
	STATUS_FLOAT_INEXACT_RESULT(0xC000008FL, "{EXCEPTION} Floating-point inexact result."),

	/** {EXCEPTION} Floating-point invalid operation. */
	STATUS_FLOAT_INVALID_OPERATION(0xC0000090L, "{EXCEPTION} Floating-point invalid operation."),

	/** {EXCEPTION} Floating-point overflow. */
	STATUS_FLOAT_OVERFLOW(0xC0000091L, "{EXCEPTION} Floating-point overflow."),

	/** {EXCEPTION} Floating-point stack check. */
	STATUS_FLOAT_STACK_CHECK(0xC0000092L, "{EXCEPTION} Floating-point stack check."),

	/** {EXCEPTION} Floating-point underflow. */
	STATUS_FLOAT_UNDERFLOW(0xC0000093L, "{EXCEPTION} Floating-point underflow."),

	/** {EXCEPTION} Integer division by zero. */
	STATUS_INTEGER_DIVIDE_BY_ZERO(0xC0000094L, "{EXCEPTION} Integer division by zero."),

	/** {EXCEPTION} Integer overflow. */
	STATUS_INTEGER_OVERFLOW(0xC0000095L, "{EXCEPTION} Integer overflow."),

	/** {EXCEPTION} Privileged instruction. */
	STATUS_PRIVILEGED_INSTRUCTION(0xC0000096L, "{EXCEPTION} Privileged instruction."),

	/**
	 * An attempt was made to install more paging files than the system
	 * supports.
	 */
	STATUS_TOO_MANY_PAGING_FILES(0xC0000097L, "An attempt was made to install more paging files than the system supports."),

	/**
	 * The volume for a file has been externally altered such that the opened
	 * file is no longer valid.
	 */
	STATUS_FILE_INVALID(0xC0000098L, "The volume for a file has been externally altered such that the opened file is no longer valid."),

	/**
	 * When a block of memory is allotted for future updates, such as the memory
	 * allocated to hold discretionary access control and primary group
	 * information, successive updates might exceed the amount of memory
	 * originally allotted. Because a quota might already have been charged to
	 * several processes that have handles to the object, it is not reasonable to
	 * alter the size of the allocated memory. Instead, a request that requires
	 * more memory than has been allotted must fail and the
	 * STATUS_ALLOTTED_SPACE_EXCEEDED error returned.
	 */
	STATUS_ALLOTTED_SPACE_EXCEEDED(
		0xC0000099L,
		"When a block of memory is allotted for future updates, such as the memory allocated " +
		"to hold discretionary access control and primary group information, successive updates " +
		"might exceed the amount of memory originally allotted. Because a quota might already " +
		"have been charged to several processes that have handles to the object, it is not " +
		"reasonable to alter the size of the allocated memory. Instead, a request that requires " +
		"more memory than has been allotted must fail and the STATUS_ALLOTTED_SPACE_EXCEEDED error returned."),

	/** Insufficient system resources exist to complete the API. */
	STATUS_INSUFFICIENT_RESOURCES(0xC000009AL, "Insufficient system resources exist to complete the API."),

	/** An attempt has been made to open a DFS exit path control file. */
	STATUS_DFS_EXIT_PATH_FOUND(0xC000009BL, "An attempt has been made to open a DFS exit path control file."),

	/** There are bad blocks (sectors) on the hard disk. */
	STATUS_DEVICE_DATA_ERROR(0xC000009CL, "There are bad blocks (sectors) on the hard disk."),

	/**
	 * There is bad cabling, non-termination, or the controller is not able to
	 * obtain access to the hard disk.
	 */
	STATUS_DEVICE_NOT_CONNECTED(0xC000009DL,
		"There is bad cabling, non-termination, or the controller is not able to obtain access to the hard disk."),

	/**
	 * Virtual memory cannot be freed because the base address is not the base
	 * of the region and a region size of zero was specified.
	 */
	STATUS_FREE_VM_NOT_AT_BASE(0xC000009FL,
		"Virtual memory cannot be freed because the base address is not the base of the region and a region size of zero was specified."),

	/** An attempt was made to free virtual memory that is not allocated. */
	STATUS_MEMORY_NOT_ALLOCATED(0xC00000A0L, "An attempt was made to free virtual memory that is not allocated."),

	/**
	 * The working set is not big enough to allow the requested pages to be
	 * locked.
	 */
	STATUS_WORKING_SET_QUOTA(0xC00000A1L, "The working set is not big enough to allow the requested pages to be locked."),

	/**
	 * {Write Protect Error} The disk cannot be written to because it is
	 * write-protected. Remove the write protection from the volume %hs in drive
	 * %hs.
	 */
	STATUS_MEDIA_WRITE_PROTECTED(
		0xC00000A2L,
		"{Write Protect Error} The disk cannot be written to because it is write-protected. " +
		"Remove the write protection from the volume %hs in drive %hs."),

	/**
	 * {Drive Not Ready} The drive is not ready for use; its door might be open.
	 * Check drive %hs and make sure that a disk is inserted and that the drive
	 * door is closed.
	 */
	STATUS_DEVICE_NOT_READY(
		0xC00000A3L,
		"{Drive Not Ready} The drive is not ready for use; its door might be open. Check drive " +
		"%hs and make sure that a disk is inserted and that the drive door is closed."),

	/**
	 * The specified attributes are invalid or are incompatible with the
	 * attributes for the group as a whole.
	 */
	STATUS_INVALID_GROUP_ATTRIBUTES(0xC00000A4L,
		"The specified attributes are invalid or are incompatible with the attributes for the group as a whole."),

	/**
	 * A specified impersonation level is invalid. Also used to indicate that a
	 * required impersonation level was not provided.
	 */
	STATUS_BAD_IMPERSONATION_LEVEL(0xC00000A5L,
		"A specified impersonation level is invalid. Also used to indicate that a required impersonation level was not provided."),

	/**
	 * An attempt was made to open an anonymous-level token. Anonymous tokens
	 * cannot be opened.
	 */
	STATUS_CANT_OPEN_ANONYMOUS(0xC00000A6L, "An attempt was made to open an anonymous-level token. Anonymous tokens cannot be opened."),

	/** The validation information class requested was invalid. */
	STATUS_BAD_VALIDATION_CLASS(0xC00000A7L, "The validation information class requested was invalid."),

	/** The type of a token object is inappropriate for its attempted use. */
	STATUS_BAD_TOKEN_TYPE(0xC00000A8L, "The type of a token object is inappropriate for its attempted use."),

	/** The type of a token object is inappropriate for its attempted use. */
	STATUS_BAD_MASTER_BOOT_RECORD(0xC00000A9L, "The type of a token object is inappropriate for its attempted use."),

	/**
	 * An attempt was made to execute an instruction at an unaligned address and
	 * the host system does not support unaligned instruction references.
	 */
	STATUS_INSTRUCTION_MISALIGNMENT(
		0xC00000AAL,
		"An attempt was made to execute an instruction at an unaligned address and the " +
		"host system does not support unaligned instruction references."),

	/** The maximum named pipe instance count has been reached. */
	STATUS_INSTANCE_NOT_AVAILABLE(0xC00000ABL, "The maximum named pipe instance count has been reached."),

	/** An instance of a named pipe cannot be found in the listening state. */
	STATUS_PIPE_NOT_AVAILABLE(0xC00000ACL, "An instance of a named pipe cannot be found in the listening state."),

	/** The named pipe is not in the connected or closing state. */
	STATUS_INVALID_PIPE_STATE(0xC00000ADL, "The named pipe is not in the connected or closing state."),

	/**
	 * The specified pipe is set to complete operations and there are current
	 * I/O operations queued so that it cannot be changed to queue operations.
	 */
	STATUS_PIPE_BUSY(
		0xC00000AEL,
		"The specified pipe is set to complete operations and there are current I/O " +
		"operations queued so that it cannot be changed to queue operations."),

	/** The specified handle is not open to the server end of the named pipe. */
	STATUS_ILLEGAL_FUNCTION(0xC00000AFL, "The specified handle is not open to the server end of the named pipe."),

	/** The specified named pipe is in the disconnected state. */
	STATUS_PIPE_DISCONNECTED(0xC00000B0L, "The specified named pipe is in the disconnected state."),

	/** The specified named pipe is in the closing state. */
	STATUS_PIPE_CLOSING(0xC00000B1L, "The specified named pipe is in the closing state."),

	/** The specified named pipe is in the connected state. */
	STATUS_PIPE_CONNECTED(0xC00000B2L, "The specified named pipe is in the connected state."),

	/** The specified named pipe is in the listening state. */
	STATUS_PIPE_LISTENING(0xC00000B3L, "The specified named pipe is in the listening state."),

	/** The specified named pipe is not in message mode. */
	STATUS_INVALID_READ_MODE(0xC00000B4L, "The specified named pipe is not in message mode."),

	/**
	 * {Device Timeout} The specified I/O operation on %hs was not completed
	 * before the time-out period expired.
	 */
	STATUS_IO_TIMEOUT(0xC00000B5L,
		"{Device Timeout} The specified I/O operation on %hs was not completed before the time-out period expired."),

	/** The specified file has been closed by another process. */
	STATUS_FILE_FORCED_CLOSED(0xC00000B6L, "The specified file has been closed by another process."),

	/** Profiling is not started. */
	STATUS_PROFILING_NOT_STARTED(0xC00000B7L, "Profiling is not started."),

	/** Profiling is not stopped. */
	STATUS_PROFILING_NOT_STOPPED(0xC00000B8L, "Profiling is not stopped."),

	/** The passed ACL did not contain the minimum required information. */
	STATUS_COULD_NOT_INTERPRET(0xC00000B9L, "The passed ACL did not contain the minimum required information."),

	/**
	 * The file that was specified as a target is a directory, and the caller
	 * specified that it could be anything but a directory.
	 */
	STATUS_FILE_IS_A_DIRECTORY(0xC00000BAL,
		"The file that was specified as a target is a directory, and the caller specified that it could be anything but a directory."),

	/** The request is not supported. */
	STATUS_NOT_SUPPORTED(0xC00000BBL, "The request is not supported."),

	/** This remote computer is not listening. */
	STATUS_REMOTE_NOT_LISTENING(0xC00000BCL, "This remote computer is not listening."),

	/** A duplicate name exists on the network. */
	STATUS_DUPLICATE_NAME(0xC00000BDL, "A duplicate name exists on the network."),

	/** The network path cannot be located. */
	STATUS_BAD_NETWORK_PATH(0xC00000BEL, "The network path cannot be located."),

	/** The network is busy. */
	STATUS_NETWORK_BUSY(0xC00000BFL, "The network is busy."),

	/** This device does not exist. */
	STATUS_DEVICE_DOES_NOT_EXIST(0xC00000C0L, "This device does not exist."),

	/** The network BIOS command limit has been reached. */
	STATUS_TOO_MANY_COMMANDS(0xC00000C1L, "The network BIOS command limit has been reached."),

	/** An I/O adapter hardware error has occurred. */
	STATUS_ADAPTER_HARDWARE_ERROR(0xC00000C2L, "An I/O adapter hardware error has occurred."),

	/** The network responded incorrectly. */
	STATUS_INVALID_NETWORK_RESPONSE(0xC00000C3L, "The network responded incorrectly."),

	/** An unexpected network error occurred. */
	STATUS_UNEXPECTED_NETWORK_ERROR(0xC00000C4L, "An unexpected network error occurred."),

	/** The remote adapter is not compatible. */
	STATUS_BAD_REMOTE_ADAPTER(0xC00000C5L, "The remote adapter is not compatible."),

	/** The print queue is full. */
	STATUS_PRINT_QUEUE_FULL(0xC00000C6L, "The print queue is full."),

	/**
	 * Space to store the file that is waiting to be printed is not available on
	 * the server.
	 */
	STATUS_NO_SPOOL_SPACE(0xC00000C7L, "Space to store the file that is waiting to be printed is not available on the server."),

	/** The requested print file has been canceled. */
	STATUS_PRINT_CANCELLED(0xC00000C8L, "The requested print file has been canceled."),

	/** The network name was deleted. */
	STATUS_NETWORK_NAME_DELETED(0xC00000C9L, "The network name was deleted."),

	/** Network access is denied. */
	STATUS_NETWORK_ACCESS_DENIED(0xC00000CAL, "Network access is denied."),

	/**
	 * {Incorrect Network Resource Type} The specified device type (LPT, for
	 * example) conflicts with the actual device type on the remote resource.
	 */
	STATUS_BAD_DEVICE_TYPE(
		0xC00000CBL,
		"{Incorrect Network Resource Type} The specified device type (LPT, for example) " +
		"conflicts with the actual device type on the remote resource."),

	/**
	 * {Network Name Not Found} The specified share name cannot be found on the
	 * remote server.
	 */
	STATUS_BAD_NETWORK_NAME(0xC00000CCL, "{Network Name Not Found} The specified share name cannot be found on the remote server."),

	/**
	 * The name limit for the network adapter card of the local computer was
	 * exceeded.
	 */
	STATUS_TOO_MANY_NAMES(0xC00000CDL, "The name limit for the network adapter card of the local computer was exceeded."),

	/** The network BIOS session limit was exceeded. */
	STATUS_TOO_MANY_SESSIONS(0xC00000CEL, "The network BIOS session limit was exceeded."),

	/** File sharing has been temporarily paused. */
	STATUS_SHARING_PAUSED(0xC00000CFL, "File sharing has been temporarily paused."),

	/**
	 * No more connections can be made to this remote computer at this time
	 * because the computer has already accepted the maximum number of
	 * connections.
	 */
	STATUS_REQUEST_NOT_ACCEPTED(
		0xC00000D0L,
		"No more connections can be made to this remote computer at this time because " +
		"the computer has already accepted the maximum number of connections."),

	/** Print or disk redirection is temporarily paused. */
	STATUS_REDIRECTOR_PAUSED(0xC00000D1L, "Print or disk redirection is temporarily paused."),

	/** A network data fault occurred. */
	STATUS_NET_WRITE_FAULT(0xC00000D2L, "A network data fault occurred."),

	/**
	 * The number of active profiling objects is at the maximum and no more can
	 * be started.
	 */
	STATUS_PROFILING_AT_LIMIT(0xC00000D3L, "The number of active profiling objects is at the maximum and no more can be started."),

	/**
	 * {Incorrect Volume} The destination file of a rename request is located on
	 * a different device than the source of the rename request.
	 */
	STATUS_NOT_SAME_DEVICE(0xC00000D4L,
		"{Incorrect Volume} The destination file of a rename request is located " +
		"on a different device than the source of the rename request."),

	/** The specified file has been renamed and thus cannot be modified. */
	STATUS_FILE_RENAMED(0xC00000D5L, "The specified file has been renamed and thus cannot be modified."),

	/**
	 * {Network Request Timeout} The session with a remote server has been
	 * disconnected because the time-out interval for a request has expired.
	 */
	STATUS_VIRTUAL_CIRCUIT_CLOSED(0xC00000D6L,
		"{Network Request Timeout} The session with a remote server has been " +
		"disconnected because the time-out interval for a request has expired."),

	/**
	 * Indicates an attempt was made to operate on the security of an object
	 * that does not have security associated with it.
	 */
	STATUS_NO_SECURITY_ON_OBJECT(0xC00000D7L,
		"Indicates an attempt was made to operate on the security of an object that does not have security associated with it."),

	/**
	 * Used to indicate that an operation cannot continue without blocking for
	 * I/O.
	 */
	STATUS_CANT_WAIT(0xC00000D8L, "Used to indicate that an operation cannot continue without blocking for I/O."),

	/** Used to indicate that a read operation was done on an empty pipe. */
	STATUS_PIPE_EMPTY(0xC00000D9L, "Used to indicate that a read operation was done on an empty pipe."),

	/**
	 * Configuration information could not be read from the domain controller,
	 * either because the machine is unavailable or access has been denied.
	 */
	STATUS_CANT_ACCESS_DOMAIN_INFO(
		0xC00000DAL,
		"Configuration information could not be read from the domain controller, " +
		"either because the machine is unavailable or access has been denied."),

	/**
	 * Indicates that a thread attempted to terminate itself by default (called
	 * NtTerminateThread with NULL) and it was the last thread in the current
	 * process.
	 */
	STATUS_CANT_TERMINATE_SELF(
		0xC00000DBL,
		"Indicates that a thread attempted to terminate itself by default (called " +
		"NtTerminateThread with NULL) and it was the last thread in the current process."),

	/**
	 * Indicates the Sam Server was in the wrong state to perform the desired
	 * operation.
	 */
	STATUS_INVALID_SERVER_STATE(0xC00000DCL, "Indicates the Sam Server was in the wrong state to perform the desired operation."),

	/**
	 * Indicates the domain was in the wrong state to perform the desired
	 * operation.
	 */
	STATUS_INVALID_DOMAIN_STATE(0xC00000DDL, "Indicates the domain was in the wrong state to perform the desired operation."),

	/**
	 * This operation is only allowed for the primary domain controller of the
	 * domain.
	 */
	STATUS_INVALID_DOMAIN_ROLE(0xC00000DEL, "This operation is only allowed for the primary domain controller of the domain."),

	/** The specified domain did not exist. */
	STATUS_NO_SUCH_DOMAIN(0xC00000DFL, "The specified domain did not exist."),

	/** The specified domain already exists. */
	STATUS_DOMAIN_EXISTS(0xC00000E0L, "The specified domain already exists."),

	/**
	 * An attempt was made to exceed the limit on the number of domains per
	 * server for this release.
	 */
	STATUS_DOMAIN_LIMIT_EXCEEDED(0xC00000E1L,
		"An attempt was made to exceed the limit on the number of domains per server for this release."),

	/**
	 * An error status returned when the opportunistic lock (oplock) request is
	 * denied.
	 */
	STATUS_OPLOCK_NOT_GRANTED(0xC00000E2L, "An error status returned when the opportunistic lock (oplock) request is denied."),

	/**
	 * An error status returned when an invalid opportunistic lock (oplock)
	 * acknowledgment is received by a file system.
	 */
	STATUS_INVALID_OPLOCK_PROTOCOL(0xC00000E3L,
		"An error status returned when an invalid opportunistic lock (oplock) acknowledgment is received by a file system."),

	/**
	 * This error indicates that the requested operation cannot be completed due
	 * to a catastrophic media failure or an on-disk data structure corruption.
	 */
	STATUS_INTERNAL_DB_CORRUPTION(
		0xC00000E4L,
		"This error indicates that the requested operation cannot be completed due " +
		"to a catastrophic media failure or an on-disk data structure corruption."),

	/** An internal error occurred. */
	STATUS_INTERNAL_ERROR(0xC00000E5L, "An internal error occurred."),

	/**
	 * Indicates generic access types were contained in an access mask which
	 * should already be mapped to non-generic access types.
	 */
	STATUS_GENERIC_NOT_MAPPED(0xC00000E6L,
		"Indicates generic access types were contained in an access mask which should already be mapped to non-generic access types."),

	/**
	 * Indicates a security descriptor is not in the necessary format (absolute
	 * or self-relative).
	 */
	STATUS_BAD_DESCRIPTOR_FORMAT(0xC00000E7L, "Indicates a security descriptor is not in the necessary format (absolute or self-relative)."),

	/**
	 * An access to a user buffer failed at an expected point in time. This code
	 * is defined because the caller does not want to accept
	 * STATUS_ACCESS_VIOLATION in its filter.
	 */
	STATUS_INVALID_USER_BUFFER(
		0xC00000E8L,
		"An access to a user buffer failed at an expected point in time. This code is " +
		"defined because the caller does not want to accept STATUS_ACCESS_VIOLATION in its filter."),

	/**
	 * If an I/O error that is not defined in the standard FsRtl filter is
	 * returned, it is converted to the following error, which is guaranteed to
	 * be in the filter. In this case, information is lost; however, the filter
	 * correctly handles the exception.
	 */
	STATUS_UNEXPECTED_IO_ERROR(
		0xC00000E9L,
		"If an I/O error that is not defined in the standard FsRtl filter is returned, it is " +
		"converted to the following error, which is guaranteed to be in the filter. In this case, " +
		"information is lost; however, the filter correctly handles the exception."),

	/**
	 * If an MM error that is not defined in the standard FsRtl filter is
	 * returned, it is converted to one of the following errors, which are
	 * guaranteed to be in the filter. In this case, information is lost;
	 * however, the filter correctly handles the exception.
	 */
	STATUS_UNEXPECTED_MM_CREATE_ERR(
		0xC00000EAL,
		"If an MM error that is not defined in the standard FsRtl filter is returned, it is " +
		"converted to one of the following errors, which are guaranteed to be in the filter. " +
		"In this case, information is lost; however, the filter correctly handles the exception."),

	/**
	 * If an MM error that is not defined in the standard FsRtl filter is
	 * returned, it is converted to one of the following errors, which are
	 * guaranteed to be in the filter. In this case, information is lost;
	 * however, the filter correctly handles the exception.
	 */
	STATUS_UNEXPECTED_MM_MAP_ERROR(
		0xC00000EBL,
		"If an MM error that is not defined in the standard FsRtl filter is returned, it is " +
		"converted to one of the following errors, which are guaranteed to be in the filter. " +
		"In this case, information is lost; however, the filter correctly handles the exception."),

	/**
	 * If an MM error that is not defined in the standard FsRtl filter is
	 * returned, it is converted to one of the following errors, which are
	 * guaranteed to be in the filter. In this case, information is lost;
	 * however, the filter correctly handles the exception.
	 */
	STATUS_UNEXPECTED_MM_EXTEND_ERR(
		0xC00000ECL,
		"If an MM error that is not defined in the standard FsRtl filter is returned, it is " +
		"converted to one of the following errors, which are guaranteed to be in the filter. " +
		"In this case, information is lost; however, the filter correctly handles the exception."),

	/**
	 * The requested action is restricted for use by logon processes only. The
	 * calling process has not registered as a logon process.
	 */
	STATUS_NOT_LOGON_PROCESS(0xC00000EDL,
		"The requested action is restricted for use by logon processes only. The calling process has not registered as a logon process."),

	/**
	 * An attempt has been made to start a new session manager or LSA logon
	 * session by using an ID that is already in use.
	 */
	STATUS_LOGON_SESSION_EXISTS(0xC00000EEL,
		"An attempt has been made to start a new session manager or LSA logon session by using an ID that is already in use."),

	/**
	 * An invalid parameter was passed to a service or function as the first
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_1(0xC00000EFL, "An invalid parameter was passed to a service or function as the first argument."),

	/**
	 * An invalid parameter was passed to a service or function as the second
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_2(0xC00000F0L, "An invalid parameter was passed to a service or function as the second argument."),

	/**
	 * An invalid parameter was passed to a service or function as the third
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_3(0xC00000F1L, "An invalid parameter was passed to a service or function as the third argument."),

	/**
	 * An invalid parameter was passed to a service or function as the fourth
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_4(0xC00000F2L, "An invalid parameter was passed to a service or function as the fourth argument."),

	/**
	 * An invalid parameter was passed to a service or function as the fifth
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_5(0xC00000F3L, "An invalid parameter was passed to a service or function as the fifth argument."),

	/**
	 * An invalid parameter was passed to a service or function as the sixth
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_6(0xC00000F4L, "An invalid parameter was passed to a service or function as the sixth argument."),

	/**
	 * An invalid parameter was passed to a service or function as the seventh
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_7(0xC00000F5L, "An invalid parameter was passed to a service or function as the seventh argument."),

	/**
	 * An invalid parameter was passed to a service or function as the eighth
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_8(0xC00000F6L, "An invalid parameter was passed to a service or function as the eighth argument."),

	/**
	 * An invalid parameter was passed to a service or function as the ninth
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_9(0xC00000F7L, "An invalid parameter was passed to a service or function as the ninth argument."),

	/**
	 * An invalid parameter was passed to a service or function as the tenth
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_10(0xC00000F8L, "An invalid parameter was passed to a service or function as the tenth argument."),

	/**
	 * An invalid parameter was passed to a service or function as the eleventh
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_11(0xC00000F9L, "An invalid parameter was passed to a service or function as the eleventh argument."),

	/**
	 * An invalid parameter was passed to a service or function as the twelfth
	 * argument.
	 */
	STATUS_INVALID_PARAMETER_12(0xC00000FAL, "An invalid parameter was passed to a service or function as the twelfth argument."),

	/**
	 * An attempt was made to access a network file, but the network software
	 * was not yet started.
	 */
	STATUS_REDIRECTOR_NOT_STARTED(
		0xC00000FBL,
		"An attempt was made to access a network file, but the network software was not yet started."
	),

	/**
	 * An attempt was made to start the redirector, but the redirector has
	 * already been started.
	 */
	STATUS_REDIRECTOR_STARTED(0xC00000FCL, "An attempt was made to start the redirector, but the redirector has already been started."),

	/** A new guard page for the stack cannot be created. */
	STATUS_STACK_OVERFLOW(0xC00000FDL, "A new guard page for the stack cannot be created."),

	/** A specified authentication package is unknown. */
	STATUS_NO_SUCH_PACKAGE(0xC00000FEL, "A specified authentication package is unknown."),

	/** A malformed function table was encountered during an unwind operation. */
	STATUS_BAD_FUNCTION_TABLE(0xC00000FFL, "A malformed function table was encountered during an unwind operation."),

	/**
	 * Indicates the specified environment variable name was not found in the
	 * specified environment block.
	 */
	STATUS_VARIABLE_NOT_FOUND(0xC0000100L,
		"Indicates the specified environment variable name was not found in the specified environment block."),

	/** Indicates that the directory trying to be deleted is not empty. */
	STATUS_DIRECTORY_NOT_EMPTY(0xC0000101L, "Indicates that the directory trying to be deleted is not empty."),

	/**
	 * {Corrupt File} The file or directory %hs is corrupt and unreadable. Run
	 * the Chkdsk utility.
	 */
	STATUS_FILE_CORRUPT_ERROR(0xC0000102L, "{Corrupt File} The file or directory %hs is corrupt and unreadable. Run the Chkdsk utility."),

	/** A requested opened file is not a directory. */
	STATUS_NOT_A_DIRECTORY(0xC0000103L, "A requested opened file is not a directory."),

	/**
	 * The logon session is not in a state that is consistent with the requested
	 * operation.
	 */
	STATUS_BAD_LOGON_SESSION_STATE(0xC0000104L, "The logon session is not in a state that is consistent with the requested operation."),

	/**
	 * An internal LSA error has occurred. An authentication package has
	 * requested the creation of a logon session but the ID of an already
	 * existing logon session has been specified.
	 */
	STATUS_LOGON_SESSION_COLLISION(
		0xC0000105L,
		"An internal LSA error has occurred. An authentication package has " +
		"requested the creation of a logon session but the ID of an already" +
		" existing logon session has been specified."),

	/** A specified name string is too long for its intended use. */
	STATUS_NAME_TOO_LONG(0xC0000106L, "A specified name string is too long for its intended use."),

	/**
	 * The user attempted to force close the files on a redirected drive, but
	 * there were opened files on the drive, and the user did not specify a
	 * sufficient level of force.
	 */
	STATUS_FILES_OPEN(
		0xC0000107L,
		"The user attempted to force close the files on a redirected drive, but there were opened " +
		"files on the drive, and the user did not specify a sufficient level of force."),

	/**
	 * The user attempted to force close the files on a redirected drive, but
	 * there were opened directories on the drive, and the user did not specify
	 * a sufficient level of force.
	 */
	STATUS_CONNECTION_IN_USE(
		0xC0000108L,
		"The user attempted to force close the files on a redirected drive, but there were opened " +
		"directories on the drive, and the user did not specify a sufficient level of force."),

	/**
	 * RtlFindMessage could not locate the requested message ID in the message
	 * table resource.
	 */
	STATUS_MESSAGE_NOT_FOUND(0xC0000109L, "RtlFindMessage could not locate the requested message ID in the message table resource."),

	/**
	 * An attempt was made to duplicate an object handle into or out of an
	 * exiting process.
	 */
	STATUS_PROCESS_IS_TERMINATING(0xC000010AL, "An attempt was made to duplicate an object handle into or out of an exiting process."),

	/**
	 * Indicates an invalid value has been provided for the LogonType requested.
	 */
	STATUS_INVALID_LOGON_TYPE(0xC000010BL, "Indicates an invalid value has been provided for the LogonType requested."),

	/**
	 * Indicates that an attempt was made to assign protection to a file system
	 * file or directory and one of the SIDs in the security descriptor could
	 * not be translated into a GUID that could be stored by the file system.
	 * This causes the protection attempt to fail, which might cause a file
	 * creation attempt to fail.
	 */
	STATUS_NO_GUID_TRANSLATION(
		0xC000010CL,
		"Indicates that an attempt was made to assign protection to a file system file or directory " +
		"and one of the SIDs in the security descriptor could not be translated into a GUID that " +
		"could be stored by the file system. This causes the protection attempt to fail, which " +
		"might cause a file creation attempt to fail."),

	/**
	 * Indicates that an attempt has been made to impersonate via a named pipe
	 * that has not yet been read from.
	 */
	STATUS_CANNOT_IMPERSONATE(0xC000010DL,
		"Indicates that an attempt has been made to impersonate via a named pipe that has not yet been read from."),

	/** Indicates that the specified image is already loaded. */
	STATUS_IMAGE_ALREADY_LOADED(0xC000010EL, "Indicates that the specified image is already loaded."),

	/**
	 * Indicates that an attempt was made to change the size of the LDT for a
	 * process that has no LDT.
	 */
	STATUS_NO_LDT(0xC0000117L, "Indicates that an attempt was made to change the size of the LDT for a process that has no LDT."),

	/**
	 * Indicates that an attempt was made to grow an LDT by setting its size, or
	 * that the size was not an even number of selectors.
	 */
	STATUS_INVALID_LDT_SIZE(0xC0000118L,
		"Indicates that an attempt was made to grow an LDT by setting its size, or that the size was not an even number of selectors."),

	/**
	 * Indicates that the starting value for the LDT information was not an
	 * integral multiple of the selector size.
	 */
	STATUS_INVALID_LDT_OFFSET(0xC0000119L,
		"Indicates that the starting value for the LDT information was not an integral multiple of the selector size."),

	/**
	 * Indicates that the user supplied an invalid descriptor when trying to set
	 * up LDT descriptors.
	 */
	STATUS_INVALID_LDT_DESCRIPTOR(0xC000011AL,
		"Indicates that the user supplied an invalid descriptor when trying to set up LDT descriptors."),

	/**
	 * The specified image file did not have the correct format. It appears to
	 * be NE format.
	 */
	STATUS_INVALID_IMAGE_NE_FORMAT(0xC000011BL, "The specified image file did not have the correct format. It appears to be NE format."),

	/**
	 * Indicates that the transaction state of a registry subtree is
	 * incompatible with the requested operation. For example, a request has
	 * been made to start a new transaction with one already in progress, or a
	 * request has been made to apply a transaction when one is not currently in
	 * progress.
	 */
	STATUS_RXACT_INVALID_STATE(
		0xC000011CL,
		"Indicates that the transaction state of a registry subtree is incompatible with the " +
		"requested operation. For example, a request has been made to start a new transaction " +
		"with one already in progress, or a request has been made to apply a transaction when " +
		"one is not currently in progress."),

	/**
	 * Indicates an error has occurred during a registry transaction commit. The
	 * database has been left in an unknown, but probably inconsistent, state.
	 * The state of the registry transaction is left as COMMITTING.
	 */
	STATUS_RXACT_COMMIT_FAILURE(
		0xC000011DL,
		"Indicates an error has occurred during a registry transaction commit. The database has " +
		"been left in an unknown, but probably inconsistent, state. The state of the registry " +
		"transaction is left as COMMITTING."),

	/**
	 * An attempt was made to map a file of size zero with the maximum size
	 * specified as zero.
	 */
	STATUS_MAPPED_FILE_SIZE_ZERO(0xC000011EL, "An attempt was made to map a file of size zero with the maximum size specified as zero."),

	/**
	 * Too many files are opened on a remote server. This error should only be
	 * returned by the Windows redirector on a remote drive.
	 */
	STATUS_TOO_MANY_OPENED_FILES(0xC000011FL,
		"Too many files are opened on a remote server. This error should only be returned by the Windows redirector on a remote drive."),

	/** The I/O request was canceled. */
	STATUS_CANCELLED(0xC0000120L, "The I/O request was canceled."),

	/**
	 * An attempt has been made to remove a file or directory that cannot be
	 * deleted.
	 */
	STATUS_CANNOT_DELETE(0xC0000121L, "An attempt has been made to remove a file or directory that cannot be deleted."),

	/**
	 * Indicates a name that was specified as a remote computer name is
	 * syntactically invalid.
	 */
	STATUS_INVALID_COMPUTER_NAME(0xC0000122L, "Indicates a name that was specified as a remote computer name is syntactically invalid."),

	/**
	 * An I/O request other than close was performed on a file after it was
	 * deleted, which can only happen to a request that did not complete before
	 * the last handle was closed via NtClose.
	 */
	STATUS_FILE_DELETED(
		0xC0000123L,
		"An I/O request other than close was performed on a file after it was deleted, which can " +
		"only happen to a request that did not complete before the last handle was closed via NtClose."),

	/**
	 * Indicates an operation that is incompatible with built-in accounts has
	 * been attempted on a built-in (special) SAM account. For example, built-in
	 * accounts cannot be deleted.
	 */
	STATUS_SPECIAL_ACCOUNT(
		0xC0000124L,
		"Indicates an operation that is incompatible with built-in accounts has been attempted " +
		"on a built-in (special) SAM account. For example, built-in accounts cannot be deleted."),

	/**
	 * The operation requested cannot be performed on the specified group
	 * because it is a built-in special group.
	 */
	STATUS_SPECIAL_GROUP(0xC0000125L,
		"The operation requested cannot be performed on the specified group because it is a built-in special group."),

	/**
	 * The operation requested cannot be performed on the specified user because
	 * it is a built-in special user.
	 */
	STATUS_SPECIAL_USER(0xC0000126L,
		"The operation requested cannot be performed on the specified user because it is a built-in special user."),

	/**
	 * Indicates a member cannot be removed from a group because the group is
	 * currently the member's primary group.
	 */
	STATUS_MEMBERS_PRIMARY_GROUP(0xC0000127L,
		"Indicates a member cannot be removed from a group because the group is currently the member's primary group."),

	/**
	 * An I/O request other than close and several other special case operations
	 * was attempted using a file object that had already been closed.
	 */
	STATUS_FILE_CLOSED(0xC0000128L,
		"An I/O request other than close and several other special case operations " +
		"was attempted using a file object that had already been closed."),

	/**
	 * Indicates a process has too many threads to perform the requested action.
	 * For example, assignment of a primary token can be performed only when a
	 * process has zero or one threads.
	 */
	STATUS_TOO_MANY_THREADS(
		0xC0000129L,
		"Indicates a process has too many threads to perform the requested action. For " +
		"example, assignment of a primary token can be performed only when a process has zero or one threads."),

	/**
	 * An attempt was made to operate on a thread within a specific process, but
	 * the specified thread is not in the specified process.
	 */
	STATUS_THREAD_NOT_IN_PROCESS(0xC000012AL,
		"An attempt was made to operate on a thread within a specific process, but the specified thread is not in the specified process."),

	/**
	 * An attempt was made to establish a token for use as a primary token but
	 * the token is already in use. A token can only be the primary token of one
	 * process at a time.
	 */
	STATUS_TOKEN_ALREADY_IN_USE(
		0xC000012BL,
		"An attempt was made to establish a token for use as a primary token but the token is " +
		"already in use. A token can only be the primary token of one process at a time."),

	/** The page file quota was exceeded. */
	STATUS_PAGEFILE_QUOTA_EXCEEDED(0xC000012CL, "The page file quota was exceeded."),

	/**
	 * {Out of Virtual Memory} Your system is low on virtual memory. To ensure
	 * that Windows runs correctly, increase the size of your virtual memory
	 * paging file. For more information, see Help.
	 */
	STATUS_COMMITMENT_LIMIT(
		0xC000012DL,
		"{Out of Virtual Memory} Your system is low on virtual memory. To ensure that Windows " +
		"runs correctly, increase the size of your virtual memory paging file. For more information, see Help."),

	/**
	 * The specified image file did not have the correct format: it appears to
	 * be LE format.
	 */
	STATUS_INVALID_IMAGE_LE_FORMAT(0xC000012EL, "The specified image file did not have the correct format: it appears to be LE format."),

	/**
	 * The specified image file did not have the correct format: it did not have
	 * an initial MZ.
	 */
	STATUS_INVALID_IMAGE_NOT_MZ(0xC000012FL, "The specified image file did not have the correct format: it did not have an initial MZ."),

	/**
	 * The specified image file did not have the correct format: it did not have
	 * a proper e_lfarlc in the MZ header.
	 */
	STATUS_INVALID_IMAGE_PROTECT(0xC0000130L,
		"The specified image file did not have the correct format: it did not have a proper e_lfarlc in the MZ header."),

	/**
	 * The specified image file did not have the correct format: it appears to
	 * be a 16-bit Windows image.
	 */
	STATUS_INVALID_IMAGE_WIN_16(0xC0000131L,
		"The specified image file did not have the correct format: it appears to be a 16-bit Windows image."),

	/**
	 * The Netlogon service cannot start because another Netlogon service
	 * running in the domain conflicts with the specified role.
	 */
	STATUS_LOGON_SERVER_CONFLICT(0xC0000132L,
		"The Netlogon service cannot start because another Netlogon service running in the domain conflicts with the specified role."),

	/**
	 * The time at the primary domain controller is different from the time at
	 * the backup domain controller or member server by too large an amount.
	 */
	STATUS_TIME_DIFFERENCE_AT_DC(
		0xC0000133L,
		"The time at the primary domain controller is different from the time at the " +
		"backup domain controller or member server by too large an amount."),

	/**
	 * The SAM database on a Windows Server operating system is significantly
	 * out of synchronization with the copy on the domain controller. A complete
	 * synchronization is required.
	 */
	STATUS_SYNCHRONIZATION_REQUIRED(
		0xC0000134L,
		"The SAM database on a Windows Server operating system is significantly out of " +
		"synchronization with the copy on the domain controller. A complete synchronization is required."),

	/**
	 * {Unable To Locate Component} This application has failed to start because
	 * %hs was not found. Reinstalling the application might fix this problem.
	 */
	STATUS_DLL_NOT_FOUND(
		0xC0000135L,
		"{Unable To Locate Component} This application has failed to start because %hs " +
		"was not found. Reinstalling the application might fix this problem."),

	/**
	 * The NtCreateFile API failed. This error should never be returned to an
	 * application; it is a place holder for the Windows LAN Manager Redirector
	 * to use in its internal error-mapping routines.
	 */
	STATUS_OPEN_FAILED(
		0xC0000136L,
		"The NtCreateFile API failed. This error should never be returned to an application; " +
		"it is a place holder for the Windows LAN Manager Redirector to use in its internal error-mapping routines."),

	/**
	 * {Privilege Failed} The I/O permissions for the process could not be
	 * changed.
	 */
	STATUS_IO_PRIVILEGE_FAILED(0xC0000137L, "{Privilege Failed} The I/O permissions for the process could not be changed."),

	/**
	 * {Ordinal Not Found} The ordinal %ld could not be located in the dynamic
	 * link library %hs.
	 */
	STATUS_ORDINAL_NOT_FOUND(0xC0000138L, "{Ordinal Not Found} The ordinal %ld could not be located in the dynamic link library %hs."),

	/**
	 * {Entry Point Not Found} The procedure entry point %hs could not be
	 * located in the dynamic link library %hs.
	 */
	STATUS_ENTRYPOINT_NOT_FOUND(0xC0000139L,
		"{Entry Point Not Found} The procedure entry point %hs could not be located in the dynamic link library %hs."),

	/**
	 * {Application Exit by CTRL+C} The application terminated as a result of a
	 * CTRL+C.
	 */
	STATUS_CONTROL_C_EXIT(0xC000013AL, "{Application Exit by CTRL+C} The application terminated as a result of a CTRL+C."),

	/**
	 * {Virtual Circuit Closed} The network transport on your computer has
	 * closed a network connection. There might or might not be I/O requests
	 * outstanding.
	 */
	STATUS_LOCAL_DISCONNECT(
		0xC000013BL,
		"{Virtual Circuit Closed} The network transport on your computer has closed a " +
		"network connection. There might or might not be I/O requests outstanding."),

	/**
	 * {Virtual Circuit Closed} The network transport on a remote computer has
	 * closed a network connection. There might or might not be I/O requests
	 * outstanding.
	 */
	STATUS_REMOTE_DISCONNECT(
		0xC000013CL,
		"{Virtual Circuit Closed} The network transport on a remote computer has " +
		"closed a network connection. There might or might not be I/O requests outstanding."),

	/**
	 * {Insufficient Resources on Remote Computer} The remote computer has
	 * insufficient resources to complete the network request. For example, the
	 * remote computer might not have enough available memory to carry out the
	 * request at this time.
	 */
	STATUS_REMOTE_RESOURCES(
		0xC000013DL,
		"{Insufficient Resources on Remote Computer} The remote computer has insufficient " +
		"resources to complete the network request. For example, the remote computer might " +
		"not have enough available memory to carry out the request at this time."),

	/**
	 * {Virtual Circuit Closed} An existing connection (virtual circuit) has
	 * been broken at the remote computer. There is probably something wrong
	 * with the network software protocol or the network hardware on the remote
	 * computer.
	 */
	STATUS_LINK_FAILED(
		0xC000013EL,
		"{Virtual Circuit Closed} An existing connection (virtual circuit) has been broken " +
		"at the remote computer. There is probably something wrong with the network software " +
		"protocol or the network hardware on the remote computer."),

	/**
	 * {Virtual Circuit Closed} The network transport on your computer has
	 * closed a network connection because it had to wait too long for a
	 * response from the remote computer.
	 */
	STATUS_LINK_TIMEOUT(
		0xC000013FL,
		"{Virtual Circuit Closed} The network transport on your computer has closed a network " +
		"connection because it had to wait too long for a response from the remote computer."),

	/** The connection handle that was given to the transport was invalid. */
	STATUS_INVALID_CONNECTION(0xC0000140L, "The connection handle that was given to the transport was invalid."),

	/** The address handle that was given to the transport was invalid. */
	STATUS_INVALID_ADDRESS(0xC0000141L, "The address handle that was given to the transport was invalid."),

	/**
	 * {DLL Initialization Failed} Initialization of the dynamic link library
	 * %hs failed. The process is terminating abnormally.
	 */
	STATUS_DLL_INIT_FAILED(0xC0000142L,
		"{DLL Initialization Failed} Initialization of the dynamic link library %hs failed. The process is terminating abnormally."),

	/** {Missing System File} The required system file %hs is bad or missing. */
	STATUS_MISSING_SYSTEMFILE(0xC0000143L, "{Missing System File} The required system file %hs is bad or missing."),

	/**
	 * {Application Error} The exception %s (0x%08lx) occurred in the
	 * application at location 0x%08lx.
	 */
	STATUS_UNHANDLED_EXCEPTION(0xC0000144L,
		"{Application Error} The exception %s (0x%08lx) occurred in the application at location 0x%08lx."),

	/**
	 * {Application Error} The application failed to initialize properly
	 * (0x%lx). Click OK to terminate the application.
	 */
	STATUS_APP_INIT_FAILURE(0xC0000145L,
		"{Application Error} The application failed to initialize properly (0x%lx). Click OK to terminate the application."),

	/**
	 * {Unable to Create Paging File} The creation of the paging file %hs failed
	 * (%lx). The requested size was %ld.
	 */
	STATUS_PAGEFILE_CREATE_FAILED(0xC0000146L,
		"{Unable to Create Paging File} The creation of the paging file %hs failed (%lx). The requested size was %ld."),

	/**
	 * {No Paging File Specified} No paging file was specified in the system
	 * configuration.
	 */
	STATUS_NO_PAGEFILE(0xC0000147L, "{No Paging File Specified} No paging file was specified in the system configuration."),

	/**
	 * {Incorrect System Call Level} An invalid level was passed into the
	 * specified system call.
	 */
	STATUS_INVALID_LEVEL(0xC0000148L, "{Incorrect System Call Level} An invalid level was passed into the specified system call."),

	/**
	 * {Incorrect Password to LAN Manager Server} You specified an incorrect
	 * password to a LAN Manager 2.x or MS-NET server.
	 */
	STATUS_WRONG_PASSWORD_CORE(0xC0000149L,
		"{Incorrect Password to LAN Manager Server} You specified an incorrect password to a LAN Manager 2.x or MS-NET server."),

	/**
	 * {EXCEPTION} A real-mode application issued a floating-point instruction
	 * and floating-point hardware is not present.
	 */
	STATUS_ILLEGAL_FLOAT_CONTEXT(0xC000014AL,
		"{EXCEPTION} A real-mode application issued a floating-point instruction and floating-point hardware is not present."),

	/**
	 * The pipe operation has failed because the other end of the pipe has been
	 * closed.
	 */
	STATUS_PIPE_BROKEN(0xC000014BL, "The pipe operation has failed because the other end of the pipe has been closed."),

	/**
	 * {The Registry Is Corrupt} The structure of one of the files that contains
	 * registry data is corrupt; the image of the file in memory is corrupt; or
	 * the file could not be recovered because the alternate copy or log was
	 * absent or corrupt.
	 */
	STATUS_REGISTRY_CORRUPT(
		0xC000014CL,
		"{The Registry Is Corrupt} The structure of one of the files that contains registry " +
		"data is corrupt; the image of the file in memory is corrupt; or the file could not " +
		"be recovered because the alternate copy or log was absent or corrupt."),

	/**
	 * An I/O operation initiated by the Registry failed and cannot be
	 * recovered. The registry could not read in, write out, or flush one of the
	 * files that contain the system's image of the registry.
	 */
	STATUS_REGISTRY_IO_FAILED(
		0xC000014DL,
		"An I/O operation initiated by the Registry failed and cannot be recovered. The registry " +
		"could not read in, write out, or flush one of the files that contain the system's image of the registry."),

	/**
	 * An event pair synchronization operation was performed using the
	 * thread-specific client/server event pair object, but no event pair object
	 * was associated with the thread.
	 */
	STATUS_NO_EVENT_PAIR(
		0xC000014EL,
		"An event pair synchronization operation was performed using the thread-specific client/server " +
		"event pair object, but no event pair object was associated with the thread."),

	/**
	 * The volume does not contain a recognized file system. Be sure that all
	 * required file system drivers are loaded and that the volume is not
	 * corrupt.
	 */
	STATUS_UNRECOGNIZED_VOLUME(
		0xC000014FL,
		"The volume does not contain a recognized file system. Be sure that all required " +
		"file system drivers are loaded and that the volume is not corrupt."),

	/**
	 * No serial device was successfully initialized. The serial driver will
	 * unload.
	 */
	STATUS_SERIAL_NO_DEVICE_INITED(0xC0000150L, "No serial device was successfully initialized. The serial driver will unload."),

	/** The specified local group does not exist. */
	STATUS_NO_SUCH_ALIAS(0xC0000151L, "The specified local group does not exist."),

	/** The specified account name is not a member of the group. */
	STATUS_MEMBER_NOT_IN_ALIAS(0xC0000152L, "The specified account name is not a member of the group."),

	/** The specified account name is already a member of the group. */
	STATUS_MEMBER_IN_ALIAS(0xC0000153L, "The specified account name is already a member of the group."),

	/** The specified local group already exists. */
	STATUS_ALIAS_EXISTS(0xC0000154L, "The specified local group already exists."),

	/**
	 * A requested type of logon (for example, interactive, network, and
	 * service) is not granted by the local security policy of the target
	 * system. Ask the system administrator to grant the necessary form of logon.
	 */
	STATUS_LOGON_NOT_GRANTED(
		0xC0000155L,
		"A requested type of logon (for example, interactive, network, and service) is not granted " +
		"by the local security policy of the target system. Ask the system administrator to grant " +
		"the necessary form of logon."),

	/**
	 * The maximum number of secrets that can be stored in a single system was
	 * exceeded. The length and number of secrets is limited to satisfy U.S.
	 * State Department export restrictions.
	 */
	STATUS_TOO_MANY_SECRETS(
		0xC0000156L,
		"The maximum number of secrets that can be stored in a single system was exceeded. " +
		"The length and number of secrets is limited to satisfy U.S. State Department export restrictions."),

	/**
	 * The length of a secret exceeds the maximum allowable length. The length
	 * and number of secrets is limited to satisfy U.S. State Department export
	 * restrictions.
	 */
	STATUS_SECRET_TOO_LONG(
		0xC0000157L,
		"The length of a secret exceeds the maximum allowable length. The length and number of " +
		"secrets is limited to satisfy U.S. State Department export restrictions."),

	/**
	 * The local security authority (LSA) database contains an internal
	 * inconsistency.
	 */
	STATUS_INTERNAL_DB_ERROR(0xC0000158L, "The local security authority (LSA) database contains an internal inconsistency."),

	/** The requested operation cannot be performed in full-screen mode. */
	STATUS_FULLSCREEN_MODE(0xC0000159L, "The requested operation cannot be performed in full-screen mode."),

	/**
	 * During a logon attempt, the user's security context accumulated too many
	 * security IDs. This is a very unusual situation. Remove the user from some
	 * global or local groups to reduce the number of security IDs to incorporate
	 * into the security context.
	 */
	STATUS_TOO_MANY_CONTEXT_IDS(
		0xC000015AL,
		"During a logon attempt, the user's security context accumulated too many security IDs. " +
		"This is a very unusual situation. Remove the user from some global or local groups to " +
		"reduce the number of security IDs to incorporate into the security context."),

	/**
	 * A user has requested a type of logon (for example, interactive or
	 * network) that has not been granted. An administrator has control over who
	 * can logon interactively and through the network.
	 */
	STATUS_LOGON_TYPE_NOT_GRANTED(
		0xC000015BL,
		"A user has requested a type of logon (for example, interactive or network) that has not " +
		"been granted. An administrator has control over who can logon interactively and through the network."),

	/**
	 * The system has attempted to load or restore a file into the registry, and
	 * the specified file is not in the format of a registry file.
	 */
	STATUS_NOT_REGISTRY_FILE(0xC000015CL,
		"The system has attempted to load or restore a file into the registry, " +
		"and the specified file is not in the format of a registry file."),

	/**
	 * An attempt was made to change a user password in the security account
	 * manager without providing the necessary Windows cross-encrypted password.
	 */
	STATUS_NT_CROSS_ENCRYPTION_REQUIRED(
		0xC000015DL,
		"An attempt was made to change a user password in the security account " +
		"manager without providing the necessary Windows cross-encrypted password."),

	/** A Windows Server has an incorrect configuration. */
	STATUS_DOMAIN_CTRLR_CONFIG_ERROR(0xC000015EL, "A Windows Server has an incorrect configuration."),

	/**
	 * An attempt was made to explicitly access the secondary copy of
	 * information via a device control to the fault tolerance driver and the
	 * secondary copy is not present in the system.
	 */
	STATUS_FT_MISSING_MEMBER(
		0xC000015FL,
		"An attempt was made to explicitly access the secondary copy of information via a device " +
		"control to the fault tolerance driver and the secondary copy is not present in the system."),

	/**
	 * A configuration registry node that represents a driver service entry was
	 * ill-formed and did not contain the required value entries.
	 */
	STATUS_ILL_FORMED_SERVICE_ENTRY(0xC0000160L,
		"A configuration registry node that represents a driver service entry " +
		"was ill-formed and did not contain the required value entries."),

	/**
	 * An illegal character was encountered. For a multibyte character set, this
	 * includes a lead byte without a succeeding trail byte. For the Unicode
	 * character set this includes the characters 0xFFFF and 0xFFFE.
	 */
	STATUS_ILLEGAL_CHARACTER(
		0xC0000161L,
		"An illegal character was encountered. For a multibyte character set, this includes a lead byte " +
		"without a succeeding trail byte. For the Unicode character set this includes the characters 0xFFFF and 0xFFFE."),

	/**
	 * No mapping for the Unicode character exists in the target multibyte code
	 * page.
	 */
	STATUS_UNMAPPABLE_CHARACTER(0xC0000162L, "No mapping for the Unicode character exists in the target multibyte code page."),

	/**
	 * The Unicode character is not defined in the Unicode character set that is
	 * installed on the system.
	 */
	STATUS_UNDEFINED_CHARACTER(0xC0000163L,
		"The Unicode character is not defined in the Unicode character set that is installed on the system."),

	/** The paging file cannot be created on a floppy disk. */
	STATUS_FLOPPY_VOLUME(0xC0000164L, "The paging file cannot be created on a floppy disk."),

	/**
	 * {Floppy Disk Error} While accessing a floppy disk, an ID address mark was
	 * not found.
	 */
	STATUS_FLOPPY_ID_MARK_NOT_FOUND(0xC0000165L, "{Floppy Disk Error} While accessing a floppy disk, an ID address mark was not found."),

	/**
	 * {Floppy Disk Error} While accessing a floppy disk, the track address from
	 * the sector ID field was found to be different from the track address that
	 * is maintained by the controller.
	 */
	STATUS_FLOPPY_WRONG_CYLINDER(
		0xC0000166L,
		"{Floppy Disk Error} While accessing a floppy disk, the track address from the sector ID " +
		"field was found to be different from the track address that is maintained by the controller."),

	/**
	 * {Floppy Disk Error} The floppy disk controller reported an error that is
	 * not recognized by the floppy disk driver.
	 */
	STATUS_FLOPPY_UNKNOWN_ERROR(0xC0000167L,
		"{Floppy Disk Error} The floppy disk controller reported an error that is not recognized by the floppy disk driver."),

	/**
	 * {Floppy Disk Error} While accessing a floppy-disk, the controller
	 * returned inconsistent results via its registers.
	 */
	STATUS_FLOPPY_BAD_REGISTERS(0xC0000168L,
		"{Floppy Disk Error} While accessing a floppy-disk, the controller returned inconsistent results via its registers."),

	/**
	 * {Hard Disk Error} While accessing the hard disk, a recalibrate operation
	 * failed, even after retries.
	 */
	STATUS_DISK_RECALIBRATE_FAILED(0xC0000169L,
		"{Hard Disk Error} While accessing the hard disk, a recalibrate operation failed, even after retries."),

	/**
	 * {Hard Disk Error} While accessing the hard disk, a disk operation failed
	 * even after retries.
	 */
	STATUS_DISK_OPERATION_FAILED(0xC000016AL, "{Hard Disk Error} While accessing " +
		"the hard disk, a disk operation failed even after retries."),

	/**
	 * {Hard Disk Error} While accessing the hard disk, a disk controller reset
	 * was needed, but even that failed.
	 */
	STATUS_DISK_RESET_FAILED(0xC000016BL,
		"{Hard Disk Error} While accessing the hard disk, a disk controller reset was needed, but even that failed."),

	/**
	 * An attempt was made to open a device that was sharing an interrupt
	 * request (IRQ) with other devices. At least one other device that uses
	 * that IRQ was already opened. Two concurrent opens of devices that share an
	 * IRQ and only work via interrupts is not supported for the particular bus
	 * type that the devices use.
	 */
	STATUS_SHARED_IRQ_BUSY(
		0xC000016CL,
		"An attempt was made to open a device that was sharing an interrupt request (IRQ) with other " +
		"devices. At least one other device that uses that IRQ was already opened. Two concurrent " +
		"opens of devices that share an IRQ and only work via interrupts is not supported for the " +
		"particular bus type that the devices use."),

	/**
	 * {FT Orphaning} A disk that is part of a fault-tolerant volume can no
	 * longer be accessed.
	 */
	STATUS_FT_ORPHANING(0xC000016DL, "{FT Orphaning} A disk that is part of a fault-tolerant volume can no longer be accessed."),

	/**
	 * The basic input/output system (BIOS) failed to connect a system interrupt
	 * to the device or bus for which the device is connected.
	 */
	STATUS_BIOS_FAILED_TO_CONNECT_INTERRUPT(
		0xC000016EL,
		"The basic input/output system (BIOS) failed to connect a system interrupt to " +
		"the device or bus for which the device is connected."),

	/** The tape could not be partitioned. */
	STATUS_PARTITION_FAILURE(0xC0000172L, "The tape could not be partitioned."),

	/**
	 * When accessing a new tape of a multi-volume partition, the current
	 * blocksize is incorrect.
	 */
	STATUS_INVALID_BLOCK_LENGTH(0xC0000173L, "When accessing a new tape of a multi-volume partition, the current blocksize is incorrect."),

	/** The tape partition information could not be found when loading a tape. */
	STATUS_DEVICE_NOT_PARTITIONED(0xC0000174L, "The tape partition information could not be found when loading a tape."),

	/** An attempt to lock the eject media mechanism failed. */
	STATUS_UNABLE_TO_LOCK_MEDIA(0xC0000175L, "An attempt to lock the eject media mechanism failed."),

	/** An attempt to unload media failed. */
	STATUS_UNABLE_TO_UNLOAD_MEDIA(0xC0000176L, "An attempt to unload media failed."),

	/** The physical end of tape was detected. */
	STATUS_EOM_OVERFLOW(0xC0000177L, "The physical end of tape was detected."),

	/** {No Media} There is no media in the drive. Insert media into drive %hs. */
	STATUS_NO_MEDIA(0xC0000178L, "{No Media} There is no media in the drive. Insert media into drive %hs."),

	/**
	 * A member could not be added to or removed from the local group because
	 * the member does not exist.
	 */
	STATUS_NO_SUCH_MEMBER(0xC000017AL, "A member could not be added to or removed from the local group because the member does not exist."),

	/**
	 * A new member could not be added to a local group because the member has
	 * the wrong account type.
	 */
	STATUS_INVALID_MEMBER(0xC000017BL, "A new member could not be added to a local group because the member has the wrong account type."),

	/**
	 * An illegal operation was attempted on a registry key that has been marked
	 * for deletion.
	 */
	STATUS_KEY_DELETED(0xC000017CL, "An illegal operation was attempted on a registry key that has been marked for deletion."),

	/** The system could not allocate the required space in a registry log. */
	STATUS_NO_LOG_SPACE(0xC000017DL, "The system could not allocate the required space in a registry log."),

	/** Too many SIDs have been specified. */
	STATUS_TOO_MANY_SIDS(0xC000017EL, "Too many SIDs have been specified."),

	/**
	 * An attempt was made to change a user password in the security account
	 * manager without providing the necessary LM cross-encrypted password.
	 */
	STATUS_LM_CROSS_ENCRYPTION_REQUIRED(
		0xC000017FL,
		"An attempt was made to change a user password in the security " +
		"account manager without providing the necessary LM cross-encrypted password."),

	/**
	 * An attempt was made to create a symbolic link in a registry key that
	 * already has subkeys or values.
	 */
	STATUS_KEY_HAS_CHILDREN(0xC0000180L,
		"An attempt was made to create a symbolic link in a registry key that already has subkeys or values."),

	/**
	 * An attempt was made to create a stable subkey under a volatile parent
	 * key.
	 */
	STATUS_CHILD_MUST_BE_VOLATILE(0xC0000181L, "An attempt was made to create a stable subkey under a volatile parent key."),

	/**
	 * The I/O device is configured incorrectly or the configuration parameters
	 * to the driver are incorrect.
	 */
	STATUS_DEVICE_CONFIGURATION_ERROR(0xC0000182L,
		"The I/O device is configured incorrectly or the configuration parameters to the driver are incorrect."),

	/** An error was detected between two drivers or within an I/O driver. */
	STATUS_DRIVER_INTERNAL_ERROR(0xC0000183L, "An error was detected between two drivers or within an I/O driver."),

	/** The device is not in a valid state to perform this request. */
	STATUS_INVALID_DEVICE_STATE(0xC0000184L, "The device is not in a valid state to perform this request."),

	/** The I/O device reported an I/O error. */
	STATUS_IO_DEVICE_ERROR(0xC0000185L, "The I/O device reported an I/O error."),

	/** A protocol error was detected between the driver and the device. */
	STATUS_DEVICE_PROTOCOL_ERROR(0xC0000186L, "A protocol error was detected between the driver and the device."),

	/**
	 * This operation is only allowed for the primary domain controller of the
	 * domain.
	 */
	STATUS_BACKUP_CONTROLLER(0xC0000187L, "This operation is only allowed for the primary domain controller of the domain."),

	/** The log file space is insufficient to support this operation. */
	STATUS_LOG_FILE_FULL(0xC0000188L, "The log file space is insufficient to support this operation."),

	/** A write operation was attempted to a volume after it was dismounted. */
	STATUS_TOO_LATE(0xC0000189L, "A write operation was attempted to a volume after it was dismounted."),

	/**
	 * The workstation does not have a trust secret for the primary domain in
	 * the local LSA database.
	 */
	STATUS_NO_TRUST_LSA_SECRET(0xC000018AL, "The workstation does not " +
		"have a trust secret for the primary domain in the local LSA database."),

	/**
	 * The SAM database on the Windows Server does not have a computer account
	 * for this workstation trust relationship.
	 */
	STATUS_NO_TRUST_SAM_ACCOUNT(0xC000018BL,
		"The SAM database on the Windows Server does not have a computer account for this workstation trust relationship."),

	/**
	 * The logon request failed because the trust relationship between the
	 * primary domain and the trusted domain failed.
	 */
	STATUS_TRUSTED_DOMAIN_FAILURE(0xC000018CL,
		"The logon request failed because the trust relationship between the primary domain and the trusted domain failed."),

	/**
	 * The logon request failed because the trust relationship between this
	 * workstation and the primary domain failed.
	 */
	STATUS_TRUSTED_RELATIONSHIP_FAILURE(0xC000018DL,
		"The logon request failed because the trust relationship between this workstation and the primary domain failed."),

	/** The Eventlog log file is corrupt. */
	STATUS_EVENTLOG_FILE_CORRUPT(0xC000018EL, "The Eventlog log file is corrupt."),

	/**
	 * No Eventlog log file could be opened. The Eventlog service did not start.
	 */
	STATUS_EVENTLOG_CANT_START(0xC000018FL, "No Eventlog log file could be opened. The Eventlog service did not start."),

	/**
	 * The network logon failed. This might be because the validation authority
	 * cannot be reached.
	 */
	STATUS_TRUST_FAILURE(0xC0000190L, "The network logon failed. This might be because the validation authority cannot be reached."),

	/**
	 * An attempt was made to acquire a mutant such that its maximum count would
	 * have been exceeded.
	 */
	STATUS_MUTANT_LIMIT_EXCEEDED(0xC0000191L,
		"An attempt was made to acquire a mutant such that its maximum count would have been exceeded."),

	/** An attempt was made to logon, but the NetLogon service was not started. */
	STATUS_NETLOGON_NOT_STARTED(0xC0000192L, "An attempt was made to logon, but the NetLogon service was not started."),

	/** The user account has expired. */
	STATUS_ACCOUNT_EXPIRED(0xC0000193L, "The user account has expired."),

	/** {EXCEPTION} Possible deadlock condition. */
	STATUS_POSSIBLE_DEADLOCK(0xC0000194L, "{EXCEPTION} Possible deadlock condition."),

	/**
	 * Multiple connections to a server or shared resource by the same user,
	 * using more than one user name, are not allowed. Disconnect all previous
	 * connections to the server or shared resource and try again.
	 */
	STATUS_NETWORK_CREDENTIAL_CONFLICT(
		0xC0000195L,
		"Multiple connections to a server or shared resource by the same user, using more than " +
		"one user name, are not allowed. Disconnect all previous connections to the server or " +
		"shared resource and try again."),

	/**
	 * An attempt was made to establish a session to a network server, but there
	 * are already too many sessions established to that server.
	 */
	STATUS_REMOTE_SESSION_LIMIT(0xC0000196L,
		"An attempt was made to establish a session to a network server, but there " +
		"are already too many sessions established to that server."),

	/** The log file has changed between reads. */
	STATUS_EVENTLOG_FILE_CHANGED(0xC0000197L, "The log file has changed between reads."),

	/**
	 * The account used is an interdomain trust account. Use your global user
	 * account or local user account to access this server.
	 */
	STATUS_NOLOGON_INTERDOMAIN_TRUST_ACCOUNT(0xC0000198L,
		"The account used is an interdomain trust account. Use your global user account or local user account to access this server."),

	/**
	 * The account used is a computer account. Use your global user account or
	 * local user account to access this server.
	 */
	STATUS_NOLOGON_WORKSTATION_TRUST_ACCOUNT(0xC0000199L,
		"The account used is a computer account. Use your global user account or local user account to access this server."),

	/**
	 * The account used is a server trust account. Use your global user account
	 * or local user account to access this server.
	 */
	STATUS_NOLOGON_SERVER_TRUST_ACCOUNT(0xC000019AL,
		"The account used is a server trust account. Use your global user account or local user account to access this server."),

	/**
	 * The name or SID of the specified domain is inconsistent with the trust
	 * information for that domain.
	 */
	STATUS_DOMAIN_TRUST_INCONSISTENT(0xC000019BL,
		"The name or SID of the specified domain is inconsistent with the trust information for that domain."),

	/**
	 * A volume has been accessed for which a file system driver is required
	 * that has not yet been loaded.
	 */
	STATUS_FS_DRIVER_REQUIRED(0xC000019CL,
		"A volume has been accessed for which a file system driver is required that has not yet been loaded."),

	/** Indicates that the specified image is already loaded as a DLL. */
	STATUS_IMAGE_ALREADY_LOADED_AS_DLL(0xC000019DL, "Indicates that the specified image is already loaded as a DLL."),

	/**
	 * Short name settings cannot be changed on this volume due to the global
	 * registry setting.
	 */
	STATUS_INCOMPATIBLE_WITH_GLOBAL_SHORT_NAME_REGISTRY_SETTING(0xC000019EL,
		"Short name settings cannot be changed on this volume due to the global registry setting."),

	/** Short names are not enabled on this volume. */
	STATUS_SHORT_NAMES_NOT_ENABLED_ON_VOLUME(0xC000019FL, "Short names are not enabled on this volume."),

	/**
	 * The security stream for the given volume is in an inconsistent state.
	 * Please run CHKDSK on the volume.
	 */
	STATUS_SECURITY_STREAM_IS_INCONSISTENT(0xC00001A0L,
		"The security stream for the given volume is in an inconsistent state. Please run CHKDSK on the volume."),

	/**
	 * A requested file lock operation cannot be processed due to an invalid
	 * byte range.
	 */
	STATUS_INVALID_LOCK_RANGE(0xC00001A1L, "A requested file lock operation cannot be processed due to an invalid byte range."),

	/** The specified access control entry (ACE) contains an invalid condition. */
	STATUS_INVALID_ACE_CONDITION(0xC00001A2L, "The specified access control entry (ACE) contains an invalid condition."),

	/** The subsystem needed to support the image type is not present. */
	STATUS_IMAGE_SUBSYSTEM_NOT_PRESENT(0xC00001A3L, "The subsystem needed to support the image type is not present."),

	/** The specified file already has a notification GUID associated with it. */
	STATUS_NOTIFICATION_GUID_ALREADY_DEFINED(0xC00001A4L, "The specified file already has a notification GUID associated with it."),

	/**
	 * A remote open failed because the network open restrictions were not
	 * satisfied.
	 */
	STATUS_NETWORK_OPEN_RESTRICTION(0xC0000201L, "A remote open failed because the network open restrictions were not satisfied."),

	/** There is no user session key for the specified logon session. */
	STATUS_NO_USER_SESSION_KEY(0xC0000202L, "There is no user session key for the specified logon session."),

	/** The remote user session has been deleted. */
	STATUS_USER_SESSION_DELETED(0xC0000203L, "The remote user session has been deleted."),

	/**
	 * Indicates the specified resource language ID cannot be found in the image
	 * file.
	 */
	STATUS_RESOURCE_LANG_NOT_FOUND(0xC0000204L, "Indicates the specified resource language ID cannot be found in the image file."),

	/** Insufficient server resources exist to complete the request. */
	STATUS_INSUFF_SERVER_RESOURCES(0xC0000205L, "Insufficient server resources exist to complete the request."),

	/** The size of the buffer is invalid for the specified operation. */
	STATUS_INVALID_BUFFER_SIZE(0xC0000206L, "The size of the buffer is invalid for the specified operation."),

	/** The transport rejected the specified network address as invalid. */
	STATUS_INVALID_ADDRESS_COMPONENT(0xC0000207L, "The transport rejected the specified network address as invalid."),

	/**
	 * The transport rejected the specified network address due to invalid use
	 * of a wildcard.
	 */
	STATUS_INVALID_ADDRESS_WILDCARD(0xC0000208L, "The transport rejected the specified network address due to invalid use of a wildcard."),

	/**
	 * The transport address could not be opened because all the available
	 * addresses are in use.
	 */
	STATUS_TOO_MANY_ADDRESSES(0xC0000209L, "The transport address could not be opened because all the available addresses are in use."),

	/** The transport address could not be opened because it already exists. */
	STATUS_ADDRESS_ALREADY_EXISTS(0xC000020AL, "The transport address could not be opened because it already exists."),

	/** The transport address is now closed. */
	STATUS_ADDRESS_CLOSED(0xC000020BL, "The transport address is now closed."),

	/** The transport connection is now disconnected. */
	STATUS_CONNECTION_DISCONNECTED(0xC000020CL, "The transport connection is now disconnected."),

	/** The transport connection has been reset. */
	STATUS_CONNECTION_RESET(0xC000020DL, "The transport connection has been reset."),

	/** The transport cannot dynamically acquire any more nodes. */
	STATUS_TOO_MANY_NODES(0xC000020EL, "The transport cannot dynamically acquire any more nodes."),

	/** The transport aborted a pending transaction. */
	STATUS_TRANSACTION_ABORTED(0xC000020FL, "The transport aborted a pending transaction."),

	/** The transport timed out a request that is waiting for a response. */
	STATUS_TRANSACTION_TIMED_OUT(0xC0000210L, "The transport timed out a request that is waiting for a response."),

	/** The transport did not receive a release for a pending response. */
	STATUS_TRANSACTION_NO_RELEASE(0xC0000211L, "The transport did not receive a release for a pending response."),

	/**
	 * The transport did not find a transaction that matches the specific token.
	 */
	STATUS_TRANSACTION_NO_MATCH(0xC0000212L, "The transport did not find a transaction that matches the specific token."),

	/** The transport had previously responded to a transaction request. */
	STATUS_TRANSACTION_RESPONDED(0xC0000213L, "The transport had previously responded to a transaction request."),

	/** The transport does not recognize the specified transaction request ID. */
	STATUS_TRANSACTION_INVALID_ID(0xC0000214L, "The transport does not recognize the specified transaction request ID."),

	/** The transport does not recognize the specified transaction request type. */
	STATUS_TRANSACTION_INVALID_TYPE(0xC0000215L, "The transport does not recognize the specified transaction request type."),

	/**
	 * The transport can only process the specified request on the server side
	 * of a session.
	 */
	STATUS_NOT_SERVER_SESSION(0xC0000216L, "The transport can only process the specified request on the server side of a session."),

	/**
	 * The transport can only process the specified request on the client side
	 * of a session.
	 */
	STATUS_NOT_CLIENT_SESSION(0xC0000217L, "The transport can only process the specified request on the client side of a session."),

	/**
	 * {Registry File Failure} The registry cannot load the hive (file): %hs or
	 * its log or alternate. It is corrupt, absent, or not writable.
	 */
	STATUS_CANNOT_LOAD_REGISTRY_FILE(0xC0000218L,
		"{Registry File Failure} The registry cannot load the hive (file): " +
		"%hs or its log or alternate. It is corrupt, absent, or not writable."),

	/**
	 * {Unexpected Failure in DebugActiveProcess} An unexpected failure occurred
	 * while processing a DebugActiveProcess API request. Choosing OK will
	 * terminate the process, and choosing Cancel will ignore the error.
	 */
	STATUS_DEBUG_ATTACH_FAILED(
		0xC0000219L,
		"{Unexpected Failure in DebugActiveProcess} An unexpected failure occurred " +
		"while processing a DebugActiveProcess API request. Choosing OK will terminate " +
		"the process, and choosing Cancel will ignore the error."),

	/**
	 * {Fatal System Error} The %hs system process terminated unexpectedly with
	 * a status of 0x%08x (0x%08x 0x%08x). The system has been shut down.
	 */
	STATUS_SYSTEM_PROCESS_TERMINATED(
		0xC000021AL,
		"{Fatal System Error} The %hs system process terminated unexpectedly with a " +
		"status of 0x%08x (0x%08x 0x%08x). The system has been shut down."),

	/**
	 * {Data Not Accepted} The TDI client could not handle the data received
	 * during an indication.
	 */
	STATUS_DATA_NOT_ACCEPTED(0xC000021BL, "{Data Not Accepted} The TDI client could not handle the data received during an indication."),

	/**
	 * {Unable to Retrieve Browser Server List} The list of servers for this
	 * workgroup is not currently available.
	 */
	STATUS_NO_BROWSER_SERVERS_FOUND(0xC000021CL,
		"{Unable to Retrieve Browser Server List} The list of servers for this workgroup is not currently available."),

	/** NTVDM encountered a hard error. */
	STATUS_VDM_HARD_ERROR(0xC000021DL, "NTVDM encountered a hard error."),

	/**
	 * {Cancel Timeout} The driver %hs failed to complete a canceled I/O request
	 * in the allotted time.
	 */
	STATUS_DRIVER_CANCEL_TIMEOUT(0xC000021EL,
		"{Cancel Timeout} The driver %hs failed to complete a canceled I/O request in the allotted time."),

	/**
	 * {Reply Message Mismatch} An attempt was made to reply to an LPC message,
	 * but the thread specified by the client ID in the message was not waiting
	 * on that message.
	 */
	STATUS_REPLY_MESSAGE_MISMATCH(
		0xC000021FL,
		"{Reply Message Mismatch} An attempt was made to reply to an LPC message, but the " +
		"thread specified by the client ID in the message was not waiting on that message."),

	/**
	 * {Mapped View Alignment Incorrect} An attempt was made to map a view of a
	 * file, but either the specified base address or the offset into the file
	 * were not aligned on the proper allocation granularity.
	 */
	STATUS_MAPPED_ALIGNMENT(
		0xC0000220L,
		"{Mapped View Alignment Incorrect} An attempt was made to map a view of a file, but either the " +
		"specified base address or the offset into the file were not aligned on the proper allocation granularity."),

	/**
	 * {Bad Image Checksum} The image %hs is possibly corrupt. The header
	 * checksum does not match the computed checksum.
	 */
	STATUS_IMAGE_CHECKSUM_MISMATCH(0xC0000221L,
		"{Bad Image Checksum} The image %hs is possibly corrupt. The header checksum does not match the computed checksum."),

	/**
	 * {Delayed Write Failed} Windows was unable to save all the data for the
	 * file %hs. The data has been lost. This error might be caused by a failure
	 * of your computer hardware or network connection. Try to save this file
	 * elsewhere.
	 */
	STATUS_LOST_WRITEBEHIND_DATA(
		0xC0000222L,
		"{Delayed Write Failed} Windows was unable to save all the data for the file %hs. The data has " +
		"been lost. This error might be caused by a failure of your computer hardware or network connection. " +
		"Try to save this file elsewhere."),

	/**
	 * The parameters passed to the server in the client/server shared memory
	 * window were invalid. Too much data might have been put in the shared
	 * memory window.
	 */
	STATUS_CLIENT_SERVER_PARAMETERS_INVALID(
		0xC0000223L,
		"The parameters passed to the server in the client/server shared memory window were invalid. " +
		"Too much data might have been put in the shared memory window."),

	/** The user password must be changed before logging on the first time. */
	STATUS_PASSWORD_MUST_CHANGE(0xC0000224L, "The user password must be changed before logging on the first time."),

	/** The object was not found. */
	STATUS_NOT_FOUND(0xC0000225L, "The object was not found."),

	/** The stream is not a tiny stream. */
	STATUS_NOT_TINY_STREAM(0xC0000226L, "The stream is not a tiny stream."),

	/** A transaction recovery failed. */
	STATUS_RECOVERY_FAILURE(0xC0000227L, "A transaction recovery failed."),

	/** The request must be handled by the stack overflow code. */
	STATUS_STACK_OVERFLOW_READ(0xC0000228L, "The request must be handled by the stack overflow code."),

	/** A consistency check failed. */
	STATUS_FAIL_CHECK(0xC0000229L, "A consistency check failed."),

	/**
	 * The attempt to insert the ID in the index failed because the ID is
	 * already in the index.
	 */
	STATUS_DUPLICATE_OBJECTID(0xC000022AL, "The attempt to insert the ID in the index failed because the ID is already in the index."),

	/**
	 * The attempt to set the object ID failed because the object already has an
	 * ID.
	 */
	STATUS_OBJECTID_EXISTS(0xC000022BL, "The attempt to set the object ID failed because the object already has an ID."),

	/**
	 * Internal OFS status codes indicating how an allocation operation is
	 * handled. Either it is retried after the containing oNode is moved or the
	 * extent stream is converted to a large stream.
	 */
	STATUS_CONVERT_TO_LARGE(
		0xC000022CL,
		"Internal OFS status codes indicating how an allocation operation is handled. Either it is " +
		"retried after the containing oNode is moved or the extent stream is converted to a large stream."),

	/** The request needs to be retried. */
	STATUS_RETRY(0xC000022DL, "The request needs to be retried."),

	/**
	 * The attempt to find the object found an object on the volume that matches
	 * by ID; however, it is out of the scope of the handle that is used for the
	 * operation.
	 */
	STATUS_FOUND_OUT_OF_SCOPE(
		0xC000022EL,
		"The attempt to find the object found an object on the volume that matches by ID; however, " +
		"it is out of the scope of the handle that is used for the operation."),

	/** The bucket array must be grown. Retry the transaction after doing so. */
	STATUS_ALLOCATE_BUCKET(0xC000022FL, "The bucket array must be grown. Retry the transaction after doing so."),

	/** The specified property set does not exist on the object. */
	STATUS_PROPSET_NOT_FOUND(0xC0000230L, "The specified property set does not exist on the object."),

	/** The user/kernel marshaling buffer has overflowed. */
	STATUS_MARSHALL_OVERFLOW(0xC0000231L, "The user/kernel marshaling buffer has overflowed."),

	/** The supplied variant structure contains invalid data. */
	STATUS_INVALID_VARIANT(0xC0000232L, "The supplied variant structure contains invalid data."),

	/** A domain controller for this domain was not found. */
	STATUS_DOMAIN_CONTROLLER_NOT_FOUND(0xC0000233L, "A domain controller for this domain was not found."),

	/**
	 * The user account has been automatically locked because too many invalid
	 * logon attempts or password change attempts have been requested.
	 */
	STATUS_ACCOUNT_LOCKED_OUT(0xC0000234L,
		"The user account has been automatically locked because too many invalid " +
		"logon attempts or password change attempts have been requested."),

	/**
	 * NtClose was called on a handle that was protected from close via
	 * NtSetInformationObject.
	 */
	STATUS_HANDLE_NOT_CLOSABLE(0xC0000235L, "NtClose was called on a handle that was protected from close via NtSetInformationObject."),

	/** The transport-connection attempt was refused by the remote system. */
	STATUS_CONNECTION_REFUSED(0xC0000236L, "The transport-connection attempt was refused by the remote system."),

	/** The transport connection was gracefully closed. */
	STATUS_GRACEFUL_DISCONNECT(0xC0000237L, "The transport connection was gracefully closed."),

	/** The transport endpoint already has an address associated with it. */
	STATUS_ADDRESS_ALREADY_ASSOCIATED(0xC0000238L, "The transport endpoint already has an address associated with it."),

	/** An address has not yet been associated with the transport endpoint. */
	STATUS_ADDRESS_NOT_ASSOCIATED(0xC0000239L, "An address has not yet been associated with the transport endpoint."),

	/** An operation was attempted on a nonexistent transport connection. */
	STATUS_CONNECTION_INVALID(0xC000023AL, "An operation was attempted on a nonexistent transport connection."),

	/** An invalid operation was attempted on an active transport connection. */
	STATUS_CONNECTION_ACTIVE(0xC000023BL, "An invalid operation was attempted on an active transport connection."),

	/** The remote network is not reachable by the transport. */
	STATUS_NETWORK_UNREACHABLE(0xC000023CL, "The remote network is not reachable by the transport."),

	/** The remote system is not reachable by the transport. */
	STATUS_HOST_UNREACHABLE(0xC000023DL, "The remote system is not reachable by the transport."),

	/** The remote system does not support the transport protocol. */
	STATUS_PROTOCOL_UNREACHABLE(0xC000023EL, "The remote system does not support the transport protocol."),

	/**
	 * No service is operating at the destination port of the transport on the
	 * remote system.
	 */
	STATUS_PORT_UNREACHABLE(0xC000023FL, "No service is operating at the destination port of the transport on the remote system."),

	/** The request was aborted. */
	STATUS_REQUEST_ABORTED(0xC0000240L, "The request was aborted."),

	/** The transport connection was aborted by the local system. */
	STATUS_CONNECTION_ABORTED(0xC0000241L, "The transport connection was aborted by the local system."),

	/** The specified buffer contains ill-formed data. */
	STATUS_BAD_COMPRESSION_BUFFER(0xC0000242L, "The specified buffer contains ill-formed data."),

	/**
	 * The requested operation cannot be performed on a file with a user mapped
	 * section open.
	 */
	STATUS_USER_MAPPED_FILE(0xC0000243L, "The requested operation cannot be performed on a file with a user mapped section open."),

	/** {Audit Failed} An attempt to generate a security audit failed. */
	STATUS_AUDIT_FAILED(0xC0000244L, "{Audit Failed} An attempt to generate a security audit failed."),

	/** The timer resolution was not previously set by the current process. */
	STATUS_TIMER_RESOLUTION_NOT_SET(0xC0000245L, "The timer resolution was not previously set by the current process."),

	/**
	 * A connection to the server could not be made because the limit on the
	 * number of concurrent connections for this account has been reached.
	 */
	STATUS_CONNECTION_COUNT_LIMIT(0xC0000246L,
		"A connection to the server could not be made because the limit on the " +
		"number of concurrent connections for this account has been reached."),

	/**
	 * Attempting to log on during an unauthorized time of day for this account.
	 */
	STATUS_LOGIN_TIME_RESTRICTION(0xC0000247L, "Attempting to log on during an unauthorized time of day for this account."),

	/** The account is not authorized to log on from this station. */
	STATUS_LOGIN_WKSTA_RESTRICTION(0xC0000248L, "The account is not authorized to log on from this station."),

	/**
	 * {UP/MP Image Mismatch} The image %hs has been modified for use on a
	 * uniprocessor system, but you are running it on a multiprocessor machine.
	 * Reinstall the image file.
	 */
	STATUS_IMAGE_MP_UP_MISMATCH(
		0xC0000249L,
		"{UP/MP Image Mismatch} The image %hs has been modified for use on a uniprocessor system, " +
		"but you are running it on a multiprocessor machine. Reinstall the image file."),

	/** There is insufficient account information to log you on. */
	STATUS_INSUFFICIENT_LOGON_INFO(0xC0000250L, "There is insufficient account information to log you on."),

	/**
	 * {Invalid DLL Entrypoint} The dynamic link library %hs is not written
	 * correctly. The stack pointer has been left in an inconsistent state. The
	 * entry point should be declared as WINAPI or STDCALL. Select YES to fail
	 * the DLL load. Select NO to continue execution. Selecting NO might cause
	 * the application to operate incorrectly.
	 */
	STATUS_BAD_DLL_ENTRYPOINT(
		0xC0000251L,
		"{Invalid DLL Entrypoint} The dynamic link library %hs is not written correctly. " +
		"The stack pointer has been left in an inconsistent state. The entry point should be declared as " +
		"WINAPI or STDCALL. Select YES to fail the DLL load. Select NO to continue execution. " +
		"Selecting NO might cause the application to operate incorrectly."),

	/**
	 * {Invalid Service Callback Entrypoint} The %hs service is not written
	 * correctly. The stack pointer has been left in an inconsistent state. The
	 * callback entry point should be declared as WINAPI or STDCALL. Selecting OK
	 * will cause the service to continue operation. However, the service process
	 * might operate incorrectly.
	 */
	STATUS_BAD_SERVICE_ENTRYPOINT(
		0xC0000252L,
		"{Invalid Service Callback Entrypoint} The %hs service is not written correctly. The stack pointer " +
		"has been left in an inconsistent state. The callback entry point should be declared as WINAPI or STDCALL. " +
		"Selecting OK will cause the service to continue operation. However, the service process might operate incorrectly."),

	/** The server received the messages but did not send a reply. */
	STATUS_LPC_REPLY_LOST(0xC0000253L, "The server received the messages but did not send a reply."),

	/** There is an IP address conflict with another system on the network. */
	STATUS_IP_ADDRESS_CONFLICT1(0xC0000254L, "There is an IP address conflict with another system on the network."),

	/** There is an IP address conflict with another system on the network. */
	STATUS_IP_ADDRESS_CONFLICT2(0xC0000255L, "There is an IP address conflict with another system on the network."),

	/**
	 * {Low On Registry Space} The system has reached the maximum size that is
	 * allowed for the system part of the registry. Additional storage requests
	 * will be ignored.
	 */
	STATUS_REGISTRY_QUOTA_LIMIT(
		0xC0000256L,
		"{Low On Registry Space} The system has reached the maximum size that is allowed for the system " +
		"part of the registry. Additional storage requests will be ignored."),

	/**
	 * The contacted server does not support the indicated part of the DFS
	 * namespace.
	 */
	STATUS_PATH_NOT_COVERED(0xC0000257L, "The contacted server does not support the indicated part of the DFS namespace."),

	/**
	 * A callback return system service cannot be executed when no callback is
	 * active.
	 */
	STATUS_NO_CALLBACK_ACTIVE(0xC0000258L, "A callback return system service cannot be executed when no callback is active."),

	/**
	 * The service being accessed is licensed for a particular number of
	 * connections. No more connections can be made to the service at this time
	 * because the service has already accepted the maximum number of
	 * connections.
	 */
	STATUS_LICENSE_QUOTA_EXCEEDED(
		0xC0000259L,
		"The service being accessed is licensed for a particular number of connections. No more connections " +
		"can be made to the service at this time because the service has already accepted the maximum number of connections."),

	/**
	 * The password provided is too short to meet the policy of your user
	 * account. Choose a longer password.
	 */
	STATUS_PWD_TOO_SHORT(0xC000025AL,
		"The password provided is too short to meet the policy of your user account. Choose a longer password."),

	/**
	 * The policy of your user account does not allow you to change passwords
	 * too frequently. This is done to prevent users from changing back to a
	 * familiar, but potentially discovered, password. If you feel your password
	 * has been compromised, contact your administrator immediately to have a new
	 * one assigned.
	 */
	STATUS_PWD_TOO_RECENT(
		0xC000025BL,
		"The policy of your user account does not allow you to change passwords too frequently. This is done " +
		"to prevent users from changing back to a familiar, but potentially discovered, password. If you feel " +
		"your password has been compromised, contact your administrator immediately to have a new one assigned."),

	/**
	 * You have attempted to change your password to one that you have used in
	 * the past. The policy of your user account does not allow this. Select a
	 * password that you have not previously used.
	 */
	STATUS_PWD_HISTORY_CONFLICT(
		0xC000025CL,
		"You have attempted to change your password to one that you have used in the past. The policy of your " +
		"user account does not allow this. Select a password that you have not previously used."),

	/**
	 * You have attempted to load a legacy device driver while its device
	 * instance had been disabled.
	 */
	STATUS_PLUGPLAY_NO_DEVICE(0xC000025EL, "You have attempted to load a legacy device driver while its device instance had been disabled."),

	/** The specified compression format is unsupported. */
	STATUS_UNSUPPORTED_COMPRESSION(0xC000025FL, "The specified compression format is unsupported."),

	/** The specified hardware profile configuration is invalid. */
	STATUS_INVALID_HW_PROFILE(0xC0000260L, "The specified hardware profile configuration is invalid."),

	/** The specified Plug and Play registry device path is invalid. */
	STATUS_INVALID_PLUGPLAY_DEVICE_PATH(0xC0000261L, "The specified Plug and Play registry device path is invalid."),

	/**
	 * {Driver Entry Point Not Found} The %hs device driver could not locate the
	 * ordinal %ld in driver %hs.
	 */
	STATUS_DRIVER_ORDINAL_NOT_FOUND(0xC0000262L,
		"{Driver Entry Point Not Found} The %hs device driver could not locate the ordinal %ld in driver %hs."),

	/**
	 * {Driver Entry Point Not Found} The %hs device driver could not locate the
	 * entry point %hs in driver %hs.
	 */
	STATUS_DRIVER_ENTRYPOINT_NOT_FOUND(0xC0000263L,
		"{Driver Entry Point Not Found} The %hs device driver could not locate the entry point %hs in driver %hs."),

	/**
	 * {Application Error} The application attempted to release a resource it
	 * did not own. Click OK to terminate the application.
	 */
	STATUS_RESOURCE_NOT_OWNED(0xC0000264L,
		"{Application Error} The application attempted to release a resource it did not own. Click OK to terminate the application."),

	/**
	 * An attempt was made to create more links on a file than the file system
	 * supports.
	 */
	STATUS_TOO_MANY_LINKS(0xC0000265L, "An attempt was made to create more links on a file than the file system supports."),

	/** The specified quota list is internally inconsistent with its descriptor. */
	STATUS_QUOTA_LIST_INCONSISTENT(0xC0000266L, "The specified quota list is internally inconsistent with its descriptor."),

	/** The specified file has been relocated to offline storage. */
	STATUS_FILE_IS_OFFLINE(0xC0000267L, "The specified file has been relocated to offline storage."),

	/**
	 * {Windows Evaluation Notification} The evaluation period for this
	 * installation of Windows has expired. This system will shutdown in 1 hour.
	 * To restore access to this installation of Windows, upgrade this
	 * installation by using a licensed distribution of this product.
	 */
	STATUS_EVALUATION_EXPIRATION(
		0xC0000268L,
		"{Windows Evaluation Notification} The evaluation period for this installation of Windows has expired. " +
		"This system will shutdown in 1 hour. To restore access to this installation of Windows, upgrade " +
		"this installation by using a licensed distribution of this product."),

	/**
	 * {Illegal System DLL Relocation} The system DLL %hs was relocated in
	 * memory. The application will not run properly. The relocation occurred
	 * because the DLL %hs occupied an address range that is reserved for Windows
	 * system DLLs. The vendor supplying the DLL should be contacted for a new
	 * DLL.
	 */
	STATUS_ILLEGAL_DLL_RELOCATION(
		0xC0000269L,
		"{Illegal System DLL Relocation} The system DLL %hs was relocated in memory. The application will " +
		"not run properly. The relocation occurred because the DLL %hs occupied an address range that is " +
		"reserved for Windows system DLLs. The vendor supplying the DLL should be contacted for a new DLL."),

	/**
	 * {License Violation} The system has detected tampering with your
	 * registered product type. This is a violation of your software license.
	 * Tampering with the product type is not permitted.
	 */
	STATUS_LICENSE_VIOLATION(
		0xC000026AL,
		"{License Violation} The system has detected tampering with your registered product type. This is a " +
		"violation of your software license. Tampering with the product type is not permitted."),

	/**
	 * {DLL Initialization Failed} The application failed to initialize because
	 * the window station is shutting down.
	 */
	STATUS_DLL_INIT_FAILED_LOGOFF(0xC000026BL,
		"{DLL Initialization Failed} The application failed to initialize because the window station is shutting down."),

	/**
	 * {Unable to Load Device Driver} %hs device driver could not be loaded.
	 * Error Status was 0x%x.
	 */
	STATUS_DRIVER_UNABLE_TO_LOAD(0xC000026CL, "{Unable to Load Device Driver} %hs " +
		"device driver could not be loaded. Error Status was 0x%x."),

	/** DFS is unavailable on the contacted server. */
	STATUS_DFS_UNAVAILABLE(0xC000026DL, "DFS is unavailable on the contacted server."),

	/** An operation was attempted to a volume after it was dismounted. */
	STATUS_VOLUME_DISMOUNTED(0xC000026EL, "An operation was attempted to a volume after it was dismounted."),

	/** An internal error occurred in the Win32 x86 emulation subsystem. */
	STATUS_WX86_INTERNAL_ERROR(0xC000026FL, "An internal error occurred in the Win32 x86 emulation subsystem."),

	/** Win32 x86 emulation subsystem floating-point stack check. */
	STATUS_WX86_FLOAT_STACK_CHECK(0xC0000270L, "Win32 x86 emulation subsystem floating-point stack check."),

	/** The validation process needs to continue on to the next step. */
	STATUS_VALIDATE_CONTINUE(0xC0000271L, "The validation process needs to continue on to the next step."),

	/** There was no match for the specified key in the index. */
	STATUS_NO_MATCH(0xC0000272L, "There was no match for the specified key in the index."),

	/** There are no more matches for the current index enumeration. */
	STATUS_NO_MORE_MATCHES(0xC0000273L, "There are no more matches for the current index enumeration."),

	/** The NTFS file or directory is not a reparse point. */
	STATUS_NOT_A_REPARSE_POINT(0xC0000275L, "The NTFS file or directory is not a reparse point."),

	/**
	 * The Windows I/O reparse tag passed for the NTFS reparse point is invalid.
	 */
	STATUS_IO_REPARSE_TAG_INVALID(0xC0000276L, "The Windows I/O reparse tag passed for the NTFS reparse point is invalid."),

	/**
	 * The Windows I/O reparse tag does not match the one that is in the NTFS
	 * reparse point.
	 */
	STATUS_IO_REPARSE_TAG_MISMATCH(0xC0000277L, "The Windows I/O reparse tag does not match the one that is in the NTFS reparse point."),

	/** The user data passed for the NTFS reparse point is invalid. */
	STATUS_IO_REPARSE_DATA_INVALID(0xC0000278L, "The user data passed for the NTFS reparse point is invalid."),

	/**
	 * The layered file system driver for this I/O tag did not handle it when
	 * needed.
	 */
	STATUS_IO_REPARSE_TAG_NOT_HANDLED(0xC0000279L, "The layered file system driver for this I/O tag did not handle it when needed."),

	/**
	 * The NTFS symbolic link could not be resolved even though the initial file
	 * name is valid.
	 */
	STATUS_REPARSE_POINT_NOT_RESOLVED(0xC0000280L,
		"The NTFS symbolic link could not be resolved even though the initial file name is valid."),

	/** The NTFS directory is a reparse point. */
	STATUS_DIRECTORY_IS_A_REPARSE_POINT(0xC0000281L, "The NTFS directory is a reparse point."),

	/** The range could not be added to the range list because of a conflict. */
	STATUS_RANGE_LIST_CONFLICT(0xC0000282L, "The range could not be added to the range list because of a conflict."),

	/** The specified medium changer source element contains no media. */
	STATUS_SOURCE_ELEMENT_EMPTY(0xC0000283L, "The specified medium changer source element contains no media."),

	/** The specified medium changer destination element already contains media. */
	STATUS_DESTINATION_ELEMENT_FULL(0xC0000284L, "The specified medium changer destination element already contains media."),

	/** The specified medium changer element does not exist. */
	STATUS_ILLEGAL_ELEMENT_ADDRESS(0xC0000285L, "The specified medium changer element does not exist."),

	/**
	 * The specified element is contained in a magazine that is no longer
	 * present.
	 */
	STATUS_MAGAZINE_NOT_PRESENT(0xC0000286L, "The specified element is contained in a magazine that is no longer present."),

	/** The device requires re-initialization due to hardware errors. */
	STATUS_REINITIALIZATION_NEEDED(0xC0000287L, "The device requires re-initialization due to hardware errors."),

	/** The file encryption attempt failed. */
	STATUS_ENCRYPTION_FAILED(0xC000028AL, "The file encryption attempt failed."),

	/** The file decryption attempt failed. */
	STATUS_DECRYPTION_FAILED(0xC000028BL, "The file decryption attempt failed."),

	/** The specified range could not be found in the range list. */
	STATUS_RANGE_NOT_FOUND(0xC000028CL, "The specified range could not be found in the range list."),

	/** There is no encryption recovery policy configured for this system. */
	STATUS_NO_RECOVERY_POLICY(0xC000028DL, "There is no encryption recovery policy configured for this system."),

	/** The required encryption driver is not loaded for this system. */
	STATUS_NO_EFS(0xC000028EL, "The required encryption driver is not loaded for this system."),

	/**
	 * The file was encrypted with a different encryption driver than is
	 * currently loaded.
	 */
	STATUS_WRONG_EFS(0xC000028FL, "The file was encrypted with a different encryption driver than is currently loaded."),

	/** There are no EFS keys defined for the user. */
	STATUS_NO_USER_KEYS(0xC0000290L, "There are no EFS keys defined for the user."),

	/** The specified file is not encrypted. */
	STATUS_FILE_NOT_ENCRYPTED(0xC0000291L, "The specified file is not encrypted."),

	/** The specified file is not in the defined EFS export format. */
	STATUS_NOT_EXPORT_FORMAT(0xC0000292L, "The specified file is not in the defined EFS export format."),

	/**
	 * The specified file is encrypted and the user does not have the ability to
	 * decrypt it.
	 */
	STATUS_FILE_ENCRYPTED(0xC0000293L, "The specified file is encrypted and the user does not have the ability to decrypt it."),

	/** The GUID passed was not recognized as valid by a WMI data provider. */
	STATUS_WMI_GUID_NOT_FOUND(0xC0000295L, "The GUID passed was not recognized as valid by a WMI data provider."),

	/**
	 * The instance name passed was not recognized as valid by a WMI data
	 * provider.
	 */
	STATUS_WMI_INSTANCE_NOT_FOUND(0xC0000296L, "The instance name passed was not recognized as valid by a WMI data provider."),

	/**
	 * The data item ID passed was not recognized as valid by a WMI data
	 * provider.
	 */
	STATUS_WMI_ITEMID_NOT_FOUND(0xC0000297L, "The data item ID passed was not recognized as valid by a WMI data provider."),

	/** The WMI request could not be completed and should be retried. */
	STATUS_WMI_TRY_AGAIN(0xC0000298L, "The WMI request could not be completed and should be retried."),

	/** The policy object is shared and can only be modified at the root. */
	STATUS_SHARED_POLICY(0xC0000299L, "The policy object is shared and can only be modified at the root."),

	/** The policy object does not exist when it should. */
	STATUS_POLICY_OBJECT_NOT_FOUND(0xC000029AL, "The policy object does not exist when it should."),

	/** The requested policy information only lives in the Ds. */
	STATUS_POLICY_ONLY_IN_DS(0xC000029BL, "The requested policy information only lives in the Ds."),

	/** The volume must be upgraded to enable this feature. */
	STATUS_VOLUME_NOT_UPGRADED(0xC000029CL, "The volume must be upgraded to enable this feature."),

	/** The remote storage service is not operational at this time. */
	STATUS_REMOTE_STORAGE_NOT_ACTIVE(0xC000029DL, "The remote storage service is not operational at this time."),

	/** The remote storage service encountered a media error. */
	STATUS_REMOTE_STORAGE_MEDIA_ERROR(0xC000029EL, "The remote storage service encountered a media error."),

	/** The tracking (workstation) service is not running. */
	STATUS_NO_TRACKING_SERVICE(0xC000029FL, "The tracking (workstation) service is not running."),

	/**
	 * The server process is running under a SID that is different from the SID
	 * that is required by client.
	 */
	STATUS_SERVER_SID_MISMATCH(0xC00002A0L,
		"The server process is running under a SID that is different from the SID that is required by client."),

	/** The specified directory service attribute or value does not exist. */
	STATUS_DS_NO_ATTRIBUTE_OR_VALUE(0xC00002A1L, "The specified directory service attribute or value does not exist."),

	/** The attribute syntax specified to the directory service is invalid. */
	STATUS_DS_INVALID_ATTRIBUTE_SYNTAX(0xC00002A2L, "The attribute syntax specified to the directory service is invalid."),

	/** The attribute type specified to the directory service is not defined. */
	STATUS_DS_ATTRIBUTE_TYPE_UNDEFINED(0xC00002A3L, "The attribute type specified to the directory service is not defined."),

	/** The specified directory service attribute or value already exists. */
	STATUS_DS_ATTRIBUTE_OR_VALUE_EXISTS(0xC00002A4L, "The specified directory service attribute or value already exists."),

	/** The directory service is busy. */
	STATUS_DS_BUSY(0xC00002A5L, "The directory service is busy."),

	/** The directory service is unavailable. */
	STATUS_DS_UNAVAILABLE(0xC00002A6L, "The directory service is unavailable."),

	/** The directory service was unable to allocate a relative identifier. */
	STATUS_DS_NO_RIDS_ALLOCATED(0xC00002A7L, "The directory service was unable to allocate a relative identifier."),

	/** The directory service has exhausted the pool of relative identifiers. */
	STATUS_DS_NO_MORE_RIDS(0xC00002A8L, "The directory service has exhausted the pool of relative identifiers."),

	/**
	 * The requested operation could not be performed because the directory
	 * service is not the master for that type of operation.
	 */
	STATUS_DS_INCORRECT_ROLE_OWNER(0xC00002A9L,
		"The requested operation could not be performed because the directory service is not the master for that type of operation."),

	/**
	 * The directory service was unable to initialize the subsystem that
	 * allocates relative identifiers.
	 */
	STATUS_DS_RIDMGR_INIT_ERROR(0xC00002AAL,
		"The directory service was unable to initialize the subsystem that allocates relative identifiers."),

	/**
	 * The requested operation did not satisfy one or more constraints that are
	 * associated with the class of the object.
	 */
	STATUS_DS_OBJ_CLASS_VIOLATION(0xC00002ABL,
		"The requested operation did not satisfy one or more constraints that are associated with the class of the object."),

	/**
	 * The directory service can perform the requested operation only on a leaf
	 * object.
	 */
	STATUS_DS_CANT_ON_NON_LEAF(0xC00002ACL, "The directory service can perform the requested operation only on a leaf object."),

	/**
	 * The directory service cannot perform the requested operation on the
	 * Relatively Defined Name (RDN) attribute of an object.
	 */
	STATUS_DS_CANT_ON_RDN(0xC00002ADL,
		"The directory service cannot perform the requested operation on the Relatively Defined Name (RDN) attribute of an object."),

	/**
	 * The directory service detected an attempt to modify the object class of
	 * an object.
	 */
	STATUS_DS_CANT_MOD_OBJ_CLASS(0xC00002AEL, "The directory service detected an attempt to modify the object class of an object."),

	/** An error occurred while performing a cross domain move operation. */
	STATUS_DS_CROSS_DOM_MOVE_FAILED(0xC00002AFL, "An error occurred while performing a cross domain move operation."),

	/** Unable to contact the global catalog server. */
	STATUS_DS_GC_NOT_AVAILABLE(0xC00002B0L, "Unable to contact the global catalog server."),

	/**
	 * The requested operation requires a directory service, and none was
	 * available.
	 */
	STATUS_DIRECTORY_SERVICE_REQUIRED(0xC00002B1L, "The requested operation requires a directory service, and none was available."),

	/**
	 * The reparse attribute cannot be set because it is incompatible with an
	 * existing attribute.
	 */
	STATUS_REPARSE_ATTRIBUTE_CONFLICT(0xC00002B2L,
		"The reparse attribute cannot be set because it is incompatible with an existing attribute."),

	/** A group marked "use for deny only" cannot be enabled. */
	STATUS_CANT_ENABLE_DENY_ONLY(0xC00002B3L, "A group marked \"use for deny only\" cannot be enabled."),

	/** {EXCEPTION} Multiple floating-point faults. */
	STATUS_FLOAT_MULTIPLE_FAULTS(0xC00002B4L, "{EXCEPTION} Multiple floating-point faults."),

	/** {EXCEPTION} Multiple floating-point traps. */
	STATUS_FLOAT_MULTIPLE_TRAPS(0xC00002B5L, "{EXCEPTION} Multiple floating-point traps."),

	/** The device has been removed. */
	STATUS_DEVICE_REMOVED(0xC00002B6L, "The device has been removed."),

	/** The volume change journal is being deleted. */
	STATUS_JOURNAL_DELETE_IN_PROGRESS(0xC00002B7L, "The volume change journal is being deleted."),

	/** The volume change journal is not active. */
	STATUS_JOURNAL_NOT_ACTIVE(0xC00002B8L, "The volume change journal is not active."),

	/** The requested interface is not supported. */
	STATUS_NOINTERFACE(0xC00002B9L, "The requested interface is not supported."),

	/** A directory service resource limit has been exceeded. */
	STATUS_DS_ADMIN_LIMIT_EXCEEDED(0xC00002C1L, "A directory service resource limit has been exceeded."),

	/**
	 * {System Standby Failed} The driver %hs does not support standby mode.
	 * Updating this driver allows the system to go to standby mode.
	 */
	STATUS_DRIVER_FAILED_SLEEP(0xC00002C2L,
		"{System Standby Failed} The driver %hs does not support standby mode. " +
		"Updating this driver allows the system to go to standby mode."),

	/**
	 * Mutual Authentication failed. The server password is out of date at the
	 * domain controller.
	 */
	STATUS_MUTUAL_AUTHENTICATION_FAILED(0xC00002C3L,
		"Mutual Authentication failed. The server password is out of date at the domain controller."),

	/** The system file %1 has become corrupt and has been replaced. */
	STATUS_CORRUPT_SYSTEM_FILE(0xC00002C4L, "The system file %1 has become corrupt and has been replaced."),

	/**
	 * {EXCEPTION} Alignment Error A data type misalignment error was detected
	 * in a load or store instruction.
	 */
	STATUS_DATATYPE_MISALIGNMENT_ERROR(0xC00002C5L,
		"{EXCEPTION} Alignment Error A data type misalignment error was detected in a load or store instruction."),

	/** The WMI data item or data block is read-only. */
	STATUS_WMI_READ_ONLY(0xC00002C6L, "The WMI data item or data block is read-only."),

	/** The WMI data item or data block could not be changed. */
	STATUS_WMI_SET_FAILURE(0xC00002C7L, "The WMI data item or data block could not be changed."),

	/**
	 * {Virtual Memory Minimum Too Low} Your system is low on virtual memory.
	 * Windows is increasing the size of your virtual memory paging file. During
	 * this process, memory requests for some applications might be denied. For
	 * more information, see Help.
	 */
	STATUS_COMMITMENT_MINIMUM(
		0xC00002C8L,
		"{Virtual Memory Minimum Too Low} Your system is low on virtual memory. Windows is " +
		"increasing the size of your virtual memory paging file. During this process, memory " +
		"requests for some applications might be denied. For more information, see Help."),

	/**
	 * {EXCEPTION} Register NaT consumption faults. A NaT value is consumed on a
	 * non-speculative instruction.
	 */
	STATUS_REG_NAT_CONSUMPTION(0xC00002C9L,
		"{EXCEPTION} Register NaT consumption faults. A NaT value is consumed on a non-speculative instruction."),

	/**
	 * The transport element of the medium changer contains media, which is
	 * causing the operation to fail.
	 */
	STATUS_TRANSPORT_FULL(0xC00002CAL, "The transport element of the medium changer " +
		"contains media, which is causing the operation to fail."),

	/**
	 * Security Accounts Manager initialization failed because of the following
	 * error: %hs Error Status: 0x%x. Click OK to shut down this system and
	 * restart in Directory Services Restore Mode. Check the event log for more
	 * detailed information.
	 */
	STATUS_DS_SAM_INIT_FAILURE(
		0xC00002CBL,
		"Security Accounts Manager initialization failed because of the following error: " +
		"%hs Error Status: 0x%x. Click OK to shut down this system and restart in Directory " +
		"Services Restore Mode. Check the event log for more detailed information."),

	/** This operation is supported only when you are connected to the server. */
	STATUS_ONLY_IF_CONNECTED(0xC00002CCL, "This operation is supported only when you are connected to the server."),

	/**
	 * Only an administrator can modify the membership list of an administrative
	 * group.
	 */
	STATUS_DS_SENSITIVE_GROUP_VIOLATION(0xC00002CDL, "Only an administrator can modify the membership list of an administrative group."),

	/** A device was removed so enumeration must be restarted. */
	STATUS_PNP_RESTART_ENUMERATION(0xC00002CEL, "A device was removed so enumeration must be restarted."),

	/** The journal entry has been deleted from the journal. */
	STATUS_JOURNAL_ENTRY_DELETED(0xC00002CFL, "The journal entry has been deleted from the journal."),

	/** Cannot change the primary group ID of a domain controller account. */
	STATUS_DS_CANT_MOD_PRIMARYGROUPID(0xC00002D0L, "Cannot change the primary group ID of a domain controller account."),

	/**
	 * {Fatal System Error} The system image %s is not properly signed. The file
	 * has been replaced with the signed file. The system has been shut down.
	 */
	STATUS_SYSTEM_IMAGE_BAD_SIGNATURE(
		0xC00002D1L,
		"{Fatal System Error} The system image %s is not properly signed. The file has " +
		"been replaced with the signed file. The system has been shut down."),

	/** The device will not start without a reboot. */
	STATUS_PNP_REBOOT_REQUIRED(0xC00002D2L, "The device will not start without a reboot."),

	/** The power state of the current device cannot support this request. */
	STATUS_POWER_STATE_INVALID(0xC00002D3L, "The power state of the current device cannot support this request."),

	/** The specified group type is invalid. */
	STATUS_DS_INVALID_GROUP_TYPE(0xC00002D4L, "The specified group type is invalid."),

	/**
	 * In a mixed domain, no nesting of a global group if the group is security
	 * enabled.
	 */
	STATUS_DS_NO_NEST_GLOBALGROUP_IN_MIXEDDOMAIN(0xC00002D5L,
		"In a mixed domain, no nesting of a global group if the group is security enabled."),

	/**
	 * In a mixed domain, cannot nest local groups with other local groups, if
	 * the group is security enabled.
	 */
	STATUS_DS_NO_NEST_LOCALGROUP_IN_MIXEDDOMAIN(0xC00002D6L,
		"In a mixed domain, cannot nest local groups with other local groups, if the group is security enabled."),

	/** A global group cannot have a local group as a member. */
	STATUS_DS_GLOBAL_CANT_HAVE_LOCAL_MEMBER(0xC00002D7L, "A global group cannot have a local group as a member."),

	/** A global group cannot have a universal group as a member. */
	STATUS_DS_GLOBAL_CANT_HAVE_UNIVERSAL_MEMBER(0xC00002D8L, "A global group cannot have a universal group as a member."),

	/** A universal group cannot have a local group as a member. */
	STATUS_DS_UNIVERSAL_CANT_HAVE_LOCAL_MEMBER(0xC00002D9L, "A universal group cannot have a local group as a member."),

	/** A global group cannot have a cross-domain member. */
	STATUS_DS_GLOBAL_CANT_HAVE_CROSSDOMAIN_MEMBER(0xC00002DAL, "A global group cannot have a cross-domain member."),

	/** A local group cannot have another cross-domain local group as a member. */
	STATUS_DS_LOCAL_CANT_HAVE_CROSSDOMAIN_LOCAL_MEMBER(0xC00002DBL,
		"A local group cannot have another cross-domain local group as a member."),

	/**
	 * Cannot change to a security-disabled group because primary members are in
	 * this group.
	 */
	STATUS_DS_HAVE_PRIMARY_MEMBERS(0xC00002DCL, "Cannot change to a security-disabled group because primary members are in this group."),

	/** The WMI operation is not supported by the data block or method. */
	STATUS_WMI_NOT_SUPPORTED(0xC00002DDL, "The WMI operation is not supported by the data block or method."),

	/** There is not enough power to complete the requested operation. */
	STATUS_INSUFFICIENT_POWER(0xC00002DEL, "There is not enough power to complete the requested operation."),

	/** The Security Accounts Manager needs to get the boot password. */
	STATUS_SAM_NEED_BOOTKEY_PASSWORD(0xC00002DFL, "The Security Accounts Manager needs to get the boot password."),

	/**
	 * The Security Accounts Manager needs to get the boot key from the floppy
	 * disk.
	 */
	STATUS_SAM_NEED_BOOTKEY_FLOPPY(0xC00002E0L, "The Security Accounts Manager needs to get the boot key from the floppy disk."),

	/** The directory service cannot start. */
	STATUS_DS_CANT_START(0xC00002E1L, "The directory service cannot start."),

	/**
	 * The directory service could not start because of the following error: %hs
	 * Error Status: 0x%x. Click OK to shut down this system and restart in
	 * Directory Services Restore Mode. Check the event log for more detailed
	 * information.
	 */
	STATUS_DS_INIT_FAILURE(
		0xC00002E2L,
		"The directory service could not start because of the following error: %hs Error Status: 0x%x. " +
		"Click OK to shut down this system and restart in Directory Services Restore Mode. " +
		"Check the event log for more detailed information."),

	/**
	 * The Security Accounts Manager initialization failed because of the
	 * following error: %hs Error Status: 0x%x. Click OK to shut down this
	 * system and restart in Safe Mode. Check the event log for more detailed
	 * information.
	 */
	STATUS_SAM_INIT_FAILURE(
		0xC00002E3L,
		"The Security Accounts Manager initialization failed because of the following error: %hs Error Status: " +
		"0x%x. Click OK to shut down this system and restart in Safe Mode. Check the event log for more detailed information."),

	/**
	 * The requested operation can be performed only on a global catalog server.
	 */
	STATUS_DS_GC_REQUIRED(0xC00002E4L, "The requested operation can be performed only on a global catalog server."),

	/**
	 * A local group can only be a member of other local groups in the same
	 * domain.
	 */
	STATUS_DS_LOCAL_MEMBER_OF_LOCAL_ONLY(0xC00002E5L, "A local group can only be a member of other local groups in the same domain."),

	/** Foreign security principals cannot be members of universal groups. */
	STATUS_DS_NO_FPO_IN_UNIVERSAL_GROUPS(0xC00002E6L, "Foreign security principals cannot be members of universal groups."),

	/**
	 * Your computer could not be joined to the domain. You have exceeded the
	 * maximum number of computer accounts you are allowed to create in this
	 * domain. Contact your system administrator to have this limit reset or
	 * increased.
	 */
	STATUS_DS_MACHINE_ACCOUNT_QUOTA_EXCEEDED(
		0xC00002E7L,
		"Your computer could not be joined to the domain. You have exceeded the maximum number of " +
		"computer accounts you are allowed to create in this domain. Contact your system " +
		"administrator to have this limit reset or increased."),

	/** This operation cannot be performed on the current domain. */
	STATUS_CURRENT_DOMAIN_NOT_ALLOWED(0xC00002E9L, "This operation cannot be performed on the current domain."),

	/** The directory or file cannot be created. */
	STATUS_CANNOT_MAKE(0xC00002EAL, "The directory or file cannot be created."),

	/** The system is in the process of shutting down. */
	STATUS_SYSTEM_SHUTDOWN(0xC00002EBL, "The system is in the process of shutting down."),

	/**
	 * Directory Services could not start because of the following error: %hs
	 * Error Status: 0x%x. Click OK to shut down the system. You can use the
	 * recovery console to diagnose the system further.
	 */
	STATUS_DS_INIT_FAILURE_CONSOLE(
		0xC00002ECL,
		"Directory Services could not start because of the following error: %hs Error Status: 0x%x. " +
		"Click OK to shut down the system. You can use the recovery console to diagnose the system further."),

	/**
	 * Security Accounts Manager initialization failed because of the following
	 * error: %hs Error Status: 0x%x. Click OK to shut down the system. You can
	 * use the recovery console to diagnose the system further.
	 */
	STATUS_DS_SAM_INIT_FAILURE_CONSOLE(
		0xC00002EDL,
		"Security Accounts Manager initialization failed because of the following error: %hs Error Status: 0x%x. " +
		"Click OK to shut down the system. You can use the recovery console to diagnose the system further."),

	/**
	 * A security context was deleted before the context was completed. This is
	 * considered a logon failure.
	 */
	STATUS_UNFINISHED_CONTEXT_DELETED(0xC00002EEL,
		"A security context was deleted before the context was completed. This is considered a logon failure."),

	/**
	 * The client is trying to negotiate a context and the server requires
	 * user-to-user but did not send a TGT reply.
	 */
	STATUS_NO_TGT_REPLY(0xC00002EFL,
		"The client is trying to negotiate a context and the server requires user-to-user but did not send a TGT reply."),

	/** An object ID was not found in the file. */
	STATUS_OBJECTID_NOT_FOUND(0xC00002F0L, "An object ID was not found in the file."),

	/**
	 * Unable to accomplish the requested task because the local machine does
	 * not have any IP addresses.
	 */
	STATUS_NO_IP_ADDRESSES(0xC00002F1L, "Unable to accomplish the requested task because the local machine does not have any IP addresses."),

	/**
	 * The supplied credential handle does not match the credential that is
	 * associated with the security context.
	 */
	STATUS_WRONG_CREDENTIAL_HANDLE(0xC00002F2L,
		"The supplied credential handle does not match the credential that is associated with the security context."),

	/**
	 * The crypto system or checksum function is invalid because a required
	 * function is unavailable.
	 */
	STATUS_CRYPTO_SYSTEM_INVALID(0xC00002F3L,
		"The crypto system or checksum function is invalid because a required function is unavailable."),

	/** The number of maximum ticket referrals has been exceeded. */
	STATUS_MAX_REFERRALS_EXCEEDED(0xC00002F4L, "The number of maximum ticket referrals has been exceeded."),

	/**
	 * The local machine must be a Kerberos KDC (domain controller) and it is
	 * not.
	 */
	STATUS_MUST_BE_KDC(0xC00002F5L, "The local machine must be a Kerberos KDC (domain controller) and it is not."),

	/**
	 * The other end of the security negotiation requires strong crypto but it
	 * is not supported on the local machine.
	 */
	STATUS_STRONG_CRYPTO_NOT_SUPPORTED(0xC00002F6L,
		"The other end of the security negotiation requires strong crypto but it is not supported on the local machine."),

	/** The KDC reply contained more than one principal name. */
	STATUS_TOO_MANY_PRINCIPALS(0xC00002F7L, "The KDC reply contained more than one principal name."),

	/**
	 * Expected to find PA data for a hint of what etype to use, but it was not
	 * found.
	 */
	STATUS_NO_PA_DATA(0xC00002F8L, "Expected to find PA data for a hint of what etype to use, but it was not found."),

	/**
	 * The client certificate does not contain a valid UPN, or does not match
	 * the client name in the logon request. Contact your administrator.
	 */
	STATUS_PKINIT_NAME_MISMATCH(0xC00002F9L,
		"The client certificate does not contain a valid UPN, or does not match " +
		"the client name in the logon request. Contact your administrator."),

	/** Smart card logon is required and was not used. */
	STATUS_SMARTCARD_LOGON_REQUIRED(0xC00002FAL, "Smart card logon is required and was not used."),

	/** An invalid request was sent to the KDC. */
	STATUS_KDC_INVALID_REQUEST(0xC00002FBL, "An invalid request was sent to the KDC."),

	/** The KDC was unable to generate a referral for the service requested. */
	STATUS_KDC_UNABLE_TO_REFER(0xC00002FCL, "The KDC was unable to generate a referral for the service requested."),

	/** The encryption type requested is not supported by the KDC. */
	STATUS_KDC_UNKNOWN_ETYPE(0xC00002FDL, "The encryption type requested is not supported by the KDC."),

	/** A system shutdown is in progress. */
	STATUS_SHUTDOWN_IN_PROGRESS(0xC00002FEL, "A system shutdown is in progress."),

	/** The server machine is shutting down. */
	STATUS_SERVER_SHUTDOWN_IN_PROGRESS(0xC00002FFL, "The server machine is shutting down."),

	/**
	 * This operation is not supported on a computer running Windows Server 2003
	 * operating system for Small Business Server.
	 */
	STATUS_NOT_SUPPORTED_ON_SBS(0xC0000300L,
		"This operation is not supported on a computer running Windows Server 2003 operating system for Small Business Server."),

	/** The WMI GUID is no longer available. */
	STATUS_WMI_GUID_DISCONNECTED(0xC0000301L, "The WMI GUID is no longer available."),

	/** Collection or events for the WMI GUID is already disabled. */
	STATUS_WMI_ALREADY_DISABLED(0xC0000302L, "Collection or events for the WMI GUID is already disabled."),

	/** Collection or events for the WMI GUID is already enabled. */
	STATUS_WMI_ALREADY_ENABLED(0xC0000303L, "Collection or events for the WMI GUID is already enabled."),

	/**
	 * The master file table on the volume is too fragmented to complete this
	 * operation.
	 */
	STATUS_MFT_TOO_FRAGMENTED(0xC0000304L, "The master file table on the volume is too fragmented to complete this operation."),

	/** Copy protection failure. */
	STATUS_COPY_PROTECTION_FAILURE(0xC0000305L, "Copy protection failure."),

	/** Copy protection error—DVD CSS Authentication failed. */
	STATUS_CSS_AUTHENTICATION_FAILURE(0xC0000306L, "Copy protection error—DVD CSS Authentication failed."),

	/** Copy protection error—The specified sector does not contain a valid key. */
	STATUS_CSS_KEY_NOT_PRESENT(0xC0000307L, "Copy protection error—The specified sector does not contain a valid key."),

	/** Copy protection error—DVD session key not established. */
	STATUS_CSS_KEY_NOT_ESTABLISHED(0xC0000308L, "Copy protection error—DVD session key not established."),

	/** Copy protection error—The read failed because the sector is encrypted. */
	STATUS_CSS_SCRAMBLED_SECTOR(0xC0000309L, "Copy protection error—The read failed because the sector is encrypted."),

	/**
	 * Copy protection error—The region of the specified DVD does not correspond
	 * to the region setting of the drive.
	 */
	STATUS_CSS_REGION_MISMATCH(0xC000030AL,
		"Copy protection error—The region of the specified DVD does not correspond to the region setting of the drive."),

	/**
	 * Copy protection error—The region setting of the drive might be permanent.
	 */
	STATUS_CSS_RESETS_EXHAUSTED(0xC000030BL, "Copy protection error—The region setting of the drive might be permanent."),

	/**
	 * The Kerberos protocol encountered an error while validating the KDC
	 * certificate during smart card logon. There is more information in the
	 * system event log.
	 */
	STATUS_PKINIT_FAILURE(
		0xC0000320L,
		"The Kerberos protocol encountered an error while validating the KDC certificate " +
		"during smart card logon. There is more information in the system event log."),

	/**
	 * The Kerberos protocol encountered an error while attempting to use the
	 * smart card subsystem.
	 */
	STATUS_SMARTCARD_SUBSYSTEM_FAILURE(0xC0000321L,
		"The Kerberos protocol encountered an error while attempting to use the smart card subsystem."),

	/** The target server does not have acceptable Kerberos credentials. */
	STATUS_NO_KERB_KEY(0xC0000322L, "The target server does not have acceptable Kerberos credentials."),

	/** The transport determined that the remote system is down. */
	STATUS_HOST_DOWN(0xC0000350L, "The transport determined that the remote system is down."),

	/**
	 * An unsupported pre-authentication mechanism was presented to the Kerberos
	 * package.
	 */
	STATUS_UNSUPPORTED_PREAUTH(0xC0000351L, "An unsupported pre-authentication mechanism was presented to the Kerberos package."),

	/**
	 * The encryption algorithm that is used on the source file needs a bigger
	 * key buffer than the one that is used on the destination file.
	 */
	STATUS_EFS_ALG_BLOB_TOO_BIG(0xC0000352L,
		"The encryption algorithm that is used on the source file needs a " +
		"bigger key buffer than the one that is used on the destination file."),

	/**
	 * An attempt to remove a processes DebugPort was made, but a port was not
	 * already associated with the process.
	 */
	STATUS_PORT_NOT_SET(0xC0000353L,
		"An attempt to remove a processes DebugPort was made, but a port was not already associated with the process."),

	/**
	 * An attempt to do an operation on a debug port failed because the port is
	 * in the process of being deleted.
	 */
	STATUS_DEBUGGER_INACTIVE(0xC0000354L,
		"An attempt to do an operation on a debug port failed because the port is in the process of being deleted."),

	/**
	 * This version of Windows is not compatible with the behavior version of
	 * the directory forest, domain, or domain controller.
	 */
	STATUS_DS_VERSION_CHECK_FAILURE(0xC0000355L,
		"This version of Windows is not compatible with the behavior version of the directory forest, domain, or domain controller."),

	/** The specified event is currently not being audited. */
	STATUS_AUDITING_DISABLED(0xC0000356L, "The specified event is currently not being audited."),

	/**
	 * The machine account was created prior to Windows NT 4.0 operating system.
	 * The account needs to be recreated.
	 */
	STATUS_PRENT4_MACHINE_ACCOUNT(0xC0000357L,
		"The machine account was created prior to Windows NT 4.0 operating system. The account needs to be recreated."),

	/** An account group cannot have a universal group as a member. */
	STATUS_DS_AG_CANT_HAVE_UNIVERSAL_MEMBER(0xC0000358L, "An account group cannot have a universal group as a member."),

	/**
	 * The specified image file did not have the correct format; it appears to
	 * be a 32-bit Windows image.
	 */
	STATUS_INVALID_IMAGE_WIN_32(0xC0000359L,
		"The specified image file did not have the correct format; it appears to be a 32-bit Windows image."),

	/**
	 * The specified image file did not have the correct format; it appears to
	 * be a 64-bit Windows image.
	 */
	STATUS_INVALID_IMAGE_WIN_64(0xC000035AL,
		"The specified image file did not have the correct format; it appears to be a 64-bit Windows image."),

	/** The client's supplied SSPI channel bindings were incorrect. */
	STATUS_BAD_BINDINGS(0xC000035BL, "The client's supplied SSPI channel bindings were incorrect."),

	/**
	 * The client session has expired; so the client must re-authenticate to
	 * continue accessing the remote resources.
	 */
	STATUS_NETWORK_SESSION_EXPIRED(0xC000035CL,
		"The client session has expired; so the client must re-authenticate to continue accessing the remote resources."),

	/**
	 * The AppHelp dialog box canceled; thus preventing the application from
	 * starting.
	 */
	STATUS_APPHELP_BLOCK(0xC000035DL, "The AppHelp dialog box canceled; thus preventing the application from starting."),

	/** The SID filtering operation removed all SIDs. */
	STATUS_ALL_SIDS_FILTERED(0xC000035EL, "The SID filtering operation removed all SIDs."),

	/** The driver was not loaded because the system is starting in safe mode. */
	STATUS_NOT_SAFE_MODE_DRIVER(0xC000035FL, "The driver was not loaded because the system is starting in safe mode."),

	/**
	 * Access to %1 has been restricted by your Administrator by the default
	 * software restriction policy level.
	 */
	STATUS_ACCESS_DISABLED_BY_POLICY_DEFAULT(0xC0000361L,
		"Access to %1 has been restricted by your Administrator by the default software restriction policy level."),

	/**
	 * Access to %1 has been restricted by your Administrator by location with
	 * policy rule %2 placed on path %3.
	 */
	STATUS_ACCESS_DISABLED_BY_POLICY_PATH(0xC0000362L,
		"Access to %1 has been restricted by your Administrator by location with policy rule %2 placed on path %3."),

	/**
	 * Access to %1 has been restricted by your Administrator by software
	 * publisher policy.
	 */
	STATUS_ACCESS_DISABLED_BY_POLICY_PUBLISHER(0xC0000363L,
		"Access to %1 has been restricted by your Administrator by software publisher policy."),

	/**
	 * Access to %1 has been restricted by your Administrator by policy rule %2.
	 */
	STATUS_ACCESS_DISABLED_BY_POLICY_OTHER(0xC0000364L, "Access to %1 has been restricted by your Administrator by policy rule %2."),

	/** The driver was not loaded because it failed its initialization call. */
	STATUS_FAILED_DRIVER_ENTRY(0xC0000365L, "The driver was not loaded because it failed its initialization call."),

	/**
	 * The device encountered an error while applying power or reading the
	 * device configuration. This might be caused by a failure of your hardware
	 * or by a poor connection.
	 */
	STATUS_DEVICE_ENUMERATION_ERROR(
		0xC0000366L,
		"The device encountered an error while applying power or reading the device configuration. " +
		"This might be caused by a failure of your hardware or by a poor connection."),

	/**
	 * The create operation failed because the name contained at least one mount
	 * point that resolves to a volume to which the specified device object is
	 * not attached.
	 */
	STATUS_MOUNT_POINT_NOT_RESOLVED(
		0xC0000368L,
		"The create operation failed because the name contained at least one mount point that " +
		"resolves to a volume to which the specified device object is not attached."),

	/**
	 * The device object parameter is either not a valid device object or is not
	 * attached to the volume that is specified by the file name.
	 */
	STATUS_INVALID_DEVICE_OBJECT_PARAMETER(0xC0000369L,
		"The device object parameter is either not a valid device object or is " +
		"not attached to the volume that is specified by the file name."),

	/**
	 * A machine check error has occurred. Check the system event log for
	 * additional information.
	 */
	STATUS_MCA_OCCURED(0xC000036AL, "A machine check error has occurred. Check the system event log for additional information."),

	/** Driver %2 has been blocked from loading. */
	STATUS_DRIVER_BLOCKED_CRITICAL(0xC000036BL, "Driver %2 has been blocked from loading."),

	/** Driver %2 has been blocked from loading. */
	STATUS_DRIVER_BLOCKED(0xC000036CL, "Driver %2 has been blocked from loading."),

	/** There was error [%2] processing the driver database. */
	STATUS_DRIVER_DATABASE_ERROR(0xC000036DL, "There was error [%2] processing the driver database."),

	/** System hive size has exceeded its limit. */
	STATUS_SYSTEM_HIVE_TOO_LARGE(0xC000036EL, "System hive size has exceeded its limit."),

	/**
	 * A dynamic link library (DLL) referenced a module that was neither a DLL
	 * nor the process's executable image.
	 */
	STATUS_INVALID_IMPORT_OF_NON_DLL(0xC000036FL,
		"A dynamic link library (DLL) referenced a module that was neither a DLL nor the process's executable image."),

	/**
	 * The local account store does not contain secret material for the
	 * specified account.
	 */
	STATUS_NO_SECRETS(0xC0000371L, "The local account store does not contain secret material for the specified account."),

	/**
	 * Access to %1 has been restricted by your Administrator by policy rule %2.
	 */
	STATUS_ACCESS_DISABLED_NO_SAFER_UI_BY_POLICY(0xC0000372L, "Access to %1 has been restricted by your Administrator by policy rule %2."),

	/**
	 * The system was not able to allocate enough memory to perform a stack
	 * switch.
	 */
	STATUS_FAILED_STACK_SWITCH(0xC0000373L, "The system was not able to allocate enough memory to perform a stack switch."),

	/** A heap has been corrupted. */
	STATUS_HEAP_CORRUPTION(0xC0000374L, "A heap has been corrupted."),

	/** An incorrect PIN was presented to the smart card. */
	STATUS_SMARTCARD_WRONG_PIN(0xC0000380L, "An incorrect PIN was presented to the smart card."),

	/** The smart card is blocked. */
	STATUS_SMARTCARD_CARD_BLOCKED(0xC0000381L, "The smart card is blocked."),

	/** No PIN was presented to the smart card. */
	STATUS_SMARTCARD_CARD_NOT_AUTHENTICATED(0xC0000382L, "No PIN was presented to the smart card."),

	/** No smart card is available. */
	STATUS_SMARTCARD_NO_CARD(0xC0000383L, "No smart card is available."),

	/** The requested key container does not exist on the smart card. */
	STATUS_SMARTCARD_NO_KEY_CONTAINER(0xC0000384L, "The requested key container does not exist on the smart card."),

	/** The requested certificate does not exist on the smart card. */
	STATUS_SMARTCARD_NO_CERTIFICATE(0xC0000385L, "The requested certificate does not exist on the smart card."),

	/** The requested keyset does not exist. */
	STATUS_SMARTCARD_NO_KEYSET(0xC0000386L, "The requested keyset does not exist."),

	/** A communication error with the smart card has been detected. */
	STATUS_SMARTCARD_IO_ERROR(0xC0000387L, "A communication error with the smart card has been detected."),

	/**
	 * The system detected a possible attempt to compromise security. Ensure
	 * that you can contact the server that authenticated you.
	 */
	STATUS_DOWNGRADE_DETECTED(0xC0000388L,
		"The system detected a possible attempt to compromise security. Ensure that you can contact the server that authenticated you."),

	/**
	 * The smart card certificate used for authentication has been revoked.
	 * Contact your system administrator. There might be additional information
	 * in the event log.
	 */
	STATUS_SMARTCARD_CERT_REVOKED(
		0xC0000389L,
		"The smart card certificate used for authentication has been revoked. Contact your " +
		"system administrator. There might be additional information in the event log."),

	/**
	 * An untrusted certificate authority was detected while processing the
	 * smart card certificate that is used for authentication. Contact your
	 * system administrator.
	 */
	STATUS_ISSUING_CA_UNTRUSTED(
		0xC000038AL,
		"An untrusted certificate authority was detected while processing the smart card " +
		"certificate that is used for authentication. Contact your system administrator."),

	/**
	 * The revocation status of the smart card certificate that is used for
	 * authentication could not be determined. Contact your system
	 * administrator.
	 */
	STATUS_REVOCATION_OFFLINE_C(
		0xC000038BL,
		"The revocation status of the smart card certificate that is used for authentication " +
		"could not be determined. Contact your system administrator."),

	/**
	 * The smart card certificate used for authentication was not trusted.
	 * Contact your system administrator.
	 */
	STATUS_PKINIT_CLIENT_FAILURE(0xC000038CL,
		"The smart card certificate used for authentication was not trusted. Contact your system administrator."),

	/**
	 * The smart card certificate used for authentication has expired. Contact
	 * your system administrator.
	 */
	STATUS_SMARTCARD_CERT_EXPIRED(0xC000038DL,
		"The smart card certificate used for authentication has expired. Contact your system administrator."),

	/**
	 * The driver could not be loaded because a previous version of the driver
	 * is still in memory.
	 */
	STATUS_DRIVER_FAILED_PRIOR_UNLOAD(0xC000038EL,
		"The driver could not be loaded because a previous version of the driver is still in memory."),

	/**
	 * The smart card provider could not perform the action because the context
	 * was acquired as silent.
	 */
	STATUS_SMARTCARD_SILENT_CONTEXT(0xC000038FL,
		"The smart card provider could not perform the action because the context was acquired as silent."),

	/**
	 * The delegated trust creation quota of the current user has been exceeded.
	 */
	STATUS_PER_USER_TRUST_QUOTA_EXCEEDED(0xC0000401L, "The delegated trust creation quota of the current user has been exceeded."),

	/** The total delegated trust creation quota has been exceeded. */
	STATUS_ALL_USER_TRUST_QUOTA_EXCEEDED(0xC0000402L, "The total delegated trust creation quota has been exceeded."),

	/**
	 * The delegated trust deletion quota of the current user has been exceeded.
	 */
	STATUS_USER_DELETE_TRUST_QUOTA_EXCEEDED(0xC0000403L, "The delegated trust deletion quota of the current user has been exceeded."),

	/** The requested name already exists as a unique identifier. */
	STATUS_DS_NAME_NOT_UNIQUE(0xC0000404L, "The requested name already exists as a unique identifier."),

	/**
	 * The requested object has a non-unique identifier and cannot be retrieved.
	 */
	STATUS_DS_DUPLICATE_ID_FOUND(0xC0000405L, "The requested object has a non-unique identifier and cannot be retrieved."),

	/**
	 * The group cannot be converted due to attribute restrictions on the
	 * requested group type.
	 */
	STATUS_DS_GROUP_CONVERSION_ERROR(0xC0000406L, "The group cannot be converted " +
		"due to attribute restrictions on the requested group type."),

	/**
	 * {Volume Shadow Copy Service} Wait while the Volume Shadow Copy Service
	 * prepares volume %hs for hibernation.
	 */
	STATUS_VOLSNAP_PREPARE_HIBERNATE(0xC0000407L,
		"{Volume Shadow Copy Service} Wait while the Volume Shadow Copy Service prepares volume %hs for hibernation."),

	/** Kerberos sub-protocol User2User is required. */
	STATUS_USER2USER_REQUIRED(0xC0000408L, "Kerberos sub-protocol User2User is required."),

	/**
	 * The system detected an overrun of a stack-based buffer in this
	 * application. This overrun could potentially allow a malicious user to
	 * gain control of this application.
	 */
	STATUS_STACK_BUFFER_OVERRUN(
		0xC0000409L,
		"The system detected an overrun of a stack-based buffer in this application. " +
		"This overrun could potentially allow a malicious user to gain control of this application."),

	/**
	 * The Kerberos subsystem encountered an error. A service for user protocol
	 * request was made against a domain controller which does not support
	 * service for user.
	 */
	STATUS_NO_S4U_PROT_SUPPORT(
		0xC000040AL,
		"The Kerberos subsystem encountered an error. A service for user protocol request was " +
		"made against a domain controller which does not support service for user."),

	/**
	 * An attempt was made by this server to make a Kerberos constrained
	 * delegation request for a target that is outside the server realm. This
	 * action is not supported and the resulting error indicates a
	 * misconfiguration on the allowed-to-delegate-to list for this server.
	 * Contact your administrator.
	 */
	STATUS_CROSSREALM_DELEGATION_FAILURE(
		0xC000040BL,
		"An attempt was made by this server to make a Kerberos constrained delegation request " +
		"for a target that is outside the server realm. This action is not supported and the " +
		"resulting error indicates a misconfiguration on the allowed-to-delegate-to list for " +
		"this server. Contact your administrator."),

	/**
	 * The revocation status of the domain controller certificate used for smart
	 * card authentication could not be determined. There is additional
	 * information in the system event log. Contact your system administrator.
	 */
	STATUS_REVOCATION_OFFLINE_KDC(
		0xC000040CL,
		"The revocation status of the domain controller certificate used for smart card authentication " +
		"could not be determined. There is additional information in the system event log. " +
		"Contact your system administrator."),

	/**
	 * An untrusted certificate authority was detected while processing the
	 * domain controller certificate used for authentication. There is
	 * additional information in the system event log. Contact your system
	 * administrator.
	 */
	STATUS_ISSUING_CA_UNTRUSTED_KDC(
		0xC000040DL,
		"An untrusted certificate authority was detected while processing the domain controller " +
		"certificate used for authentication. There is additional information in the system event log. " +
		"Contact your system administrator."),

	/**
	 * The domain controller certificate used for smart card logon has expired.
	 * Contact your system administrator with the contents of your system event
	 * log.
	 */
	STATUS_KDC_CERT_EXPIRED(
		0xC000040EL,
		"The domain controller certificate used for smart card logon has expired. " +
		"Contact your system administrator with the contents of your system event log."),

	/**
	 * The domain controller certificate used for smart card logon has been
	 * revoked. Contact your system administrator with the contents of your
	 * system event log.
	 */
	STATUS_KDC_CERT_REVOKED(
		0xC000040FL,
		"The domain controller certificate used for smart card logon has been revoked. " +
		"Contact your system administrator with the contents of your system event log."),

	/**
	 * Data present in one of the parameters is more than the function can
	 * operate on.
	 */
	STATUS_PARAMETER_QUOTA_EXCEEDED(0xC0000410L, "Data present in one of the parameters is more than the function can operate on."),

	/**
	 * The system has failed to hibernate (The error code is %hs). Hibernation
	 * will be disabled until the system is restarted.
	 */
	STATUS_HIBERNATION_FAILURE(0xC0000411L,
		"The system has failed to hibernate (The error code is %hs). Hibernation will be disabled until the system is restarted."),

	/**
	 * An attempt to delay-load a .dll or get a function address in a
	 * delay-loaded .dll failed.
	 */
	STATUS_DELAY_LOAD_FAILED(0xC0000412L, "An attempt to delay-load a .dll or get a function address in a delay-loaded .dll failed."),

	/**
	 * Logon Failure: The machine you are logging onto is protected by an
	 * authentication firewall. The specified account is not allowed to
	 * authenticate to the machine.
	 */
	STATUS_AUTHENTICATION_FIREWALL_FAILED(
		0xC0000413L,
		"Logon Failure: The machine you are logging onto is protected by an authentication firewall. " +
		"The specified account is not allowed to authenticate to the machine."),

	/**
	 * %hs is a 16-bit application. You do not have permissions to execute
	 * 16-bit applications. Check your permissions with your system
	 * administrator.
	 */
	STATUS_VDM_DISALLOWED(
		0xC0000414L,
		"%hs is a 16-bit application. You do not have permissions to execute 16-bit applications. " +
		"Check your permissions with your system administrator."),

	/**
	 * {Display Driver Stopped Responding} The %hs display driver has stopped
	 * working normally. Save your work and reboot the system to restore full
	 * display functionality. The next time you reboot the machine a dialog will
	 * be displayed giving you a chance to report this failure to Microsoft.
	 */
	STATUS_HUNG_DISPLAY_DRIVER_THREAD(
		0xC0000415L,
		"{Display Driver Stopped Responding} The %hs display driver has stopped working normally. " +
		"Save your work and reboot the system to restore full display functionality. " +
		"The next time you reboot the machine a dialog will be displayed giving you a " +
		"chance to report this failure to Microsoft."),

	/**
	 * The Desktop heap encountered an error while allocating session memory.
	 * There is more information in the system event log.
	 */
	STATUS_INSUFFICIENT_RESOURCE_FOR_SPECIFIED_SHARED_SECTION_SIZE(0xC0000416L,
		"The Desktop heap encountered an error while allocating session memory. There is more information in the system event log."),

	/** An invalid parameter was passed to a C runtime function. */
	STATUS_INVALID_CRUNTIME_PARAMETER(0xC0000417L, "An invalid parameter was passed to a C runtime function."),

	/** The authentication failed because NTLM was blocked. */
	STATUS_NTLM_BLOCKED(0xC0000418L, "The authentication failed because NTLM was blocked."),

	/** The source object's SID already exists in destination forest. */
	STATUS_DS_SRC_SID_EXISTS_IN_FOREST(0xC0000419L, "The source object's SID already exists in destination forest."),

	/** The domain name of the trusted domain already exists in the forest. */
	STATUS_DS_DOMAIN_NAME_EXISTS_IN_FOREST(0xC000041AL, "The domain name of the trusted domain already exists in the forest."),

	/** The flat name of the trusted domain already exists in the forest. */
	STATUS_DS_FLAT_NAME_EXISTS_IN_FOREST(0xC000041BL, "The flat name of the trusted domain already exists in the forest."),

	/** The User Principal Name (UPN) is invalid. */
	STATUS_INVALID_USER_PRINCIPAL_NAME(0xC000041CL, "The User Principal Name (UPN) is invalid."),

	/** There has been an assertion failure. */
	STATUS_ASSERTION_FAILURE(0xC0000420L, "There has been an assertion failure."),

	/** Application verifier has found an error in the current process. */
	STATUS_VERIFIER_STOP(0xC0000421L, "Application verifier has found an error in the current process."),

	/** A user mode unwind is in progress. */
	STATUS_CALLBACK_POP_STACK(0xC0000423L, "A user mode unwind is in progress."),

	/**
	 * %2 has been blocked from loading due to incompatibility with this system.
	 * Contact your software vendor for a compatible version of the driver.
	 */
	STATUS_INCOMPATIBLE_DRIVER_BLOCKED(
		0xC0000424L,
		"%2 has been blocked from loading due to incompatibility with this system. " +
		"Contact your software vendor for a compatible version of the driver."),

	/**
	 * Illegal operation attempted on a registry key which has already been
	 * unloaded.
	 */
	STATUS_HIVE_UNLOADED(0xC0000425L, "Illegal operation attempted on a registry key which has already been unloaded."),

	/** Compression is disabled for this volume. */
	STATUS_COMPRESSION_DISABLED(0xC0000426L, "Compression is disabled for this volume."),

	/**
	 * The requested operation could not be completed due to a file system
	 * limitation.
	 */
	STATUS_FILE_SYSTEM_LIMITATION(0xC0000427L, "The requested operation could not be completed due to a file system limitation."),

	/**
	 * The hash for image %hs cannot be found in the system catalogs. The image
	 * is likely corrupt or the victim of tampering.
	 */
	STATUS_INVALID_IMAGE_HASH(0xC0000428L,
		"The hash for image %hs cannot be found in the system catalogs. The image is likely corrupt or the victim of tampering."),

	/** The implementation is not capable of performing the request. */
	STATUS_NOT_CAPABLE(0xC0000429L, "The implementation is not capable of performing the request."),

	/**
	 * The requested operation is out of order with respect to other operations.
	 */
	STATUS_REQUEST_OUT_OF_SEQUENCE(0xC000042AL, "The requested operation is out of order with respect to other operations."),

	/** An operation attempted to exceed an implementation-defined limit. */
	STATUS_IMPLEMENTATION_LIMIT(0xC000042BL, "An operation attempted to exceed an implementation-defined limit."),

	/** The requested operation requires elevation. */
	STATUS_ELEVATION_REQUIRED(0xC000042CL, "The requested operation requires elevation."),

	/** The required security context does not exist. */
	STATUS_NO_SECURITY_CONTEXT(0xC000042DL, "The required security context does not exist."),

	/**
	 * The PKU2U protocol encountered an error while attempting to utilize the
	 * associated certificates.
	 */
	STATUS_PKU2U_CERT_FAILURE(0xC000042EL,
		"The PKU2U protocol encountered an error while attempting to utilize the associated certificates."),

	/** The operation was attempted beyond the valid data length of the file. */
	STATUS_BEYOND_VDL(0xC0000432L, "The operation was attempted beyond the valid data length of the file."),

	/**
	 * The attempted write operation encountered a write already in progress for
	 * some portion of the range.
	 */
	STATUS_ENCOUNTERED_WRITE_IN_PROGRESS(0xC0000433L,
		"The attempted write operation encountered a write already in progress for some portion of the range."),

	/**
	 * The page fault mappings changed in the middle of processing a fault so
	 * the operation must be retried.
	 */
	STATUS_PTE_CHANGED(0xC0000434L, "The page fault mappings changed in the middle of processing a fault so the operation must be retried."),

	/**
	 * The attempt to purge this file from memory failed to purge some or all
	 * the data from memory.
	 */
	STATUS_PURGE_FAILED(0xC0000435L, "The attempt to purge this file from memory failed to purge some or all the data from memory."),

	/** The requested credential requires confirmation. */
	STATUS_CRED_REQUIRES_CONFIRMATION(0xC0000440L, "The requested credential requires confirmation."),

	/**
	 * The remote server sent an invalid response for a file being opened with
	 * Client Side Encryption.
	 */
	STATUS_CS_ENCRYPTION_INVALID_SERVER_RESPONSE(0xC0000441L,
		"The remote server sent an invalid response for a file being opened with Client Side Encryption."),

	/**
	 * Client Side Encryption is not supported by the remote server even though
	 * it claims to support it.
	 */
	STATUS_CS_ENCRYPTION_UNSUPPORTED_SERVER(0xC0000442L,
		"Client Side Encryption is not supported by the remote server even though it claims to support it."),

	/** File is encrypted and should be opened in Client Side Encryption mode. */
	STATUS_CS_ENCRYPTION_EXISTING_ENCRYPTED_FILE(0xC0000443L, "File is encrypted and should be opened in Client Side Encryption mode."),

	/** A new encrypted file is being created and a $EFS needs to be provided. */
	STATUS_CS_ENCRYPTION_NEW_ENCRYPTED_FILE(0xC0000444L, "A new encrypted file is being created and a $EFS needs to be provided."),

	/** The SMB client requested a CSE FSCTL on a non-CSE file. */
	STATUS_CS_ENCRYPTION_FILE_NOT_CSE(0xC0000445L, "The SMB client requested a CSE FSCTL on a non-CSE file."),

	/**
	 * Indicates a particular Security ID cannot be assigned as the label of an
	 * object.
	 */
	STATUS_INVALID_LABEL(0xC0000446L, "Indicates a particular Security ID cannot be assigned as the label of an object."),

	/** The process hosting the driver for this device has terminated. */
	STATUS_DRIVER_PROCESS_TERMINATED(0xC0000450L, "The process hosting the driver for this device has terminated."),

	/**
	 * The requested system device cannot be identified due to multiple
	 * indistinguishable devices potentially matching the identification
	 * criteria.
	 */
	STATUS_AMBIGUOUS_SYSTEM_DEVICE(
		0xC0000451L,
		"The requested system device cannot be identified due to multiple indistinguishable " +
		"devices potentially matching the identification criteria."),

	/** The requested system device cannot be found. */
	STATUS_SYSTEM_DEVICE_NOT_FOUND(0xC0000452L, "The requested system device cannot be found."),

	/** This boot application must be restarted. */
	STATUS_RESTART_BOOT_APPLICATION(0xC0000453L, "This boot application must be restarted."),

	/**
	 * Insufficient NVRAM resources exist to complete the API. A reboot might be
	 * required.
	 */
	STATUS_INSUFFICIENT_NVRAM_RESOURCES(0xC0000454L, "Insufficient NVRAM resources exist to complete the API. A reboot might be required."),

	/** No ranges for the specified operation were able to be processed. */
	STATUS_NO_RANGES_PROCESSED(0xC0000460L, "No ranges for the specified operation were able to be processed."),

	/** The storage device does not support Offload Write. */
	STATUS_DEVICE_FEATURE_NOT_SUPPORTED(0xC0000463L, "The storage device does not support Offload Write."),

	/**
	 * Data cannot be moved because the source device cannot communicate with
	 * the destination device.
	 */
	STATUS_DEVICE_UNREACHABLE(0xC0000464L, "Data cannot be moved because the source device cannot communicate with the destination device."),

	/** The token representing the data is invalid or expired. */
	STATUS_INVALID_TOKEN(0xC0000465L, "The token representing the data is invalid or expired."),

	/** The file server is temporarily unavailable. */
	STATUS_SERVER_UNAVAILABLE(0xC0000466L, "The file server is temporarily unavailable."),

	/** The specified task name is invalid. */
	STATUS_INVALID_TASK_NAME(0xC0000500L, "The specified task name is invalid."),

	/** The specified task index is invalid. */
	STATUS_INVALID_TASK_INDEX(0xC0000501L, "The specified task index is invalid."),

	/** The specified thread is already joining a task. */
	STATUS_THREAD_ALREADY_IN_TASK(0xC0000502L, "The specified thread is already joining a task."),

	/** A callback has requested to bypass native code. */
	STATUS_CALLBACK_BYPASS(0xC0000503L, "A callback has requested to bypass native code."),

	/**
	 * A fail fast exception occurred. Exception handlers will not be invoked
	 * and the process will be terminated immediately.
	 */
	STATUS_FAIL_FAST_EXCEPTION(0xC0000602L,
		"A fail fast exception occurred. Exception handlers will not be invoked and the process will be terminated immediately."),

	/**
	 * Windows cannot verify the digital signature for this file. The signing
	 * certificate for this file has been revoked.
	 */
	STATUS_IMAGE_CERT_REVOKED(0xC0000603L,
		"Windows cannot verify the digital signature for this file. The signing certificate for this file has been revoked."),

	/** The ALPC port is closed. */
	STATUS_PORT_CLOSED(0xC0000700L, "The ALPC port is closed."),

	/** The ALPC message requested is no longer available. */
	STATUS_MESSAGE_LOST(0xC0000701L, "The ALPC message requested is no longer available."),

	/** The ALPC message supplied is invalid. */
	STATUS_INVALID_MESSAGE(0xC0000702L, "The ALPC message supplied is invalid."),

	/** The ALPC message has been canceled. */
	STATUS_REQUEST_CANCELED(0xC0000703L, "The ALPC message has been canceled."),

	/** Invalid recursive dispatch attempt. */
	STATUS_RECURSIVE_DISPATCH(0xC0000704L, "Invalid recursive dispatch attempt."),

	/** No receive buffer has been supplied in a synchronous request. */
	STATUS_LPC_RECEIVE_BUFFER_EXPECTED(0xC0000705L, "No receive buffer has been supplied in a synchronous request."),

	/** The connection port is used in an invalid context. */
	STATUS_LPC_INVALID_CONNECTION_USAGE(0xC0000706L, "The connection port is used in an invalid context."),

	/** The ALPC port does not accept new request messages. */
	STATUS_LPC_REQUESTS_NOT_ALLOWED(0xC0000707L, "The ALPC port does not accept new request messages."),

	/** The resource requested is already in use. */
	STATUS_RESOURCE_IN_USE(0xC0000708L, "The resource requested is already in use."),

	/** The hardware has reported an uncorrectable memory error. */
	STATUS_HARDWARE_MEMORY_ERROR(0xC0000709L, "The hardware has reported an uncorrectable memory error."),

	/**
	 * Status 0x%08x was returned, waiting on handle 0x%x for wait 0x%p, in
	 * waiter 0x%p.
	 */
	STATUS_THREADPOOL_HANDLE_EXCEPTION(0xC000070AL, "Status 0x%08x was returned, waiting on handle 0x%x for wait 0x%p, in waiter 0x%p."),

	/**
	 * After a callback to 0x%p(0x%p), a completion call to Set event(0x%p)
	 * failed with status 0x%08x.
	 */
	STATUS_THREADPOOL_SET_EVENT_ON_COMPLETION_FAILED(0xC000070BL,
		"After a callback to 0x%p(0x%p), a completion call to Set event(0x%p) failed with status 0x%08x."),

	/**
	 * After a callback to 0x%p(0x%p), a completion call to
	 * ReleaseSemaphore(0x%p, %d) failed with status 0x%08x.
	 */
	STATUS_THREADPOOL_RELEASE_SEMAPHORE_ON_COMPLETION_FAILED(0xC000070CL,
		"After a callback to 0x%p(0x%p), a completion call to ReleaseSemaphore(0x%p, %d) failed with status 0x%08x."),

	/**
	 * After a callback to 0x%p(0x%p), a completion call to ReleaseMutex(%p)
	 * failed with status 0x%08x.
	 */
	STATUS_THREADPOOL_RELEASE_MUTEX_ON_COMPLETION_FAILED(0xC000070DL,
		"After a callback to 0x%p(0x%p), a completion call to ReleaseMutex(%p) failed with status 0x%08x."),

	/**
	 * After a callback to 0x%p(0x%p), a completion call to FreeLibrary(%p)
	 * failed with status 0x%08x.
	 */
	STATUS_THREADPOOL_FREE_LIBRARY_ON_COMPLETION_FAILED(0xC000070EL,
		"After a callback to 0x%p(0x%p), a completion call to FreeLibrary(%p) failed with status 0x%08x."),

	/**
	 * The thread pool 0x%p was released while a thread was posting a callback
	 * to 0x%p(0x%p) to it.
	 */
	STATUS_THREADPOOL_RELEASED_DURING_OPERATION(0xC000070FL,
		"The thread pool 0x%p was released while a thread was posting a callback to 0x%p(0x%p) to it."),

	/**
	 * A thread pool worker thread is impersonating a client, after a callback
	 * to 0x%p(0x%p). This is unexpected, indicating that the callback is
	 * missing a call to revert the impersonation.
	 */
	STATUS_CALLBACK_RETURNED_WHILE_IMPERSONATING(
		0xC0000710L,
		"A thread pool worker thread is impersonating a client, after a callback to 0x%p(0x%p). " +
		"This is unexpected, indicating that the callback is missing a call to revert the impersonation."),

	/**
	 * A thread pool worker thread is impersonating a client, after executing an
	 * APC. This is unexpected, indicating that the APC is missing a call to
	 * revert the impersonation.
	 */
	STATUS_APC_RETURNED_WHILE_IMPERSONATING(
		0xC0000711L,
		"A thread pool worker thread is impersonating a client, after executing an APC. This is unexpected, " +
		"indicating that the APC is missing a call to revert the impersonation."),

	/**
	 * Either the target process, or the target thread's containing process, is
	 * a protected process.
	 */
	STATUS_PROCESS_IS_PROTECTED(0xC0000712L, "Either the target process, or " +
		"the target thread's containing process, is a protected process."),

	/** A thread is getting dispatched with MCA EXCEPTION because of MCA. */
	STATUS_MCA_EXCEPTION(0xC0000713L, "A thread is getting dispatched with MCA EXCEPTION because of MCA."),

	/** The client certificate account mapping is not unique. */
	STATUS_CERTIFICATE_MAPPING_NOT_UNIQUE(0xC0000714L, "The client certificate account mapping is not unique."),

	/** The symbolic link cannot be followed because its type is disabled. */
	STATUS_SYMLINK_CLASS_DISABLED(0xC0000715L, "The symbolic link cannot be followed because its type is disabled."),

	/** Indicates that the specified string is not valid for IDN normalization. */
	STATUS_INVALID_IDN_NORMALIZATION(0xC0000716L, "Indicates that the specified string is not valid for IDN normalization."),

	/**
	 * No mapping for the Unicode character exists in the target multi-byte code
	 * page.
	 */
	STATUS_NO_UNICODE_TRANSLATION(0xC0000717L, "No mapping for the Unicode character exists in the target multi-byte code page."),

	/** The provided callback is already registered. */
	STATUS_ALREADY_REGISTERED(0xC0000718L, "The provided callback is already registered."),

	/** The provided context did not match the target. */
	STATUS_CONTEXT_MISMATCH(0xC0000719L, "The provided context did not match the target."),

	/** The specified port already has a completion list. */
	STATUS_PORT_ALREADY_HAS_COMPLETION_LIST(0xC000071AL, "The specified port already has a completion list."),

	/**
	 * A threadpool worker thread entered a callback at thread base priority
	 * 0x%x and exited at priority 0x%x.
	 * <p>
	 * This is unexpected, indicating that the callback missed restoring the
	 * priority.
	 */
	STATUS_CALLBACK_RETURNED_THREAD_PRIORITY(
		0xC000071BL,
		"A threadpool worker thread entered a callback at thread base priority 0x%x and exited at priority 0x%x." +
		"\n\nThis is unexpected, indicating that the callback missed restoring the priority."),

	/**
	 * An invalid thread, handle %p, is specified for this operation. Possibly,
	 * a threadpool worker thread was specified.
	 */
	STATUS_INVALID_THREAD(0xC000071CL,
		"An invalid thread, handle %p, is specified for this operation. Possibly, a threadpool worker thread was specified."),

	/**
	 * A threadpool worker thread entered a callback, which left transaction
	 * state.
	 * <p>
	 * This is unexpected, indicating that the callback missed clearing the
	 * transaction.
	 */
	STATUS_CALLBACK_RETURNED_TRANSACTION(
		0xC000071DL,
		"A threadpool worker thread entered a callback, which left transaction state.\n\n" +
		"This is unexpected, indicating that the callback missed clearing the transaction."),

	/**
	 * A threadpool worker thread entered a callback, which left the loader lock
	 * held.
	 * <p>
	 * This is unexpected, indicating that the callback missed releasing the
	 * lock.
	 */
	STATUS_CALLBACK_RETURNED_LDR_LOCK(
		0xC000071EL,
		"A threadpool worker thread entered a callback, which left the loader lock held.\n\n" +
		"This is unexpected, indicating that the callback missed releasing the lock."),

	/**
	 * A threadpool worker thread entered a callback, which left with preferred
	 * languages set.
	 * <p>
	 * This is unexpected, indicating that the callback missed clearing them.
	 */
	STATUS_CALLBACK_RETURNED_LANG(
		0xC000071FL,
		"A threadpool worker thread entered a callback, which left with preferred languages set.\n\n" +
		"This is unexpected, indicating that the callback missed clearing them."),

	/**
	 * A threadpool worker thread entered a callback, which left with background
	 * priorities set.
	 * <p>
	 * This is unexpected, indicating that the callback missed restoring the
	 * original priorities.
	 */
	STATUS_CALLBACK_RETURNED_PRI_BACK(
		0xC0000720L,
		"A threadpool worker thread entered a callback, which left with background priorities set.\n\n" +
		"This is unexpected, indicating that the callback missed restoring the original priorities."),

	/** The attempted operation required self healing to be enabled. */
	STATUS_DISK_REPAIR_DISABLED(0xC0000800L, "The attempted operation required self healing to be enabled."),

	/**
	 * The directory service cannot perform the requested operation because a
	 * domain rename operation is in progress.
	 */
	STATUS_DS_DOMAIN_RENAME_IN_PROGRESS(0xC0000801L,
		"The directory service cannot perform the requested operation because a domain rename operation is in progress."),

	/** An operation failed because the storage quota was exceeded. */
	STATUS_DISK_QUOTA_EXCEEDED(0xC0000802L, "An operation failed because the storage quota was exceeded."),

	/** An operation failed because the content was blocked. */
	STATUS_CONTENT_BLOCKED(0xC0000804L, "An operation failed because the content was blocked."),

	/** The operation could not be completed due to bad clusters on disk. */
	STATUS_BAD_CLUSTERS(0xC0000805L, "The operation could not be completed due to bad clusters on disk."),

	/**
	 * The operation could not be completed because the volume is dirty. Please
	 * run the Chkdsk utility and try again.
	 */
	STATUS_VOLUME_DIRTY(0xC0000806L,
		"The operation could not be completed because the volume is dirty. Please run the Chkdsk utility and try again."),

	/** This file is checked out or locked for editing by another user. */
	STATUS_FILE_CHECKED_OUT(0xC0000901L, "This file is checked out or locked for editing by another user."),

	/** The file must be checked out before saving changes. */
	STATUS_CHECKOUT_REQUIRED(0xC0000902L, "The file must be checked out before saving changes."),

	/** The file type being saved or retrieved has been blocked. */
	STATUS_BAD_FILE_TYPE(0xC0000903L, "The file type being saved or retrieved has been blocked."),

	/** The file size exceeds the limit allowed and cannot be saved. */
	STATUS_FILE_TOO_LARGE(0xC0000904L, "The file size exceeds the limit allowed and cannot be saved."),

	/**
	 * Access Denied. Before opening files in this location, you must first
	 * browse to the e.g. site and select the option to log on automatically.
	 */
	STATUS_FORMS_AUTH_REQUIRED(
		0xC0000905L,
		"Access Denied. Before opening files in this location, you must first " +
		"browse to the e.g. site and select the option to log on automatically."),

	/**
	 * The operation did not complete successfully because the file contains a
	 * virus.
	 */
	STATUS_VIRUS_INFECTED(0xC0000906L, "The operation did not complete successfully because the file contains a virus."),

	/**
	 * This file contains a virus and cannot be opened. Due to the nature of
	 * this virus, the file has been removed from this location.
	 */
	STATUS_VIRUS_DELETED(0xC0000907L,
		"This file contains a virus and cannot be opened. Due to the nature of this virus, the file has been removed from this location."),

	/** The resources required for this device conflict with the MCFG table. */
	STATUS_BAD_MCFG_TABLE(0xC0000908L, "The resources required for this device conflict with the MCFG table."),

	/**
	 * The operation did not complete successfully because it would cause an
	 * oplock to be broken. The caller has requested that existing oplocks not
	 * be broken.
	 */
	STATUS_CANNOT_BREAK_OPLOCK(
		0xC0000909L,
		"The operation did not complete successfully because it would cause an oplock " +
		"to be broken. The caller has requested that existing oplocks not be broken."),

	/** WOW Assertion Error. */
	STATUS_WOW_ASSERTION(0xC0009898L, "WOW Assertion Error."),

	/** The cryptographic signature is invalid. */
	STATUS_INVALID_SIGNATURE(0xC000A000L, "The cryptographic signature is invalid."),

	/** The cryptographic provider does not support HMAC. */
	STATUS_HMAC_NOT_SUPPORTED(0xC000A001L, "The cryptographic provider does not support HMAC."),

	/** The IPsec queue overflowed. */
	STATUS_IPSEC_QUEUE_OVERFLOW(0xC000A010L, "The IPsec queue overflowed."),

	/** The neighbor discovery queue overflowed. */
	STATUS_ND_QUEUE_OVERFLOW(0xC000A011L, "The neighbor discovery queue overflowed."),

	/**
	 * An Internet Control Message Protocol (ICMP) hop limit exceeded error was
	 * received.
	 */
	STATUS_HOPLIMIT_EXCEEDED(0xC000A012L, "An Internet Control Message Protocol (ICMP) hop limit exceeded error was received."),

	/** The protocol is not installed on the local machine. */
	STATUS_PROTOCOL_NOT_SUPPORTED(0xC000A013L, "The protocol is not installed on the local machine."),

	/**
	 * {Delayed Write Failed} Windows was unable to save all the data for the
	 * file %hs; the data has been lost. This error might be caused by network
	 * connectivity issues. Try to save this file elsewhere.
	 */
	STATUS_LOST_WRITEBEHIND_DATA_NETWORK_DISCONNECTED(
		0xC000A080L,
		"{Delayed Write Failed} Windows was unable to save all the data for the file %hs; " +
		"the data has been lost. This error might be caused by network connectivity issues. " +
		"Try to save this file elsewhere."),

	/**
	 * {Delayed Write Failed} Windows was unable to save all the data for the
	 * file %hs; the data has been lost. This error was returned by the server
	 * on which the file exists. Try to save this file elsewhere.
	 */
	STATUS_LOST_WRITEBEHIND_DATA_NETWORK_SERVER_ERROR(
		0xC000A081L,
		"{Delayed Write Failed} Windows was unable to save all the data for the file %hs; the data has been lost. " +
		"This error was returned by the server on which the file exists. Try to save this file elsewhere."),

	/**
	 * {Delayed Write Failed} Windows was unable to save all the data for the
	 * file %hs; the data has been lost. This error might be caused if the
	 * device has been removed or the media is write-protected.
	 */
	STATUS_LOST_WRITEBEHIND_DATA_LOCAL_DISK_ERROR(
		0xC000A082L,
		"{Delayed Write Failed} Windows was unable to save all the data for the file %hs; the data has been lost. " +
		"This error might be caused if the device has been removed or the media is write-protected."),

	/** Windows was unable to parse the requested XML data. */
	STATUS_XML_PARSE_ERROR(0xC000A083L, "Windows was unable to parse the requested XML data."),

	/** An error was encountered while processing an XML digital signature. */
	STATUS_XMLDSIG_ERROR(0xC000A084L, "An error was encountered while processing an XML digital signature."),

	/**
	 * This indicates that the caller made the connection request in the wrong
	 * routing compartment.
	 */
	STATUS_WRONG_COMPARTMENT(0xC000A085L, "This indicates that the caller made the connection request in the wrong routing compartment."),

	/**
	 * This indicates that there was an AuthIP failure when attempting to
	 * connect to the remote host.
	 */
	STATUS_AUTHIP_FAILURE(0xC000A086L, "This indicates that there was an AuthIP failure when attempting to connect to the remote host."),

	/** OID mapped groups cannot have members. */
	STATUS_DS_OID_MAPPED_GROUP_CANT_HAVE_MEMBERS(0xC000A087L, "OID mapped groups cannot have members."),

	/** The specified OID cannot be found. */
	STATUS_DS_OID_NOT_FOUND(0xC000A088L, "The specified OID cannot be found."),

	/**
	 * Hash generation for the specified version and hash type is not enabled on
	 * server.
	 */
	STATUS_HASH_NOT_SUPPORTED(0xC000A100L, "Hash generation for the specified version and hash type is not enabled on server."),

	/**
	 * The hash requests is not present or not up to date with the current file
	 * contents.
	 */
	STATUS_HASH_NOT_PRESENT(0xC000A101L, "The hash requests is not present or not up to date with the current file contents."),

	/**
	 * A file system filter on the server has not opted in for Offload Read
	 * support.
	 */
	STATUS_OFFLOAD_READ_FLT_NOT_SUPPORTED(0xC000A2A1L, "A file system filter on the server has not opted in for Offload Read support."),

	/**
	 * A file system filter on the server has not opted in for Offload Write
	 * support.
	 */
	STATUS_OFFLOAD_WRITE_FLT_NOT_SUPPORTED(0xC000A2A2L, "A file system filter on the server has not opted in for Offload Write support."),

	/**
	 * Offload read operations cannot be performed on:
	 *
	 * <ul>
	 * <li>Compressed files</li>
	 * <li>Sparse files</li>
	 * <li>Encrypted files</li>
	 * <li>File system metadata files</li>
	 * </ul>
	 */
	STATUS_OFFLOAD_READ_FILE_NOT_SUPPORTED(0xC000A2A3L,
		"Offload read operations cannot be performed on: Compressed files, Sparse files, Encrypted files, system metadata files"),

	/**
	 * Offload write operations cannot be performed on:
	 *
	 * <ul>
	 * <li>Compressed files</li>
	 * <li>Sparse files</li>
	 * <li>Encrypted files</li>
	 * <li>File system metadata files</li>
	 * </ul>
	 */
	STATUS_OFFLOAD_WRITE_FILE_NOT_SUPPORTED(0xC000A2A4L,
		"Offload write operations cannot be performed on: Compressed files, Sparse files, Encrypted files, system metadata files"),

	/** The debugger did not perform a state change. */
	DBG_NO_STATE_CHANGE(0xC0010001L, "The debugger did not perform a state change."),

	/** The debugger found that the application is not idle. */
	DBG_APP_NOT_IDLE(0xC0010002L, "The debugger found that the application is not idle."),

	/** The string binding is invalid. */
	RPC_NT_INVALID_STRING_BINDING(0xC0020001L, "The string binding is invalid."),

	/** The binding handle is not the correct type. */
	RPC_NT_WRONG_KIND_OF_BINDING(0xC0020002L, "The binding handle is not the correct type."),

	/** The binding handle is invalid. */
	RPC_NT_INVALID_BINDING(0xC0020003L, "The binding handle is invalid."),

	/** The RPC protocol sequence is not supported. */
	RPC_NT_PROTSEQ_NOT_SUPPORTED(0xC0020004L, "The RPC protocol sequence is not supported."),

	/** The RPC protocol sequence is invalid. */
	RPC_NT_INVALID_RPC_PROTSEQ(0xC0020005L, "The RPC protocol sequence is invalid."),

	/** The string UUID is invalid. */
	RPC_NT_INVALID_STRING_UUID(0xC0020006L, "The string UUID is invalid."),

	/** The endpoint format is invalid. */
	RPC_NT_INVALID_ENDPOINT_FORMAT(0xC0020007L, "The endpoint format is invalid."),

	/** The network address is invalid. */
	RPC_NT_INVALID_NET_ADDR(0xC0020008L, "The network address is invalid."),

	/** No endpoint was found. */
	RPC_NT_NO_ENDPOINT_FOUND(0xC0020009L, "No endpoint was found."),

	/** The time-out value is invalid. */
	RPC_NT_INVALID_TIMEOUT(0xC002000AL, "The time-out value is invalid."),

	/** The object UUID was not found. */
	RPC_NT_OBJECT_NOT_FOUND(0xC002000BL, "The object UUID was not found."),

	/** The object UUID has already been registered. */
	RPC_NT_ALREADY_REGISTERED(0xC002000CL, "The object UUID has already been registered."),

	/** The type UUID has already been registered. */
	RPC_NT_TYPE_ALREADY_REGISTERED(0xC002000DL, "The type UUID has already been registered."),

	/** The RPC server is already listening. */
	RPC_NT_ALREADY_LISTENING(0xC002000EL, "The RPC server is already listening."),

	/** No protocol sequences have been registered. */
	RPC_NT_NO_PROTSEQS_REGISTERED(0xC002000FL, "No protocol sequences have been registered."),

	/** The RPC server is not listening. */
	RPC_NT_NOT_LISTENING(0xC0020010L, "The RPC server is not listening."),

	/** The manager type is unknown. */
	RPC_NT_UNKNOWN_MGR_TYPE(0xC0020011L, "The manager type is unknown."),

	/** The interface is unknown. */
	RPC_NT_UNKNOWN_IF(0xC0020012L, "The interface is unknown."),

	/** There are no bindings. */
	RPC_NT_NO_BINDINGS(0xC0020013L, "There are no bindings."),

	/** There are no protocol sequences. */
	RPC_NT_NO_PROTSEQS(0xC0020014L, "There are no protocol sequences."),

	/** The endpoint cannot be created. */
	RPC_NT_CANT_CREATE_ENDPOINT(0xC0020015L, "The endpoint cannot be created."),

	/** Insufficient resources are available to complete this operation. */
	RPC_NT_OUT_OF_RESOURCES(0xC0020016L, "Insufficient resources are available to complete this operation."),

	/** The RPC server is unavailable. */
	RPC_NT_SERVER_UNAVAILABLE(0xC0020017L, "The RPC server is unavailable."),

	/** The RPC server is too busy to complete this operation. */
	RPC_NT_SERVER_TOO_BUSY(0xC0020018L, "The RPC server is too busy to complete this operation."),

	/** The network options are invalid. */
	RPC_NT_INVALID_NETWORK_OPTIONS(0xC0020019L, "The network options are invalid."),

	/** No RPCs are active on this thread. */
	RPC_NT_NO_CALL_ACTIVE(0xC002001AL, "No RPCs are active on this thread."),

	/** The RPC failed. */
	RPC_NT_CALL_FAILED(0xC002001BL, "The RPC failed."),

	/** The RPC failed and did not execute. */
	RPC_NT_CALL_FAILED_DNE(0xC002001CL, "The RPC failed and did not execute."),

	/** An RPC protocol error occurred. */
	RPC_NT_PROTOCOL_ERROR(0xC002001DL, "An RPC protocol error occurred."),

	/** The RPC server does not support the transfer syntax. */
	RPC_NT_UNSUPPORTED_TRANS_SYN(0xC002001FL, "The RPC server does not support the transfer syntax."),

	/** The type UUID is not supported. */
	RPC_NT_UNSUPPORTED_TYPE(0xC0020021L, "The type UUID is not supported."),

	/** The tag is invalid. */
	RPC_NT_INVALID_TAG(0xC0020022L, "The tag is invalid."),

	/** The array bounds are invalid. */
	RPC_NT_INVALID_BOUND(0xC0020023L, "The array bounds are invalid."),

	/** The binding does not contain an entry name. */
	RPC_NT_NO_ENTRY_NAME(0xC0020024L, "The binding does not contain an entry name."),

	/** The name syntax is invalid. */
	RPC_NT_INVALID_NAME_SYNTAX(0xC0020025L, "The name syntax is invalid."),

	/** The name syntax is not supported. */
	RPC_NT_UNSUPPORTED_NAME_SYNTAX(0xC0020026L, "The name syntax is not supported."),

	/** No network address is available to construct a UUID. */
	RPC_NT_UUID_NO_ADDRESS(0xC0020028L, "No network address is available to construct a UUID."),

	/** The endpoint is a duplicate. */
	RPC_NT_DUPLICATE_ENDPOINT(0xC0020029L, "The endpoint is a duplicate."),

	/** The authentication type is unknown. */
	RPC_NT_UNKNOWN_AUTHN_TYPE(0xC002002AL, "The authentication type is unknown."),

	/** The maximum number of calls is too small. */
	RPC_NT_MAX_CALLS_TOO_SMALL(0xC002002BL, "The maximum number of calls is too small."),

	/** The string is too long. */
	RPC_NT_STRING_TOO_LONG(0xC002002CL, "The string is too long."),

	/** The RPC protocol sequence was not found. */
	RPC_NT_PROTSEQ_NOT_FOUND(0xC002002DL, "The RPC protocol sequence was not found."),

	/** The procedure number is out of range. */
	RPC_NT_PROCNUM_OUT_OF_RANGE(0xC002002EL, "The procedure number is out of range."),

	/** The binding does not contain any authentication information. */
	RPC_NT_BINDING_HAS_NO_AUTH(0xC002002FL, "The binding does not contain any authentication information."),

	/** The authentication service is unknown. */
	RPC_NT_UNKNOWN_AUTHN_SERVICE(0xC0020030L, "The authentication service is unknown."),

	/** The authentication level is unknown. */
	RPC_NT_UNKNOWN_AUTHN_LEVEL(0xC0020031L, "The authentication level is unknown."),

	/** The security context is invalid. */
	RPC_NT_INVALID_AUTH_IDENTITY(0xC0020032L, "The security context is invalid."),

	/** The authorization service is unknown. */
	RPC_NT_UNKNOWN_AUTHZ_SERVICE(0xC0020033L, "The authorization service is unknown."),

	/** The entry is invalid. */
	EPT_NT_INVALID_ENTRY(0xC0020034L, "The entry is invalid."),

	/** The operation cannot be performed. */
	EPT_NT_CANT_PERFORM_OP(0xC0020035L, "The operation cannot be performed."),

	/** No more endpoints are available from the endpoint mapper. */
	EPT_NT_NOT_REGISTERED(0xC0020036L, "No more endpoints are available from the endpoint mapper."),

	/** No interfaces have been exported. */
	RPC_NT_NOTHING_TO_EXPORT(0xC0020037L, "No interfaces have been exported."),

	/** The entry name is incomplete. */
	RPC_NT_INCOMPLETE_NAME(0xC0020038L, "The entry name is incomplete."),

	/** The version option is invalid. */
	RPC_NT_INVALID_VERS_OPTION(0xC0020039L, "The version option is invalid."),

	/** There are no more members. */
	RPC_NT_NO_MORE_MEMBERS(0xC002003AL, "There are no more members."),

	/** There is nothing to unexport. */
	RPC_NT_NOT_ALL_OBJS_UNEXPORTED(0xC002003BL, "There is nothing to unexport."),

	/** The interface was not found. */
	RPC_NT_INTERFACE_NOT_FOUND(0xC002003CL, "The interface was not found."),

	/** The entry already exists. */
	RPC_NT_ENTRY_ALREADY_EXISTS(0xC002003DL, "The entry already exists."),

	/** The entry was not found. */
	RPC_NT_ENTRY_NOT_FOUND(0xC002003EL, "The entry was not found."),

	/** The name service is unavailable. */
	RPC_NT_NAME_SERVICE_UNAVAILABLE(0xC002003FL, "The name service is unavailable."),

	/** The network address family is invalid. */
	RPC_NT_INVALID_NAF_ID(0xC0020040L, "The network address family is invalid."),

	/** The requested operation is not supported. */
	RPC_NT_CANNOT_SUPPORT(0xC0020041L, "The requested operation is not supported."),

	/** No security context is available to allow impersonation. */
	RPC_NT_NO_CONTEXT_AVAILABLE(0xC0020042L, "No security context is available to allow impersonation."),

	/** An internal error occurred in the RPC. */
	RPC_NT_INTERNAL_ERROR(0xC0020043L, "An internal error occurred in the RPC."),

	/** The RPC server attempted to divide an integer by zero. */
	RPC_NT_ZERO_DIVIDE(0xC0020044L, "The RPC server attempted to divide an integer by zero."),

	/** An addressing error occurred in the RPC server. */
	RPC_NT_ADDRESS_ERROR(0xC0020045L, "An addressing error occurred in the RPC server."),

	/** A floating point operation at the RPC server caused a divide by zero. */
	RPC_NT_FP_DIV_ZERO(0xC0020046L, "A floating point operation at the RPC server caused a divide by zero."),

	/** A floating point underflow occurred at the RPC server. */
	RPC_NT_FP_UNDERFLOW(0xC0020047L, "A floating point underflow occurred at the RPC server."),

	/** A floating point overflow occurred at the RPC server. */
	RPC_NT_FP_OVERFLOW(0xC0020048L, "A floating point overflow occurred at the RPC server."),

	/** An RPC is already in progress for this thread. */
	RPC_NT_CALL_IN_PROGRESS(0xC0020049L, "An RPC is already in progress for this thread."),

	/** There are no more bindings. */
	RPC_NT_NO_MORE_BINDINGS(0xC002004AL, "There are no more bindings."),

	/** The group member was not found. */
	RPC_NT_GROUP_MEMBER_NOT_FOUND(0xC002004BL, "The group member was not found."),

	/** The endpoint mapper database entry could not be created. */
	EPT_NT_CANT_CREATE(0xC002004CL, "The endpoint mapper database entry could not be created."),

	/** The object UUID is the nil UUID. */
	RPC_NT_INVALID_OBJECT(0xC002004DL, "The object UUID is the nil UUID."),

	/** No interfaces have been registered. */
	RPC_NT_NO_INTERFACES(0xC002004FL, "No interfaces have been registered."),

	/** The RPC was canceled. */
	RPC_NT_CALL_CANCELLED(0xC0020050L, "The RPC was canceled."),

	/** The binding handle does not contain all the required information. */
	RPC_NT_BINDING_INCOMPLETE(0xC0020051L, "The binding handle does not contain all the required information."),

	/** A communications failure occurred during an RPC. */
	RPC_NT_COMM_FAILURE(0xC0020052L, "A communications failure occurred during an RPC."),

	/** The requested authentication level is not supported. */
	RPC_NT_UNSUPPORTED_AUTHN_LEVEL(0xC0020053L, "The requested authentication level is not supported."),

	/** No principal name was registered. */
	RPC_NT_NO_PRINC_NAME(0xC0020054L, "No principal name was registered."),

	/** The error specified is not a valid Windows RPC error code. */
	RPC_NT_NOT_RPC_ERROR(0xC0020055L, "The error specified is not a valid Windows RPC error code."),

	/** A security package-specific error occurred. */
	RPC_NT_SEC_PKG_ERROR(0xC0020057L, "A security package-specific error occurred."),

	/** The thread was not canceled. */
	RPC_NT_NOT_CANCELLED(0xC0020058L, "The thread was not canceled."),

	/** Invalid asynchronous RPC handle. */
	RPC_NT_INVALID_ASYNC_HANDLE(0xC0020062L, "Invalid asynchronous RPC handle."),

	/** Invalid asynchronous RPC call handle for this operation. */
	RPC_NT_INVALID_ASYNC_CALL(0xC0020063L, "Invalid asynchronous RPC call handle for this operation."),

	/** Access to the HTTP proxy is denied. */
	RPC_NT_PROXY_ACCESS_DENIED(0xC0020064L, "Access to the HTTP proxy is denied."),

	/**
	 * The list of RPC servers available for auto-handle binding has been
	 * exhausted.
	 */
	RPC_NT_NO_MORE_ENTRIES(0xC0030001L, "The list of RPC servers available for auto-handle binding has been exhausted."),

	/** The file designated by DCERPCCHARTRANS cannot be opened. */
	RPC_NT_SS_CHAR_TRANS_OPEN_FAIL(0xC0030002L, "The file designated by DCERPCCHARTRANS cannot be opened."),

	/**
	 * The file containing the character translation table has fewer than 512
	 * bytes.
	 */
	RPC_NT_SS_CHAR_TRANS_SHORT_FILE(0xC0030003L, "The file containing the character translation table has fewer than 512 bytes."),

	/** A null context handle is passed as an [in] parameter. */
	RPC_NT_SS_IN_NULL_CONTEXT(0xC0030004L, "A null context handle is passed as an [in] parameter."),

	/** The context handle does not match any known context handles. */
	RPC_NT_SS_CONTEXT_MISMATCH(0xC0030005L, "The context handle does not match any known context handles."),

	/** The context handle changed during a call. */
	RPC_NT_SS_CONTEXT_DAMAGED(0xC0030006L, "The context handle changed during a call."),

	/** The binding handles passed to an RPC do not match. */
	RPC_NT_SS_HANDLES_MISMATCH(0xC0030007L, "The binding handles passed to an RPC do not match."),

	/** The stub is unable to get the call handle. */
	RPC_NT_SS_CANNOT_GET_CALL_HANDLE(0xC0030008L, "The stub is unable to get the call handle."),

	/** A null reference pointer was passed to the stub. */
	RPC_NT_NULL_REF_POINTER(0xC0030009L, "A null reference pointer was passed to the stub."),

	/** The enumeration value is out of range. */
	RPC_NT_ENUM_VALUE_OUT_OF_RANGE(0xC003000AL, "The enumeration value is out of range."),

	/** The byte count is too small. */
	RPC_NT_BYTE_COUNT_TOO_SMALL(0xC003000BL, "The byte count is too small."),

	/** The stub received bad data. */
	RPC_NT_BAD_STUB_DATA(0xC003000CL, "The stub received bad data."),

	/** Invalid operation on the encoding/decoding handle. */
	RPC_NT_INVALID_ES_ACTION(0xC0030059L, "Invalid operation on the encoding/decoding handle."),

	/** Incompatible version of the serializing package. */
	RPC_NT_WRONG_ES_VERSION(0xC003005AL, "Incompatible version of the serializing package."),

	/** Incompatible version of the RPC stub. */
	RPC_NT_WRONG_STUB_VERSION(0xC003005BL, "Incompatible version of the RPC stub."),

	/** The RPC pipe object is invalid or corrupt. */
	RPC_NT_INVALID_PIPE_OBJECT(0xC003005CL, "The RPC pipe object is invalid or corrupt."),

	/** An invalid operation was attempted on an RPC pipe object. */
	RPC_NT_INVALID_PIPE_OPERATION(0xC003005DL, "An invalid operation was attempted on an RPC pipe object."),

	/** Unsupported RPC pipe version. */
	RPC_NT_WRONG_PIPE_VERSION(0xC003005EL, "Unsupported RPC pipe version."),

	/** The RPC pipe object has already been closed. */
	RPC_NT_PIPE_CLOSED(0xC003005FL, "The RPC pipe object has already been closed."),

	/** The RPC call completed before all pipes were processed. */
	RPC_NT_PIPE_DISCIPLINE_ERROR(0xC0030060L, "The RPC call completed before all pipes were processed."),

	/** No more data is available from the RPC pipe. */
	RPC_NT_PIPE_EMPTY(0xC0030061L, "No more data is available from the RPC pipe."),

	/**
	 * A device is missing in the system BIOS MPS table. This device will not be
	 * used. Contact your system vendor for a system BIOS update.
	 */
	STATUS_PNP_BAD_MPS_TABLE(0xC0040035L,
		"A device is missing in the system BIOS MPS table. This device will " +
		"not be used. Contact your system vendor for a system BIOS update."),

	/** A translator failed to translate resources. */
	STATUS_PNP_TRANSLATION_FAILED(0xC0040036L, "A translator failed to translate resources."),

	/** An IRQ translator failed to translate resources. */
	STATUS_PNP_IRQ_TRANSLATION_FAILED(0xC0040037L, "An IRQ translator failed to translate resources."),

	/** Driver %2 returned an invalid ID for a child device (%3). */
	STATUS_PNP_INVALID_ID(0xC0040038L, "Driver %2 returned an invalid ID for a child device (%3)."),

	/** Reissue the given operation as a cached I/O operation */
	STATUS_IO_REISSUE_AS_CACHED(0xC0040039L, "Reissue the given operation as a cached I/O operation"),

	/** Session name %1 is invalid. */
	STATUS_CTX_WINSTATION_NAME_INVALID(0xC00A0001L, "Session name %1 is invalid."),

	/** The protocol driver %1 is invalid. */
	STATUS_CTX_INVALID_PD(0xC00A0002L, "The protocol driver %1 is invalid."),

	/** The protocol driver %1 was not found in the system path. */
	STATUS_CTX_PD_NOT_FOUND(0xC00A0003L, "The protocol driver %1 was not found in the system path."),

	/** A close operation is pending on the terminal connection. */
	STATUS_CTX_CLOSE_PENDING(0xC00A0006L, "A close operation is pending on the terminal connection."),

	/** No free output buffers are available. */
	STATUS_CTX_NO_OUTBUF(0xC00A0007L, "No free output buffers are available."),

	/** The MODEM.INF file was not found. */
	STATUS_CTX_MODEM_INF_NOT_FOUND(0xC00A0008L, "The MODEM.INF file was not found."),

	/** The modem (%1) was not found in the MODEM.INF file. */
	STATUS_CTX_INVALID_MODEMNAME(0xC00A0009L, "The modem (%1) was not found in the MODEM.INF file."),

	/**
	 * The modem did not accept the command sent to it. Verify that the
	 * configured modem name matches the attached modem.
	 */
	STATUS_CTX_RESPONSE_ERROR(0xC00A000AL,
		"The modem did not accept the command sent to it. Verify that the configured modem name matches the attached modem."),

	/**
	 * The modem did not respond to the command sent to it. Verify that the
	 * modem cable is properly attached and the modem is turned on.
	 */
	STATUS_CTX_MODEM_RESPONSE_TIMEOUT(0xC00A000BL,
		"The modem did not respond to the command sent to it. Verify " +
		"that the modem cable is properly attached and the modem is turned on."),

	/**
	 * Carrier detection has failed or the carrier has been dropped due to
	 * disconnection.
	 */
	STATUS_CTX_MODEM_RESPONSE_NO_CARRIER(0xC00A000CL, "Carrier detection has failed or the carrier has been dropped due to disconnection."),

	/**
	 * A dial tone was not detected within the required time. Verify that the
	 * phone cable is properly attached and functional.
	 */
	STATUS_CTX_MODEM_RESPONSE_NO_DIALTONE(0xC00A000DL,
		"A dial tone was not detected within the required time. Verify that the phone cable is properly attached and functional."),

	/** A busy signal was detected at a remote site on callback. */
	STATUS_CTX_MODEM_RESPONSE_BUSY(0xC00A000EL, "A busy signal was detected at a remote site on callback."),

	/** A voice was detected at a remote site on callback. */
	STATUS_CTX_MODEM_RESPONSE_VOICE(0xC00A000FL, "A voice was detected at a remote site on callback."),

	/** Transport driver error. */
	STATUS_CTX_TD_ERROR(0xC00A0010L, "Transport driver error."),

	/**
	 * The client you are using is not licensed to use this system. Your logon
	 * request is denied.
	 */
	STATUS_CTX_LICENSE_CLIENT_INVALID(0xC00A0012L,
		"The client you are using is not licensed to use this system. Your logon request is denied."),

	/** The system has reached its licensed logon limit. Try again later. */
	STATUS_CTX_LICENSE_NOT_AVAILABLE(0xC00A0013L, "The system has reached its licensed logon limit. Try again later."),

	/** The system license has expired. Your logon request is denied. */
	STATUS_CTX_LICENSE_EXPIRED(0xC00A0014L, "The system license has expired. Your logon request is denied."),

	/** The specified session cannot be found. */
	STATUS_CTX_WINSTATION_NOT_FOUND(0xC00A0015L, "The specified session cannot be found."),

	/** The specified session name is already in use. */
	STATUS_CTX_WINSTATION_NAME_COLLISION(0xC00A0016L, "The specified session name is already in use."),

	/**
	 * The requested operation cannot be completed because the terminal
	 * connection is currently processing a connect, disconnect, reset, or
	 * delete operation.
	 */
	STATUS_CTX_WINSTATION_BUSY(
		0xC00A0017L,
		"The requested operation cannot be completed because the terminal " +
		"connection is currently processing a connect, disconnect, reset, or delete operation."),

	/**
	 * An attempt has been made to connect to a session whose video mode is not
	 * supported by the current client.
	 */
	STATUS_CTX_BAD_VIDEO_MODE(0xC00A0018L,
		"An attempt has been made to connect to a session whose video mode is not supported by the current client."),

	/**
	 * The application attempted to enable DOS graphics mode. DOS graphics mode
	 * is not supported.
	 */
	STATUS_CTX_GRAPHICS_INVALID(0xC00A0022L, "The application attempted to enable DOS graphics mode. DOS graphics mode is not supported."),

	/**
	 * The requested operation can be performed only on the system console. This
	 * is most often the result of a driver or system DLL requiring direct
	 * console access.
	 */
	STATUS_CTX_NOT_CONSOLE(
		0xC00A0024L,
		"The requested operation can be performed only on the system console. " +
		"This is most often the result of a driver or system DLL requiring direct console access."),

	/** The client failed to respond to the server connect message. */
	STATUS_CTX_CLIENT_QUERY_TIMEOUT(0xC00A0026L, "The client failed to respond to the server connect message."),

	/** Disconnecting the console session is not supported. */
	STATUS_CTX_CONSOLE_DISCONNECT(0xC00A0027L, "Disconnecting the console session is not supported."),

	/** Reconnecting a disconnected session to the console is not supported. */
	STATUS_CTX_CONSOLE_CONNECT(0xC00A0028L, "Reconnecting a disconnected session to the console is not supported."),

	/** The request to control another session remotely was denied. */
	STATUS_CTX_SHADOW_DENIED(0xC00A002AL, "The request to control another session remotely was denied."),

	/**
	 * A process has requested access to a session, but has not been granted
	 * those access rights.
	 */
	STATUS_CTX_WINSTATION_ACCESS_DENIED(0xC00A002BL,
		"A process has requested access to a session, but has not been granted those access rights."),

	/** The terminal connection driver %1 is invalid. */
	STATUS_CTX_INVALID_WD(0xC00A002EL, "The terminal connection driver %1 is invalid."),

	/** The terminal connection driver %1 was not found in the system path. */
	STATUS_CTX_WD_NOT_FOUND(0xC00A002FL, "The terminal connection driver %1 was not found in the system path."),

	/**
	 * The requested session cannot be controlled remotely. You cannot control
	 * your own session, a session that is trying to control your session, a
	 * session that has no user logged on, or other sessions from the console.
	 */
	STATUS_CTX_SHADOW_INVALID(
		0xC00A0030L,
		"The requested session cannot be controlled remotely. You cannot control your own session, " +
		"a session that is trying to control your session, a session that has no user logged on, " +
		"or other sessions from the console."),

	/** The requested session is not configured to allow remote control. */
	STATUS_CTX_SHADOW_DISABLED(0xC00A0031L, "The requested session is not configured to allow remote control."),

	/**
	 * The RDP protocol component %2 detected an error in the protocol stream
	 * and has disconnected the client.
	 */
	STATUS_RDP_PROTOCOL_ERROR(0xC00A0032L,
		"The RDP protocol component %2 detected an error in the protocol stream and has disconnected the client."),

	/**
	 * Your request to connect to this terminal server has been rejected. Your
	 * terminal server client license number has not been entered for this copy
	 * of the terminal client. Contact your system administrator for help in
	 * entering a valid, unique license number for this terminal server client.
	 * Click OK to continue.
	 */
	STATUS_CTX_CLIENT_LICENSE_NOT_SET(
		0xC00A0033L,
		"Your request to connect to this terminal server has been rejected. Your terminal server client " +
		"license number has not been entered for this copy of the terminal client. Contact your system " +
		"administrator for help in entering a valid, unique license number for this terminal server " +
		"client. Click OK to continue."),

	/**
	 * Your request to connect to this terminal server has been rejected. Your
	 * terminal server client license number is currently being used by another
	 * user. Contact your system administrator to obtain a new copy of the
	 * terminal server client with a valid, unique license number. Click OK to
	 * continue.
	 */
	STATUS_CTX_CLIENT_LICENSE_IN_USE(
		0xC00A0034L,
		"Your request to connect to this terminal server has been rejected. Your terminal server " +
		"client license number is currently being used by another user. Contact your system administrator " +
		"to obtain a new copy of the terminal server client with a valid, unique license number. Click OK to continue."),

	/**
	 * The remote control of the console was terminated because the display mode
	 * was changed. Changing the display mode in a remote control session is not
	 * supported.
	 */
	STATUS_CTX_SHADOW_ENDED_BY_MODE_CHANGE(
		0xC00A0035L,
		"The remote control of the console was terminated because the display mode was changed. " +
		"Changing the display mode in a remote control session is not supported."),

	/**
	 * Remote control could not be terminated because the specified session is
	 * not currently being remotely controlled.
	 */
	STATUS_CTX_SHADOW_NOT_RUNNING(0xC00A0036L,
		"Remote control could not be terminated because the specified session is not currently being remotely controlled."),

	/**
	 * Your interactive logon privilege has been disabled. Contact your system
	 * administrator.
	 */
	STATUS_CTX_LOGON_DISABLED(0xC00A0037L, "Your interactive logon privilege has been disabled. Contact your system administrator."),

	/**
	 * The terminal server security layer detected an error in the protocol
	 * stream and has disconnected the client.
	 */
	STATUS_CTX_SECURITY_LAYER_ERROR(0xC00A0038L,
		"The terminal server security layer detected an error in the protocol stream and has disconnected the client."),

	/** The target session is incompatible with the current session. */
	STATUS_TS_INCOMPATIBLE_SESSIONS(0xC00A0039L, "The target session is incompatible with the current session."),

	/** The resource loader failed to find an MUI file. */
	STATUS_MUI_FILE_NOT_FOUND(0xC00B0001L, "The resource loader failed to find an MUI file."),

	/**
	 * The resource loader failed to load an MUI file because the file failed to
	 * pass validation.
	 */
	STATUS_MUI_INVALID_FILE(0xC00B0002L, "The resource loader failed to load an MUI file because the file failed to pass validation."),

	/**
	 * The RC manifest is corrupted with garbage data, is an unsupported
	 * version, or is missing a required item.
	 */
	STATUS_MUI_INVALID_RC_CONFIG(0xC00B0003L,
		"The RC manifest is corrupted with garbage data, is an unsupported version, or is missing a required item."),

	/** The RC manifest has an invalid culture name. */
	STATUS_MUI_INVALID_LOCALE_NAME(0xC00B0004L, "The RC manifest has an invalid culture name."),

	/** The RC manifest has and invalid ultimate fallback name. */
	STATUS_MUI_INVALID_ULTIMATEFALLBACK_NAME(0xC00B0005L, "The RC manifest has and invalid ultimate fallback name."),

	/** The resource loader cache does not have a loaded MUI entry. */
	STATUS_MUI_FILE_NOT_LOADED(0xC00B0006L, "The resource loader cache does not have a loaded MUI entry."),

	/** The user stopped resource enumeration. */
	STATUS_RESOURCE_ENUM_USER_STOP(0xC00B0007L, "The user stopped resource enumeration."),

	/** The cluster node is not valid. */
	STATUS_CLUSTER_INVALID_NODE(0xC0130001L, "The cluster node is not valid."),

	/** The cluster node already exists. */
	STATUS_CLUSTER_NODE_EXISTS(0xC0130002L, "The cluster node already exists."),

	/** A node is in the process of joining the cluster. */
	STATUS_CLUSTER_JOIN_IN_PROGRESS(0xC0130003L, "A node is in the process of joining the cluster."),

	/** The cluster node was not found. */
	STATUS_CLUSTER_NODE_NOT_FOUND(0xC0130004L, "The cluster node was not found."),

	/** The cluster local node information was not found. */
	STATUS_CLUSTER_LOCAL_NODE_NOT_FOUND(0xC0130005L, "The cluster local node information was not found."),

	/** The cluster network already exists. */
	STATUS_CLUSTER_NETWORK_EXISTS(0xC0130006L, "The cluster network already exists."),

	/** The cluster network was not found. */
	STATUS_CLUSTER_NETWORK_NOT_FOUND(0xC0130007L, "The cluster network was not found."),

	/** The cluster network interface already exists. */
	STATUS_CLUSTER_NETINTERFACE_EXISTS(0xC0130008L, "The cluster network interface already exists."),

	/** The cluster network interface was not found. */
	STATUS_CLUSTER_NETINTERFACE_NOT_FOUND(0xC0130009L, "The cluster network interface was not found."),

	/** The cluster request is not valid for this object. */
	STATUS_CLUSTER_INVALID_REQUEST(0xC013000AL, "The cluster request is not valid for this object."),

	/** The cluster network provider is not valid. */
	STATUS_CLUSTER_INVALID_NETWORK_PROVIDER(0xC013000BL, "The cluster network provider is not valid."),

	/** The cluster node is down. */
	STATUS_CLUSTER_NODE_DOWN(0xC013000CL, "The cluster node is down."),

	/** The cluster node is not reachable. */
	STATUS_CLUSTER_NODE_UNREACHABLE(0xC013000DL, "The cluster node is not reachable."),

	/** The cluster node is not a member of the cluster. */
	STATUS_CLUSTER_NODE_NOT_MEMBER(0xC013000EL, "The cluster node is not a member of the cluster."),

	/** A cluster join operation is not in progress. */
	STATUS_CLUSTER_JOIN_NOT_IN_PROGRESS(0xC013000FL, "A cluster join operation is not in progress."),

	/** The cluster network is not valid. */
	STATUS_CLUSTER_INVALID_NETWORK(0xC0130010L, "The cluster network is not valid."),

	/** No network adapters are available. */
	STATUS_CLUSTER_NO_NET_ADAPTERS(0xC0130011L, "No network adapters are available."),

	/** The cluster node is up. */
	STATUS_CLUSTER_NODE_UP(0xC0130012L, "The cluster node is up."),

	/** The cluster node is paused. */
	STATUS_CLUSTER_NODE_PAUSED(0xC0130013L, "The cluster node is paused."),

	/** The cluster node is not paused. */
	STATUS_CLUSTER_NODE_NOT_PAUSED(0xC0130014L, "The cluster node is not paused."),

	/** No cluster security context is available. */
	STATUS_CLUSTER_NO_SECURITY_CONTEXT(0xC0130015L, "No cluster security context is available."),

	/**
	 * The cluster network is not configured for internal cluster communication.
	 */
	STATUS_CLUSTER_NETWORK_NOT_INTERNAL(0xC0130016L, "The cluster network is not configured for internal cluster communication."),

	/** The cluster node has been poisoned. */
	STATUS_CLUSTER_POISONED(0xC0130017L, "The cluster node has been poisoned."),

	/** An attempt was made to run an invalid AML opcode. */
	STATUS_ACPI_INVALID_OPCODE(0xC0140001L, "An attempt was made to run an invalid AML opcode."),

	/** The AML interpreter stack has overflowed. */
	STATUS_ACPI_STACK_OVERFLOW(0xC0140002L, "The AML interpreter stack has overflowed."),

	/** An inconsistent state has occurred. */
	STATUS_ACPI_ASSERT_FAILED(0xC0140003L, "An inconsistent state has occurred."),

	/** An attempt was made to access an array outside its bounds. */
	STATUS_ACPI_INVALID_INDEX(0xC0140004L, "An attempt was made to access an array outside its bounds."),

	/** A required argument was not specified. */
	STATUS_ACPI_INVALID_ARGUMENT(0xC0140005L, "A required argument was not specified."),

	/** A fatal error has occurred. */
	STATUS_ACPI_FATAL(0xC0140006L, "A fatal error has occurred."),

	/** An invalid SuperName was specified. */
	STATUS_ACPI_INVALID_SUPERNAME(0xC0140007L, "An invalid SuperName was specified."),

	/** An argument with an incorrect type was specified. */
	STATUS_ACPI_INVALID_ARGTYPE(0xC0140008L, "An argument with an incorrect type was specified."),

	/** An object with an incorrect type was specified. */
	STATUS_ACPI_INVALID_OBJTYPE(0xC0140009L, "An object with an incorrect type was specified."),

	/** A target with an incorrect type was specified. */
	STATUS_ACPI_INVALID_TARGETTYPE(0xC014000AL, "A target with an incorrect type was specified."),

	/** An incorrect number of arguments was specified. */
	STATUS_ACPI_INCORRECT_ARGUMENT_COUNT(0xC014000BL, "An incorrect number of arguments was specified."),

	/** An address failed to translate. */
	STATUS_ACPI_ADDRESS_NOT_MAPPED(0xC014000CL, "An address failed to translate."),

	/** An incorrect event type was specified. */
	STATUS_ACPI_INVALID_EVENTTYPE(0xC014000DL, "An incorrect event type was specified."),

	/** A handler for the target already exists. */
	STATUS_ACPI_HANDLER_COLLISION(0xC014000EL, "A handler for the target already exists."),

	/** Invalid data for the target was specified. */
	STATUS_ACPI_INVALID_DATA(0xC014000FL, "Invalid data for the target was specified."),

	/** An invalid region for the target was specified. */
	STATUS_ACPI_INVALID_REGION(0xC0140010L, "An invalid region for the target was specified."),

	/** An attempt was made to access a field outside the defined range. */
	STATUS_ACPI_INVALID_ACCESS_SIZE(0xC0140011L, "An attempt was made to access a field outside the defined range."),

	/** The global system lock could not be acquired. */
	STATUS_ACPI_ACQUIRE_GLOBAL_LOCK(0xC0140012L, "The global system lock could not be acquired."),

	/** An attempt was made to reinitialize the ACPI subsystem. */
	STATUS_ACPI_ALREADY_INITIALIZED(0xC0140013L, "An attempt was made to reinitialize the ACPI subsystem."),

	/** The ACPI subsystem has not been initialized. */
	STATUS_ACPI_NOT_INITIALIZED(0xC0140014L, "The ACPI subsystem has not been initialized."),

	/** An incorrect mutex was specified. */
	STATUS_ACPI_INVALID_MUTEX_LEVEL(0xC0140015L, "An incorrect mutex was specified."),

	/** The mutex is not currently owned. */
	STATUS_ACPI_MUTEX_NOT_OWNED(0xC0140016L, "The mutex is not currently owned."),

	/**
	 * An attempt was made to access the mutex by a process that was not the
	 * owner.
	 */
	STATUS_ACPI_MUTEX_NOT_OWNER(0xC0140017L, "An attempt was made to access the mutex by a process that was not the owner."),

	/** An error occurred during an access to region space. */
	STATUS_ACPI_RS_ACCESS(0xC0140018L, "An error occurred during an access to region space."),

	/** An attempt was made to use an incorrect table. */
	STATUS_ACPI_INVALID_TABLE(0xC0140019L, "An attempt was made to use an incorrect table."),

	/** The registration of an ACPI event failed. */
	STATUS_ACPI_REG_HANDLER_FAILED(0xC0140020L, "The registration of an ACPI event failed."),

	/** An ACPI power object failed to transition state. */
	STATUS_ACPI_POWER_REQUEST_FAILED(0xC0140021L, "An ACPI power object failed to transition state."),

	/** The requested section is not present in the activation context. */
	STATUS_SXS_SECTION_NOT_FOUND(0xC0150001L, "The requested section is not present in the activation context."),

	/**
	 * Windows was unable to process the application binding information. Refer
	 * to the system event log for further information.
	 */
	STATUS_SXS_CANT_GEN_ACTCTX(0xC0150002L,
		"Windows was unable to process the application binding information. Refer to the system event log for further information."),

	/** The application binding data format is invalid. */
	STATUS_SXS_INVALID_ACTCTXDATA_FORMAT(0xC0150003L, "The application binding data format is invalid."),

	/** The referenced assembly is not installed on the system. */
	STATUS_SXS_ASSEMBLY_NOT_FOUND(0xC0150004L, "The referenced assembly is not installed on the system."),

	/**
	 * The manifest file does not begin with the required tag and format
	 * information.
	 */
	STATUS_SXS_MANIFEST_FORMAT_ERROR(0xC0150005L, "The manifest file does not begin with the required tag and format information."),

	/** The manifest file contains one or more syntax errors. */
	STATUS_SXS_MANIFEST_PARSE_ERROR(0xC0150006L, "The manifest file contains one or more syntax errors."),

	/** The application attempted to activate a disabled activation context. */
	STATUS_SXS_ACTIVATION_CONTEXT_DISABLED(0xC0150007L, "The application attempted to activate a disabled activation context."),

	/** The requested lookup key was not found in any active activation context. */
	STATUS_SXS_KEY_NOT_FOUND(0xC0150008L, "The requested lookup key was not found in any active activation context."),

	/**
	 * A component version required by the application conflicts with another
	 * component version that is already active.
	 */
	STATUS_SXS_VERSION_CONFLICT(0xC0150009L,
		"A component version required by the application conflicts with another component version that is already active."),

	/**
	 * The type requested activation context section does not match the query
	 * API used.
	 */
	STATUS_SXS_WRONG_SECTION_TYPE(0xC015000AL, "The type requested activation context section does not match the query API used."),

	/**
	 * Lack of system resources has required isolated activation to be disabled
	 * for the current thread of execution.
	 */
	STATUS_SXS_THREAD_QUERIES_DISABLED(0xC015000BL,
		"Lack of system resources has required isolated activation to be disabled for the current thread of execution."),

	/** The referenced assembly could not be found. */
	STATUS_SXS_ASSEMBLY_MISSING(0xC015000CL, "The referenced assembly could not be found."),

	/**
	 * An attempt to set the process default activation context failed because
	 * the process default activation context was already set.
	 */
	STATUS_SXS_PROCESS_DEFAULT_ALREADY_SET(0xC015000EL,
		"An attempt to set the process default activation context failed because the process default activation context was already set."),

	/**
	 * The activation context being deactivated is not the most recently
	 * activated one.
	 */
	STATUS_SXS_EARLY_DEACTIVATION(0xC015000FL, "The activation context being deactivated is not the most recently activated one."),

	/**
	 * The activation context being deactivated is not active for the current
	 * thread of execution.
	 */
	STATUS_SXS_INVALID_DEACTIVATION(0xC0150010L,
		"The activation context being deactivated is not active for the current thread of execution."),

	/** The activation context being deactivated has already been deactivated. */
	STATUS_SXS_MULTIPLE_DEACTIVATION(0xC0150011L, "The activation context being deactivated has already been deactivated."),

	/**
	 * The activation context of the system default assembly could not be
	 * generated.
	 */
	STATUS_SXS_SYSTEM_DEFAULT_ACTIVATION_CONTEXT_EMPTY(0xC0150012L,
		"The activation context of the system default assembly could not be generated."),

	/**
	 * A component used by the isolation facility has requested that the process
	 * be terminated.
	 */
	STATUS_SXS_PROCESS_TERMINATION_REQUESTED(0xC0150013L,
		"A component used by the isolation facility has requested that the process be terminated."),

	/**
	 * The activation context activation stack for the running thread of
	 * execution is corrupt.
	 */
	STATUS_SXS_CORRUPT_ACTIVATION_STACK(0xC0150014L,
		"The activation context activation stack for the running thread of execution is corrupt."),

	/**
	 * The application isolation metadata for this process or thread has become
	 * corrupt.
	 */
	STATUS_SXS_CORRUPTION(0xC0150015L, "The application isolation metadata for this process or thread has become corrupt."),

	/** The value of an attribute in an identity is not within the legal range. */
	STATUS_SXS_INVALID_IDENTITY_ATTRIBUTE_VALUE(0xC0150016L, "The value of an attribute in an identity is not within the legal range."),

	/** The name of an attribute in an identity is not within the legal range. */
	STATUS_SXS_INVALID_IDENTITY_ATTRIBUTE_NAME(0xC0150017L, "The name of an attribute in an identity is not within the legal range."),

	/** An identity contains two definitions for the same attribute. */
	STATUS_SXS_IDENTITY_DUPLICATE_ATTRIBUTE(0xC0150018L, "An identity contains two definitions for the same attribute."),

	/**
	 * The identity string is malformed. This might be due to a trailing comma,
	 * more than two unnamed attributes, a missing attribute name, or a missing
	 * attribute value.
	 */
	STATUS_SXS_IDENTITY_PARSE_ERROR(
		0xC0150019L,
		"The identity string is malformed. This might be due to a trailing comma, more than two " +
		"unnamed attributes, a missing attribute name, or a missing attribute value."),

	/** The component store has become corrupted. */
	STATUS_SXS_COMPONENT_STORE_CORRUPT(0xC015001AL, "The component store has become corrupted."),

	/**
	 * A component's file does not match the verification information present in
	 * the component manifest.
	 */
	STATUS_SXS_FILE_HASH_MISMATCH(0xC015001BL,
		"A component's file does not match the verification information present in the component manifest."),

	/**
	 * The identities of the manifests are identical, but their contents are
	 * different.
	 */
	STATUS_SXS_MANIFEST_IDENTITY_SAME_BUT_CONTENTS_DIFFERENT(0xC015001CL,
		"The identities of the manifests are identical, but their contents are different."),

	/** The component identities are different. */
	STATUS_SXS_IDENTITIES_DIFFERENT(0xC015001DL, "The component identities are different."),

	/** The assembly is not a deployment. */
	STATUS_SXS_ASSEMBLY_IS_NOT_A_DEPLOYMENT(0xC015001EL, "The assembly is not a deployment."),

	/** The file is not a part of the assembly. */
	STATUS_SXS_FILE_NOT_PART_OF_ASSEMBLY(0xC015001FL, "The file is not a part of the assembly."),

	/** An advanced installer failed during setup or servicing. */
	STATUS_ADVANCED_INSTALLER_FAILED(0xC0150020L, "An advanced installer failed during setup or servicing."),

	/**
	 * The character encoding in the XML declaration did not match the encoding
	 * used in the document.
	 */
	STATUS_XML_ENCODING_MISMATCH(0xC0150021L,
		"The character encoding in the XML declaration did not match the encoding used in the document."),

	/** The size of the manifest exceeds the maximum allowed. */
	STATUS_SXS_MANIFEST_TOO_BIG(0xC0150022L, "The size of the manifest exceeds the maximum allowed."),

	/** The setting is not registered. */
	STATUS_SXS_SETTING_NOT_REGISTERED(0xC0150023L, "The setting is not registered."),

	/** One or more required transaction members are not present. */
	STATUS_SXS_TRANSACTION_CLOSURE_INCOMPLETE(0xC0150024L, "One or more required transaction members are not present."),

	/** The SMI primitive installer failed during setup or servicing. */
	STATUS_SMI_PRIMITIVE_INSTALLER_FAILED(0xC0150025L, "The SMI primitive installer failed during setup or servicing."),

	/** A generic command executable returned a result that indicates failure. */
	STATUS_GENERIC_COMMAND_FAILED(0xC0150026L, "A generic command executable returned a result that indicates failure."),

	/** A component is missing file verification information in its manifest. */
	STATUS_SXS_FILE_HASH_MISSING(0xC0150027L, "A component is missing file verification information in its manifest."),

	/**
	 * The function attempted to use a name that is reserved for use by another
	 * transaction.
	 */
	STATUS_TRANSACTIONAL_CONFLICT(0xC0190001L, "The function attempted to use a name that is reserved for use by another transaction."),

	/** The transaction handle associated with this operation is invalid. */
	STATUS_INVALID_TRANSACTION(0xC0190002L, "The transaction handle associated with this operation is invalid."),

	/**
	 * The requested operation was made in the context of a transaction that is
	 * no longer active.
	 */
	STATUS_TRANSACTION_NOT_ACTIVE(0xC0190003L, "The requested operation was made in the context of a transaction that is no longer active."),

	/**
	 * The transaction manager was unable to be successfully initialized.
	 * Transacted operations are not supported.
	 */
	STATUS_TM_INITIALIZATION_FAILED(0xC0190004L,
		"The transaction manager was unable to be successfully initialized. Transacted operations are not supported."),

	/**
	 * Transaction support within the specified file system resource manager was
	 * not started or was shut down due to an error.
	 */
	STATUS_RM_NOT_ACTIVE(0xC0190005L,
		"Transaction support within the specified file system resource manager was not started or was shut down due to an error."),

	/**
	 * The metadata of the resource manager has been corrupted. The resource
	 * manager will not function.
	 */
	STATUS_RM_METADATA_CORRUPT(0xC0190006L,
		"The metadata of the resource manager has been corrupted. The resource manager will not function."),

	/**
	 * The resource manager attempted to prepare a transaction that it has not
	 * successfully joined.
	 */
	STATUS_TRANSACTION_NOT_JOINED(0xC0190007L,
		"The resource manager attempted to prepare a transaction that it has not successfully joined."),

	/** The specified directory does not contain a file system resource manager. */
	STATUS_DIRECTORY_NOT_RM(0xC0190008L, "The specified directory does not contain a file system resource manager."),

	/** The remote server or share does not support transacted file operations. */
	STATUS_TRANSACTIONS_UNSUPPORTED_REMOTE(0xC019000AL, "The remote server or share does not support transacted file operations."),

	/** The requested log size for the file system resource manager is invalid. */
	STATUS_LOG_RESIZE_INVALID_SIZE(0xC019000BL, "The requested log size for the file system resource manager is invalid."),

	/**
	 * The remote server sent mismatching version number or Fid for a file
	 * opened with transactions.
	 */
	STATUS_REMOTE_FILE_VERSION_MISMATCH(0xC019000CL,
		"The remote server sent mismatching version number or Fid for a file opened with transactions."),

	/** The resource manager tried to register a protocol that already exists. */
	STATUS_CRM_PROTOCOL_ALREADY_EXISTS(0xC019000FL, "The resource manager tried to register a protocol that already exists."),

	/** The attempt to propagate the transaction failed. */
	STATUS_TRANSACTION_PROPAGATION_FAILED(0xC0190010L, "The attempt to propagate the transaction failed."),

	/** The requested propagation protocol was not registered as a CRM. */
	STATUS_CRM_PROTOCOL_NOT_FOUND(0xC0190011L, "The requested propagation protocol was not registered as a CRM."),

	/**
	 * The transaction object already has a superior enlistment, and the caller
	 * attempted an operation that would have created a new superior. Only a
	 * single superior enlistment is allowed.
	 */
	STATUS_TRANSACTION_SUPERIOR_EXISTS(
		0xC0190012L,
		"The transaction object already has a superior enlistment, and the caller attempted an " +
		"operation that would have created a new superior. Only a single superior enlistment is allowed."),

	/**
	 * The requested operation is not valid on the transaction object in its
	 * current state.
	 */
	STATUS_TRANSACTION_REQUEST_NOT_VALID(0xC0190013L, "The requested operation is " +
		"not valid on the transaction object in its current state."),

	/**
	 * The caller has called a response API, but the response is not expected
	 * because the transaction manager did not issue the corresponding request
	 * to the caller.
	 */
	STATUS_TRANSACTION_NOT_REQUESTED(
		0xC0190014L,
		"The caller has called a response API, but the response is not expected because the transaction " +
		"manager did not issue the corresponding request to the caller."),

	/**
	 * It is too late to perform the requested operation, because the
	 * transaction has already been aborted.
	 */
	STATUS_TRANSACTION_ALREADY_ABORTED(0xC0190015L,
		"It is too late to perform the requested operation, because the transaction has already been aborted."),

	/**
	 * It is too late to perform the requested operation, because the
	 * transaction has already been committed.
	 */
	STATUS_TRANSACTION_ALREADY_COMMITTED(0xC0190016L,
		"It is too late to perform the requested operation, because the transaction has already been committed."),

	/**
	 * The buffer passed in to NtPushTransaction or NtPullTransaction is not in
	 * a valid format.
	 */
	STATUS_TRANSACTION_INVALID_MARSHALL_BUFFER(0xC0190017L,
		"The buffer passed in to NtPushTransaction or NtPullTransaction is not in a valid format."),

	/**
	 * The current transaction context associated with the thread is not a valid
	 * handle to a transaction object.
	 */
	STATUS_CURRENT_TRANSACTION_NOT_VALID(0xC0190018L,
		"The current transaction context associated with the thread is not a valid handle to a transaction object."),

	/**
	 * An attempt to create space in the transactional resource manager's log
	 * failed. The failure status has been recorded in the event log.
	 */
	STATUS_LOG_GROWTH_FAILED(0xC0190019L,
		"An attempt to create space in the transactional resource manager's log failed. " +
		"The failure status has been recorded in the event log."),

	/**
	 * The object (file, stream, or link) that corresponds to the handle has
	 * been deleted by a transaction savepoint rollback.
	 */
	STATUS_OBJECT_NO_LONGER_EXISTS(0xC0190021L,
		"The object (file, stream, or link) that corresponds to the handle has been deleted by a transaction savepoint rollback."),

	/**
	 * The specified file miniversion was not found for this transacted file
	 * open.
	 */
	STATUS_STREAM_MINIVERSION_NOT_FOUND(0xC0190022L, "The specified file miniversion was not found for this transacted file open."),

	/**
	 * The specified file miniversion was found but has been invalidated. The
	 * most likely cause is a transaction savepoint rollback.
	 */
	STATUS_STREAM_MINIVERSION_NOT_VALID(0xC0190023L,
		"The specified file miniversion was found but has been invalidated. The most likely cause is a transaction savepoint rollback."),

	/**
	 * A miniversion can be opened only in the context of the transaction that
	 * created it.
	 */
	STATUS_MINIVERSION_INACCESSIBLE_FROM_SPECIFIED_TRANSACTION(0xC0190024L,
		"A miniversion can be opened only in the context of the transaction that created it."),

	/** It is not possible to open a miniversion with modify access. */
	STATUS_CANT_OPEN_MINIVERSION_WITH_MODIFY_INTENT(0xC0190025L, "It is not possible to open a miniversion with modify access."),

	/** It is not possible to create any more miniversions for this stream. */
	STATUS_CANT_CREATE_MORE_STREAM_MINIVERSIONS(0xC0190026L, "It is not possible to create any more miniversions for this stream."),

	/**
	 * The handle has been invalidated by a transaction. The most likely cause
	 * is the presence of memory mapping on a file or an open handle when the
	 * transaction ended or rolled back to savepoint.
	 */
	STATUS_HANDLE_NO_LONGER_VALID(
		0xC0190028L,
		"The handle has been invalidated by a transaction. The most likely cause is the presence of " +
		"memory mapping on a file or an open handle when the transaction ended or rolled back to savepoint."),

	/** The log data is corrupt. */
	STATUS_LOG_CORRUPTION_DETECTED(0xC0190030L, "The log data is corrupt."),

	/**
	 * The transaction outcome is unavailable because the resource manager
	 * responsible for it is disconnected.
	 */
	STATUS_RM_DISCONNECTED(0xC0190032L,
		"The transaction outcome is unavailable because the resource manager responsible for it is disconnected."),

	/**
	 * The request was rejected because the enlistment in question is not a
	 * superior enlistment.
	 */
	STATUS_ENLISTMENT_NOT_SUPERIOR(0xC0190033L, "The request was rejected because the enlistment in question is not a superior enlistment."),

	/**
	 * The file cannot be opened in a transaction because its identity depends
	 * on the outcome of an unresolved transaction.
	 */
	STATUS_FILE_IDENTITY_NOT_PERSISTENT(0xC0190036L,
		"The file cannot be opened in a transaction because its identity depends on the outcome of an unresolved transaction."),

	/**
	 * The operation cannot be performed because another transaction is
	 * depending on this property not changing.
	 */
	STATUS_CANT_BREAK_TRANSACTIONAL_DEPENDENCY(0xC0190037L,
		"The operation cannot be performed because another transaction is depending on this property not changing."),

	/**
	 * The operation would involve a single file with two transactional resource
	 * managers and is, therefore, not allowed.
	 */
	STATUS_CANT_CROSS_RM_BOUNDARY(0xC0190038L,
		"The operation would involve a single file with two transactional resource managers and is, therefore, not allowed."),

	/** The $Txf directory must be empty for this operation to succeed. */
	STATUS_TXF_DIR_NOT_EMPTY(0xC0190039L, "The $Txf directory must be empty for this operation to succeed."),

	/**
	 * The operation would leave a transactional resource manager in an
	 * inconsistent state and is therefore not allowed.
	 */
	STATUS_INDOUBT_TRANSACTIONS_EXIST(0xC019003AL,
		"The operation would leave a transactional resource manager in an inconsistent state and is therefore not allowed."),

	/**
	 * The operation could not be completed because the transaction manager does
	 * not have a log.
	 */
	STATUS_TM_VOLATILE(0xC019003BL, "The operation could not be completed because the transaction manager does not have a log."),

	/**
	 * A rollback could not be scheduled because a previously scheduled rollback
	 * has already executed or been queued for execution.
	 */
	STATUS_ROLLBACK_TIMER_EXPIRED(0xC019003CL,
		"A rollback could not be scheduled because a previously scheduled rollback has already executed or been queued for execution."),

	/**
	 * The transactional metadata attribute on the file or directory %hs is
	 * corrupt and unreadable.
	 */
	STATUS_TXF_ATTRIBUTE_CORRUPT(0xC019003DL, "The transactional metadata " +
		"attribute on the file or directory %hs is corrupt and unreadable."),

	/**
	 * The encryption operation could not be completed because a transaction is
	 * active.
	 */
	STATUS_EFS_NOT_ALLOWED_IN_TRANSACTION(0xC019003EL, "The encryption operation could not be completed because a transaction is active."),

	/** This object is not allowed to be opened in a transaction. */
	STATUS_TRANSACTIONAL_OPEN_NOT_ALLOWED(0xC019003FL, "This object is not allowed to be opened in a transaction."),

	/**
	 * Memory mapping (creating a mapped section) a remote file under a
	 * transaction is not supported.
	 */
	STATUS_TRANSACTED_MAPPING_UNSUPPORTED_REMOTE(0xC0190040L,
		"Memory mapping (creating a mapped section) a remote file under a transaction is not supported."),

	/**
	 * Promotion was required to allow the resource manager to enlist, but the
	 * transaction was set to disallow it.
	 */
	STATUS_TRANSACTION_REQUIRED_PROMOTION(0xC0190043L,
		"Promotion was required to allow the resource manager to enlist, but the transaction was set to disallow it."),

	/**
	 * This file is open for modification in an unresolved transaction and can
	 * be opened for execute only by a transacted reader.
	 */
	STATUS_CANNOT_EXECUTE_FILE_IN_TRANSACTION(0xC0190044L,
		"This file is open for modification in an unresolved transaction and can be opened for execute only by a transacted reader."),

	/**
	 * The request to thaw frozen transactions was ignored because transactions
	 * were not previously frozen.
	 */
	STATUS_TRANSACTIONS_NOT_FROZEN(0xC0190045L,
		"The request to thaw frozen transactions was ignored because transactions were not previously frozen."),

	/** Transactions cannot be frozen because a freeze is already in progress. */
	STATUS_TRANSACTION_FREEZE_IN_PROGRESS(0xC0190046L, "Transactions cannot be frozen because a freeze is already in progress."),

	/**
	 * The target volume is not a snapshot volume. This operation is valid only
	 * on a volume mounted as a snapshot.
	 */
	STATUS_NOT_SNAPSHOT_VOLUME(0xC0190047L,
		"The target volume is not a snapshot volume. This operation is valid only on a volume mounted as a snapshot."),

	/**
	 * The savepoint operation failed because files are open on the transaction,
	 * which is not permitted.
	 */
	STATUS_NO_SAVEPOINT_WITH_OPEN_FILES(0xC0190048L,
		"The savepoint operation failed because files are open on the transaction, which is not permitted."),

	/**
	 * The sparse operation could not be completed because a transaction is
	 * active on the file.
	 */
	STATUS_SPARSE_NOT_ALLOWED_IN_TRANSACTION(0xC0190049L,
		"The sparse operation could not be completed because a transaction is active on the file."),

	/**
	 * The call to create a transaction manager object failed because the Tm
	 * Identity that is stored in the log file does not match the Tm Identity
	 * that was passed in as an argument.
	 */
	STATUS_TM_IDENTITY_MISMATCH(
		0xC019004AL,
		"The call to create a transaction manager object failed because the Tm Identity that is stored " +
		"in the log file does not match the Tm Identity that was passed in as an argument."),

	/**
	 * I/O was attempted on a section object that has been floated as a result
	 * of a transaction ending. There is no valid data.
	 */
	STATUS_FLOATED_SECTION(0xC019004BL,
		"I/O was attempted on a section object that has been floated as a result of a transaction ending. There is no valid data."),

	/**
	 * The transactional resource manager cannot currently accept transacted
	 * work due to a transient condition, such as low resources.
	 */
	STATUS_CANNOT_ACCEPT_TRANSACTED_WORK(0xC019004CL,
		"The transactional resource manager cannot currently accept transacted work due to a transient condition, such as low resources."),

	/**
	 * The transactional resource manager had too many transactions outstanding
	 * that could not be aborted. The transactional resource manager has been
	 * shut down.
	 */
	STATUS_CANNOT_ABORT_TRANSACTIONS(
		0xC019004DL,
		"The transactional resource manager had too many transactions outstanding that could " +
		"not be aborted. The transactional resource manager has been shut down."),

	/**
	 * The specified transaction was unable to be opened because it was not
	 * found.
	 */
	STATUS_TRANSACTION_NOT_FOUND(0xC019004EL, "The specified transaction was unable to be opened because it was not found."),

	/**
	 * The specified resource manager was unable to be opened because it was not
	 * found.
	 */
	STATUS_RESOURCEMANAGER_NOT_FOUND(0xC019004FL, "The specified resource manager was unable to be opened because it was not found."),

	/**
	 * The specified enlistment was unable to be opened because it was not
	 * found.
	 */
	STATUS_ENLISTMENT_NOT_FOUND(0xC0190050L, "The specified enlistment was unable to be opened because it was not found."),

	/**
	 * The specified transaction manager was unable to be opened because it was
	 * not found.
	 */
	STATUS_TRANSACTIONMANAGER_NOT_FOUND(0xC0190051L, "The specified transaction manager was unable to be opened because it was not found."),

	/**
	 * The specified resource manager was unable to create an enlistment because
	 * its associated transaction manager is not online.
	 */
	STATUS_TRANSACTIONMANAGER_NOT_ONLINE(0xC0190052L,
		"The specified resource manager was unable to create an enlistment because its associated transaction manager is not online."),

	/**
	 * The specified transaction manager was unable to create the objects
	 * contained in its log file in the Ob namespace. Therefore, the transaction
	 * manager was unable to recover.
	 */
	STATUS_TRANSACTIONMANAGER_RECOVERY_NAME_COLLISION(
		0xC0190053L,
		"The specified transaction manager was unable to create the objects contained in its log " +
		"file in the Ob namespace. Therefore, the transaction manager was unable to recover."),

	/**
	 * The call to create a superior enlistment on this transaction object could
	 * not be completed because the transaction object specified for the
	 * enlistment is a subordinate branch of the transaction. Only the root of
	 * the transaction can be enlisted as a superior.
	 */
	STATUS_TRANSACTION_NOT_ROOT(
		0xC0190054L,
		"The call to create a superior enlistment on this transaction object could not be completed " +
		"because the transaction object specified for the enlistment is a subordinate branch of the " +
		"transaction. Only the root of the transaction can be enlisted as a superior."),

	/**
	 * Because the associated transaction manager or resource manager has been
	 * closed, the handle is no longer valid.
	 */
	STATUS_TRANSACTION_OBJECT_EXPIRED(0xC0190055L,
		"Because the associated transaction manager or resource manager has been closed, the handle is no longer valid."),

	/**
	 * The compression operation could not be completed because a transaction is
	 * active on the file.
	 */
	STATUS_COMPRESSION_NOT_ALLOWED_IN_TRANSACTION(0xC0190056L,
		"The compression operation could not be completed because a transaction is active on the file."),

	/**
	 * The specified operation could not be performed on this superior
	 * enlistment because the enlistment was not created with the corresponding
	 * completion response in the NotificationMask.
	 */
	STATUS_TRANSACTION_RESPONSE_NOT_ENLISTED(
		0xC0190057L,
		"The specified operation could not be performed on this superior enlistment because the " +
		"enlistment was not created with the corresponding completion response in the NotificationMask."),

	/**
	 * The specified operation could not be performed because the record to be
	 * logged was too long. This can occur because either there are too many
	 * enlistments on this transaction or the combined RecoveryInformation being
	 * logged on behalf of those enlistments is too long.
	 */
	STATUS_TRANSACTION_RECORD_TOO_LONG(
		0xC0190058L,
		"The specified operation could not be performed because the record to be logged was too " +
		"long. This can occur because either there are too many enlistments on this transaction or " +
		"the combined RecoveryInformation being logged on behalf of those enlistments is too long."),

	/**
	 * The link-tracking operation could not be completed because a transaction
	 * is active.
	 */
	STATUS_NO_LINK_TRACKING_IN_TRANSACTION(0xC0190059L,
		"The link-tracking operation could not be completed because a transaction is active."),

	/** This operation cannot be performed in a transaction. */
	STATUS_OPERATION_NOT_SUPPORTED_IN_TRANSACTION(0xC019005AL, "This operation cannot be performed in a transaction."),

	/**
	 * The kernel transaction manager had to abort or forget the transaction
	 * because it blocked forward progress.
	 */
	STATUS_TRANSACTION_INTEGRITY_VIOLATED(0xC019005BL,
		"The kernel transaction manager had to abort or forget the transaction because it blocked forward progress."),

	/**
	 * The handle is no longer properly associated with its transaction. It
	 * might have been opened in a transactional resource manager that was
	 * subsequently forced to restart. Please close the handle and open a new
	 * one.
	 */
	STATUS_EXPIRED_HANDLE(
		0xC0190060L,
		"The handle is no longer properly associated with its transaction. It might have been " +
		"opened in a transactional resource manager that was subsequently forced to restart. " +
		"Please close the handle and open a new one."),

	/**
	 * The specified operation could not be performed because the resource
	 * manager is not enlisted in the transaction.
	 */
	STATUS_TRANSACTION_NOT_ENLISTED(0xC0190061L,
		"The specified operation could not be performed because the resource manager is not enlisted in the transaction."),

	/** The log service found an invalid log sector. */
	STATUS_LOG_SECTOR_INVALID(0xC01A0001L, "The log service found an invalid log sector."),

	/** The log service encountered a log sector with invalid block parity. */
	STATUS_LOG_SECTOR_PARITY_INVALID(0xC01A0002L, "The log service encountered a log sector with invalid block parity."),

	/** The log service encountered a remapped log sector. */
	STATUS_LOG_SECTOR_REMAPPED(0xC01A0003L, "The log service encountered a remapped log sector."),

	/** The log service encountered a partial or incomplete log block. */
	STATUS_LOG_BLOCK_INCOMPLETE(0xC01A0004L, "The log service encountered a partial or incomplete log block."),

	/**
	 * The log service encountered an attempt to access data outside the active
	 * log range.
	 */
	STATUS_LOG_INVALID_RANGE(0xC01A0005L, "The log service encountered an attempt to access data outside the active log range."),

	/** The log service user-log marshaling buffers are exhausted. */
	STATUS_LOG_BLOCKS_EXHAUSTED(0xC01A0006L, "The log service user-log marshaling buffers are exhausted."),

	/**
	 * The log service encountered an attempt to read from a marshaling area
	 * with an invalid read context.
	 */
	STATUS_LOG_READ_CONTEXT_INVALID(0xC01A0007L,
		"The log service encountered an attempt to read from a marshaling area with an invalid read context."),

	/** The log service encountered an invalid log restart area. */
	STATUS_LOG_RESTART_INVALID(0xC01A0008L, "The log service encountered an invalid log restart area."),

	/** The log service encountered an invalid log block version. */
	STATUS_LOG_BLOCK_VERSION(0xC01A0009L, "The log service encountered an invalid log block version."),

	/** The log service encountered an invalid log block. */
	STATUS_LOG_BLOCK_INVALID(0xC01A000AL, "The log service encountered an invalid log block."),

	/**
	 * The log service encountered an attempt to read the log with an invalid
	 * read mode.
	 */
	STATUS_LOG_READ_MODE_INVALID(0xC01A000BL, "The log service encountered an attempt to read the log with an invalid read mode."),

	/** The log service encountered a corrupted metadata file. */
	STATUS_LOG_METADATA_CORRUPT(0xC01A000DL, "The log service encountered a corrupted metadata file."),

	/**
	 * The log service encountered a metadata file that could not be created by
	 * the log file system.
	 */
	STATUS_LOG_METADATA_INVALID(0xC01A000EL, "The log service encountered a " +
		"metadata file that could not be created by the log file system."),

	/** The log service encountered a metadata file with inconsistent data. */
	STATUS_LOG_METADATA_INCONSISTENT(0xC01A000FL, "The log service encountered a metadata file with inconsistent data."),

	/**
	 * The log service encountered an attempt to erroneously allocate or dispose
	 * reservation space.
	 */
	STATUS_LOG_RESERVATION_INVALID(0xC01A0010L,
		"The log service encountered an attempt to erroneously allocate or dispose reservation space."),

	/** The log service cannot delete the log file or the file system container. */
	STATUS_LOG_CANT_DELETE(0xC01A0011L, "The log service cannot delete the log file or the file system container."),

	/**
	 * The log service has reached the maximum allowable containers allocated to
	 * a log file.
	 */
	STATUS_LOG_CONTAINER_LIMIT_EXCEEDED(0xC01A0012L, "The log service has reached the " +
		"maximum allowable containers allocated to a log file."),

	/**
	 * The log service has attempted to read or write backward past the start of
	 * the log.
	 */
	STATUS_LOG_START_OF_LOG(0xC01A0013L, "The log service has attempted to read or write backward past the start of the log."),

	/**
	 * The log policy could not be installed because a policy of the same type
	 * is already present.
	 */
	STATUS_LOG_POLICY_ALREADY_INSTALLED(0xC01A0014L,
		"The log policy could not be installed because a policy of the same type is already present."),

	/** The log policy in question was not installed at the time of the request. */
	STATUS_LOG_POLICY_NOT_INSTALLED(0xC01A0015L, "The log policy in question was not installed at the time of the request."),

	/** The installed set of policies on the log is invalid. */
	STATUS_LOG_POLICY_INVALID(0xC01A0016L, "The installed set of policies on the log is invalid."),

	/** A policy on the log in question prevented the operation from completing. */
	STATUS_LOG_POLICY_CONFLICT(0xC01A0017L, "A policy on the log in question prevented the operation from completing."),

	/**
	 * The log space cannot be reclaimed because the log is pinned by the
	 * archive tail.
	 */
	STATUS_LOG_PINNED_ARCHIVE_TAIL(0xC01A0018L, "The log space cannot be reclaimed because the log is pinned by the archive tail."),

	/** The log record is not a record in the log file. */
	STATUS_LOG_RECORD_NONEXISTENT(0xC01A0019L, "The log record is not a record in the log file."),

	/**
	 * The number of reserved log records or the adjustment of the number of
	 * reserved log records is invalid.
	 */
	STATUS_LOG_RECORDS_RESERVED_INVALID(0xC01A001AL,
		"The number of reserved log records or the adjustment of the number of reserved log records is invalid."),

	/** The reserved log space or the adjustment of the log space is invalid. */
	STATUS_LOG_SPACE_RESERVED_INVALID(0xC01A001BL, "The reserved log space or the adjustment of the log space is invalid."),

	/** A new or existing archive tail or the base of the active log is invalid. */
	STATUS_LOG_TAIL_INVALID(0xC01A001CL, "A new or existing archive tail or the base of the active log is invalid."),

	/** The log space is exhausted. */
	STATUS_LOG_FULL(0xC01A001DL, "The log space is exhausted."),

	/**
	 * The log is multiplexed; no direct writes to the physical log are allowed.
	 */
	STATUS_LOG_MULTIPLEXED(0xC01A001EL, "The log is multiplexed; no direct writes to the physical log are allowed."),

	/** The operation failed because the log is dedicated. */
	STATUS_LOG_DEDICATED(0xC01A001FL, "The operation failed because the log is dedicated."),

	/** The operation requires an archive context. */
	STATUS_LOG_ARCHIVE_NOT_IN_PROGRESS(0xC01A0020L, "The operation requires an archive context."),

	/** Log archival is in progress. */
	STATUS_LOG_ARCHIVE_IN_PROGRESS(0xC01A0021L, "Log archival is in progress."),

	/** The operation requires a nonephemeral log, but the log is ephemeral. */
	STATUS_LOG_EPHEMERAL(0xC01A0022L, "The operation requires a nonephemeral log, but the log is ephemeral."),

	/**
	 * The log must have at least two containers before it can be read from or
	 * written to.
	 */
	STATUS_LOG_NOT_ENOUGH_CONTAINERS(0xC01A0023L, "The log must have at least two containers before it can be read from or written to."),

	/** A log client has already registered on the stream. */
	STATUS_LOG_CLIENT_ALREADY_REGISTERED(0xC01A0024L, "A log client has already registered on the stream."),

	/** A log client has not been registered on the stream. */
	STATUS_LOG_CLIENT_NOT_REGISTERED(0xC01A0025L, "A log client has not been registered on the stream."),

	/** A request has already been made to handle the log full condition. */
	STATUS_LOG_FULL_HANDLER_IN_PROGRESS(0xC01A0026L, "A request has already been made to handle the log full condition."),

	/**
	 * The log service encountered an error when attempting to read from a log
	 * container.
	 */
	STATUS_LOG_CONTAINER_READ_FAILED(0xC01A0027L, "The log service encountered an error when attempting to read from a log container."),

	/**
	 * The log service encountered an error when attempting to write to a log
	 * container.
	 */
	STATUS_LOG_CONTAINER_WRITE_FAILED(0xC01A0028L, "The log service encountered an error when attempting to write to a log container."),

	/**
	 * The log service encountered an error when attempting to open a log
	 * container.
	 */
	STATUS_LOG_CONTAINER_OPEN_FAILED(0xC01A0029L, "The log service encountered an error when attempting to open a log container."),

	/**
	 * The log service encountered an invalid container state when attempting a
	 * requested action.
	 */
	STATUS_LOG_CONTAINER_STATE_INVALID(0xC01A002AL,
		"The log service encountered an invalid container state when attempting a requested action."),

	/**
	 * The log service is not in the correct state to perform a requested
	 * action.
	 */
	STATUS_LOG_STATE_INVALID(0xC01A002BL, "The log service is not in the correct state to perform a requested action."),

	/** The log space cannot be reclaimed because the log is pinned. */
	STATUS_LOG_PINNED(0xC01A002CL, "The log space cannot be reclaimed because the log is pinned."),

	/** The log metadata flush failed. */
	STATUS_LOG_METADATA_FLUSH_FAILED(0xC01A002DL, "The log metadata flush failed."),

	/** Security on the log and its containers is inconsistent. */
	STATUS_LOG_INCONSISTENT_SECURITY(0xC01A002EL, "Security on the log and its containers is inconsistent."),

	/**
	 * Records were appended to the log or reservation changes were made, but
	 * the log could not be flushed.
	 */
	STATUS_LOG_APPENDED_FLUSH_FAILED(0xC01A002FL,
		"Records were appended to the log or reservation changes were made, but the log could not be flushed."),

	/**
	 * The log is pinned due to reservation consuming most of the log space.
	 * Free some reserved records to make space available.
	 */
	STATUS_LOG_PINNED_RESERVATION(0xC01A0030L,
		"The log is pinned due to reservation consuming most of the log space. Free some reserved records to make space available."),

	/**
	 * {Display Driver Stopped Responding} The %hs display driver has stopped
	 * working normally. Save your work and reboot the system to restore full
	 * display functionality. The next time you reboot the computer, a dialog box
	 * will allow you to upload data about this failure to Microsoft.
	 */
	STATUS_VIDEO_HUNG_DISPLAY_DRIVER_THREAD(
		0xC01B00EAL,
		"{Display Driver Stopped Responding} The %hs display driver has stopped working normally. " +
		"Save your work and reboot the system to restore full display functionality. The next time you " +
		"reboot the computer, a dialog box will allow you to upload data about this failure to Microsoft."),

	/** A handler was not defined by the filter for this operation. */
	STATUS_FLT_NO_HANDLER_DEFINED(0xC01C0001L, "A handler was not defined by the filter for this operation."),

	/** A context is already defined for this object. */
	STATUS_FLT_CONTEXT_ALREADY_DEFINED(0xC01C0002L, "A context is already defined for this object."),

	/** Asynchronous requests are not valid for this operation. */
	STATUS_FLT_INVALID_ASYNCHRONOUS_REQUEST(0xC01C0003L, "Asynchronous requests are not valid for this operation."),

	/**
	 * This is an internal error code used by the filter manager to determine if
	 * a fast I/O operation should be forced down the input/output request
	 * packet (IRP) path. Minifilters should never return this value.
	 */
	STATUS_FLT_DISALLOW_FAST_IO(
		0xC01C0004L,
		"This is an internal error code used by the filter manager to determine if a fast I/O operation " +
		"should be forced down the input/output request packet (IRP) path. Minifilters should never return this value."),

	/**
	 * An invalid name request was made. The name requested cannot be retrieved
	 * at this time.
	 */
	STATUS_FLT_INVALID_NAME_REQUEST(0xC01C0005L, "An invalid name request was made. The name requested cannot be retrieved at this time."),

	/**
	 * Posting this operation to a worker thread for further processing is not
	 * safe at this time because it could lead to a system deadlock.
	 */
	STATUS_FLT_NOT_SAFE_TO_POST_OPERATION(0xC01C0006L,
		"Posting this operation to a worker thread for further processing is not " +
		"safe at this time because it could lead to a system deadlock."),

	/**
	 * The Filter Manager was not initialized when a filter tried to register.
	 * Make sure that the Filter Manager is loaded as a driver.
	 */
	STATUS_FLT_NOT_INITIALIZED(0xC01C0007L,
		"The Filter Manager was not initialized when a filter tried to register. Make sure that the Filter Manager is loaded as a driver."),

	/**
	 * The filter is not ready for attachment to volumes because it has not
	 * finished initializing (FltStartFiltering has not been called).
	 */
	STATUS_FLT_FILTER_NOT_READY(0xC01C0008L,
		"The filter is not ready for attachment to volumes because it has not finished " +
		"initializing (FltStartFiltering has not been called)."),

	/**
	 * The filter must clean up any operation-specific context at this time
	 * because it is being removed from the system before the operation is
	 * completed by the lower drivers.
	 */
	STATUS_FLT_POST_OPERATION_CLEANUP(
		0xC01C0009L,
		"The filter must clean up any operation-specific context at this time because it is being " +
		"removed from the system before the operation is completed by the lower drivers."),

	/**
	 * The Filter Manager had an internal error from which it cannot recover;
	 * therefore, the operation has failed. This is usually the result of a
	 * filter returning an invalid value from a pre-operation callback.
	 */
	STATUS_FLT_INTERNAL_ERROR(
		0xC01C000AL,
		"The Filter Manager had an internal error from which it cannot recover; therefore, the operation has failed. " +
		"This is usually the result of a filter returning an invalid value from a pre-operation callback."),

	/**
	 * The object specified for this action is in the process of being deleted;
	 * therefore, the action requested cannot be completed at this time.
	 */
	STATUS_FLT_DELETING_OBJECT(
		0xC01C000BL,
		"The object specified for this action is in the process of being deleted; " +
		"therefore, the action requested cannot be completed at this time."),

	/** A nonpaged pool must be used for this type of context. */
	STATUS_FLT_MUST_BE_NONPAGED_POOL(0xC01C000CL, "A nonpaged pool must be used for this type of context."),

	/** A duplicate handler definition has been provided for an operation. */
	STATUS_FLT_DUPLICATE_ENTRY(0xC01C000DL, "A duplicate handler definition has been provided for an operation."),

	/** The callback data queue has been disabled. */
	STATUS_FLT_CBDQ_DISABLED(0xC01C000EL, "The callback data queue has been disabled."),

	/** Do not attach the filter to the volume at this time. */
	STATUS_FLT_DO_NOT_ATTACH(0xC01C000FL, "Do not attach the filter to the volume at this time."),

	/** Do not detach the filter from the volume at this time. */
	STATUS_FLT_DO_NOT_DETACH(0xC01C0010L, "Do not detach the filter from the volume at this time."),

	/** An instance already exists at this altitude on the volume specified. */
	STATUS_FLT_INSTANCE_ALTITUDE_COLLISION(0xC01C0011L, "An instance already exists at this altitude on the volume specified."),

	/** An instance already exists with this name on the volume specified. */
	STATUS_FLT_INSTANCE_NAME_COLLISION(0xC01C0012L, "An instance already exists with this name on the volume specified."),

	/** The system could not find the filter specified. */
	STATUS_FLT_FILTER_NOT_FOUND(0xC01C0013L, "The system could not find the filter specified."),

	/** The system could not find the volume specified. */
	STATUS_FLT_VOLUME_NOT_FOUND(0xC01C0014L, "The system could not find the volume specified."),

	/** The system could not find the instance specified. */
	STATUS_FLT_INSTANCE_NOT_FOUND(0xC01C0015L, "The system could not find the instance specified."),

	/**
	 * No registered context allocation definition was found for the given
	 * request.
	 */
	STATUS_FLT_CONTEXT_ALLOCATION_NOT_FOUND(0xC01C0016L, "No registered context allocation definition was found for the given request."),

	/** An invalid parameter was specified during context registration. */
	STATUS_FLT_INVALID_CONTEXT_REGISTRATION(0xC01C0017L, "An invalid parameter was specified during context registration."),

	/**
	 * The name requested was not found in the Filter Manager name cache and
	 * could not be retrieved from the file system.
	 */
	STATUS_FLT_NAME_CACHE_MISS(0xC01C0018L,
		"The name requested was not found in the Filter Manager name cache and could not be retrieved from the file system."),

	/** The requested device object does not exist for the given volume. */
	STATUS_FLT_NO_DEVICE_OBJECT(0xC01C0019L, "The requested device object does not exist for the given volume."),

	/** The specified volume is already mounted. */
	STATUS_FLT_VOLUME_ALREADY_MOUNTED(0xC01C001AL, "The specified volume is already mounted."),

	/** The specified transaction context is already enlisted in a transaction. */
	STATUS_FLT_ALREADY_ENLISTED(0xC01C001BL, "The specified transaction context is already enlisted in a transaction."),

	/** The specified context is already attached to another object. */
	STATUS_FLT_CONTEXT_ALREADY_LINKED(0xC01C001CL, "The specified context is already attached to another object."),

	/** No waiter is present for the filter's reply to this message. */
	STATUS_FLT_NO_WAITER_FOR_REPLY(0xC01C0020L, "No waiter is present for the filter's reply to this message."),

	/** A monitor descriptor could not be obtained. */
	STATUS_MONITOR_NO_DESCRIPTOR(0xC01D0001L, "A monitor descriptor could not be obtained."),

	/**
	 * This release does not support the format of the obtained monitor
	 * descriptor.
	 */
	STATUS_MONITOR_UNKNOWN_DESCRIPTOR_FORMAT(0xC01D0002L, "This release does not support the format of the obtained monitor descriptor."),

	/** The checksum of the obtained monitor descriptor is invalid. */
	STATUS_MONITOR_INVALID_DESCRIPTOR_CHECKSUM(0xC01D0003L, "The checksum of the obtained monitor descriptor is invalid."),

	/** The monitor descriptor contains an invalid standard timing block. */
	STATUS_MONITOR_INVALID_STANDARD_TIMING_BLOCK(0xC01D0004L, "The monitor descriptor contains an invalid standard timing block."),

	/**
	 * WMI data-block registration failed for one of the MSMonitorClass WMI
	 * subclasses.
	 */
	STATUS_MONITOR_WMI_DATABLOCK_REGISTRATION_FAILED(0xC01D0005L,
		"WMI data-block registration failed for one of the MSMonitorClass WMI subclasses."),

	/**
	 * The provided monitor descriptor block is either corrupted or does not
	 * contain the monitor's detailed serial number.
	 */
	STATUS_MONITOR_INVALID_SERIAL_NUMBER_MONDSC_BLOCK(0xC01D0006L,
		"The provided monitor descriptor block is either corrupted or does not contain the monitor's detailed serial number."),

	/**
	 * The provided monitor descriptor block is either corrupted or does not
	 * contain the monitor's user-friendly name.
	 */
	STATUS_MONITOR_INVALID_USER_FRIENDLY_MONDSC_BLOCK(0xC01D0007L,
		"The provided monitor descriptor block is either corrupted or does not contain the monitor's user-friendly name."),

	/**
	 * There is no monitor descriptor data at the specified (offset or size)
	 * region.
	 */
	STATUS_MONITOR_NO_MORE_DESCRIPTOR_DATA(0xC01D0008L, "There is no monitor descriptor data at the specified (offset or size) region."),

	/** The monitor descriptor contains an invalid detailed timing block. */
	STATUS_MONITOR_INVALID_DETAILED_TIMING_BLOCK(0xC01D0009L, "The monitor descriptor contains an invalid detailed timing block."),

	/** Monitor descriptor contains invalid manufacture date. */
	STATUS_MONITOR_INVALID_MANUFACTURE_DATE(0xC01D000AL, "Monitor descriptor contains invalid manufacture date."),

	/**
	 * Exclusive mode ownership is needed to create an unmanaged primary
	 * allocation.
	 */
	STATUS_GRAPHICS_NOT_EXCLUSIVE_MODE_OWNER(0xC01E0000L, "Exclusive mode ownership is needed to create an unmanaged primary allocation."),

	/**
	 * The driver needs more DMA buffer space to complete the requested
	 * operation.
	 */
	STATUS_GRAPHICS_INSUFFICIENT_DMA_BUFFER(0xC01E0001L, "The driver needs more DMA buffer space to complete the requested operation."),

	/** The specified display adapter handle is invalid. */
	STATUS_GRAPHICS_INVALID_DISPLAY_ADAPTER(0xC01E0002L, "The specified display adapter handle is invalid."),

	/** The specified display adapter and all of its state have been reset. */
	STATUS_GRAPHICS_ADAPTER_WAS_RESET(0xC01E0003L, "The specified display adapter and all of its state have been reset."),

	/** The driver stack does not match the expected driver model. */
	STATUS_GRAPHICS_INVALID_DRIVER_MODEL(0xC01E0004L, "The driver stack does not match the expected driver model."),

	/** Present happened but ended up into the changed desktop mode. */
	STATUS_GRAPHICS_PRESENT_MODE_CHANGED(0xC01E0005L, "Present happened but ended up into the changed desktop mode."),

	/** Nothing to present due to desktop occlusion. */
	STATUS_GRAPHICS_PRESENT_OCCLUDED(0xC01E0006L, "Nothing to present due to desktop occlusion."),

	/** Not able to present due to denial of desktop access. */
	STATUS_GRAPHICS_PRESENT_DENIED(0xC01E0007L, "Not able to present due to denial of desktop access."),

	/** Not able to present with color conversion. */
	STATUS_GRAPHICS_CANNOTCOLORCONVERT(0xC01E0008L, "Not able to present with color conversion."),

	/**
	 * Present redirection is disabled (desktop windowing management subsystem
	 * is off).
	 */
	STATUS_GRAPHICS_PRESENT_REDIRECTION_DISABLED(0xC01E000BL,
		"Present redirection is disabled (desktop windowing management subsystem is off)."),

	/** Previous exclusive VidPn source owner has released its ownership */
	STATUS_GRAPHICS_PRESENT_UNOCCLUDED(0xC01E000CL, "Previous exclusive VidPn source owner has released its ownership"),

	/** Not enough video memory is available to complete the operation. */
	STATUS_GRAPHICS_NO_VIDEO_MEMORY(0xC01E0100L, "Not enough video memory is available to complete the operation."),

	/** Could not probe and lock the underlying memory of an allocation. */
	STATUS_GRAPHICS_CANT_LOCK_MEMORY(0xC01E0101L, "Could not probe and lock the underlying memory of an allocation."),

	/** The allocation is currently busy. */
	STATUS_GRAPHICS_ALLOCATION_BUSY(0xC01E0102L, "The allocation is currently busy."),

	/**
	 * An object being referenced has already reached the maximum reference
	 * count and cannot be referenced further.
	 */
	STATUS_GRAPHICS_TOO_MANY_REFERENCES(0xC01E0103L,
		"An object being referenced has already reached the maximum reference count and cannot be referenced further."),

	/**
	 * A problem could not be solved due to an existing condition. Try again
	 * later.
	 */
	STATUS_GRAPHICS_TRY_AGAIN_LATER(0xC01E0104L, "A problem could not be solved due to an existing condition. Try again later."),

	/**
	 * A problem could not be solved due to an existing condition. Try again
	 * now.
	 */
	STATUS_GRAPHICS_TRY_AGAIN_NOW(0xC01E0105L, "A problem could not be solved due to an existing condition. Try again now."),

	/** The allocation is invalid. */
	STATUS_GRAPHICS_ALLOCATION_INVALID(0xC01E0106L, "The allocation is invalid."),

	/** No more unswizzling apertures are currently available. */
	STATUS_GRAPHICS_UNSWIZZLING_APERTURE_UNAVAILABLE(0xC01E0107L, "No more unswizzling apertures are currently available."),

	/** The current allocation cannot be unswizzled by an aperture. */
	STATUS_GRAPHICS_UNSWIZZLING_APERTURE_UNSUPPORTED(0xC01E0108L, "The current allocation cannot be unswizzled by an aperture."),

	/** The request failed because a pinned allocation cannot be evicted. */
	STATUS_GRAPHICS_CANT_EVICT_PINNED_ALLOCATION(0xC01E0109L, "The request failed because a pinned allocation cannot be evicted."),

	/**
	 * The allocation cannot be used from its current segment location for the
	 * specified operation.
	 */
	STATUS_GRAPHICS_INVALID_ALLOCATION_USAGE(0xC01E0110L,
		"The allocation cannot be used from its current segment location for the specified operation."),

	/** A locked allocation cannot be used in the current command buffer. */
	STATUS_GRAPHICS_CANT_RENDER_LOCKED_ALLOCATION(0xC01E0111L, "A locked allocation cannot be used in the current command buffer."),

	/** The allocation being referenced has been closed permanently. */
	STATUS_GRAPHICS_ALLOCATION_CLOSED(0xC01E0112L, "The allocation being referenced has been closed permanently."),

	/** An invalid allocation instance is being referenced. */
	STATUS_GRAPHICS_INVALID_ALLOCATION_INSTANCE(0xC01E0113L, "An invalid allocation instance is being referenced."),

	/** An invalid allocation handle is being referenced. */
	STATUS_GRAPHICS_INVALID_ALLOCATION_HANDLE(0xC01E0114L, "An invalid allocation handle is being referenced."),

	/** The allocation being referenced does not belong to the current device. */
	STATUS_GRAPHICS_WRONG_ALLOCATION_DEVICE(0xC01E0115L, "The allocation being referenced does not belong to the current device."),

	/** The specified allocation lost its content. */
	STATUS_GRAPHICS_ALLOCATION_CONTENT_LOST(0xC01E0116L, "The specified allocation lost its content."),

	/**
	 * A GPU exception was detected on the given device. The device cannot be
	 * scheduled.
	 */
	STATUS_GRAPHICS_GPU_EXCEPTION_ON_DEVICE(0xC01E0200L,
		"A GPU exception was detected on the given device. The device cannot be scheduled."),

	/** The specified VidPN topology is invalid. */
	STATUS_GRAPHICS_INVALID_VIDPN_TOPOLOGY(0xC01E0300L, "The specified VidPN topology is invalid."),

	/**
	 * The specified VidPN topology is valid but is not supported by this model
	 * of the display adapter.
	 */
	STATUS_GRAPHICS_VIDPN_TOPOLOGY_NOT_SUPPORTED(0xC01E0301L,
		"The specified VidPN topology is valid but is not supported by this model of the display adapter."),

	/**
	 * The specified VidPN topology is valid but is not currently supported by
	 * the display adapter due to allocation of its resources.
	 */
	STATUS_GRAPHICS_VIDPN_TOPOLOGY_CURRENTLY_NOT_SUPPORTED(0xC01E0302L,
		"The specified VidPN topology is valid but is not currently supported by the display adapter due to allocation of its resources."),

	/** The specified VidPN handle is invalid. */
	STATUS_GRAPHICS_INVALID_VIDPN(0xC01E0303L, "The specified VidPN handle is invalid."),

	/** The specified video present source is invalid. */
	STATUS_GRAPHICS_INVALID_VIDEO_PRESENT_SOURCE(0xC01E0304L, "The specified video present source is invalid."),

	/** The specified video present target is invalid. */
	STATUS_GRAPHICS_INVALID_VIDEO_PRESENT_TARGET(0xC01E0305L, "The specified video present target is invalid."),

	/**
	 * The specified VidPN modality is not supported (for example, at least two
	 * of the pinned modes are not co-functional).
	 */
	STATUS_GRAPHICS_VIDPN_MODALITY_NOT_SUPPORTED(0xC01E0306L,
		"The specified VidPN modality is not supported (for example, at least two of the pinned modes are not co-functional)."),

	/** The specified VidPN source mode set is invalid. */
	STATUS_GRAPHICS_INVALID_VIDPN_SOURCEMODESET(0xC01E0308L, "The specified VidPN source mode set is invalid."),

	/** The specified VidPN target mode set is invalid. */
	STATUS_GRAPHICS_INVALID_VIDPN_TARGETMODESET(0xC01E0309L, "The specified VidPN target mode set is invalid."),

	/** The specified video signal frequency is invalid. */
	STATUS_GRAPHICS_INVALID_FREQUENCY(0xC01E030AL, "The specified video signal frequency is invalid."),

	/** The specified video signal active region is invalid. */
	STATUS_GRAPHICS_INVALID_ACTIVE_REGION(0xC01E030BL, "The specified video signal active region is invalid."),

	/** The specified video signal total region is invalid. */
	STATUS_GRAPHICS_INVALID_TOTAL_REGION(0xC01E030CL, "The specified video signal total region is invalid."),

	/** The specified video present source mode is invalid. */
	STATUS_GRAPHICS_INVALID_VIDEO_PRESENT_SOURCE_MODE(0xC01E0310L, "The specified video present source mode is invalid."),

	/** The specified video present target mode is invalid. */
	STATUS_GRAPHICS_INVALID_VIDEO_PRESENT_TARGET_MODE(0xC01E0311L, "The specified video present target mode is invalid."),

	/**
	 * The pinned mode must remain in the set on the VidPN's co-functional
	 * modality enumeration.
	 */
	STATUS_GRAPHICS_PINNED_MODE_MUST_REMAIN_IN_SET(0xC01E0312L,
		"The pinned mode must remain in the set on the VidPN's co-functional modality enumeration."),

	/** The specified video present path is already in the VidPN's topology. */
	STATUS_GRAPHICS_PATH_ALREADY_IN_TOPOLOGY(0xC01E0313L, "The specified video present path is already in the VidPN's topology."),

	/** The specified mode is already in the mode set. */
	STATUS_GRAPHICS_MODE_ALREADY_IN_MODESET(0xC01E0314L, "The specified mode is already in the mode set."),

	/** The specified video present source set is invalid. */
	STATUS_GRAPHICS_INVALID_VIDEOPRESENTSOURCESET(0xC01E0315L, "The specified video present source set is invalid."),

	/** The specified video present target set is invalid. */
	STATUS_GRAPHICS_INVALID_VIDEOPRESENTTARGETSET(0xC01E0316L, "The specified video present target set is invalid."),

	/**
	 * The specified video present source is already in the video present source
	 * set.
	 */
	STATUS_GRAPHICS_SOURCE_ALREADY_IN_SET(0xC01E0317L, "The specified video present source is already in the video present source set."),

	/**
	 * The specified video present target is already in the video present target
	 * set.
	 */
	STATUS_GRAPHICS_TARGET_ALREADY_IN_SET(0xC01E0318L, "The specified video present target is already in the video present target set."),

	/** The specified VidPN present path is invalid. */
	STATUS_GRAPHICS_INVALID_VIDPN_PRESENT_PATH(0xC01E0319L, "The specified VidPN present path is invalid."),

	/**
	 * The miniport has no recommendation for augmenting the specified VidPN's
	 * topology.
	 */
	STATUS_GRAPHICS_NO_RECOMMENDED_VIDPN_TOPOLOGY(0xC01E031AL,
		"The miniport has no recommendation for augmenting the specified VidPN's topology."),

	/** The specified monitor frequency range set is invalid. */
	STATUS_GRAPHICS_INVALID_MONITOR_FREQUENCYRANGESET(0xC01E031BL, "The specified monitor frequency range set is invalid."),

	/** The specified monitor frequency range is invalid. */
	STATUS_GRAPHICS_INVALID_MONITOR_FREQUENCYRANGE(0xC01E031CL, "The specified monitor frequency range is invalid."),

	/**
	 * The specified frequency range is not in the specified monitor frequency
	 * range set.
	 */
	STATUS_GRAPHICS_FREQUENCYRANGE_NOT_IN_SET(0xC01E031DL,
		"The specified frequency range is not in the specified monitor frequency range set."),

	/**
	 * The specified frequency range is already in the specified monitor
	 * frequency range set.
	 */
	STATUS_GRAPHICS_FREQUENCYRANGE_ALREADY_IN_SET(0xC01E031FL,
		"The specified frequency range is already in the specified monitor frequency range set."),

	/** The specified mode set is stale. Reacquire the new mode set. */
	STATUS_GRAPHICS_STALE_MODESET(0xC01E0320L, "The specified mode set is stale. Reacquire the new mode set."),

	/** The specified monitor source mode set is invalid. */
	STATUS_GRAPHICS_INVALID_MONITOR_SOURCEMODESET(0xC01E0321L, "The specified monitor source mode set is invalid."),

	/** The specified monitor source mode is invalid. */
	STATUS_GRAPHICS_INVALID_MONITOR_SOURCE_MODE(0xC01E0322L, "The specified monitor source mode is invalid."),

	/**
	 * The miniport does not have a recommendation regarding the request to
	 * provide a functional VidPN given the current display adapter
	 * configuration.
	 */
	STATUS_GRAPHICS_NO_RECOMMENDED_FUNCTIONAL_VIDPN(
		0xC01E0323L,
		"The miniport does not have a recommendation regarding the request " +
		"to provide a functional VidPN given the current display adapter configuration."),

	/** The ID of the specified mode is being used by another mode in the set. */
	STATUS_GRAPHICS_MODE_ID_MUST_BE_UNIQUE(0xC01E0324L, "The ID of the specified mode is being used by another mode in the set."),

	/**
	 * The system failed to determine a mode that is supported by both the
	 * display adapter and the monitor connected to it.
	 */
	STATUS_GRAPHICS_EMPTY_ADAPTER_MONITOR_MODE_SUPPORT_INTERSECTION(0xC01E0325L,
		"The system failed to determine a mode that is supported by both the display adapter and the monitor connected to it."),

	/**
	 * The number of video present targets must be greater than or equal to the
	 * number of video present sources.
	 */
	STATUS_GRAPHICS_VIDEO_PRESENT_TARGETS_LESS_THAN_SOURCES(0xC01E0326L,
		"The number of video present targets must be greater than or equal to the number of video present sources."),

	/** The specified present path is not in the VidPN's topology. */
	STATUS_GRAPHICS_PATH_NOT_IN_TOPOLOGY(0xC01E0327L, "The specified present path is not in the VidPN's topology."),

	/** The display adapter must have at least one video present source. */
	STATUS_GRAPHICS_ADAPTER_MUST_HAVE_AT_LEAST_ONE_SOURCE(0xC01E0328L, "The display adapter must have at least one video present source."),

	/** The display adapter must have at least one video present target. */
	STATUS_GRAPHICS_ADAPTER_MUST_HAVE_AT_LEAST_ONE_TARGET(0xC01E0329L, "The display adapter must have at least one video present target."),

	/** The specified monitor descriptor set is invalid. */
	STATUS_GRAPHICS_INVALID_MONITORDESCRIPTORSET(0xC01E032AL, "The specified monitor descriptor set is invalid."),

	/** The specified monitor descriptor is invalid. */
	STATUS_GRAPHICS_INVALID_MONITORDESCRIPTOR(0xC01E032BL, "The specified monitor descriptor is invalid."),

	/** The specified descriptor is not in the specified monitor descriptor set. */
	STATUS_GRAPHICS_MONITORDESCRIPTOR_NOT_IN_SET(0xC01E032CL, "The specified descriptor is not in the specified monitor descriptor set."),

	/**
	 * The specified descriptor is already in the specified monitor descriptor
	 * set.
	 */
	STATUS_GRAPHICS_MONITORDESCRIPTOR_ALREADY_IN_SET(0xC01E032DL,
		"The specified descriptor is already in the specified monitor descriptor set."),

	/**
	 * The ID of the specified monitor descriptor is being used by another
	 * descriptor in the set.
	 */
	STATUS_GRAPHICS_MONITORDESCRIPTOR_ID_MUST_BE_UNIQUE(0xC01E032EL,
		"The ID of the specified monitor descriptor is being used by another descriptor in the set."),

	/** The specified video present target subset type is invalid. */
	STATUS_GRAPHICS_INVALID_VIDPN_TARGET_SUBSET_TYPE(0xC01E032FL, "The specified video present target subset type is invalid."),

	/**
	 * Two or more of the specified resources are not related to each other, as
	 * defined by the interface semantics.
	 */
	STATUS_GRAPHICS_RESOURCES_NOT_RELATED(0xC01E0330L,
		"Two or more of the specified resources are not related to each other, as defined by the interface semantics."),

	/**
	 * The ID of the specified video present source is being used by another
	 * source in the set.
	 */
	STATUS_GRAPHICS_SOURCE_ID_MUST_BE_UNIQUE(0xC01E0331L,
		"The ID of the specified video present source is being used by another source in the set."),

	/**
	 * The ID of the specified video present target is being used by another
	 * target in the set.
	 */
	STATUS_GRAPHICS_TARGET_ID_MUST_BE_UNIQUE(0xC01E0332L,
		"The ID of the specified video present target is being used by another target in the set."),

	/**
	 * The specified VidPN source cannot be used because there is no available
	 * VidPN target to connect it to.
	 */
	STATUS_GRAPHICS_NO_AVAILABLE_VIDPN_TARGET(0xC01E0333L,
		"The specified VidPN source cannot be used because there is no available VidPN target to connect it to."),

	/**
	 * The newly arrived monitor could not be associated with a display adapter.
	 */
	STATUS_GRAPHICS_MONITOR_COULD_NOT_BE_ASSOCIATED_WITH_ADAPTER(0xC01E0334L,
		"The newly arrived monitor could not be associated with a display adapter."),

	/**
	 * The particular display adapter does not have an associated VidPN manager.
	 */
	STATUS_GRAPHICS_NO_VIDPNMGR(0xC01E0335L, "The particular display adapter does not have an associated VidPN manager."),

	/**
	 * The VidPN manager of the particular display adapter does not have an
	 * active VidPN.
	 */
	STATUS_GRAPHICS_NO_ACTIVE_VIDPN(0xC01E0336L, "The VidPN manager of the particular display adapter does not have an active VidPN."),

	/** The specified VidPN topology is stale; obtain the new topology. */
	STATUS_GRAPHICS_STALE_VIDPN_TOPOLOGY(0xC01E0337L, "The specified VidPN topology is stale; obtain the new topology."),

	/** No monitor is connected on the specified video present target. */
	STATUS_GRAPHICS_MONITOR_NOT_CONNECTED(0xC01E0338L, "No monitor is connected on the specified video present target."),

	/** The specified source is not part of the specified VidPN's topology. */
	STATUS_GRAPHICS_SOURCE_NOT_IN_TOPOLOGY(0xC01E0339L, "The specified source is not part of the specified VidPN's topology."),

	/** The specified primary surface size is invalid. */
	STATUS_GRAPHICS_INVALID_PRIMARYSURFACE_SIZE(0xC01E033AL, "The specified primary surface size is invalid."),

	/** The specified visible region size is invalid. */
	STATUS_GRAPHICS_INVALID_VISIBLEREGION_SIZE(0xC01E033BL, "The specified visible region size is invalid."),

	/** The specified stride is invalid. */
	STATUS_GRAPHICS_INVALID_STRIDE(0xC01E033CL, "The specified stride is invalid."),

	/** The specified pixel format is invalid. */
	STATUS_GRAPHICS_INVALID_PIXELFORMAT(0xC01E033DL, "The specified pixel format is invalid."),

	/** The specified color basis is invalid. */
	STATUS_GRAPHICS_INVALID_COLORBASIS(0xC01E033EL, "The specified color basis is invalid."),

	/** The specified pixel value access mode is invalid. */
	STATUS_GRAPHICS_INVALID_PIXELVALUEACCESSMODE(0xC01E033FL, "The specified pixel value access mode is invalid."),

	/** The specified target is not part of the specified VidPN's topology. */
	STATUS_GRAPHICS_TARGET_NOT_IN_TOPOLOGY(0xC01E0340L, "The specified target is not part of the specified VidPN's topology."),

	/** Failed to acquire the display mode management interface. */
	STATUS_GRAPHICS_NO_DISPLAY_MODE_MANAGEMENT_SUPPORT(0xC01E0341L, "Failed to acquire the display mode management interface."),

	/**
	 * The specified VidPN source is already owned by a DMM client and cannot be
	 * used until that client releases it.
	 */
	STATUS_GRAPHICS_VIDPN_SOURCE_IN_USE(0xC01E0342L,
		"The specified VidPN source is already owned by a DMM client and cannot be used until that client releases it."),

	/** The specified VidPN is active and cannot be accessed. */
	STATUS_GRAPHICS_CANT_ACCESS_ACTIVE_VIDPN(0xC01E0343L, "The specified VidPN is active and cannot be accessed."),

	/** The specified VidPN's present path importance ordinal is invalid. */
	STATUS_GRAPHICS_INVALID_PATH_IMPORTANCE_ORDINAL(0xC01E0344L, "The specified VidPN's present path importance ordinal is invalid."),

	/**
	 * The specified VidPN's present path content geometry transformation is
	 * invalid.
	 */
	STATUS_GRAPHICS_INVALID_PATH_CONTENT_GEOMETRY_TRANSFORMATION(0xC01E0345L,
		"The specified VidPN's present path content geometry transformation is invalid."),

	/**
	 * The specified content geometry transformation is not supported on the
	 * respective VidPN present path.
	 */
	STATUS_GRAPHICS_PATH_CONTENT_GEOMETRY_TRANSFORMATION_NOT_SUPPORTED(0xC01E0346L,
		"The specified content geometry transformation is not supported on the respective VidPN present path."),

	/** The specified gamma ramp is invalid. */
	STATUS_GRAPHICS_INVALID_GAMMA_RAMP(0xC01E0347L, "The specified gamma ramp is invalid."),

	/**
	 * The specified gamma ramp is not supported on the respective VidPN present
	 * path.
	 */
	STATUS_GRAPHICS_GAMMA_RAMP_NOT_SUPPORTED(0xC01E0348L, "The specified gamma ramp is not supported on the respective VidPN present path."),

	/** Multisampling is not supported on the respective VidPN present path. */
	STATUS_GRAPHICS_MULTISAMPLING_NOT_SUPPORTED(0xC01E0349L, "Multisampling is not supported on the respective VidPN present path."),

	/** The specified mode is not in the specified mode set. */
	STATUS_GRAPHICS_MODE_NOT_IN_MODESET(0xC01E034AL, "The specified mode is not in the specified mode set."),

	/** The specified VidPN topology recommendation reason is invalid. */
	STATUS_GRAPHICS_INVALID_VIDPN_TOPOLOGY_RECOMMENDATION_REASON(0xC01E034DL,
		"The specified VidPN topology recommendation reason is invalid."),

	/** The specified VidPN present path content type is invalid. */
	STATUS_GRAPHICS_INVALID_PATH_CONTENT_TYPE(0xC01E034EL, "The specified VidPN present path content type is invalid."),

	/** The specified VidPN present path copy protection type is invalid. */
	STATUS_GRAPHICS_INVALID_COPYPROTECTION_TYPE(0xC01E034FL, "The specified VidPN present path copy protection type is invalid."),

	/**
	 * Only one unassigned mode set can exist at any one time for a particular
	 * VidPN source or target.
	 */
	STATUS_GRAPHICS_UNASSIGNED_MODESET_ALREADY_EXISTS(0xC01E0350L,
		"Only one unassigned mode set can exist at any one time for a particular VidPN source or target."),

	/** The specified scan line ordering type is invalid. */
	STATUS_GRAPHICS_INVALID_SCANLINE_ORDERING(0xC01E0352L, "The specified scan line ordering type is invalid."),

	/** The topology changes are not allowed for the specified VidPN. */
	STATUS_GRAPHICS_TOPOLOGY_CHANGES_NOT_ALLOWED(0xC01E0353L, "The topology changes are not allowed for the specified VidPN."),

	/**
	 * All available importance ordinals are being used in the specified
	 * topology.
	 */
	STATUS_GRAPHICS_NO_AVAILABLE_IMPORTANCE_ORDINALS(0xC01E0354L,
		"All available importance ordinals are being used in the specified topology."),

	/**
	 * The specified primary surface has a different private-format attribute
	 * than the current primary surface.
	 */
	STATUS_GRAPHICS_INCOMPATIBLE_PRIVATE_FORMAT(0xC01E0355L,
		"The specified primary surface has a different private-format attribute than the current primary surface."),

	/** The specified mode-pruning algorithm is invalid. */
	STATUS_GRAPHICS_INVALID_MODE_PRUNING_ALGORITHM(0xC01E0356L, "The specified mode-pruning algorithm is invalid."),

	/** The specified monitor-capability origin is invalid. */
	STATUS_GRAPHICS_INVALID_MONITOR_CAPABILITY_ORIGIN(0xC01E0357L, "The specified monitor-capability origin is invalid."),

	/** The specified monitor-frequency range constraint is invalid. */
	STATUS_GRAPHICS_INVALID_MONITOR_FREQUENCYRANGE_CONSTRAINT(0xC01E0358L, "The specified monitor-frequency range constraint is invalid."),

	/** The maximum supported number of present paths has been reached. */
	STATUS_GRAPHICS_MAX_NUM_PATHS_REACHED(0xC01E0359L, "The maximum supported number of present paths has been reached."),

	/**
	 * The miniport requested that augmentation be canceled for the specified
	 * source of the specified VidPN's topology.
	 */
	STATUS_GRAPHICS_CANCEL_VIDPN_TOPOLOGY_AUGMENTATION(0xC01E035AL,
		"The miniport requested that augmentation be canceled for the specified source of the specified VidPN's topology."),

	/** The specified client type was not recognized. */
	STATUS_GRAPHICS_INVALID_CLIENT_TYPE(0xC01E035BL, "The specified client type was not recognized."),

	/**
	 * The client VidPN is not set on this adapter (for example, no user
	 * mode-initiated mode changes have taken place on this adapter).
	 */
	STATUS_GRAPHICS_CLIENTVIDPN_NOT_SET(0xC01E035CL,
		"The client VidPN is not set on this adapter (for example, no user mode-initiated mode changes have taken place on this adapter)."),

	/**
	 * The specified display adapter child device already has an external device
	 * connected to it.
	 */
	STATUS_GRAPHICS_SPECIFIED_CHILD_ALREADY_CONNECTED(0xC01E0400L,
		"The specified display adapter child device already has an external device connected to it."),

	/**
	 * The display adapter child device does not support reporting a descriptor.
	 */
	STATUS_GRAPHICS_CHILD_DESCRIPTOR_NOT_SUPPORTED(0xC01E0401L, "The display adapter child device does not support reporting a descriptor."),

	/** The display adapter is not linked to any other adapters. */
	STATUS_GRAPHICS_NOT_A_LINKED_ADAPTER(0xC01E0430L, "The display adapter is not linked to any other adapters."),

	/** The lead adapter in a linked configuration was not enumerated yet. */
	STATUS_GRAPHICS_LEADLINK_NOT_ENUMERATED(0xC01E0431L, "The lead adapter in a linked configuration was not enumerated yet."),

	/**
	 * Some chain adapters in a linked configuration have not yet been
	 * enumerated.
	 */
	STATUS_GRAPHICS_CHAINLINKS_NOT_ENUMERATED(0xC01E0432L, "Some chain adapters in a linked configuration have not yet been enumerated."),

	/**
	 * The chain of linked adapters is not ready to start because of an unknown
	 * failure.
	 */
	STATUS_GRAPHICS_ADAPTER_CHAIN_NOT_READY(0xC01E0433L,
		"The chain of linked adapters is not ready to start because of an unknown failure."),

	/**
	 * An attempt was made to start a lead link display adapter when the chain
	 * links had not yet started.
	 */
	STATUS_GRAPHICS_CHAINLINKS_NOT_STARTED(0xC01E0434L,
		"An attempt was made to start a lead link display adapter when the chain links had not yet started."),

	/**
	 * An attempt was made to turn on a lead link display adapter when the chain
	 * links were turned off.
	 */
	STATUS_GRAPHICS_CHAINLINKS_NOT_POWERED_ON(0xC01E0435L,
		"An attempt was made to turn on a lead link display adapter when the chain links were turned off."),

	/**
	 * The adapter link was found in an inconsistent state. Not all adapters are
	 * in an expected PNP/power state.
	 */
	STATUS_GRAPHICS_INCONSISTENT_DEVICE_LINK_STATE(0xC01E0436L,
		"The adapter link was found in an inconsistent state. Not all adapters are in an expected PNP/power state."),

	/**
	 * The driver trying to start is not the same as the driver for the posted
	 * display adapter.
	 */
	STATUS_GRAPHICS_NOT_POST_DEVICE_DRIVER(0xC01E0438L,
		"The driver trying to start is not the same as the driver for the posted display adapter."),

	/**
	 * An operation is being attempted that requires the display adapter to be
	 * in a quiescent state.
	 */
	STATUS_GRAPHICS_ADAPTER_ACCESS_NOT_EXCLUDED(0xC01E043BL,
		"An operation is being attempted that requires the display adapter to be in a quiescent state."),

	/** The driver does not support OPM. */
	STATUS_GRAPHICS_OPM_NOT_SUPPORTED(0xC01E0500L, "The driver does not support OPM."),

	/** The driver does not support COPP. */
	STATUS_GRAPHICS_COPP_NOT_SUPPORTED(0xC01E0501L, "The driver does not support COPP."),

	/** The driver does not support UAB. */
	STATUS_GRAPHICS_UAB_NOT_SUPPORTED(0xC01E0502L, "The driver does not support UAB."),

	/** The specified encrypted parameters are invalid. */
	STATUS_GRAPHICS_OPM_INVALID_ENCRYPTED_PARAMETERS(0xC01E0503L, "The specified encrypted parameters are invalid."),

	/**
	 * An array passed to a function cannot hold all of the data that the
	 * function wants to put in it.
	 */
	STATUS_GRAPHICS_OPM_PARAMETER_ARRAY_TOO_SMALL(0xC01E0504L,
		"An array passed to a function cannot hold all of the data that the function wants to put in it."),

	/**
	 * The GDI display device passed to this function does not have any active
	 * protected outputs.
	 */
	STATUS_GRAPHICS_OPM_NO_PROTECTED_OUTPUTS_EXIST(0xC01E0505L,
		"The GDI display device passed to this function does not have any active protected outputs."),

	/**
	 * The PVP cannot find an actual GDI display device that corresponds to the
	 * passed-in GDI display device name.
	 */
	STATUS_GRAPHICS_PVP_NO_DISPLAY_DEVICE_CORRESPONDS_TO_NAME(0xC01E0506L,
		"The PVP cannot find an actual GDI display device that corresponds to the passed-in GDI display device name."),

	/**
	 * This function failed because the GDI display device passed to it was not
	 * attached to the Windows desktop.
	 */
	STATUS_GRAPHICS_PVP_DISPLAY_DEVICE_NOT_ATTACHED_TO_DESKTOP(0xC01E0507L,
		"This function failed because the GDI display device passed to it was not attached to the Windows desktop."),

	/**
	 * The PVP does not support mirroring display devices because they do not
	 * have any protected outputs.
	 */
	STATUS_GRAPHICS_PVP_MIRRORING_DEVICES_NOT_SUPPORTED(0xC01E0508L,
		"The PVP does not support mirroring display devices because they do not have any protected outputs."),

	/**
	 * The function failed because an invalid pointer parameter was passed to
	 * it. A pointer parameter is invalid if it is null, is not correctly
	 * aligned, or it points to an invalid address or a kernel mode address.
	 */
	STATUS_GRAPHICS_OPM_INVALID_POINTER(
		0xC01E050AL,
		"The function failed because an invalid pointer parameter was passed to it. A pointer parameter is invalid " +
		"if it is null, is not correctly aligned, or it points to an invalid address or a kernel mode address."),

	/** An internal error caused an operation to fail. */
	STATUS_GRAPHICS_OPM_INTERNAL_ERROR(0xC01E050BL, "An internal error caused an operation to fail."),

	/**
	 * The function failed because the caller passed in an invalid OPM user-mode
	 * handle.
	 */
	STATUS_GRAPHICS_OPM_INVALID_HANDLE(0xC01E050CL, "The function failed because the caller passed in an invalid OPM user-mode handle."),

	/**
	 * This function failed because the GDI device passed to it did not have any
	 * monitors associated with it.
	 */
	STATUS_GRAPHICS_PVP_NO_MONITORS_CORRESPOND_TO_DISPLAY_DEVICE(0xC01E050DL,
		"This function failed because the GDI device passed to it did not have any monitors associated with it."),

	/**
	 * A certificate could not be returned because the certificate buffer passed
	 * to the function was too small.
	 */
	STATUS_GRAPHICS_PVP_INVALID_CERTIFICATE_LENGTH(0xC01E050EL,
		"A certificate could not be returned because the certificate buffer passed to the function was too small."),

	/**
	 * DxgkDdiOpmCreateProtectedOutput() could not create a protected output
	 * because the video present yarget is in spanning mode.
	 */
	STATUS_GRAPHICS_OPM_SPANNING_MODE_ENABLED(0xC01E050FL,
		"DxgkDdiOpmCreateProtectedOutput() could not create a protected output because the video present yarget is in spanning mode."),

	/**
	 * DxgkDdiOpmCreateProtectedOutput() could not create a protected output
	 * because the video present target is in theater mode.
	 */
	STATUS_GRAPHICS_OPM_THEATER_MODE_ENABLED(0xC01E0510L,
		"DxgkDdiOpmCreateProtectedOutput() could not create a protected output because the video present target is in theater mode."),

	/**
	 * The function call failed because the display adapter's hardware
	 * functionality scan (HFS) failed to validate the graphics hardware.
	 */
	STATUS_GRAPHICS_PVP_HFS_FAILED(0xC01E0511L,
		"The function call failed because the display adapter's hardware " +
		"functionality scan (HFS) failed to validate the graphics hardware."),

	/**
	 * The HDCP SRM passed to this function did not comply with section 5 of the
	 * HDCP 1.1 specification.
	 */
	STATUS_GRAPHICS_OPM_INVALID_SRM(0xC01E0512L,
		"The HDCP SRM passed to this function did not comply with section 5 of the HDCP 1.1 specification."),

	/**
	 * The protected output cannot enable the HDCP system because it does not
	 * support it.
	 */
	STATUS_GRAPHICS_OPM_OUTPUT_DOES_NOT_SUPPORT_HDCP(0xC01E0513L,
		"The protected output cannot enable the HDCP system because it does not support it."),

	/**
	 * The protected output cannot enable analog copy protection because it does
	 * not support it.
	 */
	STATUS_GRAPHICS_OPM_OUTPUT_DOES_NOT_SUPPORT_ACP(0xC01E0514L,
		"The protected output cannot enable analog copy protection because it does not support it."),

	/**
	 * The protected output cannot enable the CGMS-A protection technology
	 * because it does not support it.
	 */
	STATUS_GRAPHICS_OPM_OUTPUT_DOES_NOT_SUPPORT_CGMSA(0xC01E0515L,
		"The protected output cannot enable the CGMS-A protection technology because it does not support it."),

	/**
	 * DxgkDdiOPMGetInformation() cannot return the version of the SRM being
	 * used because the application never successfully passed an SRM to the
	 * protected output.
	 */
	STATUS_GRAPHICS_OPM_HDCP_SRM_NEVER_SET(
		0xC01E0516L,
		"DxgkDdiOPMGetInformation() cannot return the version of the SRM being used because the application " +
		"never successfully passed an SRM to the protected output."),

	/**
	 * DxgkDdiOPMConfigureProtectedOutput() cannot enable the specified output
	 * protection technology because the output's screen resolution is too high.
	 */
	STATUS_GRAPHICS_OPM_RESOLUTION_TOO_HIGH(
		0xC01E0517L,
		"DxgkDdiOPMConfigureProtectedOutput() cannot enable the specified output " +
		"protection technology because the output's screen resolution is too high."),

	/**
	 * DxgkDdiOPMConfigureProtectedOutput() cannot enable HDCP because other
	 * physical outputs are using the display adapter's HDCP hardware.
	 */
	STATUS_GRAPHICS_OPM_ALL_HDCP_HARDWARE_ALREADY_IN_USE(0xC01E0518L,
		"DxgkDdiOPMConfigureProtectedOutput() cannot enable HDCP because other " +
		"physical outputs are using the display adapter's HDCP hardware."),

	/**
	 * The operating system asynchronously destroyed this OPM-protected output
	 * because the operating system state changed. This error typically occurs
	 * because the monitor PDO associated with this protected output was removed
	 * or stopped, the protected output's session became a nonconsole session, or
	 * the protected output's desktop became inactive.
	 */
	STATUS_GRAPHICS_OPM_PROTECTED_OUTPUT_NO_LONGER_EXISTS(
		0xC01E051AL,
		"The operating system asynchronously destroyed this OPM-protected output because the operating " +
		"system state changed. This error typically occurs because the monitor PDO associated with this " +
		"protected output was removed or stopped, the protected output's session became a nonconsole session, " +
		"or the protected output's desktop became inactive."),

	/**
	 * OPM functions cannot be called when a session is changing its type. Three
	 * types of sessions currently exist: console, disconnected, and remote (RDP
	 * or ICA).
	 */
	STATUS_GRAPHICS_OPM_SESSION_TYPE_CHANGE_IN_PROGRESS(
		0xC01E051BL,
		"OPM functions cannot be called when a session is changing its type. Three types of sessions " +
		"currently exist: console, disconnected, and remote (RDP or ICA)."),

	/**
	 * The DxgkDdiOPMGetCOPPCompatibleInformation, DxgkDdiOPMGetInformation, or
	 * DxgkDdiOPMConfigureProtectedOutput function failed. This error is
	 * returned only if a protected output has OPM semantics.
	 * <p>
	 * DxgkDdiOPMGetCOPPCompatibleInformation always returns this error if a
	 * protected output has OPM semantics.
	 * <p>
	 * DxgkDdiOPMGetInformation returns this error code if the caller requested
	 * COPP-specific information.
	 * <p>
	 * DxgkDdiOPMConfigureProtectedOutput returns this error when the caller
	 * tries to use a COPP-specific command.
	 */
	STATUS_GRAPHICS_OPM_PROTECTED_OUTPUT_DOES_NOT_HAVE_COPP_SEMANTICS(
		0xC01E051CL,
		"The DxgkDdiOPMGetCOPPCompatibleInformation, DxgkDdiOPMGetInformation, or DxgkDdiOPMConfigureProtectedOutput " +
		"function failed. This error is returned only if a protected output has OPM semantics.\n\n" +
		"DxgkDdiOPMGetCOPPCompatibleInformation always returns this error if a protected output has OPM semantics.\n\n" +
		"DxgkDdiOPMGetInformation returns this error code if the caller requested COPP-specific information.\n\n" +
		"DxgkDdiOPMConfigureProtectedOutput returns this error when the caller tries to use a COPP-specific command."),

	/**
	 * The DxgkDdiOPMGetInformation and DxgkDdiOPMGetCOPPCompatibleInformation
	 * functions return this error code if the passed-in sequence number is not
	 * the expected sequence number or the passed-in OMAC value is invalid.
	 */
	STATUS_GRAPHICS_OPM_INVALID_INFORMATION_REQUEST(
		0xC01E051DL,
		"The DxgkDdiOPMGetInformation and DxgkDdiOPMGetCOPPCompatibleInformation functions return this error " +
		"code if the passed-in sequence number is not the expected sequence number or the passed-in OMAC value is invalid."),

	/**
	 * The function failed because an unexpected error occurred inside a display
	 * driver.
	 */
	STATUS_GRAPHICS_OPM_DRIVER_INTERNAL_ERROR(0xC01E051EL,
		"The function failed because an unexpected error occurred inside a display driver."),

	/**
	 * The DxgkDdiOPMGetCOPPCompatibleInformation, DxgkDdiOPMGetInformation, or
	 * DxgkDdiOPMConfigureProtectedOutput function failed. This error is
	 * returned only if a protected output has COPP semantics.
	 * <p>
	 * DxgkDdiOPMGetCOPPCompatibleInformation returns this error code if the
	 * caller requested OPM-specific information.
	 * <p>
	 * DxgkDdiOPMGetInformation always returns this error if a protected output
	 * has COPP semantics.
	 * <p>
	 * DxgkDdiOPMConfigureProtectedOutput returns this error when the caller
	 * tries to use an OPM-specific command.
	 */
	STATUS_GRAPHICS_OPM_PROTECTED_OUTPUT_DOES_NOT_HAVE_OPM_SEMANTICS(
		0xC01E051FL,
		"The DxgkDdiOPMGetCOPPCompatibleInformation, DxgkDdiOPMGetInformation, or " +
		"DxgkDdiOPMConfigureProtectedOutput function failed. This error is returned " +
		"only if a protected output has COPP semantics.\n\n" +
		"DxgkDdiOPMGetCOPPCompatibleInformation returns this error code if the caller requested OPM-specific information.\n\n" +
		"DxgkDdiOPMGetInformation always returns this error if a protected output has COPP semantics.\n\n" +
		"DxgkDdiOPMConfigureProtectedOutput returns this error when the caller tries to use an OPM-specific command."),

	/**
	 * The DxgkDdiOPMGetCOPPCompatibleInformation and
	 * DxgkDdiOPMConfigureProtectedOutput functions return this error if the
	 * display driver does not support the
	 * DXGKMDT_OPM_GET_ACP_AND_CGMSA_SIGNALING and
	 * DXGKMDT_OPM_SET_ACP_AND_CGMSA_SIGNALING GUIDs.
	 */
	STATUS_GRAPHICS_OPM_SIGNALING_NOT_SUPPORTED(
		0xC01E0520L,
		"The DxgkDdiOPMGetCOPPCompatibleInformation and DxgkDdiOPMConfigureProtectedOutput functions return this " +
		"error if the display driver does not support the " +
		"DXGKMDT_OPM_GET_ACP_AND_CGMSA_SIGNALING and DXGKMDT_OPM_SET_ACP_AND_CGMSA_SIGNALING GUIDs."),

	/**
	 * The DxgkDdiOPMConfigureProtectedOutput function returns this error code
	 * if the passed-in sequence number is not the expected sequence number or
	 * the passed-in OMAC value is invalid.
	 */
	STATUS_GRAPHICS_OPM_INVALID_CONFIGURATION_REQUEST(
		0xC01E0521L,
		"The DxgkDdiOPMConfigureProtectedOutput function returns this error code if the passed-in sequence " +
		"number is not the expected sequence number or the passed-in OMAC value is invalid."),

	/**
	 * The monitor connected to the specified video output does not have an I2C
	 * bus.
	 */
	STATUS_GRAPHICS_I2C_NOT_SUPPORTED(0xC01E0580L, "The monitor connected to the specified video output does not have an I2C bus."),

	/** No device on the I2C bus has the specified address. */
	STATUS_GRAPHICS_I2C_DEVICE_DOES_NOT_EXIST(0xC01E0581L, "No device on the I2C bus has the specified address."),

	/** An error occurred while transmitting data to the device on the I2C bus. */
	STATUS_GRAPHICS_I2C_ERROR_TRANSMITTING_DATA(0xC01E0582L, "An error occurred while transmitting data to the device on the I2C bus."),

	/** An error occurred while receiving data from the device on the I2C bus. */
	STATUS_GRAPHICS_I2C_ERROR_RECEIVING_DATA(0xC01E0583L, "An error occurred while receiving data from the device on the I2C bus."),

	/** The monitor does not support the specified VCP code. */
	STATUS_GRAPHICS_DDCCI_VCP_NOT_SUPPORTED(0xC01E0584L, "The monitor does not support the specified VCP code."),

	/** The data received from the monitor is invalid. */
	STATUS_GRAPHICS_DDCCI_INVALID_DATA(0xC01E0585L, "The data received from the monitor is invalid."),

	/**
	 * A function call failed because a monitor returned an invalid timing
	 * status byte when the operating system used the DDC/CI get timing report
	 * and timing message command to get a timing report from a monitor.
	 */
	STATUS_GRAPHICS_DDCCI_MONITOR_RETURNED_INVALID_TIMING_STATUS_BYTE(
		0xC01E0586L,
		"A function call failed because a monitor returned an invalid timing status byte when " +
		"the operating system used the DDC/CI get timing report and timing message command to " +
		"get a timing report from a monitor."),

	/**
	 * A monitor returned a DDC/CI capabilities string that did not comply with
	 * the ACCESS.bus 3.0, DDC/CI 1.1, or MCCS 2 Revision 1 specification.
	 */
	STATUS_GRAPHICS_DDCCI_INVALID_CAPABILITIES_STRING(
		0xC01E0587L,
		"A monitor returned a DDC/CI capabilities string that did not comply with the " +
		"ACCESS.bus 3.0, DDC/CI 1.1, or MCCS 2 Revision 1 specification."),

	/** An internal error caused an operation to fail. */
	STATUS_GRAPHICS_MCA_INTERNAL_ERROR(0xC01E0588L, "An internal error caused an operation to fail."),

	/**
	 * An operation failed because a DDC/CI message had an invalid value in its
	 * command field.
	 */
	STATUS_GRAPHICS_DDCCI_INVALID_MESSAGE_COMMAND(0xC01E0589L,
		"An operation failed because a DDC/CI message had an invalid value in its command field."),

	/**
	 * This error occurred because a DDC/CI message had an invalid value in its
	 * length field.
	 */
	STATUS_GRAPHICS_DDCCI_INVALID_MESSAGE_LENGTH(0xC01E058AL,
		"This error occurred because a DDC/CI message had an invalid value in its length field."),

	/**
	 * This error occurred because the value in a DDC/CI message's checksum
	 * field did not match the message's computed checksum value. This error
	 * implies that the data was corrupted while it was being transmitted from a
	 * monitor to a computer.
	 */
	STATUS_GRAPHICS_DDCCI_INVALID_MESSAGE_CHECKSUM(
		0xC01E058BL,
		"This error occurred because the value in a DDC/CI message's checksum field did not match the " +
		"message's computed checksum value. This error implies that the data was corrupted while it was " +
		"being transmitted from a monitor to a computer."),

	/** This function failed because an invalid monitor handle was passed to it. */
	STATUS_GRAPHICS_INVALID_PHYSICAL_MONITOR_HANDLE(0xC01E058CL, "This function failed because an invalid monitor handle was passed to it."),

	/**
	 * The operating system asynchronously destroyed the monitor that
	 * corresponds to this handle because the operating system's state changed.
	 * This error typically occurs because the monitor PDO associated with this
	 * handle was removed or stopped, or a display mode change occurred. A
	 * display mode change occurs when Windows sends a WM_DISPLAYCHANGE message
	 * to applications.
	 */
	STATUS_GRAPHICS_MONITOR_NO_LONGER_EXISTS(
		0xC01E058DL,
		"The operating system asynchronously destroyed the monitor that corresponds to this " +
		"handle because the operating system's state changed. This error typically occurs because " +
		"the monitor PDO associated with this handle was removed or stopped, or a display mode " +
		"change occurred. A display mode change occurs when Windows sends a WM_DISPLAYCHANGE " +
		"message to applications."),

	/**
	 * This function can be used only if a program is running in the local
	 * console session. It cannot be used if a program is running on a remote
	 * desktop session or on a terminal server session.
	 */
	STATUS_GRAPHICS_ONLY_CONSOLE_SESSION_SUPPORTED(
		0xC01E05E0L,
		"This function can be used only if a program is running in the local console session. " +
		"It cannot be used if a program is running on a remote desktop session or on a terminal server session."),

	/**
	 * This function cannot find an actual GDI display device that corresponds
	 * to the specified GDI display device name.
	 */
	STATUS_GRAPHICS_NO_DISPLAY_DEVICE_CORRESPONDS_TO_NAME(0xC01E05E1L,
		"This function cannot find an actual GDI display device that corresponds to the specified GDI display device name."),

	/**
	 * The function failed because the specified GDI display device was not
	 * attached to the Windows desktop.
	 */
	STATUS_GRAPHICS_DISPLAY_DEVICE_NOT_ATTACHED_TO_DESKTOP(0xC01E05E2L,
		"The function failed because the specified GDI display device was not attached to the Windows desktop."),

	/**
	 * This function does not support GDI mirroring display devices because GDI
	 * mirroring display devices do not have any physical monitors associated
	 * with them.
	 */
	STATUS_GRAPHICS_MIRRORING_DEVICES_NOT_SUPPORTED(
		0xC01E05E3L,
		"This function does not support GDI mirroring display devices because GDI mirroring " +
		"display devices do not have any physical monitors associated with them."),

	/**
	 * The function failed because an invalid pointer parameter was passed to
	 * it. A pointer parameter is invalid if it is null, is not correctly
	 * aligned, or points to an invalid address or to a kernel mode address.
	 */
	STATUS_GRAPHICS_INVALID_POINTER(
		0xC01E05E4L,
		"The function failed because an invalid pointer parameter was passed to it. A pointer " +
		"parameter is invalid if it is null, is not correctly aligned, or points to an invalid " +
		"address or to a kernel mode address."),

	/**
	 * This function failed because the GDI device passed to it did not have a
	 * monitor associated with it.
	 */
	STATUS_GRAPHICS_NO_MONITORS_CORRESPOND_TO_DISPLAY_DEVICE(0xC01E05E5L,
		"This function failed because the GDI device passed to it did not have a monitor associated with it."),

	/**
	 * An array passed to the function cannot hold all of the data that the
	 * function must copy into the array.
	 */
	STATUS_GRAPHICS_PARAMETER_ARRAY_TOO_SMALL(0xC01E05E6L,
		"An array passed to the function cannot hold all of the data that the function must copy into the array."),

	/** An internal error caused an operation to fail. */
	STATUS_GRAPHICS_INTERNAL_ERROR(0xC01E05E7L, "An internal error caused an operation to fail."),

	/**
	 * The function failed because the current session is changing its type.
	 * This function cannot be called when the current session is changing its
	 * type. Three types of sessions currently exist: console, disconnected, and
	 * remote (RDP or ICA).
	 */
	STATUS_GRAPHICS_SESSION_TYPE_CHANGE_IN_PROGRESS(
		0xC01E05E8L,
		"The function failed because the current session is changing its type. This function cannot be called " +
		"when the current session is changing its type. Three types of sessions currently exist: console, " +
		"disconnected, and remote (RDP or ICA)."),

	/** The volume must be unlocked before it can be used. */
	STATUS_FVE_LOCKED_VOLUME(0xC0210000L, "The volume must be unlocked before it can be used."),

	/** The volume is fully decrypted and no key is available. */
	STATUS_FVE_NOT_ENCRYPTED(0xC0210001L, "The volume is fully decrypted and no key is available."),

	/** The control block for the encrypted volume is not valid. */
	STATUS_FVE_BAD_INFORMATION(0xC0210002L, "The control block for the encrypted volume is not valid."),

	/** Not enough free space remains on the volume to allow encryption. */
	STATUS_FVE_TOO_SMALL(0xC0210003L, "Not enough free space remains on the volume to allow encryption."),

	/**
	 * The partition cannot be encrypted because the file system is not
	 * supported.
	 */
	STATUS_FVE_FAILED_WRONG_FS(0xC0210004L, "The partition cannot be encrypted because the file system is not supported."),

	/** The file system is inconsistent. Run the Check Disk utility. */
	STATUS_FVE_FAILED_BAD_FS(0xC0210005L, "The file system is inconsistent. Run the Check Disk utility."),

	/** The file system does not extend to the end of the volume. */
	STATUS_FVE_FS_NOT_EXTENDED(0xC0210006L, "The file system does not extend to the end of the volume."),

	/**
	 * This operation cannot be performed while a file system is mounted on the
	 * volume.
	 */
	STATUS_FVE_FS_MOUNTED(0xC0210007L, "This operation cannot be performed while a file system is mounted on the volume."),

	/** BitLocker Drive Encryption is not included with this version of Windows. */
	STATUS_FVE_NO_LICENSE(0xC0210008L, "BitLocker Drive Encryption is not included with this version of Windows."),

	/** The requested action was denied by the FVE control engine. */
	STATUS_FVE_ACTION_NOT_ALLOWED(0xC0210009L, "The requested action was denied by the FVE control engine."),

	/** The data supplied is malformed. */
	STATUS_FVE_BAD_DATA(0xC021000AL, "The data supplied is malformed."),

	/** The volume is not bound to the system. */
	STATUS_FVE_VOLUME_NOT_BOUND(0xC021000BL, "The volume is not bound to the system."),

	/** The volume specified is not a data volume. */
	STATUS_FVE_NOT_DATA_VOLUME(0xC021000CL, "The volume specified is not a data volume."),

	/** A read operation failed while converting the volume. */
	STATUS_FVE_CONV_READ_ERROR(0xC021000DL, "A read operation failed while converting the volume."),

	/** A write operation failed while converting the volume. */
	STATUS_FVE_CONV_WRITE_ERROR(0xC021000EL, "A write operation failed while converting the volume."),

	/**
	 * The control block for the encrypted volume was updated by another thread.
	 * Try again.
	 */
	STATUS_FVE_OVERLAPPED_UPDATE(0xC021000FL, "The control block for the encrypted volume was updated by another thread. Try again."),

	/** The volume encryption algorithm cannot be used on this sector size. */
	STATUS_FVE_FAILED_SECTOR_SIZE(0xC0210010L, "The volume encryption algorithm cannot be used on this sector size."),

	/** BitLocker recovery authentication failed. */
	STATUS_FVE_FAILED_AUTHENTICATION(0xC0210011L, "BitLocker recovery authentication failed."),

	/** The volume specified is not the boot operating system volume. */
	STATUS_FVE_NOT_OS_VOLUME(0xC0210012L, "The volume specified is not the boot operating system volume."),

	/**
	 * The BitLocker startup key or recovery password could not be read from
	 * external media.
	 */
	STATUS_FVE_KEYFILE_NOT_FOUND(0xC0210013L, "The BitLocker startup key or recovery password could not be read from external media."),

	/**
	 * The BitLocker startup key or recovery password file is corrupt or
	 * invalid.
	 */
	STATUS_FVE_KEYFILE_INVALID(0xC0210014L, "The BitLocker startup key or recovery password file is corrupt or invalid."),

	/**
	 * The BitLocker encryption key could not be obtained from the startup key
	 * or the recovery password.
	 */
	STATUS_FVE_KEYFILE_NO_VMK(0xC0210015L,
		"The BitLocker encryption key could not be obtained from the startup key or the recovery password."),

	/** The TPM is disabled. */
	STATUS_FVE_TPM_DISABLED(0xC0210016L, "The TPM is disabled."),

	/** The authorization data for the SRK of the TPM is not zero. */
	STATUS_FVE_TPM_SRK_AUTH_NOT_ZERO(0xC0210017L, "The authorization data for the SRK of the TPM is not zero."),

	/**
	 * The system boot information changed or the TPM locked out access to
	 * BitLocker encryption keys until the computer is restarted.
	 */
	STATUS_FVE_TPM_INVALID_PCR(0xC0210018L,
		"The system boot information changed or the TPM locked out access to BitLocker encryption keys until the computer is restarted."),

	/** The BitLocker encryption key could not be obtained from the TPM. */
	STATUS_FVE_TPM_NO_VMK(0xC0210019L, "The BitLocker encryption key could not be obtained from the TPM."),

	/** The BitLocker encryption key could not be obtained from the TPM and PIN. */
	STATUS_FVE_PIN_INVALID(0xC021001AL, "The BitLocker encryption key could not be obtained from the TPM and PIN."),

	/**
	 * A boot application hash does not match the hash computed when BitLocker
	 * was turned on.
	 */
	STATUS_FVE_AUTH_INVALID_APPLICATION(0xC021001BL,
		"A boot application hash does not match the hash computed when BitLocker was turned on."),

	/**
	 * The Boot Configuration Data (BCD) settings are not supported or have
	 * changed because BitLocker was enabled.
	 */
	STATUS_FVE_AUTH_INVALID_CONFIG(0xC021001CL,
		"The Boot Configuration Data (BCD) settings are not supported or have changed because BitLocker was enabled."),

	/**
	 * Boot debugging is enabled. Run Windows Boot Configuration Data Store
	 * Editor (bcdedit.exe) to turn it off.
	 */
	STATUS_FVE_DEBUGGER_ENABLED(0xC021001DL,
		"Boot debugging is enabled. Run Windows Boot Configuration Data Store Editor (bcdedit.exe) to turn it off."),

	/** The BitLocker encryption key could not be obtained. */
	STATUS_FVE_DRY_RUN_FAILED(0xC021001EL, "The BitLocker encryption key could not be obtained."),

	/** The metadata disk region pointer is incorrect. */
	STATUS_FVE_BAD_METADATA_POINTER(0xC021001FL, "The metadata disk region pointer is incorrect."),

	/** The backup copy of the metadata is out of date. */
	STATUS_FVE_OLD_METADATA_COPY(0xC0210020L, "The backup copy of the metadata is out of date."),

	/** No action was taken because a system restart is required. */
	STATUS_FVE_REBOOT_REQUIRED(0xC0210021L, "No action was taken because a system restart is required."),

	/**
	 * No action was taken because BitLocker Drive Encryption is in RAW access
	 * mode.
	 */
	STATUS_FVE_RAW_ACCESS(0xC0210022L, "No action was taken because BitLocker Drive Encryption is in RAW access mode."),

	/** BitLocker Drive Encryption cannot enter RAW access mode for this volume. */
	STATUS_FVE_RAW_BLOCKED(0xC0210023L, "BitLocker Drive Encryption cannot enter RAW access mode for this volume."),

	/**
	 * This feature of BitLocker Drive Encryption is not included with this
	 * version of Windows.
	 */
	STATUS_FVE_NO_FEATURE_LICENSE(0xC0210026L, "This feature of BitLocker Drive Encryption is not included with this version of Windows."),

	/**
	 * Group policy does not permit turning off BitLocker Drive Encryption on
	 * roaming data volumes.
	 */
	STATUS_FVE_POLICY_USER_DISABLE_RDV_NOT_ALLOWED(0xC0210027L,
		"Group policy does not permit turning off BitLocker Drive Encryption on roaming data volumes."),

	/**
	 * Bitlocker Drive Encryption failed to recover from aborted conversion.
	 * This could be due to either all conversion logs being corrupted or the
	 * media being write-protected.
	 */
	STATUS_FVE_CONV_RECOVERY_FAILED(
		0xC0210028L,
		"Bitlocker Drive Encryption failed to recover from aborted conversion. This could be due to either " +
		"all conversion logs being corrupted or the media being write-protected."),

	/** The requested virtualization size is too big. */
	STATUS_FVE_VIRTUALIZED_SPACE_TOO_BIG(0xC0210029L, "The requested virtualization size is too big."),

	/** The drive is too small to be protected using BitLocker Drive Encryption. */
	STATUS_FVE_VOLUME_TOO_SMALL(0xC0210030L, "The drive is too small to be protected using BitLocker Drive Encryption."),

	/** The callout does not exist. */
	STATUS_FWP_CALLOUT_NOT_FOUND(0xC0220001L, "The callout does not exist."),

	/** The filter condition does not exist. */
	STATUS_FWP_CONDITION_NOT_FOUND(0xC0220002L, "The filter condition does not exist."),

	/** The filter does not exist. */
	STATUS_FWP_FILTER_NOT_FOUND(0xC0220003L, "The filter does not exist."),

	/** The layer does not exist. */
	STATUS_FWP_LAYER_NOT_FOUND(0xC0220004L, "The layer does not exist."),

	/** The provider does not exist. */
	STATUS_FWP_PROVIDER_NOT_FOUND(0xC0220005L, "The provider does not exist."),

	/** The provider context does not exist. */
	STATUS_FWP_PROVIDER_CONTEXT_NOT_FOUND(0xC0220006L, "The provider context does not exist."),

	/** The sublayer does not exist. */
	STATUS_FWP_SUBLAYER_NOT_FOUND(0xC0220007L, "The sublayer does not exist."),

	/** The object does not exist. */
	STATUS_FWP_NOT_FOUND(0xC0220008L, "The object does not exist."),

	/** An object with that GUID or LUID already exists. */
	STATUS_FWP_ALREADY_EXISTS(0xC0220009L, "An object with that GUID or LUID already exists."),

	/** The object is referenced by other objects and cannot be deleted. */
	STATUS_FWP_IN_USE(0xC022000AL, "The object is referenced by other objects and cannot be deleted."),

	/** The call is not allowed from within a dynamic session. */
	STATUS_FWP_DYNAMIC_SESSION_IN_PROGRESS(0xC022000BL, "The call is not allowed from within a dynamic session."),

	/** The call was made from the wrong session and cannot be completed. */
	STATUS_FWP_WRONG_SESSION(0xC022000CL, "The call was made from the wrong session and cannot be completed."),

	/** The call must be made from within an explicit transaction. */
	STATUS_FWP_NO_TXN_IN_PROGRESS(0xC022000DL, "The call must be made from within an explicit transaction."),

	/** The call is not allowed from within an explicit transaction. */
	STATUS_FWP_TXN_IN_PROGRESS(0xC022000EL, "The call is not allowed from within an explicit transaction."),

	/** The explicit transaction has been forcibly canceled. */
	STATUS_FWP_TXN_ABORTED(0xC022000FL, "The explicit transaction has been forcibly canceled."),

	/** The session has been canceled. */
	STATUS_FWP_SESSION_ABORTED(0xC0220010L, "The session has been canceled."),

	/** The call is not allowed from within a read-only transaction. */
	STATUS_FWP_INCOMPATIBLE_TXN(0xC0220011L, "The call is not allowed from within a read-only transaction."),

	/** The call timed out while waiting to acquire the transaction lock. */
	STATUS_FWP_TIMEOUT(0xC0220012L, "The call timed out while waiting to acquire the transaction lock."),

	/** The collection of network diagnostic events is disabled. */
	STATUS_FWP_NET_EVENTS_DISABLED(0xC0220013L, "The collection of network diagnostic events is disabled."),

	/** The operation is not supported by the specified layer. */
	STATUS_FWP_INCOMPATIBLE_LAYER(0xC0220014L, "The operation is not supported by the specified layer."),

	/** The call is allowed for kernel-mode callers only. */
	STATUS_FWP_KM_CLIENTS_ONLY(0xC0220015L, "The call is allowed for kernel-mode callers only."),

	/** The call tried to associate two objects with incompatible lifetimes. */
	STATUS_FWP_LIFETIME_MISMATCH(0xC0220016L, "The call tried to associate two objects with incompatible lifetimes."),

	/** The object is built-in and cannot be deleted. */
	STATUS_FWP_BUILTIN_OBJECT(0xC0220017L, "The object is built-in and cannot be deleted."),

	/** The maximum number of boot-time filters has been reached. */
	STATUS_FWP_TOO_MANY_BOOTTIME_FILTERS(0xC0220018L, "The maximum number of boot-time filters has been reached."),

	/** The maximum number of callouts has been reached. */
	STATUS_FWP_TOO_MANY_CALLOUTS(0xC0220018L, "The maximum number of callouts has been reached."),

	/**
	 * A notification could not be delivered because a message queue has reached
	 * maximum capacity.
	 */
	STATUS_FWP_NOTIFICATION_DROPPED(0xC0220019L,
		"A notification could not be delivered because a message queue has reached maximum capacity."),

	/**
	 * The traffic parameters do not match those for the security association
	 * context.
	 */
	STATUS_FWP_TRAFFIC_MISMATCH(0xC022001AL, "The traffic parameters do not match those for the security association context."),

	/** The call is not allowed for the current security association state. */
	STATUS_FWP_INCOMPATIBLE_SA_STATE(0xC022001BL, "The call is not allowed for the current security association state."),

	/** A required pointer is null. */
	STATUS_FWP_NULL_POINTER(0xC022001CL, "A required pointer is null."),

	/** An enumerator is not valid. */
	STATUS_FWP_INVALID_ENUMERATOR(0xC022001DL, "An enumerator is not valid."),

	/** The flags field contains an invalid value. */
	STATUS_FWP_INVALID_FLAGS(0xC022001EL, "The flags field contains an invalid value."),

	/** A network mask is not valid. */
	STATUS_FWP_INVALID_NET_MASK(0xC022001FL, "A network mask is not valid."),

	/** An FWP_RANGE is not valid. */
	STATUS_FWP_INVALID_RANGE(0xC0220020L, "An FWP_RANGE is not valid."),

	/** The time interval is not valid. */
	STATUS_FWP_INVALID_INTERVAL(0xC0220021L, "The time interval is not valid."),

	/** An array that must contain at least one element has a zero length. */
	STATUS_FWP_ZERO_LENGTH_ARRAY(0xC0220022L, "An array that must contain at least one element has a zero length."),

	/** The displayData.name field cannot be null. */
	STATUS_FWP_NULL_DISPLAY_NAME(0xC0220023L, "The displayData.name field cannot be null."),

	/** The action type is not one of the allowed action types for a filter. */
	STATUS_FWP_INVALID_ACTION_TYPE(0xC0220024L, "The action type is not one of the allowed action types for a filter."),

	/** The filter weight is not valid. */
	STATUS_FWP_INVALID_WEIGHT(0xC0220025L, "The filter weight is not valid."),

	/**
	 * A filter condition contains a match type that is not compatible with the
	 * operands.
	 */
	STATUS_FWP_MATCH_TYPE_MISMATCH(0xC0220026L, "A filter condition contains a match type that is not compatible with the operands."),

	/** An FWP_VALUE or FWPM_CONDITION_VALUE is of the wrong type. */
	STATUS_FWP_TYPE_MISMATCH(0xC0220027L, "An FWP_VALUE or FWPM_CONDITION_VALUE is of the wrong type."),

	/** An integer value is outside the allowed range. */
	STATUS_FWP_OUT_OF_BOUNDS(0xC0220028L, "An integer value is outside the allowed range."),

	/** A reserved field is nonzero. */
	STATUS_FWP_RESERVED(0xC0220029L, "A reserved field is nonzero."),

	/** A filter cannot contain multiple conditions operating on a single field. */
	STATUS_FWP_DUPLICATE_CONDITION(0xC022002AL, "A filter cannot contain multiple conditions operating on a single field."),

	/** A policy cannot contain the same keying module more than once. */
	STATUS_FWP_DUPLICATE_KEYMOD(0xC022002BL, "A policy cannot contain the same keying module more than once."),

	/** The action type is not compatible with the layer. */
	STATUS_FWP_ACTION_INCOMPATIBLE_WITH_LAYER(0xC022002CL, "The action type is not compatible with the layer."),

	/** The action type is not compatible with the sublayer. */
	STATUS_FWP_ACTION_INCOMPATIBLE_WITH_SUBLAYER(0xC022002DL, "The action type is not compatible with the sublayer."),

	/**
	 * The raw context or the provider context is not compatible with the layer.
	 */
	STATUS_FWP_CONTEXT_INCOMPATIBLE_WITH_LAYER(0xC022002EL, "The raw context or the provider context is not compatible with the layer."),

	/**
	 * The raw context or the provider context is not compatible with the
	 * callout.
	 */
	STATUS_FWP_CONTEXT_INCOMPATIBLE_WITH_CALLOUT(0xC022002FL, "The raw context or the provider context is not compatible with the callout."),

	/** The authentication method is not compatible with the policy type. */
	STATUS_FWP_INCOMPATIBLE_AUTH_METHOD(0xC0220030L, "The authentication method is not compatible with the policy type."),

	/** The Diffie-Hellman group is not compatible with the policy type. */
	STATUS_FWP_INCOMPATIBLE_DH_GROUP(0xC0220031L, "The Diffie-Hellman group is not compatible with the policy type."),

	/** An IKE policy cannot contain an Extended Mode policy. */
	STATUS_FWP_EM_NOT_SUPPORTED(0xC0220032L, "An IKE policy cannot contain an Extended Mode policy."),

	/** The enumeration template or subscription will never match any objects. */
	STATUS_FWP_NEVER_MATCH(0xC0220033L, "The enumeration template or subscription will never match any objects."),

	/** The provider context is of the wrong type. */
	STATUS_FWP_PROVIDER_CONTEXT_MISMATCH(0xC0220034L, "The provider context is of the wrong type."),

	/** The parameter is incorrect. */
	STATUS_FWP_INVALID_PARAMETER(0xC0220035L, "The parameter is incorrect."),

	/** The maximum number of sublayers has been reached. */
	STATUS_FWP_TOO_MANY_SUBLAYERS(0xC0220036L, "The maximum number of sublayers has been reached."),

	/** The notification function for a callout returned an error. */
	STATUS_FWP_CALLOUT_NOTIFICATION_FAILED(0xC0220037L, "The notification function for a callout returned an error."),

	/**
	 * The IPsec authentication configuration is not compatible with the
	 * authentication type.
	 */
	STATUS_FWP_INCOMPATIBLE_AUTH_CONFIG(0xC0220038L,
		"The IPsec authentication configuration is not compatible with the authentication type."),

	/** The IPsec cipher configuration is not compatible with the cipher type. */
	STATUS_FWP_INCOMPATIBLE_CIPHER_CONFIG(0xC0220039L, "The IPsec cipher configuration is not compatible with the cipher type."),

	/** A policy cannot contain the same auth method more than once. */
	STATUS_FWP_DUPLICATE_AUTH_METHOD(0xC022003CL, "A policy cannot contain the same auth method more than once."),

	/** The TCP/IP stack is not ready. */
	STATUS_FWP_TCPIP_NOT_READY(0xC0220100L, "The TCP/IP stack is not ready."),

	/** The injection handle is being closed by another thread. */
	STATUS_FWP_INJECT_HANDLE_CLOSING(0xC0220101L, "The injection handle is being closed by another thread."),

	/** The injection handle is stale. */
	STATUS_FWP_INJECT_HANDLE_STALE(0xC0220102L, "The injection handle is stale."),

	/** The classify cannot be pended. */
	STATUS_FWP_CANNOT_PEND(0xC0220103L, "The classify cannot be pended."),

	/** The binding to the network interface is being closed. */
	STATUS_NDIS_CLOSING(0xC0230002L, "The binding to the network interface is being closed."),

	/** An invalid version was specified. */
	STATUS_NDIS_BAD_VERSION(0xC0230004L, "An invalid version was specified."),

	/** An invalid characteristics table was used. */
	STATUS_NDIS_BAD_CHARACTERISTICS(0xC0230005L, "An invalid characteristics table was used."),

	/**
	 * Failed to find the network interface or the network interface is not
	 * ready.
	 */
	STATUS_NDIS_ADAPTER_NOT_FOUND(0xC0230006L, "Failed to find the network interface or the network interface is not ready."),

	/** Failed to open the network interface. */
	STATUS_NDIS_OPEN_FAILED(0xC0230007L, "Failed to open the network interface."),

	/** The network interface has encountered an internal unrecoverable failure. */
	STATUS_NDIS_DEVICE_FAILED(0xC0230008L, "The network interface has encountered an internal unrecoverable failure."),

	/** The multicast list on the network interface is full. */
	STATUS_NDIS_MULTICAST_FULL(0xC0230009L, "The multicast list on the network interface is full."),

	/** An attempt was made to add a duplicate multicast address to the list. */
	STATUS_NDIS_MULTICAST_EXISTS(0xC023000AL, "An attempt was made to add a duplicate multicast address to the list."),

	/** At attempt was made to remove a multicast address that was never added. */
	STATUS_NDIS_MULTICAST_NOT_FOUND(0xC023000BL, "At attempt was made to remove a multicast address that was never added."),

	/** The network interface aborted the request. */
	STATUS_NDIS_REQUEST_ABORTED(0xC023000CL, "The network interface aborted the request."),

	/**
	 * The network interface cannot process the request because it is being
	 * reset.
	 */
	STATUS_NDIS_RESET_IN_PROGRESS(0xC023000DL, "The network interface cannot process the request because it is being reset."),

	/** An attempt was made to send an invalid packet on a network interface. */
	STATUS_NDIS_INVALID_PACKET(0xC023000FL, "An attempt was made to send an invalid packet on a network interface."),

	/** The specified request is not a valid operation for the target device. */
	STATUS_NDIS_INVALID_DEVICE_REQUEST(0xC0230010L, "The specified request is not a valid operation for the target device."),

	/** The network interface is not ready to complete this operation. */
	STATUS_NDIS_ADAPTER_NOT_READY(0xC0230011L, "The network interface is not ready to complete this operation."),

	/** The length of the buffer submitted for this operation is not valid. */
	STATUS_NDIS_INVALID_LENGTH(0xC0230014L, "The length of the buffer submitted for this operation is not valid."),

	/** The data used for this operation is not valid. */
	STATUS_NDIS_INVALID_DATA(0xC0230015L, "The data used for this operation is not valid."),

	/** The length of the submitted buffer for this operation is too small. */
	STATUS_NDIS_BUFFER_TOO_SHORT(0xC0230016L, "The length of the submitted buffer for this operation is too small."),

	/** The network interface does not support this object identifier. */
	STATUS_NDIS_INVALID_OID(0xC0230017L, "The network interface does not support this object identifier."),

	/** The network interface has been removed. */
	STATUS_NDIS_ADAPTER_REMOVED(0xC0230018L, "The network interface has been removed."),

	/** The network interface does not support this media type. */
	STATUS_NDIS_UNSUPPORTED_MEDIA(0xC0230019L, "The network interface does not support this media type."),

	/**
	 * An attempt was made to remove a token ring group address that is in use
	 * by other components.
	 */
	STATUS_NDIS_GROUP_ADDRESS_IN_USE(0xC023001AL,
		"An attempt was made to remove a token ring group address that is in use by other components."),

	/** An attempt was made to map a file that cannot be found. */
	STATUS_NDIS_FILE_NOT_FOUND(0xC023001BL, "An attempt was made to map a file that cannot be found."),

	/** An error occurred while NDIS tried to map the file. */
	STATUS_NDIS_ERROR_READING_FILE(0xC023001CL, "An error occurred while NDIS tried to map the file."),

	/** An attempt was made to map a file that is already mapped. */
	STATUS_NDIS_ALREADY_MAPPED(0xC023001DL, "An attempt was made to map a file that is already mapped."),

	/**
	 * An attempt to allocate a hardware resource failed because the resource is
	 * used by another component.
	 */
	STATUS_NDIS_RESOURCE_CONFLICT(0xC023001EL,
		"An attempt to allocate a hardware resource failed because the resource is used by another component."),

	/**
	 * The I/O operation failed because the network media is disconnected or the
	 * wireless access point is out of range.
	 */
	STATUS_NDIS_MEDIA_DISCONNECTED(0xC023001FL,
		"The I/O operation failed because the network media is disconnected or the wireless access point is out of range."),

	/** The network address used in the request is invalid. */
	STATUS_NDIS_INVALID_ADDRESS(0xC0230022L, "The network address used in the request is invalid."),

	/** The offload operation on the network interface has been paused. */
	STATUS_NDIS_PAUSED(0xC023002AL, "The offload operation on the network interface has been paused."),

	/** The network interface was not found. */
	STATUS_NDIS_INTERFACE_NOT_FOUND(0xC023002BL, "The network interface was not found."),

	/** The revision number specified in the structure is not supported. */
	STATUS_NDIS_UNSUPPORTED_REVISION(0xC023002CL, "The revision number specified in the structure is not supported."),

	/** The specified port does not exist on this network interface. */
	STATUS_NDIS_INVALID_PORT(0xC023002DL, "The specified port does not exist on this network interface."),

	/**
	 * The current state of the specified port on this network interface does
	 * not support the requested operation.
	 */
	STATUS_NDIS_INVALID_PORT_STATE(0xC023002EL,
		"The current state of the specified port on this network interface does not support the requested operation."),

	/** The miniport adapter is in a lower power state. */
	STATUS_NDIS_LOW_POWER_STATE(0xC023002FL, "The miniport adapter is in a lower power state."),

	/** The network interface does not support this request. */
	STATUS_NDIS_NOT_SUPPORTED(0xC02300BBL, "The network interface does not support this request."),

	/** The TCP connection is not offloadable because of a local policy setting. */
	STATUS_NDIS_OFFLOAD_POLICY(0xC023100FL, "The TCP connection is not offloadable because of a local policy setting."),

	/** The TCP connection is not offloadable by the Chimney offload target. */
	STATUS_NDIS_OFFLOAD_CONNECTION_REJECTED(0xC0231012L, "The TCP connection is not offloadable by the Chimney offload target."),

	/** The IP Path object is not in an offloadable state. */
	STATUS_NDIS_OFFLOAD_PATH_REJECTED(0xC0231013L, "The IP Path object is not in an offloadable state."),

	/**
	 * The wireless LAN interface is in auto-configuration mode and does not
	 * support the requested parameter change operation.
	 */
	STATUS_NDIS_DOT11_AUTO_CONFIG_ENABLED(0xC0232000L,
		"The wireless LAN interface is in auto-configuration mode and does not support the requested parameter change operation."),

	/**
	 * The wireless LAN interface is busy and cannot perform the requested
	 * operation.
	 */
	STATUS_NDIS_DOT11_MEDIA_IN_USE(0xC0232001L, "The wireless LAN interface is busy and cannot perform the requested operation."),

	/**
	 * The wireless LAN interface is power down and does not support the
	 * requested operation.
	 */
	STATUS_NDIS_DOT11_POWER_STATE_INVALID(0xC0232002L,
		"The wireless LAN interface is power down and does not support the requested operation."),

	/** The list of wake on LAN patterns is full. */
	STATUS_NDIS_PM_WOL_PATTERN_LIST_FULL(0xC0232003L, "The list of wake on LAN patterns is full."),

	/** The list of low power protocol offloads is full. */
	STATUS_NDIS_PM_PROTOCOL_OFFLOAD_LIST_FULL(0xC0232004L, "The list of low power protocol offloads is full."),

	/** The SPI in the packet does not match a valid IPsec SA. */
	STATUS_IPSEC_BAD_SPI(0xC0360001L, "The SPI in the packet does not match a valid IPsec SA."),

	/** The packet was received on an IPsec SA whose lifetime has expired. */
	STATUS_IPSEC_SA_LIFETIME_EXPIRED(0xC0360002L, "The packet was received on an IPsec SA whose lifetime has expired."),

	/**
	 * The packet was received on an IPsec SA that does not match the packet
	 * characteristics.
	 */
	STATUS_IPSEC_WRONG_SA(0xC0360003L, "The packet was received on an IPsec SA that does not match the packet characteristics."),

	/** The packet sequence number replay check failed. */
	STATUS_IPSEC_REPLAY_CHECK_FAILED(0xC0360004L, "The packet sequence number replay check failed."),

	/** The IPsec header and/or trailer in the packet is invalid. */
	STATUS_IPSEC_INVALID_PACKET(0xC0360005L, "The IPsec header and/or trailer in the packet is invalid."),

	/** The IPsec integrity check failed. */
	STATUS_IPSEC_INTEGRITY_CHECK_FAILED(0xC0360006L, "The IPsec integrity check failed."),

	/** IPsec dropped a clear text packet. */
	STATUS_IPSEC_CLEAR_TEXT_DROP(0xC0360007L, "IPsec dropped a clear text packet."),

	/**
	 * IPsec dropped an incoming ESP packet in authenticated firewall mode. This
	 * drop is benign.
	 */
	STATUS_IPSEC_AUTH_FIREWALL_DROP(0xC0360008L,
		"IPsec dropped an incoming ESP packet in authenticated firewall mode. This drop is benign."),

	/** IPsec dropped a packet due to DOS throttle. */
	STATUS_IPSEC_THROTTLE_DROP(0xC0360009L, "IPsec dropped a packet due to DOS throttle."),

	/** IPsec Dos Protection matched an explicit block rule. */
	STATUS_IPSEC_DOSP_BLOCK(0xC0368000L, "IPsec Dos Protection matched an explicit block rule."),

	/**
	 * IPsec Dos Protection received an IPsec specific multicast packet which is
	 * not allowed.
	 */
	STATUS_IPSEC_DOSP_RECEIVED_MULTICAST(0xC0368001L,
		"IPsec Dos Protection received an IPsec specific multicast packet which is not allowed."),

	/** IPsec Dos Protection received an incorrectly formatted packet. */
	STATUS_IPSEC_DOSP_INVALID_PACKET(0xC0368002L, "IPsec Dos Protection received an incorrectly formatted packet."),

	/** IPsec Dos Protection failed to lookup state. */
	STATUS_IPSEC_DOSP_STATE_LOOKUP_FAILED(0xC0368003L, "IPsec Dos Protection failed to lookup state."),

	/**
	 * IPsec Dos Protection failed to create state because there are already
	 * maximum number of entries allowed by policy.
	 */
	STATUS_IPSEC_DOSP_MAX_ENTRIES(0xC0368004L,
		"IPsec Dos Protection failed to create state because there are already maximum number of entries allowed by policy."),

	/**
	 * IPsec Dos Protection received an IPsec negotiation packet for a keying
	 * module which is not allowed by policy.
	 */
	STATUS_IPSEC_DOSP_KEYMOD_NOT_ALLOWED(0xC0368005L,
		"IPsec Dos Protection received an IPsec negotiation packet for a keying module which is not allowed by policy."),

	/**
	 * IPsec Dos Protection failed to create per internal IP ratelimit queue
	 * because there is already maximum number of queues allowed by policy.
	 */
	STATUS_IPSEC_DOSP_MAX_PER_IP_RATELIMIT_QUEUES(
		0xC0368006L,
		"IPsec Dos Protection failed to create per internal IP ratelimit queue " +
		"because there is already maximum number of queues allowed by policy."),

	/** The system does not support mirrored volumes. */
	STATUS_VOLMGR_MIRROR_NOT_SUPPORTED(0xC038005BL, "The system does not support mirrored volumes."),

	/** The system does not support RAID-5 volumes. */
	STATUS_VOLMGR_RAID5_NOT_SUPPORTED(0xC038005CL, "The system does not support RAID-5 volumes."),

	/** A virtual disk support provider for the specified file was not found. */
	STATUS_VIRTDISK_PROVIDER_NOT_FOUND(0xC03A0014L, "A virtual disk support provider for the specified file was not found."),

	/** The specified disk is not a virtual disk. */
	STATUS_VIRTDISK_NOT_VIRTUAL_DISK(0xC03A0015L, "The specified disk is not a virtual disk."),

	/**
	 * The chain of virtual hard disks is inaccessible. The process has not been
	 * granted access rights to the parent virtual hard disk for the
	 * differencing disk.
	 */
	STATUS_VHD_PARENT_VHD_ACCESS_DENIED(
		0xC03A0016L,
		"The chain of virtual hard disks is inaccessible. The process has not been granted " +
		"access rights to the parent virtual hard disk for the differencing disk."),

	/**
	 * The chain of virtual hard disks is corrupted. There is a mismatch in the
	 * virtual sizes of the parent virtual hard disk and differencing disk.
	 */
	STATUS_VHD_CHILD_PARENT_SIZE_MISMATCH(
		0xC03A0017L,
		"The chain of virtual hard disks is corrupted. There is a mismatch in the " +
		"virtual sizes of the parent virtual hard disk and differencing disk."),

	/**
	 * The chain of virtual hard disks is corrupted. A differencing disk is
	 * indicated in its own parent chain.
	 */
	STATUS_VHD_DIFFERENCING_CHAIN_CYCLE_DETECTED(0xC03A0018L,
		"The chain of virtual hard disks is corrupted. A differencing disk is indicated in its own parent chain."),

	/**
	 * The chain of virtual hard disks is inaccessible. There was an error
	 * opening a virtual hard disk further up the chain.
	 */
	STATUS_VHD_DIFFERENCING_CHAIN_ERROR_IN_PARENT(0xC03A0019L,
		"The chain of virtual hard disks is inaccessible. There was an error opening a virtual hard disk further up the chain.");

	private final long code;
	private final String text;

	private NTStatus(long code, String text) {
		this.code = code;
		this.text = text;
	}

	/**
	 * @return The error code.
	 */
	public long getCode() {
		return code;
	}

	@Override
		public String toString() {
			return super.toString() + ": " + text;
		}

	/**
	 * Returns the {@link NTStatus} that corresponds to the specified error
	 * code, or {@code null} if no match was found.
	 *
	 * @param code the error code to lookup.
	 * @return The corresponding {@link NTStatus} or {@code null}.
	 */
	@Nullable
	public static NTStatus typeOf(int code) {
		return typeOf(code & 0xFFFFFFFFL);
	}

	/**
	 * Returns the {@link NTStatus} that corresponds to the specified error
	 * code, or {@code null} if no match was found.
	 *
	 * @param code the error code to lookup.
	 * @return The corresponding {@link NTStatus} or {@code null}.
	 */
	@Nullable
	public static NTStatus typeOf(long code) {
		for (NTStatus ntStatus : NTStatus.values()) {
			if (ntStatus.code == code) {
				return ntStatus;
			}
		}
		return null;
	}
}
