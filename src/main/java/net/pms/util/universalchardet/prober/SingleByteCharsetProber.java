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

import net.pms.util.universalchardet.prober.sequence.SequenceModel;

public class SingleByteCharsetProber extends CharsetProber
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final int     SAMPLE_SIZE = 64;
    public static final int     SB_ENOUGH_REL_THRESHOLD = 1024;
    public static final float   POSITIVE_SHORTCUT_THRESHOLD = 0.95f;
    public static final float   NEGATIVE_SHORTCUT_THRESHOLD = 0.05f;
    public static final int     SYMBOL_CAT_ORDER = 250;
    public static final int     NUMBER_OF_SEQ_CAT = 4;
    public static final int     POSITIVE_CAT = NUMBER_OF_SEQ_CAT-1;
    public static final int     NEGATIVE_CAT = 0;
    

    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private ProbingState    state;
    private SequenceModel   model;
    private boolean         reversed;
    
    private short           lastOrder;

    private int             totalSeqs;
    private int[]           seqCounters;
    
    private int             totalChar;
    private int             freqChar;
    
    private CharsetProber   nameProber;
    
    
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public SingleByteCharsetProber(SequenceModel model)
    {
        super();
        this.model = model;
        this.reversed = false;
        this.nameProber = null;
        this.seqCounters = new int[NUMBER_OF_SEQ_CAT];
        reset();
    }
    
    public SingleByteCharsetProber(
            SequenceModel model,
            boolean reversed,
            CharsetProber nameProber)
    {
        super();
        this.model = model;
        this.reversed = reversed;
        this.nameProber = nameProber;
        this.seqCounters = new int[NUMBER_OF_SEQ_CAT];
        reset();
    }
    
    boolean keepEnglishLetters()
    {
        return this.model.getKeepEnglishLetter();
    }
    
    
    public String getProberName() {
    	return this.model.getProberName();
    }

    @Override
    public String getCharSetName()
    {
        if (this.nameProber == null) {
            return this.model.getCharsetName();
        } else {
            return this.nameProber.getCharSetName();
        }
    }

    @Override
    public float getConfidence()
    {
        if (this.totalSeqs > 0) {
            float r = 1.0f * this.seqCounters[POSITIVE_CAT] / this.totalSeqs / this.model.getTypicalPositiveRatio();
            r = r * this.freqChar / this.totalChar;
            if (r >= 1.0f) {
                r = 0.99f;
            }
            return r;
        }

        return 0.01f;
    }

    @Override
    public ProbingState getState()
    {
        return this.state;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length)
    {
        short order;
        
        int maxPos = offset + length;
        for (int i=offset; i<maxPos; ++i) {
            order = this.model.getOrder(buf[i]);
            
            if (order < SYMBOL_CAT_ORDER) {
                ++this.totalChar;
            }
            if (order < SAMPLE_SIZE) {
                ++this.freqChar;
                if (this.lastOrder < SAMPLE_SIZE) {
                    ++this.totalSeqs;
                    if (!this.reversed) {
                        ++(this.seqCounters[this.model.getPrecedence(this.lastOrder*SAMPLE_SIZE+order)]);
                    } else {
                        ++(this.seqCounters[this.model.getPrecedence(order*SAMPLE_SIZE+this.lastOrder)]);
                    }
                }
            }
            this.lastOrder = order;
        }
        
        if (this.state == ProbingState.DETECTING) {
            if (this.totalSeqs > SB_ENOUGH_REL_THRESHOLD) {
                float cf = getConfidence();
                if (cf > POSITIVE_SHORTCUT_THRESHOLD) {
                    this.state = ProbingState.FOUND_IT;
                } else if (cf < NEGATIVE_SHORTCUT_THRESHOLD){
                    this.state = ProbingState.NOT_ME;
                }
            }
        }
        
        return this.state;
    }

    @Override
    public void reset()
    {
        this.state = ProbingState.DETECTING;
        this.lastOrder = 255;
        for (int i=0; i<NUMBER_OF_SEQ_CAT; ++i) {
            this.seqCounters[i] = 0;
        }
        this.totalSeqs = 0;
        this.totalChar = 0;
        this.freqChar = 0;
    }

    @Override
    public void setOption()
    {}
}
