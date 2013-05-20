// based on https://github.com/PyYoshi/cChardet.git

package net.pms.util.universalchardet.prober.sequence;

import net.pms.util.universalchardet.Constants;

public class Win1252FrenchModel extends FrenchModel {
public static final float TYPICAL_POSITIVE_RATIO = 0.985451f;
	

	public Win1252FrenchModel() { 
		super(frenchWin1252CharToOrderMap, Constants.CHARSET_WINDOWS_1252, "Win1252FrenchModel");
	}

	/**
	255: Control characters that usually does not exist in any text
	254: Carriage/Return
	253: symbol (punctuation) that does not belong to word
	252: 0 - 9
	 */

	private static final short[] frenchWin1252CharToOrderMap = new short[] {
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255, 30, 40, 31, 37, 27, 44, 48, 52, 32, 45, 61, 25, 29, 41, 43,
		 34, 53, 38, 35, 39, 46, 42, 68, 63, 65, 71,255,255,255,255,255,
		255, 2, 20, 12, 11, 1, 17, 18, 21, 4, 24, 57, 9, 13, 5, 10,
		 14, 19, 7, 3, 6, 8, 16, 54, 23, 28, 36,255,255,255,255,255,
		 64,180,179,178,177, 75,176,175,174,173,172,171, 91,170,169,168,
		167,166, 62,111,110, 67, 78, 88,165, 97,164, 90, 82,163,162,161,
		160,159,158,157,156,155,154, 86, 84, 69,101, 50,109,153, 96,152,
		 81,151,108,150,100,149,148, 80,147,107,146, 51,145,144,143,142,
		 70,141, 79, 72,140,139,138, 73, 77, 60, 76,137,136,135, 99,134,
		106,133,132,131, 83,130, 98, 92,129, 95,128, 94,127,126,125,124,
		 22, 93, 49,123,105,104, 89, 47, 26, 15, 33, 74,122,121, 56, 66,
		120,103,119,118, 55,102, 87,117,116, 59,115, 58, 85,114,113,112,
	};
}
