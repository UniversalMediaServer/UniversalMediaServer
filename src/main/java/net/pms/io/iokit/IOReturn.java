package net.pms.io.iokit;

/**
 * Maps OS X {@code IOReturn} integer values to their defined names as defined
 * in <a href=
 * "http://web.archive.org/web/20170320182249/https://opensource.apple.com/source/xnu/xnu-201/iokit/IOKit/IOReturn.h"
 * >IOReturn.h</a>
 * <p>
 * Use {@link IOReturn#getValue()} to convert an {@link IOReturn} to its integer
 * value. Use {@link IOReturn#typeOf(int)} to convert an integer value to an
 * {@link IOReturn}.
 *
 * @author Nadahar
 */
public enum IOReturn {

	/** OK */
	kIOReturnSuccess(0),

	/** general error */
	kIOReturnError(0x2bc),

	/** can't allocate memory */
	kIOReturnNoMemory(0x2bd),

	/** resource shortage */
	kIOReturnNoResources(0x2be),

	/** error during IPC */
	kIOReturnIPCError(0x2bf),

	/** no such device */
	kIOReturnNoDevice(0x2c0),

	/** privilege violation */
	kIOReturnNotPrivileged(0x2c1),

	/** invalid argument */
	kIOReturnBadArgument(0x2c2),

	/** device read locked */
	kIOReturnLockedRead(0x2c3),

	/** device write locked */
	kIOReturnLockedWrite(0x2c4),

	/** exclusive access and */
	kIOReturnExclusiveAccess(0x2c5),

	/** sent/received messages */
	kIOReturnBadMessageID(0x2c6),

	/** unsupported function */
	kIOReturnUnsupported(0x2c7),

	/** misc. VM failure */
	kIOReturnVMError(0x2c8),

	/** internal error */
	kIOReturnInternalError(0x2c9),

	/** General I/O error */
	kIOReturnIOError(0x2ca),

	/** can't acquire lock */
	kIOReturnCannotLock(0x2cc),

	/** device not open */
	kIOReturnNotOpen(0x2cd),

	/** read not supported */
	kIOReturnNotReadable(0x2ce),

	/** write not supported */
	kIOReturnNotWritable(0x2cf),

	/** alignment error */
	kIOReturnNotAligned(0x2d0),

	/** Media Error */
	kIOReturnBadMedia(0x2d1),

	/** device(s) still open */
	kIOReturnStillOpen(0x2d2),

	/** rld failure */
	kIOReturnRLDError(0x2d3),

	/** DMA failure */
	kIOReturnDMAError(0x2d4),

	/** Device Busy */
	kIOReturnBusy(0x2d5),

	/** I/O Timeout */
	kIOReturnTimeout(0x2d6),

	/** device offline */
	kIOReturnOffline(0x2d7),

	/** not ready */
	kIOReturnNotReady(0x2d8),

	/** device not attached */
	kIOReturnNotAttached(0x2d9),

	/** no DMA channels left */
	kIOReturnNoChannels(0x2da),

	/** no space for data */
	kIOReturnNoSpace(0x2db),

	/** port already exists */
	kIOReturnPortExists(0x2dd),

	/** can't wire down */
	kIOReturnCannotWire(0x2de),

	/** no interrupt attached */
	kIOReturnNoInterrupt(0x2df),

	/** no DMA frames enqueued */
	kIOReturnNoFrames(0x2e0),

	/** oversized msg received */
	kIOReturnMessageTooLarge(0x2e1),

	/** not permitted */
	kIOReturnNotPermitted(0x2e2),

	/** no power to device */
	kIOReturnNoPower(0x2e3),

	/** media not present */
	kIOReturnNoMedia(0x2e4),

	/** media not formatted */
	kIOReturnUnformattedMedia(0x2e5),

	/** no such mode */
	kIOReturnUnsupportedMode(0x2e6),

	/** data underrun */
	kIOReturnUnderrun(0x2e7),

	/** data overrun */
	kIOReturnOverrun(0x2e8),

	/** the device is not working properly! */
	kIOReturnDeviceError(0x2e9),

	/** a completion routine is required */
	kIOReturnNoCompletion(0x2ea),

	/** operation aborted */
	kIOReturnAborted(0x2eb),

	/** bus bandwidth would be exceeded */
	kIOReturnNoBandwidth(0x2ec),

	/** device not responding */
	kIOReturnNotResponding(0x2ed),

	/** isochronous I/O request for distant past! */
	kIOReturnIsoTooOld(0x2ee),

	/** isochronous I/O request for distant future */
	kIOReturnIsoTooNew(0x2ef),

	/** data was not found */
	kIOReturnNotFound(0x2f0),

	/** should never be seen */
	kIOReturnInvalid(0x1),

	/** Not defined */
	UNKNOWN(Integer.MIN_VALUE);

	private final int value;

	private IOReturn(int value) {
		this.value = value;
	}

	/**
	 * @param the {@code IOReturn} integer value.
	 * @return The corresponding {@link IOReturn}.
	 */
	public static IOReturn typeOf(int ioReturn) {
		for (IOReturn entry : IOReturn.values()) {
			if (entry.getValue() == ioReturn) {
				return entry;
			}
		}
		return UNKNOWN;
	}
	/**
	 * @return The integer value of this {@link IOReturn}.
	 */
	public int getValue() {
		return value;
	}
}
