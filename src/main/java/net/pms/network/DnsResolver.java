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
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.pms.PMS;
import net.pms.util.SimpleThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared DNS and reverse-DNS resolution with timeouts. Runs resolution in a
 * single pool so that slow or failing resolvers do not block request threads.
 * No caching; callers must not rely on hostname↔IP mapping stability.
 * All timeouts and errors are logged with stack traces so application frames are visible (e.g. for issue 6047).
 * Timeouts are only applied when {@code dns_resolution_timeout_enabled} is true (default false), and taken from {@code dns_resolution_timeout_ms} (default 5000).
 */
public final class DnsResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(DnsResolver.class);
	private static final int DEFAULT_DNS_TIMEOUT_MS = 5000;

	private static final ExecutorService RESOLVER = Executors.newCachedThreadPool(
			new SimpleThreadFactory("DnsResolver", "DnsResolver pool", Thread.NORM_PRIORITY)
	);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("DnsResolver Executor Shutdown Hook") {
			@Override
			public void run() {
				RESOLVER.shutdownNow();
			}
		});
	}

	private DnsResolver() {
		throw new UnsupportedOperationException("This class is not meant to be instantiated.");
	}

	private static boolean isTimeoutEnabled() {
		try {
			return PMS.getConfiguration() != null && PMS.getConfiguration().isDnsResolutionTimeoutEnabled();
		} catch (Exception e) {
			LOGGER.error("Unable to read DNS resolution timeout enabled setting. DNS timeouts will be disabled.", e);
			return false;
		}
	}

	/** When timeout is enabled, returns config value (ms) from {@code dns_resolution_timeout_ms}; otherwise {@link Long#MAX_VALUE}.
	 * Clamped to at least 1 ms when enabled. */
	private static long getTimeoutMs() {
		if (!isTimeoutEnabled()) {
			return Long.MAX_VALUE;
		}
		try {
			int ms = PMS.getConfiguration() != null ? PMS.getConfiguration().getDnsResolutionTimeoutMs() : DEFAULT_DNS_TIMEOUT_MS;
			return Math.max(1, ms);
		} catch (Exception e) {
			LOGGER.error("Unable to read DNS resolution timeout setting. Using default {} ms.", DEFAULT_DNS_TIMEOUT_MS, e);
			return Math.max(1, DEFAULT_DNS_TIMEOUT_MS);
		}
	}

	/**
	 * Reverse-DNS: resolve address to hostname.
	 *
	 * @param addr address to resolve
	 * @return hostname in lowercase, or {@code addr.getHostAddress()} on timeout/failure
	 */
	public static String resolveReverse(InetAddress addr) {
		if (addr == null) {
			return "";
		}
		long timeoutMs = getTimeoutMs();
		String ip = addr.getHostAddress();
		Future<String> future = RESOLVER.submit(() -> addr.getHostName().toLowerCase(Locale.ROOT));
		try {
			if (isTimeoutEnabled()) {
				return future.get(timeoutMs, TimeUnit.MILLISECONDS);
			}
			return future.get();
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			LOGGER.warn("Reverse-DNS interrupted for {}", ip, e);
			return ip;
		} catch (TimeoutException e) {
			future.cancel(true);
			LOGGER.warn("Reverse-DNS timed out after {} ms for {}", timeoutMs, ip, e);
			return ip;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			LOGGER.warn("Reverse-DNS failed for {}: {}", ip, cause != null ? cause.getMessage() : e.getMessage(), e);
			return ip;
		}
	}

	/**
	 * Resolve hostname to a single address.
	 *
	 * @param host hostname to resolve
	 * @return resolved address or {@code null} on timeout/failure
	 */
	public static InetAddress resolveByName(String host) {
		if (host == null || host.isEmpty()) {
			return null;
		}
		long timeoutMs = getTimeoutMs();
		Future<InetAddress> future = RESOLVER.submit(() -> InetAddress.getByName(host));
		try {
			if (isTimeoutEnabled()) {
				return future.get(timeoutMs, TimeUnit.MILLISECONDS);
			}
			return future.get();
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			LOGGER.warn("DNS resolve interrupted for host \"{}\"", host, e);
			return null;
		} catch (TimeoutException e) {
			future.cancel(true);
			LOGGER.warn("DNS resolve timed out after {} ms for host \"{}\"", timeoutMs, host, e);
			return null;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			LOGGER.warn("DNS resolve failed for host \"{}\": {}", host, cause != null ? cause.getMessage() : e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Resolve hostname to all addresses.
	 *
	 * @param host hostname to resolve
	 * @return resolved addresses, or empty array on timeout/failure (never null)
	 */
	public static InetAddress[] resolveAllByName(String host) {
		if (host == null || host.isEmpty()) {
			return new InetAddress[0];
		}
		long timeoutMs = getTimeoutMs();
		Future<InetAddress[]> future = RESOLVER.submit(() -> InetAddress.getAllByName(host));
		try {
			if (isTimeoutEnabled()) {
				return future.get(timeoutMs, TimeUnit.MILLISECONDS);
			}
			return future.get();
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			LOGGER.warn("DNS resolveAll interrupted for host \"{}\"", host, e);
			return new InetAddress[0];
		} catch (TimeoutException e) {
			future.cancel(true);
			LOGGER.warn("DNS resolveAll timed out after {} ms for host \"{}\"", timeoutMs, host, e);
			return new InetAddress[0];
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			LOGGER.warn("DNS resolveAll failed for host \"{}\": {}", host, cause != null ? cause.getMessage() : e.getMessage(), e);
			return new InetAddress[0];
		}
	}
}
