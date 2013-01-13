package net.pms.util;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DTSAudioOutputStream extends FlowParserOutputStream {
	private static final Logger LOGGER = LoggerFactory.getLogger(DTSAudioOutputStream.class);
	private static int bits[] = new int[]{16, 16, 20, 20, 0, 24, 24};
	private boolean dts = false;
	private boolean dtsHD = false;
	private int framesize;
	private OutputStream out;
	private int padding;

	public DTSAudioOutputStream(OutputStream out) {
		super(out, 600000);
		if (out instanceof PCMAudioOutputStream) {
			PCMAudioOutputStream pout = (PCMAudioOutputStream) out;
			pout.swapOrderBits = 0;
		}
		this.out = out;
		neededByteNumber = 15;
	}

	@Override
	protected void afterChunkSend() throws IOException {
		padWithZeros(padding);
	}

	@Override
	protected void analyzeBuffer(byte[] data, int off, int len) {
		if (data[off + 0] == 100 && data[off + 1] == 88 && data[off + 2] == 32 && data[off + 3] == 37) {
			dtsHD = true;
			streamableByteNumber = ((data[off + 6] & 0x0f) << 11) + ((data[off + 7] & 0xff) << 3) + ((data[off + 8] & 0xf0) >> 5) + 1;
			discard = true;
		} else if (data[off + 0] == 127 && data[off + 1] == -2 && data[off + 2] == -128 && data[off + 3] == 1) {
			discard = false;
			dts = true;
			streamableByteNumber = framesize;
			if (framesize == 0) {
				framesize = ((data[off + 5] & 0x03) << 12) + ((data[off + 6] & 0xff) << 4) + ((data[off + 7] & 0xf0) >> 4) + 1;
				int bitspersample = ((data[off + 11] & 0x01) << 2) + ((data[off + 12] & 0xfc) >> 6);
				streamableByteNumber = framesize;
				//reset of default values
				int pcm_wrapped_frame_size = 2048;
				if (out instanceof PCMAudioOutputStream) {
					PCMAudioOutputStream pout = (PCMAudioOutputStream) out;
					pout.nbchannels = 2;
					pout.sampleFrequency = 48000;
					pout.bitsperSample = 16;
					pout.init();
				}
				padding = pcm_wrapped_frame_size - framesize;
				if (bitspersample < 7) {
					LOGGER.trace("DTS bits per sample: " + bits[bitspersample]);
				}
				LOGGER.trace("DTS framesize: " + framesize);
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
					discard = true;
					streamableByteNumber = i - 3;
					break;
				}
			}
		}
	}

	@Override
	protected void beforeChunkSend() throws IOException {
	}

	public boolean isDts() {
		return dts;
	}

	public boolean isDtsHD() {
		return dtsHD;
	}
}
