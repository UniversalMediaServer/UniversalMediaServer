package net.pms.dlna.virtual;

import java.io.File;
import java.util.ArrayList;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.*;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaLibraryFolder extends VirtualFolder {
	public static final int FILES = 0;
	public static final int TEXTS = 1;
	public static final int PLAYLISTS = 2;
	public static final int ISOS = 3;
	public static final int SEASONS = 4;
	public static final int FILES_NOSORT = 5;
	public static final int TEXTS_NOSORT = 6;
	private String sqls[];
	private int expectedOutputs[];
	private DLNAMediaDatabase database;
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaLibraryFolder.class);

	public MediaLibraryFolder(String name, String sql, int expectedOutput) {
		this(name, new String[]{sql}, new int[]{expectedOutput});
	}

	public MediaLibraryFolder(String name, String sql[], int expectedOutput[]) {
		super(name, null);
		this.sqls = sql;
		this.expectedOutputs = expectedOutput;
		this.database = PMS.get().getDatabase();
	}

	public MediaLibraryFolder(String name, String sql, int expectedOutput, String nameToDisplay) {
		this(name, new String[]{sql}, new int[]{expectedOutput}, nameToDisplay);
	}

	public MediaLibraryFolder(String name, String sql[], int expectedOutput[], String nameToDisplay) {
		super(name, null);
		this.sqls = sql;
		this.expectedOutputs = expectedOutput;
		this.database = PMS.get().getDatabase();
		if (nameToDisplay != null) {
			this.displayNameOverride = nameToDisplay;
		}
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
						UMSUtils.sort(list, PMS.getConfiguration().getSortMethod(null));
						for (File f : list) {
							addChild(new RealFile(f));
						}
					}
				} else if (expectedOutput == FILES_NOSORT) {
					ArrayList<File> list = database.getFiles(sql);
					if (list != null) {
						for (File f : list) {
							addChild(new RealFile(f));
						}
					}
				} else if (expectedOutput == PLAYLISTS) {
					ArrayList<File> list = database.getFiles(sql);
					if (list != null) {
						UMSUtils.sort(list, PMS.getConfiguration().getSortMethod(null));
						for (File f : list) {
							addChild(new PlaylistFolder(f));
						}
					}
				} else if (expectedOutput == ISOS) {
					ArrayList<File> list = database.getFiles(sql);
					if (list != null) {
						UMSUtils.sort(list, PMS.getConfiguration().getSortMethod(null));
						for (File f : list) {
							addChild(new DVDISOFile(f));
						}
					}
				} else if (expectedOutput == TEXTS) {
					ArrayList<String> list = database.getStrings(sql);
					if (list != null) {
						UMSUtils.sort(list, PMS.getConfiguration().getSortMethod(null));
						for (String s : list) {
							String sqls2[] = new String[sqls.length - 1];
							int expectedOutputs2[] = new int[expectedOutputs.length - 1];
							System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
							System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);
							addChild(new MediaLibraryFolder(s, sqls2, expectedOutputs2));
						}
					}
				} else if (expectedOutput == TEXTS_NOSORT) {
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
				} else if (expectedOutput == SEASONS) {
					String nameToDisplay;
					ArrayList<String> list = database.getStrings(sql);
					if (list != null) {
						UMSUtils.sort(list, PMS.getConfiguration().getSortMethod(null));
						for (String s : list) {
							nameToDisplay = null;
							if (s.length() != 4) {
								nameToDisplay = Messages.getString("VirtualFolder.6") + " " + s;
							}

							String sqls2[] = new String[sqls.length - 1];
							int expectedOutputs2[] = new int[expectedOutputs.length - 1];
							System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
							System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);
							addChild(new MediaLibraryFolder(s, sqls2, expectedOutputs2, nameToDisplay));
						}
					}
				}
			}
		}
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

	/**
	 * Removes all children and re-adds them
	 */
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
				if (expectedOutput == FILES || expectedOutput == FILES_NOSORT || expectedOutput == PLAYLISTS || expectedOutput == ISOS) {
					list = database.getFiles(sql);
				} else if (expectedOutput == TEXTS || expectedOutput == TEXTS_NOSORT) {
					strings = database.getStrings(sql);
				}
			}
		}
		ArrayList<File> addedFiles = new ArrayList<>();
		ArrayList<String> addedString = new ArrayList<>();
		ArrayList<DLNAResource> removedFiles = new ArrayList<>();
		ArrayList<DLNAResource> removedString = new ArrayList<>();

		if (list != null) {
			UMSUtils.sort(list, PMS.getConfiguration().getSortMethod(null));

			for (File file : list) {
				for (DLNAResource child : getChildren()) {
					removedFiles.add(child);
				}
				addedFiles.add(file);
			}
		}

		if (strings != null) {
			UMSUtils.sort(strings, PMS.getConfiguration().getSortMethod(null));

			for (String f : strings) {
				for (DLNAResource child : getChildren()) {
					removedString.add(child);
				}
				addedString.add(f);
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
			if (expectedOutput == TEXTS || expectedOutput == TEXTS_NOSORT) {
				String sqls2[] = new String[sqls.length - 1];
				int expectedOutputs2[] = new int[expectedOutputs.length - 1];
				System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
				System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);
				addChild(new MediaLibraryFolder(f, sqls2, expectedOutputs2));
			}
		}

		setUpdateId(this.getIntId());
		//return removedFiles.size() != 0 || addedFiles.size() != 0 || removedString.size() != 0 || addedString.size() != 0;
	}
}
