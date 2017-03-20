/*
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package net.pms.io.iokit;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * CoreFoundation framework for power supply stats. This class should be
 * considered non-API as it may be removed if/when its code is incorporated into
 * the JNA project.
 * <p>
 * This is a copy of {@link oshi.jna.platform.mac.CoreFoundation} where all
 * members are changed from package private to public.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface CoreFoundation extends Library {
    CoreFoundation INSTANCE = (CoreFoundation) Native.loadLibrary("CoreFoundation", CoreFoundation.class);

    public int UTF_8 = 0x08000100;

    public int CFArrayGetCount(CFArrayRef array);

    /*
     * Decorator class types for PointerType to better match ported code
     */
    /** Untyped type reference */
    public class CFTypeRef extends PointerType {
    }

    public class CFNumberRef extends PointerType {
    }

    public class CFBooleanRef extends PointerType {
    }

    public class CFArrayRef extends PointerType {
    }

    public class CFDictionaryRef extends PointerType {
    }

    public class CFMutableDictionaryRef extends CFDictionaryRef {
    }

    public class CFAllocatorRef extends PointerType {
    }

    public class CFStringRef extends PointerType {
        /**
         * Creates a new CFString from the given Java string.
         *
         * @param s
         *            A string
         * @return A reference to a CFString representing s
         */
        public static CFStringRef toCFString(String s) {
            final char[] chars = s.toCharArray();
            int length = chars.length;
            return CoreFoundation.INSTANCE.CFStringCreateWithCharacters(null, chars, new NativeLong(length));
        }

        public CFStringRef() {
        	super();
        }
        
        public CFStringRef(Pointer p) {
        	super();
        	this.setPointer(p);
		}
        
		@Override
		public String toString() {
			if (this.getPointer() == null) {
				return null;
			}
			long length = INSTANCE.CFStringGetLength(this.getPointer());
			long maxSize = Math.max(INSTANCE.CFStringGetMaximumSizeForEncoding(length, UTF_8), 1);
			Pointer buffer = new Memory(maxSize);
			INSTANCE.CFStringGetCString(this.getPointer(), buffer, maxSize, UTF_8);
			return buffer.getString(0);
		}
    }

    /*
     * References are owned if created by functions including "Create" or "Copy"
     * and must be released with CFRelease to avoid leaking references
     */

    public CFStringRef CFStringCreateWithCharacters(Object object, char[] chars, NativeLong length);

    public CFMutableDictionaryRef CFDictionaryCreateMutable(CFAllocatorRef allocator, int capacity, Pointer keyCallBacks,
            Pointer valueCallBacks);

    public void CFRelease(PointerType blob);

    /*
     * References are not owned if created by functions using "Get". Use
     * CFRetain if object retention is required, and then CFRelease later. Do
     * not use CFRelease if you do not own.
     */

    public void CFDictionarySetValue(CFMutableDictionaryRef dict, PointerType key, PointerType value);

    public Pointer CFDictionaryGetValue(CFDictionaryRef dictionary, CFStringRef key);

    public boolean CFDictionaryGetValueIfPresent(CFDictionaryRef dictionary, CFStringRef key, PointerType value);

    public boolean CFStringGetCString(Pointer cfString, Pointer bufferToFill, long maxSize, int encoding);

    public boolean CFBooleanGetValue(Pointer booleanRef);

    public CFTypeRef CFArrayGetValueAtIndex(CFArrayRef array, int index);

    public void CFNumberGetValue(Pointer cfNumber, int intSize, ByReference value);

    public long CFStringGetLength(Pointer str);

    public long CFStringGetMaximumSizeForEncoding(long length, int encoding);

    public CFAllocatorRef CFAllocatorGetDefault();

    public class CFDataRef extends CFTypeRef {
    }

    public int CFDataGetLength(CFTypeRef theData);

    public PointerByReference CFDataGetBytePtr(CFTypeRef theData);
}