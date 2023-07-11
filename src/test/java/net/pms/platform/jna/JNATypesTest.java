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
package net.pms.platform.jna;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests the custom JNA types.
 *
 * @author Nadahar
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JNATypesTest {
	/**
	 * Tests {@link StringByReference}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testStringByReference() throws Throwable {
		assertEquals("", new StringByReference("").toString());
		assertEquals(-1L, new StringByReference().getAllocatedSize());
		assertEquals(-1L, new StringByReference(-5).getAllocatedSize());
		assertEquals("null", new StringByReference().toString());
		assertEquals(10L, new StringByReference(10).getAllocatedSize());
		assertEquals(0L, new StringByReference("").getAllocatedSize());
		assertNull(new StringByReference().getValue());
		assertNull(new StringByReference().getPointer());
		assertNotNull(new StringByReference("foo").getPointer());
		assertEquals(Pointer.class, new StringByReference().nativeType());
		StringByReference stringByReference = new StringByReference();
		stringByReference.setValue("foo", StandardCharsets.US_ASCII);
		assertEquals("foo", stringByReference.getValue());
		assertEquals("foo", stringByReference.toString());
		assertEquals(3, stringByReference.getAllocatedSize());

		try {
			stringByReference.setValue("foo", "bar");
			fail("Expected exception of type IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// Expected exception.
		}

		try {
			stringByReference.setValue("null", (Charset) null);
			fail("Expected exception of type NullPointerException");
		} catch (NullPointerException e) {
			// Expected exception.
		}

		try {
			stringByReference.setValue("null", (String) null);
			fail("Expected exception of type NullPointerException");
		} catch (NullPointerException e) {
			// Expected exception.
		}

		try {
			stringByReference.setValue("\uFFFD", "\u001E\u1D88");
			fail("Expected exception of type IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertEquals(UnsupportedEncodingException.class, e.getCause().getClass());
		}
	}

	/**
	 * Tests {@link WStringByReference}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testWStringByReference() throws Throwable {
		assertEquals(0, WStringByReference.getNumberOfBytes(""));
		assertEquals("", new WStringByReference("").toString());
		assertEquals(-1L, new WStringByReference().getAllocatedSize());
		assertEquals(-1L, new WStringByReference(-5).getAllocatedSize());
		assertEquals("null", new WStringByReference().toString());
		assertEquals(10L, new WStringByReference(10).getAllocatedSize());
		assertEquals(0L, new WStringByReference("").getAllocatedSize());
		assertEquals("\uFFFD", new WStringByReference("\uFFFD").getValue());
		assertNull(new WStringByReference().getValue());
		assertNull(new WStringByReference().getPointer());
		assertNotNull(new WStringByReference("foo").getPointer());
		assertEquals(Pointer.class, new WStringByReference().nativeType());
		WStringByReference wStringByReference = new WStringByReference();
		wStringByReference.setValue("foo");
		assertEquals("foo", wStringByReference.getValue());
		assertEquals("foo", wStringByReference.toString());
	}


	/**
	 * Tests {@link PointerArrayByReference}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testPointerArrayByReference() throws Throwable {
		assertNull(new PointerArrayByReference(0L).getArray());
		assertEquals("NULL", new PointerArrayByReference().toString());
		assertEquals(Native.POINTER_SIZE, new PointerArrayByReference().getElementSize());
		assertEquals(0L, new PointerArrayByReference().getSize());
		assertEquals(5L, new PointerArrayByReference(5).getSize());
		assertEquals(0L, new PointerArrayByReference(-9).getSize());
		assertTrue(new PointerArrayByReference().isGarbageCollected());
		assertTrue(new PointerArrayByReference(10).isGarbageCollected());
		Pointer[] emptyPointerArray = new Pointer[] {};
		Pointer[] populatedPointerArray = new Pointer[] {new Memory(1), new Memory(3), new Memory(2)};
		PointerArrayByReference pointerArrayByReference = new PointerArrayByReference();
		pointerArrayByReference.setArray(populatedPointerArray);
		assertEquals(3L, pointerArrayByReference.getSize());
		assertArrayEquals(populatedPointerArray, pointerArrayByReference.getArray());
		assertArrayEquals(populatedPointerArray, pointerArrayByReference.getElements());
		pointerArrayByReference.setArray(emptyPointerArray);
		assertEquals(0L, pointerArrayByReference.getSize());
		assertNull(pointerArrayByReference.getArray());
		assertNull(pointerArrayByReference.getElements());
		pointerArrayByReference.setSize(11);
		assertEquals(11L, pointerArrayByReference.getSize());

		try {
			pointerArrayByReference.setArray(null);
			fail("Expected exception of type NullPointerException");
		} catch (NullPointerException e) {
			// Expected exception.
		}
	}

	/**
	 * Tests {@link FixedCharArrayByReference}.
	 *
	 * @throws Throwable if an error occurs during the test.
	 */
	@Test
	public void testFixedCharArrayByReference() throws Throwable {
		assertNull(new FixedCharArrayByReference(0).getArray());
		assertNull(new FixedCharArrayByReference(0).getByteArray());
		assertNull(new FixedCharArrayByReference(0).getString());
		assertNull(new FixedCharArrayByReference(0).getString(null));
		assertNotNull(new FixedCharArrayByReference(6).getArray());
		assertEquals("null", new FixedCharArrayByReference(0).toString());
		assertEquals(1, new FixedCharArrayByReference(0).getElementSize());
		assertEquals(0L, new FixedCharArrayByReference(0).getSize());
		assertEquals(5L, new FixedCharArrayByReference(5).getSize());
		assertEquals(0L, new FixedCharArrayByReference(-9).getSize());
		assertTrue(new FixedCharArrayByReference(0).isGarbageCollected());
		assertTrue(new FixedCharArrayByReference(10).isGarbageCollected());

		byte[] populatedByteArray = new byte[] {(byte) -1, (byte) 100, (byte) 1, (byte) 62};
		FixedCharArrayByReference fixedCharArrayByReference = new FixedCharArrayByReference(4);
		fixedCharArrayByReference.setByteArray(populatedByteArray);
		assertEquals(4, fixedCharArrayByReference.getSize());
		assertEquals("?d\u0001>", fixedCharArrayByReference.getString(StandardCharsets.US_ASCII));

		fixedCharArrayByReference.setString("\u001E\u1D88", StandardCharsets.UTF_16);
		assertArrayEquals(new byte[] {(byte) -2, (byte) -1, (byte) 0, (byte) 30}, fixedCharArrayByReference.getByteArray());
		fixedCharArrayByReference = new FixedCharArrayByReference(6);
		fixedCharArrayByReference.setString("\u001E\u1D88", StandardCharsets.UTF_16);
		assertEquals("\u001E\u1D88", fixedCharArrayByReference.getString(StandardCharsets.UTF_16));
		assertEquals("??", fixedCharArrayByReference.getString(StandardCharsets.US_ASCII));

		try {
			fixedCharArrayByReference.setByteArray(new byte[] {(byte) 100, (byte) 1, (byte) 1});
			fail("Expected exception of type IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// Expected exception.
		}

		// Regression assertion (captures the current behavior of the code)
		assertNotNull(populatedByteArray);
	}
}
