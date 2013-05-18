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
 * The Original Code is Mozilla Universal charset detector code.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 2001
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *          Shy Shalom <shooshX@gmail.com>
 *          Kohei TAKETA <k-tak@void.in> (Java port)
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


public class MBCSGroupProber extends CharsetProber
{
    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private ProbingState        state;
    private CharsetProber[]     probers;
    private boolean[]           isActive;
    private int                 bestGuess;
    private int                 activeNum;


    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public MBCSGroupProber()
    {
        super();
        
        this.probers = new CharsetProber[7];
        this.isActive = new boolean[7];
        
        this.probers[0] = new UTF8Prober();
        this.probers[1] = new SJISProber();
        this.probers[2] = new EUCJPProber();
        this.probers[3] = new GB18030Prober();
        this.probers[4] = new EUCKRProber();
        this.probers[5] = new Big5Prober();
        this.probers[6] = new EUCTWProber();
        
        reset();
    }

    @Override
    public String getCharSetName()
    {
        if (this.bestGuess == -1) {
            getConfidence();
            if (this.bestGuess == -1) {
                this.bestGuess = 0;
            }
        }
        return this.probers[this.bestGuess].getCharSetName();
    }

    @Override
    public float getConfidence()
    {
        float bestConf = 0.0f;
        float cf;

        if (this.state == ProbingState.FOUND_IT) {
            return 0.99f;
        } else if (this.state == ProbingState.NOT_ME) {
            return 0.01f;
        } else {
            for (int i=0; i<probers.length; ++i) {
                if (!this.isActive[i]) {
                    continue;
                }
                
                cf = this.probers[i].getConfidence();
                if (bestConf < cf) {
                    bestConf = cf;
                    this.bestGuess = i;
                }
            }
        }

        return bestConf;
    }

    @Override
    public ProbingState getState()
    {
        return this.state;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length)
    {
        ProbingState st;
        
        boolean keepNext = true;
        byte[] highbyteBuf = new byte[length];
        int highpos = 0;

        int maxPos = offset + length;
        for (int i=offset; i<maxPos; ++i) {
            if ((buf[i] & 0x80) != 0) {
                highbyteBuf[highpos++] = buf[i];
                keepNext = true;
            } else {
                //if previous is highbyte, keep this even it is a ASCII
                if (keepNext) {
                    highbyteBuf[highpos++] = buf[i];
                    keepNext = false;
                }
            }
        }
        
        for (int i=0; i<this.probers.length; ++i) {
            if (!this.isActive[i]) {
                continue;
            }
            st = this.probers[i].handleData(highbyteBuf, 0, highpos);
            if (st == ProbingState.FOUND_IT) {
                this.bestGuess = i;
                this.state = ProbingState.FOUND_IT;
                break;
            } else if (st == ProbingState.NOT_ME) {
                this.isActive[i] = false;
                --this.activeNum;
                if (this.activeNum <= 0) {
                    this.state = ProbingState.NOT_ME;
                    break;
                }
            }
        }
        
        return this.state;
    }

    @Override
    public void reset()
    {
        this.activeNum = 0;
        for (int i=0; i<this.probers.length; ++i) {
            this.probers[i].reset();
            this.isActive[i] = true;
            ++this.activeNum;
        }
        this.bestGuess = -1;
        this.state = ProbingState.DETECTING;
    }

    @Override
    public void setOption()
    {}
}
