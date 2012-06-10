package net.pms.io;

public class StreamModifier {
	private byte header[];
	private boolean h264_annexb;

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(byte[] header) {
		this.header = header;
	}

	public boolean isH264_annexb() {
		return h264_annexb;
	}

	public void setH264_annexb(boolean h264_annexb) {
		this.h264_annexb = h264_annexb;
	}

	private boolean dtsembed;

	public boolean isDtsembed() {
		return dtsembed;
	}

	public void setDtsembed(boolean dtsembed) {
		this.dtsembed = dtsembed;
	}

	private boolean pcm;
	private int nbchannels;
	private int sampleFrequency;
	private int bitspersample;

	public boolean isPcm() {
		return pcm;
	}

	public void setPcm(boolean pcm) {
		this.pcm = pcm;
	}

	public int getNbchannels() {
		return nbchannels;
	}

	public void setNbchannels(int nbchannels) {
		this.nbchannels = nbchannels;
	}

	public int getSampleFrequency() {
		return sampleFrequency;
	}

	public void setSampleFrequency(int sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	public int getBitspersample() {
		return bitspersample;
	}

	public void setBitspersample(int bitspersample) {
		this.bitspersample = bitspersample;
	}
}
