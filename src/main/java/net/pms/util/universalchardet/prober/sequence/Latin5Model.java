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


public class Latin5Model extends CyrillicModel
{
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public Latin5Model()
    {
        super(latin5CharToOrderMap, Constants.CHARSET_ISO_8859_5, "Latin5Model");
    }
    
    
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    private static final short[] latin5CharToOrderMap = new short[] {
        255,255,255,255,255,255,255,255,255,255,254,255,255,254,255,255,  //00
        255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,  //10
        253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,  //20
        252,252,252,252,252,252,252,252,252,252,253,253,253,253,253,253,  //30
        253,142,143,144,145,146,147,148,149,150,151,152, 74,153, 75,154,  //40
        155,156,157,158,159,160,161,162,163,164,165,253,253,253,253,253,  //50
        253, 71,172, 66,173, 65,174, 76,175, 64,176,177, 77, 72,178, 69,  //60
         67,179, 78, 73,180,181, 79,182,183,184,185,253,253,253,253,253,  //70
        191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,
        207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,
        223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,
         37, 44, 33, 46, 41, 48, 56, 51, 42, 60, 36, 49, 38, 31, 34, 35,
         45, 32, 40, 52, 53, 55, 58, 50, 57, 63, 70, 62, 61, 47, 59, 43,
          3, 21, 10, 19, 13,  2, 24, 20,  4, 23, 11,  8, 12,  5,  1, 15,
          9,  7,  6, 14, 39, 26, 28, 22, 25, 29, 54, 18, 17, 30, 27, 16,
        239, 68,240,241,242,243,244,245,246,247,248,249,250,251,252,255,
    };
}
