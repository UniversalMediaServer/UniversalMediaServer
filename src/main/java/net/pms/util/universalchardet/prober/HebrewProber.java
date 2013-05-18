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
 *          Shy Shalom <shooshX@gmail.com>
 * Portions created by the Initial Developer are Copyright (C) 2005
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

import net.pms.util.universalchardet.Constants;


public class HebrewProber extends CharsetProber
{
    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    public static final int FINAL_KAF   = 0xEA;
    public static final int NORMAL_KAF  = 0xEB;
    public static final int FINAL_MEM   = 0xED;
    public static final int NORMAL_MEM  = 0xEE;
    public static final int FINAL_NUN   = 0xEF;
    public static final int NORMAL_NUN  = 0xF0;
    public static final int FINAL_PE    = 0xF3;
    public static final int NORMAL_PE   = 0xF4;
    public static final int FINAL_TSADI = 0xF5;
    public static final int NORMAL_TSADI= 0xF6;
    
    public static final byte SPACE      = 0x20;
    
    public static final int MIN_FINAL_CHAR_DISTANCE = 5;
    public static final float MIN_MODEL_DISTANCE = 0.01f;
    

    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private int     finalCharLogicalScore;
    private int     finalCharVisualScore;
    private byte    prev;
    private byte    beforePrev;
    
    private CharsetProber logicalProber;
    private CharsetProber visualProber;


    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public HebrewProber()
    {
        super();
        this.logicalProber = null;
        this.visualProber = null;
        reset();
    }
    
    public void setModalProbers(CharsetProber logicalProber, CharsetProber visualProber)
    {
        this.logicalProber = logicalProber;
        this.visualProber = visualProber;
    }

    @Override
    public String getCharSetName()
    {
        // If the final letter score distance is dominant enough, rely on it.
        int finalsub = this.finalCharLogicalScore - this.finalCharVisualScore;
        if (finalsub >= MIN_FINAL_CHAR_DISTANCE) {
            return Constants.CHARSET_WINDOWS_1255;
        }
        if (finalsub <= -MIN_FINAL_CHAR_DISTANCE) {
            return Constants.CHARSET_ISO_8859_8;
        }
        
        // It's not dominant enough, try to rely on the model scores instead.
        float modelsub = this.logicalProber.getConfidence() - this.visualProber.getConfidence();
        if (modelsub > MIN_MODEL_DISTANCE) {
            return Constants.CHARSET_WINDOWS_1255;
        }
        if (modelsub < -MIN_MODEL_DISTANCE) {
            return Constants.CHARSET_ISO_8859_8;
        }
        
        // Still no good, back to final letter distance, maybe it'll save the day.
        if (finalsub < 0) {
            return Constants.CHARSET_ISO_8859_8;
        }
        
        // (finalsub > 0 - Logical) or (don't know what to do) default to Logical.
        return Constants.CHARSET_WINDOWS_1255;
    }

    @Override
    public float getConfidence()
    {
        return 0.0f;
    }

    @Override
    public ProbingState getState()
    {
        // Remain active as long as any of the model probers are active.
        if ((this.logicalProber.getState() == ProbingState.NOT_ME) &&
            (this.visualProber.getState() == ProbingState.NOT_ME)) {
            return ProbingState.NOT_ME;
        }

        return ProbingState.DETECTING;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length)
    {
        if (getState() == ProbingState.NOT_ME) {
            return ProbingState.NOT_ME;
        }
        
        byte c;
        int maxPos = offset + length;
        for (int i=offset; i<maxPos; ++i) {
            c = buf[i];
            if (c == SPACE) {
                if (this.beforePrev != SPACE) {
                    if (isFinal(this.prev)) {
                        ++this.finalCharLogicalScore;
                    } else if (isNonFinal(this.prev)) {
                        ++this.finalCharVisualScore;
                    }
                }
            } else {
                if ((this.beforePrev == SPACE) &&
                     isFinal(this.prev) &&
                     c != SPACE) {
                    ++this.finalCharVisualScore;
                }
            }
            this.beforePrev = this.prev;
            this.prev = c;
        }
        
        return ProbingState.DETECTING;
    }

    @Override
    public void reset()
    {
        this.finalCharLogicalScore = 0;
        this.finalCharVisualScore = 0;
        
        // mPrev and mBeforePrev are initialized to space in order to simulate a word 
        // delimiter at the beginning of the data
        this.prev = SPACE;
        this.beforePrev = SPACE;
    }

    @Override
    public void setOption()
    {}
    
    protected static boolean isFinal(byte b)
    {
        int c = b & 0xFF;
        return (
                c == FINAL_KAF ||
                c == FINAL_MEM ||
                c == FINAL_NUN ||
                c == FINAL_PE ||
                c == FINAL_TSADI
                );
    }
    
    protected static boolean isNonFinal(byte b)
    {
        int c = b & 0xFF;
        return (
                c == NORMAL_KAF ||
                c == NORMAL_MEM ||
                c == NORMAL_NUN ||
                c == NORMAL_PE
                );
        // The normal Tsadi is not a good Non-Final letter due to words like 
        // 'lechotet' (to chat) containing an apostrophe after the tsadi. This 
        // apostrophe is converted to a space in FilterWithoutEnglishLetters causing 
        // the Non-Final tsadi to appear at an end of a word even though this is not 
        // the case in the original text.
        // The letters Pe and Kaf rarely display a related behavior of not being a 
        // good Non-Final letter. Words like 'Pop', 'Winamp' and 'Mubarak' for 
        // example legally end with a Non-Final Pe or Kaf. However, the benefit of 
        // these letters as Non-Final letters outweighs the damage since these words 
        // are quite rare.
    }

	@Override
	public String getProberName() {
		// TODO Auto-generated method stub
		return null;
	}
}
