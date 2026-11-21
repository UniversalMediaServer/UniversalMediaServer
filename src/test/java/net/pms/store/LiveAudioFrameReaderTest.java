package net.pms.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LiveAudioFrameReaderTest {

	private static byte[] mp3Frame(int value) {
		// MPEG-1 Layer III, 128 kbit/s, 44.1 kHz, no padding: 417 bytes.
		byte[] frame = new byte[417];
		Arrays.fill(frame, (byte) value);
		frame[0] = (byte) 0xFF;
		frame[1] = (byte) 0xFB;
		frame[2] = (byte) 0x90;
		frame[3] = 0;
		return frame;
	}

	private static byte[] adtsFrame(int value) {
		byte[] frame = new byte[100];
		Arrays.fill(frame, (byte) value);
		frame[0] = (byte) 0xFF;
		frame[1] = (byte) 0xF1;
		frame[2] = 0x50;
		frame[3] = (byte) 0x80;
		frame[4] = 12;
		frame[5] = (byte) 0x9F;
		frame[6] = (byte) 0xFC;
		return frame;
	}

	private static InputStream fragmented(byte[] bytes) {
		return new FilterInputStream(new ByteArrayInputStream(bytes)) {
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return in.read(b, off, Math.min(13, len));
			}
		};
	}

	@Test
	public void fragmentedMp3AndAdtsReadsReturnCompleteFrames() throws Exception {
		for (boolean mp3 : new boolean[]{true, false}) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			for (int i = 0; i < 100; i++) {
				stream.write(mp3 ? mp3Frame(i) : adtsFrame(i));
			}
			LiveAudioFrameReader reader = new LiveAudioFrameReader(fragmented(stream.toByteArray()));
			for (int i = 0; i < 100; i++) {
				assertArrayEquals(mp3 ? mp3Frame(i) : adtsFrame(i), reader.next());
			}
			assertNull(reader.next());
		}
	}

	@Test
	public void resynchronizesAJoinAndDropsAnIncompleteFinalFrame() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(new byte[]{1, 2, 3, 4, 5});
		stream.write(mp3Frame(6));
		stream.write(mp3Frame(7));
		stream.write(Arrays.copyOf(mp3Frame(8), 200));
		LiveAudioFrameReader reader = new LiveAudioFrameReader(fragmented(stream.toByteArray()));
		assertArrayEquals(mp3Frame(6), reader.next());
		assertArrayEquals(mp3Frame(7), reader.next());
		assertNull(reader.next());
	}

	@Test
	public void unknownFormatsPreserveEveryByteIncludingThePrefix() throws Exception {
		byte[] expected = new byte[50000];
		for (int i = 0; i < expected.length; i++) {
			expected[i] = (byte) (i % 127);
		}
		LiveAudioFrameReader reader = new LiveAudioFrameReader(fragmented(expected));
		ByteArrayOutputStream actual = new ByteArrayOutputStream();
		byte[] chunk;
		while ((chunk = reader.next()) != null) {
			actual.write(chunk);
		}
		assertArrayEquals(expected, actual.toByteArray());
	}

	@Test
	public void aSingleFalseSyncHeaderDoesNotStripUnknownFormatBytes() throws Exception {
		byte[] expected = new byte[10000];
		System.arraycopy(mp3Frame(3), 0, expected, 20, 417);
		LiveAudioFrameReader reader = new LiveAudioFrameReader(new ByteArrayInputStream(expected));
		ByteArrayOutputStream actual = new ByteArrayOutputStream();
		byte[] chunk;
		while ((chunk = reader.next()) != null) {
			actual.write(chunk);
		}
		assertEquals(expected.length, actual.size());
		assertArrayEquals(expected, actual.toByteArray());
	}
}
