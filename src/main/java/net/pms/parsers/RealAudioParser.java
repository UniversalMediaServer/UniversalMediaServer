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
package net.pms.parsers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.InputFile;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.util.CoverSupplier;
import net.pms.util.CoverUtil;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealAudioParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(JaudiotaggerParser.class);
	private static final String PARSER_NAME = "RealAudio";

	/**
	 * This class is not meant to be instantiated.
	 */
	private RealAudioParser() {
	}

	public static boolean parse(MediaInfo media, InputFile file, int type) {
		FileChannel channel;
		try {
			channel = FileChannel.open(file.getFile().toPath(), StandardOpenOption.READ);
			if (parseRealAudio(channel, media)) {
				// If successful parsing is done, if not continue parsing the standard way
				media.postParse(type);
				return true;
			}
		} catch (IOException e) {
			LOGGER.warn("An error occurred when trying to open \"{}\" for reading: {}", file, e.getMessage());
			LOGGER.trace("", e);
		}
		return false;
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
	 * @param media the {@link MediaInfo} instance to write the parsing
	 *            results to.
	 * @return {@code true} if the {@code channel} input is in RealAudio 1.0 or
	 *         2.0 format and the parsing succeeds; false otherwise
	 */
	private static boolean parseRealAudio(ReadableByteChannel channel, MediaInfo media) {
		final byte[] magicBytes = {0x2E, 0x72, 0x61, (byte) 0xFD};
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.BIG_ENDIAN);
		MediaAudio audio = new MediaAudio();
		MediaAudioMetadata audioMetadata = new MediaAudioMetadata();
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
			int reportedDataSize;
			switch (version) {
				case 3 -> {
						audio.setCodec(FormatConfiguration.REALAUDIO_14_4);
						audio.setNumberOfChannels(1);
						audio.setSampleRate(8000);
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
							audioMetadata.setSongname(titleString);
							audio.setTitle(titleString);
						}
						b = buffer.get();
						if (b != 0) {
							byte[] artist = new byte[b & 0xFF];
							buffer.get(artist);
							audioMetadata.setArtist(new String(artist, StandardCharsets.US_ASCII));
						}
						audio.setBitRate(bytesPerMinute * 8 / 60);
						media.setBitRate(bytesPerMinute * 8 / 60);
					}
				case 4, 5 -> {
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
					short bitDepth = buffer.getShort();
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
						case "lpcJ" -> audio.setCodec(FormatConfiguration.REALAUDIO_14_4);
						case "28_8" -> audio.setCodec(FormatConfiguration.REALAUDIO_28_8);
						case "dnet" -> audio.setCodec(FormatConfiguration.AC3);
						case "sipr" -> audio.setCodec(FormatConfiguration.SIPRO);
						case "cook" -> audio.setCodec(FormatConfiguration.COOK);
						case "atrc" -> audio.setCodec(FormatConfiguration.ATRAC);
						case "ralf" -> audio.setCodec(FormatConfiguration.RALF);
						case "raac" -> audio.setCodec(FormatConfiguration.AAC_LC);
						case "racp" -> audio.setCodec(FormatConfiguration.HE_AAC);
						default -> {
							LOGGER.debug("Unknown RealMedia codec FourCC \"{}\" - parsing failed", fourCCString);
							return false;
						}
					}

					if (buffer.hasRemaining()) {
						parseRealAudioMetaData(buffer, audioMetadata, version);
					}

					audio.setBitRate((int) (bytesPerMinute * 8 / 60));
					media.setBitRate((int) (bytesPerMinute * 8 / 60));
					audio.setBitDepth(bitDepth);
					audio.setNumberOfChannels(nrChannels);
					audio.setSampleRate(sampleRate);
				}
				default -> {
					LOGGER.error("Could not parse RealAudio format - unknown format version {}", version);
					return false;
				}
			}

			media.getAudioTracks().add(audio);
			long fileSize = 0;
			if (channel instanceof FileChannel fileChannel) {
				fileSize = fileChannel.size();
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
				StringUtils.isNotBlank(audioMetadata.getSongname()) ||
				StringUtils.isNotBlank(audioMetadata.getArtist())
			)
		) {
			ID3v1Tag tag = new ID3v1Tag();
			if (StringUtils.isNotBlank(audioMetadata.getSongname())) {
				tag.setTitle(audioMetadata.getSongname());
			}
			if (StringUtils.isNotBlank(audioMetadata.getArtist())) {
				tag.setArtist(audioMetadata.getArtist());
			}
			try {
				media.setThumb(DLNAThumbnail.toThumbnail(
					CoverUtil.get().getThumbnail(tag),
					640,
					480,
					ImagesUtil.ScaleType.MAX,
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
		media.setMediaParser(PARSER_NAME);

		return true;
	}

	private static void parseRealAudioMetaData(ByteBuffer buffer, MediaAudioMetadata audioMetadata, short version) {
		buffer.position(buffer.position() + (version == 4 ? 3 : 4)); // skip unknown
		byte b = buffer.get();
		if (b != 0) {
			byte[] title = new byte[Math.min(b & 0xFF, buffer.remaining())];
			buffer.get(title);
			String titleString = new String(title, StandardCharsets.US_ASCII);
			audioMetadata.setSongname(titleString);
		}
		if (buffer.hasRemaining()) {
			b = buffer.get();
			if (b != 0) {
				byte[] artist = new byte[Math.min(b & 0xFF, buffer.remaining())];
				buffer.get(artist);
				audioMetadata.setArtist(new String(artist, StandardCharsets.US_ASCII));
			}
		}
	}

}
