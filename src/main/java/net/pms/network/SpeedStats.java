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
package net.pms.network;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.platform.IPlatformUtils;
import net.pms.platform.PlatformUtils;
import net.pms.util.SimpleThreadFactory;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network speed tester class.
 *
 * This can be used in an asynchronous way, as it returns Future objects.
 *
 * {@link CompletableFuture<Integer>} speed = SpeedStats.getDefault().calculateSpeedInMBits(addr, "Renderer");
 *
 * @see CompletableFuture
 *
 * @author zsombor <gzsombor@gmail.com>
 *
 */
public class SpeedStats {

	private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newCachedThreadPool(
			new SimpleThreadFactory("SpeedStats background worker", "SpeedStats background workers group", Thread.NORM_PRIORITY)
	);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("SpeedStats Executor Shutdown Hook") {
			@Override
			public void run() {
				BACKGROUND_EXECUTOR.shutdownNow();
			}
		});
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SpeedStats.class);


	private static final Map<String, CompletableFuture<Integer>> SPEED_STATS = new ConcurrentHashMap<>();

	@FunctionalInterface
	public interface SpeedMeasurement {
		int measure(InetAddress addr, String rendererName) throws Exception;
	}

	private static final class PingSpeedMeasurement implements SpeedMeasurement {
		@Override
		public int measure(InetAddress addr, String rendererName) throws Exception {
			return new MeasureSpeed(addr, rendererName).call();
		}
	}

	private static final SpeedStats DEFAULT = new SpeedStats(new PingSpeedMeasurement());

	public static SpeedStats getDefault() {
		return DEFAULT;
	}

	private final SpeedMeasurement measurement;

	/**
	 * @param measurement measurement strategy (injectable for tests)
	 */
	public SpeedStats(SpeedMeasurement measurement) {
		if (measurement == null) {
			throw new IllegalArgumentException("measurement cannot be null");
		}
		this.measurement = measurement;
	}

	/**
	 * Get the cached speed in Mb/s for the given address, or {@code -1} if no value is cached.
	 * Does not trigger a new measurement.
	 * <p>
	 * May block future resolution for a short amount of time
	 * to resolve the hostname if IP statistics are not available.
	 * May also block future resolution if calculation is in progress.
	 *
	 * @param addr the {@link InetAddress} to lookup.
	 * @return The {@link CompletableFuture<Integer>} with the estimated network throughput or
	 * {@code -1} if no value is cached or there was an error measuring the speed.
	 */
	public CompletableFuture<Integer> getCachedSpeedInMBits(InetAddress addr) {
		String ip = addr.getHostAddress();
		CompletableFuture<Integer> value = SPEED_STATS.get(ip);
		if (value != null) {
			return value;
		}
		return CompletableFuture.supplyAsync(
				() -> {
					final String hostname = DnsResolver.resolveReverse(addr);
					return SPEED_STATS.get(hostname);
				},
				BACKGROUND_EXECUTOR
		  ).thenCompose(
				future -> 
					future != null ? future : CompletableFuture.completedFuture(-1)
			);
	}

	/**
	 * Return the network throughput for the given IP address in MBits.
   *
   * It is calculated in the background, and cached, so only a reference is
   * given to the result, which can be retrieved by calling the get() method
   * on it.
	 * 
	 * Calling this always starts a background measurement and returns
	 * a {@link CompletableFuture} that completes with the speed in Mb/s.
	 * 
	 * When the future completes, the cache is updated (under both hostname and IP).
   *
   * @param addr
   * @param rendererName
   *
   * @return The network throughput as a {@link CompletableFuture} that completes
	 */
	public CompletableFuture<Integer> calculateSpeedInMBits(InetAddress addr, String rendererName) {
		final String ip = addr.getHostAddress();
		final CompletableFuture<Integer> future = CompletableFuture.supplyAsync(
			() -> {
				try {
					return measurement.measure(addr, rendererName);
				} catch (Exception e) {
					throw new CompletionException(e);
				}
			},
			BACKGROUND_EXECUTOR
		);
		SPEED_STATS.put(ip, future);
		// Fill by hostname if available and different from IP after reverse resolution
		CompletableFuture.runAsync(
			() -> {
				final String hostname = DnsResolver.resolveReverse(addr);
				if (!hostname.equals(ip)) {
					SPEED_STATS.put(hostname, future);
				}
			},
			BACKGROUND_EXECUTOR
		);
		return future;
	}

	private static class MeasureSpeed implements Callable<Integer> {

		private final InetAddress addr;
		private final String rendererName;

		public MeasureSpeed(InetAddress addr, String rendererName) {
			this.addr = addr;
			this.rendererName = rendererName != null ? rendererName.replaceAll("\n", "") : "Unknown";
		}

		@Override
		public Integer call() throws Exception {
			try {
				return doCall();
			} catch (Exception e) {
				LOGGER.warn("Error measuring network throughput : " + e.getMessage(), e);
				throw e;
			}
		}

		private Integer doCall() throws Exception {
			String ip = addr.getHostAddress();
			LOGGER.info("Checking IP: {} for {}", ip, rendererName);
			LOGGER.info("Renderer {} found on address: {}", rendererName, ip);

			int[] sizes = {512, 1476, 9100, 32000, 64000};
			double bps = 0;
			int cnt = 0;

			for (int i = 0; i < sizes.length; i++) {
				double p = doPing(sizes[i]);
				if (p != 0) {
					bps += p;
					cnt++;
				}
			}
			// Avoid division by zero if no ping was successful
			if (cnt == 0) {
				return -1;
			}
			double speedInMbits1 = bps / (cnt * 1000000);
			LOGGER.info("Renderer {} has an estimated network speed of {} Mb/s", rendererName, speedInMbits1);
			int speedInMbits = (int) speedInMbits1;
			if (speedInMbits1 < 1.0) {
				speedInMbits = -1;
			}
			return speedInMbits;
		}

		private double doPing(int size) {
			// let's get that speed
			OutputParams op = new OutputParams(null);
			op.setLog(true);
			op.setMaxBufferSize(1);
			IPlatformUtils sysUtil = PlatformUtils.INSTANCE;
			final ProcessWrapperImpl pw = new ProcessWrapperImpl(sysUtil.getPingCommand(addr.getHostAddress(), 5, size), op, true, false);
			Runnable r = () -> {
				UMSUtils.sleep(3000);
				pw.stopProcess();
			};

			Thread failsafe = new Thread(r, "SpeedStats Failsafe");
			failsafe.start();
			pw.runInSameThread();
			List<String> ls = pw.getOtherResults();
			double time = 0;
			int c = 0;
			String timeString;

			for (String line : ls) {
				timeString = sysUtil.parsePingLine(line);
				if (timeString == null) {
					continue;
				}
				try {
					time += Double.parseDouble(timeString);
					c++;
				} catch (NumberFormatException e) {
					// no big deal
					LOGGER.debug("Could not estimate network speed from time: \"" + timeString + "\"");
				}
			}

			if (c > 0) {
				time /= c;
				int frags = sysUtil.getPingPacketFragments(size);
				LOGGER.debug("Estimated speed from ICMP packet size {} in {} fragment(s) is {} bit/s", size, frags, ((size + 8 + (frags * 32)) * 8000 * 2) / time);
				return ((size + 8 + (frags * 32)) * 8000 * 2) / time;
			}
			return time;
		}
	}

}
