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
import net.pms.util.SimpleThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared DNS and reverse-DNS resolution with timeouts. Runs resolution in a
 * single pool so that slow or failing resolvers do not block request threads.
 * No caching; callers must not rely on hostname↔IP mapping stability.
 * All timeouts and errors are logged with context and stack traces so
 * application frames are visible (e.g. for issue 6047).
 */
public final class DnsResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(DnsResolver.class);

	private static final ExecutorService RESOLVER = Executors.newCachedThreadPool(
			new SimpleThreadFactory("DnsResolver", "DnsResolver pool", Thread.NORM_PRIORITY)
	);

	private DnsResolver() {
	}

	/**
	 * Reverse-DNS: resolve address to hostname with timeout. No caching.
	 *
	 * @param addr address to resolve
	 * @param timeoutMs timeout in milliseconds
	 * @param callContext short description of caller (e.g. "NetworkDeviceFilter.getNameForMatching") for logs
	 * @return hostname in lowercase, or {@code addr.getHostAddress()} on timeout/failure
	 */
	public static String resolveReverse(InetAddress addr, long timeoutMs, String callContext) {
		if (addr == null) {
			return "";
		}
		String ip = addr.getHostAddress();
		Future<String> future = RESOLVER.submit(() -> addr.getHostName().toLowerCase(Locale.ROOT));
		try {
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Reverse-DNS interrupted for {} (context: {})", ip, callContext, e);
			return ip;
		} catch (TimeoutException e) {
			LOGGER.warn("Reverse-DNS timed out after {} ms for {} (context: {})", timeoutMs, ip, callContext, e);
			return ip;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			LOGGER.warn("Reverse-DNS failed for {} (context: {}): {}", ip, callContext, cause != null ? cause.getMessage() : e.getMessage(), e);
			return ip;
		}
	}

	/**
	 * Resolve hostname to a single address with timeout. No caching.
	 *
	 * @param host hostname to resolve
	 * @param timeoutMs timeout in milliseconds
	 * @param callContext short description of caller for logs
	 * @return resolved address or {@code null} on timeout/failure
	 */
	public static InetAddress resolveByName(String host, long timeoutMs, String callContext) {
		if (host == null || host.isEmpty()) {
			return null;
		}
		Future<InetAddress> future = RESOLVER.submit(() -> InetAddress.getByName(host));
		try {
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("DNS resolve interrupted for host \"{}\" (context: {})", host, callContext, e);
			return null;
		} catch (TimeoutException e) {
			LOGGER.warn("DNS resolve timed out after {} ms for host \"{}\" (context: {})", timeoutMs, host, callContext, e);
			return null;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			LOGGER.warn("DNS resolve failed for host \"{}\" (context: {}): {}", host, callContext, cause != null ? cause.getMessage() : e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Resolve hostname to all addresses with timeout. No caching.
	 *
	 * @param host hostname to resolve
	 * @param timeoutMs timeout in milliseconds
	 * @param callContext short description of caller for logs
	 * @return resolved addresses, or empty array on timeout/failure (never null)
	 */
	public static InetAddress[] resolveAllByName(String host, long timeoutMs, String callContext) {
		if (host == null || host.isEmpty()) {
			return new InetAddress[0];
		}
		Future<InetAddress[]> future = RESOLVER.submit(() -> InetAddress.getAllByName(host));
		try {
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("DNS resolveAll interrupted for host \"{}\" (context: {})", host, callContext, e);
			return new InetAddress[0];
		} catch (TimeoutException e) {
			LOGGER.warn("DNS resolveAll timed out after {} ms for host \"{}\" (context: {})", timeoutMs, host, callContext, e);
			return new InetAddress[0];
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			LOGGER.warn("DNS resolveAll failed for host \"{}\" (context: {}): {}", host, callContext, cause != null ? cause.getMessage() : e.getMessage(), e);
			return new InetAddress[0];
		}
	}
}
