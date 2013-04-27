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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mozilla.universalchardet.Constants.*;

public class SubtitleUtils {
	private static final String TEMP_DIR = "temp";
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

	/**
	 * Applies timeseeking to subtitles file in SSA/ASS format
	 * @param SrtFile Subtitles file in SSA/ASS format
	 * @param timeseek  Time stamp value
	 * @return Converted subtitles file
	 * @throws IOException
	 */
	public static File applyTimeSeeking(File SrtFile, double timeseek) throws IOException {
		Double startTime;
		Double endTime;
		String line;
		File outputSubs = new File(tempFile(SrtFile.getName() + System.currentTimeMillis()));
		BufferedWriter output;
		try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(SrtFile)))) {
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs)));
			while ((line = input.readLine()) != null) {
				if (line.startsWith("Dialogue:")) {
					String[] tempStr = line.split(",");
					startTime = StringUtil.convertStringToTime(tempStr[1]);
					endTime = StringUtil.convertStringToTime(tempStr[2]);;

					if (startTime >= timeseek) {
						tempStr[1] = StringUtil.convertTimeToString(startTime - timeseek, StringUtil.ASS_FORMAT);
						tempStr[2] = StringUtil.convertTimeToString(endTime - timeseek, StringUtil.ASS_FORMAT);
					} else {
						continue;
					}

					output.write(StringUtils.join(tempStr, ",") + "\n");
				} else {
					output.write(line + "\n");
				}
			}
		}
		output.flush();
		output.close();
		PMS.get().addTempFile(outputSubs, 2 * 24 * 3600 * 1000);
		return outputSubs;
	}

	public static String tempFile(String name) {
		String dir = PMS.getConfiguration().getDataFile(TEMP_DIR); 
		File path = new File(dir);
		if (!path.exists()) {
			path.mkdirs();
		}
		return path.getAbsolutePath() + File.separator + name + ".tmp";
	}
}
