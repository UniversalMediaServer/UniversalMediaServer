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

import java.io.IOException;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

public class SearchAction extends StoreContainer {
	private Search sobj;
	private char ch;

	public SearchAction(Renderer renderer, Search sobj, char ch) {
		this(renderer, sobj, ch, String.valueOf(ch));
	}

	public SearchAction(Renderer renderer, Search sobj, char ch, String name) {
		super(renderer, name, "images/Play1Hot_120.jpg");
		this.sobj = sobj;
		this.ch = ch;
		setName(name);
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		return DLNAThumbnailInputStream.toThumbnailInputStream(HTTPResource.getResourceInputStream("images/Play1Hot_120.jpg"));
	}

	@Override
	public synchronized void resolve() {
		setDiscovered(false);  // we can't clear this enough
	}

	@Override
	public void discoverChildren() {
		sobj.append(ch);
		setDiscovered(false);
	}

	@Override
	public long length() {
		return -1; //DLNAMediaInfo.TRANS_SIZE;
	}

	@Override
	public boolean isValid() {
		return true;
	}
}
