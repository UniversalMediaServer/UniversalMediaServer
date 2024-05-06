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

import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * An audioBroadcast instance represents a continuous stream from an audio
 * broadcast (as opposed to, for example, a song or an audio book).
 *
 * It typically has at least one res property. This class is derived from the
 * audioItem class and inherits the properties defined by that class.
 */
public class AudioBroadcast extends AudioItem {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_AUDIOITEM_AUDIOBROADCAST_TYPE);

	public AudioBroadcast() {
		setUpnpClass(CLASS);
	}

	public AudioBroadcast(Item other) {
		super(other);
	}

	public AudioBroadcast(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, resource);
		setUpnpClass(CLASS);
	}

	/**
	 * The upnp:region property contains some identification of the region,
	 * associated with the source of the object.
	 *
	 * for example, “US”, “Latin America”, “Seattle”.
	 *
	 * @since ContentDirectory v1
	 */
	public String getRegion() {
		return properties.getValue(UPNP.Region.class);
	}

	/**
	 * The upnp:region property contains some identification of the region,
	 * associated with the source of the object.
	 *
	 * for example, “US”, “Latin America”, “Seattle”.
	 *
	 * @since ContentDirectory v1
	 */
	public AudioBroadcast setRegion(String region) {
		properties.set(new UPNP.Region(region));
		return this;
	}

	/**
	 * The upnp:radioCallSign property contains a radio station call sign
	 *
	 * for example, “KSJO”.
	 *
	 * @since ContentDirectory v1
	 */
	public String getRadioCallSign() {
		return properties.getValue(UPNP.RadioCallSign.class);
	}

	/**
	 * The upnp:radioCallSign property contains a radio station call sign
	 *
	 * for example, “KSJO”.
	 *
	 * @since ContentDirectory v1
	 */
	public AudioBroadcast setRadioCallSign(String radioCallSign) {
		properties.set(new UPNP.RadioCallSign(radioCallSign));
		return this;
	}

	/**
	 * The upnp:radioStationID property contains some identification
	 *
	 * for example, “107.7”, broadcast frequency of the radio station.
	 *
	 * @since ContentDirectory v1
	 */
	public String getRadioStationID() {
		return properties.getValue(UPNP.RadioStationID.class);
	}

	/**
	 * The upnp:radioStationID property contains some identification
	 *
	 * for example, “107.7”, broadcast frequency of the radio station.
	 *
	 * @since ContentDirectory v1
	 */
	public AudioBroadcast setRadioStationID(String radioStationID) {
		properties.set(new UPNP.RadioStationID(radioStationID));
		return this;
	}

	/**
	 * The upnp:radioBand property contains the radio station frequency band.
	 *
	 * @since ContentDirectory v1
	 */
	public String getRadioBand() {
		return properties.getValue(UPNP.RadioBand.class);
	}

	/**
	 * The upnp:radioBand property contains the radio station frequency band.
	 *
	 * @since ContentDirectory v1
	 */
	public AudioBroadcast setRadioBand(String radioBand) {
		properties.set(new UPNP.RadioBand(radioBand));
		return this;
	}

	/**
	 * The upnp:channelNr property contains the number of the associated
	 * broadcast channel.
	 *
	 * This is typically used for live content or recorded content.
	 *
	 * @since ContentDirectory v1
	 */
	public Integer getChannelNr() {
		return properties.getValue(UPNP.ChannelNr.class);
	}

	/**
	 * The upnp:channelNr property contains the number of the associated
	 * broadcast channel.
	 *
	 * This is typically used for live content or recorded content.
	 *
	 * @since ContentDirectory v1
	 */
	public AudioBroadcast setChannelNr(Integer channelNr) {
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
	public AudioBroadcast setSignalStrength(Integer signalStrength) {
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
	public AudioBroadcast setSignalLocked(Boolean signalLocked) {
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
	public AudioBroadcast setTuned(Boolean tuned) {
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
	public AudioBroadcast setRecordable(Boolean recordable) {
		properties.set(new UPNP.Recordable(recordable));
		return this;
	}

}
