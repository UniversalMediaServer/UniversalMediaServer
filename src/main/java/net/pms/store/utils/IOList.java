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
package net.pms.store.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.pms.PMS;
import net.pms.encoders.TranscodingSettings;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.renderers.Renderer;
import net.pms.store.ResumeObj;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.item.RealFile;
import net.pms.store.item.SevenZipEntry;
import net.pms.store.item.WebStream;
import net.pms.store.item.ZippedEntry;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A StoreResource list with built-in file i/o.
 */
public class IOList extends ArrayList<StoreResource> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IOList.class);

	/**
	 * Bitwise constants relating to playlist management.
	 */
	public static final int PERMANENT = 1;
	public static final int AUTOSAVE = 2;
	public static final int AUTOREMOVE = 4;
	private static final long serialVersionUID = 8042924548275374060L;
	private final Renderer renderer;
	private final int mode;
	private File file;

	public IOList(Renderer renderer, String uri, int mode) {
		this.renderer = renderer;
		this.mode = mode;
		if (!StringUtils.isBlank(uri)) {
			file = new File(uri);
			load(file);
		} else {
			file = null;
		}
	}

	@Override
	public boolean add(StoreResource d) {
		super.add(d);
		if (isMode(AUTOSAVE)) {
			save();
		}
		return true;
	}

	@Override
	public StoreResource remove(int index) {
		StoreResource d = super.remove(index);
		if (isMode(AUTOSAVE)) {
			save();
		}
		return d;
	}

	public boolean isMode(int m) {
		return (mode & m) == m;
	}

	public File getFile() {
		return file;
	}

	public final void load(File f) {
		if (f.exists()) {
			file = f;
			clear();
			try {
				read(this, f);
			} catch (IOException e) {
				LOGGER.debug("Error loading resource list '{}': {}", f, e);
			}
			if (PMS.getConfiguration().getSortMethod(f) == StoreResourceSorter.SORT_RANDOM) {
				Collections.shuffle(this);
			}
		}
	}

	public void save() {
		if (file != null) {
			save(file);
		}
	}

	public void save(File f) {
		if (!isEmpty()) {
			try {
				write(this, f);
			} catch (IOException e) {
				LOGGER.debug("Error saving resource list to '{}': {}", f, e);
			}
		} else if (f != null && f.exists()) {
			// Save = delete for empty playlists
			f.delete();
		}
	}

	public static void write(List<StoreResource> playlist, File f) throws IOException {
		Date now = new Date();
		try (FileWriter out = new FileWriter(f)) {
			StringBuilder sb = new StringBuilder();
			sb.append("######\n");
			sb.append("## __UPS__\n");
			sb.append("## NOTE!!!!!\n");
			sb.append("## This file is auto generated\n");
			sb.append("## Edit with EXTREME care\n");
			sb.append("## Generated: ");
			sb.append(now.toString());
			sb.append("\n");
			for (StoreResource r : playlist) {
				String data = r.write();
				if (!StringUtils.isEmpty(data) && sb.indexOf(data) == -1) {
					String id = "internal:" + r.getClass().getName();

					sb.append("master:").append(id).append(';');
					if (r instanceof StoreItem item) {
						if (item.isTranscoded()) {
							sb.append("trancoding:").append(item.getTranscodingSettings().getId()).append(';');
						}
						if (item.isResume()) {
							sb.append("resume");
							sb.append(item.getResume().getResumeFile().getAbsolutePath());
							sb.append(';');
						}
						if (item.getMediaSubtitle() != null) {
							MediaSubtitle sub = item.getMediaSubtitle();
							if (sub.getLang() != null &&
									((sub.isExternal() &&
									sub.getExternalFile() != null) || (sub.isEmbedded() &&
									sub.getId() != -1))) {
								sb.append("sub");
								sb.append(sub.getLang());
								sb.append(',');
								if (sub.isExternal()) {
									sb.append("file:");
									sb.append(sub.getExternalFile().getPath());
								} else {
									sb.append("id:");
									sb.append("").append(sub.getId());
								}
								sb.append(';');
							}
						}
					}
					sb.append(data);
					sb.append("\n");
				}
			}
			out.write(sb.toString());
			out.flush();
		}
	}

	private StoreItem parse(String clazz, String data) {
		boolean error = false;
		if (clazz.contains("RealFile")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				return new RealFile(renderer, new File(tmp[1]), tmp[0]);
			}
			error = true;
		}
		if (clazz.contains("SevenZipEntry")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				long len = Long.parseLong(tmp[2]);
				return new SevenZipEntry(renderer, new File(tmp[1]), tmp[0], len);
			}
			error = true;
		}
		if (clazz.contains("ZippedEntry")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				long len = Long.parseLong(tmp[2]);
				return new ZippedEntry(renderer, new File(tmp[1]), tmp[0], len);
			}
			error = true;
		}
		if (clazz.contains("WebStream")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				int type;
				try {
					type = Integer.parseInt(tmp[3]);
				} catch (NumberFormatException e) {
					type = Format.UNKNOWN;
				}
				return new WebStream(renderer, tmp[0], tmp[1], tmp[2], type, null);
			}
			error = true;
		}

		if (error) {
			LOGGER.debug("Error parsing playlist resource:");
			LOGGER.debug("clazz: " + clazz);
			LOGGER.debug("data:" + data);
		}

		return null;
	}

	public void read(List<StoreResource> playlist, File f) throws IOException {
		if (!f.exists()) {
			return;
		}
		try (BufferedReader in = new BufferedReader(new FileReader(f))) {
			String str;

			while ((str = in.readLine()) != null) {
				if (org.apache.commons.lang.StringUtils.isEmpty(str)) {
					continue;
				}
				if (str.startsWith("#")) {
					continue;
				}
				str = str.trim();
				if (!str.startsWith("master:")) {
					continue;
				}
				str = str.substring(7);
				int pos = str.indexOf(';');
				if (pos == -1) {
					continue;
				}
				String master = str.substring(0, pos);
				str = str.substring(pos + 1);
				pos = str.indexOf(';');
				String subData = null;
				String resData = null;
				StoreItem res = null;
				TranscodingSettings transcodingSettings = null;
				while (pos != -1) {
					if (str.startsWith("trancoding:")) {
						// find last transcoding settings
						transcodingSettings = TranscodingSettings.getTranscodingSettings(str.substring(7, pos));
					}
					if (str.startsWith("resume")) {
						// resume data
						resData = str.substring(6, pos);
					}
					if (str.startsWith("sub")) {
						// subs data
						subData = str.substring(3, pos);
					}
					str = str.substring(pos + 1);
					pos = str.indexOf(';');
				}
				LOGGER.debug("master is " + master + " str " + str);
				if (master.startsWith("internal:")) {
					res = parse(master.substring(9), str);
				} else {
					LOGGER.warn("Unknown master parents: {}", master);
				}
				if (res != null) {
					if (resData != null) {
						ResumeObj r = new ResumeObj(new File(resData));
						if (!r.isDone()) {
							r.read();
							res.setResume(r);
						}
					}
					res.setTranscodingSettings(transcodingSettings);
					if (subData != null) {
						MediaSubtitle s = res.getMediaSubtitle();
						if (s == null) {
							s = new MediaSubtitle();
							res.setMediaSubtitle(s);
						}
						String[] tmp = subData.split(",");
						s.setLang(tmp[0]);
						subData = tmp[1];
						if (subData.startsWith("file:")) {
							String sFile = subData.substring(5);
							s.setExternalFile(new File(sFile));
							SubtitleType t = SubtitleType.valueOfFileExtension(FileUtil.getExtension(sFile));
							s.setType(t);
						} else if (subData.startsWith("id:")) {
							s.setId(Integer.parseInt(subData.substring(3)));
						}
					}
					playlist.add(res);
				}
			}
		}
	}
}
