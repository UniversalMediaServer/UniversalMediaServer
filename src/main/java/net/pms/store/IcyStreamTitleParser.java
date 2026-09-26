package net.pms.store;

import java.net.URI;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits the single line an ICY station announces into artist and title. Most stations send
 * "Artist - Title", some send "Track - Station/Show" instead. Which one a station does is guessed
 * from the tail of the line and can be overridden per station.
 */
public class IcyStreamTitleParser {

	public enum Order {
		AUTO, ARTIST_FIRST, TITLE_FIRST;

		/**
		 * @return the order of a directive value; anything unknown is AUTO.
		 */
		public static Order of(String value) {
			String wanted = StringUtils.trimToEmpty(value).toLowerCase().replace('-', '_');
			for (Order order : values()) {
				if (order.name().toLowerCase().equals(wanted)) {
					return order;
				}
			}
			if (StringUtils.isNotBlank(value)) {
				LOGGER.debug("ignoring the unknown ICY title order \"{}\"", value);
			}
			return AUTO;
		}

		public String directiveValue() {
			return name().toLowerCase().replace('_', '-');
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(IcyStreamTitleParser.class.getName());

	// " - ", an en or em dash included.
	private static final Pattern SEPARATOR = Pattern.compile("\\s+[-\u2013\u2014]\\s+");

	// tries to match a domain (for the heuristic)
	private static final Pattern DOMAIN = Pattern.compile("(?i)\\b[\\w-]{2,}\\.[a-z]{2,6}\\b");

	// Host labels that say nothing about the station.
	private static final Set<String> GENERIC_HOST_LABELS = Set.of("www", "listen", "stream", "streaming", "radio", "live", "play", "audio", "media", "server", "cast", "icecast", "shoutcast");

	private static final int MIN_KEY_LENGTH = 4;

	private final Supplier<Order> order;
	private final String stationKey;
	private final String hostKey;

	private String lastLine;
	private Order lastOrder;
	private NowPlayingInfo lastInfo;

	public IcyStreamTitleParser(Supplier<Order> order, String stationName, String streamUrl) {
		this.order = order;
		this.stationKey = normalize(stationName);
		this.hostKey = hostKey(streamUrl);
	}

	/**
	 * @return NULL for a blank line
	 */
	public synchronized NowPlayingInfo parse(String streamTitle) {
		if (StringUtils.isBlank(streamTitle)) {
			return null;
		}
		Order current = nullSafeOrder();
		if (streamTitle.equals(lastLine) && current == lastOrder) {
			return lastInfo;
		}
		lastLine = streamTitle;
		lastOrder = current;
		lastInfo = split(streamTitle, current);
		return lastInfo;
	}

	private Order nullSafeOrder() {
		Order current = order == null ? null : order.get();
		return current == null ? Order.AUTO : current;
	}

	private NowPlayingInfo split(String line, Order current) {
		Matcher matcher = SEPARATOR.matcher(line);
		int firstStart = -1;
		int firstEnd = -1;
		int lastStart = -1;
		int lastEnd = -1;
		while (matcher.find()) {
			if (firstStart == -1) {
				firstStart = matcher.start();
				firstEnd = matcher.end();
			}
			lastStart = matcher.start();
			lastEnd = matcher.end();
		}
		if (firstStart == -1) {
			LOGGER.debug("ICY: \"{}\" carries no separator, keeping it as one line", line);
			return NowPlayingInfo.ofStreamTitle(line);
		}
		Order effective = current == Order.AUTO ? detect(line.substring(lastEnd)) : current;

		boolean titleFirst = effective == Order.TITLE_FIRST;
		String left = line.substring(0, titleFirst ? lastStart : firstStart).trim();
		String right = line.substring(titleFirst ? lastEnd : firstEnd).trim();
		if (left.isEmpty() || right.isEmpty()) {
			return NowPlayingInfo.ofStreamTitle(line);
		}
		String artist = titleFirst ? right : left;
		String title = titleFirst ? left : right;
		LOGGER.debug("ICY: \"{}\" split as {} into artist=\"{}\", title=\"{}\"", line,
			current == Order.AUTO ? effective + " (detected)" : effective.toString(), artist, title);
		return NowPlayingInfo.ofStreamTitle(line, artist, title);
	}

	private Order detect(String tail) {
		if (DOMAIN.matcher(tail).find()) {
			return Order.TITLE_FIRST;
		}
		String key = normalize(tail);
		if (key.length() < MIN_KEY_LENGTH) {
			return Order.ARTIST_FIRST;
		}
		if (stationKey.length() >= MIN_KEY_LENGTH && (key.contains(stationKey) || stationKey.contains(key))) {
			return Order.TITLE_FIRST;
		}
		if (hostKey.length() >= MIN_KEY_LENGTH && key.contains(hostKey)) {
			return Order.TITLE_FIRST;
		}
		return Order.ARTIST_FIRST;
	}

	/**
	 * @return the name-carrying label of the host
	 */
	private static String hostKey(String streamUrl) {
		if (StringUtils.isBlank(streamUrl)) {
			return "";
		}
		String host;
		try {
			host = URI.create(streamUrl).getHost();
		} catch (IllegalArgumentException e) {
			return "";
		}
		if (StringUtils.isBlank(host)) {
			return "";
		}
		String[] labels = host.split("\\.");
		for (int i = labels.length - 2; i >= 0; i--) {
			String label = normalize(labels[i]);
			if (label.length() >= MIN_KEY_LENGTH && !GENERIC_HOST_LABELS.contains(label)) {
				return label;
			}
		}
		return "";
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}
