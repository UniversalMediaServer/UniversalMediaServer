package net.pms.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Keeps MP3 and ADTS frames intact when radio history or a slow listener's queue is trimmed.
 * Unknown formats are passed through unchanged, including their initial bytes.
 */
final class LiveAudioFrameReader {

	private static final int BUFFER_SIZE = 32 * 1024;
	private static final int SNIFF_BYTES = 8 * 1024;
	private static final int[] MPEG1_BITRATES = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
	private static final int[] MPEG2_BITRATES = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160};
	private static final int[] SAMPLE_RATES = {44100, 48000, 32000};

	private enum Format { UNDECIDED, RAW, MP3, ADTS }

	private final InputStream input;
	private final byte[] buffer = new byte[BUFFER_SIZE];
	private int start;
	private int end;
	private boolean eof;
	private Format format = Format.UNDECIDED;

	LiveAudioFrameReader(InputStream input) {
		this.input = input;
	}

	byte[] next() throws IOException {
		if (format == Format.UNDECIDED) {
			fill(SNIFF_BYTES);
			identify();
		}
		if (format == Format.RAW) {
			if (start == end && !fill(1)) {
				return null;
			}
			return take(end - start);
		}
		while (fill(7)) {
			int length = frameLength(start, format);
			if (length > 0) {
				if (!fill(length)) {
					// A disconnected upstream may have left a partial frame. Never forward that tail.
					return null;
				}
				return take(length);
			}
			// Joining a station can start in the middle of a frame; find the next valid header.
			start++;
		}
		return null;
	}

	private void identify() {
		for (int offset = start; offset + 7 <= end; offset++) {
			for (Format candidate : new Format[]{Format.MP3, Format.ADTS}) {
				int length = frameLength(offset, candidate);
				int following = offset + length;
				// Two consecutive headers avoid mistaking arbitrary file bytes for a radio codec.
				if (length > 0 && following + 7 <= end && frameLength(following, candidate) > 0) {
					format = candidate;
					start = offset;
					return;
				}
			}
		}
		format = Format.RAW;
	}

	private int frameLength(int at, Format candidate) {
		if (at + 7 > end || (buffer[at] & 0xFF) != 0xFF) {
			return -1;
		}
		int second = buffer[at + 1] & 0xFF;
		int third = buffer[at + 2] & 0xFF;
		if (candidate == Format.ADTS) {
			// Syncword and layer=0; both MPEG-2 and MPEG-4 ADTS are allowed.
			if ((second & 0xF6) != 0xF0 || ((third >>> 2) & 15) >= 13) {
				return -1;
			}
			int length = ((buffer[at + 3] & 3) << 11) | ((buffer[at + 4] & 0xFF) << 3) | ((buffer[at + 5] & 0xE0) >>> 5);
			int header = (second & 1) == 0 ? 9 : 7;
			return length >= header ? length : -1;
		}
		// MPEG audio Layer III, excluding reserved version/rate and free-format frames.
		int version = (second >>> 3) & 3;
		int bitrateIndex = third >>> 4;
		int sampleIndex = (third >>> 2) & 3;
		if ((second & 0xE0) != 0xE0 || version == 1 || ((second >>> 1) & 3) != 1 ||
			bitrateIndex == 0 || bitrateIndex == 15 || sampleIndex == 3) {
			return -1;
		}
		int sampleRate = SAMPLE_RATES[sampleIndex];
		if (version == 2) {
			sampleRate /= 2;
		} else if (version == 0) {
			sampleRate /= 4;
		}
		int bitrate = (version == 3 ? MPEG1_BITRATES : MPEG2_BITRATES)[bitrateIndex];
		return (version == 3 ? 144000 : 72000) * bitrate / sampleRate + ((third >>> 1) & 1);
	}

	private boolean fill(int minimum) throws IOException {
		while (end - start < minimum && !eof) {
			if (start > 0) {
				System.arraycopy(buffer, start, buffer, 0, end - start);
				end -= start;
				start = 0;
			}
			int read = input.read(buffer, end, buffer.length - end);
			if (read < 0) {
				eof = true;
			} else if (read > 0) {
				end += read;
			}
		}
		return end - start >= minimum;
	}

	private byte[] take(int length) {
		byte[] chunk = Arrays.copyOfRange(buffer, start, start + length);
		start += length;
		return chunk;
	}
}
