/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2011 G. Zsombor
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.pms.PMS;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.SystemUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network speed tester class. This can be used in an asynchronous way, as it returns Future objects.
 * 
 * Future<Integer> speed = SpeedStats.getInstance().getSpeedInMBits(addr);
 * 
 *  @see Future
 * 
 * @author zsombor <gzsombor@gmail.com>
 *
 */
public class SpeedStats {
	private static SpeedStats instance = new SpeedStats();
	private static ExecutorService executor = Executors.newCachedThreadPool();
	public static SpeedStats getInstance() {
		return instance;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SpeedStats.class);

	private final Map<String, Future<Integer>> speedStats = new HashMap<String, Future<Integer>>();

	/**
	 * Return the network throughput for the given IP address in MBits. It is calculated in the background, and cached,
	 * so only a reference is given to the result, which can be retrieved by calling the get() method on it.
	 * @param addr
	 * @return  The network throughput
	 */
	public Future<Integer> getSpeedInMBits(InetAddress addr, String rendererName) {
		synchronized(speedStats) { 
			Future<Integer> value = speedStats.get(addr.getHostAddress());
			if (value != null) {
				return value;
			}
			value = executor.submit(new MeasureSpeed(addr, rendererName));
			speedStats.put(addr.getHostAddress(), value);
			return value;
		}
	}

	class MeasureSpeed implements Callable<Integer> {
		InetAddress addr;
		String rendererName;

		public MeasureSpeed(InetAddress addr, String rendererName) {
			this.addr = addr;
			this.rendererName = rendererName != null ? rendererName : "Unknown";
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
			LOGGER.info("Checking IP: " + ip + " for " + rendererName);
			// calling the canonical host name the first time is slow, so we call it in a separate thread
			String hostname = addr.getCanonicalHostName();
			synchronized(speedStats) {
				Future<Integer> otherTask = speedStats.get(hostname);
				if (otherTask != null) {
					// wait a little bit
					try {
						// probably we are waiting for ourself to finish the work...
						Integer value = otherTask.get(100, TimeUnit.MILLISECONDS);
						// if the other task already calculated the speed, we get the result,
						// unless we do it now 
						if (value != null) {
							return value;
						}
					} catch (TimeoutException e) {
						LOGGER.trace("We couldn't get the value based on the canonical name");
					}
				}
			}

			
			if (!ip.equals(hostname)) {
				LOGGER.info("Renderer " + rendererName + " found on this address: " + hostname + " (" + ip + ")");
			} else {
				LOGGER.info("Renderer " + rendererName + " found on this address: " + ip);
			}

			// let's get that speed
			OutputParams op = new OutputParams(null);
			op.log = true;
			op.maxBufferSize = 1;
			SystemUtils sysUtil = PMS.get().getRegistry();
			final ProcessWrapperImpl pw = new ProcessWrapperImpl(sysUtil.getPingCommand(addr.getHostAddress(), 3, 64000), op,
					true, false);
			Runnable r = new Runnable() {
				public void run() {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
					}
					pw.stopProcess();
				}
			};

			Thread failsafe = new Thread(r, "SpeedStats Failsafe");
			failsafe.start();
			pw.runInSameThread();
			List<String> ls = pw.getOtherResults();
			int time = 0;
			int c = 0;

			for (String line : ls) {
				int msPos = line.indexOf("ms");

				if (msPos > -1) {
					String timeString = line.substring(line.lastIndexOf("=", msPos) + 1, msPos).trim();
					try {
						time += Double.parseDouble(timeString);
						c++;
					} catch (NumberFormatException e) {
						// no big deal
						LOGGER.debug("Could not parse time from \"" + timeString + "\"");
					}
				}
			}
			if (c > 0) {
				time = time / c;
			}

			if (time > 0) {
				int speedInMbits = 1024 / time;
				LOGGER.info("Address " + addr + " has an estimated network speed of: " + speedInMbits + " Mb/s");
				synchronized(speedStats) {
					CompletedFuture<Integer> result = new CompletedFuture<Integer>(speedInMbits);
					// update the statistics with a computed future value
					speedStats.put(ip, result);
					speedStats.put(hostname, result);
				}
				return speedInMbits;
			}
			return -1;
		}
	}

	static class CompletedFuture<X> implements Future<X> {
		X value;
		
		public CompletedFuture(X value) {
			this.value = value;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public X get() throws InterruptedException, ExecutionException {
			return value;
		}

		@Override
		public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return value;
		}
	}
}
