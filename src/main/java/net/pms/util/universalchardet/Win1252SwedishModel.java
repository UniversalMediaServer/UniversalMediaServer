// based on https://github.com/PyYoshi/cChardet.git

package net.pms.util.universalchardet;

import net.pms.util.universalchardet.FrenchModel;

public class Win1252SwedishModel extends FrenchModel {
public static final float TYPICAL_POSITIVE_RATIO = 0.985451f;
	

	public Win1252SwedishModel() { 
		super(swedishhWin1252CharToOrderMap, ConstantsExtended.CHARSET_WINDOWS_1252);
	}

	/**
	255: Control characters that usually does not exist in any text
	254: Carriage/Return
	253: symbol (punctuation) that does not belong to word
	252: 0 - 9
	 */

	private static final short[] swedishhWin1252CharToOrderMap = new short[] {
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255, 31, 44, 47, 26, 33, 36, 38, 27, 40, 30, 42, 43, 29, 34, 35,
		 45, 70, 46, 28, 37, 48, 32, 53, 61, 60, 57,255,255,255,255,255,
		255, 1, 23, 21, 7, 2, 18, 11, 14, 9, 24, 13, 8, 12, 3, 10,
		 22, 52, 5, 6, 4, 19, 16, 55, 41, 25, 54,255,255,255,255,255,
		180,179,178,177, 66,176, 71,175,174,173,172,171,170,169,168,167,
		166,165, 56, 59,164,163,162,161,160,159,158,157,156,155,154,153,
		152,151,150, 69,149,148,147,146,145,144,143,142,141,140,139,138,
		 72, 85, 77, 62,137,136,135,134,133,132,131, 39, 73, 68, 76,130,
		129,128,127,126, 49, 50, 84,125,124, 81,123,122,121,120,119,118,
		117,116,115,114,113,112, 58,111,110,109,108,107, 83,106,105,104,
		 64,103, 80,102, 15, 17, 65, 74, 67, 51, 79, 78,101,100, 99, 98,
		 97, 96, 95, 94, 82, 93, 20, 92, 91, 90, 89, 75, 63, 88, 87, 86,
	};
}
