/*
 * Universal Media Server, for streaming any media to DLNA
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility class for audio related methods.
 */

public final class AudioUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioUtils.class);

	// No instantiation
	private AudioUtils() {
	}

	/**
	 * Checks if a given {@link Tag} supports a given {@link FieldKey}.
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
		} // do nothing for stereo tracks

		return mixer;
	}

	/**
	 * Parses the old RealAudio 1.0 and 2.0 formats that's not supported by
	 * neither {@link org.jaudiotagger} nor MediaInfo. Returns {@code false} if
	 * {@code channel} isn't one of these formats or the parsing fails.
	 * <p>
	 * Primary references:
	 * <ul>
	 * <li><a href="https://wiki.multimedia.cx/index.php/RealMedia">RealAudio on
	 * MultimediaWiki</a></li>
	 * <li><a
	 * href="https://github.com/FFmpeg/FFmpeg/blob/master/libavformat/rmdec.c"
	 * >FFmpeg rmdec.c</a></li>
	 * </ul>
	 *
	 * @param channel the {@link Channel} containing the input. Size will only
	 *            be parsed if {@code channel} is a {@link FileChannel}
	 *            instance.
	 * @param media the {@link DLNAMediaInfo} instance to write the parsing
	 *            results to.
	 * @return {@code true} if the {@code channel} input is in RealAudio 1.0 or
	 *         2.0 format and the parsing succeeds; false otherwise
	 */
	public static boolean parseRealAudio(ReadableByteChannel channel, DLNAMediaInfo media) {
		final byte[] magicBytes = {0x2E, 0x72, 0x61, (byte) 0xFD};
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.BIG_ENDIAN);
		DLNAMediaAudio audio = new DLNAMediaAudio();
		try {
			int count = channel.read(buffer);
			if (count < 4) {
				LOGGER.trace("Input is too short to be RealAudio");
				return false;
			}
			buffer.flip();
			byte[] signature = new byte[4];
			buffer.get(signature);
			if (!Arrays.equals(magicBytes, signature)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Input signature ({}) mismatches RealAudio version 1.0 or 2.0",
						new String(signature, StandardCharsets.US_ASCII)
					);
				}
				return false;
			}
			media.setContainer(FormatConfiguration.RA);
			short version = buffer.getShort();
			int reportedHeaderSize = 0;
			int reportedDataSize = 0;
			if (version == 3) {
				audio.setCodecA(FormatConfiguration.REALAUDIO_14_4);
				audio.getAudioProperties().setNumberOfChannels(1);
				audio.getAudioProperties().setSampleFrequency(8000);
				short headerSize = buffer.getShort();

				buffer = ByteBuffer.allocate(headerSize);
				channel.read(buffer);
				buffer.flip();
				buffer.position(8);
				int bytesPerMinute = buffer.getShort() & 0xFFFF;
				reportedDataSize = buffer.getInt();
				byte b = buffer.get();
				if (b != 0) {
					byte[] title = new byte[b & 0xFF];
					buffer.get(title);
					String titleString = new String(title, StandardCharsets.US_ASCII);
					audio.setSongname(titleString);
					audio.setAudioTrackTitleFromMetadata(titleString);
				}
				b = buffer.get();
				if (b != 0) {
					byte[] artist = new byte[b & 0xFF];
					buffer.get(artist);
					audio.setArtist(new String(artist, StandardCharsets.US_ASCII));
				}
				audio.setBitRate(bytesPerMinute * 8 / 60);
				media.setBitrate(bytesPerMinute * 8 / 60);
			} else if (version == 4 || version == 5) {
				buffer = ByteBuffer.allocate(14);
				channel.read(buffer);
				buffer.flip();
				buffer.get(signature);
				if (!".ra4".equals(new String(signature, StandardCharsets.US_ASCII))) {
					LOGGER.debug("Invalid RealAudio 2.0 signature \"{}\"", new String(signature, StandardCharsets.US_ASCII));
					return false;
				}
				reportedDataSize = buffer.getInt();
				buffer.getShort(); //skip version repeated
				reportedHeaderSize = buffer.getInt();

				buffer = ByteBuffer.allocate(reportedHeaderSize);
				channel.read(buffer);
				buffer.flip();
				buffer.getShort(); // skip codec flavor
				buffer.getInt(); // skip coded frame size
				buffer.getInt(); // skip unknown
				long bytesPerMinute = buffer.getInt() & 0xFFFFFFFFL;
				buffer.getInt(); // skip unknown
				buffer.getShort(); // skip sub packet
				buffer.getShort(); // skip frame size
				buffer.getShort(); // skip sub packet size
				buffer.getShort(); // skip unknown
				if (version == 5) {
					buffer.position(buffer.position() + 6); // skip unknown
				}
				short sampleRate = buffer.getShort();
				buffer.getShort(); // skip unknown
				short sampleSize = buffer.getShort();
				short nrChannels = buffer.getShort();
				byte[] fourCC;
				if (version == 4) {
					buffer.position(buffer.get() + buffer.position()); // skip interleaver id
					fourCC = new byte[buffer.get()];
					buffer.get(fourCC);
				} else {
					buffer.getFloat(); // skip deinterlace id
					fourCC = new byte[4];
					buffer.get(fourCC);
				}
				String fourCCString = new String(fourCC, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
				switch (fourCCString) {
					case "lpcJ":
						audio.setCodecA(FormatConfiguration.REALAUDIO_14_4);
						break;
					case "28_8":
						audio.setCodecA(FormatConfiguration.REALAUDIO_28_8);
						break;
					case "dnet":
						audio.setCodecA(FormatConfiguration.AC3);
						break;
					case "sipr":
						audio.setCodecA(FormatConfiguration.SIPRO);
						break;
					case "cook":
						audio.setCodecA(FormatConfiguration.COOK);
					case "atrc":
						audio.setCodecA(FormatConfiguration.ATRAC);
					case "ralf":
						audio.setCodecA(FormatConfiguration.RALF);
					case "raac":
						audio.setCodecA(FormatConfiguration.AAC_LC);
					case "racp":
						audio.setCodecA(FormatConfiguration.HE_AAC);
					default:
						LOGGER.debug("Unknown RealMedia codec FourCC \"{}\" - parsing failed", fourCCString);
						return false;
				}

				if (buffer.hasRemaining()) {
					parseRealAudioMetaData(buffer, audio, version);
				}

				audio.setBitRate((int) (bytesPerMinute * 8 / 60));
				media.setBitrate((int) (bytesPerMinute * 8 / 60));
				audio.setBitsperSample(sampleSize);
				audio.getAudioProperties().setNumberOfChannels(nrChannels);
				audio.getAudioProperties().setSampleFrequency(sampleRate);
			} else {
				LOGGER.error("Could not parse RealAudio format - unknown format version {}", version);
				return false;
			}

			media.getAudioTracksList().add(audio);
			long fileSize = 0;
			if (channel instanceof FileChannel) {
				fileSize = ((FileChannel) channel).size();
				media.setSize(fileSize);
			}
			// Duration is estimated based on bitrate and might not be accurate
			if (audio.getBitRate() > 0) {
				int dataSize;
				if (fileSize > 0 && reportedHeaderSize > 0) {
					int fullHeaderSize = reportedHeaderSize + (version == 3 ? 8 : 16);
					if (reportedDataSize > 0) {
						dataSize = (int) Math.min(reportedDataSize, fileSize - fullHeaderSize);
					} else {
						dataSize = (int) (fileSize - fullHeaderSize);
					}
				} else {
					dataSize = reportedDataSize;
				}
				media.setDuration((double) dataSize / audio.getBitRate() * 8);
			}

		} catch (IOException e) {
			LOGGER.debug("Error while trying to parse RealAudio version 1 or 2: {}", e.getMessage());
			LOGGER.trace("", e);
			return false;
		}
		if (
			PMS.getConfiguration() != null &&
			!PMS.getConfiguration().getAudioThumbnailMethod().equals(CoverSupplier.NONE) &&
			(
				StringUtils.isNotBlank(media.getFirstAudioTrack().getSongname()) ||
				StringUtils.isNotBlank(media.getFirstAudioTrack().getArtist())
			)
		) {
			ID3v1Tag tag = new ID3v1Tag();
			if (StringUtils.isNotBlank(media.getFirstAudioTrack().getSongname())) {
				tag.setTitle(media.getFirstAudioTrack().getSongname());
			}
			if (StringUtils.isNotBlank(media.getFirstAudioTrack().getArtist())) {
				tag.setArtist(media.getFirstAudioTrack().getArtist());
			}
			try {
				media.setThumb(DLNAThumbnail.toThumbnail(
					CoverUtil.get().getThumbnail(tag),
					640,
					480,
					ScaleType.MAX,
					ImageFormat.SOURCE,
					false
				));
			} catch (IOException e) {
				LOGGER.error(
					"An error occurred while generating thumbnail for RealAudio source: [\"{}\", \"{}\"]",
					tag.getFirstTitle(),
					tag.getFirstArtist()
				);
			}
		}
		media.setThumbready(true);
		media.setMediaparsed(true);

		return true;
	}

	private static void parseRealAudioMetaData(ByteBuffer buffer, DLNAMediaAudio audio, short version) {
		buffer.position(buffer.position() + (version == 4 ? 3 : 4)); // skip unknown
		byte b = buffer.get();
		if (b != 0) {
			byte[] title = new byte[Math.min(b & 0xFF, buffer.remaining())];
			buffer.get(title);
			String titleString = new String(title, StandardCharsets.US_ASCII);
			audio.setSongname(titleString);
			audio.setAudioTrackTitleFromMetadata(titleString);
		}
		if (buffer.hasRemaining()) {
			b = buffer.get();
			if (b != 0) {
				byte[] artist = new byte[Math.min(b & 0xFF, buffer.remaining())];
				buffer.get(artist);
				audio.setArtist(new String(artist, StandardCharsets.US_ASCII));
			}
		}
	}
}
