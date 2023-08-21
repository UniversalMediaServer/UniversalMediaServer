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
package net.pms.media.audio.metadata;

/**
 * This class keeps track of the metadata of audio media.
 */
public class MediaAudioMetadata {
	private String album;
	private String artist;
	private String composer;
	private String conductor;
	private String songname;
	private String genre;
	private int year;
	private int disc = 1;
	private int track;
	private String albumartist;
	private String mbidRecord;
	private String mbidTrack;
	private Integer rating;
	private int audiotrackId;

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
	 * Sets the MB record ID for this track.
	 *
	 * @param mbidRecord The MB record ID.
	 */
	public void setMbidRecord(String mbidRecord) {
		this.mbidRecord = mbidRecord;
	}

	/**
	 * Returns the MB record ID for this track
	 *
	 * @return The MB record ID.
	 */
	public String getMbidRecord() {
		return this.mbidRecord;
	}

	/**
	 * Sets the MB track ID for this track.
	 *
	 * @param mbidTrack The MB track ID.
	 */
	public void setMbidTrack(String mbidTrack) {
		this.mbidTrack = mbidTrack;
	}

	/**
	 * Returns MB track id for this track.
	 *
	 * @return The MB track ID.
	 */
	public String getMbidTrack() {
		return this.mbidTrack;
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
	 * Returns the composer of the audio track.
	 *
	 * @return The composer name.
	 */
	public String getComposer() {
		return composer;
	}

	/**
	 * Returns the conductor of the audio track.
	 *
	 * @return The conductor name.
	 */
	public String getConductor() {
		return conductor;
	}

	/**
	 * Sets the name of the main artist of the album of the audio track.
	 * This field is often used for the compilation type albums or "featuring..." songs.
	 *
	 * @param artist The album artist name to set.
	 */
	public void setAlbumArtist(String artist) {
		this.albumartist = artist;
	}

	/**
	 * Returns the name of the main artist of the album of the audio track.
	 *
	 * @return The album artist name.
	 */
	public String getAlbumArtist() {
		return albumartist;
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
	 * Sets the composer of the audio track.
	 *
	 * @param composer The composer name to set.
	 */
	public void setComposer(String composer) {
		this.composer = composer;
	}

	/**
	 * Sets the conductor of the audio track.
	 *
	 * @param The conductor name to set.
	 */
	public void setConductor(String conductor) {
		this.conductor = conductor;
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
	 * Returns the disc number of an album for the audio.
	 *
	 * @return The disc number.
	 */
	public int getDisc() {
		return disc;
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

	public void setDisc(int disc) {
		this.disc = disc;
	}

	/**
	 *
	 * @return user rating (0 - 5 stars)
	 */
	public Integer getRating() {
		return rating;
	}

	/**
	 * Set's user rating (0 - 5 stars)
	 * FIXME user rating is not the overall rating on file.
	 * @param rating
	 */
	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public int getAudiotrackId() {
		return audiotrackId;
	}

	public void setAudiotrackId(int audiotrackId) {
		this.audiotrackId = audiotrackId;
	}

}
