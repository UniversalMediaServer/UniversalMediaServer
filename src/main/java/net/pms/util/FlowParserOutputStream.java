package net.pms.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class FlowParserOutputStream extends OutputStream {
	private ByteBuffer buffer;
	private OutputStream out;
	protected int neededByteNumber;
	protected int streamableByteNumber;
	protected boolean discard;
	protected int internalMark;
	protected int swapOrderBits;
	protected byte[] swapRemainingByte;

	public FlowParserOutputStream(OutputStream out, int maxbuffersize) {
		this.out = out;
		buffer = ByteBuffer.allocate(maxbuffersize);
		zerobuffer = new byte[15000];
		Arrays.fill(zerobuffer, (byte) 0);
	}

	@Override
	public void write(int b) throws IOException {
	}
	public int count;

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (swapOrderBits == 2) {
			if (swapRemainingByte != null && swapRemainingByte.length == 1) {
				buffer.put(b[off]);
				buffer.put(swapRemainingByte[0]);
				off++;
			}
			int modulo = Math.abs(len - off) % swapOrderBits;
			if (modulo != 0) {
				swapRemainingByte = new byte[1];
				len -= modulo;
				System.arraycopy(b, len, swapRemainingByte, 0, modulo);
			}
			for (int i = off; i < len; i += 2) {
				byte temp = b[i];
				b[i] = b[i + 1];
				b[i + 1] = temp;
			}
		}
		buffer.put(b, off, len);

		int remains = buffer.position() - internalMark;

		while (remains > streamableByteNumber || remains > neededByteNumber) {

			if (streamableByteNumber == 0) {
				// time to analyze
				if (remains > neededByteNumber) {
					count++;
					analyzeBuffer(buffer.array(), internalMark, neededByteNumber);
					if (streamableByteNumber == 0) {
						throw new IOException("Packet size cannot be Null !");
					}
					if (!discard) {
						beforeChunkSend();
					}
				} else {
					// let's wait for more data
					buffer.position(internalMark);
					buffer.compact();
					buffer.position(remains);
					internalMark = 0;
					return;
				}
			}

			if (streamableByteNumber > 0) {
				// let's output some bytes
				if (remains >= streamableByteNumber) {
					// we can send the whole bunch
					if (!discard) {
						out.write(buffer.array(), internalMark, streamableByteNumber);
					}
					internalMark += streamableByteNumber;
					remains = remains - streamableByteNumber;
					streamableByteNumber = 0;
					if (!discard) {
						afterChunkSend();
					}
					if (remains == 0) {
						//buffer.position(internalMark);
						//buffer.compact();
						buffer.position(0);
						internalMark = 0;
					}
				} else {

					if (!discard) {
						out.write(buffer.array(), internalMark, remains);
					}
					streamableByteNumber = streamableByteNumber - remains;
					buffer.position(0);
					internalMark = 0;
					remains = 0;
				}
			}
		}
	}

	protected void writePayload(byte payload[]) throws IOException {
		out.write(payload, 0, payload.length);
	}
	private byte zerobuffer[];

	protected void padWithZeros(int numberOfZeros) throws IOException {
		if (numberOfZeros > 0) {
			out.write(zerobuffer, 0, numberOfZeros);
		}
	}

	protected abstract void analyzeBuffer(byte data[], int off, int len);

	protected abstract void beforeChunkSend() throws IOException;

	protected abstract void afterChunkSend() throws IOException;

	@Override
	public void close() throws IOException {
		int finalPos = buffer.position();
		if (finalPos > 0 && streamableByteNumber > finalPos) {
			out.write(buffer.array(), 0, finalPos);
			padWithZeros(streamableByteNumber - finalPos);
		}
		out.close();
	}
}
