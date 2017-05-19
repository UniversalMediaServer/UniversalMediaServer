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


/**
 * Maps OS X {@code kern_return} integer values to their defined names as
 * defined in {@code kern_return.h}.
 * <p>
 * Use {@link KernReturn#getValue} to convert an {@link KernReturn} to its
 * integer value. Use {@link KernReturn#typeOf} to convert an integer value to
 * an {@link KernReturn}.
 *
 * @author Nadahar
 */
public enum KernReturn implements KernReturnT {

	/** Specified address is not currently valid. */
	KERN_INVALID_ADDRESS(1),

	/**
	 * Specified memory is valid, but does not permit the required forms of
	 * access.
	 */
	KERN_PROTECTION_FAILURE(2),

	/**
	 * The address range specified is already in use, or no address range of the
	 * size specified could be found.
	 */
	KERN_NO_SPACE(3),

	/**
	 * The function requested was not applicable to this type of argument, or an
	 * argument is invalid.
	 */
	KERN_INVALID_ARGUMENT(4),

	/** The function could not be performed. A catch-all. */
	KERN_FAILURE(5),

	/**
	 * A system resource could not be allocated to fulfill this request. This
	 * failure may not be permanent.
	 */
	KERN_RESOURCE_SHORTAGE(6),

	/**
	 * The task in question does not hold receive rights for the port argument.
	 */
	KERN_NOT_RECEIVER(7),

	/** Bogus access restriction. */
	KERN_NO_ACCESS(8),

	/**
	 * During a page fault, the target address refers to a memory object that
	 * has been destroyed. This failure is permanent.
	 */
	KERN_MEMORY_FAILURE(9),

	/**
	 * During a page fault, the memory object indicated that the data could not
	 * be returned. This failure may be temporary; future attempts to access
	 * this same data may succeed, as defined by the memory object.
	 */
	KERN_MEMORY_ERROR(10),

	/** The receive right is already a member of the portset. */
	KERN_ALREADY_IN_SET(11),

	/** The receive right is not a member of a port set. */
	KERN_NOT_IN_SET(12),

	/** The name already denotes a right in the task. */
	KERN_NAME_EXISTS(13),

	/**
	 * The operation was aborted. Ipc code will catch this and reflect it as a
	 * message error.
	 */
	KERN_ABORTED(14),

	/** The name doesn't denote a right in the task. */
	KERN_INVALID_NAME(15),

	/** Target task isn't an active task. */
	KERN_INVALID_TASK(16),

	/** The name denotes a right, but not an appropriate right. */
	KERN_INVALID_RIGHT(17),

	/** A blatant range error. */
	KERN_INVALID_VALUE(18),

	/** Operation would overflow limit on user-references. */
	KERN_UREFS_OVERFLOW(19),

	/** The supplied (port) capability is improper. */
	KERN_INVALID_CAPABILITY(20),

	/**
	 * The task already has send or receive rights for the port under another
	 * name.
	 */
	KERN_RIGHT_EXISTS(21),

	/** Target host isn't actually a host. */
	KERN_INVALID_HOST(22),

	/**
	 * An attempt was made to supply "precious" data for memory that is already
	 * present in a memory object.
	 */
	KERN_MEMORY_PRESENT(23),

	/**
	 * A page was requested of a memory manager via memory_object_data_request
	 * for an object using a {@code MEMORY_OBJECT_COPY_CALL} strategy, with the
	 * {@code VM_PROT_WANTS_COPY} flag being used to specify that the page
	 * desired is for a copy of the object, and the memory manager has detected
	 * the page was pushed into a copy of the object while the kernel was
	 * walking the shadow chain from the copy to the object. This error code is
	 * delivered via {@code memory_object_data_error} and is handled by the
	 * kernel (it forces the kernel to restart the fault). It will not be seen
	 * by users.
	 */
	KERN_MEMORY_DATA_MOVED(24),

	/**
	 * A strategic copy was attempted of an object upon which a quicker copy is
	 * now possible. The caller should retry the copy using
	 * {@code vm_object_copy_quickly}. This error code is seen only by the
	 * kernel.
	 */
	KERN_MEMORY_RESTART_COPY(25),

	/**
	 * An argument applied to assert processor set privilege was not a processor
	 * set control port.
	 */
	KERN_INVALID_PROCESSOR_SET(26),

	/** The specified scheduling attributes exceed the thread's limits. */
	KERN_POLICY_LIMIT(27),

	/**
	 * The specified scheduling policy is not currently enabled for the
	 * processor set.
	 */
	KERN_INVALID_POLICY(28),

	/** The external memory manager failed to initialize the memory object. */
	KERN_INVALID_OBJECT(29),

	/**
	 * A thread is attempting to wait for an event for which there is already a
	 * waiting thread.
	 */
	KERN_ALREADY_WAITING(30),

	/** An attempt was made to destroy the default processor set. */
	KERN_DEFAULT_SET(31),

	/**
	 * An attempt was made to fetch an exception port that is protected, or to
	 * abort a thread while processing a protected exception.
	 */
	KERN_EXCEPTION_PROTECTED(32),

	/** A ledger was required but not supplied. */
	KERN_INVALID_LEDGER(33),

	/** The port was not a memory cache control port. */
	KERN_INVALID_MEMORY_CONTROL(34),

	/**
	 * An argument supplied to assert security privilege was not a host security
	 * port.
	 */
	KERN_INVALID_SECURITY(35),

	/**
	 * thread_depress_abort was called on a thread which was not currently
	 * depressed.
	 */
	KERN_NOT_DEPRESSED(36),

	/** Object has been terminated and is no longer available. */
	KERN_TERMINATED(37),

	/** Lock set has been destroyed and is no longer available. */
	KERN_LOCK_SET_DESTROYED(38),

	/** The thread holding the lock terminated before releasing the lock. */
	KERN_LOCK_UNSTABLE(39),

	/** The lock is already owned by another thread. */
	KERN_LOCK_OWNED(40),

	/** The lock is already owned by the calling thread. */
	KERN_LOCK_OWNED_SELF(41),

	/** Semaphore has been destroyed and is no longer available. */
	KERN_SEMAPHORE_DESTROYED(42),

	/**
	 * Return from RPC indicating the target server was terminated before it
	 * successfully replied.
	 */
	KERN_RPC_SERVER_TERMINATED(43),

	/** Terminate an orphaned activation. */
	KERN_RPC_TERMINATE_ORPHAN(44),

	/** Allow an orphaned activation to continue executing. */
	KERN_RPC_CONTINUE_ORPHAN(45),

	/** Empty thread activation (No thread linked to it). */
	KERN_NOT_SUPPORTED(46),

	/** Remote node down or inaccessible. */
	KERN_NODE_DOWN(47),

	/** A signalled thread was not actually waiting. */
	KERN_NOT_WAITING(48),

	/** Some thread-oriented operation (semaphore_wait) timed out. */
	KERN_OPERATION_TIMED_OUT(49),

	/**
	 * During a page fault, indicates that the page was rejected as a result of
	 * a signature check.
	 */
	KERN_CODESIGN_ERROR(50),

	/** The requested property cannot be changed at this time. */
	KERN_POLICY_STATIC(51),

	/** The provided buffer is of insufficient size for the requested data. */
	KERN_INSUFFICIENT_BUFFER_SIZE(52),

	/** Maximum return value allowable. */
	KERN_RETURN_MAX(0x100);

	private final int value;

	private KernReturn(int value) {
		this.value = value;
	}

	/**
	 * @return The integer value of this {@link KernReturn}.
	 */
	@Override
	public int getValue() {
		return value;
	}

	/**
	 * @param kernReturn the {@code KernReturn} integer value.
	 * @return The corresponding {@link KernReturn}.
	 */
	public static KernReturn typeOf(int kernReturn) {
		for (KernReturn entry : KernReturn.values()) {
			if (entry.getValue() == kernReturn) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return toStandardString();
	}

	@Override
	public String toStandardString() {
		return super.toString() + " (0x" +  Long.toHexString(value & 0xFFFFFFFFL) + ")";
	}
}
