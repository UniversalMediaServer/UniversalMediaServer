package net.pms.dlna;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeObj {
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(ResumeObj.class);
	private static final int DAYS = 3600 * 24 * 1000;
	
	public static final String CLEAN_REG = "_hash_(\\d+)";

	private File file;
	private long offsetTime;
	private long resDuration;
	private long minDur;

	private static File resumePath() {
		File path = new File(configuration.getDataFile("resume"));
		path.mkdirs();
		return path;
	}

	private static File resumeFile(DLNAResource r) {
		String wName = r.getName().replaceAll("[:\\[\\]\n\r]", "").trim();
		String fName = wName + "_hash_" + r.resumeHash() + ".resume";
		return new File(resumePath().getAbsolutePath() + File.separator + fName);
	}

	public static File[] resumeFiles() {
		File path = resumePath();
		return path.listFiles();
	}

	public static ResumeObj create(DLNAResource r) {
		if (!configuration.isResumeEnabled()) {
			// resume is off bail early
			return null;
		}
		File f = resumeFile(r);
		if (!f.exists()) {
			// no file no resume
			return null;
		}
		ResumeObj res = new ResumeObj(f);
		res.read();
		if (res.noResume()) {
			return null;
		}

		if (r.getMedia() != null) {
			double dur = r.getMedia().getDurationInSeconds();
			if (dur == 0.0 || dur == DLNAMediaInfo.TRANS_SIZE) {
				r.getMedia().setDuration(res.resDuration / 1000.0);
			}
		}
		res.setMinDuration(r.minPlayTime());
		return res;
	}

	public static ResumeObj store(DLNAResource r, long startTime) {
		File f = resumeFile(r);
		ResumeObj obj = new ResumeObj(f);
		obj.setMinDuration(r.minPlayTime());
		obj.stop(startTime, (long) r.getMedia().getDurationInSeconds() * 1000);
		if (obj.noResume()) {
			return null;
		}
		return obj;
	}

	public ResumeObj(File f) {
		offsetTime = 0;
		resDuration = 0;
		file = f;
		minDur = configuration.getMinPlayTime();
	}

	public void setMinDuration(long dur) {
		if (dur == 0) {
			dur = configuration.getMinPlayTime();
		}
		minDur = dur;
	}

	public void read() {
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
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

	private static void write(long time, long duration, File f) {
		try {
			try (BufferedWriter out = new BufferedWriter(new FileWriter(f))) {
				out.write(time + "," + duration);
				out.flush();
				out.close();
				if (configuration.getResumeKeepTime() > 0) {
					PMS.get().addTempFile(f, configuration.getResumeKeepTime() * DAYS);
				}
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

	public void update(Range.Time range, DLNAResource r) {
		if (range.isStartOffsetAvailable() && range.getStartOrZero() > 0.0) {
			stop(System.currentTimeMillis() + getTimeOffset() - (long) (range.getStart() * 1000), (long) ((r.getMedia() != null ? r.getMedia().getDuration() : 0) * 1000));
		}
	}

	public void stop(long startTime, long expDuration) {
		long now = System.currentTimeMillis();
		long thisPlay = now - startTime;
		long duration = thisPlay + offsetTime;

		if (expDuration > minDur) {
			if (duration >= (expDuration * configuration.getResumeBackFactor())) {
				// We've seen the whole video (likely)
				file.delete();
				return;
			}
		}
		if (thisPlay < configuration.getResumeRewind()) {
			return;
		}
		if (thisPlay < minDur) {
			// to short to resume (at all)
			return;
		}

		offsetTime = duration - configuration.getResumeRewind();
		resDuration = expDuration;
		LOGGER.debug("Resume stop. This segment " + thisPlay + " new time " + duration);
		write(offsetTime, expDuration, file);
	}
}
