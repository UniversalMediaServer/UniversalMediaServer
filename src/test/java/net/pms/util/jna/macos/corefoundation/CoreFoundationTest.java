package net.pms.util.jna.macos.corefoundation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import net.pms.util.jna.StringByReference;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFArrayRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFComparisonResult;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFDataRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFDictionaryRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableArrayRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableDataRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableDictionaryRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableStringRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFNumberRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFNumberType;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringBuiltInEncodings;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringCompareFlags;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFTypeRef;
import net.pms.util.jna.macos.iokit.IOKit;


/**
 * Tests for {@link CoreFoundation}.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:StaticVariableName")
public class CoreFoundationTest {
	private static CoreFoundation CF;

	/**
	 * Skip tests if platform isn't macOS, initialize a {@link CoreFoundation}
	 * instance if it is.
	 */
	@BeforeClass
	public static void setUp() {
		Assume.assumeTrue(Platform.isMac());
		CF = CoreFoundation.INSTANCE;
	}

	/**
	 * Tests {@link CFStringRef} and {@link CFMutableStringRef}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testCFString() throws Throwable {
		assertEquals("FooBar", CFStringRef.toCFStringRef("FooBar").toString());
		assertEquals("", CFStringRef.toCFStringRef("").toString());
		assertNull(CFStringRef.toCFStringRef(null));
		assertEquals(
			IOKit.kIOPMAssertionTypePreventUserIdleDisplaySleep,
			CFStringRef.toCFStringRef(IOKit.kIOPMAssertionTypePreventUserIdleDisplaySleep).toString()
		);
		CFStringRef cfStringRef = new CFStringRef(Pointer.NULL);
		CFStringRef cfStringRef2 = null;
		assertEquals(CFComparisonResult.kCFCompareLessThan, cfStringRef.compareTo(cfStringRef2));
		cfStringRef2 = new CFStringRef(Pointer.NULL);
		assertEquals(CFComparisonResult.kCFCompareEqualTo, cfStringRef.compareTo(cfStringRef2));
		cfStringRef = CFStringRef.toCFStringRef("a");
		assertEquals(CFComparisonResult.kCFCompareLessThan, cfStringRef.compareTo(cfStringRef2));
		cfStringRef2 = CFStringRef.toCFStringRef("b");
		assertEquals(CFComparisonResult.kCFCompareLessThan, cfStringRef.compareTo(cfStringRef2));
		CF.CFRelease(cfStringRef);
		CF.CFRelease(cfStringRef2);
		cfStringRef = CFStringRef.toCFStringRef("Foo-BAR");
		cfStringRef2 = CFStringRef.toCFStringRef("foo-bar");
		assertEquals(CFComparisonResult.kCFCompareLessThan, cfStringRef.compareTo(cfStringRef2));
		assertEquals(
			CFComparisonResult.kCFCompareEqualTo,
			cfStringRef.compareTo(cfStringRef2, CFStringCompareFlags.kCFCompareCaseInsensitive)
		);
		assertEquals(
			CFComparisonResult.kCFCompareEqualTo,
			cfStringRef.compareTo(
				cfStringRef2,
				CFStringCompareFlags.kCFCompareCaseInsensitive,
				CFStringCompareFlags.kCFCompareCaseInsensitive
			)
		);
		assertEquals(
			new NativeLong(4),
			CF.CFStringConvertEncodingToNSStringEncoding(CFStringBuiltInEncodings.kCFStringEncodingUTF8.getValue())
		);
		CF.CFRelease(cfStringRef);
		cfStringRef = CF.CFStringCreateWithCString(null, "test string", CFStringBuiltInEncodings.kCFStringEncodingUTF8.getValue());
		assertEquals("test string", cfStringRef.toString());
		byte[] stringBytes = "Test byte array string".getBytes(StandardCharsets.UTF_16BE);
		CF.CFRelease(cfStringRef2);
		cfStringRef2 = CF.CFStringCreateWithBytes(
			null,
			stringBytes,
			stringBytes.length,
			CFStringBuiltInEncodings.kCFStringEncodingUTF16BE.getValue(),
			false
		);
		assertEquals("Test byte array string", cfStringRef2.toString());
		StringByReference refString = new StringByReference(20);
		assertTrue(
			CF.CFStringGetCString(cfStringRef, refString, refString.getAllocatedSize(),
				CFStringBuiltInEncodings.kCFStringEncodingASCII.getValue())
		);
		assertEquals("test string", refString.getValue());
		refString = CF.CFStringGetCStringPtr(cfStringRef2, CFStringBuiltInEncodings.kCFStringEncodingMacRoman.getValue());
		assertEquals("Test byte array string", refString.toString());
		refString = new StringByReference(CF.CFStringGetMaximumSizeOfFileSystemRepresentation(cfStringRef2));
		assertTrue(CF.CFStringGetFileSystemRepresentation(cfStringRef2, refString, refString.getAllocatedSize()));
		CF.CFRelease(cfStringRef);
		cfStringRef = CF.CFStringCreateWithFileSystemRepresentation(null, refString);
		assertTrue(cfStringRef.compareTo(cfStringRef2) == CFComparisonResult.kCFCompareEqualTo);
		CFMutableStringRef mutableStringRef = CF.CFStringCreateMutableCopy(null, 0, cfStringRef2);
		assertTrue(mutableStringRef.compareTo(cfStringRef) == CFComparisonResult.kCFCompareEqualTo);
		CF.CFRelease(cfStringRef);
		CF.CFRelease(cfStringRef2);
		CF.CFRelease(mutableStringRef);
	}

	/**
	 * Tests {@link CFDictionaryRef} and {@link CFMutableDictionaryRef}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testCFDictionary() throws Throwable {
		CFStringRef cfStringRef = CFStringRef.toCFStringRef("Test");
		CFNumberRef cfNumberRef = CFNumberRef.toCFNumberRef(4.3);
		CFMutableDictionaryRef mutableDictionaryRef = IOKit.INSTANCE.IOServiceMatching("IOHIDSystem");
		assertTrue(CF.CFDictionaryGetCount(mutableDictionaryRef) > 0);
		long l = CF.CFDictionaryGetCount(mutableDictionaryRef);
		CF.CFDictionaryAddValue(mutableDictionaryRef, cfNumberRef, cfStringRef);
		assertEquals(l + 1, CF.CFDictionaryGetCount(mutableDictionaryRef));
		assertTrue(CF.CFDictionaryContainsKey(mutableDictionaryRef, cfNumberRef));
		assertTrue(CF.CFDictionaryContainsValue(mutableDictionaryRef, cfStringRef));
		CFDictionaryRef dictionaryRef = CF.CFDictionaryCreateCopy(CoreFoundation.ALLOCATOR, mutableDictionaryRef);
		CFTypeArrayRef keys = new CFTypeArrayRef(CF.CFDictionaryGetCount(dictionaryRef));
		CFTypeArrayRef values = new CFTypeArrayRef(CF.CFDictionaryGetCount(dictionaryRef));
		CF.CFDictionaryGetKeysAndValues(dictionaryRef, keys, values);
		assertTrue(Arrays.asList(keys.getArray()).contains(cfNumberRef));
		assertTrue(Arrays.asList(values.getArray()).contains(cfStringRef));
		CF.CFDictionaryRemoveValue(mutableDictionaryRef, cfNumberRef);
		assertEquals(l, CF.CFDictionaryGetCount(mutableDictionaryRef));
		PointerByReference pointerRef = new PointerByReference();
		assertTrue(CF.CFDictionaryGetValueIfPresent(dictionaryRef, cfNumberRef, pointerRef));
		CFStringRef cfStringRef2 = new CFStringRef(pointerRef.getValue());
		assertTrue(cfStringRef.compareTo(cfStringRef2) == CFComparisonResult.kCFCompareEqualTo);
		assertEquals(cfStringRef.toString(), cfStringRef2.toString());
		assertEquals(cfStringRef, cfStringRef2);

		// Don't release cfStringRef2 as it's actually the same CFStringRef instance as cfStringRef
		CF.CFRelease(dictionaryRef);
		CF.CFRelease(mutableDictionaryRef);
		CF.CFRelease(cfNumberRef);
		CF.CFRelease(cfStringRef);
	}

	/**
	 * Tests {@link CFNumberRef}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testCFNumber() throws Throwable {
		IntByReference intRef = new IntByReference();
		LongByReference longRef = new LongByReference();
		DoubleByReference doubleRef = new DoubleByReference();

		CFNumberRef cfNumberRef = CFNumberRef.toCFNumberRef((byte) 254);
		assertEquals((byte) 254, cfNumberRef.getValue());
		assertEquals(CFNumberType.kCFNumberSInt8Type, CF.CFNumberGetType(cfNumberRef));
		assertFalse(CF.CFNumberIsFloatType(cfNumberRef));
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef((short) 31400);
		assertEquals((short) 31400, cfNumberRef.getValue());
		assertEquals(CFNumberType.kCFNumberSInt16Type, CF.CFNumberGetType(cfNumberRef));
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef('a');
		assertEquals('a', cfNumberRef.getValue().intValue());
		assertEquals(CFNumberType.kCFNumberSInt8Type, CF.CFNumberGetType(cfNumberRef));
		assertFalse(CF.CFNumberIsFloatType(cfNumberRef));
		assertTrue(CF.CFNumberGetValue(cfNumberRef, CFNumberType.kCFNumberSInt32Type, intRef));
		assertEquals('a', intRef.getValue());
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef(11.43d);
		assertEquals(11.43d, cfNumberRef.getValue().doubleValue(), 0.0d);
		assertEquals(CFNumberType.kCFNumberFloat64Type, CF.CFNumberGetType(cfNumberRef));
		assertTrue(CF.CFNumberIsFloatType(cfNumberRef));
		assertFalse(CF.CFNumberGetValue(cfNumberRef, CFNumberType.kCFNumberSInt32Type, intRef));
		assertTrue(CF.CFNumberGetValue(cfNumberRef, CFNumberType.kCFNumberFloat64Type, doubleRef));
		assertEquals(11, intRef.getValue());
		assertEquals(11.43d, doubleRef.getValue(), 0.0d);
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef(-43.7f);
		assertEquals(-43.7f, cfNumberRef.getValue().floatValue(), 0.0f);
		assertEquals(CFNumberType.kCFNumberFloat32Type, CF.CFNumberGetType(cfNumberRef));
		assertTrue(CF.CFNumberIsFloatType(cfNumberRef));
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef(Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, cfNumberRef.getValue());
		assertEquals(CFNumberType.kCFNumberSInt32Type, CF.CFNumberGetType(cfNumberRef));
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef(Integer.MIN_VALUE);
		assertEquals(Integer.MIN_VALUE, cfNumberRef.getValue());
		assertEquals(CFNumberType.kCFNumberSInt32Type, CF.CFNumberGetType(cfNumberRef));
		assertFalse(CF.CFNumberIsFloatType(cfNumberRef));
		assertTrue(CF.CFNumberGetValue(cfNumberRef, CFNumberType.kCFNumberSInt32Type, intRef));
		assertEquals(Integer.MIN_VALUE, intRef.getValue());
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef(0);
		assertEquals(0, cfNumberRef.getValue());
		assertEquals(CFNumberType.kCFNumberSInt32Type, CF.CFNumberGetType(cfNumberRef));
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef(Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, cfNumberRef.getValue());
		assertEquals(CFNumberType.kCFNumberSInt64Type, CF.CFNumberGetType(cfNumberRef));
		assertFalse(CF.CFNumberIsFloatType(cfNumberRef));
		assertTrue(CF.CFNumberGetValue(cfNumberRef, CFNumberType.kCFNumberSInt64Type, longRef));
		assertEquals(Long.MAX_VALUE, longRef.getValue());
		CF.CFRelease(cfNumberRef);
		cfNumberRef = CFNumberRef.toCFNumberRef(Long.MIN_VALUE);
		assertEquals(Long.MIN_VALUE, cfNumberRef.getValue());
		assertEquals(CFNumberType.kCFNumberSInt64Type, CF.CFNumberGetType(cfNumberRef));
		CF.CFRelease(cfNumberRef);
	}

	/**
	 * Tests {@link CFArrayRef} and {@link CFMutableArrayRef}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testCFArray() {
		CFTypeRef[] cfTypeRefs = {
				CFStringRef.toCFStringRef("First array element"),
				CFStringRef.toCFStringRef("Second array element"),
				CFStringRef.toCFStringRef("Third array element"),
				CFStringRef.toCFStringRef("Fourth array element"),
				CFStringRef.toCFStringRef("Fifth array element")
		};
		CFTypeArrayRef typeArrayRef = new CFTypeArrayRef(cfTypeRefs);
		CFArrayRef cfArrayRef = CF.CFArrayCreate(null, typeArrayRef, typeArrayRef.getSize(), CoreFoundation.kCFTypeArrayCallBacks);
		assertEquals(5, CF.CFArrayGetCount(cfArrayRef));
		CFMutableArrayRef cfMutableArrayRef = CF.CFArrayCreateMutableCopy(null, 10, cfArrayRef);
		assertEquals(5, CF.CFArrayGetCount(cfMutableArrayRef));
		CFNumberRef cfNumberRef = CFNumberRef.toCFNumberRef(50);
		CF.CFArrayAppendValue(cfMutableArrayRef, cfNumberRef);
		assertEquals(6, CF.CFArrayGetCount(cfMutableArrayRef));
		assertEquals("Fourth array element", new CFStringRef(CF.CFArrayGetValueAtIndex(cfMutableArrayRef, 3)).toString());
		assertEquals("50", new CFNumberRef(CF.CFArrayGetValueAtIndex(cfMutableArrayRef, 5)).toString());
		CF.CFArrayInsertValueAtIndex(cfMutableArrayRef, 0, cfNumberRef);
		assertEquals(7, CF.CFArrayGetCount(cfMutableArrayRef));
		assertEquals("50", new CFNumberRef(CF.CFArrayGetValueAtIndex(cfMutableArrayRef, 0)).toString());
		CF.CFArrayExchangeValuesAtIndices(cfMutableArrayRef, 0, 2);
		assertEquals("50", new CFNumberRef(CF.CFArrayGetValueAtIndex(cfMutableArrayRef, 2)).toString());
		CF.CFArrayRemoveAllValues(cfMutableArrayRef);
		assertEquals(0, CF.CFArrayGetCount(cfMutableArrayRef));

		CF.CFRelease(cfNumberRef);
		CF.CFRelease(cfMutableArrayRef);
		CF.CFRelease(cfArrayRef);
		for (CFTypeRef ref : cfTypeRefs) {
			CF.CFRelease(ref);
		}
	}

	/**
	 * Tests {@link CFDataRef} and {@link CFMutableDataRef}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testCFData() {
		byte[] bytes = "First test bytes".getBytes(StandardCharsets.UTF_8);
		byte[] bytes2 = "Second test bytes".getBytes(StandardCharsets.UTF_8);

		CFDataRef cfDataRef = CF.CFDataCreate(null, bytes, bytes.length);
		PointerByReference pointerRef = CF.CFDataGetBytePtr(cfDataRef);
		assertArrayEquals(bytes, pointerRef.getPointer().getByteArray(0, bytes.length));
		CFDataRef cfDataRef2 = CF.CFDataCreate(null, bytes2, bytes2.length);
		pointerRef = CF.CFDataGetBytePtr(cfDataRef2);
		assertArrayEquals(bytes2, pointerRef.getPointer().getByteArray(0, bytes2.length));
		CFMutableDataRef cfMutableDataRef = CF.CFDataCreateMutableCopy(null, 40, cfDataRef2);
		pointerRef = CF.CFDataGetBytePtr(cfMutableDataRef);
		assertArrayEquals(bytes2, pointerRef.getPointer().getByteArray(0, bytes2.length));
		CF.CFDataAppendBytes(cfMutableDataRef, bytes, bytes.length);
		assertEquals(bytes.length + bytes2.length, CF.CFDataGetLength(cfMutableDataRef));
		CF.CFDataSetLength(cfMutableDataRef, 10);
		assertEquals(10, CF.CFDataGetLength(cfMutableDataRef));

		CF.CFRelease(cfMutableDataRef);
		CF.CFRelease(cfDataRef2);
		CF.CFRelease(cfDataRef);
	}

	/**
	 * Tests {@link TerminatedStringEncodingArray}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testTerminatedStringEncodingArray() {
		TerminatedStringEncodingArray encodings = CF.CFStringGetListOfAvailableEncodings();
		assertTrue(encodings.contains(0));
		assertEquals("Western (Mac OS Roman)", encodings.getString(0, false));
		assertEquals("0: Western (Mac OS Roman)", encodings.getString(0, true));
		EnumSet<CFStringBuiltInEncodings> builtIns = encodings.getBuiltInEncodings();
		assertNotNull(builtIns);
		assertTrue(builtIns.contains(CFStringBuiltInEncodings.kCFStringEncodingMacRoman));
		assertEquals(CFStringBuiltInEncodings.kCFStringEncodingMacRoman, encodings.getBuiltInEncoding(0));
	}
}
