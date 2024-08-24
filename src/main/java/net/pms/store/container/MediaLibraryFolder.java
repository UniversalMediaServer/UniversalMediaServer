/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.store.container;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudioMetadata;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableFilesStatus;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.database.MediaTableVideoMetadataActors;
import net.pms.database.MediaTableVideoMetadataCountries;
import net.pms.database.MediaTableVideoMetadataDirectors;
import net.pms.database.MediaTableVideoMetadataGenres;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreResource;
import net.pms.store.item.MediaLibraryTvEpisode;
import net.pms.store.item.RealFile;
import net.pms.store.utils.StoreResourceSorter;
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
public class MediaLibraryFolder extends MediaLibraryAbstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaLibraryFolder.class);

	private String[] sqls;
	private int[] expectedOutputs;
	private List<String> populatedVirtualFoldersListFromDb;
	private List<String> populatedFilesListFromDb;

	public MediaLibraryFolder(Renderer renderer, String i18nName, String sql, int expectedOutput) {
		this(renderer, i18nName, new String[]{sql}, new int[]{expectedOutput}, null);
	}

	public MediaLibraryFolder(Renderer renderer, String i18nName, String[] sql, int[] expectedOutput) {
		this(renderer, i18nName, sql, expectedOutput, null);
	}

	public MediaLibraryFolder(Renderer renderer, String i18nName, String[] sql, int[] expectedOutput, String formatString) {
		super(renderer, i18nName, null, formatString);
		this.sqls = sql;
		this.expectedOutputs = expectedOutput;
		setChildrenSorted((expectedOutput == null || expectedOutput.length < 1 || isSortableOutputExpected(expectedOutput[0])));
	}

	@Override
	public void discoverChildren() {
		doRefreshChildren();
		setDiscovered(true);
	}

	private String transformSQL(String sql) {
		int i = 1;
		StoreResource resource = this;
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
					} else if (expectedOutput == EMPTY_FILES_WITH_FILTERS) {
						return false;
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
		queries.add(SELECT + columnName + FROM + tableName + WHERE + MediaTableTVSeries.CHILD_ID + IS_NOT_NULL + ORDER_BY + columnName + ASC);
		queries.add(SELECT + MediaTableTVSeries.TABLE_COL_ID + ", " + MediaTableTVSeries.TABLE_COL_TITLE + FROM + MediaTableTVSeries.TABLE_NAME + LEFT_JOIN + tableName + ON + MediaTableTVSeries.TABLE_COL_ID + EQUAL + tableName + "." + MediaTableTVSeries.CHILD_ID + WHERE + columnName + EQUAL + "'${0}'" + ORDER_BY + MediaTableTVSeries.TABLE_COL_TITLE + ASC);
		queries.add(SELECT_ALL + FROM_FILES_VIDEOMETA_TV_SERIES + WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + AND + MediaTableTVSeries.TABLE_COL_ID + EQUAL + "${0}" + ORDER_BY + MediaTableVideoMetadata.TABLE_COL_FIRST_TVEPISODE);
		return queries;
	}

	private static List<String> getTVSeriesQueries(String columnName, boolean desc) {
		List<String> queries = new ArrayList<>();
		queries.add(SELECT + columnName + FROM + MediaTableTVSeries.TABLE_NAME + ORDER_BY + columnName + (desc ? DESC : ASC));
		queries.add(SELECT + MediaTableTVSeries.TABLE_COL_ID + ", " + MediaTableTVSeries.TABLE_COL_TITLE + FROM + MediaTableTVSeries.TABLE_NAME + WHERE + columnName + EQUAL + "'${0}'" + ORDER_BY + MediaTableTVSeries.TABLE_COL_TITLE + ASC);
		queries.add(SELECT_ALL + FROM_FILES_VIDEOMETA_TV_SERIES + WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + AND + MediaTableTVSeries.TABLE_COL_ID + EQUAL + "${0}" + ORDER_BY + MediaTableVideoMetadata.TABLE_COL_FIRST_TVEPISODE);
		return queries;
	}

	private static List<String> getTVSeriesQueriesByFirstAirDate() {
		List<String> queries = new ArrayList<>();
		queries.add(SELECT + MediaTableTVSeries.FIRSTAIRDATE_FORMATED + FROM + MediaTableTVSeries.TABLE_NAME + ORDER_BY + MediaTableTVSeries.TABLE_COL_STARTYEAR + DESC);
		queries.add(SELECT + MediaTableTVSeries.TABLE_COL_ID + ", " + MediaTableTVSeries.TABLE_COL_TITLE + FROM + MediaTableTVSeries.TABLE_NAME + WHERE + MediaTableTVSeries.FIRSTAIRDATE_FORMATED + EQUAL + "'${0}'" + ORDER_BY + MediaTableTVSeries.TABLE_COL_STARTYEAR + ASC);
		queries.add(SELECT_ALL + FROM_FILES_VIDEOMETA_TV_SERIES + WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + AND + MediaTableTVSeries.TABLE_COL_ID + EQUAL + "${0}" + ORDER_BY + MediaTableVideoMetadata.TABLE_COL_FIRST_TVEPISODE);
		return queries;
	}

	private static String getFirstNonTVSeriesQuery(String firstSql, String tableName, String columnName, boolean desc) {
		int indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();

		String selectSection = SELECT_DISTINCT + columnName + FROM_FILES;
		String orderBySection = ORDER_BY + columnName + (desc ? DESC : ASC);

		// These queries join tables
		StringBuilder query = new StringBuilder(firstSql);

		// If the query does not already join the right metadata table, do that now
		if (tableName != null && !firstSql.contains(LEFT_JOIN + tableName)) {
			String joinSection = LEFT_JOIN + tableName + ON + MediaTableFiles.TABLE_COL_ID + EQUAL + tableName + "." + MediaTableFiles.CHILD_ID;
			query.insert(indexAfterFromInFirstQuery, joinSection);
		}

		indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();

		query.replace(0, indexAfterFromInFirstQuery, selectSection);

		int indexBeforeOrderByInFirstActorsQuery = query.indexOf(ORDER_BY);
		query.replace(indexBeforeOrderByInFirstActorsQuery, query.length(), orderBySection);

		return query.toString();
	}

	private static String getSubsequentNonTVSeriesQuery(String sql, String tableName, String columnName, int i) {
		StringBuilder query = new StringBuilder(sql);
		int indexAfterFrom = sql.indexOf(FROM_FILES) + FROM_FILES.length();
		String condition = columnName + EQUAL + "'${0}'" + AND;
		if (!sql.contains(LEFT_JOIN + tableName)) {
			String joinSection = LEFT_JOIN + tableName + ON + MediaTableFiles.TABLE_COL_ID + EQUAL + tableName + "." + MediaTableFiles.CHILD_ID;
			query.insert(indexAfterFrom, joinSection);
		}
		int indexAfterWhere = query.indexOf(WHERE) + WHERE.length();
		String replacedCondition = condition.replace("${0}", "${" + i + "}");
		query.insert(indexAfterWhere, replacedCondition);
		return query.toString();
	}

	/**
	 * Removes all children and re-adds them
	 */
	@Override
	public synchronized void doRefreshChildren() {
		List<File> filesListFromDb = null;
		List<String> virtualFoldersListFromDb = null;

		List<String> unwatchedSqls = new ArrayList<>();
		List<String> watchedSqls = new ArrayList<>();

		List<String> actorsSqls = new ArrayList<>();
		List<String> countriesSqls = new ArrayList<>();
		List<String> directorsSqls = new ArrayList<>();
		List<String> genresSqls = new ArrayList<>();
		List<String> ratedSqls = new ArrayList<>();
		List<String> ratingSqls = new ArrayList<>();
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
							case FILES, FILES_NOSORT, PLAYLISTS, ISOS, EPISODES_WITHIN_SEASON -> {
								firstSql = firstSql.replaceAll(SELECT_DISTINCT_TVSEASON, SELECT_ALL + FROM_FILES_VIDEOMETA);
								filesListFromDb = MediaTableFiles.getFiles(connection, firstSql);
								populatedFilesListFromDb = MediaTableFiles.getStrings(connection, firstSql);
							}
							case FILES_NOSORT_DEDUPED -> {
								populatedFilesListFromDb = new ArrayList<>();
								filesListFromDb = new ArrayList<>();
								for (File item : MediaTableFiles.getFiles(connection, firstSql)) {
									if (!populatedFilesListFromDb.contains(item.getAbsolutePath())) {
										filesListFromDb.add(item);
										populatedFilesListFromDb.add(item.getAbsolutePath());
									}
								}
							}
							case EPISODES -> {
								filesListFromDb = MediaTableFiles.getFiles(connection, firstSql);
								populatedFilesListFromDb = MediaTableFiles.getStrings(connection, firstSql);

								// Build the season filter folders
								int indexAfterFromInFirstQuery = firstSql.indexOf(FROM_FILES) + FROM_FILES.length();
								int indexAtJointure = firstSql.indexOf(MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA);
								if (indexAtJointure > 0) {
									indexAfterFromInFirstQuery = indexAtJointure + MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA.length();
								}
								String orderBySection = ORDER_BY + MediaTableVideoMetadata.TABLE_COL_TVSEASON;

								seasonsQuery.append(firstSql);
								seasonsQuery.replace(0, indexAfterFromInFirstQuery, SELECT_DISTINCT_TVSEASON);

								int indexBeforeOrderByInFirstQuery = seasonsQuery.indexOf(ORDER_BY);
								seasonsQuery.replace(indexBeforeOrderByInFirstQuery, seasonsQuery.length(), orderBySection);
								virtualFoldersListFromDb = MediaTableFiles.getStrings(connection, seasonsQuery.toString());
								populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
							}
							case TEXTS, TEXTS_NOSORT, SEASONS, TVSERIES, TVSERIES_NOSORT, MOVIE_FOLDERS -> {
								virtualFoldersListFromDb = MediaTableFiles.getStrings(connection, firstSql);
								populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
							}
							case FILES_WITH_FILTERS, ISOS_WITH_FILTERS, TEXTS_NOSORT_WITH_FILTERS, TEXTS_WITH_FILTERS, TVSERIES_WITH_FILTERS, EMPTY_FILES_WITH_FILTERS -> {
								if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS) {
									virtualFoldersListFromDb = MediaTableFiles.getStrings(connection, firstSql);
									populatedVirtualFoldersListFromDb = virtualFoldersListFromDb;
								} else if (expectedOutput == FILES_WITH_FILTERS || expectedOutput == ISOS_WITH_FILTERS) {
									filesListFromDb = MediaTableFiles.getFiles(connection, firstSql);
									populatedFilesListFromDb = MediaTableFiles.getStrings(connection, firstSql);
								}

								if (!firstSql.toUpperCase().startsWith(SELECT)) {
									firstSql = SELECT_FILENAME_MODIFIED_FILES_WHERE + firstSql;
								}

								// This block adds the first SQL query for non-TV series, and all queries for TV series
								if (renderer.getUmsConfiguration().isUseInfoFromIMDb()) {
									/*
									* With TV series we manually add the SQL statements, otherwise we
									* attempt to modify the incoming statements to make filtering versions.
									*/
									if (expectedOutput == TVSERIES_WITH_FILTERS) {
										actorsSqls = getTVSeriesQueries(MediaTableVideoMetadataActors.TABLE_NAME, MediaTableVideoMetadataActors.TABLE_COL_ACTOR);
										countriesSqls = getTVSeriesQueries(MediaTableVideoMetadataCountries.TABLE_NAME, MediaTableVideoMetadataCountries.TABLE_COL_COUNTRY);
										directorsSqls = getTVSeriesQueries(MediaTableVideoMetadataDirectors.TABLE_NAME, MediaTableVideoMetadataDirectors.TABLE_COL_DIRECTOR);
										genresSqls = getTVSeriesQueries(MediaTableVideoMetadataGenres.TABLE_NAME, MediaTableVideoMetadataGenres.TABLE_COL_GENRE);
										ratedSqls = getTVSeriesQueries(MediaTableTVSeries.TABLE_COL_RATED, false);
										ratingSqls = getTVSeriesQueries(MediaTableTVSeries.FLOOR_RATING, true);
										releasedSqls = getTVSeriesQueriesByFirstAirDate();
									} else {
										actorsSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataActors.TABLE_NAME, MediaTableVideoMetadataActors.TABLE_COL_ACTOR, false));
										countriesSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataCountries.TABLE_NAME, MediaTableVideoMetadataCountries.TABLE_COL_COUNTRY, false));
										directorsSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataDirectors.TABLE_NAME, MediaTableVideoMetadataDirectors.TABLE_COL_DIRECTOR, false));
										genresSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadataGenres.TABLE_NAME, MediaTableVideoMetadataGenres.TABLE_COL_GENRE, false));
										ratedSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.TABLE_COL_RATED, false));
										ratingSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.FLOOR_RATING, true));
										releasedSqls.add(getFirstNonTVSeriesQuery(firstSql, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.RELEASEDATE_FORMATED, true));
									}
								}

								// This block adds the second+ queries by modifying what was passed in, allowing this to be somewhat dynamic
								int i = 0;
								for (String sql : sqls) {
									if (!sql.toUpperCase().startsWith(SELECT) && !sql.toUpperCase().startsWith(WITH)) {
										sql = SELECT_FILENAME_MODIFIED_FILES_WHERE + sql;
									}
									int indexAfterFrom = sql.indexOf(FROM_FILES) + FROM_FILES.length();

									// If the query does not already join the FILES_STATUS table, do that now
									StringBuilder sqlWithJoin = new StringBuilder(sql);
									if (!sql.contains(LEFT_JOIN + MediaTableFilesStatus.TABLE_NAME)) {
										sqlWithJoin.insert(indexAfterFrom, MediaTableFiles.SQL_LEFT_JOIN_TABLE_FILES_STATUS);
									}

									int indexAfterWhere = sqlWithJoin.indexOf(WHERE) + WHERE.length();

									StringBuilder unwatchedSql = new StringBuilder(sqlWithJoin);
									unwatchedSql.insert(indexAfterWhere, getUnWatchedCondition(renderer.getAccountUserId()) + AND);
									unwatchedSqls.add(unwatchedSql.toString());

									StringBuilder watchedSql = new StringBuilder(sqlWithJoin);
									watchedSql.insert(indexAfterWhere, getWatchedCondition(renderer.getAccountUserId()) + AND);
									watchedSqls.add(watchedSql.toString());

									// Adds modified versions of the query that filter by metadata
									if (renderer.getUmsConfiguration().isUseInfoFromIMDb() && expectedOutput != TVSERIES_WITH_FILTERS) {
										actorsSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataActors.TABLE_NAME, MediaTableVideoMetadataActors.TABLE_COL_ACTOR, i));
										countriesSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataCountries.TABLE_NAME, MediaTableVideoMetadataCountries.TABLE_COL_COUNTRY, i));
										directorsSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataDirectors.TABLE_NAME, MediaTableVideoMetadataDirectors.TABLE_COL_DIRECTOR, i));
										genresSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadataGenres.TABLE_NAME, MediaTableVideoMetadataGenres.TABLE_COL_GENRE, i));
										ratedSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.TABLE_COL_RATED, i));
										ratingSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.FLOOR_RATING, i));
										releasedSqls.add(getSubsequentNonTVSeriesQuery(sql, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.RELEASEDATE_FORMATED, i));
									}
									i++;
								}
							}
							default -> {
								// nothing to do
							}
						}
						// Output is files
						// Output is folders
						// Output is both
					}
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
		Set<File> newFiles = new LinkedHashSet<>();
		Set<String> newVirtualFolders = new LinkedHashSet<>();
		List<StoreResource> oldFiles = new ArrayList<>();
		List<StoreResource> oldVirtualFolders = new ArrayList<>();

		if (filesListFromDb != null) {
			getChildren().forEach(oldFiles::add);

			for (File file : filesListFromDb) {
				newFiles.add(file);
			}
		}

		if (virtualFoldersListFromDb != null) {
			getChildren().forEach(oldVirtualFolders::add);

			for (String f : virtualFoldersListFromDb) {
				newVirtualFolders.add(f);
			}
		}

		oldFiles.forEach(fileResource -> getChildren().remove(fileResource));

		oldVirtualFolders.forEach(virtualFolderResource -> getChildren().remove(virtualFolderResource));

		// Add filters at the top
		if (expectedOutput == TEXTS_NOSORT_WITH_FILTERS || expectedOutput == TEXTS_WITH_FILTERS || expectedOutput == FILES_WITH_FILTERS || expectedOutput == TVSERIES_WITH_FILTERS || expectedOutput == EMPTY_FILES_WITH_FILTERS) {
			// Convert the expectedOutputs to unfiltered versions
			int[] filteredExpectedOutputs = expectedOutputs.clone();
			switch (filteredExpectedOutputs[0]) {
				case FILES_WITH_FILTERS, EMPTY_FILES_WITH_FILTERS:
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

			if (!unwatchedSqls.isEmpty() && !watchedSqls.isEmpty()) {
				LocalizedStoreContainer filterByProgress = new LocalizedStoreContainer(renderer, "FilterByProgress");
				filterByProgress.addChild(new MediaLibraryFolder(
					renderer,
					"Unwatched",
					unwatchedSqls.toArray(String[]::new),
					filteredExpectedOutputs
				));
				filterByProgress.addChild(new MediaLibraryFolder(
					renderer,
					"Watched",
					watchedSqls.toArray(String[]::new),
					filteredExpectedOutputs
				));
				addChild(filterByProgress);
			}
			if (!genresSqls.isEmpty()) {
				int[] filteredExpectedOutputsWithPrependedTexts = filteredExpectedOutputs.clone();
				filteredExpectedOutputsWithPrependedTexts = ArrayUtils.insert(0, filteredExpectedOutputsWithPrependedTexts, TEXTS);
				int[] filteredExpectedOutputsWithPrependedTextsNoSort = filteredExpectedOutputs.clone();
				filteredExpectedOutputsWithPrependedTextsNoSort = ArrayUtils.insert(0, filteredExpectedOutputsWithPrependedTextsNoSort, TEXTS_NOSORT);

				LocalizedStoreContainer filterByInformation = new LocalizedStoreContainer(renderer, "FilterByInformation");
				filterByInformation.addChild(new MediaLibraryFolder(
					renderer,
					"Actors",
					actorsSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					renderer,
					"Country",
					countriesSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					renderer,
					"Director",
					directorsSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					renderer,
					"Genres",
					genresSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					renderer,
					"Rated",
					ratedSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTexts
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					renderer,
					"Rating",
					ratingSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTextsNoSort
				));
				filterByInformation.addChild(new MediaLibraryFolder(
					renderer,
					"Released",
					releasedSqls.toArray(String[]::new),
					filteredExpectedOutputsWithPrependedTextsNoSort
				));
				LOGGER.trace("filteredExpectedOutputsWithPrependedTexts: " + Arrays.toString(filteredExpectedOutputsWithPrependedTexts));
				LOGGER.trace("genresSqls: " + genresSqls.toString());
				addChild(filterByInformation);
			}
		}

		// Skip adding season folders if there is only one season
		if (!(expectedOutput == EPISODES && newVirtualFolders.size() == 1)) {
			List<StoreResource> newVirtualFoldersResources = new ArrayList<>();
			for (String virtualFolderName : newVirtualFolders) {
				if (virtualFolderName != null && isTextOutputExpected(expectedOutput)) {
					String[] sqls2 = new String[sqls.length - 1];
					int[] expectedOutputs2 = new int[expectedOutputs.length - 1];
					System.arraycopy(sqls, 1, sqls2, 0, sqls2.length);
					System.arraycopy(expectedOutputs, 1, expectedOutputs2, 0, expectedOutputs2.length);

					String i18nName = null;
					if (expectedOutput == EPISODES) {
						expectedOutputs2 = new int[]{MediaLibraryFolder.EPISODES_WITHIN_SEASON};
						StringBuilder episodesWithinSeasonQuery = new StringBuilder(sqls[0]);

						int indexAfterWhere = episodesWithinSeasonQuery.indexOf(WHERE) + WHERE.length();
						String condition = MediaTableVideoMetadata.TABLE_COL_TVSEASON + EQUAL + "'" + virtualFolderName + "'" + AND;
						episodesWithinSeasonQuery.insert(indexAfterWhere, condition);

						sqls2 = new String[] {transformSQL(episodesWithinSeasonQuery.toString())};
						if (virtualFolderName.length() != 4) {
							if ("0".equals(virtualFolderName)) {
								i18nName = "SeasonSpecials";
								virtualFolderName = null;
							} else {
								i18nName = "SeasonX";
							}
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
						MediaLibraryFolder resource = this;

						if (resource.getName() != null && MediaTableFiles.NONAME.equals(virtualFolderName)) {
							switch (resource.getSystemName()) {
								case "Actors" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableVideoMetadataActors.TABLE_COL_ACTOR + EQUAL + "'${" + i + "}'", WHERE + MediaTableVideoMetadataActors.TABLE_COL_FILEID + IS_NULL);
									}
									i18nName = "Unknown";
								}
								case "Country" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableVideoMetadataCountries.TABLE_COL_COUNTRY + EQUAL + "'${" + i + "}'", WHERE + MediaTableVideoMetadataCountries.TABLE_COL_FILEID + IS_NULL);
									}
									i18nName = "Unknown";
								}
								case "Director" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableVideoMetadataDirectors.TABLE_COL_DIRECTOR + EQUAL + "'${" + i + "}'", WHERE + MediaTableVideoMetadataDirectors.TABLE_COL_FILEID + IS_NULL);
									}
									i18nName = "Unknown";
								}
								case "Genres" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableVideoMetadataGenres.TABLE_COL_GENRE + EQUAL + "'${" + i + "}'", WHERE + MediaTableVideoMetadataGenres.TABLE_COL_FILEID + IS_NULL);
									}
									i18nName = "Unknown";
								}
								case "Rated" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableVideoMetadata.TABLE_COL_RATED + EQUAL + "'${" + i + "}'", WHERE + MediaTableVideoMetadata.TABLE_COL_RATED + IS_NULL);
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableTVSeries.TABLE_COL_RATED + EQUAL + "'${" + i + "}'", WHERE + MediaTableTVSeries.TABLE_COL_RATED + IS_NULL);
									}
									i18nName = "Unknown";
								}
								case "Rating" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableVideoMetadata.FLOOR_RATING + EQUAL + "'${" + i + "}'", WHERE + MediaTableVideoMetadata.TABLE_COL_RATING + IS_NULL);
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableTVSeries.FLOOR_RATING + EQUAL + "'${" + i + "}'", WHERE + MediaTableTVSeries.TABLE_COL_RATING + IS_NULL);
									}
									i18nName = "Unknown";
								}
								case "Released" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableVideoMetadata.RELEASEDATE_FORMATED + EQUAL + "'${" + i + "}'", WHERE + MediaTableVideoMetadata.TABLE_COL_RELEASEDATE + IS_NULL);
										sqls2[i] = sqls2[i].replace(WHERE + MediaTableTVSeries.FIRSTAIRDATE_FORMATED + EQUAL + "'${" + i + "}'", WHERE + MediaTableTVSeries.TABLE_COL_FIRSTAIRDATE + IS_NULL);
									}
									i18nName = "Unknown";
								}
								case "ByArtist" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(COALESCE_ARTIST + EQUAL + "'${" + i + "}'", COALESCE_ARTIST + EQUAL + "''");
									}
									i18nName = "Unknown";
								}
								case "ByAlbum" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(MediaTableAudioMetadata.TABLE_COL_ALBUM + EQUAL + "'${" + i + "}'", MediaTableAudioMetadata.TABLE_COL_ALBUM + EQUAL + "''");
									}
									i18nName = "Unknown";
								}
								case "ByGenre" -> {
									for (int i = 0; i < sqls2.length; i++) {
										sqls2[i] = sqls2[i].replace(MediaTableAudioMetadata.TABLE_COL_GENRE + EQUAL + "'${" + i + "}'", MediaTableAudioMetadata.TABLE_COL_GENRE +  EQUAL + "''");
									}
									i18nName = "Unknown";
								}
								default -> {
									//nothing to do
								}
							}
						}
					}
					boolean isExpectedTVSeries = expectedOutput == TVSERIES || expectedOutput == TVSERIES_NOSORT || expectedOutput == TVSERIES_WITH_FILTERS;
					boolean isExpectedTVSeason = expectedOutput == EPISODES;
					boolean isExpectedMovieFolder = expectedOutput == MOVIE_FOLDERS;
					if (isExpectedTVSeries) {
						Long tvSeriesId = getMediaLibraryTvSeriesId(virtualFolderName);
						if (tvSeriesId != null) {
							newVirtualFoldersResources.add(new MediaLibraryTvSeries(renderer, tvSeriesId, sqls2, expectedOutputs2));
						}
					} else if (isExpectedTVSeason) {
						newVirtualFoldersResources.add(new MediaLibraryTvSeason(renderer, i18nName, virtualFolderName, sqls2, expectedOutputs2));
					} else if (isExpectedMovieFolder) {
						String filename = getMediaLibraryMovieFilename(virtualFolderName);
						if (filename != null) {
							newVirtualFoldersResources.add(new MediaLibraryMovieFolder(renderer, virtualFolderName, filename, sqls2, expectedOutputs2));
						}
					} else if (i18nName != null) {
						newVirtualFoldersResources.add(new MediaLibraryFolder(renderer, i18nName, sqls2, expectedOutputs2, virtualFolderName));
					} else {
						newVirtualFoldersResources.add(new MediaLibraryFolderNamed(renderer, virtualFolderName, sqls2, expectedOutputs2, null));
					}
				}
			}
			if (expectedOutput != TEXTS_NOSORT && expectedOutput != TEXTS_NOSORT_WITH_FILTERS && expectedOutput != TVSERIES_NOSORT && expectedOutput != EPISODES) {
				StoreResourceSorter.sortResourcesByTitle(newVirtualFoldersResources);
			}
			for (StoreResource newResource : newVirtualFoldersResources) {
				addChild(newResource);
			}
		}

		// Recommendations for TV series, episodes and movies
		if (expectedOutput == EPISODES) {
			MediaLibraryFolder recommendations = new MediaLibraryFolder(
				renderer,
				"Recommendations",
				new String[]{
					WITH + "ratedSubquery" + AS + "(" +
						SELECT + MediaTableTVSeries.TABLE_COL_RATED + FROM + MediaTableTVSeries.TABLE_NAME +
						WHERE + MediaTableTVSeries.TABLE_COL_ID + EQUAL + getName() +
						LIMIT_1 +
					"), " +
					"genresSubquery" + AS + "(" +
						SELECT + MediaTableVideoMetadataGenres.TABLE_COL_GENRE + FROM + MediaTableTVSeries.TABLE_NAME +
						MediaTableTVSeries.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_GENRES +
						WHERE + MediaTableTVSeries.TABLE_COL_ID + EQUAL + getName() +
					") " +
					SELECT_DISTINCT +
						MediaTableTVSeries.TABLE_COL_ID + ", " +
						MediaTableTVSeries.TABLE_COL_RATING + ", " +
						MediaTableVideoMetadataGenres.TABLE_COL_GENRE + ", " +
						MediaTableTVSeries.TABLE_COL_RATED +
					FROM +
						"ratedSubquery, " +
						"genresSubquery, " +
						MediaTableTVSeries.TABLE_NAME +
						MediaTableTVSeries.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_GENRES +
					WHERE +
						MediaTableTVSeries.TABLE_COL_ID + NOT_EQUAL + getName() + AND +
						MediaTableVideoMetadataGenres.TABLE_COL_GENRE + IN + "(genresSubquery." + MediaTableVideoMetadataGenres.COL_GENRE + ")" + AND +
						MediaTableTVSeries.TABLE_COL_RATED  + EQUAL + "ratedSubquery." + MediaTableTVSeries.COL_RATED +
					ORDER_BY + MediaTableTVSeries.TABLE_COL_RATING + DESC,
					SELECT_ALL + FROM_FILES_VIDEOMETA_TV_SERIES + WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + AND + MediaTableTVSeries.TABLE_COL_ID + EQUAL + "${0}" + ORDER_BY + MediaTableVideoMetadata.TABLE_COL_TVSEASON + ", " + MediaTableVideoMetadata.TABLE_COL_FIRST_TVEPISODE
				},
				new int[]{MediaLibraryFolder.TVSERIES_NOSORT, MediaLibraryFolder.EPISODES}
			);
			addChild(recommendations);
		} else if (expectedOutput == FILES_WITH_FILTERS) {
			if (firstSql != null) {
				MediaLibraryFolder recommendations = new MediaLibraryFolder(
					renderer,
					"Recommendations",
					new String[]{
						firstSql,
						WITH + "ratedSubquery" + AS + "(" +
							SELECT + MediaTableVideoMetadata.TABLE_COL_RATED + FROM + MediaTableVideoMetadata.TABLE_NAME +
							WHERE + MediaTableVideoMetadata.TABLE_COL_FILEID + EQUAL + "${0}" +
							LIMIT_1 +
						"), " +
						"genresSubquery" + AS + "(" +
							SELECT + MediaTableVideoMetadataGenres.TABLE_COL_GENRE + FROM + MediaTableVideoMetadataGenres.TABLE_NAME +
							WHERE + MediaTableVideoMetadataGenres.TABLE_COL_FILEID + EQUAL + "${0}" +
						") " +
						SELECT_DISTINCT +
							MediaTableVideoMetadata.TABLE_COL_FILEID + ", " +
							MediaTableFiles.TABLE_NAME + ".*, " +
							MediaTableVideoMetadata.TABLE_COL_RATING + ", " +
							MediaTableVideoMetadataGenres.TABLE_COL_GENRE + ", " +
							MediaTableVideoMetadata.TABLE_COL_RATED +
						FROM +
							"ratedSubquery, " +
							"genresSubquery, " +
							MediaTableFiles.TABLE_NAME +
							MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA +
							MediaTableVideoMetadata.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_GENRES +
						WHERE +
							MediaTableVideoMetadata.TABLE_COL_FILEID + NOT_EQUAL + "${0}" + AND +
							MediaTableVideoMetadataGenres.TABLE_COL_GENRE + IN + "(genresSubquery." + MediaTableVideoMetadataGenres.COL_GENRE + ")" + AND +
							MediaTableVideoMetadata.TABLE_COL_RATED  + EQUAL + "ratedSubquery." + MediaTableVideoMetadata.COL_RATED +
						ORDER_BY + MediaTableVideoMetadata.TABLE_COL_RATING + DESC
					},
					new int[]{MediaLibraryFolder.MOVIE_FOLDERS, MediaLibraryFolder.FILES_NOSORT_DEDUPED}
				);
				addChild(recommendations);
			}
		}

		List<StoreResource> newFilesResources = new ArrayList<>();
		for (File file : newFiles) {
			if (renderer.hasShareAccess(file)) {
				switch (expectedOutput) {
					case FILES, FILES_NOSORT, FILES_NOSORT_DEDUPED, FILES_WITH_FILTERS -> newFilesResources.add(new RealFile(renderer, file));
					case EPISODES -> newFilesResources.add(new MediaLibraryTvEpisode(renderer, file, false));
					case EPISODES_WITHIN_SEASON -> newFilesResources.add(new MediaLibraryTvEpisode(renderer, file, true));
					case PLAYLISTS -> newFilesResources.add(new PlaylistFolder(renderer, file));
					case ISOS, ISOS_WITH_FILTERS -> newFilesResources.add(new DVDISOFile(renderer, file));
					default -> {
						// nothing to do
					}
				}
			}
		}
		if (expectedOutput != FILES_NOSORT && expectedOutput != FILES_NOSORT_DEDUPED && expectedOutput != EPISODES) {
			StoreResourceSorter.sortResourcesByTitle(newFilesResources);
		}
		for (StoreResource newResource : newFilesResources) {
			addChild(newResource);
		}
		if (isDiscovered()) {
			MediaStoreIds.incrementUpdateId(getLongId());
		}
		sortChildrenIfNeeded();
	}

	/**
	 * @param expectedOutput
	 * @return whether any text output is expected (can be in addition to file output)
	 */
	private static boolean isTextOutputExpected(int expectedOutput) {
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

	/**
	 * @param expectedOutput
	 * @return whether any text output is expected (can be in addition to file output)
	 */
	private static boolean isSortableOutputExpected(int expectedOutput) {
		return expectedOutput != FILES_NOSORT &&
			expectedOutput != TEXTS_NOSORT &&
			expectedOutput != TEXTS_NOSORT_WITH_FILTERS &&
			expectedOutput != TVSERIES_NOSORT &&
			expectedOutput != FILES_NOSORT_DEDUPED &&
			expectedOutput != SEASONS &&
			expectedOutput != EPISODES;
	}

	public Long getMediaLibraryTvSeriesId(String virtualFolderName) {
		try {
			Long tvSeriesId = Long.valueOf(virtualFolderName);
			if (MediaDatabase.isAvailable()) {
				Connection connection = null;
				List<String> filenames = null;
				try {
					connection = MediaDatabase.getConnectionIfAvailable();
					filenames = MediaTableVideoMetadata.getTvEpisodesFilesByTvSeriesId(connection, tvSeriesId);
				} finally {
					MediaDatabase.close(connection);
				}
				if (filenames != null && !filenames.isEmpty()) {
					for (String filename : filenames) {
						File file = new File(filename);
						if (file.exists() && renderer.hasShareAccess(file)) {
							return tvSeriesId;
						}
					}
				}
			}
		} catch (NumberFormatException e) {
			//we need a long, other values are null (wrong db value check)
		}
		return null;
	}

	public String getMediaLibraryMovieFilename(String virtualFolderName) {
		try {
			Long fileId = Long.valueOf(virtualFolderName);
			if (MediaDatabase.isAvailable()) {
				Connection connection = null;
				String filename = null;
				try {
					connection = MediaDatabase.getConnectionIfAvailable();
					filename = MediaTableFiles.getFilenameById(connection, fileId);
				} finally {
					MediaDatabase.close(connection);
				}
				if (filename != null) {
					File file = new File(filename);
					if (file.exists() && renderer.hasShareAccess(file)) {
						return filename;
					}
				}
			}
		} catch (NumberFormatException e) {
			//we need a long, other values are null (wrong db value check)
		}
		return null;
	}

	public boolean isTVSeries() {
		return this instanceof MediaLibraryTvSeries;
	}

	public boolean isMovieFolder() {
		return this instanceof MediaLibraryMovieFolder;
	}

	/**
	 * @return a {@link InputStream} that represents the thumbnail used.
	 * @throws IOException
	 *
	 * @see StoreResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
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
