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

package net.pms.util.universalchardet.prober.distributionanalysis;


public abstract class CharDistributionAnalysis
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final float   SURE_NO = 0.01f;
    public static final float   SURE_YES = 0.99f;
    public static final int     ENOUGH_DATA_THRESHOLD = 1024;
    public static final int     MINIMUM_DATA_THRESHOLD = 4;
    

    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private int         freqChars;
    private int         totalChars;
    protected int[]     charToFreqOrder; // set by subclasses
    protected float     typicalDistributionRatio; // set by subclasses
    protected boolean   done; // set by subclasses and reset()
    

    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public CharDistributionAnalysis()
    {
        reset();
    }
    
    public void handleData(final byte[] buf, int offset, int length)
    {}
    
    public void handleOneChar(final byte[] buf, int offset, int charLength)
    {
        int order = -1;
        
        if (charLength == 2) {
            order = getOrder(buf, offset);
        }
        
        if (order >= 0) {
            ++this.totalChars;
            if (order < this.charToFreqOrder.length) {
                if (512 > this.charToFreqOrder[order]) {
                    ++this.freqChars;
                }
            }
        }
    }
    
    public float getConfidence()
    {
        if (this.totalChars <= 0 || this.freqChars <= MINIMUM_DATA_THRESHOLD) {
            return SURE_NO;
        }
        
        if (this.totalChars != this.freqChars) {
            float r = this.freqChars / (this.totalChars - this.freqChars) * this.typicalDistributionRatio;
            
            if (r < SURE_YES) {
                return r;
            }
        }
        
        return SURE_YES;
    }
    
    public void reset()
    {
        this.done = false;
        this.totalChars = 0;
        this.freqChars = 0;
    }
    
    public void setOption()
    {}
    
    public boolean gotEnoughData()
    {
        return (this.totalChars > ENOUGH_DATA_THRESHOLD);
    }
    
    protected abstract int getOrder(final byte[] buf, int offset);
}
