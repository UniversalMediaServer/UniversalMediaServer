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
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1998
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Kohei TAKETA <k-tak@void.in> (Java port)
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

package net.pms.util.universalchardet.prober.statemachine;

public class PkgInt
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final int INDEX_SHIFT_4BITS   = 3;
    public static final int INDEX_SHIFT_8BITS   = 2;
    public static final int INDEX_SHIFT_16BITS  = 1;
    
    public static final int SHIFT_MASK_4BITS    = 7;
    public static final int SHIFT_MASK_8BITS    = 3;
    public static final int SHIFT_MASK_16BITS   = 1;
    
    public static final int BIT_SHIFT_4BITS     = 2;
    public static final int BIT_SHIFT_8BITS     = 3;
    public static final int BIT_SHIFT_16BITS    = 4;
    
    public static final int UNIT_MASK_4BITS     = 0x0000000F;
    public static final int UNIT_MASK_8BITS     = 0x000000FF;
    public static final int UNIT_MASK_16BITS    = 0x0000FFFF;

    
    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private int     indexShift;
    private int     shiftMask;
    private int     bitShift;
    private int     unitMask;
    private int[]   data;
    

    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public PkgInt(
            int indexShift,
            int shiftMask,
            int bitShift,
            int unitMask,
            int[] data)
    {
        this.indexShift = indexShift;
        this.shiftMask = shiftMask;
        this.bitShift = bitShift;
        this.unitMask = unitMask;
        this.data = data;
    }
    
    public static int pack16bits(int a, int b)
    {
        return ((b << 16) | a);
    }
    
    public static int pack8bits(int a, int b, int c, int d)
    {
        return pack16bits(
                (b << 8) | a,
                (d << 8) | c
                );
    }
    
    public static int pack4bits(int a, int b, int c, int d, int e, int f, int g, int h)
    {
        return pack8bits(
                (b << 4) | a,
                (d << 4) | c,
                (f << 4) | e,
                (h << 4) | g
                );
    }
    
    public int unpack(int i)
    {
        return (
                (this.data[i>>this.indexShift] >> ((i&this.shiftMask)<<this.bitShift)) & this.unitMask
                );
    }
}
