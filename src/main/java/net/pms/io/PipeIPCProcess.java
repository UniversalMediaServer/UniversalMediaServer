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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import com.sun.jna.Platform;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import net.pms.util.DTSAudioOutputStream;
import net.pms.util.H264AnnexBInputStream;
import net.pms.util.IEC61937AudioOutputStream;
import net.pms.util.PCMAudioOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipeIPCProcess extends Thread implements ProcessWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(PipeIPCProcess.class);
	private final PipeProcess mkin;
	private final PipeProcess mkout;
	private StreamModifier modifier;

	public StreamModifier getModifier() {
		return modifier;
	}

	public void setModifier(StreamModifier modifier) {
		this.modifier = modifier;
	}

	public PipeIPCProcess(String inPipeName, String outPipeName, boolean inForceReconnect, boolean outForcereconnect) {
		mkin = new PipeProcess(inPipeName, inForceReconnect ? "reconnect" : "dummy");
		mkout = new PipeProcess(outPipeName, "out", outForcereconnect ? "reconnect" : "dummy");
	}

	@Override
	public void run() {
		byte[] b = new byte[512 * 1024];
		int n = -1;
		InputStream in = null;
		OutputStream out = null;
		OutputStream debug = null;

		try {
			in = mkin.getInputStream();
			out = mkout.getOutputStream();

			if (modifier != null && modifier.isH264AnnexB()) {
				in = new H264AnnexBInputStream(in, modifier.getHeader());
			} else if (modifier != null && modifier.isEncodedAudioPassthrough()) {
				out = new IEC61937AudioOutputStream(new PCMAudioOutputStream(out, modifier.getNbChannels(), modifier.getSampleFrequency(), modifier.getBitsPerSample()));
			} else if (modifier != null && modifier.isDtsEmbed()) {
				out = new DTSAudioOutputStream(new PCMAudioOutputStream(out, modifier.getNbChannels(), modifier.getSampleFrequency(), modifier.getBitsPerSample()));
			} else if (modifier != null && modifier.isPcm()) {
				out = new PCMAudioOutputStream(out, modifier.getNbChannels(), modifier.getSampleFrequency(), modifier.getBitsPerSample());
			}

			if (modifier != null && modifier.getHeader() != null && !modifier.isH264AnnexB()) {
				out.write(modifier.getHeader());
			}

			while ((n = in.read(b)) > -1) {
				out.write(b, 0, n);

				if (debug != null) {
					debug.write(b, 0, n);
				}
			}
		} catch (InterruptedIOException e) {
			if (LOGGER.isDebugEnabled()) {
				if (isNotBlank(e.getMessage())) {
					LOGGER.debug("IPC pipe interrupted after writing {} bytes, shutting down: {}", e.bytesTransferred, e.getMessage());
				} else {
					LOGGER.debug("IPC pipe interrupted after writing {} bytes, shutting down...", e.bytesTransferred);
				}
				LOGGER.trace("", e);
			}
		} catch (IOException e) {
			LOGGER.warn("An error occurred duing IPC piping: {}", e.getMessage());
			LOGGER.trace("", e);
		} finally {
			try {
				// in and out may not have been initialized:
				// http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=9885&view=unread#p45142
				if (in != null) {
					in.close();
				}

				if (out != null) {
					out.close();
				}

				if (debug != null) {
					debug.close();
				}
			} catch (IOException e) {
				LOGGER.debug("Error closing IPC pipe streams: {}" + e.getMessage());
				LOGGER.trace("", e);
			}
		}
	}

	public String getInputPipe() {
		return mkin.getInputPipe();
	}

	public String getOutputPipe() {
		return mkout.getOutputPipe();
	}

	public ProcessWrapper getPipeProcess() {
		return this;
	}

	public void deleteLater() {
		mkin.deleteLater();
		mkout.deleteLater();
	}

	public InputStream getInputStream() throws IOException {
		return mkin.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return mkout.getOutputStream();
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
		return isAlive();
	}

	@Override
	public void runInNewThread() {
		if (!Platform.isWindows()) {
			mkin.getPipeProcess().runInNewThread();
			mkout.getPipeProcess().runInNewThread();

			// Allow the threads some time to do their work before
			// starting the main thread
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
			}
		}

		start();
	}

	@Override
	public void runInSameThread() {
		if (!Platform.isWindows()) {
			mkin.getPipeProcess().runInNewThread();
			mkout.getPipeProcess().runInNewThread();

			// Allow the threads some time to do their work before
			// running the main thread
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
			}
		}

		run();
	}

	@Override
	public boolean isReadyToStop() {
		return false;
	}

	@Override
	public void setReadyToStop(boolean nullable) {
	}

	@Override
	public void stopProcess() {
		this.interrupt();
		mkin.getPipeProcess().stopProcess();
		mkout.getPipeProcess().stopProcess();
	}
}
