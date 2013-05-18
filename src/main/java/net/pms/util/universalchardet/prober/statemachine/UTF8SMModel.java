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

import static net.pms.util.universalchardet.prober.statemachine.PkgInt.INDEX_SHIFT_4BITS;
import static net.pms.util.universalchardet.prober.statemachine.PkgInt.SHIFT_MASK_4BITS;
import static net.pms.util.universalchardet.prober.statemachine.PkgInt.BIT_SHIFT_4BITS;
import static net.pms.util.universalchardet.prober.statemachine.PkgInt.UNIT_MASK_4BITS;
import net.pms.util.universalchardet.Constants;


public class UTF8SMModel extends SMModel
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final int UTF8_CLASS_FACTOR = 16;
    
    
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public UTF8SMModel()
    {
        super(
                new PkgInt(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, utf8ClassTable),
                UTF8_CLASS_FACTOR,
                new PkgInt(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, utf8StateTable),
                utf8CharLenTable,
                Constants.CHARSET_UTF_8
                );
    }
    

    ////////////////////////////////////////////////////////////////
    // constants continued
    ////////////////////////////////////////////////////////////////
    private static int[] utf8ClassTable = new int[] {
//        PkgInt.pack4bits(0,1,1,1,1,1,1,1),  // 00 - 07 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 00 - 07  //allow 0x00 as a legal value
        PkgInt.pack4bits(1,1,1,1,1,1,0,0),  // 08 - 0f 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 10 - 17 
        PkgInt.pack4bits(1,1,1,0,1,1,1,1),  // 18 - 1f 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 20 - 27 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 28 - 2f 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 30 - 37 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 38 - 3f 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 40 - 47 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 48 - 4f 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 50 - 57 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 58 - 5f 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 60 - 67 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 68 - 6f 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 70 - 77 
        PkgInt.pack4bits(1,1,1,1,1,1,1,1),  // 78 - 7f 
        PkgInt.pack4bits(2,2,2,2,3,3,3,3),  // 80 - 87 
        PkgInt.pack4bits(4,4,4,4,4,4,4,4),  // 88 - 8f 
        PkgInt.pack4bits(4,4,4,4,4,4,4,4),  // 90 - 97 
        PkgInt.pack4bits(4,4,4,4,4,4,4,4),  // 98 - 9f 
        PkgInt.pack4bits(5,5,5,5,5,5,5,5),  // a0 - a7 
        PkgInt.pack4bits(5,5,5,5,5,5,5,5),  // a8 - af 
        PkgInt.pack4bits(5,5,5,5,5,5,5,5),  // b0 - b7 
        PkgInt.pack4bits(5,5,5,5,5,5,5,5),  // b8 - bf 
        PkgInt.pack4bits(0,0,6,6,6,6,6,6),  // c0 - c7 
        PkgInt.pack4bits(6,6,6,6,6,6,6,6),  // c8 - cf 
        PkgInt.pack4bits(6,6,6,6,6,6,6,6),  // d0 - d7 
        PkgInt.pack4bits(6,6,6,6,6,6,6,6),  // d8 - df 
        PkgInt.pack4bits(7,8,8,8,8,8,8,8),  // e0 - e7 
        PkgInt.pack4bits(8,8,8,8,8,9,8,8),  // e8 - ef 
        PkgInt.pack4bits(10,11,11,11,11,11,11,11),  // f0 - f7 
        PkgInt.pack4bits(12,13,13,13,14,15,0,0)   // f8 - ff 
    };
    
    private static int[] utf8StateTable = new int[] {
        PkgInt.pack4bits(ERROR,START,ERROR,ERROR,ERROR,ERROR,   12,   10),//00-07 
        PkgInt.pack4bits(    9,   11,    8,    7,    6,    5,    4,    3),//08-0f 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//10-17 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//18-1f 
        PkgInt.pack4bits(ITSME,ITSME,ITSME,ITSME,ITSME,ITSME,ITSME,ITSME),//20-27 
        PkgInt.pack4bits(ITSME,ITSME,ITSME,ITSME,ITSME,ITSME,ITSME,ITSME),//28-2f 
        PkgInt.pack4bits(ERROR,ERROR,    5,    5,    5,    5,ERROR,ERROR),//30-37 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//38-3f 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,    5,    5,    5,ERROR,ERROR),//40-47 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//48-4f 
        PkgInt.pack4bits(ERROR,ERROR,    7,    7,    7,    7,ERROR,ERROR),//50-57 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//58-5f 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,    7,    7,ERROR,ERROR),//60-67 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//68-6f 
        PkgInt.pack4bits(ERROR,ERROR,    9,    9,    9,    9,ERROR,ERROR),//70-77 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//78-7f 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,    9,ERROR,ERROR),//80-87 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//88-8f 
        PkgInt.pack4bits(ERROR,ERROR,   12,   12,   12,   12,ERROR,ERROR),//90-97 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//98-9f 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,   12,ERROR,ERROR),//a0-a7 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//a8-af 
        PkgInt.pack4bits(ERROR,ERROR,   12,   12,   12,ERROR,ERROR,ERROR),//b0-b7 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR),//b8-bf 
        PkgInt.pack4bits(ERROR,ERROR,START,START,START,START,ERROR,ERROR),//c0-c7 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR,ERROR) //c8-cf 
    };
    
    private static int[] utf8CharLenTable = new int[] {
        0, 1, 0, 0, 0, 0, 2, 3,
        3, 3, 4, 4, 5, 5, 6, 6
    };
}
