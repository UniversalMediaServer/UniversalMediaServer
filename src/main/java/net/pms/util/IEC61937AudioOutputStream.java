/**
 * Class used to embed HD Audio data (AC3, DTS, DTSHD?, TRUEHD?) into a LPCM stream, according to IEC-61937, used by S/PDIF
 *
 * As of today (2011/07/15), only AC3 and DTS are working on my receiver, Denon AVR-1910
 * DTS-HD cannot work because it needs a LPCM stream of 2 channels sampled at 192kHz = 6.1Mbits/s. (8 channels for TrueHD, 24Mbit/s)
 * But PS3 seems to limit the HDMI output's samplerate at 48kHz for Non-HDCP content,
 * so a downsample of an LPCM encoded audio track destroy the embedded data, alas.
 *
 * I disabled it for the time being (see variable skip_dtshd)
 *
 * Inspired by spdifenc.c from the FFmpeg team
 *
 * @author Arnaud Brochard
 */
package net.pms.util;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IEC61937AudioOutputStream extends FlowParserOutputStream {
	private static final Logger LOGGER = LoggerFactory.getLogger(IEC61937AudioOutputStream.class);
	private static int bits[] = new int[]{16, 16, 20, 20, 0, 24, 24};
	private static int samplerates[] = new int[]{
		0,
		8000,
		16000,
		32000,
		0,
		0,
		11025,
		22050,
		44100,
		0,
		0,
		12000,
		24000,
		48000,
		96000,
		192000
	};
	private boolean ac3 = false;
	private boolean dts = false;
	private boolean dtsHD = false;
	private int framesize;
	private int blocks;
	private int sample_rate;
	private PCMAudioOutputStream out;
	private int padding;
	private byte preamble[];
	private boolean usepreamble;
	private byte dtshdpreamble[];
	private int period;

	public IEC61937AudioOutputStream(PCMAudioOutputStream out) {
		super(out, 600000);
		this.out = out;
		out.swapOrderBits = 0;
		neededByteNumber = 5000;
		usepreamble = true;
	}

	@Override
	protected void afterChunkSend() throws IOException {
		padWithZeros(padding);
	}

	@Override
	protected void analyzeBuffer(byte[] data, int off, int len) {
		if (data[off + 0] == 100 && data[off + 1] == 88 && data[off + 2] == 32 && data[off + 3] == 37) {
			LOGGER.trace("DTS-HD stray frame, skipping this one...");
			//dtsHD = true;
			streamableByteNumber = ((data[off + 6] & 0x0f) << 11) + ((data[off + 7] & 0xff) << 3) + ((data[off + 8] & 0xf0) >> 5) + 1;
			discard = true;
		} else if (data[off + 0] == 127 && data[off + 1] == -2 && data[off + 2] == -128 && data[off + 3] == 1) {
			discard = false;
			dts = true;
			streamableByteNumber = framesize;
			if (framesize == 0 || dtsHD) {
				blocks = ((data[off + 4] & 0x01) << 6) + ((data[off + 5] & 0xfc) >> 2);
				sample_rate = samplerates[((data[off + 8] >> 2) & 0x0f)];
				framesize = ((data[off + 5] & 0x03) << 12) + ((data[off + 6] & 0xff) << 4) + ((data[off + 7] & 0xf0) >> 4) + 1;
				int framesize_sup = 0;
				int dts_rate = 48000;
				boolean skip_dtshd = true;
				if (!skip_dtshd && off + framesize + 3 < data.length && data[off + framesize] == 100 && data[off + framesize + 1] == 88 && data[off + framesize + 2] == 32 && data[off + framesize + 3] == 37) {
					dtsHD = true;
					dts_rate = 192000;
					framesize_sup = ((data[off + framesize + 6] & 0x0f) << 11) + ((data[off + framesize + 7] & 0xff) << 3) + ((data[off + framesize + 8] & 0xf0) >> 5) + 1;
					framesize += framesize_sup;
				}
				blocks++;

				int pcm_wrapped_frame_size = blocks << 7;
				if (usepreamble && preamble == null) {
					int bitspersample = ((data[off + 11] & 0x01) << 2) + ((data[off + 12] & 0xfc) >> 6);
					if (bitspersample < 7) {
						LOGGER.trace("DTS bits per sample: " + bits[bitspersample]);
					}
					preamble = new byte[8];
					preamble[1] = 114; // syncword1
					preamble[0] = -8;
					preamble[3] = 31; // syncword2
					preamble[2] = 78;
					if (dtsHD) {
						preamble[4] = 0;
						preamble[5] = 17; // DTS type IV = DTS-HD
					} else {
						preamble[4] = 0;
						switch (blocks) {
							case 512 >> 5:
								preamble[5] = 11;
								break;
							case 1024 >> 5:
								preamble[5] = 12;
								break;
							case 2048 >> 5:
								preamble[5] = 13;
								break;
							default:
								break;
						}
					}
					if (dtsHD) {
						period = dts_rate * (blocks << 5) / sample_rate;
						byte subtype = 0x0;
						switch (period) {
							case 512:
								subtype = 0x0;
								break;
							case 1024:
								subtype = 0x1;
								break;
							case 2048:
								subtype = 0x2;
								break;
							case 4096:
								subtype = 0x3;
								break;
							case 8192:
								subtype = 0x4;
								break;
							case 16384:
								subtype = 0x5;
								break;
							default:
								break;
						}
						preamble[4] = subtype;
						dtshdpreamble = new byte[12];
						dtshdpreamble[0] = 1;
						dtshdpreamble[8] = -2;
						dtshdpreamble[9] = -2;
					}
				}
				if (out.sampleFrequency != dts_rate || out.nbchannels != 2 || out.bitsperSample != 16) {
					out.nbchannels = 2;
					out.sampleFrequency = dts_rate;
					out.bitsperSample = 16;
					//out.wavMode = true;
					out.init();
				}
				if (dtsHD) {
					pcm_wrapped_frame_size = period * 4;
				}
				if (framesize > pcm_wrapped_frame_size) {
					framesize -= framesize_sup;
				}
				streamableByteNumber = framesize;
				if (dtshdpreamble != null) {
					dtshdpreamble[11] = (byte) (framesize & 0xff);
					dtshdpreamble[10] = (byte) ((framesize >> 8) & 0xff);
					framesize += dtshdpreamble.length;
				}
				if (preamble != null) {
					int framesize_bits = framesize * 8;
					preamble[7] = (byte) (framesize_bits & 0xff);
					preamble[6] = (byte) ((framesize_bits >> 8) & 0xff);
				}

				padding = pcm_wrapped_frame_size - framesize - (preamble != null ? preamble.length : 0);
				//LOGGER.debug("DTS spdif framesize: " + framesize + " / padding: " + padding);
			}
		} else if (data[off + 0] == 11 && data[off + 1] == 119) {
			ac3 = true;
			discard = false;
			streamableByteNumber = framesize;
			if (framesize == 0) {
				// find the next ones
				discard = true;
				int a0 = data[off + 0];
				int a1 = data[off + 1];
				LOGGER.debug("Looking for AC3 framesize");
				for (int i = 4; i < len - 4; i++) {
					if (data[off + i] == a0 && data[off + i + 1] == a1) {
						framesize = i;
						streamableByteNumber = framesize;
						int pcm_wrapped_frame_size = 6144; // padding_bytes = number_of_samples_in_the_audio_frame * 4 - frame_size
						if (out != null) {
							PCMAudioOutputStream pout = out;
							pout.nbchannels = 2;
							pout.sampleFrequency = 48000;
							pout.bitsperSample = 16;
							pout.init();
						}
						if (usepreamble) {
							preamble = new byte[8];
							padding = pcm_wrapped_frame_size - framesize - preamble.length;
							preamble[1] = 114; // syncword1
							preamble[0] = -8;
							preamble[3] = 31; // syncword2
							preamble[2] = 78;
							preamble[5] = 1; // ac3
							preamble[4] = 0;
							int framesize_bits = framesize * 8;
							preamble[7] = (byte) (framesize_bits % 256);
							preamble[6] = (byte) (framesize_bits / 256);
						} else {
							padding = pcm_wrapped_frame_size - framesize;
						}
						LOGGER.debug("AC3 spdif framesize: " + framesize + " / padding: " + padding + " preamble[6]: " + preamble[6]);
						discard = false;
						break;
					}
				}
			}
		} else {
			// DTS wrongly extracted ?... searching for start of the frame
			for (int i = 3; i < 2020; i++) {
				if (data.length > i && data[i - 3] == 127 && data[i - 2] == -2 && data[i - 1] == -128 && data[i] == 1) {
					// skip DTS first frame as it's incomplete
					discard = true;
					streamableByteNumber = i - 3;
					break;
				} else if (data.length > i && data[i - 3] == 100 && data[i - 2] == 88 && data[i - 1] == 32 && data[i] == 37) {
					// skip DTS-HD first frame
					// stray HD frame ?
					discard = true;
					streamableByteNumber = i - 3;
					break;
				}
			}
		}
	}

	@Override
	protected void beforeChunkSend() throws IOException {
		if (preamble != null) {
			writePayload(preamble);
		}
		if (dtshdpreamble != null) {
			writePayload(dtshdpreamble);
		}
	}

	public boolean isAc3() {
		return ac3;
	}

	public boolean isDts() {
		return dts;
	}

	public boolean isDtsHD() {
		return dtsHD;
	}
}
