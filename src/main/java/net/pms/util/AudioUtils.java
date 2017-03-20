/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.util;

import net.pms.dlna.DLNAMediaAudio;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

/**
 * This is a utility class for audio related methods
 */

public class AudioUtils {

	// No instantiation
	private AudioUtils() {
	}

	/**
	 * Checks if a given {@link Tag} supports a given {@link FieldKey}
	 *
	 * @param tag the {@link Tag} to check for support
	 * @param key the {@link FieldKey} to check for support for
	 *
	 * @return The result
	 */
	public static boolean tagSupportsFieldKey(Tag tag, FieldKey key) {
		try {
			tag.getFirst(key);
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	/**
	 * Due to mencoder/ffmpeg bug we need to manually remap audio channels for LPCM
	 * output. This function generates argument for channels/pan audio filters
	 *
	 * @param audioTrack DLNAMediaAudio resource
	 * @return argument for -af option or null if we can't remap to desired numberOfOutputChannels
	 */
	public static String getLPCMChannelMappingForMencoder(DLNAMediaAudio audioTrack) {
		// for reference
		// Channel Arrangement for Multi Channel Audio Formats
		// http://avisynth.org/mediawiki/GetChannel
		// http://flac.sourceforge.net/format.html#frame_header
		// http://msdn.microsoft.com/en-us/windows/hardware/gg463006.aspx#E6C
		// http://labs.divx.com/node/44
		// http://lists.mplayerhq.hu/pipermail/mplayer-users/2006-October/063511.html
		//
		// Format			Ch.0	Ch.1	Ch.2	Ch.3	Ch.4	Ch.5	ch.6	ch.7
		// 1.0 WAV/FLAC/MP3/WMA		FC
		// 2.0 WAV/FLAC/MP3/WMA		FL	FR
		// 4.0 WAV/FLAC/MP3/WMA		FL	FR	SL	SR
		// 5.0 WAV/FLAC/MP3/WMA		FL	FR	FC	SL	SR
		// 5.1 WAV/FLAC/MP3/WMA		FL	FR	FC	LFE	SL	SR
		// 5.1 PCM (mencoder)		FL	FR	SR	FC	SL	LFE
		// 7.1 PCM (mencoder)		FL	SL	RR	SR	FR	LFE	RL	FC
		// 5.1 AC3			FL	FC	FR	SL	SR	LFE
		// 5.1 DTS/AAC			FC	FL	FR	SL	SR	LFE
		// 5.1 AIFF			FL	SL	FC	FR	SR	LFE
		//
		//  FL : Front Left
		//  FC : Front Center
		//  FR : Front Right
		//  SL : Surround Left
		//  SR : Surround Right
		//  LFE : Low Frequency Effects (Sub)
		String mixer = null;
		int numberOfInputChannels = audioTrack.getAudioProperties().getNumberOfChannels();

		if (numberOfInputChannels == 6) { // 5.1
			// we are using PCM output and have to manually remap channels because of MEncoder's incorrect PCM mappings
			// (as of r34814 / SB28)

			// as of MEncoder r34814 '-af pan' do nothing (LFE is missing from right channel)
			// same thing for AC3 transcoding. Thats why we should always use 5.1 output on PS3MS configuration
			// and leave stereo downmixing to PS3!
			// mixer for 5.1 => 2.0 mixer = "pan=2:1:0:0:1:0:1:0.707:0.707:1:0:1:1";

			mixer = "channels=6:6:0:0:1:1:2:5:3:2:4:4:5:3";
		} else if (numberOfInputChannels == 8) { // 7.1
			// remap and leave 7.1
			// inputs to PCM encoder are FL:0 FR:1 RL:2 RR:3 FC:4 LFE:5 SL:6 SR:7
			mixer = "channels=8:8:0:0:1:4:2:7:3:5:4:1:5:3:6:6:7:2";
		} else if (numberOfInputChannels == 2) { // 2.0
			// do nothing for stereo tracks
		}

		return mixer;
	}
}
