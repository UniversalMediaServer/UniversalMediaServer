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

import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import net.pms.dlna.DidlHelper;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Playlist extends DefaultComboBoxModel<PlaylistItem> {

	private static final long serialVersionUID = 5934677633834195753L;
	private static final Logger LOGGER = LoggerFactory.getLogger(Playlist.class);

	LogicalPlayer player;

	public Playlist(LogicalPlayer p) {
		player = p;
	}

	public PlaylistItem get(String uri) {
		int index = getIndexOf(new PlaylistItem(uri, null, null));
		if (index > -1) {
			return (PlaylistItem) getElementAt(index);
		}
		return null;
	}

	public PlaylistItem resolve(String uri) {
		PlaylistItem item = null;
		try {
			Object selected = getSelectedItem();
			PlaylistItem selectedItem = selected instanceof PlaylistItem ? (PlaylistItem) selected : null;
			String selectedName = selectedItem != null ? selectedItem.getName() : null;
			// See if we have a matching item for the "uri", which could be:
			if (StringUtils.isBlank(uri) || uri.equals(selectedName)) {
				// An alias for the currently selected item
				item = selectedItem;
			} else if (uri.startsWith("$i$")) {
				// An item index, e.g. '$i$4'
				item = (PlaylistItem) getElementAt(Integer.parseInt(uri.substring(3)));
			} else {
				// Or an actual uri
				item = get(uri);
			}
		} catch (NumberFormatException e) {
			LOGGER.error("An error occurred while resolving the item for URI \"{}\": {}", uri, e.getMessage());
			LOGGER.trace("", e);
		}
		return (item != null && isValid(item, player.getRenderer())) ? item : null;
	}

	public void validate() {
		for (int i = getSize() - 1; i > -1; i--) {
			if (!isValid((PlaylistItem) getElementAt(i), player.getRenderer())) {
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
			SwingUtilities.invokeLater(() -> {
				PlaylistItem item = resolve(uri);
				if (item == null) {
					item = new PlaylistItem(uri, name, metadata);
					insertElementAt(item, index > -1 ? index : getSize());
				}
				if (select) {
					setSelectedItem(item);
				}
			});
		}
	}

	public void remove(final String uri) {
		if (!StringUtils.isBlank(uri)) {
			// TODO: check headless mode
			SwingUtilities.invokeLater(() -> {
				PlaylistItem item = resolve(uri);
				if (item != null) {
					removeElement(item);
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

	public static boolean isValid(PlaylistItem item, Renderer renderer) {
		if (StoreResource.isResourceUrl(item.getUri())) {
			// Check existence for resource uris
			if (renderer.getMediaStore().weakResourceExists(StoreResource.parseResourceId(item.getUri()))) {
				return true;
			}
			// Repair the item if possible
			StoreResource resource = renderer.getMediaStore().getValidResource(item.getUri(), item.getName());
			if (resource instanceof StoreItem storeItem) {
				item.setUri(storeItem.getMediaURL("", true));
				item.setMetadata(DidlHelper.getDidlString(resource));
				return true;
			}
			return false;
		}
		// Assume non-resource uris are valid
		return true;
	}

}
