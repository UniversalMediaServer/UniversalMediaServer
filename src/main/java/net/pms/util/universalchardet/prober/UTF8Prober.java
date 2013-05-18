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

package net.pms.util.universalchardet.prober;

import net.pms.util.universalchardet.prober.statemachine.CodingStateMachine;
import net.pms.util.universalchardet.prober.statemachine.SMModel;
import net.pms.util.universalchardet.prober.statemachine.UTF8SMModel;
import net.pms.util.universalchardet.Constants;


public class UTF8Prober extends CharsetProber
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final float ONE_CHAR_PROB = 0.50f;
    

    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private CodingStateMachine  codingSM;
    private ProbingState        state;
    private int                 numOfMBChar;
    
    private static final SMModel smModel = new UTF8SMModel();
    

    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public UTF8Prober()
    {
        super();
        this.numOfMBChar = 0;
        this.codingSM = new CodingStateMachine(smModel);

        reset();
    }

    public String getCharSetName()
    {
        return Constants.CHARSET_UTF_8;
    }

    public ProbingState handleData(final byte[] buf, int offset, int length)
    {
        int codingState;

        int maxPos = offset + length;
        for (int i=offset; i<maxPos; ++i) {
            codingState = this.codingSM.nextState(buf[i]);
            if (codingState == SMModel.ERROR) {
                this.state = ProbingState.NOT_ME;
                break;
            }
            if (codingState == SMModel.ITSME) {
                this.state = ProbingState.FOUND_IT;
                break;
            }
            if (codingState == SMModel.START) {
                if (this.codingSM.getCurrentCharLen() >= 2) {
                    ++this.numOfMBChar;
                }
            }
        }
        
        if (this.state == ProbingState.DETECTING) {
            if (getConfidence() > SHORTCUT_THRESHOLD) {
                this.state = ProbingState.FOUND_IT;
            }
        }
        
        return this.state;
    }

    public ProbingState getState()
    {
        return this.state;
    }

    public void reset()
    {
        this.codingSM.reset();
        this.numOfMBChar = 0;
        this.state = ProbingState.DETECTING;
    }

    public float getConfidence()
    {
        float unlike = 0.99f;
        
        if (this.numOfMBChar < 6) {
            for (int i=0; i<this.numOfMBChar; ++i) {
                unlike *= ONE_CHAR_PROB;
            }
            return (1.0f - unlike);
        } else {
            return 0.99f;
        }
    }

    public void setOption()
    {}
}
