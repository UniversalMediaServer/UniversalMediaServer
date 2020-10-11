package net.pms.io;

public class StreamModifier {
	private byte header[];
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
