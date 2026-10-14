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
package net.pms.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BufferedOutputFileImplTest {

	private static final int PACK_HEADER_LENGTH = 14;
	private static final int PES_HEADER_LENGTH = 14;

	@BeforeAll
	public static void setUpClass() throws ConfigurationException, InterruptedException {
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	/**
	 * After a time seek the PTS of each packet is shifted by the seek time,
	 * so the SCR of each pack header has to be shifted by the same amount.
	 * The SCR values are picked so that the bytes holding SCR bits 27..20 and
	 * 12..5 are above 127.
	 */
	@Test
	public void testTimeSeekShiftsScrWithPts() throws IOException {
		double timeSeek = 48.6;
		long scr = (0x9AL << 20) | (0xC5L << 5) | 0x13;
		long videoPts = scr + 45000;
		long audioPts = scr + 44000;

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(packHeader(scr));
		stream.write(pes(0xE0, videoPts));
		stream.write(packHeader(scr + 3600));
		stream.write(pes(0xBD, audioPts));
		byte[] input = stream.toByteArray();

		byte[] output = writeAndRead(input, timeSeek);

		long scrOffset = (long) (timeSeek * 90000);
		long ptsOffset = (int) (timeSeek * 90000);
		int secondPack = PACK_HEADER_LENGTH + PES_HEADER_LENGTH + 4;
		assertEquals(scr + scrOffset, readScr(output, 0));
		assertEquals(videoPts + ptsOffset, readPts(output, PACK_HEADER_LENGTH));
		assertEquals(scr + 3600 + scrOffset, readScr(output, secondPack));
		assertEquals(audioPts + ptsOffset, readPts(output, secondPack + PACK_HEADER_LENGTH));
	}

	@Test
	public void testNoTimeSeekKeepsTimestamps() throws IOException {
		long scr = (0x9AL << 20) | (0xC5L << 5) | 0x13;
		long videoPts = scr + 45000;

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(packHeader(scr));
		stream.write(pes(0xE0, videoPts));
		byte[] input = stream.toByteArray();

		byte[] output = writeAndRead(input, 0);

		assertEquals(scr, readScr(output, 0));
		assertEquals(videoPts, readPts(output, PACK_HEADER_LENGTH));
	}

	private static byte[] writeAndRead(byte[] input, double timeSeek) throws IOException {
		OutputParams params = new OutputParams(PMS.getConfiguration());
		params.setMinBufferSize(0);
		params.setMaxBufferSize(1);
		params.setTimeSeek(timeSeek);
		BufferedOutputFileImpl buffer = new BufferedOutputFileImpl(params);
		buffer.write(input, 0, input.length);
		buffer.close();
		byte[] output = new byte[input.length];
		assertEquals(input.length, buffer.read(true, 0, output, 0, output.length));
		return output;
	}

	/**
	 * MPEG-2 pack header with the given SCR base (extension 0), no stuffing.
	 */
	private static byte[] packHeader(long scr) {
		return new byte[] {
			0, 0, 1, (byte) 0xBA,
			(byte) (0x44 | ((scr >> 27) & 0x38) | ((scr >> 28) & 0x03)),
			(byte) (scr >> 20),
			(byte) (((scr >> 12) & 0xF8) | 0x04 | ((scr >> 13) & 0x03)),
			(byte) (scr >> 5),
			(byte) (((scr << 3) & 0xF8) | 0x04),
			0x01,
			0x01, (byte) 0x89, (byte) 0xC3, // mux rate
			(byte) 0xF8 // no stuffing
		};
	}

	/**
	 * PES packet with a PTS and a 4-byte payload.
	 */
	private static byte[] pes(int streamId, long pts) {
		int payload = 4;
		int length = PES_HEADER_LENGTH - 6 + payload;
		return new byte[] {
			0, 0, 1, (byte) streamId,
			(byte) (length >> 8), (byte) length,
			(byte) 0x80, (byte) 0x80, 5,
			(byte) (0x21 | ((pts >> 29) & 0x0E)),
			(byte) (pts >> 22),
			(byte) (((pts >> 14) & 0xFE) | 1),
			(byte) (pts >> 7),
			(byte) (((pts << 1) & 0xFE) | 1),
			1, 2, 3, 4
		};
	}

	private static long readScr(byte[] b, int pos) {
		return (((b[pos + 4] >> 3) & 0x07L) << 30) |
			((b[pos + 4] & 0x03L) << 28) |
			((b[pos + 5] & 0xFFL) << 20) |
			(((b[pos + 6] >> 3) & 0x1FL) << 15) |
			((b[pos + 6] & 0x03L) << 13) |
			((b[pos + 7] & 0xFFL) << 5) |
			((b[pos + 8] >> 3) & 0x1FL);
	}

	private static long readPts(byte[] b, int pos) {
		return (((b[pos + 9] >> 1) & 0x07L) << 30) |
			((b[pos + 10] & 0xFFL) << 22) |
			(((b[pos + 11] >> 1) & 0x7FL) << 15) |
			((b[pos + 12] & 0xFFL) << 7) |
			((b[pos + 13] >> 1) & 0x7FL);
	}

}
