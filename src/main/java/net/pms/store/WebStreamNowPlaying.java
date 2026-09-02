package net.pms.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;

/**
 * Holds what each continuous stream is currently playing.
 */
public final class WebStreamNowPlaying {

	private static final Map<String, NowPlayingInfo> CURRENT = new ConcurrentHashMap<>();

	/**
	 * Notified with (resourceId, info) on every change, so the UPnP service can push a GENA event
	 * instead of making control points poll. The info is NULL when the stream stopped.
	 */
	private static final List<BiConsumer<String, NowPlayingInfo>> LISTENERS = new CopyOnWriteArrayList<>();

	private WebStreamNowPlaying() {
		throw new UnsupportedOperationException("This class is not meant to be instantiated.");
	}

	public static void addListener(BiConsumer<String, NowPlayingInfo> listener) {
		if (listener != null && !LISTENERS.contains(listener)) {
			LISTENERS.add(listener);
		}
	}

	public static void removeListener(BiConsumer<String, NowPlayingInfo> listener) {
		LISTENERS.remove(listener);
	}

	public static void put(String resourceId, NowPlayingInfo info) {
		if (StringUtils.isBlank(resourceId)) {
			return;
		}
		NowPlayingInfo previous = info == null ? CURRENT.remove(resourceId) : CURRENT.put(resourceId, info);
		if (previous == null ? info != null : !previous.equals(info)) {
			notifyListeners(resourceId, info);
		}
	}

	public static NowPlayingInfo get(String resourceId) {
		return StringUtils.isBlank(resourceId) ? null : CURRENT.get(resourceId);
	}

	public static void remove(String resourceId) {
		if (StringUtils.isNotBlank(resourceId) && CURRENT.remove(resourceId) != null) {
			notifyListeners(resourceId, null);
		}
	}

	private static void notifyListeners(String resourceId, NowPlayingInfo info) {
		for (BiConsumer<String, NowPlayingInfo> listener : LISTENERS) {
			listener.accept(resourceId, info);
		}
	}
}
