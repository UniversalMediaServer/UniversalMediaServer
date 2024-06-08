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
import net.pms.database.MediaTableRegexpRules;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.database.MediaTableVideotracks;
import net.pms.renderers.Renderer;
import net.pms.util.FullyPlayedAction;

/**
 * This is the Media Library folder which contains dynamic folders populated
 * by the SQL (h2) database.
 */
public class MediaLibrary extends MediaLibraryAbstract {

	private MediaLibraryFolder allFolder;
	private MediaLibraryFolder albumFolder;
	private MediaLibraryFolder artistFolder;
	private MediaLibraryFolder genreFolder;
	private MediaLibraryFolder playlistFolder;
	private LocalizedStoreContainer vfAudio = null;

	public MediaLibrary(Renderer renderer) {
		super(renderer, "MediaLibrary", "/images/store/media-library.png");
		addVideoFolder();
		addAudioFolder();
		addImageFolder();
	}

	private void addVideoFolder() {
		// Videos folder
		LocalizedStoreContainer vfVideo = new LocalizedStoreContainer(renderer, "Video");

		// All videos that are unwatched
		MediaLibraryFolder unwatchedTvShowsFolder = new MediaLibraryFolder(
			renderer,
			"TvShows",
			new String[]{
				SELECT_DISTINCT + MediaTableTVSeries.TABLE_COL_ID + ", " + MediaTableTVSeries.TABLE_COL_TITLE + FROM_FILES_STATUS_VIDEO_TV_SERIES + WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableTVSeries.TABLE_COL_TITLE + ASC,
				SELECT_FILES_STATUS_VIDEO_TV_SERIES_WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + AND + getUnWatchedCondition(renderer.getAccountUserId()) + AND + MediaTableTVSeries.TABLE_COL_ID + EQUAL + "'${0}'" + ORDER_BY + MediaTableVideoMetadata.TABLE_COL_TVSEASON + ", " + MediaTableVideoMetadata.TABLE_COL_FIRST_TVEPISODE
			},
			new int[]{TVSERIES_WITH_FILTERS, EPISODES}
		);

		MediaLibraryFolder unwatchedMoviesFolder = new MediaLibraryFolder(renderer, "Movies", SELECT_FILES_STATUS_WHERE + FORMAT_TYPE_VIDEO + AND + MOVIE_CONDITION + AND + getUnWatchedCondition(renderer.getAccountUserId()) + AND + IS_NOT_3D_CONDITION + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC, FILES);
		MediaLibraryFolder unwatchedMovies3DFolder = new MediaLibraryFolder(renderer, "3dMovies", SELECT_FILES_STATUS_WHERE + FORMAT_TYPE_VIDEO + AND + MOVIE_CONDITION + AND + getUnWatchedCondition(renderer.getAccountUserId()) + AND + IS_3D_CONDITION + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC, FILES);
		MediaLibraryFolder unwatchedUnsortedFolder = new MediaLibraryFolder(renderer, "Unsorted", SELECT_FILES_STATUS_WHERE + FORMAT_TYPE_VIDEO + AND + UNSORTED_CONDITION + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC, FILES);
		MediaLibraryFolder unwatchedRecentlyAddedVideos = new MediaLibraryFolder(renderer, "RecentlyAdded", SELECT_FILES_STATUS_WHERE + FORMAT_TYPE_VIDEO + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_MODIFIED + DESC + LIMIT_100, FILES_NOSORT);
		MediaLibraryFolder unwatchedAllVideosFolder = new MediaLibraryFolder(renderer, "AllVideos", SELECT_FILES_STATUS_WHERE  + FORMAT_TYPE_VIDEO + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC, FILES);
		MediaLibraryFolder unwatchedVideosByDate = new MediaLibraryFolder(
			renderer,
			"ByDate",
			new String[]{
				SELECT + FORMATDATETIME_MODIFIED + FROM_FILES_STATUS_VIDEOMETA + WHERE + FORMAT_TYPE_VIDEO + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_MODIFIED + DESC,
				SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + FORMATDATETIME_MODIFIED + EQUAL + "'${0}'" + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC
			},
			new int[]{TEXTS_NOSORT, FILES}
		);
		MediaLibraryFolder unwatchedMlfVideo03 = new MediaLibraryFolder(renderer, "HdVideos", SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + MediaTableFiles.TABLE_COL_ID + IN + "(" + MediaTableVideotracks.SQL_GET_FILEID_BY_VIDEOHD + ")" + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC, FILES);
		MediaLibraryFolder unwatchedMlfVideo04 = new MediaLibraryFolder(renderer, "SdVideos", SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + MediaTableFiles.TABLE_COL_ID + IN + "(" + MediaTableVideotracks.SQL_GET_FILEID_BY_VIDEOSD + ")" + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC, FILES);
		MediaLibraryFolder unwatchedMlfVideo05 = new MediaLibraryFolder(renderer, "DvdImages", SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_ISO + AND + getUnWatchedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC, ISOS);

		// The following block contains all videos regardless of fully played status
		MediaLibraryFolder tvShowsFolder = new MediaLibraryFolder(
			renderer,
			"TvShows",
			new String[]{
				SELECT_DISTINCT + MediaTableTVSeries.TABLE_COL_ID + ", " + MediaTableTVSeries.TABLE_COL_TITLE + FROM_FILES_VIDEOMETA_TV_SERIES + WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + ORDER_BY + MediaTableTVSeries.TABLE_COL_TITLE + ASC,
				SELECT_ALL + FROM_FILES_VIDEOMETA_TV_SERIES + WHERE + FORMAT_TYPE_VIDEO + AND + TVEPISODE_CONDITION + AND + MediaTableTVSeries.TABLE_COL_ID + EQUAL + "'${0}'" + ORDER_BY + MediaTableVideoMetadata.TABLE_COL_TVSEASON + ", " + MediaTableVideoMetadata.TABLE_COL_FIRST_TVEPISODE
			},
			new int[]{TVSERIES_WITH_FILTERS, EPISODES}
		);
		MediaLibraryFolder moviesFolder = new MediaLibraryFolder(
			renderer,
			"Movies",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + MOVIE_CONDITION + AND + IS_NOT_3D_CONDITION + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES_WITH_FILTERS
		);
		MediaLibraryFolder movies3DFolder = new MediaLibraryFolder(
			renderer,
			"3dMovies",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + MOVIE_CONDITION + AND + IS_3D_CONDITION + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES_WITH_FILTERS
		);
		MediaLibraryFolder unsortedFolder = new MediaLibraryFolder(
			renderer,
			"Unsorted",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + UNSORTED_CONDITION + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES_WITH_FILTERS
		);
		MediaLibraryFolder allVideosFolder = new MediaLibraryFolder(
			renderer,
			"AllVideos",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES_WITH_FILTERS
		);
		MediaLibraryFolder recentlyAddedVideos = new MediaLibraryFolder(
			renderer,
			"RecentlyAdded",
			new String[]{SELECT_ALL + FROM_FILES_VIDEOMETA + WHERE + FORMAT_TYPE_VIDEO + ORDER_BY + MediaTableFiles.TABLE_COL_MODIFIED + DESC + LIMIT_100},
			new int[]{FILES_NOSORT}
		);
		MediaLibraryFolder inProgressVideos = new MediaLibraryFolder(
			renderer,
			"InProgress",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + getInProgressCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + DESC + LIMIT_100,
			FILES_NOSORT
		);
		MediaLibraryFolder mostPlayedVideos = new MediaLibraryFolder(
			renderer,
			"MostPlayed",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + getWasPlayedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFilesStatus.TABLE_COL_PLAYCOUNT + DESC + LIMIT_100,
			FILES_NOSORT
		);
		MediaLibraryFolder videosByDate = new MediaLibraryFolder(
			renderer,
			"ByDate",
			new String[]{
				SELECT + FORMATDATETIME_MODIFIED + FROM_FILES + WHERE + FORMAT_TYPE_VIDEO + ORDER_BY + MediaTableFiles.TABLE_COL_MODIFIED + DESC,
				FORMAT_TYPE_VIDEO + AND + FORMATDATETIME_MODIFIED + EQUAL + "'${0}'" + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC
			},
			new int[]{TEXTS_NOSORT_WITH_FILTERS, FILES}
		);
		MediaLibraryFolder mlfVideo03 = new MediaLibraryFolder(
			renderer,
			"HdVideos",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + IS_VIDEOHD_CONDITION + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo04 = new MediaLibraryFolder(
			renderer,
			"SdVideos",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + IS_VIDEOSD_CONDITION + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES_WITH_FILTERS
		);
		MediaLibraryFolder mlfVideo05 = new MediaLibraryFolder(
			renderer,
			"DvdImages",
			SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_ISO + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			ISOS_WITH_FILTERS
		);

		// If fully played videos are to be hidden
		if (renderer.getUmsConfiguration().getFullyPlayedAction() == FullyPlayedAction.HIDE_MEDIA) {
			vfVideo.addChild(unwatchedTvShowsFolder);
			vfVideo.addChild(unwatchedMoviesFolder);
			vfVideo.addChild(unwatchedMovies3DFolder);
			vfVideo.addChild(unwatchedUnsortedFolder);
			vfVideo.addChild(unwatchedRecentlyAddedVideos);
			vfVideo.addChild(inProgressVideos);
			vfVideo.addChild(unwatchedAllVideosFolder);
			vfVideo.addChild(unwatchedVideosByDate);
			vfVideo.addChild(unwatchedMlfVideo03);
			vfVideo.addChild(unwatchedMlfVideo04);
			vfVideo.addChild(unwatchedMlfVideo05);
		// If fully played videos are NOT to be hidden
		} else {
			vfVideo.addChild(tvShowsFolder);
			vfVideo.addChild(moviesFolder);
			vfVideo.addChild(movies3DFolder);
			vfVideo.addChild(unsortedFolder);
			vfVideo.addChild(recentlyAddedVideos);
			if (renderer.getUmsConfiguration().isShowRecentlyPlayedFolder()) {
				MediaLibraryFolder recentlyPlayedVideos = new MediaLibraryFolder(
					renderer,
					"RecentlyPlayed",
					SELECT_FILES_STATUS_VIDEO_WHERE + FORMAT_TYPE_VIDEO + AND + getWasPlayedCondition(renderer.getAccountUserId()) + ORDER_BY + MediaTableFilesStatus.TABLE_COL_DATELASTPLAY + DESC + LIMIT_100,
					FILES_NOSORT
				);
				vfVideo.addChild(recentlyPlayedVideos);
			}
			vfVideo.addChild(inProgressVideos);
			vfVideo.addChild(mostPlayedVideos);
			vfVideo.addChild(allVideosFolder);
			vfVideo.addChild(videosByDate);
			vfVideo.addChild(mlfVideo03);
			vfVideo.addChild(mlfVideo04);
			vfVideo.addChild(mlfVideo05);
		}
		addChild(vfVideo);
	}

	private void addAudioFolder() {
		vfAudio = new LocalizedStoreContainer(renderer, "Audio");
		allFolder = new MediaLibraryFolder(
			renderer,
			"AllAudioTracks",
			SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES
		);
		vfAudio.addChild(allFolder);
		playlistFolder = new MediaLibraryFolder(
			renderer,
			"AllAudioPlaylists",
			SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + WHERE + FORMAT_TYPE_AUDIO + "6" + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			PLAYLISTS
		);
		vfAudio.addChild(playlistFolder);
		artistFolder = new MediaLibraryFolder(
			renderer,
			"ByArtist",
			new String[]{
				SELECT_DISTINCT + COALESCE_ARTIST + AS + "ARTIST" + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + ORDER_BY + "ARTIST" + ASC,
				SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + COALESCE_ARTIST + EQUAL + "'${0}'"
			},
			new int[]{TEXTS, FILES}
		);
		vfAudio.addChild(artistFolder);
		albumFolder = new MediaLibraryFolder(
			renderer,
			"ByAlbum",
			new String[]{
				SELECT_DISTINCT + MediaTableAudioMetadata.TABLE_COL_ALBUM + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_ALBUM + ASC,
				SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + MediaTableAudioMetadata.TABLE_COL_ALBUM + EQUAL + "'${0}'"
			},
			new int[]{TEXTS, FILES}
		);
		vfAudio.addChild(albumFolder);
		genreFolder = new MediaLibraryFolder(
			renderer,
			"ByGenre",
			new String[]{
				SELECT_DISTINCT + MediaTableAudioMetadata.TABLE_COL_GENRE + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_GENRE + ASC,
				SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + MediaTableAudioMetadata.TABLE_COL_GENRE + EQUAL + "'${0}'"
			},
			new int[]{TEXTS, FILES}
		);
		vfAudio.addChild(genreFolder);
		MediaLibraryFolder mlf6 = new MediaLibraryFolder(
			renderer,
			"ByArtistAlbum",
			new String[]{
				SELECT_DISTINCT + COALESCE_ARTIST + AS + "ARTIST" + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + ORDER_BY + "ARTIST" + ASC,
				SELECT_DISTINCT + MediaTableAudioMetadata.TABLE_COL_ALBUM + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + COALESCE_ARTIST + EQUAL + "'${0}'" + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_ALBUM + ASC,
				SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + COALESCE_ARTIST + EQUAL + "'${1}'" + AND + MediaTableAudioMetadata.TABLE_COL_ALBUM + EQUAL + "'${0}'" + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_TRACK + ASC + ", " + MediaTableFiles.TABLE_COL_FILENAME + ASC
			},
			new int[]{TEXTS, TEXTS, FILES}
		);
		vfAudio.addChild(mlf6);
		MediaLibraryFolder mlf7 = new MediaLibraryFolder(
			renderer,
			"ByGenreArtistAlbum",
			new String[]{
				SELECT_DISTINCT + MediaTableAudioMetadata.TABLE_COL_GENRE + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_GENRE + ASC,
				SELECT_DISTINCT + COALESCE_ARTIST + AS + "ARTIST" + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + MediaTableAudioMetadata.TABLE_COL_GENRE + EQUAL + "'${0}'" + ORDER_BY + "ARTIST" + ASC,
				SELECT_DISTINCT + MediaTableAudioMetadata.TABLE_COL_ALBUM + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + MediaTableAudioMetadata.TABLE_COL_GENRE + EQUAL + "'${1}' AND " + COALESCE_ARTIST + EQUAL + "'${0}'" + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_ALBUM + ASC,
				SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + MediaTableAudioMetadata.TABLE_COL_GENRE + EQUAL + "'${2}' AND " + COALESCE_ARTIST + EQUAL + "'${1}' AND " + MediaTableAudioMetadata.TABLE_COL_ALBUM + EQUAL + "'${0}'" + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_TRACK + ASC + ", " + MediaTableFiles.TABLE_COL_FILENAME + ASC
			},
			new int[]{TEXTS, TEXTS, TEXTS, FILES}
		);
		vfAudio.addChild(mlf7);
		MediaLibraryFolder audioByDate = new MediaLibraryFolder(
			renderer,
			"ByDate",
			new String[]{
				SELECT + FORMATDATETIME_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + ORDER_BY + MediaTableFiles.TABLE_COL_MODIFIED + DESC,
				SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + FORMATDATETIME_MODIFIED + EQUAL + "'${0}'" + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_TRACK + ASC + ", " + MediaTableFiles.TABLE_COL_FILENAME + ASC
			},
			new int[]{TEXTS_NOSORT, FILES}
		);
		vfAudio.addChild(audioByDate);

		MediaLibraryFolder mlf8 = new MediaLibraryFolder(
			renderer,
			"ByLetterArtistAlbum",
			new String[]{
				SELECT + MediaTableRegexpRules.TABLE_COL_ID + FROM + MediaTableRegexpRules.TABLE_NAME + ORDER_BY + MediaTableRegexpRules.TABLE_COL_REGEXP_ORDER + ASC,
				SELECT_DISTINCT + COALESCE_ARTIST + AS + "ARTIST" + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + "ARTIST REGEXP (" + SELECT + MediaTableRegexpRules.TABLE_COL_REGEXP_RULE + FROM + MediaTableRegexpRules.TABLE_NAME + WHERE + MediaTableRegexpRules.TABLE_COL_ID + EQUAL + "'${0}')" + ORDER_BY + "ARTIST" + ASC,
				SELECT_DISTINCT + MediaTableAudioMetadata.TABLE_COL_ALBUM + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + COALESCE_ARTIST + EQUAL + "'${0}'" + ORDER_BY + MediaTableAudioMetadata.TABLE_COL_ALBUM + ASC,
				SELECT + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_MODIFIED + FROM_FILES + ", " + MediaTableAudioMetadata.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID + AND + FORMAT_TYPE_AUDIO + AND + COALESCE_ARTIST + EQUAL + "'${1}' AND " + MediaTableAudioMetadata.TABLE_COL_ALBUM + EQUAL + "'${0}'"
			},
			new int[]{TEXTS, TEXTS, TEXTS, FILES}
		);
		vfAudio.addChild(mlf8);
		addChild(vfAudio);
	}

	private void addImageFolder() {
		LocalizedStoreContainer vfImage = new LocalizedStoreContainer(renderer, "Photo");
		MediaLibraryFolder mlfPhoto01 = new MediaLibraryFolder(
			renderer,
			"AllPhotos",
			FORMAT_TYPE_IMAGE + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC,
			FILES
		);
		vfImage.addChild(mlfPhoto01);
		MediaLibraryFolder photoByDate = new MediaLibraryFolder(
			renderer,
			"ByDate",
			new String[]{
				SELECT + FORMATDATETIME_MODIFIED + FROM_FILES + WHERE + FORMAT_TYPE_IMAGE + ORDER_BY + MediaTableFiles.TABLE_COL_MODIFIED + DESC,
				FORMAT_TYPE_IMAGE + AND + FORMATDATETIME_MODIFIED + EQUAL + "'${0}'" + ORDER_BY + MediaTableFiles.TABLE_COL_FILENAME + ASC
			},
			new int[]{TEXTS_NOSORT, FILES}
		);
		vfImage.addChild(photoByDate);
		addChild(vfImage);
	}

	public MediaLibraryFolder getArtistFolder() {
		return artistFolder;
	}

	public MediaLibraryFolder getGenreFolder() {
		return genreFolder;
	}

	public MediaLibraryFolder getPlaylistFolder() {
		return playlistFolder;
	}

	public MediaLibraryFolder getAllFolder() {
		return allFolder;
	}

	public LocalizedStoreContainer getAudioFolder() {
		return vfAudio;
	}

	public MediaLibraryFolder getAlbumFolder() {
		return albumFolder;
	}
}
