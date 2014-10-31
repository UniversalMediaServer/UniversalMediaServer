package net.pms.util;

public class FileWatcher {
	public static interface Listener {
		public void notify(String filename, String event, FileWatcher.Watch watch, boolean isDir);
	}

	public static class Watch {
		public String fspec;
		public int flag;

		public Watch(String fspec, Listener listener) {}
		public Watch(String fspec, Listener listener, Object item) {}
		public Watch(String fspec, Listener listener, int flag) {}
		public Watch(String fspec, Listener listener, Object item, int flag) {}

		public Object getItem() {
			return null;
		}
	}

	public static void add(Watch w) {}

	public static boolean remove(Watch w) {
		return true;
	}
}
