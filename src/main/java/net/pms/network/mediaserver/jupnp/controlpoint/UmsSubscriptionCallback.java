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
package net.pms.network.mediaserver.jupnp.controlpoint;

import net.pms.renderers.JUPnPDeviceHelper;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmsSubscriptionCallback extends SubscriptionCallback {
	private static final Logger LOGGER = LoggerFactory.getLogger(UmsSubscriptionCallback.class);

	private final String uuid;

	public UmsSubscriptionCallback(Service s) {
		super(s);
		uuid = s.getDevice().getIdentity().getUdn().getIdentifierString();
	}

	@Override
	public void eventReceived(GENASubscription subscription) {
		JUPnPDeviceHelper.markRenderer(uuid, JUPnPDeviceHelper.ACTIVE, true);
		if (subscription.getCurrentValues().containsKey("LastChange")) {
			JUPnPDeviceHelper.xml2d(uuid, subscription.getCurrentValues().get("LastChange").toString(), null);
		}
	}

	@Override
	public void established(GENASubscription sub) {
		LOGGER.debug("Subscription established: {} on {}",
				sub.getService().getServiceId().getId(),
				JUPnPDeviceHelper.getFriendlyName(uuid)
		);
	}

	@Override
	public void failed(GENASubscription sub, UpnpResponse response, Exception ex, String defaultMsg) {
		LOGGER.debug("Subscription failed: {} on {}: {}",
				sub.getService().getServiceId().getId(),
				JUPnPDeviceHelper.getFriendlyName(uuid),
				defaultMsg.split(": ", 2)[1]
		);
	}

	@Override
	public void failed(GENASubscription sub, UpnpResponse response, Exception ex) {
		LOGGER.debug("Subscription failed: {} on {}: {}",
				sub.getService().getServiceId().getId(),
				JUPnPDeviceHelper.getFriendlyName(uuid),
				SubscriptionCallback.createDefaultFailureMessage(response, ex).split(": ", 2)[1]
		);
	}

	@Override
	public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
		// Reason should be null, or it didn't end regularly
		if (reason != null) {
			LOGGER.debug("Subscription cancelled: {} on {}: {}",
					sub.getService().getServiceId().getId(),
					uuid,
					reason
			);
		}
		JUPnPDeviceHelper.markRenderer(uuid, JUPnPDeviceHelper.RENEW, true);
	}

	@Override
	public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
		LOGGER.debug("Missed events: {} for subscription {} on {}",
				numberOfMissedEvents,
				sub.getService().getServiceId().getId(),
				JUPnPDeviceHelper.getFriendlyName(uuid)
		);
	}

}
