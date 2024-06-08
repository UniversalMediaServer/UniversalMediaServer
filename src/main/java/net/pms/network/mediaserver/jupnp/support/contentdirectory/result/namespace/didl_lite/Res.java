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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import java.net.URI;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import org.jupnp.support.model.Protocol;
import org.jupnp.util.MimeType;

/**
 * The res property indicates a resource, typically a media file, associated
 * with the object.
 *
 * If the value of the res property is not present, then the content has not yet
 * been fully imported by the ContentDirectory service and is not yet accessible
 * for playback purposes. Values shall be properly escaped URIs
 */
public class Res extends Property<URI> implements DIDL_LITE.NAMESPACE {

	/**
	 * This required property identifies the protocol that shall be used to
	 * transmit the resource.
	 *
	 * Default Value: N/A – The property is required when the res property is
	 * present.
	 */
	public static class ProtocolInfo extends Property<org.jupnp.support.model.ProtocolInfo> implements DIDL_LITE.NAMESPACE {

		public ProtocolInfo() {
		}

		public ProtocolInfo(org.jupnp.support.model.ProtocolInfo value) {
			super(value, "protocolInfo");
		}
	}

	/**
	 * The read-only res@importUri property indicates the URI via which the
	 * resource can be imported to the ContentDirectory service via the
	 * ImportResource() action or HTTP POST.
	 *
	 * The res@importUri property identifies a download portal for the
	 * associated res property of a specific target object. It is used to create
	 * a local copy of the external content. After the transfer finishes
	 * successfully, the local content is then associated with the target object
	 * by setting the target object’s res property value to a URI for that
	 * content, which may or may not be the same URI as the one specified in the
	 * res@importUri property, depending on the ContentDirectory service
	 * implementation.
	 */
	public static class ImportUri extends Property<URI> implements DIDL_LITE.NAMESPACE {

		public ImportUri() {
		}

		public ImportUri(URI value) {
			super(value, "importUri");
		}
	}

	/**
	 * The res@size property indicates the size in bytes of the resource.
	 */
	public static class Size extends Property<UnsignedLong> implements DIDL_LITE.NAMESPACE {

		public Size() {
		}

		public Size(UnsignedLong value) {
			super(value, "size");
		}
	}

	/**
	 * The res@duration property indicates the time duration of the playback of
	 * the resource, at normal speed.
	 *
	 * The form of the duration string is: H+:MM:SS[.F+] or H+:MM:SS[.F0/F1]
	 *
	 * The string may be preceded by a “+” or “-” sign, and the decimal point
	 * itself shall be omitted if there are no fractional second digits.
	 */
	public static class Duration extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Duration() {
		}

		public Duration(String value) {
			super(value, "duration");
		}
	}

	/**
	 * The res@protection property contains some identification of a protection
	 * system used for the resource.
	 */
	public static class Protection extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Protection() {
		}

		public Protection(String value) {
			super(value, "protection");
		}
	}

	/**
	 * The res@bitrate property indicates the bitrate in bytes/second of the
	 * encoding of the resource.
	 *
	 * Note that there exists an inconsistency with a res@bitrate property name
	 * and its value being expressed in bytes/sec.
	 *
	 * In case the resource has been encoded using variable bitrate (VBR), it is
	 * recommended that the res@bitrate value represents the average bitrate,
	 * calculated over the entire duration of the resource (total number of
	 * bytes divided by the total duration of the resource).
	 *
	 * The res@bitrate value should not be taken as sufficient from a QoS or
	 * other perspective to prepare for the stream; The protocol used and the
	 * physical layer headers can increase the actual bandwidth needed.
	 */
	public static class Bitrate extends Property<UnsignedInteger> implements DIDL_LITE.NAMESPACE {

		public Bitrate() {
		}

		public Bitrate(UnsignedInteger value) {
			super(value, "bitrate");
		}
	}

	/**
	 * The res@bitsPerSample property indicates the number of bits used to
	 * represent one sample of the resource.
	 */
	public static class BitsPerSample extends Property<UnsignedInteger> implements DIDL_LITE.NAMESPACE {

		public BitsPerSample() {
		}

		public BitsPerSample(UnsignedInteger value) {
			super(value, "bitsPerSample");
		}
	}

	/**
	 * The res@sampleFrequency property indicates the sample frequency used to
	 * digitize the audio resource.
	 *
	 * Expressed in Hz.
	 */
	public static class SampleFrequency extends Property<UnsignedInteger> implements DIDL_LITE.NAMESPACE {

		public SampleFrequency() {
		}

		public SampleFrequency(UnsignedInteger value) {
			super(value, "sampleFrequency");
		}
	}

	/**
	 * The res@nrAudioChannels property indicates the number of audio channels
	 * present in the audio resource.
	 *
	 * for example, 1 for mono, 2 for stereo, 6 for Dolby Surround.
	 */
	public static class NrAudioChannels extends Property<UnsignedInteger> implements DIDL_LITE.NAMESPACE {

		public NrAudioChannels() {
		}

		public NrAudioChannels(UnsignedInteger value) {
			super(value, "nrAudioChannels");
		}
	}

	/**
	 * The res@resolution property indicates the XxY resolution, in pixels, of
	 * the resource (typically an imageItem or videoItem).
	 *
	 * The string pattern is of the form: “[0-9]+x[0-9]+” (one or more digits,
	 * followed by “x”, followed by one or more digits).
	 */
	public static class Resolution extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Resolution() {
		}

		public Resolution(String value) {
			super(value, "resolution");
		}
	}

	/**
	 * The res@colorDepth property indicates the number of bits per pixel used
	 * to represent the video or image resource.
	 */
	public static class ColorDepth extends Property<UnsignedInteger> implements DIDL_LITE.NAMESPACE {

		public ColorDepth() {
		}

		public ColorDepth(UnsignedInteger value) {
			super(value, "colorDepth");
		}
	}

	/**
	 * The res@tspec property identifies the content QoS (quality of service)
	 * requirements.
	 *
	 * It has a maximum length of 256 characters.
	 */
	public static class Tspec extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Tspec() {
		}

		public Tspec(String value) {
			super(value, "tspec");
		}
	}

	/**
	 * The res@allowedUse property is composed of a comma-separated list of
	 * value pairs.
	 *
	 * Each value pair is composed of an enumerated string value, followed by a
	 * colon (“:”), followed by an integer. For example, “PLAY:5,COPY:1”.
	 */
	public static class AllowedUse extends Property<String> implements DIDL_LITE.NAMESPACE {

		public AllowedUse() {
		}

		public AllowedUse(String value) {
			super(value, "allowedUse");
		}
	}

	/**
	 * The res@validityStart property defines the beginning date&time when the
	 * corresponding uses described in the res@allowedUse property become valid.
	 *
	 * The format of the res@validityStart property MUST comply with the
	 * date-time syntax as defined in Appendix D, “EBNF Syntax Definitions”.
	 */
	public static class ValidityStart extends Property<String> implements DIDL_LITE.NAMESPACE {

		public ValidityStart() {
		}

		public ValidityStart(String value) {
			super(value, "validityStart");
		}
	}

	/**
	 * The res@validityEnd property defines the ending date&time when the
	 * corresponding uses described in the res@allowedUse property become
	 * invalid.
	 *
	 * The format of the res@validityEnd property MUST comply with the date-time
	 * syntax as defined in Appendix D, “EBNF Syntax Definitions”.
	 */
	public static class ValidityEnd extends Property<String> implements DIDL_LITE.NAMESPACE {

		public ValidityEnd() {
		}

		public ValidityEnd(String value) {
			super(value, "validityEnd");
		}
	}

	/**
	 * The res@remainingTime property is used to indicate the amount of time
	 * remaining until the use specified in the res@allowedUse property is
	 * revoked.
	 *
	 * The remaining time is an aggregate amount of time that the resource may
	 * be used either continuously or in discrete intervals.
	 */
	public static class RemainingTime extends Property<String> implements DIDL_LITE.NAMESPACE {

		public RemainingTime() {
		}

		public RemainingTime(String value) {
			super(value, "remainingTime");
		}
	}

	/**
	 * The res@usageInfo property contains a user-friendly string with
	 * additional information about the allowed use of the resource.
	 *
	 * As in the example: "Playing of the movie is allowed in high-definition
	 * mode. One copy is allowed to be made, but only the standard definition
	 * version may be copied".
	 */
	public static class UsageInfo extends Property<String> implements DIDL_LITE.NAMESPACE {

		public UsageInfo() {
		}

		public UsageInfo(String value) {
			super(value, "usageInfo");
		}
	}

	/**
	 * The res@rightsInfoURI property references an html page and a web site
	 * associated with the rights vendor for the resource.
	 *
	 * The referenced page SHOULD assist the user interface in documenting the
	 * rights and the renewal of the allowed use of the resource.
	 */
	public static class RightsInfoURI extends Property<String> implements DIDL_LITE.NAMESPACE {

		public RightsInfoURI() {
		}

		public RightsInfoURI(String value) {
			super(value, "rightsInfoURI");
		}
	}

	/**
	 * Each res@contentInfoURI property contains a URI employed to assist the
	 * user interface in providing additional information to the user about the
	 * content referenced by the resource.
	 *
	 * The value of this property refers to an html page and a web site
	 * associated with the content vendor for the resource.
	 */
	public static class ContentInfoURI extends Property<String> implements DIDL_LITE.NAMESPACE {

		public ContentInfoURI() {
		}

		public ContentInfoURI(String value) {
			super(value, "contentInfoURI");
		}
	}

	/**
	 * When the resource referenced by the res property was created by
	 * recording, the res@recordQuality property can be specified to indicate
	 * the quality level(s) used to make the recording.
	 *
	 *
	 * @see The res@recordQuality property is a CSV list of <type> “:”
	 * <recording quality> pairs.
	 */
	public static class RecordQuality extends Property<String> implements DIDL_LITE.NAMESPACE {

		public RecordQuality() {
		}

		public RecordQuality(String value) {
			super(value, "recordQuality");
		}
	}

	/**
	 * The res@daylightSaving property indicates whether the time values used in
	 * other res-dependent properties, such as the res@validityStart property
	 * and the res@validityEnd property, are expressed using as a reference
	 * either Daylight Saving Time or Standard Time.
	 *
	 * This property is only applicable when the time values in other
	 * res-dependent properties are expressed in local time. Whenever the time
	 * value in those properties are expressed in absolute time, the
	 * res@daylightSaving property shall not be present on output and shall be
	 * ignored on input.
	 */
	public static class DaylightSaving extends Property<String> implements DIDL_LITE.NAMESPACE {

		public DaylightSaving() {
		}

		public DaylightSaving(String value) {
			super(value, "daylightSaving");
		}
	}

	/**
	 * The res@framerate property indicates the frame rate in frames/second of
	 * the encoding of the resource including a trailing indication of
	 * progressive or interlaced scanning. Format of the string is:
	 * <numeric value>p or <numeric value>i.
	 *
	 * Example: “29.97i” indicates a frame rate of 29.97 frames per second
	 * interlaced scanning. “30p” indicates a frame rate of 30 frames per second
	 * progressive scanning. “50i” indicates a frame rate of 50 frames per
	 * second interlaced scanning.
	 */
	public static class Framerate extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Framerate() {
		}

		public Framerate(String value) {
			super(value, "framerate");
		}
	}

	/**
	 * The read-only res@updateCount property is an allowed property that
	 * contains the number of times the implementation detects that a change was
	 * made to the content that is referenced by the res property’s URI since
	 * the last initialization.
	 *
	 * However, the res@updateCount property is not incremented for live content
	 * (for example an object whose class is
	 * “object.item.videoItem.videoBroadcast”). When a res property is first
	 * created, the value of the res@updateCount property shall be initialized
	 * to zero (“0”) regardless of whether the res property contains an initial
	 * URI value or not. When the ContentDirectory service implementation
	 * detects that the content referenced by the res property’s URI has
	 * changed, the value of the corresponding res@updateCount property shall be
	 * incremented by one (“1”). If implemented, the current value the
	 * res@updateCount property shall be persisted even while off-line.
	 */
	public static class UpdateCount extends Property<UnsignedInteger> implements DIDL_LITE.NAMESPACE {

		public UpdateCount() {
		}

		public UpdateCount(UnsignedInteger value) {
			super(value, "updateCount");
		}
	}

	public Res() {
		this(null);
	}

	public Res(URI value) {
		super(value, "res");
	}

	public Res(String httpGetMimeType, Long size, String duration, UnsignedInteger bitrate, URI value) {
		this(new org.jupnp.support.model.ProtocolInfo(Protocol.HTTP_GET, org.jupnp.support.model.ProtocolInfo.WILDCARD, httpGetMimeType, org.jupnp.support.model.ProtocolInfo.WILDCARD), size, duration, bitrate, value);
	}

	public Res(MimeType httpGetMimeType, Long size, String duration, UnsignedInteger bitrate, URI value) {
		this(new org.jupnp.support.model.ProtocolInfo(httpGetMimeType), size, duration, bitrate, value);
	}

	public Res(MimeType httpGetMimeType, Long size, URI value) {
		this(new org.jupnp.support.model.ProtocolInfo(httpGetMimeType), size, value);
	}

	public Res(org.jupnp.support.model.ProtocolInfo protocolInfo, Long size, URI value) {
		this(value);
		setProtocolInfo(protocolInfo);
		setSize(size);
	}

	public Res(org.jupnp.support.model.ProtocolInfo protocolInfo, Long size, String duration, UnsignedInteger bitrate, URI value) {
		this(protocolInfo, size, value);
		setDuration(duration);
		setBitrate(bitrate);
	}

	public Res(URI importUri, org.jupnp.support.model.ProtocolInfo protocolInfo, Long size, String duration, UnsignedInteger bitrate, UnsignedInteger sampleFrequency, UnsignedInteger bitsPerSample, UnsignedInteger nrAudioChannels, UnsignedInteger colorDepth, String protection, String resolution, URI value) {
		this(protocolInfo, size, duration, bitrate, value);
		setImportUri(importUri);
		setProtection(protection);
		setBitsPerSample(bitsPerSample);
		setSampleFrequency(sampleFrequency);
		setNrAudioChannels(nrAudioChannels);
		setResolution(resolution);
		setColorDepth(colorDepth);
	}

	public URI getImportUri() {
		return dependentProperties.getValue(Res.ImportUri.class);
	}

	public final void setImportUri(URI importUri) {
		dependentProperties.set(new Res.ImportUri(importUri));
	}

	public org.jupnp.support.model.ProtocolInfo getProtocolInfo() {
		return dependentProperties.getValue(Res.ProtocolInfo.class);
	}

	public final void setProtocolInfo(org.jupnp.support.model.ProtocolInfo protocolInfo) {
		dependentProperties.set(new Res.ProtocolInfo(protocolInfo));
	}

	public UnsignedLong getSize() {
		return dependentProperties.getValue(Res.Size.class);
	}

	public final void setSize(UnsignedLong size) {
		dependentProperties.set(new Res.Size(size));
	}

	public final void setSize(long size) {
		setSize(UnsignedLong.asUnsigned(size));
	}

	public String getDuration() {
		return dependentProperties.getValue(Res.Duration.class);
	}

	public final void setDuration(String duration) {
		dependentProperties.set(new Res.Duration(duration));
	}

	public String getProtection() {
		return dependentProperties.getValue(Res.Protection.class);
	}

	public final void setProtection(String protection) {
		dependentProperties.set(new Res.Protection(protection));
	}

	public UnsignedInteger getBitrate() {
		return dependentProperties.getValue(Res.Bitrate.class);
	}

	public final void setBitrate(UnsignedInteger bitrate) {
		dependentProperties.set(new Res.Bitrate(bitrate));
	}

	public final void setBitrate(int bitrate) {
		setBitrate(UnsignedInteger.asUnsigned(bitrate));
	}

	public UnsignedInteger getBitsPerSample() {
		return dependentProperties.getValue(Res.BitsPerSample.class);
	}

	public final void setBitsPerSample(UnsignedInteger bitsPerSample) {
		dependentProperties.set(new Res.BitsPerSample(bitsPerSample));
	}

	public final void setBitsPerSample(int bitsPerSample) {
		setBitsPerSample(UnsignedInteger.asUnsigned(bitsPerSample));
	}

	public UnsignedInteger getSampleFrequency() {
		return dependentProperties.getValue(Res.SampleFrequency.class);
	}

	public final void setSampleFrequency(UnsignedInteger sampleFrequency) {
		dependentProperties.set(new Res.SampleFrequency(sampleFrequency));
	}

	public final void setSampleFrequency(int sampleFrequency) {
		setSampleFrequency(UnsignedInteger.asUnsigned(sampleFrequency));
	}

	public UnsignedInteger getNrAudioChannels() {
		return dependentProperties.getValue(Res.NrAudioChannels.class);
	}

	public final void setNrAudioChannels(UnsignedInteger nrAudioChannels) {
		dependentProperties.set(new Res.NrAudioChannels(nrAudioChannels));
	}

	public final void setNrAudioChannels(int nrAudioChannels) {
		setNrAudioChannels(UnsignedInteger.asUnsigned(nrAudioChannels));
	}

	public String getResolution() {
		return dependentProperties.getValue(Res.Resolution.class);
	}

	public int getResolutionX() {
		return getResolution() != null && getResolution().split("x").length == 2 ?
			Integer.parseInt(getResolution().split("x")[0]) :
			0;
	}

	public int getResolutionY() {
		return getResolution() != null && getResolution().split("x").length == 2 ?
			Integer.parseInt(getResolution().split("x")[1]) :
			0;
	}

	public final void setResolution(String resolution) {
		dependentProperties.set(new Res.Resolution(resolution));
	}

	public void setResolution(int x, int y) {
		setResolution(x + "x" + y);
	}

	public UnsignedInteger getColorDepth() {
		return dependentProperties.getValue(Res.ColorDepth.class);
	}

	public final void setColorDepth(UnsignedInteger colorDepth) {
		dependentProperties.set(new Res.ColorDepth(colorDepth));
	}

	public final void setColorDepth(int colorDepth) {
		setColorDepth(UnsignedInteger.asUnsigned(colorDepth));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getTspec() {
		return dependentProperties.getValue(Res.Tspec.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setTspec(String tspec) {
		dependentProperties.set(new Res.Tspec(tspec));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getAllowedUse() {
		return dependentProperties.getValue(Res.AllowedUse.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setAllowedUse(String allowedUse) {
		dependentProperties.set(new Res.AllowedUse(allowedUse));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getValidityStart() {
		return dependentProperties.getValue(Res.ValidityStart.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setValidityStart(String validityStart) {
		dependentProperties.set(new Res.ValidityStart(validityStart));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getValidityEnd() {
		return dependentProperties.getValue(Res.ValidityEnd.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setValidityEnd(String validityEnd) {
		dependentProperties.set(new Res.ValidityEnd(validityEnd));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getRemainingTime() {
		return dependentProperties.getValue(Res.RemainingTime.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setRemainingTime(String remainingTime) {
		dependentProperties.set(new Res.RemainingTime(remainingTime));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getUsageInfo() {
		return dependentProperties.getValue(Res.UsageInfo.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setUsageInfo(String usageInfo) {
		dependentProperties.set(new Res.UsageInfo(usageInfo));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getRightsInfoURI() {
		return dependentProperties.getValue(Res.RightsInfoURI.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setRightsInfoURI(String rightsInfoURI) {
		dependentProperties.set(new Res.RightsInfoURI(rightsInfoURI));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getContentInfoURI() {
		return dependentProperties.getValue(Res.ContentInfoURI.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setContentInfoURI(String contentInfoURI) {
		dependentProperties.set(new Res.ContentInfoURI(contentInfoURI));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getRecordQuality() {
		return dependentProperties.getValue(Res.RecordQuality.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public void setRecordQuality(String recordQuality) {
		dependentProperties.set(new Res.RecordQuality(recordQuality));
	}

	/**
	 * @since ContentDirectory v3
	 */
	public String getDaylightSaving() {
		return dependentProperties.getValue(Res.DaylightSaving.class);
	}

	/**
	 * @since ContentDirectory v3
	 */
	public void setDaylightSaving(DaylightSavingValue daylightSaving) {
		dependentProperties.set(new Res.DaylightSaving(daylightSaving.toString()));
	}

	/**
	 * @since ContentDirectory v3
	 */
	public UnsignedInteger getUpdateCount() {
		return dependentProperties.getValue(Res.UpdateCount.class);
	}

	/**
	 * @since ContentDirectory v3
	 */
	public void setUpdateCount(UnsignedInteger updateCount) {
		dependentProperties.set(new Res.UpdateCount(updateCount));
	}

	/**
	 * @since ContentDirectory v4
	 */
	public String getFramerate() {
		return dependentProperties.getValue(Res.Framerate.class);
	}

	/**
	 * @since ContentDirectory v4
	 */
	public void setFramerate(String framerate) {
		dependentProperties.set(new Res.Framerate(framerate));
	}

}
