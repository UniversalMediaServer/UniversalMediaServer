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

import java.io.File;
import java.util.List;
import net.pms.dlna.DidlHelper;
import net.pms.network.mediaserver.MediaServer;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.container.RealFolder;
import net.pms.store.item.VirtualVideoAction;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * An abstract implementation with all internal playback/playlist logic
 * included. Ideally the entire state-machine resides here and subclasses just
 * implement the details of communicating with the target device.
 */
public abstract class LogicalPlayer extends MinimalPlayer {

	protected Playlist playlist;
	protected boolean autoContinue;
	protected boolean addAllSiblings;
	protected boolean forceStop;
	protected int lastPlayback;
	protected int maxVol;

	protected LogicalPlayer(Renderer renderer) {
		super(renderer);
		playlist = new Playlist(this);
		lastPlayback = PlayerState.STOPPED;
		maxVol = renderer.getMaxVolume();
		autoContinue = renderer.getUmsConfiguration().isAutoContinue();
		addAllSiblings = renderer.getUmsConfiguration().isAutoAddAll();
		forceStop = false;
		alert();
		initAutoPlay(this);
	}

	@Override
	public abstract void setURI(String uri, String metadata);

	public PlaylistItem resolveURI(String uri, String metadata) {
		if (uri != null) {
			PlaylistItem item = playlist.get(uri);
			if (metadata != null && metadata.startsWith("<DIDL")) {
				// If it looks real assume it's valid
				return new PlaylistItem(uri, null, metadata);
			} else if (item != null) {
				// We've played it before
				return item;
			} else {
				// It's new to us, find or create the resource as required.
				// Note: here metadata (if any) is actually the resource name
				StoreResource resource = renderer.getMediaStore().getValidResource(uri, metadata);
				if (resource instanceof StoreItem storeItem) {
					return new PlaylistItem(storeItem.getMediaURL("", true), resource.getDisplayName(), DidlHelper.getDidlString(resource));
				}
			}
		}
		return null;
	}

	@Override
	public void pressPlay(String uri, String metadata) {
		forceStop = false;
		if (state.isUnknown()) {
			// unknown state, we assume it's stopped
			state.setPlayback(PlayerState.STOPPED);
		}
		if (state.isPlaying()) {
			pause();
		} else {
			if (state.isStopped()) {
				PlaylistItem item = playlist.resolve(uri);
				if (item != null) {
					uri = item.getUri();
					metadata = item.getMetadata();
					state.setName(item.getName());
				}
				if (uri != null && !uri.equals(state.getUri())) {
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
		if (!state.isStopped()) {
			stop();
		}
		state.setPlayback(PlayerState.STOPPED);
		playlist.step(n);
		pressPlay(null, null);
	}

	@Override
	public void alert() {
		boolean stopping = state.isStopped() && lastPlayback != -1 && lastPlayback != PlayerState.STOPPED;
		lastPlayback = state.getPlayback();
		super.alert();
		if (stopping && autoContinue && !forceStop) {
			next();
		}
	}

	@Override
	public int getControls() {
		return renderer.getControls();
	}

	@Override
	public Playlist getPlaylist() {
		return playlist;
	}

	@Override
	public void add(int index, String uri, String name, String metadata, boolean select) {
		if (!StringUtils.isBlank(uri)) {
			if (addAllSiblings && StoreResource.isResourceUrl(uri)) {
				StoreResource d = renderer.getMediaStore().getResource(StoreResource.parseResourceId(uri));
				if (d != null && d.getParent() != null) {
					List<StoreResource> list = d.getParent().getChildren();
					addAll(index, list, list.indexOf(d));
					return;
				}
			}
			playlist.add(index, uri, name, metadata, select);
		}
	}

	public void addAll(int index, List<StoreResource> list, int selIndex) {
		for (int i = 0; i < list.size(); i++) {
			StoreResource r = list.get(i);
			if (r instanceof StoreItem item && !(r instanceof VirtualVideoAction) && !item.isFolder()) {
				playlist.add(index, item.getMediaURL("", true), r.getDisplayName(), DidlHelper.getDidlString(r), i == selIndex);
			}
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

	private static void initAutoPlay(final LogicalPlayer player) {
		String auto = player.renderer.getUmsConfiguration().getAutoPlay();
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
			Runnable r = () -> {
				while (!MediaServer.isStarted()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				RealFolder f = new RealFolder(player.getRenderer(), new File(folder));
				f.discoverChildren();
				player.addAll(-1, f.getChildren(), -1);
				// add a short delay here since player.add uses
				// swing.invokelater
				UMSUtils.sleep(1000);
				player.pressPlay(null, null);
			};
			new Thread(r).start();
		}
	}

}
