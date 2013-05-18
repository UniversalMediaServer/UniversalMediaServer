/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Mozilla Universal charset detector code.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 2001
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *          Shy Shalom <shooshX@gmail.com>
 *          Kohei TAKETA <k-tak@void.in> (Java port)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package net.pms.util.universalchardet.prober;

import java.nio.ByteBuffer;


public abstract class CharsetProber
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final float   SHORTCUT_THRESHOLD = 0.95f;
    public static final int     ASCII_A = 0x61; // 'a'
    public static final int     ASCII_Z = 0x7A; // 'z'
    public static final int     ASCII_A_CAPITAL = 0x41; // 'A'
    public static final int     ASCII_Z_CAPITAL = 0x5A; // 'Z'
    public static final int     ASCII_LT = 0x3C; // '<'
    public static final int     ASCII_GT = 0x3E; // '>'
    public static final int     ASCII_SP = 0x20; // ' '

    
    ////////////////////////////////////////////////////////////////
    // inner types
    ////////////////////////////////////////////////////////////////
    public enum ProbingState
    {
        DETECTING,
        FOUND_IT,
        NOT_ME
    }

    
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public CharsetProber()
    {}
    
    public abstract String getCharSetName();
    public abstract ProbingState handleData(final byte[] buf, int offset, int length);
    public abstract ProbingState getState();
    public abstract void reset();
    public abstract float getConfidence();
    public abstract void setOption();

    // ByteBuffer.position() indicates number of bytes written.
    public ByteBuffer filterWithoutEnglishLetters(final byte[] buf, int offset, int length)
    {
        ByteBuffer out = ByteBuffer.allocate(length);
        
        boolean meetMSB = false;
        byte c;

        int prevPtr = offset;
        int curPtr = offset;
        int maxPtr = offset + length;
        
        for (; curPtr<maxPtr; ++curPtr) {
            c = buf[curPtr];
            if (!isAscii(c)) {
                meetMSB = true;
            } else if (isAsciiSymbol(c)) {
                // current char is a symbol, most likely a punctuation.
                // we treat it as segment delimiter
                if (meetMSB && curPtr > prevPtr) {
                    // this segment contains more than single symbol,
                    // and it has upper ASCII, we need to keep it
                    out.put(buf, prevPtr, (curPtr-prevPtr));
                    out.put((byte)ASCII_SP);
                    prevPtr = curPtr + 1;
                    meetMSB = false;
                } else {
                    // ignore current segment.
                    // (either because it is just a symbol or just an English word)
                    prevPtr = curPtr + 1;
                }
            }
        }
        
        if (meetMSB && curPtr > prevPtr) {
            out.put(buf, prevPtr, (curPtr-prevPtr));
        }
        
        return out;
    }
    
    public ByteBuffer filterWithEnglishLetters(final byte[] buf, int offset, int length)
    {
        ByteBuffer out = ByteBuffer.allocate(length);
        
        boolean isInTag = false;
        byte c;

        int prevPtr = offset;
        int curPtr = offset;
        int maxPtr = offset + length;
        
        for (; curPtr < maxPtr; ++curPtr) {
            c = buf[curPtr];
            
            if (c == ASCII_GT) {
                isInTag = false;
            } else if (c == ASCII_LT) {
                isInTag = true;
            }
            
            if (isAscii(c) && isAsciiSymbol(c)) {
                if (curPtr > prevPtr && !isInTag) {
                    // Current segment contains more than just a symbol
                    // and it is not inside a tag, keep it.
                    out.put(buf, prevPtr, (curPtr-prevPtr));
                    out.put((byte)ASCII_SP);
                    prevPtr = curPtr + 1;
                } else {
                    prevPtr = curPtr + 1;
                }
            }
        }
        
        // If the current segment contains more than just a symbol 
        // and it is not inside a tag then keep it.
        if (!isInTag && curPtr > prevPtr) {
            out.put(buf, prevPtr, (curPtr-prevPtr));
        }
        
        return out;
    }

    private boolean isAscii(byte b)
    {
        return ((b & 0x80) == 0);
    }
    
    // b must be in ASCII code range (MSB can't be 1).
    private boolean isAsciiSymbol(byte b)
    {
        int c = b & 0xFF;
        return ((c < ASCII_A_CAPITAL) ||
                (c > ASCII_Z_CAPITAL && c < ASCII_A) ||
                (c > ASCII_Z));
    }
}
