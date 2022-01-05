package net.pms.dlna.virtual;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableFilesStatus;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableVideoMetadataActors;
import net.pms.database.MediaTableVideoMetadataCountries;
import net.pms.database.MediaTableVideoMetadataDirectors;
import net.pms.database.MediaTableVideoMetadataGenres;
import net.pms.database.MediaTableVideoMetadataIMDbRating;
import net.pms.database.MediaTableVideoMetadataRated;
import net.pms.database.MediaTableVideoMetadataReleased;
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
	public static final int MOVIE_FOLDERS = 16;
	public static final int FILES_NOSORT_DEDUPED = 17;
	private boolean isTVSeries = false;
	private boolean isMovieFolder = false;
	private String[] sqls;
	private int[] expectedOutputs;
	private String displayNameOverride;
	private ArrayList<String> populatedVirtualFoldersListFromDb;
	private ArrayList<String> populatedFilesListFromDb;
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaLibraryFolder.class);

	public MediaLibraryFolder(String name, String sql, int expectedOutput) {
		this(name, new String[]{sql}, new int[]{expectedOutput}, null, false, false);
	}

	public MediaLibraryFolder(String name, String[] sql, int[] expectedOutput) {
		this(name, sql, expectedOutput, null, false, false);
	}

	public MediaLibraryFolder(String name, String sql, int expectedOutput, String nameToDisplay) {
		this(name, new String[]{sql}, new int[]{expectedOutput}, nameToDisplay, false, false);
	}

	public MediaLibraryFolder(String name, String[] sql, int[] expectedOutput, String nameToDisplay) {
		this(name, sql, expectedOutput, nameToDisplay, false, false);
	}

	public MediaLibraryFolder(String name, String[] sql, int[] expectedOutput, String nameToDisplay, boolean isTVSeriesFolder, boolean isMoviesFolder) {
		super(name, null);
		this.sqls = sql;
		this.expectedOutputs = expectedOutput;
		if (nameToDisplay != null) {
			this.displayNameOverride = nameToDisplay;
		}
		if (isTVSeriesFolder) {
			this.isTVSeries = true;
		}
		if (isMoviesFolder) {
			this.isMovieFolder = true;
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
		if (name.equals(MediaTableFiles.NONAME)) {
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
		Connection connection = null;
		try {
			connection =  MediaDatabase.getConnectionIfAvailable();
			if (connection != null && sqls.length > 0) {
				String sql = sqls[0];
				int expectedOutput = expectedOutputs[0];
				if (sql != null) {
					sql = transformSQL(sql);

					if (
						expectedOutput == EPISODES ||
						expectedOutput == EPISODES_WITHIN_SEASON ||
						expectedOutput == FILES ||
						expectedOutput == FILES_NOSORT ||
						expectedOutput == FILES_NOSORT_DEDUPED ||
						expectedOutput == FILES_WITH_FILTERS ||
						expectedOutput == ISOS ||
						expectedOutput == ISOS_WITH_FILTERS ||
						expectedOutput == PLAYLISTS
					) {
						return !UMSUtils.isListsEqual(populatedFilesListFromDb, MediaTableFiles.getStrings(connection, sql));
					} else if (isTextOutputExpected(expectedOutput)) {
						return !UMSUtils.isListsEqual(populatedVirtualFoldersListFromDb, MediaTableFiles.getStrings(connection, sql));
					}
				}
			} else {
				//database not available
				return false;
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return true;
	}

	final static String FROM_FILES = "FROM FILES ";
	final static String UNWATCHED_CONDITION = MediaTableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE AND ";
	final static String WATCHED_CONDITION = MediaTableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS TRUE AND ";
	final static String SQL_JOIN_SECTION = "LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME ";
	final static String SELECT_DISTINCT_TVSEASON = "SELECT DISTINCT TVSEASON FROM FILES ";

	private static List<String> getTVSeriesQueries(String tableName, String columnName) {
		List<String> queries = new ArrayList<>();
		queries.add("SELECT " + columnName + " FROM " + tableName + " WHERE TVSERIESID > -1 ORDER BY " + columnName + " ASC");
		queries.add("SELECT TITLE              FROM " + MediaTableTVSeries.TABLE_NAME + " LEFT JOIN " + tableName + " ON " + MediaTableTVSeries.TABLE_NAME + ".ID = " + tableName + ".TVSERIESID WHERE " + tableName + "." + columnName + " = '${0}' ORDER BY TITLE ASC");
		queries.add("SELECT          *         FROM FILES WHERE FORMAT_TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${0}' ORDER BY TVEPISODENUMBER");
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
		String firstSql = null;
		if (sqls.length > 0) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					firstSql = sqls[0];
					expectedOutput = expectedOutputs[0];
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
								filesListFromDb = MediaTableFiles.getFiles(connection, firstSql);
								populatedFilesListFromDb = MediaTableFiles.getStrings(connection, firstSql);
								break;
							case FILES_NOSORT_DEDUPED:
								populatedFilesListFromDb = new ArrayList<>();
								filesListFromDb = new ArrayList<>();
								for (File item : MediaTableFiles.getFiles(connection, firstSql)) {
									if (!populatedFilesListFromDb.contains(item.getAbsolutePath())) {
										filesListFromDb.add(item);
										populatedFilesListFromDb.add(item.getAbsolutePath());
									}
								}
								break;
							case EPISODES:
								filesListFromDb = MediaTableFiles.getFiles(connection, firstSql);
								populatedFilesListFromDb = MediaTableFiles.getStrings(connection, firstSql);

								// Build the season filter folders
								String orderByString = "ORDER BY ";
								int indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();

								String orderBySection = "ORDER BY TVSEASON";

								seasonsQuery.append(firstSql);
								seasonsQuery.replace(0, indexAfterFromInFirstQuery, SELECT_DISTINCT_TVSEASON);

								int indexBeforeOrderByInFirstQuery = seasonsQuery.indexOf(orderByString);
								seasonsQuery.replace(indexBeforeOrderByInFirstQuery, seasonsQuery.length(), orderBySection);
								virtualFoldersListFromDb = MediaTableFiles.getStrings(connection, seasonsQuery.toString());
								populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
								break;
							// Output is folders
							case TEXTS:
							case TEXTS_NOSORT:
							case SEASONS:
							case TVSERIES:
							case TVSERIES_NOSORT:
							case MOVIE_FOLDERS:
								virtualFoldersListFromDb = MediaTableFiles.getStrings(connection, firstSql);
								populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
								break;
							// Output is both
							case FILES_WITH_FILTERS:
							case ISOS_WITH_FILTERS:
							case TEXTS_NOSORT_WITH_FILTERS:
							case TEXTS_WITH_FILTERS:
							case TVSERIES_WITH_FILTERS:
								if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
									virtualFoldersListFromDb = MediaTableFiles.getStrings(connection, firstSql);
									populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
								} else if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
									filesListFromDb = MediaTableFiles.getFiles(connection, firstSql);
									populatedFilesListFromDb = MediaTableFiles.getStrings(connection, firstSql);
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
										actorsSqls = getTVSeriesQueries(MediaTableVideoMetadataActors.TABLE_NAME, "ACTOR");
										countriesSqls = getTVSeriesQueries(MediaTableVideoMetadataCountries.TABLE_NAME, "COUNTRY");
										directorsSqls = getTVSeriesQueries(MediaTableVideoMetadataDirectors.TABLE_NAME, "DIRECTOR");
										genresSqls = getTVSeriesQueries(MediaTableVideoMetadataGenres.TABLE_NAME, "GENRE");
										ratedSqls = getTVSeriesQueries(MediaTableVideoMetadataRated.TABLE_NAME, "RATING");
										releasedSqls = getTVSeriesQueries(MediaTableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(RELEASEDATE, 'yyyy')");
									} else {
										actorsSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataActors.TABLE_NAME, "ACTOR"));
										countriesSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataCountries.TABLE_NAME, "COUNTRY"));
										directorsSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataDirectors.TABLE_NAME, "DIRECTOR"));
										genresSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataGenres.TABLE_NAME, "GENRE"));
										ratedSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataRated.TABLE_NAME, "RATING"));
										releasedSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(RELEASEDATE, 'yyyy')"));
									}
								}

								// This block adds the second+ queries by modifying what was passed in, allowing this to be somewhat dynamic
								int i = 0;
								for (String sql : sqls) {
									if (!sql.toLowerCase().startsWith("select") && !sql.toLowerCase().startsWith("with")) {
										if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
											sql = "SELECT FILES.FILENAME FROM FILES WHERE " + sql;
										}
										if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
											sql = "SELECT FILES.FILENAME, FILES.MODIFIED FROM FILES WHERE " + sql;
										}
									}
									String whereString = "WHERE ";
									int indexAfterFrom = sql.indexOf(FROM_FILES) + FROM_FILES.length();

									// If the query does not already join the FILES_STATUS table, do that now
									StringBuilder sqlWithJoin = new StringBuilder(sql);
									if (!sql.contains("LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME)) {
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
										actorsSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataActors.TABLE_NAME, "ACTOR", i));
										countriesSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataCountries.TABLE_NAME, "COUNTRY", i));
										directorsSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataDirectors.TABLE_NAME, "DIRECTOR", i));
										genresSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataGenres.TABLE_NAME, "GENRE", i));
										ratedSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataRated.TABLE_NAME, "RATING", i));
										releasedSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(RELEASEDATE, 'yyyy')", i));
									}
									i++;
								}

								break;
							default:
								break;
						}
					}
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
		ArrayList<File> newFiles = new ArrayList<>();
		ArrayList<String> newVirtualFolders = new ArrayList<>();
		ArrayList<DLNAResource> oldFiles = new ArrayList<>();
		ArrayList<DLNAResource> oldVirtualFolders = new ArrayList<>();

		if (filesListFromDb != null) {
			if (expectedOutput != FILES_NOSORT && expectedOutput != FILES_NOSORT_DEDUPED) {
				UMSUtils.sort(filesListFromDb, PMS.getConfiguration().getSortMethod(null));
			}

			getChildren().forEach(child -> {
				oldFiles.add(child);
			});

			for (File file : filesListFromDb) {
				newFiles.add(file);
			}
		}

		if (virtualFoldersListFromDb != null) {
			if (expectedOutput != TEXTS_NOSORT && expectedOutput != TEXTS_NOSORT_WITH_FILTERS && expectedOutput != TVSERIES_NOSORT) {
				UMSUtils.sort(virtualFoldersListFromDb, PMS.getConfiguration().getSortMethod(null));
			}

			getChildren().forEach(child -> {
				oldVirtualFolders.add(child);
			});

			for (String f : virtualFoldersListFromDb) {
				newVirtualFolders.add(f);
			}
		}

		oldFiles.forEach(fileResource -> {
			getChildren().remove(fileResource);
		});

		oldVirtualFolders.forEach(virtualFolderResource -> {
			getChildren().remove(virtualFolderResource);
		});

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
					Messages.getString("VirtualFolder.Director"),
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

		// Skip adding season folders if there is only one season
		if (!(expectedOutput == EPISODES && newVirtualFolders.size() == 1)) {
			for (String virtualFolderName : newVirtualFolders) {
				if (isTextOutputExpected(expectedOutput)) {
					String[] sqls2 = new String[sqls.length - 1];
					int[] expectedOutputs2 = new int[expectedOutputs.length - 1];
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

						sqls2 = new String[] {transformSQL(episodesWithinSeasonQuery.toString())};
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
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataActors.TABLE_NAME + ".ACTOR = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataActors.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Country"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataCountries.TABLE_NAME + ".COUNTRY = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataCountries.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Director"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataDirectors.TABLE_NAME + ".DIRECTOR = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataDirectors.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Genres"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataGenres.TABLE_NAME + ".GENRE = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataGenres.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Rated"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataRated.TABLE_NAME + ".RATING = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataRated.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("VirtualFolder.Released"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE FORMATDATETIME(" + MediaTableVideoMetadataReleased.TABLE_NAME + ".RELEASEDATE, 'yyyy') = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataReleased.TABLE_NAME + ".FILENAME IS NULL");
								}
								nameToDisplay = "Unknown";
							}
						}
					}
					boolean isExpectedTVSeries = expectedOutput == TVSERIES || expectedOutput == TVSERIES_NOSORT || expectedOutput == TVSERIES_WITH_FILTERS;
					boolean isExpectedMovieFolder = expectedOutput == MOVIE_FOLDERS;
					addChild(new MediaLibraryFolder(virtualFolderName, sqls2, expectedOutputs2, nameToDisplay, isExpectedTVSeries, isExpectedMovieFolder));
				}
			}
		}

		// Recommendations for TV series, episodes and movies
		if (expectedOutput == EPISODES) {
			VirtualFolder recommendations = new MediaLibraryFolder(
				Messages.getString("MediaLibrary.Recommendations"),
				new String[]{
					"WITH ratedSubquery AS (" +
						"SELECT RATING FROM " + MediaTableVideoMetadataRated.TABLE_NAME + " " +
						"LEFT JOIN " + MediaTableTVSeries.TABLE_NAME + " ON " + MediaTableVideoMetadataRated.TABLE_NAME + ".TVSERIESID = " + MediaTableTVSeries.TABLE_NAME + ".ID " +
						"WHERE " + MediaTableTVSeries.TABLE_NAME + ".TITLE = " + MediaDatabase.sqlQuote(getName()) + " " +
						"LIMIT 1" +
					"), " +
					"genresSubquery AS (" +
						"SELECT GENRE FROM " + MediaTableVideoMetadataGenres.TABLE_NAME + " " +
						"LEFT JOIN " + MediaTableTVSeries.TABLE_NAME + " ON " + MediaTableVideoMetadataGenres.TABLE_NAME + ".TVSERIESID = " + MediaTableTVSeries.TABLE_NAME + ".ID " +
						"WHERE " + MediaTableTVSeries.TABLE_NAME + ".TITLE = " + MediaDatabase.sqlQuote(getName()) +
					") " +
					"SELECT " +
						"DISTINCT " + MediaTableTVSeries.TABLE_NAME + ".TITLE, " +
						MediaTableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING, " +
						MediaTableVideoMetadataGenres.TABLE_NAME + ".GENRE, " +
						MediaTableVideoMetadataRated.TABLE_NAME + ".RATING " +
					"FROM " +
						"ratedSubquery, " +
						"genresSubquery, " +
						MediaTableTVSeries.TABLE_NAME + " " +
							"LEFT JOIN " + MediaTableVideoMetadataGenres.TABLE_NAME +     " ON " + MediaTableTVSeries.TABLE_NAME + ".ID = " + MediaTableVideoMetadataGenres.TABLE_NAME     + ".TVSERIESID " +
							"LEFT JOIN " + MediaTableVideoMetadataRated.TABLE_NAME +      " ON " + MediaTableTVSeries.TABLE_NAME + ".ID = " + MediaTableVideoMetadataRated.TABLE_NAME      + ".TVSERIESID " +
							"LEFT JOIN " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + " ON " + MediaTableTVSeries.TABLE_NAME + ".ID = " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + ".TVSERIESID " +
					"WHERE " +
						MediaTableTVSeries.TABLE_NAME + ".TITLE != " + MediaDatabase.sqlQuote(getName()) + " AND " +
						MediaTableVideoMetadataGenres.TABLE_NAME + ".GENRE IN (genresSubquery.GENRE) AND " +
						MediaTableVideoMetadataRated.TABLE_NAME  + ".RATING = ratedSubquery.RATING " +
					"ORDER BY " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING DESC",
					"SELECT * FROM FILES WHERE FORMAT_TYPE = 4 AND ISTVEPISODE AND MOVIEORSHOWNAME = '${0}' ORDER BY TVSEASON, TVEPISODENUMBER"
				},
				new int[]{MediaLibraryFolder.TVSERIES_NOSORT, MediaLibraryFolder.EPISODES}
			);
			addChild(recommendations);
		} else if (expectedOutput == FILES_WITH_FILTERS) {
			if (firstSql != null) {
				if (firstSql.startsWith("SELECT FILES.FILENAME, FILES.MODIFIED")) {
					firstSql = firstSql.replaceFirst("SELECT FILES.FILENAME, FILES.MODIFIED", "SELECT FILES.MOVIEORSHOWNAME");
				}

				VirtualFolder recommendations = new MediaLibraryFolder(
					Messages.getString("MediaLibrary.Recommendations"),
					new String[]{
						firstSql,
						"WITH ratedSubquery AS (" +
							"SELECT RATING FROM " + MediaTableVideoMetadataRated.TABLE_NAME + " " +
							"LEFT JOIN " + MediaTableFiles.TABLE_NAME + " ON " + MediaTableVideoMetadataRated.TABLE_NAME + ".FILENAME = " + MediaTableFiles.TABLE_NAME + ".FILENAME " +
							"WHERE " + MediaTableFiles.TABLE_NAME + ".MOVIEORSHOWNAME = '${0}' " +
							"LIMIT 1" +
						"), " +
						"genresSubquery AS (" +
							"SELECT GENRE FROM " + MediaTableVideoMetadataGenres.TABLE_NAME + " " +
							"LEFT JOIN " + MediaTableFiles.TABLE_NAME + " ON " + MediaTableVideoMetadataGenres.TABLE_NAME + ".FILENAME = " + MediaTableFiles.TABLE_NAME + ".FILENAME " +
							"WHERE " + MediaTableFiles.TABLE_NAME + ".MOVIEORSHOWNAME = '${0}'" +
						") " +
						"SELECT " +
							"DISTINCT " + MediaTableFiles.TABLE_NAME + ".MOVIEORSHOWNAME, " +
							MediaTableFiles.TABLE_NAME + ".*, " +
							MediaTableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING, " +
							MediaTableVideoMetadataGenres.TABLE_NAME + ".GENRE, " +
							MediaTableVideoMetadataRated.TABLE_NAME + ".RATING " +
						"FROM " +
							"ratedSubquery, " +
							"genresSubquery, " +
							MediaTableFiles.TABLE_NAME + " " +
								"LEFT JOIN " + MediaTableVideoMetadataGenres.TABLE_NAME +     " ON " + MediaTableFiles.TABLE_NAME + ".FILENAME = " + MediaTableVideoMetadataGenres.TABLE_NAME     + ".FILENAME " +
								"LEFT JOIN " + MediaTableVideoMetadataRated.TABLE_NAME +      " ON " + MediaTableFiles.TABLE_NAME + ".FILENAME = " + MediaTableVideoMetadataRated.TABLE_NAME      + ".FILENAME " +
								"LEFT JOIN " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + " ON " + MediaTableFiles.TABLE_NAME + ".FILENAME = " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + ".FILENAME " +
						"WHERE " +
							MediaTableFiles.TABLE_NAME + ".MOVIEORSHOWNAME != '${0}' AND " +
							MediaTableVideoMetadataGenres.TABLE_NAME + ".GENRE IN (genresSubquery.GENRE) AND " +
							MediaTableVideoMetadataRated.TABLE_NAME  + ".RATING = ratedSubquery.RATING " +

						"ORDER BY " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + ".IMDBRATING DESC"
					},
					new int[]{MediaLibraryFolder.MOVIE_FOLDERS, MediaLibraryFolder.FILES_NOSORT_DEDUPED}
				);
				addChild(recommendations);
			}
		}

		for (File file : newFiles) {
			switch (expectedOutput) {
				case FILES:
				case FILES_NOSORT:
				case FILES_NOSORT_DEDUPED:
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
			bumpSystemUpdateId();
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
			expectedOutput == EPISODES ||
			expectedOutput == MOVIE_FOLDERS;
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

	public boolean isMovieFolder() {
		return isMovieFolder;
	}

	public void setIsMovieFolder(boolean value) {
		isMovieFolder = value;
	}

	/**
	 * @return a {@link InputStream} that represents the thumbnail used.
	 * @throws IOException
	 *
	 * @see DLNAResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (MediaDatabase.isAvailable()) {
			Connection connection = null;
			try {
				if (this.isTVSeries) {
					DLNAThumbnail tvSeriesCover = MediaTableTVSeries.getThumbnailByTitle(connection, this.getDisplayName());
					if (tvSeriesCover != null) {
						return new DLNAThumbnailInputStream(tvSeriesCover);
					}
				}

				if (this.isMovieFolder) {
					DLNAThumbnail movieCover = MediaTableFiles.getThumbnailByTitle(connection, this.getDisplayName());
					if (movieCover != null) {
						return new DLNAThumbnailInputStream(movieCover);
					}
				}
			} finally {
				MediaDatabase.close(connection);
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
		result.append(getId());
		result.append(", name=");
		result.append(getName());
		result.append(", full path=");
		result.append(getResourceId());
		result.append(", discovered=");
		result.append(isDiscovered());
		result.append(", isTVSeries=");
		result.append(isTVSeries());
		result.append(", isMovieFolder=");
		result.append(isMovieFolder());
		result.append(']');
		return result.toString();
	}
}
