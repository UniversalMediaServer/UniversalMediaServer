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
package net.pms.network.mediaserver.jupnp.model.meta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import org.jupnp.model.profile.RemoteClientInfo;

public class UmsRemoteClientInfo extends RemoteClientInfo {

	public final Renderer renderer;

	public UmsRemoteClientInfo(RemoteClientInfo info) {
		super(info.getConnection(), info.getRequestHeaders());
		renderer = toRenderer(info);
	}

	public static Renderer toRenderer(RemoteClientInfo info) {
		Map<String, String> headers = new HashMap<>();
		for (Entry<String, List<String>> header : info.getRequestHeaders().entrySet()) {
			headers.put(header.getKey(), header.getValue().get(0));
		}
		return ConnectedRenderers.getRenderer(info.getRemoteAddress(), info.getRequestUserAgent(), headers.entrySet());
	}

}
