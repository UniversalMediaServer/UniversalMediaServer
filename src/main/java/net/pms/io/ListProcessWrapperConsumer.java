/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.annotation.Nullable;
import net.pms.platform.PlatformUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ProcessWrapperConsumer} implementation that return the process
 * output as a {@link List} of {@link String}s wrapped in a
 * {@link ListProcessWrapperResult}.
 *
 * @author Nadahar
 */
public class ListProcessWrapperConsumer implements ProcessWrapperConsumer<ListProcessWrapperResult, List<String>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListProcessWrapperConsumer.class);

	@Override
	@Nullable
	public FutureTask<List<String>> consume(InputStream inputStream) {
		return consume(inputStream, null);
	}

	@Override
	@Nullable
	public FutureTask<List<String>> consume(@Nullable final InputStream inputStream, @Nullable String threadName) {
		if (inputStream == null) {
			return null;
		}
		Callable<List<String>> callable = () -> {
			List<String> result = new ArrayList<>();
			Charset outputCharset = PlatformUtils.INSTANCE.getDefaultCharset();
			try (
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, outputCharset))
					) {
				String line;
				while ((line = reader.readLine()) != null) {
					result.add(line);
				}
			}
			if (LOGGER.isTraceEnabled()) {
				for (String line : result) {
					LOGGER.trace("Process output: {}", line);
				}
			}
			return result;
		};
		FutureTask<List<String>> result = new FutureTask<>(callable);
		Thread runner;
		if (StringUtils.isBlank(threadName)) {
			runner = new Thread(result);
		} else {
			runner = new Thread(result, threadName);
		}
		runner.start();
		return result;
	}

	@Override
	public ListProcessWrapperResult createResult(
		@Nullable List<String> output,
		int exitCode,
		@Nullable Throwable throwable
	) {
		return new ListProcessWrapperResult(output, exitCode, throwable);
	}
}
