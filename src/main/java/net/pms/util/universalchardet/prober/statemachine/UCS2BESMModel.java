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

import static org.mozilla.universalchardet.prober.statemachine.PkgInt.INDEX_SHIFT_4BITS;
import static org.mozilla.universalchardet.prober.statemachine.PkgInt.SHIFT_MASK_4BITS;
import static org.mozilla.universalchardet.prober.statemachine.PkgInt.BIT_SHIFT_4BITS;
import static org.mozilla.universalchardet.prober.statemachine.PkgInt.UNIT_MASK_4BITS;
import org.mozilla.universalchardet.Constants;


public class UCS2BESMModel extends SMModel
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final int UCS2BE_CLASS_FACTOR = 6;
    
    
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public UCS2BESMModel()
    {
        super(
                new PkgInt(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, ucs2beClassTable),
                UCS2BE_CLASS_FACTOR,
                new PkgInt(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, ucs2beStateTable),
                ucs2beCharLenTable,
                Constants.CHARSET_UTF_16BE
                );
    }
    

    ////////////////////////////////////////////////////////////////
    // constants continued
    ////////////////////////////////////////////////////////////////
    private static int[] ucs2beClassTable = new int[] {
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 00 - 07 
        PkgInt.pack4bits(0,0,1,0,0,2,0,0),  // 08 - 0f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 10 - 17 
        PkgInt.pack4bits(0,0,0,3,0,0,0,0),  // 18 - 1f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 20 - 27 
        PkgInt.pack4bits(0,3,3,3,3,3,0,0),  // 28 - 2f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 30 - 37 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 38 - 3f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 40 - 47 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 48 - 4f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 50 - 57 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 58 - 5f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 60 - 67 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 68 - 6f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 70 - 77 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 78 - 7f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 80 - 87 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 88 - 8f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 90 - 97 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // 98 - 9f 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // a0 - a7 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // a8 - af 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // b0 - b7 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // b8 - bf 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // c0 - c7 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // c8 - cf 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // d0 - d7 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // d8 - df 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // e0 - e7 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // e8 - ef 
        PkgInt.pack4bits(0,0,0,0,0,0,0,0),  // f0 - f7 
        PkgInt.pack4bits(0,0,0,0,0,0,4,5)   // f8 - ff 
    };
    
    private static int[] ucs2beStateTable = new int[] {
        PkgInt.pack4bits(    5,    7,    7,ERROR,    4,    3,ERROR,ERROR),//00-07 
        PkgInt.pack4bits(ERROR,ERROR,ERROR,ERROR,ITSME,ITSME,ITSME,ITSME),//08-0f 
        PkgInt.pack4bits(ITSME,ITSME,    6,    6,    6,    6,ERROR,ERROR),//10-17 
        PkgInt.pack4bits(    6,    6,    6,    6,    6,ITSME,    6,    6),//18-1f 
        PkgInt.pack4bits(    6,    6,    6,    6,    5,    7,    7,ERROR),//20-27 
        PkgInt.pack4bits(    5,    8,    6,    6,ERROR,    6,    6,    6),//28-2f 
        PkgInt.pack4bits(    6,    6,    6,    6,ERROR,ERROR,START,START) //30-37 
    };
    
    private static int[] ucs2beCharLenTable = new int[] {
        2, 2, 2, 0, 2, 2
    };
}
