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
package net.pms.service.process;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a {@link Process} with corresponding orders to the
 * {@link ProcessTerminator}.
 *
 * @author Nadahar
 */
@Immutable
public class ProcessTicket {

	/** The ticket {@link Process} */
	protected final Process process;

	/** The ticket process name */
	protected final String processName;

	/** The ticket {@link ProcessTicketAction} */
	protected final ProcessTicketAction action;

	/** The timeout in milliseconds if applicable */
	protected final long timeoutMS;

	/** The termination timeout in milliseconds if applicable */
	protected final long terminateTimeoutMS;

	/**
	 * Creates a new ticket using the specified parameters.
	 *
	 * @param process the ticket {@link Process}.
	 * @param processName the ticket process name.
	 * @param action the {@link ProcessTicketAction}.
	 * @param timeoutMS the timeout in milliseconds if {@code action} is
	 *            {@link ProcessTicketAction#ADD}.
	 * @param terminateTimeoutMS the termination timeout in milliseconds if
	 *            {@code action} is {@link ProcessTicketAction#ADD}.
	 */
	public ProcessTicket(
		@Nonnull Process process,
		@Nonnull String processName,
		@Nonnull ProcessTicketAction action,
		long timeoutMS,
		long terminateTimeoutMS
	) {
		if (process == null) {
			throw new IllegalArgumentException("process cannot be null");
		}
		if (StringUtils.isBlank(processName)) {
			throw new IllegalArgumentException("processName cannot be blank");
		}
		if (action == null) {
			throw new IllegalArgumentException("action cannot be null");
		}
		this.process = process;
		this.processName = processName;
		this.action = action;
		this.timeoutMS = Math.max(0, timeoutMS);
		this.terminateTimeoutMS = Math.max(action == ProcessTicketAction.ADD ? 100 : 0, terminateTimeoutMS);
	}

	/**
	 * @return The {@link Process}.
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * @return The {@link Process} name.
	 */
	public String getName() {
		return processName;
	}

	/**
	 * @return The {@link ProcessTicketAction}.
	 */
	public ProcessTicketAction getAction() {
		return action;
	}

	/**
	 * @return The process timeout in milliseconds.
	 */
	public long getTimeoutMS() {
		return timeoutMS;
	}

	/**
	 * @return The process terminate timeout in milliseconds.
	 */
	public long getTerminateTimeoutMS() {
		return terminateTimeoutMS;
	}

	@Override
	public String toString() {
		return
			"ProcessTicket [Name=" + processName + ", Timeout=" + timeoutMS +
			" ms, Terminate Timeout=" + terminateTimeoutMS + " ms, Action=" + action + "]";
	}
}
