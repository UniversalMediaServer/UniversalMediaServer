/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.audio.MediaAudio;
import net.pms.platform.PlatformUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodecUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodecUtil.class);
	private static final List<String> CODECS = new ArrayList<>();

	static {
		// Make sure the list of codecs is initialized before other threads start retrieving it
		initCodecs();
	}

	/**
	 * This class is not meant to be instantiated.
	 */
	private CodecUtil() {
	}

	/**
	 * Initialize the list of codec formats that are recognized by ffmpeg by
	 * parsing the "ffmpeg_formats.txt" resource.
	 */
	private static void initCodecs() {
		InputStream is = CodecUtil.class.getClassLoader().getResourceAsStream("resources/ffmpeg_formats.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;

		try {
			while ((line = br.readLine()) != null) {
				if (line.contains(" ")) {
					CODECS.add(line.substring(0, line.indexOf(' ')));
				} else {
					CODECS.add(line);
				}
			}

			br.close();
			CODECS.add("iso");
		} catch (IOException e) {
			LOGGER.error("Error while retrieving codec list", e);
		}
	}

	/**
	 * Return the list of codec formats that will be recognized.
	 * @return The list of codecs.
	 */
	public static List<String> getPossibleCodecs() {
		return CODECS;
	}

	public static int getAC3Bitrate(UmsConfiguration configuration, MediaAudio media) {
		int defaultBitrate = configuration.getAudioBitrate();
		if (media != null && defaultBitrate >= 384) {
			if (media.getAudioProperties().getNumberOfChannels() == 2 || configuration.getAudioChannelCount() == 2) {
				defaultBitrate = 448;
			} else if (media.getAudioProperties().getNumberOfChannels() == 1) {
				defaultBitrate = 192;
			}
		}
		return defaultBitrate;
	}

	public static String getDefaultFontPath() {
		return PlatformUtils.INSTANCE.getDefaultFontPath();
	}

	/**
	 * Check the font file or font name if registered in the OS
	 *
	 * @param fontName font represented by font file or by the font name
	 *
	 * @return the registered font name or null when not found
	 */
	public static String isFontRegisteredInOS(String fontName) {
		if (StringUtils.isNotBlank(fontName)) {
			File fontFile = new File(fontName);
			if (fontFile.exists()) { // Test if the font is specified by the file.
				try {
					fontName = Font.createFont(Font.TRUETYPE_FONT, fontFile).getFontName();
				} catch (FontFormatException | IOException e) {
					LOGGER.debug("Exception when implementing the custom font: ", e.getMessage());
				}
			}

			// The font is specified by the name. Check if it is registered in the OS.
			String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			for (String font : fonts) {
				if (font.equals(fontName)) {
					return font;
				}
			}
		}

		LOGGER.debug("Font name not found. Check if it is properly specified or installed in the OS");
		return null;
	}
}
