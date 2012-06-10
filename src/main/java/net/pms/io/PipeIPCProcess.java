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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import net.pms.util.DTSAudioOutputStream;
import net.pms.util.H264AnnexBInputStream;
import net.pms.util.PCMAudioOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Platform;

public class PipeIPCProcess extends Thread implements ProcessWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(PipeIPCProcess.class);
	private PipeProcess mkin;
	private PipeProcess mkout;
	private StreamModifier modifier;

	public StreamModifier getModifier() {
		return modifier;
	}

	public void setModifier(StreamModifier modifier) {
		this.modifier = modifier;
	}

	public PipeIPCProcess(String pipeName, String pipeNameOut, boolean forcereconnect1, boolean forcereconnect2) {
		mkin = new PipeProcess(pipeName, forcereconnect1 ? "reconnect" : "dummy");
		mkout = new PipeProcess(pipeNameOut, "out", forcereconnect2 ? "reconnect" : "dummy");
	}

	public void run() {
		byte b[] = new byte[512 * 1024];
		int n = -1;
		InputStream in = null;
		OutputStream out = null;
		OutputStream debug = null;

		try {
			in = mkin.getInputStream();
			out = mkout.getOutputStream();

			if (modifier != null && modifier.isH264_annexb()) {
				in = new H264AnnexBInputStream(in, modifier.getHeader());
			} else if (modifier != null && modifier.isDtsembed()) {
				out = new DTSAudioOutputStream(new PCMAudioOutputStream(out, modifier.getNbchannels(), modifier.getSampleFrequency(), modifier.getBitspersample()));
			} else if (modifier != null && modifier.isPcm()) {
				out = new PCMAudioOutputStream(out, modifier.getNbchannels(), modifier.getSampleFrequency(), modifier.getBitspersample());
			}

			if (modifier != null && modifier.getHeader() != null && !modifier.isH264_annexb()) {
				out.write(modifier.getHeader());
			}
			while ((n = in.read(b)) > -1) {
				out.write(b, 0, n);
				if (debug != null) {
					debug.write(b, 0, n);
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Error :" + e.getMessage());
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
				LOGGER.debug("Error :" + e.getMessage());
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
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
			}
		}
		start();
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
