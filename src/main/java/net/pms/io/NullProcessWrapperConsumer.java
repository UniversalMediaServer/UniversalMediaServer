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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.service.Services;


/**
 * A {@link ProcessWrapperConsumer} implementation that doesn't return the
 * process output.
 *
 * @author Nadahar
 */
public class NullProcessWrapperConsumer implements ProcessWrapperConsumer<NullProcessWrapperResult, Void> {

	private static final Logger LOGGER = LoggerFactory.getLogger(NullProcessWrapperConsumer.class);

	@Override
	@Nullable
	public FutureTask<Void> consume(InputStream inputStream) {
		return consume(inputStream, null);
	}

	@Override
	@Nullable
	public FutureTask<Void> consume(@Nullable final InputStream inputStream, @Nullable String threadName) {
		if (inputStream == null) {
			return null;
		}
		Callable<Void> callable = new Callable<Void>() {

			@Override
			public Void call() throws IOException {
				List<String> result = LOGGER.isTraceEnabled() ? new ArrayList<String>() : null;
				Charset outputCharset;
				if (Services.WINDOWS_CONSOLE != null) {
					outputCharset = Services.WINDOWS_CONSOLE;
				} else {
					outputCharset = StandardCharsets.UTF_8;
				}

				try (
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, outputCharset))
				) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (result != null) {
							result.add(line);
						}
					}
				}
				if (result != null) {
					LOGGER.trace("Discarded {} lines of process output:", result.size());
					for (String line : result) {
						LOGGER.trace("  Process output: {}", line);
					}
				}
				return null;
			}
		};
		FutureTask<Void> result = new FutureTask<Void>(callable);
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
	public NullProcessWrapperResult createResult(
		@Nullable Void output,
		int exitCode,
		@Nullable Throwable throwable
	) {
		return null;
	}

}
