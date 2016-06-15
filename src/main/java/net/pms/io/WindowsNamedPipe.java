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
package net.pms.io;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.io.*;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsNamedPipe extends Thread implements ProcessWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsNamedPipe.class);
	private String path;
	private boolean in;
	private boolean forceReconnect;
	private Pointer handle1;
	private Pointer handle2;
	private OutputStream writable;
	private InputStream readable;
	private Thread forced;
	private boolean b2;
	private FileOutputStream debug;
	private BufferedOutputFile directBuffer;

	/**
	 * @deprecated Use {@link #setLoop(boolean)} instead.
	 *
	 * This field will be made private in a future version.
	 */
	@Deprecated
	public static boolean loop = true;

	/**
	 * Size for the buffer used in defining pipes for Windows in bytes. The buffer is used
	 * to copy from memory to an {@link java.io.OutputStream OutputStream} such as
	 * {@link net.pms.io.BufferedOutputFile BufferedOutputFile}.
	 */
	private static final int BUFSIZE = 500000;

	public interface Kernel32 extends StdCallLibrary {
		Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32",
			Kernel32.class
		);

		Kernel32 SYNC_INSTANCE = (Kernel32) Native.synchronizedLibrary(INSTANCE);

		abstract class SECURITY_ATTRIBUTES extends Structure {
			public int nLength = size();
			public Pointer lpSecurityDescriptor;
			public boolean bInheritHandle;
		}

		public static abstract class LPOVERLAPPED extends Structure { }

		Pointer CreateNamedPipeA(String lpName, int dwOpenMode, int dwPipeMode,
			int nMaxInstances, int nOutBufferSize, int nInBufferSize,
			int nDefaultTimeOut, SECURITY_ATTRIBUTES lpSecurityAttributes
		);

		boolean ConnectNamedPipe(Pointer handle, LPOVERLAPPED overlapped);
		boolean DisconnectNamedPipe(Pointer handle);
		boolean FlushFileBuffers(Pointer handle);
		boolean CloseHandle(Pointer handle);

		boolean ReadFile(Pointer hFile, Pointer lpBuffer,
			int nNumberOfBytesToRead, IntByReference lpNumberOfBytesRead,
			LPOVERLAPPED lpOverlapped
		);

		boolean WriteFile(Pointer hFile, Pointer lpBuffer,
			int nNumberOfBytesToRead, IntByReference lpNumberOfBytesRead,
			LPOVERLAPPED lpOverlapped
		);
	}

	public String getPipeName() {
		return path;
	}

	public OutputStream getWritable() {
		return writable;
	}

	public InputStream getReadable() {
		return readable;
	}

	public BufferedOutputFile getDirectBuffer() {
		return directBuffer;
	}

	@Override
	public InputStream getInputStream(long seek) throws IOException {
		return null;
	}

	@Override
	public ArrayList<String> getResults() {
		return null;
	}

	@Override
	public boolean isDestroyed() {
		return !isAlive();
	}

	@Override
	public void runInNewThread() {
		// Constructor already called start(), do nothing
	}

	@Override
	public void runInSameThread() {
		// Constructor already called start(), do nothing
	}

	@Override
	public boolean isReadyToStop() {
		return false;
	}

	@Override
	public void setReadyToStop(boolean nullable) { }

	@Override
	public void stopProcess() {
		interrupt();
	}

	/**
	 * Whether the code will loop.
	 *
	 * @param value whether the code will loop.
	 */
	// XXX this can be handled in a shutdown hook
	public static void setLoop(boolean value) {
		loop = value;
	}

	public WindowsNamedPipe(String basename, boolean forceReconnect, boolean in, OutputParams params) {
		this.path = "\\\\.\\pipe\\" + basename;
		this.in = in;
		this.forceReconnect = forceReconnect;
		LOGGER.debug("Creating pipe " + this.path);

		try {
			if (Platform.isWindows()) {
				handle1 = Kernel32.INSTANCE.CreateNamedPipeA(
					this.path,
					3,
					0,
					255,
					BUFSIZE,
					BUFSIZE,
					0,
					null
				);

				if (forceReconnect) {
					handle2 = Kernel32.INSTANCE.CreateNamedPipeA(
						this.path,
						3,
						0,
						255,
						BUFSIZE,
						BUFSIZE,
						0,
						null
					);
				}

				if (params != null) {
					directBuffer = new BufferedOutputFileImpl(params);
				} else {
					writable = new PipedOutputStream();
					readable = new PipedInputStream((PipedOutputStream) writable, BUFSIZE);
				}

				start();

				if (forceReconnect) {
					forced = new Thread(
						new Runnable() {
							@Override
							public void run() {
								b2 = Kernel32.INSTANCE.ConnectNamedPipe(handle2, null);
							}
						},
						"Forced Reconnector"
					);

					forced.start();
				}
			}
		} catch (Exception e1) {
			LOGGER.debug("Caught exception", e1);
		}
	}

	@Override
	public void run() {
		LOGGER.debug("Waiting for pipe connection " + this.path);
		boolean b1 = Kernel32.INSTANCE.ConnectNamedPipe(handle1, null);

		if (forceReconnect) {
			while (forced.isAlive()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) { }
			}

			LOGGER.debug("Forced reconnection of " + path + " with result : " + b2);
			handle1 = handle2;
		}

		LOGGER.debug("Result of " + this.path + " : " + b1);

		try {
			if (b1) {
				if (in) {
					IntByReference intRef = new IntByReference();
					Memory buffer = new Memory(BUFSIZE);

					while (loop) {
						boolean fSuccess = Kernel32.INSTANCE.ReadFile(
							handle1,
							buffer,
							BUFSIZE,
							intRef,
							null
						);

						int cbBytesRead = intRef.getValue();

						if (cbBytesRead == -1) {
							if (directBuffer != null) {
								directBuffer.close();
							}

							if (writable != null) {
								writable.close();
							}

							if (debug != null) {
								debug.close();
							}

							break;
						}

						if (directBuffer != null) {
							directBuffer.write(buffer.getByteArray(0, cbBytesRead));
						}

						if (writable != null) {
							writable.write(buffer.getByteArray(0, cbBytesRead));
						}

						if (debug != null) {
							debug.write(buffer.getByteArray(0, cbBytesRead));
						}

						if (!fSuccess || cbBytesRead == 0) {
							if (directBuffer != null) {
								directBuffer.close();
							}

							if (writable != null) {
								writable.close();
							}

							if (debug != null) {
								debug.close();
							}

							break;
						}
					}
				} else {
					byte[] b = new byte[BUFSIZE];
					IntByReference intRef = new IntByReference();
					Memory buffer = new Memory(BUFSIZE);

					while (loop) {
						int cbBytesRead = readable.read(b);

						if (cbBytesRead == -1) {
							readable.close();

							if (debug != null) {
								debug.close();
							}

							break;
						}

						buffer.write(0, b, 0, cbBytesRead);

						boolean fSuccess = Kernel32.INSTANCE.WriteFile(
							handle1,
							buffer,
							cbBytesRead,
							intRef,
							null
						);

						int cbWritten = intRef.getValue();

						if (debug != null) {
							debug.write(buffer.getByteArray(0, cbBytesRead));
						}

						if (!fSuccess || cbWritten == 0) {
							readable.close();

							if (debug != null) {
								debug.close();
							}

							break;
						}
					}
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Error: " + e.getMessage());
		}

		if (!in) {
			LOGGER.debug("Disconnected pipe: " + path);
			Kernel32.INSTANCE.FlushFileBuffers(handle1);
			Kernel32.INSTANCE.DisconnectNamedPipe(handle1);
		} else {
			Kernel32.INSTANCE.CloseHandle(handle1);
		}
	}
}
