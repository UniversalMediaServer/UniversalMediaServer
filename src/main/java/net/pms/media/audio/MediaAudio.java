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
package net.pms.media.audio;

import java.util.Locale;
import net.pms.configuration.FormatConfiguration;
import net.pms.media.MediaLang;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of the audio properties of media.
 */
public class MediaAudio extends MediaLang implements Cloneable {

	public static final int DEFAULT_BIT_DEPTH = 16;
	public static final int DEFAULT_SAMPLE_RATE = 48000;
	public static final int DEFAULT_NUMBER_OF_CHANNELS = 2;

	private Integer streamOrder;
	private boolean defaultFlag;
	private boolean forcedFlag;
	private String codec;
	private Long optionalId;
	private String title;
	private String muxingMode;
	private int numberOfChannels = DEFAULT_NUMBER_OF_CHANNELS;
	private int bitsDepth = DEFAULT_BIT_DEPTH;
	private int bitRate;
	private int sampleRate = DEFAULT_SAMPLE_RATE;
	private int videoDelay = 0;

	/**
	 * @return the container stream index.
	 */
	public Integer getStreamOrder() {
		return streamOrder;
	}

	/**
	 * @param streamIndex the container stream index to set
	 */
	public void setStreamOrder(Integer streamIndex) {
		this.streamOrder = streamIndex;
	}

	/**
	 * Whether the stream was flagged to default audio stream.
	 *
	 * @return {boolean}
	 */
	public boolean isDefault() {
		return defaultFlag;
	}

	/**
	 * @param defaultFlag the default flag to set
	 */
	public void setDefault(boolean defaultFlag) {
		this.defaultFlag = defaultFlag;
	}

	/**
	 * Whether the stream was flagged to forced video stream.
	 *
	 * @return {boolean}
	 */
	public boolean isForced() {
		return forcedFlag;
	}

	/**
	 * @param forcedFlag the forced flag to set
	 */
	public void setForced(boolean forcedFlag) {
		this.forcedFlag = forcedFlag;
	}

	/**
	 * Returns the optional id for this audio stream.
	 *
	 * @return The optional id.
	 */
	public Long getOptionalId() {
		return optionalId;
	}

	/**
	 * Sets an optional id for this audio stream.
	 *
	 * @param uid the optional id to set.
	 */
	public void setOptionalId(Long optionalId) {
		this.optionalId = optionalId;
	}

	/**
	 * Returns the number of bits per sample for the audio.
	 *
	 * @return The number of bits per sample.
	 * @since 1.50
	 */
	public int getBitDepth() {
		return bitsDepth;
	}

	/**
	 * Sets the number of bits per sample for the audio.
	 *
	 * @param bitsDepth The number of bits per sample to set.
	 * @since 1.50
	 */
	public void setBitDepth(int bitsDepth) {
		this.bitsDepth = bitsDepth;
	}

	/**
	 * Returns audio bitrate.
	 *
	 * @return Audio bitrate.
	 */
	public int getBitRate() {
		return bitRate;
	}

	/**
	 * Sets audio bitrate.
	 *
	 * @param bitRate Audio bitrate to set.
	 */
	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}

	/**
	 * Returns the name of the audio codec that is being used.
	 *
	 * @return The name of the audio codec.
	 * @since 1.50
	 */
	public String getCodec() {
		return codec;
	}

	/**
	 * Sets the name of the audio codec that is being used.
	 *
	 * @param codecA The name of the audio codec to set.
	 * @since 1.50
	 */
	public void setCodec(String codec) {
		this.codec = codec != null ? codec.toLowerCase(Locale.ROOT) : null;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * Returns the audio codec to use for muxing.
	 *
	 * @return The audio codec to use.
	 * @since 1.50
	 */
	public String getMuxingMode() {
		return muxingMode;
	}

	/**
	 * Sets the audio codec to use for muxing.
	 *
	 * @param muxingMode The audio codec to use.
	 * @since 1.50
	 */
	public void setMuxingMode(String muxingMode) {
		this.muxingMode = muxingMode;
	}

	/**
	 * Get number of channels for this audio track.
	 * @return number of channels (default is 2)
	 */
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	/**
	 * Set number of channels for this audio track.
	 * @param numberOfChannels number of channels to set
	 */
	public void setNumberOfChannels(int numberOfChannels) {
		if (numberOfChannels < 1) {
			throw new IllegalArgumentException("Channel number can't be less than 1.");
		}
		this.numberOfChannels = numberOfChannels;
	}

	/**
	 * Get sample frequency for this audio track.
	 * @return sample frequency in Hz
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	/**
	 * Set sample frequency for this audio track.
	 * @param sampleFrequency sample frequency in Hz
	 */
	public void setSampleRate(int sampleFrequency) {
		if (sampleFrequency < 1) {
			throw new IllegalArgumentException("Sample frequency can't be less than 1 Hz.");
		}
		this.sampleRate = sampleFrequency;
	}

	/**
	 * Get delay for this audio track.
	 * @return video delay in ms. May be negative.
	 */
	public int getVideoDelay() {
		return videoDelay;
	}

	/**
	 * Set delay for this audio track.
	 * @param audioDelay video delay in ms to set. May be negative.
	 */
	public void setVideoDelay(int videoDelay) {
		this.videoDelay = videoDelay;
	}

	/**
	 * Returns a standardized name for the audio codec that is used.
	 *
	 * @return The standardized name.
	 */
	public String getAudioCodec() {
		if (isAACLC()) {
			return "AAC-LC";
		} else if (isAC3()) {
			return "AC3";
		} else if (isACELP()) {
			return "ACELP";
		} else if (isADPCM()) {
			return "ADPCM";
		} else if (isAIFF()) {
			return "AIFF";
		} else if (isALAC()) {
			return "ALAC";
		} else if (isALS()) {
			return "ALS";
		} else if (isAtmos()) {
			return "Atmos";
		} else if (isATRAC()) {
			return "ATRAC";
		} else if (isCook()) {
			return "Cook";
		} else if (isDFF()) {
			return "DFF";
		} else if (isDSF()) {
			return "DSF";
		} else if (isDolbyE()) {
			return "Dolby E";
		} else if (isDTS()) {
			return "DTS";
		} else if (isDTSHD()) {
			return "DTS-HD";
		} else if (isEAC3()) {
			return "Enhanced AC-3";
		} else if (isERBSAC()) {
			return "ER BSAC";
		} else if (isFLAC()) {
			return "FLAC";
		} else if (isG729()) {
			return "G.729";
		} else if (isHEAAC()) {
			return "HE-AAC";
		} else if (isMLP()) {
			return "MLP";
		} else if (isMonkeysAudio()) {
			return "Monkey's Audio";
		} else if (isMP3()) {
			return "MP3";
		} else if (isMpegAudio()) {
			return "Mpeg Audio";
		} else if (isMPC()) {
			return "Musepack";
		} else if (isOpus()) {
			return "Opus";
		} else if (isPCM()) {
			return "LPCM";
		} else if (isQDesign()) {
			return "QDesign";
		} else if (isRealAudio144()) {
			return "RealAudio 14.4";
		} else if (isRealAudio288()) {
			return "RealAudio 28.8";
		} else if (isRALF()) {
			return "RealAudio Lossless";
		} else if (isShorten()) {
			return "Shorten";
		} else if (isSipro()) {
			return "Sipro";
		} else if (isSLS()) {
			return "SLS";
		} else if (isTrueHD()) {
			return "TrueHD";
		} else if (isTTA()) {
			return "TTA";
		} else if (isVorbis()) {
			return "Vorbis";
		} else if (isWAV()) {
			return "WAV";
		} else if (isWavPack()) {
			return "WavPack";
		} else if (isWMA()) {
			return "WMA";
		} else if (isWMA10()) {
			return "WMA 10";
		} else if (isWMALossless()) {
			return "WMA Lossless";
		} else if (isWMAPro()) {
			return "WMA Pro";
		} else if (isWMAVoice()) {
			return "WMA Voice";
		}
		return getCodec() != null ? getCodec() : "-";
	}

	/**
	 * @return whether the audio codec is one of the AAC variants.
	 */
	public boolean isAAC() {
		return isAACLC() || isHEAAC();
	}

	/**
	 * @return whether the audio codec is AAC-LC.
	 */
	public boolean isAACLC() {
		return FormatConfiguration.AAC_LC.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is AC-3.
	 */
	public boolean isAC3() {
		return FormatConfiguration.AC3.equalsIgnoreCase(getCodec()) || getCodec() != null && getCodec().contains("a52");
	}

	/**
	 * @return whether the audio codec is AC-4.
	 */
	public boolean isAC4() {
		return FormatConfiguration.AC4.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is ACELP.
	 */
	public boolean isACELP() {
		return FormatConfiguration.ACELP.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is ADPCM.
	 */
	public boolean isADPCM() {
		return FormatConfiguration.ADPCM.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is AIFF.
	 */
	public boolean isAIFF() {
		return FormatConfiguration.AIFF.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is ALAC.
	 */
	public boolean isALAC() {
		return FormatConfiguration.ALAC.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is ALS.
	 */
	public boolean isALS() {
		return FormatConfiguration.ALS.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is Atmos.
	 */
	public boolean isAtmos() {
		return FormatConfiguration.ATMOS.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is ATRAC.
	 */
	public boolean isATRAC() {
		return FormatConfiguration.ATRAC.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is Cook.
	 */
	public boolean isCook() {
		return FormatConfiguration.COOK.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is Dolby E.
	 */
	public boolean isDolbyE() {
		return FormatConfiguration.DOLBYE.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is DSD Audio.
	 */
	public boolean isDFF() {
		return FormatConfiguration.DFF.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is DSF.
	 */
	public boolean isDSF() {
		return FormatConfiguration.DSF.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is DTS.
	 */
	public boolean isDTS() {
		return FormatConfiguration.DTS.equalsIgnoreCase(getCodec()) || getCodec() != null && getCodec().contains("dca");
	}

	/**
	 * @return whether the audio codec is DTS HD.
	 */
	public boolean isDTSHD() {
		return FormatConfiguration.DTSHD.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is EAC-3.
	 */
	public boolean isEAC3() {
		return FormatConfiguration.EAC3.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is ER BSAC.
	 */
	public boolean isERBSAC() {
		return FormatConfiguration.ER_BSAC.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is FLAC.
	 */
	public boolean isFLAC() {
		return FormatConfiguration.FLAC.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is G729.
	 */
	public boolean isG729() {
		return FormatConfiguration.G729.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is HE-AAC.
	 */
	public boolean isHEAAC() {
		return FormatConfiguration.HE_AAC.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is MLP.
	 */
	public boolean isMLP() {
		return FormatConfiguration.MLP.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is MonkeysAudio.
	 */
	public boolean isMonkeysAudio() {
		return FormatConfiguration.MONKEYS_AUDIO.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is MP3.
	 */
	public boolean isMP3() {
		return FormatConfiguration.MP3.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is MPEG-1/MPEG-2.
	 */
	public boolean isMpegAudio() {
		return FormatConfiguration.MP2.equalsIgnoreCase(getCodec()) || FormatConfiguration.MPA.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is MPC.
	 */
	public boolean isMPC() {
		return FormatConfiguration.MPC.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is OPUS.
	 */
	public boolean isOpus() {
		return FormatConfiguration.OPUS.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is PCM.
	 */
	public boolean isPCM() {
		return FormatConfiguration.LPCM.equals(getCodec()) || getCodec() != null && getCodec().startsWith("pcm");
	}

	/**
	 * @return whether the audio codec is QDesign.
	 */
	public boolean isQDesign() {
		return FormatConfiguration.QDESIGN.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is RealAudio Lossless.
	 */
	public boolean isRALF() {
		return FormatConfiguration.RALF.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is RealAudio 14.4.
	 */
	public boolean isRealAudio144() {
		return FormatConfiguration.REALAUDIO_14_4.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is RealAudio 28.8.
	 */
	public boolean isRealAudio288() {
		return FormatConfiguration.REALAUDIO_28_8.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is Shorten.
	 */
	public boolean isShorten() {
		return FormatConfiguration.SHORTEN.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is Sipro Lab Telecom Audio Codec.
	 */
	public boolean isSipro() {
		return FormatConfiguration.SIPRO.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is SLS.
	 */
	public boolean isSLS() {
		return FormatConfiguration.SLS.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is TrueHD.
	 */
	public boolean isTrueHD() {
		return FormatConfiguration.TRUEHD.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is TTA.
	 */
	public boolean isTTA() {
		return FormatConfiguration.TTA.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is Vorbis.
	 */
	public boolean isVorbis() {
		return FormatConfiguration.VORBIS.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is Wav.
	 */
	public boolean isWAV() {
		return FormatConfiguration.WAV.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is WavPack.
	 */
	public boolean isWavPack() {
		return FormatConfiguration.WAVPACK.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is WMA.
	 */
	public boolean isWMA() {
		return FormatConfiguration.WMA.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is WMA10.
	 */
	public boolean isWMA10() {
		return FormatConfiguration.WMA10.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is WMA Lossless.
	 */
	public boolean isWMALossless() {
		return FormatConfiguration.WMALOSSLESS.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is WMA Pro.
	 */
	public boolean isWMAPro() {
		return FormatConfiguration.WMAPRO.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is WMA Voice.
	 */
	public boolean isWMAVoice() {
		return FormatConfiguration.WMAVOICE.equalsIgnoreCase(getCodec());
	}

	/**
	 * @return whether the audio codec is AC-3, DTS, DTS-HD or TrueHD.
	 */
	public boolean isNonPCMEncodedAudio() {
		return isAC3() || isAtmos() || isDTS() || isTrueHD() || isDTSHD();
	}

	/**
	 * @return whether the audio codec is lossless.
	 */
	public boolean isLossless() {
		return getCodec() != null &&
			(
				isAIFF() || isALAC() || isALS() || isFLAC() || isMLP() ||
				isMonkeysAudio() || isPCM() || isRALF() || isShorten() ||
				isSLS() || isTrueHD() || isTTA() || isWAV() || isWavPack() ||
				isWMALossless() || isDTSHD()
			);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Returns a string containing all identifying audio properties.
	 *
	 * @return The properties string.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Audio Id: ").append(getId());
		if (forcedFlag) {
			result.append(", Default");
		}
		if (forcedFlag) {
			result.append(", Forced");
		}
		if (StringUtils.isNotBlank(getTitle())) {
			result.append(", Title: ").append(getTitle());
		}
		if (StringUtils.isNotBlank(getLang()) && !UND.equals(getLang())) {
			result.append(", Language Code: ").append(getLang());
		}
		result.append(", Codec: ").append(getAudioCodec());
		if (getOptionalId() != null && getOptionalId() > 10) {
			result.append(", Optional Id: ").append(getOptionalId());
		}
		if (getStreamOrder() != null) {
			result.append(", Stream Order: ").append(getStreamOrder());
		}
		result.append(", Bitrate: ").append(getBitRate());
		if (getBitDepth() != 16) {
			result.append(", Bits per Sample: ").append(getBitDepth());
		}
		if (getNumberOfChannels() == 1) {
			result.append(", Channel: ").append(getNumberOfChannels());
		} else {
			result.append(", Channels: ").append(getNumberOfChannels());
		}
		result.append(", Sample Frequency: ").append(getSampleRate()).append(" Hz");
		if (getVideoDelay() != 0) {
			result.append(", Video Delay: ").append(getVideoDelay());
		}

		if (StringUtils.isNotBlank(getMuxingMode())) {
			result.append(", Muxing Mode: ").append(getMuxingMode());
		}

		return result.toString();
	}


}
