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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item;

import java.net.URI;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A videoBroadcast instance represents a continuous stream from a video
 * broadcast (for example, a convential TV channel or a Webcast).
 *
 * It typically has at least one res property. This class is derived from the
 * videoItem class and inherits the properties defined by that class.
 */
public class VideoBroadcast extends VideoItem {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_VIDEOITEM_VIDEOBROADCAST_TYPE);

	public VideoBroadcast() {
		setUpnpClass(CLASS);
	}

	public VideoBroadcast(Item other) {
		super(other);
	}

	public VideoBroadcast(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public VideoBroadcast(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, resource);
		setUpnpClass(CLASS);
	}

	public URI getIcon() {
		return properties.getValue(UPNP.Icon.class);
	}

	public VideoBroadcast setIcon(URI icon) {
		properties.set(new UPNP.Icon(icon));
		return this;
	}

	public String getRegion() {
		return properties.getValue(UPNP.Region.class);
	}

	public VideoBroadcast setRegion(String region) {
		properties.set(new UPNP.Region(region));
		return this;
	}

	public Integer getChannelNr() {
		return properties.getValue(UPNP.ChannelNr.class);
	}

	public VideoBroadcast setChannelNr(Integer channelNr) {
		properties.set(new UPNP.ChannelNr(channelNr));
		return this;
	}

	/**
	 * The upnp:signalStrength property contains the relative strength of the
	 * signal that is used to retrieve the content for the item.
	 *
	 * A value of 0 indicates “no signal detected”. A value of 100 indicates
	 * “best possible” signal strength.
	 *
	 * @since ContentDirectory v2
	 */
	public Integer getSignalStrength() {
		return properties.getValue(UPNP.SignalStrength.class);
	}

	/**
	 * The upnp:signalStrength property contains the relative strength of the
	 * signal that is used to retrieve the content for the item.
	 *
	 * A value of 0 indicates “no signal detected”. A value of 100 indicates
	 * “best possible” signal strength.
	 *
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast setSignalStrength(Integer signalStrength) {
		properties.set(new UPNP.SignalStrength(signalStrength));
		return this;
	}

	/**
	 * The upnp:signalLocked property indicates whether the signal strength is
	 * sufficiently strong to enable the hardware to lock onto the signal at the
	 * current target frequency.
	 *
	 * @since ContentDirectory v2
	 */
	public Boolean getSignalLocked() {
		return properties.getValue(UPNP.SignalLocked.class);
	}

	/**
	 * The upnp:signalLocked property indicates whether the signal strength is
	 * sufficiently strong to enable the hardware to lock onto the signal at the
	 * current target frequency.
	 *
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast setSignalLocked(Boolean signalLocked) {
		properties.set(new UPNP.SignalLocked(signalLocked));
		return this;
	}

	/**
	 * The upnp:tuned property indicates whether a hardware resource is
	 * currently tuned to retrieve the content represented by this item.
	 *
	 * When set to “1”, there is a hardware resource currently tuned to this
	 * item. When set to “0”, there is no hardware resource currently tuned.
	 *
	 * @since ContentDirectory v2
	 */
	public Boolean getTuned() {
		return properties.getValue(UPNP.Tuned.class);
	}

	/**
	 * The upnp:tuned property indicates whether a hardware resource is
	 * currently tuned to retrieve the content represented by this item.
	 *
	 * When set to “1”, there is a hardware resource currently tuned to this
	 * item. When set to “0”, there is no hardware resource currently tuned.
	 *
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast setTuned(Boolean tuned) {
		properties.set(new UPNP.Tuned(tuned));
		return this;
	}

	/**
	 * When set to “1”, the content represented by this object can potentially
	 * be used for recording purposes.
	 *
	 * If the object is not self-contained (such as an object of class other
	 * than “object.item.epgItem”), other information may be needed to set up
	 * the recording. When set to “0”, the content represented by this object is
	 * not accessible for recording due to various reasons, such as hardware
	 * limitations.
	 *
	 * @since ContentDirectory v2
	 */
	public Boolean getRecordable() {
		return properties.getValue(UPNP.Recordable.class);
	}

	/**
	 * When set to “1”, the content represented by this object can potentially
	 * be used for recording purposes.
	 *
	 * If the object is not self-contained (such as an object of class other
	 * than “object.item.epgItem”), other information may be needed to set up
	 * the recording. When set to “0”, the content represented by this object is
	 * not accessible for recording due to various reasons, such as hardware
	 * limitations.
	 *
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast setRecordable(Boolean recordable) {
		properties.set(new UPNP.Recordable(recordable));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getCallSign() {
		return properties.getValue(UPNP.CallSign.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast setCallSign(String callSign) {
		properties.set(new UPNP.CallSign(callSign));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public UPNP.Price getFirstPrice() {
		return properties.get(UPNP.Price.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public List<UPNP.Price> getPrices() {
		return properties.getAll(UPNP.Price.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast setPrices(UPNP.Price[] prices) {
		properties.remove(UPNP.Price.class);
		for (UPNP.Price price : prices) {
			properties.add(price);
		}
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast addPrice(UPNP.Price price) {
		properties.add(price);
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast addPrice(Float price, String currency) {
		properties.add(new UPNP.Price(price, currency));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public Boolean getPayPerView() {
		return properties.getValue(UPNP.PayPerView.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoBroadcast setPayPerView(Boolean payPerView) {
		properties.set(new UPNP.PayPerView(payPerView));
		return this;
	}

}
