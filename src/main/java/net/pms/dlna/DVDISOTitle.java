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
package net.pms.dlna;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.formats.FormatFactory;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DVDISOTitle extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(DVDISOTitle.class);
	private File f;
	private int title;
	private long length;

	@Override
	public void resolve() {
		String cmd[] = new String[]{PMS.getConfiguration().getMplayerPath(), "-identify", "-endpos", "0", "-v", "-ao", "null", "-vc", "null", "-vo", "null", "-dvd-device", ProcessUtil.getShortFileNameIfWideChars(f.getAbsolutePath()), "dvd://" + title};
		OutputParams params = new OutputParams(PMS.getConfiguration());
		params.maxBufferSize = 1;
		if (PMS.getConfiguration().isDvdIsoThumbnails()) {
			try {
				params.workDir = PMS.getConfiguration().getTempFolder();
			} catch (IOException e1) {
				LOGGER.debug("Caught exception", e1);
			}
			cmd[2] = "-frames";
			cmd[3] = "2";
			cmd[7] = "-quiet";
			cmd[8] = "-quiet";
			String frameName = "" + this.hashCode();
			frameName = "mplayer_thumbs:subdirs=\"" + frameName + "\"";
			frameName = frameName.replace(',', '_');
			cmd[10] = "jpeg:outdir=" + frameName;
		}
		params.log = true;
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmd, params, true, false);
		Runnable r = new Runnable() {

			public void run() {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				pw.stopProcess();
			}
		};
		Thread failsafe = new Thread(r, "DVD ISO Title Failsafe");
		failsafe.start();
		pw.runInSameThread();
		List<String> lines = pw.getOtherResults();

		String duration = null;
		int nbsectors = 0;
		String fps = null;
		String aspect = null;
		String width = null;
		String height = null;
		ArrayList<DLNAMediaAudio> audio = new ArrayList<DLNAMediaAudio>();
		ArrayList<DLNAMediaSubtitle> subs = new ArrayList<DLNAMediaSubtitle>();
		if (lines != null) {
			for (String line : lines) {
				if (line.startsWith("DVD start=")) {
					nbsectors = Integer.parseInt(line.substring(line.lastIndexOf("=") + 1).trim());
				}
				if (line.startsWith("audio stream:")) {
					DLNAMediaAudio lang = new DLNAMediaAudio();
					lang.setId(Integer.parseInt(line.substring(line.indexOf("aid: ") + 5, line.lastIndexOf(".")).trim()));
					lang.setLang(line.substring(line.indexOf("language: ") + 10, line.lastIndexOf(" aid")).trim());
					int end = line.lastIndexOf(" langu");
					if (line.lastIndexOf("(") < end && line.lastIndexOf("(") > line.indexOf("format: ")) {
						end = line.lastIndexOf("(");
					}
					lang.setCodecA(line.substring(line.indexOf("format: ") + 8, end).trim());
					if (line.contains("(stereo)")) {
						lang.setNrAudioChannels(2);
					} else {
						lang.setNrAudioChannels(6);
					}
					audio.add(lang);
				}
				if (line.startsWith("subtitle")) {
					DLNAMediaSubtitle lang = new DLNAMediaSubtitle();
					lang.setId(Integer.parseInt(line.substring(line.indexOf("): ") + 3, line.lastIndexOf("language")).trim()));
					lang.setLang(line.substring(line.indexOf("language: ") + 10).trim());
					if (lang.getLang().equals("unknown")) {
						lang.setLang(DLNAMediaLang.UND);
					}
					lang.setType(DLNAMediaSubtitle.EMBEDDED);
					subs.add(lang);
				}
				if (line.startsWith("ID_VIDEO_WIDTH=")) {
					width = line.substring(line.indexOf("ID_VIDEO_WIDTH=") + 15).trim();
				}
				if (line.startsWith("ID_VIDEO_HEIGHT=")) {
					height = line.substring(line.indexOf("ID_VIDEO_HEIGHT=") + 16).trim();
				}
				if (line.startsWith("ID_VIDEO_FPS=")) {
					fps = line.substring(line.indexOf("ID_VIDEO_FPS=") + 13).trim();
				}
				if (line.startsWith("ID_LENGTH=")) {
					duration = line.substring(line.indexOf("ID_LENGTH=") + 10).trim();
				}
				if (line.startsWith("ID_VIDEO_ASPECT=")) {
					aspect = line.substring(line.indexOf("ID_VIDEO_ASPECT=") + 16).trim();
				}
			}
		}

		if (PMS.getConfiguration().isDvdIsoThumbnails()) {
			try {
				String frameName = "" + this.hashCode();
				frameName = PMS.getConfiguration().getTempFolder() + "/mplayer_thumbs/" + frameName + "00000001/0000000";
				frameName = frameName.replace(',', '_');
				File jpg = new File(frameName + "2.jpg");
				if (jpg.exists()) {
					InputStream is = new FileInputStream(jpg);

					try {
						int sz = is.available();

						if (sz > 0) {
							getMedia().setThumb(new byte[sz]);
							is.read(getMedia().getThumb());
						}
					} finally {
						is.close();
					}

					if (!jpg.delete()) {
						jpg.deleteOnExit();
					}

					// Try and retry
					if (!jpg.getParentFile().delete() && !jpg.getParentFile().delete()) {
						LOGGER.debug("Failed to delete \"" + jpg.getParentFile().getAbsolutePath() + "\"");
					}
				}
				jpg = new File(frameName + "1.jpg");
				if (jpg.exists()) {
					if (!jpg.delete()) {
						jpg.deleteOnExit();
					}
					if (!jpg.getParentFile().delete()) {
						jpg.getParentFile().delete();
					}
				}
			} catch (IOException e) {
				LOGGER.trace("Error in DVD ISO thumbnail retrieval: " + e.getMessage());
			}
		}

		length = nbsectors * 2048;

		double d = 0;
		if (duration != null) {
			d = Double.parseDouble(duration);
		}

		getMedia().setAudioCodes(audio);
		getMedia().setSubtitlesCodes(subs);

		if (duration != null) {
			getMedia().setDuration(d);
		}
		getMedia().setFrameRate(fps);
		getMedia().setAspect(aspect);
		getMedia().setDvdtrack(title);
		getMedia().setContainer("iso");
		getMedia().setCodecV("mpeg2video");

		try {
			getMedia().setWidth(Integer.parseInt(width));
		} catch (NumberFormatException nfe) {
			LOGGER.debug("Could not parse width \"" + width + "\"");
		}

		try {
			getMedia().setHeight(Integer.parseInt(height));
		} catch (NumberFormatException nfe) {
			LOGGER.debug("Could not parse height \"" + height + "\"");
		}

		getMedia().setMediaparsed(true);

		super.resolve();
	}

	public long getLength() {
		return length;
	}

	public DVDISOTitle(File f, int title) {
		this.f = f;
		this.title = title;
		setLastmodified(f.lastModified());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return "Title " + title;
	}

	@Override
	public String getSystemName() {
		return f.getAbsolutePath();
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public boolean isValid() {
		if (getExt() == null) {
			setExt(FormatFactory.getAssociatedExtension("dummy.iso"));
		}
		return true;
	}

	@Override
	public long length() {
		return DLNAMediaInfo.TRANS_SIZE;
	}

	// Ditlew
	public long length(RendererConfiguration mediaRenderer) {
		// WDTV Live at least, needs a realistic size for stop/resume to works proberly. 2030879 = ((15000 + 256) * 1024 / 8 * 1.04) : 1.04 = overhead
		int cbr_video_bitrate = getDefaultRenderer().getCBRVideoBitrate();
		return (cbr_video_bitrate > 0) ? (long) (((cbr_video_bitrate + 256) * 1024 / 8 * 1.04) * getMedia().getDurationInSeconds()) : length();
	}

	@Override
	public InputStream getThumbnailInputStream() throws IOException {
		File cachedThumbnail = null;
		File thumbFolder = null;
		boolean alternativeCheck = false;
		while (cachedThumbnail == null) {
			if (thumbFolder == null) {
				thumbFolder = f.getParentFile();
			}
			cachedThumbnail = FileUtil.getFileNameWitNewExtension(thumbFolder, f, "jpg");
			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.getFileNameWitNewExtension(thumbFolder, f, "png");
			}
			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.getFileNameWitAddedExtension(thumbFolder, f, ".cover.jpg");
			}
			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.getFileNameWitAddedExtension(thumbFolder, f, ".cover.png");
			}
			if (alternativeCheck) {
				break;
			}
			if (StringUtils.isNotBlank(PMS.getConfiguration().getAlternateThumbFolder())) {
				thumbFolder = new File(PMS.getConfiguration().getAlternateThumbFolder());
				if (!thumbFolder.exists() || !thumbFolder.isDirectory()) {
					thumbFolder = null;
					break;
				}
			}
			alternativeCheck = true;
		}
		if (cachedThumbnail != null) {
			return new FileInputStream(cachedThumbnail);
		} else if (getMedia() != null && getMedia().getThumb() != null) {
			return getMedia().getThumbnailInputStream();
		} else {
			return getResourceInputStream("images/cdrwblank-256.png");
		}
	}
}
