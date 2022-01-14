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
package net.pms.network.mediaserver.jupnp;

import org.jupnp.UpnpServiceImpl;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.jupnp.transport.Router;
import org.jupnp.util.SpecificationViolationReporter;

/**
 * Custom Upnp Service. Configuration : set to
 * ServerlessUpnpServiceConfiguration ->	Apache HttpClient as streamclient
 * (should be able to change to HttpClient from Java 11+). We use it in
 * dependency for other things by the way. ->	Fake the http streamserver, as
 * it's not needed with current UMS config.
 *
 * Router : set to Custom router (drop IncomingDatagramMessage from server) due
 * to our current config
 */
public class CustomUpnpServiceImpl extends UpnpServiceImpl {

	public CustomUpnpServiceImpl() {
		//disable reports violations again UPnP specification (we don't need to see other upnp device violation).
		SpecificationViolationReporter.disableReporting();
		this.configuration = new CustomUpnpServiceConfiguration();
	}

	@Override
	protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
		return new CustomRouterImpl(getConfiguration(), protocolFactory);
	}

}
