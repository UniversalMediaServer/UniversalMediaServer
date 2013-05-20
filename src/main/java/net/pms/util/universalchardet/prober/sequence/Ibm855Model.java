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
 * The Original Code is Mozilla Communicator client code.
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

package net.pms.util.universalchardet.prober.sequence;

import net.pms.util.universalchardet.Constants;


public class Ibm855Model extends CyrillicModel
{
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public Ibm855Model()
    {
        super(ibm855CharToOrderMap, Constants.CHARSET_IBM855, "Ibm855Model");
    }
    
    
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    private static final short[] ibm855CharToOrderMap = new short[] {
        255,255,255,255,255,255,255,255,255,255,254,255,255,254,255,255,  //00
        255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,  //10
        253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,  //20
        252,252,252,252,252,252,252,252,252,252,253,253,253,253,253,253,  //30
        253,142,143,144,145,146,147,148,149,150,151,152, 74,153, 75,154,  //40
        155,156,157,158,159,160,161,162,163,164,165,253,253,253,253,253,  //50
        253, 71,172, 66,173, 65,174, 76,175, 64,176,177, 77, 72,178, 69,  //60
         67,179, 78, 73,180,181, 79,182,183,184,185,253,253,253,253,253,  //70
        191,192,193,194, 68,195,196,197,198,199,200,201,202,203,204,205,
        206,207,208,209,210,211,212,213,214,215,216,217, 27, 59, 54, 70,
          3, 37, 21, 44, 28, 58, 13, 41,  2, 48, 39, 53, 19, 46,218,219,
        220,221,222,223,224, 26, 55,  4, 42,225,226,227,228, 23, 60,229,
        230,231,232,233,234,235, 11, 36,236,237,238,239,240,241,242,243,
          8, 49, 12, 38,  5, 31,  1, 34, 15,244,245,246,247, 35, 16,248,
         43,  9, 45,  7, 32,  6, 40, 14, 52, 24, 56, 10, 33, 17, 61,249,
        250, 18, 62, 20, 51, 25, 57, 30, 47, 29, 63, 22, 50,251,252,255,
    };
}
