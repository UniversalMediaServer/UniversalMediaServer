package net.pms.util;

import java.io.File;

public class InfoDb implements DbHandler {
	public class InfoDbData {
		public String imdb;
		public String ep_name;
		public String year;
		public String season;
		public String episode;
		public String title;
	}

	private FileDb db;

	public InfoDb() {
		db = new FileDb(this);
		db.setMinCnt(6);
		db.init();
	}

	public void backgroundAdd(final File f, final String formattedName) {
		if (get(f) != null) {
			return;
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					String[] tmp = OpenSubtitle.getInfo(f, formattedName);
					if (tmp != null) {
						db.add(f.getAbsolutePath(), create(tmp, 0));
					} else {
						db.add(f.getAbsolutePath(), null);
					}
				} catch (Exception e) {
				}
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
		return (InfoDbData) db.get(f);
	}

	@Override
	public Object create(String[] args) {
		return create(args, 1);
	}

	public Object create(String[] args, int off) {
		InfoDbData data = new InfoDbData();
		data.imdb = FileDb.safeGetArg(args, off);

		/**
		 * Sometimes if IMDB doesn't have an episode title they call it
		 * something like "Episode #1.4", so discard that.
		 */
		data.ep_name = FileDb.safeGetArg(args, off + 1);
		if (data.ep_name.startsWith("Episode #")) {
			data.ep_name = "";
		}

		data.year = FileDb.safeGetArg(args, off + 2);
		data.episode = FileDb.safeGetArg(args, off + 3);
		data.title = FileDb.safeGetArg(args, off + 4);
		data.season = FileDb.safeGetArg(args, off + 5);

		return data;
	}

	@Override
	public String[] format(Object obj) {
		InfoDbData data = (InfoDbData) obj;
		return new String[]{
			data.imdb,
			data.ep_name,
			data.season,
			data.episode,
			data.title,
			data.year
		};
	}

	@Override
	public String name() {
		return "InfoDb.db";
	}
}
