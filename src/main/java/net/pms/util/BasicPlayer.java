package net.pms.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import javax.swing.DefaultComboBoxModel;
import net.pms.configuration.DeviceConfiguration;

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

	public void refresh();

	public void start();

	public void reset();

	public void close();

	// An empty implementation with some basic funtionalities defined
	public static class Minimal implements BasicPlayer {

		public DeviceConfiguration renderer;
		protected State state;
		protected LinkedHashSet<ActionListener> listeners;

		public Minimal(DeviceConfiguration renderer) {
			this.renderer = renderer;
			state = new State();
			listeners = new LinkedHashSet();
			if (renderer.gui != null) {
				connect(renderer.gui);
			}
			reset();
			refresh();
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
			refresh();
		}

		@Override
		public void connect(ActionListener listener) {
			if (listener != null) {
				listeners.add(listener);
			}
		}

		@Override
		public void disconnect(ActionListener listener) {
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				close();
			}
		}

		@Override
		public void refresh() {
			for (ActionListener l : listeners) {
				l.actionPerformed(new ActionEvent(this, 0, null));
			}
		}

		@Override
		public BasicPlayer.State getState() {
			return state;
		}

		@Override
		public void close() {
			listeners.clear();
			renderer.setPlayer(null);
		}

		@Override
		public void setBuffer(long mb) {
			state.buffer = mb;
			refresh();
		}

		@Override
		public void setURI(String uri, String metadata) {
		}

		@Override
		public void pressPlay(String uri, String metadata) {
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
}
