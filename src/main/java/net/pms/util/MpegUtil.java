package net.pms.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MpegUtil {
	public static int getDurationFromMpeg(File f) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(f, "r");
		if (raf.length() >= 500000) {
			Map<Integer, Integer> ptsStart = checkRange(raf, 0, 250000, false);
			if (ptsStart != null) {
				Map<Integer, Integer> ptsEnd = checkRange(raf, 0, 250000, true);
				if (ptsEnd != null) {
					Iterator<Integer> iterator = ptsStart.keySet().iterator();
					while (iterator.hasNext()) {
						Integer id = iterator.next();
						if (ptsEnd.get(id) != null) {
							int dur = ptsEnd.get(id).intValue()
								- ptsStart.get(id).intValue();
							dur = dur / 90000;
							return dur;
						}
					}
				}
			}
		}
		raf.close();
		return 0;
	}

	private static Map<Integer, Integer> checkRange(RandomAccessFile raf, long startingPos,
		int range, boolean end) throws IOException {
		Map<Integer, Integer> pts = new HashMap<Integer, Integer>();
		byte buffer[] = new byte[range];
		if (end) // statringPos not applicable for end==true
		{
			raf.seek(raf.length() - range);
		} else {
			raf.seek(0 + startingPos);
		}
		raf.read(buffer, 0, buffer.length);
		int ps = 0;
		int start = 0;
		for (int i = 0; i < 400; i++) {
			if (buffer[i] == 71 && buffer[i + 188] == 71) { // T
				// ok
				ps = 188;
				start = i;
				break;
			} else if (buffer[i] == 71 && buffer[i + 192] == 71) { // MT
				// ok
				ps = 192;
				start = i;
				break;
			}
		}
		if (ps == 0) {
			return null;
		}
		for (int i = start; i < buffer.length - ps; i += ps) {
			Integer id = (((buffer[i + 1] + 256) % 256) - 64) * 256 + ((buffer[i + 2] + 256) % 256); // calc id
			if (buffer[i + 7] == -32 && buffer[i + 6] == 1) {
				int diff = i + 7 + 4; // 47 50 11 11 00 00 01 E0 00 00 84 C0
				// check pts
				if ((buffer[diff] & 128) == 128 && (buffer[diff + 2] & 32) == 32
						&& (pts.get(id) == null || (pts.get(id) != null && end))) {
					pts.put(id, Integer.valueOf(getTS(buffer, diff + 3)));
				}
			}
		}
		return pts;
	}

	private static int getTS(byte buffer[], int diff) {
		return (((((buffer[diff + 0] & 0xff) << 8) + (buffer[diff + 1] & 0xff)) >> 1) << 15)
			+ ((((buffer[diff + 2] & 0xff) << 8) + (buffer[diff + 3] & 0xff)) >> 1);
	}

	/**
	 * @deprecated Use {@link #getPositionForTimeInMpeg(File, int)} instead.
	 * gets position for specified time in MPEG stream (M2TS, TS)
	 * @param f - file to check
	 * @param timeS - time (in seconds) to find
	 * @return position in stream (in bytes).
	 * @throws IOException
	 */
	@Deprecated
	public static long getPossitionForTimeInMpeg(File f, int timeS) throws IOException {
	    return getPositionForTimeInMpeg(f, timeS);
	}

	/**
	 * gets position for specified time in MPEG stream (M2TS, TS)
	 * @param f - file to check
	 * @param timeS - time (in seconds) to find
	 * @return position in stream (in bytes).
	 * @throws IOException
	 */
	public static long getPositionForTimeInMpeg(File f, int timeS) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(f, "r");
		Map<Integer, Integer> ptsStart = checkRange(raf, 0, 250000, false);
		long currentPos = 0;

		if (ptsStart != null && !ptsStart.isEmpty()) {
			long minRangePos = 0;
			long maxRangePos = raf.length();
			boolean nextPosition = true;
			while (maxRangePos - minRangePos > 250000 && nextPosition) {
				nextPosition = false;
				currentPos = minRangePos + (maxRangePos - minRangePos) / 2;
				Map<Integer, Integer> ptsEnd = checkRange(raf, currentPos, 250000, false);
				if (ptsEnd != null) {
					Iterator<Integer> iterator = ptsStart.keySet().iterator();
					while (iterator.hasNext()) {
						Integer id = iterator.next();
						if (ptsEnd.get(id) != null) {
							int time = (ptsEnd.get(id).intValue() - ptsStart.get(id).intValue()) / 90000;

							if (time == timeS) // found it
							{
								return currentPos;
							}

							nextPosition = true;
							if (time > timeS) {
								maxRangePos = currentPos;
							} else {
								minRangePos = currentPos;
							}
							break;
						}
					}
				} else {
					return currentPos;
				}
			}
		}
		return currentPos;
	}
}
