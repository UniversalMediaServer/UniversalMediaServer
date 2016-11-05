/*
 * Digital Media Server, for streaming digital media to DLNA compatible devices
 * based on www.ps3mediaserver.org and www.universalmediaserver.com.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is a free software; you can redistribute it and/or
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * A container for {@link Process} results with its output stored as an array of
 * bytes.
 *
 * @author Nadahar
 */
@Immutable
public class ByteProcessWrapperResult implements ProcessWrapperResult<byte[]> {

	/** The process output */
	protected final byte[] output;

	/** The exit code */
	protected final int exitCode;

	/** The {@link Throwable} if any */
	protected final Throwable throwable;

	/**
	 * Creates a new {@link ByteProcessWrapperResult} instance with the given
	 * data.
	 *
	 * @param output the process output.
	 * @param exitCode the process exit code.
	 * @param throwable a {@link Throwable} if one was thrown during execution.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public ByteProcessWrapperResult(@Nullable byte[] output, int exitCode, @Nullable Throwable throwable) {
		this.output = output;
		this.exitCode = exitCode;
		this.throwable = throwable;
	}

	@Override
	@Nonnull
	public byte[] getOutput() {
		return output != null ? output : new byte[0];
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public Throwable getError() {
		return throwable;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Process exited with code ").append(exitCode).append(":\n");
		sb.append("Captured {} bytes of output").append(output.length);
		return sb.toString();
	}
}
