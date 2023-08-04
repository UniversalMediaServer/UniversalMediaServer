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
package net.pms.media.chapter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnail;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of the chapter properties of media.
 */
public class MediaChapter extends MediaLang {
	public static final Pattern CHAPTERS_TITLE_DEFAULT = Pattern.compile("^Chapter\\s\\d\\d$");
	public static final Pattern CHAPTERS_TITLE_TIMESTAMP = Pattern.compile("^\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d$");
	public static final DateTimeFormatter CHAPTERS_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private int parentId;
	private String title;
	private double start;
	private double end;
	private Map<String, String> metadatas;
	private DLNAThumbnail thumbnail;

	/**
	 * Returns the parent id for this chapter object
	 *
	 * @return The id.
	 */
	public int getParentId() {
		return parentId;
	}

	/**
	 * Sets a parent id for this chapter object.
	 *
	 * @param parentId the parent id to set.
	 */
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	/**
	 * Returns the title for this chapter object
	 *
	 * @return The title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets a title for this chapter object.
	 *
	 * @param title the title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the metadatas for this chapter object.
	 *
	 * @return The metadatas.
	 */
	public Map<String, String> getMetadatas() {
		return metadatas;
	}

	/**
	 * Sets the metadatas for this chapter object.
	 *
	 * @param metadatas The metadatas to set.
	 */
	public void setMetadatas(Map<String, String> metadatas) {
		this.metadatas = metadatas;
	}

	/**
	 * Sets a metadata for this chapter object.
	 *
	 * @param key The metadata key to set.
	 * @param value The metadata value to set.
	 */
	public void setMetadata(String key, String value) {
		if (metadatas == null) {
			metadatas = new HashMap<>();
		}
		this.metadatas.put(key, value);
	}

	/**
	 * Returns the start time for this chapter object.
	 *
	 * @return The start time.
	 */
	public double getStart() {
		return start;
	}

	/**
	 * Sets the start time for this chapter object.
	 *
	 * @param start The start time to set.
	 */
	public void setStart(double start) {
		this.start = start;
	}

	/**
	 * Returns the end time for this chapter object.
	 *
	 * @return The end time.
	 */
	public double getEnd() {
		return end;
	}

	/**
	 * Sets the end time for this chapter object.
	 *
	 * @param end The end time to set.
	 */
	public void setEnd(double end) {
		this.end = end;
	}

	/**
	 * Returns the thumbnail for this chapter object.
	 * Format png 63x30
	 *
	 * @return The thumbnail.
	 */
	public DLNAThumbnail getThumbnail() {
		return thumbnail;
	}

	/**
	 * Sets the thumbnail for this chapter object.
	 *
	 * @param thumbnail The thumbnail to set.
	 */
	public void setThumbnail(DLNAThumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}

	/**
	 * Check if the title is a default title.
	 *
	 * @param title The title to check.
	 * @return true if the title is a default title.
	 */
	public static boolean isTitleDefault(String title) {
		return CHAPTERS_TITLE_DEFAULT.matcher(title).matches() || CHAPTERS_TITLE_TIMESTAMP.matcher(title).matches();
	}

	/**
	 * Return a WebVtt from a resource.
	 *
	 * @param dlna The dlna resource.
	 * @return The WebVtt representation of the chapter list.
	 */
	public static String getWebVtt(DLNAResource dlna) {
		StringBuilder chaptersVtt = new StringBuilder();
		chaptersVtt.append("WEBVTT\n");
		MediaInfo mediaVideo = dlna.getMedia();
		if (mediaVideo != null && mediaVideo.hasChapters()) {
			for (MediaChapter chapter : mediaVideo.getChapters()) {
				int chaptersNum = chapter.getId() + 1;
				chaptersVtt.append("\nChapter ").append(chaptersNum).append("\n");
				long nanoOfDay = (long) (chapter.getStart() * 1000_000_000D);
				LocalTime lt = LocalTime.ofNanoOfDay(nanoOfDay);
				chaptersVtt.append(lt.format(CHAPTERS_TIMESTAMP_FORMATTER));
				chaptersVtt.append(" --> ");
				nanoOfDay = (long) (chapter.getEnd() * 1000_000_000D);
				lt = LocalTime.ofNanoOfDay(nanoOfDay);
				chaptersVtt.append(lt.format(CHAPTERS_TIMESTAMP_FORMATTER)).append("\n");
				if (StringUtils.isNotBlank(chapter.getTitle())) {
					chaptersVtt.append(chapter.getTitle());
				} else {
					chaptersVtt.append(Messages.getString("Chapter")).append(" ").append(String.format("%02d", chaptersNum));
				}
				chaptersVtt.append("\n");
			}
		}
		return chaptersVtt.toString();
	}

	/**
	 * Return a HLS json representation of a dlna resource's chapters.
	 *
	 * @param dlna The dlna resource.
	 * @return The HLS json representation of the chapter list.
	 */
	public static String getHls(DLNAResource dlna) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		MediaInfo mediaVideo = dlna.getMedia();
		if (mediaVideo != null && mediaVideo.hasChapters()) {
			for (MediaChapter chapter : mediaVideo.getChapters()) {
				sb.append("{").append("\"start-time\": ").append(chapter.getStart()).append("},");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.append("]");
		return sb.toString();
	}
}
