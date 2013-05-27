// based on https://github.com/PyYoshi/cChardet.git

package net.pms.util.universalchardet;

import org.mozilla.universalchardet.Constants;
import net.pms.util.universalchardet.GermanModel;

public class Win1252GermanModel extends GermanModel {
public static final float TYPICAL_POSITIVE_RATIO = 0.947368f;
	

	public Win1252GermanModel() { 
		super(germanWin1252CharToOrderMap, Constants.CHARSET_WINDOWS_1252);
	}

	/**
	255: Control characters that usually does not exist in any text
	254: Carriage/Return
	253: symbol (punctuation) that does not belong to word
	252: 0 - 9
	 */

	private static final short[] germanWin1252CharToOrderMap = new short[] {
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255, 30, 34, 50, 25, 27, 36, 31, 28, 35, 49, 41, 39, 32, 42, 48,
		 45, 58, 44, 21, 37, 40, 43, 29, 62, 61, 47,255,255,255,255,255,
		255, 8, 16, 12, 9, 1, 18, 13, 7, 3, 46, 20, 11, 14, 2, 15,
		 33, 53, 4, 5, 6, 10, 23, 17, 55, 51, 19,255,255,255,255,255,
		180,179,178,177,176,175,174,173,172,171,170, 64,169,168,167,166,
		165,164, 54,163,162,161, 66,160,159,158,157, 63,156,155,154,153,
		 65,152,151,150,149,148,147,146,145,144,143, 57,142,141,140,139,
		138,137,136,135,134,133,132,131,130,129,128, 56,127,126,125,124,
		123,122,121,120, 59,119,118, 71,117,116,115,114,113,112,111,110,
		109,108,107,106,105,104, 60,103,102,101,100, 99, 52, 98, 97, 26,
		 70, 96, 69, 95, 24, 94, 68, 93, 92, 67, 91, 90, 89, 88, 87, 86,
		 85, 84, 83, 82, 81, 80, 38, 79, 78, 77, 76, 75, 22, 74, 73, 72,
	};
}
