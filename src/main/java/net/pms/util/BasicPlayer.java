package net.pms.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.virtual.VirtualVideoAction;
import static net.pms.network.UPNPHelper.unescape;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface BasicPlayer extends ActionListener {
	public class State {
		public int playback;
		public boolean mute;
		public int volume;
		public String position, duration;
		public String name, uri, metadata;
		public long buffer;
	}

	final static int STOPPED = 0;
	final static int PLAYING = 1;
	final static int PAUSED = 2;
	final static int PLAYCONTROL = 1;
	final static int VOLUMECONTROL = 2;

	public void setURI(String uri, String metadata);

	public void pressPlay(String uri, String metadata);

	public void pressStop();

	public void play();

	public void pause();

	public void stop();

	public void next();

	public void prev();

	public void forward();

	public void rewind();

	public void mute();

	public void setVolume(int volume);

	public void add(int index, String uri, String name, String metadata, boolean select);

	public void remove(String uri);

	public void setBuffer(long mb);

	public State getState();

	public int getControls();

	public DefaultComboBoxModel getPlaylist();

	public void connect(ActionListener listener);

	public void disconnect(ActionListener listener);

	public void alert();

	public void start();

	public void reset();

	public void close();

	// An empty implementation with some basic funtionalities defined

	public static class Minimal implements BasicPlayer {

		public DeviceConfiguration renderer;
		protected State state;
		protected final ReentrantReadWriteLock listenersLock = new ReentrantReadWriteLock();
		protected final LinkedHashSet<ActionListener> listeners = new LinkedHashSet<>();

		public Minimal(DeviceConfiguration renderer) {
			this.renderer = renderer;
			state = new State();
			if (renderer.gui != null) {
				connect(renderer.gui);
			}
			reset();
		}

		@Override
		public void start() {
		}

		@Override
		public void reset() {
			state.playback = STOPPED;
			state.position = "";
			state.duration = "";
			state.name = " ";
			state.buffer = 0;
			alert();
		}

		@Override
		public void connect(ActionListener listener) {
			if (listener != null) {
				listenersLock.writeLock().lock();
				try {
					listeners.add(listener);
				} finally {
					listenersLock.writeLock().unlock();
				}
			}
		}

		@Override
		public void disconnect(ActionListener listener) {
			listenersLock.writeLock().lock();
			try {
				listeners.remove(listener);
				if (listeners.isEmpty()) {
					close();
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}

		@Override
		public void alert() {
			listenersLock.readLock().lock();
			try {
				for (ActionListener l : listeners) {
					l.actionPerformed(new ActionEvent(this, 0, null));
				}
			} finally {
				listenersLock.readLock().unlock();
			}
		}

		@Override
		public BasicPlayer.State getState() {
			return state;
		}

		@Override
		public void close() {
			listenersLock.writeLock().lock();
			try {
				listeners.clear();
			} finally {
				listenersLock.writeLock().unlock();
			}

			renderer.setPlayer(null);
		}

		@Override
		public void setBuffer(long mb) {
			state.buffer = mb;
			alert();
		}

		@Override
		public void setURI(String uri, String metadata) {
		}

		@Override
		public void pressPlay(String uri, String metadata) {
		}

		@Override
		public void pressStop() {
		}

		@Override
		public void play() {
		}

		@Override
		public void pause() {
		}

		@Override
		public void stop() {
		}

		@Override
		public void next() {
		}

		@Override
		public void prev() {
		}

		@Override
		public void forward() {
		}

		@Override
		public void rewind() {
		}

		@Override
		public void mute() {
		}

		@Override
		public void setVolume(int volume) {
		}

		@Override
		public void add(int index, String uri, String name, String metadata, boolean select) {
		}

		@Override
		public void remove(String uri) {
		}

		@Override
		public int getControls() {
			return 0;
		}

		@Override
		public DefaultComboBoxModel getPlaylist() {
			return null;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
		}
	}

	// An abstract implementation with all internal playback/playlist logic included.
	// Ideally the entire state-machine resides here and subclasses just implement the
	// details of communicating with the target device.

	public static abstract class Logical extends Minimal {
		public Playlist playlist;
		protected boolean autoContinue, addAllSiblings, forceStop;
		protected int lastPlayback;
		protected int maxVol;

		public Logical(DeviceConfiguration renderer) {
			super(renderer);
			playlist = new Playlist(this);
			lastPlayback = STOPPED;
			maxVol = renderer.getMaxVolume();
			autoContinue = renderer.isAutoContinue();
			addAllSiblings = renderer.isAutoAddAll();
			forceStop = false;
			alert();
			initAutoPlay(this);
		}

		@Override
		public abstract void setURI(String uri, String metadata);

		public Playlist.Item resolveURI(String uri, String metadata) {
			if (uri != null) {
				Playlist.Item item;
				if (metadata != null && metadata.startsWith("<DIDL")) {
					// If it looks real assume it's valid
					return new Playlist.Item(uri, null, metadata);
				} else if ((item = playlist.get(uri)) != null) {
					// We've played it before
					return item;
				} else {
					// It's new to us, find or create the resource as required.
					// Note: here metadata (if any) is actually the resource name
					DLNAResource resource = DLNAResource.getValidResource(uri, metadata, renderer);
					if (resource != null) {
						return new Playlist.Item(resource.getURL("", true), resource.getDisplayName(renderer), resource.getDidlString(renderer));
					}
				}
			}
			return null;
		}

		@Override
		public void pressPlay(String uri, String metadata) {
			forceStop = false;
			if (state.playback == -1) {
				// unknown state, we assume it's stopped
				state.playback = STOPPED;
			}
			if (state.playback == PLAYING) {
				pause();
			} else {
				if (state.playback == STOPPED) {
					Playlist.Item item = playlist.resolve(uri);
					if (item != null) {
						uri = item.uri;
						metadata = item.metadata;
						state.name = item.name;
					}
					if (uri != null && !uri.equals(state.uri)) {
						setURI(uri, metadata);
					}
				}
				play();
			}
		}

		@Override
		public void pressStop() {
			forceStop = true;
			stop();
		}

		@Override
		public void next() {
			step(1);
		}

		@Override
		public void prev() {
			step(-1);
		}

		public void step(int n) {
			if (state.playback != STOPPED) {
				stop();
			}
			state.playback = STOPPED;
			playlist.step(n);
			pressPlay(null, null);
		}

		@Override
		public void alert() {
			boolean stopping = state.playback == STOPPED && lastPlayback != -1 && lastPlayback != STOPPED;
			lastPlayback = state.playback;
			super.alert();
			if (stopping && autoContinue && !forceStop) {
				next();
			}
		}

		@Override
		public int getControls() {
			return renderer.controls;
		}

		@Override
		public DefaultComboBoxModel getPlaylist() {
			return playlist;
		}

		@Override
		public void add(int index, String uri, String name, String metadata, boolean select) {
			if (!StringUtils.isBlank(uri)) {
				if (addAllSiblings && DLNAResource.isResourceUrl(uri)) {
					DLNAResource d = PMS.getGlobalRepo().get(DLNAResource.parseResourceId(uri));
					if (d != null && d.getParent() != null) {
						List<DLNAResource> list = d.getParent().getChildren();
						addAll(index, list, list.indexOf(d));
						return;
					}
				}
				playlist.add(index, uri, name, metadata, select);
			}
		}

		public void addAll(int index, List<DLNAResource> list, int selIndex) {
			for (int i = 0; i < list.size(); i++) {
				DLNAResource r = list.get(i);
				if ((r instanceof VirtualVideoAction) || r.isFolder()) {
					// skip these
					continue;
				}
				playlist.add(index, r.getURL("", true), r.getDisplayName(), r.getDidlString(renderer), i == selIndex);
			}
		}

		@Override
		public void remove(String uri) {
			if (!StringUtils.isBlank(uri)) {
				playlist.remove(uri);
			}
		}

		public void clear() {
			playlist.removeAllElements();
		}

		private static void initAutoPlay(final Logical player) {
			String auto = player.renderer.getAutoPlay();
			if (StringUtils.isEmpty(auto)) {
				return;
			}
			String[] strs = auto.split(" ");
			for (String s : strs) {
				String[] tmp = s.split(":", 2);
				if (tmp.length != 2) {
					continue;
				}
				if (!player.renderer.getConfName().equalsIgnoreCase(tmp[0])) {
					continue;
				}
				final String folder = tmp[1];
				Runnable r = new Runnable() {
					@Override
					public void run() {
						while(PMS.get().getServer().getHost() == null) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								return;
							}
						}
						RealFile f = new RealFile(new File(folder));
						f.discoverChildren();
						f.analyzeChildren(-1);
						player.addAll(-1, f.getChildren(), -1);
						// add a short delay here since player.add uses swing.invokelater
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
						player.pressPlay(null, null);
					}
				};
				new Thread(r).start();
			}
		}

		public static class Playlist extends DefaultComboBoxModel {
			private static final long serialVersionUID = 5934677633834195753L;
			private static final Logger LOGGER = LoggerFactory.getLogger(Playlist.class);

			Logical player;

			public Playlist(Logical p) {
				player = p;
			}

			public Item get(String uri) {
				int index = getIndexOf(new Item(uri, null, null));
				if (index > -1) {
					return (Item) getElementAt(index);
				}
				return null;
			}

			public Item resolve(String uri) {
				Item item = null;
				try {
					Object selected = getSelectedItem();
					Item selectedItem = selected instanceof Item ? (Item) selected : null;
					String selectedName = selectedItem != null ? selectedItem.name : null;
					// See if we have a matching item for the "uri", which could be:
					item = (Item) (
						// An alias for the currently selected item
						StringUtils.isBlank(uri) || uri.equals(selectedName) ? selectedItem :
						// An item index, e.g. '$i$4'
						uri.startsWith("$i$") ? getElementAt(Integer.parseInt(uri.substring(3))) :
						// Or an actual uri
						get(uri));
				} catch (Exception e) {
					LOGGER.error("An error occurred while resolving the item for URI \"{}\": {}", uri, e.getMessage());
					LOGGER.trace("", e);
				}
				return (item != null && isValid(item, player.renderer)) ? item : null;
			}

			public static boolean isValid(Item item, DeviceConfiguration renderer) {
				if (DLNAResource.isResourceUrl(item.uri)) {
					// Check existence for resource uris
					if (PMS.getGlobalRepo().exists(DLNAResource.parseResourceId(item.uri))) {
						return true;
					}
					// Repair the item if possible
					DLNAResource d = DLNAResource.getValidResource(item.uri, item.name, renderer);
					if (d != null) {
						item.uri = d.getURL("", true);
						item.metadata = d.getDidlString(renderer);
						return true;
					}
					return false;
				}
				// Assume non-resource uris are valid
				return true;
			}

			public void validate() {
				for (int i = getSize() - 1; i > -1; i--) {
					if (!isValid((Item) getElementAt(i), player.renderer)) {
						removeElementAt(i);
					}
				}
			}

			public void set(String uri, String name, String metadata) {
				add(0, uri, name, metadata, true);
			}

			public void add(final int index, final String uri, final String name, final String metadata, final boolean select) {
				if (!StringUtils.isBlank(uri)) {
					// TODO: check headless mode (should work according to https://java.net/bugzilla/show_bug.cgi?id=2568)
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Item item = resolve(uri);
							if (item == null) {
								item = new Item(uri, name, metadata);
								insertElementAt(item, index > -1 ? index : getSize());
							}
							if (select) {
								setSelectedItem(item);
							}
						}
					});
				}
			}

			public void remove(final String uri) {
				if (!StringUtils.isBlank(uri)) {
					// TODO: check headless mode
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Item item = resolve(uri);
							if (item != null) {
								removeElement(item);
							}
						}
					});
				}
			}

			public void step(int n) {
				if (getSize() > 0) {
					int i = (getIndexOf(getSelectedItem()) + getSize() + n) % getSize();
					setSelectedItem(getElementAt(i));
				}
			}

			@Override
			protected void fireContentsChanged(Object source, int index0, int index1) {
				player.alert();
				super.fireContentsChanged(source, index0, index1);
			}

			@Override
			protected void fireIntervalAdded(Object source, int index0, int index1) {
				player.alert();
				super.fireIntervalAdded(source, index0, index1);
			}

			@Override
			protected void fireIntervalRemoved(Object source, int index0, int index1) {
				player.alert();
				super.fireIntervalRemoved(source, index0, index1);
			}

			public static class Item {
				private static final Logger LOGGER = LoggerFactory.getLogger(Item.class);
				public String name, uri, metadata;
				static final Matcher dctitle = Pattern.compile("<dc:title>(.+)</dc:title>").matcher("");

				public Item(String uri, String name, String metadata) {
					this.uri = uri;
					this.name = name;
					this.metadata = metadata;
				}

				@Override
				public String toString() {
					if (StringUtils.isBlank(name)) {
						try {
							name = (! StringUtils.isEmpty(metadata) && dctitle.reset(unescape(metadata)).find()) ?
								dctitle.group(1) :
								new File(StringUtils.substringBefore(unescape(uri), "?")).getName();
						} catch (UnsupportedEncodingException e) {
							LOGGER.error("URL decoding error ", e);
						}
					}
					return name;
				}

				@Override
				public boolean equals(Object other) {
					return other == null ? false :
						other == this ? true :
						other instanceof Item ? ((Item)other).uri.equals(uri) :
						other.toString().equals(uri);
				}

				@Override
				public int hashCode() {
					return uri.hashCode();
				}
			}
		}
	}
}
