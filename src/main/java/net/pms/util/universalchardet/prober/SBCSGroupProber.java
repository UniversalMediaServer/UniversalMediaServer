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

import net.pms.configuration.PmsConfiguration;
import net.pms.util.universalchardet.prober.sequence.Latin2CzechModel;

import net.pms.util.universalchardet.prober.sequence.SequenceModel;
import net.pms.util.universalchardet.prober.sequence.Win1250CzechModel;
import net.pms.util.universalchardet.prober.sequence.Win1251Model;
import net.pms.util.universalchardet.prober.sequence.Koi8rModel;
import net.pms.util.universalchardet.prober.sequence.Latin5Model;
import net.pms.util.universalchardet.prober.sequence.MacCyrillicModel;
import net.pms.util.universalchardet.prober.sequence.Ibm866Model;
import net.pms.util.universalchardet.prober.sequence.Ibm855Model;
import net.pms.util.universalchardet.prober.sequence.Latin7Model;
import net.pms.util.universalchardet.prober.sequence.Win1253Model;
import net.pms.util.universalchardet.prober.sequence.Latin5BulgarianModel;
import net.pms.util.universalchardet.prober.sequence.Win1251BulgarianModel;
import net.pms.util.universalchardet.prober.sequence.HebrewModel;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SBCSGroupProber extends CharsetProber
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PmsConfiguration.class);
    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private ProbingState        state;
    private CharsetProber[]     probers;
    private boolean[]           isActive;
    private int                 bestGuess;
    private int                 activeNum;


    // models
    private static final SequenceModel win1251Model = new Win1251Model();
    private static final SequenceModel koi8rModel = new Koi8rModel();
    private static final SequenceModel latin5Model = new Latin5Model();
    private static final SequenceModel macCyrillicModel = new MacCyrillicModel();
    private static final SequenceModel ibm866Model = new Ibm866Model();
    private static final SequenceModel ibm855Model = new Ibm855Model();
    private static final SequenceModel latin7Model = new Latin7Model();
    private static final SequenceModel win1253Model = new Win1253Model();
    private static final SequenceModel latin5BulgarianModel = new Latin5BulgarianModel();
    private static final SequenceModel win1251BulgarianModel = new Win1251BulgarianModel();
    private static final SequenceModel hebrewModel = new HebrewModel();
    private static final SequenceModel win1250CzechModel = new Win1250CzechModel();
	private static final SequenceModel latin2CzechModel = new Latin2CzechModel();
    

    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    public SBCSGroupProber()
    {
        super();

        this.probers = new CharsetProber[15];
        this.isActive = new boolean[15];
        
        this.probers[0] = new SingleByteCharsetProber(win1251Model);
        this.probers[1] = new SingleByteCharsetProber(koi8rModel);
        this.probers[2] = new SingleByteCharsetProber(latin5Model);
        this.probers[3] = new SingleByteCharsetProber(macCyrillicModel);
        this.probers[4] = new SingleByteCharsetProber(ibm866Model);
        this.probers[5] = new SingleByteCharsetProber(ibm855Model);
        this.probers[6] = new SingleByteCharsetProber(latin7Model);
        this.probers[7] = new SingleByteCharsetProber(win1253Model);
        this.probers[8] = new SingleByteCharsetProber(latin5BulgarianModel);
        this.probers[9] = new SingleByteCharsetProber(win1251BulgarianModel);
        
        HebrewProber hebprober = new HebrewProber();
        this.probers[10] = hebprober;
        this.probers[11] = new SingleByteCharsetProber(hebrewModel, false, hebprober);
        this.probers[12] = new SingleByteCharsetProber(hebrewModel, true, hebprober);
        hebprober.setModalProbers(this.probers[11], this.probers[12]);

		this.probers[13] = new SingleByteCharsetProber(latin2CzechModel);
		this.probers[14] = new SingleByteCharsetProber(win1250CzechModel);    
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
        
        LOGGER.debug("Detected by " + this.probers[this.bestGuess].toString());
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
        
        do {
            ByteBuffer newbuf = filterWithoutEnglishLetters(buf, offset, length);
            if (newbuf.position() == 0) {
                break;
            }
            
            for (int i=0; i<this.probers.length; ++i) {
                if (!this.isActive[i]) {
                    continue;
                }
                st = this.probers[i].handleData(newbuf.array(), 0, newbuf.position());
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
        } while (false);
        
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
