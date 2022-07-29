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
import net.pms.database.MediaTableVideoMetadatas;
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
	protected static final String FROM_FILES = "FROM " + MediaTableFiles.TABLE_NAME + " ";
	protected static final String JOIN_VIDEO_METADATAS = "LEFT JOIN " + MediaTableVideoMetadatas.TABLE_NAME + " ON " + MediaTableFiles.ID + " = " + MediaTableVideoMetadatas.FILEID + " ";
	protected static final String JOIN_FILES_STATUS = "LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON " + MediaTableFiles.FILENAME + " = " + MediaTableFilesStatus.FILENAME + " ";
	protected static final String FROM_FILES_VIDEOMETA = FROM_FILES + JOIN_VIDEO_METADATAS;
	protected static final String FROM_FILES_STATUS = FROM_FILES + JOIN_FILES_STATUS;
	protected static final String FROM_FILES_STATUS_VIDEOMETA = FROM_FILES + JOIN_FILES_STATUS + JOIN_VIDEO_METADATAS;
	private static final String UNWATCHED_CONDITION = MediaTableFilesStatus.ISFULLYPLAYED + " IS NOT TRUE AND ";
	private static final String WATCHED_CONDITION = MediaTableFilesStatus.ISFULLYPLAYED + " IS TRUE AND ";
	private static final String SELECT_DISTINCT_TVSEASON = "SELECT DISTINCT " + MediaTableVideoMetadatas.TVSEASON + " " + FROM_FILES_VIDEOMETA;

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
			connection = MediaDatabase.getConnectionIfAvailable();
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

	private static List<String> getTVSeriesQueries(String tableName, String columnName) {
		List<String> queries = new ArrayList<>();
		queries.add("SELECT " + columnName + " FROM " + tableName + " WHERE " + MediaTableTVSeries.CHILD_ID + " IS NOT NULL ORDER BY " + columnName + " ASC");
		queries.add("SELECT " + MediaTableTVSeries.TITLE + " FROM " + MediaTableTVSeries.TABLE_NAME + " LEFT JOIN " + tableName + " ON " + MediaTableTVSeries.ID + " = " + tableName + ".TVSERIESID WHERE " + columnName + " = '${0}' ORDER BY " + MediaTableTVSeries.TITLE + " ASC");
		queries.add("SELECT          *     " + FROM_FILES_VIDEOMETA + "WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableVideoMetadatas.ISTVEPISODE + " AND " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " = '${0}' ORDER BY " + MediaTableVideoMetadatas.TVEPISODENUMBER);
		return queries;
	}

	private static String getFirstNonTVSeriesQuery(String firstSql, String tableName, String columnName) {
		String orderByString = "ORDER BY ";
		int indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();

		String selectSection = "SELECT DISTINCT " + columnName + " FROM FILES ";
		String orderBySection = "ORDER BY " + columnName + " ASC";

		// These queries join tables
		StringBuilder query = new StringBuilder(firstSql);

		// If the query does not already join the right metadata table, do that now
		if (!firstSql.contains("LEFT JOIN " + tableName)) {
			String joinSection = "LEFT JOIN " + tableName + " ON " + MediaTableFiles.ID + " = " + tableName + "." + MediaTableVideoMetadatas.COL_FILEID + " ";
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
		String condition = columnName + " = '${0}' AND ";
		if (!sql.contains("LEFT JOIN " + tableName)) {
			String joinSection = "LEFT JOIN " + tableName + " ON " + MediaTableFiles.ID + " = " + tableName + "." + MediaTableVideoMetadatas.COL_FILEID + " ";
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
								firstSql = firstSql.replaceAll(SELECT_DISTINCT_TVSEASON, "SELECT * " + FROM_FILES_VIDEOMETA);
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
								if (firstSql.indexOf(JOIN_VIDEO_METADATAS) > 0) {
									indexAfterFromInFirstQuery = firstSql.indexOf(JOIN_VIDEO_METADATAS) + JOIN_VIDEO_METADATAS.length();
								}
								String orderBySection = "ORDER BY " + MediaTableVideoMetadatas.TVSEASON;

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
										firstSql = "SELECT " + MediaTableFiles.FILENAME + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + firstSql;
									}
									if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
										firstSql = "SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + firstSql;
									}
								}

								// This block adds the first SQL query for non-TV series, and all queries for TV series
								if (configuration.isUseInfoFromIMDb()) {
									/*
									 * With TV series we manually add the SQL statements, otherwise we
									 * attempt to modify the incoming statements to make filtering versions.
									 */
									if (expectedOutput == TVSERIES_WITH_FILTERS) {
										actorsSqls = getTVSeriesQueries(MediaTableVideoMetadataActors.TABLE_NAME, MediaTableVideoMetadataActors.ACTOR);
										countriesSqls = getTVSeriesQueries(MediaTableVideoMetadataCountries.TABLE_NAME, MediaTableVideoMetadataCountries.COUNTRY);
										directorsSqls = getTVSeriesQueries(MediaTableVideoMetadataDirectors.TABLE_NAME, MediaTableVideoMetadataDirectors.DIRECTOR);
										genresSqls = getTVSeriesQueries(MediaTableVideoMetadataGenres.TABLE_NAME, MediaTableVideoMetadataGenres.GENRE);
										ratedSqls = getTVSeriesQueries(MediaTableVideoMetadataRated.TABLE_NAME, MediaTableVideoMetadataRated.RATED);
										releasedSqls = getTVSeriesQueries(MediaTableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(" + MediaTableVideoMetadataReleased.RELEASEDATE + ", 'yyyy')");
									} else {
										actorsSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataActors.TABLE_NAME, MediaTableVideoMetadataActors.ACTOR));
										countriesSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataCountries.TABLE_NAME, MediaTableVideoMetadataCountries.COUNTRY));
										directorsSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataDirectors.TABLE_NAME, MediaTableVideoMetadataDirectors.DIRECTOR));
										genresSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataGenres.TABLE_NAME, MediaTableVideoMetadataGenres.GENRE));
										ratedSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataRated.TABLE_NAME, MediaTableVideoMetadataRated.RATED));
										releasedSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(" + MediaTableVideoMetadataReleased.RELEASEDATE + ", 'yyyy')"));
									}
								}

								// This block adds the second+ queries by modifying what was passed in, allowing this to be somewhat dynamic
								int i = 0;
								for (String sql : sqls) {
									if (!sql.toLowerCase().startsWith("select") && !sql.toLowerCase().startsWith("with")) {
										if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
											sql = "SELECT " + MediaTableFiles.FILENAME + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + sql;
										}
										if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
											sql = "SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + sql;
										}
									}
									String whereString = "WHERE ";
									int indexAfterFrom = sql.indexOf(FROM_FILES) + FROM_FILES.length();

									// If the query does not already join the FILES_STATUS table, do that now
									StringBuilder sqlWithJoin = new StringBuilder(sql);
									if (!sql.contains("LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME)) {
										sqlWithJoin.insert(indexAfterFrom, JOIN_FILES_STATUS);
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
										actorsSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataActors.TABLE_NAME, MediaTableVideoMetadataActors.ACTOR, i));
										countriesSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataCountries.TABLE_NAME, MediaTableVideoMetadataCountries.COUNTRY, i));
										directorsSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataDirectors.TABLE_NAME, MediaTableVideoMetadataDirectors.DIRECTOR, i));
										genresSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataGenres.TABLE_NAME, MediaTableVideoMetadataGenres.GENRE, i));
										ratedSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataRated.TABLE_NAME, MediaTableVideoMetadataRated.RATED, i));
										releasedSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataReleased.TABLE_NAME, "FORMATDATETIME(" + MediaTableVideoMetadataReleased.RELEASEDATE + ", 'yyyy')", i));
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
				VirtualFolder filterByProgress = new VirtualFolder(Messages.getString("FilterByProgress"), null);
				filterByProgress.addChild(new MediaLibraryFolder(
					Messages.getString("Unwatched"),
					unwatchedSqls.toArray(String[]::new),
					filteredExpectedOutputs
				));
				filterByProgress.addChild(new MediaLibraryFolder(
					Messages.getString("Watched"),
					watchedSqls.toArray(String[]::new),
					filteredExpectedOutputs
				));
				addChild(filterByProgress);
			}
			if (!genresSqls.isEmpty()) {
				VirtualFolder filterByInformation = new VirtualFolder(Messages.getString("FilterByInformation"), null);
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("Actors"),
					actorsSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("Country"),
					countriesSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("Director"),
					directorsSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("Genres"),
					genresSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("Rated"),
					ratedSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					Messages.getString("Released"),
					releasedSqls.toArray(String[]::new),
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
						String condition = MediaTableVideoMetadatas.TVSEASON + " = '" + virtualFolderName + "' AND ";
						episodesWithinSeasonQuery.insert(indexAfterWhere, condition);

						sqls2 = new String[] {transformSQL(episodesWithinSeasonQuery.toString())};
						if (virtualFolderName.length() != 4) {
							nameToDisplay = Messages.getString("Season") + " " + virtualFolderName;
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
							if (resource.getName().equals(Messages.getString("Actors"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataActors.ACTOR + " = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataActors.FILEID + " IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("Country"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataCountries.COUNTRY + " = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataCountries.FILEID + " IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("Director"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataDirectors.DIRECTOR + " = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataDirectors.FILEID + " IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("Genres"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataGenres.GENRE + " = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataGenres.FILEID + " IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("Rated"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE " + MediaTableVideoMetadataRated.RATED + " = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataRated.FILEID + " IS NULL");
								}
								nameToDisplay = "Unknown";
							} else if (resource.getName().equals(Messages.getString("Released"))) {
								for (int i = 0; i < sqls2.length; i++) {
									sqls2[i] = sqls2[i].replace("WHERE FORMATDATETIME(" + MediaTableVideoMetadataReleased.TABLE_NAME + ".RELEASEDATE, 'yyyy') = '${" + i + "}'", "WHERE " + MediaTableVideoMetadataReleased.FILEID + " IS NULL");
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
				Messages.getString("Recommendations"),
				new String[]{
					"WITH ratedSubquery AS (" +
						"SELECT " + MediaTableVideoMetadataRated.RATED + " FROM " + MediaTableVideoMetadataRated.TABLE_NAME + " " +
						"LEFT JOIN " + MediaTableTVSeries.TABLE_NAME + " ON " + MediaTableVideoMetadataRated.TVSERIESID + " = " + MediaTableTVSeries.ID + " " +
						"WHERE " + MediaTableTVSeries.TITLE + " = " + MediaDatabase.sqlQuote(getName()) + " " +
						"LIMIT 1" +
					"), " +
					"genresSubquery AS (" +
						"SELECT " + MediaTableVideoMetadataGenres.GENRE + " FROM " + MediaTableVideoMetadataGenres.TABLE_NAME + " " +
						"LEFT JOIN " + MediaTableTVSeries.TABLE_NAME + " ON " + MediaTableVideoMetadataGenres.TVSERIESID + " = " + MediaTableTVSeries.ID + " " +
						"WHERE " + MediaTableTVSeries.TITLE + " = " + MediaDatabase.sqlQuote(getName()) +
					") " +
					"SELECT " +
						"DISTINCT " + MediaTableTVSeries.TITLE + ", " +
						MediaTableVideoMetadataIMDbRating.IMDBRATING + ", " +
						MediaTableVideoMetadataGenres.GENRE + ", " +
						MediaTableVideoMetadataRated.RATED + " " +
					"FROM " +
						"ratedSubquery, " +
						"genresSubquery, " +
						MediaTableTVSeries.TABLE_NAME + " " +
							"LEFT JOIN " + MediaTableVideoMetadataGenres.TABLE_NAME +     " ON " + MediaTableTVSeries.ID + " = " + MediaTableVideoMetadataGenres.TVSERIESID     + " " +
							"LEFT JOIN " + MediaTableVideoMetadataRated.TABLE_NAME +      " ON " + MediaTableTVSeries.ID + " = " + MediaTableVideoMetadataRated.TVSERIESID      + " " +
							"LEFT JOIN " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + " ON " + MediaTableTVSeries.ID + " = " + MediaTableVideoMetadataIMDbRating.TVSERIESID + " " +
					"WHERE " +
						MediaTableTVSeries.TITLE + " != " + MediaDatabase.sqlQuote(getName()) + " AND " +
						MediaTableVideoMetadataGenres.GENRE + " IN (genresSubquery." + MediaTableVideoMetadataGenres.COL_GENRE + ") AND " +
						MediaTableVideoMetadataRated.RATED  + " = ratedSubquery." + MediaTableVideoMetadataRated.COL_RATED + " " +
					"ORDER BY " + MediaTableVideoMetadataIMDbRating.IMDBRATING + " DESC",
					"SELECT * " + FROM_FILES_VIDEOMETA + "WHERE " + MediaTableFiles.FORMAT_TYPE + " = 4 AND " + MediaTableVideoMetadatas.ISTVEPISODE + " AND " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " = '${0}' ORDER BY " + MediaTableVideoMetadatas.TVSEASON + ", " + MediaTableVideoMetadatas.TVEPISODENUMBER
				},
				new int[]{MediaLibraryFolder.TVSERIES_NOSORT, MediaLibraryFolder.EPISODES}
			);
			addChild(recommendations);
		} else if (expectedOutput == FILES_WITH_FILTERS) {
			if (firstSql != null) {
				if (firstSql.startsWith("SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED)) {
					firstSql = firstSql.replaceFirst("SELECT " + MediaTableFiles.FILENAME + ", " + MediaTableFiles.MODIFIED, "SELECT " + MediaTableVideoMetadatas.MOVIEORSHOWNAME);
				}

				VirtualFolder recommendations = new MediaLibraryFolder(
					Messages.getString("Recommendations"),
					new String[]{
						firstSql,
						"WITH ratedSubquery AS (" +
							"SELECT " + MediaTableVideoMetadataRated.RATED + " FROM " + MediaTableVideoMetadataRated.TABLE_NAME + " " +
							MediaTableVideoMetadataRated.SQL_LEFT_JOIN_TABLE_FILES +
							JOIN_VIDEO_METADATAS +
							"WHERE " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " = '${0}' " +
							"LIMIT 1" +
						"), " +
						"genresSubquery AS (" +
							"SELECT " + MediaTableVideoMetadataGenres.GENRE + " FROM " + MediaTableVideoMetadataGenres.TABLE_NAME + " " +
							MediaTableVideoMetadataGenres.SQL_LEFT_JOIN_TABLE_FILES +
							JOIN_VIDEO_METADATAS +
							"WHERE " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + " = '${0}'" +
						") " +
						"SELECT " +
							"DISTINCT " + MediaTableVideoMetadatas.MOVIEORSHOWNAME + ", " +
							MediaTableFiles.TABLE_NAME + ".*, " +
							MediaTableVideoMetadataIMDbRating.IMDBRATING + ", " +
							MediaTableVideoMetadataGenres.GENRE + ", " +
							MediaTableVideoMetadataRated.RATED + " " +
						"FROM " +
							"ratedSubquery, " +
							"genresSubquery, " +
							MediaTableFiles.TABLE_NAME + " " +
								JOIN_VIDEO_METADATAS +
								MediaTableVideoMetadataGenres.SQL_LEFT_JOIN_TABLE_FILES +
								MediaTableVideoMetadataRated.SQL_LEFT_JOIN_TABLE_FILES +
								MediaTableVideoMetadataIMDbRating.SQL_LEFT_JOIN_TABLE_FILES +
						"WHERE " +
							MediaTableVideoMetadatas.MOVIEORSHOWNAME + " != '${0}' AND " +
							MediaTableVideoMetadataGenres.GENRE + " IN (genresSubquery." + MediaTableVideoMetadataGenres.COL_GENRE + ") AND " +
							MediaTableVideoMetadataRated.RATED  + " = ratedSubquery." + MediaTableVideoMetadataRated.COL_RATED + " " +

						"ORDER BY " + MediaTableVideoMetadataIMDbRating.IMDBRATING + " DESC"
					},
					new int[]{MediaLibraryFolder.MOVIE_FOLDERS, MediaLibraryFolder.FILES_NOSORT_DEDUPED}
				);
				addChild(recommendations);
			}
		}

		for (File file : newFiles) {
			switch (expectedOutput) {
				case FILES, FILES_NOSORT, FILES_NOSORT_DEDUPED, FILES_WITH_FILTERS -> addChild(new RealFile(file));
				case EPISODES -> addChild(new RealFile(file, false, true));
				case EPISODES_WITHIN_SEASON -> addChild(new RealFile(file, true));
				case PLAYLISTS -> addChild(new PlaylistFolder(file));
				case ISOS, ISOS_WITH_FILTERS -> addChild(new DVDISOFile(file));
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
				connection = MediaDatabase.getConnectionIfAvailable();

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
