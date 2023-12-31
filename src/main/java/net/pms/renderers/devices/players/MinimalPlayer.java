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
package net.pms.renderers.devices.players;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.renderers.Renderer;

/**
 * An empty implementation with some basic functionalities defined.
 */
public abstract class MinimalPlayer implements BasicPlayer {
	protected Renderer renderer;
	protected PlayerState state;
	protected final ReentrantReadWriteLock listenersLock = new ReentrantReadWriteLock();
	protected final LinkedHashSet<ActionListener> listeners = new LinkedHashSet<>();

	protected MinimalPlayer(Renderer renderer) {
		this.renderer = renderer;
		state = new PlayerState();
		reset();
	}

	@Override
	public void start() {
	}

	@Override
	public final void reset() {
		state.reset();
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
		renderer.refreshPlayerStateGui(state);
	}

	@Override
	public PlayerState getState() {
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
		state.setBuffer(mb);
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
	public Playlist getPlaylist() {
		return null;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
	}

	public Renderer getRenderer() {
		return renderer;
	}
}

