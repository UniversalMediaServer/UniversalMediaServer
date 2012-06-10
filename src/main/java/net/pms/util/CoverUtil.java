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

package net.pms.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;

import net.pms.network.HTTPResource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoverUtil extends HTTPResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(CoverUtil.class);

	/**
	 * Use www.discogs.com as backend for cover lookups.
	 */
	public static final int AUDIO_DISCOGS = 0;

	/**
	 * Use www.amazon.com as backend for cover lookups.
	 */
	public static final int AUDIO_AMAZON = 1;

	/**
	 * Class instance.
	 */
	private static CoverUtil instance;

	/**
	 * Storage for cover information (artist + album and thumbnail data).
	 * FIXME: Storing the thumbnail image data in memory is very costly.
	 * It would be wiser to store the image as a file instead.
	 */
	private HashMap<String, byte[]> covers;

	/**
	 * Returns the instance of the CoverUtil class.
	 *
	 * @return The class instance.
	 */
	public static synchronized CoverUtil get() {
		if (instance == null) {
			instance = new CoverUtil();
		}
		return instance;
	}

	/**
	 * This class is not meant to be instantiated. Use {@link #get()} instead.
	 */
	private CoverUtil() {
		covers = new HashMap<String, byte[]>();
	}

	/**
	 * Tries to look up a thumbnail based on artist and album information from a
	 * given backend and returns the image data on success or <code>null</code>
	 * if no thumbnail could be determined.
	 * 
	 * @param backend
	 *            The backend to use for thumbnail lookup. Can be
	 *            {@link #AUDIO_AMAZON} or {@link #AUDIO_DISCOGS}.
	 * @param info The name of the artist and the album.
	 *
	 * @return The thumbnail image data or <code>null</code>.
	 * @throws IOException Thrown when downloading the thumbnail fails.
	 */
	public synchronized byte[] getThumbnailFromArtistAlbum(int backend, String... info) throws IOException {
		if (info.length >= 2 && StringUtils.isNotBlank(info[0]) && StringUtils.isNotBlank(info[1])) {
			String artist = URLEncoder.encode(info[0], "UTF-8");
			String album = URLEncoder.encode(info[1], "UTF-8");

			if (covers.get(artist + album) != null) {
				byte[] data = covers.get(artist + album);

				if (data.length == 0) {
					return null;
				} else {
					return data;
				}
			}

			if (backend == AUDIO_DISCOGS) {
				String url = "http://www.discogs.com/advanced_search?artist=" + artist 
						+ "&release_title=" + album + "&btn=Search+Releases";
				byte[] data = downloadAndSendBinary(url);

				if (data != null) {
					try {
						String html = new String(data, "UTF-8");
						int firstItem = html.indexOf("<li style=\"background:");

						if (firstItem > -1) {
							String detailUrl = html.substring(html.indexOf("<a href=\"/", firstItem) + 10,
									html.indexOf("\"><em>", firstItem));
							data = downloadAndSendBinary("http://www.discogs.com/" + detailUrl);
							html = new String(data, "UTF-8");
							firstItem = html.indexOf("<a href=\"/viewimages?");

							if (firstItem > -1) {
								String imageUrl = html.substring(html.indexOf("<img src=\"", firstItem) + 10,
										html.indexOf("\" border", firstItem));
								data = downloadAndSendBinary(imageUrl);

								if (data != null) {
									covers.put(artist + album, data);
								} else {
									covers.put(artist + album, new byte[0]);
								}
								return data;
							}
						}
					} catch (IOException e) {
						LOGGER.error("Error while retrieving cover for " + artist + album, e);
					}
				}
			} else if (backend == AUDIO_AMAZON) {
				String url = "http://www.amazon.com/gp/search/ref=sr_adv_m_pop/?search-alias=popular&unfiltered=1&field-keywords=&field-artist="
						+ artist + "&field-title=" + album 
						+ "&field-label=&field-binding=&sort=relevancerank&Adv-Srch-Music-Album-Submit.x=35&Adv-Srch-Music-Album-Submit.y=13";
				byte[] data = downloadAndSendBinary(url);

				if (data != null) {
					try {
						String html = new String(data, "UTF-8");
						int firstItem = html.indexOf("class=\"imageColumn\"");

						if (firstItem > -1) {
							int imageUrlPos = html.indexOf("src=\"", firstItem) + 5;
							String imageUrl = html.substring(imageUrlPos, html.indexOf("\" class", imageUrlPos));
							data = downloadAndSendBinary(imageUrl);

							if (data != null) {
								covers.put(artist + album, data);
							} else {
								covers.put(artist + album, new byte[0]);
							}
							return data;
						}
					} catch (IOException e) {
						LOGGER.error("Error while retrieving cover for " + artist + album, e);
					}
				}
			}
			covers.put(artist + album, new byte[0]);
		}
		return null;
	}
}
