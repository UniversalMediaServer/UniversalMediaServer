/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package net.pms.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaAudio;

import com.sun.jna.Platform;

public class CodecUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodecUtil.class);
	private static final ArrayList<String> codecs = new ArrayList<String>();

	static {
		// Make sure the list of codecs is initialized before other threads start retrieving it
		initCodecs();
	}

	/**
	 * Initialize the list of codec formats that are recognized by ffmpeg by
	 * parsing the "ffmpeg_formats.txt" resource. 
	 */
	private static void initCodecs() {
		InputStream is = CodecUtil.class.getClassLoader().getResourceAsStream("resources/ffmpeg_formats.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;

		try {
			while ((line = br.readLine()) != null) {
				if (line.contains(" ")) {
					codecs.add(line.substring(0, line.indexOf(" ")));
				} else {
					codecs.add(line);
				}
			}

			br.close();
			codecs.add("iso");
		} catch (IOException e) {
			LOGGER.error("Error while retrieving codec list", e);
		}
	}


	/**
	 * Return the list of codec formats that will be recognized.
	 * @return The list of codecs.
	 */
	public static ArrayList<String> getPossibleCodecs() {
		return codecs;
	}

	public static int getAC3Bitrate(PmsConfiguration configuration, DLNAMediaAudio media) {
		int defaultBitrate = configuration.getAudioBitrate();
		if (media != null && defaultBitrate >= 384) {
			if (media.getNrAudioChannels() == 2 || configuration.getAudioChannelCount() == 2) {
				defaultBitrate = 448;
			} else if (media.getNrAudioChannels() == 1) {
				defaultBitrate = 192;
			}
		}
		return defaultBitrate;
	}

	public static int getAC3ChannelCount(PmsConfiguration configuration, DLNAMediaAudio audio) {
		int channelCount = configuration.getAudioChannelCount();
		if (audio.isAC3() && configuration.isRemuxAC3() && audio.getNrAudioChannels() > 0 && audio.getNrAudioChannels() != channelCount) {
			channelCount = audio.getNrAudioChannels();
		}
		return channelCount;
	}

	public static int getRealChannelCount(PmsConfiguration configuration, DLNAMediaAudio audio) {
		int channelCount = configuration.getAudioChannelCount();
		if (audio.getNrAudioChannels() > 0 && audio.getNrAudioChannels() < channelCount) {
			channelCount = audio.getNrAudioChannels();
		}
		return channelCount;
	}

	public static String getDefaultFontPath() {
		String font = null;
		if (Platform.isWindows()) {
			// get Windows Arial
			String winDir = PMS.get().getRegistry().getWindowsDirectory();
			if (winDir != null) {
				File winDirFile = new File(winDir);
				if (winDirFile.exists()) {
					File fontsDir = new File(winDirFile, "Fonts");
					if (fontsDir.exists()) {
						File arialDir = new File(fontsDir, "Arial.ttf");
						if (arialDir.exists()) {
							font = arialDir.getAbsolutePath();
						} else {
							arialDir = new File(fontsDir, "arial.ttf");
							if (arialDir.exists()) {
								font = arialDir.getAbsolutePath();
							}
						}
					}
				}
			}
			if (font == null) {
				font = getFont("C:\\Windows\\Fonts", "Arial.ttf");
			}
			if (font == null) {
				font = getFont("C:\\WINNT\\Fonts", "Arial.ttf");
			}
			if (font == null) {
				font = getFont("D:\\Windows\\Fonts", "Arial.ttf");
			}
			if (font == null) {
				font = getFont(".\\win32\\mplayer\\", "subfont.ttf");
			}
			return font;
		} else if (Platform.isLinux()) {
			// get Linux default font
			font = getFont("/usr/share/fonts/truetype/msttcorefonts/", "Arial.ttf");
			if (font == null) {
				font = getFont("/usr/share/fonts/truetype/ttf-bitstream-veras/", "Vera.ttf");
			}
			if (font == null) {
				font = getFont("/usr/share/fonts/truetype/ttf-dejavu/", "DejaVuSans.ttf");
			}
			return font;
		} else if (Platform.isMac()) {
			// get default osx font
			font = getFont("/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/fonts/", "LucidaSansRegular.ttf");
			return font;
		}
		return null;
	}

	private static String getFont(String path, String name) {
		File f = new File(path, name);
		if (f.exists()) {
			return f.getAbsolutePath();
		}
		return null;
	}

	public static String getMixerOutput(boolean pcmonly, int nbchannels) {
		String mixer = "volume=0";
		if (pcmonly) { // we are using real PCM output
			// thanks to JR Cash and his 5.1 sample
			// et merci Yann :p
			if (nbchannels == 5) /* Were missing an LFE channel so create one from the fronts */ {
				mixer = "channels=6:6:0:0:1:1:3:2:5:3:4:4:2:5,sub=80:5";
// New MEncoder builds don't need to remap audio channels anymore (for 5.1 audio) 
//			} else {
//				mixer = "channels=6:6:0:0:1:1:3:2:5:3:4:4:2:5";
			}
			if (nbchannels <= 2) { // downmixing to 2 channels
				mixer = "pan=2:1:0:0:1:1:0:0:1:0.707:0.707:1:1";
			}
		}
		return mixer;
	}
}
