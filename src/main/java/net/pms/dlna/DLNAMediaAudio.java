/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.dlna;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the audio properties of media.
 * 
 * TODO: Change all instance variables to private. For backwards compatibility
 * with external plugin code the variables have all been marked as deprecated
 * instead of changed to private, but this will surely change in the future.
 * When everything has been changed to private, the deprecated note can be
 * removed.
 */
public class DLNAMediaAudio extends DLNAMediaLang implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaAudio.class);

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int bitsperSample;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String sampleFrequency;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int nrAudioChannels;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String codecA;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String album;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String artist;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String songname;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String genre;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int year;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int track;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int delay;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String flavor;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String muxingModeAudio;

	/**
	 * Constructor
	 */
	public DLNAMediaAudio() {
		setBitsperSample(16);
	}

	/**
	 * Returns the sample rate for this audio media.
	 * 
	 * @return The sample rate.
	 */
	public int getSampleRate() {
		int sr = 0;
		if (getSampleFrequency() != null && getSampleFrequency().length() > 0) {
			try {
				if ("48000 / 24000".equals(getSampleFrequency())) {
					sr = 48000;
				} else {
					sr = Integer.parseInt(getSampleFrequency());
				}
			} catch (NumberFormatException e) {
				LOGGER.debug("Could not parse sample rate from \"" + getSampleFrequency() + "\"");
			}
		}
		return sr;
	}

	/**
	 * Returns true if this media uses the AC3 audio codec, false otherwise.
	 * 
	 * @return True if the AC3 audio codec is used.
	 */
	public boolean isAC3() {
		return getCodecA() != null && (getCodecA().equalsIgnoreCase("ac3") || getCodecA().equalsIgnoreCase("a52") || getCodecA().equalsIgnoreCase("liba52"));
	}

	/**
	 * Returns true if this media uses the TrueHD audio codec, false otherwise.
	 * 
	 * @return True if the TrueHD audio codec is used.
	 */
	public boolean isTrueHD() {
		return getCodecA() != null && getCodecA().equalsIgnoreCase("truehd");
	}

	/**
	 * Returns true if this media uses the TrueHD audio codec, false otherwise.
	 * 
	 * @return True if the TrueHD audio codec is used.
	 */
	public boolean isDTS() {
		return getCodecA() != null && (getCodecA().startsWith("dts") || getCodecA().equalsIgnoreCase("dca") || getCodecA().equalsIgnoreCase("dca (dts)"));
	}

	/**
	 * Returns true if this media uses an AC3, DTS or TrueHD codec, false otherwise.
	 * 
	 * @return True if the AC3, DTS or TrueHD codec is used.
	 */
	public boolean isNonPCMEncodedAudio() {
		return isAC3() || isDTS() || isTrueHD();
	}

	/**
	 * Returns true if this media uses the MP3 audio codec, false otherwise.
	 * 
	 * @return True if the MP3 audio codec is used.
	 */
	public boolean isMP3() {
		return getCodecA() != null && getCodecA().equalsIgnoreCase("mp3");
	}

	/**
	 * Returns true if this media uses audio that is PCM encoded, false otherwise.
	 * 
	 * @return True if the audio is PCM encoded.
	 */
	public boolean isPCM() {
		return getCodecA() != null && (getCodecA().startsWith("pcm") || getCodecA().equals("LPCM"));
	}

	/**
	 * Returns true if this media uses a lossless audio compression codec, false otherwise.
	 * 
	 * @return True if the audio is lossless compressed.
	 */
	public boolean isLossless() {
		return getCodecA() != null && (isPCM() || getCodecA().startsWith("fla") || getCodecA().equals("mlp") || getCodecA().equals("wv"));
	}

	/**
	 * Returns a standardized name for the audio codec that is used.
	 * 
	 * @return The standardized name.
	 */
	public String getAudioCodec() {
		if (isAC3()) {
			return "AC3";
		} else if (isDTS()) {
			return "DTS";
		} else if (isTrueHD()) {
			return "TrueHD";
		} else if (isPCM()) {
			return "LPCM";
		} else if (getCodecA() != null && getCodecA().equals("vorbis")) {
			return "OGG";
		} else if (getCodecA() != null && getCodecA().equals("aac")) {
			return "AAC";
		} else if (getCodecA() != null && getCodecA().equals("mp3")) {
			return "MP3";
		} else if (getCodecA() != null && getCodecA().startsWith("wm")) {
			return "WMA";
		} else if (getCodecA() != null && getCodecA().equals("mp2")) {
			return "Mpeg Audio";
		}
		return getCodecA() != null ? getCodecA() : "-";
	}

	/**
	 * Returns the identifying name for the audio properties.
	 * 
	 * @return The name.
	 */
	public String toString() {
		return "Audio: " + getAudioCodec() + " / lang: " + getLang() + " / flavor: " + getFlavor() + " / ID: " + getId();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Returns the number of bits per sample for the audio.
	 * 
	 * @return The number of bits per sample.
	 * @since 1.50
	 */
	public int getBitsperSample() {
		return bitsperSample;
	}

	/**
	 * Sets the number of bits per sample for the audio.
	 * 
	 * @param bitsperSample The number of bits per sample to set.
	 * @since 1.50
	 */
	public void setBitsperSample(int bitsperSample) {
		this.bitsperSample = bitsperSample;
	}

	/**
	 * Returns the sample frequency for the audio.
	 * 
	 * @return The sample frequency.
	 * @since 1.50
	 */
	public String getSampleFrequency() {
		return sampleFrequency;
	}

	/**
	 * Sets the sample frequency for the audio.
	 * 
	 * @param sampleFrequency The sample frequency to set.
	 * @since 1.50
	 */
	public void setSampleFrequency(String sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	/**
	 * Returns the number of channels for the audio.
	 * 
	 * @return The number of channels
	 * @since 1.50
	 */
	public int getNrAudioChannels() {
		return nrAudioChannels;
	}

	/**
	 * Sets the number of channels for the audio.
	 * 
	 * @param nrAudioChannels The number of channels to set.
	 * @since 1.50
	 */
	public void setNrAudioChannels(int nrAudioChannels) {
		this.nrAudioChannels = nrAudioChannels;
	}

	/**
	 * Returns the name of the audio codec that is being used.
	 * 
	 * @return The name of the audio codec.
	 * @since 1.50
	 */
	public String getCodecA() {
		return codecA;
	}

	/**
	 * Sets the name of the audio codec that is being used.
	 * 
	 * @param codecA The name of the audio codec to set.
	 * @since 1.50
	 */
	public void setCodecA(String codecA) {
		this.codecA = codecA;
	}

	/**
	 * Returns the name of the album to which an audio track belongs.
	 * 
	 * @return The album name.
	 * @since 1.50
	 */
	public String getAlbum() {
		return album;
	}

	/**
	 * Sets the name of the album to which an audio track belongs.
	 * 
	 * @param album The name of the album to set.
	 * @since 1.50
	 */
	public void setAlbum(String album) {
		this.album = album;
	}

	/**
	 * Returns the name of the artist performing the audio track.
	 * 
	 * @return The artist name.
	 * @since 1.50
	 */
	public String getArtist() {
		return artist;
	}

	/**
	 * Sets the name of the artist performing the audio track.
	 * 
	 * @param artist The artist name to set.
	 * @since 1.50
	 */
	public void setArtist(String artist) {
		this.artist = artist;
	}

	/**
	 * Returns the name of the song for the audio track.
	 * 
	 * @return The song name.
	 * @since 1.50
	 */
	public String getSongname() {
		return songname;
	}

	/**
	 * Sets the name of the song for the audio track.
	 * 
	 * @param songname The song name to set.
	 * @since 1.50
	 */
	public void setSongname(String songname) {
		this.songname = songname;
	}

	/**
	 * Returns the name of the genre for the audio track.
	 * 
	 * @return The genre name.
	 * @since 1.50
	 */
	public String getGenre() {
		return genre;
	}

	/**
	 * Sets the name of the genre for the audio track.
	 * 
	 * @param genre The name of the genre to set.
	 * @since 1.50
	 */
	public void setGenre(String genre) {
		this.genre = genre;
	}

	/**
	 * Returns the year of inception for the audio track.
	 * 
	 * @return The year.
	 * @since 1.50
	 */
	public int getYear() {
		return year;
	}

	/**
	 * Sets the year of inception for the audio track.
	 * 
	 * @param year The year to set.
	 * @since 1.50
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * Returns the track number within an album for the audio.
	 * 
	 * @return The track number.
	 * @since 1.50
	 */
	public int getTrack() {
		return track;
	}

	/**
	 * Sets the track number within an album for the audio.
	 * 
	 * @param track The track number to set.
	 * @since 1.50
	 */
	public void setTrack(int track) {
		this.track = track;
	}

	/**
	 * Returns the delay for the audio.
	 * 
	 * @return The delay.
	 * @since 1.50
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Sets the delay for the audio.
	 * 
	 * @param delay The delay to set.
	 * @since 1.50
	 */
	public void setDelay(int delay) {
		this.delay = delay;
	}

	/**
	 * Returns the flavor for the audio.
	 * 
	 * @return The flavor.
	 * @since 1.50
	 */
	public String getFlavor() {
		return flavor;
	}

	/**
	 * Sets the flavor for the audio.
	 * 
	 * @param flavor The flavor to set.
	 * @since 1.50
	 */
	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	/**
	 * Returns the audio codec to use for muxing.
	 * 
	 * @return The audio codec to use.
	 * @since 1.50
	 */
	public String getMuxingModeAudio() {
		return muxingModeAudio;
	}

	/**
	 * Sets the audio codec to use for muxing.
	 * 
	 * @param muxingModeAudio The audio codec to use.
	 * @since 1.50
	 */
	public void setMuxingModeAudio(String muxingModeAudio) {
		this.muxingModeAudio = muxingModeAudio;
	}
}
