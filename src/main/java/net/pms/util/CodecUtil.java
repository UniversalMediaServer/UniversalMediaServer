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

import com.sun.jna.Platform;
import java.io.*;
import java.util.ArrayList;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		if (audio.getNrAudioChannels() > 0 && audio.getNrAudioChannels() != channelCount) {
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

	public static String getMixerOutput(boolean pcmonly, int nbInputChannels, int nbOutputChannels) {
		// for reference
		// Channel Arrangement for Multi Channel Audio Formats
		// http://avisynth.org/mediawiki/GetChannel
		// http://flac.sourceforge.net/format.html#frame_header
		// http://msdn.microsoft.com/en-us/windows/hardware/gg463006.aspx#E6C
		// http://labs.divx.com/node/44
		// http://lists.mplayerhq.hu/pipermail/mplayer-users/2006-October/063511.html
		//
		// Format				Chan 0	Chan 1	Chan 2	Chan 3	Chan 4	Chan 5
		// 1.0 WAV/FLAC/MP3/WMA	FC
		// 2.0 WAV/FLAC/MP3/WMA	FL		FR
		// 4.0 WAV/FLAC/MP3/WMA	FL		FR		SL		SR
		// 5.0 WAV/FLAC/MP3/WMA	FL		FR		FC		SL		SR
		// 5.1 WAV/FLAC/MP3/WMA	FL		FR		FC		LFE		SL		SR
		// 5.1 PCM (mencoder)	FL		FR		SR		FC		SL		LFE
		// 7.1 PCM (mencoder)	FL		SL		RR		SR		FR		LFE		RL		FC
		// 5.1 AC3				FL		FC		FR		SL		SR		LFE
		// 5.1 DTS/AAC			FC		FL		FR		SL		SR		LFE
		// 5.1 AIFF				FL		SL		FC		FR		SR		LFE
		//
		//  FL : Front Left
		//  FC : Front Center
		//  FR : Front Right
		//  SL : Surround Left
		//  SR : Surround Right
		//  LFE : Low Frequency Effects (Sub)
		String mixer = null;
		if (pcmonly) { 
			if (nbInputChannels == 6) { // 5.1
				// we are using PCM output and have to manually remap channels because of incorrect mencoder's PCM mappings 
				// (as of r34814 / SB28) 
				if (nbOutputChannels <= 2) {
					// remap and downmix to 2 channels
					// as of mencoder r34814 '-af pan' do nothing (LFE is missing from right channel)
					// same thing for AC3 transcoding. Thats why we should always use 5.1 output on PS3MS configuration
					// and leave stereo downmixing to PS3!
					mixer = "pan=2:1:0:0:1:0:1:0.707:0.707:1:0:1:1";
				} else {
					// remap and leave 5.1
					mixer = "channels=6:6:0:0:1:1:2:5:3:2:4:4:5:3";
				}
			} else if (nbInputChannels == 8) { // 7.1
				// remap and leave 7.1
				// inputs to PCM encoder are FL:0 FR:1 RL:2 RR:3 FC:4 LFE:5 SL:6 SR:7
				mixer = "channels=8:8:0:0:1:4:2:7:3:5:4:1:5:3:6:6:7:2";
			} else if (nbInputChannels == 2) { // 2.0
				// do nothing for stereo tracks
			}
		}
		return mixer;
	}
}
