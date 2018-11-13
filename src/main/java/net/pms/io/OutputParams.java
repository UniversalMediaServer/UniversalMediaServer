/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008 A.Brochard
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License only.
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
package net.pms.io;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.IPushOutput;

public class OutputParams {

	private File workDir;
	private Map<String, String> env;
	private double minFileSize;
	private double minBufferSize;
	private double maxBufferSize;
	private double timeseek;
	private double timeend;
	private int fromFrame;
	private int toFrame;
	private int waitbeforestart;
	private PipeProcess[] inputpipes = new PipeProcess[2];
	private PipeProcess[] outputpipes = new PipeProcess[2];
	private DLNAMediaAudio aid;
	private DLNAMediaSubtitle sid;
	private int secondread_minsize;
	private int outputByteArrayStreamBufferSize;
	private boolean noexitcheck;
	private boolean log;
	private boolean lossyaudio;
	private boolean losslessaudio;
	private boolean no_videoencode;
	private String forceFps;
	private String forceType;
	private RendererConfiguration mediaRenderer;
	private boolean hidebuffer;
	private byte header[];
	private IPushOutput stdin;
	private boolean avidemux;
	private boolean shift_scr;
	private boolean cleanup;

	public OutputParams(PmsConfiguration configuration) {
		if (configuration != null) {
			setWaitBeforeStart(configuration.getVideoTranscodeStartDelay() * 1000);
		} else {
			setWaitBeforeStart(6000);
		}
		setFromFrame(-1);
		setToFrame(-1);
		setSecondReadMinSize(1000000);
		if (configuration != null) {
			setMinFileSize(configuration.getMinStreamBuffer());
			setMinBufferSize(configuration.getMinMemoryBufferSize());
			setMaxBufferSize(configuration.getMaxMemoryBufferSize());
		}
		if (getMaxBufferSize() < 100) {
			setMaxBufferSize(100);
		}
		setTimeSeek(0);
		setEnv(null);
	}

	/**
	 * Set some values to allow fast streaming start of transcoded videos
	 */
	public void manageFastStart() {
		if (getMediaRenderer() != null && getMediaRenderer().isTranscodeFastStart()) {
			setWaitBeforeStart(0); // no delay when the transcode is starting
			setMinBufferSize(1); // 1Mb of minimum buffer before sending the
								 // file
		}
	}

	@Override
	public String toString() {
		return "OutputParams [aid=" + getAid() +
			", avidemux=" + isAvidemux() + 
			", cleanup=" + isCleanup() + 
			", forceFps=" + getForceFps()+ 
			", forceType=" + getForceType() + 
			", fromFrame=" + getFromFrame() + 
			", header=" + Arrays.toString(getHeader()) + 
			", hidebuffer=" + isHideBuffer() + 
			", input_pipes=" + Arrays.toString(getInputPipes()) + 
			", log=" + isLog() + 
			", losslessaudio=" + isLosslessAudio() + 
			", lossyaudio=" + isLossyAudio() + 
			", maxBufferSize=" + getMaxBufferSize()	+ 
			", mediaRenderer=" + getMediaRenderer() + 
			", minBufferSize=" + getMinBufferSize() + 
			", minFileSize=" + getMinFileSize()	+ 
			", no_videoencode=" + isNoVideoEncode() + 
			", outputByteArrayStreamBufferSize= " + getOutputByteArrayStreamBufferSize() + 
			", noexitcheck=" + isNoExitCheck() + 
			", output_pipes=" + Arrays.toString(getOutputPipes()) + 
			", secondread_minsize="	+ getSecondReadMinSize() + 
			", shift_scr=" + isShiftSscr() + 
			", sid=" + getSid() + 
			", stdin=" + getStdIn() + 
			", timeend=" + getTimeEnd() + 
			", timeseek=" + getTimeSeek() + 
			", toFrame=" + getToFrame() + 
			", waitbeforestart=" + getWaitBeforeStart()	+ 
			", workDir=" + getWorkDir() + 
			", env=" + getEnv() + "]";
	}

	public File getWorkDir() {
		return workDir;
	}

	public void setWorkDir(File workDir) {
		this.workDir = workDir;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public double getMinBufferSize() {
		return minBufferSize;
	}

	public void setMinBufferSize(double minBufferSize) {
		this.minBufferSize = minBufferSize;
	}

	public double getMinFileSize() {
		return minFileSize;
	}

	public void setMinFileSize(double minFileSize) {
		this.minFileSize = minFileSize;
	}

	public double getMaxBufferSize() {
		return maxBufferSize;
	}

	public void setMaxBufferSize(double maxBufferSize) {
		this.maxBufferSize = maxBufferSize;
	}

	public double getTimeSeek() {
		return timeseek;
	}

	public void setTimeSeek(double timeseek) {
		this.timeseek = timeseek;
	}

	public double getTimeEnd() {
		return timeend;
	}

	public void setTimeEnd(double timeend) {
		this.timeend = timeend;
	}

	public int getFromFrame() {
		return fromFrame;
	}

	public void setFromFrame(int fromFrame) {
		this.fromFrame = fromFrame;
	}

	public int getWaitBeforeStart() {
		return waitbeforestart;
	}

	public void setWaitBeforeStart(int waitbeforestart) {
		this.waitbeforestart = waitbeforestart;
	}

	public int getToFrame() {
		return toFrame;
	}

	public void setToFrame(int toFrame) {
		this.toFrame = toFrame;
	}

	public PipeProcess[] getOutputPipes() {
		return outputpipes;
	}

	public void setOutputPipes(PipeProcess[] outputpipes) {
		this.outputpipes = outputpipes;
	}

	public PipeProcess[] getInputPipes() {
		return inputpipes;
	}

	public void setInputPipes(PipeProcess[] inputpipes) {
		this.inputpipes = inputpipes;
	}

	public DLNAMediaAudio getAid() {
		return aid;
	}

	public void setAid(DLNAMediaAudio aid) {
		this.aid = aid;
	}

	public DLNAMediaSubtitle getSid() {
		return sid;
	}

	public void setSid(DLNAMediaSubtitle sid) {
		this.sid = sid;
	}

	public int getOutputByteArrayStreamBufferSize() {
		return outputByteArrayStreamBufferSize;
	}

	public void setOutputByteArrayStreamBufferSize(int outputByteArrayStreamBufferSize) {
		this.outputByteArrayStreamBufferSize = outputByteArrayStreamBufferSize;
	}

	public int getSecondReadMinSize() {
		return secondread_minsize;
	}

	public void setSecondReadMinSize(int secondread_minsize) {
		this.secondread_minsize = secondread_minsize;
	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}

	public boolean isNoExitCheck() {
		return noexitcheck;
	}

	public void setNoExitCheck(boolean noexitcheck) {
		this.noexitcheck = noexitcheck;
	}

	public boolean isLosslessAudio() {
		return losslessaudio;
	}

	public void setLosslessAudio(boolean losslessaudio) {
		this.losslessaudio = losslessaudio;
	}

	public boolean isLossyAudio() {
		return lossyaudio;
	}

	public void setLossyAudio(boolean lossyaudio) {
		this.lossyaudio = lossyaudio;
	}

	public boolean isNoVideoEncode() {
		return no_videoencode;
	}

	public void setNoVideoEncode(boolean no_videoencode) {
		this.no_videoencode = no_videoencode;
	}

	public String getForceFps() {
		return forceFps;
	}

	public void setForceFps(String forceFps) {
		this.forceFps = forceFps;
	}

	public String getForceType() {
		return forceType;
	}

	public void setForceType(String forceType) {
		this.forceType = forceType;
	}

	public RendererConfiguration getMediaRenderer() {
		return mediaRenderer;
	}

	public void setMediaRenderer(RendererConfiguration mediaRenderer) {
		this.mediaRenderer = mediaRenderer;
	}

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(byte header[]) {
		this.header = header;
	}

	public boolean isHideBuffer() {
		return hidebuffer;
	}

	public void setHideBuffer(boolean hidebuffer) {
		this.hidebuffer = hidebuffer;
	}

	public IPushOutput getStdIn() {
		return stdin;
	}

	public void setStdIn(IPushOutput stdin) {
		this.stdin = stdin;
	}

	public boolean isAvidemux() {
		return avidemux;
	}

	public void setAvidemux(boolean avidemux) {
		this.avidemux = avidemux;
	}

	public boolean isShiftSscr() {
		return shift_scr;
	}

	public void setShiftScr(boolean shift_scr) {
		this.shift_scr = shift_scr;
	}

	public boolean isCleanup() {
		return cleanup;
	}

	public void setCleanup(boolean cleanup) {
		this.cleanup = cleanup;
	}
}
