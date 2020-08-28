package net.pms.dlna.virtual;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.database.TableFilesStatus;
import net.pms.database.TableTVSeries;
import net.pms.database.TableVideoMetadataActors;
import net.pms.database.TableVideoMetadataCountries;
import net.pms.database.TableVideoMetadataDirectors;
import net.pms.database.TableVideoMetadataGenres;
import net.pms.database.TableVideoMetadataIMDbRating;
import net.pms.database.TableVideoMetadataRated;
import net.pms.database.TableVideoMetadataReleased;
import static net.pms.database.Tables.sqlQuote;
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
	public static final int TVSERIES_WITH_FILTERS = 12;
	public static final int TVSERIES = 13;
	public static final int TVSERIES_NOSORT = 14;
	public static final int EPISODES_WITHIN_SEASON = 15;
	private boolean isTVSeries = false;
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

	public MediaLibraryFolder(String name, String sql[], int expectedOutput[], String nameToDisplay, boolean isTVSeriesFolder) {
		super(name, null);
		this.sqls = sql;
		this.expectedOutputs = expectedOutput;
		this.database = PMS.get().getDatabase();
		if (nameToDisplay != null) {
			this.displayNameOverride = nameToDisplay;
		}
		if (isTVSeriesFolder) {
			this.isTVSeries = true;
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
			expectedOutput = expectedOutputs[0];
			if (sql != null) {
				sql = transformSQL(sql);

				if (
					expectedOutput == EPISODES ||
					expectedOutput == EPISODES_WITHIN_SEASON ||
					expectedOutput == FILES ||
					expectedOutput == FILES_NOSORT ||
					expectedOutput == FILES_WITH_FILTERS ||
					expectedOutput == ISOS ||
					expectedOutput == ISOS_WITH_FILTERS ||
					expectedOutput == PLAYLISTS
				) {
					return !UMSUtils.isListsEqual(populatedFilesListFromDb, database.getStrings(sql));
				} else if (isTextOutputExpected(expectedOutput)) {
					return !UMSUtils.isListsEqual(populatedVirtualFoldersListFromDb, database.getStrings(sql));
				}
			}
		}

		return true;
	}

	final static String FROM_FILES = "FROM FILES ";
	final static String UNWATCHED_CONDITION = TableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE AND ";
	final static String WATCHED_CONDITION = TableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS TRUE AND ";
	final static String SQL_JOIN_SECTION = "LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME ";
	final static String SELECT_DISTINCT_TVSEASON = "SELECT DISTINCT TVSEASON FROM FILES ";

	private static List<String> getTVSeriesQueries(String tableName, String columnName) {
		List<String> queries = new ArrayList<>();
		queries.add("SELECT " + columnName + " FROM " + tableName + " WHERE TVSERIESID > -1 ORDER BY " + columnName + " ASC");
		queries.add("SELECT TITLE              FROM " + TableTVSeries.TABLE_NAME + " LEFT JOIN " + tableName + " ON " + TableTVSeries.TABLE_NAME + ".ID = " + tableName + ".TVSERIESID WHERE " + tableName + "." + columnName + " = '${0}' ORDER BY TITLE ASC");
		queries.add("SELECT          *         FROM FILES WHERE TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${0}' ORDER BY TVEPISODENUMBER");
		return queries;
	}

	private static String getFirstNonTVSeriesQuery(String firstSql, String tableName, String columnName) {
		String orderByString = "ORDER BY ";
		int indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();

		String selectSection = "SELECT DISTINCT " + tableName + "." + columnName + " FROM FILES ";
		String orderBySection = "ORDER BY " + tableName + "." + columnName + " ASC";

		// These queries join tables
		StringBuilder query = new StringBuilder(firstSql);

		// If the query does not already join the right metadata table, do that now
		if (!firstSql.contains("LEFT JOIN " + tableName)) {
			String joinSection = "LEFT JOIN " + tableName + " ON FILES.FILENAME = " + tableName + ".FILENAME ";
			query.insert(indexAfterFromInFirstQuery, joinSection);
		}

		indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();

		query.replace(0, indexAfterFromInFirstQuery, selectSection);

		int indexBeforeOrderByInFirstActorsQuery = query.indexOf(orderByString);
		query.replace(indexBeforeOrderByInFirstActorsQuery, query.length(), orderBySection);

		return query.toString();
	}

	private static String getSubsequentNonTVSeriesQuery(String sql, String tableName, String columnName, int i) {
		StringBuilder query = new StringBuilder(sql);
		String whereString = "WHERE ";
		int indexAfterFrom = sql.indexOf(FROM_FILES) + FROM_FILES.length();
		String condition = tableName + "." + columnName + " = '${0}' AND ";
		if (!sql.contains("LEFT JOIN " + tableName)) {
			String joinSection = "LEFT JOIN " + tableName + " ON FILES.FILENAME = " + tableName + ".FILENAME ";
			query.insert(indexAfterFrom, joinSection);
		}
		int indexAfterWhere = query.indexOf(whereString) + whereString.length();
		String replacedCondition = condition.replace("${0}", "${" + i + "}");
		query.insert(indexAfterWhere, replacedCondition);
		return query.toString();
	}

	/**
	 * Removes all children and re-adds them
	 */
	@Override
	public void doRefreshChildren() {
		ArrayList<File> filesListFromDb = null;
		ArrayList<String> virtualFoldersListFromDb = null;

		List<String> unwatchedSqls = new ArrayList<>();
		List<String> watchedSqls = new ArrayList<>();

		List<String> actorsSqls = new ArrayList<>();
		List<String> countriesSqls = new ArrayList<>();
		List<String> directorsSqls = new ArrayList<>();
		List<String> genresSqls = new ArrayList<>();
		List<String> ratedSqls = new ArrayList<>();
		List<String> releasedSqls = new ArrayList<>();

		StringBuilder seasonsQuery = new StringBuilder();

		int expectedOutput = 0;
		if (sqls.length > 0) {
			String firstSql = sqls[0];
			expectedOutput = expectedOutputs[0];

LOGGER.info("2expectedOutput: " + expectedOutput);
LOGGER.info("2firstSql: " + firstSql);
			if (firstSql != null) {
				firstSql = transformSQL(firstSql);
				switch (expectedOutput) {
					// Output is files
					case FILES:
					case FILES_NOSORT:
					case PLAYLISTS:
					case ISOS:
					case EPISODES_WITHIN_SEASON:
						firstSql = firstSql.replaceAll(SELECT_DISTINCT_TVSEASON, "SELECT * FROM FILES ");
						filesListFromDb = database.getFiles(firstSql);
						populatedFilesListFromDb = database.getStrings(firstSql);
						break;
					case EPISODES:
						filesListFromDb = database.getFiles(firstSql);
						populatedFilesListFromDb = database.getStrings(firstSql);

						// Build the season filter folders
						String orderByString = "ORDER BY ";
						int indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();

						String orderBySection = "ORDER BY TVSEASON";

						seasonsQuery.append(firstSql);
						seasonsQuery.replace(0, indexAfterFromInFirstQuery, SELECT_DISTINCT_TVSEASON);

						int indexBeforeOrderByInFirstQuery = seasonsQuery.indexOf(orderByString);
						seasonsQuery.replace(indexBeforeOrderByInFirstQuery, seasonsQuery.length(), orderBySection);
						LOGGER.info("11 " + seasonsQuery.toString());
						virtualFoldersListFromDb = database.getStrings(seasonsQuery.toString());
						populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
						break;
					// Output is folders
					case TEXTS:
					case TEXTS_NOSORT:
					case SEASONS:
					case TVSERIES:
					case TVSERIES_NOSORT:
						virtualFoldersListFromDb = database.getStrings(firstSql);
						populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
						break;
					// Output is both
					case FILES_WITH_FILTERS:
					case ISOS_WITH_FILTERS:
					case TEXTS_NOSORT_WITH_FILTERS:
					case TEXTS_WITH_FILTERS:
					case TVSERIES_WITH_FILTERS:
						if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
							virtualFoldersListFromDb = database.getStrings(firstSql);
							populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
						} else if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
							filesListFromDb = database.getFiles(firstSql);
							populatedFilesListFromDb = database.getStrings(firstSql);
						}

						if (!firstSql.toLowerCase().startsWith("select")) {
							if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
								firstSql = "SELECT FILES.FILENAME FROM FILES WHERE " + firstSql;
							}
							if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
								firstSql = "SELECT FILES.FILENAME, FILES.MODIFIED FROM FILES WHERE " + firstSql;
							}
						}

						// This block adds the first SQL query for non-TV series, and all queries for TV series
						if (configuration.isUseInfoFromIMDb()) {
							/*
							 * With TV series we manually add the SQL statements, otherwise we
							 * attempt to modify the incoming statements to make filtering versions.
							 */
							if (expectedOutput == TVSERIES_WITH_FILTERS) {
								actorsSqls = getTVSeriesQueries(TableVideoMetadataActors.TABLE_NAME, "ACTOR");
								countriesSqls = getTVSeriesQueries(TableVideoMetadataCountries.TABLE_NAME, "COUNTRY");
								directorsSqls = getTVSeriesQueries(TableVideoMetadataDirectors.TABLE_NAME, "DIRECTOR");
								genresSqls = getTVSeriesQueries(TableVideoMetadataGenres.TABLE_NAME, "GENRE");
								ratedSqls = getTVSeriesQueries(TableVideoMetadataRated.TABLE_NAME, "RATING");
								releasedSqls = getTVSeriesQueries(TableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(RELEASEDATE, 'yyyy')");
							} else {
								actorsSqls.add(getFirstNonTVSeriesQuery(firstSql, TableVideoMetadataActors.TABLE_NAME, "ACTOR"));
								countriesSqls.add(getFirstNonTVSeriesQuery(firstSql, TableVideoMetadataCountries.TABLE_NAME, "COUNTRY"));
								directorsSqls.add(getFirstNonTVSeriesQuery(firstSql, TableVideoMetadataDirectors.TABLE_NAME, "DIRECTOR"));
								genresSqls.add(getFirstNonTVSeriesQuery(firstSql, TableVideoMetadataGenres.TABLE_NAME, "GENRE"));
								ratedSqls.add(getFirstNonTVSeriesQuery(firstSql, TableVideoMetadataRated.TABLE_NAME, "RATING"));
								releasedSqls.add(getFirstNonTVSeriesQuery(firstSql, TableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(RELEASEDATE, 'yyyy')"));
							}
						}

						// This block adds the second+ queries by modifying what was passed in, allowing this to be somewhat dynamic
						int i = 0;
						for (String sql : sqls) {
							if (!sql.toLowerCase().startsWith("select")) {
								if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
									sql = "SELECT FILES.FILENAME FROM FILES WHERE " + sql;
								}
								if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
									sql = "SELECT FILES.FILENAME, FILES.MODIFIED FROM FILES WHERE " + sql;
								}
							};
							String whereString = "WHERE ";
							int indexAfterFrom = sql.indexOf(FROM_FILES) + FROM_FILES.length();

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

							// Adds modified versions of the query that filter by metadata
							if (configuration.isUseInfoFromIMDb() && expectedOutput != TVSERIES_WITH_FILTERS) {
								actorsSqls.add(getSubsequentNonTVSeriesQuery(sql, TableVideoMetadataActors.TABLE_NAME, "ACTOR", i));
								countriesSqls.add(getSubsequentNonTVSeriesQuery(sql, TableVideoMetadataCountries.TABLE_NAME, "COUNTRY", i));
								directorsSqls.add(getSubsequentNonTVSeriesQuery(sql, TableVideoMetadataDirectors.TABLE_NAME, "DIRECTOR", i));
								genresSqls.add(getSubsequentNonTVSeriesQuery(sql, TableVideoMetadataGenres.TABLE_NAME, "GENRE", i));
								ratedSqls.add(getSubsequentNonTVSeriesQuery(sql, TableVideoMetadataRated.TABLE_NAME, "RATING", i));
								releasedSqls.add(getSubsequentNonTVSeriesQuery(sql, TableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(RELEASEDATE, 'yyyy')", i));
							}
							i++;
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
			if (expectedOutput != TEXTS_NOSORT && expectedOutput != TEXTS_NOSORT_WITH_FILTERS && expectedOutput != TVSERIES_NOSORT) {
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
		if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == FILES_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
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
				case TVSERIES_WITH_FILTERS:
					filteredExpectedOutputs[0] = TVSERIES;
					break;
				case TEXTS_NOSORT_WITH_FILTERS:
					filteredExpectedOutputs[0] = TEXTS_NOSORT;
					break;
				default:
					break;
			}
			
			int[] filteredExpectedOutputsWithPrependedTexts = filteredExpectedOutputs.clone();
			filteredExpectedOutputsWithPrependedTexts = ArrayUtils.insert(0, filteredExpectedOutputsWithPrependedTexts, TEXTS);

			if (!unwatchedSqls.isEmpty() && !watchedSqls.isEmpty()) {
				VirtualFolder filterByProgress = new VirtualFolder(Messages.getString("VirtualFolder.FilterByProgress"), null);
				filterByProgress.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.9"),
					unwatchedSqls.toArray(new String[0]),
					filteredExpectedOutputs
				));
				filterByProgress.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Watched"),
					watchedSqls.toArray(new String[0]),
					filteredExpectedOutputs
				));
				addChild(filterByProgress);
			}
			if (!genresSqls.isEmpty()) {
				VirtualFolder filterByInformation = new VirtualFolder(Messages.getString("VirtualFolder.FilterByInformation"), null);
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Actors"),
					actorsSqls.toArray(new String[0]),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Country"),
					countriesSqls.toArray(new String[0]),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Directors"),
					directorsSqls.toArray(new String[0]),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Genres"),
					genresSqls.toArray(new String[0]),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Rated"),
					ratedSqls.toArray(new String[0]),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("VirtualFolder.Released"),
					releasedSqls.toArray(new String[0]),
					filteredExpectedOutputsWithPrependedTexts
				));
				LOGGER.trace("filteredExpectedOutputsWithPrependedTexts: " + Arrays.toString(filteredExpectedOutputsWithPrependedTexts));
				LOGGER.trace("genresSqls: " + genresSqls.toString());
				addChild(filterByInformation);
			}
		}

		if (expectedOutput == EPISODES && newVirtualFolders.size() == 1) {
			// Skip adding season folders if there is only one season
		} else {
			for (String virtualFolderName : newVirtualFolders) {
				if (isTextOutputExpected(expectedOutput)) {
					String sqls2[] = new String[sqls.length - 1];
					int expectedOutputs2[] = new int[expectedOutputs.length - 1];
					System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
					System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);

					String nameToDisplay = null;
					if (expectedOutput == EPISODES) {
						expectedOutputs2 = new int[]{MediaLibraryFolder.EPISODES_WITHIN_SEASON};
						StringBuilder episodesWithinSeasonQuery = new StringBuilder(sqls[0]);

						String whereString = "WHERE ";
						int indexAfterWhere = episodesWithinSeasonQuery.indexOf(whereString) + whereString.length();
						String condition = "FILES.TVSEASON = '" + virtualFolderName + "' AND ";
						episodesWithinSeasonQuery.insert(indexAfterWhere, condition);

						sqls2 = new String[] { transformSQL(episodesWithinSeasonQuery.toString()) };
						LOGGER.info("15 " + episodesWithinSeasonQuery.toString());

						if (virtualFolderName.length() != 4) {
							nameToDisplay = Messages.getString("VirtualFolder.6") + " " + virtualFolderName;
						}
					}

					/**
					 * Handle entries that have no value in the joined table
					 * Converts e.g. FILES LEFT JOIN VIDEO_METADATA_GENRES ON FILES.FILENAME = VIDEO_METADATA_GENRES.FILENAME WHERE VIDEO_METADATA_GENRES.GENRE = ''
					 * To e.g. FILES LEFT JOIN VIDEO_METADATA_GENRES ON FILES.FILENAME = VIDEO_METADATA_GENRES.FILENAME WHERE VIDEO_METADATA_GENRES.FILENAME IS NULL
					 *
					 * @todo this doesn't work for TV series because we are querying by the external tables. fix that
					 */
					if (expectedOutput == TEXTS || expectedOutput == TEXTS_NOSORT) {
						DLNAResource resource = this;

						if (resource.getName() != null && "###".equals(virtualFolderName)) {
							if (resource.getName().equals(Messages.getString("VirtualFolder.Actors"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + TableVideoMetadataActors.TABLE_NAME + ".ACTOR = '${" + i + "}'", "WHERE " + TableVideoMetadataActors.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Country"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + TableVideoMetadataCountries.TABLE_NAME + ".COUNTRY = '${" + i + "}'", "WHERE " + TableVideoMetadataCountries.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Directors"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + TableVideoMetadataDirectors.TABLE_NAME + ".DIRECTOR = '${" + i + "}'", "WHERE " + TableVideoMetadataDirectors.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Genres"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + TableVideoMetadataGenres.TABLE_NAME + ".GENRE = '${" + i + "}'", "WHERE " + TableVideoMetadataGenres.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Rated"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + TableVideoMetadataRated.TABLE_NAME + ".RATING = '${" + i + "}'", "WHERE " + TableVideoMetadataRated.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Released"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE FORMATDATETIME(" + TableVideoMetadataReleased.TABLE_NAME + ".RELEASEDATE, 'yyyy') = '${" + i + "}'", "WHERE " + TableVideoMetadataReleased.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							}
							LOGGER.trace("2 sqls2: " + Arrays.toString(sqls2));
						}
					}
					boolean isExpectedTVSeries = expectedOutput == TVSERIES || expectedOutput == TVSERIES_NOSORT || expectedOutput == TVSERIES_WITH_FILTERS;
					LOGGER.info("33 " + virtualFolderName);
					LOGGER.info("34 " + Arrays.toString(sqls2));
					LOGGER.info("35 " + Arrays.toString(expectedOutputs2));
					addChild(new MediaLibraryFolder(virtualFolderName, sqls2, expectedOutputs2, nameToDisplay, isExpectedTVSeries));
				}
			}
		}

		// Recommendations for TV series, episodes and movies
		if (expectedOutput == EPISODES) {
			HashSet genres = TableVideoMetadataGenres.getByTVSeriesName(getName());
			String genresCondition = "";
			Iterator<String> i = genres.iterator();
			while (i.hasNext()) {
				String genre = i.next();
				if (isNotBlank(genresCondition)) {
					genresCondition += " OR ";
				}
				genresCondition += TableVideoMetadataGenres.TABLE_NAME + ".GENRE = " + sqlQuote(genre);
			}

			String rated = TableVideoMetadataRated.getByTVSeriesName(getName());
			String ratedCondition = "";
			if (isNotBlank(rated)) {
				ratedCondition = TableVideoMetadataRated.TABLE_NAME + ".RATING = " + sqlQuote(rated) + " ";
			}

			if (isNotBlank(genresCondition) && isNotBlank(ratedCondition)) {
				VirtualFolder recommendations = new MediaLibraryFolder(
					Messages.getString("MediaLibrary.Recommendations"),
					new String[]{
						"SELECT DISTINCT " + TableTVSeries.TABLE_NAME + ".TITLE, " + TableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING " +
							"FROM " + TableTVSeries.TABLE_NAME + " " +
								"LEFT JOIN " + TableVideoMetadataGenres.TABLE_NAME + " ON " + TableTVSeries.TABLE_NAME + ".ID = " + TableVideoMetadataGenres.TABLE_NAME + ".TVSERIESID " +
								"LEFT JOIN " + TableVideoMetadataIMDbRating.TABLE_NAME + " ON " + TableTVSeries.TABLE_NAME + ".ID = " + TableVideoMetadataIMDbRating.TABLE_NAME + ".TVSERIESID " +
								"LEFT JOIN " + TableVideoMetadataRated.TABLE_NAME + " ON " + TableTVSeries.TABLE_NAME + ".ID = " + TableVideoMetadataRated.TABLE_NAME + ".TVSERIESID " +
							"WHERE " +
								"(" + genresCondition + ") AND " +
								ratedCondition + " AND " +
								TableTVSeries.TABLE_NAME + ".TITLE != " + sqlQuote(getName()) + " " +
							"ORDER BY " + TableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING DESC",
						"SELECT * FROM FILES WHERE TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${0}' ORDER BY TVSEASON, TVEPISODENUMBER"
					},
					new int[]{MediaLibraryFolder.TVSERIES_NOSORT, MediaLibraryFolder.EPISODES}
				);
				// note for file recommendations
	//			VirtualFolder recommendations = new MediaLibraryFolder(
	//				Messages.getString("MediaLibrary.Recommendations"),
	//				"SELECT DISTINCT " + DLNAMediaDatabase.TABLE_NAME + ".FILENAME, " + DLNAMediaDatabase.TABLE_NAME + ".MODIFIED, " + TableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING " +
	//				"FROM " + DLNAMediaDatabase.TABLE_NAME + " " +
	//					"LEFT JOIN " + TableFilesStatus.TABLE_NAME + " ON " + DLNAMediaDatabase.TABLE_NAME + ".FILENAME = " + TableFilesStatus.TABLE_NAME + ".FILENAME " +
	//					"LEFT JOIN " + TableVideoMetadataGenres.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataGenres.TABLE_NAME + ".FILENAME " +
	//					"LEFT JOIN " + TableVideoMetadataIMDbRating.TABLE_NAME + " ON " + TABLE_NAME + ".FILENAME = " + TableVideoMetadataIMDbRating.TABLE_NAME + ".FILENAME " +
	//				"WHERE " + DLNAMediaDatabase.TABLE_NAME + ".TYPE = 4 " +
	//					"AND NOT " + DLNAMediaDatabase.TABLE_NAME + ".ISTVEPISODE " +
	//					"AND " + DLNAMediaDatabase.TABLE_NAME + ".YEAR != '' " +
	//					"AND " + DLNAMediaDatabase.TABLE_NAME + ".STEREOSCOPY = '' "	+
	//					"AND " + TableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE " +
	//					"AND (" + genresCondition + ") " +
	//				"ORDER BY " + TableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING ASC",
	//				MediaLibraryFolder.FILES_NOSORT
	//			);
				addChild(recommendations);
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
					addChild(new RealFile(file, false, true));
					break;
				case EPISODES_WITHIN_SEASON:
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

		if (isDiscovered()) {
			setUpdateId(this.getIntId());
		}
	}

	/**
	 * @param expectedOutput
	 * @return whether any text output is expected (can be in addition to file output)
	 */
	public boolean isTextOutputExpected(int expectedOutput) {
		return expectedOutput == TEXTS ||
			expectedOutput == TEXTS_NOSORT ||
			expectedOutput == TEXTS_NOSORT_WITH_FILTERS ||
			expectedOutput == TEXTS_WITH_FILTERS ||
			expectedOutput == SEASONS ||
			expectedOutput == TVSERIES_WITH_FILTERS ||
			expectedOutput == TVSERIES_NOSORT ||
			expectedOutput == TVSERIES ||
			expectedOutput == EPISODES;
	}

	@Override
	protected String getDisplayNameBase() {
		if (isNotBlank(displayNameOverride)) {
			return displayNameOverride;
		}

		return super.getDisplayNameBase();
	}

	public boolean isTVSeries() {
		return isTVSeries;
	}

	public void setIsTVSeries(boolean value) {
		isTVSeries = value;
	}

	/**
	 * @return a {@link InputStream} that represents the thumbnail used.
	 * @throws IOException
	 *
	 * @see DLNAResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (this.isTVSeries) {
			DLNAThumbnail tvSeriesCover = TableTVSeries.getThumbnailByTitle(this.getDisplayName());
			if (tvSeriesCover != null) {
				return new DLNAThumbnailInputStream(tvSeriesCover);
			}
		}

		try {
			return super.getThumbnailInputStream();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" [id=");
		result.append(id);
		result.append(", name=");
		result.append(getName());
		result.append(", full path=");
		result.append(getResourceId());
		result.append(", discovered=");
		result.append(isDiscovered());
		result.append(", isTVSeries=");
		result.append(isTVSeries());
		result.append(']');
		return result.toString();
	}
}
