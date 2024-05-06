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
package net.pms.formats;

import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.FileUtil;

public class WEB extends Format {
	protected String url = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.WEB;
	}

	/**
	 * Matches the supplied filename against this format,
	 * returning true if the filename is a valid URI
	 * and false otherwise. Protocol-specific matches
	 * are handled by {@link net.pms.encoders.Player#isCompatible(StoreResource)}.
	 *
	 * @param filename the filename to match against
	 * @return <code>true</code> if the filename matches, <code>false</code> otherwise.
	 */
	@Override
	public boolean match(String filename) {
		String protocol = FileUtil.getProtocol(filename);

		if (protocol == null) {
			return false;
		}
		url = filename;
		setMatchedExtension(protocol);
		return true;
	}

	@Override
	public boolean transcodable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(StoreItem resource, Renderer renderer) {
		return type == IMAGE;
	}

	@Override
	public String mimeType() {
		if (url != null) {
			Format f = FormatFactory.getAssociatedFormat("." + FileUtil.getUrlExtension(url));
			if (f != null) {
				return f.mimeType();
			}
		}
		return super.mimeType();
	}
}
