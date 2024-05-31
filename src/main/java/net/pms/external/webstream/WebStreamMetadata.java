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
package net.pms.external.webstream;

public class WebStreamMetadata {

	private final String url;
	private final Integer type;

	private String logoUrl;
	private String genre;
	private String contentType;
	private Integer sampleRate;
	private Integer bitrate;

	public WebStreamMetadata(String url, Integer type) {
		this.url = url;
		this.type = type;
	}

	public WebStreamMetadata(String url,
			String logoUrl,
			String genre,
			String contentType,
			Integer sampleRate,
			Integer bitrate,
			Integer type) {
		this(url, type);
		this.logoUrl = logoUrl;
		this.genre = genre;
		this.contentType = contentType;
		this.sampleRate = sampleRate;
		this.bitrate = bitrate;
	}

	public String getUrl() {
		return url;
	}

	public Integer getType() {
		return type;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logo) {
		this.logoUrl = logo;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Integer getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(Integer sampleRate) {
		this.sampleRate = sampleRate;
	}

	public Integer getBitrate() {
		return bitrate;
	}

	public void setBitrate(Integer bitrate) {
		this.bitrate = bitrate;
	}

}
