/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.encoders;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import net.pms.PMS;
import net.pms.io.Gob;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperLiteImpl;
import net.pms.util.H264AnnexBInputStream;
import net.pms.util.PCMAudioOutputStream;
import net.pms.util.ProcessUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AviDemuxerInputStream extends InputStream {
	private static final Logger LOGGER = LoggerFactory.getLogger(AviDemuxerInputStream.class);

	@Override
	public void close() throws IOException {
		if (process != null) {
			ProcessUtil.destroy(process);
		}
		super.close();
	}
	private Process process;
	private InputStream stream;
	private ArrayList<ProcessWrapper> attachedProcesses;
	private long readCount = -1;
	private String streamVideoTag;
	private Track track[] = new Track[2];
	private int numberOfAudioChannels;
	private OutputStream aOut;
	private OutputStream vOut;
	private long audiosize;
	private long videosize;
	private InputStream realIS;
	private Thread parsing;
	private OutputParams params;

	public AviDemuxerInputStream(InputStream fin, final OutputParams params, ArrayList<ProcessWrapper> at) throws IOException {
		stream = fin;
		LOGGER.trace("Opening AVI Stream");
		this.attachedProcesses = at;
		this.params = params;

		aOut = params.output_pipes[1].getOutputStream();
		if (params.no_videoencode && params.forceType != null && params.forceType.equals("V_MPEG4/ISO/AVC") && params.header != null) {
			// NOT USED RIGHT NOW
			PipedOutputStream pout = new PipedOutputStream();
			final InputStream pin = new H264AnnexBInputStream(new PipedInputStream(pout), params.header);
			final OutputStream out = params.output_pipes[0].getOutputStream();
			Runnable r = new Runnable() {
				public void run() {
					try {
						byte b[] = new byte[512 * 1024];
						int n = -1;
						while ((n = pin.read(b)) > -1) {
							out.write(b, 0, n);
						}
					} catch (Exception e) {
						LOGGER.error(null, e);
					}

				}
			};
			vOut = pout;
			new Thread(r, "Avi Demuxer").start();
		} else {
			vOut = params.output_pipes[0].getOutputStream();
		}

		Runnable r = new Runnable() {
			public void run() {
				try {
					// TODO(tcox): Is this used anymore?
					TSMuxerVideo ts = new TSMuxerVideo(PMS.getConfiguration());
					File f = new File(PMS.getConfiguration().getTempFolder(), "pms-tsmuxer.meta");
					PrintWriter pw = new PrintWriter(f);
					pw.println("MUXOPT --no-pcr-on-video-pid --no-asyncio --new-audio-pes --vbr --vbv-len=500");
					String videoType = "V_MPEG-2";
					if (params.no_videoencode && params.forceType != null) {
						videoType = params.forceType;
					}
					String fps = "";
					if (params.forceFps != null) {
						fps = "fps=" + params.forceFps + ", ";
					}
					String audioType = "A_LPCM";
					if (params.lossyaudio) {
						audioType = "A_AC3";
					}
					pw.println(videoType + ", \"" + params.output_pipes[0].getOutputPipe() + "\", " + fps + "level=4.1, insertSEI, contSPS, track=1");
					pw.println(audioType + ", \"" + params.output_pipes[1].getOutputPipe() + "\", track=2");
					pw.close();

					PipeProcess tsPipe = new PipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");
					ProcessWrapper pipe_process = tsPipe.getPipeProcess();
					attachedProcesses.add(pipe_process);
					pipe_process.runInNewThread();
					tsPipe.deleteLater();

					String cmd[] = new String[]{ts.executable(), f.getAbsolutePath(), tsPipe.getInputPipe()};
					ProcessBuilder pb = new ProcessBuilder(cmd);
					process = pb.start();
					ProcessWrapper pwi = new ProcessWrapperLiteImpl(process);
					attachedProcesses.add(pwi);

					// "Gob": a cryptic name for (e.g.) StreamGobbler - i.e. a stream
					// consumer that reads and discards the stream
					new Gob(process.getErrorStream()).start();
					new Gob(process.getInputStream()).start();

					realIS = tsPipe.getInputStream();
					ProcessUtil.waitFor(process);
					LOGGER.trace("tsMuxeR muxing finished");
				} catch (IOException e) {
					LOGGER.error(null, e);
				}
			}
		};

		Runnable r2 = new Runnable() {
			public void run() {
				try {
					//Thread.sleep(500);
					parseHeader();
				} catch (IOException e) {
					LOGGER.debug("Parsing error", e);
				}
			}
		};

		LOGGER.trace("Launching tsMuxeR muxing");
		new Thread(r, "Avi Demuxer tsMuxeR").start();
		parsing = new Thread(r2, "Avi Demuxer Header Parser");
		LOGGER.trace("Ready to mux");
	}

	private void parseHeader() throws IOException {
		LOGGER.trace("Parsing AVI Stream");
		String id = getString(stream, 4);
		getBytes(stream, 4);
		String type = getString(stream, 4);
		if (!"RIFF".equalsIgnoreCase(id) || !"AVI ".equalsIgnoreCase(type))
		{
			throw new IOException("Not AVI file");
		}
		byte[] hdrl = null;

		while (true) {
			String command = getString(stream, 4);
			int length = (readBytes(stream, 4) + 1) & ~1;

			if ("LIST".equalsIgnoreCase(command)) {
				command = getString(stream, 4);
				length -= 4;
				if ("movi".equalsIgnoreCase(command)) {
					break;
				}
				if ("hdrl".equalsIgnoreCase(command)) {
					hdrl = getBytes(stream, length);
				}
				if ("idx1".equalsIgnoreCase(command)) {
					/*idx = */
					getBytes(stream, length);
				}
				if ("iddx".equalsIgnoreCase(command)) {
					/*idx = */
					getBytes(stream, length);
				}

			} else {

				getBytes(stream, length);
			}
		}

		int streamNumber = 0;
		int lastTagID = 0;
		for (int i = 0; i < hdrl.length;) {
			String command = new String(hdrl, i, 4);
			int size = str2ulong(hdrl, i + 4);

			if ("LIST".equalsIgnoreCase(command)) {
				i += 12;
				continue;
			}

			String command2 = new String(hdrl, i + 8, 4);
			if ("strh".equalsIgnoreCase(command)) {
				lastTagID = 0;
				if ("vids".equalsIgnoreCase(command2)) {
					String compressor = new String(hdrl, i + 12, 4);
					int scale = str2ulong(hdrl, i + 28);
					int rate = str2ulong(hdrl, i + 32);
					track[0] = new Track(compressor, scale, rate, -1);
					streamVideoTag = new String(new char[]{
							(char) ((streamNumber / 10) + '0'),
							(char) ((streamNumber % 10) + '0'), 'd', 'b'});
					streamNumber++;
					lastTagID = 1;
				}
				if ("auds".equalsIgnoreCase(command2)) {
					int scale = str2ulong(hdrl, i + 28);
					int rate = str2ulong(hdrl, i + 32);
					int sampleSize = str2ulong(hdrl, i + 52);
					track[1 + numberOfAudioChannels++] = new Track(null, scale, rate, sampleSize);

					streamNumber++;
					lastTagID = 2;


				}
			}

			if ("strf".equalsIgnoreCase(command)) {
				if (lastTagID == 1) {

					byte[] information = new byte[size]; // formerly size-4
					System.arraycopy(hdrl, i + 8, information, 0, information.length);  // formerly i+4
					track[0].setBih(information);
				}
				if (lastTagID == 2) {

					byte[] information = new byte[size]; // formerly size-4
					System.arraycopy(hdrl, i + 8, information, 0, information.length);// formerly i+4
					Track aud = track[1 + numberOfAudioChannels - 1];
					aud.setBih(information);
					int bitspersample = str2ushort(information, 14);
					aud.setBitspersample(bitspersample);
					int nbaudio = str2ushort(information, 2);
					aud.setNbaudio(nbaudio);
					long filelength = 100;

					if (params.losslessaudio) {
						aOut = new PCMAudioOutputStream(aOut, nbaudio, 48000, bitspersample);
					}

					if (!params.lossyaudio && params.losslessaudio) {
						writePCMHeader(aOut, filelength, nbaudio, aud.getRate(), aud.getSampleSize(), bitspersample);
					}
				}
			}

			if (size % 2 != 0) {
				size++;
			}
			i += size + 8;
		}

		LOGGER.trace("Found " + streamNumber + " stream(s)");

		boolean init = false;
		while (true) {

			String command = null;
			try {
				command = getString(stream, 4);
			} catch (Exception e) {
				LOGGER.trace("Error attendue: " + e.getMessage());
				break;
			}
			if (command == null) {
				break;
			}
			command = command.toUpperCase();
			int size = readBytes(stream, 4);

			boolean framed = false;

			while ("LIST".equals(command)
				|| "RIFF".equals(command)
				|| "JUNK".equals(command)) {
				if (size < 0) {
					size = 4;
				}
				getBytes(stream, "RIFF".equals(command) ? 4 : size);
				command = getString(stream, 4).toUpperCase();
				size = readBytes(stream, 4);
				if (("LIST".equals(command) || "RIFF".equals(command) || "JUNK".equals(command)) && (size % 2 != 0))
				{
					readByte(stream);
				}
			}


			String videoTag = streamVideoTag.substring(0, 3);
			if (command.substring(0, 3).equalsIgnoreCase(videoTag)
				&& (command.charAt(3) == 'B' || command.charAt(3) == 'C')) {
				byte buffer[] = getBytes(stream, size);
				if (!command.equalsIgnoreCase("IDX1")) {
					vOut.write(buffer);
					videosize += size;
				}
				framed = true;
			}

			if (!framed) {
				for (int i = 0; i < numberOfAudioChannels; i++) {

					byte buffer[] = getBytes(stream, size);
					if (!command.equalsIgnoreCase("IDX1")) {
						aOut.write(buffer, init ? 4 : 0, init ? (size - 4) : size);
						init = false;
						audiosize += size;
					}
					framed = true;


				}
			}

			if (!framed) {
				throw new IOException("Not header: " + command);
			}

			if (size % 2 != 0) {
				readByte(stream);
			}

		}
		LOGGER.trace("output pipes closed");
		aOut.close();
		vOut.close();
	}

	private String getString(InputStream input, int sz) throws IOException {
		byte bb[] = getBytes(input, sz);
		return new String(bb);
	}

	private byte[] getBytes(InputStream input, int sz) throws IOException {
		byte bb[] = new byte[sz];
		int n = input.read(bb);
		while (n < sz) {
			int u = input.read(bb, n, sz - n);
			if (u == -1) {
				break;
			}
			n += u;
		}
		return bb;
	}

	private final int readBytes(InputStream input, int number) throws IOException {
		byte buffer[] = new byte[number];
		int read = input.read(buffer);

		if (read < number) {
			if (read < 0) {
				throw new IOException("End of Stream");
			}
			for (int i = read; i < number; i++) {
				buffer[i] = (byte) readByte(input);
			}
		}

		/**
		 * Create integer
		 */
		switch (number) {
			case 1:
				return (buffer[0] & 0xff);
			case 2:
				return (buffer[0] & 0xff) | ((buffer[1] & 0xff) << 8);
			case 3:
				return (buffer[0] & 0xff) | ((buffer[1] & 0xff) << 8)
					| ((buffer[2] & 0xff) << 16);
			case 4:
				return (buffer[0] & 0xff) | ((buffer[1] & 0xff) << 8)
					| ((buffer[2] & 0xff) << 16) | ((buffer[3] & 0xff) << 24);
			default:
				throw new IOException("Illegal Read quantity");
		}
	}

	private final int readByte(InputStream input) throws IOException {
		return input.read();
	}

	public static final int str2ulong(byte[] data, int i) {
		return (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8)
			| ((data[i + 2] & 0xff) << 16) | ((data[i + 3] & 0xff) << 24);
	}

	public static final int str2ushort(byte[] data, int i) {
		return (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8);
	}

	public static final byte[] getLe32(long value) {
		byte buffer[] = new byte[4];
		buffer[0] = (byte) (value & 0xff);
		buffer[1] = (byte) ((value >> 8) & 0xff);
		buffer[2] = (byte) ((value >> 16) & 0xff);
		buffer[3] = (byte) ((value >> 24) & 0xff);
		return buffer;
	}

	public static final byte[] getLe16(int value) {
		byte buffer[] = new byte[2];
		buffer[0] = (byte) (value & 0xff);
		buffer[1] = (byte) ((value >> 8) & 0xff);
		return buffer;
	}

	@Override
	public int read() throws IOException {
		if (readCount == -1) {
			parsing.start();
			readCount = 0;
		}
		int c = 0;
		while ((realIS == null || videosize == 0 || audiosize == 0) && c < 15) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.trace("Sleep interrupted", e);
			}
			c++;
		}
		if (realIS != null) {
			readCount++;
			return realIS.read();
		} else {
			return -1;
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		if (readCount == -1) {
			parsing.start();
			readCount = 0;
		}
		int c = 0;
		while ((realIS == null || videosize == 0 || audiosize == 0) && c < 15) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.trace("Sleep interrupted", e);
			}
			c++;
		}

		if (realIS != null) {
			int n = realIS.read(b);
			readCount += n;
			return n;
		} else {
			return -1;
		}
	}

	public static void writePCMHeader(OutputStream aOut, long filelength, int nbaudio, int rate, int samplesize, int bitspersample) throws IOException {
	}
}
