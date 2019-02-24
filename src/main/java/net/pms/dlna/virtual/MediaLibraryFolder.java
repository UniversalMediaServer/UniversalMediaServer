package net.pms.dlna.virtual;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.File;
import java.util.ArrayList;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.*;
import net.pms.util.UMSUtils;

/**
 * A MediaLibraryFolder can be populated by either virtual folders (e.g. TEXTS
 * and SEASONS) or virtual/real files (e.g. FILES and ISOS). All of these are
 * connected to SQL queries.
 */
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
	private String displayNameOverride;
	private String sqlResult;

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
		this.doRefreshChildren();
	}

	private String transformSQL(String sql) {
		int i = 1;
		DLNAResource resource = this;
		sql = sql.replace("${0}", transformName(getName()));
		while (resource.getParent() != null) {
			resource = resource.getParent();
			sql = sql.replace("${" + i + "}", transformName(resource.getName()));
			i++;
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
		ArrayList<File> filesListFromDb = null;
		ArrayList<String> virtualFoldersListFromDb = null;
		int expectedOutput = 0;
		if (sqls.length > 0) {
			String sql = sqls[0];

			/**
			 * @todo work with all expectedOutputs instead of just the first
			 */
			expectedOutput = expectedOutputs[0];
			if (sql != null) {
				sql = transformSQL(sql);
				if (expectedOutput == FILES || expectedOutput == FILES_NOSORT || expectedOutput == PLAYLISTS || expectedOutput == ISOS) {
					filesListFromDb = database.getFiles(sql);
				} else if (expectedOutput == TEXTS || expectedOutput == TEXTS_NOSORT || expectedOutput == SEASONS) {
					virtualFoldersListFromDb = database.getStrings(sql);
				}
			}
		}
		ArrayList<File> newFiles = new ArrayList<>();
		ArrayList<String> newVirtualFolders = new ArrayList<>();
		ArrayList<DLNAResource> oldFiles = new ArrayList<>();
		ArrayList<DLNAResource> oldVirtualFolders = new ArrayList<>();

		if (filesListFromDb != null) {
			UMSUtils.sort(filesListFromDb, PMS.getConfiguration().getSortMethod(null));

			for (DLNAResource child : getChildren()) {
				oldFiles.add(child);
			}

			for (File file : filesListFromDb) {
				newFiles.add(file);
			}
		}

		if (virtualFoldersListFromDb != null) {
			UMSUtils.sort(virtualFoldersListFromDb, PMS.getConfiguration().getSortMethod(null));

			for (DLNAResource child : getChildren()) {
				oldVirtualFolders.add(child);
			}

			for (String f : virtualFoldersListFromDb) {
				newVirtualFolders.add(f);
			}
		}

		for (DLNAResource fileResource : oldFiles) {
			getChildren().remove(fileResource);
		}
		for (DLNAResource virtualFolderResource : oldVirtualFolders) {
			getChildren().remove(virtualFolderResource);
		}
		for (File file : newFiles) {
			if (expectedOutput == FILES) {
				addChild(new RealFile(file));
			} else if (expectedOutput == PLAYLISTS) {
				addChild(new PlaylistFolder(file));
			} else if (expectedOutput == ISOS) {
				addChild(new DVDISOFile(file));
			}
		}
		for (String virtualFolderName : newVirtualFolders) {
			if (expectedOutput == TEXTS || expectedOutput == TEXTS_NOSORT || expectedOutput == SEASONS) {
				String nameToDisplay = null;

				// Don't prepend "Season" text to years 
				if (expectedOutput == SEASONS && virtualFolderName.length() != 4) {
					nameToDisplay = Messages.getString("VirtualFolder.6") + " " + virtualFolderName;
				}

				String sqls2[] = new String[sqls.length - 1];
				int expectedOutputs2[] = new int[expectedOutputs.length - 1];
				System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
				System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);
				addChild(new MediaLibraryFolder(virtualFolderName, sqls2, expectedOutputs2, nameToDisplay));
			}
		}

		setUpdateId(this.getIntId());
		//return oldFiles.size() != 0 || newFiles.size() != 0 || oldVirtualFolders.size() != 0 || newVirtualFolders.size() != 0;
	}

	@Override
	protected String getDisplayNameBase() {
		if (isNotBlank(displayNameOverride)) {
			return displayNameOverride;
		}

		return super.getDisplayNameBase();
	}
}
