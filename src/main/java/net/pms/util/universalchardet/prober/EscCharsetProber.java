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

import org.mozilla.universalchardet.prober.statemachine.CodingStateMachine;
import org.mozilla.universalchardet.prober.statemachine.SMModel;
import org.mozilla.universalchardet.prober.statemachine.HZSMModel;
import org.mozilla.universalchardet.prober.statemachine.ISO2022CNSMModel;
import org.mozilla.universalchardet.prober.statemachine.ISO2022JPSMModel;
import org.mozilla.universalchardet.prober.statemachine.ISO2022KRSMModel;


public class EscCharsetProber extends CharsetProber
{
    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private CodingStateMachine[]    codingSM;
    private int                     activeSM;
    private ProbingState            state;
    private String                  detectedCharset;
    
    private static final HZSMModel hzsModel = new HZSMModel();
    private static final ISO2022CNSMModel iso2022cnModel = new ISO2022CNSMModel();
    private static final ISO2022JPSMModel iso2022jpModel = new ISO2022JPSMModel();
    private static final ISO2022KRSMModel iso2022krModel = new ISO2022KRSMModel();


    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public EscCharsetProber()
    {
        super();

        this.codingSM = new CodingStateMachine[4];
        this.codingSM[0] = new CodingStateMachine(hzsModel);
        this.codingSM[1] = new CodingStateMachine(iso2022cnModel);
        this.codingSM[2] = new CodingStateMachine(iso2022jpModel);
        this.codingSM[3] = new CodingStateMachine(iso2022krModel);

        reset();
    }
    
    @Override
    public String getCharSetName()
    {
        return this.detectedCharset;
    }

    @Override
    public float getConfidence()
    {
        return 0.99f;
    }

    @Override
    public ProbingState getState()
    {
        return this.state;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length)
    {
        int codingState;
        
        int maxPos = offset + length;
        for (int i=offset; i<maxPos && this.state==ProbingState.DETECTING; ++i) {
            for (int j=this.activeSM-1; j>=0; --j) {
                codingState = this.codingSM[j].nextState(buf[i]);
                if (codingState == SMModel.ERROR) {
                    --this.activeSM;
                    if (this.activeSM <= 0) {
                        this.state = ProbingState.NOT_ME;
                        return this.state;
                    } else if (j != this.activeSM) {
                        CodingStateMachine t;
                        t = this.codingSM[this.activeSM];
                        this.codingSM[this.activeSM] = this.codingSM[j];
                        this.codingSM[j] = t;
                    }
                } else if (codingState == SMModel.ITSME) {
                    this.state = ProbingState.FOUND_IT;
                    this.detectedCharset = this.codingSM[j].getCodingStateMachine();
                    return this.state;
                }
            }
        }
        
        return this.state;
    }

    @Override
    public void reset()
    {
        this.state = ProbingState.DETECTING;
        for (int i=0; i<this.codingSM.length; ++i) {
            this.codingSM[i].reset();
        }
        this.activeSM = this.codingSM.length;
        this.detectedCharset = null;
    }

    @Override
    public void setOption()
    {}
}
