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

import net.pms.util.jna.macos.kernreturn.DefaultKernReturnT.Subsystem;


/**
 * Maps OS X {@code IOReturn} integer values to their defined names as defined
 * in <a href=
 * "http://web.archive.org/web/20170320182249/https://opensource.apple.com/source/xnu/xnu-201/iokit/IOKit/IOReturn.h"
 * >IOReturn.h</a>
 * <p>
 * Use {@link IOReturn#getValue} to convert an {@link IOReturn} to its integer
 * value. Use {@link IOReturn#typeOf} to convert an integer value to an
 * {@link IOReturn}.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum IOReturn implements KernReturnT {

	/** General error. */
	kIOReturnError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2bc),

	/** Can't allocate memory. */
	kIOReturnNoMemory(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2bd),

	/** Resource shortage. */
	kIOReturnNoResources(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2be),

	/** Error during IPC. */
	kIOReturnIPCError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2bf),

	/** No such device. */
	kIOReturnNoDevice(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c0),

	/** Privilege violation. */
	kIOReturnNotPrivileged(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c1),

	/** Invalid argument. */
	kIOReturnBadArgument(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c2),

	/** Device read locked. */
	kIOReturnLockedRead(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c3),

	/** Device write locked. */
	kIOReturnLockedWrite(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c4),

	/** Exclusive access and. */
	kIOReturnExclusiveAccess(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c5),

	/** Sent/received messages. */
	kIOReturnBadMessageID(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c6),

	/** Unsupported function. */
	kIOReturnUnsupported(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c7),

	/** Misc. VM failure. */
	kIOReturnVMError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c8),

	/** Internal error. */
	kIOReturnInternalError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2c9),

	/** General I/O error. */
	kIOReturnIOError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2ca),

	/** Can't acquire lock. */
	kIOReturnCannotLock(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2cc),

	/** Device not open. */
	kIOReturnNotOpen(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2cd),

	/** Read not supported. */
	kIOReturnNotReadable(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2ce),

	/** Write not supported. */
	kIOReturnNotWritable(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2cf),

	/** Alignment error. */
	kIOReturnNotAligned(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d0),

	/** Media Error. */
	kIOReturnBadMedia(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d1),

	/** Device(s) still open. */
	kIOReturnStillOpen(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d2),

	/** Rld failure. */
	kIOReturnRLDError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d3),

	/** DMA failure. */
	kIOReturnDMAError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d4),

	/** Device Busy. */
	kIOReturnBusy(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d5),

	/** I/O Timeout. */
	kIOReturnTimeout(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d6),

	/** Device offline. */
	kIOReturnOffline(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d7),

	/** Not ready. */
	kIOReturnNotReady(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d8),

	/** Device not attached. */
	kIOReturnNotAttached(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2d9),

	/** No DMA channels left. */
	kIOReturnNoChannels(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2da),

	/** No space for data. */
	kIOReturnNoSpace(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2db),

	/** Port already exists. */
	kIOReturnPortExists(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2dd),

	/** Can't wire down. */
	kIOReturnCannotWire(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2de),

	/** No interrupt attached. */
	kIOReturnNoInterrupt(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2df),

	/** No DMA frames enqueued. */
	kIOReturnNoFrames(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e0),

	/** Oversized msg received. */
	kIOReturnMessageTooLarge(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e1),

	/** Not permitted. */
	kIOReturnNotPermitted(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e2),

	/** No power to device. */
	kIOReturnNoPower(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e3),

	/** Media not present. */
	kIOReturnNoMedia(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e4),

	/** Media not formatted. */
	kIOReturnUnformattedMedia(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e5),

	/** No such mode. */
	kIOReturnUnsupportedMode(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e6),

	/** Data underrun. */
	kIOReturnUnderrun(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e7),

	/** Data overrun. */
	kIOReturnOverrun(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e8),

	/** The device is not working properly! */
	kIOReturnDeviceError(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2e9),

	/** A completion routine is required. */
	kIOReturnNoCompletion(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2ea),

	/** Operation aborted. */
	kIOReturnAborted(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2eb),

	/** Bus bandwidth would be exceeded. */
	kIOReturnNoBandwidth(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2ec),

	/** Device not responding. */
	kIOReturnNotResponding(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2ed),

	/** Isochronous I/O request for distant past! */
	kIOReturnIsoTooOld(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2ee),

	/** Isochronous I/O request for distant future. */
	kIOReturnIsoTooNew(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2ef),

	/** Data was not found. */
	kIOReturnNotFound(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x2f0),

	/** Should never be seen. */
	kIOReturnInvalid(
		DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.sys_iokit) |
		DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common) |
		0x1);

	private final int value;

	private IOReturn(int value) {
		this.value = value;
	}

	/**
	 * @return The integer value of this {@link IOReturn}.
	 */
	@Override
	public int getValue() {
		return value;
	}

	/**
	 * @param ioReturn the {@code IOReturn} integer value.
	 * @return The corresponding {@link IOReturn}.
	 */
	public static IOReturn typeOf(int ioReturn) {
		for (IOReturn entry : IOReturn.values()) {
			if (entry.getValue() == ioReturn) {
				return entry;
			}
		}
		return null;
	}

	/**
	 * Maps the OS X {@code Subsystem} part of a {@link KernReturnT} error to
	 * their integer values.
	 * <p>
	 * Use {@link IOReturnSubsystem#getValue} to convert an {@link IOReturnSubsystem} to its
	 * integer value. Use {@link IOReturnSubsystem#typeOf} to convert an integer value
	 * to an {@link IOReturnSubsystem}.
	 *
	 * @author Nadahar
	 */
	public enum IOReturnSubsystem implements Subsystem {

		sub_iokit_common(0),
		sub_iokit_usb(1),
		sub_iokit_firewire(2),
		sub_iokit_block_storage(4),
		sub_iokit_graphics(5),
		sub_iokit_networking(6),
		sub_iokit_bluetooth(8),
		sub_iokit_pmu(9),
		sub_iokit_acpi(10),
		sub_iokit_smbus(11),
		sub_iokit_ahci(12),
		sub_iokit_powermanagement(13),
		sub_iokit_hidsystem(14),
		sub_iokit_scsi(16),
		sub_iokit_usbaudio(17),
		sub_iokit_thunderbolt(29),
		sub_iokit_platform(0x2A),
		sub_iokit_audio_video(0x45),
		sub_iokit_baseband(0x80),
		sub_iokit_HDA(254),
		sub_iokit_hsic(0x147),
		sub_iokit_sdio(0x174),
		sub_iokit_wlan(0x208),
		sub_iokit_vendor_specific(-2),
		sub_iokit_reserved(-1);

		private final int value;

		private IOReturnSubsystem(int value) {
			this.value = value;
		}

		/**
		 * @return The subsystem error code value of this {@link IOReturnSubsystem}.
		 */
		@Override
		public int getValue() {
			return value;
		}

		/**
		 * @param subSystemCode the {@code Subsystem} integer value.
		 * @return The corresponding {@link IOReturnSubsystem}.
		 */
		public static Subsystem typeOf(int subSystemCode) {
			for (IOReturnSubsystem entry : IOReturnSubsystem.values()) {
				if (entry.getValue() == subSystemCode) {
					return entry;
				}
			}
			return null;
		}
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
