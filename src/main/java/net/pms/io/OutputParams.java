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
	private PipeProcess[] input_pipes = new PipeProcess[2];
	private PipeProcess[] output_pipes = new PipeProcess[2];
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
			setWaitbeforestart(configuration.getVideoTranscodeStartDelay() * 1000);
		} else {
			setWaitbeforestart(6000);
		}
		setFromFrame(-1);
		setToFrame(-1);
		setSecondread_minsize(1000000);
		if (configuration != null) {
			setMinFileSize(configuration.getMinStreamBuffer());
			setMinBufferSize(configuration.getMinMemoryBufferSize());
			setMaxBufferSize(configuration.getMaxMemoryBufferSize());
		}
		if (getMaxBufferSize() < 100) {
			setMaxBufferSize(100);
		}
		setTimeseek(0);
		setEnv(null);
	}

	/**
	 * Set some values to allow fast streaming start of transcoded videos
	 */
	public void manageFastStart() {
		if (getMediaRenderer() != null && getMediaRenderer().isTranscodeFastStart()) {
			setWaitbeforestart(0); // no delay when the transcode is starting
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
			", hidebuffer=" + isHidebuffer() + 
			", input_pipes=" + Arrays.toString(getInput_pipes()) + 
			", log=" + isLog() + 
			", losslessaudio=" + isLosslessaudio() + 
			", lossyaudio=" + isLossyaudio() + 
			", maxBufferSize=" + getMaxBufferSize()	+ 
			", mediaRenderer=" + getMediaRenderer() + 
			", minBufferSize=" + getMinBufferSize() + 
			", minFileSize=" + getMinFileSize()	+ 
			", no_videoencode=" + isNoVideoEncode() + 
			", outputByteArrayStreamBufferSize= " + getOutputByteArrayStreamBufferSize() + 
			", noexitcheck=" + isNoexitcheck() + 
			", output_pipes=" + Arrays.toString(getOutput_pipes()) + 
			", secondread_minsize="	+ getSecondread_minsize() + 
			", shift_scr=" + isShift_scr() + 
			", sid=" + getSid() + 
			", stdin=" + getStdin() + 
			", timeend=" + getTimeend() + 
			", timeseek=" + getTimeseek() + 
			", toFrame=" + getToFrame() + 
			", waitbeforestart=" + getWaitbeforestart()	+ 
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

	public double getTimeseek() {
		return timeseek;
	}

	public void setTimeseek(double timeseek) {
		this.timeseek = timeseek;
	}

	public double getTimeend() {
		return timeend;
	}

	public void setTimeend(double timeend) {
		this.timeend = timeend;
	}

	public int getFromFrame() {
		return fromFrame;
	}

	public void setFromFrame(int fromFrame) {
		this.fromFrame = fromFrame;
	}

	public int getWaitbeforestart() {
		return waitbeforestart;
	}

	public void setWaitbeforestart(int waitbeforestart) {
		this.waitbeforestart = waitbeforestart;
	}

	public int getToFrame() {
		return toFrame;
	}

	public void setToFrame(int toFrame) {
		this.toFrame = toFrame;
	}

	public PipeProcess[] getOutput_pipes() {
		return output_pipes;
	}

	public void setOutput_pipes(PipeProcess[] output_pipes) {
		this.output_pipes = output_pipes;
	}

	public PipeProcess[] getInput_pipes() {
		return input_pipes;
	}

	public void setInput_pipes(PipeProcess[] input_pipes) {
		this.input_pipes = input_pipes;
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

	public int getSecondread_minsize() {
		return secondread_minsize;
	}

	public void setSecondread_minsize(int secondread_minsize) {
		this.secondread_minsize = secondread_minsize;
	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}

	public boolean isNoexitcheck() {
		return noexitcheck;
	}

	public void setNoexitcheck(boolean noexitcheck) {
		this.noexitcheck = noexitcheck;
	}

	public boolean isLosslessaudio() {
		return losslessaudio;
	}

	public void setLosslessaudio(boolean losslessaudio) {
		this.losslessaudio = losslessaudio;
	}

	public boolean isLossyaudio() {
		return lossyaudio;
	}

	public void setLossyaudio(boolean lossyaudio) {
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

	public boolean isHidebuffer() {
		return hidebuffer;
	}

	public void setHidebuffer(boolean hidebuffer) {
		this.hidebuffer = hidebuffer;
	}

	public IPushOutput getStdin() {
		return stdin;
	}

	public void setStdin(IPushOutput stdin) {
		this.stdin = stdin;
	}

	public boolean isAvidemux() {
		return avidemux;
	}

	public void setAvidemux(boolean avidemux) {
		this.avidemux = avidemux;
	}

	public boolean isShift_scr() {
		return shift_scr;
	}

	public void setShift_scr(boolean shift_scr) {
		this.shift_scr = shift_scr;
	}

	public boolean isCleanup() {
		return cleanup;
	}

	public void setCleanup(boolean cleanup) {
		this.cleanup = cleanup;
	}
}
