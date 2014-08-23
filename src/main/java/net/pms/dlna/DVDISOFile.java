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
import java.util.List;
import java.util.Map;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.ProcessUtil;

public class DVDISOFile extends VirtualFolder {
	private static final String NAME = "[DVD ISO] %s";
	private File file;
	private boolean isVideoTS;

	private static String getName(File file) {
		return String.format(NAME, getFilename(file));
	}

	// FIXME the logic here (all folders are VIDEO_TS folders) isn't
	// consistent with the logic used to set isVideoTS
	private static String getFilename(File file) {
		return file.isFile() ? file.getName() : "VIDEO_TS";
	}

	public String getFilename() {
		return getFilename(file);
	}

	public DVDISOFile(File file) {
		super(getName(file), null);
		this.file = file;

		/*
		 * XXX this is the logic used in the old (pre 1.90.0) getDisplayName override,
		 * though it should probably be:
		 *
		 *     this.isVideoTS = file.isDirectory() && file.getName().toUpperCase().equals("VIDEO_TS");
		 */
		isVideoTS = file.getName().toUpperCase().equals("VIDEO_TS");

		setLastModified(file.lastModified());
	}

	protected void finalizeDisplayNameVars(Map<String, Object> vars) {
		if (isVideoTS) {
			vars.put("isVTS", true);

			File dvdFolder = file.getParentFile();
			if (dvdFolder != null) {
				vars.put("vtsDVD", dvdFolder.getName());
			}
		}
	}

	@Override
	protected void resolveOnce() {
		double titles[] = new double[100];
		String cmd[] = new String[]{
			configuration.getMplayerPath(),
			"-identify",
			"-endpos",
			"0",
			"-v",
			"-ao",
			"null",
			"-vc",
			"null",
			"-vo",
			"null",
			"-dvd-device",
			ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath()),
			"dvd://1"
		};
		OutputParams params = new OutputParams(configuration);
		params.maxBufferSize = 1;
		params.log = true;
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmd, params, true, false);
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				pw.stopProcess();
			}
		};
		Thread failsafe = new Thread(r, "DVDISO Failsafe");
		failsafe.start();
		pw.runInSameThread();
		List<String> lines = pw.getOtherResults();
		if (lines != null) {
			for (String line : lines) {
				if (line.startsWith("ID_DVD_TITLE_") && line.contains("_LENGTH")) {
					int rank = Integer.parseInt(line.substring(13, line.indexOf("_LENGT")));
					double duration = Double.parseDouble(line.substring(line.lastIndexOf("LENGTH=") + 7));
					titles[rank] = duration;
				}
			}
		}

		double oldduration = -1;

		for (int i = 1; i < 99; i++) {
			/**
			 * Don't take into account titles less than 10 seconds
			 * Also, workaround for the MPlayer bug which reports a unique title with the same length several times
			 * The "maybe wrong" title is taken into account only if its duration is less than 1 hour.
			 * Common-sense is a single video track on a DVD is usually greater than 1h
			 */
			if (titles[i] > 10 && (titles[i] != oldduration || oldduration < 3600)) {
				DVDISOTitle dvd = new DVDISOTitle(file, i);
				addChild(dvd);
				oldduration = titles[i];
			}
		}

		if (childrenNumber() > 0) {
			PMS.get().storeFileInCache(file, Format.ISO);
		}

	}
}
