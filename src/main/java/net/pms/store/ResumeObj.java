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
package net.pms.store;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.util.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeObj {

	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(ResumeObj.class);
	private static final int DAYS = 3600 * 24 * 1000;

	public static final String CLEAN_REG = "_hash_(\\d+)";

	private final File file;
	private long offsetTime;
	private long resDuration;
	private long minDur;

	public ResumeObj(File f) {
		offsetTime = 0;
		resDuration = 0;
		file = f;
		minDur = CONFIGURATION.getMinimumWatchedPlayTime();
	}

	public void setMinDuration(long dur) {
		if (dur == 0) {
			dur = CONFIGURATION.getMinimumWatchedPlayTime();
		}
		minDur = dur;
	}

	public void read() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String str;
			while ((str = in.readLine()) != null) {
				String[] tmp = str.split(",");
				offsetTime = Long.parseLong(tmp[0]);
				if (tmp.length > 1) {
					resDuration = Long.parseLong(tmp[1]);
				}
				break;
			}
		} catch (IOException e) {
		}
	}

	public File getResumeFile() {
		return file;
	}

	public boolean noResume() {
		return (offsetTime == 0);
	}

	public long getTimeOffset() {
		if (isDone()) {
			return 0;
		}
		read();
		return offsetTime;
	}

	public boolean isDone() {
		return !file.exists();
	}

	public void update(TimeRange range, StoreResource r) {
		if (range.isStartOffsetAvailable() && range.getStartOrZero() > 0.0) {
			long now = System.currentTimeMillis();
			if (r.getMediaInfo() != null) {
				stop(now + getTimeOffset() - (long) (range.getStart() * 1000), (long) (r.getMediaInfo().getDuration() * 1000));
			} else {
				stop(now + getTimeOffset() - (long) (range.getStart() * 1000), 0);
			}
		}
	}

	public void stop(long startTime, long expDuration) {
		long now = System.currentTimeMillis();
		long thisPlay = now - startTime;
		long duration = thisPlay + offsetTime;

		if (expDuration > minDur && duration >= (expDuration * CONFIGURATION.getResumeBackFactor())) {
			// We've seen the whole video (likely)
			file.delete();
			return;
		}
		if (thisPlay < CONFIGURATION.getResumeRewind()) {
			return;
		}
		if (thisPlay < minDur) {
			// too short to resume (at all)
			return;
		}

		offsetTime = duration - CONFIGURATION.getResumeRewind();
		resDuration = expDuration;
		LOGGER.debug("Resume stop. This segment " + thisPlay + " new time " + duration);
		write(offsetTime, expDuration, file);
	}

	private static File resumePath() {
		File path = new File(CONFIGURATION.getDataFile("resume"));
		path.mkdirs();
		return path;
	}

	private static File resumeFile(StoreItem r) {
		String wName = r.getName().replaceAll("[:\\[\\]\n\r]", "").trim();
		String fName = wName + "_hash_" + r.resumeHash() + ".resume";
		return new File(resumePath(), fName);
	}

	public static File[] resumeFiles() {
		File path = resumePath();
		return path.listFiles();
	}

	/**
	 * Creates a "Resume" version of the incoming resource, which is a video
	 * that has a particular starting point past the beginning.
	 *
	 * @param originalResource
	 * @return
	 */
	public static ResumeObj create(StoreItem originalResource) {
		// resume is off bail early
		if (!originalResource.renderer.getUmsConfiguration().isResumeEnabled()) {
			return null;
		}

		// no file no resume
		File resumeFile = resumeFile(originalResource);
		if (!resumeFile.exists()) {
			return null;
		}

		ResumeObj res = new ResumeObj(resumeFile);
		res.read();
		if (res.noResume()) {
			return null;
		}

		if (originalResource.getMediaInfo() != null) {
			double dur = originalResource.getMediaInfo().getDurationInSeconds();
			if (dur == 0.0 || dur == StoreResource.TRANS_SIZE) {
				//TODO : investigate why setting original resource mediaInfo
				//duration seems never been set to TRANS_SIZE
				originalResource.getMediaInfo().setDuration(res.resDuration / 1000.0);
			}
		}

		res.setMinDuration(originalResource.minPlayTime());

		return res;
	}

	public static ResumeObj store(StoreItem r, long startTime) {
		File f = resumeFile(r);
		ResumeObj obj = new ResumeObj(f);
		obj.setMinDuration(r.minPlayTime());
		obj.stop(startTime, (long) r.getMediaInfo().getDurationInSeconds() * 1000);
		if (obj.noResume()) {
			return null;
		}
		return obj;
	}

	private static void write(long time, long duration, File f) {
		try {
			try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
				out.write(time + "," + duration);
				out.flush();
				out.close();
				if (CONFIGURATION.getResumeKeepTime() > 0) {
					PMS.get().addTempFile(f, CONFIGURATION.getResumeKeepTime() * DAYS);
				}
			}
		} catch (IOException e) {
		}
	}

}
