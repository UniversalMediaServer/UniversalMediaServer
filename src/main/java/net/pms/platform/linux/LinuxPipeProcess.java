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
package net.pms.platform.linux;

import com.sun.jna.Platform;
import java.io.*;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.BufferedOutputFile;
import net.pms.io.IPipeProcess;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.platform.windows.WindowsNamedPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process to create a platform specific communications pipe that provides
 * an input stream and output stream. Other processes can then transmit
 * content via this pipe.
 */
public class LinuxPipeProcess implements IPipeProcess {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxPipeProcess.class);
	private UmsConfiguration configuration;

	private String linuxPipeName;
	private WindowsNamedPipe mk;

	public LinuxPipeProcess(String pipeName, OutputParams params, String... extras) {
		// Use device-specific pms conf
		configuration = PMS.getConfiguration(params);
		linuxPipeName = getPipeName(pipeName);
	}

	public LinuxPipeProcess(String pipeName, String... extras) {
		this(pipeName, null, extras);
	}

	@Override
	public String getInputPipe() {
		return linuxPipeName;
	}

	@Override
	public String getOutputPipe() {
		return linuxPipeName;
	}

	@Override
	public ProcessWrapper getPipeProcess() {
		OutputParams mkfifoVidParams = new OutputParams(configuration);
		mkfifoVidParams.setMaxBufferSize(0.1);
		mkfifoVidParams.setLog(true);
		String[] cmdArray;

		if (Platform.isMac() || Platform.isFreeBSD() || Platform.isSolaris()) {
			cmdArray = new String[] {"mkfifo", "-m", "777", linuxPipeName};
		} else {
			cmdArray = new String[] {"mkfifo", "--mode=777", linuxPipeName};
		}

		return new ProcessWrapperImpl(cmdArray, mkfifoVidParams);
	}

	@Override
	public void deleteLater() {
		File f = new File(linuxPipeName);
		f.deleteOnExit();
	}

	@Override
	public BufferedOutputFile getDirectBuffer() {
		if (!Platform.isWindows()) {
			return null;
		}

		return mk.getDirectBuffer();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		LOGGER.trace("Opening file {} for reading...", linuxPipeName);
		RandomAccessFile raf = new RandomAccessFile(linuxPipeName, "r");

		return new FileInputStream(raf.getFD());
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		LOGGER.trace("Opening file {} for writing...", linuxPipeName);
		RandomAccessFile raf = new RandomAccessFile(linuxPipeName, "rw");

		return new FileOutputStream(raf.getFD());
	}

	private static String getPipeName(String pipeName) {
		try {
			return PMS.getConfiguration().getTempFolder() + "/" + pipeName;
		} catch (IOException e) {
			LOGGER.error("Pipe may not be in temporary directory: {}", e.getMessage());
			LOGGER.trace("", e);
			return pipeName;
		}
	}

}
