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

import java.io.InputStream;
import java.util.concurrent.FutureTask;
import javax.annotation.Nullable;


/**
 * This is an interface for consuming a process wrapper output
 * {@link InputStream}. An implementation will start consuming an
 * {@link InputStream} in a new {@link Thread} when {@link #consume} is called,
 * and return the output as a {@link FutureTask} of {@code T}. It will also
 * create a new {@link ProcessWrapperResult} instance of the appropriate type
 * {@code R} for the process wrapper with {@link #createResult}.
 *
 * @param <R> the {@link ProcessWrapperResult} to use.
 * @param <T> the object type in which to store the output.
 *
 * @author Nadahar
 */
public interface ProcessWrapperConsumer<R, T> {

	/**
	 * Spawns a new {@link Thread} with a default name and starts consuming the
	 * specified {@link InputStream}.
	 *
	 * @param inputStream the {@link InputStream} to consume.
	 * @return A {@link FutureTask} of {@code T} containing the consumed process
	 *         output.
	 */
	@Nullable
	public FutureTask<T> consume(@Nullable final InputStream inputStream);

	/**
	 * Spawns a new {@link Thread} with the specified name and starts consuming
	 * the specified {@link InputStream}.
	 *
	 * @param inputStream the {@link InputStream} to consume.
	 * @param threadName the name of the consuming {@link Thread}.
	 * @return A {@link FutureTask} of {@code T} containing the consumed process
	 *         output.
	 */
	@Nullable
	public FutureTask<T> consume(@Nullable final InputStream inputStream, @Nullable String threadName);

	/**
	 * Creates a new {@link ProcessWrapperResult} instance of type {@code R}
	 * using the specified parameters.
	 *
	 * @param output the resulting output of type {@code T}.
	 * @param exitCode the process exit code.
	 * @param throwable a {@link Throwable} that was thrown while running the
	 *            process or consuming its output if applicable.
	 * @return The new instance of {@code R}.
	 */
	public R createResult(@Nullable T output, int exitCode, @Nullable Throwable throwable);
}
