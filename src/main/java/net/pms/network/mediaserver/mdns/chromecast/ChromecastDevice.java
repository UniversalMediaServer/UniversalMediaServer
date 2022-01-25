/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.mediaserver.mdns.chromecast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.network.mediaserver.UPNPControl;
import net.pms.util.BasicPlayer;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.litvak.chromecast.api.v2.ChromeCast;

public final class ChromecastDevice extends DeviceConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChromecastDevice.class);
	public final ChromeCast chromeCast;

	public ChromecastDevice(
		ChromeCast chromeCast,
		RendererConfiguration renderer,
		InetAddress inetAddress
	) throws ConfigurationException, InterruptedException {
		super(renderer, inetAddress);
		this.chromeCast = chromeCast;
		uuid = chromeCast.getAddress();
		controls = UPNPControl.ANY;
		active = true;
		associateIP(inetAddress);
	}

	@Override
	public String getRendererName() {
		try {
			if (StringUtils.isNotEmpty(chromeCast.getName())) {
				return chromeCast.getName();
			}
		} catch (Exception e) {
			LOGGER.debug("Failed to find name for Chromecast \"{}\"", chromeCast);
			LOGGER.trace("", e);
		}
		return getConfName();
	}

	@Override
	public BasicPlayer getPlayer() {
		if (player == null) {
			player = new ChromecastPlayer(this, chromeCast);
			((ChromecastPlayer) player).startPoll();
		}
		return player;
	}

	@Override
	public InetAddress getAddress() {
		try {
			return InetAddress.getByName(chromeCast.getAddress());
		} catch (UnknownHostException e) {
			LOGGER.debug("Failed to find address for Chromecast \"{}\"", chromeCast);
			LOGGER.trace("", e);
			return null;
		}
	}
}
