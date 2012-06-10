package net.pms.dlna.virtual;

import java.io.File;
import java.util.ArrayList;

import net.pms.PMS;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DVDISOFile;
import net.pms.dlna.PlaylistFolder;
import net.pms.dlna.RealFile;

public class MediaLibraryFolder extends VirtualFolder {
	public static final int FILES = 0;
	public static final int TEXTS = 1;
	public static final int PLAYLISTS = 2;
	public static final int ISOS = 3;
	private String sqls[];
	private int expectedOutputs[];
	private DLNAMediaDatabase database;

	public MediaLibraryFolder(String name, String sql, int expectedOutput) {
		this(name, new String[]{sql}, new int[]{expectedOutput});
	}

	public MediaLibraryFolder(String name, String sql[], int expectedOutput[]) {
		super(name, null);
		this.sqls = sql;
		this.expectedOutputs = expectedOutput;
		this.database = PMS.get().getDatabase();
		// double check the database has been initialized (via PMS.init -> PMS.initializeDatabase)
		// http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=11474
		assert this.database != null;
	}

	@Override
	public void discoverChildren() {
		if (sqls.length > 0) {
			String sql = sqls[0];
			int expectedOutput = expectedOutputs[0];
			if (sql != null) {
				sql = transformSQL(sql);
				if (expectedOutput == FILES) {
					ArrayList<File> list = database.getFiles(sql);
					if (list != null) {
						for (File f : list) {
							addChild(new RealFile(f));
						}
					}
				} else if (expectedOutput == PLAYLISTS) {
					ArrayList<File> list = database.getFiles(sql);
					if (list != null) {
						for (File f : list) {
							addChild(new PlaylistFolder(f));
						}
					}
				} else if (expectedOutput == ISOS) {
					ArrayList<File> list = database.getFiles(sql);
					if (list != null) {
						for (File f : list) {
							addChild(new DVDISOFile(f));
						}
					}
				} else if (expectedOutput == TEXTS) {
					ArrayList<String> list = database.getStrings(sql);
					if (list != null) {
						for (String s : list) {
							String sqls2[] = new String[sqls.length - 1];
							int expectedOutputs2[] = new int[expectedOutputs.length - 1];
							System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
							System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);
							addChild(new MediaLibraryFolder(s, sqls2, expectedOutputs2));
						}
					}
				}
			}
		}
	}

	@Override
	public void resolve() {
		super.resolve();
	}

	private String transformSQL(String sql) {

		sql = sql.replace("${0}", transformName(getName()));
		if (getParent() != null) {
			sql = sql.replace("${1}", transformName(getParent().getName()));
			if (getParent().getParent() != null) {
				sql = sql.replace("${2}", transformName(getParent().getParent().getName()));
				if (getParent().getParent().getParent() != null) {
					sql = sql.replace("${3}", transformName(getParent().getParent().getParent().getName()));
					if (getParent().getParent().getParent().getParent() != null) {
						sql = sql.replace("${4}", transformName(getParent().getParent().getParent().getParent().getName()));
					}
				}
			}
		}
		return sql;
	}

	private String transformName(String name) {
		if (name.equals(DLNAMediaDatabase.NONAME)) {
			name = "";
		}
		name = name.replace("'", "''"); // issue 448
		return name;
	}

	@Override
	public boolean isRefreshNeeded() {
		return true;
	}

	@Override
	public void doRefreshChildren() {
		ArrayList<File> list = null;
		ArrayList<String> strings = null;
		int expectedOutput = 0;
		if (sqls.length > 0) {
			String sql = sqls[0];
			expectedOutput = expectedOutputs[0];
			if (sql != null) {
				sql = transformSQL(sql);
				if (expectedOutput == FILES || expectedOutput == PLAYLISTS || expectedOutput == ISOS) {
					list = database.getFiles(sql);
				} else if (expectedOutput == TEXTS) {
					strings = database.getStrings(sql);
				}
			}
		}
		ArrayList<File> addedFiles = new ArrayList<File>();
		ArrayList<String> addedString = new ArrayList<String>();
		ArrayList<DLNAResource> removedFiles = new ArrayList<DLNAResource>();
		ArrayList<DLNAResource> removedString = new ArrayList<DLNAResource>();
		int i = 0;
		if (list != null) {
			for (File f : list) {
				boolean present = false;
				for (DLNAResource d : getChildren()) {
					if (i == 0 && (!(d instanceof VirtualFolder) || (d instanceof MediaLibraryFolder))) {
						removedFiles.add(d);
					}
					String name = d.getName();
					long lm = d.getLastmodified();
					boolean video_ts_hack = (d instanceof DVDISOFile) && d.getName().startsWith(DVDISOFile.PREFIX) && d.getName().substring(DVDISOFile.PREFIX.length()).equals(f.getName());
					if ((f.getName().equals(name) || video_ts_hack) && f.lastModified() == lm) {
						removedFiles.remove(d);
						present = true;
					}
				}
				i++;
				if (!present) {
					addedFiles.add(f);
				}
			}
		}
		i = 0;
		if (strings != null) {
			for (String f : strings) {
				boolean present = false;
				for (DLNAResource d : getChildren()) {
					if (i == 0 && (!(d instanceof VirtualFolder) || (d instanceof MediaLibraryFolder))) {
						removedString.add(d);
					}
					String name = d.getName();
					if (f.equals(name)) {
						removedString.remove(d);
						present = true;
					}
				}
				i++;
				if (!present) {
					addedString.add(f);
				}
			}
		}

		for (DLNAResource f : removedFiles) {
			getChildren().remove(f);
		}
		for (DLNAResource s : removedString) {
			getChildren().remove(s);
		}
		for (File f : addedFiles) {
			if (expectedOutput == FILES) {
				addChild(new RealFile(f));
			} else if (expectedOutput == PLAYLISTS) {
				addChild(new PlaylistFolder(f));
			} else if (expectedOutput == ISOS) {
				addChild(new DVDISOFile(f));
			}
		}
		for (String f : addedString) {
			if (expectedOutput == TEXTS) {
				String sqls2[] = new String[sqls.length - 1];
				int expectedOutputs2[] = new int[expectedOutputs.length - 1];
				System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
				System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);
				addChild(new MediaLibraryFolder(f, sqls2, expectedOutputs2));
			}
		}

		//return removedFiles.size() != 0 || addedFiles.size() != 0 || removedString.size() != 0 || addedString.size() != 0;
	}
}
