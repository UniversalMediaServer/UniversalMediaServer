package net.pms;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import net.pms.io.iokit.CoreFoundation;
import net.pms.io.iokit.CoreFoundation.CFDictionaryRef;
import net.pms.io.iokit.CoreFoundation.CFStringRef;
import net.pms.io.iokit.IOKit;

public class Test2 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//com.sun.jna.platform.mac.XAttrUtil
		//System.out.print(JnaTest.getInstance().hasTrash() ? "Yes" : "Nope");
		//System.out.print(FileManager.INSTANCE.FSPathMakeRef("", 0, new ByteByReference((byte) 1)));
		Native.setProtected(true);
		IntByReference assertionID = new IntByReference();
		CFStringRef assertionType = CFStringRef.toCFString(IOKit.kIOPMAssertPreventUserIdleSystemSleep);
		CFStringRef name = CFStringRef.toCFString("TestName");
		/*int ioResult = IOKit.INSTANCE.IOPMAssertionCreateWithName(assertionType, IOKit.kIOPMAssertionLevelOn, name, assertionID);
		System.out.println(ioResult);
		System.out.println(Integer.toHexString(ioResult));
		System.out.println(assertionID.getValue());*/
		CFStringRef details = CFStringRef.toCFString("Testing out IOKit");
		CFStringRef timeOutAction = CFStringRef.toCFString(IOKit.kIOPMAssertionTimeoutActionTurnOff);
		int ioResult = IOKit.INSTANCE.IOPMAssertionCreateWithDescription(assertionType, name, details, null, null, 10d, timeOutAction, assertionID);
		System.out.println(ioResult);
		System.out.println(Integer.toHexString(ioResult));
		System.out.println(assertionID.getValue());
		CFDictionaryRef dict =
		IOKit.INSTANCE.IOPMAssertionCopyProperties(assertionID.getValue());
		CoreFoundation coreFoundation = CoreFoundation.INSTANCE;
		CFStringRef key = CFStringRef.toCFString(IOKit.kIOPMAssertionTimeoutActionKey);
		Pointer p = coreFoundation.CFDictionaryGetValue(dict, key);
		CFStringRef test = new CFStringRef(p);
		System.out.println(test);
	}

}

