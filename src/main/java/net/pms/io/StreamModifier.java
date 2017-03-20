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

	/**
	 * @deprecated Use {@link #isH264AnnexB()}.
	 */
	@Deprecated
	public boolean isH264_annexb() {
		return isH264AnnexB();
	}

	public boolean isH264AnnexB() {
		return h264AnnexB;
	}

	/**
	 * @deprecated Use {@link #setH264AnnexB(boolean)}.
	 */
	@Deprecated
	public void setH264_annexb(boolean h264AnnexB) {
		setH264AnnexB(h264AnnexB);
	}

	public void setH264AnnexB(boolean h264AnnexB) {
		this.h264AnnexB = h264AnnexB;
	}

	/**
	 * @deprecated Use {@link #isDtsEmbed()}.
	 */
	@Deprecated
	public boolean isDtsembed() {
		return isDtsEmbed();
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

	/**
	 * @deprecated Use {@link #setDtsEmbed(boolean)}.
	 */
	@Deprecated
	public void setDtsembed(boolean dtsEmbed) {
		setDtsEmbed(dtsEmbed);
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

	/**
	 * @deprecated Use {@link #getNbChannels()}.
	 */
	@Deprecated
	public int getNbchannels() {
		return getNbChannels();
	}

	public int getNbChannels() {
		return nbChannels;
	}

	/**
	 * @deprecated Use {@link #setNbChannels(int)}.
	 */
	@Deprecated
	public void setNbchannels(int nbChannels) {
		setNbChannels(nbChannels);
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

	/**
	 * @deprecated Use {@link #getBitsPerSample()}.
	 */
	@Deprecated
	public int getBitspersample() {
		return getBitsPerSample();
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	/**
	 * @deprecated Use {@link #setBitsPerSample(int)}.
	 */
	@Deprecated
	public void setBitspersample(int bitsPerSample) {
		setBitsPerSample(bitsPerSample);
	}

	public void setBitsPerSample(int bitsPerSample) {
		this.bitsPerSample = bitsPerSample;
	}
}
