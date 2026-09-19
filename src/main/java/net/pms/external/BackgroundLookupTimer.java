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
package net.pms.external;

import java.io.File;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/** Measures only synchronous preparation, never the queued lookup itself. */
public final class BackgroundLookupTimer {
	private final long started = System.nanoTime();
	private long phaseStarted = started;
	private String currentPhase = "setup";
	private final String[] phases = new String[12];
	private final long[] durations = new long[12];
	private int count;

	public static void phase(BackgroundLookupTimer timer, String phase) {
		if (timer != null) {
			timer.phase(phase);
		}
	}

	public void phase(String phase) {
		long now = System.nanoTime();
		record(now);
		phaseStarted = now;
		currentPhase = phase;
	}

	private void record(long now) {
		if (count < phases.length) {
			phases[count] = currentPhase;
			durations[count++] = now - phaseStarted;
		}
	}

	public void log(Logger logger, String provider, File file) {
		long finished = System.nanoTime();
		long totalMillis = TimeUnit.NANOSECONDS.toMillis(finished - started);
		if (totalMillis < 20 || !logger.isInfoEnabled()) {
			return;
		}
		record(finished);
		StringBuilder detail = new StringBuilder();
		for (int i = 0; i < count; i++) {
			if (!detail.isEmpty()) {
				detail.append(", ");
			}
			detail.append(phases[i]).append(' ')
				.append(TimeUnit.NANOSECONDS.toMillis(durations[i])).append(" ms");
		}
		logger.info("Slow background lookup preparation for \"{}\": {} ms total, provider {}; {}",
			file.getAbsolutePath(), totalMillis, provider, detail);
	}
}
