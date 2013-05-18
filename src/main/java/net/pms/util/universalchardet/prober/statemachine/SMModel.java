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

public abstract class SMModel
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final int START    = 0;
    public static final int ERROR    = 1;
    public static final int ITSME    = 2;
    

    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    protected PkgInt    classTable;
    protected int       classFactor;
    protected PkgInt    stateTable;
    protected int[]     charLenTable;
    protected String    name;
    
    
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public SMModel(
            PkgInt classTable,
            int classFactor,
            PkgInt stateTable,
            int[] charLenTable,
            String name)
    {
        this.classTable = classTable;
        this.classFactor = classFactor;
        this.stateTable = stateTable;
        this.charLenTable = charLenTable;
        this.name = name;
    }
    
    public int getClass(byte b)
    {
        int c = b & 0xFF;
        return this.classTable.unpack(c);
    }
    
    public int getNextState(int cls, int currentState)
    {
        return this.stateTable.unpack(currentState * this.classFactor + cls);
    }
    
    public int getCharLen(int cls)
    {
        return this.charLenTable[cls];
    }
    
    public String getName()
    {
        return this.name;
    }
}
