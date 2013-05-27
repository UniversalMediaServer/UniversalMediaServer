// based on https://github.com/PyYoshi/cChardet.git

package net.pms.util.universalchardet;

import org.mozilla.universalchardet.Constants;
import net.pms.util.universalchardet.FrenchModel;

public class Win1252FinnishModel extends FrenchModel {
public static final float TYPICAL_POSITIVE_RATIO = 0.985451f;
	

	public Win1252FinnishModel() { 
		super(finnishWin1252CharToOrderMap, Constants.CHARSET_WINDOWS_1252);
	}

	/**
	255: Control characters that usually does not exist in any text
	254: Carriage/Return
	253: symbol (punctuation) that does not belong to word
	252: 0 - 9
	 */

	private static final short[] finnishWin1252CharToOrderMap = new short[] {
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255, 30, 48, 45, 50, 22, 46, 40, 29, 27, 32, 24, 33, 21, 26, 31,
		 36, 60, 34, 23, 28, 39, 35, 59, 49, 41, 54,255,255,255,255,255,
		255, 1, 43, 47, 19, 5, 44, 37, 13, 2, 18, 9, 8, 12, 3, 11,
		 17, 57, 15, 6, 4, 10, 14, 51, 58, 16, 56,255,255,255,255,255,
		175,255,174,173,172,171,170,169,168,167,166,165,164,255,163,255,
		255,162,161,160,159,158,157,156,155,154,153,152,151,255,150,149,
		148,147,146,145,144,143, 63,142,141,140,139,138,137,136,135,134,
		133,132,131,130,129,128,127,126,125,124,123, 25,122,121,120,119,
		118,117,116,115, 38,114,113,112,111,110,109,108,107,106,105,104,
		103,102,101,100, 99, 98, 42, 97, 96, 95, 94, 93, 92, 91, 90, 89,
		 88, 87, 62, 86, 7, 55, 85, 52, 84, 53, 83, 82, 81, 80, 79, 78,
		 77, 76, 75, 74, 73, 72, 20, 71, 70, 69, 68, 67, 61, 66, 65, 64,
	};
}
