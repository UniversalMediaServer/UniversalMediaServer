package net.pms.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;

/**
 * Holds the title a web radio is currently announcing via ICY, so a control point can ask for the
 * live title even when the renderer itself never requested in-band metadata.
 */
public final class WebStreamNowPlaying {

	private static final Map<String, String> CURRENT_TITLES = new ConcurrentHashMap<>();

	// We do GENA push
	private static final List<BiConsumer<String, String>> LISTENERS = new CopyOnWriteArrayList<>();

	private WebStreamNowPlaying() {
		throw new UnsupportedOperationException("This class is not meant to be instantiated.");
	}

	public static void addListener(BiConsumer<String, String> listener) {
		if (listener != null && !LISTENERS.contains(listener)) {
			LISTENERS.add(listener);
		}
	}

	public static void removeListener(BiConsumer<String, String> listener) {
		LISTENERS.remove(listener);
	}

	public static void put(String resourceId, String streamTitle) {
		if (StringUtils.isBlank(resourceId)) {
			return;
		}
		String previous;
		if (StringUtils.isBlank(streamTitle)) {
			previous = CURRENT_TITLES.remove(resourceId);
		} else {
			previous = CURRENT_TITLES.put(resourceId, streamTitle);
		}
		if (!StringUtils.equals(previous, streamTitle)) {
			notifyListeners(resourceId, streamTitle);
		}
	}

	public static String get(String resourceId) {
		return StringUtils.isBlank(resourceId) ? null : CURRENT_TITLES.get(resourceId);
	}

	public static void remove(String resourceId) {
		if (StringUtils.isNotBlank(resourceId) && CURRENT_TITLES.remove(resourceId) != null) {
			notifyListeners(resourceId, null);
		}
	}

	private static void notifyListeners(String resourceId, String streamTitle) {
		for (BiConsumer<String, String> listener : LISTENERS) {
			listener.accept(resourceId, streamTitle);
		}
	}
}
