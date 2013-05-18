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

public class CodingStateMachine
{
    protected SMModel    model;
    protected int        currentState;
    protected int        currentCharLen;
    protected int        currentBytePos;
    
    public CodingStateMachine(SMModel model)
    {
        this.model = model;
        this.currentState = SMModel.START;
    }
    
    public int nextState(byte c)
    {
        int byteCls = this.model.getClass(c);
        if (this.currentState == SMModel.START) {
            this.currentBytePos = 0;
            this.currentCharLen = this.model.getCharLen(byteCls);
        }
        
        this.currentState = this.model.getNextState(byteCls, this.currentState);
        ++this.currentBytePos;
        
        return this.currentState;
    }
    
    public int getCurrentCharLen()
    {
        return this.currentCharLen;
    }
    
    public void reset()
    {
        this.currentState = SMModel.START;
    }
    
    public String getCodingStateMachine()
    {
        return this.model.getName();
    }
}
