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
package net.pms.platform.windows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.pms.io.BufferedOutputFile;
import net.pms.io.IPipeProcess;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;

/**
 * Process to create a platform specific communications pipe that provides
 * an input stream and output stream. Other processes can then transmit
 * content via this pipe.
 */
public class WindowsPipeProcess implements IPipeProcess {
	private WindowsNamedPipe mk;

	public WindowsPipeProcess(String pipeName, OutputParams params, String... extras) {
		boolean forcereconnect = false;
		boolean in = true;

		if (extras != null && extras.length > 0 && extras[0].equals("out")) {
			in = false;
		}

		if (extras != null) {
			for (String extra : extras) {
				if (extra.equals("reconnect")) {
					forcereconnect = true;
				}
			}
		}
		mk = new WindowsNamedPipe(pipeName, forcereconnect, in, params);
	}

	public WindowsPipeProcess(String pipeName, String... extras) {
		this(pipeName, null, extras);
	}

	@Override
	public String getInputPipe() {
		return mk.getPipeName();
	}

	@Override
	public String getOutputPipe() {
		return mk.getPipeName();
	}

	@Override
	public ProcessWrapper getPipeProcess() {
		return mk;
	}

	@Override
	public void deleteLater() {
		//not implemented
	}

	@Override
	public BufferedOutputFile getDirectBuffer() {
		return mk.getDirectBuffer();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return mk.getReadable();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return mk.getWritable();
	}
}
