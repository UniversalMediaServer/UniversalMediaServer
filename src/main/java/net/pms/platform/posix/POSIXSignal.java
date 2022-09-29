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
package net.pms.platform.posix;

/**
 * This {@code enum} represents the different POSIX signals
 *
 * @author Nadahar
 */
public enum POSIXSignal {

	/**
	 * 1: POSIX {@code SIGHUP} - Hangup detected on controlling terminal or
	 * death of controlling process
	 */
	SIGHUP(1),

	/** 2: POSIX {@code SIGINT} - Interrupt from keyboard */
	SIGINT(2),

	/** 3: POSIX {@code SIGQUIT} - Quit from keyboard */
	SIGQUIT(3),

	/** 4: POSIX {@code SIGILL} - Illegal Instruction */
	SIGILL(4),

	/** 6: POSIX {@code SIGABRT} - Abort signal from abort() */
	SIGABRT(6),

	/** 8: POSIX {@code SIGFPE} - Floating-point exception */
	SIGFPE(8),

	/** 9: POSIX {@code SIGKILL} - Kill signal */
	SIGKILL(9),

	/** 11: POSIX {@code SIGSEGV} - Invalid memory reference */
	SIGSEGV(11),

	/**
	 * 13: POSIX {@code SIGPIPE} - Broken pipe: write to pipe with no
	 * readers
	 */
	SIGPIPE(13),

	/** 14: POSIX {@code SIGALRM} - Timer signal from alarm() */
	SIGALRM(14),

	/** 15: POSIX {@code SIGTERM} - Termination signal */
	SIGTERM(15);

	private final int value;

	private POSIXSignal(int value) {
		this.value = value;
	}

	/**
	 * @return The integer value.
	 */
	public int getValue() {
		return value;
	}

	@Override
	public String toString() {
		return name() + " (" + value + ")";
	}
}
