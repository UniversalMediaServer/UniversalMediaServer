package net.pms.dlna.virtual;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.database.TableFilesStatus;
import net.pms.database.TableVideoMetadataGenres;
import net.pms.dlna.*;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MediaLibraryFolder can be populated by either virtual folders (e.g. TEXTS
 * and SEASONS) or virtual/real files (e.g. FILES and ISOS). All of these are
 * connected to SQL queries.
 *
 * When the expectedOutput is appended with "_WITH_FILTERS", Watched and Unwatched
 * variants will be added at the top.
 */
public class MediaLibraryFolder extends VirtualFolder {
	public static final int FILES = 0;
	public static final int TEXTS = 1;
	public static final int PLAYLISTS = 2;
	public static final int ISOS = 3;
	public static final int SEASONS = 4;
	public static final int FILES_NOSORT = 5;
	public static final int TEXTS_NOSORT = 6;
	public static final int EPISODES = 7;
	public static final int TEXTS_WITH_FILTERS = 8;
	public static final int FILES_WITH_FILTERS = 9;
	public static final int TEXTS_NOSORT_WITH_FILTERS = 10;
	public static final int ISOS_WITH_FILTERS = 11;
	private String sqls[];
	private int expectedOutputs[];
	private DLNAMediaDatabase database;
	private String displayNameOverride;
	private ArrayList<String> populatedVirtualFoldersListFromDb;
	private ArrayList<String> populatedFilesListFromDb;
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
		doRefreshChildren();
		setDiscovered(true);
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

	/**
	 * Bumps the placeholder (e.g. ${0}) in a query to allow us to add a
	 * dynamic condition to the start.
	 *
	 * @param sql
	 * @return a string where the placeholders start from 1, not 0
	 */
	private StringBuilder incrementPlaceholders(String sql) {
		int i = 1;
		int secondOccurrenceOfCurrentIterator;
		sql = sql.replace("${0}", "${1}");
		while (true) {
			secondOccurrenceOfCurrentIterator = sql.indexOf("${" + i + "}", sql.indexOf("${" + i + "}") + 1);
			if (secondOccurrenceOfCurrentIterator > -1) {
				sql = sql.replaceFirst("\\$\\{\\" + i + "\\}", "${" + (i + 1) + "}");
			} else {
				break;
			}
			i++;
		}

		return new StringBuilder(sql);
	}

	private String transformName(String name) {
		if (name.equals(DLNAMediaDatabase.NONAME)) {
			name = "";
		}
		name = name.replace("'", "''"); // issue 448
		return name;
	}

	/**
	 * Whether the contents of this virtual folder should be refreshed.
	 *
	 * @return true if the old cached SQL result matches the new one.
	 */
	@Override
	public boolean isRefreshNeeded() {
		int expectedOutput = 0;
		if (sqls.length > 0) {
			String sql = sqls[0];

			/**
			 * @todo work with all expectedOutputs instead of just the first
			 */
			expectedOutput = expectedOutputs[0];
			if (sql != null) {
				sql = transformSQL(sql);

				if (
					expectedOutput == EPISODES ||
					expectedOutput == FILES ||
					expectedOutput == FILES_NOSORT ||
					expectedOutput == FILES_WITH_FILTERS ||
					expectedOutput == ISOS ||
					expectedOutput == ISOS_WITH_FILTERS ||
					expectedOutput == PLAYLISTS
				) {
					return !UMSUtils.isListsEqual(populatedFilesListFromDb, database.getStrings(sql));
				} else if (
					expectedOutput == TEXTS ||
					expectedOutput == TEXTS_NOSORT ||
					expectedOutput == TEXTS_NOSORT_WITH_FILTERS ||
					expectedOutput == TEXTS_WITH_FILTERS ||
					expectedOutput == SEASONS
				) {
					return !UMSUtils.isListsEqual(populatedVirtualFoldersListFromDb, database.getStrings(sql));
				}
			}
		}

		return true;
	}

	final static String UNWATCHED_CONDITION = TableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE AND ";
	final static String WATCHED_CONDITION = TableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS TRUE AND ";
	final static String GENRES_CONDITION = TableVideoMetadataGenres.TABLE_NAME + ".GENRE = '${0}' AND ";
	final static String SQL_JOIN_SECTION = "LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME ";
	final static String SQL_JOIN_GENRE_SECTION = "LEFT JOIN " + TableVideoMetadataGenres.TABLE_NAME + " ON FILES.FILENAME = " + TableVideoMetadataGenres.TABLE_NAME + ".FILENAME ";
	final static String GENRES_SELECT = "SELECT DISTINCT " + TableVideoMetadataGenres.TABLE_NAME + ".GENRE FROM FILES ";
	final static String GENRES_ORDERBY = "ORDER BY " + TableVideoMetadataGenres.TABLE_NAME + ".GENRE ASC";

	/**
	 * Removes all children and re-adds them
	 */
	@Override
	public void doRefreshChildren() {
		ArrayList<File> filesListFromDb = null;
		ArrayList<String> virtualFoldersListFromDb = null;

		List<String> unwatchedSqls = new ArrayList<>();
		List<String> watchedSqls = new ArrayList<>();
		List<String> genresSqls = new ArrayList<>();
		int expectedOutput = 0;
		if (sqls.length > 0) {
			String firstSql = sqls[0];

			expectedOutput = expectedOutputs[0];
			if (firstSql != null) {
				firstSql = transformSQL(firstSql);
				switch (expectedOutput) {
					case FILES:
					case FILES_NOSORT:
					case EPISODES:
					case PLAYLISTS:
					case ISOS:
						filesListFromDb = database.getFiles(firstSql);
						populatedFilesListFromDb = database.getStrings(firstSql);
						break;
					case TEXTS:
					case TEXTS_NOSORT:
					case SEASONS:
						virtualFoldersListFromDb = database.getStrings(firstSql);
						populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
						break;
					case FILES_WITH_FILTERS:
					case ISOS_WITH_FILTERS:
					case TEXTS_NOSORT_WITH_FILTERS:
					case TEXTS_WITH_FILTERS:
						if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS) {
							virtualFoldersListFromDb = database.getStrings(firstSql);
							populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
						} else if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
							filesListFromDb = database.getFiles(firstSql);
							populatedFilesListFromDb = database.getStrings(firstSql);
						}

						// Generic strings to match to help us manipulate queries on the fly
						String fromFilesString = "FROM FILES ";
						String orderByString = "ORDER BY ";

						if (!firstSql.toLowerCase().startsWith("select")) {
							if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS) {
								firstSql = "SELECT FILES.FILENAME FROM FILES WHERE " + firstSql;
							}
							if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
								firstSql = "SELECT FILES.FILENAME, FILES.MODIFIED FROM FILES WHERE " + firstSql;
							}
						};

						int indexAfterFromInFirstQuery = firstSql.indexOf(fromFilesString) + fromFilesString.length();
						// Prepare the first query in the genres filter
						StringBuilder firstGenresSql = new StringBuilder(firstSql);
						// If the query does not already join the genres table, do that now
						if (!firstSql.contains("LEFT JOIN " + TableVideoMetadataGenres.TABLE_NAME)) {
							firstGenresSql.insert(indexAfterFromInFirstQuery, SQL_JOIN_GENRE_SECTION);
						}

						indexAfterFromInFirstQuery = firstSql.indexOf(fromFilesString) + fromFilesString.length();
						firstGenresSql.replace(0, indexAfterFromInFirstQuery, GENRES_SELECT);
						int indexBeforeOrderByInFirstQuery = firstGenresSql.indexOf(orderByString);
						firstGenresSql.replace(indexBeforeOrderByInFirstQuery, firstGenresSql.length(), GENRES_ORDERBY);
						LOGGER.info("firstGenresSql: " + firstGenresSql.toString());
						genresSqls.add(firstGenresSql.toString());

						// Make "Fully Played" (Unwatched and Watched) variations of the queries
						for (String sql : sqls) {
							if (!sql.toLowerCase().startsWith("select")) {
								if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS) {
									sql = "SELECT FILES.FILENAME FROM FILES WHERE " + sql;
								}
								if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
									sql = "SELECT FILES.FILENAME, FILES.MODIFIED FROM FILES WHERE " + sql;
								}
							};
							String whereString = "WHERE ";
							int indexAfterFrom = sql.indexOf(fromFilesString) + fromFilesString.length();

							// If the query does not already join the FILES_STATUS table, do that now
							StringBuilder sqlWithJoin = new StringBuilder(sql);
							if (!sql.contains("LEFT JOIN " + TableFilesStatus.TABLE_NAME)) {
								sqlWithJoin.insert(indexAfterFrom, SQL_JOIN_SECTION);
							}

							int indexAfterWhere = sqlWithJoin.indexOf(whereString) + whereString.length();

							StringBuilder unwatchedSql = new StringBuilder(sqlWithJoin);
							unwatchedSql.insert(indexAfterWhere, UNWATCHED_CONDITION);
							unwatchedSqls.add(unwatchedSql.toString());

							StringBuilder watchedSql = new StringBuilder(sqlWithJoin);
							watchedSql.insert(indexAfterWhere, WATCHED_CONDITION);
							watchedSqls.add(watchedSql.toString());

							// Prepare the first query in the genres filter
							StringBuilder genresSql = new StringBuilder(sql);
							// If the query does not already join the genres table, do that now
							if (!sql.contains("LEFT JOIN " + TableVideoMetadataGenres.TABLE_NAME)) {
								genresSql.insert(indexAfterFrom, SQL_JOIN_GENRE_SECTION);
							}

							genresSql = incrementPlaceholders(genresSql.toString());
							indexAfterWhere = genresSql.indexOf(whereString) + whereString.length();
							genresSql.insert(indexAfterWhere, GENRES_CONDITION);
							LOGGER.info("genresSql: " + genresSql.toString());
							genresSqls.add(genresSql.toString());
						}

						break;
					default:
						break;
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
			if (expectedOutput != TEXTS_NOSORT && expectedOutput != TEXTS_NOSORT_WITH_FILTERS) {
				UMSUtils.sort(virtualFoldersListFromDb, PMS.getConfiguration().getSortMethod(null));
			}

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

		// Add filters at the top
		if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == FILES_WITH_FILTERS) {
			// Convert the expectedOutputs to unfiltered versions
			int[] filteredExpectedOutputs = expectedOutputs.clone();
			switch (filteredExpectedOutputs[0]) {
				case FILES_WITH_FILTERS:
					filteredExpectedOutputs[0] = FILES;
					break;
				case ISOS_WITH_FILTERS:
					filteredExpectedOutputs[0] = ISOS;
					break;
				case TEXTS_WITH_FILTERS:
					filteredExpectedOutputs[0] = TEXTS;
					break;
				case TEXTS_NOSORT_WITH_FILTERS:
					filteredExpectedOutputs[0] = TEXTS_NOSORT;
					break;
				default:
					break;
			}
			
			int[] filteredExpectedOutputsWithPrependedTexts = filteredExpectedOutputs.clone();
			filteredExpectedOutputsWithPrependedTexts = ArrayUtils.insert(0, filteredExpectedOutputsWithPrependedTexts, TEXTS);

			if (!unwatchedSqls.isEmpty()) {
				addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.9"),
					unwatchedSqls.toArray(new String[0]),
					filteredExpectedOutputs
				));
			}
			if (!watchedSqls.isEmpty()) {
				addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Watched"),
					watchedSqls.toArray(new String[0]),
					filteredExpectedOutputs
				));
			}
			if (!genresSqls.isEmpty()) {
				addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Genres"),
					genresSqls.toArray(new String[0]),
					filteredExpectedOutputsWithPrependedTexts
				));
				LOGGER.info("filteredExpectedOutputsWithPrependedTexts: " + Arrays.toString(filteredExpectedOutputsWithPrependedTexts));
				LOGGER.info("genresSqls: " + genresSqls.toString());
			}
		}

		for (File file : newFiles) {
			switch (expectedOutput) {
				case FILES:
				case FILES_NOSORT:
				case FILES_WITH_FILTERS:
					addChild(new RealFile(file));
					break;
				case EPISODES:
					addChild(new RealFile(file, true));
					break;
				case PLAYLISTS:
					addChild(new PlaylistFolder(file));
					break;
				case ISOS:
				case ISOS_WITH_FILTERS:
					addChild(new DVDISOFile(file));
					break;
				default:
					break;
			}
		}
		for (String virtualFolderName : newVirtualFolders) {
			if (expectedOutput == TEXTS || expectedOutput == TEXTS_NOSORT || expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == SEASONS) {
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

		if (isDiscovered()) {
			setUpdateId(this.getIntId());
		}
	}

	@Override
	protected String getDisplayNameBase() {
		if (isNotBlank(displayNameOverride)) {
			return displayNameOverride;
		}

		return super.getDisplayNameBase();
	}
}
