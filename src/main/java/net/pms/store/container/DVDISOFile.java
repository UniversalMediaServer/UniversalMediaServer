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
package net.pms.store.container;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.pms.database.MediaTableFiles;
import net.pms.formats.Format;
import net.pms.parsers.MPlayerParser;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.item.DVDISOTitle;
import org.apache.commons.lang3.StringUtils;

public class DVDISOFile extends StoreContainer {
	private static final String NAME = "[DVD ISO] %s";

	private final File file;
	private final boolean isVideoTS;

	private String volumeId;

	public DVDISOFile(Renderer renderer, File file) {
		super(renderer, getName(file), "images/store/optical-disc.png");
		this.file = file;

		/*
		 * XXX this is the logic used in the old (pre 1.90.0) getDisplayName override,
		 * though it should probably be:
		 *
		 *     this.isVideoTS = file.isDirectory() && file.getName().toUpperCase().equals("VIDEO_TS");
		 */
		isVideoTS = file.getName().equalsIgnoreCase("VIDEO_TS");

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
	public String getFileName() {
		return getFileName(file);
	}

	@Override
	public String getName() {
		if (StringUtils.isNotBlank(volumeId)) {
			if (renderer.getUmsConfiguration().isPrettifyFilenames()) {
				return "[DVD] " + volumeId;
			}
			return super.getName() + " (" + volumeId + ")";
		}
		return super.getName();
	}

	@Override
	public String getSystemName() {
		return file == null ? "Unknown" : file.getAbsolutePath();
	}

	@Override
	protected void resolveOnce() {
		Map<Integer, Double> titles = new HashMap<>();
		volumeId = MPlayerParser.parseIsoFile(file, titles);

		double oldduration = -1;

		for (int i = 1; i < 99; i++) {
			/**
			 * Don't take into account titles less than 10 seconds
			 * Also, workaround for the MPlayer bug which reports a unique title with the same length several times
			 * The "maybe wrong" title is taken into account only if its duration is less than 1 hour.
			 * Common-sense is a single video track on a DVD is usually greater than 1h
			 */
			Double duration = titles.get(i);
			if (duration != null && duration > 10 && (duration != oldduration || oldduration < 3600)) {
				DVDISOTitle dvd = new DVDISOTitle(renderer, file, volumeId, i);
				addChild(dvd);
				oldduration = duration;
			}
		}

		if (childrenCount() > 0) {
			MediaTableFiles.getOrInsertFileId(file.getAbsolutePath(), file.lastModified(), Format.ISO);
		}

	}

	private static String getName(File file) {
		return String.format(NAME, getFileName(file));
	}

	// FIXME the logic here (all folders are VIDEO_TS folders) isn't
	// consistent with the logic used to set isVideoTS
	private static String getFileName(File file) {
		return file.isFile() ? file.getName() : "VIDEO_TS";
	}

}
