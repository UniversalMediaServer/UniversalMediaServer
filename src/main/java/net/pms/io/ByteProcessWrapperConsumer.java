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

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link ProcessWrapperConsumer} implementation that return the process
 * output as an array of bytes wrapped in a {@link ByteProcessWrapperResult}.
 *
 * @author Nadahar
 */
public class ByteProcessWrapperConsumer implements ProcessWrapperConsumer<ByteProcessWrapperResult, byte[]> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ByteProcessWrapperConsumer.class);

	@Override
	@Nullable
	public FutureTask<byte[]> consume(InputStream inputStream) {
		return consume(inputStream, null);
	}

	@Override
	@Nullable
	public FutureTask<byte[]> consume(@Nullable final InputStream inputStream, @Nullable String threadName) {
		if (inputStream == null) {
			return null;
		}
		Callable<byte[]> callable = new Callable<byte[]>() {

			@Override
			public byte[] call() throws Exception {
				byte[] result = IOUtils.toByteArray(inputStream);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Captured {} bytes of process output", result.length);
				}
				return result;
			}
		};
		FutureTask<byte[]> result = new FutureTask<byte[]>(callable);
		Thread runner;
		if (isBlank(threadName)) {
			runner = new Thread(result);
		} else {
			runner = new Thread(result, threadName);
		}
		runner.start();
		return result;
	}

	@Override
	public ByteProcessWrapperResult createResult(
		@Nullable byte[] output,
		int exitCode,
		@Nullable Throwable throwable
	) {
		return new ByteProcessWrapperResult(output, exitCode, throwable);
	}
}
