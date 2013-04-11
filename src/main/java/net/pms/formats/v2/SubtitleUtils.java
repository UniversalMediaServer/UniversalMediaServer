/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  I. Sokolov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.formats.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.codehaus.plexus.util.StringUtils;
import static org.mozilla.universalchardet.Constants.*;

public class SubtitleUtils {
	private final static Map<String, String> fileCharsetToMencoderSubcpOptionMap = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			// Cyrillic / Russian
			put(CHARSET_IBM855, "enca:ru:cp1251");
			put(CHARSET_ISO_8859_5, "enca:ru:cp1251");
			put(CHARSET_KOI8_R, "enca:ru:cp1251");
			put(CHARSET_MACCYRILLIC, "enca:ru:cp1251");
			put(CHARSET_WINDOWS_1251, "enca:ru:cp1251");
			put(CHARSET_IBM866, "enca:ru:cp1251");
			// Greek
			put(CHARSET_WINDOWS_1253, "cp1253");
			put(CHARSET_ISO_8859_7, "ISO-8859-7");
			// Western Europe
			put(CHARSET_WINDOWS_1252, "cp1252");
			// Hebrew
			put(CHARSET_WINDOWS_1255, "cp1255");
			put(CHARSET_ISO_8859_8, "ISO-8859-8");
			// Chinese
			put(CHARSET_ISO_2022_CN, "ISO-2022-CN");
			put(CHARSET_BIG5, "enca:zh:big5");
			put(CHARSET_GB18030, "enca:zh:big5");
			put(CHARSET_EUC_TW, "enca:zh:big5");
			put(CHARSET_HZ_GB_2312, "enca:zh:big5");
			// Korean
			put(CHARSET_ISO_2022_KR, "cp949");
			put(CHARSET_EUC_KR, "euc-kr");
			// Japanese
			put(CHARSET_ISO_2022_JP, "ISO-2022-JP");
			put(CHARSET_EUC_JP, "euc-jp");
			put(CHARSET_SHIFT_JIS, "shift-jis");
		}
	};

	/**
	 * Returns value for -subcp option for non UTF-8 external subtitles based on
	 * detected charset.
	 * @param dlnaMediaSubtitle DLNAMediaSubtitle with external subtitles file.
	 * @return value for mencoder's -subcp option or null if can't determine.
	 */
	public static String getSubCpOptionForMencoder(DLNAMediaSubtitle dlnaMediaSubtitle) {
		if (dlnaMediaSubtitle == null) {
			throw new NullPointerException("dlnaMediaSubtitle can't be null.");
		}
		if (isBlank(dlnaMediaSubtitle.getExternalFileCharacterSet())) {
			return null;
		}
		return fileCharsetToMencoderSubcpOptionMap.get(dlnaMediaSubtitle.getExternalFileCharacterSet());
	}
	
	public static String dumpSrtTc(String in0, double timeseek) throws Exception {
		File in = new File(in0);
		File out = new File(PMS.getConfiguration().getDataFile(in.getName() + "_tc_.srt"));
		out.delete();
		String cp = PMS.getConfiguration().getMencoderSubCp();
		BufferedWriter w;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(in), cp))) {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)));
			String line;
			boolean skip = false;
			int n = 1;
			while ((line = reader.readLine()) != null) {
				try {
					Integer.parseInt(line);
					continue;
				} catch (NumberFormatException e1) {
				}
				if (StringUtils.isEmpty(line) ) {
					if (!skip) {
						w.write("\n");
					}
					skip = false;
					continue;
				}
				if (skip) {
					continue;
				}
				if (line .contains("-->")) {
					String startTime = line.substring(0, line.indexOf("-->") - 1).replaceAll(",", ".");
					String endTime = line.substring(line.indexOf("-->") + 4).replaceAll(",", ".");
					Double start = DLNAMediaInfo.parseDurationString(startTime);
					Double stop = DLNAMediaInfo.parseDurationString(endTime);
					if (timeseek > start) {
						skip  = true;
						continue;
					}
					w.write(String.valueOf(n++));
					w.write("\n");
					w.write(DLNAMediaInfo.getDurationString(start - timeseek));
					w.write(" --> ");				
					w.write(DLNAMediaInfo.getDurationString(stop - timeseek));
					w.write("\n");
					continue;
				}
				
				w.write(line);
				w.write("\n");
			}
		}
		w.flush();
		w.close();
		PMS.get().addTempFile(out, 2 * 24 * 3600 * 1000); /* 2 days only */
		return out.getAbsolutePath();
	}
}
