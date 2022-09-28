/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.io;

public class StreamModifier {
	private byte[] header;
	private boolean h264AnnexB;
	private boolean pcm;
	private int nbChannels;
	private int sampleFrequency;
	private int bitsPerSample;
	private boolean dtsEmbed;

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(byte[] header) {
		this.header = header;
	}

	public boolean isH264AnnexB() {
		return h264AnnexB;
	}

	public void setH264AnnexB(boolean h264AnnexB) {
		this.h264AnnexB = h264AnnexB;
	}

	public boolean isDtsEmbed() {
		return dtsEmbed;
	}

	private boolean spdifembed;

	public boolean isEncodedAudioPassthrough() {
		return spdifembed;
	}

	public void setEncodedAudioPassthrough(boolean spdifembed) {
		this.spdifembed = spdifembed;
	}

	public void setDtsEmbed(boolean dtsEmbed) {
		this.dtsEmbed = dtsEmbed;
	}

	public boolean isPcm() {
		return pcm;
	}

	public void setPcm(boolean pcm) {
		this.pcm = pcm;
	}

	public int getNbChannels() {
		return nbChannels;
	}

	public void setNbChannels(int nbChannels) {
		this.nbChannels = nbChannels;
	}

	public int getSampleFrequency() {
		return sampleFrequency;
	}

	public void setSampleFrequency(int sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public void setBitsPerSample(int bitsPerSample) {
		this.bitsPerSample = bitsPerSample;
	}
}
