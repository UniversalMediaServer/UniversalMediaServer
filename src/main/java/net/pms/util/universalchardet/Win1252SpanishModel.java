// based on https://github.com/PyYoshi/cChardet.git

package net.pms.util.universalchardet;

import net.pms.util.universalchardet.SpanishModel;

public class Win1252SpanishModel extends SpanishModel {
public static final float TYPICAL_POSITIVE_RATIO = 0.983906f;
	

	public Win1252SpanishModel() { 
		super(spanishhWin1252CharToOrderMap, ConstantsExtended.CHARSET_WINDOWS_1252, "Win1252SpanishModel");
	}

	/**
	255: Control characters that usually does not exist in any text
	254: Carriage/Return
	253: symbol (punctuation) that does not belong to word
	252: 0 - 9
	 */

	private static final short[] spanishhWin1252CharToOrderMap = new short[] {
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
		255, 32, 44, 29, 38, 26, 48, 49, 53, 37, 55, 63, 31, 35, 40, 46,
		 30, 58, 36, 33, 41, 47, 54, 52, 64, 51, 67,255,255,255,255,255,
		255, 2, 15, 11, 9, 1, 19, 16, 21, 6, 27, 43, 8, 13, 4, 3,
		 14, 22, 7, 5, 10, 12, 17, 42, 39, 18, 24,255,255,255,255,255,
		180,179,178,177,176, 71,175,174,173,172,171,170,169,168,167,166,
		165, 76, 70, 61, 62,164, 57,163,162,161,160,159,158,157,156,155,
		154, 56,153,152,151,150,149,148,147, 79, 73, 59,146,145,144,143,
		 83, 82,142,141,140,139,138,137,136,135, 78, 60,134,133,132, 50,
		131, 72,130,129,128,127,126,125,124, 66,123,122,121, 68,120,119,
		118, 74,117, 69,116,115,114,113,112,111, 81,110,109,108,107,106,
		105, 25,104,103,102,101,100, 77, 75, 28, 99, 98, 97, 23, 96, 95,
		 94, 34, 80, 20, 93, 92, 91, 90, 89, 88, 45, 87, 65, 86, 85, 84,
	};
}
