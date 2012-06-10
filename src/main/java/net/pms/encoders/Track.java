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
package net.pms.encoders;

public class Track {
	private String compressor;
	private int scale;
	private int rate;
	private int sampleSize;
	private int bitspersample;
	private int nbaudio;

	public Track(String compressor, int scale, int rate, int sampleSize) {
		this.compressor = compressor;
		this.scale = scale;
		this.rate = rate;
		this.sampleSize = sampleSize;
	}

	private byte bih[];

	public byte[] getBih() {
		return bih;
	}

	public void setBih(byte[] bih) {
		this.bih = bih;
	}

	public String getCompressor() {
		return compressor;
	}

	public int getScale() {
		return scale;
	}

	public int getRate() {
		return rate;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public int getBitspersample() {
		return bitspersample;
	}

	public void setBitspersample(int bitspersample) {
		this.bitspersample = bitspersample;
	}

	public int getNbaudio() {
		return nbaudio;
	}

	public void setNbaudio(int nbaudio) {
		this.nbaudio = nbaudio;
	}
}
