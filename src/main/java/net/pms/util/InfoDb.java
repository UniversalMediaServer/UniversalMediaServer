package net.pms.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfoDb implements DbHandler {
	public static class InfoDbData {
		public String actors;
		public String awards;
		public String boxoffice;
		public String country;
		public String directors;
		public String ep_name;
		public String episode;
		public HashSet genres;
		public String goofs;
		public String imdb;
		public String metascore;
		public String production;
		public String poster;
		public String rated;
		public String rating;
		public String ratings;
		public String released;
		public String runtime;
		public String season;
		public String tagline;
		public String title;
		public String trivia;
		public String type;
		public String votes;
		public String year;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(InfoDb.class);
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
			HashMap<String, Object> apiResult = OpenSubtitle.getInfo(f, formattedName, null);
			Object obj = FileDb.nullObj();
			if (apiResult != null) {
				obj = create(apiResult, 0);
			}
			synchronized (db) {
				db.add(f.getAbsolutePath(), obj);
			}
		} catch (Exception e) {
			LOGGER.error("Error while inserting in InfoDb: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public void backgroundAdd(final File f, final String formattedName) {
		synchronized (db) {
			if (db.get(f.getAbsolutePath()) != null) {
				// we need to use the raw get to see so it's
				// truly null
				// also see if we should redo
				redoNulls();
				return;
			}
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
		synchronized (db) {
			InfoDbData data = get(old_file);
			if (data != null) {
				db.removeNoSync(old_file.getAbsolutePath());
				db.addNoSync(new_file.getAbsolutePath(), data);
				db.sync();
			}
		}
	}

	public InfoDbData get(File f) {
		return get(f.getAbsolutePath());
	}

	public InfoDbData get(String f) {
		synchronized (db) {
			Object obj = db.get(f);
			return (InfoDbData) (db.isNull(obj) ? null : obj);
		}
	}

	/**
	 * We don't use this, meaning this isn't a good implementation of a
	 * FileDb; there is refactoring needed. This file acting more like a memory
	 * cache and is probably not needed at all since we already have that
	 * caching ability in the DLNAResources themselves.
	 *
	 * @param info
	 * @return 
	 */
	@Override
	public Object create(String[] info) {
		return null;
	}

	public Object create(HashMap info) {
		return create(info, 1);
	}

	public Object create(HashMap metadataFromAPI, int off) {
		InfoDbData data = new InfoDbData();

//		data.actors     = metadataFromAPI.get("actors");
//		data.awards     = metadataFromAPI.get("awards");
//		data.boxoffice  = metadataFromAPI.get("boxoffice");
//		data.country    = metadataFromAPI.get("country");
//		data.directors  = metadataFromAPI.get("directors");
//		data.ep_name    = metadataFromAPI.get("episodeTitle");
//		data.episode    = metadataFromAPI.get("episodeNumber");
		data.genres     = new HashSet((ArrayList) metadataFromAPI.get("genres"));
//		data.goofs      = metadataFromAPI.get("goofs");
//		data.imdb       = metadataFromAPI.get("imdbID");
//		data.metascore  = metadataFromAPI.get("metascore");
//		data.production = metadataFromAPI.get("production");
//		data.poster     = metadataFromAPI.get("poster");
//		data.rated      = metadataFromAPI.get("rated");
//		data.rating     = metadataFromAPI.get("rating");
//		data.ratings    = metadataFromAPI.get("ratings");
//		data.released   = metadataFromAPI.get("released");
//		data.runtime    = metadataFromAPI.get("runtime");
//		data.season     = metadataFromAPI.get("seasonNumber");
//		data.tagline    = metadataFromAPI.get("tagline");
//		data.title      = metadataFromAPI.get("title");
//		data.trivia     = metadataFromAPI.get("trivia");
//		data.type       = metadataFromAPI.get("type");
//		data.votes      = metadataFromAPI.get("votes");
//		data.year       = metadataFromAPI.get("year");

		return data;
	}

	/**
	 * We don't use this, meaning this isn't a good implementation of a
	 * FileDb; there is refactoring needed. This file acting more like a memory
	 * cache and is probably not needed at all since we already have that
	 * caching ability in the DLNAResources themselves.
	 * 
	 *
	 * @param obj
	 * @return 
	 */
	@Override
	public String[] format(Object obj) {
		return null;
	}

	@Override
	public String name() {
		return "InfoDb.db";
	}

	private static boolean redo() {
		long now = System.currentTimeMillis();
		long last = now;
		try {
			last = Long.parseLong(PMS.getKey(LAST_INFO_REREAD_KEY));
		} catch (NumberFormatException e) {
		}
		return (now - last) > REDO_PERIOD;
	}

	private void redoNulls() {
		synchronized (db) {
			// no nulls in db skip this
			if (!db.hasNulls()) {
				return;
			}
			if (!redo() || !PMS.getConfiguration().isInfoDbRetry()) {
				// no redo
				return;
			}
		}

		// update this first to make redo() return false for other
		PMS.setKey(LAST_INFO_REREAD_KEY, "" + System.currentTimeMillis());
		Runnable r = new Runnable() {
			@Override
			public void run() {
				synchronized (db) {
					// this whole iterator stuff is to avoid
					// CMEs
					Iterator<Entry<String, Object>> it = db.iterator();
					boolean sync = false;
					while (it.hasNext()) {
						Map.Entry<String, Object> kv = it.next();
						String key = kv.getKey();

						// nonNull -> no need to ask again
						if (!db.isNull(kv.getValue())) {
							continue;
						}
						File f = new File(key);
						String name = f.getName();
						try {
							HashMap apiResult = OpenSubtitle.getInfo(f, name, null);
							// if we still get nothing from opensubs
							// we don't fiddle with the db
							if (apiResult != null) {
								kv.setValue(create(apiResult, 0));
								sync = true;
							}
						} catch (Exception e) {
							LOGGER.error("Exception in redoNulls: {}", e.getMessage());
							LOGGER.trace("", e);
						}
					}
					if (sync) {
						// we need a manual sync here
						db.sync();
					}
				}
			}
		};
		new Thread(r).start();
	}
}
