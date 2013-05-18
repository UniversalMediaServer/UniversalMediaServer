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

package net.pms.util.universalchardet;


import net.pms.util.universalchardet.prober.CharsetProber;
import net.pms.util.universalchardet.prober.EscCharsetProber;
import net.pms.util.universalchardet.prober.Latin1Prober;
import net.pms.util.universalchardet.prober.MBCSGroupProber;
import net.pms.util.universalchardet.prober.SBCSGroupProber;

public class UniversalDetector
{
    ////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////
    public static final float SHORTCUT_THRESHOLD = 0.95f;
    public static final float MINIMUM_THRESHOLD = 0.20f;
    

    ////////////////////////////////////////////////////////////////
    // inner types
    ////////////////////////////////////////////////////////////////
    public enum InputState
    {
        PURE_ASCII,
        ESC_ASCII,
        HIGHBYTE
    }
    

    ////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////
    private InputState  inputState;
    private boolean     done;
    private boolean     start;
    private boolean     gotData;
    private byte        lastChar;
    private String      detectedCharset;

    private CharsetProber[]     probers;
    private CharsetProber       escCharsetProber;
    
    private CharsetListener     listener;

    
    ////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////
    /**
     * @param listener a listener object that is notified of
     *         the detected encocoding. Can be null.
     */
    public UniversalDetector(CharsetListener listener)
    {
        this.listener = listener;
        this.escCharsetProber = null;
        this.probers = new CharsetProber[3];
        for (int i=0; i<this.probers.length; ++i) {
            this.probers[i] = null;
        }
        
        reset();
    }
    
    public boolean isDone()
    {
        return this.done;
    }
    
    /**
     * @return The detected encoding is returned. If the detector couldn't
     *          determine what encoding was used, null is returned.
     */
    public String getDetectedCharset()
    {
        return this.detectedCharset;
    }
    
    public void setListener(CharsetListener listener)
    {
        this.listener = listener;
    }
    
    public CharsetListener getListener()
    {
        return this.listener;
    }
    
    public void handleData(final byte[] buf, int offset, int length)
    {
        if (this.done) {
            return;
        }
        
        if (length > 0) {
            this.gotData = true;
        }
        
        if (this.start) {
            this.start = false;
            if (length > 3) {
                int b1 = buf[offset] & 0xFF;
                int b2 = buf[offset+1] & 0xFF;
                int b3 = buf[offset+2] & 0xFF;
                int b4 = buf[offset+3] & 0xFF;
                
                switch (b1) {
                case 0xEF:
                    if (b2 == 0xBB && b3 == 0xBF) {
                        this.detectedCharset = Constants.CHARSET_UTF_8;
                    }
                    break;
                case 0xFE:
                    if (b2 == 0xFF && b3 == 0x00 && b4 == 0x00) {
                        this.detectedCharset = Constants.CHARSET_X_ISO_10646_UCS_4_3412;
                    } else if (b2 == 0xFF) {
                        this.detectedCharset = Constants.CHARSET_UTF_16BE;
                    }
                    break;
                case 0x00:
                    if (b2 == 0x00 && b3 == 0xFE && b4 == 0xFF) {
                        this.detectedCharset = Constants.CHARSET_UTF_32BE;
                    } else if (b2 == 0x00 && b3 == 0xFF && b4 == 0xFE) {
                        this.detectedCharset = Constants.CHARSET_X_ISO_10646_UCS_4_2143;
                    }
                    break;
                case 0xFF:
                    if (b2 == 0xFE && b3 == 0x00 && b4 == 0x00) {
                        this.detectedCharset = Constants.CHARSET_UTF_32LE;
                    } else if (b2 == 0xFE) {
                        this.detectedCharset = Constants.CHARSET_UTF_16LE;
                    }
                    break;
                } // swich end
                
                if (this.detectedCharset != null) {
                    this.done = true;
                    return;
                }
            }
        } // if (start) end
        
        int maxPos = offset + length;
        for (int i=offset; i<maxPos; ++i) {
            int c = buf[i] & 0xFF;
            if ((c & 0x80) != 0 && c != 0xA0) {
                if (this.inputState != InputState.HIGHBYTE) {
                    this.inputState = InputState.HIGHBYTE;
                    
                    if (this.escCharsetProber != null) {
                        this.escCharsetProber = null;
                    }
                    
                    if (this.probers[0] == null) {
                        this.probers[0] = new MBCSGroupProber();
                    }
                    if (this.probers[1] == null) {
                        this.probers[1] = new SBCSGroupProber();
                    }
                    if (this.probers[2] == null) {
                        this.probers[2] = new Latin1Prober();
                    }
                }
            } else {
                if (this.inputState == InputState.PURE_ASCII &&
                    (c == 0x1B || (c == 0x7B && this.lastChar == 0x7E))) {
                    this.inputState = InputState.ESC_ASCII;
                }
                this.lastChar = buf[i];
            }
        } // for end
        
        CharsetProber.ProbingState st;
        if (this.inputState == InputState.ESC_ASCII) {
            if (this.escCharsetProber == null) {
                this.escCharsetProber = new EscCharsetProber();
            }
            st = this.escCharsetProber.handleData(buf, offset, length);
            if (st == CharsetProber.ProbingState.FOUND_IT) {
                this.done = true;
                this.detectedCharset = this.escCharsetProber.getCharSetName();
            }
        } else if (this.inputState == InputState.HIGHBYTE) {
            for (int i=0; i<this.probers.length; ++i) {
                st = this.probers[i].handleData(buf, offset, length);
                if (st == CharsetProber.ProbingState.FOUND_IT) {
                    this.done = true;
                    this.detectedCharset = this.probers[i].getCharSetName();
                    return;
                }
            }
        } else { // pure ascii
            // do nothing
        }
    }
    
    public void dataEnd()
    {
        if (!this.gotData) {
            return;
        }
        
        if (this.detectedCharset != null) {
            this.done = true;
            if (this.listener != null) {
                this.listener.report(this.detectedCharset);
            }
            return;
        }
        
        if (this.inputState == InputState.HIGHBYTE) {
            float proberConfidence;
            float maxProberConfidence = 0.0f;
            int maxProber = 0;
            
            for (int i=0; i<this.probers.length; ++i) {
                proberConfidence = this.probers[i].getConfidence();
                if (proberConfidence > maxProberConfidence) {
                    maxProberConfidence = proberConfidence;
                    maxProber = i;
                }
            }
            
            if (maxProberConfidence > MINIMUM_THRESHOLD) {
                this.detectedCharset = this.probers[maxProber].getCharSetName();
                if (this.listener != null) {
                    this.listener.report(this.detectedCharset);
                }
            }
        } else if (this.inputState == InputState.ESC_ASCII) {
            // do nothing
        } else {
            // do nothing
        }
    }
    
    public void reset()
    {
        this.done = false;
        this.start = true;
        this.detectedCharset = null;
        this.gotData = false;
        this.inputState = InputState.PURE_ASCII;
        this.lastChar = 0;
        
        if (this.escCharsetProber != null) {
            this.escCharsetProber.reset();
        }
        
        for (int i=0; i<this.probers.length; ++i) {
            if (this.probers[i] != null) {
                this.probers[i].reset();
            }
        }
    }
    
    
    ////////////////////////////////////////////////////////////////
    // testing
    ////////////////////////////////////////////////////////////////
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.out.println("USAGE: java UniversalDetector filename");
            return;
        }

        UniversalDetector detector = new UniversalDetector(
                new CharsetListener() {
                    public void report(String name)
                    {
                        System.out.println("charset = " + name);
                    }
                }
                );
        
        byte[] buf = new byte[4096];
        java.io.FileInputStream fis = new java.io.FileInputStream(args[0]);
        
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        
        fis.close();
        detector.dataEnd();
    }
}
