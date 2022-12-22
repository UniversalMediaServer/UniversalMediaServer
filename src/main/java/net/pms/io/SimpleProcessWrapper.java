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
package net.pms.io;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.service.Services;

/**
 * A simple blocking {@link Process} wrapper that runs the process in the same
 * thread, consumes all output and returns the result as a
 * {@link ProcessWrapperResult}.
 *
 * @param <C> the {@link ProcessWrapperConsumer} to use.
 * @param <R> the {@link ProcessWrapperResult} to use.
 * @param <T> the process output type.
 *
 * @author Nadahar
 */
public class SimpleProcessWrapper<C extends ProcessWrapperConsumer<R, T>, R extends ProcessWrapperResult<T>, T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleProcessWrapper.class);

	/** The {@link ProcessWrapperConsumer} instance */
	protected final C consumer;

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ListProcessWrapperResult}.
	 *
	 * @param timeout the process timeout in {@code timeUnit}.
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @param command the command(s) used to create the {@link Process}.
	 * @return The process result as a {@link ListProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ListProcessWrapperResult runProcessListOutput(
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ListProcessWrapperConsumer()
		).runProcess(timeUnit.toMillis(timeout), terminateTimeoutMS, command);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ListProcessWrapperResult}.
	 *
	 * @param timeoutMS the process timeout in milliseconds.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @param command the command(s) used to create the {@link Process}.
	 * @return The process result as a {@link ListProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ListProcessWrapperResult runProcessListOutput(
		long timeoutMS,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ListProcessWrapperConsumer()
		).runProcess(timeoutMS, terminateTimeoutMS, command);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ListProcessWrapperResult}.
	 *
	 * @param command the command(s) used to create the {@link Process}.
	 * @param timeout the process timeout in {@code timeUnit}.
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @return The process result as a {@link ListProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ListProcessWrapperResult runProcessListOutput(
		@Nonnull List<String> command,
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ListProcessWrapperConsumer()
		).runProcess(command, timeUnit.toMillis(timeout), terminateTimeoutMS);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ListProcessWrapperResult}.
	 *
	 * @param command the command(s) used to create the {@link Process}.
	 * @param timeoutMS the process timeout in milliseconds.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @return The process result as a {@link ListProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ListProcessWrapperResult runProcessListOutput(
		@Nonnull List<String> command,
		long timeoutMS,
		long terminateTimeoutMS
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ListProcessWrapperConsumer()
		).runProcess(command, timeoutMS, terminateTimeoutMS);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ByteProcessWrapperResult}.
	 *
	 * @param timeout the process timeout in {@code timeUnit}.
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @param command the command(s) used to create the {@link Process}.
	 * @return The process result as a {@link ByteProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ByteProcessWrapperResult runProcessByteOutput(
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ByteProcessWrapperConsumer()
		).runProcess(timeUnit.toMillis(timeout), terminateTimeoutMS, command);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ByteProcessWrapperResult}.
	 *
	 * @param timeoutMS the process timeout in milliseconds.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @param command the command(s) used to create the {@link Process}.
	 * @return The process result as a {@link ByteProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ByteProcessWrapperResult runProcessByteOutput(
		long timeoutMS,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ByteProcessWrapperConsumer()
		).runProcess(timeoutMS, terminateTimeoutMS, command);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ByteProcessWrapperResult}.
	 *
	 * @param command the command(s) used to create the {@link Process}.
	 * @param timeout the process timeout in {@code timeUnit}.
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @return The process result as a {@link ByteProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ByteProcessWrapperResult runProcessByteOutput(
		@Nonnull List<String> command,
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ByteProcessWrapperConsumer()
		).runProcess(command, timeUnit.toMillis(timeout), terminateTimeoutMS);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is returned as a
	 * {@link ByteProcessWrapperResult}.
	 *
	 * @param command the command(s) used to create the {@link Process}.
	 * @param timeoutMS the process timeout in milliseconds.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @return The process result as a {@link ByteProcessWrapperResult}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	@Nonnull
	public static ByteProcessWrapperResult runProcessByteOutput(
		@Nonnull List<String> command,
		long timeoutMS,
		long terminateTimeoutMS
	) throws InterruptedException {
		return new SimpleProcessWrapper<>(
			new ByteProcessWrapperConsumer()
		).runProcess(command, timeoutMS, terminateTimeoutMS);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is discarded.
	 *
	 * @param timeout the process timeout in {@code timeUnit}.
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @param command the command(s) used to create the {@link Process}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	public static void runProcessNullOutput(
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		new SimpleProcessWrapper<>(
			new NullProcessWrapperConsumer()
		).runProcess(timeUnit.toMillis(timeout), terminateTimeoutMS, command);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is discarded.
	 *
	 * @param timeoutMS the process timeout in milliseconds.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @param command the command(s) used to create the {@link Process}.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	public static void runProcessNullOutput(
		long timeoutMS,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		new SimpleProcessWrapper<>(
			new NullProcessWrapperConsumer()
		).runProcess(timeoutMS, terminateTimeoutMS, command);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is discarded.
	 *
	 * @param command the command(s) used to create the {@link Process}.
	 * @param timeout the process timeout in {@code timeUnit}.
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	public static void runProcessNullOutput(
		@Nonnull List<String> command,
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS
	) throws InterruptedException {
		new SimpleProcessWrapper<>(
			new NullProcessWrapperConsumer()
		).runProcess(command, timeUnit.toMillis(timeout), terminateTimeoutMS);
	}

	/**
	 * Creates and runs a new {@link Process} in the same thread using the
	 * specified commands. The process result is discarded.
	 *
	 * @param command the command(s) used to create the {@link Process}.
	 * @param timeoutMS the process timeout in milliseconds.
	 * @param terminateTimeoutMS the timeout in milliseconds for each
	 *            termination attempt before a new attempt is made.
	 * @throws InterruptedException If interrupted while waiting for the
	 *             {@link Process} to finish.
	 */
	public static void runProcessNullOutput(
		@Nonnull List<String> command,
		long timeoutMS,
		long terminateTimeoutMS
	) throws InterruptedException {
		new SimpleProcessWrapper<>(
			new NullProcessWrapperConsumer()
		).runProcess(command, timeoutMS, terminateTimeoutMS);
	}

	/**
	 * Creates a new instance instance that will use the specified
	 * {@link ProcessWrapperConsumer} instance. In most cases it's simpler to
	 * use one of the static factory methods.
	 *
	 * @param consumer the {@link ProcessWrapperConsumer} instance of type
	 *            {@code C} to use.
	 */
	public SimpleProcessWrapper(C consumer) {
		this.consumer = consumer;
	}

	/**
	 * Creates a new instance instance that will create and use a new instance
	 * the specified {@link ProcessWrapperConsumer} class. In most cases it's
	 * simpler to use one of the static factory methods.
	 *
	 * @param consumer the {@link ProcessWrapperConsumer} class of type
	 *            {@code C} to create.
	 * @throws InstantiationException If an error occurs during creating of the
	 *             new {@link ProcessWrapperConsumer} instance.
	 * @throws IllegalAccessException If an access error occurs during creating
	 *             of the new {@link ProcessWrapperConsumer} instance.
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
	public SimpleProcessWrapper(Class<C> consumer) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		this.consumer = consumer.getDeclaredConstructor().newInstance();
	}

	/**
	 * Runs a process with the given command array.
	 *
	 * @param timeout the process timeout in {@code timeUnit} after which the
	 *            process is terminated. Use zero for no timeout, but be aware
	 *            of the <a href=
	 *            "https://web.archive.org/web/20121201070147/http://kylecartmell.com/?p=9"
	 *            >pitfalls</a>
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds to wait for each
	 *            termination attempt.
	 * @param command the {@link String}s used to build the command line.
	 * @return The {@link ProcessWrapperResult} from running the process.
	 * @throws InterruptedException If the operation is interrupted.
	 * @throws IllegalArgumentException If {@code command} is {@code null} or
	 *             empty.
	 */
	@Nonnull
	public R runProcess(
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		return runProcess(Arrays.asList(command), timeUnit.toMillis(timeout), terminateTimeoutMS);
	}

	/**
	 * Runs a process with the given command array.
	 *
	 * @param timeoutMS the process timeout in milliseconds after which the
	 *            process is terminated. Use zero for no timeout, but be aware
	 *            of the <a href=
	 *            "https://web.archive.org/web/20121201070147/http://kylecartmell.com/?p=9"
	 *            >pitfalls</a>
	 * @param terminateTimeoutMS the timeout in milliseconds to wait for each
	 *            termination attempt.
	 * @param command the {@link String}s used to build the command line.
	 * @return The {@link ProcessWrapperResult} from running the process.
	 * @throws InterruptedException If the operation is interrupted.
	 * @throws IllegalArgumentException If {@code command} is {@code null} or
	 *             empty.
	 */
	@Nonnull
	public R runProcess(
		long timeoutMS,
		long terminateTimeoutMS,
		@Nonnull String... command
	) throws InterruptedException {
		return runProcess(Arrays.asList(command), timeoutMS, terminateTimeoutMS);
	}

	/**
	 * Runs a process with the given command {@link List}.
	 *
	 * @param command an array of {@link String} used to build the command line.
	 * @param timeout the process timeout in {@code timeUnit} after which the
	 *            process is terminated. Use zero for no timeout, but be aware
	 *            of the <a href=
	 *            "https://web.archive.org/web/20121201070147/http://kylecartmell.com/?p=9"
	 *            >pitfalls</a>
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout in milliseconds to wait for each
	 *            termination attempt.
	 * @return The {@link ProcessWrapperResult} from running the process.
	 * @throws InterruptedException If the operation is interrupted.
	 * @throws IllegalArgumentException If {@code command} is {@code null} or
	 *             empty.
	 */
	@Nonnull
	public R runProcess(
		@Nonnull List<String> command,
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS
	) throws InterruptedException {
		return runProcess(command, timeUnit.toMillis(timeout), terminateTimeoutMS);
	}

	/**
	 * Runs a process with the given command {@link List}.
	 *
	 * @param command an array of {@link String} used to build the command line.
	 * @param timeoutMS the process timeout in milliseconds after which the
	 *            process is terminated. Use zero for no timeout, but be aware
	 *            of the <a href=
	 *            "https://web.archive.org/web/20121201070147/http://kylecartmell.com/?p=9"
	 *            >pitfalls</a>
	 * @param terminateTimeoutMS the timeout in milliseconds to wait for each
	 *            termination attempt.
	 * @return The {@link ProcessWrapperResult} from running the process.
	 * @throws InterruptedException If the operation is interrupted.
	 * @throws IllegalArgumentException If {@code command} is {@code null} or
	 *             empty.
	 */
	@Nonnull
	public R runProcess(
		@Nonnull List<String> command,
		long timeoutMS,
		long terminateTimeoutMS
	) throws InterruptedException {
		if (command.isEmpty()) {
			throw new IllegalArgumentException("command can't be null or empty");
		}
		final String executableName;
		if (StringUtils.isNotBlank(command.get(0))) {
			Path executable = Paths.get(command.get(0)).getFileName();
			if (executable != null) {
				executableName = executable.toString();
			} else {
				executableName = command.get(0);
			}
		} else {
			executableName = command.get(0);
		}
		boolean manageProcess = timeoutMS > 0;
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		if (LOGGER.isTraceEnabled()) {
			//XXX: Replace with String.join() in Java 8
			LOGGER.trace("Executing \"{}\"", StringUtils.join(command, " "));
		}
		Process process;
		try {
			process = processBuilder.start();
		} catch (IOException e) {
			LOGGER.debug("IOException when trying to start \"{}\" process: {}", executableName, e.getMessage());
			LOGGER.trace("", e);
			return consumer.createResult(null, Integer.MIN_VALUE, e);
		}
		Future<T> output = consumer.consume(process.getInputStream(), "SPW \"" + executableName + "\" consumer");
		if (manageProcess) {
			Services.processManager().addProcess(process, command.get(0), timeoutMS, terminateTimeoutMS);
		}
		int exitCode = Integer.MIN_VALUE;
		boolean interrupted;
		boolean shutdown = false;
		do {
			interrupted = false;
			try {
				exitCode = process.waitFor();
			} catch (InterruptedException e) {
				interrupted = Thread.interrupted();
				if (!shutdown) {
					if (manageProcess) {
						Services.processManager().shutdownProcess(process, executableName);
						manageProcess = false;
					} else {
						Services.processManager().addProcess(process, executableName, 0, terminateTimeoutMS);
					}
					shutdown = true;
				}
			}
		} while (interrupted);
		if (manageProcess) {
			Services.processManager().removeProcess(process, command.get(0));
		}
		try {
			return consumer.createResult(output.get(), exitCode, null);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			LOGGER.error(
				"ExecutionException in \"{}\" consumer, no output will be returned: {}",
				executableName,
				cause.getMessage()
			);
			LOGGER.trace("", e);
			return consumer.createResult(null, exitCode, cause);
		}
	}
}
