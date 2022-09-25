/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.platform.mac.iokit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import com.sun.jna.Platform;
import net.pms.platform.mac.corefoundation.CoreFoundation;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFDictionaryRef;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFMutableDictionaryRefByReference;
import net.pms.platform.mac.corefoundation.CoreFoundation.CFStringRef;
import net.pms.platform.mac.kernreturn.DefaultKernReturnT;
import net.pms.platform.mac.types.IOIteratorT;
import net.pms.platform.mac.types.IOIteratorTRef;
import net.pms.platform.mac.types.IONameT;
import net.pms.platform.mac.types.IOObjectT;
import net.pms.platform.mac.types.IORegistryEntryT;

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
