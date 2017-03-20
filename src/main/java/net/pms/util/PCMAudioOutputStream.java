package net.pms.util;

import java.io.IOException;
import java.io.OutputStream;

public class PCMAudioOutputStream extends FlowParserOutputStream {
	protected int nbchannels;
	protected int sampleFrequency;
	protected int bitsperSample;
	protected int blocksize;
	protected byte payload[];
	protected boolean wavMode; // WAVEform (RIFF) output mode not used at the moment
	protected boolean headerSent;

	public PCMAudioOutputStream(OutputStream source, int nbchannels, int sampleFrequency, int bitsperSample) {
		super(source, 600000);
		this.nbchannels = nbchannels;
		this.sampleFrequency = sampleFrequency;
		this.bitsperSample = bitsperSample;
		swapOrderBits = 2; // swap endian
		init();
	}

	protected void init() {
		if (!wavMode) {
			blocksize = (2 * ((nbchannels + 1) / 2)) * sampleFrequency * bitsperSample / 1600;
			payload = new byte[4];
			switch (nbchannels) {
				case 1:
					payload[2] = 17;
					break;
				case 2:
					payload[2] = 49;
					break;
				case 3:
					payload[2] = 65;
					break;
				case 4:
					payload[2] = 113;
					break;
				case 5:
					payload[2] = -127;
					break;
				case 6:
					payload[2] = -111;
					break;
				case 7:
					payload[2] = -95;
					break;
				case 8:
					payload[2] = -79;
					break;
				default:
					break;
			}
			payload[0] = (byte) ((blocksize >> 8) & 0xff);
			payload[1] = (byte) ((blocksize + 256) % 256);
			if (sampleFrequency == 96000) {
				payload[2] = (byte) (payload[2] + 3);
			}
			if (sampleFrequency == 192000) {
				payload[2] = (byte) (payload[2] + 4);
			}
			payload[3] = (byte) (16 * (bitsperSample - 12));
		} else {
			// http://fr.wikipedia.org/wiki/WAVEform_audio_format#En-t.C3.AAte_de_fichier_WAV
			int BytePerBloc = nbchannels * bitsperSample / 8;
			int BytePerSec = sampleFrequency * BytePerBloc;
			payload = new byte [44];
			payload[0] = 82; // "RIFF"
			payload[1] = 73;
			payload[2] = 70;
			payload[3] = 70;
			payload[8] = 87; // "WAVEfmt "
			payload[9] = 65;
			payload[10] = 86;
			payload[11] = 69;
			payload[12] = 102;
			payload[13] = 109;
			payload[14] = 116;
			payload[15] = 32;
			payload[16] = 16; // BlocSize
			payload[20] = 1; // AudioFormat: 1 = PCM
			payload[22] = (byte) nbchannels; // Nb channels
			payload[25] = (byte)(sampleFrequency & 0xff); // frequency
			payload[24] = (byte)((sampleFrequency >> 8) & 0xff);
			payload[30] = (byte)(BytePerSec & 0xff); // BytePerSec
			payload[29] = (byte)((BytePerSec >> 8) & 0xff);
			payload[32] = (byte)(BytePerBloc & 0xff); //BytePerBloc
			payload[34] = (byte)(bitsperSample & 0xff); // bits per sample
			payload[36] = 100; // "data"
			payload[37] = 97;
			payload[38] = 116;
			payload[39] = 97;
			payload[40] = -1;
			payload[41] = -1;
			payload[42] = -1;
			payload[43] = -1;
		}
	}

	@Override
	protected void afterChunkSend() throws IOException {
	}

	@Override
	protected void analyzeBuffer(byte[] data, int off, int len) {
		streamableByteNumber = blocksize;
	}

	@Override
	protected void beforeChunkSend() throws IOException {
		if (!wavMode) {
			writePayload(payload);
		} else if (!headerSent) {
			writePayload(payload);
			headerSent = true;
		}
	}
}
