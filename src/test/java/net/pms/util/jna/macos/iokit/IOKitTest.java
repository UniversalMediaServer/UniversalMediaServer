package net.pms.util.jna.macos.iokit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import com.sun.jna.Platform;
import net.pms.util.jna.macos.corefoundation.CoreFoundation;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFDictionaryRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFMutableDictionaryRefByReference;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringRef;
import net.pms.util.jna.macos.kernreturn.DefaultKernReturnT;
import net.pms.util.jna.macos.types.IOIteratorT;
import net.pms.util.jna.macos.types.IOIteratorTRef;
import net.pms.util.jna.macos.types.IONameT;
import net.pms.util.jna.macos.types.IOObjectT;
import net.pms.util.jna.macos.types.IORegistryEntryT;

/**
 * Tests for {@link IOKit}.
 *
 * @author Nadahar
 */
@SuppressWarnings("checkstyle:StaticVariableName")
public class IOKitTest {
	private static CoreFoundation CF;
	private static IOKit IO;

	/**
	 * Skip tests if platform isn't macOS, initialize {@link CoreFoundation} and
	 * {@link IOKit} instances if it is.
	 */
	@BeforeClass
	public static void setUp() {
		Assume.assumeTrue(Platform.isMac());
		CF = CoreFoundation.INSTANCE;
		IO = IOKit.INSTANCE;
	}

	/**
	 * Tests some {@link IOKit} mappings.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testIOKit() throws Throwable {
		CFDictionaryRef dictionaryRef = IO.IOServiceMatching("IOHIDSystem");
		IOIteratorTRef iteratorRef = new IOIteratorTRef(true);
		assertEquals(DefaultKernReturnT.SUCCESS, IO.IOServiceGetMatchingServices(IOKit.kIOMasterPortDefault, dictionaryRef, iteratorRef));
		IOIteratorT iterator = iteratorRef.getValue();
		IONameT name = new IONameT(true);
		assertEquals(DefaultKernReturnT.SUCCESS, IO.IOObjectGetClass(iterator, name));
		assertEquals("IOUserIterator", name.getString(StandardCharsets.UTF_8));
		IOObjectT object = IO.IOIteratorNext(iterator);

		assertEquals(DefaultKernReturnT.SUCCESS, IO.IOObjectGetClass(object, name));
		assertEquals("IOHIDSystem", name.getString(StandardCharsets.UTF_8));

		IORegistryEntryT registryEntry = IORegistryEntryT.toIORegistryT(object);
		CFMutableDictionaryRefByReference dictionaryRefRef = new CFMutableDictionaryRefByReference();
		assertEquals(
			DefaultKernReturnT.SUCCESS,
			IO.IORegistryEntryCreateCFProperties(registryEntry, dictionaryRefRef, CoreFoundation.ALLOCATOR, 0)
		);
		CFStringRef key = CFStringRef.toCFStringRef("IOClass");
		assertTrue(CF.CFDictionaryContainsKey(dictionaryRefRef.getCFMutableDictionaryRef(), key));
		CFStringRef value = new CFStringRef(CF.CFDictionaryGetValue(dictionaryRefRef.getCFMutableDictionaryRef(), key));
		assertEquals("IOHIDSystem", value.toString());
		CF.CFRelease(key);
		key = CFStringRef.toCFStringRef("IOProviderClass");
		value = new CFStringRef(CF.CFDictionaryGetValue(dictionaryRefRef.getCFMutableDictionaryRef(), key));
		assertEquals("IOResources", value.toString());
		CF.CFRelease(key);

		IO.IOObjectRelease(object);
		object = IO.IOIteratorNext(iterator);
		assertEquals(0, object.intValue());
		assertTrue(IO.IOIteratorIsValid(iterator));

		IO.IOObjectRelease(iterator);
		CF.CFRelease(dictionaryRefRef.getCFMutableDictionaryRef());
	}

	/**
	 * Tests {@link IONameT}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testIONameT() throws Throwable {
		IONameT name = new IONameT(
			"abcdefghijklmnopqrstuvwxyz!.? abcdefghijklmnopqrstuvwxyz!.? abcdefghijklmnopqrstuvwxyz!.? " +
			"abcdefghijklmnopqrstuvwxyz!.? abcdefghijklmnopqrstuvwxyz!.? ",
			StandardCharsets.US_ASCII
		);
		assertEquals(
			"abcdefghijklmnopqrstuvwxyz!.? abcdefghijklmnopqrstuvwxyz!.? abcdefghijklmnopqrstuvwxyz!.? " +
			"abcdefghijklmnopqrstuvwxyz!.? abcdefgh",
			name.getString(StandardCharsets.US_ASCII)
		);
		name = new IONameT(
			"abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 " +
			"abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 " +
			"abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 ",
			StandardCharsets.UTF_8
		);
		assertEquals(
			"abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 " +
			"abcdefghijklmnopqrstuvwxyz\u00F8\u00E6\u00E5 abcdefghijklmnopqrstuvwxyz\u00F8?",
			name.getString(StandardCharsets.UTF_8)
		);
	}
}
