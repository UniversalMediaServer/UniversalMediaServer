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
package net.pms.dlna;

import java.util.HashMap;
import java.util.Map;

/**
 * This class keeps track of the chapter properties of media.
 */
public class DLNAMediaChapter {
	private int id;
	private int parentId;
	private double start;
	private Map<String, String> metadatas;

	/**
	 * Returns the unique id for this chapter object
	 *
	 * @return The id.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets a unique id for this chapter object.
	 *
	 * @param id the id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

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
			metadatas = new HashMap();
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
	 * Sets the Id, start time and end time for this chapter object.
	 *
	 * @param line The line from ffmpeg.
	 */
	public void setChapterFromFfmpeg(String line) {
		if (line.contains("Chapter #")) {
			String idStr = line.substring(line.indexOf("Chapter #") + 9);
			if (idStr.contains(" ")) {
				idStr = idStr.substring(0, idStr.indexOf(" "));
			}
			String[] ids = idStr.split(":");
			if (ids.length > 1) {
				setParentId(Integer.valueOf(ids[0]));
				setId(Integer.valueOf(ids[0]));
			} else {
				setId(Integer.valueOf(ids[0]));
			}
		}
		if (line.contains("start ")) {
			String startStr = line.substring(line.indexOf("start ") + 6);
			if (startStr.contains(" ")) {
				startStr = startStr.substring(0, startStr.indexOf(" "));
			}
			setStart(Double.valueOf(startStr));
		}
	}
}
