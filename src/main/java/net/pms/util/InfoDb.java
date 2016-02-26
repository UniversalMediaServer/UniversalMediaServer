package net.pms.util;

import java.io.File;
import net.pms.PMS;

public class InfoDb implements DbHandler {
	public static class InfoDbData {
		public String imdb;
		public String ep_name;
		public String year;
		public String season;
		public String episode;
		public String title;
	}

	private static final long REDO_PERIOD = 7 * 24 * 60 * 60 * 1000; // one week
	private static final String LAST_INFO_REREAD_KEY = "lastInfoReread";

	private FileDb db;

	public InfoDb() {
		db = new FileDb(this);
		db.setMinCnt(6);
		db.setUseNullObj(true);
		db.init();
		if (PMS.getKey(LAST_INFO_REREAD_KEY) == null) {
			PMS.setKey(LAST_INFO_REREAD_KEY, "" + System.currentTimeMillis());
		}
		redoNulls();
	}

	private void askAndInsert(File f, String formattedName) {
		try {
			String[] tmp = OpenSubtitle.getInfo(f, formattedName);
			if (tmp != null) {
				db.add(f.getAbsolutePath(), create(tmp, 0));
			} else {
				db.add(f.getAbsolutePath(), db.nullObj());
			}
		} catch (Exception e) {
		}
	}

	public void backgroundAdd(final File f, final String formattedName) {
		if (db.get(f.getAbsolutePath()) != null) {
			// we need to use the raw get to see so it's
			// truly null
			// also see if we should redo
			redoNulls();
			return;
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				askAndInsert(f, formattedName);
			}
		};
		new Thread(r).start();
	}

	public void moveInfo(File old_file, File new_file) {
		InfoDbData data = get(old_file);
		if (data != null) {
			db.removeNoSync(old_file.getAbsolutePath());
			db.addNoSync(new_file.getAbsolutePath(), data);
			db.sync();
		}
	}

	public InfoDbData get(File f) {
		return get(f.getAbsolutePath());
	}

	public InfoDbData get(String f) {
		Object obj = db.get(f);
		return (InfoDbData) (db.isNull(obj) ? null : obj);
	}

	@Override
	public Object create(String[] args) {
		return create(args, 1);
	}

	public Object create(String[] args, int off) {
		InfoDbData data = new InfoDbData();
		data.imdb = FileDb.safeGetArg(args, off);

		/**
		 * Sometimes if IMDb doesn't have an episode title they call it
		 * something like "Episode #1.4", so discard that.
		 */
		data.ep_name = FileDb.safeGetArg(args, off + 1);
		if (data.ep_name.startsWith("Episode #")) {
			data.ep_name = "";
		}

		data.title = FileDb.safeGetArg(args, off + 2);
		data.season = FileDb.safeGetArg(args, off + 3);
		data.episode = FileDb.safeGetArg(args, off + 4);
		data.year = FileDb.safeGetArg(args, off + 5);

		return data;
	}

	@Override
	public String[] format(Object obj) {
		InfoDbData data = (InfoDbData) obj;
		return new String[]{
			data.imdb,
			data.ep_name,
			data.title,
			data.season,
			data.episode,
			data.year
		};
	}

	@Override
	public String name() {
		return "InfoDb.db";
	}

	private boolean redo() {
		long now = System.currentTimeMillis();
		long last = now;
		try {
			last = Long.parseLong(PMS.getKey(LAST_INFO_REREAD_KEY));
		} catch (NumberFormatException e) {
		}
		return (now - last) > REDO_PERIOD;
	}

	private void redoNulls() {
		if (!redo() || !PMS.getConfiguration().isInfoDbRetry()) {
			// no redo
			return;
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				for (String key : db.keys()) {
					if (!db.isNull(db.get(key))) // nonNull -> no need to ask again
						continue;
					File f = new File(key);
					String name = f.getName();
					askAndInsert(f, name);
				}
				PMS.setKey(LAST_INFO_REREAD_KEY, "" + System.currentTimeMillis());
			}
		};
		new Thread(r).start();

	}
}
