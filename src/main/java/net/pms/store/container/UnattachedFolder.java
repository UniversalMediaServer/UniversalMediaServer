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
package net.pms.store.container;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.item.FeedItem;
import net.pms.store.item.RealFile;
import net.pms.store.item.WebAudioStream;
import net.pms.store.item.WebVideoStream;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A general-purpose free-floating folder
 */
public class UnattachedFolder extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UnattachedFolder.class);
	private static final List<String> IDS = new ArrayList<>();

	public UnattachedFolder(Renderer renderer, String name) {
		super(renderer, name, null);
		setId(name);
	}

	private int getIdOf(String uri) {
		synchronized (IDS) {
			if (!IDS.contains(uri)) {
				IDS.add(uri);
			}
			return IDS.indexOf(uri);
		}
	}

	public StoreResource add(String uri, String name) {
		StoreResource d = autoMatch(uri, name);
		if (d != null) {
			// Now add the item and resolve its rendering details
			d.setId(getId() + getIdOf(uri));
			addChild(d);
			d.syncResolve();
		}

		return d;
	}

	public int getIndex(String objectId) {
		int index = indexOf(objectId);
		if (index == -1) {
			index = indexOf(recreate(objectId, null).getResourceId());
		}

		return index;
	}

	public StoreResource get(String objectId) {
		int index = getIndex(objectId);
		return index > -1 ? getChildren().get(index) : null;
	}

	public List<StoreResource> asList(String objectId) {
		int index = getIndex(objectId);
		return index > -1 ? getChildren().subList(index, index + 1) : null;
	}

	// Try to recreate a lost item from a previous session
	// using its objectId's trailing uri, if any
	public StoreResource recreate(String objectId, String name) {
		try {
			return add(StringUtils.substringAfter(objectId, "/"), name);
		} catch (Exception e) {
			return null;
		}
	}

	// Attempts to automatically create the appropriate container for
	// the given uri. Defaults to mpeg video for indeterminate local uris.
	private StoreResource autoMatch(String uri, String name) {
		try {
			uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			LOGGER.error("URL decoding error ", e);
		}

		boolean isweb = uri.matches("\\S+://.+");
		Format format = FormatFactory.getAssociatedFormat(isweb ? "." + FileUtil.getUrlExtension(uri) : uri);
		int type = format == null ? Format.VIDEO : format.getType();
		if (name == null) {
			name = new File(StringUtils.substringBefore(uri, "?")).getName();
		}

		StoreItem resource;
		if (isweb) {
			resource = switch (type) {
				case Format.VIDEO -> new WebVideoStream(renderer, name, uri, null);
				case Format.AUDIO -> new WebAudioStream(renderer, name, uri, null);
				case Format.IMAGE -> new FeedItem(renderer, name, uri, null, null, Format.IMAGE);
				default -> null;
			};
		} else {
			resource = new RealFile(renderer, new File(uri));
		}

		if (resource != null && format == null && !isweb) {
			resource.setFormat(FormatFactory.getAssociatedFormat(".mpg"));
		}

		LOGGER.debug(resource == null ? ("Could not auto-match " + uri) : ("Created auto-matched container: " + resource));
		return resource;
	}

}
