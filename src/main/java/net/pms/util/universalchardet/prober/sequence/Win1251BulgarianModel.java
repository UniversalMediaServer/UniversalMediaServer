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

import org.mozilla.universalchardet.Constants;


public class Win1251BulgarianModel extends BulgarianModel
{
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public Win1251BulgarianModel()
    {
        super(win1251BulgarianCharToOrderMap, Constants.CHARSET_WINDOWS_1251);
    }
    
    
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    private static final short[] win1251BulgarianCharToOrderMap = new short[] {
        255,255,255,255,255,255,255,255,255,255,254,255,255,254,255,255,  //00
        255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,  //10
        253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,253,  //20
        252,252,252,252,252,252,252,252,252,252,253,253,253,253,253,253,  //30
        253, 77, 90, 99,100, 72,109,107,101, 79,185, 81,102, 76, 94, 82,  //40
        110,186,108, 91, 74,119, 84, 96,111,187,115,253,253,253,253,253,  //50
        253, 65, 69, 70, 66, 63, 68,112,103, 92,194,104, 95, 86, 87, 71,  //60
        116,195, 85, 93, 97,113,196,197,198,199,200,253,253,253,253,253,  //70
        206,207,208,209,210,211,212,213,120,214,215,216,217,218,219,220,  //80
        221, 78, 64, 83,121, 98,117,105,222,223,224,225,226,227,228,229,  //90
         88,230,231,232,233,122, 89,106,234,235,236,237,238, 45,239,240,  //a0
         73, 80,118,114,241,242,243,244,245, 62, 58,246,247,248,249,250,  //b0
         31, 32, 35, 43, 37, 44, 55, 47, 40, 59, 33, 46, 38, 36, 41, 30,  //c0
         39, 28, 34, 51, 48, 49, 53, 50, 54, 57, 61,251, 67,252, 60, 56,  //d0
          1, 18,  9, 20, 11,  3, 23, 15,  2, 26, 12, 10, 14,  6,  4, 13,  //e0
          7,  8,  5, 19, 29, 25, 22, 21, 27, 24, 17, 75, 52,253, 42, 16,  //f0
    };
}
