package net.pms.util.jna.macos.kernreturn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import net.pms.util.jna.macos.kernreturn.DefaultKernReturnT.DefaultSubsystem;
import net.pms.util.jna.macos.kernreturn.IOReturn.IOReturnSubsystem;

/**
 * Tests the {@link KernReturnT} interface.
 *
 * @author Nadahar
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KernReturnTest {

	/**
	 * Tests {@link IOReturn}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testIOReturn() throws Throwable {

		assertEquals(56, DefaultKernReturnT.getSystemCode(IOReturn.kIOReturnUnsupportedMode));
		assertEquals(56, DefaultKernReturnT.getSystemCode(IOReturn.kIOReturnNotReadable));
		assertEquals(56, DefaultKernReturnT.getSystemCode(IOReturn.kIOReturnPortExists));
		assertEquals(DefaultKernReturnT.System.sys_iokit, DefaultKernReturnT.getSystem(IOReturn.kIOReturnExclusiveAccess));
		assertEquals(IOReturnSubsystem.sub_iokit_common.getValue(), DefaultKernReturnT.getSubsystemCode(IOReturn.kIOReturnPortExists));
		assertEquals(IOReturnSubsystem.sub_iokit_common.getValue(), DefaultKernReturnT.getSubsystemCode(IOReturn.kIOReturnBadMedia));
		assertEquals(IOReturnSubsystem.sub_iokit_common, DefaultKernReturnT.getSubsystem(IOReturn.kIOReturnNotReadable));
		assertEquals(IOReturnSubsystem.sub_iokit_common, DefaultKernReturnT.getSubsystem(IOReturn.kIOReturnUnformattedMedia));
		assertEquals(0, DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_common));
		assertEquals(0x74000, DefaultKernReturnT.convertToSubsystem(IOReturnSubsystem.sub_iokit_thunderbolt));
		assertEquals("kIOReturnVMError (0xe00002c8)", IOReturn.kIOReturnVMError.toString());
		assertEquals("kIOReturnBadMessageID (0xe00002c6)", IOReturn.kIOReturnBadMessageID.toStandardString());
		assertNull(IOReturn.typeOf(' '));
		assertNull(IOReturn.typeOf(100));
		assertEquals(IOReturn.kIOReturnInternalError, IOReturn.typeOf(-536870199));
		assertEquals(IOReturn.kIOReturnBadMessageID, IOReturn.typeOf(0xE00002C6));
	}

	/**
	 * Tests {@link DefaultKernReturnT}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testDefaultKernReturnT() throws Throwable {
		assertEquals(1589248, DefaultKernReturnT.convertToSubsystem('a'));
		assertEquals(0x28000000, DefaultKernReturnT.convertToSystem(10));
		assertEquals("Success (0)", DefaultKernReturnT.getDefaultKernReturnTFromCode(0).toString());
		assertEquals(10, DefaultKernReturnT.getErrorCode(10));
		assertEquals(7, DefaultKernReturnT.getErrorCode(7));
		assertEquals(97, DefaultKernReturnT.getSubsystemCode(1589248));
		assertEquals(0, DefaultKernReturnT.getSubsystemCode(268435457));
		assertEquals(0, DefaultKernReturnT.getSubsystemCode(10));
		assertEquals(0, DefaultKernReturnT.getSystemCode(0));
		assertEquals(0, DefaultKernReturnT.getSystemCode(1589248));
		assertEquals(DefaultKernReturnT.class, DefaultKernReturnT.typeOf(100).getClass());
		assertEquals(-1, DefaultKernReturnT.typeOf(-1).getValue());
		assertEquals(7, DefaultKernReturnT.System.err_dipc.getValue());
		assertNull(DefaultKernReturnT.DefaultSubsystem.typeOf(100));
		assertEquals(49152, DefaultKernReturnT.convertToSubsystem(DefaultKernReturnT.DefaultSubsystem.unix_err));
		assertEquals(DefaultKernReturnT.class, DefaultKernReturnT.getDefaultKernReturnTFromCode(4).getClass());
		assertEquals(4, DefaultKernReturnT.getDefaultKernReturnTFromCode(4).getValue());
		assertEquals(0, DefaultKernReturnT.getDefaultKernReturnTFromCode(0).getValue());
		assertEquals(12, DefaultKernReturnT.getDefaultKernReturnTFromCode(12).getValue());
		assertEquals(52, DefaultKernReturnT.getErrorCode(KernReturn.KERN_INSUFFICIENT_BUFFER_SIZE));
		assertEquals(0, DefaultKernReturnT.getSystem(KernReturn.KERN_LOCK_OWNED).getValue());
		assertEquals(3, DefaultKernReturnT.DefaultSubsystem.unix_err.getValue());
		assertEquals(2, DefaultKernReturnT.System.err_server.getValue());
		assertEquals(201326592, DefaultKernReturnT.convertToSystem(DefaultKernReturnT.System.err_ipc));
		assertEquals(DefaultKernReturnT.DefaultSubsystem.none, DefaultKernReturnT.DefaultSubsystem.typeOf(0));
		assertEquals(12, DefaultKernReturnT.getErrorCode(KernReturn.KERN_NOT_IN_SET));
		assertEquals("Success (0)", DefaultKernReturnT.getDefaultKernReturnTFromCode(0).toString());
		assertEquals("Success (0)", DefaultKernReturnT.none.toStandardString());
		assertEquals(0, DefaultKernReturnT.getSystemCode(KernReturn.KERN_ALREADY_IN_SET));
		assertEquals("Success (0)", DefaultKernReturnT.SUCCESS.toStandardString());
		assertNull(DefaultKernReturnT.DefaultSubsystem.typeOf(-1));
	}

	/**
	 * Tests {@link KernReturn}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testKernReturn() throws Throwable {
		assertEquals("KERN_ALREADY_IN_SET (0xb)", KernReturn.KERN_ALREADY_IN_SET.toStandardString());
		assertNull(KernReturn.typeOf(-1));
		assertEquals("KERN_MEMORY_PRESENT (0x17)", KernReturn.KERN_MEMORY_PRESENT.toString());
		assertEquals("KERN_LOCK_SET_DESTROYED (0x26)", KernReturn.KERN_LOCK_SET_DESTROYED.toString());
		assertEquals("KERN_INVALID_ARGUMENT (0x4)", KernReturn.KERN_INVALID_ARGUMENT.toString());
		assertEquals("KERN_LOCK_OWNED_SELF (0x29)", KernReturn.KERN_LOCK_OWNED_SELF.toString());
		assertEquals(31, KernReturn.KERN_DEFAULT_SET.getValue());
		assertEquals("KERN_RPC_CONTINUE_ORPHAN (0x2d)", KernReturn.KERN_RPC_CONTINUE_ORPHAN.toString());
		assertEquals("KERN_TERMINATED (0x25)", KernReturn.KERN_TERMINATED.toStandardString());
		assertEquals("KERN_ABORTED (0xe)", KernReturn.KERN_ABORTED.toStandardString());
		assertEquals("KERN_FAILURE (0x5)", KernReturn.KERN_FAILURE.toString());
		assertNull(KernReturn.typeOf(671088640));
		assertEquals(52, KernReturn.KERN_INSUFFICIENT_BUFFER_SIZE.getValue());
		assertEquals(DefaultSubsystem.none, DefaultKernReturnT.getSubsystem(KernReturn.KERN_INSUFFICIENT_BUFFER_SIZE));
		assertEquals(DefaultKernReturnT.System.err_kern, DefaultKernReturnT.getSystem(KernReturn.KERN_INSUFFICIENT_BUFFER_SIZE));
		assertEquals(0, DefaultKernReturnT.getSystemCode(KernReturn.KERN_INSUFFICIENT_BUFFER_SIZE));
	}

	/**
	 * Tests {@link Message}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testMessage() throws Throwable {
		assertNull(Message.typeOf(0));
		assertNull(Message.typeOf(4));
		assertNull(Message.typeOf(-536870170));
		assertEquals(Message.MACH_RCV_PORT_DIED, Message.typeOf(268451849));
		assertEquals(4, DefaultKernReturnT.getSystemCode(Message.MACH_SEND_INVALID_DEST));
		assertEquals("MACH_SEND_INTERRUPTED (0x10000007)", Message.MACH_SEND_INTERRUPTED.toStandardString());
		assertEquals("MACH_RCV_IN_PROGRESS_TIMED (0x10004011)", Message.MACH_RCV_IN_PROGRESS_TIMED.toString());
		assertEquals(268435457, Message.MACH_SEND_IN_PROGRESS.getValue());
		assertEquals("MACH_SEND_TIMED_OUT (0x10000004)", Message.MACH_SEND_TIMED_OUT.toStandardString());
		assertEquals(268451848, Message.MACH_RCV_INVALID_DATA.getValue());
		assertEquals(0x10000015, Message.MACH_SEND_INVALID_RT_OOL_SIZE.getValue());
		assertEquals("MACH_RCV_TIMED_OUT (0x10004003)", Message.MACH_RCV_TIMED_OUT.toStandardString());
		assertEquals(268435466, Message.MACH_SEND_INVALID_RIGHT.getValue());
		assertEquals("MACH_SEND_INVALID_RT_OOL_SIZE (0x10000015)", Message.MACH_SEND_INVALID_RT_OOL_SIZE.toString());
		assertEquals(21, DefaultKernReturnT.getErrorCode(Message.MACH_SEND_INVALID_RT_OOL_SIZE));
		assertEquals("MACH_MSG_IPC_SPACE (0x2000)", Message.MACH_MSG_IPC_SPACE.toString());
		assertEquals("MACH_RCV_IN_PROGRESS (0x10004001)", Message.MACH_RCV_IN_PROGRESS.toStandardString());
		assertEquals("MACH_SEND_INVALID_DEST (0x10000003)", Message.MACH_SEND_INVALID_DEST.toStandardString());
	}
}
