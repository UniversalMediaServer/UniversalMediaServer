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
 * Maps OS X {@code mach_msg_return_t} integer values to their defined names as
 * defined in {@code messages.h}.
 * <p>
 * Use {@link Message#getValue} to convert an {@link Message} to its integer
 * value. Use {@link Message#typeOf} to convert an integer value to an
 * {@link Message}.
 *
 * It's unclear exactly what system and subsystem values are used for these
 * return codes, so they aren't mapped for automatic conversion by JNA. To check
 * if a return code is one of these, pass the value to
 * {@link DefaultKernReturnT#getErrorCode} and then pass the result to
 * {@link Message#typeOf}.
 */
public enum Message implements KernReturnT {

	/*
	 *  See <mach/error.h> for the format of error codes.
	 *  IPC errors are system 4.  Send errors are subsystem 0;
	 *  receive errors are subsystem 1.  The code field is always non-zero.
	 *  The high bits of the code field communicate extra information
	 *  for some error codes.  MACH_MSG_MASK masks off these special bits.
	 */

	/** No room in IPC name space for another capability name. */
	MACH_MSG_IPC_SPACE(0x00002000),

	/** No room in VM address space for out-of-line memory. */
	MACH_MSG_VM_SPACE(0x00001000),

	/** Kernel resource shortage handling an IPC capability. */
	MACH_MSG_IPC_KERNEL(0x00000800),

	/** Kernel resource shortage handling out-of-line memory. */
	MACH_MSG_VM_KERNEL(0x00000400),

	/** Thread is waiting to send.  (Internal use only.) */
	MACH_SEND_IN_PROGRESS(0x10000001),

	/** Bogus in-line data. */
	MACH_SEND_INVALID_DATA(0x10000002),

	/** Bogus destination port. */
	MACH_SEND_INVALID_DEST(0x10000003),

	/** Message not sent before timeout expired. */
	MACH_SEND_TIMED_OUT(0x10000004),

	/** Bogus voucher port. */
	MACH_SEND_INVALID_VOUCHER(0x10000005),

	/** Software interrupt. */
	MACH_SEND_INTERRUPTED(0x10000007),

	/** Data doesn't contain a complete message. */
	MACH_SEND_MSG_TOO_SMALL(0x10000008),

	/** Bogus reply port. */
	MACH_SEND_INVALID_REPLY(0x10000009),

	/** Bogus port rights in the message body. */
	MACH_SEND_INVALID_RIGHT(0x1000000a),

	/** Bogus notify port argument. */
	MACH_SEND_INVALID_NOTIFY(0x1000000b),

	/** Invalid out-of-line memory pointer. */
	MACH_SEND_INVALID_MEMORY(0x1000000c),

	/** No message buffer is available. */
	MACH_SEND_NO_BUFFER(0x1000000d),

	/** Send is too large for port. */
	MACH_SEND_TOO_LARGE(0x1000000e),

	/** Invalid msg-type specification. */
	MACH_SEND_INVALID_TYPE(0x1000000f),

	/** A field in the header had a bad value. */
	MACH_SEND_INVALID_HEADER(0x10000010),

	/** The trailer to be sent does not match kernel format. */
	MACH_SEND_INVALID_TRAILER(0x10000011),

	/** Compatibility: No longer a returned error. */
	MACH_SEND_INVALID_RT_OOL_SIZE(0x10000015),

	/** Thread is waiting for receive.  (Internal use only.) */
	MACH_RCV_IN_PROGRESS(0x10004001),

	/** Bogus name for receive port/port-set. */
	MACH_RCV_INVALID_NAME(0x10004002),

	/** Didn't get a message within the timeout value. */
	MACH_RCV_TIMED_OUT(0x10004003),

	/** Message buffer is not large enough for inline data. */
	MACH_RCV_TOO_LARGE(0x10004004),

	/** Software interrupt. */
	MACH_RCV_INTERRUPTED(0x10004005),

	/** Compatibility: No longer a returned error. */
	MACH_RCV_PORT_CHANGED(0x10004006),

	/** Bogus notify port argument. */
	MACH_RCV_INVALID_NOTIFY(0x10004007),

	/** Bogus message buffer for inline data. */
	MACH_RCV_INVALID_DATA(0x10004008),

	/** Port/set was sent away/died during receive. */
	MACH_RCV_PORT_DIED(0x10004009),

	/** Compatibility: No longer a returned error. */
	MACH_RCV_IN_SET(0x1000400a),

	/** Error receiving message header.  See special bits. */
	MACH_RCV_HEADER_ERROR(0x1000400b),

	/** Error receiving message body.  See special bits. */
	MACH_RCV_BODY_ERROR(0x1000400c),

	/** Invalid msg-type specification in scatter list. */
	MACH_RCV_INVALID_TYPE(0x1000400d),

	/** Out-of-line overwrite region is not large enough. */
	MACH_RCV_SCATTER_SMALL(0x1000400e),

	/** trailer type or number of trailer elements not supported. */
	MACH_RCV_INVALID_TRAILER(0x1000400f),

	/** Waiting for receive with timeout (Internal use only). */
	MACH_RCV_IN_PROGRESS_TIMED(0x10004011);

	/** The mask for isolating the basic error code */
	public static final int	MACH_MSG_MASK = 0x00003e00;

	private final int value;

	private Message(int value) {
		this.value = value;
	}

	/**
	 * @return The integer value of this {@link Message}.
	 */
	@Override
	public int getValue() {
		return value;
	}

	/**
	 * @param msgReturn the {@code mach_msg_return_t} integer value.
	 * @return The corresponding {@link Message}.
	 */
	public static Message typeOf(int msgReturn) {
		for (Message entry : Message.values()) {
			if (entry.getValue() == msgReturn) {
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
