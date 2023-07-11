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
package net.pms.service;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.pms.service.process.ProcessManager;
import net.pms.service.sleep.SleepManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates and destroys global services ands offers a static way of
 * referencing them.
 * <p>
 * It is <i>not</i> thread-safe: The services must be created before any threads
 * use the services and destroyed after other threads have terminated. This is
 * to avoid the cost of synchronization each time a service reference is needed.
 * <p>
 * <b>Note:</b> This class holds instance references that rely on
 * {@link UmsConfiguration} being initialized. This is therefore not suitable to
 * hold instance references that must exist when {@link UmsConfiguration} itself
 * is initialized.
 *
 * @author Nadahar
 */
@NotThreadSafe
public class Services {
	private static final Logger LOGGER = LoggerFactory.getLogger(Services.class);

	private static Services instance;

	private ProcessManager processManager;

	private SleepManager sleepManager;

	/**
	 * Returns current static {@link Services} instance. Isn't normally needed,
	 * use the static method for the individual service instead.
	 *
	 * @return The current static {@link Services} instance.
	 */
	@Nullable
	public static Services get() {
		return instance;
	}

	/**
	 * Creates a new static {@link Services} instance and starts the services.
	 *
	 * @throws IllegalStateException If the services have already been created.
	 */
	public static void create() {
		if (instance != null) {
			throw new IllegalStateException("Services have already been created");
		}
		instance = new Services();
	}

	/**
	 * Stops the services and nulls the static {@link Services} instance.
	 */
	public static void destroy() {
		if (instance != null) {
			instance.stop();
			instance = null;
		}
	}

	/**
	 * @return The {@link ProcessManager} instance.
	 */
	@Nullable
	public static ProcessManager processManager() {
		return instance == null ? null : instance.getProcessManager();
	}

	/**
	 * @return The {@link SleepManager} instance.
	 */
	@Nullable
	public static SleepManager sleepManager() {
		return instance == null ? null : instance.getSleepManager();
	}

	/**
	 * Sets the static {@link Services} instance. Isn't normally needed, use
	 * {@link Services#create()} instead.
	 *
	 * @param newInstance the {@link Services} instance to set.
	 */
	public static void set(Services newInstance) {
		instance = newInstance;
	}

	/**
	 * Stops the {@link Services} {@link ProcessManager}.
	 * This will cause its process terminator thread to terminate all managed
	 * processes and stop.
	 */
	public static void stopProcessManager() {
		if (instance != null && instance.processManager != null) {
				instance.processManager.stop();
		}
	}

	/**
	 * Creates a new instance and starts the services. Isn't normally needed,
	 * use {@link Services#create()} instead.
	 */
	public Services() {
		start();
	}

	/**
	 * Creates and starts the services. Isn't normally needed, use
	 * {@link Services#create()} instead.
	 *
	 * @throws IllegalStateException If the services have already been started.
	 */
	public final void start() {
		if (processManager != null || sleepManager != null) {
			throw new IllegalStateException("Services have already been started");
		}
		LOGGER.debug("Starting services");

		processManager = new ProcessManager();
		sleepManager = new SleepManager();
	}

	/**
	 * Stops and releases the current services.
	 */
	public void stop() {
		LOGGER.debug("Stopping services");

		if (processManager != null) {
			processManager.stop();
			processManager = null;
		}

		if (sleepManager != null) {
			sleepManager.stop();
			sleepManager = null;
		}
	}

	/**
	 * Isn't normally needed, use {@link Services#processManager} instead.
	 *
	 * @return The {@link ProcessManager} instance.
	 */
	public ProcessManager getProcessManager() {
		return processManager;
	}

	/**
	 * Isn't normally needed, use {@link Services#sleepManager} instead.
	 *
	 * @return The {@link SleepManager} instance.
	 */
	public SleepManager getSleepManager() {
		return sleepManager;
	}
}
