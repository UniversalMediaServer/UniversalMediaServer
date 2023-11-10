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

import net.pms.database.MediaTableAudioMetadata;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableFilesStatus;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.database.MediaTableVideotracks;
import net.pms.formats.Format;
import net.pms.renderers.Renderer;

abstract class MediaLibraryAbstract extends LocalizedStoreContainer {
	/**
	 * According to the Academy of Motion Picture Arts and Sciences, the American
	 * Film Institute, and the British Film Institute, a feature film runs for
	 * more than 40 minutes.
	 *
	 * @see https://www.oscars.org/sites/oscars/files/93aa_rules.pdf
	 * @see https://www.bfi.org.uk/bfi-national-archive/research-bfi-archive/bfi-filmography/bfi-filmography-faq
	 */
	private static final Double FORTY_MINUTES_IN_SECONDS = 2400.0;

	protected static final String EMPTY_STRING = "''";
	protected static final String TRUE = "TRUE";
	protected static final String NULL = "NULL";

	protected static final String AND = " AND ";
	protected static final String AS = " AS ";
	protected static final String ASC = " ASC";
	protected static final String DESC = " DESC";
	protected static final String NOT_EQUAL = " != ";
	protected static final String EQUAL = " = ";
	protected static final String GREATER_THAN = " > ";
	protected static final String FROM = " FROM ";
	protected static final String NOT = "NOT ";
	protected static final String IS = " IS ";
	protected static final String IN = " IN ";
	protected static final String LEFT_JOIN = " LEFT JOIN ";
	protected static final String NOT_IN = " NOT" + IN;
	protected static final String ON = " ON ";
	protected static final String OR = " OR ";
	protected static final String ORDER_BY = " ORDER BY ";
	protected static final String SELECT = "SELECT ";
	protected static final String SELECT_ALL = SELECT + "*";
	protected static final String SELECT_DISTINCT = SELECT + "DISTINCT ";
	protected static final String WHERE = " WHERE ";
	protected static final String WITH = "WITH ";
	protected static final String LIMIT = " LIMIT ";

	protected static final String LIMIT_1 = LIMIT + 1;
	protected static final String LIMIT_100 = LIMIT + 100;
	protected static final String IS_NULL = IS + NULL;
	protected static final String IS_TRUE = IS + TRUE;
	protected static final String IS_NOT_NULL = IS + NOT + NULL;
	protected static final String IS_NOT_TRUE = IS + NOT + TRUE;

	protected static final String FORMATDATETIME_MODIFIED = "FORMATDATETIME(" + MediaTableFiles.TABLE_COL_MODIFIED + ", 'yyyy MM d')";
	protected static final String COALESCE_ARTIST = "COALESCE(" + MediaTableAudioMetadata.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudioMetadata.TABLE_COL_ARTIST + ")";
	protected static final String FORMAT_TYPE_AUDIO = MediaTableFiles.TABLE_COL_FORMAT_TYPE + EQUAL + Format.AUDIO;
	protected static final String FORMAT_TYPE_IMAGE = MediaTableFiles.TABLE_COL_FORMAT_TYPE + EQUAL + Format.IMAGE;
	protected static final String FORMAT_TYPE_VIDEO = MediaTableFiles.TABLE_COL_FORMAT_TYPE + EQUAL + Format.VIDEO;
	protected static final String FORMAT_TYPE_ISO = MediaTableFiles.TABLE_COL_FORMAT_TYPE + EQUAL + Format.ISO;
	private static final String FULLYPLAYED_CONDITION = MediaTableFilesStatus.TABLE_COL_ISFULLYPLAYED + IS_TRUE;
	private static final String NOT_FULLYPLAYED_CONDITION = MediaTableFilesStatus.TABLE_COL_ISFULLYPLAYED + IS_NOT_TRUE;
	private static final String WAS_PLAYED_CONDITION = MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + IS_NOT_NULL;
	protected static final String TVEPISODE_CONDITION = MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE;
	protected static final String MOVIE_CONDITION = NOT + TVEPISODE_CONDITION + AND + MediaTableVideoMetadata.TABLE_COL_MEDIA_YEAR + IS_NOT_NULL + AND + MediaTableFiles.TABLE_COL_DURATION + GREATER_THAN + FORTY_MINUTES_IN_SECONDS;
	protected static final String IS_NOT_3D_CONDITION = MediaTableFiles.TABLE_COL_ID + NOT_IN + "(" + MediaTableVideotracks.SQL_GET_FILEID_BY_IS3D + ")";
	protected static final String IS_3D_CONDITION = MediaTableFiles.TABLE_COL_ID + IN + "(" + MediaTableVideotracks.SQL_GET_FILEID_BY_IS3D + ")";
	protected static final String IS_VIDEOHD_CONDITION = MediaTableFiles.TABLE_COL_ID + IN + "(" + MediaTableVideotracks.SQL_GET_FILEID_BY_VIDEOHD + ")";
	protected static final String IS_VIDEOSD_CONDITION = MediaTableFiles.TABLE_COL_ID + IN + "(" + MediaTableVideotracks.SQL_GET_FILEID_BY_VIDEOSD + ")";

	protected static final String UNSORTED_CONDITION = NOT + TVEPISODE_CONDITION + AND + "(" + MediaTableVideoMetadata.TABLE_COL_MEDIA_YEAR + IS_NULL + ")";

	protected static final String FROM_FILES = FROM + MediaTableFiles.TABLE_NAME;
	protected static final String FROM_FILES_VIDEOMETA = FROM_FILES + MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA;
	protected static final String FROM_FILES_VIDEOMETA_TV_SERIES = FROM_FILES_VIDEOMETA + MediaTableVideoMetadata.SQL_LEFT_JOIN_TABLE_TV_SERIES;
	protected static final String FROM_FILES_STATUS = FROM_FILES + MediaTableFiles.SQL_LEFT_JOIN_TABLE_FILES_STATUS;
	protected static final String FROM_FILES_STATUS_VIDEOMETA = FROM_FILES_STATUS + MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA;
	protected static final String FROM_FILES_STATUS_VIDEO_TV_SERIES = FROM_FILES_STATUS_VIDEOMETA + MediaTableVideoMetadata.SQL_LEFT_JOIN_TABLE_TV_SERIES;

	protected static final String SELECT_DISTINCT_TVSEASON = SELECT_DISTINCT + MediaTableVideoMetadata.TABLE_COL_TVSEASON + FROM_FILES_STATUS_VIDEOMETA;
	protected static final String SELECT_FILES_STATUS_WHERE = SELECT_ALL + FROM_FILES_STATUS + WHERE;
	protected static final String SELECT_FILES_STATUS_VIDEO_WHERE = SELECT_ALL + FROM_FILES_STATUS_VIDEOMETA + WHERE;
	protected static final String SELECT_FILES_STATUS_VIDEO_TV_SERIES_WHERE = SELECT_ALL + FROM_FILES_STATUS_VIDEO_TV_SERIES + WHERE;
	protected static final String SELECT_FILENAME_FILES_WHERE = SELECT + MediaTableFiles.TABLE_COL_FILENAME + FROM_FILES + WHERE;
	protected static final String SELECT_FILENAME_MODIFIED = SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED;
	protected static final String SELECT_FILENAME_MODIFIED_FILES_WHERE = SELECT_FILENAME_MODIFIED + FROM_FILES + WHERE;

	protected static final int FILES = 0;
	protected static final int TEXTS = 1;
	protected static final int PLAYLISTS = 2;
	protected static final int ISOS = 3;
	protected static final int SEASONS = 4;
	protected static final int FILES_NOSORT = 5;
	protected static final int TEXTS_NOSORT = 6;
	protected static final int EPISODES = 7;
	protected static final int TEXTS_WITH_FILTERS = 8;
	protected static final int FILES_WITH_FILTERS = 9;
	protected static final int TEXTS_NOSORT_WITH_FILTERS = 10;
	protected static final int ISOS_WITH_FILTERS = 11;
	protected static final int TVSERIES_WITH_FILTERS = 12;
	protected static final int TVSERIES = 13;
	protected static final int TVSERIES_NOSORT = 14;
	protected static final int EPISODES_WITHIN_SEASON = 15;
	protected static final int MOVIE_FOLDERS = 16;
	protected static final int FILES_NOSORT_DEDUPED = 17;
	protected static final int EMPTY_FILES_WITH_FILTERS = 18;

	MediaLibraryAbstract(Renderer renderer, String i18nName, String thumbnailIcon) {
		this(renderer, i18nName, thumbnailIcon, null);
	}

	MediaLibraryAbstract(Renderer renderer, String i18nName, String thumbnailIcon, String formatString) {
		super(renderer, i18nName, thumbnailIcon, formatString);
	}

	private static String getUserCondition(int userId) {
		return MediaTableFilesStatus.TABLE_COL_USERID + EQUAL + userId;
	}

	private static String getUserConditionIncludingNull(int userId) {
		return "(" + getUserCondition(userId) + OR + MediaTableFilesStatus.TABLE_COL_USERID + IS_NULL + ")";
	}

	protected static String getWatchedCondition(int userId) {
		return getUserCondition(userId) + AND + FULLYPLAYED_CONDITION;
	}

	protected static String getUnWatchedCondition(int userId) {
		return getUserConditionIncludingNull(userId) + AND + NOT_FULLYPLAYED_CONDITION;
	}

	protected static String getInProgressCondition(int userId) {
		return getWasPlayedCondition(userId) + AND + NOT_FULLYPLAYED_CONDITION;
	}

	protected static String getWasPlayedCondition(int userId) {
		return getUserCondition(userId) + AND + WAS_PLAYED_CONDITION;
	}

}
